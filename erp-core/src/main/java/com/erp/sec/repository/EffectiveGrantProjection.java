package com.erp.sec.repository;

/**
 * Read-only projection of one effective action grant, carrying the registry FACTS RULE-SEC-007
 * needs: the permission code itself, the {@code SEC_SCREEN_REG} row it belongs to, and its
 * {@code SEC_ACTION_REG} action code. The screen is read from the registry rather than derived
 * from the permission code's text, so a multi-word action code
 * ({@code FIN_PERIODS} / {@code CLOSE_APPROVE}) resolves to the same screen as a single-word one.
 *
 * @param permissionCode {@code SEC_ACTION_REG.PERMISSION_CODE} (DBF-SEC-050)
 * @param screenRegPk    {@code SEC_SCREEN_REG.SCREEN_REG_PK} the action belongs to (DBF-SEC-051)
 * @param actionCode     {@code SEC_ACTION_REG.ACTION_CODE} (DBF-SEC-052)
 */
public record EffectiveGrantProjection(String permissionCode, Long screenRegPk, String actionCode) {
}
