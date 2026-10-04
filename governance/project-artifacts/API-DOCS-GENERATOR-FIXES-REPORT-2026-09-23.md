# api-doc-generator fixes — execution report (2026-09-23)

Executed from `PROMPT-api-docs-generator-fixes.md` against
`API-DOCS-GENERATOR-IMPROVEMENTS-2026-09-23.md`. Backend running on 7272; baseline captured for all six
modules into a scratch copy before any change, regenerated after every item, every changed line
classified. Nothing committed by this session (see "Working tree" — a separate commit swept up a
mid-task snapshot). No Java source, migration or annotation was changed.

## Measured baseline vs result

`@PreAuthorize`/`@Secured` counted in source with comments and string literals stripped (the prompt's
numbers, which a comment-blind grep inflates to 48 for FIN).

| Module | endpoints | annotations in source | permission rows **before** | permission rows **after** | authorization rules (`isAuthenticated()`) | declared none (checked, nothing there) | not extracted |
|---|---|---|---|---|---|---|---|
| CU | 5 | 5 | 0 | **5** | 0 | 0 | 0 |
| NOTIF | 14 | 15 | 0 | **12** | 2 | 0 | 0 |
| FILE | 12 | 11 | 0 | **9** | 2 | 1 (`FileService.softDelete`) | 0 |
| SEC | 34 | 34 | 27 | **27** | 3 | 4 (the public auth endpoints: login, signup, password-reset request/complete) | 0 |
| MDL | 11 | 11 | 11 | **11** | 0 | 0 | 0 |
| FIN | 38 | 43 | 0 | **38** | 0 | 0 | 0 |

SEC's "27 of 34" was never a parser gap: 3 endpoints carry `isAuthenticated()` (published as an
authorization rule) and 4 are public endpoints with no annotation anywhere on their path. The docs now
say which, per endpoint, and `check` distinguishes "declared none" from "not extracted" (0 everywhere).

Per-endpoint business errors, new: CU 4/5, NOTIF 11/14, FILE 10/12, SEC 29/34, MDL 9/11, FIN 38/38
endpoints carry a Business Responses table; every other endpoint prints what was walked and that no
throw site was found. Every walk left its controller (6/6 modules, 114/114 endpoints). Zero codes are
"thrown but reached by no walk" in any module after the depth fix below.

## Per item

1. **`@PreAuthorize`/`@Secured` parsing** — `extractors/security_extractor.py`.
   `_collect_annotations_above` returned only the FIRST line of a multi-line annotation (the report's
   claim that it "already returns the annotation paren-balanced" is wrong — that was the actual
   blackout). It now collects whole annotations by balanced parentheses with string literals honoured;
   `parse_authorization_annotation` joins the literal parts (any count, any whitespace, trailing-`+`
   style included), reads `value = "..."`, lists `@Secured({...})` authorities, and keeps a spliced
   non-literal part verbatim rather than dropping it. `resolve_permission` returns a `PermissionLookup`
   with an outcome — `FOUND`, `DECLARED_NONE`, `CONTROLLER_NOT_MATCHED`, `DELEGATE_UNRESOLVED`,
   `SERVICE_SOURCE_MISSING`, `SERVICE_METHOD_MISSING` — and the list of `Class.method` it read;
   the renderer prints the negative outcomes (`Authorization: no @PreAuthorize/@Secured declared —
   checked …` vs `not extracted — …`). No module, prefix or holder class is assumed; CU's `CONFIG_*`
   resolves like any other constant. Before/after: table above.
2. **Determinism** — `dto_extractor.find_page_envelope` (sorted schema pick, sorted bookkeeping rows),
   `response_model_extractor.find_envelope` (sorted pick), `openapi_extractor` (endpoints sorted by
   group, path, verb; security schemes sorted), `security_extractor` (sorted `rglob` for the delegate
   file). `sync.py`'s false "rendering is deterministic" docstring corrected. Proof: two separate
   processes render byte-identical files for all six modules; the on-disk docs from an earlier JVM run
   differed from a fresh render only in Page<T> row order before, in nothing after.
