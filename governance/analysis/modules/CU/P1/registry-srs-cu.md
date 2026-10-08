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
Steps          : 01, 03, 05, 06, 08, 09, 10, 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Registry deltas only; full text and file:line sources in `srs-cu.md` → "Implementation Addendum — erp-core 1.2.0".
Rule ids mirror the ones minted there (RULE-CU-004 … RULE-CU-015); no API or ENTITY id is minted.

### ENTITIES — delta
| Kind | ENTITY-ID | Entity Name | Delta |
|---|---|---|---|
| CHANGED | ENTITY-CU-001 | AppConfiguration | global entity (`GlobalAuditableEntity`, no `@TenantId`); + `tenantId` (nullable, immutable: NULL = platform default, else tenant override), + `version` (not on the wire); `isActiveFl` → `isActive`; `configKey` stored upper-case; JPA `@UniqueConstraint` removed (expression index) |

### RULES — delta
| Kind | RULE-ID | Short Title | Test-Hint |
|---|---|---|---|
| CHANGED | RULE-CU-001 | Key unique per owner (`COALESCE(TENANT_ID, 0)`, `CONFIG_KEY`) | duplicate → 409 `APP_CONFIGURATION_KEY_DUPLICATE`; race → 409 `DATA_INTEGRITY_VIOLATION` (TC-CORE-SETTINGS-002) |
| CHANGED | RULE-CU-002 | Required fields — bean validation first, Domain fallback | blank → 400 `VALIDATION_ERROR` over REST |
| CHANGED | RULE-CU-003 | Key immutable by DTO shape | `configKey` in a PUT body ignored; `APP_CONFIGURATION_KEY_IMMUTABLE` never raised |
| NEW | RULE-CU-004 | Resolution: active override → active default → absent | `SettingsIntegrationTest` |
| NEW | RULE-CU-005 | `scope=PLATFORM` needs `PLATFORM_SETTINGS_MANAGE` → else 403 `ACCESS_DENIED` (`SETTING_PLATFORM_SCOPE_FORBIDDEN` is the backup check) | TC-CORE-SETTINGS-004 |
| NEW | RULE-CU-006 | Typed reads; unsupported type or malformed value → 422 `SETTING_TYPE_MISMATCH` | `SettingValueConverterTest` |
| NEW | RULE-CU-007 | Cache `erpCoreSettings`, key `<tenantId>:<KEY>`, null cached, evict-all on every write | `SettingsCacheTest` |
| NEW | RULE-CU-008 | Cache operation: application-chosen provider, no TTL, not cluster-coherent, out-of-band changes stale, evict after commit | `SettingsIntegrationTest.aCachedValue_survivesAChangeBehindTheApi_untilACrudWriteEvictsIt` |
| NEW | RULE-CU-009 | Key normalisation: stored upper-case, lookups trim + upper-case (trim-on-lookup-only defect, ADR-CU-003) | — |
| NEW | RULE-CU-010 | Reactivation via PUT `isActive: true`; omitted fields unchanged; DELETE repeatable | TC-CORE-SETTINGS-009 |
| NEW | RULE-CU-011 | Optimistic lock server-side only (`version` not exposed) | — |
| NEW | RULE-CU-012 | `@Audited`; `configValue` recorded under the acting tenant | — |
| NEW | RULE-CU-013 | No settings-changed event | — |
| NEW | RULE-CU-014 | No CU provisioning contributor; new tenants inherit the defaults | — |
| NEW | RULE-CU-015 | `SettingsApi` needs a bound `TenantContext` (500 `TENANT_CONTEXT_MISSING`) | — |

### LIFECYCLE STATES — delta
| Kind | Delta |
|---|---|
| CHANGED | still `isActive` only (two states); reactivation exists through PUT (RULE-CU-010) |

### DEPENDENCIES — delta
| Kind | Type | Target | Target Module | XM candidate |
|---|---|---|---|---|
| NEW | HARD-FK + context | `CORE_TENANT` (`FK_CU_APP_CONFIGURATION_TENANT`), `TenantContext` | tenant | — (no XM id minted) |
| NEW | SPI | `PermissionContributor` (`CuPermissions`) | SEC | — |
| NEW | SOFT | `@Audited` on `AppConfiguration` | audit | — |
| NEW | exposed | `SettingsApi` (no core consumer; applications consume it); `ConfigurationService.resolve` (internal, cached, unauthorised) | — | — |
"CU is ROOT" no longer holds strictly.

### SCREENS — delta
| Kind | Delta |
|---|---|
| CHANGED | still no CU screen specification here; the consuming frontend renders `/settings/configurations` and `/platform/settings` from its own analysis (`governance/frontend/modules/CU/tests/`) |

