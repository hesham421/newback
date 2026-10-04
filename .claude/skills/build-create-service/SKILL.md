---
name: build-create-service
description: "Generates the @Service orchestration layer: CRUD, search, activate, deactivate, getUsage. Owns transactions, authorization, caching and logging; delegates every business rule to the entity's Domain object. Build step 5. Enforces @PreAuthorize, @Transactional, ServiceResult<T>, LocalizedException, shared search builders, and the cross-module boundary."
---

# Skill: build-create-service

## Description
Generates the service class for one entity. This is **build step 5**. The service is the
ORCHESTRATION layer: it loads data, delegates business decisions to the Domain object, manages
transactions, enforces authorization, and persists results. It does NOT own business rules.

## When to Use
- After entity, repository, DTOs, mapper, error codes and permission constants all exist
- BEFORE creating the controller

## When NOT to Use
- Before its prerequisites exist
- When the service exists and one method needs modification (edit directly)

## Prerequisites
- Entity with `activate()`/`deactivate()` helpers, and its `<Entity>Domain` if it has guard rules
- Repository with the needed `existsBy*` and count queries
- All DTOs
- Mapper with `toEntity()`, `updateEntityFromRequest()`, `toResponse()`, `toUsageResponse()`
- Error codes registered in the module's error-code constants class
- Permission constants registered in the project's permission constants class

---

## Variables

Inherits `<module>`, `<Entity>`, `<ENTITY_CLASS>`, `<base.package>` from
[`build-create-entity`](../build-create-entity/SKILL.md), plus:

| Variable | Meaning |
|----------|---------|
| `<Module>ErrorCodes` | The module's error-code constants class |
| `<PERMISSIONS_CLASS>` | Fully-qualified name of the project's permission constants class, used inside `@PreAuthorize` SpEL |
| `<CACHE_NAME>` | Approved cache name — **only** if the entity passed the eligibility gate in [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) |

## Responsibilities

- Generate `create`, `getById`, `update`, `delete`, `activate`, `deactivate`, `search`, `getUsage`
- `@PreAuthorize` on every public method, referencing permission constants
- `@Transactional` on writes, `@Transactional(readOnly = true)` on reads
- `@CacheEvict` on writes / `@Cacheable` on reads — only for cache-eligible entities
- Return `ServiceResult<T>` from every method except `delete()`
- Throw `LocalizedException` with registered error codes for every error path
- Validate sort fields against an `ALLOWED_SORT_FIELDS` whitelist
- Use the shared specification and pageable builders for search

## Constraints

- MUST NOT generate entity, repository, DTO, mapper, or controller code
- MUST NOT assume a missing error code or permission — define them first
- MUST NOT hardcode error message text — use error-code constants
- MUST NOT catch `DataIntegrityViolationException` — let the global handler map it
- MUST NOT set the active flag directly — use `activate()`/`deactivate()`
- MUST NOT inject another module's `@Service`, `Repository`, or `@Entity`, or any class outside
  its cross-module package — see "Cross-Module Calls" below

## Output

- `src/main/java/<base/package>/<module>/service/<Entity>Service.java`

---

## Domain Delegation Rule

Before implementing any conditional, apply the **Decision Test**: does this code answer
*"is this operation allowed?"* If yes, it MUST be a call to the entity's Domain companion
(`<Entity>Domain`, produced by [`build-create-entity`](../build-create-entity/SKILL.md)) — never
an inline `if` in the service body.

1. Fetch whatever data the rule needs via the repository (counts, flags, sibling records).
2. Build or load the Domain object: `<Entity>Domain.create(...)` for new instances,
   `<Entity>Domain.from(entity)` to evaluate a rule against an existing one.
3. Call its decision method — it throws `LocalizedException` on violation.
4. On success, call the entity's own mutation method and persist.

Cross-module data needed for a decision is resolved **by the service**, then passed into the
Domain object as a plain argument. The Domain object never imports or calls another module.

---

## Cross-Module Calls

Modules are package-by-feature areas inside one deployable — always deployed together. Cross-module
reads go through **direct Spring interface injection**, not loopback HTTP. Nothing but the
project's ArchUnit suite enforces the boundary, so never bypass a module's cross-module package
"because it would still compile."

**Consuming another module:**
- Inject the producing module's designated interface from its own
  `<base.package>.<other-module>.crossmodule` package — never that module's internal `@Service`,
  `Repository`, or `@Entity`, and never any class outside that package.
