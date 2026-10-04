---
name: build-create-controller
description: "Generates a thin @RestController delegating ALL logic to the service. Build step 6 — the final implementation step. Enforces the shared response-crafting helper, @Valid, @Operation, zero business logic, 204 on delete, POST /search, and separate activate/deactivate endpoints."
---

# Skill: build-create-controller

## Description
Generates a thin REST controller that delegates ALL logic to the service layer. This is
**build step 6** — the final implementation step.

## When to Use
- After `build-create-service` is complete

## When NOT to Use
- Before the service exists — the controller depends on its method signatures
- When the controller exists and one endpoint needs modification (edit directly)
- To add business logic — controllers are pure delegation

## Prerequisites
- Service with all operations returning `ServiceResult<T>` (except `delete()`)
- The shared response-crafting bean available for injection
- All DTOs defined

---

## Variables

Inherits `<module>`, `<Entity>`, `<base.package>` from
[`build-create-entity`](../build-create-entity/SKILL.md), plus `<entity-url>` — the kebab-case
plural URL segment for this entity.

## Responsibilities

- Generate a thin `@RestController` delegating everything to the service
- Wrap every non-delete response through the shared response-crafting helper
- Apply `@Valid` on every `@RequestBody`
- Apply `@Operation` on every endpoint
- Use `@ResponseStatus(HttpStatus.NO_CONTENT)` ONLY on delete
- ZERO business logic

## Constraints

- MUST NOT generate any other layer's code
- MUST NOT inject a repository or a mapper — only service(s) + the response helper
- MUST NOT reference entity classes — DTOs only
- MUST NOT use `@ResponseStatus(CREATED)` — 201 is derived from the service's `Status`
- MUST NOT contain business logic, validation, or conditional branching

## Output

- `src/main/java/<base/package>/<module>/controller/<Entity>Controller.java`

---

## Steps

### 1. Class declaration
```java
@RestController
@RequestMapping("/api/<module>/<entity-url>")
@RequiredArgsConstructor
@Tag(name = "<Entity> Management", description = "<English> - <عربي>")
public class <Entity>Controller {

    private final <Entity>Service service;
    private final OperationCode operationCode;
```

### 2. Create
```java
@PostMapping
@Operation(summary = "Create <Entity>", description = "<عربي>")
public ResponseEntity<ApiResponse<<Entity>Response>> create(
        @Valid @RequestBody <Entity>CreateRequest request) {
    return operationCode.craftResponse(service.create(request));
}
```

### 3. Update
```java
@PutMapping("/{id}")
@Operation(summary = "Update <Entity>", description = "<عربي>")
public ResponseEntity<ApiResponse<<Entity>Response>> update(
        @PathVariable Long id,
        @Valid @RequestBody <Entity>UpdateRequest request) {
    return operationCode.craftResponse(service.update(id, request));
}
```

### 4. Get by id
```java
@GetMapping("/{id}")
@Operation(summary = "Get <Entity> by ID", description = "<عربي>")
public ResponseEntity<ApiResponse<<Entity>Response>> getById(@PathVariable Long id) {
    return operationCode.craftResponse(service.getById(id));
}
```

### 5. Search — POST, not GET
```java
@PostMapping("/search")
@Operation(summary = "Search <Entity>", description = "<عربي>")
public ResponseEntity<ApiResponse<Page<<Entity>Response>>> search(
        @Valid @RequestBody <Entity>SearchRequest searchRequest) {
    return operationCode.craftResponse(service.search(searchRequest));
}
```

### 6. Activate / deactivate — separate endpoints
```java
@PutMapping("/{id}/activate")
@Operation(summary = "Activate <Entity>", description = "<عربي>")
public ResponseEntity<ApiResponse<<Entity>Response>> activate(@PathVariable Long id) {
    return operationCode.craftResponse(service.activate(id));
}

@PutMapping("/{id}/deactivate")
@Operation(summary = "Deactivate <Entity>", description = "<عربي>")
public ResponseEntity<ApiResponse<<Entity>Response>> deactivate(@PathVariable Long id) {
    return operationCode.craftResponse(service.deactivate(id));
}
```

### 7. Delete — 204, void
```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@Operation(summary = "Delete <Entity>", description = "<عربي>")
public void delete(@PathVariable Long id) {
    service.delete(id);
}
```

### 8. Usage
```java
@GetMapping("/{id}/usage")
@Operation(summary = "Get <Entity> usage", description = "<عربي>")
public ResponseEntity<ApiResponse<<Entity>UsageResponse>> getUsage(@PathVariable Long id) {
    return operationCode.craftResponse(service.getUsage(id));
}
```

