# REGISTRY EXTRACT — registry-srs-FILE
══════════════════════════════════════════════════════════════════
Module          : File Service (FILE)
Source artifact : srs-FILE.md (v1.1)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : File Service (خدمة الملفات)
Module Prefix : FILE
OQ count : 0 (none — delete semantics resolved via RULE-FILE-006, no OQ raised)

## ENTITIES (PART A — A3)
| ENTITY-ID | Entity Name | Type |
|---|---|---|
| ENTITY-FILE-001 | FileDocument | PRIVATE |
| ENTITY-FILE-002 | FileCategory | PRIVATE |

## RULES (PART A — A4)
| RULE-ID | Short Title | Test-Hint |
|---|---|---|
| RULE-FILE-001 | Max file size limit | — |
| RULE-FILE-002 | MIME auto-detect, restrict types | — |
| RULE-FILE-003 | Time-limited encrypted access token | — |
| RULE-FILE-004 | Delegate auth to Security filter | — |
| RULE-FILE-005 | Ownership fields required | — |
| RULE-FILE-006 | Soft delete, bytes retained | — |
| RULE-FILE-007 | Unique category code | — |

## LOVs (PART A — A5)
| LOV-ID | LOV Name |
|---|---|
| LOV-FILE-001 | FileType (FILE_FILE_TYPE) |
| LOV-FILE-002 | FileStatus (FILE_FILE_STATUS) |

## LIFECYCLE STATES (PART A — A6)
FileDocument: ACTIVE → ARCHIVED → DELETED (also ACTIVE → DELETED direct soft-delete)

## DEPENDENCIES (PART A — A7)
| Type | Target ENTITY-ID | Target Module | XM candidate |
|---|---|---|---|
| SOFT | ENTITY-SEC-001 (UserAccount, via auth filter / created_by) | SEC | Yes |
Note: CU (exceptions/config/events/filtering) is USES (library) — not a governed dependency type.

## SCREENS (PART B)
| SCR-ID | page_code | Screen Name | Pattern |
|---|---|---|---|
| SCR-FILE-001 | FILE_CATEGORIES | File Categories | PATTERN-2 (SIDE_DRAWER) |
| SCR-FILE-002 | FILE_BROWSER | File Browser / Management | PATTERN-2 (SIDE_DRAWER) |

## APIs (PART B — B5)
| API-ID | Method | Endpoint | Owning SCR-ID |
|---|---|---|---|
| API-FILE-001 | POST | /api/v1/files | SCR-FILE-002 (upload optional/contextual) |
| API-FILE-002 | POST | /api/v1/files/{id}/access-token | SCR-FILE-002 |
| API-FILE-003 | GET | /api/v1/files/download?token= | SCR-FILE-002 |
| API-FILE-004 | GET | /api/v1/files/{id} | SCR-FILE-002 |
| API-FILE-005 | GET | /api/v1/files?ownerId=&ownerType=&moduleCode= | SCR-FILE-002 |
| API-FILE-006 | DELETE | /api/v1/files/{id} | SCR-FILE-002 |
| API-FILE-007 | POST/GET/PUT/DELETE | /api/v1/files/categories | SCR-FILE-001 |
| API-FILE-008 | GET | /api/v1/files/lookups/{lookupKey} | (cross-screen) |

## PERMISSIONS (Permissions Summary)
| PERM Name | Linked SCR-ID(s) |
|---|---|
| PERM_FILE_CATEGORIES_{VIEW,CREATE,UPDATE,DELETE} | SCR-FILE-001 |
| PERM_FILE_BROWSER_{VIEW,UPDATE,DELETE} (no CREATE — upload is contextual) | SCR-FILE-002 |
All granted to FILE_ADMIN per srs-FILE Permissions Summary.

## OQ LOG STATUS
| OQ-ID | Status | One-line topic | Escalation |
|---|---|---|---|
| (none logged) | — | Delete semantics resolved via RULE-FILE-006 (soft-delete) | — |

