# REGISTRY EXTRACT — registry-db-CU
══════════════════════════════════════════════════════════════════
Module          : Common Utils (CU)
Source artifact : db-script-CU.md
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Common Utils
Module Prefix : CU

## TABLES (DBS-ID register)
| DBS-ID | Table Name | Source ENTITY-ID |
|---|---|---|
| DBS-CU-001 | CU_APP_CONFIGURATION | ENTITY-CU-001 |

## DB FIELD TRACEABILITY (compact)
| DBF-ID | Column Name | DB Type | Table | SRS Source |
|---|---|---|---|---|
| DBF-0001 | ID | BIGINT | CU_APP_CONFIGURATION | ENTITY-CU-001 |
| DBF-0002 | CONFIG_KEY | VARCHAR(150) | CU_APP_CONFIGURATION | ENTITY-CU-001 |
| DBF-0003 | CONFIG_VALUE | TEXT | CU_APP_CONFIGURATION | ENTITY-CU-001 |
| DBF-0004 | NOTES | VARCHAR(2000) | CU_APP_CONFIGURATION | ENTITY-CU-001 |
| DBF-0005 | IS_ACTIVE_FL | SMALLINT | CU_APP_CONFIGURATION | ENTITY-CU-001 |
Total: 5 DBF-IDs across 1 table.

## LOV DDL REGISTER
None — CU owns zero LOVs; no MD_MASTER_LOOKUP in Foundation scope (srs-CU.md A5).

## XM REGISTER
| XM-ID | Type | Target Table | Target Module | Initial Status |
|---|---|---|---|---|
| (none) | — | — | — | — |
CU is the ROOT cross-cutting library; it has no outbound cross-module dependencies.

---
*End of registry-db-CU.md*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 09 (migrations V2, V7, V10, V14; shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Registry deltas only; detail and sources in `db-script.md` → "Implementation Addendum — erp-core 1.2.0". No DBS / DBF /
XM ids are assigned here. Migrations are under `erp-core/src/main/resources/db/migration/core/`.

### TABLES — delta
| Kind | DBS-ID | Table Name | Delta |
|---|---|---|---|
| CHANGED | DBS-CU-001 | CU_APP_CONFIGURATION | global table with a nullable `TENANT_ID` (NULL = platform default, else tenant override); + `VERSION` |

### DB FIELD TRACEABILITY — delta
| Kind | Column Name | DB Type | Table | Migration |
|---|---|---|---|---|
| NEW | TENANT_ID | BIGINT, NULLABLE (FK `CORE_TENANT`; NULL = platform default) | CU_APP_CONFIGURATION | V10 (added NOT NULL, backfilled 1), V14 (nullable, column comment) |
| NEW | VERSION | BIGINT NOT NULL DEFAULT 0 | CU_APP_CONFIGURATION | V10 |
| CHANGED | CONFIG_KEY | VARCHAR(150) NOT NULL — DDL unchanged; values stored upper-case by the entity | CU_APP_CONFIGURATION | V2 |
| CHANGED | CREATED_BY, UPDATED_BY, CREATED_AT, UPDATED_AT | VARCHAR(255) / TIMESTAMP in DDL; the entity maps length 100 / `Instant` (no migration changes the types) | CU_APP_CONFIGURATION | V2 |

### SEQUENCES — delta
| Kind | Sequence | Delta | Migration |
|---|---|---|---|
| CHANGED | SEQ_CU_APP_CONFIGURATION | `CACHE 1` (db-script §4: `NO CACHE`); start 1, increment 1, no cycle | V2 |

### CONSTRAINTS AND INDEXES — delta
| Kind | Name | Definition | Migration |
|---|---|---|---|
| NEW | FK_CU_APP_CONFIGURATION_TENANT | FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID) | V10 |
| NEW | IDX_CU_APP_CONFIGURATION_TENANT | INDEX (TENANT_ID) | V10 |
| CHANGED | UQ_CU_APP_CONFIG_CONFIG_KEY | V2 `UNIQUE (CONFIG_KEY)` → V10 `UNIQUE (TENANT_ID, CONFIG_KEY)` → V14 unique INDEX `((COALESCE(TENANT_ID, 0)), CONFIG_KEY)` (same name; JPA `@UniqueConstraint` removed) | V10, V14 |
Unchanged: `PK_CU_APP_CONFIGURATION`, `CHK_CU_APP_CONFIG_ACTIVE_FL`.

### SEED ROWS — delta (in SEC's registry tables; no CU table row)
| Kind | Rows | Migration |
|---|---|---|
| NEW | module `CU` (`الأدوات المشتركة` / `Common Utilities`); screen `CU_CONFIGURATIONS`; actions VIEW / CREATE / UPDATE / DEACTIVATE = `CONFIG_VIEW` / `CONFIG_CREATE` / `CONFIG_UPDATE` / `CONFIG_DEACTIVATE`; role `CU_ADMIN` with the `CU` module grant and its derived screen and action grants; `SYS_ADMIN` module grant `CU` | V7 |
| NEW | module `PLATFORM` | V10 |
| NEW | screen `PLATFORM_SETTINGS` and actions VIEW (`PERM_PLATFORM_SETTINGS_VIEW`) / MANAGE (`PLATFORM_SETTINGS_MANAGE`) | none — inserted at startup by `PermissionCatalogSynchronizer` from `CuPermissions` |

### XM REGISTER — delta
| Kind | XM-ID | Type | Target Table | Target Module | Initial Status |
|---|---|---|---|---|---|
| NEW | (none assigned) | HARD-FK (`FK_CU_APP_CONFIGURATION_TENANT`) | CORE_TENANT | tenant | ACTIVE |
"CU has no outbound cross-module dependencies" no longer holds.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (commit 6b01816; CHANGELOG [Unreleased]); no CU behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `AppConfigurationDomain` | uses `com.erp.common.domain.DomainRules`; no schema effect | erp-core/src/main/java/com/erp/cu/domain/AppConfigurationDomain.java:39-40,51 |
| CHANGED | `ConfigurationService.owner` | uses `TenantContext.isPlatform()`; no schema effect | erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:214 |

No table, column, sequence, constraint, index or seed delta; no CU migration after V14.
