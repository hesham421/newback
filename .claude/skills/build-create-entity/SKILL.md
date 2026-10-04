---
name: build-create-entity
description: "Generates a JPA entity class following the canonical pattern, plus its Domain companion object (business rules) when the entity has rules that answer \"is this operation allowed?\". Build step 1 — MUST be completed before any other backend artifact. Enforces AuditableEntity, @SuperBuilder, boolean converters, @Formula counts, DB naming conventions, and Domain construction via static factories."
---

# Skill: build-create-entity

## Description
Generates a JPA entity class following the project's canonical persistence pattern. This is
**build step 1** and MUST be completed before any other backend artifact.

## When to Use
- When implementing a new domain entity in any backend module
- BEFORE creating repository, DTO, mapper, service, or controller

## When NOT to Use
- When the entity already exists — edit the entity file directly
- When adding a single field to an existing entity (edit it; do not re-run this skill)
- When only DTOs, mapper, service, or controller change

---

## Variables (resolve ALL before generating)

Nothing below is hardcoded to a particular module or entity. Resolve every variable from the
module's own design documents (DB script, execution plan) — never guess, never carry a value
over from another module.

| Variable | Meaning | Example shape |
|----------|---------|---------------|
| `<module>` | Java sub-package under the project base package | lowercase, single word |
| `<MODULE_TABLE_PREFIX>` | DB table prefix for this module | 2–4 chars, UPPER |
| `<CLASS_PREFIX>` | Optional class-name prefix for this module's entities. **May be empty** — use one only if the module already established it | PascalCase |
| `<Entity>` | PascalCase entity name | — |
| `<ENTITY_CLASS>` | `<CLASS_PREFIX><Entity>` — the actual Java class name | — |
| `<ENTITY_TABLE>` | `<MODULE_TABLE_PREFIX>_<ENTITY_UPPER>` | UPPER_SNAKE_CASE |
| `<ENTITY_SEQ>` | Sequence name from the DB script. If the DB script declares `GENERATED ALWAYS AS IDENTITY` or "BLOCK 1 — SEQUENCES / none", it names no sequence — use `SEQ_<ENTITY_TABLE>` and create it in the module's Flyway migration (see step 2) | UPPER_SNAKE_CASE |
| `<ENTITY_PK_COLUMN>` | PK column name **as defined in the DB script** — entity-specific, never generic `ID` | UPPER_SNAKE_CASE |
| `<Parent>` / `<PARENT_FK_COLUMN>` | *(optional)* parent entity and its FK column, if this is a child | — |
| `<base.package>` | Project base package | e.g. the value of `groupId` in `pom.xml` |

> If a variable cannot be resolved from the DB script or execution plan, STOP and ask.
> Never invent a column, table, or sequence name.

## Responsibilities

- Generate a JPA entity extending `AuditableEntity`
- Define the DB mapping: table, columns, constraints, indexes, sequence
- Map boolean fields through the project's boolean converter
- Add FK relationships (`@ManyToOne` LAZY) if this is a child entity
- Add child collections (`@OneToMany`) and `@Formula` counts if this is a parent entity
- Implement `@PrePersist`/`@PreUpdate` hooks for key normalization
- Provide `activate()`/`deactivate()` helpers — pure field mutation, no guard logic
- Generate the entity's Domain companion object when the entity has Business Rules that answer
  "is this operation allowed?" — see "Domain Companion Object" below. An entity with no such
  rules (a pure reference/lookup table) needs no Domain companion.

## Constraints

- MUST NOT generate repository, DTO, mapper, service, or controller code
- MUST NOT modify other existing entity files
- MUST NOT assume a missing variable value — require it before generating
- MUST NOT apply uppercase normalization outside `@PrePersist`/`@PreUpdate`
- MUST NOT use `@Builder` — always `@SuperBuilder`, due to `AuditableEntity` inheritance

## Output

- `src/main/java/<base/package>/<module>/entity/<ENTITY_CLASS>.java`
- `src/main/java/<base/package>/<module>/domain/<Entity>Domain.java` — only when the entity has
  Business Rules requiring Business Decision ownership

---

## Steps

