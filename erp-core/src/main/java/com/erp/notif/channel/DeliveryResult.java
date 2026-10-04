package com.erp.notif.channel;

import java.util.Objects;

/** What a {@link ChannelProvider} reports for one message: a {@link DeliveryStatus} plus a reason. */
public record DeliveryResult(DeliveryStatus status, String detail) {

    public DeliveryResult {
        Objects.requireNonNull(status, "status");
    }

    public static DeliveryResult sent() {
        return new DeliveryResult(DeliveryStatus.SENT, null);
    }

    public static DeliveryResult skippedNoProvider(String reason) {
        return new DeliveryResult(DeliveryStatus.SKIPPED_NO_PROVIDER, reason);
    }

    public static DeliveryResult failed(String error) {
        return new DeliveryResult(DeliveryStatus.FAILED, error);
    }

    /** A permanent failure ({@link DeliveryStatus#REJECTED}): no retry, the row ends {@code FAILED}. */
    public static DeliveryResult rejected(String reason) {
        return new DeliveryResult(DeliveryStatus.REJECTED, reason);
    }
}