- The interface returns only a narrow, producing-module-defined read-model type — never a real
  JPA entity or internal DTO. This anti-corruption boundary is deliberate.
- State transaction-propagation intent explicitly at every call site; do not rely on the default.
  Use `@Transactional(readOnly = true)` on the producing side for a pure read; use
  `@Transactional(propagation = Propagation.REQUIRES_NEW)` when the producing side performs a
  write that must commit independently of the caller's outcome. Say why in a comment.
- Authorization on the producing side is enforced by its own `@PreAuthorize`, which fires
  regardless of call path — one application context. Do NOT forward an `Authorization` header;
  the security context already travels with a synchronous call. An async or scheduled thread must
  propagate the security context explicitly through its executor.
- Catch the producing side's access-denied / not-found exception at the call site — do not let it
  surface as an unhandled 500, and do not silently swallow it.
- The service is the ONLY layer allowed to hold a cross-module interface reference — never inject
  one into a Domain object, a mapper, or a controller.

**Exposing this module to others:**
- Define a narrow interface and its read-model type(s) in this module's own `crossmodule` package.
- Implement it in a small dedicated class delegating to this module's internal service — do not
  implement it directly on the main internal service, so the exposed surface stays narrow.
- That package is the ONLY surface another module may depend on.

**Forbidden in either direction:**
- Importing or injecting another module's internal classes
- Returning a real entity or internal DTO from a cross-module interface
- A circular dependency between two modules' cross-module interfaces — resolve it
  architecturally, never force it

### Internal trusted-caller calls

Some methods are only ever reached from another in-process caller with no HTTP principal (a
Spring event listener, a cross-module implementation invoked from one). Such a method still needs
a real gate — do NOT leave it ungated on the reasoning that "no controller calls it today."

The sanctioned pattern:
- Gate the method with `@PreAuthorize("hasAuthority('<INTERNAL_AUTHORITY>')")`, written as a
  plain string literal — not a SpEL type reference, which would itself create an untracked
  cross-module bypass.
- Every legitimate in-process caller wraps its call in the project's internal-caller utility,
  which installs a synthetic authentication carrying exactly that authority for the duration of
  the call and restores the previous security context afterwards. No authentication entry point
  ever grants this authority, so no external request can satisfy the check.
- Add a build-time guard alongside the runtime one: an ArchUnit test failing if any
  `@RestController`/`@Controller` reaches the gated method — method-scoped, not class-scoped, if
  the class also exposes legitimately HTTP-reachable methods.
- Document both the gate and the guard in the method's javadoc.

---

## Publishing Domain Events

To notify another part of the system that something happened, publish an in-process application
event. Do NOT reach into another module's service to perform the side effect, and do NOT
introduce a message broker.

- Define one immutable event class per trigger, named `<Action><Entity>Event`, carrying only the
  plain data the listener needs.
- Publish from the service via an injected `ApplicationEventPublisher`.
- The `@EventListener` / `@TransactionalEventListener` stays in the module that defines the
  event. Another module reacts to the outcome only by calling back through that module's
  cross-module interface — never by listening for another module's internal event type.

---

## Steps

### 1. Class declaration
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class <Entity>Service {

    private final <Entity>Repository repository;
    private final <Entity>Mapper mapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "<field>", "isActive", "createdAt"
    );
