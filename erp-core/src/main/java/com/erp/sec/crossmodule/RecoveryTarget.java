package com.erp.sec.crossmodule;

/**
 * tenant-maturity B (REQ-SEC-091) — the facts the platform's admin-reset needs about a STAFF user of the current
 * tenant: its id, its username and whether it holds an active super role. A read-model, never the entity.
 */
public record RecoveryTarget(Long userId, String username, boolean superRole) {
}
