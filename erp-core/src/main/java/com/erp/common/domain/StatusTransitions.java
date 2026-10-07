package com.erp.common.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import java.util.Map;
import java.util.Set;

/**
 * A status state machine given as a table of allowed transitions ({@code from -> {to...}}). A status
 * absent from the table, or mapped to an empty set, is terminal. Held as a static constant by a
 * Domain object.
 */
public final class StatusTransitions {

    private final Map<String, Set<String>> allowed;
    private final String errorCode;

    /**
     * @param allowed   the allowed transitions
     * @param errorCode thrown as {@code BUSINESS_RULE_VIOLATION} with arguments {@code (from, to)}
     */
    public StatusTransitions(Map<String, Set<String>> allowed, String errorCode) {
        this.allowed = Map.copyOf(allowed);
        this.errorCode = errorCode;
    }

    /** Throws unless {@code to} is a non-null status reachable from {@code from}. */
    public void assertAllowed(String from, String to) {
        Set<String> targets = from == null ? Set.of() : allowed.getOrDefault(from, Set.of());
        if (to == null || !targets.contains(to)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION, errorCode, from, to);
        }
    }
}
