# Generate Backend API Docs

```
Lives at   : .claude/commands/generate-api-docs.md, so it auto-loads as a
             Claude Code slash command
Runs       : docs/api-docs/_tools/generate_all.py (the driver) over
             governance/tools/api-doc-generator/generate.py (the pipeline)
Writes to  : docs/api-docs/<module>/  — the repository's single API contract
```

(Re)generates the API documentation of every module from the **running
reference application**, so the frontend and the `api-verify` skill read
documentation that matches the implementation instead of a stale copy.

$ARGUMENTS = optional: a single module to report on (`sec`, `tenant`, `file`,
`notif`, `mdl`, `cu`, `sequence`, `audit`, `report`, `app`). The generation
itself is always whole-app: one aggregate OpenAPI document, every module.

## What the module list is

The live modules of the running app, and nothing else: the `MODULES` table in
`docs/api-docs/_tools/generate_all.py` (one entry per `com.erp.<module>`
controller package in `erp-core`, plus `app` for `erp-app-reference`), mirrored
by the table in `docs/api-docs/README.md`. There is no registry file to consult.
A new module is added to that table when its controllers land — never
guessed from a folder name.

## Preconditions

**Backend running:** `erp-app-reference` must be up (profile `dev`) and
`GET /v3/api-docs` must answer. The default base URL is
`http://localhost:7272` (`server.port` in
`erp-app-reference/src/main/resources/application.yml`); pass `--base` to the
driver for another port and `--server-url http://localhost:7272` so the
published server URL stays the documented one.

**Python 3.10+** with no extra packages (`python docs/api-docs/_tools/...`).

## Your Task

### STEP 1 — Review first, always

```bash
python docs/api-docs/_tools/generate_all.py --function review
```

`review` writes nothing. Read its report before changing any file, and report
per module: endpoints added/removed/updated/unchanged, which shared `index.md`
sections changed, and any conflicts.

### STEP 2 — Then write, picking the mode from what review reported

- **Conflicts reported** (`unmanaged file already exists`) → STOP and show the
  list. Those are hand-written or hand-edited files; the tool refuses to
  clobber them. Ask before overwriting anything.
- **A module folder has no docs yet, or every file reported as added/unmanaged**
  → `--function generate` (full write, stamps every file with the
  AUTO-GENERATED marker).
- **Docs exist and carry the marker** → `--function update` (writes only what
  changed, deletes endpoint files for endpoints the backend no longer has,
  leaves everything else alone).

### STEP 3 — Run the guards, and file every FAIL with an owner

```bash
python docs/api-docs/_tools/generate_all.py --function check
python docs/api-docs/_tools/check_completeness.py
```

