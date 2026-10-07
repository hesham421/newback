<!-- Source: content OUTSIDE all PHASE markers (trailing / between-phase sections — e.g. Plan Index, DB Alignment Manifest, Error Catalog, Agent Handoff Summary) -->

<!-- backend-execution-plan.md — Governed by Execution Plan Governance Engine (Project 3.1 / PASS 1) -->

# BACKEND EXECUTION PLAN — Common Utils (CU)

## SECTION 0 — PLAN HEADER
══════════════════════════════════════════════════════════════════
Plan Name        : New Feature — Platform Configuration Store — Common Utils — BE
Plan ID          : PLAN-CU-001
Task Type        : 🆕 New Feature
Feature Code     : CU-001
Module           : Common Utils (CU) — L1 Cross-Cutting Foundation — ROOT — Backend-only
Platform         : Foundation (Domain: ERP)
Governed by      : Execution Plan Governance Engine (Project 3.1) v2
Truth Layer      : Layer 3.1 — Backend Execution Truth
DB_TARGET        : POSTGRESQL_16 (per master-registry v1.1.0 — Architect decision 2026-09-02)
BACKEND_STACK    : SPRING_BOOT_JAVA
DBS-ID           : DBS-CU-001
Output Mode      : SINGLE-FILE — Agent-Ready Specification
GOVERNANCE STATE : NORMAL (srs.md + db-script.md both PRESENT)
Open Questions   : None — see OQ Log (SECTION 3)
══════════════════════════════════════════════════════════════════

⚠ GOVERNANCE ADVISORY (CORE-10 STEP A-5): GOVERNANCE-CONFIG.md declares
DB_TARGET = ORACLE_19C, while master-registry.md + db-script-CU.md declare
POSTGRESQL_16 (recorded Architect decision, 2026-09-02). POSTGRESQL_16 is
used throughout this plan per the authoritative registry decision and
explicit human confirmation. Recommend reconciling GOVERNANCE-CONFIG.md.

---

## SECTION 1 — PLAN INDEX — CU — PLAN-ID: PLAN-CU-001
══════════════════════════════════════════════════════════════════

ENTITY REGISTRY (this plan)
───────────────────────────────────────────────────────────────
ENTITY-ID        │ Entity Name       │ DB Table              │ Business Code │ Operations
─────────────────┼───────────────────┼───────────────────────┼───────────────┼───────────
ENTITY-CU-001    │ AppConfiguration  │ CU_APP_CONFIGURATION  │ NO (BC-RULE-0)│ C,R,U,Deactivate

FIELD REGISTRY (this plan)
───────────────────────────────────────────────────────────────
FIELD-ID   │ Java Property       │ DBF-ID   │ Type          │ Read-Only
───────────┼─────────────────────┼──────────┼───────────────┼──────────
FIELD-0001 │ appConfigurationPk  │ DBF-0001 │ Long          │ System
FIELD-0002 │ configKey           │ DBF-0002 │ String(150)   │ After-create (RULE-CU-003)
FIELD-0003 │ configValue         │ DBF-0003 │ String(text)  │ No
FIELD-0004 │ notes               │ DBF-0004 │ String(2000)  │ No
FIELD-0005 │ isActiveFl          │ DBF-0005 │ Boolean       │ System
FIELD-0006 │ createdBy           │ —        │ String(255)   │ System (audit)
FIELD-0007 │ createdAt           │ —        │ LocalDateTime │ System (audit)
FIELD-0008 │ updatedBy           │ —        │ String(255)   │ System (audit)
FIELD-0009 │ updatedAt           │ —        │ LocalDateTime │ System (audit)
Note: CU owns no Business Code (BC-RULE-0 — internal config entity, per srs-CU A3).

API REGISTRY (this plan)
───────────────────────────────────────────────────────────────
API-ID       │ Operation           │ HTTP   │ Endpoint
─────────────┼─────────────────────┼────────┼───────────────────────────────────────
API-CU-001   │ Create              │ POST   │ /api/v1/common/configurations
API-CU-002   │ Search / List       │ GET    │ /api/v1/common/configurations
API-CU-003   │ Update by key       │ PUT    │ /api/v1/common/configurations/{key}
API-CU-004   │ Deactivate (soft)   │ DELETE │ /api/v1/common/configurations/{key}
API-CU-005   │ Get by key          │ GET    │ /api/v1/common/configurations/{key}

