---
name: gov-enforce-error-handling
description: "ERROR HANDLING ENFORCER — ensures every exception is a LocalizedException carrying a registered, module-specific error code. Rejects raw runtime exceptions, generic not-found exceptions, and hardcoded message strings. Validates error-code registration in both message bundles."
---

# Skill: gov-enforce-error-handling

## Description
**ERROR HANDLING GOVERNANCE ENFORCER.** Ensures that ALL error handling follows the
`LocalizedException` pattern with error codes registered in the module's own error-code constants
class. Detects and rejects raw runtime exceptions, generic not-found exceptions, hardcoded error
strings, and any non-standard error pattern.

## When to Use
- After any service or controller is created or modified
- When reviewing exception handling
- When validating that error codes are registered
- As part of the [`gov-validate-backend-feature`](../gov-validate-backend-feature/SKILL.md) pipeline

## When NOT to Use
- For HTTP status configuration in a reverse proxy or in Spring Security — that is deployment
  or security configuration, not application error handling
- When the code under review has no error handling at all — report it as missing, do not run a
  full validation pass
- For the project's global exception handler itself — that file is shared infrastructure. This
  skill validates a module's *usage* of the taxonomy, it does not own or modify the taxonomy

## Responsibilities

- Verify every exception is a `LocalizedException` carrying a registered error code
- Detect and reject raw `RuntimeException`, generic not-found exceptions, `IllegalArgumentException`
- Verify error codes are declared in the module's error-code constants class
- Verify every code has a message in both language bundles
- Validate the correct `Status` per error scenario

## Constraints

- MUST NOT generate or modify application code — validation only
- MUST NOT create new error codes — report missing ones for manual addition
- MUST NOT modify shared exception infrastructure
- MUST NOT validate concerns owned by another enforce skill

## Output

- An error-handling compliance report identifying exception-type violations, missing error-code
  registrations, missing translations, and incorrect `Status` usage

---

## Core Rule (NON-NEGOTIABLE)

> **A generic not-found exception type is never used.**
>
> Every not-found path throws:
> ```java
> new LocalizedException(Status.NOT_FOUND, <Module>ErrorCodes.<ENTITY>_NOT_FOUND, id)
> ```
> A generic exception carries no error code, so the client cannot localize or branch on it.

---

## Enforcement Checklist (23 checks)

### CHECK 1: Exception types (5)

```
[ ] No generic not-found exception type is imported anywhere in the module
[ ] No generic not-found exception is constructed anywhere
[ ] No "throw new RuntimeException(" for a business error
[ ] No "throw new IllegalArgumentException(" for a validation error
[ ] Every exception is LocalizedException(Status, errorCode, ...args)
```

### CHECK 2: Status usage (4)

| Scenario | Required `Status` | Error-code shape |
|----------|-------------------|------------------|
| Entity not found | `NOT_FOUND` | `<ENTITY>_NOT_FOUND` |
| Duplicate key/code | `ALREADY_EXISTS` | `<ENTITY>_KEY_DUPLICATE` |
| FK / constraint violation | `CONFLICT` | `<ENTITY>_FK_VIOLATION` |
| Cannot deactivate — active children exist | `CONFLICT` | `<ENTITY>_ACTIVE_CHILDREN_EXIST` |
| Cannot delete — children exist | `CONFLICT` | `<ENTITY>_CHILDREN_EXIST` |
| Cannot delete — referenced elsewhere | `CONFLICT` | `<ENTITY>_REFERENCES_EXIST` |
| Invalid state transition / cycle detected | `BUSINESS_RULE_VIOLATION` | `<ENTITY>_<RULE>` |

```
[ ] Every not-found uses Status.NOT_FOUND
[ ] Every duplicate uses Status.ALREADY_EXISTS
[ ] Every "blocked by a referencing record" uses Status.CONFLICT — not BUSINESS_RULE_VIOLATION
[ ] Every Status maps to the intended HTTP code
```

### CHECK 3: Error-code registration (5)

```
[ ] Error codes declared in <Module>ErrorCodes as static final String constants
[ ] NO inline error strings in service methods
[ ] Every code follows ONE format consistently within the module — either the descriptive
    <ENTITY>_<SCENARIO> form or a numbered registry form, never both mixed
[ ] The error-codes class has a private constructor
[ ] That constructor throws, so the class cannot be instantiated
```

### CHECK 4: Message registration (4)

```
[ ] Every error code has an entry in the default message bundle
[ ] Every error code has an entry in every other supported-locale bundle
[ ] Messages support parameter substitution ({0}, {1}) where the code passes arguments
[ ] Messages are human-readable — not stack traces or internal identifiers
```

### CHECK 5: Service error patterns (5)

```
[ ] Every findById uses .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, ...))
[ ] Every uniqueness check throws LocalizedException(Status.ALREADY_EXISTS, ...)
[ ] delete() does NOT try-catch the constraint-violation exception — it propagates to the
    global handler
[ ] Every deactivate() validates its constraints before mutating
[ ] No catch-all handler that swallows errors
```

