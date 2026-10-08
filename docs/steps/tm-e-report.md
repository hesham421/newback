# TM-E — tenant branding: logo, brand colour, `/api/v1/tenant/me`, public branding

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` §7 E.1–E.4 (plus §0 D5, §9 ADR-TENANT-005) |
| Branch | `tm/e-tenant-branding` (from `main` @ 4a1f455, which includes A, G, C3, D, B) |
| Migration | `V20__tenant_branding.sql` (plan V18) |
| Date | 2026-10-08 |

## Summary

The platform operator gives a tenant a logo and an optional brand colour from the `PLATFORM_TENANTS` screen
(decision D5; no module, screen, permission or grant seed): `PUT /api/v1/platform/tenants/{id}/logo` (multipart
`file`, PNG / JPEG / WebP / plain SVG ≤ 1 MB, validated by FILE's image store), `DELETE …/{id}/logo` (204) and
`PATCH …/{id}/branding` (`brandColor` `#RRGGBB`, upper-cased; null or blank clears). The logo is a PUBLIC
`FILE_DOCUMENT` stored **in the target tenant's own rows** — written inside `TenantContext.callAs(id)` in one
`REQUIRES_NEW` transaction that stores the image, points `CORE_TENANT.LOGO_FILE_ID` at it, discards the previous
document and records `TENANT_LOGO_CHANGED` in that tenant and in PLATFORM — so its URL is
`/api/v1/public/files/{thatTenantCode}/{slug}`. `TenantResponse` carries `logoUrl` (resolved inside the tenant) and
`brandColor`. Two reads return `TenantBrandingResponse { code, nameAr, nameEn, logoUrl, brandColor, defaultLocale }`:
`GET /api/v1/tenant/me` (authenticated, **either realm** — realm-neutral on the core chain, allowed during a pending
forced password change) and the anonymous `GET /api/v1/public/tenants/{tenantCode}/branding` (tenant from the path via
the `path-tenant-paths` default, 404 / 403, and a per-client-address bucket4j limit counted before the tenant lookup,
429 `TENANT_BRANDING_RATE_LIMITED`). Carry-over from D's review: FILE's SVG allow-list refuses duplicate `id` values.

## Analysis entries written (commit 84d9bcc, before any code)

| File | Section | Ids |
|---|---|---|
| `TENANT/P1/srs-tenant.md` | 1.3.0 addendum, package-E block E1–E11 | REQ/AC-TENANT-029 … 032, RULE-TENANT-018 … 022, XM-TENANT-003; CHANGED RULE-TENANT-006, -012; 3 error codes; ENT-TENANT-001 fields; DTOs; dependencies; SVG serving decision (E7); audit (E8); SCR-REQ-TENANT-001 |
| `TENANT/P1/registry-srs-tenant.md` | package-E block | registry deltas; last sequences REQ 032 · RULE 022 · XM 003 · POL 014 · US 014 · DBF 044 · ADR 005 |
| `TENANT/P0/business-policies-tenant.md`, `module-registry-tenant.md`, `platform-summary.md` | package-E blocks | POL-TENANT-014; CHANGED POL-006, -008; config, exposed surface, FILE dependency, resolved decision 5 |
| `TENANT/P0_5/prd-tenant.md` | package-E block | US-TENANT-012, -013, -014; CHANGED US-002, US-005 |
| `TENANT/P2/db-script-tenant.md`, `registry-db-tenant.md` | package-E blocks | DBF-TENANT-043 (`LOGO_FILE_ID BIGINT`), -044 (`BRAND_COLOR VARCHAR(7)`), `CHK_CORE_TENANT_BRAND_COLOR`, XM-TENANT-003, V20 script |
| `decisions/TENANT/ADR-TENANT-005.md` | NEW | who sets a tenant's logo (D5, ACCEPTED; draft of the reference snapshot verified and adopted) |
| `FILE/P1/srs.md` §8, `registry-srs-file.md` | package-E rows | RULE-FILE-009 item (8) CHANGED (duplicate ids); TENANT consumer implemented; §3 open points resolved |
| `SEC/P1/srs-sec.md` §11, `registry-srs-sec.md` | package-E rows | realm rule CHANGED (`/tenant/me` realm-neutral), RULE-SEC-059 CHANGED (exemption) |
| `governance/README.md` | ADR count | TENANT 2 |