### 1. Class-level setup
```java
@Entity
@Table(name = "<ENTITY_TABLE>",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_<ENTITY_TABLE>_<DESC>", columnNames = {"<COLUMN>"})
    },
    indexes = {
        @Index(name = "IDX_<ENTITY_TABLE>_<COLUMN>", columnList = "<COLUMN>")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class <ENTITY_CLASS> extends AuditableEntity {
```

### 2. Primary key
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "<entity>_seq")
@SequenceGenerator(name = "<entity>_seq", sequenceName = "<ENTITY_SEQ>", allocationSize = 1)
@Column(name = "<ENTITY_PK_COLUMN>") // from the DB script — never a generic "ID"
private Long id;
```

> **When the DB script says `GENERATED ALWAYS AS IDENTITY`, this skill still wins.** Some
> generated `db-script-*.md` files declare identity columns and a "BLOCK 1 — SEQUENCES / none"
> block. Ignore that: the entity still uses `GenerationType.SEQUENCE` with
> `@SequenceGenerator(..., sequenceName = "SEQ_<ENTITY_TABLE>", allocationSize = 1)`, and the
> module's own Flyway migration creates those sequences instead of identity columns. This is
> settled — see `governance/shared/platform/rules/GOVERNANCE-RULES.md` → "Convention Precedence — db-script vs
> skills". Do not re-derive or re-argue it per module.

### 3. Business fields
```java
@NotBlank(message = "{validation.required}")
@Size(max = <LENGTH>, message = "{validation.size}")
@Column(name = "<COLUMN_NAME>", length = <LENGTH>, nullable = false)
private String <fieldName>;
```

### 4. Boolean fields
```java
@Column(name = "<ACTIVE_COLUMN>", nullable = false)
@Builder.Default
@Convert(converter = BooleanNumberConverter.class) // numeric column. CHAR(1) -> BooleanCharYNConverter; native BOOLEAN -> omit @Convert entirely
private Boolean isActive = Boolean.TRUE;
```

> Pick the mapping from the DB script's actual column type. Do not default to one without
> checking:
>
> | DB column type | Mapping |
> |----------------|---------|
> | native `BOOLEAN` | plain `Boolean` field — **no `@Convert` at all** |
> | numeric (`SMALLINT` / `NUMBER`) | `@Convert(converter = BooleanNumberConverter.class)` |
> | `CHAR(1)` | `@Convert(converter = BooleanCharYNConverter.class)` |
>
> A converter on an already-boolean column is a defect, not compliance. See
> `governance/shared/platform/rules/GOVERNANCE-RULES.md` → "Convention Precedence — db-script vs skills".

### 5. FK relationship (child entity only)
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "<PARENT_FK_COLUMN>", nullable = false,
    foreignKey = @ForeignKey(name = "FK_<ENTITY_TABLE>_<PARENT_REF>"))
private <CLASS_PREFIX><Parent> <parentField>;
```

### 6. Child collection + computed count (parent entity only)
```java
@OneToMany(mappedBy = "<parentField>", cascade = CascadeType.ALL,
           orphanRemoval = false, fetch = FetchType.LAZY)
private List<<CLASS_PREFIX><Child>> <children> = new ArrayList<>();

@Formula("(SELECT COUNT(*) FROM <CHILD_TABLE> c WHERE c.<PARENT_FK_COLUMN> = <ENTITY_PK_COLUMN>)")
private Integer <child>Count;
```

### 7. Lifecycle hooks
```java
@PrePersist
protected void onCreate() {
    if (isActive == null) {
        isActive = Boolean.TRUE;
    }
    if (<naturalKeyField> != null) {
        <naturalKeyField> = <naturalKeyField>.toUpperCase();
    }
}

@PreUpdate
protected void onUpdate() {
    if (<naturalKeyField> != null) {
        <naturalKeyField> = <naturalKeyField>.toUpperCase();
    }
}
```

### 8. Helper methods
```java
public void activate() {
    this.isActive = Boolean.TRUE;
}

public void deactivate() {
    this.isActive = Boolean.FALSE;
}
```

---

## Shared Layer Mandate

Consume the project's shared persistence layer — do NOT reinvent any of it:

