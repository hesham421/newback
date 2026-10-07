# NOTIF — legacy API suite re-run against erp-core 1.2.0

| | |
|---|---|
| Run date | 2026-10-05 |
| Target | erp-core 1.2.0 code (newback `main`), `erp-app-reference` jar, profile `dev`, `spring.cache.type=simple`, own instance on `http://localhost:7302`, fresh PostgreSQL 16 database `erp_legacy_notif`. No mail starter, so no `JavaMailSender`: this is the default build. |
| Caller | PLATFORM bootstrap admin (`X-Tenant-Code: PLATFORM`, password from `ERP_BOOTSTRAP_ADMIN_PASSWORD`) |
| Script | `test_notif_apis.py`. Adapted in place; the original is in git history. |
| Sources for intended changes | erp-core `docs/DEVIATIONS.md` (`[NN]` = step), `docs/CHANGELOG.md`, `docs/test-api/core-test-plan.md`, and the "Implementation Addendum — erp-core 1.2.0" sections in `analysis/modules/NOTIF/P0/business-policies-notif.md` (BP#n), `P0_5/prd-notif.md` (PRD) and `P1/srs.md` (SRS §n) |

**Final result: 50 passed, 0 failed** in two consecutive runs (RUN_ID `199509` and `199529`), with 14 observations. Observations never count as pass or fail. **No suspected app defects (class c).**

## 1. Baseline (original script, unmodified, only BASE_URL pointed at the instance)

| Run | Result |
|---|---|
| Original, as committed (`python test_notif_apis.py http://localhost:7302`) | **0 executed, 0 passed, 0 failed.** The script stopped with `FATAL: could not authenticate — ENVIRONMENT_FAILURE` (exit 2). The login sends `admin`/`admin` without a tenant header, and the server answers 400 `TENANT_REQUIRED`. With the header it answers 401 `SEC-401-INVALID-CREDENTIALS`, because `admin/admin` no longer exists. All 40 tests are therefore blocked by the login. |
| Diagnostic only: original script with the login patched from outside the file (adds `X-Tenant-Code: PLATFORM` and the bootstrap password; the script file was not edited) | **39 passed, 1 failed** (40 tests). The single failure: `TC-BE-NOTIF-015 — dispatch over an enabled channel reaches SENT` → `SKIPPED_NO_PROVIDER`. Two observations also changed: the old "terminal FAILED branch" now ends `SKIPPED_NO_PROVIDER`, and "unknown filter field" now gives 400. See §3. |

## 2. Classification of every baseline failure / change

Classes: **(a)** intended change, with a reference; **(b)** test defect (the old test was wrong or brittle whatever erp-core does); **(c)** suspected app defect.

| # | Test / spot | Baseline outcome | Class | Reference | Adaptation |
|---|---|---|---|---|---|
| 1 | Login (blocks all 40 tests) | 400 `TENANT_REQUIRED`; with the header, 401 for `admin/admin` | (a) | DEVIATIONS [05] (multi-tenancy, `X-Tenant-Code` / TenantResolutionFilter), [06] (realms, JWT `tid`/`realm`); core-test-plan TC-CORE-TENANT-001, TC-CORE-SEC-001/002 (bootstrap admin, `admin/admin` rejected); BP#1 | Login sends `X-Tenant-Code` (env `ERP_TENANT_CODE`, default `PLATFORM`). The password comes from env `ERP_BOOTSTRAP_ADMIN_PASSWORD`, and the script stops at once if it is unset. The hard-coded `admin/admin` was removed. |
| 2 | `TC-BE-NOTIF-015 — dispatch over an enabled channel reaches SENT` (run-owned SMS channel) | `SKIPPED_NO_PROVIDER`, reason `NOTIF_CHANNEL_UNAVAILABLE` | (a) | DEVIATIONS [08] (`LoggingChannelProvider`; `EmailChannelProvider`: "non-EMAIL channels no longer fake success … end `SKIPPED_NO_PROVIDER`"); BP#4; PRD US-NOTIF-004; SRS §2 (provider resolution); core-test-plan TC-CORE-NOTIF-004 | The intent (an enabled channel that has a provider delivers) now runs on the seeded `IN_APP` channel. It polls until the status is final (`SENT`, `sentAt` set) and also checks that the item reached the recipient's inbox (DEVIATIONS [08] inbox; TC-CORE-NOTIF-007). The run-owned enabled channel's new outcome is asserted as a separate test: `SKIPPED_NO_PROVIDER` with `lastError=NOTIF_CHANNEL_UNAVAILABLE`. |
| 3 | Every read of an asynchronous outcome | Correct only by timing: the old test read the log immediately | (a) | DEVIATIONS [08] (state machine `PENDING → QUEUED \| CHANNEL_DISABLED`, `QUEUED → SENT \| FAILED \| SKIPPED_NO_PROVIDER`), [15] (claim/lease); BP#2; SRS §3 | New `wait_final_log()` polls `GET logs/{id}` until the status leaves `PENDING`/`QUEUED`. The timeout is 60 s (`NOTIF_FINAL_STATUS_TIMEOUT_S`), which covers the 2+4+8+16 s retry schedule. `CHANNEL_DISABLED` is still read immediately, because it is final when dispatch returns and reading it at once proves the row was never queued. |
| 4 | Obs. "terminal FAILED branch" (TC-BE-NOTIF-003) | Was `FAILED`, `retryCount 4`, `missing recipient email address`. Now `SKIPPED_NO_PROVIDER` | (a) | DEVIATIONS [08] (no mail sender → EMAIL `SKIPPED_NO_PROVIDER`), [14] (EMAIL falls back to the account e-mail; no address at all → `REJECTED` → `FAILED` after 1 attempt, never retried); BP#6 | Still an observation, never asserted. It now records the final status after polling. The old arrangement cannot be reproduced over HTTP: SEC makes `email` mandatory on users, so every recipient has an address. |
| 5 | Dispatch payload omitted `variables` "so no real mail can be sent" | The guarantee no longer holds: an EMAIL without `variables.email` goes to the recipient's account e-mail | (a) | DEVIATIONS [14]; BP#6 | Dispatches now carry the documented override `variables.email = notif-verify-<RUN>@example.invalid` (RFC 2606 reserved domain). This keeps the original intent that the run never mails a real mailbox. |
| 6 | Obs. "dispatch to an inactive recipient" (TC-BE-NOTIF-010) | Not runnable: a fresh database has no inactive user. Before 1.2.0 it was observation-only because XM-NOTIF-001 was DEFERRED | (a) | BP#7; SRS §2 (RULE-NOTIF-007 CHANGED); DEVIATIONS [06], [08]; core-test-plan TC-CORE-NOTIF-011 | **Upgraded to an assertion**, since the reason for the deferral no longer applies. Preflight creates a STAFF user and disables it with SEC's `DELETE` (expects `statusCode=DISABLED`). A dispatch to that user must return `logIds=[]`, and a log search by its `recipientId` must find no rows. |
| 7 | Permission half of TC-BE-NOTIF-014 | A GAP before 1.2.0: every `PERM_NOTIF_*` check was commented out (SEC-PENDING) | (a) | SRS §1 / §5 (`PERM_NOTIF_TEMPLATES_VIEW`, `…CHANNELS_VIEW`, `…LOG_VIEW`); DEVIATIONS [06] (XxxPermissions SPI); api-docs Known Error Codes `ACCESS_DENIED` 403 | Four new assertions. A STAFF fixture user with no role gets 403 `ACCESS_DENIED` on template read, template search, channel search and log search. The `grants()` docstring was corrected. |
| 8 | Preflight lookup contents | Passed, but checked only the pre-1.2.0 values | (a) | DEVIATIONS [08] (LOV seeds); SRS §6 | `NOTIF_CHANNEL` must also contain `IN_APP`; `NOTIF_STATUS` must also contain `QUEUED` and `SKIPPED_NO_PROVIDER`. The seeded IN_APP channel config is read but never changed. |
| 9 | `TC-BE-NOTIF-013 — get log by id` shape | Passed; it did not check the new fields | (a) | SRS §1 (API-NOTIF-002/003 responses gain `attempts`, `nextAttemptAt`, `lastError`); BP#8 / DEVIATIONS [08] (`VARIABLES_JSON` is never exposed) | Requires `attempts`, `nextAttemptAt` and `lastError`, and fails if `variables` or `variablesJson` appears. `TC-BE-NOTIF-004/015` (disabled channel) also requires `attempts=0` (TC-CORE-NOTIF-005). |
| 10 | Recipient choice: "first active user in SEC search" | Worked on a fresh database by coincidence | (b) | DEVIATIONS [08] (an inbox can only be read by its owner) | The caller (JWT `uid` claim) is now preferred as the recipient, so that the IN_APP delivery can be checked in its own inbox. Otherwise it falls back to the first active user, as before. |
| 11 | The script overwrote `notif_problems_report.md` on every run and embedded static prose about the old code (SEC-PENDING, the `DefaultRecipientStatusReader` stub, the synchronous FAILED branch) | That prose is stale against 1.2.0 | (b) | — | `write_report()` now writes only the results of a single run, and only when `NOTIF_RUN_REPORT=<path>` is set. This maintained report is never overwritten. |
| 12 | BASE_URL constant | — | (a) owner convention | brief | `ERP_BASE_URL` env (default `http://localhost:7272`). The positional `[BASE_URL]` argument is kept. |

Counts: **(a) 10, (b) 2, (c) 0.** Only one baseline test failed for a behaviour reason (#2), and one environment failure blocked everything (#1). The other rows are adaptations needed to keep the intent of existing checks or observations under the documented 1.2.0 contract (#3–#9), plus test-hygiene fixes (#10–#11).

## 3. Observations whose outcome changed (not pass/fail)

| Observation | Before 1.2.0 | 1.2.0 | Note |
|---|---|---|---|
| Filtering logs on a field outside the whitelist (`errorMessage LIKE`) | 200, filter ignored (`totalElements=130`) | 400 `VALIDATION_ERROR` | A deliberate platform change made on 2026-09-19, before erp-core existed (it shipped in erp-core's initial commit a9ed097). It is not a 1.2.0 step change: `SpecBuilder.assertFieldsAllowed` rejects a filter field outside the entity's allowed set. The wire code is `VALIDATION_ERROR`, with detail code `UNSUPPORTED_FILTER_FIELD` (`CommonErrorCodes`). DEVIATIONS [11] mentions it only in passing, as existing behaviour ("like `SpecBuilder`'s unknown filter fields"). This is stricter than before and is not a NOTIF regression. It stays an observation. |
| EMAIL final status | `FAILED`, retryCount 4 | `SKIPPED_NO_PROVIDER`, attempts 1, `lastError=NOTIF_CHANNEL_UNAVAILABLE` | Row #4 above. With a mail-starter build it would reach `SENT` to the override address. |
| Dispatch over a lookup-valid but unconfigured channel | `CHANNEL_DISABLED` | `CHANNEL_DISABLED` | Unchanged (TC-CORE-NOTIF-005). From the second run on, the channel the observation picks may be a disabled leftover from an earlier run, not a truly unconfigured one. Both lead to the same outcome. |

The other observations are unchanged from the pre-1.2.0 run: maxLength 400s, second DELETE gives 204, unknown sortField gives 200, `size=201` is capped at 200, `channelTypeId` outside the lookup gives 201, `channelHint` outside the lookup gives 200 with a log row, empty `channelHint` gives 400, missing `recipientId` gives 400, and SQLi as a filter value is parameterised.

## 4. Final results (2 consecutive runs, each with a fresh RUN suffix)

| Suite | Passed | Failed |
|---|---|---|
| PREFLIGHT (stage A0) | 7 | 0 |
| NotificationTemplate | 10 | 0 |
| NotificationChannelConfig | 6 | 0 |
| Dispatch | 9 | 0 |
| NotificationLog | 5 | 0 |
| Lookups | 4 | 0 |
| Authorization (RULE-NOTIF-005 + TC-BE-NOTIF-014 permission half) | 9 | 0 |
| **Total** | **50** | **0** |

Both runs: `TOTAL: 50 passed, 0 failed`, exit 0. The application log showed no ERROR line and no 5xx.

## 5. Suspected app defects (class c)

_None._ Every failure or behaviour change found during the re-run traces to a documented decision listed in §2.

## 6. Coverage notes (governed cases TC-BE-NOTIF-001..016)

- **Now asserted, previously GAP or observation:** TC-BE-NOTIF-010 (inactive recipient → no log) and the permission half of TC-BE-NOTIF-014 (403 `ACCESS_DENIED`).
- **Retry cases — not covered by this suite** (the HTTP surface has no fault injection; exercising the retry path needs a mail-starter build pointed at a dead SMTP port, which is outside this suite's default-build scope):
  - TC-BE-NOTIF-003 (retries exhausted → `FAILED`) is covered outside this suite. In JUnit: erp-core `NotificationAsyncDeliveryIntegrationTest.providerThrowing_isRetriedFiveTimes_thenFailed_withNotificationFailedEvent`. Over HTTP: core-test-plan TC-CORE-NOTIF-006 (P-MAIL-DOWN profile).
  - TC-BE-NOTIF-002 (fails, then succeeds on retry) has **no exact automated coverage** anywhere. The closest test is erp-core `NotificationClaimIntegrationTest`: there a lost outcome record is retried and the row ends `SENT` with `attempts=2`. That is a failure to record the outcome, not a provider that fails and then succeeds.
- **Known pre-existing limitation (not caused by 1.2.0):** `CHANNEL_TYPE_ID` is unique per tenant and NOTIF documents no hard delete. Each run therefore takes the next free type from `SMS, WHATSAPP, PUSH, INTERNAL`, so a database supports four runs before the channel create reports a precondition failure. Purge with `DELETE FROM NOTIF_CHANNEL_CONFIG WHERE CHANNEL_TYPE_ID IN ('SMS','WHATSAPP','PUSH','INTERNAL')`, or use a fresh database.
- **Dispatch status** is still 200, not the plan's 202 (unchanged; the api-docs are authoritative).

## 7. Residue per run

- Logs with `referenceType='USER_ACCOUNT_<RUN>'`. They are read-only, so they stay.
- Two templates, deactivated.
- One run-owned channel config plus the `ZZ<RUN>` probe, disabled. `CHANNEL_TYPE_ID` stays taken.
- SEC users `notif-inact-<RUN>` (only when no inactive user existed) and `notif-noperm-<RUN>`, both disabled.
- One IN_APP inbox item for the caller.

The test databases for this re-run were dropped afterwards.
