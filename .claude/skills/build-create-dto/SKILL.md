---
name: build-create-dto
description: "Generates the complete DTO set: CreateRequest, UpdateRequest, Response, SearchRequest, UsageResponse, and OptionResponse when needed. Build step 3 — AFTER repository, BEFORE mapper. Enforces @Schema documentation, i18n validation keys, and immutability rules."
---

# Skill: build-create-dto

## Description
Generates the complete DTO set for one entity. This is **build step 3**. Creates EXACTLY the
required DTOs — no more, no less.

## When to Use
- After `build-create-repository` is complete
- BEFORE creating mapper, service, or controller

## When NOT to Use
- Before the entity and repository exist
- When the DTOs exist and a single field needs updating (edit directly)

---

## Variables

Inherits `<module>`, `<Entity>`, `<ENTITY_CLASS>`, `<base.package>` from
[`build-create-entity`](../build-create-entity/SKILL.md). Field names and types come from the
generated entity — never assume them.

## Responsibilities

- Generate `CreateRequest`, `UpdateRequest`, `Response`, `SearchRequest`, `UsageResponse`, and
  `OptionResponse` when the entity feeds dropdowns
- Apply `@Schema` documentation on every class and field
- Apply i18n validation message keys — never literal message text
- Enforce immutability: exclude `id` and audit fields from requests, exclude natural keys and
  FK references from `UpdateRequest`
- Make `SearchRequest` extend the shared search base type

## Constraints

- MUST NOT generate entity, repository, mapper, service, or controller code
- MUST NOT include a mutable natural key in `UpdateRequest` without explicit approval
- MUST NOT assume field types — derive them from the entity
- MUST NOT hardcode validation messages — i18n keys only

> **Scope note:** structural validation (`@NotBlank`, `@Size`, format checks) is not a Business
> Rule. It stays here on the DTO. Do not move it into `<Entity>Domain`.

## Output

5–6 files in `src/main/java/<base/package>/<module>/dto/`:

| # | DTO | Purpose |
|---|-----|---------|
| 1 | `<Entity>CreateRequest` | POST request body |
| 2 | `<Entity>UpdateRequest` | PUT request body — excludes immutable fields |
| 3 | `<Entity>Response` | All GET/POST/PUT responses |
| 4 | `<Entity>SearchRequest` | `POST /search` body |
| 5 | `<Entity>UsageResponse` | `GET /{id}/usage` response |
| 6 | `<Entity>OptionResponse` | *(only if used in dropdowns)* slim option DTO |

---

## Steps

### 1. CreateRequest
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "<English description> - <الوصف بالعربية>")
public class <Entity>CreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = <LENGTH>, message = "{validation.size}")
    @Schema(description = "<English> - <عربي>", example = "<EXAMPLE>")
    private String <fieldName>;

    @Schema(description = "<English> - <عربي>", example = "true")
    @Builder.Default
    private Boolean isActive = true;
}
```

### 2. UpdateRequest — excludes immutable fields
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "<English description> - <الوصف بالعربية>")
public class <Entity>UpdateRequest {

    // ❌ NO natural keys — they are IMMUTABLE
    // ❌ NO FK references — they are IMMUTABLE

    @Size(max = <LENGTH>, message = "{validation.size}")
    @Schema(description = "<English> - <عربي>", example = "<EXAMPLE>")
    private String <mutableField>;
}
```

### 3. Response — all fields + audit
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "<English description> - <الوصف بالعربية>")
public class <Entity>Response {

    @Schema(description = "Unique identifier - المعرف الفريد")
    private Long id;

    // ... every business field

    @Schema(description = "Active status - حالة التفعيل")
    private Boolean isActive;

    // Computed counts, if this is a parent entity
    @Schema(description = "Number of child records - عدد السجلات الفرعية")
    private Integer <child>Count;