---
*End of registry-srs-FILE.md*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 03, 05, 06, 07, 08, 10
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs.md` → "Implementation Addendum — erp-core 1.2.0". No new ids are
assigned here.

### ENTITIES — delta
| Entity | Type | Delta |
|---|---|---|
| ENTITY-FILE-001 FileDocument | PRIVATE | CHANGED: + storage provider / ref, visibility, public slug, content hash; tenant-scoped + version; content optional |
| ENTITY-FILE-002 FileCategory | PRIVATE | CHANGED: + allowPublic; tenant-scoped + version |

### RULES — delta
| Rule | Delta |
|---|---|
| RULE-FILE-001 | CHANGED: limits configurable (`erp.core.files.max-*`) |
| RULE-FILE-003 | CHANGED: token store in-memory or Redis |
| RULE-FILE-007 | CHANGED: unique per tenant |
| (new) | storage provider selection; LOCAL / S3 key layout and traversal guard; publish only in `ALLOW_PUBLIC` categories; public serving conditions and headers; public URL forms; tenant confinement |

### DEPENDENCIES — delta
| Type | Target | Module |
|---|---|---|
| HARD-FK + API | `CORE_TENANT`; `TenantLookupApi.codeOf` | tenant |
| publishes | `FileDocumentPublishedEvent` | events |

### APIs — delta
| Kind | Method | Endpoint | Owning SCR-ID |
|---|---|---|---|
| NEW | PATCH | /api/v1/files/{id}/visibility | SCR-FILE-002 |
| NEW | GET (HEAD) | /api/v1/public/files/{tenantCode}/{publicSlug} | — (public, no screen) |
| CHANGED | POST / GET | /api/v1/files, /api/v1/files/{id}, /api/v1/files/download, /api/v1/files/categories | SCR-FILE-001 / 002 |

### PERMISSIONS — delta
| PERM Name | Linked SCR-ID(s) |
|---|---|
| `FILE:DOCUMENT:PUBLISH` (NEW) | SCR-FILE-002 (`FILE_BROWSER`) |
| analysed `PERM_FILE_*` | unchanged, now code-defined (`FilePermissions`) |

### ERROR CODES — delta
NEW: `FILE_PUBLIC_NOT_ALLOWED` (409), `FILE_STORAGE_UNAVAILABLE` (500). Source: docs/api-docs/file/index.md.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.4 — shared image store
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs.md` → "Implementation Addendum — erp-core 1.3.0". Ids continue from the
highest ever issued (RULE-FILE-007, XM-FILE-001, no ADR).

### ENTITIES — delta
None (no field, no LOV value). Image-store documents are ordinary `FILE_DOCUMENT` rows without a category.

### RULES — delta
| Rule | Delta |
|---|---|
| RULE-FILE-008 | NEW: image type detected from the content (PNG / JPEG / WebP magic bytes, SVG text), must be allowed by the request; size 1..`maxBytes` |
| RULE-FILE-009 | NEW: SVG only when the request allows it, and only if it passes the allow-list (strict UTF-8, hardened parse, comments + one `<svg>` root and no processing instruction, SVG-namespace static elements, listed attributes plus inert `data-*`, local `#` references, CSS without escapes / `//` / at-rules but `@media` / fetching functions, `url(#…)` only, `<style>` without comments, depth ≤ 64, ≤ 100 flat `<use>`) — rejected, never rewritten (review rounds 1–2) |
| RULE-FILE-010 | NEW: image-store documents are uncategorised, PUBLIC at once with a random slug, served on the public path; discard = DELETED + PRIVATE |
| step 07 publish rule | CHANGED: the public lookup also serves an uncategorised PUBLIC document (only the image store creates one; `PATCH /visibility` unchanged) |

### DEPENDENCIES — delta (exposed)
| Kind | Id | Surface | Consumers |
|---|---|---|---|
| NEW | XM-FILE-002 | `FileImageStoreApi.storePublicImage` / `discard` | SEC (photos, XM-SEC-006); TENANT (logos, package E) |
| CHANGED | XM-FILE-001 | `FileDocumentLookupApi` + `publicUrls(Collection<Long>)` | SEC (user lists) |

### APIs — delta
None (no new FILE endpoint). The public GET serves uncategorised image-store documents too.

### PERMISSIONS / ERROR CODES — delta
None.

Last sequence per atom (highest ever issued): RULE: 010 · XM: 002 · API: 008 · ADR: 008 (ADR-FILE-001..007 are
the as-built FILE ADRs of the analysis-coverage work; the next free FILE ADR is 009).

