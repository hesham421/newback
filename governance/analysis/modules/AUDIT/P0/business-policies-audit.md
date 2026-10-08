## BUSINESS POLICIES — سجل التدقيق العام / Audit (AUDIT)
══════════════════════════════════════════════════════════════════
Module   : AUDIT  Source of truth : the code at main @ 19b19a4 (erp-core 1.2.0 behaviour);
           erp-core-plan/10-STEP-generic-audit-log.md; docs/steps/10-report.md; docs/DEVIATIONS.md [10], [11]
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

As-built baseline: every policy below is CONFIRMED by the code location in its `Source` line, not by a
dialogue. Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

CLIENT-SPECIFIC POLICIES

POL-AUDIT-001 — سجل واحد لكل مستأجر / One tenant-scoped timeline
  Statement (ar) : يجب على النظام نسب كل صف تدقيق إلى مستأجر واحد بالضبط، وألا يُظهره إلا داخل ذلك المستأجر؛ صف الكيان الخاص بمستأجر يحمل مستأجر الكيان، وصف الكيان العام (مثل `CORE_TENANT`) أو الإدخال الصريح يحمل المستأجر الحالي.
  Statement (en) : The system shall assign every audit row to exactly one tenant and show it only inside that tenant; a row about a tenant-scoped entity carries the entity's tenant, a row about a global entity (such as `CORE_TENANT`) or an explicit entry carries the current tenant.
  Pattern   : ubiquitous
  Trigger   : Any write or read of `CORE_AUDIT_EVENT`
  Rationale : POL-TENANT-007 applied to the log; one timeline per organisation
  Source    : V15__audit_schema.sql:18, :40, :44; audit/entity/AuditEvent.java:20-21, :38; audit/listener/AuditedEntityListener.java:44-46, :129; audit/service/AuditRecordingService.java:65
  Status    : CONFIRMED (as built)

POL-AUDIT-002 — لا سجل إلا لتغيير ملتزم / An audit row exists only for a committed change
  Statement (ar) : يجب على النظام كتابة صف التدقيق متزامنًا داخل معاملة الكاتب وعلى اتصاله نفسه، بحيث يزول الصف مع تراجع التغيير ولا يُفقد أو يتكرر بإعادة المحاولة.
  Statement (en) : The system shall write the audit row synchronously inside the writer's transaction and on its connection, so that the row disappears with a rolled-back change and is never lost or duplicated by a retry.
  Pattern   : ubiquitous
  Trigger   : `AuditApi.record`; every insert / update / delete of an `@Audited` entity
  Rationale : the log must describe what actually happened, nothing more and nothing less
  Source    : audit/crossmodule/AuditApi.java:10-13; audit/service/AuditEventStore.java:23-32, :54-59, :62-90; audit/listener/AuditedEntityListener.java:39-42, :138; ADR-AUDIT-001
  Status    : CONFIRMED (as built)

POL-AUDIT-003 — الأسرار لا تُكتب أبدًا / Secrets are never recorded
  Statement (ar) : يجب على النظام ألا يكتب في `CHANGES` أي حقل يحتوي اسمه على `password` أو `secret` أو `token` أو `hash` أو `credential` أو `apikey` أو `privatekey` أو `salt` (دون تمييز حالة الأحرف)، أيًا كان من قدّمه، ولا الحقول التقنية، ولا الحقول التي يستثنيها الكيان بـ `@Audited(ignore)`.
  Statement (en) : The system shall never write to `CHANGES` a field whose name contains `password`, `secret`, `token`, `hash`, `credential`, `apikey`, `privatekey` or `salt` (case-insensitive), whoever supplies it, nor the technical fields, nor the fields an entity excludes with `@Audited(ignore)`.
  Pattern   : unwanted
  Trigger   : Every recorded change
  Rationale : secrets discipline (POL-SEC-004; tenant-maturity plan §1 rule 7)
  Source    : audit/crossmodule/AuditApi.java:32-38; audit/domain/AuditEventDomain.java:28-29, :50-73; audit/crossmodule/Audited.java:16-25; ADR-AUDIT-002
  Status    : CONFIRMED (as built)

