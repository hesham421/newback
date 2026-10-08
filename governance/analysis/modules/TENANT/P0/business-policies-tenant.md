## BUSINESS POLICIES — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Module   : TENANT  Source of truth : the code at main @ 2274f86 (erp-core 1.2.0 behaviour);
           erp-core-plan/05-STEP-multi-tenancy.md; docs/steps/05-report.md; docs/DEVIATIONS.md [05], [07], [09], [15]
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

As-built baseline: every policy below is CONFIRMED by the code location in its `Source` line, not by a
dialogue. Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

CLIENT-SPECIFIC POLICIES

POL-TENANT-001 — رمز المستأجر ثابت وبصيغة محددة / Tenant code is immutable and well-formed
  Statement (ar) : يجب على النظام قبول رمز المستأجر فقط إذا طابق `^[A-Z0-9_]{3,32}$` كما أُدخل، وكان غير مستخدم، وألا يغيّر الرمز بعد الإنشاء أبدًا.
  Statement (en) : The system shall accept a tenant code only if it matches `^[A-Z0-9_]{3,32}$` as entered and is not taken, and shall never change a tenant's code after creation.
  Pattern   : ubiquitous
  Trigger   : Tenant creation; any later write of the tenant
  Rationale : the code is the natural key clients send as `X-Tenant-Code` and embed in public URLs; changing it would break every client and link
  Source    : tenant/domain/TenantDomain.java:18, :34-40; tenant/entity/Tenant.java:50; V10__tenant_schema.sql:49, :51
  Status    : CONFIRMED (as built)

POL-TENANT-002 — حالتان فقط، والمعلّق لا يخدم / Two statuses; a suspended tenant is not served
  Statement (ar) : يجب على النظام أن يحمل كل مستأجر إحدى حالتين فقط: ACTIVE أو SUSPENDED، وأثناء تعليق المستأجر يجب على النظام رفض تسجيل الدخول وكل طلب لذلك المستأجر، بما فيه الرموز الصادرة قبل التعليق.
  Statement (en) : The system shall hold every tenant in exactly one of two statuses, ACTIVE or SUSPENDED; while a tenant is suspended, the system shall refuse its logins and every request made in it, including requests carrying tokens issued before the suspension.
  Pattern   : state
  Trigger   : Any request resolved to that tenant
  Rationale : suspension must take effect at once, without waiting for tokens to expire
  Source    : V10__tenant_schema.sql:50; tenant/TenantConstants.java:23, :26; tenant/security/TenantResolutionFilter.java:89-99, :109-112, :139-142
  Status    : CONFIRMED (as built)

POL-TENANT-003 — مستأجر المنصة لا يُعلَّق / The PLATFORM tenant is never suspended
  Statement (ar) : يجب على النظام ألا يعلّق مستأجر المنصة (المعرّف 1) أبدًا.
  Statement (en) : The system shall never suspend the PLATFORM tenant (id 1).
  Pattern   : unwanted
  Trigger   : Status change request
  Rationale : PLATFORM hosts the platform operators; suspending it would lock everyone out of tenant management
  Source    : tenant/domain/TenantDomain.java:53-59; tenant/TenantConstants.java:11
  Status    : CONFIRMED (as built)

POL-TENANT-004 — التجهيز ذرّي / Provisioning is atomic
  Statement (ar) : يجب على النظام إنشاء المستأجر وتجهيز بيانات كل وحدة له في معاملة واحدة، بحيث يكون المستأجر مجهزًا بالكامل أو غير موجود.
  Statement (en) : The system shall create a tenant and every module's set-up of it in one transaction, so that a tenant is either fully provisioned or does not exist.
  Pattern   : ubiquitous
  Trigger   : Tenant creation
  Rationale : a half-provisioned tenant (no administrator, no roles, no lookups) cannot be used or repaired from inside
  Source    : tenant/service/TenantService.java:68-103; tenant/TenantProvisioningContributor.java:11-13
  Status    : CONFIRMED (as built)

