---
name: build-create-mapper
description: "Generates the @Component entity-to-DTO mapper. Build step 4 — AFTER DTOs, BEFORE service. Enforces toEntity, updateEntityFromRequest (void, skips immutables), null-safe boolean mapping, toUsageResponse, and manual mapping over MapStruct."
---

# Skill: build-create-mapper

## Description
Generates the entity-to-DTO mapper. This is **build step 4**. One mapper per entity, manual
mapping — no annotation-based mapping framework.

## When to Use
- After `build-create-dto` is complete
- BEFORE creating the service or controller

## When NOT to Use
- Before the DTOs exist
- When the mapper exists and a single method needs updating (edit directly)
- For MapStruct or any annotation-driven mapping — this project uses manual mappers only

---

## Variables

Inherits `<module>`, `<Entity>`, `<ENTITY_CLASS>`, `<base.package>` from
[`build-create-entity`](../build-create-entity/SKILL.md).

## Responsibilities

- Generate a `@Component` mapper with manual mapping
- `toEntity()` — CreateRequest → entity
- `updateEntityFromRequest()` — void, mutates in place, skips immutable fields
- `toResponse()` — entity → Response, null-safe boolean mapping
- `toUsageResponse()` — entity + counts → usage/dependency view
- Handle null input gracefully in every method

## Constraints

- MUST NOT generate entity, repository, DTO, service, or controller code
- MUST NOT set FK relationships in `toEntity()` for a root entity — the service resolves those
- MUST NOT apply case normalization — the entity's `@PrePersist` owns that
- MUST NOT update natural keys or FK references in `updateEntityFromRequest()`
- MUST NOT call a repository or a service — mappers are pure

## Output

- `src/main/java/<base/package>/<module>/mapper/<Entity>Mapper.java`

---

## Steps

### 1. Class declaration
```java
@Component
public class <Entity>Mapper {
```

### 2. toEntity — root entity
```java
public <ENTITY_CLASS> toEntity(<Entity>CreateRequest request) {
    if (request == null) return null;
    return <ENTITY_CLASS>.builder()
            .<fieldName>(request.get<FieldName>())   // NOT .toUpperCase() — @PrePersist owns it
            .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
            .build();
}
```

### 3. toEntity — child entity (compile-time FK safety)
```java
public <CHILD_CLASS> toEntity(<Child>CreateRequest request, <PARENT_CLASS> parent) {
    if (request == null) return null;
    return <CHILD_CLASS>.builder()
            .<fieldName>(request.get<FieldName>())
            .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
            .<parentField>(parent)   // FK set at compile time — the caller cannot forget it
            .build();
}
```

### 4. updateEntityFromRequest — mutate in place
```java
public void updateEntityFromRequest(<ENTITY_CLASS> entity, <Entity>UpdateRequest request) {
    if (entity == null || request == null) return;
    // ❌ NEVER update natural keys
    // ❌ NEVER update FK references
    entity.set<MutableField>(request.get<MutableField>());
}
```

### 5. toResponse
```java
public <Entity>Response toResponse(<ENTITY_CLASS> entity) {
    if (entity == null) return null;
    return <Entity>Response.builder()
            .id(entity.getId())
            .<fieldName>(entity.get<FieldName>())
            .isActive(Boolean.TRUE.equals(entity.getIsActive()))
            .<child>Count(entity.get<Child>Count() != null ? entity.get<Child>Count() : 0)
            // Audit fields — ALWAYS mapped
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
}
```

### 6. toOptionResponse (if the entity feeds dropdowns)
```java
public <Entity>OptionResponse toOptionResponse(<ENTITY_CLASS> entity) {
    if (entity == null) return null;
    return <Entity>OptionResponse.builder()
            .id(entity.getId())
            .label(entity.get<LabelField>())
            .code(entity.get<CodeField>())
            .build();
}
```

### 7. toUsageResponse
```java
public <Entity>UsageResponse toUsageResponse(<ENTITY_CLASS> entity, long <child>Count) {
    if (entity == null) return null;
    boolean canDelete = <child>Count == 0;
    boolean canDeactivate = <child>Count == 0;   // or based on ACTIVE children only
    return <Entity>UsageResponse.builder()
            .id(entity.getId())
            .<child>Count(<child>Count)
            .canDelete(canDelete)
            .canDeactivate(canDeactivate)
            .reason(!canDelete ? "<reason text or message key>" : null)
            .build();
}
```

---

## Shared Layer Mandate

| # | Requirement | Convention | Why |
|---|-------------|------------|-----|
| SH.1 | Boolean mapping uses `Boolean.TRUE.equals(...)` | Null-safe unboxing | Avoids `NullPointerException` |
| SH.2 | No case normalization in the mapper | Entity `@PrePersist` owns it | One canonical location |
| SH.3 | FK relationships not resolved in the mapper | The service looks them up | Mappers stay query-free |
| SH.4 | Date/time formatting uses the shared timestamp utility | `TimestampUtils` (`<base.package>.common.util`) | Consistent ISO-8601 output |
| SH.5 | Audit fields read from the inherited getters | `AuditableEntity` | They are listener-managed |

**Rules:**
- NEVER format dates by hand
- NEVER duplicate boolean null-safety patterns
- NEVER set audit fields in a mapper

> After creating the mapper, run [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md).

---

## Rules (STRICT)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.4.1 | One `@Component` mapper per entity | YES |
| A.4.2 | Child `toEntity()` accepts the parent entity as a parameter | YES |
| A.4.3 | `updateEntityFromRequest()` returns `void` and mutates in place | YES |
| A.4.4 | `updateEntityFromRequest()` skips natural keys and FK references | YES |
| A.4.5 | `toResponse()` maps booleans with `Boolean.TRUE.equals(...)` | YES |
| A.4.6 | Every mapper method handles `null` input by returning null / no-op | YES |
| A.4.7 | `toUsageResponse()` computes eligibility from counts, never hardcoded | YES |

---

## Violations (MUST NOT)

- ❌ Inline mapping in a service or controller instead of using the mapper
- ❌ A mapper calling a repository or a service
- ❌ A mapper applying case normalization
- ❌ A child `toEntity()` that relies on the service to set the FK afterwards
- ❌ `updateEntityFromRequest()` returning a new entity instead of mutating
- ❌ Updating an immutable field in `updateEntityFromRequest()`
- ❌ Unboxing `getIsActive()` directly without `Boolean.TRUE.equals(...)`
- ❌ Missing null checks
- ❌ Hardcoded `canDelete = true` instead of deriving it from counts
- ❌ Using MapStruct — manual mapping is the project standard
- ❌ Audit fields missing from `toResponse()`
- ❌ A conditional encoding a business decision rather than a field transformation — that
  belongs in `<Entity>Domain`
