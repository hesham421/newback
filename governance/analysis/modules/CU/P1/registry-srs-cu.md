# REGISTRY EXTRACT — registry-srs-CU
══════════════════════════════════════════════════════════════════
Module          : Common Utils (CU)
Source artifact : srs-CU.md (v1.0)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Common Utils (المرافق المشتركة)
Module Prefix : CU
OQ count : 0 (none)

## ENTITIES (PART A — A3)
| ENTITY-ID | Entity Name | Type |
|---|---|---|
| ENTITY-CU-001 | AppConfiguration | PRIVATE |

## RULES (PART A — A4)
| RULE-ID | Short Title | Test-Hint |
|---|---|---|
| RULE-CU-001 | Config key uniqueness | — |
| RULE-CU-002 | configKey and configValue required | — |
| RULE-CU-003 | Config key immutable after create | — |

## LOVs (PART A — A5)
None. CU owns zero LOVs (module-registry §LOVs OWNED = none).

## LIFECYCLE STATES (PART A — A6)
Not applicable — isActiveFl only (two states), no statusId, no Workflow (RULE-13).

## DEPENDENCIES (PART A — A7)
| Type | Target ENTITY-ID | Target Module | XM candidate |
|---|---|---|---|
| (none) | — | — | — |
CU is ROOT — it does not depend on or consume any other module's entity. No XM candidates.

## SCREENS (PART B)
None — Backend-only module (Architect decision 2026-09-02). No SCR-IDs, no SEC_PAGES; CORE-9 does
not apply.

## APIs (PART B — B5)
| API-ID | Method | Endpoint | Owning SCR-ID |
|---|---|---|---|
| API-CU-001 | POST | /api/v1/common/configurations | — (no screens) |
| API-CU-002 | GET | /api/v1/common/configurations | — (no screens) |
| API-CU-003 | PUT | /api/v1/common/configurations/{key} | — (no screens) |
| API-CU-004 | DELETE | /api/v1/common/configurations/{key} | — (no screens) |
| API-CU-005 | GET | /api/v1/common/configurations/{key} | — (no screens) |
Internal (in-process, not an HTTP endpoint): ConfigurationService.getValue(configKey).

## PERMISSIONS (Permissions Summary)
None — no pages/CORE-9 permissions (Backend-only, no screens). Authorization is API-level only.

## OQ LOG STATUS
| OQ-ID | Status | One-line topic | Escalation |
|---|---|---|---|
| (none) | — | No open questions | — |

---
*End of registry-srs-CU.md*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 05, 06, 08, 09, 10, 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-cu.md` → "Implementation Addendum — erp-core 1.2.0". No new ids
are assigned here.

### ENTITIES — delta
| ENTITY-ID | Delta |
|---|---|
| ENTITY-CU-001 AppConfiguration | nullable tenant (platform default / tenant override), version, global entity |

### RULES — delta
| Rule | Delta |
|---|---|
| RULE-CU-001 | CHANGED: unique per owner (`COALESCE(TENANT_ID, 0)`, `CONFIG_KEY`) |
| (new) | resolution override → default; `scope=PLATFORM` guard; typed reads; settings cache; optimistic lock; audit |

### APIs — delta
| Kind | Item |
|---|---|
| CHANGED | API-CU-001..005: query parameter `scope` (TENANT default / PLATFORM); responses carry `scope` |
| REMOVED | internal `ConfigurationService.getValue` |
| NEW | `SettingsApi` (cross-module, typed, cached) |

### ERROR CODES — delta
NEW: `SETTING_NOT_FOUND` (404), `SETTING_TYPE_MISMATCH` (422), `SETTING_PLATFORM_SCOPE_FORBIDDEN` (403);
common `CONCURRENT_MODIFICATION` (409), `NOT_FOUND` (404, 1.2.0).

### PERMISSIONS — delta
`CONFIG_VIEW`, `CONFIG_CREATE`, `CONFIG_UPDATE`, `CONFIG_DEACTIVATE` (code-defined, `CuPermissions`);
NEW `PERM_PLATFORM_SETTINGS_VIEW`, `PLATFORM_SETTINGS_MANAGE`.

### DEPENDENCIES — delta
| Type | Target | Module |
|---|---|---|
| HARD-FK + context | `CORE_TENANT`, `TenantContext` | tenant |
| SPI | `PermissionContributor` | SEC |


## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C5 — tenant data export contributor
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-cu.md` → "Implementation Addendum — erp-core 1.3.0".

### DEPENDENCIES — delta
| Type | Target | Module |
|---|---|---|
| SPI (implemented) | `TenantExportContributor` (XM-TENANT-004) — `CuTenantExportContributor`: the tenant's `CU_APP_CONFIGURATION` overrides | tenant |
No id minted; endpoints, rules, error codes, permissions, schema unchanged.
