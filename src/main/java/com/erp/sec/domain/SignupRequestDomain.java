package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.SignupRequest;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-013 (SignupRequest): API-SEC-011's A7 lifecycle guard — only a
 * PENDING request may transition, APPROVED and REJECTED are both terminal →
 * {@code SEC-409-INVALID-TRANSITION} — plus API-SEC-002's email-availability decision
 * ({@code SEC-409-SIGNUP-DUP}).
 */
public final class SignupRequestDomain {

    /** SIGNUP_STATUS codes (A6 closed set, CHK_SEC_SIGNUP_REQUEST_STATUS). */
    private static final String STATUS_PENDING = "PENDING";

    private final String email;
    private final String statusCode;

    private SignupRequestDomain(String email, String statusCode) {
        this.email = email;
        this.statusCode = statusCode;
    }

    /**
     * API-SEC-002 (submit sign-up): the email must be free of both an existing User.email and an
     * already-PENDING SignupRequest.email.
     *
     * @throws LocalizedException {@code SEC-409-SIGNUP-DUP} (Status.ALREADY_EXISTS → 409)
     */
    public static SignupRequestDomain create(String email, boolean emailAlreadyTaken) {
        if (emailAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SecErrorCodes.SEC_409_SIGNUP_DUP);
        }
        return new SignupRequestDomain(email, STATUS_PENDING);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static SignupRequestDomain from(SignupRequest entity) {
        return new SignupRequestDomain(entity.getEmail(), entity.getStatusCode());
    }

    /**
     * API-SEC-011 (approve / reject) — APPROVED and REJECTED are both terminal.
     *
     * @throws LocalizedException {@code SEC-409-INVALID-TRANSITION} (Status.CONFLICT → 409)
     */
    public void assertCanDecide() {
        if (!STATUS_PENDING.equals(statusCode)) {
            throw new LocalizedException(Status.CONFLICT,
                SecErrorCodes.SEC_409_INVALID_TRANSITION);
        }
    }

    public String getEmail() {
        return email;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