---

## Automatic Rejection Patterns

| Pattern found | Reason |
|---------------|--------|
| An import of a generic not-found exception type | Prohibited — use `LocalizedException` |
| `throw new RuntimeException(` | Unstructured — use `LocalizedException` |
| `throw new IllegalArgumentException(` | Use `LocalizedException(Status.VALIDATION_ERROR, ...)` |
| `throw new Exception(` | Too generic |
| An inline literal message string in a throw | Must use an error-code constant |
| `catch (Exception e) { /* ignored */ }` | Error swallowing prohibited |

---

## Error-Code Class Template

```java
public final class <Module>ErrorCodes {

    private <Module>ErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static final String <ENTITY>_NOT_FOUND             = "<ENTITY>_NOT_FOUND";
    public static final String <ENTITY>_KEY_DUPLICATE         = "<ENTITY>_KEY_DUPLICATE";
    public static final String <ENTITY>_FK_VIOLATION          = "<ENTITY>_FK_VIOLATION";
    public static final String <ENTITY>_ACTIVE_CHILDREN_EXIST = "<ENTITY>_ACTIVE_CHILDREN_EXIST";
    public static final String <ENTITY>_CHILDREN_EXIST        = "<ENTITY>_CHILDREN_EXIST";
    public static final String <ENTITY>_REFERENCES_EXIST      = "<ENTITY>_REFERENCES_EXIST";
}
```

## Message Bundle Template

```properties
# messages.properties (default locale)
<ENTITY>_NOT_FOUND=<Entity> with ID {0} was not found
<ENTITY>_KEY_DUPLICATE=<Entity> with key ''{0}'' already exists
<ENTITY>_FK_VIOLATION=Cannot delete <Entity> {0} because it is referenced by other records
<ENTITY>_ACTIVE_CHILDREN_EXIST=Cannot deactivate <Entity> {0} because it has active child records
<ENTITY>_CHILDREN_EXIST=Cannot delete <Entity> {0} because it has child records
```

```properties
# messages_<locale>.properties — one entry per code, same keys
<ENTITY>_NOT_FOUND=...
```

> Every code registered in the class above must exist in **every** bundle. A code present in one
> bundle only is a violation, because the client receives the raw key for the missing locale.

---

## Canonical Not-Found Pattern

```java
// ✅ CORRECT — the error code reaches the client, which can localize and branch on it
<ENTITY_CLASS> entity = repository.findById(id)
    .orElseThrow(() -> new LocalizedException(
        Status.NOT_FOUND,
        <Module>ErrorCodes.<ENTITY>_NOT_FOUND,
        id));

// ❌ REJECTED — no error code; the client gets an opaque message it cannot localize
<ENTITY_CLASS> entity = repository.findById(id)
    .orElseThrow(() -> new RuntimeException("Entity not found: " + id));
```

---

## Violation Response

```
❌ ERROR HANDLING VIOLATION

Rule: [Description]
Location: [File:Line]
Found: [What was found]
Expected: new LocalizedException(Status.<STATUS>, <Module>ErrorCodes.<CODE>, args)
Severity: CRITICAL

Impact: The client cannot display a localized, domain-specific message —
the error-code contract is broken.

Fix: Replace with LocalizedException carrying a registered error code.
```

---

## Enforcement Report Format

```
## Error Handling Governance Report

### Module: [Module]   ### Date: [Date]

| Check                | Rules | Passed | Failed |
|----------------------|-------|--------|--------|
| Exception Types      | 5     | ?      | ?      |
| Status Usage         | 4     | ?      | ?      |
| Code Registration    | 5     | ?      | ?      |
| Message Registration | 4     | ?      | ?      |
| Service Patterns     | 5     | ?      | ?      |
| **TOTAL**            | **23**| **?**  | **?**  |

### Critical Violations:
[Any raw or generic exception usage]

### Missing Error Codes:
[Any unregistered scenario]

### Verdict: COMPLIANT / NON-COMPLIANT
```

---

## Shared Error-Handling Classes

All error handling uses the project's shared classes — never a per-module equivalent:

| Class | Package | Role |
|-------|---------|------|
| `LocalizedException` | `<base.package>.common.exception` | Every business error, with i18n support |
| `Status` | `<base.package>.common.domain.status` | The status taxonomy |
| `CommonErrorCodes` | `<base.package>.common.exception` | Shared codes; module codes live in `<Module>ErrorCodes` |
| `GlobalExceptionHandler` | `<base.package>.common.web` | Centralized exception → response mapping |
| `ApiError` | `<base.package>.common.web` | The error envelope — never a custom error DTO |

---

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | Full layered-architecture compliance |
| [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) | Caching eligibility and annotation rules |
| [`gov-validate-backend-feature`](../gov-validate-backend-feature/SKILL.md) | Master validation with scoring |