    // Audit fields — ALWAYS included
    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
```

### 4. SearchRequest
```java
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@Schema(description = "Search request for <Entity> - طلب بحث")
public class <Entity>SearchRequest extends BaseSearchContractRequest {
    // Inherits filters, sorts, page, size
}
```

**Child variant** — excludes the parent-id filter from the generic search and exposes it
explicitly:
```java
@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@Schema(description = "Search request for <Child> - طلب بحث")
public class <Child>SearchRequest extends BaseSearchContractRequest {

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of("<parentIdFilter>"));
    }

    public Long get<Parent>Id() {
        return extractLongFilter("<parentIdFilter>");
    }
}
```

### 5. UsageResponse
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Usage information - معلومات الاستخدام")
public class <Entity>UsageResponse {

    private Long id;

    @Schema(description = "Number of child references - عدد المراجع الفرعية")
    private long <child>Count;

    @Schema(description = "Can be deleted - هل يمكن الحذف")
    private boolean canDelete;

    @Schema(description = "Can be deactivated - هل يمكن إلغاء التفعيل")
    private boolean canDeactivate;

    @Schema(description = "Reason if blocked - سبب الحظر")
    private String reason;
}
```

### 6. OptionResponse — slim, no audit fields
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Dropdown option - خيار القائمة")
public class <Entity>OptionResponse {
    private Long id;
    private String label;
    private String code;
    // NO audit fields
}
```

---

## Shared Layer Mandate

| # | Requirement | Shared class | Package |
|---|-------------|--------------|---------|
| SH.1 | Search input type already exists — do not redefine it | `SearchRequest` | `<base.package>.common.search` |
| SH.2 | Filter criteria type already exists | `SearchFilter` | `<base.package>.common.search` |
| SH.3 | API-layer search base type | `BaseSearchContractRequest` | `<base.package>.common.dto` |
| SH.4 | Response envelope — never a per-module wrapper | `ApiResponse` | `<base.package>.common.web` |
| SH.5 | Field-level validation error items | `FieldErrorItem` | `<base.package>.common.web` |

**Rules:**
- NEVER redefine the shared search/filter/operator types in a feature module
- NEVER create a custom API response wrapper
- Validation messages MUST be i18n keys resolved by the shared locale configuration

> After creating the DTOs, run [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md).

---

## Rules (STRICT)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.3.1 | All DTOs use `@Data @Builder @NoArgsConstructor @AllArgsConstructor` | YES |
| A.3.2 | Class-level `@Schema` with a bilingual description | YES |
| A.3.3 | Every field has `@Schema(description, example)` | YES |
| A.3.4 | Validation messages use i18n keys | YES |
| A.3.5 | `CreateRequest` excludes `id` and audit fields | YES |
| A.3.6 | `UpdateRequest` excludes immutable fields (natural keys, FKs) | YES |
| A.3.7 | `Response` includes all fields + audit fields + computed counts | YES |
| A.3.8 | Audit timestamps carry the project's `@JsonFormat` pattern in UTC | YES |
| A.3.9 | `SearchRequest` extends `BaseSearchContractRequest` | YES |
| A.3.10 | Child `SearchRequest` overrides `toCommonSearchRequest()` to exclude the parent id | YES |
| A.3.11 | Child `SearchRequest` exposes a parent-id extractor | YES |
| A.3.12 | `UsageResponse` carries `canDelete`/`canDeactivate` + reason | YES |
| A.3.13 | `OptionResponse` is slim — no audit fields | YES |

---

## Violations (MUST NOT)

- ❌ `id` present in `CreateRequest`
- ❌ Natural keys or FK references present in `UpdateRequest`
- ❌ Audit fields missing from `Response`
- ❌ Literal validation message text instead of an i18n key
- ❌ Missing `@Schema` on a class or field
- ❌ Missing `@JsonFormat` on `Instant` audit fields
- ❌ Re-implementing filter/sort parsing instead of extending the shared base type
- ❌ Reusing the full `Response` for dropdowns instead of `OptionResponse`
- ❌ `UsageResponse` without `canDelete`/`canDeactivate`
- ❌ `@SuperBuilder` on a non-search DTO