POL-TENANT-005 — لا حذف للمستأجر / A tenant is never deleted
  Statement (ar) : يجب على النظام ألا يحذف مستأجرًا؛ الإيقاف يتم بالتعليق فقط.
  Statement (en) : The system shall not delete a tenant; a tenant is taken out of service only by suspension.
  Pattern   : ubiquitous
  Trigger   : Tenant lifecycle
  Rationale : every tenant-scoped row references the tenant (FK, 22 tables); deleting it would orphan or destroy that tenant's data
  Source    : tenant/controller/PlatformTenantController.java:28-30 (no DELETE mapping); V10__tenant_schema.sql:102-119 (FKs without cascade); docs/DEVIATIONS.md [05] (controller shape)
  Status    : CONFIRMED (as built)

POL-TENANT-006 — إدارة المستأجرين من المنصة فقط / Tenant management is platform-only
  Statement (ar) : يجب على النظام قصر إدارة المستأجرين على مستخدم مصادَق من مستأجر المنصة يحمل صلاحية `PLATFORM_TENANT_MANAGE`، ويجب ألا تخرج هذه الصلاحية من مستأجر المنصة.
  Statement (en) : The system shall restrict tenant management to an authenticated caller of the PLATFORM tenant holding `PLATFORM_TENANT_MANAGE`, and that permission shall never become effective outside the PLATFORM tenant.
  Pattern   : ubiquitous
  Trigger   : Any `/api/v1/platform/tenants` request; tenant provisioning; super-role authority resolution
  Rationale : a tenant administrator must never create, read or suspend other tenants
  Source    : autoconfigure/ErpCoreSecurityAutoConfiguration.java:130-131, :199-205; tenant/service/TenantService.java:69, :106, :117, :135, :142; sec/tenant/SecTenantProvisioningContributor.java:84-121; sec/service/MenuService.java:142-143
  Status    : CONFIRMED (as built)

POL-TENANT-007 — عزل البيانات على مستوى الصف / Row-level data isolation
  Statement (ar) : يجب على النظام أن ينسب كل صف خاص بمستأجر إلى مستأجر واحد فقط، وألا يُظهره أو يعدّله إلا داخل ذلك المستأجر — ومستأجر المنصة نفسه لا يرى صفوف غيره.
  Statement (en) : The system shall assign every tenant-scoped row to exactly one tenant and shall read or change it only inside that tenant; the PLATFORM tenant itself sees no other tenant's rows.
  Pattern   : ubiquitous
  Trigger   : Any data access
  Rationale : one shared schema serves many tenants; isolation must hold by construction, not by each query remembering it
  Source    : common/domain/AuditableEntity.java:35-37; tenant/config/TenantIdentifierResolver.java:24-25, :36-42; V10__tenant_schema.sql:63-138
  Status    : CONFIRMED (as built)

POL-TENANT-008 — كل طلب يعرف مستأجره / Every request names its tenant
  Statement (ar) : يجب على النظام تحديد مستأجر كل طلب من المسار أو من رمز الدخول أو من الترويسة `X-Tenant-Code`، وإذا وصل طلب عام إلى مسار غير مستثنى دون مستأجر فيجب على النظام رفضه.
  Statement (en) : The system shall determine every request's tenant from its path, its access token or its `X-Tenant-Code` header; if a public request reaches a non-exempt path without a tenant, then the system shall refuse it.
  Pattern   : unwanted
  Trigger   : Any web request
  Rationale : usernames, codes and every row are unique per tenant only; a request without a tenant cannot be answered correctly
  Source    : tenant/security/TenantResolutionFilter.java:81-128; autoconfigure/ErpCoreProperties.java:233-261
  Status    : CONFIRMED (as built)

