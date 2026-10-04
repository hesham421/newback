# api-doc-generator: the FIN deficiencies, their causes, and what to change

Analysis of `governance/governance-tools/api-doc-generator/` against six verified deficiencies in
`governance/shared/backend/modules/FIN/api-docs/`, plus one this review found on its own. Read-only.

## Verdict per item, ranked

Value = what a consuming client (frontend, test author, `api-verify`) can do that it cannot today.

| # | Item | Owner | Reachable from | Cost | Verdict |
|---|---|---|---|---|---|
| 0 | **All 38 FIN endpoints publish no permission and no 403** — `PREAUTH_RE` cannot parse a concatenated `@PreAuthorize`, which is how every FIN service method spells it | generator | Java source, already read | ~6 lines + fixture test | **Do first.** One regex blanking 38/38 |
| 6 | Non-deterministic `Page<T>` row order | generator | already in hand | ~12 lines + a `check` mode | **Do second.** Cheapest way to make the diff mean something |
| 1 | Business 409s not bound to the endpoint that raises them | generator | controller → service → domain throw sites | ~130 lines, new extractor reusing `security_extractor`'s delegate walk | **Do third.** Highest client value of the six |
| 3 | `fieldErrors[].field` semantics undocumented | generator | `GlobalExceptionHandler`, already parsed per handler | ~45 lines | **Do fourth.** Kills a class of wasted client work |
| 4 | Six served endpoints carry no contract id | **factory** (+ a 25-line signpost) | the plan's API REGISTRY (rows missing); the id is in controller Javadoc | factory: 6 table rows | **Register the ids.** Generator change is a signpost only |
| 2 | Uniqueness invariants published nowhere | generator | `@Table(uniqueConstraints)` **and** Flyway, incl. partial indexes | ~90 lines, new extractor | **Do, but last.** The invariant as stated to me is also wrong |
| 5 | Response-only field with no statement of who writes it | **backend** | field-set: yes. The writer: only by scanning migrations | backend: 1 line | **Do not build the inference.** Ship the structural label at most |

First pick: item 0. Do-not-do pick: item 5's "find the migration that writes this column" extractor —
it is a search for a guessed snake_case name across `db/migration/*.sql` that must also tell a seed from
a schema change, and a wrong citation in an api-doc is worse than the silence it replaces.

## What in the framing is wrong

- **Item 2's invariant is not what the backend enforces.** V22 declares
  `UQ_FIN_EVENT_TYPE_RULE_CODE UNIQUE (event_type_code)` — one rule per event type, active or not —
  and `EventTypeRuleService.create` gates on `existsByEventTypeCode`, which takes no active flag. So
  deactivating a rule does **not** free its event type; a precondition of "no *active* rule" is looser
  than reality and still fails. `EventTypeRuleRepository`'s own Javadoc says "one active rule per event
  type" and is wrong — a backend documentation defect worth fixing on its own.
- **Item 2 is trivially reachable, not doubtful.** `EventTypeRule` declares that constraint in its own
  `@Table(uniqueConstraints = {@UniqueConstraint(name = "UQ_FIN_EVENT_TYPE_RULE_CODE", …)})`.
  `UQ_FIN_ACCOUNT_RETAINED_EARNINGS` is the exception that needs the Flyway reader, not the rule.
- **Item 5's misleading sentence is the backend's, verbatim** — "read-only, never settable through the
  account APIs" is copied from `AccountResponse`'s `@Schema`. The wording is the defect, not the copy.
- **Item 4's ids are in the Java source** — Javadoc on the exact methods (`API-FIN-034` on
  `EventTypeRuleService.deactivate`, `API-FIN-033` on `FiscalPeriodSearchRequest`, `API-FIN-036/037` on
  two controllers). The served code knows its id; the registry does not.

## Item 0 — the permission and 403 blackout

`extractors/security_extractor.py:42` is `@(?:PreAuthorize|Secured)\s*\(\s*"([^"]*)"\s*\)`. FIN writes
all 59 of its annotations as a concatenation:

```java
@PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
    + ".PERM_FIN_RULES_CREATE)")
```

