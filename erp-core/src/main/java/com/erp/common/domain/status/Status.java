package com.erp.common.domain.status;

import org.springframework.http.HttpStatus;

public enum Status {

    SUCCESS(HttpStatus.OK),
    CREATED(HttpStatus.CREATED),
    UPDATED(HttpStatus.OK),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    ALREADY_EXISTS(HttpStatus.CONFLICT),
    CONFLICT(HttpStatus.CONFLICT),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    Status(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
