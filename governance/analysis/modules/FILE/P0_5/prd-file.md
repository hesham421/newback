# PRD — File Service (FILE)
══════════════════════════════════════════════════════════════════
Module          : File Service (FILE prefix)
Source artifacts: platform-summary.md, module-registry-FILE.md,
                  business-policies-FILE.md
Status          : DRAFT — awaiting Reconciliation Gate (Project 2.5)
Open Questions  : None — see OQ Log
══════════════════════════════════════════════════════════════════

## USER STORIES

US-FILE-001
  Story    : كمستخدم، أحتاج رفع ملف وتخزينه بأمان، ليُحفظ لاسترجاعه لاحقاً.
  Priority : —
  Success metric : —
  Source   : module-registry-FILE.md §ENTITIES OWNED (FileDocument);
             §SCOPE NOTE (bytes في PostgreSQL BYTEA)
  Status   : DRAFT

US-FILE-002
  Story    : كمستخدم، أحتاج الوصول إلى ملفي عبر رابط آمن محدود المدة
             (ينتهي بعد فترة)، ليبقى الوصول تحت التحكّم.
  Priority : —
  Success metric : —
  Source   : module-registry-FILE.md §SCOPE NOTE (AES/GCM encrypted URL token,
             TTL ~100 min); business-policies-FILE.md §POLICY-CLI-03
  Status   : DRAFT

US-FILE-003
  Story    : كأدمن/موديول مستهلِك، أحتاج تعريف فئات المستندات مع أنواعها
             وحدود حجمها، لتُتحقَّق عمليات الرفع حسب كل فئة.
  Priority : —
  Success metric : —
  Source   : module-registry-FILE.md §ENTITIES OWNED (FileCategory);
             business-policies-FILE.md §POLICY-CLI-01 / §POLICY-CLI-02
  Status   : DRAFT

US-FILE-004
  Story    : كموديول مستهلِك، أحتاج إرفاق ملفات بسجلاتي أياً كان الموديول
             (ملكية عامة owner_id/owner_type/module_code)، ليكون تخزين
             الملفات قابلاً لإعادة الاستخدام عبر المنصة.
  Priority : —
  Success metric : —
  Source   : module-registry-FILE.md §SCOPE NOTE (generic ownership,
             provider pattern)
  Status   : DRAFT

US-FILE-005
  Story    : كمستخدم، أحتاج أرشفة أو إزالة الملفات التي لم أعد أحتاجها،
             لإدارة دورة حياة ملفاتي.
  Priority : —
  Success metric : —
  Source   : module-registry-FILE.md §LOVs OWNED (FileStatus:
             ACTIVE/ARCHIVED/DELETED)
  Status   : DRAFT

## STORIES EXCLUDED (justified)

  — FileType / FileStatus (LOVs) — قوائم قيم داعمة للقصص أعلاه، لا قصص
    مستقلة.

## SCOPE EXCLUSIONS (خارج النطاق — من P0)

  — PDF processing/preview (PDFBox) — business-policies-FILE §SCOPE EXCEPTIONS.
  — Async file pipeline / message broker (RabbitMQ) — كل العمليات متزامنة.
  — External filesystem storage — البايتات في DB (BYTEA).

## OPEN ITEMS (ambiguous, not yet a story)

  ? دلالة الحذف (حذف دائم مقابل soft-delete عبر status=DELETED) — قرار
    مؤجّل لـ P1 (business-policies-FILE §SCOPE EXCEPTIONS)؛ الـ LOV
    يدعم الحالتين. لا يفصّل في PRD.

══════════════════════════════════════════════════════════════════
*End of prd-FILE.md*
*Next stage: Project 2.5 (UI/UX Design Engine) — requires this file
 AND srs.md together (CONTRACT-11). Does not gate Project 1.*
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 03, 05, 06, 07, 08, 10
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository (`com/erp/…` = `erp-core/src/main/java/com/erp/…`).
No US ids are minted here.