`[^"]*` stops at the first closing quote, so nothing matches, `resolve_permission` returns
`([], None, None)`, and `attach_endpoint_error_codes`'s `if ep.permission or ep.permission_expression`
never fires — so the 403 row is suppressed too. FIN publishes zero `Required permission(s)` lines across
all nine group files; SEC and MDL (single-line spellings) publish theirs. This is the real cause of the
symptom the slash command attributes to under-annotation.

Change, in `security_extractor.py` only — match the argument list and join its string literals:

```python
PREAUTH_ARGS_RE = re.compile(r'@(?:PreAuthorize|Secured)\s*\((.*?)\)\s*$', re.DOTALL)
_STRING_LITERAL_RE = re.compile(r'"([^"]*)"')
# joined: "hasAuthority(T(...)" + ".PERM_FIN_RULES_CREATE)" -> "hasAuthority(T(...).PERM_FIN_RULES_CREATE)"
```

`_collect_annotations_above` already returns the annotation paren-balanced, so only `find_preauthorize`
changes. Add a fixture test carrying both spellings.

`endpoints/fin-event-rule-management.md` today carries only `Not determined from the OpenAPI document.`
and a lone 400 row. After the fix, two lines appear that no FIN endpoint has ever published (the auth
line stays blocked by the backend gap below):

```
**Required permission(s)**: PERM_FIN_RULES_CREATE (found on service:EventTypeRuleService)
...
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint … |
```

Related backend gap, cheap: `com/erp/main/config/OpenApiConfig.java` declares no `SecurityScheme` and no
global `SecurityRequirement`, so `_operation_auth` returns `None`, all 38 endpoints read "Not determined",
`index.md` has no Authentication section, and no 401 row is attached anywhere. Six lines there fix every
module at once. Generator side: nothing.

## Item 1 — business 409s bound to their endpoint

Cause, `error_mapping_extractor.py:188`: `find_business_status_associations` walks every `*.java` and
records `{code: Status}`, **discarding the file the throw was in**. The result is the module-wide catalog
in `index.md` (where both FIN 409s do appear, at `409 CONFLICT`) and nothing endpoint-scoped.
`attach_endpoint_error_codes` then attaches only its three framework rules. Nothing ever asks which
endpoint reaches a throw.

Reachable, and the walk already exists: `find_controller_for_endpoint` maps an endpoint to a controller
method, `find_delegate` + `_method_body_span` resolve the service class and method it delegates to. For
`POST /api/v1/fin/event-rules`: `EventTypeRuleController.create` → `EventTypeRuleService.create` →
`EventTypeRuleDomain.create` → `throw new LocalizedException(Status.ALREADY_EXISTS,
FinErrorCodes.FIN_409_RULE_DUP, …)` — two hops past the controller, all in the module's own source, all
matched by the existing `THROW_RE`.

Proposal: `business_error_extractor.py` (~130 lines), run when `source_root` is present. Index the
module's source once as `{ClassName: {method: body}}` by brace balance (reusing `_method_body_span`);
per endpoint start at the controller method and follow calls to depth 3, only where the callee class or
`private final` field type exists in this module's own source — never into `com.erp.common`, never a
repository, never a lambda captured elsewhere; run `THROW_RE` in each visited body, recording
`(code, Status, "Class.method")`; emit `Endpoint.business_errors` with the HTTP status resolved through
the existing shared `Status` table. Render above the existing block, so its "structural, not business"
disclaimer keeps its meaning:

```
### Business Responses

Raised by this endpoint's own rules. Each row cites the throw site it was read from.

| HTTP Status | Code | Constant | Throw site |
|---|---|---|---|
| 409 CONFLICT | FIN-409-RULE-DUP | FIN_409_RULE_DUP | EventTypeRuleDomain.create |
```

Truthfulness boundary: a code reached only past the depth cut-off or through an ambiguous delegate is
omitted, exactly as `_find_delegate` already omits an ambiguous call — and the walk publishes its own
coverage, so a failing walk is not indistinguishable from a module with no business rules. No backend
convention needed: a `@ThrowsBusinessError` annotation would be cheaper to parse but would block the fix
on 59 controller edits for information already reachable.

