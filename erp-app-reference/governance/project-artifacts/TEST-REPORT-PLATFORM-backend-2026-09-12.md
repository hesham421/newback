# PLATFORM TEST REPORT — all six backend modules — 2026-09-12

First run in which **every module of the ERP backend was verified against the live API in one
session**. Three of the six had no runner at all before today.

Mechanism: `api-verify` (`.claude/skills/api-verify/SKILL.md`), **Full tier for all six** — every
module carries a test-execution manifest, so negatives are stage-C mapped from real
RULE→code→TC triples rather than self-derived. TestSprite was not used (retired).
Target: `http://localhost:7272`, Dev/Test per `api-verify-config.md` §4.1.

---

## 1. Result

| Module | Cases | Mechanism | Passed | Failed |
|---|---|---|---|---|
| FIN | 108 in force | `test_fin_apis.py` + `Fin*CoverageIntegrationTest` | 124 + 10 | **0** |
| SEC | 35 | `test_sec_apis.py` + `SecCoverageIntegrationTest` | 67 | 2 |
| MDL | 14 | `test_mdl_apis.py` | 30 | **0** |
| CU | 13 | `test_cu_apis.py` — **new today** | 19 | 3 |
| NOTIF | 16 | `test_notif_apis.py` — **new today** | 38 | 2 |
| FILE | 20 | `test_file_apis.py` — **new today** | 41 | 1 |
| | **206 in force** | | **319 + 10** | **8** |

**No production code was changed to make any assertion pass.** Every failure below was left
failing deliberately, because a test that goes green by asserting less than its specification is
worse than one that stays red.

Generated today: api-docs for CU (5 endpoints), NOTIF (14) and FILE (12), plus their three
api-verify scripts and problems reports under `governance/modules/<MOD>/test-api/`.

---

## 2. The two findings that matter, and why per-module testing could not have found them

### 2.1 Four governed error codes are unreachable over HTTP — one root cause, three modules

Half of this run's eight failures are the **same defect wearing three module names**:

| Module | Governed code | TC | What a caller actually gets |
|---|---|---|---|
| CU | `APP_CONFIGURATION_FIELDS_REQUIRED` | TC-BE-CU-004 | `400 VALIDATION_ERROR` |
| NOTIF | `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` | TC-BE-NOTIF-006 (create) | `400 VALIDATION_ERROR` |
| NOTIF | `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` | TC-BE-NOTIF-006 (update) | `400 VALIDATION_ERROR` |
| FILE | `FILE_DOCUMENT_OWNERSHIP_REQUIRED` | TC-BE-FILE-009 | `400 VALIDATION_ERROR` |

Each of these codes is **published in its module's api-docs "Known Error Codes" table** and named
in a manifest RULE→code→TC triple — and **no caller can ever receive one**. The DTO's own
`@NotBlank` rejects at the controller boundary before the domain rule can throw, so the response
is always the generic envelope. The HTTP status is correct (400) and the offending field is named
in `fieldErrors`; only the governed code is unreachable. The rules themselves are enforced — the
data cannot be written — so this is a **contract defect, not a data-integrity one**.

Classification: **spec-vs-implementation mismatch**, not an application bug and not a test bug.
The decision is a human's: either relax the bean validation so the domain rule owns the refusal,
or strike these codes from the catalogs as structurally unreachable. Publishing a code no caller
can receive is the one option that should not survive.

This is exactly the class of finding a per-module run reports as a local oddity and a
platform run reports as a pattern.

### 2.2 CU, NOTIF and FILE enforce no permissions at all

Every `@PreAuthorize(hasAuthority(...))` in those three modules is commented out behind
`TODO: SEC-PENDING — re-add ... once the new SEC module ships PermissionConstants`.

**That blocker is half-lifted, and nobody noticed.** `com.erp.sec.permission.PermissionConstants`
ships 43 constants — **27 of them `PERM_FIN_*`, and zero for `CONFIG_*`, `PERM_NOTIF_*` or
`PERM_FILE_*`**. FIN's gates were restored when its constants landed; the other three modules'
were not, and their TODOs still read as though nothing had shipped.

Verified independently, and consistently across three separate agent runs:

| Module | Active permission gates | Effect |
|---|---|---|
| FIN | 42 | fully gated |
| NOTIF | 3 (`isAuthenticated()` only) | 12 permission gates commented out |
| FILE | 2 (`isAuthenticated()` only) | all permission gates commented out |
| CU | **0** | 5 permission gates commented out; CU has no `SEC_MODULE_REG` row at all |

**Consequence: any authenticated user can create, update and delete platform configuration,
notification templates and channels, and files.** Anonymous access is correctly refused 401, so
this is a privilege-separation gap rather than an open door — but inside the authenticated
boundary there is no separation at all in three of six modules.

It is invisible to per-module testing by construction: a module with no permission to check has
no 403 to provoke, so its own suite reports the permission cases as *unreachable* rather than as
*failing*. Three independent agents each reported it as a local gap; only side by side is it one
finding. It is the platform's largest open security gap.

---

## 3. Other findings, by severity

**Likely real bug — update endpoints return a stale `updatedAt`.** Reproduced independently on CU
across three successive updates: the PUT response carries the *pre-update* timestamp while the
row holds the new one. `mapper.toResponse(saved)` reads the entity before `AuditEntityListener`'s
`@PreUpdate` stamps at flush. The persisted value is always right, so nothing is corrupted — but
a client that trusts the response body for optimistic concurrency or display will be wrong.
Map-before-commit is the `build-create-service` house pattern, so **this is worth checking on
every module's update endpoint, not just CU's**.

