<!-- Source: content OUTSIDE all PHASE markers (trailing / between-phase sections — e.g. Plan Index, DB Alignment Manifest, Error Catalog, Agent Handoff Summary) -->

<!-- backend-execution-plan.md — Governed by Execution Plan Governance Engine (Project 3.1 / PASS 1) -->

# BACKEND EXECUTION PLAN — File Service (FILE)

## SECTION 0 — PLAN HEADER
══════════════════════════════════════════════════════════════════
Plan Name        : New Feature — File Storage Provider — File Service — BE
Plan ID          : PLAN-FILE-001
Task Type        : 🆕 New Feature   |   Feature Code: FILE-001 (srs-FILE.md v1.1)
Module           : File Service (FILE) — L1 Service (Provider) — dep: CU, SEC — Backend + Frontend
Platform         : Foundation (Domain: ERP)   |   Truth Layer: Layer 3.1
DB_TARGET        : POSTGRESQL_16    BACKEND_STACK: SPRING_BOOT_JAVA    DBS-ID: DBS-FILE-001
Output Mode      : SINGLE-FILE — Agent-Ready Specification
GOVERNANCE STATE : NORMAL (srs.md + db-script.md both PRESENT)
Open Questions   : None (file-delete semantics resolved — RULE-FILE-006 soft-delete)
══════════════════════════════════════════════════════════════════

---

## SECTION 1 — PLAN INDEX — FILE — PLAN-ID: PLAN-FILE-001
══════════════════════════════════════════════════════════════════
ENTITY REGISTRY
ENTITY-FILE-001 │ FileDocument │ FILE_DOCUMENT │ NO (secure id/token) │ Upload,Read/Download,List,Archive,Delete(soft) │ Transactional
ENTITY-FILE-002 │ FileCategory │ FILE_CATEGORY │ NO                   │ Create,Read,Update,Deactivate                   │ Reference

FIELD REGISTRY (FIELD-ID ↔ DBF-ID, 1:1)
FIELD-0001..0011 → DBF-0001..0011 : FILE_DOCUMENT (ID, OWNER_ID, OWNER_TYPE, MODULE_CODE, FILE_NAME, CONTENT_TYPE, FILE_SIZE, FILE_CONTENT[BYTEA], FILE_TYPE_ID, FILE_STATUS_ID, FILE_CATEGORY_FK)
FIELD-0012..0018 → DBF-0012..0018 : FILE_CATEGORY (ID, CATEGORY_CODE, NAME_AR, NAME_EN, MAX_SIZE_BYTES, ALLOWED_CONTENT_TYPES, IS_ACTIVE_FL)
Audit fields (createdBy/At, updatedBy/At): no DBF-ID (AuditEntityListener).

API REGISTRY
API-FILE-001 Upload POST /api/v1/files                       | API-FILE-002 Access-token POST /api/v1/files/{id}/access-token
API-FILE-003 Download GET /api/v1/files/download?token=      | API-FILE-004 Metadata GET /api/v1/files/{id}
API-FILE-005 List-by-owner GET /api/v1/files?ownerId=&ownerType=&moduleCode= | API-FILE-006 Archive/Delete DELETE /api/v1/files/{id}
API-FILE-007 Categories CRUD /api/v1/files/categories        | API-FILE-008 Lookups GET /api/v1/files/lookups/{lookupKey}
Provider (in-process): FileService.store/retrieve/issueAccessToken — injected into NOTIF & future modules.

RULE REGISTRY : RULE-FILE-001..007 (RULE-FILE-001/002/003 carry ⚠ Client-Policy defaults).
SCREEN REGISTRY (CORE-9)
SCR-FILE-001 │ File Categories │ COMPOSITE PATTERN-2 SIDE_DRAWER │ ENTITY-FILE-002 │ page_code FILE_CATEGORIES
SCR-FILE-002 │ File Browser    │ COMPOSITE PATTERN-2 SIDE_DRAWER │ ENTITY-FILE-001 │ page_code FILE_BROWSER
LOV REGISTRY (FILE-local runtime codes)
LOV-FILE-001 │ FILE_FILE_TYPE   │ fileTypeId   │ IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER
LOV-FILE-002 │ FILE_FILE_STATUS │ fileStatusId │ ACTIVE, ARCHIVED, DELETED
QRC SUMMARY : QR-FILE-0001..0011 (SECTION B). ⚠ AGENT REFERENCE only.
DB ALIGNMENT : SECTION 2 — ALIGNED ✓ (18 FIELD↔DBF)
XM STATUS    : XM-FILE-001 SOFT-READ → SEC_USER_ACCOUNT (Security) — READY ✓ (SEC gated ACTIVE, built earlier)
SECURITY     : 2 admin screens × 4 CORE-9 permissions (FILE_ADMIN). Auth delegated to Security filter (RULE-FILE-004).
══════════════════════════════════════════════════════════════════

