package com.erp.notif.crossmodule;

import java.util.List;

/**
 * NOTIF's cross-module dispatch surface — the ONLY way another module triggers a notification. A
 * narrow interface accepting the {@link DispatchCommand} read-model and returning the created
 * NOTIF_LOG ids. Supersedes the plan's "consume CU NotificationEvent" listener: the service skill
 * forbids listening to another module's internal events, so callers invoke this interface instead
 * (recorded as an api_doc_gap). Authorization is enforced on the producing side: the delegate's
 * dispatch gate requires an authenticated principal (@PreAuthorize isAuthenticated(), SEC-BE).
 */
public interface NotificationDispatchApi {

    /**
     * Dispatches per RULE-NOTIF-001..007 (fan-out one log per channel; skip inactive recipient;
     * disabled channel → CHANNEL_DISABLED; retry ≤5 then FAILED). Returns the created log ids —
     * empty when the recipient is inactive (RULE-NOTIF-007).
     */
    List<Long> dispatch(DispatchCommand command);

    /**
     * Same semantics as {@link #dispatch}, but committed in its OWN transaction (REQUIRES_NEW), so a
     * dispatch failure can never mark the caller's transaction rollback-only. A caller whose own
     * writes must survive a failed notification — it catches and logs the failure and still expects
     * to commit, e.g. SEC's password-reset token issuance — MUST use this entry point. The recipient
     * is checked inside the caller's transaction, so a user the caller has not yet committed is seen.
     */
    List<Long> dispatchIndependently(DispatchCommand command);
}
