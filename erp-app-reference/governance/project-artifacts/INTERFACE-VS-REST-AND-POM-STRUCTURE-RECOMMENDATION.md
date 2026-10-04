# POM Structure & Module Boundaries — Decision Record

Referenced by `pom.xml` (project description + Maven-plugin comments) and
`Dockerfile`. This records the standing decision so those references resolve to
a real rationale rather than a dangling link.

## Decision

The backend is **one Spring Boot deployable** with a **single `pom.xml`**, laid
out **package-by-feature** under `com.erp.*` — not a multi-module Maven reactor
(the prior 9-module layout was consolidated). Each business domain is a package
subtree:

```
com.erp.security.*        com.erp.notification.*     com.erp.masterdata.*
com.erp.org.*             com.erp.file.*             com.erp.common.*
```

## Why one module, not many

- **Simplicity of build & deploy.** One artifact (`erp-system-*.jar`), one
  version, one Flyway history, one Swagger UI — no inter-module version drift,
  no reactor ordering to maintain.
- **No packaging ceremony for internal boundaries.** Separate `pom.xml` files
  gave a *compile-time nudge* toward module isolation but at a high maintenance
  cost, and they never actually prevented the real bypasses (SpEL/reflection
  references across modules).

## How boundaries are enforced instead

Module isolation is enforced by an **ArchUnit** test suite (dependency in
`pom.xml`), not by separate POMs. The rules it encodes:

- No class outside a module's own `crossmodule/` package may be referenced from
  another module's package (cross-module access goes through the published
  cross-module interface only).
- The known SpEL/reflection bypass
  (`hasAuthority(T(com.erp.security.constants.SecurityPermissions).*)`) is
  pattern-matched too, since plain import-based rules would miss it.

The suite lives at `src/test/java/com/erp/architecture/` and is added/populated
as the first modules land — on a clean start (no `com.erp.*` module packages
yet) there is nothing to enforce.

## REST vs interface for cross-module calls

Cross-module calls are **in-process Java interface calls** through a module's
published `crossmodule/` interface — NOT internal REST hops. REST is the
*external* contract (controllers, OpenAPI); it is not used for one module to
call another inside the same deployable.
