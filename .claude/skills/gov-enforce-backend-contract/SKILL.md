---
name: gov-enforce-backend-contract
description: "GOVERNANCE ENFORCER — validates generated backend code against all 85 contract rules across 7 layers (Domain, Entity, Repository, DTO, Mapper, Service, Controller). Rejects any violation. Use after ANY backend code generation or review."
---

# Skill: gov-enforce-backend-contract

## Description
**GOVERNANCE ENFORCER.** Analyzes backend code and rejects ANY violation of the implementation
contract. This skill is the architectural gatekeeper — it does NOT generate code, it VALIDATES
and BLOCKS non-compliant code.

## When to Use
- After ANY backend code generation (entity, repository, DTO, mapper, service, controller)
- As a mandatory post-generation validation step
- When reviewing a pull request touching a backend module
- When anyone claims a feature is "complete"

## When NOT to Use
- During code generation — this skill validates only
- When validating a single cross-cutting concern — use
  [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) or
  [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) instead

## Responsibilities

- Evaluate all 85 contract rules across the 7 layers
- Mark every check PASS or VIOLATION
- Reject non-compliant code with specific rule references

## Constraints

- MUST NOT generate or modify application code
- MUST NOT fix violations automatically — report them for the matching `build-*` skill
- MUST NOT skip any check — all 85 rules are evaluated

## Output

- A compliance report with PASS/VIOLATION per check across all 7 layers, and rule IDs for
  every failure

---

## Enforcement Checklist

### LAYER 0: Domain (7 checks)

This check is **unconditional** — it applies to every entity whose rules answer
"is this operation allowed?", regardless of what a module's own design docs say.

```
[ ] A.0.1 — A dedicated <Entity>Domain exists for every entity whose rules answer
             "is this operation allowed?" (deactivation guards, immutability,
             cycle prevention, state-transition checks)
[ ] A.0.2 — <Entity>Domain carries NO Spring or JPA annotations
[ ] A.0.3 — <Entity>Domain never accesses a Repository or the database, including via
             constructor injection — all data is passed in as plain arguments
[ ] A.0.4 — <Entity>Domain throws LocalizedException for every rule violation
[ ] A.0.5 — <Entity>Domain is constructed ONLY via static factories create(...) / from(...)
[ ] A.0.6 — <Entity>Domain never imports or calls another module — cross-module data is
             resolved by the service and passed in
[ ] A.0.7 — At most one Domain object per entity; a Domain Service exists only when a rule
             genuinely spans several entities. Flag "one Domain Service per entity" as
             over-application
```

### LAYER 1: Entity (19 checks)

```
[ ] A.1.1  — Extends AuditableEntity. Exception: a declared session-artifact exemption
             (own lifecycle fields) — verify the exemption is intentional before flagging
[ ] A.1.2  — PK @Column name matches the DB script — not a generic "ID" or invented name
[ ] A.1.3  — PK uses GenerationType.SEQUENCE with @SequenceGenerator, even when the module's
             db-script declares GENERATED ALWAYS AS IDENTITY (or "BLOCK 1 — SEQUENCES / none"):
             the skill wins, sequenceName = "SEQ_<TABLE>", and the module's Flyway migration
             creates the sequence. Settled — see GOVERNANCE-RULES.md "Convention Precedence"
[ ] A.1.4  — allocationSize = 1
[ ] A.1.5  — FK columns follow the project's FK suffix convention consistently
[ ] A.1.6  — Boolean mapping matches the DB column type: a native BOOLEAN column takes NO
             @Convert at all; numeric (SMALLINT/NUMBER) uses BooleanNumberConverter; CHAR(1)
             uses BooleanCharYNConverter. A converter on a native BOOLEAN column is itself the
             violation — do not flag its absence there
[ ] A.1.7  — Boolean default declared via @Builder.Default
[ ] A.1.8  — Every @ManyToOne uses fetch = FetchType.LAZY
[ ] A.1.9  — @OneToMany uses cascade = ALL, orphanRemoval = false, fetch = LAZY
[ ] A.1.10 — Uses @SuperBuilder, not @Builder
[ ] A.1.11 — Table name is UPPER_SNAKE_CASE with the module prefix
[ ] A.1.12 — @UniqueConstraint and @Index declared inside @Table
[ ] A.1.13 — Unique constraints named UQ_<TABLE>_<DESC> — taken verbatim from the module's
             db-script, since they are physical DB objects that must match the migration
[ ] A.1.14 — Indexes named IDX_<TABLE>_<COLUMN>
[ ] A.1.15 — FK constraints named FK_<TABLE>_<REF>
[ ] A.1.16 — Computed counts use @Formula, not collection.size()
[ ] A.1.17 — @PrePersist/@PreUpdate are the sole location for normalization
[ ] A.1.18 — Entity has activate() and deactivate() helpers
[ ] A.1.19 — No helper methods iterating or filtering lazy @OneToMany collections
```

