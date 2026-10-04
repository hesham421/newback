package com.erp.sec.security;

import org.springframework.security.core.Authentication;

/**
 * erp-core step 06 — the auth realm of an authenticated request, attached by
 * {@link JwtAuthenticationFilter} as the {@link Authentication#getDetails() details} of the
 * authentication it installs (taken from the token's {@code realm} claim). {@link RealmEnforcementFilter}
 * compares it with the realm its security chain serves.
 *
 * @param realm {@code STAFF} or {@code CUSTOMER} ({@code SEC_USER.REALM})
 */
public record AuthRealm(String realm) {

    /** The realm of {@code authentication}, or {@code null} when it carries none (e.g. anonymous). */
    public static String of(Authentication authentication) {
        return authentication != null && authentication.getDetails() instanceof AuthRealm details
            ? details.realm() : null;
    }
}
