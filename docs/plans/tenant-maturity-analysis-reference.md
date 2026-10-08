# Tenant-maturity plan — analysis reference rows (for the implementing packages)

> Superseded 2026-10-08 by the implemented per-package addenda on origin/main (f48b9ab); kept as the pre-implementation reference.

| | |
|---|---|
| Date | 2026-10-08 |
| Status | REFERENCE — not governance; the implementing run writes the governance rows per package and mints the ids; these rows were verified against erp-core main @ 19b19a4 by the analysis-coverage session |
| Plan | `docs/plans/tenant-maturity-plan.md` (packages, decisions D1–D7, proposed names) |
| Coverage review | `docs/plans/analysis-coverage-review.md` |
| Why this file | the analysis-coverage session had written these rows into the `## Implementation Addendum — erp-core 1.3.0` sections of `governance/analysis/modules/{TENANT,FILE,NOTIF,AUDIT,EVENTS,COMMON}/**` and the ADR drafts `ADR-TENANT-002..005`, `ADR-FILE-001`. The implementing run owns those addenda per package (analysis-first, written before each package's code), so the rows were moved here on 2026-10-08 and the governance sections now hold only what is already on `main` (the common-helper refactors). |

## How to read this file

- One section per plan package, in plan order (B, C1 … C6, D1 … D4, E, G). Each row is the row that stood in the named governance file's 1.3.0 addendum on 2026-10-08, tables and `Kind` column kept. A row that served several packages is shown under the first package and pointed to from the others.
- **Placeholders.** The ids this session had minted for plan work are replaced by `REQ-TENANT-nnn`, `AC-TENANT-nnn`, `RULE-TENANT-nnn`, `XM-TENANT-nnn`, `POL-TENANT-nnn`, `US-TENANT-nnn`, `DBF-TENANT-nnn`, `XM-FILE-nnn`, `RULE-NOTIF-nnn`. The implementing package mints the real id from the module registry's "Last sequence" line when it writes its addendum. Where a row cites another draft row, the placeholder carries that row's short name in parentheses, e.g. `RULE-TENANT-nnn (suspension reason)`.
- **Ids the implementing run has already minted — never re-mint them:** REQ-TENANT-024, AC-TENANT-024, RULE-TENANT-010, RULE-TENANT-011 (package C3); REQ-SEC-082+, AC-SEC-088+, RULE-SEC-056+, ENT-SEC-015+ (package D); ADR-TENANT-002 … 005 and ADR-FILE-008 (drafts in the last section).
- **Real ids, cited as such:** the as-built TENANT body — REQ/AC-TENANT-001 … 023, RULE-TENANT-001 … 009 and **012 … 015** (renumbered on 2026-10-08 from 010 … 013 because C3 holds 010/011: 012 resolution order, 013 thread/session binding, 014 bootstrap tolerance, 015 search allow-list and no caching), ENT-TENANT-001, XM-TENANT-001/002, POL-TENANT-001 … 011, US-TENANT-001 … 008, DBF-TENANT-001 … 032, SCR-REQ-TENANT-001; FILE's RULE-FILE-*, XM-FILE-001, ENTITY-FILE-001; NOTIF's RULE-NOTIF-001 … 022, ENTITY-NOTIF-001 … 004; EVENTS' RULE-EVENTS-007/008, XM-EVENTS-001; COMMON's ENT-COMMON-002, RULE-COMMON-004 … 009, REQ-COMMON-018.
- Migration numbers (`V16 … V21`) are the plan's **expected** numbers; the implementing package re-derives them from the directory listing at creation time (`CLAUDE.md` → Database migrations). `V19__sec_user_profile.sql` belongs to SEC (package D).
- "plan §n" cites `docs/plans/tenant-maturity-plan.md`. Java paths are relative to `erp-core/src/main/java/com/erp/`. **may slip** = packages C.5 / C.6 may move to 1.4.0 without blocking the tag (plan §2); **FE** = the frontend reacts (plan §8).

### Placeholder legend (one row per draft id, so the sequence can be minted in this order)

| Placeholder | Short name | Package |
|---|---|---|
| REQ/AC-TENANT-nnn | update a tenant's names and profile | B |
| REQ/AC-TENANT-nnn | suspend with a reason; suspension facts | B |
| REQ/AC-TENANT-nnn | reset a tenant administrator's password | B |
| REQ/AC-TENANT-nnn | tenant usage figures | B |
| REQ/AC-TENANT-nnn | tenant lifecycle events | C1 |
| REQ/AC-TENANT-nnn | token cut-off | C2 |
| (minted: REQ-TENANT-024 / AC-TENANT-024) | automated isolation tests | C3 |
| REQ/AC-TENANT-nnn | idempotent provisioning | C4 |
| REQ/AC-TENANT-nnn | tenant data export (**may slip**) | C5 |
| REQ/AC-TENANT-nnn | tenant logo set by the platform administrator | E |
| REQ/AC-TENANT-nnn | brand colour | E |
| REQ/AC-TENANT-nnn | branding of the token's tenant | E |
| REQ/AC-TENANT-nnn | public branding by tenant code | E |
| RULE-TENANT-nnn | suspension reason | B |
| RULE-TENANT-nnn | admin-reset target | B |
| RULE-TENANT-nnn | token cut-off | C2 |
| (minted: RULE-TENANT-010, -011) | isolation (C3's own wording) | C3 |
| RULE-TENANT-nnn | idempotent provisioning | C4 |
| RULE-TENANT-nnn | export bounded and secret-free (**may slip**) | C5 |
| RULE-TENANT-nnn | tenant logo | E |
| RULE-TENANT-nnn | PLATFORM logo | E |
| RULE-TENANT-nnn | branding platform-only | E |
| RULE-TENANT-nnn | brand colour format | E |
| XM-TENANT-nnn | export SPI `TenantExportContributor` (**may slip**) | C5 |
| XM-TENANT-nnn | logo soft reference `CORE_TENANT.LOGO_FILE_ID` | E |
| POL-TENANT-nnn | suspension carries a recorded reason | B |
| POL-TENANT-nnn | re-activation and revocation cut off earlier tokens | C2 |
| POL-TENANT-nnn | provisioning is idempotent under an idempotency key | C4 |
| POL-TENANT-nnn | a tenant's data can be exported, never its secrets | C5 |
| POL-TENANT-nnn | branding is set by the platform administrator only | E |
| POL-TENANT-nnn | platform-side recovery of a tenant administrator | B |
| US-TENANT-nnn | edit names and profile · recover an administrator · usage figures | B |
| US-TENANT-nnn | revoke a tenant's tokens | C2 |
| US-TENANT-nnn | export a tenant's data | C5 |
| US-TENANT-nnn | logo and brand colour · read my tenant's branding · branding before login | E |
| DBF-TENANT-nnn ×12 | `CORE_TENANT` columns of V16 / V17 / V18 | B, C2, E |
| DBF-TENANT-nnn | `CORE_IDEMPOTENCY_KEY.TENANT_ID` | C4 |
| XM-FILE-nnn | `FileImageStoreApi` | D4 |
| RULE-NOTIF-nnn | job skips non-ACTIVE tenants | C1 |

---

## Package B — tenant level 1: profile, editable names, suspension with a reason, admin-reset, usage

### B.1 Endpoints — `TENANT/P1/srs-tenant.md` §1
All `/api/v1/platform/tenants/**` rows keep the 1.2.0 gate: authority `PLATFORM_TENANT_MANAGE` plus the chain gate "request tenant = PLATFORM" (`isPlatformOperator`, REQ-TENANT-015). Paths are relative to `/api/v1/platform/tenants` unless written in full.

| Kind | Method | Path | Access | Body / result | Rules & errors | Traces | Source |
|---|---|---|---|---|---|---|---|
| NEW | PUT | `/{id}` | `PLATFORM_TENANT_MANAGE` | `TenantUpdateRequest`: `nameAr`, `nameEn`, `contactEmail`, `contactPhone`, `countryCode`, `defaultLocale`, `timezone`, `notes` → 200 `TenantResponse` | `code` and `statusCode` are **not** in the body (RULE-TENANT-003; status has its own endpoint); 404 `TENANT_NOT_FOUND`; 409 `CONCURRENT_MODIFICATION` | REQ-TENANT-nnn (update profile) | plan §4 B.2 |
| CHANGED | PATCH | `/{id}/status` | as before | `TenantStatusUpdateRequest` `+ reason` (required when `SUSPENDED`, 3–500 chars; ignored when `ACTIVE`) → 200 `TenantResponse` | `TenantDomain.assertCanChangeStatusTo` unchanged (RULE-TENANT-005); on suspend sets `SUSPENDED_AT` / `SUSPENDED_BY` / `SUSPENSION_REASON`; on activate clears them and sets `TOKENS_INVALID_BEFORE = now()` (C2); publishes `TenantSuspendedEvent` / `TenantActivatedEvent` after commit (C1); 400 `TENANT_SUSPENSION_REASON_REQUIRED` | REQ-TENANT-nnn (suspend with a reason), (lifecycle events), (token cut-off); RULE-TENANT-nnn (suspension reason) | plan §4 B.2, §5 C.1, C.2 |
| NEW | POST | `/{id}/admin-reset` | `PLATFORM_TENANT_MANAGE` | `{ username, newPassword }` → 200 `{ username, sessionsTerminated }` | `username` must be a STAFF user of tenant `{id}` holding a role with `IS_SUPER = TRUE` (RULE-TENANT-nnn (admin-reset target)); BCrypt-hashed; every session of that user terminated; audit `ADMIN_PASSWORD_RESET` in `CORE_AUDIT_EVENT` (actor = platform operator, target tenant = `{id}`); 404 `TENANT_ADMIN_NOT_FOUND`, 422 `TENANT_ADMIN_NOT_SUPER`, 400 `SEC-400-PASSWORD-POLICY` (SEC, plan §6 D.1) | REQ-TENANT-nnn (admin-reset) | plan §4 B.2, B.4 |
| NEW | GET | `/{id}/usage` | `PLATFORM_TENANT_MANAGE` | → 200 `TenantUsageResponse`: `staffUsers`, `customerUsers`, `activeSessions`, `fileDocuments`, `fileBytes`, `notificationsLast30Days`, `collectedAt` | each count obtained inside `TenantContext.callAs(id)` through the cross-module methods of B.6; never joins another module's tables; 404 `TENANT_NOT_FOUND` | REQ-TENANT-nnn (usage) | plan §4 B.2, B.4 |
| CHANGED | GET / POST / GET | `/{id}`, `/search`, list | as before | `TenantResponse` `+ contactEmail, contactPhone, countryCode, defaultLocale, timezone, notes, suspendedAt, suspendedBy, suspensionReason, logoUrl, brandColor` (`logoUrl`, `brandColor` with E) | search / sort allow-list (RULE-TENANT-015) gains `contactEmail`, `countryCode`, `suspendedAt` | REQ-TENANT-nnn (update profile), (suspend with a reason), (logo) | plan §4 B.2, §7 E.2 |

### B.2 Business rules — `srs-tenant.md` §2
| Kind | Id | Rule | Traces | Source |
|---|---|---|---|---|
| NEW | RULE-TENANT-nnn — التعليق يتطلب سببًا والتفعيل يمحوه / Suspension requires a reason; activation clears it | The system shall suspend a tenant only with a `reason` of 3–500 characters and shall record `SUSPENDED_AT`, `SUSPENDED_BY` (the operator) and `SUSPENSION_REASON`; on activation it shall clear the three facts and set `TOKENS_INVALID_BEFORE = now()`. Decided in `TenantDomain`. Message: `TENANT_SUSPENSION_REASON_REQUIRED` (AR + EN bound at implementation). | REQ-TENANT-nnn (suspend with a reason) | plan §4 B.2, B.3 |
| NEW | RULE-TENANT-nnn — هدف إعادة تعيين كلمة مرور المدير / Admin-reset target | The system shall reset a password through `/{id}/admin-reset` only for a STAFF user of that tenant holding a role with `IS_SUPER = TRUE`; the fact "holds a super role" is computed by SEC and passed into `TenantDomain` by the service. Unknown user → `TENANT_ADMIN_NOT_FOUND`; not super → `TENANT_ADMIN_NOT_SUPER`; the new password obeys SEC's `PasswordPolicy`. | REQ-TENANT-nnn (admin-reset) | plan §4 B.2, B.3; §6 D.1 |

### B.3 Error codes — `srs-tenant.md` §3
| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW | `TENANT_SUSPENSION_REASON_REQUIRED` | 400 | `PATCH /{id}/status` to `SUSPENDED` without a 3–500-character `reason` | plan §4 B.2 |
| NEW | `TENANT_ADMIN_NOT_FOUND` | 404 | `/{id}/admin-reset`: no STAFF user with that `username` in tenant `{id}` | plan §4 B.2 |
| NEW | `TENANT_ADMIN_NOT_SUPER` | 422 (`BUSINESS_RULE_VIOLATION`) | `/{id}/admin-reset`: the user holds no role with `IS_SUPER = TRUE` | plan §4 B.2 |
| referenced (SEC) | `SEC-400-PASSWORD-POLICY` | 400 | admin-reset password outside SEC's `PasswordPolicy` | plan §6 D.1 |
Every new code gets an entry in `messages.properties` and `messages_ar.properties`; the seven 1.2.0 codes are unchanged.

### B.4 Permissions — `srs-tenant.md` §4 (applies to every package)
| Kind | Authority | Screen / meaning | Source |
|---|---|---|---|
| unchanged | `PERM_PLATFORM_TENANTS_VIEW`, `PLATFORM_TENANT_MANAGE` | every new platform endpoint is behind `PLATFORM_TENANT_MANAGE`; **nothing new** — decision D5: the logo is managed from the existing `PLATFORM_TENANTS` screen; no module, screen, permission or grant seed | plan §0 D5, §7 E.1 |

### B.5 Entities, fields, lifecycle — `srs-tenant.md` §5
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENT-TENANT-001 Tenant | + `contactEmail` (≤ 255), `contactPhone` (≤ 30), `countryCode` (2), `defaultLocale` (`ar` \| `en`), `timezone` (≤ 64), `notes` (≤ 1000) — all optional, editable through `PUT /{id}` (V16); + `suspendedAt`, `suspendedBy` (≤ 100), `suspensionReason` (≤ 500), `tokensInvalidBefore` — system-written by status changes and `revoke-tokens`, read-only to clients (V17); + `logoFileId` (soft reference to `FILE_DOCUMENT.ID`, no FK — same convention as `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID`), `brandColor` (7) (V18, package E). `nameAr` / `nameEn` become editable; `code` stays immutable. Physical names and widths: B.14 / E.6 | plan §4 B.1, §7 E.1 |
| NEW (DTOs) | `TenantUpdateRequest`, `TenantUsageResponse`, `TenantBrandingResponse` (E) | as in B.1 / E.1; `TenantResponse` and `TenantStatusUpdateRequest` extended | plan §4 B.2, §7 E.2 |
| unchanged | A7 lifecycle | still `ACTIVE` ⇄ `SUSPENDED`, no `ARCHIVED` (level 2 is out of scope, plan §0 D2); a suspension now carries facts and an activation a token cut-off | plan §0 D2, §4 B.2 |

### B.6 Dependencies (A8) — `srs-tenant.md` §6
| Direction | Item | Source |
|---|---|---|
| consumed (SEC `crossmodule`) | `SecUserDirectoryApi` + `int countStaff()`, `int countCustomers()`, `int countActiveSessions()` (current tenant) — usage | plan §4 B.4 |
| consumed (SEC `crossmodule`) | NEW `SecAdminRecoveryApi.int resetSuperUserPassword(String username, String rawPassword)` — runs in the current tenant (the tenant service wraps it in `callAs`), returns the terminated-session count, throws the SEC codes | plan §4 B.4 |
| consumed (FILE `crossmodule`) | `FileDocumentLookupApi` + `long countDocuments()`, `long sumBytes()` — usage; NEW `FileImageStoreApi.storePublicImage(ImageStoreRequest)` → `StoredImage(documentId, publicUrl)`, `discard(Long documentId)` — logo (D4 / E) | plan §4 B.4, §6 D.4 |
| consumed (NOTIF `crossmodule`) | existing dispatch API + `long countDispatchedSince(Instant)` — usage | plan §4 B.4 |

### B.7 Requirements and acceptance criteria — `srs-tenant.md` §7
| Kind | REQ / AC | Statement (EARS) → acceptance | Traces | Source |
|---|---|---|---|---|
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn — تعديل أسماء المستأجر وملفه / Update a tenant's names and profile | When a platform operator puts valid names and profile fields to `/{id}`, the system shall store them and return the tenant; a body carrying `code` or `statusCode` has those fields ignored. → Given tenant A, when `PUT /{id}` with new names and `countryCode`, then 200 echoes them and `code` is unchanged; unknown id 404 `TENANT_NOT_FOUND` (TC-CORE-TENANT-027, -028) | US-TENANT-nnn (edit names and profile); POL-TENANT-001; RULE-TENANT-003, -015 | plan §4 B.2, B.5 |
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn — تعليق بسبب وحقائق التعليق / Suspend with a reason; suspension facts | When a status change to `SUSPENDED` carries a reason, the system shall record the facts; if it lacks one, the system shall refuse it; when the tenant is activated, the system shall clear the facts. → suspend without reason 400 `TENANT_SUSPENSION_REASON_REQUIRED`; suspend with reason → `suspendedAt/By/suspensionReason` set; activate → cleared (TC-CORE-TENANT-029, -030, -031) | US-TENANT-003; POL-TENANT-002, POL-TENANT-nnn (suspension reason); RULE-TENANT-nnn (suspension reason) | plan §4 B.2, B.5 |
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn — إعادة تعيين كلمة مرور مدير المستأجر / Reset a tenant administrator's password | When a platform operator posts a super user's username and a new password to `/{id}/admin-reset`, the system shall set the password, terminate that user's sessions and record `ADMIN_PASSWORD_RESET`. → unknown user 404; non-super 422; success returns `sessionsTerminated` and the user logs in with the new password (TC-CORE-TENANT-032, -033, -034) | US-TENANT-nnn (recover an administrator); POL-TENANT-006, -011, POL-TENANT-nnn (admin recovery); RULE-TENANT-nnn (admin-reset target) | plan §4 B.2, B.5 |
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn — أرقام استخدام المستأجر / Tenant usage figures | When a platform operator asks for `/{id}/usage`, the system shall return the counts of B.1 collected inside `callAs(id)`. → a fresh tenant answers `staffUsers = 1` (TC-CORE-TENANT-035) | US-TENANT-nnn (usage figures); POL-TENANT-006, -007 | plan §4 B.2, B.5 |
Test-case ids `TC-CORE-TENANT-027…` are the next free numbers of `docs/test-api/core-test-plan.md` (last used on 2026-10-07: 026); their final numbering is bound when the plan is extended.

### B.8 Screen (SCR-REQ-TENANT-001 PLATFORM_TENANTS) — `srs-tenant.md` §8 (**FE**, plan §8 F3; covers B, C2, C5, E)
| Kind | Section | Delta | Source |
|---|---|---|---|
| CHANGED | B2 Search / list | + filters / sorts `contactEmail`, `countryCode`, `suspendedAt`; rows show `logoUrl` | plan §4 B.2 |
| CHANGED | B3 Input | + edit form (names + profile fields, `code` read-only); suspend dialog with mandatory `reason`; admin-reset form (`username`, `newPassword`); logo upload / remove + optional `brandColor`; revoke-tokens and export as confirmed actions | plan §8 F3 |
| CHANGED | B5 API expectations | + the `/{id}` PUT, `/{id}/admin-reset`, `/{id}/usage`, `/{id}/revoke-tokens`, `/{id}/export`, `/{id}/logo` PUT / DELETE, `/{id}/branding` rows — all `PLATFORM_TENANT_MANAGE` | plan §4, §5, §7 |
| unchanged | B4 Access | same two actions; no new page code (D5) | plan §0 D5 |
No new screen: `/api/v1/tenant/me` and the public branding endpoint feed the frontend shell and the login page (plan §8 F2), not a screen of this module.

### B.9 Registry deltas — `TENANT/P1/registry-srs-tenant.md`
ENTITIES — delta
| Kind | ENT id | Delta | Source |
|---|---|---|---|
| CHANGED | ENT-TENANT-001 Tenant | + profile (`contactEmail`, `contactPhone`, `countryCode`, `defaultLocale`, `timezone`, `notes`), + suspension facts (`suspendedAt`, `suspendedBy`, `suspensionReason`), + `tokensInvalidBefore`, + branding (`logoFileId`, `brandColor`); names editable | plan §4 B.1, §7 E.1 |

REQUIREMENTS — delta (one AC per REQ)
| Kind | REQ / AC | Title | Plan |
|---|---|---|---|
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn | Update a tenant's names and profile (`PUT /{id}`) | §4 B.2 |
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn | Suspend with a reason; suspension facts | §4 B.2 |
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn | Reset a tenant administrator's password (`/{id}/admin-reset`) | §4 B.2 |
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn | Tenant usage figures (`/{id}/usage`) | §4 B.2 |

RULES — delta
| Kind | RULE id | Title | Plan |
|---|---|---|---|
| NEW | RULE-TENANT-nnn | Suspension requires a reason; activation clears it | §4 B.3 |
| NEW | RULE-TENANT-nnn | Admin-reset target must be a super-role STAFF user of that tenant | §4 B.3 |

SCREENS — delta
| Kind | SCR-REQ id | Delta | Plan |
|---|---|---|---|
| CHANGED | SCR-REQ-TENANT-001 PLATFORM_TENANTS | B2 + 3 filter fields; B3 edit / suspend-reason / admin-reset / logo / branding forms; B5 + 9 endpoints; B4 unchanged (no new page code, D5) | §4, §5, §7, §8 F3 |

ERROR CODES — delta (all packages): NEW `TENANT_SUSPENSION_REASON_REQUIRED` (400), `TENANT_ADMIN_NOT_FOUND` (404), `TENANT_ADMIN_NOT_SUPER` (422), `TENANT_TOKEN_REVOKED` (401), `IDEMPOTENCY_KEY_CONFLICT` (409, common mechanism), `TENANT_EXPORT_TOO_LARGE` (422, **may slip**), `TENANT_EXPORT_IN_PROGRESS` (409, **may slip**), `TENANT_LOGO_INVALID` (400), `TENANT_BRAND_COLOR_INVALID` (400); referenced SEC `SEC-400-PASSWORD-POLICY` (400). — plan §4 B.2, §5 C.2 / C.4 / C.5, §7 E.2

PERMISSIONS — delta: None (decision D5): every new platform endpoint is behind the existing `PLATFORM_TENANT_MANAGE`. — plan §0 D5, §7 E.1

DEPENDENCIES — delta
| Type | Target | Module | Plan |
|---|---|---|---|
| crossmodule (consumed) | `SecUserDirectoryApi.countStaff / countCustomers / countActiveSessions`; NEW `SecAdminRecoveryApi.resetSuperUserPassword` | SEC | §4 B.4 |
| crossmodule (consumed) | `FileDocumentLookupApi.countDocuments / sumBytes`; NEW `FileImageStoreApi` | FILE | §4 B.4, §6 D.4 |
| crossmodule (consumed) | dispatch API `countDispatchedSince(Instant)` | NOTIF | §4 B.4 |

### B.10 Policies — `TENANT/P0/business-policies-tenant.md`
| Kind | Policy | Statement (ar / en) | Pattern · Trigger | Rationale | Source | Status |
|---|---|---|---|---|---|---|
| NEW | POL-TENANT-nnn — التعليق بسبب مسجَّل / Suspension carries a recorded reason | يجب على النظام ألا يعلّق مستأجرًا دون سبب (3–500 حرفًا)، وأن يسجّل من علّقه ومتى ولماذا، وأن يمحو هذه الحقائق عند إعادة التفعيل. / The system shall not suspend a tenant without a reason (3–500 characters), shall record who suspended it, when and why, and shall clear those facts on re-activation. | event · status change | an operator's decision that locks an organisation out must be accountable and visible on the tenant | plan §4 B.1, B.2 (`SUSPENDED_AT`, `SUSPENDED_BY`, `SUSPENSION_REASON`, `TENANT_SUSPENSION_REASON_REQUIRED`) | PLANNED (1.3.0) |
| NEW | POL-TENANT-nnn — استعادة مدير المستأجر من المنصة / Platform-side recovery of a tenant administrator | يجب على النظام تمكين مشغّل المنصة من إعادة تعيين كلمة مرور مستخدم STAFF يحمل دورًا فائقًا في مستأجر معيّن، مع إنهاء جلساته وتسجيل العملية في سجل التدقيق دون كشف كلمة المرور. / The system shall let a platform operator reset the password of a STAFF user holding a super role in a given tenant, terminating that user's sessions and recording the operation in the audit log without the secret. | event · `/{id}/admin-reset` | a tenant whose only administrator is locked out cannot repair itself from inside (POL-TENANT-004 rationale) | plan §4 B.2, B.4 (`SecAdminRecoveryApi`, `TENANT_ADMIN_NOT_FOUND`, `TENANT_ADMIN_NOT_SUPER`, audit `ADMIN_PASSWORD_RESET`) | PLANNED (1.3.0) |
| CHANGED | POL-TENANT-001 | unchanged for the code; the names and the new profile fields become editable (`PUT /{id}`) — the body's "not editable through the API" notes on `nameAr` / `nameEn` end with 1.3.0 | ubiquitous | same | plan §4 B.2 | PLANNED (1.3.0) |

SCOPE EXCEPTIONS — delta
| Kind | Excluded / Deferred | Delta | Source |
|---|---|---|---|
| CHANGED | Rename / delete a tenant | rename of the names leaves the exceptions (POL-TENANT-001 CHANGED); delete stays excluded (POL-TENANT-005) | plan §4 B.2 |
| unchanged | Quotas, `ARCHIVED`, per-tenant self-signup switch, per-tenant rate limits, platform-set tenant settings | level 2, later version | plan §0 D2, §9 |

### B.11 Module registry — `TENANT/P0/module-registry-tenant.md`
ENTITIES OWNED — delta
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | المستأجر / Tenant (`CORE_TENANT`) | + profile columns (V16), suspension facts + `TOKENS_INVALID_BEFORE` (V17), `LOGO_FILE_ID` + `BRAND_COLOR` (V18); still global, still SHARED (owner) | plan §4 B.1, §7 E.1, §11 |

LOOKUPS OWNED — delta
| Kind | Lookup key | Delta | Source |
|---|---|---|---|
| NEW | (value set of `CORE_TENANT.DEFAULT_LOCALE`) | `ar`, `en` — CHECK `CHK_CORE_TENANT_LOCALE` (NULL allowed), not an MDL lookup | plan §4 B.1 |
| unchanged | `STATUS_CODE` | `ACTIVE`, `SUSPENDED` (no `ARCHIVED`, level 2) | plan §0 D2 |

DEPENDENCIES — delta
| Kind | Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|---|
| NEW | SEC | crossmodule | `SecUserDirectoryApi` + `int countStaff()`, `int countCustomers()`, `int countActiveSessions()`; NEW `SecAdminRecoveryApi.int resetSuperUserPassword(String username, String rawPassword)` (called inside `TenantContext.callAs(id)`) | plan §4 B.4 |
| NEW | FILE | crossmodule | `FileDocumentLookupApi` + `long countDocuments()`, `long sumBytes()`; NEW `FileImageStoreApi.storePublicImage(ImageStoreRequest)` / `discard(Long)` (logo, as tenant `{id}`); `FileDocumentLookupApi.isAvailable` / `publicUrl` for `LOGO_FILE_ID` | plan §4 B.4, §6 D.4, §7 E.1 |
| NEW | NOTIF | crossmodule | existing dispatch API + `long countDispatchedSince(Instant)`; NOTIF's claim job reads `TenantLookupApi.isActive` (NOTIF consumes tenant, C1) | plan §4 B.4, §5 C.1 |
| NEW | audit | SOFT | new `CORE_AUDIT_EVENT` actions `ADMIN_PASSWORD_RESET`, `TOKENS_REVOKED` (C2), `TENANT_EXPORTED` (C5, **may slip**), `TENANT_LOGO_CHANGED` (E) | plan §4 B.2, §5 C.2 / C.5, §7 E.2 |

PERMISSION MODULE → SCREEN → ACTIONS — delta: None (decision D5): no module, screen, permission or grant seed; the logo and every new endpoint sit behind `PLATFORM_TENANT_MANAGE` on `PLATFORM_TENANTS`. — plan §0 D5, §7 E.1

### B.12 Platform summary — `TENANT/P0/platform-summary.md`
| Kind | Aspect | Delta | Source |
|---|---|---|---|
| CHANGED | Tenant record | `CORE_TENANT` gains a profile (V16), suspension facts + a token cut-off (V17) and branding (V18); names become editable (`PUT /{id}`); `code` stays immutable | plan §4 B.1, §7 E.1, §11 |
| CHANGED | Tenant management | `/api/v1/platform/tenants` gains update, admin-reset, usage, revoke-tokens, export (**may slip**), logo and branding; still `PLATFORM_TENANT_MANAGE` + PLATFORM-tenant caller; no new permission (D5) | plan §4 B.2, §5 C.2 / C.5, §7 E.2 |
| CHANGED | DEPENDENCY MAP | + `TENANT ──crossmodule──▶ SEC (SecUserDirectoryApi counts, SecAdminRecoveryApi)`, `──▶ FILE (FileDocumentLookupApi counts, FileImageStoreApi)`, `──▶ NOTIF (countDispatchedSince)`, `──▶ common (idempotency)`; `TENANT ──publishes──▶ events (2 new)`; `NOTIF ──TenantLookupApi.isActive──▶ TENANT` | plan §4 B.4, §5 C.1, §6 D.4 |
| CHANGED | DEFERRED | "Editing a tenant's name, a usage endpoint" and "profile, suspension facts, token cut-off, lifecycle events, branding, idempotent provisioning, export" leave DEFERRED; level 2 (quotas, `ARCHIVED`, per-tenant self-signup switch, per-tenant rate limits, platform-set tenant settings) stays deferred | plan §0 D2, §9 |

### B.13 Stories — `TENANT/P0_5/prd-tenant.md`
| Kind | US | Title (ar / en) | Story | Actor | Traces (POL) | Source |
|---|---|---|---|---|---|---|
| NEW | US-TENANT-nnn | تعديل أسماء المستأجر وملفه / Edit a tenant's names and profile | As a platform operator, I need to correct a tenant's names and keep its contact e-mail, phone, country, default locale, timezone and notes, so that the record describes the organisation — without ever changing its code. | platform operator (`PLATFORM_TENANT_MANAGE`) | POL-TENANT-001, -006 | plan §4 B.2 (`PUT /{id}`, `TenantUpdateRequest`) |
| NEW | US-TENANT-nnn | استعادة مدير مستأجر / Recover a tenant's administrator | As a platform operator, I need to set a new password for a tenant's super administrator when that tenant is locked out, so that the organisation regains access without a database intervention. | platform operator | POL-TENANT-006, -011, POL-TENANT-nnn (admin recovery) | plan §4 B.2 (`/{id}/admin-reset`) |
| NEW | US-TENANT-nnn | أرقام استخدام المستأجر / Tenant usage figures | As a platform operator, I need a tenant's counts (staff, customers, sessions, documents, bytes, notifications of the last 30 days), so that I can see how large and how active an organisation is. | platform operator | POL-TENANT-006, -007 | plan §4 B.2 (`/{id}/usage`, `TenantUsageResponse`) |

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-TENANT-003 suspend and re-activate | suspension needs a `reason` (3–500) and records `suspendedAt` / `suspendedBy` / `suspensionReason`; re-activation clears them and cuts off tokens issued before (`TOKENS_INVALID_BEFORE`, C2); both publish an event (C1); SEC closes the tenant's sessions on suspension, NOTIF's job skips it (POL-TENANT-nnn (suspension reason), (token cut-off)) | plan §4 B.2, §5 C.1, C.2 |
| US-TENANT-002 list, view, search | responses carry the profile, suspension and branding fields; search gains `contactEmail`, `countryCode`, `suspendedAt` | plan §4 B.2, §7 E.2 |

TRACEABILITY — delta: US (edit) → POL-TENANT-001, -006 · US (recover) → POL-TENANT-006, -011, POL-nnn (admin recovery) · US (usage) → POL-TENANT-006, -007 · US-TENANT-003 (CHANGED) + POL-nnn (suspension reason), POL-nnn (token cut-off).

DEFERRED — delta
| Kind | US | Reason | Activation trigger |
|---|---|---|---|
| CHANGED | (rename, measure a tenant) | leaves DEFERRED: US (edit), US (usage) | this addendum |
| unchanged | (delete a tenant) | POL-TENANT-005 | — |
| NEW | (quotas, `ARCHIVED`, self-signup switch, per-tenant rate limits, platform-set tenant settings) | level 2 | plan §0 D2, §9 |

APPROVAL — delta: scope APPROVED by the platform owner on 2026-10-07 (plan header); the stories are PLANNED until the code exists and is compared with them.

### B.14 Schema — `TENANT/P2/db-script-tenant.md` and `registry-db-tenant.md` (V16, V17; V18 in E.6, V20 in C4.5)
The migrations are written **from this entry**. Everything is additive (nullable columns, CHECK constraints the existing rows satisfy); `MigrationNamingTest` applies.

Migration chain — delta
| Kind | Expected file | Package | Content |
|---|---|---|---|
| NEW | `V16__tenant_profile.sql` | B | profile columns on `CORE_TENANT` + `CHK_CORE_TENANT_LOCALE` |
| NEW | `V17__tenant_lifecycle.sql` | B / C.2 | suspension facts + `TOKENS_INVALID_BEFORE` on `CORE_TENANT` |

Table CORE_TENANT (ENT-TENANT-001) — delta
| Kind | DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | Constraint | Migration |
|---|---|---|---|---|---|---|---|---|---|
| NEW | DBF-TENANT-nnn | CONTACT_EMAIL | VARCHAR(255) | ENT-TENANT-001.contactEmail | REQ (update profile) | NULL | — | — | V16 |
| NEW | DBF-TENANT-nnn | CONTACT_PHONE | VARCHAR(30) | ENT-TENANT-001.contactPhone | REQ (update profile) | NULL | — | — | V16 |
| NEW | DBF-TENANT-nnn | COUNTRY_CODE | VARCHAR(2) | ENT-TENANT-001.countryCode | REQ (update profile) | NULL | — | — | V16 |
| NEW | DBF-TENANT-nnn | DEFAULT_LOCALE | VARCHAR(5) | ENT-TENANT-001.defaultLocale | REQ (update profile), (branding of the token's tenant) | NULL | — | `CHK_CORE_TENANT_LOCALE CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar','en'))` | V16 |
| NEW | DBF-TENANT-nnn | TIMEZONE | VARCHAR(64) | ENT-TENANT-001.timezone | REQ (update profile) | NULL | — | — | V16 |
| NEW | DBF-TENANT-nnn | NOTES | VARCHAR(1000) | ENT-TENANT-001.notes | REQ (update profile) | NULL | — | — | V16 |
| NEW | DBF-TENANT-nnn | SUSPENDED_AT | TIMESTAMPTZ | ENT-TENANT-001.suspendedAt | REQ (suspend with a reason) | NULL | — | — | V17 |
| NEW | DBF-TENANT-nnn | SUSPENDED_BY | VARCHAR(100) | ENT-TENANT-001.suspendedBy | REQ (suspend with a reason) | NULL | — | — | V17 |
| NEW | DBF-TENANT-nnn | SUSPENSION_REASON | VARCHAR(500) | ENT-TENANT-001.suspensionReason | REQ (suspend with a reason) | NULL | — | — | V17 |
| NEW | DBF-TENANT-nnn | TOKENS_INVALID_BEFORE | TIMESTAMPTZ | ENT-TENANT-001.tokensInvalidBefore | REQ (token cut-off) | NULL | — | — | V17 |
All columns are nullable without default, so V16 … V18 are additive on a populated table; `code`, `statusCode` and the 1.2.0 constraints are untouched.

```sql
-- V16__tenant_profile.sql (package B)
ALTER TABLE CORE_TENANT ADD COLUMN CONTACT_EMAIL  VARCHAR(255);
ALTER TABLE CORE_TENANT ADD COLUMN CONTACT_PHONE  VARCHAR(30);
ALTER TABLE CORE_TENANT ADD COLUMN COUNTRY_CODE   VARCHAR(2);
ALTER TABLE CORE_TENANT ADD COLUMN DEFAULT_LOCALE VARCHAR(5);
ALTER TABLE CORE_TENANT ADD COLUMN TIMEZONE       VARCHAR(64);
ALTER TABLE CORE_TENANT ADD COLUMN NOTES          VARCHAR(1000);
ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_LOCALE CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar','en'));

-- V17__tenant_lifecycle.sql (package B / C.2)
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENDED_AT          TIMESTAMPTZ;
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENDED_BY          VARCHAR(100);
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENSION_REASON     VARCHAR(500);
ALTER TABLE CORE_TENANT ADD COLUMN TOKENS_INVALID_BEFORE TIMESTAMPTZ;
```

registry-db — Tables delta: `CHANGED | CORE_TENANT | ENT-TENANT-001 | global | + DBF-TENANT-nnn (12 columns) | V16, V17, V18`. DB FIELD TRACEABILITY delta: the ten rows above (`DB Type`, `Null`, `Constraint / FK`, `Index —`, `Migration` as in the table). CONSTRAINTS delta: `CHK_CORE_TENANT_LOCALE CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar','en'))` (V16); the 1.2.0 constraints are untouched.

CHECK-constrained value sets — delta
| Key | Values | Constraint | Owner |
|---|---|---|---|
| `CORE_TENANT.DEFAULT_LOCALE` | `ar`, `en` (NULL allowed) | `CHK_CORE_TENANT_LOCALE` | TENANT |
| `CORE_TENANT.BRAND_COLOR` (E) | `^#[0-9A-Fa-f]{6}$` (NULL allowed) | `CHK_CORE_TENANT_BRAND_COLOR` | TENANT |
| `CORE_TENANT.STATUS_CODE` | unchanged: `ACTIVE`, `SUSPENDED` | `CHK_CORE_TENANT_STATUS` | TENANT |

### B.15 Audit action — `AUDIT/P1/srs-audit.md` §1 and `AUDIT/P0/module-registry-audit.md`
The deltas are action codes that other modules pass to `AuditApi.record` (XM-AUDIT-002, REQ-AUDIT-001). Every code matches `^[A-Z_]{3,64}$` (RULE-AUDIT-001), so no REQ, AC, RULE, ENT, SCR-REQ, API or error code of the audit module changes; its A6 `ACTION` table gains the rows once the callers exist. None of the codes was present in the code at main @ 19b19a4 on 2026-10-07.
| Kind | Action | Caller (plan package) | Entity / actor as planned | Source |
|---|---|---|---|---|
| NEW | `ADMIN_PASSWORD_RESET` | TENANT — `POST /api/v1/platform/tenants/{id}/admin-reset` (B) | `CORE_AUDIT_EVENT` row with actor = platform operator, target tenant = `{id}` | plan §4 B.2 (line 100) |

---

## Package C1 — tenant lifecycle events; sessions terminated; NOTIF job skips suspended tenants

### C1.1 TENANT — `srs-tenant.md`
Endpoint: the `PATCH /{id}/status` row of B.1 (publishes `TenantSuspendedEvent` / `TenantActivatedEvent` after commit).

| Kind | Id | Rule | Traces | Source |
|---|---|---|---|---|
| CHANGED | RULE-TENANT-006 — المستأجر المعلّق لا يُخدَم / A suspended tenant is not served | + on `TenantSuspendedEvent` SEC terminates every active session of the tenant (`SEC_ACTIVE_SESSION` rows were left open in 1.2.0), and NOTIF's retry / claim job does not claim rows of a suspended tenant (reads `TenantLookupApi.isActive(tenantId)`); the NOTIF status set is unchanged. | REQ-TENANT-010, REQ-TENANT-nnn (lifecycle events) | plan §5 C.1 |

| Kind | REQ / AC | Statement (EARS) → acceptance | Traces | Source |
|---|---|---|---|---|
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn — أحداث دورة حياة المستأجر / Tenant lifecycle events | When a tenant is suspended or activated and the transaction commits, the system shall publish `TenantSuspendedEvent` / `TenantActivatedEvent`. → SEC's listener closes the tenant's `SEC_ACTIVE_SESSION` rows; NOTIF's job claims none of its rows while suspended | US-TENANT-003; POL-TENANT-002; RULE-TENANT-006 | plan §5 C.1 |

Dependencies (A8) — delta
| Direction | Item | Source |
|---|---|---|
| published (events) | NEW `TenantSuspendedEvent(tenantId, code, reason, actor)`, `TenantActivatedEvent(tenantId, code, actor)` — after commit from `TenantService.updateStatus`; core catalogue 10 → 12 | plan §5 C.1 |
| exposed (XM-TENANT-001, CHANGED) | `TenantLookupApi` + `boolean isActive(Long tenantId)` — consumer NOTIF (claim job) | plan §5 C.1 |
| listeners (SEC, NOTIF) | SEC: `TenantSuspendedEvent` → terminate the tenant's sessions; NOTIF: claim job skips suspended tenants | plan §5 C.1 |

`registry-srs-tenant.md`: RULES delta `CHANGED | RULE-TENANT-006 | + sessions terminated on TenantSuspendedEvent; NOTIF job skips suspended tenants | §5 C.1`; REQUIREMENTS delta `NEW | REQ/AC-TENANT-nnn | Tenant lifecycle events | §5 C.1`; XM delta `CHANGED | XM-TENANT-001 | TenantLookupApi + boolean isActive(Long tenantId) (consumer NOTIF) | §5 C.1`; DEPENDENCIES delta `published | TenantSuspendedEvent, TenantActivatedEvent | events | §5 C.1`.

`business-policies-tenant.md`: `CHANGED | POL-TENANT-002 | + a suspension ends the tenant's open sessions (SEC listener on TenantSuspendedEvent) and pauses NOTIF's delivery job for it; the two statuses are unchanged (no ARCHIVED, level 2) | state | same | plan §5 C.1; §0 D2 | PLANNED (1.3.0)`.

`module-registry-tenant.md`: DEPENDENCIES `NEW | SEC | listener (SEC consumes tenant) | TenantSuspendedEvent → terminates the tenant's active sessions | plan §5 C.1`; `NEW | events | publishes | TenantSuspendedEvent(tenantId, code, reason, actor), TenantActivatedEvent(tenantId, code, actor) — after commit | plan §5 C.1`; EXPOSED SURFACE `CHANGED | TenantLookupApi + boolean isActive(Long tenantId) | NOTIF (claim job) | XM-TENANT-001 | plan §5 C.1`.

`platform-summary.md`: `NEW | Lifecycle events | TenantSuspendedEvent, TenantActivatedEvent published after commit; SEC terminates the tenant's sessions, NOTIF's claim job skips suspended tenants; core catalogue 10 → 12 events | plan §5 C.1`.

`P2/db-script-tenant.md` XM REGISTER / `registry-db-tenant.md` XM index: `CHANGED | XM-TENANT-001 | crossmodule read (exposed) | + TenantLookupApi.isActive(Long) → CORE_TENANT.STATUS_CODE (DBF-TENANT-005) | consumer NOTIF (claim job) | none | PLANNED (1.3.0)`.

### C1.2 NOTIF
`P0/business-policies-notif.md`
| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 15 | A suspended tenant's queued notifications are not delivered: the requeue / claim path skips rows of a tenant that is not ACTIVE (`TenantLookupApi.isActive`); they keep their status and are delivered once the tenant is active again. | NEW | plan §C.1; RULE-NOTIF-nnn |

`P0/platform-summary.md`: `NEW | Tenant lifecycle: TenantSuspendedEvent / TenantActivatedEvent on the bus; NOTIF's requeue / claim path skips rows of a suspended tenant (TenantLookupApi.isActive) and registers no listener | plan §C.1`.

`P0/module-registry-notif.md`: `NEW | DEPENDENCIES (tenant) | the requeue / claim path reads TenantLookupApi.isActive(tenantId) and leaves rows of a suspended tenant QUEUED (status set unchanged) | plan §C.1`; `NEW | DEPENDENCIES (events, consumed) | TenantSuspendedEvent / TenantActivatedEvent exist but NOTIF registers no listener for them | plan §C.1`.

`P0_5/prd-notif.md`: `CHANGED | US-NOTIF-001 / US-NOTIF-005 for a suspended tenant | operator | Queued notifications of a suspended tenant are not delivered while it is suspended (the job skips them, status unchanged) and resume once it is activated. | plan §C.1`.

`P1/srs.md`
| Kind | Section | Delta | Source |
|---|---|---|---|
| NEW | A4 rules | RULE-NOTIF-nnn Suspended tenants: the requeue / claim path does not claim or re-dispatch a `QUEUED` row whose tenant is not `ACTIVE` (`TenantLookupApi.isActive(tenantId)`, a new method of the tenant cross-module surface — **it does not exist today**, see Facts); the row keeps its status — the status set is unchanged (no `DEFERRED`) — and is delivered once the tenant is active again and the row is picked up as stale. | plan §C.1 |
| NEW | A7 events | `TenantSuspendedEvent(tenantId, code, reason, actor)` / `TenantActivatedEvent(tenantId, code, actor)` exist on `com.erp.events`; NOTIF registers no listener for them and terminates nothing — the job-side `isActive` check of RULE-NOTIF-nnn is the mechanism. | plan §C.1 |

`P1/registry-srs-notif.md`: `NEW | RULES | RULE-NOTIF-nnn | the requeue / claim path skips rows of a tenant that is not ACTIVE (TenantLookupApi.isActive); status set unchanged`; `NEW | DEPENDENCIES | TenantSuspendedEvent / TenantActivatedEvent | exist on the bus; NOTIF registers no listener — the job check is the mechanism`.

`P2/db-script.md` and `registry-db-notif.md`: `NOT IMPLEMENTED | NOTIF_LOG | no schema change for C.1: rows of a suspended tenant keep QUEUED; the job skips them by tenant status (TenantLookupApi.isActive) | plan §C.1`.

### C1.3 EVENTS (catalogue rows for the two tenant events)
`P0/platform-summary.md`
| # | Kind | Delta | Publisher | Consumers | Source |
|---|---|---|---|---|---|
| 1 | NEW | `TenantSuspendedEvent(tenantId, code, reason, actor)` in `com.erp.events` | `TenantService.updateStatus` (after commit) | SEC: terminates every active session of the tenant; NOTIF: the claim/retry job skips rows of suspended tenants (reads `TenantLookupApi.isActive(tenantId)`), no status change | plan C.1 |
| 2 | NEW | `TenantActivatedEvent(tenantId, code, actor)` in `com.erp.events` | `TenantService.updateStatus` (after commit) | `@TransactionalEventListener` consumers (none required by the plan) | plan C.1 |
| 4 | CHANGED | catalogue count 10 → 13 (12 with C1, 13 with D3) in this file, `../P1/srs-events.md` A3 and `governance/analysis/platform/PROJECT-OVERVIEW.md` ("10 core events") | — | — | plan C.1 ("count 10 → 12") + D.3 |

`P0/business-policies-events.md`: `1 | NEW (catalogue) | tenant lifecycle facts are events: suspension and re-activation are published after commit; SEC closes the tenant's sessions on suspension, NOTIF's job skips suspended tenants' rows — every policy above applies unchanged (the events carry tenantId, code, reason/actor, plain values) | plan C.1`.

`P0/module-registry-events.md` EVENT CATALOGUE — deltas
| Kind | Event | Payload (besides the envelope) | Publisher(s) | Consumers | Source |
|---|---|---|---|---|---|
| NEW | `TenantSuspendedEvent` | `tenantId`, `code`, `reason`, `actor` | TENANT `TenantService.updateStatus` (after commit) | SEC (session termination of the tenant), NOTIF (claim/retry job skips suspended tenants) | plan C.1 |
| NEW | `TenantActivatedEvent` | `tenantId`, `code`, `actor` | TENANT `TenantService.updateStatus` (after commit) | `@TransactionalEventListener` consumers | plan C.1 |
DEPENDENCIES — deltas: none. EXPOSED SURFACE — deltas: the new classes join the root package (XM-EVENTS-001).

`P0_5/prd-events.md`: `1 | NEW (instance of US-EVENTS-001/002) | TENANT publishes TenantSuspendedEvent(tenantId, code, reason, actor) and TenantActivatedEvent(tenantId, code, actor) from TenantService.updateStatus; SEC listens to the suspension to terminate the tenant's sessions; NOTIF's job skips suspended tenants | plan C.1`.

`P1/srs-events.md` §2 Event catalogue (A3) — deltas
| Kind | Event | Payload | Constructor | Publisher(s) | Consumers | Source |
|---|---|---|---|---|---|---|
| NEW | `TenantSuspendedEvent` | `tenantId`, `code`, `reason`, `actor` | explicit (the fact belongs to the suspended tenant; the publisher is the PLATFORM operator — RULE-EVENTS-008, same shape as `TenantCreatedEvent`) | TENANT `TenantService.updateStatus`, after commit | SEC: terminate every active session of the tenant; NOTIF: the claim/retry job skips rows of suspended tenants (reads `TenantLookupApi.isActive(tenantId)`) | plan C.1 |
| NEW | `TenantActivatedEvent` | `tenantId`, `code`, `actor` | explicit (as above) | TENANT `TenantService.updateStatus`, after commit | `@TransactionalEventListener` consumers | plan C.1 |
| CHANGED | catalogue count | 10 → 13 (A3 header, `../P0/platform-summary.md`, `governance/analysis/platform/PROJECT-OVERVIEW.md`) | — | — | — | plan C.1 |
§1 Endpoints: none (the module has no HTTP surface; the publishing endpoints belong to TENANT and SEC). §3–6: none — RULE-EVENTS-007 (plain values, no secrets) binds the new events.

`P1/registry-srs-events.md`: `NEW | TenantSuspendedEvent | tenantId, code, reason, actor | TENANT TenantService.updateStatus | SEC (session termination), NOTIF (job skips suspended tenants) | plan C.1`; `NEW | TenantActivatedEvent | tenantId, code, actor | TENANT TenantService.updateStatus | — | plan C.1`. No new REQ / AC / RULE / ENT / XM id.

`P2/db-script-events.md`, `registry-db-events.md`: none (no events table, no outbox); the publishers' tables (`CORE_TENANT` suspension facts, `SEC_USER` password flags) are TENANT's and SEC's P2 addenda.

---

## Package C2 — per-tenant token cut-off (`TOKENS_INVALID_BEFORE`, `/{id}/revoke-tokens`)

### C2.1 TENANT — `srs-tenant.md`
| Kind | Method | Path | Access | Body / result | Rules & errors | Traces | Source |
|---|---|---|---|---|---|---|---|
| NEW | POST | `/{id}/revoke-tokens` | `PLATFORM_TENANT_MANAGE` | → 200 `TenantResponse` | sets `TOKENS_INVALID_BEFORE = now()` and terminates the tenant's sessions; audit `TOKENS_REVOKED`; 404 `TENANT_NOT_FOUND` | REQ-TENANT-nnn (token cut-off); RULE-TENANT-nnn (token cut-off) | plan §5 C.2 |
| CHANGED | (filter) | every authenticated request | — | — | `TenantResolutionFilter`: a token whose `iat` < the tenant's `TOKENS_INVALID_BEFORE` → 401 `TENANT_TOKEN_REVOKED` (the JWT filter exposes `iat` on the authentication details — binding detail of C.2; **it does not today**, see Facts) | REQ-TENANT-nnn (token cut-off); RULE-TENANT-nnn (token cut-off) | plan §5 C.2 |
Activation (`PATCH /{id}/status` to `ACTIVE`, B.1) also sets the cut-off.

| Kind | Id | Rule | Traces | Source |
|---|---|---|---|---|
| NEW | RULE-TENANT-nnn — حدّ إبطال الرموز لكل مستأجر / Per-tenant token cut-off | The system shall refuse, with 401 `TENANT_TOKEN_REVOKED`, any access token of a tenant whose `iat` is before the tenant's `TOKENS_INVALID_BEFORE`; the cut-off is set by activation (RULE-TENANT-nnn (suspension reason)) and by `/{id}/revoke-tokens`, which also terminates the tenant's sessions. | REQ-TENANT-nnn (token cut-off) | plan §5 C.2; ADR-TENANT-002 |

| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW | `TENANT_TOKEN_REVOKED` | 401 | token `iat` < `TOKENS_INVALID_BEFORE` (written by the filter, like `TENANT_SUSPENDED`) | plan §5 C.2 |

| Kind | REQ / AC | Statement (EARS) → acceptance | Traces | Source |
|---|---|---|---|---|
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn — إبطال الرموز الصادرة / Token cut-off | When a tenant is activated or `/{id}/revoke-tokens` is called, the system shall set `TOKENS_INVALID_BEFORE = now()`; while a token's `iat` is before it, the system shall refuse the token. → a token issued before re-activation answers 401 `TENANT_TOKEN_REVOKED`; a fresh login works; audit `TOKENS_REVOKED` (TC-CORE-TENANT-036, -037) | US-TENANT-003, US-TENANT-nnn (revoke tokens); POL-TENANT-nnn (token cut-off); RULE-TENANT-nnn (token cut-off) | plan §5 C.2; ADR-TENANT-002 |

§9 Decisions applied: `ADR-TENANT-002 | per-tenant token cut-off (TOKENS_INVALID_BEFORE) instead of a jti denylist | PROPOSED → 1.3.0`.

`registry-srs-tenant.md`: `NEW | REQ/AC-TENANT-nnn | Token cut-off (TOKENS_INVALID_BEFORE, /{id}/revoke-tokens) | §5 C.2`; `NEW | RULE-TENANT-nnn | Per-tenant token cut-off (TENANT_TOKEN_REVOKED) | §5 C.2`; DECISIONS `ADR-TENANT-002 (PROPOSED → 1.3.0)`.

`business-policies-tenant.md`
| Kind | Policy | Statement (ar / en) | Pattern · Trigger | Rationale | Source | Status |
|---|---|---|---|---|---|---|
| NEW | POL-TENANT-nnn — إعادة التفعيل والإبطال يقطعان الرموز السابقة / Re-activation and revocation cut off earlier tokens | يجب على النظام ألا يقبل رمز دخول صدر قبل آخر إعادة تفعيل أو آخر إبطال صريح للمستأجر؛ يحفظ النظام حدًّا زمنيًا واحدًا لكل مستأجر. / The system shall refuse an access token issued before the tenant's last re-activation or explicit revocation; one cut-off instant is kept per tenant. | ubiquitous · every authenticated request | tokens issued before a suspension must not come back to life when the tenant is re-activated; a platform-wide denylist is avoided (ADR-TENANT-002) | plan §5 C.2 (`TOKENS_INVALID_BEFORE`, `/{id}/revoke-tokens`, `TENANT_TOKEN_REVOKED`) | PLANNED (1.3.0) |
RESOLVED DECISIONS — delta: `2 | Token cut-off vs jti denylist | per-tenant cut-off TOKENS_INVALID_BEFORE | plan §9 (recommendation), ADR pending code | ADR-TENANT-002` (also in `module-registry-tenant.md`).

`platform-summary.md`: `CHANGED | Tenant of a request | the resolution order is unchanged; a path-tenant path is added (/api/v1/public/tenants/{tenantCode}/branding, E), and a token whose iat is before the tenant's TOKENS_INVALID_BEFORE is refused 401 TENANT_TOKEN_REVOKED | plan §5 C.2, §7 E.2`.

`prd-tenant.md`: `NEW | US-TENANT-nnn | إبطال رموز مستأجر / Revoke a tenant's tokens | As a platform operator, I need to invalidate every token a tenant's users hold, so that a compromised or re-activated tenant starts from fresh logins. | platform operator | POL-TENANT-002, POL-nnn (token cut-off) | plan §5 C.2 (/{id}/revoke-tokens, TOKENS_INVALID_BEFORE)`; `US-TENANT-004 work inside one tenant (CHANGED) | a token issued before the tenant's cut-off is refused 401 TENANT_TOKEN_REVOKED; the isolation is proven by TenantScopedEntityTest and TenantIsolationIT (C3) | plan §5 C.2, C.3`.

Schema: `TOKENS_INVALID_BEFORE` row and V17 in B.14. `db-script-tenant.md` / `registry-db-tenant.md` DECISIONS: `ADR-TENANT-002 | TOKENS_INVALID_BEFORE on CORE_TENANT instead of a token denylist table | PROPOSED → 1.3.0`.

### C2.2 Audit action
| Kind | Action | Caller (plan package) | Entity / actor as planned | Source |
|---|---|---|---|---|
| NEW | `TOKENS_REVOKED` | TENANT — `POST /api/v1/platform/tenants/{id}/revoke-tokens` (C.2) | actor = platform operator | plan §5 C.2 (line 131) |

---

## Package C3 — automated isolation tests (C3 has minted REQ-TENANT-024 / AC-TENANT-024, RULE-TENANT-010, RULE-TENANT-011 itself)

The draft row this session held, for comparison with C3's own wording:
| Kind | REQ / AC | Statement (EARS) → acceptance | Traces | Source |
|---|---|---|---|---|
| NEW | (C3: REQ-TENANT-024 / AC-TENANT-024) — اختبارات العزل الآلية / Automated isolation tests | The system shall fail the build when an `@Entity` under `com.erp` neither extends `AuditableEntity` nor is on the documented global list (`Tenant`, `ModuleRegistry`, `ScreenRegistry`, `ActionRegistry`, `GlobalAuditableEntity` subclasses) — ArchUnit `TenantScopedEntityTest`; `TenantIsolationIT` provisions two tenants and proves per-module isolation. → both tests exist and are green in `mvn verify`; `gov-validate-backend-feature` checks that every `JdbcTemplate` statement names `TENANT_ID` (RULE-TENANT-008) | US-TENANT-004; POL-TENANT-007; RULE-TENANT-008 | plan §5 C.3 |

`platform-summary.md`: `NEW | Isolation proof | ArchUnit TenantScopedEntityTest (every @Entity extends AuditableEntity unless on the documented global list) and TenantIsolationIT; gov-validate-backend-feature checks TENANT_ID in every JdbcTemplate statement | plan §5 C.3`. `prd-tenant.md`: US-TENANT-004 CHANGED row in C2.1.

---

## Package C4 — idempotent provisioning (`Idempotency-Key`, `CORE_IDEMPOTENCY_KEY`, `com.erp.common.idempotency`)

### C4.1 TENANT — `srs-tenant.md`
| Kind | Method | Path | Access | Body / result | Rules & errors | Traces | Source |
|---|---|---|---|---|---|---|---|
| CHANGED | POST | `/api/v1/platform/tenants` | as before | optional header `Idempotency-Key` (≤ 64 chars) | same key + same body hash → the stored response replayed with header `Idempotent-Replayed: true`; same key + different body → 409 `IDEMPOTENCY_KEY_CONFLICT`; keys kept 24 h (RULE-TENANT-nnn (idempotent provisioning)) | REQ-TENANT-nnn (idempotent provisioning) | plan §5 C.4 |

| Kind | Id | Rule | Traces | Source |
|---|---|---|---|---|
| NEW | RULE-TENANT-nnn — تجهيز المستأجر متكرر بلا أثر / Idempotent provisioning | Where `POST /api/v1/platform/tenants` carries `Idempotency-Key`, the system shall store the response under (tenant, key, endpoint, request hash) and replay it for the same key and hash with `Idempotent-Replayed: true`; a different hash under the same key → 409 `IDEMPOTENCY_KEY_CONFLICT`; keys older than 24 h are purged by a scheduled job (audit-retention precedent). Without the header the 1.2.0 behaviour is unchanged. | REQ-TENANT-nnn (idempotent provisioning) | plan §5 C.4; ADR-TENANT-003 |

| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW (common mechanism, first consumer tenant) | `IDEMPOTENCY_KEY_CONFLICT` | 409 | same `Idempotency-Key`, different request body | plan §5 C.4 |

| Kind | Item | Delta | Source |
|---|---|---|---|
| NEW (common, registered in TENANT's P2 as first consumer) | `CORE_IDEMPOTENCY_KEY` (V20) | owned by `com.erp.common.idempotency`; tenant create is its first consumer; its DDL and its `TENANT_ID` are registered in `../P2/db-script-tenant.md` (and, since COMMON got an analysis folder, in `COMMON/P2/db-script-common.md`) | plan §5 C.4; ADR-TENANT-003 |
Dependencies: `consumed (common) | com.erp.common.idempotency (mechanism behind Idempotency-Key) | plan §5 C.4`.

| Kind | REQ / AC | Statement (EARS) → acceptance | Traces | Source |
|---|---|---|---|---|
| NEW | REQ-TENANT-nnn / AC-TENANT-nnn — تجهيز متكرر بلا أثر / Idempotent provisioning | Where a create request carries `Idempotency-Key`, the system shall replay the stored response for the same key and body and refuse a different body. → second identical POST answers the first response with `Idempotent-Replayed: true` and creates nothing; a different body under the same key → 409 (TC-CORE-TENANT-038, -039) | US-TENANT-001; POL-TENANT-004, POL-TENANT-nnn (idempotent); RULE-TENANT-nnn (idempotent provisioning) | plan §5 C.4; ADR-TENANT-003 |

§9: `ADR-TENANT-003 | idempotency keys stored in CORE_IDEMPOTENCY_KEY (common mechanism), first consumer tenant create | PROPOSED → 1.3.0`.

`registry-srs-tenant.md`: ENTITIES `(none) | CORE_IDEMPOTENCY_KEY | owned by com.erp.common.idempotency, no ENT-TENANT id; registered in ../P2/registry-db-tenant.md (DBF-TENANT-nnn for its TENANT_ID) | plan §5 C.4`; `NEW | REQ/AC-TENANT-nnn | Idempotent provisioning (Idempotency-Key) | §5 C.4`; `NEW | RULE-TENANT-nnn | Idempotent provisioning | §5 C.4`; DEPENDENCIES `mechanism (consumed) | com.erp.common.idempotency | common | §5 C.4`.

`business-policies-tenant.md`
| Kind | Policy | Statement (ar / en) | Pattern · Trigger | Rationale | Source | Status |
|---|---|---|---|---|---|---|
| NEW | POL-TENANT-nnn — التجهيز لا يتكرر بمفتاح التكرار / Provisioning is idempotent under an idempotency key | عند تكرار طلب إنشاء مستأجر بالمفتاح نفسه والمحتوى نفسه يجب على النظام إعادة الاستجابة المخزّنة دون إنشاء شيء، ورفض المحتوى المختلف تحت المفتاح نفسه. / When a tenant-create request is repeated with the same `Idempotency-Key` and body, the system shall replay the stored response and create nothing; a different body under the same key shall be refused. | optional · `POST /api/v1/platform/tenants` with the header | a retried provisioning call (timeout, client crash) must not create a second tenant or fail with a duplicate | plan §5 C.4 (`CORE_IDEMPOTENCY_KEY`, `IDEMPOTENCY_KEY_CONFLICT`, 24 h retention); ADR-TENANT-003 | PLANNED (1.3.0) |
RESOLVED DECISIONS — delta: `3 | Where idempotency keys live | DB table in common, 24 h retention, first consumer tenant create | plan §9 | ADR-TENANT-003` (also in `module-registry-tenant.md`).

`module-registry-tenant.md`: ENTITIES OWNED `NEW (owned by common, registered here) | CORE_IDEMPOTENCY_KEY (V20) | mechanism of com.erp.common.idempotency; tenant create is its first consumer; tenant-scoped (TENANT_ID FK) | plan §5 C.4; ADR-TENANT-003`; DEPENDENCIES `NEW | common | mechanism | com.erp.common.idempotency (Idempotency-Key, Idempotent-Replayed, IDEMPOTENCY_KEY_CONFLICT) | plan §5 C.4`; AUTO-DECISIONS — delta: `AUTO: CORE_IDEMPOTENCY_KEY is registered in TENANT's P2 although common owns it — FROM: plan §5 C.4 ("lives in com.erp.common.idempotency (mechanism) + tenant (first consumer)") — IF WRONG: move the DBF rows to the COMMON folder (which now exists: COMMON/P2/db-script-common.md); the physical names do not change.`

`platform-summary.md`: `NEW | Idempotency (common) | CORE_IDEMPOTENCY_KEY (V20) under com.erp.common.idempotency; first consumer POST /api/v1/platform/tenants with Idempotency-Key; 24 h retention | plan §5 C.4; ADR-TENANT-003`.

`prd-tenant.md`: `US-TENANT-001 provision a tenant (CHANGED) | the create request may carry Idempotency-Key: a retry with the same key and body replays the first answer (Idempotent-Replayed: true) and creates nothing; a different body under the same key → 409 IDEMPOTENCY_KEY_CONFLICT (POL-nnn (idempotent)) | plan §5 C.4`; TRACEABILITY `US-TENANT-001 (CHANGED) | + POL-nnn (idempotent)`; THE PROVISIONING STORY — delta: step 1 may carry `Idempotency-Key`; step 5 is unchanged; a later export (C5) is the mirror of step 4: every module writes its own rows through `TenantExportContributor`.

### C4.2 Schema — `TENANT/P2/db-script-tenant.md`, `registry-db-tenant.md`
Migration chain: `NEW — may slip (as C.4 was marked) | V20__core_idempotency_key.sql | C.4 | CORE_IDEMPOTENCY_KEY (owned by common, registered here)`.

Table CORE_IDEMPOTENCY_KEY — NEW (owned by `com.erp.common.idempotency`; first consumer tenant create)
| Kind | DBF id | Column | Type (postgresql16) | Nullable | Default | Constraint | Migration |
|---|---|---|---|---|---|---|---|
| NEW | — (common) | ID | BIGINT | NOT NULL | `SEQ_CORE_IDEMPOTENCY_KEY` | PK | V20 |
| NEW | DBF-TENANT-nnn | TENANT_ID | BIGINT | NOT NULL | — | FK → `CORE_TENANT(ID)` (`FK_CORE_IDEMPOTENCY_KEY_TENANT` and `IDX_CORE_IDEMPOTENCY_KEY_TENANT` per the `FK_<TABLE>_TENANT` / `IDX_<TABLE>_TENANT` convention — expected names, bound at creation; see Facts) | V20 |
| NEW | — (common) | IDEMPOTENCY_KEY | VARCHAR(64) | NOT NULL | — | part of `UQ_CORE_IDEMPOTENCY_KEY` | V20 |
| NEW | — (common) | ENDPOINT | VARCHAR(200) | NOT NULL | — | part of `UQ_CORE_IDEMPOTENCY_KEY` | V20 |
| NEW | — (common) | REQUEST_HASH | VARCHAR(64) | NOT NULL | — | — | V20 |
| NEW | — (common) | RESPONSE_STATUS | INT | NOT NULL | — | — | V20 |
| NEW | — (common) | RESPONSE_BODY | TEXT | NULL | — | — | V20 |
| NEW | — (common) | CREATED_AT | TIMESTAMPTZ | NOT NULL | now() | index on `CREATED_AT` (retention purge; name bound at creation) | V20 |
| NEW | — (common) | VERSION | BIGINT | NOT NULL | 0 | — | V20 |
Constraints and sequence: `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)` — tenant-leading, as every unique of a tenant-scoped table; `SEQ_CORE_IDEMPOTENCY_KEY`. Only its `TENANT_ID` carries a DBF-TENANT id (the discriminator register, 22 → 23 columns); the other columns are `common`'s, listed for the migration. Plan §5 C.4; ADR-TENANT-003.

```sql
-- V20__core_idempotency_key.sql (package C.4) — columns exactly as plan §5 C.4
CREATE SEQUENCE SEQ_CORE_IDEMPOTENCY_KEY START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE TABLE CORE_IDEMPOTENCY_KEY (
  ID              BIGINT        NOT NULL,
  TENANT_ID       BIGINT        NOT NULL,
  IDEMPOTENCY_KEY VARCHAR(64)   NOT NULL,
  ENDPOINT        VARCHAR(200)  NOT NULL,
  REQUEST_HASH    VARCHAR(64)   NOT NULL,
  RESPONSE_STATUS INT           NOT NULL,
  RESPONSE_BODY   TEXT,
  CREATED_AT      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  VERSION         BIGINT        NOT NULL DEFAULT 0
);
-- PK, FK_CORE_IDEMPOTENCY_KEY_TENANT, IDX_CORE_IDEMPOTENCY_KEY_TENANT, UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT),
-- index on CREATED_AT — PK / index names bound at creation.
```

`registry-db-tenant.md`: Tables `NEW | CORE_IDEMPOTENCY_KEY | — (owned by common; registered here) | tenant-scoped (TENANT_ID FK) | DBF-TENANT-nnn (its TENANT_ID only) | V20`; DB FIELD `NEW | DBF-TENANT-nnn | CORE_IDEMPOTENCY_KEY.TENANT_ID | BIGINT | NOT NULL | FK_CORE_IDEMPOTENCY_KEY_TENANT (expected, convention); UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT) | IDX_CORE_IDEMPOTENCY_KEY_TENANT (expected, convention) | V20`; CONSTRAINTS `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)` (V20); Sequences `SEQ_CORE_IDEMPOTENCY_KEY` (V20; owned by common). Counts after V20: 23 discriminator columns (22 + `CORE_IDEMPOTENCY_KEY`); 15 tenant-leading unique constraints (+ `UQ_CORE_IDEMPOTENCY_KEY`); `TenantSchemaIntegrationTest`'s asserted numbers move with V20 and are re-bound when it is written. Decisions: `ADR-TENANT-003 | CORE_IDEMPOTENCY_KEY in common, 24 h retention | PROPOSED → 1.3.0`.

### C4.3 COMMON (`governance/analysis/modules/COMMON/**`)
`P0/platform-summary.md`: `8 | NEW | com.erp.common.idempotency (mechanism) + tenant (first consumer, POST /api/v1/platform/tenants): table CORE_IDEMPOTENCY_KEY (ID BIGINT PK, TENANT_ID BIGINT NOT NULL FK, IDEMPOTENCY_KEY VARCHAR(64) NOT NULL, ENDPOINT VARCHAR(200) NOT NULL, REQUEST_HASH VARCHAR(64) NOT NULL, RESPONSE_STATUS INT NOT NULL, RESPONSE_BODY TEXT, CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT now(), VERSION BIGINT NOT NULL DEFAULT 0), UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT), SEQ_CORE_IDEMPOTENCY_KEY, index on CREATED_AT; expected migration V20__core_idempotency_key.sql (number re-derived at creation); header Idempotency-Key (optional, ≤ 64); same key + same body hash → the stored response replayed with header Idempotent-Replayed: true; same key + different body → 409 IDEMPOTENCY_KEY_CONFLICT; retention 24 h by the scheduled-job pattern (audit retention precedent); ADR-TENANT-003 | plan C.4, §9, §11`.

`P0/module-registry-common.md`: EXPOSED SURFACE `NEW | com.erp.common.idempotency (mechanism) | first consumer: tenant (POST /api/v1/platform/tenants); header Idempotency-Key; 409 IDEMPOTENCY_KEY_CONFLICT; Idempotent-Replayed: true on replay; 24 h retention job | plan C.4; ADR-TENANT-003`. ENTITIES OWNED `idempotency key record (CORE_IDEMPOTENCY_KEY, tenant-scoped: extends AuditableEntity by POL-COMMON-006; columns below) | NEW, PRIVATE to the mechanism | plan C.4`. DEPENDENCIES `tenant | HARD FK (TENANT_ID → CORE_TENANT) | the idempotency table is tenant-scoped; "COMMON depends on nothing in com.erp" would no longer hold for the idempotency sub-package if it imports TenantContext — to be recorded against the code | plan C.4`.

`P0/business-policies-common.md`: `2 | NEW | an idempotent request is replayed, never re-executed: with header Idempotency-Key (≤ 64), the same key and body hash answer the stored response with Idempotent-Replayed: true; the same key with another body is refused 409 IDEMPOTENCY_KEY_CONFLICT; keys are kept 24 h; first consumer POST /api/v1/platform/tenants | plan C.4; ADR-TENANT-003`.

`P0_5/prd-common.md`: `2 | NEW (story) | As a platform operator retrying a tenant creation after a timeout, I need to resend the same request with the same Idempotency-Key and receive the original response (Idempotent-Replayed: true) instead of a duplicate tenant or a TENANT_CODE_DUPLICATE; a different body under the same key is refused 409 IDEMPOTENCY_KEY_CONFLICT; keys expire after 24 h — mechanism com.erp.common.idempotency, first consumer POST /api/v1/platform/tenants | plan C.4; ADR-TENANT-003`.

`P1/srs-common.md`: §1 Endpoints `CHANGED (consumer: tenant) | POST | /api/v1/platform/tenants | as before (PLATFORM_TENANT_MANAGE) | optional header Idempotency-Key (≤ 64 chars); a replay answers the stored status and body with header Idempotent-Replayed: true | plan C.4`; §2 `NEW | Idempotency (com.erp.common.idempotency): with header Idempotency-Key, the system stores (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT) → REQUEST_HASH, RESPONSE_STATUS, RESPONSE_BODY; the same key + the same request hash replays the stored response (Idempotent-Replayed: true); the same key + a different hash → 409 IDEMPOTENCY_KEY_CONFLICT; rows older than 24 h are deleted by a scheduled job on the audit-retention pattern (run() always available, the trigger only when the application enables scheduling) | plan C.4; ADR-TENANT-003`; §3 `NEW | IDEMPOTENCY_KEY_CONFLICT | 409 | the same Idempotency-Key is reused with a different request body | plan C.4`; §5 `NEW | idempotency key record (CORE_IDEMPOTENCY_KEY) | tenant-scoped (extends ENT-COMMON-002): id, tenantId, idempotencyKey (≤ 64), endpoint (≤ 200), requestHash (≤ 64), responseStatus, responseBody, createdAt, version | plan C.4`.

`P1/registry-srs-common.md`: Rules `NEW | idempotency | Idempotency-Key header (≤ 64), replay with Idempotent-Replayed: true, 409 IDEMPOTENCY_KEY_CONFLICT, 24 h retention; first consumer POST /api/v1/platform/tenants`; Entities `idempotency key record (CORE_IDEMPOTENCY_KEY) | NEW, tenant-scoped (ENT-COMMON-002)`; Error codes NEW `IDEMPOTENCY_KEY_CONFLICT` (409).

`P2/db-script-common.md` — Table CORE_IDEMPOTENCY_KEY (NEW; expected migration `V20__core_idempotency_key.sql`)
| Kind | Column | Type (postgresql16) | Nullable | Default | Constraint / index | Source |
|---|---|---|---|---|---|---|
| NEW | ID | BIGINT | NOT NULL | `SEQ_CORE_IDEMPOTENCY_KEY` | `PK_CORE_IDEMPOTENCY_KEY` | plan C.4 |
| NEW | TENANT_ID | BIGINT | NOT NULL | — | `FK_CORE_IDEMPOTENCY_KEY_TENANT` → `CORE_TENANT (ID)`; `IDX_CORE_IDEMPOTENCY_KEY_TENANT` (§1c convention) | plan C.4 (`TENANT_ID BIGINT NOT NULL FK`) |
| NEW | IDEMPOTENCY_KEY | VARCHAR(64) | NOT NULL | — | part of `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)` | plan C.4 |
| NEW | ENDPOINT | VARCHAR(200) | NOT NULL | — | part of the unique | plan C.4 |
| NEW | REQUEST_HASH | VARCHAR(64) | NOT NULL | — | SHA-256 hex of the body (`TokenHasher.sha256Hex`, RULE-COMMON-009) | plan C.4 |
| NEW | RESPONSE_STATUS | INT | NOT NULL | — | — | plan C.4 |
| NEW | RESPONSE_BODY | TEXT | NULL | — | — | plan C.4 |
| NEW | CREATED_AT | TIMESTAMPTZ | NOT NULL | `now()` | index on `CREATED_AT` (retention scan, 24 h) | plan C.4 |
| NEW | VERSION | BIGINT | NOT NULL | 0 | §1b convention | plan C.4 |
| NEW | CREATED_BY, UPDATED_BY, UPDATED_AT | VARCHAR(100), VARCHAR(100), TIMESTAMPTZ | NULL | — | §1a convention (SEC/MDL/CORE_* family) — not listed in the plan's column list; required by ENT-COMMON-002 (`AuditableEntity`), to be confirmed against the migration | §1a; POL-COMMON-005 |
Sequence `SEQ_CORE_IDEMPOTENCY_KEY`. Retention: rows older than 24 h deleted by a scheduled job on the audit-retention pattern (docs/DEVIATIONS.md [10] "Retention job"). First consumer: `POST /api/v1/platform/tenants`. Decision: ADR-TENANT-003.
Open point between the two registers: the TENANT P2 table (plan §5 C.4, nine columns) has no `CREATED_BY` / `UPDATED_BY` / `UPDATED_AT`; the COMMON P2 table adds them because the entity would extend `AuditableEntity`. The package decides (entity base class → columns) and records the choice in both registers.

`P2/registry-db-common.md`: Tables `CORE_IDEMPOTENCY_KEY | idempotency key record (tenant-scoped, ENT-COMMON-002) | NEW: ID, TENANT_ID, IDEMPOTENCY_KEY VARCHAR(64), ENDPOINT VARCHAR(200), REQUEST_HASH VARCHAR(64), RESPONSE_STATUS INT, RESPONSE_BODY TEXT, CREATED_AT TIMESTAMPTZ DEFAULT now(), VERSION; UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT), SEQ_CORE_IDEMPOTENCY_KEY, index on CREATED_AT; audit columns per §1a convention (to be confirmed against the migration) | expected V20__core_idempotency_key.sql`; XM index `(to be assigned when written) | HARD-FK (FK_CORE_IDEMPOTENCY_KEY_TENANT) | CORE_TENANT | TENANT | ACTIVE`; Sequences `SEQ_CORE_IDEMPOTENCY_KEY`.

---

## Package C5 — tenant data export (**may slip**)

### C5.1 TENANT — `srs-tenant.md`
| Kind | Method | Path | Access | Body / result | Rules & errors | Traces | Source |
|---|---|---|---|---|---|---|---|
| NEW — **may slip** | POST | `/{id}/export` | `PLATFORM_TENANT_MANAGE` | → 200 `{ fileId, downloadToken }` (single-use token, existing FILE mechanism) | synchronous; every `TenantExportContributor` writes its tenant rows as CSV into a ZIP stored as a PRIVATE `FILE_DOCUMENT` (`ownerType = CORE_TENANT`, `ownerId = {id}`, `moduleCode = TENANT`) in the PLATFORM tenant; > `erp.core.tenant.export.max-rows` (default 200 000) → 422 `TENANT_EXPORT_TOO_LARGE`; a second export of the same tenant while one runs → 409 `TENANT_EXPORT_IN_PROGRESS`; audit `TENANT_EXPORTED` (RULE-TENANT-nnn (export)) | REQ-TENANT-nnn (export) | plan §5 C.5 |

| Kind | Id | Rule | Traces | Source |
|---|---|---|---|---|
| NEW — **may slip** | RULE-TENANT-nnn — التصدير محدود وبلا أسرار / Export is bounded and secret-free | The system shall export a tenant synchronously through every `TenantExportContributor` (one per module, `moduleCode()` + `export(TenantExport ctx)`), never the password hash and never file bytes (file metadata only); more than `erp.core.tenant.export.max-rows` rows → 422 `TENANT_EXPORT_TOO_LARGE`; a concurrent export of the same tenant → 409 `TENANT_EXPORT_IN_PROGRESS` (in-memory guard keyed by tenant id). | REQ-TENANT-nnn (export) | plan §5 C.5 |

| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW — **may slip** | `TENANT_EXPORT_TOO_LARGE` | 422 | export exceeds `erp.core.tenant.export.max-rows` | plan §5 C.5 |
| NEW — **may slip** | `TENANT_EXPORT_IN_PROGRESS` | 409 | a second export of the same tenant while one runs | plan §5 C.5 |

Dependencies: `exposed (XM-TENANT-nnn, NEW — may slip) | SPI com.erp.tenant.TenantExportContributor { String moduleCode(); void export(TenantExport ctx) } — implemented by every core module, mirrors XM-TENANT-002 | plan §5 C.5`; config `NEW erp.core.tenant.export.max-rows (default 200 000)`.

| Kind | REQ / AC | Statement (EARS) → acceptance | Traces | Source |
|---|---|---|---|---|
| NEW — **may slip** | REQ-TENANT-nnn / AC-TENANT-nnn — تصدير بيانات المستأجر / Tenant data export | When a platform operator posts `/{id}/export`, the system shall collect every module's rows of that tenant as CSV, zip them into a PRIVATE document of the PLATFORM tenant and return a single-use download token. → the ZIP holds one CSV per contributing module, no password hash, no file bytes; oversize 422; concurrent 409 | US-TENANT-nnn (export); POL-TENANT-nnn (export); RULE-TENANT-nnn (export) | plan §5 C.5 |

`registry-srs-tenant.md`: `NEW — may slip | REQ/AC-TENANT-nnn | Tenant data export (/{id}/export) | §5 C.5`; `NEW — may slip | RULE-TENANT-nnn | Export is bounded and secret-free | §5 C.5`; XM `NEW — may slip | XM-TENANT-nnn | SPI TenantExportContributor { String moduleCode(); void export(TenantExport ctx) } (exposed; implemented by every core module) | §5 C.5`.

`business-policies-tenant.md`
| Kind | Policy | Statement (ar / en) | Pattern · Trigger | Rationale | Source | Status |
|---|---|---|---|---|---|---|
| NEW — **may slip** | POL-TENANT-nnn — تصدير بيانات المستأجر بلا أسرار / A tenant's data can be exported, never its secrets | يجب على النظام تمكين مشغّل المنصة من تصدير بيانات مستأجر كملف واحد تكتبه كل وحدة بنفسها، دون كلمات المرور المجزّأة أو محتوى الملفات، وبحدّ أقصى للحجم. / The system shall let a platform operator export a tenant's data as one archive each module writes itself, never the password hashes or the file bytes, within a configured size cap. | event · `/{id}/export` | portability of a tenant's data is a platform duty; the shared schema makes it a filtered copy by `TENANT_ID` (ADR-TENANT-001 consequences) | plan §5 C.5 (`TenantExportContributor`, `erp.core.tenant.export.max-rows`, `TENANT_EXPORT_TOO_LARGE`, `TENANT_EXPORT_IN_PROGRESS`) | PLANNED (1.3.0, may slip to 1.4.0) |

`module-registry-tenant.md`: Owned runtime surface — delta: + SPI `TenantExportContributor` / `TenantExport`; + in-memory export guard keyed by tenant id. EXPOSED SURFACE `NEW — may slip | com.erp.tenant.TenantExportContributor { String moduleCode(); void export(TenantExport ctx) } | every core module (CSV streams of its tenant rows; never password hashes, never file bytes) | XM-TENANT-nnn | plan §5 C.5`. CONFIGURATION `NEW — may slip | erp.core.tenant.export.max-rows | 200 000 | plan §5 C.5`.

`platform-summary.md`: `NEW — may slip | Export SPI | TenantExportContributor beside the provisioning SPI; a tenant's data as a ZIP of per-module CSVs, PRIVATE document of the PLATFORM tenant | plan §5 C.5`.

`prd-tenant.md`: `NEW — may slip | US-TENANT-nnn | تصدير بيانات مستأجر / Export a tenant's data | As a platform operator, I need one downloadable archive of a tenant's data written by every module, so that the organisation can take its data with it. | platform operator | POL-nnn (export) | plan §5 C.5 (/{id}/export, TenantExportContributor)`.

`P2/db-script-tenant.md` XM REGISTER / `registry-db-tenant.md`: `NEW — may slip | XM-TENANT-nnn | SPI (exposed) | TenantExportContributor.export(TenantExport) | every core module reads its own rows by TENANT_ID | none (CSV streams; ZIP as a PRIVATE FILE_DOCUMENT of PLATFORM) | PLANNED`. Cascade: FILE `FILE_DOCUMENT` rows with `OWNER_TYPE = CORE_TENANT`, `MODULE_CODE = TENANT` (export ZIP in the PLATFORM tenant) — data, not schema.

### C5.2 Audit action
| NEW | `TENANT_EXPORTED` | TENANT — `POST /api/v1/platform/tenants/{id}/export` (C.5) | actor = platform operator | plan §5 C.5 (line 146) |

---

## Package C6 — `ScopedValue` for `TenantContext` (spike, go / no-go; **may slip**)

`platform-summary.md`: `spike — may slip | TenantContext on ScopedValue | same public API (current / find / require / set / clear / runAs / callAs); go / no-go decided by ADR-TENANT-004 after the spike (touches events and notif) | plan §0 D6, §5 C.6`.
`prd-tenant.md`: `US-TENANT-008 run system work as a tenant (CHANGED) | TenantContext may move to ScopedValue behind the same API — go / no-go by ADR-TENANT-004 (may slip) | plan §0 D6, §5 C.6`.
`module-registry-tenant.md`: Owned runtime surface — delta: `TenantContext` keeps its public API whatever ADR-TENANT-004 decides. RESOLVED DECISIONS `4 | ScopedValue for TenantContext | pending the spike (may slip) | ADR-TENANT-004 (PROPOSED)` (also `business-policies-tenant.md` row 4: `decided by the spike (go / no-go) — may slip | plan §0 D6, §5 C.6 | ADR-TENANT-004`).
`registry-srs-tenant.md` DECISIONS: `ADR-TENANT-004 (PROPOSED, pending the spike, may slip)`.

---

## Package D1 — `PasswordPolicy` (SEC) — the "not common" notes held in COMMON's files

SEC's own rows (`PasswordPolicy`, `erp.core.security.password-policy.*`, `SEC-400-PASSWORD-POLICY`, `V19__sec_user_profile.sql`) are the implementing run's (package D). What COMMON's addenda said, so that nothing lands in `com.erp.common` by mistake:

- `P0/platform-summary.md`: `9 | NEW (not common) | PasswordPolicy lives in SEC (com.erp.sec.domain), not in com.erp.common: length 8–200, at least one letter and one digit, property erp.core.security.password-policy.*, error SEC-400-PASSWORD-POLICY | plan D.1`.
- `P0/module-registry-common.md`: Not in common: `PasswordPolicy` (`com.erp.sec.domain`; `erp.core.security.password-policy.*`; `SEC-400-PASSWORD-POLICY`) — plan D.1.
- `P0/business-policies-common.md`: `3 | (not common) | the password policy (8–200 characters, a letter and a digit, erp.core.security.password-policy.*, SEC-400-PASSWORD-POLICY) is a SEC domain rule (com.erp.sec.domain.PasswordPolicy), not a foundation policy | plan D.1`.
- `P0_5/prd-common.md`: `3 | (not common) | the password policy story belongs to SEC (PasswordPolicy in com.erp.sec.domain) | plan D.1`.
- `P1/srs-common.md` §2: `(not common) | PasswordPolicy (com.erp.sec.domain): length 8–200, at least one letter and one digit; erp.core.security.password-policy.* | plan D.1`; §3: `(not common) | SEC-400-PASSWORD-POLICY | 400 | password outside the SEC policy | plan D.1`.
- `P1/registry-srs-common.md`: Not common: `SEC-400-PASSWORD-POLICY` (SEC `PasswordPolicy`, plan D.1).
- `P2/db-script-common.md`: Not common: the SEC password columns of plan D.1 (`SEC_USER.PASSWORD_CHANGE_REQUIRED_FL`, `PASSWORD_CHANGED_AT`, expected `V19__sec_user_profile.sql`) belong to `../../SEC/P2/`.
- TENANT consumes it through `/{id}/admin-reset` (B.1, B.3: referenced `SEC-400-PASSWORD-POLICY`).

---

## Package D2 — admin-set password, own password, photo, staff `/me` (SEC endpoints): the audit codes

`AUDIT/P1/srs-audit.md` §1 / `AUDIT/P0/module-registry-audit.md` (header text in B.15)
| Kind | Action | Caller (plan package) | Entity / actor as planned | Source |
|---|---|---|---|---|
| NEW | `PASSWORD_SET_BY_ADMIN` | SEC — `PUT /api/v1/sec/users/{id}/password` (D.2) | no secret in the row (RULE-AUDIT-004 applies) | plan §6 D.2 (line 176), D.3 (line 192) |
| NEW | `PASSWORD_CHANGED` | SEC — `PUT /api/v1/sec/me/password` (D.2) | actor = the account | plan §6 D.2 (line 179), D.3 (line 192) |
| NEW | `PROFILE_PHOTO_CHANGED` | SEC — staff profile photo (D) | actor = the account | plan §6 D.3 (line 192) |
`AUDIT` §2 Everything else: endpoints, entity, table, rules, error codes, permissions, report, retention — none; nothing else changes in AUDIT for 1.3.0 (all seven codes of B.15, C2.2, C5.2, D2, E.9).

---

## Package D3 — `UserPasswordChangedEvent` → NOTIF `STAFF_PASSWORD_CHANGED`

### D3.1 NOTIF
`P0/business-policies-notif.md`: `16 | A staff user is notified when their password is changed (by an administrator or by themselves): SEC publishes UserPasswordChangedEvent(userId, byAdmin) and NOTIF dispatches the new STAFF_PASSWORD_CHANGED template, seeded for every tenant and copied to new ones. | NEW | plan §D.3`.
`P0/platform-summary.md`: `NEW | UserPasswordChangedEvent (SEC) → STAFF_PASSWORD_CHANGED template (seeded, copied by provisioning) — NOTIF's first consumption of a foreign event | plan §D.3`.
`P0/module-registry-notif.md`: `NEW | DEPENDENCIES (events, consumed) | UserPasswordChangedEvent(userId, byAdmin) → dispatch of STAFF_PASSWORD_CHANGED | plan §D.3`; `NEW | SEEDS | template STAFF_PASSWORD_CHANGED (seed script per plan §11); copied to new tenants by NotifTenantProvisioningContributor | plan §D.3, §11`.
`P0_5/prd-notif.md`: `NEW | Password-changed notice | staff user | When an administrator sets a user's password (PUT /api/v1/sec/users/{id}/password) or the user changes it, SEC publishes UserPasswordChangedEvent(userId, byAdmin) and NOTIF sends the STAFF_PASSWORD_CHANGED template (seeded in every tenant, copied to new ones). | plan §D.1, §D.3`.

`P1/srs.md`
| Kind | Section | Delta | Source |
|---|---|---|---|
| NEW | A3 / seeds | template `STAFF_PASSWORD_CHANGED` (AR/EN, subject + body) seeded for every tenant existing at migration time by a NOTIF seed script (plan §11: `V21__notif_seed_password_changed.sql`, or part of the V19 package — the number is re-derived from the directory when written); later tenants receive it through `NotifTenantProvisioningContributor`, which already copies every PLATFORM template (no code change). | plan §D.3, §11; notif/tenant/NotifTenantProvisioningContributor.java:42-49 |
| NEW | A7 events consumed | `UserPasswordChangedEvent(userId, byAdmin)` (published by SEC after an admin password set or a self-service change) → a NOTIF listener dispatches `STAFF_PASSWORD_CHANGED` to `userId` (`channelHint` and `variables` as implemented). This is NOTIF's first consumer of a foreign event: the 1.2.0 row "consumes no foreign event" becomes "consumes `UserPasswordChangedEvent` of the shared bus"; ADR-NOTIF-001 (no CU `NotificationEvent` relay) stands. | plan §D.3 (D.1 `PUT /api/v1/sec/users/{id}/password`) |

`P1/registry-srs-notif.md`: `NEW | SEEDS | template STAFF_PASSWORD_CHANGED | seed migration per plan §11; copied to new tenants by the provisioning contributor`; `NEW | DEPENDENCIES | UserPasswordChangedEvent(userId, byAdmin) (SEC, shared bus) | consumed → dispatch STAFF_PASSWORD_CHANGED; NOTIF's first foreign-event consumer`.
`P2/db-script.md`: `NEW | NOTIF_TEMPLATE (seed) | STAFF_PASSWORD_CHANGED (AR/EN name, subject and body) inserted for every tenant existing at migration time (CROSS JOIN CORE_TENANT, as V11 §6 does); later tenants copy PLATFORM through the provisioning contributor | a NOTIF seed script of its own (plan §11: V21__notif_seed_password_changed.sql) or part of the V19 package — the number is re-derived from the directory listing when written`.
`P2/registry-db-notif.md`: `NEW | SEEDS | NOTIF_TEMPLATE STAFF_PASSWORD_CHANGED | one row per tenant existing at migration time; later tenants copy PLATFORM | plan §11: V21__notif_seed_password_changed.sql or part of the V19 package — number re-derived when written`.

### D3.2 EVENTS
`P0/platform-summary.md`: `3 | NEW | UserPasswordChangedEvent(userId, byAdmin) in com.erp.events | SEC password endpoints: PUT /api/v1/sec/users/{id}/password (byAdmin = true), PUT /api/v1/sec/me/password | NOTIF template STAFF_PASSWORD_CHANGED (e-mail to the user) | plan D.2, D.3`.
`P0/business-policies-events.md`: `2 | NEW (catalogue) | a password change is an event (userId, byAdmin) — never the password, per POL-EVENTS-004 | plan D.3; §1 rule 7`.
`P0/module-registry-events.md`: `NEW | UserPasswordChangedEvent | userId, byAdmin | SEC password endpoints (PUT /api/v1/sec/users/{id}/password, PUT /api/v1/sec/me/password) | NOTIF (STAFF_PASSWORD_CHANGED template) | plan D.2, D.3`.
`P0_5/prd-events.md`: `2 | NEW (instance of US-EVENTS-001/002) | SEC publishes UserPasswordChangedEvent(userId, byAdmin) from the admin-set and self-change password endpoints; NOTIF mails STAFF_PASSWORD_CHANGED | plan D.2, D.3`.
`P1/srs-events.md` §2: `NEW | UserPasswordChangedEvent | userId, byAdmin | no-arg capture (published inside the caller's tenant) | SEC PUT /api/v1/sec/users/{id}/password (byAdmin = true), PUT /api/v1/sec/me/password | NOTIF template STAFF_PASSWORD_CHANGED | plan D.2, D.3`; §3: RULE-EVENTS-007 (plain values, no secrets) binds it — `UserPasswordChangedEvent` carries no password (plan §1 rule 7).
`P1/registry-srs-events.md`: `NEW | UserPasswordChangedEvent | userId, byAdmin | SEC password endpoints | NOTIF (STAFF_PASSWORD_CHANGED) | plan D.3`.

---

## Package D4 — `FileImageStoreApi` (shared image store for user photos and tenant logos)

### D4.1 FILE — `P1/srs.md`
§1 In-process surface — الواجهة البينية
| Kind | Id | Item | Statement | Source |
|---|---|---|---|---|
| NEW | XM-FILE-nnn | `com.erp.file.crossmodule.FileImageStoreApi` (inbound) | `StoredImage storePublicImage(ImageStoreRequest)` — request `ownerType`, `ownerId`, `moduleCode`, `bytes`, `contentType`, `fileName`, `maxBytes`, `allowedTypes`; result `documentId`, `publicUrl`. `void discard(Long documentId)`. Validates type and size, stores through the configured `StorageProvider`, publishes the document (`VISIBILITY = PUBLIC`, random slug) on the step-07 public path; no new table. A validation failure is reported to the caller as a FILE result the caller maps to its own code (`SEC-400-PHOTO-INVALID`, `TENANT_LOGO_INVALID`), never a raw exception. | plan §D.4; ADR-FILE-008 (draft below) |
| NEW | — | Consumer SEC | User photo: `ownerType = SEC_USER`, `ownerId` = user id, `moduleCode = SEC`, via `PUT /api/v1/sec/me/photo` (multipart `file` → `{ photoUrl }`); replacing a photo discards the previous document. | plan §D.2, §D.4 |
| NEW | — | Consumer TENANT | Tenant logo: `ownerType = CORE_TENANT`, `ownerId` = tenant id, `moduleCode = TENANT`, via `PUT` / `DELETE /api/v1/platform/tenants/{id}/logo` (`PLATFORM_TENANT_MANAGE`). The document is written inside `TenantContext.callAs(tenantId)`, so its row belongs to the target tenant (`FILE_DOCUMENT.TENANT_ID = {id}`) and its public URL is `/api/v1/public/files/{thatTenantCode}/{slug}`; `CORE_TENANT.LOGO_FILE_ID` keeps a soft reference. | plan §E.1, §E.2; tenant/TenantContext.java:79 |

§2 Image rules — قواعد الصور
| Kind | Rule | Statement | Source |
|---|---|---|---|
| NEW | Allowed image types | `image/png`, `image/jpeg`, `image/webp`; `image/svg+xml` for logos only, sanitised: reject `<script`, `on*=` attributes and external hrefs. | plan §D.4 |
| NEW | Image size | ≤ 1 MB for a photo and for a logo (`maxBytes` passed by the caller); the server never resizes (the frontend constrains the preview). | plan §D.4 |
| NEW | Public category | The 1.2.0 publish rule is unchanged: a PUBLIC document needs a category with `ALLOW_PUBLIC = TRUE`, so the image store writes each image into such a category. The category code(s) and their seed are fixed in `P2/db-script.md` → 1.3.0 addendum before the migration and the code are written; until then this row is the **open item of D.4**. | file/domain/FileDocumentDomain.java:57-61; plan §D.4 |

§4 Frontend-relevant: `NEW | Photo and logo URLs | A user's photoUrl and a tenant's logoUrl are ordinary public file URLs (§1); they are rendered without a token. No FILE screen, page code or permission changes; nothing is added to P2_5 for FILE.`

Open points of D.4 (from ADR-FILE-008 draft and the rows above) that the package must settle and record:
1. **`ALLOW_PUBLIC` category for the image store** — which `FILE_CATEGORY` code(s) the store writes into, their tenant scope (seeded per tenant by provisioning, or resolved at store time) and the seed migration.
2. **SVG vs the public inline allow-list** — `FileDocumentDomain.INLINE_SAFE_CONTENT_TYPES` excludes SVG (ADR-FILE-002: inline only for raster images and PDF), so a sanitised SVG logo would be served as an attachment unless the list is extended for sanitised SVG.
3. **JDK sniff limitation** — see Facts: a category allow-list naming PDF / ZIP / OOXML / OLE2 types rejects those uploads with 415 because `URLConnection.guessContentTypeFromStream` returns null for them; the image types the store uses (PNG, JPEG, WebP?, SVG?) must be checked against what the sniffer actually recognises (it recognises PNG, JPEG, GIF; WebP and SVG are **not** recognised by the JDK sniffer, so the store cannot rely on `sniffContentType` alone for them).

### D4.2 FILE — registry and P0 / P0_5 / P2 rows
`P1/registry-srs-file.md`: APIs `NEW | XM-FILE-nnn | in-process FileImageStoreApi.storePublicImage(ImageStoreRequest) → StoredImage, discard(Long) | SEC (user photo: SEC_USER / SEC), TENANT (logo: CORE_TENANT / TENANT, written under TenantContext.callAs(tenantId))`; RULES `NEW | — | Image types | png, jpeg, webp; svg for logos only, sanitised (<script, on*=, external hrefs rejected)`, `NEW | — | Image size | ≤ 1 MB; no server-side resize`, `NEW | — | Public category for images | the 1.2.0 publish rule holds; the category code(s) and seed are fixed in the P2 1.3.0 addendum before the code`; DEPENDENCIES `NEW | exposed | FileImageStoreApi | SEC, TENANT`.

`P0/business-policies-file.md`
| # | Policy | Kind | Policy-level delta | Source |
|---|---|---|---|---|
| 1 | (public images) | NEW | Profile photos (SEC) and tenant logos (TENANT) are PUBLIC documents with non-guessable slugs, stored through the shared `FileImageStoreApi`; the URL is unauthenticated, the slug is random and per tenant, and replacing or removing the image discards the previous document. | plan §D.4, §E.3; ADR-FILE-008 |
| 2 | (image rules) | NEW | `image/png`, `image/jpeg`, `image/webp`; `image/svg+xml` for logos only and only after sanitisation (`<script`, `on*=` attributes, external hrefs rejected); ≤ 1 MB; the server never resizes. | plan §D.4 |
| 3 | (tenant logo ownership) | NEW | A logo document belongs to the tenant it depicts (`FILE_DOCUMENT.TENANT_ID` = that tenant, `ownerType = CORE_TENANT`, `moduleCode = TENANT`), even though a platform operator uploads it; its public URL is under that tenant's code. | plan §E.1 |

`P0/platform-summary.md`: `NEW | FileImageStoreApi (XM-FILE-nnn): one in-process image store for SEC user photos and TENANT logos, both PUBLIC documents with non-guessable slugs | plan §D.4; ADR-FILE-008`; `CHANGED | DEPENDENCY MAP: Security → FILE and tenant → FILE (image store) are new consumers of FILE | plan §D.4, §E.2`.

`P0/module-registry-file.md`: EXPOSED SURFACE `NEW | com.erp.file.crossmodule.FileImageStoreApi (XM-FILE-nnn) | StoredImage storePublicImage(ImageStoreRequest{ownerType, ownerId, moduleCode, bytes, contentType, fileName, maxBytes, allowedTypes}) → {documentId, publicUrl}; discard(Long documentId). Stores through the configured provider and publishes PUBLIC with a random slug (step-07 path, no new table). | plan §D.4; ADR-FILE-008`; DEPENDENCIES `NEW | SEC (consumer of FILE) | API (exposed) | user photo: ownerType = SEC_USER, moduleCode = SEC; PUT /api/v1/sec/me/photo | plan §D.2, §D.4`, `NEW | TENANT (consumer of FILE) | API (exposed) | tenant logo: ownerType = CORE_TENANT, ownerId = tenant id, moduleCode = TENANT; written inside TenantContext.callAs(tenantId) so the row and the public URL belong to that tenant; CORE_TENANT.LOGO_FILE_ID soft reference | plan §E.1, §E.2`; AUTO-DECISIONS `NEW | Image rules | png / jpeg / webp (+ svg for logos, sanitised); ≤ 1 MB; no server resize; the image lives in an ALLOW_PUBLIC category whose code and seed are fixed in the P2 1.3.0 addendum | plan §D.4`.

`P0_5/prd-file.md`: `NEW | Profile photo stored as a public image | any staff user (through SEC's "my profile") | PNG / JPEG / WebP, ≤ 1 MB; the photo is a PUBLIC file with a non-guessable URL; replacing it discards the previous one. | plan §D.2, §D.4; ADR-FILE-008`; `NEW | Tenant logo stored as a public image | platform administrator (through the platform tenants screen) | PNG / JPEG / WebP / sanitised SVG, ≤ 1 MB; the logo belongs to the tenant it depicts and is served under that tenant's code; replacing or removing it discards the previous document. | plan §E.1, §E.2`; `CHANGED | US-FILE-004 attach files to any module's records | A module that needs a public image (photo, logo) uses the in-process FileImageStoreApi instead of the REST upload. | plan §D.4`.

`P2/db-script.md`
| Kind | Object | Delta | Migration | Source |
|---|---|---|---|---|
| NEW | (no FILE table or column) | D.4 adds no FILE schema: photos and logos are ordinary `FILE_DOCUMENT` rows (`VISIBILITY = PUBLIC`, `PUBLIC_SLUG` set, `STORAGE_PROVIDER` as configured). | — | plan §D.4 |
| NEW | `CORE_TENANT.LOGO_FILE_ID BIGINT NULL` (TENANT module) | Soft reference to `FILE_DOCUMENT.ID`; no FK. The logo row has `TENANT_ID` = that tenant, `OWNER_TYPE = 'CORE_TENANT'`, `OWNER_ID` = tenant id, `MODULE_CODE = 'TENANT'`. Recorded in TENANT's P2; listed here as the consumer side. | `V18__tenant_branding.sql` (expected; number confirmed from the directory at creation time) | plan §E.1 |
| NEW | FILE_CATEGORY seed for public images | A PUBLIC document needs a category with `ALLOW_PUBLIC = TRUE` (`CHK_FILE_DOCUMENT_PUBLIC_SLUG` and the publish rule). The category code(s) the image store uses, their tenant scope (seeded per tenant by provisioning, or resolved at store time) and the seed migration are fixed in this table before the migration is written. | (to be numbered from the directory listing) | plan §D.4; file/domain/FileDocumentDomain.java:57-61 |

`P2/registry-db-file.md`: `NEW | COLUMNS | none in FILE (photos and logos are ordinary FILE_DOCUMENT rows); CORE_TENANT.LOGO_FILE_ID BIGINT NULL is a soft reference to FILE_DOCUMENT.ID, registered under TENANT | V18__tenant_branding.sql (expected)`; `NEW | SEED | FILE_CATEGORY row(s) with ALLOW_PUBLIC = TRUE for the image store — codes and scope fixed in db-script.md → 1.3.0 addendum before the migration | (numbered from the directory listing)`; `NEW | XM REGISTER | inbound: SEC and TENANT call FileImageStoreApi (XM-FILE-nnn, application-layer; no physical FK) | —`.

---

## Package E — tenant branding (logo, brand colour, `/api/v1/tenant/me`, public branding)

### E.1 Endpoints — `TENANT/P1/srs-tenant.md` §1
| Kind | Method | Path | Access | Body / result | Rules & errors | Traces | Source |
|---|---|---|---|---|---|---|---|
| NEW — **FE** | PUT | `/{id}/logo` | `PLATFORM_TENANT_MANAGE` | multipart `file` → 200 `TenantResponse` | stored through `FileImageStoreApi` (FILE, plan §6 D.4) as tenant `{id}`; replaces and discards the previous logo (RULE-TENANT-nnn (logo)); 404 `TENANT_NOT_FOUND`, 400 `TENANT_LOGO_INVALID`; audit `TENANT_LOGO_CHANGED` (actor = platform operator) | REQ-TENANT-nnn (logo) | plan §7 E.2 |
| NEW — **FE** | DELETE | `/{id}/logo` | `PLATFORM_TENANT_MANAGE` | → 204 | discards the document; the client falls back to the platform mark | REQ-TENANT-nnn (logo) | plan §7 E.2 |
| NEW — **FE** | PATCH | `/{id}/branding` | `PLATFORM_TENANT_MANAGE` | `{ brandColor }` (null clears) → 200 `TenantResponse` | 400 `TENANT_BRAND_COLOR_INVALID` (RULE-TENANT-nnn (brand colour format)) | REQ-TENANT-nnn (brand colour) | plan §7 E.1, E.2 |
| NEW — **FE** | GET | `/api/v1/tenant/me` | `isAuthenticated()` (any realm) | → 200 `TenantBrandingResponse`: `code`, `nameAr`, `nameEn`, `logoUrl` (nullable), `brandColor` (nullable), `defaultLocale` | the token's tenant; read-only, no contact / profile fields; what the shell loads after login. Open point bound at implementation: the path is named by neither chain today (see Facts) — the chain that serves a CUSTOMER token must be named in `ErpCoreSecurityAutoConfiguration` | REQ-TENANT-nnn (branding of the token's tenant) | plan §7 E.2 |
| NEW — **FE** | GET | `/api/v1/public/tenants/{tenantCode}/branding` | public, no auth | → 200 `TenantBrandingResponse` | tenant resolved **from the path** (`{tenantCode}` added to the `erp.core.tenant.path-tenant-paths` defaults, RULE-TENANT-012 source 1); unknown → 404 `TENANT_NOT_FOUND`; suspended → 403 `TENANT_SUSPENDED`; rate-limited per IP (`erp.core.tenant.public-branding-rate-limit.*`) — what the login page calls before any token exists | REQ-TENANT-nnn (public branding) | plan §7 E.2 |

### E.2 Business rules — §2
| Kind | Id | Rule | Traces | Source |
|---|---|---|---|---|
| NEW — **FE** | RULE-TENANT-nnn — شعار المستأجر / Tenant logo | A logo shall be a PUBLIC document ≤ 1 MB of type `image/png`, `image/jpeg`, `image/webp` or `image/svg+xml` (SVG sanitised: `<script`, `on*=` attributes and external hrefs rejected), stored in the tenant's own rows (`FILE_DOCUMENT.TENANT_ID = {id}`, `ownerType = CORE_TENANT`, `ownerId = {id}`, `moduleCode = TENANT`) so its URL is `/api/v1/public/files/{thatTenantCode}/{slug}`; one logo per tenant; replacing or removing it discards the previous document. Rejection: `TENANT_LOGO_INVALID` (raised by the tenant service from FILE's validation result). | REQ-TENANT-nnn (logo) | plan §7 E.1, E.3; §6 D.4 |
| NEW | RULE-TENANT-nnn — مستأجر المنصة يحمل شعارًا أيضًا / PLATFORM may carry a logo | The PLATFORM tenant may carry a logo like any tenant; the platform **mark** shown by the frontend is a static asset, never a tenant logo. | REQ-TENANT-nnn (logo) | plan §7 E.3 |
| NEW | RULE-TENANT-nnn — العلامة التجارية من المنصة فقط / Branding is platform-only | The logo and branding endpoints shall be served only to a PLATFORM operator holding `PLATFORM_TENANT_MANAGE` (decision D5); a tenant administrator has no write path to branding in 1.3.0. | REQ-TENANT-nnn (logo), (brand colour) | plan §0 D5, §7 E.3; ADR-TENANT-005 |
| NEW | RULE-TENANT-nnn — صيغة لون العلامة / Brand colour format | `brandColor` shall be null or match `^#[0-9A-Fa-f]{6}$` (`CHK_CORE_TENANT_BRAND_COLOR`); anything else → 400 `TENANT_BRAND_COLOR_INVALID`. | REQ-TENANT-nnn (brand colour) | plan §7 E.1, E.2 |

