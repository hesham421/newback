## TEST EXECUTION MANIFEST — SEC v1
══════════════════════════════════════════════════════════════════
Derived from: backend-test-plan-sec.md v1 (this run) · db-script-sec.md v1 (FK/XM) ·
srs-sec.md v1 (RULE messages). Introduces no ID. Regenerate whenever the backend plan or
test plan changes.
══════════════════════════════════════════════════════════════════

## DEPENDENCY ORDER (topological entity build order)
1. SignupRequest (no FK dependency)
2. User (no FK dependency; SignupRequest→User only via the approval action, not a schema FK)
3. Role (no FK dependency)
4. ModuleRegistry (no FK dependency)
5. UserRoleAssignment (FK → User, Role)
6. ScreenRegistry (FK → ModuleRegistry)
7. RoleModuleGrant (FK → Role, ModuleRegistry)
8. ActionRegistry (FK → ScreenRegistry)
9. RoleScreenGrant (FK → Role, ScreenRegistry; must-belong-to RULE-SEC-001: role's ModuleRegistry via RoleModuleGrant)
10. RoleActionGrant (FK → Role, ActionRegistry; must-belong-to RULE-SEC-002/007: role's RoleScreenGrant + VIEW)
11. ActiveSession (FK → User)
12. PasswordResetToken (FK → User)
13. AuditLogEntry (FK → User, nullable)

## RULE → CODE → TC
| RULE | Catalog code | TC | HTTP | API |
|---|---|---|---|---|
| RULE-SEC-001 | SEC-409-NO-MODULE-GRANT | TC-SEC-013 | 409 | API-SEC-016 |
| RULE-SEC-002 | SEC-409-NO-SCREEN-GRANT | TC-SEC-014 | 409 | API-SEC-017 |
| RULE-SEC-003 | — (success-path cascade, no error code) | TC-SEC-015 | 200 | API-SEC-015 |
| RULE-SEC-004 | SEC-409-MODULE-NOT-REGISTERED | TC-SEC-018 | 409 | API-SEC-019 |
| RULE-SEC-005 | SEC-409-SOD-CONFLICT | TC-SEC-020 | 409 | API-SEC-017 |
| RULE-SEC-006 | SEC-409-RESET-TOKEN-INVALID | TC-SEC-008 | 409 | API-SEC-004 |
| RULE-SEC-007 | SEC-409-NO-VIEW-GRANT / SEC-403-FORBIDDEN | TC-SEC-030 | 409 / 403 | API-SEC-017 |

## ENTITY CRUD CHECKLIST
| ENT | create | read | search | update | deactivate (soft) | activate |
|---|---|---|---|---|---|---|
| ENT-SEC-001 User | ✓ (API-SEC-006) | ✓ (API-SEC-022 read entry equiv.) | ✓ (API-SEC-005) | ✓ (API-SEC-007) | ✓ (API-SEC-009) | ✓ (API-SEC-010) |
| ENT-SEC-002 Role | ✓ (API-SEC-013) | — | ✓ (API-SEC-012) | — | — | — |
| ENT-SEC-003 UserRoleAssignment | ✓ (API-SEC-008) | — | — | — | ✓ (API-SEC-008, replace-set) | — |
| ENT-SEC-004 ModuleRegistry | ✓ (API-SEC-018) | — | ✓ (API-SEC-021) | — | — | — |
| ENT-SEC-005 ScreenRegistry | ✓ (API-SEC-019) | — | ✓ (API-SEC-021) | — | — | — |
| ENT-SEC-006 ActionRegistry | ✓ (API-SEC-020) | — | ✓ (API-SEC-021) | — | — | — |
| ENT-SEC-007 RoleModuleGrant | ✓ (API-SEC-014) | — | — | — | ✓ (API-SEC-015) | — |
| ENT-SEC-008 RoleScreenGrant | ✓ (API-SEC-016) | — | — | — | — (cascade only) | — |
| ENT-SEC-009 RoleActionGrant | ✓ (API-SEC-017) | — | — | — | — (cascade only) | — |
| ENT-SEC-010 ActiveSession | ✓ (API-SEC-001, system) | — | ✓ (API-SEC-025) | — | ✓ (API-SEC-026) | — |
| ENT-SEC-011 AuditLogEntry | ✓ (system, side-effect) | — | ✓ (API-SEC-023) | — | — | — |
| ENT-SEC-012 PasswordResetToken | ✓ (API-SEC-003, system) | — | — | ✓ (API-SEC-004, mark used) | — | — |
| ENT-SEC-013 SignupRequest | ✓ (API-SEC-002) | — | — | ✓ (API-SEC-011, decision) | — | — |
══════════════════════════════════════════════════════════════════
