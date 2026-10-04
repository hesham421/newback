package com.erp.notif.channel;

/** Outcome of one {@link ChannelProvider#send} call. */
public enum DeliveryStatus {

    /** Delivered; final ({@code NOTIF_LOG} → {@code SENT}). */
    SENT,

    /** No provider handles the channel; final, never retried ({@code NOTIF_LOG} → {@code SKIPPED_NO_PROVIDER}). */
    SKIPPED_NO_PROVIDER,

    /** This attempt failed; NOTIF retries, then marks the row {@code FAILED}. */
    FAILED,

    /**
     * erp-core step 14 — the message can never be delivered as it stands (for example an EMAIL with
     * no recipient address), so retrying cannot help: NOTIF marks the row {@code FAILED} at once,
     * after this single attempt.
     */
    REJECTED
}