### LAYER 2: Repository (9 checks)

```
[ ] A.2.1 — Extends JpaRepository AND JpaSpecificationExecutor
[ ] A.2.2 — Has @Repository
[ ] A.2.3 — Not injected outside its own module
[ ] A.2.4 — Existence checks use existsBy<Field>()
[ ] A.2.5 — Update uniqueness uses existsBy<Field>AndIdNot() only for mutable fields
[ ] A.2.6 — Child queries use JOIN FETCH
[ ] A.2.7 — Reference checks use JPQL count queries
[ ] A.2.8 — Projections used for read-only multi-table queries
[ ] A.2.9 — No dead code — every method has a caller in a service
```

### LAYER 3: DTO (13 checks)

```
[ ] A.3.1  — All DTOs use @Data @Builder @NoArgsConstructor @AllArgsConstructor
[ ] A.3.2  — Class-level @Schema with a bilingual description
[ ] A.3.3  — Every field has @Schema(description, example)
[ ] A.3.4  — Validation messages use i18n keys
[ ] A.3.5  — CreateRequest excludes id and audit fields
[ ] A.3.6  — UpdateRequest excludes immutable fields
[ ] A.3.7  — Response includes all fields + audit + computed counts
[ ] A.3.8  — Audit timestamps carry the project's @JsonFormat pattern in UTC
[ ] A.3.9  — SearchRequest extends the shared search base type
[ ] A.3.10 — Child SearchRequest overrides the common-request conversion
[ ] A.3.11 — Child SearchRequest exposes a parent-id extractor
[ ] A.3.12 — UsageResponse carries canDelete/canDeactivate + reason
[ ] A.3.13 — OptionResponse is slim — no audit fields
```

### LAYER 4: Mapper (7 checks)

```
[ ] A.4.1 — One @Component mapper per entity
[ ] A.4.2 — Child toEntity() accepts the parent entity as a parameter
[ ] A.4.3 — updateEntityFromRequest() returns void
[ ] A.4.4 — updateEntityFromRequest() skips immutable fields
[ ] A.4.5 — toResponse() maps booleans null-safely
[ ] A.4.6 — Every method handles null input
[ ] A.4.7 — toUsageResponse() computes eligibility from counts
```

### LAYER 5: Service (18 checks)

```
[ ] A.5.1  — @Service @RequiredArgsConstructor @Slf4j
[ ] A.5.2  — @PreAuthorize on EVERY public method
[ ] A.5.3  — @Transactional on every write
[ ] A.5.4  — @Transactional(readOnly = true) on every read
[ ] A.5.5  — @CacheEvict on writes, for cache-eligible entities only
[ ] A.5.6  — ALLOWED_SORT_FIELDS whitelist present
[ ] A.5.7  — Search uses the shared spec + pageable builders
[ ] A.5.8  — Returns ServiceResult<T>; delete() is void
[ ] A.5.9  — create() → decide → map → save → Status.CREATED
[ ] A.5.10 — update() → find → not-found throw → decide → save → Status.UPDATED
[ ] A.5.11 — delete() → find → reference check → delete, no try-catch
[ ] A.5.12 — activate()/deactivate() → find → (decide) → entity helper → save
[ ] A.5.13 — Error codes come from constants
[ ] A.5.14 — log.info() for writes, log.debug() for reads
[ ] A.5.15 — Every exception is a LocalizedException
[ ] A.5.16 — Child search requires a non-null parent id
[ ] A.5.17 — Child search uses an explicit Specification join
[ ] A.5.18 — Business-rule checks delegated to <Entity>Domain — not inlined in the service,
             and not left on the entity beyond a trivial field mutation. The service body is
             orchestration-only. Automatic rejection trigger
```

### LAYER 6: Controller (12 checks)

```
[ ] A.6.1  — @RestController @RequestMapping @RequiredArgsConstructor
[ ] A.6.2  — @Tag with a bilingual description
[ ] A.6.3  — Injects ONLY service(s) + the response helper
[ ] A.6.4  — Non-delete endpoints return the crafted response
[ ] A.6.5  — Delete: @ResponseStatus(NO_CONTENT) + void
[ ] A.6.6  — Search uses POST /search with @RequestBody
[ ] A.6.7  — Separate activate and deactivate endpoints
[ ] A.6.8  — Usage exposed at GET /{id}/usage
[ ] A.6.9  — Child endpoints under the same controller
[ ] A.6.10 — @Operation on every method
[ ] A.6.11 — @Valid @RequestBody on every request body
[ ] A.6.12 — ZERO business logic
```

---

## Status → HTTP Mapping

Used by the checks above. This is the project's canonical error taxonomy:

