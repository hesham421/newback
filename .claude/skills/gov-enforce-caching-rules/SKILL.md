---
name: gov-enforce-caching-rules
description: "CACHING GOVERNANCE ENFORCER — validates @Cacheable/@CacheEvict usage against the project's approved-entity register and eligibility criteria. Prevents unauthorized caching, wrong annotation order, cache-name drift, and missing eviction on write methods."
---

# Skill: gov-enforce-caching-rules

## Description
**CACHING GOVERNANCE ENFORCER.** Validates that caching annotations, cache names and caching
patterns comply with the project's caching architecture. Prevents unauthorized caching, stale-data
risk, and annotation misplacement.

## When to Use
- After any service is created or modified
- When `@Cacheable` or `@CacheEvict` is added anywhere
- When someone proposes caching a new entity
- As part of the [`gov-validate-backend-feature`](../gov-validate-backend-feature/SKILL.md) pipeline

## When NOT to Use
- When the service uses no caching annotations at all
- For the JPA second-level cache or a database query cache — this skill covers the Spring cache
  abstraction only

## Responsibilities

- Decide cache eligibility against the criteria below and the project's approved register
- Validate annotation usage, placement and ordering
- Ensure cache names match between read and write methods
- Detect unauthorized caching of transactional, financial or workflow data

## Constraints

- MUST NOT generate or modify application code — validation only
- MUST NOT approve caching for an entity that is not on the approved register
- MUST NOT add an entity to the register — that requires an explicit governance decision
- MUST NOT allow caching annotations on repositories or controllers

## Output

- A caching compliance report: eligibility verdict, annotation correctness, and specific
  violations with rule references

---

## Cache Eligibility Gate (FIRST CHECK)

### The approved register

The project maintains its own list of cache-eligible entities and their approved cache names. It
is **explicit and exhaustive** — an entity absent from it is not cacheable, full stop.

| Entity | Approved cache name | Justification |
|--------|---------------------|---------------|
| *(populated per project — one row per approved entity)* | | |

> This table starts empty for a new project. An entity is added only by an explicit governance
> decision recording that it satisfies every criterion below. Never add a row while implementing
> a feature.

### Eligibility criteria (ALL must be true)

| # | Criterion | Threshold |
|---|-----------|-----------|
| 1 | Dataset size | Small and bounded — a few hundred rows, not growing with transactions |
| 2 | Update frequency | Low, administrator-initiated |
| 3 | Financial or transactional impact | NONE |
| 4 | Workflow / state lifecycle | NONE |
| 5 | Reuse breadth | Read by more than one module or on nearly every request |
| 6 | Usage pattern | Dropdowns, authorization checks, navigation — not business records |

> **If ANY criterion is false → the entity is NOT cacheable. Period.**

### Never cacheable, regardless of the criteria

- ❌ Financial or accounting records (postings, journals, balances, invoices)
- ❌ Any entity carrying a monetary amount that participates in a transaction
- ❌ Approval-based or workflow-driven entities
- ❌ High write-frequency entities
- ❌ Per-user or session-scoped data held in a shared cache
- ❌ Search result sets — any `search()` method

---

## Enforcement Checklist (30 checks)

### CHECK 1: Eligibility (6)

```
[ ] D.1.1 — The entity is on the project's approved register
[ ] D.1.2 — The entity satisfies ALL 6 eligibility criteria
[ ] D.1.3 — The entity is not financial or transactional
[ ] D.1.4 — The entity is not approval- or workflow-driven
[ ] D.1.5 — The entity is not per-user/session data in a shared cache
[ ] D.1.6 — @Cacheable is not on a search() or paginated method
```

### CHECK 2: Cache naming (4)

```
[ ] D.2.1 — The cache name is domain-specific camelCase — not "cache1", "data", "temp"
[ ] D.2.2 — The cache name matches the register's approved name for this entity
[ ] D.2.3 — No alias or alternative name is used for the same data
[ ] D.2.4 — @Cacheable and @CacheEvict use the SAME cacheNames value
```

### CHECK 3: Annotation placement (7)

