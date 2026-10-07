<!-- Source: PHASE:DATA-DOM -->

## PHASE DATA+DOM — Entity & Domain Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
(2 entities < 5 → no SUB split)

### ENTITY-FILE-001 — FileDocument
  DB Table FILE_DOCUMENT | PK ID | Sequence SEQ_FILE_DOCUMENT | DBS-FILE-001. BUSINESS CODE: NONE.
  FIELDS (0001..0011 → DBF-0001..0011):
    FIELD-0001 fileDocumentPk  ID              DBF-0001 BIGINT       PK SEQ_FILE_DOCUMENT     | المعرف / ID
    FIELD-0002 ownerId         OWNER_ID        DBF-0002 BIGINT       polymorphic (no FK)      | معرّف المالك / Owner ID
    FIELD-0003 ownerType       OWNER_TYPE      DBF-0003 VARCHAR(100) polymorphic              | نوع المالك / Owner Type
    FIELD-0004 moduleCode      MODULE_CODE     DBF-0004 VARCHAR(50)  owning module            | رمز الموديول / Module Code
    FIELD-0005 fileName        FILE_NAME       DBF-0005 VARCHAR(255) NOT NULL                 | اسم الملف / File Name
    FIELD-0006 contentType     CONTENT_TYPE    DBF-0006 VARCHAR(150) System — auto-detected (RULE-FILE-002) | نوع المحتوى / Content Type
    FIELD-0007 fileSize        FILE_SIZE       DBF-0007 BIGINT       System (bytes)           | الحجم / Size
    FIELD-0008 fileContent     FILE_CONTENT    DBF-0008 BYTEA        NOT NULL (@Lob, streamed)| المحتوى / Content
    FIELD-0009 fileTypeId      FILE_TYPE_ID    DBF-0009 VARCHAR(50)  LOV-FILE-001 code        | نوع الملف / File Type
    FIELD-0010 fileStatusId    FILE_STATUS_ID  DBF-0010 VARCHAR(50)  LOV-FILE-002 code (A6)   | الحالة / Status
    FIELD-0011 fileCategoryFk  FILE_CATEGORY_FK DBF-0011 BIGINT      FK→FILE_CATEGORY (nullable)| الفئة / Category
  DTO: UploadRequest{multipart file, ownerId, ownerType, moduleCode, fileCategoryFk?} (contentType/fileSize/fileTypeId system-derived);
       ResponseDTO: metadata only — fileContent NEVER in a JSON DTO (downloaded via secure token).
  STATE MACHINE (fileStatusId — LOV-FILE-002): ACTIVE → ARCHIVED → DELETED (and ACTIVE → DELETED soft). Initial: ACTIVE.
  DOMAIN RULES: RULE-FILE-001 (size ≤5MB content/≤10MB request, category override), RULE-FILE-002 (MIME auto-detect + allowed types),
       RULE-FILE-003 (AES/GCM single-use ~100m token), RULE-FILE-004 (delegate auth to Security), RULE-FILE-005 (ownership required),
       RULE-FILE-006 (soft delete). Full text in Error Catalog + SVC+API.
  CROSS-MODULE: XM-FILE-001 SOFT-READ → SEC_USER_ACCOUNT (created_by identity + auth filter) — no FK. QR: QR-FILE-0001..0006.

### ENTITY-FILE-002 — FileCategory
  DB Table FILE_CATEGORY | PK ID | Sequence SEQ_FILE_CATEGORY. FIELDS 0012..0018 (categoryCode UNIQUE, nameAr, nameEn, maxSizeBytes, allowedContentTypes[TEXT], isActiveFl).
  DOMAIN RULES: RULE-FILE-007 (unique categoryCode — Message-AR: رمز الفئة مستخدَم مسبقاً.). Feeds RULE-FILE-001/002 per-category limits.
  QR: QR-FILE-0007..0011.

DATA+DOM Governance: BIND-RULE-1/2/3/4 — exact column/sequence/LOOKUP_CODE/RULE text from srs-FILE/db-script-FILE.
─────────────────────────────────────────────────────────────────