3. **Business error codes bound to endpoints** — new `extractors/business_error_extractor.py`.
   Indexes the module's own classes (comments and strings masked, methods found by balanced parens so
   annotated parameters do not hide a method), walks controller → service → domain by following
   static calls, field receivers, local/param receivers, `this`, method references, `new X(...)` and
   `X.factory(...).method(...)` chains — only into classes in the module's source. Depth 8: at depth 3
   FIN reported `FIN_422_MAPPING_UNSUPPORTED` as unbound and 21 endpoints hit the limit although the
   codes were reachable through real calls; 8 converges for SEC, FIN and NOTIF. Overloads that
   differ in what they throw are skipped and named as ambiguous. `ErrorDetail.of/ofField` codes are
   bound as `fieldErrors[]` entries with the `withDetails` Status when it is in the same body, else
   "not determined". Known Error Codes gains a "Raised by" column (endpoint count, or `unbound —
   thrown at …`, or `no throw site in module source`). Also found: `FinForbiddenAdvisor` and
   `SecForbiddenAdvisor` catch `AccessDeniedException` inside `com.erp.<mod>.service.*` and re-raise a
   module code, so SEC's 30 and FIN's 38 published "403 ACCESS_DENIED" rows were wrong; the 403 row now
   names `SEC_403_FORBIDDEN` / `FIN_403_FORBIDDEN` with the advisor and its scope, read from source.
4. **`fieldErrors[].field` semantics** — `error_mapping_extractor.find_field_error_semantics`: per
   `@ExceptionHandler` chunk, the expression passed to `.field(...)` verbatim, its exceptions, HTTP
   status and code, and a mechanical reading (a null-test ternary is spelled out as its branches).
   Rendered once in `index.md` under the Common Response Envelope.
5. **Unregistered contract ids** — `contract_extractor.claimed_ids`: for an undeclared endpoint, ids of
   the registry's own prefix that the controller's method Javadoc (else class Javadoc) names and no
   registry row carries. One candidate → `source claims`; several → `source mentions … (ambiguous)`;
   the row keeps "not registered" and says registering is the API REGISTRY's to do. Result: SEC logout
   → `API-SEC-028`; FIN fiscal-periods/search → `API-FIN-033`, dimensions/values/{id}/deactivate →
   `API-FIN-035`. The other four FIN undeclared endpoints get no claim because their controllers'
   class Javadoc cross-references several unregistered ids (034/035/037 together) — listed as nothing
   rather than guessed.
6. **Uniqueness invariants** — new `extractors/constraint_extractor.py` + `discovery.find_migration_roots`
   (from `spring.flyway.locations`). Entities' `@Table(uniqueConstraints)` merged with a version-ordered
   replay of the migrations (`ADD CONSTRAINT … UNIQUE`, `CREATE UNIQUE INDEX … WHERE`, inline `UNIQUE`,
   `DROP CONSTRAINT/INDEX/TABLE`), only for tables the module's entities declare; the WHERE clause is
   quoted verbatim; a column set that differs between entity and migration is reported, not resolved.
   FIN publishes 8 rows including `UQ_FIN_ACCOUNT_RETAINED_EARNINGS … WHERE is_retained_earnings_fl`.

Item 7 not done, as instructed.

## The six per-module diffs, every changed line classified

