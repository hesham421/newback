## REGISTRY — P2 — SEC v1
══════════════════════════════════════════════════════════════════

Tables
| Table | ENT id | Kind | DBF range |
|---|---|---|---|
| SEC_USER | ENT-SEC-001 | security | DBF-SEC-001 … DBF-SEC-013 |
| SEC_ROLE | ENT-SEC-002 | security | DBF-SEC-014 … DBF-SEC-024 |
| SEC_USER_ROLE | ENT-SEC-003 | security | DBF-SEC-025 … DBF-SEC-029 |
| SEC_MODULE_REG | ENT-SEC-004 | security | DBF-SEC-030 … DBF-SEC-038 |
| SEC_SCREEN_REG | ENT-SEC-005 | security | DBF-SEC-039 … DBF-SEC-048 |
| SEC_ACTION_REG | ENT-SEC-006 | security | DBF-SEC-049 … DBF-SEC-059 |
| SEC_ROLE_MODULE_GRANT | ENT-SEC-007 | security | DBF-SEC-060 … DBF-SEC-064 |
| SEC_ROLE_SCREEN_GRANT | ENT-SEC-008 | security | DBF-SEC-065 … DBF-SEC-069 |
| SEC_ROLE_ACTION_GRANT | ENT-SEC-009 | security | DBF-SEC-070 … DBF-SEC-074 |
| SEC_ACTIVE_SESSION | ENT-SEC-010 | security | DBF-SEC-075 … DBF-SEC-082 |
| SEC_AUDIT_LOG | ENT-SEC-011 | security | DBF-SEC-083 … DBF-SEC-090 |
| SEC_PWD_RESET_TOKEN | ENT-SEC-012 | security | DBF-SEC-091 … DBF-SEC-096 |
| SEC_SIGNUP_REQUEST | ENT-SEC-013 | security | DBF-SEC-097 … DBF-SEC-104 |

DBF ids (full detail in db-script-sec.md → §1): DBF-SEC-001, DBF-SEC-002, DBF-SEC-003,
DBF-SEC-004, DBF-SEC-005, DBF-SEC-006, DBF-SEC-007, DBF-SEC-008, DBF-SEC-009, DBF-SEC-010,
DBF-SEC-011, DBF-SEC-012, DBF-SEC-013, DBF-SEC-014, DBF-SEC-015, DBF-SEC-016, DBF-SEC-017,
DBF-SEC-018, DBF-SEC-019, DBF-SEC-020, DBF-SEC-021, DBF-SEC-022, DBF-SEC-023, DBF-SEC-024,
DBF-SEC-025, DBF-SEC-026, DBF-SEC-027, DBF-SEC-028, DBF-SEC-029, DBF-SEC-030, DBF-SEC-031,
DBF-SEC-032, DBF-SEC-033, DBF-SEC-034, DBF-SEC-035, DBF-SEC-036, DBF-SEC-037, DBF-SEC-038,
DBF-SEC-039, DBF-SEC-040, DBF-SEC-041, DBF-SEC-042, DBF-SEC-043, DBF-SEC-044, DBF-SEC-045,
DBF-SEC-046, DBF-SEC-047, DBF-SEC-048, DBF-SEC-049, DBF-SEC-050, DBF-SEC-051, DBF-SEC-052,
DBF-SEC-053, DBF-SEC-054, DBF-SEC-055, DBF-SEC-056, DBF-SEC-057, DBF-SEC-058, DBF-SEC-059,
DBF-SEC-060, DBF-SEC-061, DBF-SEC-062, DBF-SEC-063, DBF-SEC-064, DBF-SEC-065, DBF-SEC-066,
DBF-SEC-067, DBF-SEC-068, DBF-SEC-069, DBF-SEC-070, DBF-SEC-071, DBF-SEC-072, DBF-SEC-073,
DBF-SEC-074, DBF-SEC-075, DBF-SEC-076, DBF-SEC-077, DBF-SEC-078, DBF-SEC-079, DBF-SEC-080,
DBF-SEC-081, DBF-SEC-082, DBF-SEC-083, DBF-SEC-084, DBF-SEC-085, DBF-SEC-086, DBF-SEC-087,
DBF-SEC-088, DBF-SEC-089, DBF-SEC-090, DBF-SEC-091, DBF-SEC-092, DBF-SEC-093, DBF-SEC-094,
DBF-SEC-095, DBF-SEC-096, DBF-SEC-097, DBF-SEC-098, DBF-SEC-099, DBF-SEC-100, DBF-SEC-101,
DBF-SEC-102, DBF-SEC-103, DBF-SEC-104

