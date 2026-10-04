---
name: gov-validate-backend-feature
description: "MASTER VALIDATION — runs every enforcement check across a completed backend feature: build order, file inventory, the 85 layer contract rules, and the cross-cutting validations (error handling, caching, security, immutability, envelope, domain delegation, cross-module). Produces a score and a verdict."
---

# Skill: gov-validate-backend-feature

## Description
**MASTER VALIDATION SKILL.** The final gatekeeper. Runs ALL enforcement checks against a
completed backend feature, detects violations, reports missing layers, and issues a verdict. It
orchestrates the other governance skills into a single pass.

## When to Use
- After anyone claims a backend feature is "complete"
- Before merging a backend feature branch
- As the final step before moving on to the client implementation
- During review of any backend module

## When NOT to Use
- During individual layer creation — use the matching `build-*` skill
- For a partial feature where layers are still missing — report the missing layers instead of
  running a full validation pass

## Responsibilities

- Verify the build order was followed
- Run the file inventory check
- Execute all 85 layer contract checks
- Validate the cross-cutting concerns
- Produce a score and a verdict

## Constraints

- MUST NOT generate or modify application code — validation only
- MUST NOT accept a partial feature
- MUST NOT skip a stage
- MUST NOT fix violations automatically — report them against the responsible skill

---

## STAGE 0: Build Order (9 checks)

```
[ ] 1. Entity        — exists, extends AuditableEntity
[ ] 2. Repository    — exists, extends JpaRepository + JpaSpecificationExecutor
[ ] 3. DTOs          — the full required set exists
[ ] 4. Mapper        — exists, @Component
[ ] 5. Domain        — a plain <Entity>Domain exists for every entity whose rules answer
                       "is this operation allowed?"; no Spring/JPA annotations; built only
                       via create()/from(); the service delegates to it rather than inlining
[ ] 6. Error codes   — registered in <Module>ErrorCodes
[ ] 7. Permissions   — VIEW/CREATE/UPDATE/DELETE registered in the permission constants class
[ ] 8. Service       — exists, @Service
[ ] 9. Controller    — exists, @RestController
```

> **If any step is missing → REJECT immediately. No partial features.**

---

## STAGE 1: File Inventory (15 mandatory files)

```
src/main/java/<base/package>/<module>/
├── entity/<ENTITY_CLASS>.java                      [ ]
├── repository/<Entity>Repository.java              [ ]
├── dto/<Entity>CreateRequest.java                  [ ]
├── dto/<Entity>UpdateRequest.java                  [ ]
├── dto/<Entity>Response.java                       [ ]
├── dto/<Entity>SearchRequest.java                  [ ]
├── dto/<Entity>UsageResponse.java                  [ ]
├── dto/<Entity>OptionResponse.java                 [ ]  ← conditional: dropdown use only
├── mapper/<Entity>Mapper.java                      [ ]
├── domain/<Entity>Domain.java                      [ ]  ← required when the entity has
│                                                          decision rules
├── crossmodule/<Name>Api.java                      [ ]  ← conditional: only if this feature
│                                                          exposes data to another module
├── exception/<Module>ErrorCodes.java (updated)     [ ]
├── service/<Entity>Service.java                    [ ]
└── controller/<Entity>Controller.java              [ ]

<permission constants class> (updated)              [ ]
src/main/resources/i18n/
├── messages.properties (updated)                   [ ]
└── messages_<locale>.properties (updated)          [ ]
```

> Conditional files (`OptionResponse`, `crossmodule/`) are not counted in the 15 and are not a
> violation when genuinely not applicable.

---

## STAGE 2: Layer Contracts (85 checks)

