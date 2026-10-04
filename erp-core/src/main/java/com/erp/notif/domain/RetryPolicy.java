package com.erp.notif.domain;

/**
 * RULE-NOTIF-002 (DRV-003) — pure retry policy for dispatch send failures: at most 5 attempts, base
 * delay 2s, ×1.5 exponential backoff, then FAILED. A plain value object — no Spring/JPA — computing
 * the attempt ceiling and per-attempt backoff. Actual asynchronous scheduling of the delay is an
 * in-process implementation detail (no external broker); this object only computes the numbers so the
 * retry-then-FAILED path is exercisable.
 */
public final class RetryPolicy {

    public static final int MAX_ATTEMPTS = 5;
    public static final long BASE_DELAY_MILLIS = 2000L;
    public static final double BACKOFF_MULTIPLIER = 1.5d;

    /** Total attempts allowed before the log is marked FAILED (RULE-NOTIF-002). */
    public int maxAttempts() {
        return MAX_ATTEMPTS;
    }

    /**
     * Backoff before the next retry, given a 1-based attempt number: 2000ms × 1.5^(n-1). Returns 0
     * for a non-positive attempt number.
     */
    public long backoffMillis(int attemptNumber) {
        if (attemptNumber < 1) {
            return 0L;
        }
        return (long) (BASE_DELAY_MILLIS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber - 1));
    }

    /** Whether the given number of consumed attempts has reached the ceiling (RULE-NOTIF-002). */
    public boolean isExhausted(int attemptCount) {
        return attemptCount >= MAX_ATTEMPTS;
    }
}