POL-AUDIT-004 — السجل للإلحاق فقط / The log is append-only
  Statement (ar) : يجب على النظام ألا يعدّل أو يحذف صف تدقيق عبر الواجهة أو عبر JPA، وألا يعرض أي عملية كتابة عبر HTTP؛ الحذف الوحيد هو الاحتفاظ الذي يقرره التطبيق.
  Statement (en) : The system shall never update or delete an audit row through the API or through JPA, and shall expose no write operation over HTTP; the only deletion is the retention the application decides.
  Pattern   : ubiquitous
  Trigger   : Any access to `CORE_AUDIT_EVENT`
  Rationale : an audit trail that can be edited is not an audit trail
  Source    : audit/entity/AuditEvent.java:23-27, :30 (`@Immutable`, no setters); audit/repository/AuditEventRepository.java:9-10; audit/controller/AuditEventController.java:18-21; audit/service/AuditRetentionJob.java:36-46
  Status    : CONFIRMED (as built)

POL-AUDIT-005 — الإجراء حرّ لكن منضبط الصيغة / Actions are free but well-formed
  Statement (ar) : يجب على النظام قبول أي إجراء يطابق `^[A-Z_]{3,64}$` ورفض ما عداه؛ الإجراءات القياسية `CREATE` `UPDATE` `DELETE` `STATUS_CHANGE` `LOGIN` `LOGOUT` `PASSWORD_RESET` ثوابت على `AuditApi`.
  Statement (en) : The system shall accept any action matching `^[A-Z_]{3,64}$` and refuse any other; the standard actions `CREATE`, `UPDATE`, `DELETE`, `STATUS_CHANGE`, `LOGIN`, `LOGOUT`, `PASSWORD_RESET` are constants on `AuditApi`.
  Pattern   : ubiquitous
  Trigger   : `AuditApi.record`; the listener (standard actions only)
  Rationale : callers name their own facts without a core change; the shape stays filterable
  Source    : audit/domain/AuditEventDomain.java:21-22, :37-44; V15__audit_schema.sql:41, :49; audit/crossmodule/AuditApi.java:20-26
  Status    : CONFIRMED (as built)

POL-AUDIT-006 — كل صف يقول من ومتى ومن أين / Every row says who, when and from where
  Statement (ar) : يجب على النظام أن يملأ لكل صف المنفذ (اسم المستخدم أو `system`)، ونطاقه (`STAFF` / `CUSTOMER` / `SYSTEM`)، ووقت الحدوث، وعنوان IP ووكيل المستخدم عندما يكون هناك طلب HTTP، ومعرّف المستخدم عندما يمرّره المستدعي.
  Statement (en) : The system shall fill, for every row, the actor (username or `system`), its realm (`STAFF` / `CUSTOMER` / `SYSTEM`), the time of occurrence, the IP and User-Agent when an HTTP request is current, and the actor's user id when the caller passes it.
  Pattern   : ubiquitous
  Trigger   : Every recorded row
  Rationale : "who changed what, when" is the module's purpose
  Source    : audit/service/AuditRecordingService.java:61-80; audit/web/RequestInfoHolder.java:46-61; common/util/SecurityContextHelper.java:64, :74-81; docs/DEVIATIONS.md [10] (`ACTOR_USER_ID` default)
  Status    : CONFIRMED (as built)

