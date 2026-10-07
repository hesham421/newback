# AMEND-P3-O — Backend Stage-2 + Prompt/Instruction Maturation

Scope: the backend `governance/` package — the Stage-2 toolset, its docs, and
the `.claude/commands/` prompts. Goal: bring prose, prompts, and tooling into
one consistent, verified reality and harden against silent failure modes.
Everything below is covered by the pytest suite in
`governance-tools/tests/` (34 tests) and verified end-to-end.

## governance-tools/ (code)

```
ID          FINDING                                       RESOLUTION
──────────────────────────────────────────────────────────────────────────────
C1          Agent 3 re-prefixed already phase-qualified   Filename = the SUB's
            SUB labels → SVC-API-SVC-API-CRUD.md          own qualified label.
FINDING-19  backend-test SUBs pre-created as FOLDERS but   backend-test is a
            written as flat FILES → dead folders.         container-only package.
FINDING-22a Agent 1 --dry-run + --auto-register wrote      Dry-run is read-only;
            the registry despite "no changes".            registration deferred.
C4          Content outside all phases (Plan Index, Error  Captured as
            Catalog, Agent Handoff Summary) was dropped.  _SECTIONS.md; dead
                                                          SECTIONS folder removed.
M2          Non-canonical PHASE key / un-qualified SUB /   New blocking semantic
            orphan atomic passed validation → silent loss. validator (Stage 1 +
                                                          --validate-markers).
M4          Agent 2 --force was inert; its "won't          Existing files kept
            overwrite" note was false.                    unless --force; honest.
M1          ALLOWED_PARENTS defined twice.                 Single source (config).
(parser)    Tokenizer kept only the FIRST marker per line. finditer, all markers.
M3          No auto split-threshold check — an             config.PHASE_SPLIT_
            over-threshold unsplit phase passed unnoticed. THRESHOLDS + advisory
                                                          check. Flexible: warns by
                                                          default, blocks under
                                                          --strict-thresholds. Only
                                                          marker-countable phases;
                                                          DATA-DOM entities left to
                                                          the engine.
M7          No tests.                                      pytest suite added (34).
```

## Autonomy: deterministic self-repair + autonomous split orchestrator

- `agent3_splitter.py --fix-safe --file <f>` (NEW): deterministic, reversible
  marker repairs — phase-key separator typos (`SVC_API`→`SVC-API`) and SUB
  phase-qualification (`SUB:CRUD`→`SUB:SVC-API-CRUD`) — with a `<file>.orig`
  backup and re-validation. Refuses (exit 1, reports) anything needing
  judgment: unmatched markers, duplicate IDs, orphan atomics, ambiguous keys,
  threshold restructuring. Covered by tests/test_safe_autofix.py.
- `process-project-files.md` rewritten into a fully autonomous split
  orchestrator: scan → filename repair → validate → `--fix-safe` → re-validate
  → archive → split (all 5 stages) → report. Runs end to end without prompts,
  stopping ONLY for: an overwrite of existing work, an unfixable/ambiguous
  error, or an undeterminable module code. Threshold advisories are reported,
  never auto-restructured. Test suite now 34 tests.

## Execution prompts (plan/state generation + automatic execution)

- `orchestrate-module.md` (automatic execution) — REWRITTEN backend-only. It was
  written with frontend framing (create-*/TanStack skills, `.github/skills/
  frontend/` paths, a cross-repo "reach into the other side" gap protocol) while
  living in the backend repo — a genuine mis-scoping. Now it is a pure BACKEND
  orchestrator: backend phases (CORE…ALIGN-BE), backend skills from
  `GOVERNANCE-RULES.md` + `.claude/skills/` (`build-*`/`gov-*`), architecture
  rules read from inside the skills, `db-script.md`/SRS as contract ground truth,
  and a same-repo spec-gap protocol (STEP 2) that NEVER reaches into `frontend/`.
  Keeps all the hard-won discipline (one sub per session, re-read the command each
  sub, mandatory skill read + STEP 1.3 verification) and the opt-in `--auto`
  autonomous mode. The `api_doc_gaps` shape matches the generator's canonical one.
