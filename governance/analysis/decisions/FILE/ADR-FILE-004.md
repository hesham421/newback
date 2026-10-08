# ADR-FILE-004 — Legacy-style permission code `FILE:DOCUMENT:PUBLISH`

Module  : FILE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (SRS) — recorded after the fact (step 07, rebase onto 06)
Status  : ACCEPTED (non-breaking)

## Context
Every FILE permission the analysis knew follows the platform format `PERM_<SCREEN>_<ACTION>`
(`PERM_FILE_CATEGORIES_VIEW` … `PERM_FILE_BROWSER_DELETE`), the format step 06's permission catalogue
(`PermissionContributor`, `PermissionDef.of(screen, action, label)`) derives by default. Step 07 added
the publish / withdraw action (`PATCH /api/v1/files/{id}/visibility`) on screen `FILE_BROWSER` and
seeded it in `V12__file_storage.sql` as `FILE:DOCUMENT:PUBLISH`, granted to the PLATFORM tenant's
`SYS_ADMIN` and `FILE_ADMIN`, while the step-06 contributor mechanism did not yet exist on that branch.
When step 07 was rebased onto step 06, V12 was already applied history (core migrations are never
edited or renumbered) and the catalogue synchronizer matches registry rows by permission code.

## Decision
`FilePermissions.DOCUMENT_PUBLISH = "FILE:DOCUMENT:PUBLISH"` is contributed explicitly
(`PermissionDef.of(BROWSER, "PUBLISH", "نشر", DOCUMENT_PUBLISH)`), the same mechanism step 06 uses for
the CU and PLATFORM legacy codes, instead of the derived `PERM_FILE_BROWSER_PUBLISH`. The synchronizer
therefore reproduces V12's row (same code, same derived names `مستعرض الملفات - نشر` /
`File Browser - PUBLISH`) rather than adding a second action; V12's explicit grants stay. The action
is effective for a role holding the `FILE_BROWSER` VIEW gateway (`PERM_FILE_BROWSER_VIEW`).

## Consequences
- One FILE authority string departs from the `PERM_*` format; `@PreAuthorize` references it through
  the constant (`T(com.erp.file.permission.FilePermissions).DOCUMENT_PUBLISH`), and the generated
  contract prints the constant name `DOCUMENT_PUBLISH` where other endpoints print the authority.
- Renaming it to `PERM_FILE_BROWSER_PUBLISH` would need a fix-forward migration that re-keys the
  registry row and every grant; not worth a breaking change for one code.
- The frontend grants editor shows the action under `FILE_BROWSER` like any other; its code is simply
  different.

## Traces
Permissions Summary (srs.md) · `FILE:DOCUMENT:PUBLISH` row (`srs.md` → 1.2.0 addendum §4) ·
erp-core/src/main/java/com/erp/file/permission/FilePermissions.java:10-14, 34-40, 57-68 ·
erp-core/src/main/java/com/erp/file/service/FileService.java:313-314 ·
erp-core/src/main/resources/db/migration/core/V12__file_storage.sql L57-69 ·
docs/api-docs/file/endpoints/file-documents.md (PATCH visibility) ·
docs/steps/06-report.md · docs/steps/07-report.md · docs/DEVIATIONS.md [07] (task 5 permission; rebase
onto step 06 — `FilePermissions` add/add)