Baseline → after-1 (item 1): CU 29, NOTIF 71, FILE 64, SEC 8, MDL 0, FIN 240 changed lines. Every line
is one of: `**Required permission(s)**: … (found on service:X)`, a `403 FORBIDDEN` row, an "Other
Possible Responses" header/intro/rule it created, or an `Authorization: no @PreAuthorize/@Secured
declared — checked …` line (SEC 4, FILE 1). **Intended fix, item 1. No churn, no regression** (MDL 0).

after-1 → after-2 (item 2): CU 226, NOTIF 464, FILE 298, SEC 1546, MDL 472, FIN 1952 changed lines.
Sorted-line comparison of every endpoint file is identical before and after: a pure reorder of
endpoints (by path, verb) and groups (by name), the 10 Pagination Envelope rows now sorted, catalog
rows reordered, and the illustrative id in Contract Traceability following the first sorted endpoint.
**Intended fix, item 2 — a one-time reorder; a second run is byte-identical.**

after-2 → after-3 (item 3): business-response headings/intros/rows per endpoint (CU 6, NOTIF 16,
FILE 22, SEC 58, MDL 11, FIN 136 rows), `None reached — walked …` lines (CU 1, NOTIF 3, FILE 2, SEC 5,
MDL 2), Known Error Codes rows gaining the "Raised by" column (all modules), and SEC 30 / FIN 38 `403`
rows changed from `ACCESS_DENIED` to the advisor's module code. **Intended fix, item 3.**

after-3 → after-4 (item 4): each module's index gains the same 3-row fieldErrors table + 2 text
lines. **Intended fix, item 4.**

after-4 → after-5 (item 5): SEC 1 and FIN 2 drift rows gain a source claim; nothing else.
**Intended fix, item 5.**

after-5 → after-6 (item 6): Uniqueness Invariants section: CU 1, NOTIF 2, FILE 1, SEC 10, MDL 2, FIN 8
rows. **Intended fix, item 6.**

No changed line in any step was churn or a regression. Final render regenerated twice from two
processes: identical for all six; real docs regenerated with `--function update` (38 files in the
shared submodule) and equal to the final render.

## `--function check` verdicts (exit 2 on FAIL; all six exit 2)

| Module | FAIL | Everything else |
|---|---|---|
| CU | `contract-drift` (1 undeclared, 1 unimplemented API-CU-002), `auth-determined` | PASS |
| NOTIF | `contract-ids` (registry heading, 0 parseable rows — prose), `auth-determined` | PASS |
| FILE | `contract-ids` (same), `auth-determined` | PASS |
| SEC | `contract-drift` (7 undeclared), `auth-determined` | PASS |
| MDL | `auth-determined` | PASS |
| FIN | `contract-drift` (6 undeclared), `auth-determined` | PASS |

All are genuine outstanding problems, not tool failures, and none was waived: `auth-determined` is the
backend's missing `SecurityScheme` (a SecurityFilterChain is declared in source, no endpoint carries an
OpenAPI security requirement); the drift and prose registries are the factory's. `deterministic`,
`stale`, `permissions`, `envelope`, `error-codes`, `status-table`, `business-errors`,
`field-error-semantics`, `unique-constraints` pass on all six. Waivers: `waivers/<MOD>.json` with
check/reason/by/on; an invalid waiver waives nothing and is itself a FAIL. No waiver file exists.

## Tests added (37, stdlib unittest, `python3 -m unittest discover -s tests -t .`)

- `test_security_extractor.py` + `fixtures/spellings/SpellingsService.java`: single-line, two-part,
  three-line (behind a multi-line `@Operation`), trailing-`+`, no-`PERM_`-prefix (`CONFIG_CREATE`),
  `isAuthenticated()` (rule, not permission), `value =` + `hasRole`, `@Secured` array, a spliced
  constant, an annotation inside a string literal, and the four lookup outcomes. Would have caught the
  original blackout and the three-line variant that the first patch also missed.
- `test_determinism.py`: shuffled schema/path/method order and shuffled Page properties render
  identically; DTO declaration order preserved; endpoint sort order. Would have caught the Page<T>
  churn.
- `test_business_errors.py` + `fixtures/walk/`: static domain call, second service, `orElseThrow`
  lambda, private helper, chained static factory, local-variable receiver, constructor, detail codes
  with status, depth limit (explicit), ambiguous overloads left unresolved, comment text not a throw
  site, advisor override with scope. Would have caught mis-attribution and the constructor naming.
- `test_error_mapping.py` + `fixtures/common*/`: `statusMappings.put` and the `Status` enum
  constructor; `.code("LITERAL")` and `.code(CommonErrorCodes.CONSTANT)`; the three fieldErrors rows.
- `test_constraints.py` + `fixtures/entities`, `fixtures/migrations`: entity+migration merge, partial
  index WHERE verbatim, entity/migration column disagreement reported, dropped constraint and dropped
  table not live, numeric version order (V10 after V2), foreign tables ignored.

## What contradicts the report or the prompt

- The annotation collector was not paren-balanced across lines; that, not `PREAUTH_RE` alone, was the
  cause. Fixing only the regex as the report proposed would have fixed nothing.
- SEC's 27/34 needed no parser fix: 3 `isAuthenticated()` + 4 public endpoints.
- The published "403 ACCESS_DENIED" rows for SEC (and what FIN would have published) were wrong:
  both modules' advisors re-raise a module code. Neither document saw this.
- A depth-3 walk (the report's proposal) leaves reachable codes unbound in FIN; depth 8 is needed.
- Item 5's ids are mostly in **class** Javadoc that cross-references several unregistered ids, so
  only 3 of the 13 undeclared endpoints across SEC/FIN can be signposted without guessing.
- The prompt's "land each one separately" cannot be honoured without commits; each item is instead
  isolated by its own regeneration snapshot and diff above.

## Remedies found and deliberately not applied

Backend:
- `com/erp/main/config/OpenApiConfig.java`: declare a bearer `SecurityScheme` + global
  `SecurityRequirement`; fixes `auth-determined` and the missing 401 row for every module at once.
- `EventTypeRuleRepository` Javadoc says "one active rule per event type"; the constraint and
  `existsByEventTypeCode` are unconditional. Documentation defect.
- `AccountResponse.isRetainedEarningsFl` `@Schema`: add who sets it (V34 seed, no API writer).
- `FileService.softDelete` (DELETE /api/v1/files/{id}) carries no `@PreAuthorize` while every other
  FILE mutation does — confirm intentional.

Factory (read-only here):
- FIN: register `API-FIN-033/034/035/036/037` + fiscal-years/search (6 rows); SEC: 7 undeclared
  endpoints incl. logout (`API-SEC-028` claimed in source); CU: `/configurations/search` undeclared
  and `API-CU-002 GET /configurations` unimplemented.
- NOTIF and FILE plans: write the API REGISTRY as a verb+path table.
- `profile-summary.json`: publish the api-docs path (`discovery.default_output_dir` still infers it).

## Working tree

Changed by this session and still uncommitted (16 files, +562/−108) plus new: `checks.py`,
`tests/test_constraints.py`, `tests/test_error_mapping.py`, `tests/fixtures/{common,common_legacy,
entities,migrations,spellings}/`; `.claude/commands/generate-api-docs.md` STEP 3 rewritten; README
updated; the six modules' api-docs regenerated in `governance/shared` (branch `main`, 38 modified
files, not committed or pushed). Commit `75d01fa` ("Refactor code structure…", 18:10) — not made by
this session — captured a mid-task snapshot of the generator work along with unrelated Oracle/seed
files; the finished state is the working tree on top of it. `sync.py` went CRLF→LF in that snapshot
because the docstring patch rewrote the file; it was the only CRLF file in the tool.

## Round 2 (same day, on "fix everything") — the remedies applied

Backend, this repo:
- `OpenApiConfig.erpOpenApi()` declares the `bearerAuth` scheme (HTTP bearer, JWT) and applies it
  globally; the four public auth endpoints (login, signup, password-reset request/complete — exactly
  `SecurityConfig`'s `permitAll` POSTs) carry an empty `@SecurityRequirements`. Served document now
  has `securitySchemes.bearerAuth`, global `security`, and `security: []` on the four. Every endpoint
  page reads `Authentication: Required (bearerAuth).` or `Not required.`; `check` passes
  `auth-determined` on all six modules.
- `FileService.softDelete` (DELETE /api/v1/files/{id}) gained the `@PreAuthorize` its SEC-PENDING TODO
  described: PERM_FILE_BROWSER_UPDATE when `#action == 'ARCHIVE'`, PERM_FILE_BROWSER_DELETE when
  `'DELETE'`, any other action passes the gate so the existing 400 still answers it. The two
  constants were added to `PermissionConstants` (values identical to the V9/V31 rows, which already
  grant them). Probed live after the restart: admin → 404 FILE_DOCUMENT_NOT_FOUND for both actions,
  `action=FOO` → 400 FILE_DOCUMENT_INVALID_TRANSITION, no token → 401 SEC-401-INVALID-CREDENTIALS.
  FILE's execution-state records it (IMPLEMENTED).
