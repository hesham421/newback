package com.erp.sec.domain;

import com.erp.common.domain.DomainRules;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Domain companion for ENT-SEC-001 (User): the reactivation transition guard (API-SEC-010 — only
 * a DISABLED user may be reactivated → {@code SEC-409-INVALID-TRANSITION}) and username/email
 * uniqueness (API-SEC-006/007, fact = QR-SEC-033 → {@code SEC-409-USER-DUP}); since tenant-maturity D
 * also RULE-SEC-057/058/061 (passwords, photo). Decides only; the entity executes the field mutation.
 */
public final class UserDomain {

    /** USER_STATUS codes (A6 closed set, CHK_SEC_USER_STATUS). */
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";

    /**
     * PASSWORD_HASH the core seed ({@code V7__sec_seed.sql}) gives the bootstrap {@code admin}
     * account. It is not a BCrypt hash, so no password matches it; the two must stay identical.
     */
    public static final String BOOTSTRAP_PASSWORD_PLACEHOLDER = "BOOTSTRAP-PASSWORD-NOT-SET";

    /** RULE-SEC-061 (tenant-maturity D) — a profile photo is at most 1 MB (plan §6 D.4). */
    public static final long PHOTO_MAX_BYTES = 1_048_576L;

    /** RULE-SEC-061 — PNG, JPEG, WebP; never SVG (logos only, RULE-FILE-009). */
    public static final Set<String> PHOTO_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    /** RULE-FILE-005 owner of a photo document: {@code SEC_USER} / user id / module {@code SEC}. */
    public static final String PHOTO_OWNER_TYPE = "SEC_USER";
    public static final String PHOTO_MODULE_CODE = "SEC";

    /** Base name of a stored photo; FILE adds the extension of the detected type (never the client's name). */
    public static final String PHOTO_BASE_NAME = "photo";

    /** The multipart part name of a photo upload. */
    private static final String FIELD_PHOTO_FILE = "file";

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

    /**
     * erp-core step 06 — customer self-registration: the e-mail must not have a customer account in
     * the tenant yet (the same person may still hold a staff account). The new account starts
     * {@code PENDING_VERIFICATION}.
     *
     * @throws LocalizedException {@code CUSTOMER_EMAIL_TAKEN} (Status.ALREADY_EXISTS → 409)
     */
    public static UserDomain createCustomer(String email, boolean customerEmailTaken) {
        DomainRules.assertUnique(customerEmailTaken, SecErrorCodes.CUSTOMER_EMAIL_TAKEN);
        return new UserDomain(email, email, STATUS_PENDING_VERIFICATION, true);
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
     * Whether {@code entity} is still the un-initialised bootstrap account of the core seed:
     * status {@code PENDING} and exactly the seed's placeholder hash. Only such an account may
     * receive the configured bootstrap password; once it has a real hash (or any other status) it
     * is never touched again, so the bootstrap property cannot overwrite a password later.
     */
    public static boolean awaitsBootstrapPassword(User entity) {
        return STATUS_PENDING.equals(entity.getStatusCode())
            && BOOTSTRAP_PASSWORD_PLACEHOLDER.equals(entity.getPasswordHash());
    }

    /**
     * erp-core step 06 — a customer whose password matched may log in only once the e-mail is verified.
     * Called after the credential check, so the code never reveals an account to a wrong password.
     *
     * @throws LocalizedException {@code CUSTOMER_NOT_VERIFIED} (Status.FORBIDDEN → 403)
     */
    public void assertCustomerVerified() {
        if (STATUS_PENDING_VERIFICATION.equals(statusCode)) {
            throw new LocalizedException(Status.FORBIDDEN, SecErrorCodes.CUSTOMER_NOT_VERIFIED);
        }
    }

    /** erp-core step 06 — whether the account still awaits its e-mail verification. */
    public boolean awaitsVerification() {
        return STATUS_PENDING_VERIFICATION.equals(statusCode);
    }

    /**
     * Whether the account may receive notifications (REQ-SEC-034's {@code UserContact.active}): an
     * ACTIVE account, and since erp-core step 06 a customer awaiting its e-mail verification — the
     * verification mail is exactly what it must receive.
     */
    public boolean canReceiveNotifications() {
        return STATUS_ACTIVE.equals(statusCode) || STATUS_PENDING_VERIFICATION.equals(statusCode);
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

    /**
     * RULE-SEC-058 (tenant-maturity D, ADR-SEC-063) — whether a password an administrator chose (user
     * create, admin-set) must be changed by its owner: unless the request says {@code false}, yes.
     */
    public static boolean passwordChangeRequiredFor(Boolean requireChangeAtNextLogin) {
        return requireChangeAtNextLogin == null || requireChangeAtNextLogin;
    }

    /**
     * RULE-SEC-057 — the administrator-set endpoint never targets the caller's own account; one's own
     * password changes through {@code PUT /api/v1/sec/me/password}, which asks for the current one.
     *
     * @throws LocalizedException {@code SEC-422-PASSWORD-SELF} (Status.BUSINESS_RULE_VIOLATION → 422)
     */
    public static void assertNotSelfForAdminPasswordSet(boolean targetIsCaller) {
        if (targetIsCaller) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION, SecErrorCodes.SEC_422_PASSWORD_SELF);
        }
    }

    /**
     * RULE-SEC-060 — a self-change needs the current password ({@code PasswordEncoder.matches}, checked by
     * the service as at login, ADR-SEC-002).
     *
     * @throws LocalizedException {@code SEC-403-PASSWORD-CURRENT-INVALID} (Status.FORBIDDEN → 403)
     */
    public static void assertCurrentPasswordMatches(boolean matches) {
        if (!matches) {
            throw new LocalizedException(Status.FORBIDDEN, SecErrorCodes.SEC_403_PASSWORD_CURRENT_INVALID);
        }
    }

    /**
     * RULE-SEC-061 — refuses a profile photo FILE's image store did not accept against
     * {@link #PHOTO_MAX_BYTES} / {@link #PHOTO_TYPES}.
     *
     * @throws LocalizedException {@code SEC-400-PHOTO-INVALID} (400) with {@code fieldErrors[0].field = file}
     */
    public static void assertPhotoAccepted(boolean accepted) {
        if (!accepted) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                List.of(ErrorDetail.ofField(FIELD_PHOTO_FILE, SecErrorCodes.SEC_400_PHOTO_INVALID)));
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
