<!-- Source: PHASE:CORE -->

## PHASE CORE — Architectural Policies
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓

CANONICAL ARCHITECTURE — NON-NEGOTIABLE (backend layers):
  controller/ → REST endpoints only (delegate to service; no business logic)
  service/    → application orchestration + transaction boundaries
  mapper/     → Entity ↔ DTO transformation only
  domain/     → business rules owner
  repository/ → data access only
  entity/     → JPA entity + domain behavior
  dto/ · exception/ · config/

Domain behavior placement: embedded in Entity methods (single simple entity —
medium-complexity mandate POLICY-CLI-01; no separate domain/ classes needed).

PROJECT-STANDARD CONSTRAINTS:
  Entity base      : AuditableEntity (createdBy/createdAt/updatedBy/updatedAt via AuditEntityListener)
                     ✗ audit fields never appear in CreateRequest/UpdateRequest
                     ✗ orgUnitId never appears in any DTO
  Error signaling  : service layer signals LocalizedException — NotFoundException BANNED
  Error catalog    : every ERR-ID registered in 4 places — ErrorCodes constant +
                     messages.properties + i18n JSON + ErpErrorMapperService
  Search contract  : SearchRequest extends BaseSearchContractRequest;
                     ALLOWED_SORT_FIELDS declared per search operation;
                     PageableBuilder.from(...) + SpecBuilder.build(...)
  Deactivation     : isActiveFl = false (record preserved — never hard-deleted)
  i18n / Bundle    : AR + EN resolved via CU's own resource bundles (messages_ar/_en)

TYPE MAPPING STANDARDS (POSTGRESQL_16 — project-standard, no DRV-ID):
  BIGINT        → Java Long
  VARCHAR(N)    → Java String
  TEXT          → Java String (+ @Lob if streamed)
  SMALLINT (_FL)→ Java Boolean
  TIMESTAMP     → Java LocalDateTime

MODULE-SPECIFIC NOTES:
  - CU is the ROOT cross-cutting library; it provides infrastructure
    (Specification/Filtering, Global Exceptions, Bundle, Configuration, Events)
    consumed by SEC/FILE/NOTIF via code injection — NOT an XM dependency.
  - AppConfiguration is the ONLY persisted entity. It is key/value (configKey is
    identity) — no nameAr/nameEn (LOC rule waived for this entity; DRV traced in
    srs-CU A2 general note — pure key/value store).
  - No Workflow Engine (RULE-13 = OFF). No LOVs. No screens (Backend-only).
─────────────────────────────────────────────────────────────────
