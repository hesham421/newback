## BUSINESS POLICIES — سلاسل الترقيم / Sequence (SEQUENCE)
══════════════════════════════════════════════════════════════════
Module   : SEQUENCE  Source of truth : the code at main @ 19b19a4 (erp-core 1.2.0 behaviour);
           erp-core-plan/09-STEP-sequences-and-settings-api.md; docs/steps/09-report.md; docs/DEVIATIONS.md [09], [10]
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

As-built baseline: every policy below is CONFIRMED by the code location in its `Source` line, not by a
dialogue. Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

CLIENT-SPECIFIC POLICIES

POL-SEQUENCE-001 — نمط الترقيم بقواعد محددة / A number pattern follows a fixed grammar
  Statement (ar) : يجب على النظام قبول نمط الترقيم فقط إذا تكوّن من نص حرفي والرموز `{PREFIX}` `{YYYY}` `{YY}` `{MM}` `{SEQ:n}` `{TENANT}`، وتضمّن `{SEQ:n}` واحدًا بالضبط حيث 1 ≤ n ≤ 18، ويجب رفض أي رمز مجهول أو قوس غير مغلق.
  Statement (en) : The system shall accept a number pattern only if it consists of literal text and the tokens `{PREFIX}` `{YYYY}` `{YY}` `{MM}` `{SEQ:n}` `{TENANT}` with exactly one `{SEQ:n}` where 1 ≤ n ≤ 18, and shall reject an unknown token or an unmatched brace.
  Pattern   : ubiquitous
  Trigger   : Series creation; pattern update
  Rationale : the pattern is parsed on every allocation; an invalid pattern must never reach the table
  Source    : sequence/domain/NumberPattern.java:24-30, :43-108; sequence/exception/SequenceErrorCodes.java:16-18
  Status    : CONFIRMED (as built)

POL-SEQUENCE-002 — النمط يحمل الفترة التي يُعاد فيها الترقيم / The pattern carries the period it resets in
  Statement (ar) : يجب على النظام رفض نمط لا يُظهر الفترة التي تُعيد فيها السلسلة الترقيم: سياسة `YEARLY` تتطلب `{YYYY}` أو `{YY}`، وسياسة `MONTHLY` تتطلب رمز سنة و`{MM}`، وإلا تكرّرت الأرقام بين الفترات.
  Statement (en) : The system shall reject a pattern that does not render the period in which the series resets: `YEARLY` needs `{YYYY}` or `{YY}`, `MONTHLY` needs a year token and `{MM}`; otherwise numbers would repeat across periods.
  Pattern   : unwanted
  Trigger   : Series creation; pattern update (the reset policy is immutable, so the check is made against it)
  Rationale : a reset restarts the counter at 1; the rendered number must differ from the previous period's
  Source    : sequence/domain/NumberPattern.java:110-125; sequence/domain/NumberSeriesDomain.java:37-43, :52-54
  Status    : CONFIRMED (as built)

POL-SEQUENCE-003 — صف لكل فترة، والتهيئة واحدة عبر الفترات / One row per period, one configuration across periods
  Statement (ar) : يجب على النظام تمثيل السلسلة بصف لكل (رمز، فترة) داخل المستأجر، وأن يحافظ على رمزها وسياسة إعادتها ومفتاح فترتها ثابتة بعد الإنشاء، وأن يطبّق كل تعديل في البادئة أو النمط أو التفعيل على كل صفوف الرمز معًا.
  Statement (en) : The system shall represent a series as one row per (code, period) inside the tenant, shall keep its code, reset policy and period key immutable after creation, and shall apply every change of prefix, pattern or activation to every row of the code together.
  Pattern   : ubiquitous
  Trigger   : Every write of a series
  Rationale : the counter of a period must not be shared with another period; the configuration must not diverge between periods
  Source    : V14__sequence_and_settings.sql:57; sequence/entity/NumberSeries.java:28-38, :61, :76, :81; sequence/service/NumberSeriesService.java:101-141
  Status    : CONFIRMED (as built)