### 9. Child endpoints — same controller, nested path
```java
@PostMapping("/<children-url>")
@Operation(summary = "Create <Child>", description = "<عربي>")
public ResponseEntity<ApiResponse<<Child>Response>> create<Child>(
        @Valid @RequestBody <Child>CreateRequest request) {
    return operationCode.craftResponse(childService.create(request));
}

@PostMapping("/<children-url>/search")
public ResponseEntity<ApiResponse<Page<<Child>Response>>> search<Children>(
        @Valid @RequestBody <Child>SearchRequest searchRequest) {
    return operationCode.craftResponse(childService.search(searchRequest));
}

@PutMapping("/<children-url>/{id}")
public ResponseEntity<ApiResponse<<Child>Response>> update<Child>(
        @PathVariable Long id,
        @Valid @RequestBody <Child>UpdateRequest request) {
    return operationCode.craftResponse(childService.update(id, request));
}

@DeleteMapping("/<children-url>/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete<Child>(@PathVariable Long id) {
    childService.delete(id);
}
```

---

## Shared Layer Mandate

| # | Requirement | Shared class | Package |
|---|-------------|--------------|---------|
| SH.1 | Response mapping via the shared crafting helper | `OperationCode` | `<base.package>.common.web` |
| SH.2 | Envelope is the shared `ApiResponse<T>`, applied automatically | `ApiResponse` | `<base.package>.common.web` |
| SH.3 | Exceptions handled centrally — never caught in a controller | `GlobalExceptionHandler` | `<base.package>.common.web` |
| SH.4 | Pagination constraints enforced by the shared utility | `PageableUtils` | `<base.package>.common.web.util` |
| SH.5 | JSON serialization configured globally — no local `ObjectMapper` | shared Jackson config | `<base.package>.common.web.config` |

**Rules:**
- NEVER create a custom response wrapper
- NEVER catch exceptions in a controller
- NEVER use `@ResponseStatus(CREATED)` — 201 comes from the service's `Status.CREATED`
- NEVER configure a local `ObjectMapper`
- NEVER implement pagination validation by hand

> After creating the controller, run [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md).

---

## Rules (STRICT)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.6.1 | `@RestController @RequestMapping @RequiredArgsConstructor` | YES |
| A.6.2 | `@Tag` with a bilingual description | YES |
| A.6.3 | Injects ONLY service(s) + the response helper | YES |
| A.6.4 | Non-delete endpoints return the crafted `ResponseEntity<ApiResponse<T>>` | YES |
| A.6.5 | Delete: `@ResponseStatus(NO_CONTENT)` + `void` | YES |
| A.6.6 | Search: `POST /search` with `@RequestBody` — not `GET` with `@ModelAttribute` | YES |
| A.6.7 | Separate `activate` and `deactivate` endpoints — not one toggle | YES |
| A.6.8 | Usage exposed at `GET /{id}/usage` | YES |
| A.6.9 | Child endpoints live under the same controller | YES |
| A.6.10 | Every method has `@Operation(summary, description)` | YES |
| A.6.11 | Every request body is `@Valid @RequestBody` | YES |
| A.6.12 | ZERO business logic — pure delegation | YES |

### HTTP status mapping (automatic)

| Service `Status` | HTTP | How |
|------------------|------|-----|
| `Status.CREATED` | 201 | Automatic through the response helper |
| `Status.UPDATED` | 200 | Automatic through the response helper |
| `Status.SUCCESS` | 200 | Automatic through the response helper |
| delete (`void`) | 204 | `@ResponseStatus(NO_CONTENT)` on the method |

---

## Violations (MUST NOT)

- ❌ Injecting a repository, mapper, or entity into a controller
- ❌ Any conditional, validation, or transformation in a controller
- ❌ Returning a raw DTO instead of the crafted response
- ❌ `@ResponseStatus(CREATED)` on POST
- ❌ Wrapping `delete()` in a `ServiceResult` or crafted response
- ❌ `GET /search` with `@ModelAttribute`
- ❌ A single toggle endpoint instead of separate activate/deactivate
- ❌ A separate controller for a child entity
- ❌ Missing `@Valid` on a request body
- ❌ Missing `@Operation` on an endpoint
- ❌ Any code answering "is this operation allowed?" — that belongs in `<Entity>Domain`,
  reached through the service