**Silent acceptance — CU answers 200 to a `configKey` change it ignores.** The key genuinely is
immutable, which is the point: the invariant holds and the report of it lies. A client is told a
rename succeeded that never happened. Same family as the platform-wide behaviour where an unknown
search *filter* field is silently ignored while an unknown *sort* field is properly rejected
(confirmed today on FIN, CU and MDL).

**SEC — 2 failures, both pre-existing, neither introduced today.**
- TC-SEC-020 (SoD conflict guard) is **known-unreachable by design in SEC v1**:
  `UserRoleService.conflictingCounterpartActions()` returns `Set.of()`, and its own javadoc says
  so — "the platform's only real pair is FIN-owned and FIN-enforced". The guard is live for the
  day a source exists. Verified at the line.
- Session search ignores the documented top-level `userId` filter — the script labels it a real
  bug and the evidence supports that: filtered and unfiltered responses are identical.

**NOTIF — a DEFERRED cross-module stub makes one case unassertable.**
`DefaultRecipientStatusReader` (XM-NOTIF-001) returns true for every recipient, so a dispatch to
an inactive SEC user still creates a log. TC-BE-NOTIF-010 was recorded as an observation rather
than a failure, correctly: asserting it would report a **documented deferral** as a defect.

**Legacy error-code format.** CU, NOTIF and FILE use `SCREAMING_SNAKE` codes
(`NOTIF_LOG_NOT_FOUND`) and a `{code, message, fieldErrors}` envelope, where
`api-verify-config.md` §3 specifies `{MOD}-{http}-{SLUG}` and `{code, messageAr, messageEn}`.
SEC/MDL/FIN follow the config. This is almost certainly Legacy-Path convention rather than drift,
but it means no bilingual message assertion is possible on those three modules, and it is
unrecorded anywhere until now.

---

## 4. Coverage honestly stated — what was *not* asserted

| Case | Why not | Recorded as |
|---|---|---|
| TC-BE-NOTIF-002 (retry then success) | No documented way to make the provider fail N times then succeed; no fault injection on the HTTP surface | GAP |
| TC-BE-NOTIF-003 (exhausted retries) | Terminal FAILED branch observed, but "fails 6 times" is not arrangeable from the API | GAP (partial) |
| TC-BE-NOTIF-010 (skip inactive recipient) | XM-NOTIF-001 is a DEFERRED stub — see §3 | observation |
| TC-BE-NOTIF-014 / TC-BE-FILE-019 (permission halves) | No permission is enforced — §2.2 | GAP |
| TC-BE-FILE-006 (expired-token half) | Token TTL is 10 minutes; needs a real idle wait. The **reuse** half is exercised for real: issue → download 200 → download again 401 | GAP (partial) |

Nothing in this table was quietly downgraded to make a total look better. Each is named in its
module's own problems report with the same reason.

---

## 5. Surviving records

No module in this platform publishes a hard delete, so **every run is additive to the dev
database**. Each module's problems report carries its own itemised `Surviving records` list;
the notable ones:

- **FILE** — documents 1-15, all soft-`DELETED`, and RULE-FILE-006 **retains the bytes**: a
  direct read confirms `length(file_content) = file_size` on every row, the largest ~2 MiB. That
  is the rule behaving exactly as specified, and also real disk residue.
- **NOTIF** — channel-config rows are **permanent residue**: `CHANNEL_TYPE_ID` is unique and
  dispatch reads the table for every module, so an abandoned row keeps influencing behaviour.
  Purge SQL is in the report.
- **CU / FIN / SEC** — deactivated rows only.

One row in the CU report, `PROBE_KEY_61874`, was created by a manual diagnostic of mine during
this session and is called out there as belonging to another session. It was retired through the
documented `DELETE` afterwards; recorded here so the two accounts agree.

**Privileges: none granted, none revoked, nothing standing.** Stage I was skipped in all six
runs — FIN/SEC/MDL because the bootstrap `admin` already holds what they need, and CU/NOTIF/FILE
because there is no permission in existence to grant (§2.2).

---

## 6. One environment defect worth writing down

The run was blocked three times by the same trap: **`target/classes` is shared with the VSCode
Java language server, which empties directories under a running application.** Symptoms differ
each time and none points at the cause — a `NoClassDefFoundError` on `UserDomain` making every
login 500; missing Lombok builder inner classes making `/v3/api-docs/{cu,notif,file}` return 500
while `sec`, `mdl` and `fin` were fine.

`mvn clean compile` fixes it until the next time. The durable fix, adopted mid-session: build
once with `mvn clean package -DskipTests`, copy the jar **out of `target/`**, and run
`java -jar <copy>`. The running app is then immune to any build or IDE activity, and every
subsequent module run was stable. Recommended as the default for any multi-module verification
session.

---

## 7. What to decide next

Nothing below was actioned, because each needs a human decision rather than an obvious fix.

1. **§2.2 — restore the permission gates on CU, NOTIF and FILE.** This is the highest-value item
   on the list, and the work is mostly mechanical: add the missing constants to
   `PermissionConstants`, seed the module/screen/action rows, uncomment the gates. It needs a
   decision because seeding permissions changes who can do what in every environment.
2. **§2.1 — decide what to do about the four unreachable error codes**: relax the bean validation
   so the domain rule answers, or strike the codes. Four TC assertions are failing against this
   right now.
3. **§3 — audit the `updatedAt` response defect across modules**, since the pattern is shared.
4. **§3 — decide whether an unknown search filter field should be rejected** as an unknown sort
   field already is. Cross-module contract change, so it is not FIN's or CU's to make alone.
5. **`system-test-index-erp.md` still rolls up only SEC, MDL and FIN.** After today the other
   three have real results; widening the governed rollup is a generation step, not an edit.
