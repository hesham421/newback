# `governance/` — the mount, and the repo's own tools

This repository holds **no governance artifacts**. They live in one place that
all three repositories mount, and this directory is where that place is
attached plus the backend's own tooling.

```
governance/
  shared/            ← the shared governance repository (git submodule)
  governance-tools/  ← this repo's own tools (api-doc-generator)
  mcp-servers/       ← reference copies of MCP servers wired in .mcp.json
  project-artifacts/ ← this repo's reports and notes (not governance)
  testsprite/        ← TestSprite mechanism, prompts and dated run bundles
```

**After a clone:** `git submodule update --init --recursive`. Without it,
`governance/shared/` is empty and nothing that reads a plan, a package or an
api-doc will find anything.

## Where to read, where to write

`CLAUDE.md` §"Where governance lives" carries the full table. In short:

- **read** `governance/shared/platform/rules/` and
  `governance/shared/analysis/modules/<MOD>/` (the analysis), `governance/shared/backend/modules/<MOD>/packages/` (the delivered packages)
- **write** only `governance/shared/backend/modules/<MOD>/api-docs/` and
  the rest of `governance/shared/backend/modules/<MOD>/` (execution-state.json, test-api/) — never its `packages/`

Anything else under `governance/shared/` belongs to the factory or to the
frontend, and a write there is refused at review by that repo's `CODEOWNERS`.

## What used to be here

Until this migration the backend kept its own copy of every plan, package and
registry the factory produced, delivered by `gov.py deliver`. Three copies of
the same artifact drifted: the backend amended its copy during implementation
— a whole requirement, `REQ-SEC-034`, was added there and the factory never
saw it. The copies were reconciled into the shared repo before being removed;
their history remains in this repository, readable with
`git log -- governance/modules/<MOD>`.

The frontend used to reach into this repo to read `GOVERNANCE-RULES.md`, under
a heading that had to call it a "sanctioned cross-repo read". That file is now
`governance/shared/platform/rules/GOVERNANCE-RULES.md` and there is no boundary
to cross.
