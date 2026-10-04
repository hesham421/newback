package com.erp.events;

/** Names shared by the event bus and its listeners (erp-core step 08). */
public final class ErpCoreEvents {

    private ErpCoreEvents() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /**
     * Bean name of the core event executor, for {@code @Async(ErpCoreEvents.EXECUTOR)} on a listener.
     * A {@code ThreadPoolTaskExecutor} whose task decorator copies {@code TenantContext} and the
     * Spring Security context into the worker thread and clears both afterwards. It is not a default
     * autowiring candidate, so it never replaces the application's own {@code TaskExecutor}.
     */
    public static final String EXECUTOR = "erpCoreEventExecutor";
}
