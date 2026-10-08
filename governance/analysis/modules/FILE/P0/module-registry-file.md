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
Steps          : 02, 03, 05, 06, 07, 08, 10
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No ENTITY / LOV / XM ids are minted.

ENTITIES OWNED — deltas
| Entity | Delta | Source |
|---|---|---|
| FileDocument | CHANGED: tenant-scoped + `version`; + storage provider / reference, visibility, public slug, content hash; content bytes only for the DB provider | V10, V12__file_storage.sql |
| FileCategory | CHANGED: tenant-scoped + `version`; + `allowPublic` | V10, V12 |

DEPENDENCIES — deltas
| Module | Kind | What | Source |
|---|---|---|---|
| tenant | HARD FK + API | `CORE_TENANT` FK; `TenantLookupApi.codeOf(tenantId)` to build public URLs; path-based tenant resolution for public files | DEVIATIONS [07] (`publicUrl` entry) |
| events | publishes | `FileDocumentPublishedEvent` (upload: PRIVATE; made public: PUBLIC) | DEVIATIONS [08] |
| audit | SOFT | `@Audited` on `FileDocument`, `FileCategory` | DEVIATIONS [10] |
| SEC | SPI | `FilePermissions` implements `PermissionContributor` | docs/steps/06-report.md |
"File Service calls NO other module" no longer holds strictly: it reads the tenant code through the
tenant module's `crossmodule` API (additive, no cycle).

EXPOSED SURFACE — deltas
| Surface | Delta | Source |
|---|---|---|
| `com.erp.file.storage.StorageProvider` (`key/put/get/delete/publicUrl`) | NEW public SPI; built-in DB (always), LOCAL (with `local.root`), S3 (SDK present + bucket); an application may override a provider by key | docs/steps/07-report.md; DEVIATIONS [07] |
| `FileDocumentLookupApi.publicUrl(Long)` | NEW: public URL of a servable public document | docs/steps/07-report.md |

PERMISSIONS
Code-defined by `FilePermissions`: `PERM_FILE_CATEGORIES_VIEW/CREATE/UPDATE/DELETE`,
`PERM_FILE_BROWSER_VIEW/CREATE/UPDATE/DELETE` (unchanged), NEW `FILE:DOCUMENT:PUBLISH` (screen
`FILE_BROWSER`, action `PUBLISH`, explicit legacy-style authority). — DEVIATIONS [07] (rebase onto 06)

AUTO-DECISIONS revisited
- "File bytes stored in PostgreSQL BYTEA" → DB provider is the default; LOCAL and S3 are selectable (step 07).
- "Secure access via AES/GCM encrypted URL token" → unchanged; the single-use store is `DownloadTokenStore`
  (in-memory, or Redis when present) — DEVIATIONS [02], [03].

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
