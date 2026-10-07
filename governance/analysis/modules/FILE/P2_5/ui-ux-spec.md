# ui-ux-spec-FILE.md
## File Service (FILE) — Component-Level Design Intent

```
Produced by     : UI/UX Design Engine (Project 2.5)
Governed by     : CONTRACT-11 · fields/permissions are SRS B3/B4 reference only
Inputs          : srs-FILE.md v1.1 (PART B: B1/B3/B4) + prd-FILE.md
Status          : RECONCILED — pending human design approval (CONTRACT-12)
Date            : 2026-09-02
Scope note      : Component NAMES/CSS/routing are PROPOSAL/intent only (P3.2 F1/F4
                  decides binding spec). React/TS/Vite; t() for all strings; CSS
                  logical properties only.
```

---

```
Screen           : SCR-FILE-001 — File Categories (إدارة فئات الملفات)
UI Pattern       : PATTERN-2 (Search list + Side Drawer entry) — from SRS B1, unchanged
Create/Edit Container Pattern : SIDE_DRAWER
                   (reference entity, bounded fields, no repeating child rows →
                    SIDE_DRAWER per AMEND-P3-O; matches SRS B1)
Fields shown     : List/filters — categoryCode, nameAr, isActiveFl.
                   Drawer (create/edit) — categoryCode, nameAr, nameEn, maxSizeBytes,
                   allowedContentTypes, isActiveFl.
                   [SRS B3 — no additions, no omissions]
Permissions      : FILE_CATEGORIES → VIEW/CREATE/UPDATE/DELETE = FILE_ADMIN
                   (SRS B4 / CORE-9 — reference only; guard wired by P3.2)
Empty state      : "No categories defined." + "Create category" (if CREATE granted).
Loading state    : List skeleton rows; drawer field skeletons on edit-load.
Error state      : categoryCode uniqueness (RULE-FILE-007) as inline error; other
                   failures as a generic banner.
Design intent note (PROPOSAL): maxSizeBytes shown with a human-readable helper
                   (e.g. "= 5 MB") but stored in bytes as SRS specifies.
                   allowedContentTypes edited as a chips/tags input mapping to the
                   stored TEXT list. categoryCode read-only after create. These limits
                   are the per-category overrides that feed RULE-FILE-001/002 at upload.
```

---

```
Screen           : SCR-FILE-002 — File Browser / Management (مستعرض الملفات)
UI Pattern       : PATTERN-2 (Search list + Side Drawer detail/actions) — from SRS B1
Create/Edit Container Pattern : SIDE_DRAWER (detail + actions panel)
                   (this is primarily a SEARCH + read-only DETAIL screen with actions;
                    metadata is read-only, so the drawer is a detail/action panel, not
                    a create form. CREATE/upload is contextual — see note. Matches
                    SRS B1 SIDE_DRAWER.)
Fields shown     : List/filters — fileName, moduleCode, ownerType, ownerId,
                   fileTypeId (LOV-FILE-001), fileStatusId (LOV-FILE-002).
                   Drawer (read-only metadata) — fileName, contentType, fileSize,
                   fileTypeId, fileStatusId, fileCategoryFk, ownerId, ownerType,
                   moduleCode. Actions — Download (secure token), Archive, Delete (soft).
                   (fileContent BYTEA is NOT a displayed field — download only.)
                   [SRS B3]
Permissions      : FILE_BROWSER → VIEW=FILE_ADMIN · CREATE = — (upload is contextual
                   in the owning module) · UPDATE=FILE_ADMIN (archive) ·
                   DELETE=FILE_ADMIN (soft delete). (SRS B4)
Empty state      : "No files match these filters." Ownership filters prominent since
                   listing is scoped by owner (RULE-FILE-005).
Loading state    : List skeleton; a distinct "preparing secure link…" state while the
                   access token is issued before download begins.
Error state      : expired/invalid access link (RULE-FILE-003) → offer to re-request
                   the link; download-auth failures handled by Security filter
                   (RULE-FILE-004), surfaced generically.
Design intent note (PROPOSAL): Download is a two-step affordance under the hood
                   (issue token → fetch) but a single user action. Archive and Delete
                   are confirmed actions; Delete is explicitly soft (bytes retained,
                   RULE-FILE-006) and the confirmation says so. LOV dropdowns show
                   active values only. Upload affordance, if surfaced here at all, is
                   optional and secondary — the primary upload path is the owning
                   module's screen via the FileService provider.
```

---

## CROSS-MODULE (UXD) REFERENCE

`UXD-FILE-001` — SCR-FILE-002 displays user-identity (created-by / owner name) sourced
from the **SEC UserAccount API** (SOFT-READ). Recorded here per Section 3A; confirmed
against a real SEC API by P4.2 before FE implementation clears.

---

*End of ui-ux-spec-FILE.md — every field/permission traced to srs-FILE.md B3/B4.*
*Component names/CSS/routing are PROPOSAL intent, not binding (CONTRACT-11).*
