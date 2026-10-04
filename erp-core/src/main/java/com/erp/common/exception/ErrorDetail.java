package com.erp.common.exception;

/**
 * One business failure, carrying exactly what a single-code {@link LocalizedException} carries:
 * a registered error code plus the arguments its message bundle entry substitutes, and optionally
 * the request field the failure belongs to.
 *
 * <p>Shared infrastructure, added additively so that one {@code LocalizedException} can report
 * several failures at once (REQ-FIN-015 / AC-FIN-015 — "returns both failures and posts
 * nothing"). It is deliberately minimal: no severity, no builder. Each detail localizes through
 * {@code GlobalExceptionHandler}'s existing {@code MessageSource} lookup exactly as a single-code
 * exception does.
 *
 * <p>A domain class produces one of these from a non-throwing guard sibling (for example
 * {@code checkBalanced(...)}), and its throwing counterpart turns it straight back into a
 * {@code LocalizedException} — so every rule keeps ONE implementation.
 *
 * @param field     the request field this failure concerns, or {@code null} when the failure is
 *                  not attributable to one field. When null, {@code GlobalExceptionHandler} falls
 *                  back to reporting {@code errorCode} in the {@code fieldErrors[].field} slot,
 *                  which is what every pre-existing multi-error throw relies on.
 * @param errorCode a code registered in a module's {@code <Module>ErrorCodes} class
 * @param args      the message-bundle substitution arguments, may be empty
 */
public record ErrorDetail(String field, String errorCode, Object[] args) {

    /** Convenience factory reading exactly like a {@code LocalizedException} construction. */
    public static ErrorDetail of(String errorCode, Object... args) {
        return new ErrorDetail(null, errorCode, args);
    }

    /**
     * The same, but attributed to a request field so a client can render the message inline on the
     * input that caused it rather than only as a panel-level message.
     */
    public static ErrorDetail ofField(String field, String errorCode, Object... args) {
        return new ErrorDetail(field, errorCode, args);
    }
}
