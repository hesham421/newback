package com.erp.common.domain;

public final class HxTransitions {

    private final Map<String, Set<String>> allowed;
    private final String errorCode;

    public HxTransitions(Map<String, Set<String>> allowed, String errorCode) {
        this.allowed = Map.copyOf(allowed);
        this.errorCode = errorCode;
    }

    public void assertAllowed(String from, String to) {
        if (!allowed.getOrDefault(from, Set.of()).contains(to)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION, errorCode, from, to);
        }
    }
}