## Item 3 — what `fieldErrors[].field` actually carries

Cause: `response_model_extractor.find_envelope` describes the envelope from the OpenAPI schema alone, and
`FieldErrorItem` is a two-field Lombok class with no `@Schema`, so both rows render with an empty
description. Nothing wrong; nothing said.

The semantics sit in source the tool already opens: the handler line is
`.field(detail.field() != null ? detail.field() : detail.errorCode())`, and `ErrorDetail`'s Javadoc states
the rule outright — when `field` is null, `GlobalExceptionHandler` reports `errorCode` in that slot.
Proposal: `find_field_error_semantics(common_source_roots)` in `error_mapping_extractor` (~45 lines) — it
already splits the file per `@ExceptionHandler`. Capture the expression passed to `.field(...)` and
classify it structurally, inferring nothing beyond which expression it is. Render once in `index.md`,
below the envelope table whose `field` / `message` rows today have an empty Description column:

```
### What `error.fieldErrors[].field` carries

Read from GlobalExceptionHandler.java. It is NOT always a form field path.

| Failure kind | `field` holds | Source expression |
|---|---|---|
| Bean-validation rejection (400 VALIDATION_ERROR) | the request field path | `fe.getField()` |
| Missing or mistyped request parameter (400) | the parameter name | `parameterName` |
| Business multi-error (LocalizedException) | the request field when the throw named one, **otherwise the error code itself** | `detail.field() != null ? detail.field() : detail.errorCode()` |

A business refusal therefore carries no line index and no form path unless its throw site supplied one.
```

That last line is the finding the frontend proved by probe. It is derived, not guessed, and stable across
modules because the handler is shared.

## Item 6 — determinism

Cause: `dto_extractor.find_page_envelope` iterates `schemas.items()` to pick the first Page-shaped schema,
then `props.items()` for its rows. Both orders are springdoc's, whose order for Spring Data's
`Pageable`/`Sort` varies between JVM runs. `sync.py`'s docstring asserts rendering is deterministic; that
assertion is false, and section diffing and reviewer attention both rest on it.

Fix, both in `find_page_envelope`: `sorted(schemas.items())` to pick the schema, and sort the bookkeeping
rows by name — so `empty, first, last, number, numberOfElements, pageable, size, sort, totalElements,
totalPages`, every run. Scope it to this one machine-generated shared table; do **not** sort per-endpoint
DTO tables, where declaration order matches the Java class a reader will open. Then prove it instead of
asserting it: `build_document()` twice in one run, compare the rendered dicts, fail on a difference.

## Item 4 — the six unregistered ids

The generator already does its job: `contract_extractor` reports all six as `undeclared` in the published
drift table, which is the list the frontend needed. It cannot mint an id — ids live in the plan's API
REGISTRY, in the factory's read-only analysis tree. **Factory defect: add six rows to
`backend-execution-plan-fin.md`.** A generator that invented `API-FIN-038` would be inventing contract.
What it can honestly add (~25 lines in `contract_extractor`): when an undeclared endpoint's controller or
service Javadoc mentions an `API-<MOD>-NNN`, quote it as a claim with its source, so nobody has to write
`PROVISIONAL id` in a source comment.

```
| undeclared | — (source claims `API-FIN-034`, EventTypeRuleController Javadoc) | PUT | `/api/v1/fin/event-rules/{id}/deactivate` | implemented but absent from the API REGISTRY — the id above is a source comment, not a registered contract |
```

The row must keep both "claims" and "not registered". It is a signpost, never a substitute.

## Item 2 — uniqueness invariants

Nothing in the tool reads entities or migrations, so no invariant of any kind is published. Both sources are
reachable and neither needs a new backend convention: `@Table(uniqueConstraints = …)` on the module's
entities, and `db/migration/*.sql` for both `ADD CONSTRAINT … UNIQUE (…)` and `CREATE UNIQUE INDEX … WHERE …`.
The partial index is the case JPA cannot express, and V23 says so in its own comment.

