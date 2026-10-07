# Generate Backend API Docs

```
Lives at   : backend/.claude/commands/generate-api-docs.md, so it
             auto-loads as a Claude Code slash command
Runs       : erp-app-reference/governance/governance-tools/api-doc-generator/generate.py
Writes to  : governance/backend/modules/<MOD>/api-docs/  (this track's own
             partition of the in-repo governance tree)
```

(Re)generates a module's API documentation from the **running backend**, so
the frontend and the `api-verify` skill read documentation that matches the
implementation instead of a stale hand-written copy.

$ARGUMENTS = MODULE

The module code is the only input. Everything else — which springdoc group the
module is, the OpenAPI URL, the module's source root, the shared
`com.erp.common` root, the output directory — is auto-discovered from the
repository itself. Do NOT pass override flags unless discovery actually fails
and names the flag to pass.

> **erp-core modules:** the springdoc groups of the erp-core build do not map 1:1
> to modules, so for SEC, TENANT, FILE, NOTIF, MDL, CU, SEQUENCE, AUDIT and REPORT
> the working pipeline is `python docs/api-docs/_tools/generate_all.py` (same
> generator, hand-built context — see `docs/api-docs/README.md`). It writes
> `docs/api-docs/<mod>/` and mirrors each module into
> `governance/backend/modules/<MOD>/api-docs/`; then continue at STEP 4.

## Step 0 — where governance lives

```bash
GOV=governance                                # the project's governance tree — a plain folder in this repo
MODULES=$GOV/analysis/modules                 # every module's analysis — read-only here
GOVROOT=$GOV/analysis/platform                # project-registry.md and the platform artifacts
PART=$GOV/backend/modules/{MOD}               # this track's own partition ({MOD} unexpanded): execution-state.json, api-docs/, test-api/
PKGS=$GOV/backend/modules/{MOD}/packages      # the backend packages ({MOD} unexpanded) — read here, never rewritten
```

Bare `packages/…` paths below resolve under `$PKGS` with `{MOD}` expanded; `execution-state.json`
and `api-docs/` under `$PART` with `{MOD}` expanded; stage artifacts and `manifest.json` under
`$MODULES/{MODULE}/`. A version-suffixed base (`vN/`) applies to each of the three the same way —
with ONE exception: `api-docs/` is never version-suffixed. It is derived from the running
application, so there is one current set per module, not one per plan version.

`$MODULES`, `$GOVROOT`, `$PART` and `$PKGS` below are those values.
**`$MODULES` is the read-only analysis tree; api-docs are written under `$PART`,
never under `$MODULES`** — the two differ (`analysis/modules/<MOD>` vs
`backend/modules/<MOD>`) and confusing them writes into the owner's analysis
tree. Nothing refuses it for you.

## Preconditions

**Module validation:** confirm `$MODULES/$MODULE/` exists before
running anything. Resolve the code from `governance/modules-registry.json` or
`$GOVROOT/project-registry.md` — never invent one.

**Backend running:** the Spring Boot app must be up and `/v3/api-docs/<group>`
must answer for this module. The generator reads the real port from
`src/main/resources/application.properties` — never assume `8080`.

## Your Task

### STEP 1 — Review first, always

```bash
python3 erp-app-reference/governance/governance-tools/api-doc-generator/generate.py \
    --module $MODULE --function review
```

`review` writes nothing. Read its report before changing any file, and report:
endpoints added/removed/updated/unchanged, which shared `index.md` sections
changed, and any conflicts.

### STEP 2 — Then write, picking the mode from what review reported

- **Conflicts reported** (`unmanaged file already exists`) → STOP and show the
  list. Those are hand-written or hand-edited files; the tool refuses to
  clobber them. Ask before overwriting anything.
- **No `api-docs/` yet, or every file reported as added/unmanaged** →
  `--function generate` (full write, stamps every file with the
  AUTO-GENERATED marker).
- **Docs exist and carry the marker** → `--function update` (writes only what
  changed, deletes endpoint files for endpoints the backend no longer has,
  leaves everything else alone).

### STEP 3 — Run the guard, and file every FAIL with an owner

```bash
python3 erp-app-reference/governance/governance-tools/api-doc-generator/generate.py \
    --module $MODULE --function check
```

