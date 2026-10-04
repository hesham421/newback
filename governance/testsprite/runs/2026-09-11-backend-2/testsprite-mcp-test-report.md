
# TestSprite AI Testing Report(MCP)

---

## 1️⃣ Document Metadata
- **Project Name:** backend
- **Module:** SEC
- **Date:** 2026-09-11
- **Prepared by:** TestSprite AI Team + Claude (SEC test-phase re-run)

**IMPORTANT — supersedes the raw run captured in `testsprite_tests/tmp/raw_report.md`.**
Mid-run, `target/classes` was found completely emptied (0 `.class` files against 245
source files) while the app's JVM was still live — the same environmental
class of issue (a live JVM served from a target/classes directory rewritten out
from under it) previously described in `governance/modules/SEC/execution-state.json`'s
`api_doc_gaps[]` entry #7 and in the false-alarm this dispatch was sent to correct.
This corrupted TC002/TC004/TC005/TC006/TC007's results in TestSprite's own dashboard
run (`ClassNotFoundException`/`NoClassDefFoundError` on Lombok builder inner classes —
`ApiError$ApiErrorBuilder`, `UserCreateRequest$UserCreateRequestBuilder`,
`PasswordResetCompleteRequest$PasswordResetCompleteRequestBuilder`). The app was
stopped, rebuilt with `mvn clean compile` (377/377 classes confirmed present), and
restarted fresh; every previously-500ing scenario was manually re-verified via curl
to return its correct status/code. The 7 generated test scripts (unchanged — the API
surface did not change, only the environment) were then re-executed directly
(`python3`) against the now-stable app. **The results below are from that re-run**,
not from the original TestSprite dashboard execution.

---

## 2️⃣ Requirement Validation Summary

#### Test TC001 post api v1 sec auth login with valid credentials
- **Test Code:** [TC001_post_api_v1_sec_auth_login_with_valid_credentials.py](./TC001_post_api_v1_sec_auth_login_with_valid_credentials.py)
- **Status:** ✅ Passed
- **Analysis / Findings:** Login with `admin`/`admin` returns 200 with `data.accessToken`/`tokenType`/`expiresIn`. Matches TC-SEC-001.
---

#### Test TC002 post api v1 sec auth login with invalid credentials
- **Test Code:** [TC002_post_api_v1_sec_auth_login_with_invalid_credentials.py](./TC002_post_api_v1_sec_auth_login_with_invalid_credentials.py)
- **Status:** ❌ Failed
- **Analysis / Findings:** The script iterates 4 credential combinations and asserts 401 for all of them. The 3 combinations the governed TC-SEC-002 actually specifies (wrong password / unknown username / both) passed with 401 SEC-401-INVALID-CREDENTIALS as expected. The script's own 4th, self-added combination — empty username — correctly receives 400 (Bean Validation rejecting a blank required field) rather than 401, since input validation runs before the credentials check. This is a **test-script assumption bug** (over-broad assertion on a case outside TC-SEC-002's scope), not an application defect. See taxonomy: TEST_STRUCTURE_FAILURE.
---

#### Test TC003 post api v1 sec auth signup with new email
- **Test Code:** [TC003_post_api_v1_sec_auth_signup_with_new_email.py](./TC003_post_api_v1_sec_auth_signup_with_new_email.py)
- **Status:** ✅ Passed
- **Analysis / Findings:** Signup with a unique email returns 201 with `statusCode=PENDING`; no User row created. Matches TC-SEC-003.
---

