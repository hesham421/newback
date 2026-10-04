package com.erp.notif.service;

/**
 * Internal retry signal of the asynchronous delivery (erp-core step 08): thrown by
 * {@link NotificationDeliveryWorker#deliver} after a failed attempt has been recorded, so that Spring
 * Retry schedules the next attempt. It never leaves the worker — once the attempts are exhausted the
 * worker's {@code @Recover} method marks the row {@code FAILED} — and so never reaches a client; it is
 * a control-flow signal, not a business error (no error code).
 */
class DeliveryAttemptFailedException extends RuntimeException {

    DeliveryAttemptFailedException(String message) {
        super(message);
    }

    DeliveryAttemptFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