`check` prints everything `review` prints, then one line per assertion, each
with its **expected side read from source** and a ratio — never a bare count —
and exits non-zero on any FAIL. Report the block verbatim, then for each FAIL
name the owner and the item you filed. A FAIL is a correct result when the
problem is real; never satisfy it by weakening an assertion or by hand-editing
a generated file. A module that legitimately cannot pass one assertion declares
that once, with a reason, in
`erp-app-reference/governance/governance-tools/api-doc-generator/waivers/$MODULE.json`
(see the tool's README) — that is the only sanctioned way to silence one.

What each FAIL means, and who can own the fix. Every row lists causes on
**both** sides, because the generator and the backend have each produced every
one of these symptoms at least once — a symptom is a question, not a verdict:

| FAIL | Generator-side cause to rule out | Backend/analysis-side cause to rule out | File the item with |
|---|---|---|---|
| `permissions` — 0/N while source has `@PreAuthorize` | the annotation parser misses a spelling (this is how 4 modules published 0/N until 2026-09-23: a concatenated `@PreAuthorize` never matched) — add the spelling to `tests/fixtures/spellings/` first | the controller delegates to two services, or the service source is outside the module root | generator owner, unless the row says `not extracted: DELEGATE_UNRESOLVED` for a controller that genuinely calls two services |
| `permissions` — `not extracted` > 0 | route or delegate matcher (`security_extractor`) cannot follow this controller's shape | a controller method that is not a thin delegate | generator owner first |
| `auth-determined` — 0/N with a `SecurityFilterChain` in source | none: the tool reads `security` from the OpenAPI document and there is none to read | `OpenApiConfig.erpOpenApi()` (the global bearer scheme, added 2026-09-23) was removed or stopped applying, so springdoc emits no `security` and no endpoint can state that it needs a token | backend owner; waive per module only with the backend item's id in the reason |
| `envelope` — no envelope while `ApiResponse` exists | `is_envelope_shape` no longer matches the wrapper's key set | `ApiResponse<T>` lost the `success`/`data` pair, or a controller returns something else | whichever side changed last |
| `status-table` / `error-codes` — 0 read while declared | a third spelling of the table or the constants class | the class was renamed or moved out of the shared root | generator owner first |
| `business-errors` — 0/N bound while source throws | the walk cannot resolve this module's delegate shape (`business_error_extractor`) | services throw only from code the controllers never reach | generator owner first; the `unbound` list names the codes to explain |
| `contract-ids` — heading present, 0 rows | none: the tool needs a verb+path table | the plan writes its API REGISTRY as prose (NOTIF, FILE today) | analysis owner |
| `contract-drift` — any row | none: both sides are read verbatim | an endpoint the plan never registered, or a registered id nothing serves — the drift table quotes the id a controller Javadoc **claims**, which is a signpost for the registrar, not a registration | analysis owner (register or retire the id); backend owner if the served path is the mistake |
| `stale` | — | the backend changed after the last regeneration | run `--function update`, then STEP 4 |
| `deterministic` | a new extractor iterates a mapping or directory listing unsorted | — | generator owner |

Then give the endpoint count, group count, error-code count, output path and
the per-endpoint change list. If a section is EMPTY and `check` did not fail
on it, say which assertion covered it and why it passed (a legitimately absent
feature is stated as such in the docs themselves — e.g. an endpoint's
`Authorization: no @PreAuthorize/@Secured declared — checked …` line).

### STEP 4 — Commit them (they are NOT published until you do)

`governance/` is a plain folder in this repository, so the regenerated docs are
ordinary changes in this repo's working tree. The frontend (`newfront`) reads
them from its sibling checkout of this repo (`../newback/governance`), i.e. from
what is committed and pushed here — regenerating and stopping delivers nothing.

```bash
git status --short -- "governance/backend/modules/$MODULE/api-docs"
git add "governance/backend/modules/$MODULE/api-docs"
git commit -m "api-docs($MODULE): regenerated from the running app"
```

Push through the usual branch → review → merge flow of this repo.

## Constraints (NON-NEGOTIABLE)

- **NEVER hand-edit a generated file under `api-docs/`.** It is regenerated
  output; a manual edit is destroyed on the next run AND turns the file into a
  conflict the tool then refuses to manage. If the docs are wrong, either the
  backend is wrong or under-annotated, or the generator missed something — the
  STEP 3 table says how to tell the two apart; fix the right one and regenerate.
- **NEVER pass `--openapi` / `--source` / `--common-source` / `--output` to
  "make it work."** If discovery fails it names the exact flag and why; report
  that instead of working around it.
- **NEVER invent content for a section the generator left out.** Absent means
  "not discoverable from the implemented backend" — that is information.
- **NEVER fall back to a saved or older OpenAPI JSON.** If the generator
  reports HTTP 500 from `/v3/api-docs/<group>`, that is a backend/springdoc
  fault, not a documentation fault: report it with the response body and stop.
  Docs that look current but aren't are worse than no docs.

## Notes

- Output always lands in this track's own partition — `$PART` with `{MOD}`
  expanded, i.e. `governance/backend/modules/[MODULE]/api-docs/`
  (`index.md` + `endpoints/<group-slug>.md`). The path derives from the tool's
  own location (`<repo>/governance/backend/modules/<MOD>/api-docs`), so the
  command works from any working directory. The generator resolves the layout itself and **refuses**
  rather than guessing when two candidate directories exist — if it says so,
  report that; do not pick one for it.
- `review` is safe to run any time, including in CI, to answer "have the API
  docs drifted from the backend?" without touching a file. `check` is the CI
  gate: the same, plus a non-zero exit on drift, staleness, non-determinism or
  a whole-module silent-empty section (STEP 3).
- Consumers of this output: the frontend repo (`newfront`), which reads **this
  same single copy** from its sibling checkout of this repo
  (`$GOV_ROOT`, default `../newback/governance`), and the `api-verify` skill — see
  `governance/rules/api-verify-config.md` §1, which lists api-docs as its
  **mandatory** input and says to regenerate rather than trust a stale copy.
