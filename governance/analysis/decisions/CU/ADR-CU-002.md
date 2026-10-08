# ADR-CU-002 — Platform-scope refusal is `ACCESS_DENIED` from `@PreAuthorize`; `SETTING_PLATFORM_SCOPE_FORBIDDEN` is a domain backup check

Module  : CU     Version : v1 (erp-core 1.2.0, as-built)     Stage raised : P3 (erp-core step 09)
Status  : ACCEPTED (as-built, non-breaking)

## Context
Every configuration endpoint takes `?scope=TENANT|PLATFORM`. `scope=PLATFORM` addresses the platform
defaults (`TENANT_ID IS NULL`), which may hold values tenants must not see, so the step-09 plan required
that platform defaults be read and written only from the PLATFORM tenant by a caller holding
`PLATFORM_SETTINGS_MANAGE`. Two layers can express that rule: the method-level authorization
(`@PreAuthorize`, evaluated by Spring Security before the service body runs, answered by
`GlobalExceptionHandler.handleAccessDenied` as 403 `ACCESS_DENIED`) and a domain rule inside the
service (a `LocalizedException` with a CU-specific code). The previous revision of the CU addendum named
the CU-specific code as the HTTP answer; the test suite and the core test plan expect `ACCESS_DENIED`
(`ConfigurationScopeApiIntegrationTest.java:82-88`, TC-CORE-SETTINGS-004 in
`docs/test-api/core-test-plan.md:279`).

## Decision
- The authority half of the rule is the `@PreAuthorize` on each `ConfigurationService` method
  (`erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:72-75,99-102,122-125,152-155,171-174`):
  `scope == PLATFORM and hasAuthority('PLATFORM_SETTINGS_MANAGE')`, otherwise
  `hasAuthority('CONFIG_<action>')`. `scope=PLATFORM` does not additionally need `CONFIG_*`.
- `PLATFORM_SETTINGS_MANAGE` is declared under registry module `PLATFORM` (`CuPermissions.java:36-45`),
  so the super-role expansion grants it only inside the PLATFORM tenant (`MenuService.withSuperRole`,
  `erp-core/src/main/java/com/erp/sec/service/MenuService.java:138-147`) and tenant provisioning never
  copies PLATFORM-module grants. A caller of another tenant therefore fails the `@PreAuthorize` and the
  HTTP answer is **403 `ACCESS_DENIED`**.
- The tenant half of the rule is kept as defence in depth in the Domain:
  `AppConfigurationDomain.assertScopeAllowed(scope, callerIsPlatformTenant)` throws 403
  `SETTING_PLATFORM_SCOPE_FORBIDDEN` (`AppConfigurationDomain.java:59-63`, bilingual message), called by
  `ConfigurationService.owner` after authorization. It is reached only when a non-PLATFORM caller holds
  `PLATFORM_SETTINGS_MANAGE` — possible solely through an explicit role grant of a PLATFORM-module action
  in that tenant (the role-grant API does not reject PLATFORM-module rows).

## Consequences
- The contract clients code against is `ACCESS_DENIED` (RULE-CU-005). `SETTING_PLATFORM_SCOPE_FORBIDDEN`
  stays registered, documented and unit-tested (`AppConfigurationDomainScopeTest.java:17-24`) but is not
  an answer a correctly provisioned tenant ever sees; the generated api-docs list it on all five endpoints
  because the generator walks the call graph, not the authorization order.
- The analysis addendum (`../../modules/CU/P1/srs-cu.md` §2 RULE-CU-005, §3) is corrected to say so;
  the frontend must treat `ACCESS_DENIED` as the platform-scope refusal.
- Should a future change let tenant roles hold PLATFORM-module authorities deliberately, the domain
  check becomes the effective rule without any API change.
- `scope` absent is `TENANT` (`ConfigurationController.java:56`); an unknown value is 400
  `VALIDATION_ERROR` with `fieldErrors[0].field = scope`, never a 403.

## Traces
ENTITY-CU-001 · RULE-CU-005 · API-CU-001 … API-CU-005 · `PLATFORM_SETTINGS_MANAGE`,
`PERM_PLATFORM_SETTINGS_VIEW` · docs/DEVIATIONS.md [09] (`PLATFORM_SETTINGS_MANAGE` entry, docs/DEVIATIONS.md:139) ·
TC-CORE-SETTINGS-004
