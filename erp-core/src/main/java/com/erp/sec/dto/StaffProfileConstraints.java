package com.erp.sec.dto;

/**
 * tenant-maturity D — the bean-validation patterns shared by the staff profile fields of
 * {@link UserCreateRequest}, {@link UserUpdateRequest} and {@link StaffProfileUpdateRequest}. An empty
 * string passes the phone and locale patterns: on update and patch it clears the field.
 */
public final class StaffProfileConstraints {

    /** E.164-ish (DBF-SEC-117): optional {@code +}, digits, spaces, hyphens, 7..30 characters; or empty. */
    public static final String PHONE_PATTERN = "^$|^\\+?[0-9][0-9 -]{5,28}[0-9]$";

    /** RULE-SEC-062 / CHK_SEC_USER_LOCALE: {@code ar}, {@code en}, or empty. */
    public static final String LOCALE_PATTERN = "^$|^(ar|en)$";

    /** A name that, when present, is not blank (PATCH: null = unchanged). */
    public static final String NOT_BLANK_PATTERN = "(?s).*\\S.*";

    private StaffProfileConstraints() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }
}