Run the full checklist from
[`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md):

| Layer | Rules | Checks |
|-------|-------|--------|
| Domain | A.0.1 – A.0.7 | 7 |
| Entity | A.1.1 – A.1.19 | 19 |
| Repository | A.2.1 – A.2.9 | 9 |
| DTO | A.3.1 – A.3.13 | 13 |
| Mapper | A.4.1 – A.4.7 | 7 |
| Service | A.5.1 – A.5.18 | 18 |
| Controller | A.6.1 – A.6.12 | 12 |
| **TOTAL** | | **85** |

---

## STAGE 3: Cross-Cutting (37 checks)

### 3.1 Error handling (8)
Full checklist in [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md).
```
[ ] No generic not-found or raw runtime exception anywhere in the module
[ ] Every not-found      → LocalizedException(Status.NOT_FOUND, ...)
[ ] Every duplicate      → LocalizedException(Status.ALREADY_EXISTS, ...)
[ ] Every blocked-by-reference → LocalizedException(Status.CONFLICT, ...)
[ ] delete() does not try-catch the constraint-violation exception
[ ] Error codes registered in <Module>ErrorCodes
[ ] Messages present in the default bundle
[ ] Messages present in every other supported-locale bundle
```

### 3.2 Caching (5)
Full checklist in [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md).
```
[ ] The entity is on the approved register, OR carries no caching annotations at all
[ ] If cached: @CacheEvict on every write method
[ ] If cached: @Cacheable only on approved read methods
[ ] If cached: annotation order is Cache → Transaction → Security
[ ] If not cached: zero caching annotations anywhere
```

### 3.3 Security (4)
```
[ ] Four permissions defined: VIEW, CREATE, UPDATE, DELETE
[ ] Permission constants follow the project's naming convention
[ ] @PreAuthorize on every public service method
[ ] Permission constants referenced — never hardcoded strings
```

### 3.4 Immutability (4)
```
[ ] Natural keys identified and documented
[ ] UpdateRequest excludes natural keys and FK references
[ ] The mapper's update method skips immutable fields
[ ] The service never updates an immutable field
```

### 3.5 Response envelope (8)
```
[ ] Service methods return ServiceResult<T> — except delete()
[ ] create() uses Status.CREATED
[ ] update()/activate()/deactivate() use Status.UPDATED
[ ] getById()/search()/getUsage() use the default success status
[ ] delete() returns void
[ ] The controller crafts every non-delete response through the shared helper
[ ] The controller uses @ResponseStatus(NO_CONTENT) for delete
[ ] The controller does not use @ResponseStatus(CREATED)
```

### 3.6 Domain delegation (4)
```
[ ] Business-rule guards are not inlined in service method bodies
[ ] The service body is orchestration-only: load → delegate → persist → return
[ ] <Entity>Domain throws the rule violations, not the service
[ ] <Entity>Domain never calls another module — the service resolves and passes that data in
```

### 3.7 Cross-module calls & eventing (4)
```
[ ] Every cross-module read goes through the target module's cross-module interface
[ ] That interface is injected only into this module's service — never a Domain object,
    mapper, or controller
[ ] Every published event uses ApplicationEventPublisher with a dedicated
    <Action><Entity>Event — no message broker, no custom publisher/listener port
[ ] The listener lives in the module that defines the event
```

---

## STAGE 4: Compilation (2 checks)

```
[ ] The project compiles cleanly
[ ] No new compilation warnings attributable to this feature
```

---

## Scoring

| Stage | Max points |
|-------|------------|
| Stage 0 — Build order | 9 |
| Stage 1 — File inventory | 15 |
| Stage 2 — Layer contracts | 85 |
| Stage 3 — Cross-cutting | 37 |
| Stage 4 — Compilation | 2 |
| **TOTAL** | **148** |

> One point per checklist item. Stage 3 sums its seven areas: 8+5+4+4+8+4+4 = 37. The shared-layer
> consumption checks (CU.1–CU.8) are deliberately outside this pool — they are a separate
> pass/fail gate, below.

### Verdict thresholds

| Score | Verdict | Action |
|-------|---------|--------|
| 148 / 148 (100%) | ✅ **APPROVED** | Proceed |
| 141–147 (95%+) | ⚠️ **APPROVED WITH NOTES** | Document the minor issues and proceed |
| 118–140 (80%+) | 🔶 **CONDITIONAL** | Fix before proceeding |
| < 118 (< 80%) | ❌ **REJECTED** | Major rework required |

### Automatic rejection — regardless of score

- A raw or generic exception used instead of `LocalizedException`
- A service method without `@PreAuthorize`
- A service returning a raw entity outside its module
- `GenerationType.IDENTITY` or `AUTO`
- A repository injected in another module
- Direct import/injection of another module's `@Service`, `Repository`, `@Entity`, or any class
  outside its cross-module package
- An event published through a message broker or a custom publisher/listener port
- Business logic in a controller
- An entity not extending `AuditableEntity` without a declared exemption
- `@Builder` instead of `@SuperBuilder` on an entity
- Repository methods with no caller in any service
- Entity helpers iterating or filtering a lazy collection
- A child mapper `toEntity()` without the parent FK parameter
- A business-rule condition inlined in a service instead of delegated to `<Entity>Domain`
- `<Entity>Domain` annotated with `@Component`/`@Service`/`@Entity`, or accessing a repository

### Shared-layer compliance gate (CU.1–CU.8)

Run [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md)'s CU checks. A
feature failing any of them is non-compliant regardless of its score above.

---

## Validation Report Template

```
# Backend Feature Validation Report

## Feature: [Name]   ## Module: [Module]   ## Entity: [Entity]   ## Date: [Date]

## STAGE 0: Build Order        [x/9]
## STAGE 1: File Inventory     [x/15]

## STAGE 2: Layer Contracts
| Layer      | Checks | Passed | Failed |
|------------|--------|--------|--------|
| Domain     | 7      | ?      | ?      |
| Entity     | 19     | ?      | ?      |
| Repository | 9      | ?      | ?      |
| DTO        | 13     | ?      | ?      |
| Mapper     | 7      | ?      | ?      |
| Service    | 18     | ?      | ?      |
| Controller | 12     | ?      | ?      |

## STAGE 3: Cross-Cutting
| Area                        | Checks | Passed | Failed |
|-----------------------------|--------|--------|--------|
| Error Handling              | 8      | ?      | ?      |
| Caching                     | 5      | ?      | ?      |
| Security                    | 4      | ?      | ?      |
| Immutability                | 4      | ?      | ?      |
| Response Envelope           | 8      | ?      | ?      |
| Domain Delegation           | 4      | ?      | ?      |
| Cross-Module & Eventing     | 4      | ?      | ?      |

## STAGE 4: Compilation         [x/2]

## VIOLATIONS FOUND
1. [Rule ID] — [Description] — [Location] — [Severity]

## SCORE: [X] / 148 ([Y]%)
## VERDICT: APPROVED / APPROVED WITH NOTES / CONDITIONAL / REJECTED

## REQUIRED FIXES
1. [Fix]
```

---

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | The 85 layer checks + CU gate |
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | 23-check error-handling compliance |
| [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) | 30-check caching eligibility and annotations |
