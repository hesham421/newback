## MODULE REGISTRY — FILE SERVICE
══════════════════════════════════════════════════════════════════
Module Name    : File Service
Module Code    : FILE
Layer          : L1
Type           : Service (Foundation — binary storage + secure access)
Execution Tier : T1 (built after Common Utils + Security)
P0 Date        : 2026-09-01
Readiness      : READY
Domain KB      : none supplied — derived from domain-profile-ERP.md + ARCH-REF-1.10 (idea source)
Source         : NEW  (fresh — ARCH-REF-1.10-FILE-SERVICE.md used as IDEA reference only)
══════════════════════════════════════════════════════════════════

SCOPE NOTE
──────────────────────────────────────────────────────────────────
Reusable file-storage foundation. Stores file bytes in PostgreSQL BYTEA
with metadata; grants time-limited access via AES/GCM encrypted URL tokens
(separate from the Security JWT). Provider pattern: consumer modules call
File Service via @Service injection (Modular Monolith) — no RabbitMQ, no
external filesystem, no Eureka/Feign. Generic ownership (owner_id +
owner_type + module_code) so any current/future module can own files.
Adapted from the HEAC government reference: Oracle BLOB → PostgreSQL BYTEA,
Oracle UCP → HikariCP, PDFBox dropped, RabbitMQ dropped, composite PK →
standard Long PK. No MasterData module exists in the Foundation domain, so
File Service owns its own small LOVs locally.

ENTITIES OWNED
──────────────────────────────────────────────────────────────────
FileDocument │ Transactional │ PRIVATE  (accessed by consumers via service API, not as a shared table)
FileCategory │ Reference     │ PRIVATE  (per-consumer document types + limits — extensible config mechanism)
──────────────────────────────────────────────────────────────────
Note: names only — ENTITY-IDs assigned by P1. FileDocument fields
(owner_id, owner_type, module_code, file_name, content_type, file_size,
file_content=BYTEA, file_type, status) are detailed at P1, not here.

LOVs OWNED
──────────────────────────────────────────────────────────────────
FileType   │ IMAGE / DOCUMENT / SPREADSHEET / ARCHIVE / OTHER  │ small fixed lookup
FileStatus │ ACTIVE / ARCHIVED / DELETED                       │ status lifecycle
──────────────────────────────────────────────────────────────────
Note: LOV-IDs assigned by P1. Owned locally (no MasterData module in
this domain). preferred over a central lookup — medium complexity.

LOVs CONSUMED (from other modules)
──────────────────────────────────────────────────────────────────
(none)
──────────────────────────────────────────────────────────────────

SHARED ENTITIES CONSUMED
──────────────────────────────────────────────────────────────────
(none — owner_id/owner_type is a polymorphic app-layer reference, not a
 governed shared-entity FK; created_by audit reads Security identity SOFT)
──────────────────────────────────────────────────────────────────

DEPENDENCIES
──────────────────────────────────────────────────────────────────
Common Utils │ USES (library) │ exceptions, config, events, specification/filtering
Security     │ SOFT           │ trusts Security auth filter; created_by identity
──────────────────────────────────────────────────────────────────
ROOT: NO — depends on Common Utils (lib) + Security (SOFT).
File Service calls NO other module (no circular dependency); it is a
provider consumed by NOTIF and future modules.

AUTO-DECISIONS
──────────────────────────────────────────────────────────────────
AUTO: File bytes stored in PostgreSQL BYTEA (not BLOB, not Large Objects).
FROM: DB_TARGET=POSTGRESQL_16 + ARCH-REF RESOLUTION-01 (5MB fits BYTEA).
IF WRONG: revisit only if max file size grows to GB-scale (then LO/object store).

AUTO: Secure access via AES/GCM encrypted URL token — payload
      {action, ts, ownerId, ownerType, moduleCode, fileName, fileCategory},
      TTL ~100 min. Separate from Security JWT.
