package com.erp.sec.service;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.sec.domain.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * tenant-maturity D — builds the one STAFF {@link PasswordPolicy} (RULE-SEC-056) from
 * {@code erp.core.security.password-policy.*}. Every SEC path that sets a person-chosen staff password
 * asks it (user create, reset completion, admin-set, self-change, tenant first administrator; package B's
 * tenant admin-reset reuses it).
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicyProvider {

    private final ErpCoreProperties properties;

    /** The policy as currently configured. */
    public PasswordPolicy current() {
        ErpCoreProperties.PasswordPolicySettings settings = properties.getSecurity().getPasswordPolicy();
        return PasswordPolicy.create(settings.getMinLength(), settings.getMaxLength(),
            settings.isRequireLetter(), settings.isRequireDigit());
    }
}
