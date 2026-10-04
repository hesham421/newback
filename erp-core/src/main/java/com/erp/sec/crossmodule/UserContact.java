package com.erp.sec.crossmodule;

/**
 * Read-model for {@link SecUserDirectoryApi#findContact} (REQ-SEC-034) — the only ENT-SEC-001
 * fields SEC exposes outside the module: DBF-SEC-001 user pk, DBF-SEC-003 email, DBF-SEC-005 /
 * DBF-SEC-006 display names, and {@code active} derived from DBF-SEC-007 status_code. Never the
 * password hash (POL-SEC-004), a reset or session token, an audit row, or any role/grant data.
 */
public record UserContact(
    Long userPk,
    String email,
    String fullNameAr,
    String fullNameEn,
    boolean active) {
}