- `EventTypeRuleRepository` Javadoc corrected ("one rule per event type, active or not");
  `AccountResponse.isRetainedEarningsFl` `@Schema` now says who sets it (V34 seed, no API writer).
- Jar rebuilt (`mvn -DskipTests package`, ArchUnit boundary test green) and the dev server
  restarted from `.env`; log at `logs/app-restart-2026-09-23.log`.

Generator, two more derivations:
- The 401 row: `error_mapping_extractor.find_auth_entry_point` resolves the class `SecurityConfig`
  wires as `authenticationEntryPoint` through its field type and reads the code its `commence(...)`
  writes beside a 401 (`SEC_401_INVALID_CREDENTIALS`), so every authenticated endpoint now carries a
  cited 401 row and SEC's catalog binds the code to 30 endpoints via the handler. Fixture + tests.
- Comments are masked before any controller/service regex runs: a `// public: …` comment between
  `@PostMapping` and the method had made the route matcher read `Operation` as the method name.
  Regression test added. 40 tests, all green.

Governance channel: NOTIF and FILE `execution-state.json` gained an OPEN `api_doc_gaps` entry for
their prose API REGISTRY (the `contract-ids` FAIL); CU/SEC/FIN drift and the profile api-docs path
were already recorded there.

`check` after round 2: MDL PASS; CU/SEC/FIN FAIL only `contract-drift`; NOTIF/FILE FAIL only
`contract-ids`. Every remaining FAIL is the factory's, filed, and left failing on purpose.

Concurrent work noticed, not mine: another session is editing FIN (`FinErrorCodes` gained
`FIN_409_MAPPING_DUP`, new `FinPersistenceRefusalAdvisor.java`, both i18n bundles) and committed
snapshots of this session's tree along the way (latest: `bf9dc12 Add FIN v2 configuration delta seed script`); the regenerated docs
reflect the FIN source as it stood at regeneration time.

