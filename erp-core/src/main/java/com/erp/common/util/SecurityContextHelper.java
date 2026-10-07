package com.erp.common.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextHelper {

    private static final String SYSTEM_USER = "system";

    /** Realm of a back-office (staff) caller. */
    public static final String REALM_STAFF = "STAFF";

    /** Realm of a self-registered customer (storefront account). */
    public static final String REALM_CUSTOMER = "CUSTOMER";

    /** Realm of code running without a caller (startup runners, jobs, asynchronous workers). */
    public static final String REALM_SYSTEM = "SYSTEM";

    /** The single authority of a customer-realm caller. */
    public static final String CUSTOMER_AUTHORITY = "ROLE_CUSTOMER";

    private SecurityContextHelper() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return SYSTEM_USER;
        }
        return authentication.getName();
    }

    /**
     * Whether the current principal holds {@code authority}. For the rare gate a single
     * {@code @PreAuthorize} cannot express — a method whose required permission depends on what
     * the request body carries. A method with one fixed permission still uses {@code @PreAuthorize}.
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(granted -> granted.getAuthority().equals(authority));
    }

    /**
     * The current caller, or {@code null} when there is none: no authentication, an unauthenticated
     * one, or an {@link AnonymousAuthenticationToken}. Unlike {@link #getCurrentUsername()}, an
     * anonymous request counts as no caller.
     */
    public static Authentication currentCaller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    /** The current caller's name, or {@code "system"} when there is no caller (see {@link #currentCaller()}). */
    public static String currentActorOrSystem() {
        Authentication authentication = currentCaller();
        return authentication == null || authentication.getName() == null ? SYSTEM_USER : authentication.getName();
    }

    /**
     * The current caller's realm: {@value #REALM_SYSTEM} when there is no caller (see
     * {@link #currentCaller()}), {@value #REALM_CUSTOMER} when it holds {@value #CUSTOMER_AUTHORITY},
     * otherwise {@value #REALM_STAFF}.
     */
    public static String currentRealm() {
        Authentication authentication = currentCaller();
        if (authentication == null) {
            return REALM_SYSTEM;
        }
        boolean customer = authentication.getAuthorities().stream()
            .anyMatch(granted -> CUSTOMER_AUTHORITY.equals(granted.getAuthority()));
        return customer ? REALM_CUSTOMER : REALM_STAFF;
    }
}