```
[ ] D.3.1 — @Cacheable appears on service-layer read methods only
[ ] D.3.2 — @CacheEvict appears on service-layer write methods only
[ ] D.3.3 — @Cacheable is not on a write method
[ ] D.3.4 — @CacheEvict is not on a read-only method
[ ] D.3.5 — @CacheEvict uses allEntries = true
[ ] D.3.6 — Cached read order:  @Cacheable  → @Transactional(readOnly) → @PreAuthorize
[ ] D.3.7 — Cached write order: @CacheEvict → @Transactional          → @PreAuthorize
```

### CHECK 4: Eviction completeness (6)

```
[ ] D.4.1 — create() has @CacheEvict
[ ] D.4.2 — update() has @CacheEvict
[ ] D.4.3 — activate() and deactivate() have @CacheEvict
[ ] D.4.4 — delete() has @CacheEvict
[ ] D.4.5 — No partial key-based eviction without an explicit governance decision
[ ] D.4.6 — Eviction is co-located with the @Transactional method that performs the write
```

### CHECK 5: Prohibited patterns (7)

```
[ ] D.5.1 — No @Cacheable on a search or paginated method
[ ] D.5.2 — No direct cache-client calls (e.g. a raw template) in service code
[ ] D.5.3 — No caching of financial entities
[ ] D.5.4 — No caching of workflow entities
[ ] D.5.5 — No @Cacheable on an entity absent from the register
[ ] D.5.6 — No @CachePut without a paired eviction strategy
[ ] D.5.7 — No caching annotations on a repository or a controller
```

---

## Annotation Order Enforcement

```java
// Cached read — eligible entities only
@Cacheable(cacheNames = "<CACHE_NAME>", key = "#id")   // 1. Cache
@Transactional(readOnly = true)                        // 2. Transaction
@PreAuthorize("hasAuthority(...)")                     // 3. Security
public ServiceResult<<Entity>Response> getById(Long id) { }

// Cached write — eligible entities only
@CacheEvict(cacheNames = "<CACHE_NAME>", allEntries = true)  // 1. Evict
@Transactional                                                // 2. Transaction
@PreAuthorize("hasAuthority(...)")                            // 3. Security
public ServiceResult<<Entity>Response> update(Long id, ...) { }

// Non-cached method — the default for almost every entity
@Transactional(readOnly = true)     // 1. Transaction
@PreAuthorize("hasAuthority(...)")  // 2. Security
public ServiceResult<<Entity>Response> getById(Long id) { }
```

## Non-Eligible Entity Pattern

An entity absent from the register carries **zero** caching annotations:

```java
// ✅ CORRECT — not on the register, so no caching at all
@Transactional
@PreAuthorize("hasAuthority(...)")
public ServiceResult<<Entity>Response> create(<Entity>CreateRequest request) { }

// ❌ VIOLATION — caching an entity that was never approved
@CacheEvict(cacheNames = "<something>", allEntries = true)
@Transactional
public ServiceResult<<Entity>Response> create(<Entity>CreateRequest request) { }
```

---

## Violation Response

```
❌ CACHING VIOLATION

Rule: [Rule ID] — [Description]
Location: [File:Method]
Found: [What was found]
Problem: [Why this is dangerous]
Fix: [Exact correction]

Impact: [Stale-data risk / performance issue / security risk]
```

---

## Enforcement Report Format

```
## Caching Governance Report

### Entity: [Name]
### On approved register: YES / NO
### Approved cache name: [name] / N/A

| Check              | Rules  | Passed | Failed |
|--------------------|--------|--------|--------|
| Eligibility        | 6      | ?      | ?      |
| Naming             | 4      | ?      | ?      |
| Placement          | 7      | ?      | ?      |
| Eviction           | 6      | ?      | ?      |
| Prohibited         | 7      | ?      | ?      |
| **TOTAL**          | **30** | **?**  | **?**  |

### Verdict: COMPLIANT / NON-COMPLIANT
```

---

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | Full layered-architecture compliance |
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | Error handling patterns |
| [`gov-validate-backend-feature`](../gov-validate-backend-feature/SKILL.md) | Master validation with scoring |