RULE REGISTRY (this plan)
───────────────────────────────────────────────────────────────
RULE-ID      │ Rule Name              │ Scope   │ ENTITY-ID     │ Message-AR defined
─────────────┼────────────────────────┼─────────┼───────────────┼───────────────────
RULE-CU-001  │ Config key uniqueness  │ CREATE  │ ENTITY-CU-001 │ YES
RULE-CU-002  │ Required fields        │ C/U     │ ENTITY-CU-001 │ YES
RULE-CU-003  │ Config key immutable   │ UPDATE  │ ENTITY-CU-001 │ YES

SCREEN REGISTRY (this plan)
───────────────────────────────────────────────────────────────
(none — CU is Backend-only, no SCR-IDs, no SEC_PAGES; CORE-9 does not apply — srs-CU PART B)

LOV REGISTRY (this plan)
───────────────────────────────────────────────────────────────
(none — CU owns zero LOVs — srs-CU A5)

QUERY REFERENCE CATALOG SUMMARY
───────────────────────────────────────────────────────────────
QR-ID        │ Operation           │ Phase     │ Entity
─────────────┼─────────────────────┼───────────┼───────────────
QR-CU-0001   │ FIND_ONE (by key)   │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0002   │ FIND_BY_CRITERIA    │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0003   │ SAVE (create)       │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0004   │ UPDATE (by key)     │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0005   │ DEACTIVATE (soft)   │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0006   │ EXISTS (key unique) │ DATA+DOM  │ ENTITY-CU-001
⚠ ALL QRC entries are AGENT REFERENCE only — agent rewrites every query
  during implementation using actual entity/field names.

DB ALIGNMENT     : see SECTION 2 — ALIGNED ✓
XM STATUS        : None (ROOT module)
CONTRACT GATE    : DOC ✓ | INT-C ✓ (no XM)
SECURITY         : API-level authorization only (Backend-only — no SEC_PAGES)
══════════════════════════════════════════════════════════════════

---

## SECTION 2 — DB ALIGNMENT MANIFEST — CU — PLAN-ID: PLAN-CU-001 / DBS-ID: DBS-CU-001
══════════════════════════════════════════════════════════════════
FIELD-ID  │ DBF-ID   │ Plan Type     │ FK/XM-ID │ Match Status
──────────┼──────────┼───────────────┼──────────┼─────────────
FIELD-0001│ DBF-0001 │ Long          │ —        │ ✓
FIELD-0002│ DBF-0002 │ String(150)   │ —        │ ✓
FIELD-0003│ DBF-0003 │ String(text)  │ —        │ ✓
FIELD-0004│ DBF-0004 │ String(2000)  │ —        │ ✓
FIELD-0005│ DBF-0005 │ Boolean       │ —        │ ✓
══════════════════════════════════════════════════════════════════
Legend: ✓ = aligned | ✗ = type mismatch | ⏸ = XM deferred
Audit FIELD-0006..0009 (createdBy/At, updatedBy/At): no DBF-ID by convention
(AuditableEntity — filled by AuditEntityListener). Derived, not DB-invented.
CONTRACT-1: 5 columns only — no Column Name / DB Type / SRS Source reproduced.

---

## SECTION 3 — OPEN QUESTIONS LOG (continuation)
══════════════════════════════════════════════════════════════════
Open Questions: None — inherited clean from srs-CU.md OQ Log. No new OQ raised in PASS 1.
══════════════════════════════════════════════════════════════════

---

