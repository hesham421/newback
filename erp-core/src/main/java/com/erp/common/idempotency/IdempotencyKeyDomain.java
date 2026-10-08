package com.erp.common.idempotency;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The rules of the {@code Idempotency-Key} mechanism: the key's format (RULE-TENANT-025), and when a stored key is
 * expired, replayed or refused, and which answers are stored (RULE-TENANT-026). No Spring/JPA annotations, no
 * repository; the caller passes every fact in.
 */
public final class IdempotencyKeyDomain {

    /** RULE-TENANT-025: 1 to 64 characters of letters, digits and {@code . _ : -} (a UUID or a ULID fits). */
    public static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    private final String requestHash;
    private final String owner;
    private final Instant createdAt;

    private IdempotencyKeyDomain(String requestHash, String owner, Instant createdAt) {
        this.requestHash = requestHash;
        this.owner = owner;
        this.createdAt = createdAt;
    }

    public static IdempotencyKeyDomain from(IdempotencyKey stored) {
        return new IdempotencyKeyDomain(stored.getRequestHash(), stored.getCreatedBy(), stored.getCreatedAt());
    }

    /** RULE-TENANT-025: a header that is present must match {@link #KEY_PATTERN}; an empty value does not. */
    public static void assertKeyValid(String idempotencyKey) {
        if (idempotencyKey == null || !KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new LocalizedException(Status.VALIDATION_ERROR, IdempotencyErrorCodes.IDEMPOTENCY_KEY_INVALID);
        }
    }

    /** RULE-TENANT-026: only a successful (2xx) answer is stored; anything else leaves no row. */
    public static boolean isStorable(int httpStatus) {
        return httpStatus >= 200 && httpStatus < 300;
    }

    /** RULE-TENANT-026: a key older than the retention is treated as unused. */
    public boolean isExpired(Instant now, Duration retention) {
        return createdAt == null || !createdAt.plus(retention).isAfter(now);
    }

    /** RULE-TENANT-026: the stored answer is replayed only for the same request body and the same user. */
    public void assertReplayableFor(String requestHash, String caller) {
        if (!Objects.equals(this.requestHash, requestHash) || !Objects.equals(this.owner, caller)) {
            throw new LocalizedException(Status.CONFLICT, IdempotencyErrorCodes.IDEMPOTENCY_KEY_CONFLICT);
        }
    }
}
