# PLATFORM SUMMARY — سلاسل الترقيم / Sequence (SEQUENCE)
══════════════════════════════════════════════════════════════════
Profile : erp   Source version : erp-core 1.2.0 (as built)   Analysis : v1 (as-built baseline)
Written : 2026-10-07, from the code at main @ 19b19a4 (1.3.0-SNAPSHOT; the sequence code is unchanged
          since tag v1.2.0 apart from the helper move of 6b01816, no behaviour change)
══════════════════════════════════════════════════════════════════

Paths cited below are relative to the repository root, except: Java sources are relative to
`erp-core/src/main/java/com/erp/`, and core migrations (`V<N>__*.sql`) to
`erp-core/src/main/resources/db/migration/core/`. `file:line` points at main @ 19b19a4.

This analysis was NOT produced before the code: the sequence module was built by erp-core plan step 09
(`erp-core-plan/09-STEP-sequences-and-settings-api.md`, report `docs/steps/09-report.md`) and annotated for the
audit log when step 10 was rebased onto it. It records what exists, so that every later change to the
module is written as an "Implementation Addendum" on top of it, like the other modules' 1.2.0 addenda.
The full description of the platform as a whole stays in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (L85–146: the module table, the per-module summary blocks and the platform conventions;
the single-copy decision recorded there); this file describes only the sequence module's place in it.

## OVERVIEW
وحدة الترقيم التسلسلي تمنح كل مستأجر سلاسل ترقيم للمستندات (`CORE_NUMBER_SERIES`): لكل سلسلة رمز
ونمط وسياسة إعادة (أبدًا / سنويًا / شهريًا)، ولكل فترة صف خاص بعدّاده. تسحب الوحدات والتطبيقات الرقم
التالي عبر `NumberSeriesApi.next(code)` في معاملة مستقلة تقفل صف السلسلة، فتكون الأرقام متتابعة بلا
تكرار؛ والرقم المسحوب لا يُعاد إن تراجعت معاملة المستدعي (الفجوات مقبولة). يدير موظفو المستأجر
السلاسل عبر `/api/v1/sequence/series`، والمستأجر الجديد يبدأ بنسخة من سلاسل المنصة وعدّاد يساوي 1.
[`erp-core-plan/09-STEP-sequences-and-settings-api.md`; docs/steps/09-report.md Summary]

The sequence module gives every tenant document number series: a series is a code with a pattern and a
reset policy, and every period of it is one row with its own counter. Modules and applications draw
the next number through `NumberSeriesApi.next(code)`, which commits on its own after locking the
series' anchor row, so numbers are consecutive and never duplicated; a number drawn by a transaction
that later rolls back is not reused (gaps are accepted). Tenant staff manage series over
`/api/v1/sequence/series`; a new tenant starts with the platform's series, counters at 1.

