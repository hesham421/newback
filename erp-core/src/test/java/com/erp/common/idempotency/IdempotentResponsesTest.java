package com.erp.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * tenant-maturity C4 — {@link IdempotentResponses} without a database (RULE-TENANT-025 / -026): the header ignored when
 * absent or disabled, refused when invalid, a non-2xx answer never stored, a stored answer replayed, a lost claim race.
 */
class IdempotentResponsesTest {

    private static final String ENDPOINT = "POST /api/v1/things";
    private static final String KEY = "3f2b9c1e-7a4d-4e2f-9b1a-0c5d6e7f8a9b";

    record Payload(String name) {
    }

    private final IdempotencyKeyRepository repository = mock(IdempotencyKeyRepository.class);
    private final IdempotencyKeyClaims claims = mock(IdempotencyKeyClaims.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final AtomicInteger runs = new AtomicInteger();
    private SimpleTransactionStatus lastTransaction;
    private String claimedHash;

    @BeforeEach
    void transactions() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> lastTransaction = new SimpleTransactionStatus());
        when(repository.saveAndFlush(any(IdempotencyKey.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByIdempotencyKeyAndEndpoint(anyString(), anyString())).thenReturn(Optional.empty());
        when(claims.claim(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            claimedHash = invocation.getArgument(2);
            return true;
        });
    }

    @Test
    void withoutAKey_orDisabled_theActionRunsAsBefore_andNoRowIsReadOrWritten() {
        ResponseEntity<ApiResponse<Payload>> plain = responses(true).craftResponse(null, ENDPOINT, "body", Payload.class,
            this::created);
        ResponseEntity<ApiResponse<Payload>> disabled = responses(false).craftResponse("bad key!", ENDPOINT, "body",
            Payload.class, this::created);

        assertThat(plain.getStatusCode().value()).isEqualTo(201);
        assertThat(disabled.getStatusCode().value()).isEqualTo(201);
        assertThat(disabled.getHeaders().containsHeader(IdempotentResponses.REPLAYED_HEADER)).isFalse();
        assertThat(runs).hasValue(2);
        verifyNoInteractions(repository, claims, transactionManager);
    }

    @Test
    void anInvalidKey_is400_beforeAnythingIsReadOrRun() {
        assertThatThrownBy(() -> responses(true).craftResponse("", ENDPOINT, "body", Payload.class, this::created))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(IdempotencyErrorCodes.IDEMPOTENCY_KEY_INVALID);
        assertThat(runs).hasValue(0);
        verifyNoInteractions(repository, claims);
    }

    @Test
    void a2xxAnswer_isStoredInTheClaimsTransaction_andReplayedForTheSameBody_withoutRunningAgain() {
        IdempotencyKey row = row();
        when(repository.findByIdempotencyKeyAndEndpoint(KEY, ENDPOINT)).thenReturn(Optional.empty(), Optional.of(row));
        IdempotentResponses responses = responses(true);
        ResponseEntity<ApiResponse<Payload>> first = responses.craftResponse(KEY, ENDPOINT, new Payload("same"),
            Payload.class, this::created);

        ArgumentCaptor<IdempotencyKey> saved = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue()).isSameAs(row);
        assertThat(row.getResponseStatus()).isEqualTo(201);
        assertThat(row.getResponseBody()).contains("\"name\":\"thing\"");
        assertThat(claimedHash).matches("[0-9a-f]{64}");
        assertThat(lastTransaction.isRollbackOnly()).isFalse();

        row.setRequestHash(claimedHash);
        when(repository.findByIdempotencyKeyAndEndpoint(KEY, ENDPOINT)).thenReturn(Optional.of(row));
        ResponseEntity<ApiResponse<Payload>> replay = responses.craftResponse(KEY, ENDPOINT, new Payload("same"),
            Payload.class, this::created);

        assertThat(replay.getStatusCode().value()).isEqualTo(201);
        assertThat(replay.getHeaders().getFirst(IdempotentResponses.REPLAYED_HEADER)).isEqualTo("true");
        assertThat(replay.getBody().getData()).isEqualTo(first.getBody().getData());
        assertThat(replay.getBody().getTimestamp()).isEqualTo(first.getBody().getTimestamp());
        assertThat(runs).as("the action ran once").hasValue(1);

        assertThatThrownBy(() -> responses.craftResponse(KEY, ENDPOINT, new Payload("other"), Payload.class, this::created))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(IdempotencyErrorCodes.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    void aNon2xxAnswer_rollsTheClaimBack_andStoresNothing() {
        ResponseEntity<ApiResponse<Payload>> answer = responses(true).craftResponse(KEY, ENDPOINT, "body", Payload.class,
            () -> ServiceResult.success(new Payload("odd"), Status.CONFLICT));

        assertThat(answer.getStatusCode().value()).isEqualTo(409);
        assertThat(lastTransaction.isRollbackOnly()).isTrue();
        verify(claims).claim(any(), any(), any());
        verify(repository, never()).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    void aClaimLostToAConcurrentRequest_readsAndReplaysItsAnswer_withoutRunningTheAction() {
        when(claims.claim(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            claimedHash = invocation.getArgument(2);
            return false;
        });
        IdempotencyKey winner = row();
        winner.answer(201, "{\"success\":true,\"data\":{\"name\":\"winner\"},\"timestamp\":\"2026-10-08T08:00:00Z\"}");
        when(repository.findByIdempotencyKeyAndEndpoint(KEY, ENDPOINT))
            .thenReturn(Optional.empty())
            .thenAnswer(invocation -> {
                winner.setRequestHash(claimedHash);
                return Optional.of(winner);
            });

        ResponseEntity<ApiResponse<Payload>> answer = responses(true).craftResponse(KEY, ENDPOINT, "body", Payload.class,
            this::created);

        assertThat(answer.getHeaders().getFirst(IdempotentResponses.REPLAYED_HEADER)).isEqualTo("true");
        assertThat(answer.getBody().getData()).isEqualTo(new Payload("winner"));
        assertThat(runs).as("the loser never ran the action").hasValue(0);
        verify(repository, never()).saveAndFlush(any(IdempotencyKey.class));
        verify(repository, never()).deleteExpired(any());
    }

    private static IdempotencyKey row() {
        IdempotencyKey row = IdempotencyKey.builder().idempotencyKey(KEY).endpoint(ENDPOINT).requestHash("pending")
            .responseStatus(IdempotencyKey.CLAIMED_STATUS).build();
        row.setCreatedBy(SecurityContextHelper.getCurrentUsername());
        row.setCreatedAt(Instant.now());
        return row;
    }

    private IdempotentResponses responses(boolean enabled) {
        return new IdempotentResponses(repository, claims, new OperationCode(), transactionManager,
            JsonMapper.builder().build(),
            IdempotencySettings.of(enabled, Duration.ofHours(24), "test-only-secret-0123456789abcdef0123456789"));
    }

    private ServiceResult<Payload> created() {
        runs.incrementAndGet();
        return ServiceResult.success(new Payload("thing"), Status.CREATED);
    }
}