- `generate-module-setup.md` (plan + execution-state generation): the generated
  executor now reads the phase `*-HEADER.md` and `_SECTIONS.md` shared context
  once (STEP 1.0); the scan no longer miscounts a `[PHASE]-HEADER.md` as a SUB
  and ignores `_SECTIONS.md` for phase detection; per-sub reads the
  phase-qualified SUB filename. (Earlier: `oracle-sql` → PostgreSQL MCP; bogus
  `MANDATORY-J.md` / `RULE-SCENARIOS-HEADER.md` fields → real
  `TEST-PLAN-BE-HEADER.md`.)

## TestSprite

- `testsprite/TESTSPRITE-GOVERNANCE.md`: the §3 endpoint→module classification
  table was stale (describes modules that no longer exist; `modules-registry.json`
  is empty). Moved the stale-table warning ABOVE the table and tied it to the
  empty registry, so no one classifies against a fictional layout — rebuild it
  from the real registered modules before a run. The rest (mechanism, archival
  rules, sync rule, fix-bugs loop) was audited and is already backend-correct
  (references GOVERNANCE-RULES, `gov-*`/`build-*` skills, no oracle/P4).

## Docs

- `STAGE-2-GOVERNANCE-TOOLS.md` — rewritten to current reality (backend-only,
  no MARK, no P4, `backend-*` filenames, single 5-stage run, the new
  structural+semantic validation, `_SECTIONS.md`, test suite, impact checklist).
- `AGENTS-GUIDE.md` — rewritten (correct filenames/folders, correct package
  layout, "files ∝ structure, not element count", validate-first workflow).
- `governance-tools/README.md` — corrected the two stale file-list lines.

## .claude/commands/ (prompts / instructions)

- `governance-tools-launcher.md` — corrected artifact filenames; removed flags
  that do not exist in the tools' argparse (`--version/-v`, agent1 `-d/-n/-a`,
  agent2 `-d`); removed the deleted `audit-report.md`; documented the real
  `--validate-markers --file` pre-archive check and `--output`.
- `generate-module-setup.md` — `oracle-sql` → the PostgreSQL MCP (the stack this
  repo actually ships); test-phase `header_file` → real `TEST-PLAN-BE-HEADER.md`
  and removed the non-existent `MANDATORY-J.md` / `RULE-SCENARIOS-HEADER.md`
  references (mandatory scenarios are TCs inside the SUB files).
- `process-project-files.md` — replaced the hardcoded `/Users/ezzat/my project/`
  absolute paths with runtime-derived roots, per the same portability law
  `orchestrate-module.md` already follows.
- `bootstrap-legacy-frontend.md` — was BROKEN: it invoked the backend tools with
  `--frontend-only` / `--track frontend` flags that do not exist (the split
  toolsets have no track flag), and referenced a non-existent
  `generate-module-setup-3.md`. Rewritten to invoke the FRONTEND toolset
  (`frontend/governance/governance-tools/`) plainly, with runtime-derived roots
  and the precondition gate defined inline.

## P4 is optional and non-breaking (clarification)

- Backend model has NO P4/audit stage (`BACKEND_STAGES = P0..P3_5_BE`); the
  tools never create, require, or split P4. A missing optional artifact is a
  graceful skip (e.g. no test plan → Stage 3 skips, run still completes), and a
  leftover `P4*` folder in a legacy module is ignored.
- `bootstrap-legacy-frontend.md` no longer lists `P4_2` among the folders to
  bootstrap; added an explicit note that P4 is absent, optional, and
  non-breaking.

## Unchanged (audited, already correct)

`GOVERNANCE-RULES.md`, `WORKSPACE.md`, `vision.md`, `mcp-servers/postgres/`,
`modules-registry.json` — audited, no drift found. (`orchestrate-module.md` and
`bootstrap-legacy-frontend.md` WERE changed — see the sections above; the
latter's broken `--track`/`--frontend-only` flags and dangling
`generate-module-setup-3.md` reference were fixed to use the split frontend
toolset.)
