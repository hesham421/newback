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
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag (`cu/` = `erp-core/src/main/java/com/erp/cu/`).
No ENTITY / XM ids are minted here; the rule ids cited are the ones minted in `../P1/srs-cu.md`.

RESPONSIBILITIES — deltas
| Kind | Capability | Delta | Source |
|---|---|---|---|
| CHANGED | (packaging) | The four code capabilities (Specification / Filtering, Global Exceptions, Bundle, Events) live in the foundation packages `com.erp.common` and `com.erp.events`, not in `com.erp.cu`; `com.erp.cu` holds the Configuration capability only (entity, CRUD API, `SettingsApi`, permissions) | erp-core/src/main/java/com/erp/{common,events,cu}; docs/RELEASE.md |
| CHANGED | Configuration | AppConfiguration becomes GLOBAL with a nullable `TENANT_ID` (NULL = platform default, else tenant override); new typed, cached read API `com.erp.cu.crossmodule.SettingsApi` replaces the internal `ConfigurationService.getValue` (removed: no caller, tenant-unaware); keys are case-insensitive and stored upper-case; no settings-changed event; no provisioning of a new tenant (it inherits the defaults) | docs/steps/09-report.md; docs/DEVIATIONS.md [09]; srs-cu.md RULE-CU-004, 009, 013, 014 |
| CHANGED | Events | Moved out of CU into the dedicated core module `com.erp.events` (asynchronous, after commit, tenant-propagating executor); CU publishes and listens to nothing | docs/steps/08-report.md |
| CHANGED | Global Exceptions (`com.erp.common`) | + `CONCURRENT_MODIFICATION` 409 (step 05), + `CommonErrorCodes.NOT_FOUND` 404 and `GlobalExceptionHandler.handleNoResource` (1.2.0), wrapped `LocalizedException` answered with its own code (1.2.0); `DATA_INTEGRITY_VIOLATION` 409 covers the CU create race | docs/DEVIATIONS.md [05], [15] |
| CHANGED | Specification / Filtering | list endpoints are `POST …/search` with a filter envelope; an unknown filter field → 400 `VALIDATION_ERROR` (`UNSUPPORTED_FILTER_FIELD`), an unknown sort field is ignored, page size ≤ 200; `PageableBuilder` rejects an overflowing `page` with 400 (1.2.0); `tenantId` is never a client filter | docs/DEVIATIONS.md [15], [09]; common/search/SpecBuilder.java:54-63; common/search/PageableBuilder.java:29-41 |
| CHANGED | Bundle (i18n) | the library contributes a `messageSource` (application bundles first, then core `i18n/messages`; UTF-8; no system-locale fallback); English base is `messages.properties`, Arabic `messages_ar.properties` (no `messages_en`) | docs/DEVIATIONS.md [03], [01] |
| NEW | Base entities | `GlobalAuditableEntity` (audit columns + `@Version`) is the parent of `AuditableEntity` (+ `@TenantId TENANT_ID`); AppConfiguration extends the global one | docs/DEVIATIONS.md [05]; cu/entity/AppConfiguration.java:41 |
| NEW | Caching | `ErpCoreCacheAutoConfiguration` enables Spring caching for the one core cache `erpCoreSettings`; the application chooses the provider; no TTL; eviction on CU writes only | erp-core/src/main/java/com/erp/autoconfigure/ErpCoreCacheAutoConfiguration.java:30-46; srs-cu.md RULE-CU-007, 008 |

ENTITIES OWNED — deltas
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | AppConfiguration | global entity (`GlobalAuditableEntity`), nullable immutable `tenantId`, `version` (not on the wire); key unique per owner (platform default or one tenant); `isActiveFl` → `isActive`; `configKey` stored upper-case | V10, V14__sequence_and_settings.sql; cu/entity/AppConfiguration.java |

DEPENDENCIES — deltas ("CU is ROOT" no longer holds strictly)
| Kind | Module | Type | What | Source |
|---|---|---|---|---|
| NEW | tenant | HARD FK + `TenantContext` | `CORE_TENANT` FK on `TENANT_ID` (`FK_CU_APP_CONFIGURATION_TENANT`); the acting tenant decides override vs default; `SettingsApi` needs a bound tenant | V10; docs/DEVIATIONS.md [09] |
| NEW | SEC | SPI | `CuPermissions` implements `PermissionContributor` | docs/steps/06-report.md |
| NEW | audit | SOFT | `@Audited` on `AppConfiguration` | docs/DEVIATIONS.md [10] |
| NEW | (exposed) | cross-module | `SettingsApi` — no core consumer (applications consume it); `ConfigurationService.resolve` backs it (internal, cached, unauthorised) | cu/crossmodule/SettingsApi.java; cu/service/ConfigurationService.java:198-205 |

PERMISSIONS — deltas (CU had "no CORE-9 permissions")
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | module `CU` | registry names `الأدوات المشتركة` / `Common Utilities` (this file's header says Common Utils) | cu/permission/CuPermissions.java:52; V7__sec_seed.sql:40 |
| CHANGED | screen `CU_CONFIGURATIONS` (`إدارة إعدادات المنصة` / `Platform Configuration`) | backend-only holder screen with the literal authorities `CONFIG_VIEW`, `CONFIG_CREATE`, `CONFIG_UPDATE`, `CONFIG_DEACTIVATE` (action codes VIEW / CREATE / UPDATE / DEACTIVATE); seeded by V7 (old `V31` seed), code-defined in `CuPermissions` | CuPermissions.java:22-28,62-66; V7:66,114-117; docs/steps/04-report.md (mapping) |
| NEW | screen `PLATFORM_SETTINGS` (`إعدادات المنصة الافتراضية` / `Platform Default Settings`) under module `PLATFORM` | `PERM_PLATFORM_SETTINGS_VIEW` (gateway, gates no API) and `PLATFORM_SETTINGS_MANAGE` (every `scope=PLATFORM` call); inserted by the startup catalog synchroniser, not seeded; held implicitly by PLATFORM-tenant super roles only | CuPermissions.java:36-45,67-68; docs/DEVIATIONS.md [09] |
| NEW | role `CU_ADMIN` | seeded by V7 with the `CU` module grant (screen + four actions derived); `SYS_ADMIN` holds the same | V7:151,172,175,187-200 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (commit 6b01816; CHANGELOG [Unreleased]); no CU behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `AppConfigurationDomain` | uses the shared `com.erp.common.domain.DomainRules` (`assertNotBlank`, `assertUnique`) for RULE-CU-002 / RULE-CU-001; same codes and statuses | erp-core/src/main/java/com/erp/cu/domain/AppConfigurationDomain.java:39-40,51 |
| CHANGED | `ConfigurationService.owner` | uses `TenantContext.isPlatform()` (new in 1.3.0) for the tenant half of RULE-CU-005; same outcome | erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:214 |

No responsibility, entity, dependency or permission delta.