Ids were re-verified against the tree, this repository's full history and `governance-shared` (all history): the
highest TENANT ids ever issued were REQ/AC 028, RULE 017 (012 … 015 reserved), XM 002, POL 013, US 011, DBF 042;
ADR-TENANT-005 is reserved for E. No new SEC or FILE id.

## Files changed

- Migration: `erp-core/src/main/resources/db/migration/core/V20__tenant_branding.sql` (new).
- TENANT: `entity/Tenant.java`, `domain/TenantDomain.java`, `exception/TenantErrorCodes.java`, `dto/TenantResponse.java`,
  new `dto/TenantBrandingUpdateRequest.java`, `dto/TenantBrandingResponse.java`, `mapper/TenantMapper.java`,
  `service/TenantService.java`, new `service/TenantBrandingService.java`, `service/TenantLogoUrls.java`,
  `controller/PlatformTenantController.java`, new `controller/TenantBrandingController.java`,
  new `security/PublicBrandingRateLimitFilter.java`.
- Wiring: `autoconfigure/ErpCoreProperties.java` (`path-tenant-paths` default, `Tenant.PublicBrandingRateLimit`),
  `autoconfigure/ErpCoreSecurityAutoConfiguration.java` (`PUBLIC_TENANT_BRANDING_PATHS`, `TENANT_ME_PATH`, the filter).
- SEC: `sec/security/PasswordChangeRequiredFilter.java` (exemption). FILE: `file/domain/SvgAllowList.java` (duplicate ids).
- i18n: one `tenant-maturity E` block in `messages.properties` and `messages_ar.properties`.
- Tests: new `tenant/TenantBrandingIntegrationTest` (9), `tenant/security/PublicBrandingRateLimitFilterTest` (3);
  `tenant/domain/TenantDomainTest` (+ 4 methods), `tenant/TenantIsolationIntegrationTest` (+1), `tenant/TenantHttp`
  (`putFile`, `delete`, `getBytes`), `file/domain/ImageValidationDomainServiceTest` (+1),
  `erp-app-reference/.../ReferenceApplicationSmokeTest` (V20).
- Docs: `docs/api-docs/{README.md, tenant/index.md, tenant/endpoints/platform-tenants.md, tenant/endpoints/tenant-branding.md (new)}`
  (generated; README by hand), `docs/test-api/{core-test-plan.md, core_api_verify.py,
  results/20261008T052614-P-LIVE.json, -report.md}`, `docs/{CHANGELOG, CONSUMING, DEVIATIONS}.md`,
  `governance/analysis/platform/{PROJECT-OVERVIEW, project-registry}.md`, the analysis files above, this report.
  Deleted: none (a test class added in the branch was removed again in `7f23f1d`).

## Decisions & deviations (all in `docs/DEVIATIONS.md` `[TM-E]` and srs-tenant.md E10)

1. Migration V20 for the plan's V18 (reserved numbers); the plan's CHECK expression verbatim.
2. Rate limit = a filter keyed by client address only, counted before the token and tenant lookup (unknown codes
   count, so the endpoint cannot enumerate codes faster than the limit); new code `TENANT_BRANDING_RATE_LIMITED` 429;
   `erp.core.tenant.public-branding-rate-limit.capacity` / `period` = 60 / 1 min (RULE-TENANT-022).
3. `/api/v1/tenant/me` realm-neutral on the core chain + exempt from the forced-change gate (SEC rules CHANGED).
4. Audit `TENANT_LOGO_CHANGED` in the target tenant **and** in PLATFORM, same transaction, for set and remove; the
   entity audit's `logoFileId` row lands in the target tenant; brand colour relies on the entity audit (PLATFORM).