### E.3 Error codes — §3
| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW | `TENANT_LOGO_INVALID` | 400 | logo type, size or SVG content rejected (from FILE's validation result, never a raw exception) | plan §7 E.2; §6 D.4 |
| NEW | `TENANT_BRAND_COLOR_INVALID` | 400 | `brandColor` not `^#[0-9A-Fa-f]{6}$` | plan §7 E.2 |

### E.4 Dependencies and configuration — §6
| Direction | Item | Source |
|---|---|---|
| consumed (XM-TENANT-nnn (logo soft reference), NEW) | soft reference `CORE_TENANT.LOGO_FILE_ID` → `FILE_DOCUMENT.ID` (no FK), validated through `FileDocumentLookupApi.isAvailable`, resolved to a URL through `FileDocumentLookupApi.publicUrl` | plan §7 E.1; §6 D.1 (same convention) |
| config | NEW `erp.core.tenant.public-branding-rate-limit.*`; `erp.core.tenant.path-tenant-paths` default + `/api/v1/public/tenants/{tenantCode}/branding` | plan §7 E.2 |
`module-registry-tenant.md` CONFIGURATION: `CHANGED | erp.core.tenant.path-tenant-paths | + /api/v1/public/tenants/{tenantCode}/branding | plan §7 E.2`; `NEW | erp.core.tenant.public-branding-rate-limit.* | bucket per IP, like customer login | plan §7 E.2`. EXPOSED SURFACE `NEW | GET /api/v1/tenant/me, GET /api/v1/public/tenants/{tenantCode}/branding (TenantBrandingResponse) | the frontend shell and login page (plan §8 F2) | — (HTTP) | plan §7 E.2`. "ROOT for data no longer holds strictly: `CORE_TENANT.LOGO_FILE_ID` is a soft reference to `FILE_DOCUMENT.ID` (no FK, XM-TENANT-nnn)."

### E.5 Requirements and acceptance criteria — §7
| Kind | REQ / AC | Statement (EARS) → acceptance | Traces | Source |
|---|---|---|---|---|
| NEW — **FE** | REQ-TENANT-nnn / AC-TENANT-nnn — شعار المستأجر يضبطه مدير المنصة / Tenant logo set by the platform administrator | When a platform operator uploads a valid image to `/{id}/logo`, the system shall store it as tenant `{id}`'s public document, reference it from the tenant and discard the previous one; `DELETE` removes it. → `GET /platform/tenants/{id}.logoUrl` set; T's own `GET /api/v1/tenant/me.logoUrl` equal; the URL serves under T's code; replace discards the previous document; DELETE → `logoUrl` null; SVG with `<script>` rejected; a tenant admin → 403 (TC-CORE-TENANT-040 … -045) | US-TENANT-nnn (logo and brand colour); POL-TENANT-nnn (branding); RULE-TENANT-nnn (logo), (PLATFORM logo), (branding platform-only) | plan §7 E.2, E.4; ADR-TENANT-005 |
| NEW — **FE** | REQ-TENANT-nnn / AC-TENANT-nnn — لون العلامة / Brand colour | When a platform operator patches `/{id}/branding` with a valid `brandColor` (or null), the system shall store (or clear) it. → `#1A2B3C` accepted; `red` → 400 `TENANT_BRAND_COLOR_INVALID` (TC-CORE-TENANT-046) | US-TENANT-nnn (logo and brand colour); RULE-TENANT-nnn (branding platform-only), (brand colour format) | plan §7 E.1, E.2 |
| NEW — **FE** | REQ-TENANT-nnn / AC-TENANT-nnn — علامة المستأجر الحالي / Branding of the token's tenant | When an authenticated caller of any realm asks for `/api/v1/tenant/me`, the system shall return its tenant's `TenantBrandingResponse` and nothing else. → the payload carries `code`, names, `logoUrl`, `brandColor`, `defaultLocale` and no contact / profile fields (TC-CORE-TENANT-041) | US-TENANT-nnn (read my tenant's branding); POL-TENANT-007 | plan §7 E.2 |
| NEW — **FE** | REQ-TENANT-nnn / AC-TENANT-nnn — علامة عامة برمز المستأجر / Public branding by tenant code | When an anonymous caller asks for `/api/v1/public/tenants/{tenantCode}/branding`, the system shall resolve the tenant from the path and return its branding; unknown → not found, suspended → forbidden, over the rate limit → too many requests. → unknown code 404 `TENANT_NOT_FOUND`; suspended 403 `TENANT_SUSPENDED` (TC-CORE-TENANT-047, -048) | US-TENANT-nnn (branding before login); POL-TENANT-008; RULE-TENANT-012 | plan §7 E.2, E.4 |
§9: `ADR-TENANT-005 | the tenant logo is set by the platform administrator from PLATFORM_TENANTS; no tenant self-service screen in 1.3.0 | ACCEPTED (decision D5)`.

`registry-srs-tenant.md`: REQUIREMENTS `NEW | REQ/AC-nnn | Tenant logo set by the platform administrator (/{id}/logo) | §7 E.2`, `Brand colour (/{id}/branding)`, `Branding of the token's tenant (GET /api/v1/tenant/me)`, `Public branding by tenant code (GET /api/v1/public/tenants/{tenantCode}/branding)`; RULES `NEW | RULE-TENANT-nnn | Tenant logo (type, size, SVG sanitising, one per tenant, discard on replace) | §7 E.3`, `PLATFORM may carry a logo; the platform mark is a static asset | §7 E.3`, `Branding is platform-only (D5) | §7 E.3`, `Brand colour format ^#[0-9A-Fa-f]{6}$ | §7 E.1`; XM `NEW | XM-TENANT-nnn | soft reference CORE_TENANT.LOGO_FILE_ID → FILE_DOCUMENT.ID (consumed; no FK; FileDocumentLookupApi, FileImageStoreApi) | §7 E.1, §6 D.4`; DECISIONS `ADR-TENANT-005 (ACCEPTED, decision D5)`.

### E.6 Policies, stories, platform summary, schema
`business-policies-tenant.md`
| Kind | Policy | Statement (ar / en) | Pattern · Trigger | Rationale | Source | Status |
|---|---|---|---|---|---|---|
| NEW | POL-TENANT-nnn — العلامة التجارية يضبطها مدير المنصة فقط / Branding is set by the platform administrator only | يجب على النظام قصر ضبط شعار المستأجر ولون علامته على مشغّل المنصة من شاشة `PLATFORM_TENANTS`؛ لا شاشة ذاتية للمستأجر في 1.3.0، والشعار ملف عام في صفوف المستأجر نفسه. / The system shall let only a platform operator, from the `PLATFORM_TENANTS` screen, set a tenant's logo and brand colour; there is no tenant self-service screen in 1.3.0, and the logo is a PUBLIC document in the tenant's own rows. | ubiquitous · logo / branding endpoints | decision D5: no new module, screen or permission; a tenant self-service screen is a later, separately registered feature (ADR-TENANT-005) | plan §0 D5, §7 E.1–E.3; ADR-TENANT-005 | PLANNED (1.3.0) |
SCOPE EXCEPTIONS `NEW | Tenant self-service branding screen (TENANT_BRANDING) | deferred: alternative recorded in ADR-TENANT-005 | plan §9`. RESOLVED DECISIONS `5 | Who sets a tenant's logo | the platform administrator from PLATFORM_TENANTS | decision D5 (2026-10-07) | ADR-TENANT-005` (also in `module-registry-tenant.md`).

`prd-tenant.md`
| Kind | US | Title (ar / en) | Story | Actor | Traces (POL) | Source |
|---|---|---|---|---|---|---|
| NEW — **FE** | US-TENANT-nnn | شعار المستأجر ولون علامته / A tenant's logo and brand colour | As a platform operator, I need to upload, replace or remove a tenant's logo and set an optional brand colour from the tenants screen, so that the organisation's users see their own mark beside the platform's. | platform operator (D5) | POL-nnn (branding) | plan §7 E.2 (`/{id}/logo`, `/{id}/branding`) |
| NEW — **FE** | US-TENANT-nnn | قراءة علامة مستأجري / Read my tenant's branding | As any signed-in user (staff or customer), I need my tenant's code, names, logo URL, brand colour and default locale, so that the application shell can show them after login. | any authenticated user | POL-TENANT-007 | plan §7 E.2 (`GET /api/v1/tenant/me`) |
| NEW — **FE** | US-TENANT-nnn | علامة المستأجر قبل الدخول / Tenant branding before login | As an anonymous visitor on the login page, I need the branding of the tenant whose code I typed, so that I see the right logo before I sign in. | anonymous visitor | POL-TENANT-008 | plan §7 E.2 (`GET /api/v1/public/tenants/{tenantCode}/branding`) |
CHANGED `US-TENANT-005 public URL carrying its tenant | a second path-tenant path: the public branding endpoint | plan §7 E.2`. DEFERRED `NEW | (tenant self-service branding screen) | decision D5 | later version (ADR-TENANT-005 alternative)`.

`platform-summary.md`: `NEW — FE | Branding for the UI | GET /api/v1/tenant/me (authenticated, any realm) and GET /api/v1/public/tenants/{tenantCode}/branding (public, path tenant, rate-limited) feed the shell and the login page; the logo document lives in the tenant's own rows as a PUBLIC file | plan §7 E.1, E.2, §8 F2`.

Schema — `db-script-tenant.md`: migration chain `NEW | V18__tenant_branding.sql | E | LOGO_FILE_ID, BRAND_COLOR + CHK_CORE_TENANT_BRAND_COLOR; no registry rows (D5)`.
| Kind | DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | Constraint | Migration |
|---|---|---|---|---|---|---|---|---|---|
| NEW | DBF-TENANT-nnn | LOGO_FILE_ID | BIGINT | ENT-TENANT-001.logoFileId | REQ (logo) | NULL | — | soft reference to `FILE_DOCUMENT.ID`, **no FK** (XM-TENANT-nnn; same convention as `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID`) | V18 |
| NEW | DBF-TENANT-nnn | BRAND_COLOR | VARCHAR(7) | ENT-TENANT-001.brandColor | REQ (brand colour) | NULL | — | `CHK_CORE_TENANT_BRAND_COLOR CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$')` | V18 |
The logo document itself is a `FILE_DOCUMENT` row **in the target tenant's rows** (`TENANT_ID = {id}`, `OWNER_TYPE = CORE_TENANT`, `OWNER_ID = {id}`, `MODULE_CODE = TENANT`, PUBLIC), written inside `TenantContext.callAs(id)` so its URL is `/api/v1/public/files/{thatTenantCode}/{slug}` — no schema change in FILE (plan §7 E.1).
```sql
-- V18__tenant_branding.sql (package E) — no registry rows (D5)
ALTER TABLE CORE_TENANT ADD COLUMN LOGO_FILE_ID BIGINT;
ALTER TABLE CORE_TENANT ADD COLUMN BRAND_COLOR  VARCHAR(7);
ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_BRAND_COLOR CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$');
```
XM REGISTER / registry-db XM index: `NEW | XM-TENANT-nnn | SOFT-REF (consumed) | CORE_TENANT.LOGO_FILE_ID → FILE_DOCUMENT.ID | FILE (FileDocumentLookupApi.isAvailable / publicUrl, FileImageStoreApi) | column only, no FK (soft reference, NOTIF precedent) | PLANNED (1.3.0)`. DECISIONS: `ADR-TENANT-005 | no registry rows for branding (D5) | ACCEPTED`; `DEFAULT | LOGO_FILE_ID soft reference without FK | plan §6 D.1 (same convention as NOTIF_TEMPLATE.ATTACHMENT_FILE_ID, XM-NOTIF-002) | bound here`. CONSTRAINTS: `CHK_CORE_TENANT_BRAND_COLOR CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$')` (V18). Cascade: FILE `FILE_DOCUMENT` rows with `OWNER_TYPE = CORE_TENANT`, `MODULE_CODE = TENANT` (logo) — data, not schema; FILE's addendum records `FileImageStoreApi` (D4) and ADR-FILE-008. SEC / NOTIF: no TENANT-owned column lands in their tables.

### E.7 Audit action
| NEW | `TENANT_LOGO_CHANGED` | TENANT — `PUT /api/v1/platform/tenants/{id}/logo` (E.2) | actor = platform operator | plan §7 E.2 (line 222) |

---

## Package G — revoke a single screen / action grant

No row of package G stood in the TENANT, FILE, NOTIF, AUDIT, EVENTS, COMMON, MDL or CU files; its analysis rows are SEC's (`governance/analysis/modules/SEC/**`, ADR-SEC-041), owned by the implementing run.

---

## ADR drafts handed over

The five drafts below are the full text of the files this session had written. They are deleted from `governance/analysis/decisions/` so that the implementing run creates them when its package lands. Numbers are kept as the plan names them (ADR-TENANT-002 … 005). **ADR-FILE-001 of the plan is renumbered to ADR-FILE-008**: the as-built FILE decisions now occupy ADR-FILE-001 … 007 (renamed on 2026-10-08 from 002 … 008; the plan's two mentions were updated), so package D's ADR is the next free number, 008. Inside the drafts, real as-built ids are cited as renumbered (RULE-TENANT-012 … 015; ADR-FILE-002 = public files, formerly 003) and draft ids are placeholders.

### ADR-TENANT-002 — Per-tenant token cut-off (`TOKENS_INVALID_BEFORE`) instead of a `jti` denylist

Module  : TENANT     Version : 1.3.0 (tenant-maturity plan, package C.2)     Stage raised : P1 (SRS) — before the code
Status  : PROPOSED → 1.3.0 (becomes ACCEPTED when the code of C.2 is compared with the addendum)

#### Context
Since erp-core 1.2.0 a suspended tenant's tokens stop working at once, because `TenantResolutionFilter`
re-reads `CORE_TENANT` for every authenticated request and refuses a tenant that is not `ACTIVE`
(`erp-core/src/main/java/com/erp/tenant/security/TenantResolutionFilter.java:89-99`, REQ-TENANT-010).
Two gaps remain (`docs/plans/tenant-maturity-plan.md` §5 C.1, C.2):
- when the tenant is **re-activated**, every token issued before the suspension works again — a
  suspension never ends a session, it only pauses it (REQ-TENANT-008; TC-CORE-TENANT-024 "re-activation
  restores login" applies to the old tokens too);
- the platform has no way to **invalidate a tenant's tokens** without suspending it (a leaked token, a
  rotated administrator), and `SEC_ACTIVE_SESSION` rows stay open after a suspension.

Access tokens are JWTs carrying `tid`, `realm` and `jti` (`sec/security/JwtTokenIssuer.java:41-49`,
SEC 1.2.0 addendum); they are stateless on every request except the tenant and user lookups the filters
already perform. Two designs were available:
- **A denylist keyed by `jti`** (in the database, or Redis when present, as the download-token store
  does): each revoked token is listed until it expires; every request looks the `jti` up. Revoking "every
  token of a tenant" means enumerating its open sessions, and a token whose session row is gone can
  still be replayed until expiry.
- **A per-tenant cut-off instant** `TOKENS_INVALID_BEFORE` on `CORE_TENANT`: a token is valid only if
  its `iat` is at or after the instant; revoking a tenant's tokens is one `UPDATE`.

#### Decision
`CORE_TENANT.TOKENS_INVALID_BEFORE TIMESTAMPTZ NULL` (expected `V17__tenant_lifecycle.sql`,
DBF-TENANT-nnn). `TenantResolutionFilter`, which already loads the tenant for every authenticated
request, additionally refuses a token whose `iat` is before the cut-off with 401
`TENANT_TOKEN_REVOKED`; the JWT filter exposes `iat` on the authentication details (it does not today —
C.2 adds it). The cut-off is written by two operations, both `PLATFORM_TENANT_MANAGE`: activation
(`PATCH /{id}/status` to `ACTIVE`, RULE-TENANT-nnn (suspension reason)) and the new
`POST /api/v1/platform/tenants/{id}/revoke-tokens` (audit `TOKENS_REVOKED`), which also terminates the
tenant's sessions. No `jti` store is introduced; `jti` keeps its present use (session reference).

Reasons:
1. **No per-request store.** The check rides on the row the filter already reads (RULE-TENANT-015: the
   filter caches nothing, so the cut-off is effective on the next request, like a suspension).
2. **Tenant-wide by construction.** The platform's cases are tenant-wide (re-activation, "log everyone
   out"); a denylist is the right tool for single-token revocation, which SEC's session deletion
   already covers at the session level.
3. **Nothing to expire or clean.** One nullable instant per tenant, no retention job, no Redis
   dependency, no growth with the number of tokens.
4. **Additive.** One nullable column and one filter branch; the 1.2.0 behaviour is unchanged while the
   column is NULL.

#### Consequences
- Revocation granularity is the tenant, never one user: a single leaked token is handled by deleting
  the session (SEC) or by revoking the whole tenant's tokens; a per-user cut-off (`SEC_USER`) is a
  later, SEC-owned extension that would reuse the same mechanism.
- A token issued in the same second as the cut-off is refused (`iat` < cut-off is strict; `iat` has
  second precision) — acceptable, the user logs in again.
- Clock skew between nodes shifts the cut-off by that skew; the instants are server-side only.
- The filter's behaviour on a revoked token mirrors `TENANT_SUSPENDED`: the security context is
  cleared and the hand-written envelope (`FilterErrorResponseWriter`) answers, so the code is written by
  the filter, not thrown.

#### Traces
ENT-TENANT-001 · REQ-TENANT-nnn (token cut-off), REQ-TENANT-nnn (suspend with a reason) ·
RULE-TENANT-nnn (suspension reason), RULE-TENANT-nnn (token cut-off) · POL-TENANT-nnn (token cut-off) ·
DBF-TENANT-nnn (`TOKENS_INVALID_BEFORE`) · plan §5 C.2, §9

### ADR-TENANT-003 — Idempotency keys stored in `CORE_IDEMPOTENCY_KEY`; first consumer is tenant create

Module  : TENANT (mechanism owned by `com.erp.common`)     Version : 1.3.0 (tenant-maturity plan, package C.4)     Stage raised : P2 (Database) — before the code
Status  : PROPOSED → 1.3.0 (becomes ACCEPTED when the code of C.4 is compared with the addendum; may slip to 1.4.0 with the package)

#### Context
`POST /api/v1/platform/tenants` is the platform's most expensive and least repeatable call: it inserts
the `CORE_TENANT` row and runs every `TenantProvisioningContributor` in one transaction
(`erp-core/src/main/java/com/erp/tenant/service/TenantService.java:68-103`, POL-TENANT-004). A client
that times out and retries today either receives 409 `TENANT_CODE_DUPLICATE` (the first call
committed) or creates the tenant twice under different codes (the operator changed the code to get
past the 409). The plan (§5 C.4, item 13) asks for an idempotent create: a retry with the same key
replays the first answer. Three places were available for the stored responses:
- **In memory** (per node) — lost on restart, wrong across nodes; the download-token store's
  in-memory default exists only because a token is short-lived and single-use.
- **Redis** (optional, like the download-token store) — a second code path when Redis is absent, and
  the platform's only mandatory store is PostgreSQL.
- **A core table** — one row per (tenant, key, endpoint), purged by a scheduled job, visible to every
  node, inside the same transaction as the write it protects.

The mechanism is generic (any POST could use it) and therefore belongs to `com.erp.common`, which every
module may consume; the table is registered in COMMON's P2 (`governance/analysis/modules/COMMON/P2/`)
and, as the first consumer's discriminator column, in TENANT's P2.

#### Decision
A core table `CORE_IDEMPOTENCY_KEY` (expected `V20__core_idempotency_key.sql`), columns exactly as the
plan names them: `ID BIGINT PK`, `TENANT_ID BIGINT NOT NULL FK`, `IDEMPOTENCY_KEY VARCHAR(64) NOT NULL`,
`ENDPOINT VARCHAR(200) NOT NULL`, `REQUEST_HASH VARCHAR(64) NOT NULL`, `RESPONSE_STATUS INT NOT NULL`,
`RESPONSE_BODY TEXT`, `CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT now()`, `VERSION BIGINT NOT NULL DEFAULT 0`;
`UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)`, `SEQ_CORE_IDEMPOTENCY_KEY`, an index on
`CREATED_AT` (C4.2 / C4.3 above). The mechanism lives in `com.erp.common.idempotency`; v1 applies it to
`POST /api/v1/platform/tenants` only:
- header `Idempotency-Key`, optional, ≤ 64 characters; without it the 1.2.0 behaviour is unchanged;
- same key + same `REQUEST_HASH` → the stored status and body are replayed with the response header
  `Idempotent-Replayed: true`; nothing runs;
- same key + different hash → 409 `IDEMPOTENCY_KEY_CONFLICT`;
- rows older than 24 h are purged by a scheduled job on the audit-retention precedent (core never
  schedules by default; the application enables it).

Reasons:
1. **One mandatory store.** PostgreSQL is the only store every deployment has; the row commits with the
   tenant it protects, so a crash between the insert and the key write cannot leave a replayable
   response for a tenant that does not exist.
2. **Tenant-scoped like every core row.** The key is unique per (tenant, endpoint): two platform
   operators of PLATFORM share the namespace, as they share the tenant; the row is written inside the
   operator's tenant (PLATFORM) with explicit `TENANT_ID`.
3. **Generic without a second consumer.** The table carries `ENDPOINT`, so a later consumer (an
   application's own expensive POST) adds no column; it also keeps the mechanism out of `tenant`.
4. **Bounded growth.** 24 h retention keeps the table at one row per retried call per day.

#### Consequences
- The replayed body is the stored JSON as it was answered; a later change of `TenantResponse` is not
  reflected in a replay of an older key (24 h window, acceptable).
- The hash covers the request body only (not headers); a retry with the same body but another
  `X-Tenant-Code` header is still the same request — the platform path is tenant-exempt and the
  operator's tenant is PLATFORM either way.
- `TenantSchemaIntegrationTest`'s counts (22 discriminator columns, 14 tenant-leading unique
  constraints) move with V20 and are re-bound when it is written.
- Enabling the purge job follows `erp.core.audit.*`'s pattern (`docs/CONSUMING.md` §9); its property
  keys are bound in the addendum when written, not here.

#### Traces
ENT-TENANT-001 (FK target) · REQ-TENANT-nnn (idempotent provisioning), REQ-TENANT-001 ·
RULE-TENANT-nnn (idempotent provisioning) · POL-TENANT-004, POL-TENANT-nnn (idempotent) ·
DBF-TENANT-nnn (`CORE_IDEMPOTENCY_KEY.TENANT_ID`) · plan §5 C.4, §9, §11

### ADR-TENANT-004 — `ScopedValue` for `TenantContext`: a spike with go / no-go

Module  : TENANT     Version : 1.3.0 (tenant-maturity plan, package C.6)     Stage raised : P1 (SRS) — before the code
Status  : PROPOSED — decision pending the spike (plan §0 D6); may slip to 1.4.0 without blocking the tag

#### Context
`TenantContext` holds the request tenant in a `ThreadLocal`
(`erp-core/src/main/java/com/erp/tenant/TenantContext.java:27`): the security chain sets it and clears
it in a `finally`; `runAs` / `callAs` restore the previous value (RULE-TENANT-013); the event executor's
`TenantAndSecurityContextTaskDecorator` copies it into worker threads and clears it afterwards
(`events/support/TenantAndSecurityContextTaskDecorator.java:19`); a value left on a pooled thread is
detected and cleared at the next request (REQ-TENANT-023, erp-core 1.2.0). Hibernate reads it through
`TenantIdentifierResolver` when a session opens (RULE-TENANT-014).

`ScopedValue` (final in JDK 25, the project's JDK) binds a value to a bounded dynamic scope instead
of a thread: it cannot leak past the scope, it is inherited by structured-concurrency subtasks, and it
is cheap under virtual threads, where a `ThreadLocal` per carrier is a known cost. The plan (item 11)
wants the question answered by measurement, not by rewriting: `TenantContext` is consumed by every
module, by `events` (executor decorator, after-commit listeners) and by `notif` (delivery listener,
requeue job), and its public API (`current`, `find`, `require`, `set`, `clear`, `runAs`, `callAs`,
`isPlatform`) is part of the library's versioned surface (`docs/RELEASE.md`).

Alternatives considered:
- **Keep the `ThreadLocal`** — known behaviour, the 1.2.0 leak guard, no change to `events` / `notif`.
- **`ScopedValue` behind the same public API** — `runAs` / `callAs` become
  `ScopedValue.where(...).run/call`; the servlet filter keeps `set` / `clear` working through a bounded
  `ScopedValue.where(...).run(chain)`; the task decorator rebinds the captured value in the worker.
- **`ScopedValue` with a new API** (no `set` / `clear`) — a MAJOR change for every consumer; excluded.

#### Decision
Run the spike on branch `spike/tenant-scoped-value`: replace the `ThreadLocal` behind the **same public
API**; keep `set` / `clear` for the servlet filter by a bounded `ScopedValue.where(...).run(chain)` in
the filter. The spike must pass the full `mvn verify`, `TenantIsolationIT` (package C.3), the
async-executor tests of `TenantAndSecurityContextTaskDecorator`, and the NOTIF claim job under virtual
threads (`spring.threads.virtual.enabled=true`). It records the context-leak tests and the p95 of the
resolution filter before and after.

Go / no-go is written **into this ADR** when the spike ends:
- **go** — the change merges as its own PR, this ADR becomes ACCEPTED with the measurements, and
  REQ-TENANT-023's leak guard is re-stated for the new binding;
- **no-go** — this ADR records why (a failing test, no measurable gain, a consumer that needs an
  unbounded `set`), becomes REJECTED, and item 11 closes; the `ThreadLocal` stays.

Until then nothing in `com.erp.tenant`, `com.erp.events` or `com.erp.notif` changes for this item.

#### Consequences
- The public API is frozen either way: no consumer, in core or in an application, changes a line.
- A "go" removes the class of bug REQ-TENANT-023 guards against (a tenant cannot outlive its scope),
  but makes `set` outside a scope an error — every caller of `TenantContext.set` in core (the two
  filters) is already inside a request scope; the spike lists any other caller it finds.
- A "no-go" costs nothing: the spike branch is deleted and the item is closed with its reasons.
- The decision is independent of packages B, C.1–C.5 and E; it may slip to 1.4.0.

#### Traces
REQ-TENANT-017, REQ-TENANT-018, REQ-TENANT-023 · RULE-TENANT-013, RULE-TENANT-014 · POL-TENANT-007 ·
plan §0 D6, §5 C.6, §9

### ADR-TENANT-005 — The tenant logo is set by the platform administrator from `PLATFORM_TENANTS`; no tenant self-service screen in 1.3.0

Module  : TENANT     Version : 1.3.0 (tenant-maturity plan, package E)     Stage raised : P0 (Policies) — decision D5 of the plan, recorded before the code
Status  : ACCEPTED (decision D5, 2026-10-07)

#### Context
Package E gives a tenant a logo (and an optional brand colour) that the frontend shows beside the
platform mark after login and on the login page (`docs/plans/tenant-maturity-plan.md` §7, §8 F2).
Someone must upload it. Two owners were possible:
- **The tenant's own administrator**, through a new per-tenant screen `TENANT_BRANDING` with its own
  gateway action (`PERM_TENANT_BRANDING_VIEW`) and a manage action, under a registry module the
  permission catalog does not have today; the screen would be copied into every tenant's `SYS_ADMIN`
  grants by `SecTenantProvisioningContributor` and would need its own frontend module, route, menu
  entry and E2E archive.
- **The platform administrator**, from the existing `PLATFORM_TENANTS` screen (`SCR-REQ-TENANT-001`),
  behind the existing `PLATFORM_TENANT_MANAGE`, as one more attribute of the tenant record — the same
  place where the tenant is created, suspended and (from 1.3.0) edited.

The permission catalog is code-defined and global (`TenantPermissions`,
`erp-core/src/main/java/com/erp/tenant/permission/TenantPermissions.java:28-45`); every new screen is a
registry row, a grant row per tenant and a frontend page code (`PROJECT-OVERVIEW.md`, `project-registry.md`).
The branding read side needs no permission at all: `GET /api/v1/tenant/me` is `isAuthenticated()` and
the public branding endpoint is anonymous (plan §7 E.2).

#### Decision
**Decision D5 of the plan, as taken on 2026-10-07:** the logo and the brand colour are set, replaced
and removed by a platform operator holding `PLATFORM_TENANT_MANAGE`, from the `PLATFORM_TENANTS`
screen, through `PUT` / `DELETE /api/v1/platform/tenants/{id}/logo` and
`PATCH /api/v1/platform/tenants/{id}/branding` (RULE-TENANT-nnn (branding platform-only)). No module,
screen, permission or grant seed is added (`V18__tenant_branding.sql` carries no registry rows); the
frontend adds a branding row to the tenant detail drawer (plan §8 F3), not a screen. A tenant
administrator has no write path to branding in 1.3.0; every user of the tenant reads it through
`GET /api/v1/tenant/me`.

The logo document is stored in the **target tenant's own rows** (`FILE_DOCUMENT.TENANT_ID = {id}`,
written inside `TenantContext.callAs(id)`, `ownerType = CORE_TENANT`, `ownerId = {id}`,
`moduleCode = TENANT`, PUBLIC with a random slug — ADR-FILE-008), so its public URL is
`/api/v1/public/files/{thatTenantCode}/{slug}` and the tenant's suspension withdraws it with the rest
of the tenant's public files (REQ-TENANT-010).

Reasons:
1. **Branding is onboarding.** The platform operator provisions the tenant and hands it over; the logo
   belongs to that handover, like the first administrator (POL-TENANT-010).
2. **No catalog growth for one field.** A self-service screen costs a registry module, a screen, two
   actions, a grant per tenant, a page code and a frontend module for an attribute that changes once.
3. **Platform-only is the existing shape.** `PLATFORM_TENANT_MANAGE` already covers every tenant
   attribute; the gate `isPlatformOperator` and RULE-TENANT-007 keep it inside PLATFORM.
4. **The read side needs nothing.** The shell and the login page read branding without a permission,
   so no tenant user is blocked by the decision.

#### Consequences
- A tenant that wants to change its logo asks the platform operator (a support operation, audited as
  `TENANT_LOGO_CHANGED` with the operator as actor).
- The deferred alternative is recorded for a later version: a per-tenant `TENANT_BRANDING` screen
  (registry module to be chosen, gateway + manage actions, copied by provisioning, own frontend route);
  it would reuse the same columns (`LOGO_FILE_ID`, `BRAND_COLOR`), the same `FileImageStoreApi` and
  the same read endpoints — only the write path and its permission are new, so nothing in 1.3.0 has to
  be undone.
- The PLATFORM tenant may carry a logo like any tenant (RULE-TENANT-nnn (PLATFORM logo)); the platform
  **mark** stays a static frontend asset, so a missing tenant logo always has a fallback.
- The logo's size and type rules (≤ 1 MB; PNG, JPEG, WebP, sanitised SVG) are FILE's validation,
  surfaced by the tenant service as `TENANT_LOGO_INVALID` (RULE-TENANT-nnn (logo)).

#### Traces
ENT-TENANT-001 · REQ-TENANT-nnn (logo), (brand colour), (branding of the token's tenant), (public
branding) · RULE-TENANT-nnn (logo), (PLATFORM logo), (branding platform-only), (brand colour format) ·
POL-TENANT-nnn (branding) · SCR-REQ-TENANT-001 · DBF-TENANT-nnn (`LOGO_FILE_ID`), DBF-TENANT-nnn
(`BRAND_COLOR`) · XM-TENANT-nnn (logo soft reference) · plan §0 D5, §7 E.1–E.3, §8 F3, §9

### ADR-FILE-008 (the plan's "ADR-FILE-001") — Profile photos and tenant logos are PUBLIC documents with non-guessable slugs

Module  : FILE     Version : v1 (erp-core 1.3.0, in progress)     Stage raised : P1 (SRS) — docs/plans/tenant-maturity-plan.md §D.4
Status  : PROPOSED (non-breaking)

#### Context
Package D of the tenant-maturity plan gives a staff user a profile photo (`PUT /api/v1/sec/me/photo`)
and package E gives a tenant a logo (`PUT` / `DELETE /api/v1/platform/tenants/{id}/logo`, shown by the
frontend shell beside the platform mark and on the login page before any token exists). Both are
images rendered by a browser `<img>` tag, many times per session, by every user who sees the shell
and by anonymous visitors of the login page.

erp-core 1.2.0 offers two ways to serve a stored document (`srs.md` → addendum §1):
- PRIVATE: a per-request AES/GCM token issued to one user, valid 10 minutes, single use, consumed
  after the content is opened (`FileAccessTokenDomainService`, `FileService.issueAccessToken`,
  `retrieve`). Every render would need a token round-trip, the token could not be cached by the
  browser, and the login page has no authenticated caller at all.
- PUBLIC: `/api/v1/public/files/{tenantCode}/{publicSlug}` — unauthenticated, tenant from the path,
  slug = 24 `SecureRandom` bytes base64url (192 bits, unique per tenant), served only while the
  document is PUBLIC and ACTIVE in a category with `ALLOW_PUBLIC`, cacheable one day, `inline` only
  for raster images, `nosniff` and a CSP sandbox (`PublicFileController`, `PublicFileUrls`,
  DEVIATIONS [07]).

The plan requires one shared store for both uses (`FileImageStoreApi`, §D.4) so SEC and TENANT do not
each re-implement validation, storage and publication.

#### Decision
A profile photo and a tenant logo are stored as ordinary `FILE_DOCUMENT` rows with
`VISIBILITY = PUBLIC` and a random slug, through the new in-process
`com.erp.file.crossmodule.FileImageStoreApi` (`storePublicImage(ImageStoreRequest) → StoredImage`,
`discard(Long)`), reusing the step-07 public path unchanged. The logo row belongs to the tenant it
depicts (written inside `TenantContext.callAs(tenantId)`), so its URL is under that tenant's code.
Image rules are enforced by the store: `image/png`, `image/jpeg`, `image/webp`, plus `image/svg+xml`
for logos only after sanitisation (`<script`, `on*=` attributes and external hrefs rejected);
≤ 1 MB; no server-side resize. Replacing or removing an image discards the previous document.

Alternative rejected: PRIVATE documents with per-request tokens. Every avatar or logo render would
cost a token issue plus a download, the browser could not cache the image, and the unauthenticated
login page could not show the logo at all.

#### Consequences
- The image URL is unauthenticated: anyone holding it can fetch the image until it is withdrawn or
  discarded. The trade-off is accepted because the slug is 192 random bits per tenant, the content
  is a photo or logo the user or tenant chose to display, and withdrawal (`discard`, or
  `PRIVATE`) stops the platform URL immediately. ADR-FILE-002 applies: with an S3 direct URL a
  CDN-cached copy is not revoked.
- The 1.2.0 publish rule is unchanged: a PUBLIC document needs a category with `ALLOW_PUBLIC`
  (`FileDocumentDomain.assertCanBePublic`). The category the image store uses, its tenant scope
  and its seed are fixed in `P2/db-script.md` → "Implementation Addendum — erp-core 1.3.0" before
  the migration and the code are written.
- SVG is excluded from the inline allow-list of the public endpoint
  (`FileDocumentDomain.INLINE_SAFE_CONTENT_TYPES`); a sanitised SVG logo is therefore served as an
  attachment unless that list is extended for sanitised SVG — a point the implementation must settle
  and record here.
- No new table, endpoint or permission in FILE; the consumers' endpoints and error codes
  (`SEC-400-PHOTO-INVALID`, `TENANT_LOGO_INVALID`) belong to SEC and TENANT.
- This ADR moves to ACCEPTED when the rows of the 1.3.0 addenda are verified against the code
  before the tag; a deviation is recorded there and in `docs/DEVIATIONS.md`.

#### Traces
ENTITY-FILE-001 · RULE-FILE-003 · XM-FILE-nnn (`FileImageStoreApi`) · publish rule and public serving
(`srs.md` → 1.2.0 addendum §2) · docs/plans/tenant-maturity-plan.md §D.4, §E.1, §E.2 ·
erp-core/src/main/java/com/erp/file/domain/FileDocumentDomain.java:57-61, 79-81 ·
erp-core/src/main/java/com/erp/file/service/PublicFileUrls.java:38-43 · docs/steps/07-report.md ·
docs/DEVIATIONS.md [07]

---

## Facts the implementers must match (verified against main @ 19b19a4, 2026-10-08)

1. **Image rules** (plan §D.4, both uses): `image/png`, `image/jpeg`, `image/webp`; `image/svg+xml` for logos only and only after sanitisation (`<script`, `on*=` attributes, external hrefs rejected); ≤ 1 MB; no server-side resize; replacing or removing an image discards the previous `FILE_DOCUMENT`; a logo row belongs to the tenant it depicts.
2. **JDK content sniffing**: `FileService.sniffContentType` uses `java.net.URLConnection.guessContentTypeFromStream` (`file/service/FileService.java:446-452`), which returns **null** for PDF, ZIP, OOXML (`.docx` / `.xlsx`) and OLE2 (`.doc` / `.xls`) content, and also for WebP and SVG. `FileValidationDomainService.assertContentTypeAllowed` (`:72-86`) rejects an unverifiable upload with 415 `FILE_DOCUMENT_TYPE_NOT_ALLOWED` **whenever the category carries an allow-list**, so a category allow-list naming those types rejects every such upload. The image store must therefore not rely on the sniffer alone for WebP / SVG, and the `ALLOW_PUBLIC` image category must not carry an allow-list the sniffer cannot satisfy.
3. **`TenantLookupApi` has only `Optional<String> codeOf(Long tenantId)` today** (`tenant/crossmodule/TenantLookupApi.java`); `boolean isActive(Long tenantId)` (C1, consumed by NOTIF's claim job) does not exist and is minted with package C1 as a CHANGED XM-TENANT-001.
4. **CU key-trim defect** (coverage review R1): `AppConfiguration`'s persist hook upper-cases the key but does not trim it, while `ConfigurationService.normalize` (`cu/service/ConfigurationService.java:233-235`) trims **and** upper-cases for the duplicate pre-check and the lookup — a key with surrounding spaces is stored as `" KEY"` and is unreachable. Not a plan package; whoever touches CU records it before fixing it.
5. **`GET /api/v1/tenant/me` is a path neither chain names today**: the customer chain's `securityMatcher` is `/api/v1/public/**`, `/api/v1/customers/**` (`autoconfigure/ErpCoreSecurityAutoConfiguration.java:173`); the core (staff) chain has no matcher and takes every other path, so `/api/v1/tenant/me` would reach the staff chain, where a CUSTOMER token is not served. Package E must name the chain (or add the path to the customer matcher) so that `isAuthenticated()` of any realm holds.
6. **The JWT filter does not expose `iat` today**: no `iat` / `getIssuedAt` handling exists under `sec/security/` (`JwtAuthenticationFilter`, `JwtTokenIssuer`); package C2 adds it to the authentication details before `TenantResolutionFilter` can compare it with `TOKENS_INVALID_BEFORE`.
7. **`CORE_IDEMPOTENCY_KEY` FK / index names are convention-derived, not in the plan**: `FK_CORE_IDEMPOTENCY_KEY_TENANT`, `IDX_CORE_IDEMPOTENCY_KEY_TENANT` follow the `FK_<TABLE>_TENANT` / `IDX_<TABLE>_TENANT` rule of `db/migration/core/README.md`; `PK_CORE_IDEMPOTENCY_KEY` and the `CREATED_AT` index name are bound at creation. Plan §5 C.4 names only the columns, `UQ_CORE_IDEMPOTENCY_KEY` and `SEQ_CORE_IDEMPOTENCY_KEY`. Whether the table carries `CREATED_BY` / `UPDATED_BY` / `UPDATED_AT` (COMMON's P2 draft) or only the plan's nine columns (TENANT's P2 draft) is decided by the entity base class the package chooses.
8. **As-built TENANT rule numbering**: RULE-TENANT-012 (request-tenant resolution order), 013 (thread / Hibernate-session binding), 014 (bootstrap tolerance, no root tenant), 015 (search allow-list, no caching); RULE-TENANT-010 / 011 are C3's. Every cross-reference in `governance/analysis/modules/TENANT/**` uses the renumbered ids; the earlier `docs/steps/tm-a-report.md` ("next … RULE-TENANT-010") is history.
9. **Migration directory** ended at `V15__audit_schema.sql` when the plan was written; packages derive `V16+` from the directory at creation time, never from this file.
