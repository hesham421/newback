package com.erp.notif.crossmodule;

/**
 * NOTIF's cross-module channel-administration surface (API-NOTIF-005's enable/disable half,
 * RULE-NOTIF-003). Narrow on purpose: a caller may only flip a channel's enabled flag by its
 * public {@code channelTypeId} (e.g. {@code "EMAIL"}) — no create/delete, no provider config.
 * Added 2026-09-11 to close a module-boundary gap: a consumer previously toggled
 * {@code NotificationChannelConfig} via NOTIF's repository/entity directly instead of going
 * through this package (see {@code CrossModuleBoundaryArchTest}).
 */
public interface NotificationChannelAdminApi {

    /** Sets the named channel's enabled flag (RULE-NOTIF-003 governs dispatch behavior once set). */
    void setChannelEnabled(String channelTypeId, boolean enabled);
}
