package com.erp.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** tenant-maturity C4 — RULE-TENANT-025 (key format) and RULE-TENANT-026 (expiry, replay or conflict, what is stored). */
class IdempotencyKeyDomainTest {

    private static final Duration DAY = Duration.ofHours(24);

    @Test
    void aKeyOf1To64LettersDigitsAndDotUnderscoreColonHyphen_isValid() {
        for (String key : List.of("a", "k".repeat(64), "3f2b9c1e-7a4d-4e2f-9b1a-0c5d6e7f8a9b", "01J9Z3K4M5N6P7Q8R9S0T1V2W3",
            "order:42.retry_1")) {
            assertThatCode(() -> IdempotencyKeyDomain.assertKeyValid(key)).as(key).doesNotThrowAnyException();
        }
    }

    @Test
    void anEmptyTooLongOrForeignCharacterKey_is400IdempotencyKeyInvalid() {
        for (String key : List.of("", "k".repeat(65), "bad key", "a/b", "key\n", "ключ", "a+b")) {
            assertThatThrownBy(() -> IdempotencyKeyDomain.assertKeyValid(key))
                .as("'%s'", key)
                .isInstanceOf(LocalizedException.class)
                .satisfies(e -> {
                    assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                    assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(IdempotencyErrorCodes.IDEMPOTENCY_KEY_INVALID);
                });
        }
        assertThatThrownBy(() -> IdempotencyKeyDomain.assertKeyValid(null)).isInstanceOf(LocalizedException.class);
    }

    @Test
    void onlyA2xxAnswer_isStorable() {
        assertThat(IdempotencyKeyDomain.isStorable(200)).isTrue();
        assertThat(IdempotencyKeyDomain.isStorable(201)).isTrue();
        assertThat(IdempotencyKeyDomain.isStorable(299)).isTrue();
        for (int status : List.of(0, 199, 300, 400, 409, 422, 500)) {
            assertThat(IdempotencyKeyDomain.isStorable(status)).as("%d", status).isFalse();
        }
    }

    @Test
    void aKeyExpires_whenItsRetentionHasPassed() {
        Instant now = Instant.parse("2026-10-08T12:00:00Z");
        assertThat(domain("h", "op", now.minus(DAY).plusSeconds(1)).isExpired(now, DAY)).isFalse();
        assertThat(domain("h", "op", now.minus(DAY)).isExpired(now, DAY)).isTrue();
        assertThat(domain("h", "op", now.minus(Duration.ofHours(25))).isExpired(now, DAY)).isTrue();
        assertThat(domain("h", "op", null).isExpired(now, DAY)).isTrue();
    }

    @Test
    void theStoredAnswer_isReplayedForTheSameBodyAndUserOnly_elseIs409IdempotencyKeyConflict() {
        IdempotencyKeyDomain stored = domain("hash-1", "operator-a", Instant.now());

        assertThatCode(() -> stored.assertReplayableFor("hash-1", "operator-a")).doesNotThrowAnyException();
        for (String[] other : new String[][] {{"hash-2", "operator-a"}, {"hash-1", "operator-b"}, {"hash-1", null}}) {
            assertThatThrownBy(() -> stored.assertReplayableFor(other[0], other[1]))
                .isInstanceOf(LocalizedException.class)
                .satisfies(e -> {
                    assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.CONFLICT);
                    assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(IdempotencyErrorCodes.IDEMPOTENCY_KEY_CONFLICT);
                });
        }
    }

    private static IdempotencyKeyDomain domain(String hash, String owner, Instant createdAt) {
        IdempotencyKey row = IdempotencyKey.builder().idempotencyKey("k").endpoint("POST /x").requestHash(hash)
            .responseStatus(IdempotencyKey.CLAIMED_STATUS).build();
        row.setCreatedBy(owner);
        row.setCreatedAt(createdAt);
        return IdempotencyKeyDomain.from(row);
    }
}
