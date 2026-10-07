# Tenant maturity, user profile and tenant branding — execution plan

| | |
|---|---|
| Date | 2026-10-07 |
| Target version | erp-core `1.3.0` (backend, `main` is `1.3.0-SNAPSHOT`) · frontend `mxdashboard` next minor after `1.2.0` |
| Status | APPROVED scope, NOT STARTED — nothing below is implemented; every name is a **proposal bound at addendum time** |
| Owner | platform owner (hesham421) |
| Repositories | backend `newback` (this repo) · frontend `newfront` (`mxdashboard/`) |

This plan turns the 2026-10-07 discussion into ordered work packages. It is a plan, not analysis:
**no package starts coding before its analysis entries exist** (`CLAUDE.md` → "Analysis first").
Where the plan names a column, endpoint, permission or error code, that name is the proposal the
addendum will either confirm or replace; the code is written from the addendum, never from this file.

---

## 0. Decisions already taken (from the discussion)

| # | Decision |
|---|---|
| D1 | The TENANT module gets its own analysis folder `governance/analysis/modules/TENANT/` in the exact pattern of the existing modules, written **from the code as it is today** (V10, `com.erp.tenant`, `docs/steps/05-report.md`). This is package **A** and is the prerequisite of everything else. |
| D2 | Tenant maturity **level 1** (items 1–5) and **level 3** (items 11–16) are in scope. Level 2 (quotas, `ARCHIVED`, per-tenant self-signup switch, per-tenant rate limit, platform-set tenant settings) is **out of scope** for 1.3.0 and is listed in §9 for a later version. |
| D3 | A staff administrator can **set a user's password** from the Users screen. |
| D4 | A user gets a **photo** and a small set of extra profile fields (only the important ones: phone, job title, preferred language). |
| D5 | A tenant gets a **logo**, uploaded by the **platform administrator from the existing `PLATFORM_TENANTS` screen** (no new screen, module or permission); the UI shows the tenant logo **beside** the platform mark; when a tenant has no logo, the platform mark alone is shown. |
| D6 | Item 11 (`ScopedValue` for `TenantContext`) is a spike with a go/no-go ADR, never a silent rewrite (it touches `events` and `notif`). |
| D7 | **Gap confirmed 2026-10-07**: a role's **screen** and **action** grants cannot be revoked individually — the API has `DELETE /roles/{id}/modules/{moduleId}` only (`RoleGrantController`); the data registry (`srs-sec.md` rows ENT-SEC-008/009: "delete (revoke)") and the audit catalogue (`SCREEN_REVOKED`, `ACTION_REVOKED`) promise it, but `SCR-REQ-SEC-005` B5 never listed the endpoints, so they were never built. Today the only way to take one screen away is to revoke the whole module and re-grant the rest. Fixed as package **G**. |

---

## 1. Rules every package obeys

1. **Analysis first, then code, then check** — the four "after the code" steps of `CLAUDE.md` apply to every package: compare code ↔ addendum, regenerate `docs/api-docs/<module>/` + `check_completeness.py`, extend `docs/test-api/core-test-plan.md` + api-verify, add the `docs/CHANGELOG.md` line under `[Unreleased]`.
2. **Ids continue the module's sequence** from the last used number at writing time (today: `REQ-SEC-035`, `AC-SEC-035`, `RULE-SEC-007`, `ENT-SEC-013`; `RULE-FILE-007`, `XM-FILE-001`; TENANT starts at `-001` in package A). Nothing is renumbered.
3. **Migrations are additive** (`V<N>` in `erp-core/src/main/resources/db/migration/core/`): nullable or defaulted columns, new tables, seed rows, indexes. The next number is read from the directory at creation time — **today the directory ends at `V15`**, so the numbers below (`V16…V20`) are the expected sequence, not a promise.
4. **Business rules live in Domain objects** (`TenantDomain`, `UserDomain`/`User` rules), never inline in a service. Cross-module calls go through `crossmodule` packages only.
5. **Skill order for every backend feature**: `gov-enforce-backend-contract` → `build-create-entity` → … → `build-create-controller` → `gov-validate-backend-feature`.
6. **Frontend**: backend first for anything that needs an endpoint/field/permission/page code (`newfront/CLAUDE.md` §11c); the frontend documents its screens in `P2_5` addenda, builds against `docs/api-docs/` only, archives its specs to `governance/frontend/modules/<MOD>/tests/`.
7. **Secrets**: raw passwords are never logged, never returned, never audited (`@Audited(ignore = passwordHash)` stays; new password endpoints log only the user id).

---

## 2. Package map and order

```
A  TENANT analysis (from code)                 ── prerequisite, no code
B  Tenant level 1  (edit, admin reset, profile, suspension facts, usage)
C  Tenant level 3  (events, token cut-off, isolation tests, idempotency, export, ScopedValue spike)
D  Users: admin-set password, own password, photo, profile fields, staff /me
E  Tenant branding: logo (+ optional brand colour) set from the platform tenants screen, public branding endpoint
G  Role grants: revoke a single screen / action grant (closes the D7 gap)
F  Frontend: F1 users & my-profile · F2 dynamic shell (tenant logo) · F3 platform tenants screen upgrades (incl. logo) · F4 grant tree revoke
```

