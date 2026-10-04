package com.erp.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Unit tests for the Redis-less {@link DownloadTokenStore}: TTL expiry and single-use consumption. */
class InMemoryDownloadTokenStoreTest {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final InMemoryDownloadTokenStore store = new InMemoryDownloadTokenStore(clock);

    @Test
    void storedValueIsReadableUntilConsumed_andConsumableExactlyOnce() {
        store.put("k", "alice", TTL);

        assertThat(store.get("k")).isEqualTo("alice");
        assertThat(store.consume("k")).isTrue();
        assertThat(store.consume("k")).isFalse();
        assertThat(store.get("k")).isNull();
    }

    @Test
    void expiredEntryIsAbsent_andCannotBeConsumed() {
        store.put("k", "alice", TTL);
        clock.advance(TTL);

        assertThat(store.get("k")).isNull();
        assertThat(store.consume("k")).isFalse();
    }

    @Test
    void unknownKeyIsAbsent() {
        assertThat(store.get("missing")).isNull();
        assertThat(store.consume("missing")).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
