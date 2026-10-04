package com.erp.notif.crossmodule;

import java.util.List;

/**
 * NOTIF's cross-module dispatch-history read surface. A caller that triggered a dispatch through
 * {@link NotificationDispatchApi} and needs to confirm what was actually written to NOTIF_LOG
 * (e.g. which template fired) uses this instead of reaching into
 * {@code notif.repository}/{@code notif.entity} directly. Added 2026-09-11 alongside
 * {@link NotificationChannelAdminApi} to close a module-boundary gap (see
 * {@code CrossModuleBoundaryArchTest}).
 */
public interface NotificationLogQueryApi {

    /** Logs matching all three criteria exactly, most recent first. Empty list when none match. */
    List<DispatchLogRecord> findByRecipientModuleAndReference(
        Long recipientId, String moduleCode, String referenceType);
}