POL-AUDIT-007 — قراءة السجل امتياز للموظفين / Reading the log is a staff privilege
  Statement (ar) : يجب على النظام قصر قراءة السجل على موظف مصادَق يحمل `AUDIT:EVENT:READ`، وألا يقرأه العملاء أبدًا؛ تقرير القائمة يتطلب صلاحيته المستقلة `AUDIT:REPORT:AUDIT_EVENT_LIST`.
  Statement (en) : The system shall restrict reading the log to an authenticated staff member holding `AUDIT:EVENT:READ`, and customers shall never read it; the list report requires its own independent authority `AUDIT:REPORT:AUDIT_EVENT_LIST`.
  Pattern   : ubiquitous
  Trigger   : `GET /api/v1/audit/events`; report `AUDIT_EVENT_LIST`
  Rationale : the log holds field values of every audited entity of the tenant
  Source    : audit/service/AuditEventService.java:56; audit/controller/AuditEventController.java:20-21; audit/permission/AuditPermissions.java:23-24, :41; audit/report/AuditEventListReport.java:42-44, :104
  Status    : CONFIRMED (as built)

POL-AUDIT-008 — الاحتفاظ قرار التطبيق / Retention is the application's decision
  Statement (ar) : يجب على النظام الاحتفاظ بكل صف إلى الأبد ما لم يضبط التطبيق `erp.core.audit.retention-days` ويشغّل `AuditRetentionJob.run()` بنفسه أو يفعّل الجدولة مع `erp.core.audit.retention-cron`؛ النواة لا تجدول شيئًا.
  Statement (en) : The system shall keep every row forever unless the application sets `erp.core.audit.retention-days` and either calls `AuditRetentionJob.run()` itself or enables scheduling with `erp.core.audit.retention-cron`; core schedules nothing.
  Pattern   : optional
  Trigger   : Application configuration
  Rationale : retention periods are a legal and business choice per deployment
  Source    : audit/service/AuditRetentionJob.java:11-20, :30-46; autoconfigure/ErpCoreProperties.java:361-374; docs/CONSUMING.md §2 "Scheduling"; ADR-AUDIT-003
  Status    : CONFIRMED (as built)

POL-AUDIT-009 — التدقيق على مستوى الحقول اختياري، والحقائق الصريحة عبر الواجهة / Field-level auditing is opt-in; explicit facts go through the API
  Statement (ar) : يجب على النظام تسجيل تاريخ الحقول فقط للكيانات المعلَّمة بـ `@Audited` (صف `CREATE` / `UPDATE` / `DELETE` لكل تغيير، والـ `UPDATE` بالحقول المختلفة فقط)، وأن يترك الحقائق الأخرى (اعتماد، دخول، تصدير…) للوحدة لتسجّلها عبر `AuditApi.record` داخل معاملتها.
  Statement (en) : The system shall record field history only for entities annotated with `@Audited` (one `CREATE` / `UPDATE` / `DELETE` row per change, an `UPDATE` with the differing fields only), and shall leave other facts (approvals, logins, exports, …) to the owning module to record through `AuditApi.record` inside its transaction.
  Pattern   : ubiquitous
  Trigger   : Entity annotation; module code
  Rationale : auditing every entity by default would flood the log; the owning module knows its facts
  Source    : audit/crossmodule/Audited.java:9-30; audit/listener/AuditedEntityListener.java:33-37, :63-118; audit/crossmodule/AuditApi.java:15-16, :40-47; docs/CONSUMING.md §9
  Status    : CONFIRMED (as built)

POL-AUDIT-010 — سجل الأمان يبقى، وSEC تكتب في السجلين / The security log stays; SEC writes to both
  Statement (ar) : يجب على النظام إبقاء `SEC_AUDIT_LOG` كما هو، وأن تسجّل SEC إضافةً إلى ذلك `LOGIN` و`LOGOUT` و`PASSWORD_RESET` في السجل العام بالمنفذ = الحساب نفسه، بينما تبقى محاولات الدخول الفاشلة وطلبات إعادة التعيين في `SEC_AUDIT_LOG` فقط.
  Statement (en) : The system shall keep `SEC_AUDIT_LOG` unchanged, and SEC shall additionally record `LOGIN`, `LOGOUT` and `PASSWORD_RESET` in the generic log with the account itself as actor, while failed logins and reset requests stay in `SEC_AUDIT_LOG` only.
  Pattern   : ubiquitous
  Trigger   : SEC account events
  Rationale : one timeline per user without rewriting the security-specific log (REQ-SEC-024)
  Source    : V15__audit_schema.sql:7-8; sec/service/SecAuditEntries.java:6-11, :22-34; sec/service/AuthService.java:117, :166; sec/service/CustomerAccountService.java:206, :266; sec/service/PasswordResetService.java:146; ADR-AUDIT-003
  Status    : CONFIRMED (as built)

