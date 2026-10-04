---
name: build-create-repository
description: "Generates a JPA repository interface extending JpaRepository + JpaSpecificationExecutor. Build step 2 — AFTER entity, BEFORE DTOs. Enforces existsBy checks, JOIN FETCH queries, JPQL count queries, and module-internal scope."
---

# Skill: build-create-repository

## Description
Generates a JPA repository interface following the project's canonical pattern. This is
**build step 2** — created AFTER the entity, BEFORE the DTOs.

## When to Use
- After `build-create-entity` is complete
- BEFORE creating DTOs, mapper, service, or controller

## When NOT to Use
- Before the entity class exists — `build-create-entity` must run first
- When the repository exists and only needs a method added (edit it directly)

---

## Variables

Inherits every variable resolved by [`build-create-entity`](../build-create-entity/SKILL.md) —
`<module>`, `<Entity>`, `<ENTITY_CLASS>`, `<base.package>`. Derive field and column names from
the entity that was actually generated; never assume them.

## Responsibilities

- Generate an interface extending `JpaRepository` and `JpaSpecificationExecutor`
- Define `existsBy<Field>()` methods for uniqueness checks
- Define `existsBy<Field>AndIdNot()` ONLY when that field is mutable on update
- Add count queries if this is a parent entity
- Add `JOIN FETCH` queries where a child needs its parent loaded

## Constraints

- MUST NOT generate entity, DTO, mapper, service, or controller code
- MUST NOT modify existing repository files unless asked
- MUST NOT assume entity field names — derive them from the entity
- MUST NOT add `existsBy<Field>AndIdNot()` for an immutable natural key (dead code by design)
- Repository is module-internal — MUST NOT be designed for cross-module use

## Output

- `src/main/java/<base/package>/<module>/repository/<Entity>Repository.java`

---

## Cross-Module Access

This repository is injected ONLY inside its own module's service. If another module needs this
data, it consumes it through this module's own cross-module interface — see
[`build-create-service`](../build-create-service/SKILL.md)'s "Cross-Module Calls" section.

Do NOT add methods here to make the repository "easier to reuse" from another module, and do
NOT inject it from another module. Module boundaries are package-based and enforced by the
project's ArchUnit suite, not by repository design.

---

## Steps

### 1. Interface declaration
```java
@Repository
public interface <Entity>Repository
    extends JpaRepository<<ENTITY_CLASS>, Long>,
            JpaSpecificationExecutor<<ENTITY_CLASS>> {
```

### 2. Standard finders
```java
Optional<<ENTITY_CLASS>> findBy<UniqueField>(<Type> value);

boolean existsBy<UniqueField>(<Type> value);

// ONLY if <UniqueField> is mutable on update — otherwise omit
boolean existsBy<UniqueField>AndIdNot(<Type> value, Long id);
```

### 3. Paginated queries
```java
Page<<ENTITY_CLASS>> findBy<Filter>(<Type> value, Pageable pageable);
```

### 4. Count queries (parent entity)
```java
@Query("SELECT COUNT(c) FROM <CHILD_CLASS> c WHERE c.<parentField>.id = :entityId")
long count<Children>(@Param("entityId") Long entityId);

@Query("SELECT COUNT(c) FROM <CHILD_CLASS> c WHERE c.<parentField>.id = :entityId AND c.isActive = true")
long countActive<Children>(@Param("entityId") Long entityId);
```

### 5. JOIN FETCH queries (child entity)
```java
@Query(value = "SELECT c FROM <CHILD_CLASS> c JOIN FETCH c.<parentField> WHERE c.<parentField>.id = :parentId",
       countQuery = "SELECT COUNT(c) FROM <CHILD_CLASS> c WHERE c.<parentField>.id = :parentId")
Page<<CHILD_CLASS>> searchByParentId(@Param("parentId") Long parentId, Pageable pageable);
```

### 6. Projections for read-only multi-table reads
```java
public interface <Entity>SummaryView {
    Long getId();
    String get<Field>();
}
```

---

## Shared Layer Mandate

Search, paging and sort validation are provided by the project's shared search layer — do NOT
reinvent any of it:

| # | Requirement | Shared class | Package |
|---|-------------|--------------|---------|
| SH.1 | Specifications built via `SpecBuilder` — never a hand-written `Specification<E>` | `SpecBuilder` | `<base.package>.common.search` |
| SH.2 | Pageables built via `PageableBuilder.from()` with sort-field validation | `PageableBuilder` | `<base.package>.common.search` |
| SH.3 | Allowed sort/filter fields declared via the shared allowed-fields type | `SetAllowedFields` | `<base.package>.common.search` |
| SH.4 | Active-flag filtering uses the shared helper | `ActiveFlagQueryHelper` | `<base.package>.common.search` |
| SH.5 | Boolean values in search specs go through the shared converter | `BooleanFieldValueConverter` | `<base.package>.common.search` |

**Rules:**
- NEVER build a `Specification<E>` manually
- NEVER build a `Pageable` manually
- NEVER write custom sort-field validation

> After creating the repository, run [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md).

---

## Rules (STRICT)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.2.1 | Extends `JpaRepository` AND `JpaSpecificationExecutor` | YES |
| A.2.2 | Annotated with `@Repository` | YES |
| A.2.3 | Never injected outside its own module | YES |
| A.2.4 | Existence checks use `boolean existsBy<Field>(...)` | YES |
| A.2.5 | Update uniqueness uses `existsBy<Field>AndIdNot(value, id)` — only for mutable fields | YES |
| A.2.6 | Child queries use `JOIN FETCH` to avoid N+1 | YES |
| A.2.7 | Reference checks use JPQL `@Query("SELECT COUNT(...)")` | YES |
| A.2.8 | Projection interfaces used for read-only multi-table queries | YES |
| A.2.9 | No dead code — every method has at least one caller in a service | YES |

---

## Violations (MUST NOT)

- ❌ Missing `JpaSpecificationExecutor` — blocks search/filter functionality
- ❌ Missing `@Repository`
- ❌ `findBy(...).isPresent()` for an existence check — use `existsBy<Field>()`
- ❌ Update uniqueness check that does not exclude the current id
- ❌ Derived queries that navigate paths and cause N+1 — use `JOIN FETCH`
- ❌ Loading a full collection just to count it — use a count query
- ❌ Injecting this repository in another module's service
- ❌ Returning full entities when a projection would do
- ❌ Repository methods never called by any service
- ❌ `existsBy<Field>AndIdNot()` for a field that is immutable on update
- ❌ A query predicate that decides a business outcome (an "is eligible" filter) — repositories
  fetch and count; the decision belongs to `<Entity>Domain`
