# Releasing erp-core — versioning and compatibility policy

`com.erp:erp-core` follows [Semantic Versioning](https://semver.org/). An application pins an exact
version (`<erp.core.version>1.2.0</erp.core.version>`) and upgrades on purpose.

## What a version number promises

| Bump | Allowed changes | Upgrade cost for an application |
|---|---|---|
| **MAJOR** (`2.0.0`) | Only for removing or renaming public API, changing migration semantics, or removing or renaming a property key. **Expected: never.** If one is unavoidable, the release notes carry a migration guide, and the old form stays deprecated for at least one MINOR release first. | Code or configuration changes, described in the release notes |
| **MINOR** (`1.1.0`) | Additive only: new modules, tables, nullable or defaulted columns, endpoints, events, SPI methods with a default implementation, property keys with a default, permissions, error codes, settings. | None. Bump the version; Flyway applies the new core scripts on startup. |
| **MINOR — behaviour tightening** (`1.3.0`) | Behaviour tightening on existing endpoints (a request accepted before is now refused, or an answer changes status) is allowed in a MINOR **only** when it is listed under "Behaviour changes" in that version's `docs/CHANGELOG.md` entry **and** its client ships with it (the in-house frontend release that handles it). Nothing else of MAJOR's list may change. | The client release listed in "Behaviour changes"; other applications read the list before upgrading. |
| **PATCH** (`1.0.1`) | Fixes that change no public contract, plus additive migrations that only correct data or add an index. | None |

**Public API** means:
- every type in a module's `crossmodule` package;
- the SPIs: `PermissionContributor`/`PermissionDef`/`PermissionModule`/`PermissionScreen`,
  `TenantProvisioningContributor`, `TenantExportContributor` with `TenantExport` / `TenantExportJdbc` (1.3.0),
  `ReportProvider` and the `com.erp.report` records, `ChannelProvider`, `StorageProvider`;
- the `com.erp.events` contract and `TenantContext`;
- `com.erp.common.*` (the foundation the generated code consumes);
- the REST endpoints and their envelopes and error codes;
- the `erp.core.*` property keys;
- the core tables and columns an application may read or reference.

## Migration rules (restated)

- Core owns `V1..V999` (`erp-core/src/main/resources/db/migration/core/`). Applications own `V1000+` in
  their own location. Neither ever writes into the other's range.
- Core scripts are **additive only**: a new table, a nullable or defaulted column, an index or constraint
  that existing data already satisfies, or seed rows. No core script renames or drops a table or column,
  changes a column type, or edits or renumbers a shipped script. A mistake is fixed forward with a new
  script.
- `MigrationNamingTest` enforces the naming, the range and the additive rule in the build. Any core
  script after `V9` that contains `DROP TABLE`, `DROP COLUMN`, `RENAME` or `ALTER COLUMN ... TYPE`
  fails it.

## How a release is cut

1. All steps for the release are merged into `main`, and CI is green on `main`.
2. One commit sets every pom (`pom.xml`, `erp-core/pom.xml`, `erp-app-reference/pom.xml`) to `X.Y.Z`.
   It also updates `docs/CHANGELOG.md`. Tag that commit `vX.Y.Z` and push the tag.
3. The `ci` workflow runs on the tag:
   - `build-test`: `mvn verify` with Testcontainers, the ArchUnit rules, the enforcer and the JaCoCo
     gate (60 % lines).
   - `docker-image`.
   - `publish`: the tag must equal the pom version. Then `mvn -pl erp-core -am deploy` publishes the
     `erp-platform` parent pom, the `erp-core` jar and the `erp-core` test-jar to GitHub Packages.
   - `consume-published`: builds `erp-app-reference` alone against the published artifacts and runs its
     tests.
4. A second commit moves `main` to the next development version, `X.(Y+1).0-SNAPSHOT`.

A published version is immutable. Never re-tag; release a new PATCH instead.
(`v1.0.0` was tagged but never published, because its CI failed; `1.1.0` is the first published version.)

Out of scope: Maven Central, signed artifacts, SBOM.