Product capabilities — القدرات
| Kind | Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|---|
| NEW | Publish / withdraw a file | staff with `FILE:DOCUMENT:PUBLISH` | Makes a document PUBLIC (a random 32-character slug; an already public file keeps its slug) or PRIVATE again (slug cleared). Refused when the category does not allow public files; refused (404) for a deleted file. An archived file can be published but is not served until it is active, which the lifecycle does not allow. | docs/steps/07-report.md; DEVIATIONS [07]; com/erp/file/service/FileService.java:313-346 |
| NEW | Open a public file without signing in | anyone (e.g. a storefront visitor) | `GET` the stable public URL; cacheable for one day (`Cache-Control`, `ETag` = content hash); redirected to the storage provider's URL when it has one (S3 with a public base URL). Only raster images and PDF open inline; anything else downloads. | DEVIATIONS [07]; ADR-FILE-002 |
| NEW | Mark a category as public-capable | staff managing categories | Category flag `allowPublic` (create and update). | V12__file_storage.sql |
| NEW | Choose where files are stored | platform operator | Configuration selects DB, local disk or S3 for new uploads; existing files stay readable where they are. | docs/steps/07-report.md; ADR-FILE-001 |

Existing stories — القصص الحالية
| Kind | Story | Delta | Source |
|---|---|---|---|
| CHANGED | US-FILE-001 upload and store | Upload needs `PERM_FILE_BROWSER_CREATE` (the SRS left upload to the owning module's screen). Content goes to the configured storage provider; a SHA-256 hash is recorded; metadata shows `storageProvider`, `visibility`, `publicUrl`. Size: the category limit replaces the default; type: only a category allow-list restricts, matched on the sniffed content (PDF / Office / ZIP cannot be sniffed by the JDK — an allow-list naming them rejects them, a known limitation). Files are visible only inside the uploader's tenant. | DEVIATIONS [07] (`publicUrl` entry); docs/steps/05-report.md; FileService.java:108-163 |
| CHANGED | US-FILE-002 time-limited secure link | The link expires after 10 minutes (the story said ~100), can be used once, and only by the user who requested it; a deleted file yields no link. | FileService.java:166-230; com/erp/file/domain/FileAccessTokenDomainService.java:28; ADR-FILE-003 |
| CHANGED | US-FILE-003 categories with types and limits | + `allowPublic`; category code unique per tenant, stored upper-cased; the list is a POST search; a category can be deactivated but not reactivated, and its code cannot be changed. | V10; V12; com/erp/file/service/FileCategoryService.java; ADR-FILE-006 |
| CHANGED | US-FILE-004 attach files to any module's records | A consuming module uploads over REST with the ownership triple and keeps the id as a soft reference; it validates that reference in-process through `FileDocumentLookupApi.isAvailable` (NOTIF does so for template attachments). Ownership filters the list but does not restrict who may read. | com/erp/file/crossmodule/FileDocumentLookupApi.java; FileService.java:52-55 |
| CHANGED | US-FILE-005 archive or remove files | One endpoint, `DELETE /api/v1/files/{id}?action=ARCHIVE` (default) or `DELETE`, needing the browser UPDATE permission to archive and the DELETE permission to remove; archive → delete is allowed, un-archive is not, a removed file is final. Removal is a soft delete: the file disappears from downloads, links and public URLs but stays listed with its status; its bytes are kept. | FileService.java:280-304; com/erp/file/domain/FileDocumentDomain.java:27-31 |
| REMOVED | SCOPE EXCLUSIONS "External filesystem storage — bytes in DB (BYTEA)" | LOCAL and S3 providers exist; DB remains the default. | docs/steps/07-report.md |
| CHANGED | OPEN ITEMS "delete semantics" | Resolved as soft delete (RULE-FILE-006) with the visibility rules above. | FileService.java:177-180, 216-219 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.4 — shared image store
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

NEW product capabilities
| Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|
| Profile photo stored as a public image | any staff user (SEC "my profile"), or an administrator for another user | PNG / JPEG / WebP, at most 1 MB, detected from the content; a PUBLIC file at a non-guessable URL; replacing or removing it withdraws the previous one. | srs.md 1.3.0 (RULE-FILE-008..010); ADR-FILE-008 |
| Tenant logo stored as a public image (package E) | platform administrator | as above, plus SVG without active content; the logo belongs to the tenant it depicts. | srs.md 1.3.0 §5 (planned consumer) |

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-FILE-004 attach files to any module's records | A core module that needs a public image (photo, logo) uses the in-process `FileImageStoreApi` instead of the REST upload; such images carry no category. | XM-FILE-002; ADR-FILE-008 |
