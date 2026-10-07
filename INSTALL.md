# erp-core plan — install & run

Copy into the root of your fresh backend copy:

```
<repo-root>/
├── erp-core-plan/                         ← the 15 plan files + execution-state.json
└── .claude/commands/erp-core/
    ├── generate-plan-setup.md             ← /erp-core/generate-plan-setup  (refresh state if plan files change)
    ├── execute-step.md                    ← /erp-core/execute-step NN      (one step; used by dispatched agents)
    └── orchestrate-plan.md                ← /erp-core/orchestrate-plan     (multi-agent, --auto)
```

Prerequisites on the machine that runs it: git, JDK 25+, Maven, Docker (Testcontainers).
Optional for step 12: a GitHub remote + `GITHUB_TOKEN`. Step 13 needs nothing external: it was closed by the owner's decision to vendor the governance into this repository (`governance/`, see `governance/README.md`).

Run:
```
/erp-core/generate-plan-setup            # optional — execution-state.json is already shipped
/erp-core/orchestrate-plan --auto        # full autonomous run, 01 → 13
/erp-core/orchestrate-plan --auto --parallel 3   # same, 3 concurrent worktrees for steps 06–11
/erp-core/orchestrate-plan --only 05     # one step, reviewed and merged
```

How it runs: one implementer agent per step (in a git worktree for parallel steps) → an independent reviewer agent re-runs verification and checks every acceptance bullet → a merger agent rebases, verifies, merges `--no-ff` to `main` in numeric order → the orchestrator records state. Halts only after a second-agent debate fails, or on human-only input (external repos, credentials).
