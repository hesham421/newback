# SEC legacy API suite — erp-core 1.2.0 re-run

| | |
|---|---|
| Run date | 2026-10-05 |
| Target | erp-core 1.2.0 (newback `main`, `erp-app-reference` jar), dev profile, fresh PostgreSQL 16 database, PLATFORM tenant |
| Script | `test_sec_apis.py` (adapted in place; the original is in git history) |
| Run | `ERP_BASE_URL=http://localhost:<port> ERP_BOOTSTRAP_ADMIN_PASSWORD=<pw> python test_sec_apis.py` |

## Baseline — original script, unmodified (only the port pointed at the instance)

**0 passed, 0 failed, 0 executed — aborted before the first test.**

`FATAL: could not log in as admin — aborting before any test runs.` The admin login
(`POST /api/v1/sec/auth/login {"username":"admin","password":"admin"}`) answered:

- without a tenant: `400 TENANT_REQUIRED` ("A tenant is required: send the X-Tenant-Code header");
- with `X-Tenant-Code: PLATFORM`: `401 SEC-401-INVALID-CREDENTIALS` (`admin/admin` no longer exists).

Every one of the 67 assertions therefore failed transitively on the login precondition.

## Classification

| Test / spot | Class | Change | Reference |
|---|---|---|---|
| Admin login (precondition of all 67 tests) — tenant | (a) INTENDED CHANGE | Every request now sends `X-Tenant-Code` (`ERP_TENANT_CODE`, default `PLATFORM`). Public paths (login, signup, password reset) need it; on token calls the token's `tid` wins. | P0 business-policies-sec.md addendum #1; P1 srs-sec.md addendum §1 (CHANGED `POST /auth/login`), §3 `TENANT_REQUIRED`; docs/steps/05-report.md |
| Admin login (precondition of all 67 tests) — password | (a) INTENDED CHANGE | Hard-coded `admin`/`admin` removed; password read from env `ERP_BOOTSTRAP_ADMIN_PASSWORD` (script exits 2 when unset). | P0 addendum #7; P1 addendum §2 (Bootstrap admin); P2 db-script-sec.md addendum (SEC_USER seed); docs/steps/04-report.md |
| All other logins in the suite (u2 / u3 login, menu checks, TC-SEC-001/002, session tests) | (a) INTENDED CHANGE | Covered by the same tenant header (users are created in, and sign in to, the PLATFORM tenant). | same as row 1 |
| SoD observation (RULE-SEC-005 / TC-SEC-020) | (a) comment only | Still an observation; comment notes `fin` (owner of the only real pair) was removed. No assertion change. | P0 addendum #10; P0_5 prd-sec.md addendum (US-SEC-007); docs/steps/01-report.md |
| TC-SEC-028 / AC-SEC-028 — "use u2's token after its session was terminated" (was an observation recording HTTP 200) | (b) TEST-DEFECT | The old test re-logged u2 (who already held earlier ACTIVE sessions from the suite-10 menu checks) and terminated the *first* u2 session the search returned — not the session behind the token it then re-used, so the 200 measured nothing. The login step now identifies its own new session exactly (u2's session ids before vs after the login; the session response exposes no `jti`/`tokenRef`), the terminate step terminates that one, and the follow-up call is now an **assertion: 401**. The same exact-session identification is applied to the double-terminate test. | P1 srs-sec.md AC-SEC-028 ("the associated token is no longer accepted for any subsequent request"); erp-core `JwtAuthenticationFilter.authenticate()` refuses a token whose session (tokenRef = jti) is unknown or terminated; review finding 2026-10-05 |
| Fixture passwords | (b) TEST-DEFECT (hygiene, review advisory) | Hard-coded fixture passwords (`N3wP@ssw0rd!`, `An0therP@ss!`) replaced by `random_password()` (per run, `secrets`-based). Deliberately-wrong login values (`definitely-wrong`, `whatever`) kept — they are negative inputs, not secrets. | review advisory 2026-10-05 |
| BASE_URL / report output | (b) TEST-DEFECT (portability) | `BASE_URL` from env `ERP_BASE_URL` (default `http://localhost:7272`). Generated HTML + failure list now go to `ERP_REPORT_DIR` (default OS temp dir) instead of a hard-coded relative `governance/modules/SEC/test-api` path that crashed when run from elsewhere and overwrote this curated report. | legacy-suite brief |

Counts: (a) 4 spots (all rooted in the login/tenant change), (b) 3, (c) 0.

No other test needed adapting. In particular the intentional changes that this suite touches only
indirectly all held without any change to an assertion:
staff-only user search / by-id user APIs / session list / dashboard (srs-sec.md addendum §1, DEVIATIONS [14]) —
the suite only works with STAFF accounts; the `SYS_ADMIN` super role (P0 addendum #6) — admin still
passes every `@PreAuthorize`; the per-module `SecPermissions` replacing `PermissionConstants` (P1 addendum §4) —
authority strings unchanged; registry endpoints (catalog now also synchronized from code at startup) —
API registration of a test module/screen/action still works and every RULE-SEC-001..004/007 guard fires.
The customer-realm features (register/verify/login rate limit, `REALM_MISMATCH`) and `tid`/`realm` token
claims are new surface the legacy suite never covered; they are exercised by newback
`docs/test-api/core_api_verify.py`, not added here.

## Final results

| Run | RUN_ID | Passed | Failed | Observations |
|---|---|---|---|---|
| 1 | 199235 | 67 | 0 | 10 |
| 2 | 199261 | 67 | 0 | 10 |
| 3 | 199265 | 67 | 0 | 10 |
| 4 (after review fix, fresh DB) | 200331 | 68 | 0 | 9 |
| 5 (after review fix) | 200338 | 68 | 0 | 9 |

Baseline RUN_ID was 199205. Every run used a fresh RUN suffix. Runs 4 and 5 are the final two
consecutive stable runs of the current script: **68 passed, 0 failed, 9 observations**. The extra test
is the terminated-session token, which moved from observations (10 → 9) to assertions (67 → 68) and
answers **401** as AC-SEC-028 requires.

Runs 1–3 (before the review fix) recorded "terminated token → HTTP 200", as did the last pre-1.2.0 run
(Run ID 223693). That was the test defect described above, not app behaviour. The other 9 observations
have the same outcome as in that run.

## (c) Suspected app defects

None.

## Records left behind per run (unchanged by design)

Role, ModuleRegistry, ScreenRegistry, ActionRegistry rows, RoleScreen/RoleAction grants not cascaded,
PasswordResetToken, AuditLogEntry, decided SignupRequests and the user created by a sign-up approval —
SEC exposes no delete surface for them (see `cleanup()` in the script).