| # | Requirement | Shared class | Package |
|---|-------------|--------------|---------|
| SH.1 | Extend `AuditableEntity` for audit fields | `AuditableEntity` | `<base.package>.common.domain` |
| SH.2 | Map **numeric / `CHAR(1)`** boolean columns through the project's converter — never write your own (a native `BOOLEAN` column needs none) | `BooleanNumberConverter` / `BooleanCharYNConverter` | `<base.package>.common.converter` |
| SH.3 | Audit fields are auto-populated — never set them manually | `AuditEntityListener` | `<base.package>.common.audit` |
| SH.4 | Use `@SuperBuilder`, never `@Builder` | — | Lombok |

**SH.1 exemption:** a short-lived session/security artifact with its own lifecycle fields
(issued-at / expires-at / revoked) need not extend `AuditableEntity`. Declare the exemption
explicitly in the module's design docs before relying on it.

**Rules:**
- NEVER create a custom audit base class
- NEVER create a custom boolean converter
- NEVER set `createdAt`/`createdBy`/`updatedAt`/`updatedBy` manually
- NEVER add a tenant column unless the project is actually multi-tenant

> After creating the entity, run [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md).

---

## Domain Companion Object (Business Rules)

The entity file above is **persistence-only**. Any Business Rule that answers
**"is this operation allowed?"** — deactivation guards, immutability enforcement, uniqueness
preconditions, state-transition validation, cycle prevention — does not belong on the entity.
It belongs on a separate Domain object.

### The Decision Test

Before writing any conditional, ask: *does this code decide whether an operation is permitted?*
If yes → it belongs in `<Entity>Domain`, not in the entity, the mapper, the service body, or
the controller.

### Package

Default: `<base.package>.<module>.domain`. Before generating, check whether the module already
uses `<module>.domain` for something else (some modules place JPA entities there). If it does,
stop and confirm the intended package rather than mixing two different concerns under one name.

### Rules (STRICT — mirrors `gov-enforce-backend-contract` LAYER 0)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.0.1 | A dedicated `<Entity>Domain` exists for every entity with rules answering "is this allowed?" | YES |
| A.0.2 | `<Entity>Domain` carries no Spring or JPA annotations | YES |
| A.0.3 | `<Entity>Domain` never accesses a Repository or the database | YES |
| A.0.4 | `<Entity>Domain` throws `LocalizedException` for every rule violation | YES |
| A.0.5 | `<Entity>Domain` is constructed only via static factories `create(...)` / `from(...)` | YES |
| A.0.6 | `<Entity>Domain` never imports or calls another module — cross-module data is passed in | YES |
| A.0.7 | At most one Domain object per entity; a Domain Service only when a rule genuinely spans several entities | YES |

### Responsibilities

- Static factory `create(...)` — runs construction-time validations, returns a valid instance or
  throws `LocalizedException`
- Static factory `from(<ENTITY_CLASS> entity)` — reconstructs a Domain view over a persisted
  entity, to evaluate a rule before mutating it
- Guard methods taking primitives or the entity itself (counts, flags, already-fetched sibling
  data) and throwing `LocalizedException` on violation
- Division of labour on state transitions: the Domain object **decides**; the entity's
  `activate()`/`deactivate()` **executes**; the service calls the Domain object first

### Violations (MUST NOT)

- ❌ `<Entity>Domain` annotated with `@Component` / `@Service` / `@Entity`
- ❌ A Repository field or constructor parameter on `<Entity>Domain`
- ❌ A public constructor used from outside the class instead of `create()` / `from()`
- ❌ A business rule left inline in the service instead of expressed here
- ❌ Skipping this section for an entity that has guard rules in its execution plan

### Shape

```java
public final class <Entity>Domain {

    private final String <naturalKeyField>;
    private final boolean active;

    private <Entity>Domain(String <naturalKeyField>, boolean active) {
        this.<naturalKeyField> = <naturalKeyField>;
        this.active = active;
    }

    public static <Entity>Domain create(String <naturalKeyField>, boolean keyAlreadyTaken) {
        if (<naturalKeyField> == null || <naturalKeyField>.isBlank()) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                <Module>ErrorCodes.<ENTITY>_KEY_REQUIRED);
        }
        if (keyAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                <Module>ErrorCodes.<ENTITY>_KEY_DUPLICATE, <naturalKeyField>);
        }
        return new <Entity>Domain(<naturalKeyField>, true);
    }

    public static <Entity>Domain from(<ENTITY_CLASS> entity) {
        return new <Entity>Domain(entity.get<NaturalKeyField>(),
                                  Boolean.TRUE.equals(entity.getIsActive()));
    }

    // Decision only — the service calls entity.deactivate() after this returns
    public void assertCanDeactivate(long activeChildCount) {
        if (activeChildCount > 0) {
            throw new LocalizedException(Status.CONFLICT,
                <Module>ErrorCodes.<ENTITY>_ACTIVE_CHILDREN_EXIST);
        }
    }
}
```

