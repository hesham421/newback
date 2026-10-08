# Tenant-maturity plan — plan report (erp-core 1.3.0, backend)

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` (2026-10-07); run state `docs/plans/tenant-maturity-state.json` |
| Scope of this run | backend packages A, G, C3, D, B, E, C12, C6, C4, C5 and the closure Z; frontend package F is the frontend repository's next step (`docs/steps/tm-frontend-handover.md`) |
| Main at closure | `e643d91` (all ten packages merged) + the closure branch `tm/z-closure` |
| Version | `main` is `1.3.0-SNAPSHOT`; nothing is tagged or released (owner's decision, see "Release recommendation") |
| Date | 2026-10-08 |

## 1. What shipped, per package

Execution order A → G → C3 → D → B → E → C12 → C6 / C4 (parallel) → C5 → Z. Review rounds and deviation counts are the
state file's; the deviation lines themselves are under `## [TM-<X>]` in `docs/DEVIATIONS.md`.

| Pkg | What shipped | Merge | Review rounds | Deviations | Decisions (ADR) | Tests added (JUnit · HTTP) | Migration |
|---|---|---|---|---|---|---|---|
| A | TENANT module analysis, as built from erp-core 1.2.0: `governance/analysis/modules/TENANT/{P0,P0_5,P1,P2}` (POL 001–011, US 001–008, REQ/AC 001–023, RULE 001–009, DBF 001–032, XM 001–002, SCR-REQ-TENANT-001) | `ca02e2e` | 2 (PASS + fixes) | 1 | ADR-TENANT-001 (as built: row-level tenancy) | none (documentation) | — |
| G | Revoke a single screen or action grant: `DELETE /api/v1/sec/roles/{id}/screens/{screenId}`, `DELETE …/actions/{actionId}` (VIEW revoke cascades the screen's other actions; 200 with a count); generator binds codes raised through `com.erp.common` helpers | `486a8a8` | 2 (ids renumbered, generator fix) | 6 | ADR-SEC-062 (plan ADR-SEC-041) | `RoleGrantRevokeIntegrationTest` (7), `GrantRevokeDomainRulesTest` (2) · TC-CORE-SEC-035…040 | — |
| C3 | Tenant-isolation tests: ArchUnit `TenantScopedEntityTest` (explicit global-entity list), HTTP isolation matrix over SEC, MDL, FILE, NOTIF, CU, SEQUENCE, AUDIT; governance rule RULE-TENANT-011 (raw SQL names `TENANT_ID`, 15 statements audited) | `260778a` | 2 (run archive fix) | 5 | — | `TenantScopedEntityTest` (2), `TenantIsolationIntegrationTest` (+7) · none (no HTTP behaviour) | — |
| D | Staff passwords and profile: admin-set password (`PUT /sec/users/{id}/password`), forced change at next sign-in (403 gate), own change (`PUT /sec/me/password`), `GET/PATCH /sec/me` (no roles), photos (`/sec/me/photo`, `/sec/users/{id}/photo`), profile fields, `PasswordPolicy` (8 chars … 72 bytes, letter + digit), `UserPasswordChangedEvent` + NOTIF `STAFF_PASSWORD_CHANGED`; FILE `FileImageStoreApi` with a strict SVG allow-list; multipart errors 400 | `38f6d91` | 3 (SVG allow-list, BCrypt 72-byte cap, multipart 400) | 12 | ADR-SEC-063 (plan ADR-SEC-039), ADR-SEC-064 (plan ADR-SEC-040), ADR-FILE-008 (plan ADR-FILE-001) | 9 classes (`StaffPasswordIntegrationTest`, `PasswordPolicyIntegrationTest`, `StaffProfileIntegrationTest`, `UserPhotoIntegrationTest`, `PasswordAndProfileDomainRulesTest`, `FileImageStoreIntegrationTest`, `ImageValidationDomainServiceTest` — 112 SVG / image cases, `StaffPasswordChangedNotificationIntegrationTest`, `AbstractStaffAccountIntegrationTest`) · TC-CORE-SEC-041…055 | `V16__sec_user_profile.sql`, `V17__notif_seed_password_changed.sql` |
| B | Tenant level 1: `PUT /platform/tenants/{id}` (names + profile), suspension reason and facts, `POST /{id}/admin-reset` (super STAFF user, never on PLATFORM), `GET /{id}/usage`; SEC `SecAdminRecoveryApi`, counts on SEC / FILE / NOTIF cross-module APIs | `79aa807` | 2 (5 LOW fixed) | 11 | — (choices in srs-tenant B10) | `TenantProfileIntegrationTest`, `TenantAdminResetIntegrationTest`, `TenantUsageIntegrationTest`, `TenantDomainTest` (+5) · TC-CORE-TENANT-027…037 | `V18__tenant_profile.sql`, `V19__tenant_lifecycle.sql` |
| E | Tenant branding: logo (`PUT/DELETE /{id}/logo`, PUBLIC document in the tenant's rows) and brand colour (`PATCH /{id}/branding`), `GET /api/v1/tenant/me` (either realm), anonymous `GET /api/v1/public/tenants/{code}/branding` (rate-limited per address, 429 + `Retry-After`); SVG duplicate-id fix | `ee74409` | 2 (test pool, GET-only realm skip, /64 + LRU buckets, `Retry-After`) | 14 | ADR-TENANT-005 | `TenantBrandingIntegrationTest` (9), `PublicBrandingRateLimitFilterTest` (5), extensions · TC-CORE-TENANT-038…046, TC-CORE-PLATFORM-005 | `V20__tenant_branding.sql` |
| C12 | Lifecycle events `TenantSuspendedEvent` / `TenantActivatedEvent` (13 core events); SEC ends a suspended tenant's sessions; NOTIF holds queued rows of a non-active tenant; per-tenant token cut-off (401 `TENANT_TOKEN_REVOKED`), `POST /{id}/revoke-tokens` | `381d000` | 2 (cut-off at the next whole second, audited retryable failure) | 9 | ADR-TENANT-002 | `TenantTokenCutOffIntegrationTest`, `TenantLifecycleEventsIntegrationTest`, `NotificationSuspendedTenantIntegrationTest`, `NotificationTenantActivationListenerTest`, `TenantDomainTest` (+3) · TC-CORE-TENANT-047…050 | — |
| C6 | `ScopedValue` spike for `TenantContext`: **NO-GO**, spike reverted; `TenantContext` stays a `ThreadLocal` | `d351bcd` | 1 (docs-only fixes) | 7 | ADR-TENANT-004 (REJECTED) | `TenantContextLeakTest` (8) · none | — |
| C4 | Optional `Idempotency-Key` on `POST /platform/tenants` (replay with `Idempotent-Replayed: true`, 409 conflict, owner-only, 2xx only, 24 h retention job); mechanism `com.erp.common.idempotency`; ArchUnit rule "common depends on no module" | `bf37562` | 2 (ON CONFLICT claim, generator binds common components) | 9 | ADR-TENANT-003 | `IdempotencyKeyDomainTest`, `IdempotentResponsesTest`, `TenantIdempotentProvisioningIntegrationTest` · TC-CORE-TENANT-051…053 | `V21__core_idempotency_key.sql` |
| C5 | Tenant data export: SPI `TenantExportContributor` (8 core contributors, 21 CSV files, no secrets), `POST /{id}/export` (snapshot, row cap, per-node guard, concurrency cap 429), PRIVATE PLATFORM archive + single-use token; FILE `FilePrivateStoreApi`; **restricted FILE documents** (`REQUIRED_AUTHORITY`, 404 without it, bytes removed on delete) | `840a450` | 2 (HIGH: archive readable by FILE viewers → restricted documents) | 11 | ADR-TENANT-006 | `TenantExportIntegrationTest`, `TenantExportArchiveTest`, `TenantExportGuardTest`, `FileDocumentDomainRestrictedTest`, DB / LOCAL storage purge tests · TC-CORE-TENANT-054…058 | `V22__file_document_required_authority.sql` |
| Z | Closure: carry-over fixes (Javadoc, RULE-FILE-012 purge limitation recorded), whole-repo consistency check and documentation fixes, full api-docs check, full HTTP verification in every profile, frontend handover, this report | (this branch) | — | 5 (`[TM-Z]`) | — | none (no behaviour change) · none | — |

Total: 85 deviation lines over the ten packages (state file), 5 at closure.

## 2. Totals

| Measure | Before the plan (main after A) | At closure (`tm/z-closure`) |
|---|---|---|
| `mvn -q verify` (clean `target/`, JDK 25) | erp-core 390 · app 10 | **erp-core 669 · app 10**, 0 failures / errors / skipped; JaCoCo erp-core lines 82.92 % (gate 60 %) |
| HTTP suite (`core-test-plan.md`) | 172 cases (150 P-LIVE + 22 profile) | **226 cases (204 P-LIVE + 22 profile), all PASS** — P-LIVE run `26100811257C` 204/204; P-MAIL 19/19, P-MAIL-DOWN 1/1, P-CAP 1/1 at cap 2 and at cap 3, P-LOCAL 1/1 (`docs/test-api/results/20261008T112525-*`, `20261008T1134*`, `20261008T1135*`, `20261008T1136*`) |
| HTTP cases per module | — | TENANT 58, SEC 55, FILE 25, REPORT 17, NOTIF 16, AUDIT 16, SEQ 14, SETTINGS 10, CORE 8, PLATFORM 5, APP 2 |
| api-docs operations | 105 | **125** (sec 50, tenant 15, notif 18, file 14, mdl 11, sequence 6, cu 5, report 4, audit 1, app 1); `check_completeness.py` missing 0 / duplicated 0 / stale 0 |
| `generate_all.py --function check` | 5 known FAILs | the same 5 known generator limitations (FILE, NOTIF permissions; CU unique-constraints; AUDIT business-errors; APP), no new FAIL |
| api-doc generator unit tests | 59 (one failing on a cp1252 default) | 71, OK (G +6, E +2, C4 +4; the cp1252 failure fixed in G) |
| Core events | 10 | 13 |
| Core tables | 26 | 27 (`CORE_IDEMPOTENCY_KEY`); 23 carry `TENANT_ID` |
| ADR files | SEC 13, MDL 12 | SEC 16, MDL 12, TENANT 6, FILE 1 |

## 3. Migrations

| Script | Package | Content | Plan §11 expected |
|---|---|---|---|
| `V16__sec_user_profile.sql` | D | `SEC_USER` phone, job titles, preferred locale (`CHK_SEC_USER_LOCALE`), photo reference, password-change flag and time | `V19` |
| `V17__notif_seed_password_changed.sql` | D | `STAFF_PASSWORD_CHANGED` template for every tenant | `V21` |
| `V18__tenant_profile.sql` | B | `CORE_TENANT` profile, `CHK_CORE_TENANT_LOCALE` | `V16` |
| `V19__tenant_lifecycle.sql` | B | suspension facts, `TOKENS_INVALID_BEFORE` | `V17` |
| `V20__tenant_branding.sql` | E | `LOGO_FILE_ID`, `BRAND_COLOR`, `CHK_CORE_TENANT_BRAND_COLOR` | `V18` |
| `V21__core_idempotency_key.sql` | C4 | `CORE_IDEMPOTENCY_KEY` (12 columns, tenant-scoped) | `V20` |
| `V22__file_document_required_authority.sql` | C5 (review) | `FILE_DOCUMENT.REQUIRED_AUTHORITY` | — (unplanned) |

All additive (`MigrationNamingTest`), sequential, each named exactly in its module's P2 addendum, all listed in
`ReferenceApplicationSmokeTest`; the mapping is one consolidated line in `docs/DEVIATIONS.md` `[TM-Z]`. Next core
migration: `V23`.

## 4. What the closure checked and fixed

- Carry-over: `FileDocumentDomain.from()` Javadoc restored (comment only); RULE-FILE-012's `LOCAL` / `S3` purge failure
  recorded as a known limitation (FILE `P1/srs.md` 1.3.0 §10, registry note, DEVIATIONS).
- Analysis: 32 files carry a 1.3.0 addendum, each with one heading and its package blocks in execution order; no id is
  defined twice; the C6 → C4 block boundary in two TENANT files was glued to a table (blank line restored); package E's
  four citations of the reserved `RULE-TENANT-012` now name REQ-TENANT-011 (and TC-CORE-TENANT-043's trace); every
  referenced ADR file exists and is ACCEPTED, except ADR-TENANT-004 (REJECTED, intended); RULE-TENANT-012 … 015 are
  mentioned only as reserved.
- Counts and pointers: `PROJECT-OVERVIEW.md` (125 operations, C12 / C4 / C6 facts, version row, 72-byte cap),
  `project-registry.md` (125 operations, 1.3.0 cross-module reads), `governance/README.md`, the core migration README
  (V16 … V22), `docs/RELEASE.md` (the new SPI in the public-API list), `docs/CHANGELOG.md` `[Unreleased]` (one section,
  package order, a "Behaviour changes — read before upgrading" list). `docs/CONSUMING.md` was already complete: every new
  property documented once with its default (`password-policy.*`, `public-branding-rate-limit.*`, `idempotency.*`,
  `tenant.export.*`), the proxy guidance (`forward-headers-strategy=native` + `remoteip.internal-proxies`) and the
  `TenantExportContributor` SPI with an example.
- No product defect was found: every HTTP case of every profile passes on the merged code.

## 5. Open follow-ups (collected from all package reports and the closure)

Retention and robustness
1. **Export archive retention** — archives stay until an operator deletes them; a retention job
   (`erp.core.tenant.export.retention`, `AuditRetentionJob` pattern) is not built (C5, DEVIATIONS `[TM-C5]`).
2. **Purge-failure retry** — a failed after-commit delete of a restricted document's `LOCAL` / `S3` object is only logged
   (WARN); a sweeper for `DELETED` restricted documents should go with item 1 (Z, FILE srs §10).
3. **`LoginRateLimiter` eviction** — still clears all buckets above 10 000 keys; the branding limiter's expiring LRU map
   is the model (E review).
4. Per-node state: the export guard, the rate-limit buckets and the in-memory download-token store are per JVM (C5,
   E; documented). The idempotency claim has no lock timeout (C4, decided not done).
5. `PublicBrandingRateLimitFilter` keeps a named-zone link-local IPv6 address (`fe80::1%eth0`) raw (E review, LOW).
6. Test pool: Hikari 4 per cached context; a test firing ≥ 3 parallel async events that each nest `REQUIRES_NEW` could
   starve it (E, LOW).

Security / SEC
7. **No password history / reuse rule** (a forced change may reuse the same password) (D).
8. **The current-password check of `PUT /sec/me/password` is not throttled** (D review).
9. **No super-role guard on admin-set password** — an administrator holding `PERM_SEC_USERS_UPDATE` can set a super-role
   user's password (D review).
10. A bootstrap admin password above 72 bytes fails startup inside BCrypt (operator configuration, not policy-checked) (D).
11. `UserSessionTerminator` not reused by `deactivate` / reset completion (D nit).

Data and documentation
12. `TOKENS_INVALID_BEFORE` is derivable from `CORE_TENANT.UPDATED_AT` in an export (accepted: not a secret) (C5 review).
13. `V17__notif_seed_password_changed.sql`'s header comment still says RULE-NOTIF-009 (the id is RULE-NOTIF-023); a
    shipped migration is never edited. `V19`'s column comment says the cut-off is "set on activation" — revoke-tokens
    sets it too. `TenantService`'s Javadoc names the provisioning contributors "(SEC, MDL, NOTIF)" — SEQUENCE (order 40) is
    missing (comment drift noted by A; left so the closure changes no code after its final `mvn verify`).
14. **ADR-SEC numbering gaps** — ADR-SEC-012 … 061 were issued historically and dropped at vendoring; ADR-SEC-065 was held
    spare and is unused; 066 … 068 belong to the analysis-coverage work; the next free SEC ADR is 069. FILE ADR-001 … 007
    likewise belong to that work (the next free FILE ADR is 009).
15. **The other session's analysis-coverage work** (RULE-TENANT-012 … 015, FILE ADR-FILE-001 … 007, SEC ADR-SEC-066 … 068,
    `REQ-SEC-036` logout ids) is uncommitted in the main checkout, pending the owner's decision; this run kept its ids free.
16. `CLAUDE.md`'s documentation map still names only the 1.2.0 addenda (not edited: repository instructions are the
    owner's); `erp-app-reference/docker/docker-compose.yml` still pins `postgres:17` (pre-existing drift).
17. Archived HTTP runs keep the request URLs of the checks, FILE download tokens included (pre-existing practice; the
    tokens are single-use, expired and belong to dropped scratch databases).

Generator (`governance/tools/api-doc-generator`)
18. The five known `check` FAILs (FILE / NOTIF permissions `CONTROLLER_NOT_MATCHED`, CU unique-constraints, AUDIT
    business-errors, APP); response headers not rendered (`Idempotent-Replayed`, `Retry-After`); no cross-module call walk
    (`SEC-400-PASSWORD-POLICY` on tenant create / admin-reset lives in the description); provisioning-SPI errors; a
    lower-case "public" before `word (` in an `@Operation` text confused method matching (worked around in E).

Scope
19. `ScopedValue`: revisit only by dropping the public-API constraint (accept a MAJOR change) and judging on the leak
    class (ADR-TENANT-004).
20. Level 2 (quotas, `ARCHIVED`, per-tenant self-signup switch, per-tenant rate limits, platform-set tenant settings) —
    out of scope for 1.3.0 (plan §9).
21. **Frontend package F** (F1–F4) — next, in `newfront`; handover `docs/steps/tm-frontend-handover.md`.

## 6. Release recommendation

**Version: MINOR — `1.3.0`.** `docs/RELEASE.md` reserves MAJOR for removing or renaming public API, changing migration
semantics, or removing / renaming a property key. None happened: every `crossmodule` type, SPI, event and property key of
1.2.0 is still there with the same shape (interfaces gained methods, `NotificationRequeueJob` gained a constructor, the
old ones stay); every migration is additive; new properties all have defaults. What changes for a client is behaviour on
existing endpoints — a suspension needs a `reason`, old tokens are refused after a re-activation, administrator-created
users must change their password, passwords are capped at 72 bytes, multipart errors are 400, SVGs are strict, restricted
FILE documents answer 404 — and the 1.2.0 precedent (answers changing from 500 to 4xx in a MINOR) plus the plan's own
release line ("everything is additive, so it all ships as 1.3.0") place these in a MINOR with explicit upgrade notes,
which `docs/CHANGELOG.md` `[Unreleased]` now carries under "Behaviour changes — read before upgrading". The JDK 25
requirement was already part of `[Unreleased]` before this plan. The one real client is the in-house frontend: ship
package F with (or right after) the backend, because today's frontend sends no suspension reason and has no
forced-change page.

What the owner does (`docs/RELEASE.md` "How a release is cut"):
1. Merge `tm/z-closure` into `main`; CI green on `main`.
2. One commit: every pom (`pom.xml`, `erp-core/pom.xml`, `erp-app-reference/pom.xml`) to `1.3.0`, and
   `docs/CHANGELOG.md` `## [Unreleased]` → `## [1.3.0] — <date>` with a one-paragraph MINOR rationale (the 1.2.0 entry's
   pattern); `CLAUDE.md` / `PROJECT-OVERVIEW.md` version rows if wanted.
3. Tag `v1.3.0`, push the tag: CI runs `build-test` (Testcontainers), `docker-image`, `publish` (GitHub Packages) and
   `consume-published`.
4. A second commit moves `main` to `1.4.0-SNAPSHOT`.
5. Tell the frontend which backend version to pin when F lands.
