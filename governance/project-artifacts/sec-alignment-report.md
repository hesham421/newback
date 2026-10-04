# SEC — ALIGN-BE alignment report

Phase 8 (ALIGN-BE) is a verification-and-reporting phase: no application code was produced,
and nothing found here was fixed. Every claim below was re-derived from the artifacts and the
implemented sources on 2026-09-11, not carried from the generated plan.

Scope: `src/main/java/com/erp/sec/**` (129 files), `src/main/resources/db/migration/V16__sec_schema.sql`,
`V17__sec_security_seed.sql`, `governance/modules/SEC/**`, against
`.claude/skills/gov-validate-backend-feature/SKILL.md`,
`.claude/skills/gov-enforce-backend-contract/SKILL.md` and
`governance/GOVERNANCE-RULES.md` (including its **Convention Precedence** section).

Companion documents: `governance/project-artifacts/sec-implementation-notes.md` (the decision
record for the whole build) and `governance/modules/SEC/execution-state.json` → `api_doc_gaps[]`.

---

## Part 1 — Re-verification of the generated ALIGN self-check

The `## Alignment self-check (ALIGN) — SEC v1` block in
`governance/modules/SEC/packages/backend-execution/_SECTIONS.md` (mirrored verbatim in
`governance/modules/SEC/P3_1/backend-execution-plan-sec.md`) closed with
`RESULT  PASSED ✓ — 0 findings`. That verdict was false when generated and is false now.
Each of its ten rows was re-checked against the current artifacts and the implemented module.

