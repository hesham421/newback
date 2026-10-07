# Tenant-maturity package D — Users: passwords, photo, profile, staff `/me` — report

Branch `tm/d-sec-user-profile-passwords`, from `main` `36c17db` (packages A and G merged). erp-core 1.3.0-SNAPSHOT.
Plan: `docs/plans/tenant-maturity-plan.md` §6 D.1–D.5 (plus §0 D3/D4, §9 rows ADR-SEC-039/040 and ADR-FILE-001).

## Summary

- **Admin-set password** `PUT /api/v1/sec/users/{id}/password` (`PERM_SEC_USERS_UPDATE`, STAFF targets only, never
  one's own → 422 `SEC-422-PASSWORD-SELF`): hashes, ends every session of the user, and by default flags the account
  `passwordChangeRequired`. Admin-created users (`POST /api/v1/sec/users`) are flagged by default too.
- **Forced-change gate**: `PasswordChangeRequiredFilter` in the staff chain (after `RealmEnforcementFilter`) answers
  403 `SEC-403-PASSWORD-CHANGE-REQUIRED` for every call of a flagged caller except `GET /sec/me`,
  `PUT /sec/me/password`, `POST /sec/auth/logout` and the public paths. The flag comes from the `SEC_USER` row
  `JwtAuthenticationFilter` already loads (on `AuthRealm` details) — no extra query, no token claim (ADR-SEC-063).
- **Own password** `PUT /api/v1/sec/me/password` (current password → else 403 `SEC-403-PASSWORD-CURRENT-INVALID`):
  clears the flag, ends the user's *other* sessions, the calling token keeps working.
- **Password policy** (`PasswordPolicy`, `com.erp.sec.domain`; `erp.core.security.password-policy.*`, 8..200,
  a letter and a digit): user create, reset completion, admin-set, own change and a new tenant's first
  administrator → 400 `SEC-400-PASSWORD-POLICY` naming the field. Customers keep their length rule.
- **Staff `/me`** `GET`/`PATCH` (no roles or permissions, ADR-SEC-064), profile fields `phone`, `jobTitleAr/En`,
  `preferredLocale` (`ar`/`en`) on user create/update/response, `photoUrl`, `passwordChangeRequired`,
  `passwordChangedAt`; login answers `passwordChangeRequired`.
- **Photos** `PUT/DELETE /api/v1/sec/me/photo`, `PUT/DELETE /api/v1/sec/users/{id}/photo` through the new FILE
  **`FileImageStoreApi`** (D.4): PNG/JPEG/WebP ≤ 1 MB detected from the bytes, stored uncategorised and PUBLIC under a
  random slug on step 07's public path, the previous photo discarded (URL 404 at once); 400 `SEC-400-PHOTO-INVALID`.
- **Events / NOTIF / audit**: `UserPasswordChangedEvent(userId, byAdmin)` (11th core event); NOTIF's
  `StaffPasswordChangedNotifier` e-mails template `STAFF_PASSWORD_CHANGED` (V17 seed, every tenant; copied to new
  tenants); `CORE_AUDIT_EVENT` actions `PASSWORD_SET_BY_ADMIN`, `PASSWORD_CHANGED`, `PROFILE_PHOTO_CHANGED`;
  `SESSION_TERMINATED` SEC audit rows per ended session.
- Migrations `V16__sec_user_profile.sql`, `V17__notif_seed_password_changed.sql`. 14 HTTP cases
  (`TC-CORE-SEC-041..054`), 9 new JUnit classes; full P-LIVE run 170/170 PASS; api-docs regenerated (115 ops).

## Analysis entries written

| File | Section | Ids |
|---|---|---|
| `governance/analysis/modules/SEC/P1/srs-sec.md` | 1.3.0 addendum §9 (appended after G's §1–§8) | REQ-SEC-082..089, AC-SEC-088..095, RULE-SEC-056..062, ENT-SEC-001 CHANGED, SCR-REQ-SEC-004 CHANGED, endpoints, audit, XM-SEC-006, decisions, frontend impact |
| `SEC/P1/registry-srs-sec.md` | 1.3.0 addendum (package D block) | same ids; last sequence REQ 089 · AC 095 · RULE 062 · DBF 123 · XM 006 · ADR 064 |
| `SEC/P0/business-policies-sec.md` | 1.3.0 rows #4–#8 | — |
| `SEC/P0/module-registry-sec.md`, `SEC/P0_5/prd-sec.md` | new 1.3.0 addenda | — |
| `SEC/P2/db-script-sec.md`, `SEC/P2/registry-db-sec.md` | new 1.3.0 addenda | DBF-SEC-117..123 (V16, widths, `CHK_SEC_USER_LOCALE`), XM-SEC-006 |
| `FILE/P1/srs.md`, `registry-srs-file.md`, `P0/business-policies-file.md`, `P0/module-registry-file.md`, `P2/db-script.md` | new 1.3.0 addenda | XM-FILE-002 (NEW), XM-FILE-001 (CHANGED, `publicUrls`), RULE-FILE-008..010, open points (SVG inline, category) |
| `NOTIF/P1/srs.md`, `registry-srs-notif.md`, `P2/db-script.md`, `registry-db-notif.md`, `P0_5/prd-notif.md` | new 1.3.0 addenda | RULE-NOTIF-023, XM-NOTIF-003, V17 seed |
| `TENANT/P1/srs-tenant.md`, `registry-srs-tenant.md` | new 1.3.0 addenda (package D rows) | XM-TENANT-001 CHANGED (`summaryOf`), REQ-TENANT-001 CHANGED (admin password policy) |
| `decisions/SEC/ADR-SEC-063.md` | NEW | plan ADR-SEC-039 |
| `decisions/SEC/ADR-SEC-064.md` | NEW | plan ADR-SEC-040 |
| `decisions/FILE/ADR-FILE-008.md` | NEW | plan ADR-FILE-001 (FILE ADRs 001..007 are the analysis-coverage work's as-built ADRs) |

Ids were checked against the tree, this repository's full history and `governance-shared` (all history): FILE
RULE 007 / XM 001 / API 008 → RULE-FILE-008, XM-FILE-002; NOTIF RULE 008 (governance-shared) / XM 002 → RULE-NOTIF-023,
XM-NOTIF-003; SEC per `notes-for-later.md`.

## Files changed

Created (main): `events/UserPasswordChangedEvent`; `file/crossmodule/{FileImageStoreApi, FileImageStoreApiImpl,
ImageRejection, ImageStoreRequest, ImageStoreResult, StoredImage}`; `file/domain/ImageValidationDomainService`;
`file/service/FileImageStoreService`; `notif/service/StaffPasswordChangedNotifier`;
`sec/controller/StaffProfileController`; `sec/domain/PasswordPolicy`; `sec/dto/{AdminPasswordSetRequest,
PasswordChangeRequest, PasswordChangeResponse, ProfilePhotoResponse, StaffProfileConstraints, StaffProfileResponse,
StaffProfileTenantResponse, StaffProfileUpdateRequest}`; `sec/security/PasswordChangeRequiredFilter`;
`sec/service/{PasswordPolicyProvider, StaffProfileService, UserPasswordService, UserPhotoUrls,
UserSessionTerminator}`; `tenant/crossmodule/TenantSummary`; `V16__sec_user_profile.sql`,
`V17__notif_seed_password_changed.sql`.

Created (test): `sec/{AbstractStaffAccountIntegrationTest, StaffPasswordIntegrationTest, PasswordPolicyIntegrationTest,
StaffProfileIntegrationTest, UserPhotoIntegrationTest}`, `sec/domain/PasswordAndProfileDomainRulesTest`,
`file/{FileImageStoreIntegrationTest, domain/ImageValidationDomainServiceTest}`,
`notif/StaffPasswordChangedNotificationIntegrationTest`.

Changed (main): `autoconfigure/{ErpCoreProperties, ErpCoreSecurityAutoConfiguration}`; `file/{crossmodule/FileDocumentLookupApi(+Impl),
domain/FileDocumentDomain, repository/FileDocumentRepository, service/FileService, service/PublicFileUrls}`;
`sec/{controller/UserController, domain/UserDomain, dto/{LoginResponse, UserCreateRequest, UserResponse,
UserUpdateRequest}, entity/User, exception/SecErrorCodes, mapper/UserMapper, security/{AuthRealm,
BootstrapAdminPasswordRunner, JwtAuthenticationFilter}, service/{AuthService, CustomerAccountService,
PasswordResetService, SignupRequestService, UserRoleService, UserService}, tenant/SecTenantProvisioningContributor}`;
`tenant/crossmodule/TenantLookupApi(+Impl)`; `i18n/messages.properties`, `messages_ar.properties`.

Changed (test): `testsupport/StaffApiClient` (patch, login, multipart PUT, anonymous GET/POST),
`tenant/PlatformTenantApiIntegrationTest` (names the 5 copied templates), `erp-app-reference/.../ReferenceApplicationSmokeTest` (V2..V17).

Docs: `docs/api-docs/{README.md, sec/index.md, sec/endpoints/{users, authentication, customer-accounts-public}.md,
sec/endpoints/my-profile.md (new), file/endpoints/file-documents.md}` (generated, README by hand);
`docs/test-api/{core-test-plan.md, core_api_verify.py, results/20261008T011609-P-LIVE.json, -report.md}`;
`docs/{CHANGELOG, CONSUMING, DEVIATIONS}.md`; `governance/README.md` (ADR counts);
`governance/analysis/platform/{PROJECT-OVERVIEW, project-registry}.md`; analysis files above. Deleted: none.

## Decisions & deviations (all in `docs/DEVIATIONS.md` `[TM-D]`)

| Plan said | Built | Why |
|---|---|---|
| ADR-SEC-039 / ADR-SEC-040 | ADR-SEC-063 / ADR-SEC-064 | SEC ADRs up to 061 issued historically, 062 = TM-G; ADR-SEC-065 (held spare) unused |
| ADR-FILE-001 | ADR-FILE-008 | FILE 001..007 = as-built ADRs of the analysis-coverage work (orchestrator agreement) |
| `V19__sec_user_profile.sql`, `V21__notif_seed_password_changed.sql` | `V16…`, `V17…` | orchestrator reservation for the execution order |
| admin-set → `UserStatusResponse` | `PasswordChangeResponse { userPk, passwordChangeRequired, passwordChangedAt, sessionsTerminated }` (also the own change) | status does not change; the session count is the useful fact |
| `StoredImage storePublicImage(r)`, request with `contentType` | `ImageStoreResult` (stored / rejected), no `contentType` | rejection must be a value; type always from the bytes |
| publish via an `ALLOW_PUBLIC` category | image documents uncategorised; the public lookup also serves them | ADR-FILE-008 (no per-tenant seed, no tenant-editable switch) |
| PUT users + new fields | absent keeps, empty clears | an older client never wipes `/me` values |
| policy on create/reset/admin-set/self/admin-reset | + tenant first administrator; not customers; not the bootstrap property | STAFF user create; customers out of the plan's list |
| flag in token vs DB read | the row `JwtAuthenticationFilter` already loads | ADR-SEC-063 |
| NOTIF reacts to the event | NOTIF listens to the public `com.erp.events` event | brief + plan D.3; skill caveat recorded |
| — | `UserPhotoDomain` folded into `UserDomain` | one Domain object per entity (A.0.7) |
| — | controller method names and plain `@PutMapping` for photos; typed local for the policy | stable operation ids and generator bindings |

## Acceptance checklist

| # | Item (plan §6 + brief) | Evidence | |
|---|---|---|---|
| 1 | D.1 V16 columns, widths, `CHK_SEC_USER_LOCALE`, additive | `V16__sec_user_profile.sql` = db-script-sec 1.3.0; `MigrationNamingTest` green; `StaffProfileIntegrationTest.theDatabaseRefusesAnUnknownLocale_too` | ✅ |
| 2 | PasswordPolicy, `erp.core.security.password-policy.*`, `SEC-400-PASSWORD-POLICY` AR+EN, applied everywhere | `PasswordAndProfileDomainRulesTest`, `PasswordPolicyIntegrationTest` (create, reset, tenant), SEC-044/050; `Test1234` passes (bootstrap admin of the P-LIVE instance) | ✅ |
| 3 | D.2 endpoints exactly per table (paths, permissions, STAFF-only, errors) | `docs/api-docs/sec/endpoints/{users,my-profile}.md`; SEC-041..052; JUnit classes | ✅ |
| 4 | Admin-set ends all sessions; own change ends the others | SEC-041, SEC-043; `StaffPasswordIntegrationTest` | ✅ |
| 5 | `requireChangeAtNextLogin` default true for admin-set and admin-created; HTTP flows adapted, no assertion dropped | SEC-041, SEC-050; `first_login` in 6 fixture cases; P-LIVE 170/170 | ✅ |
| 6 | `/me` without roles/permissions (ADR-SEC-064) | SEC-047; `StaffProfileIntegrationTest.me_carriesTheAccountProfileAndTenant_butNoRolesOrPermissions` | ✅ |
| 7 | Forced-change filter, same envelope, decision recorded | `PasswordChangeRequiredFilter`; SEC-041/042; ADR-SEC-063 | ✅ |
| 8 | D.3 event (registered, counted), NOTIF reaction, template seeded + copied, audit actions without secrets | `StaffPasswordChangedNotificationIntegrationTest`; SEC-053, SEC-054; PROJECT-OVERVIEW 11 events; `PlatformTenantApiIntegrationTest` | ✅ |
| 9 | D.4 `FileImageStoreApi`: result not exception, png/jpeg/webp, SVG only when requested and sanitised, size from request, active provider, PUBLIC random slug on step 07's path, discard | `FileImageStoreIntegrationTest`, `ImageValidationDomainServiceTest`; SEC-045/046/048/051 | ✅ |
| 10 | Inline/CSP and category open points documented for E | FILE srs.md 1.3.0 §3; ADR-FILE-008 Consequences; notes below | ✅ |
| 11 | Analysis first (commit 1), ADRs, plan names mapped in DEVIATIONS | `14f7964`; DEVIATIONS `[TM-D]` | ✅ |
| 12 | Tests per feature area | 9 classes, 33 tests | ✅ |
| 13 | HTTP cases SEC-041.., full P-LIVE run archived | run `26100801161F`, 170 PASS / 0 FAIL | ✅ |
| 14 | api-docs regenerated with the merged generator, check_completeness clean, no helper-raised row lost | 115/115; only count cells changed in removed lines | ✅ |
| 15 | CHANGELOG `[TM-D]`, DEVIATIONS, PROJECT-OVERVIEW, report | this file | ✅ |
| DoD §10 | analysis before code · code = entry · `mvn -q verify` green · api-docs + completeness · test plan + archived run · CHANGELOG | sections below | ✅ (frontend item n/a) |

## Code ↔ addendum check

| Item | Addendum | Code | |
|---|---|---|---|
| Migration | `V16__sec_user_profile.sql` | same | ✅ |
| Columns / widths | PHONE 30, JOB_TITLE_AR/EN 150, PREFERRED_LOCALE 5, PHOTO_FILE_ID BIGINT, PASSWORD_CHANGE_REQUIRED_FL BOOLEAN NOT NULL DEFAULT FALSE, PASSWORD_CHANGED_AT TIMESTAMPTZ | V16 + `User` `@Column` lengths | ✅ |
| Constraint | `CHK_SEC_USER_LOCALE` | V16 | ✅ |
| Seed | `V17__notif_seed_password_changed.sql`, NOT EXISTS guard, `{changedAt}` `{changedBy}` | same | ✅ |
| Endpoints | 8 NEW + CHANGED rows (§9.5) | `UserController`, `StaffProfileController`; api-docs | ✅ |
| Permissions | `PERM_SEC_USERS_UPDATE` (admin-set, user photo), `isAuthenticated()` STAFF chain (`/me/**`) | `@PreAuthorize` in services | ✅ |
| Error codes | 5 new, statuses 400/422/403/403/400 | `SecErrorCodes`, both bundles, api-docs index | ✅ |
| Order of checks | admin-set: user → self → policy; self: current → policy; photo: user → validation | services | ✅ |
| Rules' owners | RULE-SEC-056 `PasswordPolicy`; 057/058/060/061 `UserDomain`; 059 filter | code (060/061 corrected in the check commit) | ✅ |
| Audit / event | 3 actions, `UserPasswordChangedEvent(userId, byAdmin)` | services | ✅ |
| FILE API | `ImageStoreRequest` fields, `ImageStoreResult`, `StoredImage`, `discard`, `publicUrls` | `file/crossmodule` | ✅ |
| FILE rules | RULE-FILE-008/009 (+ CSS `url()`, `@import`, check commit)/010 | `ImageValidationDomainService`, `FileImageStoreService`, repository query | ✅ |
| NOTIF | RULE-NOTIF-023 listener, variables format | `StaffPasswordChangedNotifier` | ✅ |
| TENANT | `summaryOf` → `TenantSummary(id, code, nameAr, nameEn)`; admin password policy | `TenantLookupApi`, `SecTenantProvisioningContributor` | ✅ |
| Properties | `erp.core.security.password-policy.min-length/max-length/require-letter/require-digit` = 8/200/true/true | `ErpCoreProperties.Security.PasswordPolicySettings` | ✅ |

## Verification output

- `mvn verify` (offline, clean `target/`), code of `82aa2e5` (later commits change docs and four Javadoc/comment lines only, re-compiled): **BUILD SUCCESS**; JaCoCo "All coverage checks have been met".
  - erp-core: tests 432, failures 0, errors 0, skipped 0 (82 `TEST-*.xml`).
  - erp-app-reference: tests 10, failures 0, errors 0, skipped 0.
- HTTP suite: run **`26100801161F`**, profile P-LIVE, port 18103, fresh `erp_tm_d` (dropped afterwards):
  **170 PASS, 0 FAIL, 0 BLOCKED** (22 profile cases not run) — `docs/test-api/results/20261008T011609-P-LIVE.json` and
  `-report.md`. A trial run before the operation-id rename also gave 170/170.
- api-docs: `review` → `update` from the running app (`--base http://localhost:18103 --server-url http://localhost:7272`).
  `check_completeness.py --base http://localhost:18103`: `per module: app=1, audit=1, cu=5, file=14, mdl=11, notif=18,
  report=4, sec=50, sequence=6, tenant=5 (sum 115) missing=0 duplicated=0 stale=0 RESULT: PASS`.
  `check`: SEC, TENANT, MDL, SEQUENCE, REPORT PASS; FILE, NOTIF (permissions), CU (unique-constraints), AUDIT
  (business-errors), APP — the five known generator limitations of `docs/api-docs/README.md`, unchanged. New codes bound:
  `SEC-400-PASSWORD-POLICY` (4 endpoints), `SEC-422-PASSWORD-SELF`, `SEC-403-PASSWORD-CURRENT-INVALID`,
  `SEC-400-PHOTO-INVALID`; `SEC-403-PASSWORD-CHANGE-REQUIRED` is filter-raised (like `REALM_MISMATCH`, "no throw site").
  Removed lines in the diff are only the `SEC-404-USER` / `SEC-403-FORBIDDEN` endpoint counts — no row lost.
- Generator unit tests: `Ran 65 tests … OK`.

## Reference snapshot reconciliation (`reference-snapshot.md`, sections D1–D4 and "Facts")

| Snapshot row | Verdict | Where |
|---|---|---|
| D1 PasswordPolicy in `com.erp.sec.domain`, not common; 8–200, letter + digit; `SEC-400-PASSWORD-POLICY` | adopted (matches) | SEC addendum; no COMMON folder on main, nothing written there |
| D2 audit actions `PASSWORD_SET_BY_ADMIN`, `PASSWORD_CHANGED`, `PROFILE_PHOTO_CHANGED`, no secrets | adopted; actor of `PROFILE_PHOTO_CHANGED` is the **caller** (an administrator for another user's photo), not always "the account" | SEC srs §9.6 (no AUDIT folder on main) |
| D3 NOTIF policy / PRD / srs / seed rows; NOTIF's first foreign-event consumer | adopted (NOTIF business-policies row added in the renumbering commit) | NOTIF addenda; EVENTS rows kept in SEC §9.6, NOTIF XM-NOTIF-003, PROJECT-OVERVIEW (no EVENTS folder on main) |
| D4 `StoredImage storePublicImage(...)`, request with `contentType` | **disagree**: `ImageStoreResult` (value, never an exception) and no declared type (always detected) | FILE srs §1; DEVIATIONS |
| D4 "the image store writes into an `ALLOW_PUBLIC` category; code and seed fixed in P2" (open item) | **disagree, resolved otherwise**: images are uncategorised and the public lookup serves them (no seed, no tenant-editable switch) | ADR-FILE-008, RULE-FILE-010 |
| D4 SVG vs inline allow-list (open) | recorded: SVG stays an attachment, `<img>` renders it; E keeps the list (FILE ADR 009+ if it changes) | FILE srs §3 |
| Facts #2 JDK sniffer misses WebP/SVG (and PDF/ZIP/OOXML/OLE2) | confirmed; the store detects PNG/JPEG/WebP magic bytes and SVG text itself and never calls the sniffer | `ImageValidationDomainService` |
| Facts #3 `TenantLookupApi` has only `codeOf` | confirmed; D adds `summaryOf` (C1 adds `isActive`) | TENANT addendum |
| Facts #4 CU key-trim defect | not touched (D changes no CU code) | — |
| ADR-FILE-008 draft (PROPOSED, category in the 1.2.0 rule) | written ACCEPTED with the uncategorised decision | `decisions/FILE/ADR-FILE-008.md` |
| D4 FILE PRD rows | adopted | FILE `P0_5/prd-file.md` |

## Skills checked

`gov-enforce-backend-contract` (85 rules; helper `@Component`s `PasswordPolicyProvider`, `UserPhotoUrls`,
`UserSessionTerminator` carry no `@PreAuthorize` — they are not `@Service`s and every caller is a gated service),
`build-create-entity` (Domain companion: one `UserDomain`; `PasswordPolicy` static factory), `build-create-repository`,
`build-create-dto`, `build-create-mapper`, `build-create-service` (cross-module calls only through
`file.crossmodule` / `tenant.crossmodule`; the NOTIF listener on a public core event is the recorded exception),
`build-create-controller` (DELETE 204 + void; `@Valid @RequestBody`), `gov-enforce-error-handling` (codes in
`SecErrorCodes`, both bundles), `gov-enforce-caching-rules` (no caching added), `gov-validate-backend-feature`,
`api-verify` (translation, cases in the plan's format).

## Notes for later steps

- **What package B can reuse**: `PasswordPolicyProvider.current().assertAcceptable("newPassword", raw)` (or
  `PasswordPolicy`), `User.changePassword(hash, changeRequired, now)`, `UserSessionTerminator.terminateOpenSessions(user,
  null, ar, en)` (returns the count), `UserDomain.passwordChangeRequiredFor(..)`. No `SecAdminRecoveryApi` was
  prepared; B adds it in `sec.crossmodule` with these. A tenant admin-reset that should force a change sets the flag
  through `changePassword(..., true, ...)`.
- **What package E can reuse**: `FileImageStoreApi.storePublicImage(new ImageStoreRequest("CORE_TENANT", tenantId,
  "TENANT", bytes, fileName, 1_048_576L, Set.of(TYPE_PNG, TYPE_JPEG, TYPE_WEBP, TYPE_SVG)))` inside
  `TenantContext.callAs(tenantId, …)` → `ImageStoreResult`; on `!isStored()` raise `TENANT_LOGO_INVALID`; replace →
  `discard(previousId)`. `FileDocumentLookupApi.publicUrl(id)` gives `logoUrl`; `TenantLookupApi.summaryOf` gives
  code and names for the branding response.
- **SVG / inline (open for E)**: `image/svg+xml` is not inline-safe (step 07): an SVG logo is served `attachment` with
  `nosniff` and `sandbox; default-src 'none'`; `<img src>` renders it, opening the URL downloads it. Adding SVG to the
  inline list would be a FILE decision of its own (ADR-FILE-009+; 002..008 are the analysis-coverage session's).
- **Category (resolved)**: no `ALLOW_PUBLIC` category is needed; image documents are uncategorised (ADR-FILE-008).
- **Merge hotspots**: `ReferenceApplicationSmokeTest` lists every core migration (B/E/C4 must append theirs);
  `PlatformTenantApiIntegrationTest` names the copied templates; TENANT 1.3.0 addendum sections in
  `P1/srs-tenant.md` / `registry-srs-tenant.md` were opened here too (C3 opened them on its branch — keep both blocks).
- **HTTP suite**: next free `TC-CORE-SEC-055`; counts SEC 54, total 192, P-LIVE 170. Users an administrator creates
  must change their password first: use `first_login(...)` (or send `requireChangeAtNextLogin: false`).
- **Open**: `MaxUploadSizeExceededException` → 500 in apps with Spring's default 1 MB multipart ceiling (pre-existing);
  no password reuse/history rule; `PasswordPolicy` message names the default composition.
- **Next free ids**: REQ-SEC-090, AC-SEC-096, RULE-SEC-063, DBF-SEC-124, XM-SEC-007, ADR-SEC-065 (held, unused) /
  066+ (other session); RULE-FILE-011, XM-FILE-003, ADR-FILE-009; RULE-NOTIF-024, XM-NOTIF-004.

## Review round 1

Verdict FAIL (two MEDIUM, four LOW, nits); fixed on the same branch, no rebase. Evidence of the reviewer:
`rev-d/security_probes.py`, `rev-d/svg/SvgProbe.java`. Commits `33e16d8` … `03ce288`.

| # | Finding | Fix | Evidence |
|---|---|---|---|
| 1 | MEDIUM — RULE-FILE-009 blacklist bypassed by namespace prefixes (`<s:script>`, `<h:script>`, `<x:foreignObject>`), CSS escapes (`@imp\ort`, `u\rl(`), SMIL (`animate attributeName="href"`, `set attributeName="onmouseover"`) and `xml:base` | `SvgAllowList` (FILE domain): strict UTF-8 (other declared encodings refused), hardened namespace-aware DOM parse (`disallow-doctype-decl`, no external/parameter entities, no external DTD, no XInclude, no entity expansion, secure processing, silent error handler), SVG-namespace static-drawing elements only (no `script`, `foreignObject`, `a`, `image`, `feImage`, animation, `switch`, `metadata`, no foreign elements), listed attributes only (no `on…`, `attributeName`, `src`, `xml:base`, foreign attributes; `xml:space`/`xml:lang` and `xmlns` allowed), `href`/`xlink:href` only `#fragment`, no `\` / `@import` / `javascript:` / `expression(` / `<` / non-`#` `url(` in any attribute value or `<style>` text, CDATA only in `<style>`, no PI. RULE-FILE-009 rewritten. ArchUnit rule 1 now allows the JDK XML API (`org.w3c.dom..`, `org.xml.sax..`) | `ImageValidationDomainServiceTest` 49 tests (46 rejected inputs incl. every bypass of the probe); the reviewer's `SvgProbe` re-run: only plain SVG, inert char-ref text and the three raster-magic files are accepted |
| 2 | MEDIUM — passwords over BCrypt's 72 bytes answered 500 | `PasswordPolicy` refuses more than 72 UTF-8 bytes (`MAX_BYTES`), `max-length` default 72, `@Max(72)` fails startup above it (decision: fail, not clamp; `create` also clamps defensively), customers get `PasswordPolicy.CUSTOMER` (8..72, byte limit only). Messages AR/EN, RULE-SEC-056, REQ/AC-SEC-082, CONSUMING, P0/P0_5/TENANT rows updated. DTO `@Size(max = 200)` kept as transport bound so the policy code answers | `PasswordPolicyIntegrationTest.everyPasswordPath_refusesMoreThanSeventyTwoBytes_with400_andAcceptsExactlySeventyTwo` (create, admin-set, own change, reset completion, tenant first administrator, customer register; 73 ASCII, 62 Arabic = 122 bytes, exactly 72); `PasswordAndProfileDomainRulesTest` (bounds, clamp, `@Max` validation); HTTP `TC-CORE-SEC-055` |
| 3 | LOW — missing upload part answered 500 | `GlobalExceptionHandler.handleMultipart`: `MissingServletRequestPartException` and `MultipartException` (incl. `MaxUploadSizeExceededException`, non-multipart request) → 400 `VALIDATION_ERROR`, part named in `fieldErrors` when known; no new code. Also fixes `POST /api/v1/files` without `file` (CHANGELOG Fixed) | `StaffProfileIntegrationTest.malformedOrOversizePhotoRequests_answer400ValidationError` (missing part, non-multipart, 2 MB over the default 1 MB ceiling) |
| 4 | LOW — id coordination | RULE-NOTIF-009 → **RULE-NOTIF-023** (addenda, registry, notifier Javadoc, test, report, DEVIATIONS); the V17 header comment keeps the old id (no migration edit for a comment). XM-NOTIF-003 kept: the snapshot names no as-built XM-NOTIF ≥ 003 | `git grep RULE-NOTIF-009` → only V17's comment and DEVIATIONS' history |
| 5 | LOW — tenant create api-docs miss the SPI error | `@Operation` description of `POST /api/v1/platform/tenants` names it; `docs/api-docs/README.md` lists provisioning-SPI errors as a known limitation; regenerated | `docs/api-docs/tenant/endpoints/platform-tenants.md` |
| 6 | LOW — stored image kept the client's file name | `ImageStoreRequest.fileName` → `baseName`; stored name `<baseName>.<png|jpg|webp|svg>` from the detected type (photos `photo.png`) | `FileImageStoreIntegrationTest` (`../poly.html` → `polyhtml.png`, header has no `.html`), `UserPhotoIntegrationTest` (`photo.png`) |
| 7 | nits | Open items recorded (srs-sec §9.9, ADR-SEC-063): no throttling of the current-password check on `PUT /me/password`; no super-role guard on admin-set. `UserSessionTerminator` reuse in `deactivate` / reset completion not done (different audit texts, `assertCanTerminate`) | DEVIATIONS `[TM-D]` |

Verification after the fixes:
- `mvn -q verify` (clean `target/`, code `3db5538`): BUILD SUCCESS, JaCoCo met. erp-core **480** tests / 0 failures / 0 errors / 0 skipped (82 suites); erp-app-reference **10** / 0 / 0 / 0.
- P-LIVE run **`2610080232BA`**, port 18103, fresh `erp_tm_d` (dropped afterwards): **171 PASS, 0 FAIL** (22 profile cases not run) — `docs/test-api/results/20261008T023217-P-LIVE.json` / `-report.md`, replacing run `26100801161F`. Test plan: SEC 55, total 193, P-LIVE 171.
- api-docs regenerated (whole app, 115 ops); `check_completeness`: 115/115, 0 missing / duplicated / stale; `check` verdicts unchanged (SEC, TENANT, MDL, SEQUENCE, REPORT pass; the five known limitations).

Notes for later after round 1: next free TC-CORE-SEC-056; RULE-NOTIF-024; E builds logos with `new ImageStoreRequest("CORE_TENANT", id, "TENANT", bytes, "logo", 1_048_576L, …)` (stored `logo.svg` / `logo.png`); an Inkscape-exported SVG with `inkscape:` / `sodipodi:` metadata is refused — export "plain SVG".