Proposal: `constraint_extractor.py` (~90 lines) plus one `index.md` section. Map constraint columns to
documented field names via the entity's explicit `@Column(name = …)`, not a guessed case conversion, and
cite the source file per row; an unmappable constraint is still listed with raw column names.

```
## Uniqueness Invariants

Enforced by the database. A request violating one is refused.

| Table | Columns | Scope | Name | Source |
|---|---|---|---|---|
| FIN_EVENT_TYPE_RULE | eventTypeCode | all rows, active or not | UQ_FIN_EVENT_TYPE_RULE_CODE | EventTypeRule.java, V22__fin_schema.sql |
| FIN_ACCOUNT | isRetainedEarningsFl | only rows where the flag is true (partial index) | UQ_FIN_ACCOUNT_RETAINED_EARNINGS | V23__fin_account_retained_earnings_flag.sql |
```

The Scope column is what matters and what a JPA-only reader cannot produce. It is read from the `WHERE`
clause verbatim, never summarised.

## Item 5 — response-only fields

The generator holds the only honest half already: the field is in response schemas and in no request schema
of this module. Publishing that as a label costs ~20 lines in `markdown_renderer` — append
`response-only — not present in any request schema in this module` to the description. Who writes it is not
reachable without guessing. The real fix is one sentence in `AccountResponse`: *"set by data seed
(V34__fin_account_retained_earnings_seed.sql); no API writer, by decision"*. The generator would publish it
verbatim, as it already publishes the misleading half. Minimal convention worth adopting project-wide: a
field whose `@Schema` says what cannot be done must also say what does it.

## The regression guard

`review` reports and always exits 0 — `generate.py` returns 1 only when a load or parse throws.
`Contract ids: 32/38 … 6 DRIFT` was printed, correct, and read by nobody in time. Counting is not guarding.
Add `--function check`: everything `review` does, plus a non-zero exit naming module and condition, on any of:

1. **Drift** — any `contract_drift` row. Six unregistered ids should have failed FIN's gate.
2. **Staleness** — any rendered file differing from disk, i.e. not regenerated after a backend change.
3. **Non-determinism** — two `build_document()` runs rendering differently.
4. **Silent-empty assertions**, the class that produced items 0 and 1 — a whole-module emptiness that one
   module has already proved illegitimate: no response envelope; no `Required permission(s)` anywhere while
   the source contains `@PreAuthorize`; every endpoint `requires_auth is None`; zero error codes; a wholly
   blank Status column. Report as a ratio: `permissions: 0/38 endpoints, 59 @PreAuthorize in source → FAIL`.
5. **Walk coverage**, once item 1 lands: `business errors resolved for 0 of 38 endpoints` must fail.

Waiving is explicit, per module, in a file the tool reads, with a reason — never by deleting an assertion.
Unit-test the extractors against fixture Java holding **both** spellings of every construct that has
changed shape here: `@PreAuthorize` single-line and concatenated, `statusMappings.put` and the `Status`
enum constructor, `.code("LITERAL")` and `.code(CommonErrorCodes.CONSTANT)`. Each alternative is recorded
in a code comment as a bug that already shipped once; not one has a test.

## What the slash command contributed

`.claude/commands/generate-api-docs.md` STEP 3 lists exactly the two symptoms FIN exhibits — good
instruction design — and then disarms both.

- *"No `Required permission(s)` anywhere → `@PreAuthorize` constants not resolved from source"* states the
  observation as if the source were at fault. It is the generator's regex, so a reader following this table
  files a backend gap and closes the loop on a generator bug.
- *"Every endpoint says Authentication: Not determined → a known backend gap, not a generator failure"* is
  factually correct and operationally harmful. "Known" reads as "expected", so a condition covering 38/38
  endpoints becomes a line in a report instead of six lines in `OpenApiConfig`. The row should carry the
  remedy and an owner, not an absolution.

Both want the same rewrite: name the candidate causes on **both** sides and require a filed item with an
owner. And none of it should live only in a human-facing instruction — a symptom a human is asked to notice
every run is a check the tool should fail on.