FROM: ARCH-REF AD-FILE-02 + ADAPT-01. Key injected from env (AD-FILE-09).
IF WRONG: token payload/TTL tuned at P1/P3; mechanism stays.

AUTO: File Service does NOT validate JWT itself — trusts Security's filter.
FROM: ARCH-REF ADAPT-03 (single JWT authority = Security).
IF WRONG: n/a — avoids duplicate auth authority.

AUTO: Size limits 5MB content / 10MB request; MIME auto-detected.
FROM: ARCH-REF AD-FILE-05 / AD-FILE-06. Configurable per FileCategory.
IF WRONG: adjust defaults per category at P1.

AUTO: Integration via direct @Service injection — no RabbitMQ; no PDFBox.
FROM: ARCH-REF RESOLUTION-04 / RESOLUTION-03 (Modular Monolith, sync ops).
IF WRONG: async (virus scan/thumbnails) opened as a new decision if needed.

AUTO: FileType + FileStatus owned as module-local LOVs.
FROM: no MasterData module in Foundation domain; medium complexity.
IF WRONG: relocate to a shared lookup if one is introduced later.

INF-IDs
──────────────────────────────────────────────────────────────────
(none — all decisions traced to ARCH-REF + domain adaptation via
 AUTO-DECISIONS above; no unresolved gap)
──────────────────────────────────────────────────────────────────
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 03, 05, 06, 07, 08, 10, 15
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository (`com/erp/…` = `erp-core/src/main/java/com/erp/…`;
`V<N>` = `erp-core/src/main/resources/db/migration/core/`). No ENTITY / LOV / XM ids are minted.

ENTITIES OWNED — deltas
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | FileDocument | tenant-scoped + `version`; + storage provider (DB / LOCAL / S3, CHECK-closed) and reference, visibility (PRIVATE / PUBLIC), public slug, content hash; content bytes only for the DB provider | V10; V12__file_storage.sql |
| CHANGED | FileCategory | tenant-scoped + `version`; + `allowPublic` (native BOOLEAN); code normalised (trim + upper), immutable; no reactivation after DELETE | V10; V12; com/erp/file/entity/FileCategory.java:74-105 |

LOVs OWNED — deltas
| Kind | LOV | Delta | Source |
|---|---|---|---|
| CHANGED | FileType, FileStatus | Not module-local: MDL lookup types `FILE_FILE_TYPE` / `FILE_FILE_STATUS` (owner module FILE) seeded by V8 into `MDL_LOOKUP_TYPE` / `MDL_LOOKUP_VALUE`, same codes and labels; API-FILE-008 reads them through `MdlLookupApi`. The codes remain constants in FILE for the classifier and the state machine. | V8__mdl_seed.sql L25-31, L38-62; com/erp/file/service/FileLookupService.java:33-46; ADR-FILE-005 |

DEPENDENCIES — deltas
| Kind | Module | Type | What | Source |
|---|---|---|---|---|
| NEW | tenant | HARD FK + API | `CORE_TENANT` FK; `TenantLookupApi.codeOf(tenantId)` for public URLs; `TenantContext.require()` for the storage target; path-based tenant resolution for public files | DEVIATIONS [07] (`publicUrl` entry) |
| NEW | MDL | API (consumed) | `MdlLookupApi.readActiveValuesByKey` / `LookupOptionView` behind API-FILE-008 — "File Service calls NO other module" no longer holds (additive, no cycle: MDL does not call FILE) | FileLookupService.java:7-8, 48-57 |
| NEW | events | publishes | `FileDocumentPublishedEvent` (upload: PRIVATE; made public: PUBLIC; never on withdraw / archive / delete); no consumer in erp-core | DEVIATIONS [08] |
| NEW | audit | SOFT | `@Audited` on `FileDocument`, `FileCategory` | DEVIATIONS [10] |
| NEW | SEC | SPI + SOFT | `FilePermissions implements PermissionContributor`; `SecurityContextHelper.getCurrentUsername()` binds the download token (XM-FILE-001 unchanged) | docs/steps/06-report.md; com/erp/file/service/FileService.java:187, 205 |
| NEW | NOTIF (consumer of FILE) | API (exposed) | `FileDocumentLookupApi.isAvailable(Long)` validates `attachmentFileId` (XM-NOTIF-002) | com/erp/notif/service/NotificationTemplateService.java:153-157 |