### DECISIONS
| Kind | ADR | Subject |
|---|---|---|
| NEW | ADR-FILE-008 | Profile photos and logos are PUBLIC documents with non-guessable slugs (no category) |

Package B (tenant-maturity plan §4 B.4) — registry delta; full text in `srs.md` 1.3.0 §7.
| Kind | Id | Surface | Consumers |
|---|---|---|---|
| CHANGED | XM-FILE-001 | `FileDocumentLookupApi` + `countDocuments()`, `sumBytes()` (current tenant, documents not DELETED) | TENANT (usage) |
No rule, entity, API, permission or error-code delta. Last sequence per atom unchanged (RULE: 010 · XM: 002 ·
API: 008 · ADR: 008).

Package E (tenant-maturity plan §7) — registry delta; full text in `srs.md` 1.3.0 §8.
| Kind | Id | Delta |
|---|---|---|
| CHANGED | RULE-FILE-009 | item (8) + every `id` unique: an SVG with a duplicate `id` is refused (`UNSAFE_SVG`), so the nested-`<use>` guard cannot be fooled by a decoy (package D review round 3, carried over) |
| CHANGED | XM-FILE-002 | consumer TENANT implemented (logos, `CORE_TENANT` / tenant id / `TENANT`, inside `TenantContext.callAs(tenantId)`) |
No entity, API, permission, error-code or migration delta. Last sequence per atom unchanged (RULE: 010 · XM: 002 ·
API: 008 · ADR: 008; next free FILE ADR 009).

Package C5 (tenant-maturity plan §5 C.5) — registry delta; full text in `srs.md` 1.3.0 §9.
| Kind | Id | Delta |
|---|---|---|
| NEW | XM-FILE-003 | `FilePrivateStoreApi.storePrivateFile(PrivateFileStoreRequest)` → `StoredPrivateFile`, `issueDownloadToken(Long)` → `DownloadGrant` (current tenant; `isAuthenticated()`, the consumer's permission) — consumer TENANT (export archive) |
| NEW | RULE-FILE-011 | a privately stored file: current tenant, no category, PRIVATE, ACTIVE, type from the declared content type, SHA-256 hash, no upload limits (bounded by the producer); token = the upload's single-use token bound to the issuing username |
| NEW | — | `com.erp.file.tenant.FileTenantExportContributor` implements TENANT XM-TENANT-004: `FILE_CATEGORY`, `FILE_DOCUMENT` metadata (never `FILE_CONTENT`, `STORAGE_REF`, `PUBLIC_SLUG`) |
No entity, API, permission, error-code or migration delta. Last sequence per atom (highest ever issued): RULE: 011 ·
XM: 003 · API: 008 · ADR: 008 (next free FILE ADR 009).

Package C5 review round 1 — registry delta; full text in `srs.md` 1.3.0 §9.5.
| Kind | Id | Delta |
|---|---|---|
| NEW | RULE-FILE-012 | restricted documents (`REQUIRED_AUTHORITY`): hidden from the owner list, 404 on metadata / token / download / visibility / archive-delete without the authority; deleting one removes its bytes (tombstone kept) |
| CHANGED | XM-FILE-003 | `PrivateFileStoreRequest` + `requiredAuthority`; `issueDownloadToken` checks it |
| CHANGED | XM-FILE-001 | `isAvailable` false for a restricted document |
| CHANGED | ENTITY-FILE-001 | `@Audited(ignore = {"storageRef", "publicSlug"})`; + field `requiredAuthority` (`V22__file_document_required_authority.sql`) |
Last sequence per atom (highest ever issued): RULE: 012 · XM: 003 · API: 008 · ADR: 008 (next free FILE ADR 009).

Closure (tenant-maturity Z) — registry note; full text in `srs.md` 1.3.0 §10. No id minted.
| Kind | Id | Delta |
|---|---|---|
| NOTE | RULE-FILE-012 | known limitation: on `LOCAL` / `S3` a failed after-commit delete of a restricted document's object is only logged (WARN); the tombstone is committed, the object stays, nothing retries it (follow-up: a sweeper with the export-archive retention job) |