## SECTION 4 — DERIVATION LOG
══════════════════════════════════════════════════════════════════
DRV-ID   │ Element                         │ Criterion │ Source
─────────┼─────────────────────────────────┼───────────┼──────────────────────────────
DRV-001  │ ERR-0004 NOT_FOUND (config)     │ PLATFORM  │ Platform-standard 404 for get/update/deactivate by key
DRV-002  │ QR-CU-0006 EXISTS uniqueness    │ CRIT-2    │ RULE-CU-001 requires a pre-insert existence check on configKey
DRV-003  │ get/update/deactivate by {key}  │ CRIT-1    │ srs-CU MODULE-LEVEL APIs address entity by configKey (business key), not PK
══════════════════════════════════════════════════════════════════

---









---

## SECTION A — ERROR CATALOG (canonical)
══════════════════════════════════════════════════════════════════════════════════
ERR-ID   │ RULE-ID      │ API-ID              │ HTTP │ Trigger                  │ Message-AR                                   │ Message-EN
─────────┼──────────────┼─────────────────────┼──────┼──────────────────────────┼──────────────────────────────────────────────┼────────────────────────────────
ERR-0001 │ RULE-CU-001  │ API-CU-001          │ 409  │ Duplicate configKey       │ مفتاح الإعداد موجود مسبقاً — اختر مفتاحاً فريداً. │ Configuration key already exists — choose a unique key.
ERR-0002 │ RULE-CU-002  │ API-CU-001/003      │ 400  │ Required field missing    │ مفتاح الإعداد وقيمته إلزاميان.                  │ Config key and value are required.
ERR-0003 │ RULE-CU-003  │ API-CU-003          │ 422  │ configKey change attempt  │ لا يمكن تعديل مفتاح الإعداد بعد إنشائه.          │ Config key cannot be changed after creation.
ERR-0004 │ PLATFORM-STD │ API-CU-003/004/005  │ 404  │ configKey not found       │ الإعداد غير موجود.                              │ Configuration not found.
══════════════════════════════════════════════════════════════════════════════════
Total Errors: 4 (ERR-0004 = PLATFORM-STD, documented DRV-001).
Every ERR-ID registered in 4 places (ErrorCodes + messages.properties + i18n JSON + ErpErrorMapperService).

---

## SECTION B — QUERY REFERENCE CATALOG (agent reference)
══════════════════════════════════════════════════════════════════
⚠ AGENT REFERENCE ONLY — rewrite every query using actual JPA entity/field names.

QR-CU-0001 — Find configuration by key
  Phase: DATA+DOM | API-ID: API-CU-005/003/004 | Entity: ENTITY-CU-001 | Operation: FIND_ONE
  Intent: fetch one AppConfiguration by its business key (configKey).
  Spec: SELECT * FROM CU_APP_CONFIGURATION WHERE CONFIG_KEY = :key
  Transaction: READ_ONLY | Result: entity or throw LocalizedException(ERR-0004) | Join: NONE

QR-CU-0002 — Search configurations
  Phase: DATA+DOM | API-ID: API-CU-002 | Entity: ENTITY-CU-001 | Operation: FIND_BY_CRITERIA
  Intent: paged search with optional configKey (LIKE) and isActiveFl (EXACT).
  Spec: SELECT * FROM CU_APP_CONFIGURATION WHERE (:key IS NULL OR CONFIG_KEY LIKE %:key%)
        AND (:active IS NULL OR IS_ACTIVE_FL = :active) ORDER BY :sort
  Transaction: READ_ONLY | Pagination: YES | Filters: configKey LIKE, isActiveFl EXACT
  Empty → HTTP 200 content [] | Join: NONE

QR-CU-0003 — Create configuration
  Phase: DATA+DOM | API-ID: API-CU-001 | Entity: ENTITY-CU-001 | Operation: SAVE
  Intent: insert a new configuration; PK from SEQ_CU_APP_CONFIGURATION; audit via listener.
  Spec: INSERT INTO CU_APP_CONFIGURATION (ID, CONFIG_KEY, CONFIG_VALUE, NOTES, IS_ACTIVE_FL, ...audit)
        VALUES (SEQ_CU_APP_CONFIGURATION.next, :key, :value, :notes, 1, ...)
  Transaction: READ_WRITE