### APIs — delta
| Kind | API-ID | Method | Endpoint | Delta |
|---|---|---|---|---|
| CHANGED | API-CU-001 | POST | /api/v1/common/configurations | 201 Created; `?scope`; `configKey` upper-cased in storage and response |
| CHANGED | API-CU-002 | **POST** | **/api/v1/common/configurations/search** | was `GET /api/v1/common/configurations` with query parameters; filter envelope — filters `configKey`, `isActive`, `createdAt`, `updatedAt`; sorts `configKey`, `createdAt`, `updatedAt`; page 0 / size 20, maximum 200; unknown filter field → 400 `VALIDATION_ERROR` (`UNSUPPORTED_FILTER_FIELD`); unknown sort field ignored |
| CHANGED | API-CU-003 | PUT | /api/v1/common/configurations/{key} | partial semantics; `isActive: true` reactivates; `configKey` in the body ignored; `saveAndFlush` → response carries the new audit fields |
| CHANGED | API-CU-004 | DELETE | /api/v1/common/configurations/{key} | 204 no body; repeatable on an inactive row |
| CHANGED | API-CU-005 | GET | /api/v1/common/configurations/{key} | key case-insensitive; miss → 404 `APP_CONFIGURATION_NOT_FOUND` |
| CHANGED | (all) | — | query parameter `scope` | `TENANT` default / `PLATFORM`; invalid value → 400 (`field = scope`); responses carry `id`, `scope`, `isActive` (no `version`) |
| REMOVED | (internal) | — | `ConfigurationService.getValue(configKey)` | replaced by `SettingsApi` |
| NEW | (in-process) | — | `com.erp.cu.crossmodule.SettingsApi` (6 methods, 6 types, cached, tenant-aware) | — |

### ERROR CODES — delta
| Kind | Code | HTTP | Note |
|---|---|---|---|
| NEW | `APP_CONFIGURATION_KEY_DUPLICATE` | 409 | RULE-CU-001 |
| NEW | `APP_CONFIGURATION_FIELDS_REQUIRED` | 400 | Domain fallback; REST answers `VALIDATION_ERROR` first |
| NOT IMPLEMENTED | `APP_CONFIGURATION_KEY_IMMUTABLE` | — | registered, never raised |
| NEW | `APP_CONFIGURATION_NOT_FOUND` | 404 | GET / PUT / DELETE `/{key}` |
| NEW | `SETTING_NOT_FOUND` | 404 | `SettingsApi.get` only |
| NEW | `SETTING_TYPE_MISMATCH` | 422 | `SettingsApi` typed reads only |
| NEW | `SETTING_PLATFORM_SCOPE_FORBIDDEN` | 403 | backup check only; the HTTP refusal is `ACCESS_DENIED` |
| NEW | common `ACCESS_DENIED` 403, `VALIDATION_ERROR` (+ `UNSUPPORTED_FILTER_FIELD`) 400, `DATA_INTEGRITY_VIOLATION` 409, `CONCURRENT_MODIFICATION` 409, `NOT_FOUND` 404 (1.2.0); tenant `TENANT_CONTEXT_MISSING` 500 | — | shared codes reachable from CU |

### PERMISSIONS — delta
| Kind | Authority | Screen (module) | Note |
|---|---|---|---|
| CHANGED | `CONFIG_VIEW`, `CONFIG_CREATE`, `CONFIG_UPDATE`, `CONFIG_DEACTIVATE` (action codes VIEW / CREATE / UPDATE / DEACTIVATE) | `CU_CONFIGURATIONS` (`CU` — `الأدوات المشتركة` / `Common Utilities`) | V7 seed; code-defined in `CuPermissions` |
| NEW | `PERM_PLATFORM_SETTINGS_VIEW` (gateway, gates no API), `PLATFORM_SETTINGS_MANAGE` (every `scope=PLATFORM` call) | `PLATFORM_SETTINGS` — `إعدادات المنصة الافتراضية` / `Platform Default Settings` (`PLATFORM`) | synchroniser-inserted, not seeded; implicit for PLATFORM-tenant super roles only |
| NEW | role `CU_ADMIN` | — | V7: module grant `CU` → `CU_CONFIGURATIONS` and its four actions |


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

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no CU behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `AppConfigurationDomain` (RULE-CU-001, RULE-CU-002) | uses `com.erp.common.domain.DomainRules.assertUnique` / `assertNotBlank`; same codes, statuses and order | erp-core/src/main/java/com/erp/cu/domain/AppConfigurationDomain.java:39-40,51 |
| CHANGED | `ConfigurationService.owner` (RULE-CU-005) | uses `TenantContext.isPlatform()`; same outcome | erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:214 |
