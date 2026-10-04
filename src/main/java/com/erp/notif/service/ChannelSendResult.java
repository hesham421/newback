package com.erp.notif.service;

/**
 * Outcome of a single {@link ChannelProvider} send attempt — success flag plus an optional failure
 * reason recorded on the NOTIF_LOG (errorMessage) when a retry is exhausted (RULE-NOTIF-002). Plain
 * value type, no persistence surface.
 */
public record ChannelSendResult(boolean success, String errorMessage) {

    public static ChannelSendResult ok() {
        return new ChannelSendResult(true, null);
    }

    public static ChannelSendResult failure(String errorMessage) {
        return new ChannelSendResult(false, errorMessage);
    }
}