EXPOSED SURFACE — deltas
| Kind | Surface | Delta | Source |
|---|---|---|---|
| NEW | `com.erp.file.storage.StorageProvider` (`key / put / get / delete / publicUrl`; records `StorageTarget`, `StoredObject`) | Public SPI; built-in DB (always), LOCAL (with `local.root`), S3 (SDK present + bucket). An application may REPLACE a built-in provider by contributing a bean with the same key (LOCAL / S3) or by defining `erpDbStorageProvider`; it cannot add a provider under a new key, because `CHK_FILE_DOCUMENT_STORAGE_PROVIDER` closes the set to DB / LOCAL / S3. Failures are reported only as `FILE_STORAGE_UNAVAILABLE`; the DB `put` runs inside the persisting transaction. | docs/steps/07-report.md; com/erp/autoconfigure/FileStorageAutoConfiguration.java:43-45, 52-87; V12 L34-35; ADR-FILE-001 |
| NEW | `FileDocumentLookupApi.isAvailable(Long)` | True for an existing, not-DELETED (ACTIVE or ARCHIVED) document of the current tenant; no `@PreAuthorize`; consumer NOTIF | com/erp/file/crossmodule/FileDocumentLookupApiImpl.java:24-29 |
| NEW | `FileDocumentLookupApi.publicUrl(Long)` | Public URL of a servable public document (PUBLIC, ACTIVE, category `ALLOW_PUBLIC`), else empty | FileDocumentLookupApiImpl.java:31-40 |
| CHANGED | "Provider pattern: consumer modules call File Service via @Service injection" | `FileService` is module-internal; a consumer stores a file over REST (API-FILE-001, `PERM_FILE_BROWSER_CREATE`) and validates its soft reference through `FileDocumentLookupApi` | FileService.java:52-55 |