POL-TENANT-009 — الرمز لا يعمل في مستأجر آخر / A token never acts in another tenant
  Statement (ar) : يجب على النظام ألا يسمح لرمز دخول صادر لمستأجر بالعمل داخل مستأجر آخر، سواء بترويسة `X-Tenant-Code` أو بمسار يحمل رمز مستأجر آخر.
  Statement (en) : The system shall never let an access token issued in one tenant act in another tenant, neither through an `X-Tenant-Code` header nor through a path that names another tenant.
  Pattern   : ubiquitous
  Trigger   : Authenticated request
  Rationale : cross-tenant access must be impossible by construction
  Source    : tenant/security/TenantResolutionFilter.java:89-99 (header not read when a token tenant is set), :143-147 (foreign authentication dropped on a path-tenant path)
  Status    : CONFIRMED (as built)

POL-TENANT-010 — المستأجر الجديد يبدأ من كتالوج المنصة / A new tenant starts from the platform catalog
  Statement (ar) : عند إنشاء مستأجر، يجب على النظام منحه نسخة من بيانات المنصة المرجعية (أدوار الكتالوج الأربعة SYS_ADMIN وCU_ADMIN وNOTIF_ADMIN وFILE_ADMIN فقط ومنحها عدا وحدة PLATFORM — الأدوار الأخرى التي ينشئها المشغّل في المنصة لا تُنسخ، القوائم المرجعية، إعدادات القنوات بلا بيانات اعتماد، القوالب بلا مرفقات، سلاسل الترقيم بعدّاد يبدأ من 1) ومديرًا أوّل نشطًا يحمل الدور `SYS_ADMIN`.
  Statement (en) : When a tenant is created, the system shall give it a copy of the platform's reference data (only the four catalog roles `SYS_ADMIN`, `CU_ADMIN`, `NOTIF_ADMIN`, `FILE_ADMIN` and their grants except the PLATFORM module — other roles an operator created in PLATFORM are not copied; the lookup catalog, channel configurations without credentials, templates without attachments, number series with the counter at 1) and an ACTIVE first administrator holding `SYS_ADMIN`.
  Pattern   : event
  Trigger   : Tenant creation
  Rationale : without it the new tenant would have no role, no lookup or template and nobody able to log in
  Source    : sec/tenant/SecTenantProvisioningContributor.java:40 (`CATALOG_ROLE_CODES`), :80 (`r.CODE IN (...)`), :66-148; mdl/tenant/MdlTenantProvisioningContributor.java:31-55; notif/tenant/NotifTenantProvisioningContributor.java:32-53; sequence/tenant/SequenceTenantProvisioningContributor.java:32-44
  Status    : CONFIRMED (as built)

POL-TENANT-011 — كلمة مرور المدير الأول لا تُخزَّن ولا تُسجَّل صريحة / The first administrator's password is never kept in clear
  Statement (ar) : يجب على النظام ألا يخزّن كلمة مرور المدير الأول أو يسجّلها أو يعيدها بصيغتها الصريحة؛ تُخزَّن مجزّأة فقط.
  Statement (en) : The system shall never store, log or return the first administrator's password in clear; only its hash is stored.
  Pattern   : ubiquitous
  Trigger   : Tenant creation
  Rationale : secrets discipline (tenant-maturity plan §1 rule 7; POL-SEC-004)
  Source    : tenant/dto/TenantCreateRequest.java:65-69; tenant/TenantProvisioning.java:24-27; sec/tenant/SecTenantProvisioningContributor.java:128
  Status    : CONFIRMED (as built)

CUSTOM LOOKUP VALUES
| Lookup key | Added values | Source |
|---|---|---|
None — the tenant's only value set (`CORE_TENANT.STATUS_CODE`: ACTIVE, SUSPENDED) is a CHECK constraint
(`CHK_CORE_TENANT_STATUS`, V10__tenant_schema.sql:50), not an MDL lookup.

