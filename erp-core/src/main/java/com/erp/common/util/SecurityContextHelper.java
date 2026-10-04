package com.erp.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextHelper {

    private static final String SYSTEM_USER = "system";

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
}
