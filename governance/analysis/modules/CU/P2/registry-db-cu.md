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
Steps          : 04, 05, 09 (migrations V2, V10, V14)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script.md` → "Implementation Addendum — erp-core 1.2.0". No DBS / DBF /
XM ids are assigned here.

### DB FIELD TRACEABILITY — delta
| Column Name | DB Type | Table | Migration |
|---|---|---|---|
| TENANT_ID | BIGINT, NULLABLE (FK `CORE_TENANT`; NULL = platform default) | CU_APP_CONFIGURATION | V10 (added NOT NULL), V14 (nullable) |
| VERSION | BIGINT NOT NULL DEFAULT 0 | CU_APP_CONFIGURATION | V10 |

### CONSTRAINTS — delta
`UQ_CU_APP_CONFIG_CONFIG_KEY`: V10 `(TENANT_ID, CONFIG_KEY)`, then V14 unique index
`((COALESCE(TENANT_ID, 0)), CONFIG_KEY)`.

### XM REGISTER — delta
| XM-ID | Type | Target Table | Target Module | Initial Status |
|---|---|---|---|---|
| (none assigned) | HARD-FK | CORE_TENANT | tenant (no analysis folder) | ACTIVE |
