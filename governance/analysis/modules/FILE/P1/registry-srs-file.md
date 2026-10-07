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
| RULE-FILE-009 | NEW: SVG only when the request allows it, and only if the safety check passes (no script, no `on…=`, no external `href` or CSS `url()`, no `javascript:` / `@import`, no DOCTYPE / ENTITY, no `foreignObject`) — rejected, never rewritten |
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
