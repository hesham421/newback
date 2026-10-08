# ADR-CU-001 — Settings cache: one Spring cache `erpCoreSettings`, provider chosen by the application, no TTL, eviction on write only

Module  : CU     Version : v1 (erp-core 1.2.0, as-built)     Stage raised : P3 (erp-core step 09)
Status  : ACCEPTED (as-built, non-breaking)

## Context
Step 09 of the erp-core plan made `AppConfiguration` tenant-aware (platform default + tenant override)
and gave every module a typed read API, `com.erp.cu.crossmodule.SettingsApi`. A resolved setting is
read on hot paths (a module may consult a key on every request), the resolution is two rows and a
rule (`AppConfigurationDomain.resolve`), and writes are rare administrator actions. Core had no
`@EnableCaching` before this step (DEVIATIONS [03]); the gov-enforce-caching-rules register allowed
exactly one cached entity, and the step-09 plan named `AppConfiguration` as that entity. The plan
left the cache's provider, lifetime and coherence model to the implementation.

## Decision
- One Spring cache, `SettingsApi.CACHE_NAME = "erpCoreSettings"`, keyed `<tenantId>:<KEY>`, holding the
  raw `CONFIG_VALUE` text (or `null` for "absent"); type conversion happens after the cache, so the
  cached value is a plain serializable `String` that works with the `simple` and the Redis cache types
  (`ConfigurationService.resolve`, `erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:198-205`).
- The cache manager is the application's: `ErpCoreCacheAutoConfiguration` only enables caching and
  registers the cache name on a `ConcurrentMapCacheManager` (`spring.cache.type=simple`); with
  `spring.cache.type=none` settings are read uncached. Core ships no cache provider and no cache
  configuration of its own (`erp-core/src/main/java/com/erp/autoconfigure/ErpCoreCacheAutoConfiguration.java:30-46`).
- No TTL. An entry lives until a write through the configuration CRUD API (create, update, deactivate)
  evicts the whole cache (`@CacheEvict(allEntries = true)`), because a platform-default change affects
  every tenant's key and writes are rare (`ConfigurationService.java:70,120,169`).
- Eviction happens after commit: `@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)` places the
  cache advice outside the transaction advice, so a cache hit opens no transaction and an evict never
  runs before the write is durable.

## Consequences
- A value changed in the database outside the CRUD API (SQL, another application, a migration) stays
  stale until the next API write (`SettingsIntegrationTest.aCachedValue_survivesAChangeBehindTheApi_untilACrudWriteEvictsIt`;
  docs/steps/09-report.md:320). Operators and applications must write through the API or through
  `ConfigurationService`.
- A local (`simple`) cache is not cluster-coherent: a write on one node evicts that node only. A
  multi-node deployment needs a shared provider (Redis) or must accept that staleness.
- "After commit" holds only when the CRUD service call is the outermost transaction; a caller that
  wraps `ConfigurationService.create/update/deactivate` in its own transaction evicts before its own
  commit (RULE-CU-008).
- No settings-changed event is published (RULE-CU-013); a consumer that must react to a change re-reads
  through `SettingsApi`.
- `ConfigurationService.resolve` is public and carries no `@PreAuthorize` (DEVIATIONS [09],
  docs/DEVIATIONS.md:146): it is a library read used from already authorised work or from paths with no
  principal; it is not a cross-module contract and must not be bound to a controller.

## Traces
ENTITY-CU-001 · RULE-CU-004, RULE-CU-007, RULE-CU-008, RULE-CU-013 · US-CU-001 · docs/DEVIATIONS.md [09]
(caching entry) · `SettingsCacheTest`, `SettingsIntegrationTest`