QR-CU-0004 — Update configuration
  Phase: DATA+DOM | API-ID: API-CU-003 | Entity: ENTITY-CU-001 | Operation: UPDATE
  Intent: update configValue/notes/isActiveFl by key; CONFIG_KEY excluded (immutable).
  Spec: UPDATE CU_APP_CONFIGURATION SET CONFIG_VALUE=:value, NOTES=:notes, IS_ACTIVE_FL=:active
        WHERE CONFIG_KEY = :key
  Transaction: READ_WRITE

QR-CU-0005 — Deactivate configuration (soft)
  Phase: DATA+DOM | API-ID: API-CU-004 | Entity: ENTITY-CU-001 | Operation: UPDATE (soft delete)
  Intent: set IS_ACTIVE_FL = 0; record preserved.
  Spec: UPDATE CU_APP_CONFIGURATION SET IS_ACTIVE_FL = 0 WHERE CONFIG_KEY = :key
  Transaction: READ_WRITE

QR-CU-0006 — Config key existence check
  Phase: DATA+DOM | API-ID: API-CU-001 | Entity: ENTITY-CU-001 | Operation: EXISTS
  Intent: pre-insert uniqueness check for RULE-CU-001.
  Spec: SELECT COUNT(*) > 0 FROM CU_APP_CONFIGURATION WHERE CONFIG_KEY = :key
  Transaction: READ_ONLY
══════════════════════════════════════════════════════════════════

---

## SECTION C — REGISTRY UPDATE BLOCK
══════════════════════════════════════════════════════════════════
## REGISTRY UPDATE — 2026-09-02
Source          : Project 3.1 — PASS 1 (Backend)
Feature Code    : CU-001
DBS-ID          : DBS-CU-001
Plan ID         : PLAN-CU-001
New Entities    : (none new — ENTITY-CU-001 from P1)
New APIs        : API-CU-001..005
QR-IDs Created  : QR-CU-0001..0006 (6)
XM-IDs Open     : None
OQ-IDs Open     : None
Gate Status     : ALIGN-BE PASSED ✓
Next Action     : Project 4.1 — Backend Audit Gate → Pipeline Status Grid: CU · P3.1 = done
══════════════════════════════════════════════════════════════════

---

## SECTION D — TC COVERAGE MATRIX SUMMARY (backend)
══════════════════════════════════════════════════════════════════
NOTE: TC-IDs are placeholders — full blocks in backend-test-plan-CU.md.

RULE-ID COVERAGE:
RULE-ID      │ Happy path TC   │ Violation TC    │ Status
─────────────┼─────────────────┼─────────────────┼────────
RULE-CU-001  │ TC-BE-CU-001    │ TC-BE-CU-002    │ COVERED ✓
RULE-CU-002  │ TC-BE-CU-003    │ TC-BE-CU-004    │ COVERED ✓
RULE-CU-003  │ TC-BE-CU-005    │ TC-BE-CU-006    │ COVERED ✓
Rule coverage: 3/3 covered — 0 deferred — 0 gaps

API-ID COVERAGE:
API-ID       │ Success TC      │ Status
─────────────┼─────────────────┼────────
API-CU-001   │ TC-BE-CU-001    │ COVERED ✓
API-CU-002   │ TC-BE-CU-007    │ COVERED ✓
API-CU-003   │ TC-BE-CU-008    │ COVERED ✓
API-CU-004   │ TC-BE-CU-009    │ COVERED ✓
API-CU-005   │ TC-BE-CU-010    │ COVERED ✓
API coverage: 5/5 covered — 0 deferred

DEFERRED TC REGISTRY: (none)
══════════════════════════════════════════════════════════════════
Gate SECTION D: PASSED ✓ (no GAP ✗ without DEFERRED)

---

## AGENT HANDOFF SUMMARY (BACKEND) — not a phase
This plan is agent-ready. Agent MUST: read full plan first; rewrite all QRC
entries from scratch using actual entity/field names; follow CORE architecture;
apply RULE-CU-001..003 in the domain layer; use ERR-0001..0004 in error handling;
enforce API-level authorization; write tests per backend-test-plan-CU.md; after
implementation run api-doc-generator before PASS 2.

*End of backend-execution-plan.md — CU — PLAN-CU-001 — ALIGN-BE ✓*