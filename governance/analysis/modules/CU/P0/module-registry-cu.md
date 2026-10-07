## MODULE REGISTRY — COMMON UTILS
══════════════════════════════════════════════════════════════════
Module Name    : Common Utils
Module Code    : CU
Layer          : L1
Type           : Cross-Cutting Foundation (library — not a business module)
Execution Tier : T1 (ROOT — built first, depended-on by all)
P0 Date        : 2026-09-01
Readiness      : READY
Domain KB      : none supplied — derived from domain-profile-ERP.md
Source         : NEW
══════════════════════════════════════════════════════════════════

SCOPE NOTE
──────────────────────────────────────────────────────────────────
Per domain-profile-ERP.md, Common Utils is ONE analytical cross-cutting
component — a shared library, not a standalone business module. It groups
five capabilities: Specification/Filtering, Global Exceptions, Bundle,
Configuration, and Events. Its outputs are reusable utilities consumed by
every other module; it owns almost no persistent data. Governing mandate:
medium complexity — the simplest solution that satisfies the requirement.
No FK from CU to any module (it is ROOT); other modules depend on CU.

RESPONSIBILITIES (capabilities — code-level unless noted)
──────────────────────────────────────────────────────────────────
Specification / Filtering │ dynamic query + filter mechanism (predicate
                          │ builder over entity queries). Pure code — no entity.
Global Exceptions         │ base exception hierarchy + centralized handler +
                          │ standard error-response shape. Pure code — no entity.
                          │ NOTE: the governed Error Catalog / ERR-IDs are a P3
                          │ artifact — CU provides the exception INFRASTRUCTURE,
                          │ not the catalog.
Bundle (i18n)             │ AR/EN message resolution. Default: resource bundles
                          │ (messages_ar / messages_en) — file-based, no entity.
Configuration             │ platform "Configurable" goal — lightweight persisted
                          │ key/value settings store (see ENTITIES OWNED).
Events                    │ in-process domain events (publisher/listener). Default:
                          │ synchronous in-process bus — no entity, no broker.
──────────────────────────────────────────────────────────────────

ENTITIES OWNED
──────────────────────────────────────────────────────────────────
AppConfiguration │ Config / Master │ PRIVATE
──────────────────────────────────────────────────────────────────
Note: names only — ENTITY-IDs assigned by P1, not here.
This is the ONLY persisted entity CU owns by default. It backs the
"Configurable" design goal with runtime-adjustable key/value settings.
All other CU capabilities are code mechanisms with no table.

LOVs OWNED
──────────────────────────────────────────────────────────────────
(none by default)
──────────────────────────────────────────────────────────────────
Note: any config value-type / scope enumeration, if needed, is a P1
LOV decision — not pre-declared here to avoid over-engineering.

LOVs CONSUMED (from other modules)
──────────────────────────────────────────────────────────────────
(none — CU is ROOT)
──────────────────────────────────────────────────────────────────

SHARED ENTITIES CONSUMED
──────────────────────────────────────────────────────────────────
(none — CU is ROOT)
──────────────────────────────────────────────────────────────────

DEPENDENCIES
──────────────────────────────────────────────────────────────────
(none)
──────────────────────────────────────────────────────────────────
ROOT: YES — no external deps. Depended-on by SEC, FILE, NOTIF.

AUTO-DECISIONS
──────────────────────────────────────────────────────────────────
AUTO: Configuration is a persisted key/value store (1 entity: AppConfiguration).
FROM: domain-profile "Configurable" design goal + medium-complexity rule.
IF WRONG: if configuration should be file/env-only (Spring properties), drop
          the entity — CU then owns zero persisted entities.

AUTO: Bundle (i18n) uses file-based resource bundles (messages_ar/_en), no entity.
FROM: Step 4 default (simplest solution) — standard Spring i18n.
IF WRONG: if translations must be runtime-editable by admins, add a
          MessageCatalog entity (P1 decision).

AUTO: Events use a synchronous in-process publisher (Spring ApplicationEvent),
      no broker, no outbox table.