---

## SECTION 2 — DB ALIGNMENT MANIFEST — FILE — DBS-ID: DBS-FILE-001
══════════════════════════════════════════════════════════════════
FIELD-ID       │ DBF-ID        │ Plan Type   │ FK/XM-ID                   │ Match
───────────────┼───────────────┼─────────────┼────────────────────────────┼──────
FIELD-0002/3/4 │ DBF-0002/3/4  │ (polymorphic)│ owner ref (NO FK — app-level)│ ✓
FIELD-0008     │ DBF-0008      │ byte[]      │ BYTEA                      │ ✓
FIELD-0011     │ DBF-0011      │ Long        │ FK FILE_CATEGORY_FK→FILE_CATEGORY │ ✓
(created_by identity read of SEC_USER_ACCOUNT is XM-FILE-001 SOFT-READ — application layer, no column FK)
All other FIELD-IDs align 1:1 to their DBF-ID. No type mismatch.
══════════════════════════════════════════════════════════════════
Legend: ✓ aligned | ✗ mismatch | ⏸ XM deferred. CONTRACT-1: FIELD-ID/DBF-ID/Type/FK/Status only. Audit cols: no DBF-ID.

---

## SECTION 3 — OPEN QUESTIONS LOG (continuation)
══════════════════════════════════════════════════════════════════
Open Questions: None — file-delete semantics resolved in srs-FILE (RULE-FILE-006 soft-delete). No new OQ in PASS 1.
══════════════════════════════════════════════════════════════════

---

## SECTION 4 — DERIVATION LOG
══════════════════════════════════════════════════════════════════
DRV-001 │ ERR-0006 NOT_FOUND (file/category)     │ PLATFORM │ Standard 404 for get/metadata/archive by id
DRV-002 │ QR-FILE-0011 EXISTS categoryCode        │ CRIT-2   │ RULE-FILE-007 pre-insert uniqueness check
DRV-003 │ BYTEA streamed with @Lob                │ CRIT-1   │ FILE_CONTENT BYTEA (db-script) — large binary, streamed not eager-loaded on list
DRV-004 │ Effective size/type limit resolution    │ CRIT-2   │ RULE-FILE-001/002 defaults overridden per FileCategory (maxSizeBytes/allowedContentTypes)
══════════════════════════════════════════════════════════════════

---









---

## SECTION A — ERROR CATALOG (canonical)
══════════════════════════════════════════════════════════════════════════════════
ERR-ID   │ RULE-ID       │ API-ID              │ HTTP │ Trigger                 │ Message-AR                      │ Message-EN
─────────┼───────────────┼─────────────────────┼──────┼─────────────────────────┼──────────────────────────────────┼──────────────────────────
ERR-0001 │ RULE-FILE-001 │ API-FILE-001        │ 413  │ File exceeds size limit  │ حجم الملف يتجاوز المسموح.        │ File exceeds allowed size.
ERR-0002 │ RULE-FILE-002 │ API-FILE-001        │ 415  │ Disallowed content type  │ نوع الملف غير مسموح.             │ File type not allowed.
ERR-0003 │ RULE-FILE-003 │ API-FILE-003        │ 401  │ Invalid/expired token    │ رابط الوصول غير صالح/منتهٍ.       │ Access link invalid or expired.
ERR-0004 │ RULE-FILE-005 │ API-FILE-001        │ 400  │ Missing ownership fields │ بيانات الملكية إلزامية.          │ Ownership fields required.
ERR-0005 │ RULE-FILE-007 │ API-FILE-007        │ 409  │ Duplicate category code  │ رمز الفئة مستخدَم مسبقاً.         │ Category code already exists.
ERR-0006 │ PLATFORM-STD  │ API-FILE-002/004/006/007/008 │ 404 │ Resource not found │ العنصر غير موجود.               │ Resource not found.
══════════════════════════════════════════════════════════════════════════════════
Total Errors: 6 (ERR-0006 = PLATFORM-STD, DRV-001). RULE-FILE-004 internal; RULE-FILE-006 permissive (own message, no error).
Every ERR-ID registered in 4 places.