## THE MODULE IN THE PLATFORM (as built)
| Aspect | As built | Code location |
|---|---|---|
| Package / surface | `com.erp.sequence`; public surface = `com.erp.sequence.crossmodule` only (`NumberSeriesApi`), enforced by ArchUnit | `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:61-62`; `autoconfigure/ErpCoreAutoConfiguration.java:91` |
| Table | `CORE_NUMBER_SERIES`, tenant-scoped (`TENANT_ID` FK `FK_CORE_NUMBER_SERIES_TENANT`, index `IDX_CORE_NUMBER_SERIES_TENANT`, unique `UQ_CORE_NUMBER_SERIES_CODE_PERIOD (TENANT_ID, CODE, PERIOD_KEY)`), one row per (code, period) | `V14__sequence_and_settings.sql:25-61`; `sequence/entity/NumberSeries.java:40-48` |
| Pattern language | tokens `{PREFIX}` `{YYYY}` `{YY}` `{MM}` `{SEQ:n}` `{TENANT}`, literal text elsewhere; exactly one `{SEQ:n}`, 1 ≤ n ≤ 18; validated at save time | `sequence/domain/NumberPattern.java:24-30`, `:43-108` |
| Reset policy | `NEVER` (period key `''`), `YEARLY` (`YYYY`), `MONTHLY` (`YYYY-MM`); a CHECK-constrained column plus a Java enum, not an MDL lookup | `sequence/domain/ResetPolicy.java:9-25`; `V14__sequence_and_settings.sql:59` |
| Allocation | `NumberSeriesApi.next` → `NumberAllocationService.next` in `REQUIRES_NEW`: `PESSIMISTIC_WRITE` on the anchor row (lowest id of the code), then on the current period's row (created at 1 when the period is new), counter taken, commit | `sequence/service/NumberAllocationService.java:55-78`; `sequence/repository/NumberSeriesRepository.java:31-36` |
| Preview | `NumberSeriesApi.preview` — read-only, no lock, nothing written | `sequence/service/NumberAllocationService.java:80-97` |
| Admin API | `/api/v1/sequence/series`: create, search, get, update (prefix + pattern, every period row of the code), activate, deactivate; no delete, no usage endpoint | `sequence/controller/NumberSeriesController.java:29-75`; `docs/api-docs/sequence/index.md:122-131` |
| Permissions | registry module `SEQUENCE` (الترقيم التسلسلي / Sequences), screen `SEQUENCE_SERIES` (سلاسل الترقيم / Number Series), actions `VIEW` → `PERM_SEQUENCE_SERIES_VIEW` (reads, gateway) and `MANAGE` → `PERM_SEQUENCE_SERIES_MANAGE` (writes); code-contributed, never seeded by a migration; every tenant's super role holds both | `sequence/permission/SequencePermissions.java:20-46`; docs/DEVIATIONS.md [09] (permission format) |
| Tenant | the current tenant (`TenantContext`) is numbered; Hibernate adds `TENANT_ID` to every query, the locking finders included; `{TENANT}` is resolved through `TenantLookupApi.codeOf` | `sequence/service/NumberAllocationService.java:34-36`, `:99-101`; `sequence/repository/NumberSeriesRepository.java:13-15` |
| Provisioning | `SequenceTenantProvisioningContributor` (order 40) copies each code's anchor row from the source tenant (PLATFORM) with `NEXT_VALUE = 1`, explicit JDBC naming `TENANT_ID` | `sequence/tenant/SequenceTenantProvisioningContributor.java:27-44` |
| Audit | `@Audited(entityType = "CORE_NUMBER_SERIES", ignore = {"nextValue"})`: configuration changes are recorded in `CORE_AUDIT_EVENT`, allocations are not | `sequence/entity/NumberSeries.java:41`; docs/DEVIATIONS.md [10] (rebase onto step 09) |
| Errors | `SEQUENCE_NOT_CONFIGURED` 422, `SEQUENCE_PATTERN_INVALID` 400, `NUMBER_SERIES_NOT_FOUND` 404, `NUMBER_SERIES_CODE_DUPLICATE` 409 | `sequence/exception/SequenceErrorCodes.java:14-24`; `erp-core/src/main/resources/i18n/messages.properties:166-169` |
| Configuration | none (`erp.core.*` has no sequence key); an optional application `java.time.Clock` bean decides the period and the date tokens | `sequence/service/NumberAllocationService.java:103-105`; `sequence/service/NumberSeriesService.java:148-150` |
| Consumers in core | none — `NumberSeriesApi` is for applications (seed series in `V1000+`, inject the API) | docs/CONSUMING.md §3 "Number series"; docs/steps/09-report.md "Notes for later steps" |
| OpenAPI | no dedicated springdoc group (the aggregate document lists the endpoints); api-docs folder `docs/api-docs/sequence/` | `autoconfigure/ErpCoreOpenApiAutoConfiguration.java` (no sequence group); `docs/api-docs/README.md` |

## DEPENDENCY MAP
```
SEQUENCE ──HARD (FK TENANT_ID → CORE_TENANT; TenantContext; TenantLookupApi.codeOf, XM-SEQUENCE-001)──▶ TENANT
SEQUENCE ──SPI (TenantProvisioningContributor, order 40, XM-SEQUENCE-002)──▶ TENANT
SEQUENCE ──SPI (PermissionContributor: SequencePermissions)──▶ SEC
SEQUENCE ──@Audited (CORE_NUMBER_SERIES, nextValue ignored)──▶ audit
applications ──crossmodule (NumberSeriesApi.next / preview, XM-SEQUENCE-003)──▶ SEQUENCE   (no core consumer)
SEQUENCE ──foundation──▶ common (AuditableEntity, ServiceResult/Status, LocalizedException, DomainRules, SpecBuilder, PageableBuilder)
```
Build order: created by V14 after the tenant chain (V10) and after SEC's permission SPI (step 06); the
audit annotation was added when step 10 was rebased onto step 09. Tier: core service (L1), beside the
other `CORE_*` modules.

## DEFERRED (not in scope of the as-built module)
| Item | Reason / activation trigger |
|---|---|
| Gap-free (legally contiguous) numbering across rolled-back transactions | out of scope by design: a business module that needs it must number inside its own transaction or reconcile gaps (`sequence/service/NumberAllocationService.java:28-31`; ADR-SEQUENCE-001) |
| Per-branch, per-document-type series as first-class dimensions | not built: applications compose codes themselves (`SALES_INVOICE_BR01`) — `V14__sequence_and_settings.sql:49`; docs/steps/09-report.md "Notes for later steps" |
| Deleting a series; a usage endpoint; editing the counter, the code or the reset policy over HTTP | not built: issued numbers are never reissued, nothing references a series (ADR-SEQUENCE-003; docs/DEVIATIONS.md [09]) |
| A core consumer of `NumberSeriesApi` | none exists; business modules (Phase 4) and applications are the consumers (docs/CONSUMING.md §3) |
| A dedicated OpenAPI group | not built; the aggregate OpenAPI document lists the endpoints |

## OPEN ITEMS
None — this file describes what exists. Behaviour the code does not have is listed under DEFERRED,
never described as present.

## NEXT STEP
Module registry and policies: `module-registry-sequence.md`, `business-policies-sequence.md`.

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
