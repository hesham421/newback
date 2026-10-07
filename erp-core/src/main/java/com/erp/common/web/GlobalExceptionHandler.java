package com.erp.common.web;

import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized exception-to-response mapping — shared infrastructure. Feature modules never
 * declare their own {@code @ControllerAdvice} (see gov-enforce-backend-contract, check CU.7).
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocalizedException(LocalizedException ex) {
        log.warn("Business error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiError.ApiErrorBuilder builder = ApiError.builder()
            .code(ex.getErrorCode())
            .message(resolveMessage(ex.getErrorCode(), ex.getArgs()));
        // Additive (REQ-FIN-015): a multi-error exception also lists every failure, itself
        // included, in the fieldErrors slot ApiError already exposes. A single-code exception
        // carries an empty list and therefore serializes byte-identically to before.
        // A detail that names a request field reports that field; one that does not keeps
        // reporting its error code there, which is what every pre-existing throw relies on.
        if (!ex.getErrors().isEmpty()) {
            builder.fieldErrors(ex.getErrors().stream()
                .map(detail -> FieldErrorItem.builder()
                    .field(detail.field() != null ? detail.field() : detail.errorCode())
                    .message(resolveMessage(detail.errorCode(), detail.args()))
                    .build())
                .toList());
        }
        ApiError error = builder.build();
        return ResponseEntity.status(ex.getStatus().getHttpStatus())
            .body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> FieldErrorItem.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build())
            .toList();
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.VALIDATION_ERROR)
            .message(resolveMessage(CommonErrorCodes.VALIDATION_ERROR, null))
            .fieldErrors(fieldErrors)
            .build();
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    /**
     * Deliberately NOT routed through {@code resolveMessage}: this handler emits the SAME wire code
     * as {@link #handleValidation} ({@code VALIDATION_ERROR}) but a DIFFERENT message, and one
     * bundle key cannot carry two texts. The key {@code VALIDATION_ERROR} is registered with the
     * generic "Validation failed" text that {@link #handleValidation} needs; localizing this
     * sentence too would require either overwriting that text or giving this response its own wire
     * code — a contract change, not a localization change. Left hardcoded and recorded as the
     * remaining half of the PLATFORM I18N finding.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequestBody(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.VALIDATION_ERROR)
            .message("The request body is malformed or does not match the expected structure")
            .build();
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    /**
     * Spring's own request-binding failures: a missing required request parameter, or one that
     * could not be converted to the declared type. Without this handler both fall through to the
     * catch-all and are reported as 500, although they are client errors. Reuses the already
     * registered {@link CommonErrorCodes#VALIDATION_ERROR} wire code and its existing bundle rows
     * — no new code is introduced — and names the offending parameter in {@code fieldErrors},
     * mirroring {@link #handleValidation}'s shape.
     */
    @ExceptionHandler({MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleRequestParameter(Exception ex) {
        String parameterName = ex instanceof MissingServletRequestParameterException missing
            ? missing.getParameterName()
            : ((MethodArgumentTypeMismatchException) ex).getName();
        log.warn("Invalid request parameter [{}]: {}", parameterName, ex.getMessage());
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.VALIDATION_ERROR)
            .message(resolveMessage(CommonErrorCodes.VALIDATION_ERROR, null))
            .fieldErrors(List.of(FieldErrorItem.builder()
                .field(parameterName)
                .message(ex.getMessage())
                .build()))
            .build();
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    /**
     * erp-core 1.3.0 (TM-D review round 1): a multipart request without its required part, or one the
     * container refused (not multipart, or above {@code spring.servlet.multipart.*}), is a client error
     * that fell through to the 500 catch-all. Same {@code VALIDATION_ERROR} code; the part is named in
     * {@code fieldErrors} when Spring knows it.
     */
    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMultipart(Exception ex) {
        String partName = ex instanceof MissingServletRequestPartException missing ? missing.getRequestPartName() : null;
        log.warn("Invalid multipart request [{}]: {}", partName, ex.getMessage());
        ApiError.ApiErrorBuilder error = ApiError.builder()
            .code(CommonErrorCodes.VALIDATION_ERROR)
            .message(resolveMessage(CommonErrorCodes.VALIDATION_ERROR, null));
        if (partName != null) {
            error.fieldErrors(List.of(FieldErrorItem.builder().field(partName).message(ex.getMessage()).build()));
        }
        return ResponseEntity.badRequest().body(ApiResponse.failure(error.build()));
    }

    // Added 2026-09-12 by an explicit recorded human decision, not by any pre-existing
    // requirement: no artifact registered a platform code for HTTP 405. Introduced so that an
    // unsupported HTTP method reports 405 instead of the 500 the catch-all handler previously
    // produced (found by the FIN api-verify run, TC-FIN-016).
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Unsupported HTTP method [{}]: {}", ex.getMethod(), ex.getMessage());
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.METHOD_NOT_ALLOWED)
            .message(resolveMessage(CommonErrorCodes.METHOD_NOT_ALLOWED, null))
            .build();
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        String[] supported = ex.getSupportedMethods();
        if (supported != null && supported.length > 0) {
            response.header(HttpHeaders.ALLOW, String.join(", ", supported));
        }
        return response.body(ApiResponse.failure(error));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.DATA_INTEGRITY_VIOLATION)
            .message(resolveMessage(CommonErrorCodes.DATA_INTEGRITY_VIOLATION, null))
            .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    /** A stale update rejected by the {@code VERSION} optimistic lock (erp-core step 05). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic-lock conflict: {}", ex.getMessage());
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.CONCURRENT_MODIFICATION)
            .message(resolveMessage(CommonErrorCodes.CONCURRENT_MODIFICATION, null))
            .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.ACCESS_DENIED)
            .message(resolveMessage(CommonErrorCodes.ACCESS_DENIED, null))
            .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(error));
    }

    /**
     * erp-core 1.2.0: a path no controller or static resource serves. Spring MVC raises
     * {@link NoResourceFoundException}, which used to reach the catch-all and answer 500. Reached only
     * when the security chain let the request through: an authenticated caller, or an anonymous one
     * on a permitted path (an anonymous request on a protected path still gets 401 first).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        log.debug("No resource: {}", ex.getMessage());
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.NOT_FOUND)
            .message(resolveMessage(CommonErrorCodes.NOT_FOUND, null))
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    /**
     * The catch-all. erp-core 1.2.0: an exception that merely wraps a {@link LocalizedException} —
     * e.g. {@code TENANT_CONTEXT_MISSING}, raised by Hibernate's tenant resolver while the transaction
     * manager opens a session and wrapped in a {@code CannotCreateTransactionException} — is answered
     * as that {@code LocalizedException} (its own code and status) instead of a bare
     * {@code INTERNAL_ERROR}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        LocalizedException wrapped = wrappedLocalizedException(ex);
        if (wrapped != null) {
            log.error("Error wrapped in {}", ex.getClass().getName(), ex);
            return handleLocalizedException(wrapped);
        }
        log.error("Unexpected error", ex);
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.INTERNAL_ERROR)
            .message(resolveMessage(CommonErrorCodes.INTERNAL_ERROR, null))
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }

    /** The first {@link LocalizedException} in the cause chain of {@code ex}, or {@code null}. */
    private static LocalizedException wrappedLocalizedException(Throwable ex) {
        Throwable cause = ex.getCause();
        for (int depth = 0; cause != null && depth < 16; depth++) {
            if (cause instanceof LocalizedException localized) {
                return localized;
            }
            if (cause == cause.getCause()) {
                break;
            }
            cause = cause.getCause();
        }
        return null;
    }

    private String resolveMessage(String code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("No message bundle entry for error code [{}]", code);
            return code;
        }
    }
}