PERMISSIONS — deltas
| Kind | Authority | Delta | Source |
|---|---|---|---|
| NEW | `PERM_FILE_BROWSER_CREATE` | gates the upload (the SRS said "no CREATE") | com/erp/file/permission/FilePermissions.java:28-29; V7 L134 |
| CHANGED | `PERM_FILE_BROWSER_UPDATE` / `_DELETE` | selected by API-FILE-006's `action` (ARCHIVE / DELETE) | FileService.java:280-285 |
| CHANGED | `PERM_FILE_CATEGORIES_VIEW/CREATE/UPDATE/DELETE`, `PERM_FILE_BROWSER_VIEW` | code-defined by `FilePermissions` AND seeded by V7 (module `FILE`, screens, 8 actions, role `FILE_ADMIN`, three-tier grants to `SYS_ADMIN` and `FILE_ADMIN`) | FilePermissions.java:17-68; V7 L39, L70-71, L129-136, L153, L166-199 |
| NEW | `FILE:DOCUMENT:PUBLISH` | screen `FILE_BROWSER`, action `PUBLISH`, explicit legacy-style authority seeded by V12 | DEVIATIONS [07] (rebase onto 06); ADR-FILE-004 |
| CHANGED | download, lookups | `isAuthenticated()` only (download additionally bound to the token's user); `FileDocumentLookupApi` ungated | FileService.java:194-207; FileLookupService.java:50-51 |

AUTO-DECISIONS revisited
| Kind | AUTO-DECISION | Revisit | Source |
|---|---|---|---|
| CHANGED | "File bytes stored in PostgreSQL BYTEA" | DB (BYTEA) is the default provider only; LOCAL and S3 are selectable through the `StorageProvider` SPI; `FILE_CONTENT` is nullable. The "IF WRONG" trigger (GB-scale) did not apply — the step-07 plan prescribed the SPI. | docs/steps/07-report.md; ADR-FILE-001 |
| CHANGED | "Secure access via AES/GCM encrypted URL token — payload {action, ts, ownerId, ownerType, moduleCode, fileName, fileCategory}, TTL ~100 min" | Payload is `fileId + expiry + nonce`; TTL 10 minutes; the token is bound to the issuing user and single-use (consumed after the content was opened); the single-use store is `DownloadTokenStore` (in-memory, or Redis when present). Download only — no upload token. | com/erp/file/domain/FileAccessTokenDomainService.java:28, 60-67; DEVIATIONS [02], [03]; ADR-FILE-003 |
| CHANGED | "Size limits 5MB content / 10MB request; MIME auto-detected. Configurable per FileCategory." | Defaults configurable (`erp.core.files.max-*`); the category limit replaces the content default (may be larger); the request ceiling is global. MIME is content-sniffed by the JDK (PDF / Office / ZIP / text not recognised); no platform type list — only the category allow-list. | FileValidationDomainService.java:39-86; FileService.java:130-141, 445-458 |
| CHANGED | "Integration via direct @Service injection" | In-process integration is limited to `FileDocumentLookupApi` (and the `StorageProvider` SPI); uploads by other modules go over REST. | FileService.java:52-55 |
| CHANGED | "FileType + FileStatus owned as module-local LOVs" | The "IF WRONG: relocate to a shared lookup if one is introduced" case occurred: MDL exists and owns the two lookup types (V8). | V8; ADR-FILE-005 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.4 — shared image store
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

EXPOSED SURFACE — deltas
| Surface | Delta | Source |
|---|---|---|
| `com.erp.file.crossmodule.FileImageStoreApi` (XM-FILE-002) | NEW: `storePublicImage(ImageStoreRequest)` → `ImageStoreResult`, `discard(Long)`; consumed by SEC (photos) and, in package E, TENANT (logos) | srs.md 1.3.0 §1 |
| `FileDocumentLookupApi.publicUrls(Collection<Long>)` (XM-FILE-001) | NEW method | srs.md 1.3.0 §1 |
| `FileDocumentLookupApi.countDocuments()`, `sumBytes()` (XM-FILE-001) | NEW methods (package B): the current tenant's live documents and their bytes; consumed by TENANT (usage figures) | srs.md 1.3.0 §7 |

Entities owned, permissions, dependencies: unchanged.

Package C5 (tenant-maturity plan §5 C.5) — EXPOSED SURFACE delta
| Surface | Delta | Source |
|---|---|---|
| `com.erp.file.crossmodule.FilePrivateStoreApi` (XM-FILE-003) | NEW: `storePrivateFile(PrivateFileStoreRequest)` → `StoredPrivateFile`, `issueDownloadToken(Long)` → `DownloadGrant`; consumed by TENANT (tenant export archive, PRIVATE in PLATFORM) | srs.md 1.3.0 §9 |
| `com.erp.file.tenant.FileTenantExportContributor` | NEW: implements TENANT's export SPI (XM-TENANT-004) — the tenant's `FILE_CATEGORY` and `FILE_DOCUMENT` metadata, never the bytes | srs.md 1.3.0 §9.3 |
Entities owned, permissions: unchanged. Dependencies: + TENANT root SPI `TenantExportContributor` (implemented).

Package C5 review round 1 — `FILE_DOCUMENT.REQUIRED_AUTHORITY` (V22) and RULE-FILE-012 (restricted documents, purge on
delete); `FilePrivateStoreApi` request + `requiredAuthority` (srs.md 1.3.0 §9.5).

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no FILE behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

AUTO-DECISIONS — deltas
| Kind | Item | Statement | Source |
|---|---|---|---|
| CHANGED | Shared helpers | `DomainRules`, `StatusTransitions`, `OwnedLookups`, `LookupOptionResponse`, `TokenHasher` moved to `com.erp.common` — no behaviour change | CHANGELOG [Unreleased] |
