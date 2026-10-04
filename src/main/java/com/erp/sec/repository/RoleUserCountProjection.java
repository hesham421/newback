package com.erp.sec.repository;

/**
 * Read-only projection for QR-SEC-022's {@code usersPerRole} aggregate (A.2.8) — a grouped
 * ENT-SEC-002 / ENT-SEC-003 count that maps to no single entity.
 */
public interface RoleUserCountProjection {

    Long getRolePk();

    String getCode();

    String getNameAr();

    String getNameEn();

    Long getUserCount();
}