Dependencies: `B`, `C`, `D`, `E` all depend on `A`; `G` is independent (SEC only). `E` depends on the FILE cross-module addition in `D.4` (shared image-store API). `F1` ← `D`; `F2` ← `E`; `F3` ← `B` + `C` + `E`; `F4` ← `G`.
Recommended execution: **A → G (small, independent — can go first) → D.1–D.3 → B → E → C.1–C.4 → F1/F2/F3/F4 (each as soon as its backend package's api-docs are regenerated) → C.5 (export) → C.6 (ScopedValue spike)**.

Release: everything is additive, so it all ships as `1.3.0`. If `C.5`/`C.6` slip, they move to `1.4.0` without blocking the tag.

---

## 3. Package A — TENANT module analysis (prerequisite)

Create `governance/analysis/modules/TENANT/` mirroring SEC's file set, content derived **only** from the code, V10, `TenantConstants`, `TenantErrorCodes`, `TenantPermissions`, the api-docs and `docs/steps/05-report.md`:

| File | Content (as-built today) |
|---|---|
| `P0/platform-summary.md` | tenant's place in the platform: shared schema, `TENANT_ID` on every core table, `PLATFORM` (id 1), realms interplay |
| `P0/business-policies-tenant.md` | POL-TENANT-001…: code immutable and `^[A-Z0-9_]{3,32}$`; `ACTIVE`/`SUSPENDED` only; PLATFORM never suspended; provisioning atomic; no delete; platform-only management |
| `P0/module-registry-tenant.md` | module `PLATFORM`, screen `PLATFORM_TENANTS`, actions `PERM_PLATFORM_TENANTS_VIEW` + `PLATFORM_TENANT_MANAGE` (V10 §6) |
| `P0_5/prd-tenant.md` | the five operations and the provisioning story (first admin, SPI contributors SEC/MDL/NOTIF/SEQUENCE) |
| `P1/srs-tenant.md`, `P1/registry-srs-tenant.md` | `REQ-TENANT-001…`, `AC-TENANT-…`, `RULE-TENANT-…`, `ENT-TENANT-001` (Tenant), `XM-TENANT-001` (`TenantLookupApi`), `XM-TENANT-002` (`TenantProvisioningContributor`), screen `SCR-TENANT-001` = `PLATFORM_TENANTS`; the resolution order (path → token `tid` → `X-Tenant-Code` → exempt/public) with its error codes `TENANT_REQUIRED` 400 / `TENANT_NOT_FOUND` 404 / `TENANT_SUSPENDED` 403 / `TENANT_CONTEXT_MISSING` 500 / `TENANT_CODE_INVALID` / `TENANT_CODE_DUPLICATE` / `TENANT_PLATFORM_PROTECTED` |
| `P2/db-script-tenant.md`, `P2/registry-db-tenant.md` | `CORE_TENANT` exactly as V10 (`ID`, `CODE VARCHAR(32)`, `NAME_AR/EN VARCHAR(200)`, `STATUS_CODE VARCHAR(20)`, audit cols, `VERSION`), `SEQ_CORE_TENANT`, `PK_/UQ_/CHK_` names, the 18 `TENANT_ID` columns + FKs + indexes as `DBF-TENANT-…` |
| `governance/analysis/decisions/TENANT/ADR-TENANT-001.md` | "Discriminator (row-level) multi-tenancy over schema-per-tenant" — the as-built decision (reasons from step 05) |
| `governance/analysis/platform/project-registry.md` | the TENANT row already exists; add the pointer to the new folder |
| `governance/analysis/platform/PROJECT-OVERVIEW.md` | documentation map row |

Then packages B–E are written as `## Implementation Addendum — erp-core 1.3.0` sections **inside these new files**, exactly like SEC's 1.2.0 addendum (`Source version`, `Change`, `Statement`, tables with NEW/CHANGED/REMOVED).

Definition of done: the folder exists, every id is traceable to a code location, `governance/README.md` counts updated, one commit `docs(governance): TENANT module analysis — as-built from erp-core 1.2.0`.

---

## 4. Package B — Tenant level 1

### B.1 Schema (`P2/db-script-tenant.md` addendum → migrations)

| Migration (expected) | Table | Columns / objects | Notes |
|---|---|---|---|
| `V16__tenant_profile.sql` | `CORE_TENANT` | `CONTACT_EMAIL VARCHAR(255) NULL`, `CONTACT_PHONE VARCHAR(30) NULL`, `COUNTRY_CODE VARCHAR(2) NULL`, `DEFAULT_LOCALE VARCHAR(5) NULL`, `TIMEZONE VARCHAR(64) NULL`, `NOTES VARCHAR(1000) NULL` | all nullable; `CHK_CORE_TENANT_LOCALE CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar','en'))` |
| `V17__tenant_lifecycle.sql` | `CORE_TENANT` | `SUSPENDED_AT TIMESTAMPTZ NULL`, `SUSPENDED_BY VARCHAR(100) NULL`, `SUSPENSION_REASON VARCHAR(500) NULL`, `TOKENS_INVALID_BEFORE TIMESTAMPTZ NULL` | `TOKENS_INVALID_BEFORE` is used by C.2; shipped here so lifecycle is one script |

### B.2 Endpoints (all `PLATFORM_TENANT_MANAGE`, PLATFORM tenant only, `/api/v1/platform/tenants`)

| Kind | Method | Path | Body / result | Rules & errors |
|---|---|---|---|---|
| NEW | PUT | `/{id}` | `TenantUpdateRequest`: `nameAr`, `nameEn`, `contactEmail`, `contactPhone`, `countryCode`, `defaultLocale`, `timezone`, `notes` | `code` and `statusCode` are **not** in the body (immutable / separate endpoint); `TENANT_NOT_FOUND` 404; optimistic lock `CONCURRENT_MODIFICATION` 409 |
| CHANGED | PATCH | `/{id}/status` | `+ reason` (required when `SUSPENDED`, 3–500 chars; ignored when `ACTIVE`) | `TenantDomain.assertCanChangeStatusTo` unchanged; on suspend sets `SUSPENDED_AT/BY/REASON`; on activate clears them and sets `TOKENS_INVALID_BEFORE = now()` (C.2); `TENANT_SUSPENSION_REASON_REQUIRED` 400 |
| NEW | POST | `/{id}/admin-reset` | `{ username, newPassword }` → `{ username, sessionsTerminated }` | `username` must be a STAFF user of that tenant holding a role with `IS_SUPER = TRUE`; BCrypt-hashed; all that user's sessions terminated; audit `ADMIN_PASSWORD_RESET` in `CORE_AUDIT_EVENT` (actor = platform operator, target tenant = `{id}`); errors `TENANT_ADMIN_NOT_FOUND` 404, `TENANT_ADMIN_NOT_SUPER` 422 (BUSINESS_RULE_VIOLATION), `SEC-400-PASSWORD-POLICY` (D.1) |
| NEW | GET | `/{id}/usage` | `TenantUsageResponse`: `staffUsers`, `customerUsers`, `activeSessions`, `fileDocuments`, `fileBytes`, `notificationsLast30Days`, `collectedAt` | each count obtained inside `TenantContext.callAs(id)` through new cross-module methods (§B.4); never joins another module's tables |
| CHANGED | GET | `/{id}`, `/search`, list | `TenantResponse` + the profile fields + `suspendedAt`, `suspendedBy`, `suspensionReason` | search/sort allow-list gains `contactEmail`, `countryCode`, `suspendedAt` |

### B.3 Domain rules (`TenantDomain`)
- `RULE-TENANT-…` suspension requires a reason; activation clears it.
- `RULE-TENANT-…` admin reset target must be super-role STAFF of that tenant (the fact "holds super role" is passed in by the service, computed by SEC).

### B.4 Cross-module additions
| Owner | API | Method |
|---|---|---|
| SEC `crossmodule` | `SecUserDirectoryApi` | `int countStaff()`, `int countCustomers()`, `int countActiveSessions()` (current tenant) |
| SEC `crossmodule` | NEW `SecAdminRecoveryApi` | `int resetSuperUserPassword(String username, String rawPassword)` — runs in the current tenant (the tenant service wraps it in `callAs`), returns terminated-session count, throws the SEC error codes above |
| FILE `crossmodule` | `FileDocumentLookupApi` | `long countDocuments()`, `long sumBytes()` |
| NOTIF `crossmodule` | existing dispatch API | `long countDispatchedSince(Instant)` |

### B.5 Tests, docs
- `TC-CORE-TENANT-*` (next free): update happy path, update rejects `code`, suspend without reason 400, suspend stores facts, activate clears them, admin-reset unknown user 404, admin-reset non-super 422, admin-reset terminates sessions, usage for a fresh tenant = 1 staff user.
- api-verify `tenant`; CHANGELOG lines; `PROJECT-OVERVIEW.md` tenancy paragraph.

---

## 5. Package C — Tenant level 3

### C.1 Lifecycle events (item 15)
- NEW `TenantSuspendedEvent(tenantId, code, reason, actor)` and `TenantActivatedEvent(tenantId, code, actor)` in `com.erp.events`, published by `TenantService.updateStatus` after commit (`@TransactionalEventListener` consumers), registered in `ErpCoreEvents` (count 10 → 12 in `PROJECT-OVERVIEW.md`).
- NOTIF: the retry/claim job **skips** rows of suspended tenants (it reads `TenantLookupApi.isActive(tenantId)`), and a `TenantSuspendedEvent` listener marks the tenant's `PENDING` deliveries as `DEFERRED`? — **no**: keep the status set unchanged (`REJECTED`/`FAILED` exist); the job simply does not claim them while suspended. Document in `NOTIF` P1 addendum.
- SEC: `TenantSuspendedEvent` listener terminates every active session of the tenant (today tokens are refused by the filter, but `SEC_ACTIVE_SESSION` rows stay open — this closes them).

### C.2 Token cut-off on re-activation (item 14)
- `CORE_TENANT.TOKENS_INVALID_BEFORE` (V17). `TenantResolutionFilter` already loads the tenant for every authenticated request: add "if `iat` of the token `< TOKENS_INVALID_BEFORE` → 401 `TENANT_TOKEN_REVOKED`" (the JWT filter exposes `iat` on the authentication details).
- NEW `POST /api/v1/platform/tenants/{id}/revoke-tokens` → sets the cut-off to `now()` and terminates sessions (`PLATFORM_TENANT_MANAGE`), audit `TOKENS_REVOKED`.
- ADR-TENANT-002: "Per-tenant token cut-off instead of a token blacklist" (alternatives: Redis denylist per `jti`; rejected for the platform-wide case).

### C.3 Automated isolation tests (item 12)
- ArchUnit `TenantScopedEntityTest`: every `@Entity` under `com.erp` extends `AuditableEntity` (carries `@TenantId`) **except** the documented global set (`Tenant`, `ModuleRegistry`, `ScreenRegistry`, `ActionRegistry`, the `GlobalAuditableEntity` subclasses). A new global entity must be added to the list explicitly — the test message says so.
- Integration `TenantIsolationIT` (Testcontainers/embedded PG): provision two tenants, create one row per tenant in SEC, MDL, FILE, NOTIF, CU, SEQUENCE, AUDIT; assert each tenant's search/getById sees only its own; assert a cross-tenant id answers 404.
- Review rule in `governance/rules/GOVERNANCE-RULES.md`: every `JdbcTemplate` statement names `TENANT_ID`; `TenantProvisioningContributor` Javadoc already says it — the rule makes it a checklist item of `gov-validate-backend-feature`.

### C.4 Idempotent provisioning (item 13)
- `V20__core_idempotency_key.sql`: `CORE_IDEMPOTENCY_KEY (ID BIGINT PK, TENANT_ID BIGINT NOT NULL FK, IDEMPOTENCY_KEY VARCHAR(64) NOT NULL, ENDPOINT VARCHAR(200) NOT NULL, REQUEST_HASH VARCHAR(64) NOT NULL, RESPONSE_STATUS INT NOT NULL, RESPONSE_BODY TEXT, CREATED_AT TIMESTAMPTZ NOT NULL DEFAULT now(), VERSION BIGINT NOT NULL DEFAULT 0)`, `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)`, `SEQ_CORE_IDEMPOTENCY_KEY`, index on `CREATED_AT`.
- Behaviour on `POST /api/v1/platform/tenants` only (v1): header `Idempotency-Key` (optional, ≤ 64 chars). Same key + same body hash → the stored response is replayed with header `Idempotent-Replayed: true`; same key + different body → 409 `IDEMPOTENCY_KEY_CONFLICT`; retention 24 h via the existing scheduled-job pattern (audit retention job as precedent).
- Lives in `com.erp.common.idempotency` (mechanism) + `tenant` (first consumer). ADR-TENANT-003.

### C.5 Tenant data export (item 16)
- SPI `TenantExportContributor { String moduleCode(); void export(TenantExport ctx) }` in `com.erp.tenant`, mirroring the provisioning SPI; each core module writes its tenant rows as CSV streams into the context (never the password hash, never file bytes — file metadata only).
- `POST /api/v1/platform/tenants/{id}/export` → runs synchronously in v1 (bounded: refuse > N rows with `TENANT_EXPORT_TOO_LARGE` 422, N configurable `erp.core.tenant.export.max-rows`, default 200 000), zips the CSVs, stores the ZIP as a **PRIVATE** `FILE_DOCUMENT` (`ownerType = CORE_TENANT`, `ownerId = {id}`, `moduleCode = TENANT`) in the **PLATFORM** tenant, returns `{ fileId, downloadToken }` (single-use token, existing FILE mechanism); audit `TENANT_EXPORTED`.
- Second export while one is running for the same tenant → 409 `TENANT_EXPORT_IN_PROGRESS` (in-memory guard keyed by tenant id).
- Can slip to 1.4.0 without blocking the rest.

### C.6 `ScopedValue` spike (item 11) — go/no-go, not a rewrite
- Branch `spike/tenant-scoped-value`: replace the `ThreadLocal` in `TenantContext` behind the **same public API** (`current/find/require/set/clear/runAs/callAs`); keep `set/clear` working for the servlet filter by a bounded `ScopedValue.where(...).run(chain)` in the filter.
- Must pass: full `mvn verify`, `TenantIsolationIT`, the async-executor tests (`TenantAndSecurityContextTaskDecorator`), NOTIF claim job under virtual threads (`spring.threads.virtual.enabled=true`).
- Output: ADR-TENANT-004 with the measurement (context leak tests, p95 of the filter) and the decision. If "go": merged as its own PR; if "no-go": the ADR records why and the item closes.

---

## 6. Package D — Users: passwords, photo, profile, staff `/me`

### D.1 Schema (`P2/db-script-sec.md` addendum, expected `V19__sec_user_profile.sql`)

| Column | Type | Notes |
|---|---|---|
| `SEC_USER.PHONE` | `VARCHAR(30) NULL` | E.164-ish, validated by regex in the DTO |
| `SEC_USER.JOB_TITLE_AR` / `JOB_TITLE_EN` | `VARCHAR(150) NULL` | bilingual like every label |
| `SEC_USER.PREFERRED_LOCALE` | `VARCHAR(5) NULL` | `CHK_SEC_USER_LOCALE IN ('ar','en')`; the frontend applies it at login |
| `SEC_USER.PHOTO_FILE_ID` | `BIGINT NULL` | **soft reference** to `FILE_DOCUMENT.ID` (no FK — same convention as `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID`, XM-NOTIF-002); validated via `FileDocumentLookupApi.isAvailable` |
| `SEC_USER.PASSWORD_CHANGE_REQUIRED_FL` | `BOOLEAN NOT NULL DEFAULT FALSE` | set by admin-set password; cleared when the user changes it |
| `SEC_USER.PASSWORD_CHANGED_AT` | `TIMESTAMPTZ NULL` | audit fact, shown on demand |

Password policy (one place, `PasswordPolicy` in `com.erp.sec.domain`, used by create, reset, admin-set, self-change, tenant admin-reset): length 8–200, at least one letter and one digit; property `erp.core.security.password-policy.*` with these defaults. Error `SEC-400-PASSWORD-POLICY`.

### D.2 Endpoints (SEC)

| Kind | Method | Path | Access | Body / result | Rules & errors |
|---|---|---|---|---|---|
| NEW | PUT | `/api/v1/sec/users/{id}/password` | `PERM_SEC_USERS_UPDATE` | `{ newPassword, requireChangeAtNextLogin: true }` → `UserStatusResponse` | STAFF only (404 `SEC-404-USER` otherwise); cannot target **yourself** (use `/me/password`) → 422 `SEC-422-PASSWORD-SELF`; hash, set `PASSWORD_CHANGED_AT`, set the flag per body (default **true**), terminate the user's sessions, audit `PASSWORD_SET_BY_ADMIN` (no secret), publish `UserPasswordChangedEvent` (NOTIF may e-mail the user) |
| NEW | GET | `/api/v1/sec/me` | `isAuthenticated()` STAFF realm | `StaffProfileResponse`: `userPk`, `username`, `email`, `fullNameAr/En`, `phone`, `jobTitleAr/En`, `preferredLocale`, `photoUrl` (nullable), `passwordChangeRequired`, `lastLoginAt`, `tenant { code, nameAr, nameEn }` | **no roles, no permissions** in the payload (ADR-SEC-005: the effective menu is the only client authority) |
| NEW | PATCH | `/api/v1/sec/me` | same | `fullNameAr`, `fullNameEn`, `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale` | email/username are admin-only, stay on `PUT /users/{id}` |
| NEW | PUT | `/api/v1/sec/me/password` | same | `{ currentPassword, newPassword }` | current must match → else 403 `SEC-403-PASSWORD-CURRENT-INVALID`; clears the flag; terminates **other** sessions; audit `PASSWORD_CHANGED` |
| NEW | PUT | `/api/v1/sec/me/photo` | same | multipart `file` → `{ photoUrl }` | image rules in D.4; replaces the previous photo (old document soft-deleted) |
| NEW | DELETE | `/api/v1/sec/me/photo` | same | 204 | |
| NEW | PUT | `/api/v1/sec/users/{id}/photo` · DELETE | `PERM_SEC_USERS_UPDATE` | same as `/me/photo` for another staff user | |
| CHANGED | PUT | `/api/v1/sec/users/{id}` | as before | `+ phone, jobTitleAr, jobTitleEn, preferredLocale` | |
| CHANGED | POST | `/api/v1/sec/users` | as before | `+` the same optional fields; `requireChangeAtNextLogin` default **true** for admin-created accounts | decision row — see §8 |
| CHANGED | GET/search | users | `UserResponse` `+ phone, jobTitleAr/En, preferredLocale, photoUrl, passwordChangeRequired, passwordChangedAt` | |
| CHANGED | POST | `/api/v1/sec/auth/login` | public | response `+ passwordChangeRequired` | |

**Forced change enforcement**: while `PASSWORD_CHANGE_REQUIRED_FL = TRUE`, every STAFF endpoint except `GET /sec/me`, `PUT /sec/me/password`, `POST /sec/auth/logout` answers 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` (a filter after authentication, same envelope as `FilterErrorResponseWriter`). The frontend routes the user to the change-password page on `passwordChangeRequired = true`.

### D.3 Events, audit
- NEW `UserPasswordChangedEvent(userId, byAdmin)`; NOTIF template `STAFF_PASSWORD_CHANGED` (seed in NOTIF, copied by its provisioning contributor).
- `CORE_AUDIT_EVENT` actions: `PASSWORD_SET_BY_ADMIN`, `PASSWORD_CHANGED`, `PROFILE_PHOTO_CHANGED`.

### D.4 FILE cross-module addition (shared by photos and logos)
- NEW `com.erp.file.crossmodule.FileImageStoreApi`:
  `StoredImage storePublicImage(ImageStoreRequest r)` where the request carries `ownerType`, `ownerId`, `moduleCode`, `bytes`, `contentType`, `fileName`, `maxBytes`, `allowedTypes`, and the result carries `documentId`, `publicUrl`; plus `void discard(Long documentId)`.
  Internally: validates type/size (`SEC-400-PHOTO-INVALID` / `TENANT_LOGO_INVALID` raised by the caller from a `FILE` validation result, never a raw exception), stores through the configured `StorageProvider`, publishes (`VISIBILITY = PUBLIC`, random slug) — reuses step 07's public-file path, no new table.
- Image rules (both uses): `image/png`, `image/jpeg`, `image/webp`, `image/svg+xml` (SVG **logos only**, sanitised: reject `<script`, `on*=` attributes, external hrefs); photo ≤ 1 MB, logo ≤ 1 MB; the server never resizes in v1 (frontend constrains the preview).
- ADR-FILE-001 (first FILE ADR): "Profile photos and logos are PUBLIC documents with non-guessable slugs" — alternatives: PRIVATE + per-request tokens (every avatar render would need a token round-trip). Trade-off recorded: the URL is unauthenticated; the slug is random and per tenant; `DELETE` withdraws it.

### D.5 Tests
`TC-CORE-SEC-*`: admin-set password → login works, flag true, other endpoints 403 until self-change; self-change wrong current 403; self-change clears flag; admin-set on self 422; photo upload png ok / exe rejected / > 1 MB rejected; `/me` has no `roles` field; PUT users accepts phone/locale; locale `fr` rejected.

---

## 7. Package E — Tenant branding and dynamic UI

### E.1 Schema (expected `V18__tenant_branding.sql`)

| Object | Definition |
|---|---|
| `CORE_TENANT.LOGO_FILE_ID BIGINT NULL` | soft reference to `FILE_DOCUMENT.ID` (document owned by `ownerType = CORE_TENANT`, `ownerId = tenant id`, `moduleCode = TENANT`, **stored in the tenant's own rows**, PUBLIC) |
| `CORE_TENANT.BRAND_COLOR VARCHAR(7) NULL` | optional accent `#RRGGBB`, `CHK_CORE_TENANT_BRAND_COLOR CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$')` — optional part of D5; the frontend uses it only if present |
| registry | **nothing new** (D5): the logo is managed under the existing `PLATFORM_TENANTS` screen and `PLATFORM_TENANT_MANAGE`; no module, screen, permission or grant seed |

The logo document itself is stored **in the target tenant's rows** (`FILE_DOCUMENT.TENANT_ID = {id}`, written inside `TenantContext.callAs(id)` so the public URL is `/api/v1/public/files/{thatTenantCode}/{slug}`), owned by `ownerType = CORE_TENANT`, `ownerId = {id}`, `moduleCode = TENANT`.

### E.2 Endpoints (`com.erp.tenant`: `PlatformTenantController` additions + a public read)

| Kind | Method | Path | Access | Body / result | Rules & errors |
|---|---|---|---|---|---|
| NEW | PUT | `/api/v1/platform/tenants/{id}/logo` | `PLATFORM_TENANT_MANAGE` | multipart `file` → `TenantResponse` | via `FileImageStoreApi` (D.4) as tenant `{id}`; replaces and discards the previous logo; `TENANT_NOT_FOUND` 404, `TENANT_LOGO_INVALID` 400; audit `TENANT_LOGO_CHANGED` (actor = platform operator) |
| NEW | DELETE | `/api/v1/platform/tenants/{id}/logo` | same | 204 | client falls back to the platform mark |
| NEW | PATCH | `/api/v1/platform/tenants/{id}/branding` | same | `{ brandColor }` (null clears) | `TENANT_BRAND_COLOR_INVALID` 400 |
| NEW | GET | `/api/v1/tenant/me` | `isAuthenticated()` (any realm) | `TenantBrandingResponse`: `code`, `nameAr`, `nameEn`, `logoUrl` (nullable), `brandColor` (nullable), `defaultLocale` | the token's tenant; **read-only**, no contact/profile fields; what the shell loads after login |
| NEW | GET | `/api/v1/public/tenants/{tenantCode}/branding` | **public, no auth** | same `TenantBrandingResponse` | tenant resolved **from the path** (added to the `erp.core.tenant.path-tenant-paths` defaults, same mechanism as public files, step 07); unknown → 404 `TENANT_NOT_FOUND`; suspended → 403 `TENANT_SUSPENDED`; rate-limited like customer login (bucket per IP, `erp.core.tenant.public-branding-rate-limit.*`) — what the login page calls before any token exists |
| CHANGED | `TenantResponse` (platform) | | `+ logoUrl, brandColor` | |

### E.3 Domain rules
- `RULE-TENANT-…` a logo is `PUBLIC`, ≤ 1 MB, PNG/JPEG/WebP/SVG(sanitised); one logo per tenant; replacing or removing the logo discards the previous document.
- `RULE-TENANT-…` the PLATFORM tenant may also carry a logo (it is a tenant); the platform **mark** in the frontend is a static asset, never a tenant logo.
- `RULE-TENANT-…` the logo endpoints are platform-only (D5); a tenant administrator has no write path to branding in 1.3.0.

### E.4 Tests
`TC-CORE-TENANT-*`: platform admin uploads logo for tenant T → `GET /platform/tenants/{id}.logoUrl` set, T's own `GET /tenant/me.logoUrl` equal, the public URL serves it under T's code; public branding for unknown code 404; suspended tenant 403; a tenant admin calling the logo endpoints → 403 (`PLATFORM_TENANT_MANAGE` is platform-only); replace discards the previous document; DELETE → `logoUrl` null; SVG with `<script>` rejected.

---

## 8. Package F — Frontend (`newfront/mxdashboard`)

Backend first: each sub-package is **BLOCKED** until the corresponding backend package's `docs/api-docs/<mod>/` is regenerated and the gap rows in `newfront/docs/api-doc-gaps.md` are `RESOLVED`. Everything below is written into `P2_5` addenda (`governance/analysis/modules/<MOD>/P2_5/`, TENANT gets its first `ui-ux-spec-tenant.md` + `flow-diagram-tenant.md`) and `docs/test-e2e/front-test-plan.md` **before** code (`newfront/CLAUDE.md` §11c).

### F1 — Users and my profile (module `security`)

| Item | Spec |
|---|---|
| Users list (`/security/users`) | row avatar (photo or initials), new columns hidden by default: phone, job title |
| User drawer (`?action=create`, `?editId=`) | `+ phone, jobTitleAr, jobTitleEn, preferredLocale`; **photo** as a summary row that opens a second-level drawer `?photoFor=<id>` with file input + preview + remove (one drawer, one job) |
| Set password | action in the user detail drawer → second-level drawer `?passwordFor=<id>` (`newPassword`, confirm, "require change at next login" default on); it is a form, so a Drawer, not a dialog; success toast `users.toastPasswordSet` (both languages) |
| Login flow | `passwordChangeRequired = true` → redirect to `/account/change-password` (new route, authentication-only guard); until changed, every other route redirects there |
| My profile | route `/account/profile` (authentication-only: a decision row — `ProtectedRoute` is page-code based; the profile is for every authenticated staff user, so the route uses the authentication-only form documented in the addendum); `GET/PATCH /sec/me`, photo second-level drawer, change-password second-level drawer `?changePassword=1`; `preferredLocale` saved → `useTranslation` locale switches at once |
| Topbar | avatar menu (photo / initials) from `['me']` query; cleared on sign-out |
| i18n | keys for every new field, both dictionaries |
| Tests | `TC-FE-SEC-013…` set password, forced change redirect, profile edit, photo upload/remove, locale switch |

### F2 — Dynamic shell: tenant logo beside the platform mark (shell code under `src/app/`, no new module)

| Item | Spec |
|---|---|
| Shell (`Sidebar.tsx` brand header) | query `['tenant','branding']` (`GET /api/v1/tenant/me`) at shell mount; render **`<TenantLogo>`** beside the platform mark when `logoUrl` is present: `[platform mark] · [tenant logo]` (height 28px, `object-fit: contain`, `onError` hides it and falls back to mark-only); no logo → today's markup unchanged. Collapsed rail: tenant logo only when present, else mark |
| Brand colour | if `brandColor` present, set `--color-accent-tenant` on `:root` and use it for the sidebar active state only (no global repaint in v1) |
| Login / sign-up / reset pages | after the tenant code field settles (debounced, or from `VITE_TENANT_HOST_SUFFIX`), call `GET /api/v1/public/tenants/{code}/branding`; show the tenant logo above the form; 404 → mark only, no error toast (the login submit will produce the real tenant error) |
| Cache | branding is tenant-scoped: keyed by tenant code, cleared on sign-out and on tenant code change |
| Tests | `TC-FE-XCUT-…` (shell) shows both logos, fallback on no logo, login page shows tenant logo, collapsed rail behaviour, cache cleared on sign-out |

### F3 — Platform tenants screen (module `platform`)

| Item | Spec |
|---|---|
| Edit | `?editId=` drawer with names + profile fields (code read-only, locked skin) |
| Logo (D5) | in the detail drawer, a **branding summary row** (current logo preview or "platform mark only") that opens a second-level drawer `?logoFor=<id>` with file input + preview + constraints text + optional brand colour; **remove** is a sensitive action → `<ConfirmDialog>`; toasts `tenants.toastLogoSaved` / `tenants.toastLogoRemoved` in both languages |
| Suspend | `TenantSuspendConfirm` gains the mandatory reason (confirm dialog with a text field is still a sensitive-action confirmation, allowed) |
| Detail drawer | suspension facts (`suspendedAt/By/reason`), usage section (collapsible, loads `GET /{id}/usage` on open) |
| Admin reset | action → second-level drawer `?adminResetFor=<id>` (username, new password); sensitive, so a final `<ConfirmDialog>` before submit |
| Revoke tokens · Export | actions with `<ConfirmDialog>`; export result offers the download (single-use token) |
| Tests | `TC-FE-PLATFORM-007…` incl. upload/replace/remove logo, invalid file rejected, logo visible after re-login as that tenant |

### F4 — Grant tree: uncheck a screen or an action (module `security`, screen `SEC_ROLES`)

| Item | Spec |
|---|---|
| Tree behaviour | unchecking a **screen** → `DELETE /roles/{id}/screens/{screenId}` behind a `<ConfirmDialog>` naming the action grants that will cascade (count from the tree); unchecking an **action** → `DELETE /roles/{id}/actions/{actionId}`; unchecking the **VIEW** action of a screen warns that the screen's other actions cascade (RULE-SEC-007 gateway) |
| Super roles | for a role with `isSuper = true` the tree shows the hint "this role holds every authority regardless of grants; grants only shape its menu" (the backend already behaves so) |
| Tests | `TC-FE-SEC-0NN…` revoke one screen (actions cascade), revoke one action, revoke VIEW cascades, module revoke unchanged |

---

## 8b. Package G — Revoke a single screen or action grant (closes D7)

Analysis: `srs-sec.md` 1.3.0 addendum rows for `SCR-REQ-SEC-005` B5 (the two missing endpoints), `REQ-SEC-036` (revoke screen grant, cascades its action grants) and `REQ-SEC-037` (revoke action grant; revoking `VIEW` cascades the screen's other actions — the inverse of RULE-SEC-007), `RULE-SEC-008` (screen-revoke cascade), `RULE-SEC-009` (VIEW-revoke cascade); no schema change; audit actions `SCREEN_REVOKED` / `ACTION_REVOKED` already in the catalogue.

| Kind | Method | Path | Access | Result | Rules & errors |
|---|---|---|---|---|---|
| NEW | DELETE | `/api/v1/sec/roles/{id}/screens/{screenId}` | `PERM_SEC_ROLES_UPDATE` | `ScreenGrantRevokeResponse { revokedActionGrants }` | 404 `SEC-404-ROLE` / `SEC-404-GRANT` when the role or the grant does not exist in the caller's tenant; deletes the screen grant and every action grant of that screen for that role, in one transaction; audit `SCREEN_REVOKED` + one `ACTION_REVOKED` per cascaded action (same pattern as `revokeModule`) |
| NEW | DELETE | `/api/v1/sec/roles/{id}/actions/{actionId}` | same | `ActionGrantRevokeResponse { revokedActionGrants }` | if the action is the screen's `VIEW`, the screen's other action grants cascade (RULE-SEC-009) — the response count tells the client; otherwise exactly one row |
| unchanged | DELETE | `/modules/{moduleId}` | | | RULE-SEC-003 cascade as today |

Domain: the cascade decisions go into the existing grant Domain objects (`RoleScreenGrantDomain` / `RoleActionGrantDomain`), not the service. Sessions are not terminated (grants are re-read per request). A super role (`IS_SUPER`) keeps every authority regardless — only its menu changes; document this in the addendum since it surprises administrators.

Tests `TC-CORE-SEC-*`: revoke screen cascades N actions and audits N+1 rows; revoke non-VIEW action removes one; revoke VIEW cascades; unknown grant 404; other tenant's role 404; module revoke still cascades.

---

## 9. Decision points (ADRs to write before the related code)

| ADR | Question | Recommendation |
|---|---|---|
| ADR-TENANT-001 | as-built: row-level multi-tenancy | record (package A) |
| ADR-TENANT-002 | token cut-off vs `jti` denylist | per-tenant cut-off (`TOKENS_INVALID_BEFORE`) |
| ADR-TENANT-003 | idempotency storage | DB table in `common`, 24 h retention, first consumer = tenant create |
| ADR-TENANT-004 | `ScopedValue` go/no-go | decided by the spike (C.6) |
| ADR-TENANT-005 | who sets a tenant's logo | **decided (D5)**: the platform administrator from `PLATFORM_TENANTS`; no tenant self-service screen in 1.3.0 (alternative recorded: a per-tenant `TENANT_BRANDING` screen — more rows in the registry and grants, deferred) |
| ADR-SEC-041 | revoking `VIEW` cascades the screen's other actions vs. refusing while they exist | cascade with a counted response (consistent with the module revoke, RULE-SEC-003) |
| ADR-FILE-001 | photo/logo visibility | PUBLIC non-guessable slug (reuse step 07), not PRIVATE + tokens |
| ADR-SEC-039 | admin-set password forces a change at next login | yes, default `true`, enforced server-side by the 403 filter |
| ADR-SEC-040 | staff `/me` payload excludes roles/permissions | yes (ADR-SEC-005) |
| Frontend decision rows (P2_5) | authentication-only routes for `/account/*`; logo placement beside the mark; logo upload lives in the tenant detail drawer | as in §8 |

Out of scope for 1.3.0 (level 2, for a later plan): quotas (`MAX_USERS`, `MAX_STORAGE_MB`), `ARCHIVED` status, per-tenant self-signup switch, per-tenant rate-limit keys, platform-set tenant settings screen.

---

## 10. Definition of done per package (checklist)

- [ ] Analysis entries exist (addendum rows, schema entry with the migration file name, ADRs) **before** the first code commit.
- [ ] Code matches the entry item by item (names, widths, paths, permissions, error codes, migration number); deliberate deviations are in the addendum **and** `docs/DEVIATIONS.md`.
- [ ] `mvn -q verify` green (ArchUnit incl. `TenantScopedEntityTest`, `MigrationNamingTest`, JaCoCo ≥ 60 %).
- [ ] `docs/api-docs/<module>/` regenerated from the running app; `check_completeness.py` clean; every endpoint of the addendum present.
- [ ] `docs/test-api/core-test-plan.md` extended; api-verify run archived under `docs/test-api/`.
- [ ] `docs/CHANGELOG.md` `[Unreleased]` line(s).
- [ ] Frontend: P2_5 addendum, `front-test-plan.md` cases, specs green and archived to `governance/frontend/modules/<MOD>/tests/`, `docs/ui-verify/<MOD>/` output, changelog.

---

## 11. Expected migration sequence (to be re-derived from the directory at creation time)

| Expected | Package | Content |
|---|---|---|
| `V16__tenant_profile.sql` | B | profile columns on `CORE_TENANT` |
| `V17__tenant_lifecycle.sql` | B / C.2 | suspension facts + `TOKENS_INVALID_BEFORE` |
| `V18__tenant_branding.sql` | E | `LOGO_FILE_ID`, `BRAND_COLOR` (+ check constraint); no registry rows |
| `V19__sec_user_profile.sql` | D | phone, job titles, locale, photo ref, password flags |
| `V20__core_idempotency_key.sql` | C.4 | idempotency table |

NOTIF seed for `STAFF_PASSWORD_CHANGED` (D.3) is a seed script of its own if it lands separately (`V21__notif_seed_password_changed.sql`), otherwise part of V19's package — decided when written, one logical change per file either way.
