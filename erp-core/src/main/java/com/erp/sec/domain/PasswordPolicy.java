package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.exception.SecErrorCodes;
import java.util.List;

/**
 * RULE-SEC-056 — the one STAFF password policy (tenant-maturity D): length {@code minLength..maxLength}
 * (code points) and, when required, at least one letter and one digit (any script). Applied wherever a
 * person chooses a staff password: user create, reset completion, admin-set, self-change, a new tenant's
 * first administrator (and package B's tenant admin-reset). Built from
 * {@code erp.core.security.password-policy.*} by {@code com.erp.sec.service.PasswordPolicyProvider}.
 */
public final class PasswordPolicy {

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

    /** A policy with these settings; {@code minLength} is at least 1 and {@code maxLength} at least {@code minLength}. */
    public static PasswordPolicy create(int minLength, int maxLength, boolean requireLetter, boolean requireDigit) {
        int min = Math.max(1, minLength);
        return new PasswordPolicy(min, Math.max(min, maxLength), requireLetter, requireDigit);
    }

    /** Whether {@code rawPassword} meets the policy ({@code null} never does). */
    public boolean accepts(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        int length = rawPassword.codePointCount(0, rawPassword.length());
        if (length < minLength || length > maxLength) {
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