POL-AUDIT-011 — القيم محدودة الطول / Values are bounded
  Statement (ar) : يجب على النظام قصّ كل قيمة تغيير نصية عند 2000 حرف مع علامة `…`، وقصّ المنفذ عند 100 ونوع الكيان عند 128 ومعرّفه عند 64 والملخصين عند 1000 والمرجع عند 100 وعنوان IP عند 64 ووكيل المستخدم عند 256.
  Statement (en) : The system shall cut every textual change value at 2000 characters with a trailing `…`, and cut the actor at 100, the entity type at 128, the entity id at 64, both summaries at 1000, the reference at 100, the IP at 64 and the User-Agent at 256.
  Pattern   : ubiquitous
  Trigger   : Every recorded row
  Rationale : the columns have fixed widths; a caller must never fail its business transaction because of an audit value
  Source    : audit/service/AuditRecordingService.java:35-42, :67-79, :90-99; audit/web/RequestInfoHolder.java:31-33, :50-51; V15__audit_schema.sql:20-31
  Status    : CONFIRMED (as built)

CUSTOM LOOKUP VALUES
| Lookup key | Added values | Source |
|---|---|---|
None — the module's only value set (`CORE_AUDIT_EVENT.ACTOR_REALM`: STAFF, CUSTOMER, SYSTEM) is a CHECK
constraint (`CHK_CORE_AUDIT_EVENT_REALM`, V15__audit_schema.sql:42) whose values are the
`SecurityContextHelper.REALM_*` constants, not an MDL lookup; `ACTION` is a free, regex-checked code.

SCOPE EXCEPTIONS
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Asynchronous writer, `DomainEvent` listener | not built by design (POL-AUDIT-002) | none — an application may mirror events itself | docs/DEVIATIONS.md [10] "Step 08 events for async write" |
| Write / edit / delete endpoints | not built (POL-AUDIT-004) | none | docs/DEVIATIONS.md [10] (query API shape) |
| Default `ACTOR_USER_ID` from the principal | not built (module cycle with SEC) | none — callers pass it | docs/DEVIATIONS.md [10] (`ACTOR_USER_ID` default) |
| Core-scheduled retention, `X-Forwarded-For` trust | not built (POL-AUDIT-008; `RequestInfoHolder` Javadoc) | application configuration | audit/service/AuditRetentionJob.java:15-18; audit/web/RequestInfoHolder.java:22-25 |
| New action codes of the tenant-maturity plan | callers' codes, no audit change | packages B–E (1.3.0) | `docs/plans/tenant-maturity-analysis-reference.md` (each package records its code in `../P1/srs-audit.md` → "Implementation Addendum — erp-core 1.3.0" when it lands) |

RESOLVED DECISIONS
| # | Question | Answer | Confirmed by | Sources |
|---|---|---|---|---|
| 1 | How is the row written? | synchronous JDBC on the caller's connection, also from inside the Hibernate flush | erp-core plan step 10 (fixed decision), as built | ADR-AUDIT-001 |
| 2 | What is redacted? | name-substring denylist + technical fields + `@Audited(ignore)`; collections and binary skipped | step 10, as built | ADR-AUDIT-002 |
| 3 | `SEC_AUDIT_LOG`, retention, timestamp type | coexistence with dual write; opt-in app-scheduled retention; `OCCURRED_AT` TIMESTAMP without zone | step 10, as built | ADR-AUDIT-003 |
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 10
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