| Row | Original claim | Verdict | Evidence |
|---|---|---|---|
| TRACEABILITY | every API-*/QR-*/RULE-*/DBF-* in the Plan Index; every PHASE/SUB/atom carries `traces=`; every traces target exists upstream | **HOLDS** | 27/27 `<!-- API:API-SEC-nnn:START` markers carry `traces=`; 8 phases (6 `PHASE:` markers + DATA-DOM/SVC-API via 6 `SUB:` markers) all carry `traces=`; `diff` of the REQ-SEC id sets in the plan and `P1/srs-sec.md` → identical (33); `diff` of the DBF-SEC id sets in the plan and `P2/db-script-sec.md` → identical (104) |
| BINDING (§2A) | no placeholder table/column/key/generation object; every column cites a DBF; every RULE message in ar+en | **PARTLY** | No placeholder token (`TBD`/`TBC`/`XXX`/`???`) occurs in the plan, and every column cites a DBF — but the row did not catch `api_doc_gaps` **#2**, a real column-name mismatch (`grant_at` in ENT-SEC-007's FIELDS table vs `granted_at` in db-script §1/§3/COMMENT ON, DBF-SEC-064) that it should have. And the *generation object* it certifies is not what ships: `V16__sec_schema.sql` creates 13 `SEQ_SEC_*` sequences and all 13 entities use `GenerationType.SEQUENCE` + `allocationSize = 1`, per GOVERNANCE-RULES.md Convention Precedence 1 — not `GENERATED ALWAYS AS IDENTITY`, which the plan still names in 13 BINDINGS lines and the Phase 1 type table |
| MANIFEST (§4) | only the 4 mandated columns beyond DBF/ENT; all 104 DBF listed; 0 ⏸ rows (0 XM) | **HOLDS** | 104 distinct DBF ids in the manifest, matching db-script §1 exactly; db-script §2 XM REGISTER reads "None — SEC is ROOT"; no ⏸ row present |
| QRC (§5) | every API with a DB operation has ≥1 QR; "logical spec, not code" framing; no join for a lookup label; exact generation object named | **PARTLY** | Verified mechanically: all 27 API atoms in `packages/backend-execution/SVC-API/*.md` cite at least one `QR-SEC-nnn`; the catalog header carries the framing; lookups are CHECK-constrained so no label join exists. Two defects the row does not report: the generation object it names is superseded (see BINDING), and **QR-SEC-025's declared query was dead code** — `ActiveSessionRepository.findNonTerminated(Pageable)` had no caller anywhere in `src/main/java` after API-SEC-025 moved to a `SpecBuilder`-driven `POST /search` (finding F-ALIGN-1, A.2.9 — **FIXED** 2026-09-11 by deleting the orphan method; `plan:792` declares `join NONE`, so no artifact ever required the query) |
| API (R3) | every RULE in a Validations line has a catalog row; platform errors carry PLATFORM-STD + ADR-SEC-002; create/update requests exclude PK/audit/system fields | **PARTLY** | The catalog half holds, restated after the 2026-09-11 fix: the §Error Catalog has **28 rows** — 27 module-owned `SEC-*` rows pairing 1:1 with the **27** `SecErrorCodes` constants, all 27 present in **both** `messages.properties` and `messages_ar.properties`, plus **1** platform row (`INTERNAL_ERROR`) that the shared `GlobalExceptionHandler` owns and SEC neither declares nor throws. As originally written this row read "28 catalog rows = 28 `SecErrorCodes` constants", which held only while SEC wrongly declared `SEC_500`. The DTO half holds: no `*CreateRequest`/`*UpdateRequest` carries a PK, audit or system field, and `UserUpdateRequest` correctly omits the immutable `username`/`password`. But the check only ever tests *exclusion*, never whether a field an API **names** is actually **defined** — which is exactly why it passed API-SEC-011 while the approved sign-up's credential was unspecified (`api_doc_gaps` **#4**) and API-SEC-022 while three of its Response figures were undefined (`api_doc_gaps` **#6**). Separately, `SEC-500` — named by **8** API blocks (the report originally said nine; the true count is eight) as their only error — was never emitted (finding F-ALIGN-2, **FIXED** 2026-09-11) |
| CROSS-MODULE | 0 XM from db-script, 0 placed, 0 mismatched; inbound stub uses XM-INBOUND-STUB-1 notation, not TODO | **PARTLY** | True at the **schema** level and only there: db-script §2 records no consumed entity, table or FK, so the XM-row count is genuinely 0/0/0 in the CONSUME direction, and `XM-INBOUND-STUB-1` is present in `INT-R.md` as claimed. At the **API** level SEC is not isolated — `PasswordResetService` imports and calls `com.erp.notif.crossmodule.NotificationDispatchApi` (with `DispatchCommand`), srs-sec.md §A8's one declared SOFT integration. INT-R additionally recorded two inbound contracts SEC did **not** satisfy: XM-INBOUND-GAP-1 and XM-INBOUND-GAP-2 (`api_doc_gaps` #8, #9). **SUPERSEDED 2026-09-11:** the sentence "`src/main/java/com/erp/sec/crossmodule` does not exist" was true when this row was written and is now false — SEC exposes `SecUserDirectoryApi` (`findContact` → REQ-SEC-034; `findUserIdsHoldingPermission` → REQ-SEC-035, QR-SEC-039) with `UserContact` and `SecUserDirectoryApiImpl`, and both inbound gaps are CLOSED. The XM-row count is still 0: the surface registers no entity, table or column. Note the resulting MODULE-level cycle once NOTIF adopts it (SEC→NOTIF dispatch, NOTIF→SEC contact) — not a crossmodule-interface cycle, but stated rather than left to be discovered |
| SECURITY (R7) | every secured API declares its PERM_*; every secured screen has a Phase 7 seed row; ERP-4: every mutation endpoint declares its PERM_* | **PARTLY** | The seed half now **holds and is checkable**: all 9 Phase 7 page codes (`SEC_LOGIN`, `SEC_SIGNUP`, `SEC_PWD_RESET`, `SEC_USERS`, `SEC_ROLES`, `SEC_MODULE_REGISTRY`, `SEC_DASHBOARD`, `SEC_AUDIT_LOG`, `SEC_SESSIONS`) are seeded by `V17__sec_security_seed.sql`, which also seeds all 13 permission codes the Phase 7 matrix names. The API half holds with two stated exceptions and one false sub-claim: 22 of 27 `Security :` lines name a `PERM_*` and exactly 22 `PERM_`-based `@PreAuthorize` annotations exist in `com.erp.sec.service` — a clean 1:1 — while API-SEC-027 has no page code of its own (SRS B4) and is gated `isAuthenticated()`, and API-SEC-001..004 are public by contract. **The ERP-4 sub-claim is false as written**: API-SEC-002, API-SEC-003 and API-SEC-004 are `POST` mutations that write `SEC_SIGNUP_REQUEST` / `SEC_PWD_RESET_TOKEN` / `SEC_USER` rows and state "public — no permission required", so "every POST/PUT/PATCH/DELETE API above states one" is untrue. `PERM_SEC_ROLES_DELETE` is seeded but absent from `PermissionConstants` (finding F-ALIGN-3) |
| CORE (R1) | layers, domain placement, error signalling (`SEC-<HTTP-status>-<SCENARIO>`) and type mapping all declared | **HOLDS — but only after a correction** | The declared format now matches reality: all **27** `SecErrorCodes` values have the shape `SEC-<HTTP>-<SCENARIO>`. At ALIGN-BE there were 28, of which `SEC-500` was the one scenario-less generic row; it was removed on 2026-09-11 (F-ALIGN-2), so the shape is now exceptionless. As **generated**, this row certified `SEC-<3-digit-sequence>` — a format matching no catalog row and no emitted code. That was `api_doc_gaps` **#1**; the row passes today because the gap was corrected, not because the check worked |
| DECISIONS | ADR-SEC-001 and ADR-SEC-002 both ACCEPTED, non-breaking; no BLOCKED ADR | **FALSE** | `find . -iname 'ADR-SEC-*'` returns nothing, and no `decisions/` directory exists anywhere in the repository. Both are cited as `erp/decisions/SEC/ADR-SEC-00N.md` — in the plan, `_SECTIONS.md`, `governance/modules/project-registry.md:176-177` and across the MDL artifacts — and neither file has ever existed here. Their content is stated inline in the artifacts that cite them and nowhere else; the "ACCEPTED" status is therefore unevidenced. (Independently noted at `governance/project-artifacts/generator-defect-report-and-fix-prompt.md:155-156`.) Correct on its own terms: there is no BLOCKED ADR |
| RESULT | `PASSED ✓ — 0 findings` | **FALSE** | 11 findings at ALIGN-BE: the 9 then-recorded `api_doc_gaps[]` entries plus 2 new at this phase. Four of the ten rows above are only partly true and one is false. **Since updated:** `execution-state.json` now holds **12** entries — the 2 ALIGN-BE findings (#10, #11) were closed on 2026-09-11 and a 12th (`_SECTIONS.md`'s stale index rows) was recorded and closed the same day |

---

## Part 2 — `gov-validate-backend-feature`, re-run for real

### Feature: SEC v1 (27 APIs, 13 entities)  ·  Module: SEC  ·  Date: 2026-09-11

**How N/A and deviations are counted.** A check that cannot apply to this module is marked
**N/A with its reason** and removed from *both* numerator and denominator — never counted as a
pass. A check the build deliberately departed from under a recorded decision is labelled
**DEVIATION**, named in the deviation register below, and **not** awarded its point.

### STAGE 0 — Build order — 8 / 9

| # | Check | Result |
|---|---|---|
| 1 | Entity extends `AuditableEntity` | ✅ 13 entities; 5 extend it, 8 carry the SH.1 declared exemption (their tables have no `created_*`/`updated_*` columns — `sec-implementation-notes.md` §1, evidenced per-table against db-script §3) |
| 2 | Repository extends `JpaRepository` + `JpaSpecificationExecutor` | ✅ 13/13, all `@Repository` |
| 3 | DTOs — full required set | ✅ every one of the 27 API contracts has its request/response DTO (47 DTOs). See D4 for `UsageResponse` |
| 4 | Mapper `@Component` | ✅ 12 mappers, all `@Component`. `PasswordResetToken` has none — N/A, the token never becomes a DTO (the raw token leaves only in the dispatch variables) |
| 5 | Domain per decision-making entity | ✅ 12 Domain classes, plain, `create()`/`from()` only. `AuditLogEntry` has none — N/A, an append-only record makes no permit/deny decision (A.0.1 Decision Test) |
| 6 | Error codes in `SecErrorCodes` | ✅ 28 constants at ALIGN-BE; **27** after `SEC_500` was removed on 2026-09-11 (F-ALIGN-2) |
| 7 | VIEW/CREATE/UPDATE/DELETE in the permission constants class | ❌ **FAIL** — `PermissionConstants` declares 12 of the 13 codes `V17` seeds; `PERM_SEC_ROLES_DELETE` is missing (F-ALIGN-3) |
| 8 | Service `@Service` | ✅ 13 |
| 9 | Controller `@RestController` | ✅ 10 |

### STAGE 1 — File inventory — 14 / 15

Present: entity, repository, `*CreateRequest` (5), `UserUpdateRequest`, `*Response`,
`*SearchRequest` (5), mapper, domain, `exception/SecErrorCodes.java`, service, controller,
`permission/PermissionConstants.java`, `messages.properties`, `messages_ar.properties`.

- `dto/<Entity>UsageResponse.java` — **absent for every entity**. Recorded as deviation **D4**,
  not as N/A: SEC has no row-delete operation and no `GET /{id}/usage` in any of its 27 API
  contracts, so nothing would consume it — but the skill lists it among the 15 mandatory files,
  so the point is not awarded.
- `dto/<Entity>OptionResponse.java` — conditional, not counted; SEC exposes no dropdown endpoint.
- `crossmodule/<Name>Api.java` — conditional, not counted. **SUPERSEDED 2026-09-11:** as scored,
  this line read "SEC publishes no cross-module surface", flagged because `api_doc_gaps` #8 and #9
  were precisely two consumers that needed one. SEC now publishes exactly one:
  `crossmodule/SecUserDirectoryApi.java` (+ `UserContact.java`, `SecUserDirectoryApiImpl.java`),
  REQ-SEC-034 / REQ-SEC-035. The file is conditional, so the 14/15 score is unchanged.

### STAGE 2 — Layer contracts — 73 / 78 applicable (7 N/A)

| Layer | Checks | Applicable | Passed | Failed / deviation | N/A (reason) |
|---|---|---|---|---|---|
| Domain | 7 | 7 | 7 | 0 | — |
| Entity | 19 | 17 | 17 | 0 | A.1.9 (`@OneToMany`), A.1.16 (`@Formula` counts) — SEC maps no collection anywhere |
| Repository | 9 | 9 | 8 | **A.2.9** | — |
| DTO | 13 | 11 | 11 | 0 | A.3.12 (no `UsageResponse`), A.3.13 (no `OptionResponse`) |
| Mapper | 7 | 6 | 6 | 0 | A.4.7 (no `toUsageResponse`) |
| Service | 18 | 17 | 16 | **A.5.2** (D7) | A.5.11 (module has no `delete()` method) |
| Controller | 12 | 11 | 8 | **A.6.4** (D9), **A.6.5** (D8), **A.6.9** | A.6.8 (no usage endpoint) |
| **TOTAL** | **85** | **78** | **73** | **5** | **7** |

Notable positives, verified rather than assumed:

- **A.5.18 / domain delegation is genuinely clean.** All 21 business-rule throws live in
  `com.erp.sec.domain.*` (10 × `Status.ALREADY_EXISTS`, 11 × `Status.CONFLICT`). The only
  exceptions raised inside a service are `Status.UNAUTHORIZED` (login), `Status.VALIDATION_ERROR`
  (invalid sort field) and 12 `Status.NOT_FOUND` load failures via `orElseThrow` — none of them a
  business rule. No Domain class carries a Spring/JPA annotation or holds a repository.
- **A.1.3 / A.1.4**: 13/13 `GenerationType.SEQUENCE`, 13/13 `allocationSize = 1`, 13
  `CREATE SEQUENCE` in V16. Zero `IDENTITY`/`AUTO`.
- **A.1.8**: all 13 `@ManyToOne` are `FetchType.LAZY`. **A.1.10**: 13 `@SuperBuilder`, zero `@Builder`.
- **A.2.x**: 12 `existsBy*`, `existsByEmailAndUserPkNot` for the one mutable unique field, 16
  `JOIN FETCH`, 9 JPQL `SELECT COUNT`, and `RoleUserCountProjection` for the one grouped read.
  No SEC repository is referenced outside `com.erp.sec`.
- **A.3.x**: 47/47 DTOs carry a class-level bilingual `@Schema`; every data field carries its own
  `@Schema`; validation messages are i18n keys only (`{validation.required|size|invalid}`); 22
  `@JsonFormat(... "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")`. The five `*SearchRequest`
  types use `@SuperBuilder` rather than `@Builder` — the correct form for a subclass of
  `BaseSearchContractRequest`, counted as a pass.
- **A.6.x**: 27 `@Operation` for 27 endpoints, 20/20 `@Valid @RequestBody`, 10/10 bilingual
  `@Tag`, zero repositories injected into a controller, zero business logic.

### STAGE 3 — Cross-cutting — 25 / 29 applicable (8 N/A)

| Area | Checks | Applicable | Passed | Failed / deviation | N/A (reason) |
|---|---|---|---|---|---|
| 3.1 Error handling | 8 | 7 | 6 | **not-found → NOT_FOUND** (D6) | "delete() does not try-catch" — no `delete()` exists |
| 3.2 Caching | 5 | 2 | 2 | 0 | the three "if cached" checks — SEC caches nothing and the approved register is empty |
| 3.3 Security | 4 | 4 | 2 | **four permissions defined** (F-ALIGN-3), **@PreAuthorize on every public service method** (D7) | — |
| 3.4 Immutability | 4 | 4 | 4 | 0 | — |
| 3.5 Response envelope | 8 | 6 | 5 | **controller crafts every non-delete response** (D9) | "delete returns void", "@ResponseStatus(NO_CONTENT) for delete" — no `delete()` |
| 3.6 Domain delegation | 4 | 4 | 4 | 0 | — |
| 3.7 Cross-module & eventing | 4 | 2 | 2 | 0 | the two eventing checks — SEC publishes no event (`ApplicationEventPublisher` appears 0 times) |
| **TOTAL** | **37** | **29** | **25** | **4** | **8** |

Verified: zero raw/generic exceptions in the module; all codes in both bundles (28 at ALIGN-BE, 27 after
the 2026-09-11 `SEC_500` removal — re-verified, 27 keys in each bundle); zero caching
annotations; zero `@ControllerAdvice` in `com.erp.sec`; all 28 public service methods return
`ServiceResult<T>` (30 `ServiceResult.success(...)`, 9 `Status.CREATED`, 7 `Status.UPDATED`);
zero `@ResponseStatus` anywhere in the controllers; the one cross-module dependency SEC *consumes*
is injected only into `PasswordResetService` and never into a Domain, mapper or controller (still
true after the 2026-09-11 amendment: the interface SEC *exposes*, `SecUserDirectoryApi`, is
implemented by a dedicated `@Component` delegating to `UserService` and is referenced by no
Domain, mapper or controller either).

### STAGE 4 — Compilation — 2 / 2

```
export JAVA_HOME="$HOME/opt/jdk-25-temurin/Contents/Home"
mvn -DskipTests clean compile
...
[INFO] Compiling 241 source files with javac [debug parameters release 25] to target/classes
[INFO] BUILD SUCCESS
```

Exit 0. The only warnings emitted are JVM-level and pre-existing, not attributable to SEC:
`sun.misc.Unsafe::objectFieldOffset has been called by lombok.permit.Permit`. A repeat
`mvn -DskipTests compile` reports "Nothing to compile — all classes are up to date", also exit 0.

### Shared-layer compliance gate (CU.1 – CU.8) — **PASS**

| # | Result |
|---|---|
| CU.1 | ✅ no custom audit base class; `AuditableEntity` or a declared exemption |
| CU.2 | ✅ the 5 native `BOOLEAN` columns take no `@Convert` — zero `@Convert` in the module, which is the correct mapping (Convention Precedence 2) |
| CU.3 | ✅ 28/28 `ServiceResult<T>`; no custom wrapper |
| CU.4 | ✅ 25 `LocalizedException` throws, zero raw exceptions |
| CU.5 | ✅ `SpecBuilder` + `PageableBuilder` in every search |
| CU.6 | ✅ with a note — 26 of 27 endpoints return `operationCode.craftResponse(...)`; API-SEC-024's CSV export returns a raw body. That is a bypass, not a *custom envelope*, so the CU.6 violation condition ("custom wrapping") is not met; it is scored against A.6.4 / 3.5 instead (D9) |
| CU.7 | ✅ zero `@ControllerAdvice`/`@RestControllerAdvice` in `com.erp.sec` |
| CU.8 | ✅ no per-module envelope |

### Score

| Stage | Applicable | Passed |
|---|---|---|
| 0 — Build order | 9 | 8 |
| 1 — File inventory | 15 | 14 |
| 2 — Layer contracts | 78 | 73 |
| 3 — Cross-cutting | 29 | 25 |
| 4 — Compilation | 2 | 2 |
| **TOTAL** | **133** | **122** |

**SCORE: 122 / 133 applicable (91.7%).** Against the skill's fixed 133-point-free denominator of
148 it is 122 / 148 (82.4%). Both land in the same band, so the verdict does not depend on how
the 15 N/A checks are treated.

**VERDICT: 🔶 CONDITIONAL** (80%+ — fix before proceeding).

The skill's automatic-rejection list is triggered on two counts, both of which this report
classifies as labelled deviations rather than silent passes, and neither of which is re-graded
downward to REJECTED because each has a recorded, artifact-grounded justification:
"a service method without `@PreAuthorize`" (D7) and "repository methods with no caller in any
service" (F-ALIGN-1 — a genuine defect, but one method, not a systemic breach).

---

## Part 3 — Deviation register

Each deviation is a decided departure, traceable to `GOVERNANCE-RULES.md` §Convention Precedence
or to `sec-implementation-notes.md`. None is scored as a pass it did not earn.

| # | Deviation | Justification | Cost |
|---|---|---|---|
| D1 | PK = `GenerationType.SEQUENCE` + 13 `SEQ_SEC_*`, not the db-script's `GENERATED ALWAYS AS IDENTITY` | GOVERNANCE-RULES.md Convention Precedence 1 — settled; one database already holds four modules on `SEQ_<TABLE>` (V1–V15 applied) and Flyway forbids rewriting them | none (A.1.3's own text mandates it) — but the plan text was never updated, which is why BINDING and QRC now certify a generation object that is not built |
| D2 | Native `BOOLEAN` columns mapped to plain `Boolean`, no converter | Convention Precedence 2 — a converter on a native `BOOLEAN` column is itself the violation | none |
| D3 | `UQ_<TABLE>_<DESC>` constraint naming taken verbatim from the db-script | Convention Precedence 3 — physical DB object names must match the migration; `V16` contains 10 `UQ_` and zero `UK_` | none |
| D4 | No `UsageResponse` and no `GET /{id}/usage` anywhere | SEC has no row-delete operation to gate, and no API contract asks for one | STAGE 1 −1 |
| D5 | `POST /search` for the 5 criteria searches; `GET` retained for `/dashboard`, `/menu` and `/audit-log/export` | `sec-implementation-notes.md` §8 — the earlier all-`GET` deviation was reversed; the three that stay `GET` carry no criteria or are a file download | none (A.6.6 passes) |
| D6 | An unknown reset token answers `SEC-409-RESET-TOKEN-INVALID` (409), not a 404 | `sec-implementation-notes.md` §5 — an unknown and an expired token must be indistinguishable, or the endpoint becomes a token oracle | 3.1 −1 |
| D7 | 4 public service methods with no `@PreAuthorize` (`AuthService.login`, `PasswordResetService.request`/`.complete`, `SignupRequestService.submit`) | API-SEC-001..004 are declared "public — no permission required" in their plan `Security :` lines; a gate there would make login unreachable | A.5.2 −1, 3.3 −1. **On the automatic-rejection list** |
| D8 | Three `DELETE`-verb endpoints return 200 + body instead of 204 + void | None of them deletes a row: `DELETE /users/{id}` deactivates, `DELETE /sessions/{id}` terminates, `DELETE /roles/{id}/modules/{moduleId}` returns the cascade count RULE-SEC-003 requires | A.6.5 −1 |
| D9 | API-SEC-024 `GET /audit-log/export` returns a raw `text/csv` body outside `ApiResponse` | A file download cannot be wrapped in the JSON envelope | A.6.4 −1, 3.5 −1 |