`check` prints everything `review` prints, then one line per assertion per
module, each with its **expected side read from source** and a ratio — never
a bare count — and exits non-zero on any FAIL. `check_completeness.py` then
asserts that every operation the app serves is documented in exactly one
module folder and that no folder documents an operation the app no longer
serves. Report both blocks verbatim, then for each FAIL name the owner and the
item you filed. A FAIL is a correct result when the problem is real; never
satisfy it by weakening an assertion or by hand-editing a generated file. A
module that legitimately cannot pass one assertion declares that once, with a
reason, in `governance/tools/api-doc-generator/waivers/<MODULE>.json` (see the
tool's README) — that is the only sanctioned way to silence one.

What each FAIL means, and who can own the fix. Every row lists causes on
**both** sides, because the generator and the backend have each produced every
one of these symptoms at least once — a symptom is a question, not a verdict:

| FAIL | Generator-side cause to rule out | Backend-side cause to rule out | File the item with |
|---|---|---|---|
| `permissions` — 0/N while source has `@PreAuthorize` | the annotation parser misses a spelling — add the spelling to `tests/fixtures/spellings/` first | the controller delegates to two services, or the service source is outside the module root | generator owner, unless the row says `not extracted: DELEGATE_UNRESOLVED` for a controller that genuinely calls two services |
| `permissions` — `not extracted` > 0 | route or delegate matcher (`security_extractor`) cannot follow this controller's shape | a controller method that is not a thin delegate | generator owner first |
| `auth-determined` — 0/N with a `SecurityFilterChain` in source | none: the tool reads `security` from the OpenAPI document and there is none to read | the global bearer scheme in the OpenAPI configuration was removed or stopped applying, so springdoc emits no `security` | backend owner; waive per module only with the backend item's id in the reason |
| `envelope` — no envelope while `ApiResponse` exists | `is_envelope_shape` no longer matches the wrapper's key set | `ApiResponse<T>` lost the `success`/`data` pair, or a controller returns something else | whichever side changed last |
| `status-table` / `error-codes` — 0 read while declared | a third spelling of the table or the constants class | the class was renamed or moved out of the shared root | generator owner first |
| `business-errors` — 0/N bound while source throws | the walk cannot resolve this module's delegate shape (`business_error_extractor`) | services throw only from code the controllers never reach | generator owner first; the `unbound` list names the codes to explain |
| `contract-ids` / `contract-drift` | — | — | not applicable here: no execution plan is read, so no contract ids are stamped (`docs/api-docs/README.md`) |
| `stale` | — | the backend changed after the last regeneration | run `--function update`, then STEP 4 |
| `deterministic` | a new extractor iterates a mapping or directory listing unsorted | — | generator owner |

Then give the endpoint count per module and in total, the error-code count,
and the per-endpoint change list. If a section is EMPTY and `check` did not
fail on it, say which assertion covered it and why it passed (a legitimately
absent feature is stated as such in the docs themselves — e.g. an endpoint's
`Authorization: no @PreAuthorize/@Secured declared — checked …` line).

### STEP 4 — Commit them (they are NOT published until you do)

The docs are ordinary tracked files of this repository. Update the
`Generated` row of `docs/api-docs/README.md` (date, source build) and the
operation counts if they changed, then commit `docs/api-docs/` together with
the code change that made the regeneration necessary:

```bash
git add docs/api-docs
git commit -m "docs(api-docs): regenerated from the running app"
```

A regeneration that stays uncommitted looks like success and delivers
nothing: the frontend reads the committed copy.

## Constraints (NON-NEGOTIABLE)

- **NEVER hand-edit a generated file under `docs/api-docs/<module>/`.** It is
  regenerated output; a manual edit is destroyed on the next run AND turns
  the file into a conflict the tool then refuses to manage. If the docs are
  wrong, either the backend is wrong or under-annotated, or the generator
  missed something — the STEP 3 table says how to tell the two apart; fix the
  right one and regenerate. (`docs/api-docs/README.md` is hand-maintained and
  is the one exception.)
- **NEVER pass `--openapi` / `--source` / `--common-source` / `--output` to
  `generate.py` to "make it work."** The driver already supplies every input
  from the repository; if the driver cannot assign an operation to exactly
  one module it says so — report that instead of working around it.
- **NEVER invent content for a section the generator left out.** Absent means
  "not discoverable from the implemented backend" — that is information.
- **NEVER fall back to a saved or older OpenAPI JSON.** If `/v3/api-docs`
  answers HTTP 500, that is a backend/springdoc fault, not a documentation
  fault: report it with the response body and stop. Docs that look current
  but aren't are worse than no docs.

## Notes

- Output always lands in `docs/api-docs/<module>/` (`index.md` +
  `endpoints/<tag-slug>.md`). The driver derives the module of every operation
  from the controller source that declares its verb+route (with a single-owner
  `@Tag` fallback), so springdoc groups play no part.
- `review` is safe to run any time, including in CI, to answer "have the API
  docs drifted from the backend?" without touching a file. `check` plus
  `check_completeness.py` is the gate: the same, plus a non-zero exit on drift,
  staleness, non-determinism, a whole-module silent-empty section, or an
  operation that is undocumented or documented twice.
- Consumers of this output: the frontend repository and the `api-verify`
  skill (`governance/rules/api-verify-config.md` §1 lists api-docs as its
  **mandatory** input and says to regenerate rather than trust a stale copy).
- Single-module runs with `governance/tools/api-doc-generator/generate.py
  --module <X>` still work where a springdoc group maps 1:1 to the module, but
  lose the i18n message columns and the unique-constraint section the driver
  supplies — prefer the driver.
