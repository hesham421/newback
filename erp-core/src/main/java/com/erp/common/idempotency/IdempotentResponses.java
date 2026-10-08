package com.erp.common.idempotency;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.crypto.Mac;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The response helper of an endpoint that honours {@code Idempotency-Key} (tenant-maturity C4, ADR-TENANT-003): without
 * a key it is {@link OperationCode#craftResponse}; with one it replays or refuses a stored answer, or runs the action in
 * one transaction with the key's row and stores a 2xx answer (RULE-TENANT-025, -026). Logs never name the key or body.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentResponses {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    /** A lost claim race is read back once more; a second loss (a purge in between) answers a conflict. */
    private static final int MAX_CLAIM_ATTEMPTS = 2;

    /** The request hash is taken over this canonical form, independent of the application's JSON settings. */
    private static final JsonMapper CANONICAL = JsonMapper.builder()
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build();

    private final IdempotencyKeyRepository repository;
    private final OperationCode operationCode;
    private final PlatformTransactionManager transactionManager;
    private final JsonMapper jsonMapper;
    private final IdempotencySettings settings;

    /**
     * {@code endpoint} is the consumer's constant id ({@code POST /api/v1/platform/tenants}); {@code request} the bound
     * body; {@code action} must run in the caller's transaction (a {@code @Transactional} service method joins it).
     */
    public <T> ResponseEntity<ApiResponse<T>> craftResponse(String idempotencyKey, String endpoint, Object request,
                                                            Class<T> dataType, Supplier<ServiceResult<T>> action) {
        if (idempotencyKey == null || !settings.enabled()) {
            return operationCode.craftResponse(action.get());
        }
        IdempotencyKeyDomain.assertKeyValid(idempotencyKey);
        String requestHash = hash(request);
        String caller = SecurityContextHelper.getCurrentUsername();

        for (int attempt = 1; attempt <= MAX_CLAIM_ATTEMPTS; attempt++) {
            Optional<IdempotencyKey> stored = findLive(idempotencyKey, endpoint);
            if (stored.isPresent()) {
                IdempotencyKeyDomain.from(stored.get()).assertReplayableFor(requestHash, caller);
                log.info("Replaying the stored answer of {}", endpoint);
                return replay(stored.get(), dataType);
            }
            Optional<ResponseEntity<ApiResponse<T>>> answered = runClaimed(idempotencyKey, endpoint, requestHash, action);
            if (answered.isPresent()) {
                return answered.get();
            }
        }
        throw new LocalizedException(Status.CONFLICT, IdempotencyErrorCodes.IDEMPOTENCY_KEY_CONFLICT);
    }

    /** The live row of the key in the current tenant; an expired one is deleted and reported absent. */
    private Optional<IdempotencyKey> findLive(String idempotencyKey, String endpoint) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Optional<IdempotencyKey> stored = repository.findByIdempotencyKeyAndEndpoint(idempotencyKey, endpoint);
            if (stored.isPresent() && IdempotencyKeyDomain.from(stored.get()).isExpired(Instant.now(), settings.retention())) {
                repository.deleteExpired(stored.get().getId());
                log.info("Deleted an expired idempotency key of {}", endpoint);
                return Optional.<IdempotencyKey>empty();
            }
            return stored;
        });
    }

    /**
     * Claims the key, runs the action and stores a 2xx answer, all in one transaction; anything else rolls back. Empty
     * when a concurrent request committed the same key first (the claim waited on {@code UQ_CORE_IDEMPOTENCY_KEY}).
     */
    private <T> Optional<ResponseEntity<ApiResponse<T>>> runClaimed(String idempotencyKey, String endpoint,
                                                                    String requestHash, Supplier<ServiceResult<T>> action) {
        AtomicBoolean claimed = new AtomicBoolean();
        try {
            return Optional.ofNullable(new TransactionTemplate(transactionManager).execute(status -> {
                IdempotencyKey claim = repository.saveAndFlush(IdempotencyKey.claim(idempotencyKey, endpoint, requestHash));
                claimed.set(true);
                ResponseEntity<ApiResponse<T>> response = operationCode.craftResponse(action.get());
                int httpStatus = response.getStatusCode().value();
                if (IdempotencyKeyDomain.isStorable(httpStatus)) {
                    claim.answer(httpStatus, jsonMapper.writeValueAsString(response.getBody()));
                    repository.saveAndFlush(claim);
                    log.info("Stored the answer of {} ({})", endpoint, httpStatus);
                } else {
                    status.setRollbackOnly();
                }
                return response;
            }));
        } catch (DataIntegrityViolationException e) {
            if (claimed.get()) {
                throw e;
            }
            log.info("The idempotency key of {} was claimed by a concurrent request; reading its answer", endpoint);
            return Optional.empty();
        }
    }

    /** The stored status and envelope ({@code data} and {@code timestamp} of the first answer), marked as a replay. */
    private <T> ResponseEntity<ApiResponse<T>> replay(IdempotencyKey stored, Class<T> dataType) {
        JsonNode body = stored.getResponseBody() == null ? null : jsonMapper.readTree(stored.getResponseBody());
        JsonNode data = body == null ? null : body.get("data");
        JsonNode timestamp = body == null ? null : body.get("timestamp");
        ApiResponse<T> envelope = ApiResponse.<T>builder()
            .success(true)
            .data(data == null || data.isNull() ? null : jsonMapper.treeToValue(data, dataType))
            .timestamp(timestamp == null || timestamp.isNull() ? null : jsonMapper.treeToValue(timestamp, Instant.class))
            .build();
        return ResponseEntity.status(stored.getResponseStatus()).header(REPLAYED_HEADER, "true").body(envelope);
    }

    /** Lower-case hex HMAC-SHA256 of the canonical JSON of the bound request (a password in it stays unverifiable). */
    private String hash(Object request) {
        try {
            Mac mac = Mac.getInstance(IdempotencySettings.HASH_ALGORITHM);
            mac.init(settings.hashKey());
            return HexFormat.of().formatHex(mac.doFinal(CANONICAL.writeValueAsBytes(request)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable in this JVM", e);
        }
    }
}
