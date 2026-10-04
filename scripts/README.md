# scripts/

## `rename-project.sh` — make this template yours

The base package `com.erp` and every `erp`-derived name (the main class,
artifact id, app name, DB name, Docker names, OpenAPI/branding strings) are the
**single variable** of this backend template. `erp` is a working default so the
project compiles and runs as-is; run this script once on a fresh clone to
re-brand everything to your project in one shot:

```bash
./scripts/rename-project.sh acme
```

turns `com.erp` → `com.acme`, `ErpMainApplication` → `AcmeMainApplication`,
`erp-system` → `acme-system`, `"ERP System"` → `"Acme System"`, `erp_db` →
`acme_db`, `erp-backend` → `acme-backend`, across `src/`, `pom.xml`,
`Dockerfile`, `docker/`, the `application*.properties`, `.env.example`,
`.mcp.json`, and the governance docs — and physically moves the
`src/main/java/com/erp` package directory.

The name must be lowercase letters/digits (e.g. `acme`, `finance`, `hr2`). After
running it:

```bash
mvn -q -DskipTests compile      # verify it builds on JDK 25
```

The governance skills are already project-agnostic — they generate code under
`<base.package>` (read from `pom.xml`'s `groupId`), so no skill needs editing
after a rename.
