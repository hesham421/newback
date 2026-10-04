# Generate Backend API Docs

```
Lives at   : backend/.claude/commands/generate-api-docs.md, so it
             auto-loads as a Claude Code slash command
Runs       : governance/governance-tools/api-doc-generator/generate.py
Writes to  : $PART/api-docs/  ({MOD} expanded — this track's own writable
             partition, today governance/shared/backend/modules/<MOD>/)
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

## Step 0 — resolve where governance lives (never type it)

```bash
SUMMARY=governance/shared/platform/profile-summary.json
test -f "$SUMMARY" || { echo "MISSING — git submodule update --init governance/shared"; exit 1; }
MODULES=governance/shared/$(jq -r .paths.modules "$SUMMARY")            # every module's analysis — read-only here
GOVROOT=governance/shared/$(jq -r .paths.platform "$SUMMARY")           # project-registry.md and the platform artifacts
PART=governance/shared/$(jq -r .tracks.backend.partition "$SUMMARY")    # this track's own partition ({MOD} unexpanded): execution-state.json, api-docs/, test-api/
PKGS=governance/shared/$(jq -r .tracks.backend.delivery "$SUMMARY")     # the delivered packages ({MOD} unexpanded) — written by the factory, read here
```

Bare `packages/…` paths below resolve under `$PKGS` with `{MOD}` expanded; `execution-state.json`
and `api-docs/` under `$PART` with `{MOD}` expanded; stage artifacts and `manifest.json` under
`$MODULES/{MODULE}/`. A version-suffixed base (`vN/`) applies to each of the three the same way —
with ONE exception: `api-docs/` is never version-suffixed. It is derived from the running
application, so there is one current set per module, not one per plan version.

`$MODULES`, `$GOVROOT`, `$PART` and `$PKGS` below are those values. The profile
folder is the factory's to name; spelling it here makes a second profile an edit
to this file. **`$MODULES` is the read-only analysis tree; api-docs are written
under `$PART`, never under `$MODULES`** — the two differ (`analysis/modules/<MOD>`
vs `backend/modules/<MOD>`) and confusing them writes into the factory's
analysis tree, which the next publish overwrites. Nothing refuses it for you.

## Preconditions

**Module validation:** confirm `$MODULES/$MODULE/` exists before
running anything. Resolve the code from `governance/shared/platform/modules-registry.json` or
`$GOVROOT/project-registry.md` — never invent one.

**Backend running:** the Spring Boot app must be up and `/v3/api-docs/<group>`
must answer for this module. The generator reads the real port from
`src/main/resources/application.properties` — never assume `8080`.

## Your Task

### STEP 1 — Review first, always

```bash
python3 governance/governance-tools/api-doc-generator/generate.py \
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
python3 governance/governance-tools/api-doc-generator/generate.py \
    --module $MODULE --function check