> **Status choice:** "cannot deactivate/delete because a referencing record exists" is
> `Status.CONFLICT` (409). `Status.BUSINESS_RULE_VIOLATION` (422) is for invariant violations
> that are not about a specific referencing record — invalid state transitions, cycle detection.

---

## Rules (STRICT)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.1.1 | Extends `AuditableEntity` — except a declared session-artifact exemption (see SH.1) | YES |
| A.1.2 | PK `@Column` name comes from the DB script — never generic `ID` or `ID_PK` | YES |
| A.1.3 | PK uses `GenerationType.SEQUENCE` with an explicit `@SequenceGenerator` — **even when the DB script declares `GENERATED ALWAYS AS IDENTITY`**; use `sequenceName = "SEQ_<TABLE>"` and create it in the module's migration | YES |
| A.1.4 | `allocationSize = 1` on `@SequenceGenerator` | YES |
| A.1.5 | FK columns end with the project's FK suffix, consistently | YES |
| A.1.6 | Boolean mapping matches the DB column type: native `BOOLEAN` → plain `Boolean`, **no converter**; numeric → `BooleanNumberConverter`; `CHAR(1)` → `BooleanCharYNConverter` | YES |
| A.1.7 | Boolean default via `@Builder.Default` | YES |
| A.1.8 | Every `@ManyToOne` uses `fetch = FetchType.LAZY` | YES |
| A.1.9 | `@OneToMany` uses `cascade = ALL, orphanRemoval = false, fetch = LAZY` | YES |
| A.1.10 | Uses `@SuperBuilder`, not `@Builder` | YES |
| A.1.11 | Table name is UPPER_SNAKE_CASE with the module prefix | YES |
| A.1.12 | `@UniqueConstraint` and `@Index` declared inside `@Table` | YES |
| A.1.13 | Unique constraints named `UQ_<TABLE>_<DESC>` | YES |
| A.1.14 | Indexes named `IDX_<TABLE>_<COLUMN>` | YES |
| A.1.15 | FK constraints named `FK_<TABLE>_<REF>` via `@ForeignKey(name)` | YES |
| A.1.16 | Computed counts use `@Formula`, never collection `.size()` | YES |
| A.1.17 | `@PrePersist`/`@PreUpdate` are the sole location for normalization | YES |
| A.1.18 | Entity has `activate()` and `deactivate()` helpers | YES |
| A.1.19 | No helper methods iterating or filtering lazy `@OneToMany` collections — use repository count queries | YES |

> Constraint and index names (A.1.12–A.1.15) are taken **verbatim from the module's db-script** — they are physical database objects and must match the migration exactly.

---

## Violations (MUST NOT)

- ❌ `@Builder` instead of `@SuperBuilder`
- ❌ `GenerationType.IDENTITY` or `GenerationType.AUTO` — including when the DB script declares an identity column; the entity uses `SEQUENCE` and the migration creates `SEQ_<TABLE>`
- ❌ `allocationSize` other than 1
- ❌ A generic or invented PK column name instead of the DB script's
- ❌ FK columns not following the project's FK suffix convention
- ❌ `boolean` primitive for a boolean field — must be the `Boolean` wrapper
- ❌ `fetch = FetchType.EAGER` on any relationship
- ❌ `orphanRemoval = true` without explicit approval
- ❌ Normalization performed in the mapper or the service
- ❌ `entity.setIsActive(...)` in a service — must use `activate()` / `deactivate()`
- ❌ Declaring audit fields directly instead of inheriting them
- ❌ `entity.getChildren().size()` instead of `@Formula`
- ❌ Entity helpers that stream/filter a lazy collection
- ❌ Lowercase or camelCase table names
