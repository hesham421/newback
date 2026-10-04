package com.erp.common.exception;

import com.erp.common.domain.status.Status;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public class LocalizedException extends RuntimeException {

    private final Status status;
    private final String errorCode;
    private final transient Object[] args;

    /**
     * Every additional failure this exception reports beyond {@link #errorCode}'s own — empty for
     * every single-code throw, which is the overwhelming majority. See
     * {@link #LocalizedException(Status, List)}.
     */
    private final transient List<ErrorDetail> errors;

    /**
     * The single-code form — unchanged. {@link #errors} stays empty, so
     * {@code GlobalExceptionHandler} renders exactly the {@code code} / {@code message} envelope
     * it always has, with {@code ApiError.fieldErrors} left unset.
     */
    public LocalizedException(Status status, String errorCode, Object... args) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
        this.args = args;
        this.errors = List.of();
    }

    /**
     * The multi-error form (additive). Reports several business failures from one throw: the
     * first detail keeps the existing top-level {@code code}/{@code message} contract, and the
     * complete list — the first included — is surfaced through the {@code fieldErrors} list
     * {@link com.erp.common.web.ApiError} already exposes.
     *
     * <p>Introduced for REQ-FIN-015 / AC-FIN-015 ("returns both failures and posts nothing"). It
     * changes nothing for any existing caller: no existing constructor, {@code Status}, HTTP
     * mapping or {@code ApiError} field was altered.
     *
     * @param status the shared status for the aggregate — all collected failures must map to it
     * @param errors at least one failure; the first supplies the top-level code and arguments
     */
    public LocalizedException(Status status, List<ErrorDetail> errors) {
        super(Objects.requireNonNull(errors, "errors").get(0).errorCode());
        this.status = status;
        this.errorCode = errors.get(0).errorCode();
        this.args = errors.get(0).args();
        this.errors = List.copyOf(errors);
    }

    /**
     * The multi-error form with an explicit top-level code (additive). Used when the aggregate
     * itself has a name the client matches on — {@code SEC-409-USER-DUP} covering a username and
     * an email collision, say — so the envelope's {@code code} must NOT be borrowed from the first
     * detail. The details still populate {@code fieldErrors}.
     *
     * <p>A static factory rather than a constructor: {@code (Status, String, List)} would be
     * ambiguous at the call site with the {@code (Status, String, Object...)} single-code form.
     *
     * @param status    the shared status for the aggregate
     * @param errorCode the top-level wire code and message key
     * @param errors    at least one failure, each rendered into {@code fieldErrors}
     */
    public static LocalizedException withDetails(Status status, String errorCode,
                                                 List<ErrorDetail> errors) {
        return new LocalizedException(status, errorCode,
            Objects.requireNonNull(errors, "errors"), null);
    }

    /** Private disambiguating constructor behind {@link #withDetails}. */
    private LocalizedException(Status status, String errorCode, List<ErrorDetail> errors,
                               Void ignored) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
        this.args = new Object[0];
        this.errors = List.copyOf(errors);
    }
}
