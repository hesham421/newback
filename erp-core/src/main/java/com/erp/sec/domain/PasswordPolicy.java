package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.exception.SecErrorCodes;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RULE-SEC-056 — the one STAFF password policy (tenant-maturity D): length {@code minLength..maxLength}
 * (code points, at most {@value #MAX_BYTES}), at most {@value #MAX_BYTES} UTF-8 bytes (BCrypt hashes no
 * more) and, when required, a letter and a digit (any script). Built from
 * {@code erp.core.security.password-policy.*} by {@code PasswordPolicyProvider}; {@link #CUSTOMER} holds the
 * customer realm to its 8-character minimum and the same byte limit.
 */
public final class PasswordPolicy {

    /** The most bytes BCrypt hashes; a longer password cannot be stored (review round 1). */
    public static final int MAX_BYTES = 72;

    /** Customer passwords: 8..72 characters and bytes, no composition rule (only the hash limit is added). */
    public static final PasswordPolicy CUSTOMER = create(8, MAX_BYTES, false, false);

    private final int minLength;
    private final int maxLength;
    private final boolean requireLetter;
    private final boolean requireDigit;

    private PasswordPolicy(int minLength, int maxLength, boolean requireLetter, boolean requireDigit) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.requireLetter = requireLetter;
        this.requireDigit = requireDigit;
    }

    /** A policy with these settings, lengths clamped to {@code 1 <= minLength <= maxLength <= MAX_BYTES}. */
    public static PasswordPolicy create(int minLength, int maxLength, boolean requireLetter, boolean requireDigit) {
        int max = Math.min(MAX_BYTES, Math.max(1, maxLength));
        int min = Math.min(max, Math.max(1, minLength));
        return new PasswordPolicy(min, max, requireLetter, requireDigit);
    }

    /** Whether {@code rawPassword} meets the policy ({@code null} never does). */
    public boolean accepts(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        int length = rawPassword.codePointCount(0, rawPassword.length());
        if (length < minLength || length > maxLength
            || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return false;
        }
        boolean hasLetter = rawPassword.codePoints().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.codePoints().anyMatch(Character::isDigit);
        return (!requireLetter || hasLetter) && (!requireDigit || hasDigit);
    }

    /**
     * RULE-SEC-056 — refuses a password the policy does not accept, naming the request field.
     *
     * @throws LocalizedException {@code SEC-400-PASSWORD-POLICY} (400) with {@code fieldErrors[0].field = field}
     */
    public void assertAcceptable(String field, String rawPassword) {
        if (!accepts(rawPassword)) {
            throw new LocalizedException(Status.VALIDATION_ERROR, List.of(
                ErrorDetail.ofField(field, SecErrorCodes.SEC_400_PASSWORD_POLICY, minLength, maxLength)));
        }
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public boolean isRequireLetter() {
        return requireLetter;
    }

    public boolean isRequireDigit() {
        return requireDigit;
    }
}
