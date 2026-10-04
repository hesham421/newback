# Skills: backend

Skill pack for the backend (Spring Boot / JPA). Read this file first when working under
`.claude/skills/`. These skills live here — rather than under `governance/` — so they auto-load
via the Skill tool in every session; `governance/shared/platform/rules/GOVERNANCE-RULES.md` remains the authoritative
routing table for which skill to use and when.

Skills are split into two lanes by prefix. Both lanes sit directly under `.claude/skills/`, since
skill discovery only scans one level deep — the prefix is what separates them, not a subfolder.

## `build-*` — the build pipeline

Run in order, one entity at a time. Each step depends on the previous one.

| Step | Skill | Use when |
|------|-------|----------|
| 1 | [build-create-entity](build-create-entity/SKILL.md) | Starting any new backend artifact — JPA entity + its Domain companion. Always first. |
| 2 | [build-create-repository](build-create-repository/SKILL.md) | After the entity — `JpaRepository` + `JpaSpecificationExecutor` |
| 3 | [build-create-dto](build-create-dto/SKILL.md) | After the repository — Create, Update, Response, Search, Usage, Option |
| 4 | [build-create-mapper](build-create-mapper/SKILL.md) | After the DTOs — the `@Component` entity↔DTO mapper |
| 5 | [build-create-service](build-create-service/SKILL.md) | Orchestration: CRUD, search, activate/deactivate, transactions, authorization, caching |
| 6 | [build-create-controller](build-create-controller/SKILL.md) | Last — the thin `@RestController` |

## `gov-*` — governance enforcement

Validation only. These skills never generate or modify code.

| Skill | Use when |
|-------|----------|
| [gov-enforce-backend-contract](gov-enforce-backend-contract/SKILL.md) | After any generation or review — the 85 contract rules across 7 layers |
| [gov-enforce-error-handling](gov-enforce-error-handling/SKILL.md) | Reviewing exception handling — 23 checks on `LocalizedException` + registered error codes |
| [gov-enforce-caching-rules](gov-enforce-caching-rules/SKILL.md) | Reviewing `@Cacheable`/`@CacheEvict` — 30 checks against the approved register |
| [gov-validate-backend-feature](gov-validate-backend-feature/SKILL.md) | Final review of a completed feature — 148-point scored pass |

## Conventions these skills follow

- **No hardcoded names.** Module, entity, table, column, sequence, cache and permission names are
  variables resolved from the module's own DB script and execution plan. If a value cannot be
  resolved, the skill stops and asks rather than inventing one.
- **`<base.package>`** stands for the project's base Java package throughout.
- **Placeholders** (`<Entity>`, `<ENTITY_CLASS>`, `<Module>ErrorCodes`, `<PERMISSIONS_CLASS>`)
  appear in every code block. They are meant to be substituted, never copied literally.

## Adding a new skill

1. Create `<lane-prefix>-<skill-name>/SKILL.md` with `name` and `description` frontmatter, where
   the lane prefix is `build-` (generates code) or `gov-` (validates code).
2. Make the `name:` in the frontmatter match the folder name exactly.
3. Add a row to the matching table above.
4. If the skill changes one of the 85 contract rules, update `gov-enforce-backend-contract`
   and the affected counts in `gov-validate-backend-feature`.
