# Step 13 — Register `erp-core` in the Governance Factory as a "given" dependency

**Repos:** factory `https://github.com/hesham421/factory.git`, project data `https://github.com/hesham421/governance-shared.git` (branch `feat/v7` or later), and the reference app's `erp-app-reference/governance/` copy.
**Goal:** the factory and the project repo treat `erp-core` as an external, versioned platform dependency whose modules are **not analysed or regenerated**, and whose contracts (cross-module APIs, events, SPIs, permissions, tables) are available as **inputs** to the analysis of app modules. Governed modules = only what lives in the app.
**Why:** today the project repo's `governance/shared/platform/profile-summary.json` and the registries list SEC/NOTIF/FILE/MDL/CU as governed modules (`$GOV/modules/[MODULE]/backend/execution-state.json`). After the split, re-analysing them would waste cost and could generate conflicting outputs. Phase 4 (business modules through the factory) depends on this step.

## Preconditions
- Step 12 merged; `erp-core` `v1.0.0` published; `docs/CONSUMING.md` exists.
- The implementing agent has read access to the two repos above. **Do not modify factory engines' logic**; this step changes data/profile/registry content and, only if strictly required, the registry schema (additively).

## Discovery (do first, record findings in `docs/steps/13-report.md`)
1. In `governance-shared`: locate the platform profile (`platform/profile-summary.json`, `profiles/<id>.yaml`), the module registry/list that drives `paths.modules`, the platform registry of cross-module contracts (`XM-*` atoms), and any "platform brief"/domain-profile inputs.
2. In `factory`: locate how `factory.yaml` / `profiles/<domain>.yaml` declare modules, dependencies and knowledge inputs; check whether a concept of **external dependency / platform package** already exists (search for `dependency`, `given`, `external`, `platform`, `package`). If it exists, use it; if not, add it additively (see "Schema change" below).

## Required outcome (data changes in `governance-shared`)
3. **Platform dependency entry** (new): `platform/dependencies/erp-core.yaml` (or the equivalent location the discovery found):
   ```yaml
   id: erp-core
   kind: platform-library
   coordinates: com.erp:erp-core:1.0.0
   repo: <new erp-core repo url>
   docs: docs/CONSUMING.md
   governed: false            # never analysed, never generated
   provides:
     modules: [common, cu, mdl, sec, file, notif, tenant, audit, events, sequence, report]
     apis:                    # crossmodule interfaces apps may call
       - com.erp.sec.crossmodule.SecUserDirectoryApi
       - com.erp.sec.crossmodule.SecModuleRegistryApi
       - com.erp.mdl.crossmodule.MdlLookupApi
       - com.erp.file.crossmodule.FileDocumentLookupApi
       - com.erp.notif.crossmodule.NotificationDispatchApi
       - com.erp.cu.crossmodule.SettingsApi
       - com.erp.sequence.crossmodule.NumberSeriesApi
       - com.erp.audit.crossmodule.AuditApi
       - com.erp.events.DomainEventPublisher
     spis:                    # extension points apps implement
       - PermissionContributor, ChannelProvider, StorageProvider, ReportProvider, DomainEvent listeners
     events: [UserCreatedEvent, UserStatusChangedEvent, CustomerRegisteredEvent, CustomerVerifiedEvent, TenantCreatedEvent, FileDocumentPublishedEvent, NotificationRequestedEvent, NotificationDispatchedEvent, NotificationFailedEvent]
     tables: [CORE_TENANT, CORE_AUDIT_EVENT, CORE_NUMBER_SERIES, SEC_*, FILE_*, NOTIF_*, MDL_*, CU_APP_CONFIGURATION]
     migrations: { range: "V1-V999", apps_start_at: 1000 }
     conventions: { tenant: "row-level @TenantId", base_entity: AuditableEntity, response: ApiResponse, errors: LocalizedException, search: SpecBuilder, realms: [STAFF, CUSTOMER] }
   ```
4. **Governed module list**: remove SEC/NOTIF/FILE/MDL/CU (and any FIN remnants) from the governed modules; the list becomes empty until Phase 4 adds business modules. Remove or archive their `modules/<MODULE>/backend/execution-state.json` under `_archive/` per the project's history convention (do not delete history).
5. **Knowledge input**: copy `erp-core/docs/CONSUMING.md` (and the per-module docs when step 99 exists) into the project's knowledge folder so domain-profile/P0/P1 can cite them.
6. **Profile**: the active profile's platform section references `dependencies: [erp-core]`, and the analysis maturity/consistency checks treat `XM-*` atoms that target an `erp-core` API as **external** (satisfied by the dependency, no implementation stage generated).

## Schema change in `factory` (only if discovery found no existing concept)
7. Add `dependencies[]` to the profile schema with the fields above; add one `analyze` check: *an app module must not declare a table, permission, event or API that `erp-core` already provides* (duplicate detection by name) — severity `error`. Render docs so the generated SKILL/README mention the dependency. Keep every existing test green; add tests for the new check and for a profile with a dependency.

## Acceptance
- Running the factory's `analyze`/`lint` on the project repo passes with zero governed modules and one platform dependency.
- A dry run of the pipeline on a toy app module (e.g. `PARTY` stub used only for this test, then removed) shows the engines reading `erp-core` contracts as inputs and generating **no** stage for core modules.
- Both repos' changes are committed on branches named `step/13-erp-core-given`, with the report in this repo's `docs/steps/13-report.md`.

## Out of scope
Any business module analysis; changing engine prompts beyond what the dependency concept needs.