---

## Part 4 — Findings

### Already recorded — the nine `api_doc_gaps[]` entries

All nine are in `governance/modules/SEC/execution-state.json`, each with its own `detail` and
`resolution`. Summarised by disposition:

- **Closed by realignment (2):** #1 error-code format (`SEC-<HTTP>-<SCENARIO>`), #2 `grant_at` →
  `granted_at`.
- **Answered by an implementation choice that still awaits human confirmation (4):** #3
  RULE-SEC-005's missing conflicting-pair source (guards implemented, resolver empty), #4 the
  approved sign-up's credential (unusable random secret + the existing reset pair as the route
  in), #6 the three undefined dashboard figures (N = 10, stalled = 7 days, privileged = holds a
  non-VIEW action grant), #5 the internal-caller pattern (authorization half closed; the
  transaction half explicitly still open).
- **Open, needing a decision outside SEC (3 as scored; 1 today):** #7 no producer for the
  published `api-docs` artifact, #8 XM-INBOUND-GAP-1 (FIN assumes a role-members read API that
  does not exist), #9 XM-INBOUND-GAP-2 (NOTIF cannot resolve a SEC user's email, so REQ-SEC-029
  does not deliver). **UPDATED 2026-09-11:** #8 and #9 are both CLOSED — the human authorized
  the P1/P2 decision this summary said was owed, and SEC gained its first cross-module read
  surface (`SecUserDirectoryApi`), so #7 is the only one of the three still open. Re-derived
  across all 13 `api_doc_gaps[]` entries the same day: 8 closed · 3 answered by an
  implementation choice with a human-only question left over · 0 partially closed · 2 open
  (#7, #13).

### New at ALIGN-BE

**F-ALIGN-1 — `ActiveSessionRepository.findNonTerminated(Pageable)` was dead code (A.2.9). CLOSED 2026-09-11.**
Severity MEDIUM (state at ALIGN-BE; see the FIXED note below). The method carried QR-SEC-025's
`JOIN FETCH` + paired count query and was the only repository method in the module with no caller
anywhere in `src/main/java` (all declared repository methods were checked). It was orphaned by the
§8 reversal: API-SEC-025 now builds its
page through `SpecBuilder` + the unconditional `notTerminated` predicate in `SessionService`, so
QR-SEC-025 no longer maps to the query the plan declares for it. A.2.9 is on the skill's
automatic-rejection list. **Appended** to `api_doc_gaps[]` as a `MAPPING_GAP`.

**FIXED 2026-09-11.** The orphan method and its two then-unused imports (`Page`, `Pageable`) were
deleted from `src/main/java/com/erp/sec/repository/ActiveSessionRepository.java`; the three
surviving methods (`findNonTerminatedByUser`, `findByTokenRef`, `countNonTerminated`) each have a
caller, so A.2.9 now holds across all 34 remaining SEC repository methods. **Zero governance edits
were required**: `backend-execution-plan-sec.md:792` declares `Repository : QR-SEC-025 · join NONE`
and the QRC at `plan:376` already registers QR-SEC-025 as `FIND_BY_CRITERIA`, so every artifact
naming QR-SEC-025 already described the `SpecBuilder`-driven behaviour that ships. Known, accepted
consequence: `ActiveSessionMapper.toResponse` now dereferences the LAZY `user` per row inside the
read-only transaction (an N+1 on API-SEC-025's page). Correct but slower. No `@EntityGraph` was
added — `join NONE` mandates no fetch join, so that would be an un-mandated optimisation.

**F-ALIGN-2 — `SEC-500` was contracted but unreachable. CLOSED 2026-09-11.** Severity MEDIUM (state at ALIGN-BE; see the FIXED note below). **Eight** API blocks stated
`Errors : SEC-500 only` (and `Errors : none beyond platform-standard (§Error Catalog SEC-500)`),
the catalog registered `SEC-500` with both ar and en messages, and `SecErrorCodes.SEC_500` existed —
but it had **zero references** in `src/main/java`, and the shared
`com.erp.common.web.GlobalExceptionHandler`'s `@ExceptionHandler(Exception.class)` answers
`.code("INTERNAL_ERROR")`. A server error on any SEC endpoint therefore carried `INTERNAL_ERROR`,
never `SEC-500`, and the two bundle entries were unreachable strings. The shared handler is
infrastructure this phase must not modify and a per-module `@ControllerAdvice` is forbidden
(CU.7), so this is a contract question, not a local fix. **Appended** to `api_doc_gaps[]` as a
`MAPPING_GAP`.

**FIXED 2026-09-11** — SEC stopped claiming a code it does not own. Governance: the 8 `Errors :`
lines (`plan:719, 733, 747, 761, 776, 790, 804, 1093` and their twins
`SVC-API/SVC-API-SEARCH.md:14, 28, 42, 56, 71, 85, 99` + `SVC-API/SVC-API-INT.md:112`) now read
`INTERNAL_ERROR only (platform-standard, shared handler)`; the catalog row (`plan` §Error Catalog
and `_SECTIONS.md`) is now `INTERNAL_ERROR | PLATFORM-STD (infrastructure, shared
GlobalExceptionHandler — not module-scoped)` with both message cells copied verbatim from the
`INTERNAL_ERROR=` bundle entries; the CORE error-signalling paragraph (`plan` + `CORE/CORE.md`) now
states that every `SecErrorCodes` value carries the full `SEC-<HTTP-status>-<SCENARIO>` shape and
that the 500 path is owned by the shared handler; `P3_1/registry-exec-be-sec.md`'s CATALOG
arithmetic was corrected to 26 + 2 generic = 28. Source: on an explicit human decision (no rule
compelled it — the automatic-rejection list covers dead *repository methods*, not constants),
`SecErrorCodes.SEC_500` and both `SEC-500=` bundle keys were removed, and `MenuService`'s stale
javadoc was rewritten. `com.erp.common` was **not** touched and **no** per-module
`@ControllerAdvice` was added (CU.7 holds). Durable cross-module note: the byte-identical
`<MOD>-500` row exists at `governance/modules/MDL/P3_1/backend-execution-plan-mdl.md:540` and
`governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1118` — a generator-level defect that
will recur verbatim when MDL and FIN are built. Neither was modified.

**F-ALIGN-3 — `PERM_SEC_ROLES_DELETE` is seeded but not declared.** Severity LOW.
`V17__sec_security_seed.sql` seeds 13 permission codes and grants all 13 to `SYS_ADMIN`;
`PermissionConstants` declares 12. The missing one is `PERM_SEC_ROLES_DELETE`, which the Phase 7
matrix itself marks "reserved — no delete-role endpoint in v1". **Not appended** — the plan
already says it is reserved, so this is a known scope decision, not a documentation gap. Costs
STAGE 0 check 7 and one 3.3 check.

**F-ALIGN-4 — `/api/v1/sec/roles` is served by two controllers (A.6.9).** Severity LOW.
`RoleController` and `RoleGrantController` both declare
`@RequestMapping("/api/v1/sec/roles")`. Spring accepts it and the paths do not collide, but
A.6.9 asks for child endpoints under the same controller and no artifact records a decision to
split them. Reported as ambiguous rather than asserted as a defect; the point is not awarded.
**Not appended** — a code-structure question, not a documentation gap.

**F-ALIGN-5 — the two ADRs do not exist.** Severity MEDIUM (governance, not runtime). See the
DECISIONS row in Part 1. **Not appended** — already recorded at
`governance/project-artifacts/generator-defect-report-and-fix-prompt.md:155-156`.

### Observation, not a finding

`activate()` / `deactivate()` on `Role`, `ModuleRegistry`, `ScreenRegistry` and `ActionRegistry`
(8 methods) have no caller: the only call sites in the module are `UserService`'s two. A.1.18
mandates the helpers on any entity with an `IS_ACTIVE_FL` column, so their presence is correct —
but the consequence is that **no role and no registry row can be deactivated through any SEC v1
endpoint**, while `V17` seeds and grants `PERM_SEC_ROLES_DELETE` and
`PERM_SEC_MODULE_REGISTRY_UPDATE` for exactly that purpose, and `MenuService` and both grant
queries already exclude inactive rows. The plan states this ("reserved — no delete-role endpoint
in v1, deactivate only, which is UPDATE"), so it is a stated v1 scope boundary rather than a
drift — but SRS B4 calls deactivation the role's only DELETE, so the capability the SRS describes
has no implementation.

---

## Part 5 — What changed as a result of this phase

Nothing under `src/`. `git diff --name-only` lists no `.java` file, no migration, no i18n bundle.
The phase produced: this report; the rewritten `## Alignment self-check (ALIGN)` block in both
`packages/backend-execution/_SECTIONS.md` and `P3_1/backend-execution-plan-sec.md` (that *block*
was kept byte-identical); two appended `api_doc_gaps[]` entries; the ALIGN-BE phase and sub marked
COMPLETE; and §12 of `sec-implementation-notes.md`.

**Correction (2026-09-11).** The claim that the plan and `_SECTIONS.md` are kept byte-identical was
untrue of the file as a whole: the EXECUTION PLAN INDEX API table in `_SECTIONS.md` still carried
the pre-reversal `GET` verbs and collection paths for API-SEC-005, 012, 021, 023 and 025 while the
plan's twin table already carried `POST .../search`. The five rows were brought into exact
agreement with the plan on 2026-09-11 (the plan's table is authoritative, and matches the five
`@PostMapping("/search")` controllers); the two tables now `diff` clean. API-SEC-022, 024 and 027
are genuinely `GET` and were left alone.

**Amendment landed after this phase (2026-09-11).** SEC gained its first cross-module read
surface on an explicit human authorization, closing `api_doc_gaps` #8 and #9 together:
`src/main/java/com/erp/sec/crossmodule/{SecUserDirectoryApi,UserContact,SecUserDirectoryApiImpl}.java`,
`UserService.findContact` / `findUserIdsHoldingPermission`,
`RoleActionGrantRepository.findUserIdsHoldingPermission` (QR-SEC-039), and `User.STATUS_ACTIVE`
widened to `public` so the derivation of `UserContact.active` does not repeat the literal. No
migration (existing columns only) and nothing under `src/test/`. Reflected into the analysis
artifacts as the human required: REQ-SEC-034/035 + AC + the US-SEC-007 / US-SEC-012 rows + a
third §A8 table in `P1/srs-sec.md`; §2 XM REGISTER in `P2/db-script-sec.md`; the QRC, the QR id
definitions, the PRE-GENERATION counts, PHASE 6 INT-R and the ALIGN CROSS-MODULE / RESULT rows
in `P3_1/backend-execution-plan-sec.md` and its `packages/backend-execution/` twins
(`_SECTIONS.md`, `INT-R/INT-R.md`, `DATA-DOM/DATA-DOM-MASTER.md`); TC-SEC-034/035 in
`test_gen/backend-test-plan-sec.md`. Nothing under `governance/modules/FIN|NOTIF|CU|MDL|FILE/`
or `src/main/java/com/erp/{notif,cu,file,masterdata,common}/` was touched.

**Fixes landed after this phase (2026-09-11).** F-ALIGN-1 and F-ALIGN-2 are now closed — see their
entries in Part 4. `src/` is no longer untouched: `ActiveSessionRepository.java`,
`SecErrorCodes.java`, `MenuService.java` (javadoc only) and both i18n bundles changed.