---

## SECTION B — QUERY REFERENCE CATALOG (agent reference)
══════════════════════════════════════════════════════════════════
⚠ AGENT REFERENCE ONLY — rewrite every query using actual JPA entity/field names.
QR-FILE-0001 SAVE file document + bytes (SEQ_FILE_DOCUMENT) — READ_WRITE
QR-FILE-0002 FIND_ONE document by ID (for token issue) — READ_ONLY
QR-FILE-0003 FIND_ONE document bytes by ID (download, @Lob stream) — READ_ONLY
QR-FILE-0004 FIND_ONE document metadata by ID (bytes excluded) — READ_ONLY
QR-FILE-0005 FIND_BY_CRITERIA documents by owner (metadata only; ownerId/type/moduleCode EXACT, fileName LIKE, status/type EXACT) — READ_ONLY, paged
QR-FILE-0006 UPDATE fileStatusId (archive/soft-delete) — READ_WRITE
QR-FILE-0007 FIND_ONE category by ID — READ_ONLY
QR-FILE-0008 FIND_BY_CRITERIA categories — READ_ONLY, paged
QR-FILE-0009 SAVE category (SEQ_FILE_CATEGORY) — READ_WRITE
QR-FILE-0010 UPDATE category — READ_WRITE
QR-FILE-0011 EXISTS categoryCode (RULE-FILE-007) — READ_ONLY
Join governance: NEVER join to a lookups table (none). List queries exclude BYTEA content (DRV-003).
══════════════════════════════════════════════════════════════════

---

## SECTION C — REGISTRY UPDATE BLOCK
══════════════════════════════════════════════════════════════════
## REGISTRY UPDATE — 2026-09-02
Source: Project 3.1 PASS 1 | Feature Code FILE-001 | DBS-FILE-001 | Plan PLAN-FILE-001
New APIs: API-FILE-001..008 | QR-IDs: QR-FILE-0001..0011 (11)
XM-IDs: XM-FILE-001 SOFT-READ → SEC (READY ✓, ACTIVE)
OQ-IDs Open: None | Gate Status: ALIGN-BE PASSED ✓ | Next: Project 4.1 → Pipeline Grid FILE · P3.1 = done
══════════════════════════════════════════════════════════════════

---

## SECTION D — TC COVERAGE MATRIX SUMMARY (backend)
══════════════════════════════════════════════════════════════════
RULE-ID COVERAGE:
RULE-FILE-001 │ TC-BE-FILE-001 │ TC-BE-FILE-002 │ COVERED ✓
RULE-FILE-002 │ TC-BE-FILE-003 │ TC-BE-FILE-004 │ COVERED ✓
RULE-FILE-003 │ TC-BE-FILE-005 │ TC-BE-FILE-006 │ COVERED ✓
RULE-FILE-004 │ TC-BE-FILE-007 │ —             │ COVERED ✓ (auth-delegation assertion)
RULE-FILE-005 │ TC-BE-FILE-008 │ TC-BE-FILE-009 │ COVERED ✓
RULE-FILE-006 │ TC-BE-FILE-010 │ —             │ COVERED ✓ (soft-delete, bytes retained)
RULE-FILE-007 │ TC-BE-FILE-011 │ TC-BE-FILE-012 │ COVERED ✓
Rule coverage: 7/7 — 0 gaps.
API-ID COVERAGE: API-FILE-001..008 each ≥1 happy-path TC (TC-BE-FILE-013..020) — 8/8 covered.
DEFERRED TC REGISTRY: (none)
══════════════════════════════════════════════════════════════════
Gate SECTION D: PASSED ✓

---

## AGENT HANDOFF SUMMARY (BACKEND) — not a phase
Agent-ready. Rewrite QRC from scratch; store bytes in BYTEA @Lob (stream, never eager-load on list); auto-detect MIME server-side;
issue AES/GCM single-use access tokens (~100m) separate from JWT; delegate auth to Security filter (never self-verify JWT);
soft-delete = fileStatusId=DELETED (retain bytes); enforce per-category size/type limits. Run api-doc-generator before PASS 2.

*End of backend-execution-plan.md — FILE — PLAN-FILE-001 — ALIGN-BE ✓*