SCOPE EXCEPTIONS
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Schema-per-tenant, PostgreSQL RLS | not built; row-level discriminator chosen | ADR-TENANT-001 | erp-core-plan/05-STEP-multi-tenancy.md "Out of scope" |
| Tenant billing, per-tenant feature flags | not built | explicit future request | same |
| Rename / delete a tenant | not built (POL-TENANT-001, POL-TENANT-005) | tenant-maturity plan package B (rename of names only) | docs/DEVIATIONS.md [05] |
| Quotas, `ARCHIVED` status, per-tenant self-signup switch, per-tenant rate limits | not built | tenant-maturity plan §9 "level 2", later version | docs/plans/tenant-maturity-plan.md §0 D2 |

RESOLVED DECISIONS
| # | Question | Answer | Confirmed by | Sources |
|---|---|---|---|---|
| 1 | Row-level (discriminator) or schema-per-tenant? | row-level, shared schema | erp-core plan step 05 (fixed decision), as built | ADR-TENANT-001 |
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant level 1 (edit, suspension facts, admin-reset, usage)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Policy ids continue from POL-TENANT-011 (this module mints its policy ids, as the baseline did). Full
behaviour in `../P1/srs-tenant.md` → "Implementation Addendum — erp-core 1.3.0", package B block.

POL-TENANT-012 — التعليق بسبب مسجَّل / Suspension carries a recorded reason
  Statement (ar) : يجب على النظام ألا يعلّق مستأجرًا دون سبب (من 3 إلى 500 حرف)، وأن يسجّل من علّقه ومتى ولماذا، وأن يمحو هذه الحقائق عند إعادة التفعيل.
  Statement (en) : The system shall not suspend a tenant without a reason (3 to 500 characters), shall record who suspended it, when and why, and shall clear those facts when the tenant is re-activated.
  Pattern   : event
  Trigger   : Status change of a tenant
  Rationale : an operator's decision that locks an organisation out must be accountable and visible on the tenant record
  Source    : docs/plans/tenant-maturity-plan.md §4 B.1, B.2; RULE-TENANT-016; REQ-TENANT-026
  Status    : CONFIRMED (erp-core 1.3.0, package B)