#### Test TC004 post api v1 sec auth signup with duplicate email
- **Test Code:** [TC004_post_api_v1_sec_auth_signup_with_duplicate_email.py](./TC004_post_api_v1_sec_auth_signup_with_duplicate_email.py)
- **Status:** ✅ Passed
- **Analysis / Findings:** A second signup for the same email returns 409 SEC-409-SIGNUP-DUP. This scenario is not one of the 35 governed `TC-SEC-*` ids (the governed plan's TC-SEC-003 is happy-path only) — TestSprite generated it as additional, self-initiated coverage. Recorded as extra confidence, not counted against the governed coverage ratio.
---

#### Test TC005 post api v1 sec auth password reset request
- **Test Code:** [TC005_post_api_v1_sec_auth_password_reset_request.py](./TC005_post_api_v1_sec_auth_password_reset_request.py)
- **Status:** ❌ Failed
- **Analysis / Findings:** The script's own setup step (`POST /api/v1/sec/users`, creating a fixture user before requesting the reset) asserts the create-user response is `200`. The actual, correct response is `201 Created` — matching TC-SEC-009's own governed expectation ("201; User created with statusCode=ACTIVE"). The script never reaches its actual password-reset-request assertion. **Test-script bug** (wrong expected status on an unrelated setup call), not an application defect. TEST_STRUCTURE_FAILURE.
---

#### Test TC006 post api v1 sec auth password reset complete with valid token
- **Test Code:** [TC006_post_api_v1_sec_auth_password_reset_complete_with_valid_token.py](./TC006_post_api_v1_sec_auth_password_reset_complete_with_valid_token.py)
- **Status:** ❌ Failed
- **Analysis / Findings:** Same root cause as TC005 — the setup step's `POST /api/v1/sec/users` call is asserted against `200` instead of the correct `201`. Script never reaches the actual reset-complete assertion. TEST_STRUCTURE_FAILURE.
---

#### Test TC007 post api v1 sec auth password reset complete with invalid token
- **Test Code:** [TC007_post_api_v1_sec_auth_password_reset_complete_with_invalid_token.py](./TC007_post_api_v1_sec_auth_password_reset_complete_with_invalid_token.py)
- **Status:** ✅ Passed
- **Analysis / Findings:** A bogus/nonexistent token returns 409 SEC-409-RESET-TOKEN-INVALID. This is an adjacent proxy for governed TC-SEC-008 (which specifies an **expired** or **already-used** token fixture specifically, not a nonexistent one) — the same error code is returned via the same "not a valid, unused, unexpired token" code path, but TC-SEC-008's exact preconditions were never constructed. Recorded as a partial signal; TC-SEC-008 is still tracked as a coverage GAP in the governed cross-check below.
---

## 3️⃣ Coverage & Matching Metrics

- **57.14%** of the 7 generated tests passed (4/7)
- Against the 35-TC governed plan (`governance/modules/SEC/test_gen/backend-test-plan-sec.md`): 2 governed TCs PASS, 3 FAIL (script bugs, not app bugs), 30 GAP (never generated this run — see the module report's STEP 1.9 table for the full breakdown and taxonomy).

| Requirement (SEC Authentication) | Total Tests | ✅ Passed | ❌ Failed |
|---|---|---|---|
| Login (TC-SEC-001/002) | 2 | 1 | 1 (script bug) |
| Signup (TC-SEC-003 + 1 extra) | 2 | 2 | 0 |
| Password reset (TC-SEC-006/007/008-proxy) | 3 | 1 | 2 (script bug) |
---

## 4️⃣ Key Gaps / Risks

- **Environmental (resolved):** `target/classes` was found wiped mid-run under the live JVM, producing the same false-alarm class of failure this dispatch was sent to correct. Rebuilt and restarted; all previously-500ing scenarios independently re-verified as correct. No source code was touched.
- **Tool-capacity gap:** TestSprite's plan generator (Starter plan, 49 credits) produced only 7 test cases for the entire backend this run — all falling under SEC's `/api/v1/sec/auth/*` surface — versus the 35-TC governed SEC plan. 30 governed TCs have no TestSprite counterpart this run; see the module report's coverage table.
- **Script-bug risk:** 3 of the 7 generated scripts assert an incorrect expected HTTP status (200 where the API correctly returns 201, and 401 for an out-of-scope empty-username case where the API correctly returns 400). These are TestSprite generation artifacts, not defects — flagged for awareness if this run's scripts are reused verbatim in a future rerun.
- **Structurally unreachable:** TC-SEC-034/TC-SEC-035 exercise `com.erp.sec.crossmodule.SecUserDirectoryApi`, an in-process Spring interface, not an HTTP endpoint — never reachable by TestSprite's HTTP-only mechanism.
---
