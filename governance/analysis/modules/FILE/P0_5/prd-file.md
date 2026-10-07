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
Steps          : 03, 05, 07, 10
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No US ids are minted here.

NEW product capabilities
| Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|
| Publish / withdraw a file | staff with `FILE:DOCUMENT:PUBLISH` | Makes a document PUBLIC (a random 32-character slug; an already public file keeps its slug) or PRIVATE again (slug cleared). Refused when the category does not allow public files. | docs/steps/07-report.md; DEVIATIONS [07] |
| Open a public file without signing in | anyone (e.g. a storefront visitor) | `GET` the stable public URL; cacheable for one day (`Cache-Control`, `ETag` = content hash); redirected to the storage provider's URL when it has one (S3 with a public base URL). | DEVIATIONS [07] |
| Mark a category as public-capable | staff managing categories | Category flag `allowPublic`. | V12__file_storage.sql |
| Choose where files are stored | platform operator | Configuration selects DB, local disk or S3 for new uploads. | docs/steps/07-report.md |

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-FILE-001 upload and store | Content goes to the configured storage provider; a SHA-256 hash is recorded; metadata shows `storageProvider`, `visibility`, `publicUrl`. Files are visible only inside the uploader's tenant. | DEVIATIONS [07] (`publicUrl` entry); docs/steps/05-report.md |
| US-FILE-002 time-limited secure link | Unchanged for private files (single-use token). | DEVIATIONS [07] |
| US-FILE-003 categories with types and limits | + `allowPublic`; category code unique per tenant. | V10, V12 |

Scope exclusion "External filesystem storage — bytes in DB (BYTEA)" no longer holds: LOCAL and S3
providers exist; DB remains the default.