5. `setLogo` / `removeLogo` not `@Transactional`; one `REQUIRES_NEW` `TransactionTemplate` inside `callAs(id)` (B precedent).
6. `logoUrl` resolved inside the logo's tenant: one read-only `REQUIRES_NEW` transaction per tenant with a logo on a
   page (no cross-tenant FILE query added).
7. `brandColor` upper-cased and trimmed by the entity; blank clears; codes carry the field (`file`, `brandColor`).
8. DELETE logo → 204 without body, idempotent (plan, D's photo precedent; G's 200+count concerns cascades).
9. The public read resolves the tenant again from the path code (`findByCode`) and both reads call
   `TenantDomain.assertServed()` (so the api-docs bind `TENANT_SUSPENDED`).
10. SVG logos stay an attachment (no FILE ADR): render through `<img>` only; PNG / WebP recommended (E7).
11. Duplicate SVG ids refused (D's review carry-over); RULE-FILE-009 changed through a FILE package-E row.
12. Rate-limit integration test runs in the shared context from the IPv6 loopback (an extra context exhausts the test
    database's 100 connections); assumption-skipped on a host without `::1`.
13. api-docs generator limit: the word "public" in an `@Operation` text before the method made the generator take
    "path" as the method name; the texts avoid it (generator unchanged).

Reference snapshot (package E, ADR-TENANT-005 draft, facts): adopted after checking each row against the code.
Disagreements: V18 (→ V20); its rule text "`<script`, `on*=`, external hrefs rejected" (→ FILE's allow-list,
RULE-FILE-009); placeholder cases TC-CORE-TENANT-040 … 048 (→ 038 … 046, PLATFORM-005); no rate-limit code / rule in
the snapshot (→ `TENANT_BRANDING_RATE_LIMITED`, RULE-TENANT-022); its open point on the chain of `/tenant/me`
(→ realm-neutral on the core chain). Fact 5 (`/tenant/me` would reach only the staff chain) confirmed; facts 2 and 3
unchanged by E (the image store detects types itself; `TenantLookupApi` gained nothing in E).

## Acceptance checklist

| # | Item (plan §7 E.1–E.4, the task's list, §10 DoD) | Evidence |
|---|---|---|
| 1 | E.1 `LOGO_FILE_ID BIGINT NULL` (soft ref, no FK), `BRAND_COLOR VARCHAR(7)` + `CHK_CORE_TENANT_BRAND_COLOR`, no registry rows, additive | V20; `MigrationNamingTest`; smoke test lists 2..20, 1000 |
| 2 | Logo stored in the target tenant's rows (`callAs`, `TransactionTemplate`), owner `CORE_TENANT`/{id}, module `TENANT`, PUBLIC, ≤ 1 MB, png/jpeg/webp/svg; URL under that tenant's code | `TenantBrandingIntegrationTest.aLogo_…` (row checked with JDBC); TC-TENANT-038 |
| 3 | E.2 `PUT /{id}/logo` → `TenantResponse`, replace discards previous, 404, `TENANT_LOGO_INVALID` 400, audit `TENANT_LOGO_CHANGED` (operator; tenant + PLATFORM rows) | JUnit `replacingTheLogo_…`, `platformMayCarryALogo_…`; TC-038, -040, -041, -045 |
| 4 | `DELETE /{id}/logo` (204, consistent with D) | JUnit `removingTheLogo_…`; TC-044 |
| 5 | `PATCH /{id}/branding` (null clears, `TENANT_BRAND_COLOR_INVALID`) | JUnit `theBrandColour_…`; `TenantDomainTest`; TC-042 |
| 6 | `GET /api/v1/tenant/me` (`isAuthenticated()`, any realm, six fields, read-only) | JUnit `tenantMe_…` (staff, customer, forced change, PLATFORM, 401); TC-039 |
| 7 | Public branding: path tenant wired like public files, unknown 404, suspended 403, rate limit per IP (bucket4j, properties in `ErpCoreProperties`, defaults in CONSUMING) | JUnit `publicBranding_takesTheTenantFromThePath_…`, `publicBranding_isRateLimitedPerClientAddress_…`, `PublicBrandingRateLimitFilterTest`; TC-043, -046 |
| 8 | `TenantResponse` + mapper (`logoUrl`, `brandColor`) | JUnit; TC-038 (GET, search) |
| 9 | Unique controller method names; no pre-existing operation id changed | api-docs review: only `tenant/` changed; no removed "Operation ID" line |
| 10 | E.3 Domain rules on `TenantDomain` (one logo, discard; PLATFORM may carry a logo; platform-only writes) | `assertLogoAccepted`, `assertBrandColorValid`, `assertServed`; RULE-TENANT-018 … 021; TC-044 (PLATFORM logo), PLATFORM-005 |
| 11 | ADR-TENANT-005 | `decisions/TENANT/ADR-TENANT-005.md` |
| 12 | SVG served as attachment + nosniff + sandbox CSP confirmed, documented for the frontend, raster preference decided; plain/optimised SVG in the error text | TC-041 (headers), E7, `TENANT_LOGO_INVALID` message, CONSUMING |
| 13 | Carry-over: duplicate SVG ids refused, decoy test, RULE-FILE-009 via a FILE package-E row | `d2fd18c`; `ImageValidationDomainServiceTest.duplicateIds_…`; `SvgProbe3` 0 unexpected; FILE srs §8 |
| 14 | Tests per E.4 (upload/logoUrl/me/public URL; public 404/403/rate limit; tenant admin 403; replace discards; DELETE null; SVG `<script>` rejected; colour; isolation) | 9 + 3 new JUnit tests, 5 test classes extended; `TenantIsolationIntegrationTest.aTenantsLogo_…` |
| 15 | HTTP cases TC-CORE-TENANT-038 … 046 + PLATFORM-005; full P-LIVE run archived with flags | run `26100805267B`, 192/192 PASS |
| 16 | api-docs regenerated, `check_completeness` clean, no helper row lost | 123/123; removed lines = walked-method narration and count cells only |
| 17 | CHANGELOG `[TM-E]`, DEVIATIONS, CONSUMING, PROJECT-OVERVIEW, report | this commit set |
| DoD | analysis before code · code = entry · `mvn -q verify` green · api-docs · test plan + archived run · CHANGELOG | `84d9bcc` precedes `d2fd18c` / `af64416`; sections below; frontend item n/a (package F) |
17/17 backend items met.

## Code ↔ addendum check

| Addendum item | Code | Match |
|---|---|---|
| `PUT` / `DELETE /{id}/logo`, `PATCH /{id}/branding`, `PLATFORM_TENANT_MANAGE`, 404 / 400 codes, 204 | `PlatformTenantController.setTenantLogo / removeTenantLogo / updateTenantBranding`; `TenantService.setLogo / removeLogo / updateBranding` | yes |
| `GET /api/v1/tenant/me` `isAuthenticated()` any realm; `GET /api/v1/public/tenants/{tenantCode}/branding` `permitAll()`, 404 / 403 / 429 | `TenantBrandingController`, `TenantBrandingService`, `TENANT_ME_PATH`, `PUBLIC_TENANT_BRANDING_PATHS`, `PublicBrandingRateLimitFilter` | yes |
| Order of checks (binding → tenant → image; tenant → colour; rate limit → path tenant → read) | services, filter placed before `JwtAuthenticationFilter` | yes |
| DBF-TENANT-043/044, `CHK_CORE_TENANT_BRAND_COLOR`, V20 | V20, `Tenant` `@Column` (`LOGO_FILE_ID`, `BRAND_COLOR` length 7) | yes |
| Error codes `TENANT_LOGO_INVALID` 400 (field `file`), `TENANT_BRAND_COLOR_INVALID` 400 (field `brandColor`, arg value), `TENANT_BRANDING_RATE_LIMITED` 429 | `TenantErrorCodes`, `TenantDomain`, filter, both bundles | yes |
| RULE-TENANT-018 constants (`CORE_TENANT`, `TENANT`, `logo`, 1 048 576, four types) | `TenantDomain.LOGO_*` | yes |
| RULE-TENANT-021 (trim, upper case, blank clears) | `TenantDomain.assertBrandColorValid`, `Tenant.normalize` | yes |
| RULE-TENANT-022 defaults 60 / 1 min, per address, map ≤ 10 000 | `ErpCoreProperties.PublicBrandingRateLimit`, filter | yes |
| E8 audit rows (tenant + PLATFORM, one for PLATFORM; summaries with code and document id) | `TenantService.recordLogoChange` | yes |
| XM-TENANT-003 (store / discard / publicUrl inside `callAs`) | `TenantService.replaceLogo / clearLogo`, `TenantLogoUrls` | yes |
| SEC RULE-SEC-059 exemption, realm-neutral path | `PasswordChangeRequiredFilter.EXEMPTIONS`, `realmNeutralPaths` | yes |
| FILE RULE-FILE-009 (8) unique ids | `SvgAllowList.Walk` | yes |
| Registry "verified by" names | after the check: the rate-limit test is `TenantBrandingIntegrationTest.publicBranding_isRateLimitedPerClientAddress_…` (registry row amended in `7f23f1d`) | fixed |

## Verification output

- `mvn -q verify` (offline, clean `target/`, code of `5ee1fd2`): BUILD SUCCESS, JaCoCo met (erp-core lines 81.56 %).
  - erp-core: tests **605**, failures 0, errors 0, skipped 0 (88 suites; B ended at 574 / 86).
  - erp-app-reference: tests **10**, failures 0, errors 0, skipped 0.
- HTTP suite: run **`26100805267B`**, profile P-LIVE, port 18106, fresh `erp_tm_e` (dropped afterwards):
  **192 PASS, 0 FAIL, 0 BLOCKED** (22 profile cases not run) — `docs/test-api/results/20261008T052614-P-LIVE.json` /
  `-report.md` (flags `--instance`, `--code-under-test`). An earlier run on `7f23f1d` also gave 192/192 (not archived;
  replaced after the `@Operation` text fix). TENANT-046 reached 429 after 57 calls of this run (60 minus the run's
  three earlier public-branding calls).
- api-docs: `review` → `update` (`--base http://localhost:18106 --server-url http://localhost:7272`): only `tenant/`
  changed (5 added, 6 updated). `check_completeness.py`: `per module: app=1, audit=1, cu=5, file=14, mdl=11,
  notif=18, report=4, sec=50, sequence=6, tenant=13 (sum 123) missing=0 duplicated=0 stale=0 RESULT: PASS`. `check`:
  SEC, TENANT, MDL, SEQUENCE, REPORT PASS; FILE, NOTIF (permissions), CU (unique-constraints), AUDIT (business-errors),
  APP — the five known limitations, unchanged. Generator unit tests: `OK`.

## Skills checked

`gov-enforce-backend-contract` (Domain: static rules on `TenantDomain`, no annotations, facts passed in; A.5.3
deviation recorded; A.6.5 DELETE 204 + void), `build-create-entity` (two columns, normalisation in
`@PrePersist`/`@PreUpdate`), `build-create-repository` (no change: `findById` / `findByCode` reused), `build-create-dto`
(two new DTOs, `@Schema` bilingual, no bean validation for the colour so the domain code answers), `build-create-mapper`
(`updateBrandingFromRequest` void, null-safe, `toBrandingResponse`), `build-create-service` (cross-module only through
`file.crossmodule`; `TenantLogoUrls` a `@Component` helper like SEC's `UserPhotoUrls`), `build-create-controller`
(thin, `@Operation` on every method, `@Valid @RequestBody`, unique method names), `gov-enforce-error-handling` (codes
in `TenantErrorCodes`, both bundles, `LocalizedException` only), `gov-enforce-caching-rules` (no caching),
`gov-validate-backend-feature` (no raw SQL added), `api-verify` (cases in the plan's format).

## Notes for later steps

- **What C (events, token cut-off) needs to know**
  - A suspended tenant's branding is refused everywhere: the public branding answers 403 `TENANT_SUSPENDED` (filter
    first, `TenantDomain.assertServed` second), `/tenant/me` 403 through the token check, and the logo URL 403 like any
    public file of that tenant. C.1's session termination on suspension changes nothing here.
  - C.2's cut-off: `/api/v1/tenant/me` is served by the **core chain** to tokens of **both realms** (realm-neutral
    path) — the `iat` / `TOKENS_INVALID_BEFORE` check must apply there for customer tokens too (it does if it lives in
    `TenantResolutionFilter`'s token branch). The public branding is anonymous (any token on it is dropped when it names
    another tenant, irrelevant otherwise). TC-CORE-TENANT-043 suspends and re-activates tenant D and signs `td-admin`
    in again afterwards, so a cut-off on re-activation will not break the later TM-E cases.
  - Filter changes: the customer chain now starts with `PublicBrandingRateLimitFilter` (before `JwtAuthenticationFilter`,
    acting only on `/api/v1/public/tenants/*/branding`); the core chain's `RealmEnforcementFilter` skips
    `TENANT_ME_PATH`; `PasswordChangeRequiredFilter.EXEMPTIONS` has four entries. A C.1 lifecycle event can carry the
    tenant's code; the logo needs no event.
  - `CORE_TENANT` changes made inside `callAs(id)` (the logo) are audited by the entity listener in that tenant;
    C.2's revoke-tokens should write its `CORE_TENANT` update in the PLATFORM request (as `updateStatus` does).
- **HTTP suite**: run order now ends `TENANT-038 … 045, PLATFORM-005, TENANT-046`; TENANT-046 spends the runner
  address's public-branding budget (60 per minute) — put nothing that calls the public branding after it. Tenant D ends
  with brand colour `#0A0B0C` and no logo; a PLATFORM logo is set and removed by TENANT-044. Test-plan counts TENANT 46,
  PLATFORM 5, total 214, P-LIVE 192.
- **JUnit**: the cached Spring contexts' pools already fill the test database's 100 connections — a new
  `@TestPropertySource` variant fails with "too many clients"; reuse an existing property set (TM-E reuses
  `UserPhotoIntegrationTest`'s) or test in the base context.
- **api-docs generator**: a lower-case "public" in an `@Operation` text followed by `word (` before the method makes the
  generator misread the method name (recorded in DEVIATIONS).
- **Next free ids** (tree + both histories): TENANT REQ/AC-033, RULE-023 (012 … 015 reserved), POL-015, US-015,
  ENT-002, SCR-REQ-002, XM-004, DBF-045, ADR-TENANT-002 (C.2), 003 (C.4), 004 (C.6) — 005 used; SEC unchanged
  (REQ-092, AC-098, RULE-063, ENT-015, DBF-124, XM-007, ADR-069); FILE RULE-011, XM-003, ADR-009; NOTIF RULE-024,
  XM-004. HTTP: TC-CORE-TENANT-047, TC-CORE-PLATFORM-006, TC-CORE-SEC-056.

## Review round 1

Verdict PASS (106/106 probes, P-LIVE 192/192, clean trial merge; evidence `rev-e/`) with one MEDIUM and four LOW
items plus two notes; fixed on the same branch, analysis first (`e7a226a` amends the package-E block), no rebase.

| # | Finding | Fix | Evidence |
|---|---|---|---|
| 1 | MEDIUM — test contexts' pools: ~10 cached contexts × Hikari's default 10 = the 100 `max_connections` of the embedded / CI database (pre-existing; E's first extra context failed with "too many clients") | `application-test.properties`: `spring.datasource.hikari.maximum-pool-size=4`, `minimum-idle=1` (`11fc2c6`); no test needed more. `TestPostgres.java:86` keeps `max_connections=100` (mirrors CI's `postgres:16`) | Scratch run (not committed) of the whole erp-core suite in alphabetical order with one deliberately added context last: `HEADROOM contexts=11 connectionsThisDb=27 clientBackends=27 max_connections=100` — the 11th context started; worst case 11 × 4 = 44. CHANGELOG (tests only), DEVIATIONS |
| 2 | LOW — the realm skip of `/api/v1/tenant/me` ignored the method | `RealmEnforcementFilter` gains `realmNeutralGetPaths` (GET only, like `PasswordChangeRequiredFilter.EXEMPTIONS`); the core chain passes `List.of(TENANT_ME_PATH)` (`615612f`); SEC / TENANT analysis say "GET only" | `TenantBrandingIntegrationTest.tenantMe_…`: POST / PUT / PATCH with a customer token → 403 `REALM_MISMATCH` |
| 3 | LOW — proxy guidance, 429 behaviour | CONSUMING: `server.forward-headers-strategy=native` + `server.tomcat.remoteip.internal-proxies`; `framework` trusts any client's `X-Forwarded-For` unless the proxy overwrites it; frontend shows the platform mark on 429 and honours `Retry-After`. **`Retry-After` added** (whole seconds until one request refills, bucket4j `ConsumptionProbe`) | `PublicBrandingRateLimitFilterTest` (1 190..1 200 s for 3/h), MockMvc test (`> 0`), TC-CORE-TENANT-046 (`>= 1`) |
| 4 | LOW — bucket keys and eviction | IPv6 keyed by /64 (`InetAddress.ofLiteral`, no DNS; IPv4-mapped → IPv4); access-ordered map: idle buckets expire after `period`, at most 10 000 keys (LRU) instead of clear-all (`db0738d`). No new dependency (Caffeine is not on the classpath). `LoginRateLimiter` unchanged — recorded as a SEC follow-up in DEVIATIONS | `PublicBrandingRateLimitFilterTest` (5 tests: keys, same/other /64, expiry with a test clock, the 10 000 bound, defaults) |
| 5 | LOW — api-doc generator read string literals as code | `security_extractor.blank_string_literals` (string, text-block and char literals blanked, offsets kept), used by `find_controller_for_endpoint` (`607e6f2`); two unit tests, the first fails on the old code. The natural `@Operation` wording is back | generator suite 67 OK; regeneration: only the public-branding endpoint's summary / description changed (diff against the previous regeneration), every other module unchanged |
| 6 | optional — host-independent rate-limit wiring test | MockMvc over the context's `springSecurityFilterChain` with explicit `remoteAddr` (shared context, never skipped) replaces the IPv6-loopback test (`d084294`) | `TenantBrandingIntegrationTest.publicBranding_isRateLimitedPerClientAddress_unknownCodesIncluded` |
| 7 | note — F2 caching | Confirmed: every upload is a new `FILE_DOCUMENT` with a new random slug, so a new logo never hides behind a cached URL; but public files carry `max-age=86400, public`, so an **old** URL (replaced or removed logo) may be served from a browser / CDN cache for up to 24 h — the shell must take `logoUrl` from `/tenant/me` / the public branding each time, never from a remembered URL. Written into srs-tenant.md E11 and CONSUMING | — |

Verification after the fixes:
- `mvn -q verify` (clean `target/`, code `d084294`): BUILD SUCCESS, JaCoCo met (erp-core lines 81.68 %). erp-core **607** /
  0 / 0 / 0 (88 suites); erp-app-reference **10** / 0 / 0 / 0.
- P-LIVE run **`261008055707`**, port 18106, fresh `erp_tm_e` (dropped afterwards): **192 PASS, 0 FAIL, 0 BLOCKED** —
  `docs/test-api/results/20261008T055705-P-LIVE.json` / `-report.md`, replacing run `26100805267B`.
- api-docs: regenerated (only `tenant/endpoints/tenant-branding.md` and the tenant catalog line changed);
  `check_completeness`: 123/123, 0 missing / duplicated / stale; `check` verdicts unchanged (SEC, TENANT, MDL,
  SEQUENCE, REPORT pass; the five known limitations).

Notes for later (round 1): every cached test context now has a pool of 4 — a new `@TestPropertySource` variant is
affordable again (about 14 more contexts fit); `RealmEnforcementFilter` has a method-specific skip list C or others can
reuse; SEC's `LoginRateLimiter` still clears all buckets above 10 000 keys (follow-up).
