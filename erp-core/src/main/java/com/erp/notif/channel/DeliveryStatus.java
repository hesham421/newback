package com.erp.notif.channel;

/** Outcome of one {@link ChannelProvider#send} call. */
public enum DeliveryStatus {

    /** Delivered; final ({@code NOTIF_LOG} → {@code SENT}). */
    SENT,

    /** No provider handles the channel; final, never retried ({@code NOTIF_LOG} → {@code SKIPPED_NO_PROVIDER}). */
    SKIPPED_NO_PROVIDER,

    /** This attempt failed; NOTIF retries, then marks the row {@code FAILED}. */
    FAILED
}