```

`check` prints everything `review` prints, then one line per assertion, each
with its **expected side read from source** and a ratio — never a bare count —
and exits non-zero on any FAIL. Report the block verbatim, then for each FAIL
name the owner and the item you filed. A FAIL is a correct result when the
problem is real; never satisfy it by weakening an assertion or by hand-editing
a generated file. A module that legitimately cannot pass one assertion declares
that once, with a reason, in
`governance/governance-tools/api-doc-generator/waivers/$MODULE.json`
(see the tool's README) — that is the only sanctioned way to silence one.

What each FAIL means, and who can own the fix. Every row lists causes on
**both** sides, because the generator and the backend have each produced every
one of these symptoms at least once — a symptom is a question, not a verdict:

| FAIL | Generator-side cause to rule out | Backend/factory-side cause to rule out | File the item with |
|---|---|---|---|
| `permissions` — 0/N while source has `@PreAuthorize` | the annotation parser misses a spelling (this is how 4 modules published 0/N until 2026-09-23: a concatenated `@PreAuthorize` never matched) — add the spelling to `tests/fixtures/spellings/` first | the controller delegates to two services, or the service source is outside the module root | generator owner, unless the row says `not extracted: DELEGATE_UNRESOLVED` for a controller that genuinely calls two services |
| `permissions` — `not extracted` > 0 | route or delegate matcher (`security_extractor`) cannot follow this controller's shape | a controller method that is not a thin delegate | generator owner first |
| `auth-determined` — 0/N with a `SecurityFilterChain` in source | none: the tool reads `security` from the OpenAPI document and there is none to read | `OpenApiConfig.erpOpenApi()` (the global bearer scheme, added 2026-09-23) was removed or stopped applying, so springdoc emits no `security` and no endpoint can state that it needs a token | backend owner; waive per module only with the backend item's id in the reason |
| `envelope` — no envelope while `ApiResponse` exists | `is_envelope_shape` no longer matches the wrapper's key set | `ApiResponse<T>` lost the `success`/`data` pair, or a controller returns something else | whichever side changed last |
| `status-table` / `error-codes` — 0 read while declared | a third spelling of the table or the constants class | the class was renamed or moved out of the shared root | generator owner first |
| `business-errors` — 0/N bound while source throws | the walk cannot resolve this module's delegate shape (`business_error_extractor`) | services throw only from code the controllers never reach | generator owner first; the `unbound` list names the codes to explain |
| `contract-ids` — heading present, 0 rows | none: the tool needs a verb+path table | the plan writes its API REGISTRY as prose (NOTIF, FILE today) | factory owner |
| `contract-drift` — any row | none: both sides are read verbatim | an endpoint the plan never registered, or a registered id nothing serves — the drift table quotes the id a controller Javadoc **claims**, which is a signpost for the registrar, not a registration | factory owner (register or retire the id); backend owner if the served path is the mistake |
| `stale` | — | the backend changed after the last regeneration | run `--function update`, then STEP 4 |
| `deterministic` | a new extractor iterates a mapping or directory listing unsorted | — | generator owner |

Then give the endpoint count, group count, error-code count, output path and
the per-endpoint change list. If a section is EMPTY and `check` did not fail
on it, say which assertion covered it and why it passed (a legitimately absent
feature is stated as such in the docs themselves — e.g. an endpoint's
`Authorization: no @PreAuthorize/@Secured declared — checked …` line).

### STEP 4 — Publish them (they are NOT published until you do)

`governance/shared` is a **submodule**: a separate repository mounted here.
Files the generator wrote there are untracked in *that* repository and invisible
to everyone else — the factory's `fetch-inputs` and the frontend both read the
pushed commit, not your working tree. Regenerating and stopping looks like
success and delivers nothing.

```bash
cd governance/shared
git fetch origin main
git merge-base --is-ancestor HEAD origin/main \
  && git checkout main \
  || echo "HEAD is NOT on origin/main — skip the checkout, use 'git push HEAD:main' below"
git status --short                       # the regenerated files
git add "${PART/\{MOD\}/$MODULE}/api-docs"   # $PART from Step 0 — never a typed path
git commit -m "api-docs($MODULE): regenerated from the running app"
git push                                 # or: git push HEAD:main, if the guard said so
cd ../..
git add governance/shared                # this repo's pointer to that commit
git commit -m "bump shared: $MODULE api-docs"
git push
```

Two failure modes worth naming, because neither announces itself:

- **Detached HEAD.** `git submodule update --init` checks out a *commit*, so
  the submodule normally sits on no branch at all and a plain `git push` has
  nothing to push to. (Both consumer repos were found detached on 2026-09-17 —
  this is the normal state, not a mishap.) Getting onto `main` fixes that, but
  only when the checked-out commit is an **ancestor of `origin/main`** — hence
  the guard above. When it is not, `git checkout main` would silently move you
  off the tree you just generated against; commit where you are and
  `git push HEAD:main` instead.
- **Pointer not bumped.** Pushing the submodule without committing the pointer
  here leaves this repo claiming the previous api-docs. `gov.py sync` in the
  factory reports it, but only if someone runs it.

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
  expanded, i.e. `governance/shared/backend/modules/[MODULE]/api-docs/`
  (`index.md` + `endpoints/<group-slug>.md`). The path derives from the tool's
  own location and `profile-summary.json`, so the command works from any
  working directory. The generator resolves the layout itself and **refuses**
  rather than guessing when two candidate directories exist — if it says so,
  report that; do not pick one for it.
- `review` is safe to run any time, including in CI, to answer "have the API
  docs drifted from the backend?" without touching a file. `check` is the CI
  gate: the same, plus a non-zero exit on drift, staleness, non-determinism or
  a whole-module silent-empty section (STEP 3).
- Consumers of this output: the frontend repo and the governance factory, both
  of which read **this same single copy** through their own `governance/shared`
  submodule (there is no second copy to keep in step — superseded 2026-09-17),
  and the `api-verify` skill — see
  `governance/shared/platform/rules/api-verify-config.md` §1, which lists api-docs as its
  **mandatory** input and says to regenerate rather than trust a stale copy.
