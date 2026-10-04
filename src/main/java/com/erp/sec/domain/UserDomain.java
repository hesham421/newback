package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain companion for ENT-SEC-001 (User): the reactivation transition guard (API-SEC-010 — only
 * a DISABLED user may be reactivated → {@code SEC-409-INVALID-TRANSITION}) and username/email
 * uniqueness (API-SEC-006/007, fact = QR-SEC-033 → {@code SEC-409-USER-DUP}). Decides only;
 * {@code User.activate()} / {@code deactivate()} execute the field mutation.
 */
public final class UserDomain {

    /** USER_STATUS codes (A6 closed set, CHK_SEC_USER_STATUS). */
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    /** Request-body field names as UserCreateRequest/UserUpdateRequest spell them. */
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_EMAIL = "email";

    private final String username;
    private final String email;
    private final String statusCode;
    private final boolean active;

    private UserDomain(String username, String email, String statusCode, boolean active) {
        this.username = username;
        this.email = email;
        this.statusCode = statusCode;
        this.active = active;
    }

    /**
     * API-SEC-006 (create user): username and email must both be free (QR-SEC-033).
     *
     * <p>One code covers both fields, and it stays the envelope's top-level {@code code} — but
     * since 2026-09-19 the response also names which of the two actually collided, in the
     * {@code fieldErrors} slot {@code ApiError} already exposes, so a client can show the message
     * on the offending input instead of only as a panel-level message (TC-SEC-042). Both fields
     * are listed when both collide. No wire code changed.
     *
     * @throws LocalizedException {@code SEC-409-USER-DUP}
     */
    public static UserDomain create(String username,
                                    String email,
                                    boolean usernameAlreadyTaken,
                                    boolean emailAlreadyTaken) {
        if (usernameAlreadyTaken || emailAlreadyTaken) {
            throw duplicate(usernameAlreadyTaken, emailAlreadyTaken);
        }
        return new UserDomain(username, email, STATUS_ACTIVE, true);
    }

    /** {@code SEC-409-USER-DUP}, attributed to whichever field(s) were already taken. */
    private static LocalizedException duplicate(boolean usernameAlreadyTaken,
                                                boolean emailAlreadyTaken) {
        List<ErrorDetail> fields = new ArrayList<>();
        if (usernameAlreadyTaken) {
            fields.add(ErrorDetail.ofField(FIELD_USERNAME, SecErrorCodes.SEC_409_USER_DUP));
        }
        if (emailAlreadyTaken) {
            fields.add(ErrorDetail.ofField(FIELD_EMAIL, SecErrorCodes.SEC_409_USER_DUP));
        }
        return LocalizedException.withDetails(
            Status.ALREADY_EXISTS, SecErrorCodes.SEC_409_USER_DUP, fields);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static UserDomain from(User entity) {
        return new UserDomain(entity.getUsername(), entity.getEmail(), entity.getStatusCode(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    /**
     * API-SEC-010 — only a DISABLED user can be reactivated; ACTIVE or PENDING cannot (A7).
     *
     * @throws LocalizedException {@code SEC-409-INVALID-TRANSITION} (Status.CONFLICT → 409)
     */
    public void assertCanReactivate() {
        if (!STATUS_DISABLED.equals(statusCode)) {
            throw new LocalizedException(Status.CONFLICT,
                SecErrorCodes.SEC_409_INVALID_TRANSITION);
        }
    }

    /**
     * API-SEC-007 — email uniqueness excluding the current row
     * ({@code existsByEmailAndUserPkNot}); username is immutable after create, so it has no guard.
     *
     * @throws LocalizedException {@code SEC-409-USER-DUP} (409)
     */
    public void assertEmailAvailable(boolean emailAlreadyTaken) {
        if (emailAlreadyTaken) {
            throw duplicate(false, true);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public boolean isActive() {
        return active;
    }
}