XM index
none — SEC is ROOT (→ dependency index: no row this module).

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| USER_STATUS | 3 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |
| SIGNUP_STATUS | 3 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |
| AUDIT_EVENT_TYPE | 14 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |

Sequences
Last DBF: DBF-SEC-104 · Last XM: none assigned (0 rows)

Decisions
ADR-SEC-001 (ACCEPTED, non-breaking) — see erp/decisions/SEC/ADR-SEC-001.md

Event
"P2 completed: SEC v1 — 13 tables, 104 DBF, 0 XM"

Cascade
No registry XM row anywhere in the platform currently targets SEC with status DEFERRED
(SEC is the first module through this pipeline this batch) — nothing to resolve.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 07 (migrations V4, V7, V10, V11, V12)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script-sec.md` → "Implementation Addendum — erp-core 1.2.0". No DBF or
XM ids are assigned here.

Tables
| Table | ENT | Delta | Migration |
|---|---|---|---|
| SEC_CUSTOMER_VERIFY_TOKEN | CustomerVerifyToken (no ENT id yet) | NEW, tenant-scoped (11 columns) | V11__sec_realms.sql |
| 10 tenant-scoped SEC tables | ENT-SEC-001..003, 007..013 | + `TENANT_ID` (FK `CORE_TENANT`), + `VERSION` | V10__tenant_schema.sql |
| SEC_MODULE_REG / SEC_SCREEN_REG / SEC_ACTION_REG | ENT-SEC-004..006 | + `VERSION` only (global) | V10 |
| 8 SEC tables (user-role, 3 grants, session, audit log, reset token, sign-up) | ENT-SEC-003, 007..013 | + nullable `CREATED_BY/AT`, `UPDATED_BY/AT` | V10 |
| SEC_USER | ENT-SEC-001 | + `REALM` | V11 |
| SEC_ROLE | ENT-SEC-002 | + `IS_SUPER` | V11 |

Constraints changed (same names, composite with `TENANT_ID`)
`UQ_SEC_USER_USERNAME (TENANT_ID, REALM, USERNAME)`, `UQ_SEC_USER_EMAIL (TENANT_ID, REALM, EMAIL)`,
`UQ_SEC_ROLE_CODE (TENANT_ID, CODE)`, `UQ_SEC_USER_ROLE_USER_ROLE`, `UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE`,
`UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN`, `UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION` (each `(TENANT_ID, …)`);
`CHK_SEC_USER_STATUS` widened; new `CHK_SEC_USER_REALM`. Registry uniques (`UQ_SEC_MODULE_REG_CODE`,
`UQ_SEC_SCREEN_REG_PAGE`, `UQ_SEC_ACTION_REG_PERM`) stay global.

Lookups
| Key | Delta | Owner |
|---|---|---|
| USER_STATUS | 4 values (CHECK-constrained, ADR-SEC-001 pattern) | SEC |
| REALM | 2 values `STAFF`, `CUSTOMER` (CHECK-constrained) | SEC |

XM index
SEC is no longer free of outbound references: every `TENANT_ID` is a HARD FK to `CORE_TENANT` (tenant
module, no analysis folder; see `analysis/modules/SEC/P0/platform-summary.md` addendum). No XM id assigned.