| Situation | `Status` | HTTP |
|-----------|----------|------|
| Entity not found | `NOT_FOUND` | 404 |
| Duplicate natural key / code | `ALREADY_EXISTS` | 409 |
| Blocked because a referencing record exists (delete/deactivate) | `CONFLICT` | 409 |
| Invariant violation that is NOT about a specific referencing record (invalid state transition, cycle) | `BUSINESS_RULE_VIOLATION` | 422 |
| Structural/input validation failure | `VALIDATION_ERROR` | 400 |
| Created | `CREATED` | 201 |
| Updated / read | `UPDATED` / `SUCCESS` | 200 |

---

## Automatic Rejection Triggers

These patterns trigger IMMEDIATE rejection — no exceptions:

| Pattern | Reason |
|---------|--------|
| A raw `RuntimeException` thrown for a business error | Must use `LocalizedException` |
| A service method without `@PreAuthorize` | Authorization gap |
| A service returning a raw DTO or entity instead of `ServiceResult` | Envelope contract broken |
| `GenerationType.IDENTITY` or `AUTO` | Must use `SEQUENCE` — still a trigger when the db-script declares an identity column; the migration creates `SEQ_<TABLE>` |
| `@Builder` on an entity instead of `@SuperBuilder` | Breaks `AuditableEntity` inheritance |
| A repository injected outside its module | Cross-module violation |
| Direct import/injection of another module's `@Service`, `Repository`, `@Entity`, or any class outside its cross-module package | Cross-module violation |
| An event published through a message broker or a custom publisher/listener port instead of `ApplicationEventPublisher` | Not this architecture |
| A controller containing business logic | Thin-controller violation |
| A controller injecting a repository | Layer violation |
| `@ResponseStatus(CREATED)` on POST | Derived from `Status.CREATED` |
| An entity not extending `AuditableEntity` without a declared exemption | Missing audit trail |
| A **numeric** or **`CHAR(1)`** boolean column mapped without the project's converter | Storage convention breach. Does NOT apply to a native `BOOLEAN` column — that one correctly takes no converter |
| A mapper applying case normalization | `@PrePersist` owns it |
| The active flag set directly in a service | Must use `activate()`/`deactivate()` |
| A business-rule `if` inlined in a service method | Must delegate to `<Entity>Domain` (A.5.18) |
| `<Entity>Domain` annotated with `@Component`/`@Service`/`@Entity` | Must be a plain class (A.0.2) |
| `<Entity>Domain` holding a Repository | Must never touch persistence (A.0.3) |

---

## Violation Response Template

```
❌ VIOLATION DETECTED

Rule: [Rule ID] — [Rule description]
Location: [File:Line]
Found: [What was found]
Expected: [What should be there]
Severity: CRITICAL / HIGH / MEDIUM

Fix: [Exact correction]
```

---

## Shared-Layer Consumption Checks (CU.1–CU.8)

| # | Check | Expected | Violation |
|---|-------|----------|-----------|
| CU.1 | Entity extends `AuditableEntity` | `extends AuditableEntity` | A custom audit base class |
| CU.2 | Non-native boolean columns use the project's converter (native `BOOLEAN` takes none) | `@Convert(converter = ...)` on numeric / `CHAR(1)` only | A custom boolean converter, or a converter on a native `BOOLEAN` column |
| CU.3 | Service returns `ServiceResult<T>` | Every non-delete method | A custom result wrapper |
| CU.4 | Errors use `LocalizedException` | `throw new LocalizedException(...)` | Raw exceptions |
| CU.5 | Search uses the shared builders | `SpecBuilder` + `PageableBuilder` | Manual construction |
| CU.6 | Controller uses the shared response helper | `operationCode.craftResponse(...)` | Custom wrapping |
| CU.7 | No duplicate global exception handler | Zero exception `@ControllerAdvice` in a feature module | A per-module handler |
| CU.8 | No duplicate response envelope | Zero custom envelope in a feature module | A per-module envelope |

A feature failing any CU check is non-compliant regardless of its layer scores.

---

## Enforcement Report Format

```
## Backend Contract Enforcement Report

### Feature: [Name]   ### Module: [Module]   ### Date: [Date]

| Layer      | Checks | Passed | Failed | Status |
|------------|--------|--------|--------|--------|
| Domain     | 7      | ?      | ?      | ✅/❌  |
| Entity     | 19     | ?      | ?      | ✅/❌  |
| Repository | 9      | ?      | ?      | ✅/❌  |
| DTO        | 13     | ?      | ?      | ✅/❌  |
| Mapper     | 7      | ?      | ?      | ✅/❌  |
| Service    | 18     | ?      | ?      | ✅/❌  |
| Controller | 12     | ?      | ?      | ✅/❌  |
| **TOTAL**  | **85** | **?**  | **?**  | **?**  |

### Violations Found:
1. [Rule ID] — [Description] — [Location]

### Verdict: APPROVED / REJECTED
```

---

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | Deep dive on error handling: `LocalizedException`, `Status`, error codes |
| [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) | Caching eligibility and annotation rules |
| [`gov-validate-backend-feature`](../gov-validate-backend-feature/SKILL.md) | Master validation across all layers, with scoring |