POL-SEQUENCE-004 — السحب ذرّي ومتتابع، والفجوات مقبولة / Allocation is atomic and consecutive; gaps are accepted
  Statement (ar) : يجب على النظام أن يسحب الرقم التالي في معاملة مستقلة تقفل صف السلسلة، بحيث لا يرى سحبان القيمة نفسها وتكون قيم الفترة متتابعة، ويجب ألا يُعاد رقم سُحب لمعاملة تراجعت لاحقًا.
  Statement (en) : The system shall allocate the next number in its own transaction that locks the series row, so that two allocations never see the same value and the values of one period are consecutive, and shall never reuse a number drawn by a transaction that later rolled back.
  Pattern   : ubiquitous
  Trigger   : `NumberSeriesApi.next`
  Rationale : correctness under concurrency without serialising callers' business transactions; strict gap-free numbering is a business-module concern
  Source    : sequence/service/NumberAllocationService.java:23-32, :55-78; sequence/crossmodule/NumberSeriesApi.java:14-19; ADR-SEQUENCE-001
  Status    : CONFIRMED (as built)

POL-SEQUENCE-005 — لا شيء يُنشأ ضمنيًا / Nothing is created implicitly
  Statement (ar) : يجب على النظام رفض سحب أو معاينة رقم من رمز غير مهيأ أو غير مفعّل في المستأجر الحالي، وألا ينشئ سلسلة تلقائيًا.
  Statement (en) : The system shall refuse to draw or preview a number from a code that is not configured, or not active, in the current tenant, and shall never create a series implicitly.
  Pattern   : unwanted
  Trigger   : `NumberSeriesApi.next` / `preview`
  Rationale : a series is tenant reference data that an administrator or a seed script configures deliberately
  Source    : sequence/crossmodule/NumberSeriesApi.java:7-10; sequence/service/NumberAllocationService.java:62-65, :86-89, :107-109; sequence/domain/NumberSeriesDomain.java:56-61
  Status    : CONFIRMED (as built)

POL-SEQUENCE-006 — العدّاد لا يُحرَّر ولا تُحذف السلسلة / The counter is never edited; a series is never deleted
  Statement (ar) : يجب على النظام ألا يسمح بتعديل العدّاد أو الرمز أو سياسة الإعادة عبر الواجهة، وألا يحذف سلسلة؛ إيقاف السلسلة يكون بإلغاء تفعيلها فقط.
  Statement (en) : The system shall not let the counter, the code or the reset policy be edited over the API, and shall not delete a series; a series is taken out of use only by deactivation.
  Pattern   : ubiquitous
  Trigger   : Admin API
  Rationale : issued numbers must never be reissued
  Source    : sequence/dto/NumberSeriesUpdateRequest.java:11-13, :20-30; sequence/mapper/NumberSeriesMapper.java:45-52; sequence/controller/NumberSeriesController.java:24-28; ADR-SEQUENCE-003
  Status    : CONFIRMED (as built)

POL-SEQUENCE-007 — السلاسل خاصة بالمستأجر / Series are tenant-scoped
  Statement (ar) : يجب على النظام نسب كل سلسلة إلى مستأجر واحد، وترقيم المستأجر الحالي فقط، وألا يُظهر سلاسل مستأجر لآخر — ومستأجر المنصة نفسه لا يرى سلاسل غيره.
  Statement (en) : The system shall assign every series to one tenant, number only the current tenant, and never show one tenant's series to another; the PLATFORM tenant itself sees no other tenant's series.
  Pattern   : ubiquitous
  Trigger   : Any read, write or allocation
  Rationale : POL-TENANT-007 applied to this table (`AuditableEntity`, Hibernate `@TenantId`)
  Source    : V14__sequence_and_settings.sql:33, :57-58, :61; sequence/entity/NumberSeries.java:35-36, :48; sequence/repository/NumberSeriesRepository.java:13-15; sequence/service/NumberAllocationService.java:34-36
  Status    : CONFIRMED (as built)

POL-SEQUENCE-008 — المستأجر الجديد يبدأ بسلاسل المنصة من 1 / A new tenant starts with the platform's series at 1
  Statement (ar) : عند إنشاء مستأجر، يجب على النظام نسخ تعريف كل سلسلة من المستأجر المصدر (المنصة) — صف المرساة لكل رمز — مع عدّاد يساوي 1، في معاملة التجهيز نفسها.
  Statement (en) : When a tenant is created, the system shall copy each series definition of the source tenant (PLATFORM) — the anchor row of each code — with the counter at 1, inside the provisioning transaction.
  Pattern   : event
  Trigger   : Tenant provisioning (contributor order 40)
  Rationale : numbering an application seeds for PLATFORM in `V1000+` must also exist in tenants created later; each tenant counts on its own
  Source    : sequence/tenant/SequenceTenantProvisioningContributor.java:10-18, :27-44; docs/CONSUMING.md §3 "Number series"
  Status    : CONFIRMED (as built)