```

### 2. create()
```java
@CacheEvict(cacheNames = "<CACHE_NAME>", allEntries = true)   // ONLY if cache-eligible
@Transactional
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_CREATE)")
public ServiceResult<<Entity>Response> create(<Entity>CreateRequest request) {
    log.info("Creating <Entity> with key: {}", request.get<NaturalKey>());

    // 1. Fetch what the rule needs — repository access stays in the service
    boolean keyTaken = repository.existsBy<NaturalKey>(request.get<NaturalKey>().toUpperCase());

    // 2. Delegate the decision — throws LocalizedException on violation
    <Entity>Domain.create(request.get<NaturalKey>(), keyTaken);

    // 3. Map, then set FK relationships if this is a child
    <ENTITY_CLASS> entity = mapper.toEntity(request);

    // 4. Persist
    <ENTITY_CLASS> saved = repository.save(entity);
    log.info("Created <Entity> ID: {}", saved.getId());

    return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
}
```

### 3. update()
```java
@CacheEvict(cacheNames = "<CACHE_NAME>", allEntries = true)   // ONLY if cache-eligible
@Transactional
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_UPDATE)")
public ServiceResult<<Entity>Response> update(Long id, <Entity>UpdateRequest request) {
    log.info("Updating <Entity> ID: {}", id);

    <ENTITY_CLASS> entity = repository.findById(id)
        .orElseThrow(() -> new LocalizedException(
            Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id));

    // Fetch what an update-time rule needs, then delegate the decision
    <Entity>Domain.from(entity).assertCanUpdate(/* ...facts... */);

    mapper.updateEntityFromRequest(entity, request);

    <ENTITY_CLASS> saved = repository.save(entity);
    log.info("Updated <Entity> ID: {}", saved.getId());

    return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
}
```

### 4. getById()
```java
@Transactional(readOnly = true)
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_VIEW)")
public ServiceResult<<Entity>Response> getById(Long id) {
    log.debug("Fetching <Entity> ID: {}", id);

    <ENTITY_CLASS> entity = repository.findById(id)
        .orElseThrow(() -> new LocalizedException(
            Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id));

    return ServiceResult.success(mapper.toResponse(entity));
}
```

### 5. search()
```java
@Transactional(readOnly = true)
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_VIEW)")
public ServiceResult<Page<<Entity>Response>> search(<Entity>SearchRequest searchRequest) {
    log.debug("Searching <Entity>");

    SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

    SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
    Specification<<ENTITY_CLASS>> spec =
        SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE);
    Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

    Page<<ENTITY_CLASS>> page = repository.findAll(spec, pageable);

    return ServiceResult.success(page.map(mapper::toResponse));
}
```

### 6. activate()
```java
@CacheEvict(cacheNames = "<CACHE_NAME>", allEntries = true)   // ONLY if cache-eligible
@Transactional
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_UPDATE)")
public ServiceResult<<Entity>Response> activate(Long id) {
    log.info("Activating <Entity> ID: {}", id);

    <ENTITY_CLASS> entity = repository.findById(id)
        .orElseThrow(() -> new LocalizedException(
            Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id));

    entity.activate();

    return ServiceResult.success(mapper.toResponse(repository.save(entity)), Status.UPDATED);
}
```

### 7. deactivate()
```java
@CacheEvict(cacheNames = "<CACHE_NAME>", allEntries = true)   // ONLY if cache-eligible
@Transactional
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_UPDATE)")
public ServiceResult<<Entity>Response> deactivate(Long id) {
    log.info("Deactivating <Entity> ID: {}", id);

    <ENTITY_CLASS> entity = repository.findById(id)
        .orElseThrow(() -> new LocalizedException(
            Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id));

    // Fetch the facts, then delegate the decision
    long activeChildren = repository.countActive<Children>(id);
    <Entity>Domain.from(entity).assertCanDeactivate(activeChildren);

    entity.deactivate();

    return ServiceResult.success(mapper.toResponse(repository.save(entity)), Status.UPDATED);
}
```

### 8. delete()
```java
@CacheEvict(cacheNames = "<CACHE_NAME>", allEntries = true)   // ONLY if cache-eligible
@Transactional
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_DELETE)")
public void delete(Long id) {
    log.info("Deleting <Entity> ID: {}", id);

    <ENTITY_CLASS> entity = repository.findById(id)
        .orElseThrow(() -> new LocalizedException(
            Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id));

    long childCount = repository.count<Children>(id);
    if (childCount > 0) {
        throw new LocalizedException(Status.CONFLICT,
            <Module>ErrorCodes.<ENTITY>_CHILDREN_EXIST, id);
    }

    // No try-catch — a constraint violation propagates to the global exception handler
    repository.delete(entity);
    log.info("Deleted <Entity> ID: {}", id);
}
```

### 9. getUsage()
```java
@Transactional(readOnly = true)
@PreAuthorize("hasAuthority(T(<PERMISSIONS_CLASS>).<ENTITY>_VIEW)")
public ServiceResult<<Entity>UsageResponse> getUsage(Long id) {
    log.debug("Fetching usage for <Entity> ID: {}", id);

    <ENTITY_CLASS> entity = repository.findById(id)
        .orElseThrow(() -> new LocalizedException(
            Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id));

    return ServiceResult.success(
        mapper.toUsageResponse(entity, repository.count<Children>(id)));
}
```

---

## Shared Layer Mandate

| # | Requirement | Shared class | Package |
|---|-------------|--------------|---------|
| SH.1 | Return `ServiceResult<T>` from every method except `delete()` | `ServiceResult` | `<base.package>.common.domain.status` |
| SH.2 | Status values come from the shared `Status` type | `Status` | `<base.package>.common.domain.status` |
| SH.3 | Every error is a `LocalizedException(Status, errorCode, ...args)` | `LocalizedException` | `<base.package>.common.exception` |
| SH.4 | Dynamic specifications via `SpecBuilder.build()` | `SpecBuilder` | `<base.package>.common.search` |
| SH.5 | Pagination via `PageableBuilder.from()` with a sort whitelist | `PageableBuilder` | `<base.package>.common.search` |
| SH.6 | Sort validation via the shared allowed-fields type | `SetAllowedFields` | `<base.package>.common.search` |
| SH.7 | Current-user access via the shared security-context helper | `SecurityContextHelper` | `<base.package>.common.util` |
| SH.8 | Common validations via the shared validation utility | `ValidationUtils` | `<base.package>.common.util` |

**Rules:**
- NEVER throw a raw `RuntimeException`
- NEVER build a `Specification` or `Pageable` by hand
- NEVER create a custom result wrapper
- NEVER hardcode error message text
- NEVER catch `DataIntegrityViolationException`

> After creating the service, run [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md)
> and [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md).

---

## Rules (STRICT)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.5.1 | `@Service @RequiredArgsConstructor @Slf4j` | YES |
| A.5.2 | `@PreAuthorize` with a permission constant on EVERY public method | YES |
| A.5.3 | `@Transactional` on every write method | YES |
| A.5.4 | `@Transactional(readOnly = true)` on every read method | YES |
| A.5.5 | `@CacheEvict(allEntries = true)` on writes, for cache-eligible entities only | YES |
| A.5.6 | `ALLOWED_SORT_FIELDS` as a `private static final Set<String>` | YES |
| A.5.7 | Search uses the shared spec + pageable builders | YES |
| A.5.8 | Returns `ServiceResult<T>` — never a raw DTO or entity. `delete()` stays `void` | YES |
| A.5.9 | `create()` → decide → map → save → `ServiceResult.success(dto, Status.CREATED)` | YES |
| A.5.10 | `update()` → find → not-found throw → decide → map → save → `Status.UPDATED` | YES |
| A.5.11 | `delete()` → find → reference check → delete, with no try-catch | YES |
| A.5.12 | `activate()`/`deactivate()` → find → (decide) → entity helper → save | YES |
| A.5.13 | Error codes come from constants, never inline strings | YES |
| A.5.14 | `log.info()` for writes, `log.debug()` for reads | YES |
| A.5.15 | Every exception is a `LocalizedException` | YES |
| A.5.16 | A child search requires a non-null parent id | YES |
| A.5.17 | A child search uses an explicit `Specification` join | YES |
| A.5.18 | Business-rule checks are delegated to `<Entity>Domain`, never inlined; the service body is orchestration-only | YES |

### Annotation order (CRITICAL)

```
Cached read:            @Cacheable  → @Transactional(readOnly = true) → @PreAuthorize
Cached write:           @CacheEvict → @Transactional                  → @PreAuthorize
Non-cached method:                    @Transactional[(readOnly)]      → @PreAuthorize
```

---

## Violations (MUST NOT)

- ❌ Throwing a raw `RuntimeException` or any non-`LocalizedException`
- ❌ Returning a raw DTO or entity instead of `ServiceResult<T>`
- ❌ A public method without `@PreAuthorize`
- ❌ A hardcoded permission string instead of a constant
- ❌ A write method without `@Transactional`
- ❌ A read method without `readOnly = true`
- ❌ Accepting arbitrary sort fields
- ❌ Manual specification or pageable construction
- ❌ Inline error message strings
- ❌ Setting the active flag directly instead of using the entity helpers
- ❌ Deleting without a reference check
- ❌ try-catch around a constraint violation in `delete()`
- ❌ `log.info()` for a read operation
- ❌ Missing cache eviction on a write for a cached entity
- ❌ A business-rule `if` inlined in a service method instead of delegated to `<Entity>Domain`
- ❌ Injecting another module's internal classes instead of its cross-module interface
- ❌ Publishing through a message broker or a custom publisher/listener port instead of
  `ApplicationEventPublisher`