POL-TENANT-013 — استعادة مدير المستأجر من المنصة / Platform-side recovery of a tenant administrator
  Statement (ar) : يجب على النظام تمكين مشغّل المنصة من تعيين كلمة مرور جديدة لمستخدم موظف يحمل دورًا فائقًا في مستأجر معيّن غير مستأجر المنصة، مع إنهاء جلساته وإلزامه افتراضيًا بتغييرها عند الدخول التالي وتسجيل العملية في سجل تدقيق ذلك المستأجر وسجل تدقيق المنصة دون كلمة المرور.
  Statement (en) : The system shall let a platform operator set a new password for a staff user holding a super role in a given tenant other than PLATFORM, terminating that user's sessions, requiring by default a change at the next sign-in, and recording the operation in that tenant's and in PLATFORM's audit logs without the secret.
  Pattern   : event
  Trigger   : `POST /api/v1/platform/tenants/{id}/admin-reset`
  Rationale : a tenant whose only administrator is locked out cannot repair itself from inside (POL-TENANT-004's rationale); the recovery must not become a way to take over ordinary users (super role only)
  Source    : docs/plans/tenant-maturity-plan.md §4 B.2, B.4; RULE-TENANT-017; REQ-TENANT-027; SEC ADR-SEC-063
  Status    : CONFIRMED (erp-core 1.3.0, package B)

CHANGED policies
| Policy | Delta | Source |
|---|---|---|
| POL-TENANT-001 | unchanged for the code (immutable, `^[A-Z0-9_]{3,32}$`); the names and the new profile fields (contact e-mail and phone, country, default language, time zone, notes) become editable by the platform operator (`PUT /{id}`) | REQ-TENANT-025 |
| POL-TENANT-002 | unchanged: two statuses; a suspension now carries a reason and its facts (POL-TENANT-012), an activation records a token cut-off that package C.2 enforces | REQ-TENANT-026 |
| POL-TENANT-006 | unchanged: the new operations (edit, admin-reset, usage) are platform-only like the others (`PLATFORM_TENANT_MANAGE`, plan §0 D5) | srs-tenant.md 1.3.0 B1 |
| POL-TENANT-007 | applies to the usage figures: each is counted inside the tenant asked for, never including another tenant's rows | REQ-TENANT-028 |

CUSTOM LOOKUP VALUES — delta
| Lookup key | Added values | Source |
|---|---|---|
| (value set of `CORE_TENANT.DEFAULT_LOCALE`, CHECK `CHK_CORE_TENANT_LOCALE`, not an MDL lookup) | `ar`, `en` (NULL allowed) | V18__tenant_profile.sql |

SCOPE EXCEPTIONS — delta
| Kind | Excluded / Deferred | Delta |
|---|---|---|
| CHANGED | Rename / delete a tenant | renaming the names leaves the exceptions (POL-TENANT-001 CHANGED); delete stays excluded (POL-TENANT-005) |
| unchanged | Quotas, `ARCHIVED` status, per-tenant self-signup switch, per-tenant rate limits | level 2, later version (plan §0 D2) |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding (logo, brand colour, `/api/v1/tenant/me`, public branding; plan §0 D5, §7)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Policy ids continue from POL-TENANT-013. Full behaviour in `../P1/srs-tenant.md` → "Implementation Addendum —
erp-core 1.3.0", package E block.

POL-TENANT-014 — العلامة التجارية يضبطها مدير المنصة فقط / Branding is set by the platform administrator only
  Statement (ar) : يجب على النظام قصر ضبط شعار المستأجر ولون علامته على مشغّل المنصة من شاشة `PLATFORM_TENANTS`؛ لا شاشة ذاتية للمستأجر في 1.3.0، والشعار ملف عام في صفوف المستأجر نفسه، ويقرأ كل مستخدم للمستأجر علامته دون صلاحية، ويقرؤها الزائر المجهول برمز المستأجر قبل الدخول.
  Statement (en) : The system shall let only a platform operator, from the `PLATFORM_TENANTS` screen, set a tenant's logo and brand colour; there is no tenant self-service screen in 1.3.0, the logo is a PUBLIC document in the tenant's own rows, every user of the tenant reads the branding without a permission, and an anonymous visitor reads it by tenant code before signing in.
  Pattern   : ubiquitous
  Trigger   : the logo / branding endpoints; `GET /api/v1/tenant/me`; `GET /api/v1/public/tenants/{tenantCode}/branding`
  Rationale : decision D5 — branding belongs to the onboarding the platform operator performs; no new module, screen, permission or grant for one attribute (ADR-TENANT-005)
  Source    : docs/plans/tenant-maturity-plan.md §0 D5, §7 E.1–E.3; ADR-TENANT-005; RULE-TENANT-018 … 022; REQ-TENANT-029 … 032
  Status    : CONFIRMED (erp-core 1.3.0, package E)

CHANGED policies
| Policy | Delta | Source |
|---|---|---|
| POL-TENANT-006 | unchanged: the branding writes are platform-only like every tenant operation (`PLATFORM_TENANT_MANAGE`); the two branding reads are not tenant management and need no permission | srs-tenant.md 1.3.0 E1 |
| POL-TENANT-008 | the public branding is a second path whose tenant comes from the path (after the public files) | REQ-TENANT-032 |

SCOPE EXCEPTIONS — delta
| Kind | Excluded / Deferred | Delta |
|---|---|---|
| NEW | Tenant self-service branding screen (`TENANT_BRANDING`) | deferred: the alternative recorded in ADR-TENANT-005 |
| NEW | Server-side resizing of logos; per-tenant theme beyond one accent colour | out of scope (plan §6 D.4, §8 F2) |

RESOLVED DECISIONS — delta
| # | Question | Answer | Decided | ADR |
|---|---|---|---|---|
| 5 | Who sets a tenant's logo | the platform administrator from `PLATFORM_TENANTS` | decision D5 (2026-10-07) | ADR-TENANT-005 |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — tenant lifecycle events and the per-tenant token cut-off (plan §5 C.1, C.2)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Policy ids continue from POL-TENANT-014. Full behaviour in `../P1/srs-tenant.md` → "Implementation Addendum —
erp-core 1.3.0", package C12 block.

POL-TENANT-015 — إعادة التفعيل والإبطال يقطعان الرموز السابقة / Re-activation and revocation cut off earlier tokens
  Statement (ar) : يجب على النظام ألا يقبل رمز دخول صدر قبل آخر إعادة تفعيل للمستأجر أو آخر إبطال صريح لرموزه من مشغّل المنصة؛ يحفظ النظام حدًّا زمنيًا واحدًا لكل مستأجر، وينهي الإبطال جلسات المستأجر، ولا تُبطَل رموز مستأجر المنصة.
  Statement (en) : The system shall refuse an access token issued before the tenant's last re-activation or the platform operator's last explicit revocation of its tokens; one cut-off instant is kept per tenant, a revocation also ends the tenant's sessions, and the PLATFORM tenant's tokens are never revoked this way.
  Pattern   : ubiquitous
  Trigger   : every authenticated request; `PATCH /{id}/status` to ACTIVE; `POST /api/v1/platform/tenants/{id}/revoke-tokens`
  Rationale : tokens issued before a suspension must not come back to life when the tenant is re-activated, and a compromised tenant needs a "sign everyone out" without being suspended; a platform-wide denylist is avoided (ADR-TENANT-002)
  Source    : docs/plans/tenant-maturity-plan.md §5 C.2; RULE-TENANT-023, -024; REQ-TENANT-034, -035; ADR-TENANT-002
  Status    : CONFIRMED (erp-core 1.3.0, package C12)

CHANGED policies
| Policy | Delta | Source |
|---|---|---|
| POL-TENANT-002 | still two statuses (no `ARCHIVED`, level 2); a suspension now also **ends every open session** of the tenant (SEC, on `TenantSuspendedEvent`) and **holds its queued notifications** (NOTIF claims none until the tenant is ACTIVE again, then sends them); a re-activation cuts off every token issued before it (POL-TENANT-015) | REQ-TENANT-033, -034 |
| POL-TENANT-006 | unchanged: revoke-tokens is platform-only like every tenant operation (`PLATFORM_TENANT_MANAGE`) | REQ-TENANT-035 |

RESOLVED DECISIONS — delta
| # | Question | Answer | Decided | ADR |
|---|---|---|---|---|
| 2 | Token cut-off vs `jti` denylist | per-tenant cut-off `TOKENS_INVALID_BEFORE`, compared in whole seconds (activation: a token of the activation's own second is served; revoke-tokens: the cut-off is the next whole second, so the revoke's own second is refused); PLATFORM not revocable | plan §9 (recommendation), package C12 | ADR-TENANT-002 |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C4 — idempotent provisioning (plan §5 C.4)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Policy ids continue from POL-TENANT-015. Full behaviour in `../P1/srs-tenant.md` → "Implementation Addendum —
erp-core 1.3.0", package C4 block.

POL-TENANT-016 — التجهيز لا يتكرر بمفتاح عدم التكرار / Provisioning is idempotent under an idempotency key
  Statement (ar) : عند تكرار طلب إنشاء مستأجر بمفتاح عدم التكرار نفسه والمحتوى نفسه ومن المستخدم نفسه يجب على النظام إعادة الاستجابة المخزّنة دون إنشاء شيء، ورفض المحتوى المختلف أو المستخدم المختلف تحت المفتاح نفسه؛ لا تُخزَّن إلا الاستجابات الناجحة، وتُحفظ المفاتيح 24 ساعة.
  Statement (en) : When a tenant-create request is repeated with the same `Idempotency-Key`, the same body and by the same user, the system shall replay the stored answer and create nothing; another body or another user under the same key shall be refused; only successful answers are stored, and keys are kept for 24 hours.
  Pattern   : optional · `POST /api/v1/platform/tenants` with the header
  Trigger   : a create request carrying `Idempotency-Key`
  Rationale : a retried provisioning call (timeout, client crash) must neither create a second tenant nor fail with `TENANT_CODE_DUPLICATE`; the stored answer commits with the tenant it describes (ADR-TENANT-003)
  Source    : docs/plans/tenant-maturity-plan.md §5 C.4; RULE-TENANT-025, -026; REQ-TENANT-036; ADR-TENANT-003
  Status    : CONFIRMED (erp-core 1.3.0, package C4)

CHANGED policies
| Policy | Delta | Source |
|---|---|---|
| POL-TENANT-004 | still all-or-nothing; with `Idempotency-Key` the stored answer is part of the same transaction (a crash before the commit leaves neither the tenant nor a replayable answer) | REQ-TENANT-036 |

RESOLVED DECISIONS — delta
| # | Question | Answer | Decided | ADR |
|---|---|---|---|---|
| 3 | Where idempotency keys live | a core table `CORE_IDEMPOTENCY_KEY` behind the common mechanism `com.erp.common.idempotency`, tenant-scoped, 24 h retention (expired rows ignored at lookup and purged by a job), only 2xx answers stored in the operation's transaction, replayed to the same user only; first consumer tenant create | plan §9 (recommendation), package C4 | ADR-TENANT-003 |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C5 — tenant data export (plan §5 C.5)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Policy ids continue from POL-TENANT-016. Full behaviour in `../P1/srs-tenant.md` → "Implementation Addendum —
erp-core 1.3.0", package C5 block.

POL-TENANT-017 — تصدير بيانات المستأجر بلا أسرار / A tenant's data can be exported, never its secrets
  Statement (ar) : يجب على النظام تمكين مشغّل المنصة من تصدير بيانات مستأجر كملف مضغوط واحد تكتب فيه كل وحدة جداولها بنفسها بصيغة CSV، دون كلمات المرور المجزّأة أو الرموز أو بيانات اعتماد القنوات أو محتوى الملفات، وبحدّ أقصى لعدد السجلات، ويُسلَّم الملف عبر رمز تنزيل يُستخدم مرة واحدة.
  Statement (en) : The system shall let a platform operator export a tenant's data as one archive in which every module writes its own tables as CSV, never the password hashes, tokens, channel credentials or file bytes, within a configured row limit, handed out through a single-use download token.
  Pattern   : event · `POST /api/v1/platform/tenants/{id}/export`
  Trigger   : a platform operator's export request
  Rationale : portability of a tenant's data is a platform duty; the shared schema makes it a `TENANT_ID`-filtered copy (ADR-TENANT-001 consequences); the archive must not become a way to carry credentials out (ADR-TENANT-006)
  Source    : docs/plans/tenant-maturity-plan.md §5 C.5; REQ-TENANT-037; RULE-TENANT-027, -028; ADR-TENANT-006
  Status    : CONFIRMED (erp-core 1.3.0, package C5)

RESOLVED DECISIONS — delta
| # | Question | Answer | Decided | ADR |
|---|---|---|---|---|
| 6 | How a tenant's data is exported | synchronously in v1, bounded by `erp.core.tenant.export.max-rows`, one snapshot, one CSV per table written by its module (SPI), ZIP + manifest stored as a PRIVATE PLATFORM document, single-use download token, one export per tenant at a time per node | plan §5 C.5, package C5 | ADR-TENANT-006 |