POL-SEQUENCE-009 — إدارة السلاسل لموظفي المستأجر المخوّلين، والسحب داخلي / Series management is for authorised staff; allocation is an in-process call
  Statement (ar) : يجب على النظام قصر قراءة السلاسل على حامل `PERM_SEQUENCE_SERIES_VIEW` وكتابتها على حامل `PERM_SEQUENCE_SERIES_MANAGE`، بينما `NumberSeriesApi` استدعاء داخلي بلا فحص صلاحية يجريه كود مخوَّل أصلًا.
  Statement (en) : The system shall restrict reading series to holders of `PERM_SEQUENCE_SERIES_VIEW` and writing them to holders of `PERM_SEQUENCE_SERIES_MANAGE`, while `NumberSeriesApi` is an in-process call without an authority check, made by already authorised code.
  Pattern   : ubiquitous
  Trigger   : Any `/api/v1/sequence/series` request; any `NumberSeriesApi` call
  Rationale : the admin API is on the staff chain; the allocation runs inside another module's authorised work
  Source    : sequence/permission/SequencePermissions.java:20-46; sequence/service/NumberSeriesService.java:64, :82, :89, :102, :118, :131; sequence/service/NumberAllocationService.java:41-43; docs/DEVIATIONS.md [09] (no `@PreAuthorize` on `NumberSeriesApi`)
  Status    : CONFIRMED (as built)

POL-SEQUENCE-010 — تهيئة السلسلة تُدقَّق، والسحب لا يُدقَّق / Series configuration is audited, allocation is not
  Statement (ar) : يجب على النظام تسجيل تغييرات تهيئة السلسلة (الإنشاء، البادئة، النمط، التفعيل) في سجل التدقيق العام تحت `CORE_NUMBER_SERIES`، وألا يسجّل حركة العدّاد؛ المستند الذي يتلقى الرقم هو ما يُدقَّق.
  Statement (en) : The system shall record series configuration changes (creation, prefix, pattern, activation) in the platform audit log under `CORE_NUMBER_SERIES`, and shall not record counter movements; the document that receives the number is what gets audited.
  Pattern   : ubiquitous
  Trigger   : Any write of `CORE_NUMBER_SERIES`
  Rationale : one audit `UPDATE` row per document number would be noise and would double every allocation's cost
  Source    : sequence/entity/NumberSeries.java:41; docs/DEVIATIONS.md [10] "Rebase onto step 09 — `NumberSeries`"
  Status    : CONFIRMED (as built)

CUSTOM LOOKUP VALUES
| Lookup key | Added values | Source |
|---|---|---|
None — the module's only value set (`CORE_NUMBER_SERIES.RESET_POLICY`: NEVER, YEARLY, MONTHLY) is a CHECK
constraint (`CHK_CORE_NUMBER_SERIES_RESET`, V14__sequence_and_settings.sql:59) mirrored by the Java enum
`sequence/domain/ResetPolicy.java`, not an MDL lookup (same pattern as ADR-SEC-001).

SCOPE EXCEPTIONS
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Gap-free numbering across rolled-back callers | not built; gaps accepted by design (POL-SEQUENCE-004) | a business module that needs it numbers inside its own transaction | ADR-SEQUENCE-001; sequence/service/NumberAllocationService.java:28-31 |
| Delete, usage endpoint, counter / code / policy edits | not built (POL-SEQUENCE-006) | none planned | ADR-SEQUENCE-003; docs/DEVIATIONS.md [09] |
| Branch / document-type dimensions on a series | not built; applications compose codes (`SALES_INVOICE_BR01`) | explicit future request | V14__sequence_and_settings.sql:49; docs/steps/09-report.md "Notes for later steps" |
| Caching of series rows | not built; `CORE_NUMBER_SERIES` is transactional and is not on the caching register | none | sequence/service/NumberSeriesService.java:43-44 |

RESOLVED DECISIONS
| # | Question | Answer | Confirmed by | Sources |
|---|---|---|---|---|
| 1 | How are concurrent allocations serialised? | pessimistic lock on the code's anchor row, in a `REQUIRES_NEW` transaction; gaps accepted | erp-core plan step 09 (fixed decision), as built | ADR-SEQUENCE-001 |
| 2 | One counter per series or one per period? | one row per (code, period); reset policy immutable; the pattern must carry the period | step 09, as built | ADR-SEQUENCE-002 |
| 3 | Can a series be deleted or its counter edited? | no — deactivate only; new tenants restart at 1; the allocation API is ungated | step 09, as built | ADR-SEQUENCE-003 |
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
