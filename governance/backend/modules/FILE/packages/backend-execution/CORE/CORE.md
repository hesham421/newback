<!-- Source: PHASE:CORE -->

## PHASE CORE — Architectural Policies
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
CANONICAL ARCHITECTURE (backend): controller/ service/ mapper/ domain/ repository/ entity/ dto/ exception/ config/
Domain behavior placement: separate classes in domain/ (access-token issuing AES/GCM, MIME auto-detect, size/type policy resolution are non-trivial — e.g. FileAccessTokenDomainService, FileValidationDomainService).
PROJECT-STANDARD CONSTRAINTS:
  Entity base      : AuditableEntity on both tables (audit via AuditEntityListener). ✗ orgUnitId never in any DTO.
  Error signaling  : LocalizedException — NotFoundException BANNED.
  Error catalog    : every ERR-ID registered 4× (ErrorCodes + messages.properties + i18n JSON + ErpErrorMapperService).
  Search contract  : SearchRequest extends BaseSearchContractRequest; ALLOWED_SORT_FIELDS per search.
  Deactivation     : FileCategory isActiveFl=false; FileDocument soft-delete via fileStatusId=DELETED (RULE-FILE-006) — bytes retained.
  Auth             : delegate to Security filter — FILE never self-verifies JWT (RULE-FILE-004). Owner-based visibility (RULE-FILE-005).
  Access tokens    : AES/GCM, ~100m TTL, single-use — separate from JWT (RULE-FILE-003).
TYPE MAPPING (POSTGRESQL_16): BIGINT→Long · VARCHAR(N)→String · TEXT→String(+@Lob) · BYTEA→byte[](@Lob) · SMALLINT(_FL)→Boolean · TIMESTAMP→LocalDateTime.
MODULE-SPECIFIC NOTES:
  - Provider pattern: FileService is a @Service injected into consumers (NOTIF uses it for template attachments). No HTTP call needed in-process.
  - Ownership is polymorphic (ownerId/ownerType/moduleCode) — application reference, NOT a governed FK.
  - LOV values runtime codes (no ENUM, no lookup table). contentType is server-detected, never trusted from client (RULE-FILE-002).
  - No Workflow Engine (RULE-13 = OFF).
─────────────────────────────────────────────────────────────────