FROM: Modular Monolith + medium-complexity rule (no RabbitMQ/Kafka).
IF WRONG: if durable/async cross-module delivery is required later, add an
          event-outbox entity + async dispatch (opened as a new decision then).

AUTO: Specification/Filtering and Global Exceptions are pure code mechanisms
      (no entities, no tables).
FROM: their nature — query helpers and error handling.
IF WRONG: n/a — these are not data-owning by definition.

INF-IDs
──────────────────────────────────────────────────────────────────
(none — all decisions traced to domain-profile + medium-complexity rule
 via AUTO-DECISIONS above; no unresolved gap)
──────────────────────────────────────────────────────────────────
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 03, 05, 06, 08, 09, 10, 12, 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No ENTITY / XM ids are minted here.

RESPONSIBILITIES — deltas
| Capability | Delta | Source |
|---|---|---|
| Configuration | AppConfiguration becomes GLOBAL with nullable `TENANT_ID` (NULL = platform default, else tenant override); new typed, cached read API `com.erp.cu.crossmodule.SettingsApi` replaces the internal `ConfigurationService.getValue` (removed: no caller, tenant-unaware) | docs/steps/09-report.md; DEVIATIONS [09] |
| Events | Moved out of CU into the dedicated core module `com.erp.events` (asynchronous, after commit, tenant-propagating executor) | docs/steps/08-report.md |
| Global Exceptions (`com.erp.common`) | + `CONCURRENT_MODIFICATION` 409 (step 05), + `Status.TOO_MANY_REQUESTS` 429 (step 06), + `CommonErrorCodes.NOT_FOUND` 404 and `GlobalExceptionHandler.handleNoResource` (1.2.0), wrapped `LocalizedException` answered with its own code (1.2.0) | DEVIATIONS [05], [06], [15] |
| Specification / Filtering | `PageableBuilder` rejects an overflowing `page` with 400 `VALIDATION_ERROR` (1.2.0); `tenantId` is never a client filter field | DEVIATIONS [15], [09] |
| Bundle (i18n) | the library contributes a `messageSource` (application bundles first, then core `i18n/messages`; UTF-8; no system-locale fallback); English base is `messages.properties`, Arabic `messages_ar.properties` (no `messages_en`) | DEVIATIONS [03], [01] |
| Base entities | `GlobalAuditableEntity` (audit columns + `@Version`) is the parent of `AuditableEntity` (+ `@TenantId TENANT_ID`) | DEVIATIONS [05] |

ENTITIES OWNED — deltas
| Entity | Delta | Source |
|---|---|---|
| AppConfiguration | CHANGED: global entity (`GlobalAuditableEntity`), nullable `tenantId`, `version`; key unique per (tenant or platform) | V10, V14__sequence_and_settings.sql |

DEPENDENCIES — deltas ("CU is ROOT" no longer holds strictly)
| Module | Kind | What | Source |
|---|---|---|---|
| tenant | HARD FK + `TenantContext` | `CORE_TENANT` FK on `TENANT_ID`; the acting tenant decides override vs default | V10; DEVIATIONS [09] |
| SEC | SPI | `CuPermissions` implements `PermissionContributor` | docs/steps/06-report.md |
| audit | SOFT | `@Audited` on `AppConfiguration` | DEVIATIONS [10] |

PERMISSIONS (CU had "no CORE-9 permissions")
Code-defined by `CuPermissions`: module `CU`, screen `CU_CONFIGURATIONS` with legacy literal authorities
`CONFIG_VIEW`, `CONFIG_CREATE`, `CONFIG_UPDATE`, `CONFIG_DEACTIVATE` (registry rows pre-date erp-core —
old `V31` seed, now in `V7__sec_seed.sql`); NEW under registry module `PLATFORM`: screen
`PLATFORM_SETTINGS`, `PERM_PLATFORM_SETTINGS_VIEW` (gateway) and `PLATFORM_SETTINGS_MANAGE`.
— DEVIATIONS [06], [09]; docs/steps/04-report.md (mapping)
