# Step 06 — Two auth realms (STAFF / CUSTOMER) and a pluggable permission catalog — report

## Summary

**Realms.** `SEC_USER.REALM` holds `STAFF` or `CUSTOMER`. Usernames and e-mails are unique per `(tenant, realm)`, so one person can have a staff account and a customer account in the same tenant.

The STAFF flows are unchanged: login, the admin-approved sign-up (now explicitly `REALM='STAFF'`), password reset and RBAC. The existing staff lookups in `UserRepository` keep their names and are now STAFF-only `@Query` methods.

The new CUSTOMER realm (`CustomerAccountService`) has these endpoints:
- `POST /api/v1/public/customers/register`: tenant from `X-Tenant-Code`; creates a `PENDING_VERIFICATION` account and mails `CUSTOMER_VERIFY_EMAIL` through NOTIF's `NotificationDispatchApi`.
- `POST /api/v1/public/customers/verify`: checks a hashed, single-use token (`SEC_CUSTOMER_VERIFY_TOKEN`, valid 24 h).
- `POST /api/v1/public/customers/login`: bucket4j rate limit keyed `tenant:realm:username`; answers 429 `CUSTOMER_LOGIN_RATE_LIMITED`.
- `POST /api/v1/public/customers/password-reset/{request,complete}`: mails `CUSTOMER_PASSWORD_RESET`.
- `GET` / `PATCH /api/v1/customers/me`.

Customers hold no roles. Their only authority is `ROLE_CUSTOMER`.

**JWT.** Tokens carry `realm`, `tid` and `jti`. `JwtAuthenticationFilter` looks the user up by `(username, realm)`:
- STAFF gets RBAC authorities, as before.
- CUSTOMER gets `ROLE_CUSTOMER` only, with no grant query.

The filter attaches the realm to the authentication as `AuthRealm` details.

**Chains.**
- `erpCoreCustomerSecurityFilterChain` (`@Order(90)`, `securityMatcher("/api/v1/public/**", "/api/v1/customers/**")`) opens `erp.core.security.customer-public-paths`. Every other path in it needs `ROLE_CUSTOMER`.
- `erpCoreSecurityFilterChain` (`@Order(100)`) is unchanged.

Both chains back off by bean name. Each one runs the JWT filter, the tenant filter and a `RealmEnforcementFilter` for its own realm. A token from the other realm gets 403 `REALM_MISMATCH`; this is checked from the claim, not from the path.

**Permission catalog.** `PermissionConstants.java` is deleted. The SPI lives in `com.erp.sec.permission`: `PermissionDef`, `PermissionContributor`, plus `PermissionModule` / `PermissionScreen`. Each core module has one contributor, which also holds that module's constants: `SecPermissions`, `FilePermissions`, `NotifPermissions`, `MdlPermissions`, `CuPermissions` and `TenantPermissions`. All 75 `@PreAuthorize` `T(...)` references point at the class of their own module.

`PermissionCatalogSynchronizer` is an `ApplicationRunner` that runs first, as PLATFORM, in one transaction. It upserts the catalog into the global `SEC_*_REG` tables, never deletes, and running it twice changes nothing. The authority format is still the registry's `PERMISSION_CODE` (`PERM_<SCREEN>_<ACTION>`), which is exactly what `MenuService` produces.

**Super role.** `SEC_ROLE.IS_SUPER` is set for every tenant's `SYS_ADMIN`, and tenant provisioning copies it. `MenuService` gives a super role every active catalog authority. PLATFORM-module authorities are included only inside the PLATFORM tenant.

**ArchUnit.**
- New rule: `sec..` may reach `file|notif|mdl|cu` only through their `crossmodule` packages.
- The SpEL exception for the old constants class is gone.
- `com.erp.sec.permission` is now part of SEC's public surface.

`mvn -q verify` is green: erp-core has 138 tests (111 before, plus 27 new), erp-app-reference has 7.

## Files changed

**Created**
- `erp-core/src/main/resources/db/migration/core/V11__sec_realms.sql`
- `sec/permission/`: `PermissionDef`, `PermissionContributor`, `PermissionModule`, `PermissionScreen`, `SecPermissions`
- The other module contributors:
  - `cu/permission/CuPermissions`
  - `file/permission/FilePermissions`
  - `mdl/permission/MdlPermissions`
  - `notif/permission/NotifPermissions`
  - `tenant/permission/TenantPermissions`
- `sec/service/PermissionCatalogSynchronizer`, `sec/service/CustomerAccountService`
- `sec/entity/CustomerVerifyToken`, `sec/domain/CustomerVerifyTokenDomain`, `sec/repository/CustomerVerifyTokenRepository`
- `sec/dto/`: `CustomerRegisterRequest`, `CustomerVerifyRequest`, `CustomerLoginRequest`, `CustomerProfileUpdateRequest`, `CustomerProfileResponse`
- `sec/mapper/CustomerAccountMapper`
- `sec/controller/PublicCustomerController`, `sec/controller/CustomerProfileController`
- `sec/security/AuthRealm`, `RealmEnforcementFilter`, `LoginRateLimiter`
- Tests:
  - `sec/CustomerRealmIntegrationTest` (9)
  - `sec/PermissionCatalogIntegrationTest` (4)
  - `sec/RealmHttp` (helper)
  - `sec/domain/CustomerDomainRulesTest` (4)
  - `sec/permission/PermissionDefTest` (4)
  - `sec/security/LoginRateLimiterTest` (3)
- `docs/steps/06-report.md`

**Modified**
- `autoconfigure/`:
  - `ErpCoreSecurityAutoConfiguration`: customer chain, realm filters, `PLATFORM_TENANT_MANAGE_AUTHORITY` taken from `TenantPermissions`.
  - `ErpCoreProperties`: `customer-public-paths`, `customer-login-rate-limit`, `frontend.customer-verify-path` / `customer-password-reset-path`.
  - `ErpCoreOpenApiAutoConfiguration`: group `customers`.
- `common/domain/status/Status`: `TOO_MANY_REQUESTS`.
- `sec/entity/User` (`realm`, `markVerified`, per-realm uniques) and `sec/entity/Role` (`isSuper`).
- `sec/repository/`:
  - `UserRepository`: STAFF-scoped queries plus realm-aware variants.
  - `RoleRepository`: `holdsActiveSuperRole`.
  - `ActionRegistryRepository`: `findByPermissionCode`, `findActiveAuthorityCodesExcludingModules`.
- `sec/security/`:
  - `JwtTokenIssuer`: `realm` claim.
  - `JwtAuthenticationFilter`: lookup by realm, CUSTOMER authorities, `AuthRealm` details.
  - `SecSecurityErrorHandler`: `write` is now public.
- `sec/service/`:
  - `MenuService`: super role.
  - `PasswordResetService`: realm check on token completion.
  - `UserService`: `UserContact.active` via `canReceiveNotifications`.
- `sec/domain/UserDomain`: customer rules. `sec/domain/PasswordResetTokenDomain`: realm-aware `assertUsable`.
- `sec/dto/UserResponse` and `RoleResponse`, `sec/mapper/UserMapper` and `RoleMapper`: `realm` / `isSuper` fields; staff users explicitly STAFF.
- `sec/exception/SecErrorCodes`: 5 codes.
- `sec/tenant/SecTenantProvisioningContributor`: copies `IS_SUPER`, the administrator is STAFF, local `PLATFORM_TENANT_MANAGE` literal.
- `@PreAuthorize` constant class switched to the module's own class in:
  - `cu/service/ConfigurationService`
  - `file/service/FileCategoryService` and `FileService`
  - `mdl/service/LookupConsumerService`, `LookupTypeService` and `LookupValueService`
  - `notif/service/NotificationChannelConfigService`, `NotificationLogService` and `NotificationTemplateService`
  - `sec/service/AuditLogService`, `DashboardService`, `RegistryService`, `RoleGrantService`, `RoleService`, `SessionService`, `SignupRequestService`, `UserRoleService` and `UserService`
  - `tenant/service/TenantService`
- `resources/i18n/messages.properties` and `messages_ar.properties`: step-06 block. `db/migration/core/README.md`: V11 row.
- Tests:
  - `architecture/CrossModuleBoundaryArchTest`
  - `autoconfigure/ErpCoreAutoConfigurationTest`
  - `sec/SecCoverageIntegrationTest`, `SecFrontendGapIntegrationTest`, `SecReadOneIntegrationTest`, `SecSearchFilterIntegrationTest`, `UserRolesInResponseIntegrationTest` (import of `SecPermissions`)
  - `tenant/TenantHttp`, `TenantSchemaIntegrationTest`, `PlatformTenantApiIntegrationTest`
  - `erp-app-reference/.../ReferenceApplicationSmokeTest`
- `docs/DEVIATIONS.md`: 15 `[06]` entries.

**Deleted**: `erp-core/src/main/java/com/erp/sec/permission/PermissionConstants.java`.

**Untouched**: `erp-core-plan/execution-state.json`, `V7__sec_seed.sql` (an applied core migration), `erp-app-reference/governance/`.

## Decisions & deviations

These mirror the 15 `[06]` entries in `docs/DEVIATIONS.md`.

1. **Authority format.** The registry's `PERM_<SCREEN>_<ACTION>` is kept instead of `module:screen:action`. `PermissionDef` has an optional sixth component, `authority`, for the legacy literals `CONFIG_*` and `PLATFORM_TENANT_MANAGE`.
2. **`PermissionContributor` extras.** It has default methods `modules()` and `screens()`, because module and screen rows need names. The synchronizer inserts new rows and renames existing ones only when a contributor declares different names. It never deletes a row and never touches `IS_ACTIVE_FL`.
3. **V7 not edited.** The synchronizer reproduces the V7/V10 catalog row for row (this is tested), so the seed rows are just redundant. The acceptance grep still matches one SQL comment in V7, and V7's checksum must not change.
4. **ArchUnit.** `com.erp.sec.permission` is part of SEC's public surface. The SpEL exception was removed and the task-8 rule added. One sec test now uses a NOTIF permission as a literal string.
5. **V11's non-additive parts.** The `REALM` default is dropped (as the step prescribes). The user uniques are re-created as `(TENANT_ID, REALM, …)`. The status CHECK is widened with `PENDING_VERIFICATION`.
6. **Staff lookups.** The `UserRepository` staff lookups are STAFF-scoped. The staff user API is not realm-filtered, and the response now shows `realm`.
7. **Customer details the step leaves open.**
   - Customer resets reuse `SEC_PWD_RESET_TOKEN`, with a realm check on both completion endpoints.
   - Verification tokens are valid for 24 h.
   - Completing a reset also verifies the account.
   - The register body has a single `fullName`; the username is the e-mail.
   - There is no customer logout endpoint.
8. **Realm enforcement.** It uses the `realm` claim and `AuthRealm` details, through a `RealmEnforcementFilter` in each chain. Tokens without `realm` no longer authenticate.
9. **Notifications to unverified customers.** `UserContact.active` is true for ACTIVE and for PENDING_VERIFICATION, so the verification mail is not skipped. The `NotificationDispatchApi` signature is untouched.
10. **Templates.** They are seeded for every tenant that exists at migration time. Later tenants get them through the existing NOTIF provisioning contributor, which copies all templates. The template-count assertion went from 2 to 4.
11. **Super role.** It is set on `SYS_ADMIN` in every tenant and copied by provisioning. PLATFORM-module authorities stay inside the PLATFORM tenant. The menu still comes from grants.
12. **Rate limit.** bucket4j, in memory and per JVM; 10 attempts per minute by default; customer login only; 429 with a new code and `Status.TOO_MANY_REQUESTS`.
13. **Audit.** No `SEC_AUDIT_LOG` rows for customer events.
14. **New properties, placement and OpenAPI.** New properties: `customer-public-paths`, the customer frontend paths and the rate limit. The synchronizer lives in `sec.service`. The customer endpoints appear in the OpenAPI groups `customers` and `sec`.
15. **Existing test adaptations.** No assertion was weakened. See *Files changed*.

## Acceptance checklist

3/3 ✅ (the grep bullet with the recorded V7 exception)

- ✅ **All tests green; `PermissionConstants.java` does not exist; `grep -rn "PermissionConstants" erp-core` is empty.**
  - `mvn -q verify` EXIT=0: erp-core 138/0/0/0, erp-app-reference 7/0/0/0.
  - The file is deleted (`D` in the commit).
  - The grep finds no Java or test reference. One match remains: the SQL comment at `V7__sec_seed.sql:23`. V7 is an applied core migration, and editing it would change its Flyway checksum (deviation 3).
  - `CrossModuleBoundaryArchTest.the_shared_permission_constants_class_is_gone` asserts that the class is gone.
- ✅ **Every `@PreAuthorize` references a constant class inside its own module.**
  - Enforced by `CrossModuleBoundaryArchTest.spel_type_references_do_not_bypass_the_module_boundary`, which no longer has an exception and passes.
  - The grep lists, by `T(...)` class: CuPermissions 5, FilePermissions 12, MdlPermissions 11, NotifPermissions 12, SecPermissions 30, TenantPermissions 5.
  - There are no `hasAuthority('literal')` gates.
- ✅ **OpenAPI shows the customer endpoints under group `sec` (or a new group `customers`).**
  - `CustomerRealmIntegrationTest.openApi_listsTheCustomerEndpoints_underTheCustomersAndSecGroups` checks that `/v3/api-docs/customers` and `/v3/api-docs/sec` both contain all six customer paths.

The step's tests (task 10) are all green:

| Task 10 test | Where |
|---|---|
| register → verify → login → me | `register_verify_login_me_happyPath` (NOTIF_LOG row asserted, token claims `realm=CUSTOMER` and `tid`) |
| duplicate e-mail per tenant → 409 | `duplicateEmailInTheSameTenant_is409_whileAnotherTenantMayReuseIt` |
| staff token on `/api/v1/customers/me` → 403 | `staffToken_onCustomerEndpoint_is403RealmMismatch` |
| customer token on `/api/v1/sec/users` → 403 | `customerToken_onStaffEndpoints_is403` |
| the same e-mail as STAFF and as CUSTOMER | `theSameEmail_mayHoldAStaffAndACustomerAccount` |
| synchronizer is idempotent | `synchronizer_isIdempotent_rowCountsStable` |
| super role sees a test-only contributor's permission | `superRole_holdsAPermissionContributedByATestOnlyContributor_withoutAnyGrant` |

There are also failure-path, reset, rate-limit and cross-tenant super-role tests.

## Verification output

```
$ java -version → openjdk 21.0.7
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q -o verify
EXIT=0 secs=166      [TestPostgres] backend: EMBEDDED

surefire totals (target/surefire-reports/TEST-*.xml)              tests fail err skip
== erp-core
com.erp.architecture.CrossModuleBoundaryArchTest                   4 0 0 0   (+2)
com.erp.autoconfigure.DownloadTokenStoreAutoConfigurationTest      2 0 0 0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest                10 0 0 0   (+1)
com.erp.autoconfigure.ErpCoreFlywayAutoConfigurationTest           3 0 0 0
com.erp.autoconfigure.MigrationNamingTest                          2 0 0 0
com.erp.file.service.InMemoryDownloadTokenStoreTest                3 0 0 0
com.erp.notif.service.DefaultChannelProviderTest                   2 0 0 0
com.erp.sec.BootstrapAdminPasswordIntegrationTest                  4 0 0 0
com.erp.sec.CustomerRealmIntegrationTest                           9 0 0 0   (new)
com.erp.sec.MenuServiceGatewayIntegrationTest                      2 0 0 0
com.erp.sec.PermissionCatalogIntegrationTest                       4 0 0 0   (new)
com.erp.sec.SecCoverageIntegrationTest                             6 0 0 0
com.erp.sec.SecFrontendGapIntegrationTest                          5 0 0 0
com.erp.sec.SecLogoutIntegrationTest                               3 0 0 0
com.erp.sec.SecReadOneIntegrationTest                              6 0 0 0
com.erp.sec.SecSearchFilterIntegrationTest                        10 0 0 0
com.erp.sec.TenantScopedQueryIntegrationTest                       3 0 0 0
com.erp.sec.UserRolesInResponseIntegrationTest                     8 0 0 0
com.erp.sec.domain.CustomerDomainRulesTest                         4 0 0 0   (new)
com.erp.sec.permission.PermissionDefTest                           4 0 0 0   (new)
com.erp.sec.security.LoginRateLimiterTest                          3 0 0 0   (new)
com.erp.tenant.PlatformTenantApiIntegrationTest                    8 0 0 0
com.erp.tenant.TenantContextIntegrationTest                        1 0 0 0
com.erp.tenant.TenantContextTest                                   4 0 0 0
com.erp.tenant.TenantIsolationIntegrationTest                      6 0 0 0
com.erp.tenant.TenantSchemaIntegrationTest                         4 0 0 0
com.erp.tenant.domain.TenantDomainTest                            16 0 0 0
com.erp.testsupport.TestProfileWiringIntegrationTest               2 0 0 0
erp-core tests=138 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                          7 0 0 0   (Flyway 2..11, 1000)
erp-app-reference tests=7 failures=0 errors=0 skipped=0

$ grep -rn "PermissionConstants" erp-core/src || echo OK
erp-core/src/main/resources/db/migration/core/V7__sec_seed.sql:23:--     they equal the CU constants in PermissionConstants that hasAuthority(...) compares against;
  → an applied migration's SQL comment; cannot change without changing V7's Flyway checksum (deviation 3)

$ grep -rhoE "T\(com\.erp\.[a-z]+\.permission\.[A-Za-z]+\)" erp-core/src/main/java | sort | uniq -c
  5 CuPermissions  12 FilePermissions  11 MdlPermissions  12 NotifPermissions  30 SecPermissions  5 TenantPermissions

Failed attempt on the way (fixed): the first full verify failed in
ErpCoreAutoConfigurationTest.coreCustomerSecurityFilterChain_backsOff... — the test's replacement chain
had no @Order, so Spring Security put it after the any-request staff chain (UnreachableFilterChainException).
The fixture now registers it with @Order(90); this is a real rule for applications (see Notes).
```

## Skills checked

- **`.claude/skills/build-create-entity`**
  - `CustomerVerifyToken` is compliant: `AuditableEntity`, `@SuperBuilder`, `SEQ_SEC_CUSTOMER_VERIFY_TOKEN` with `allocationSize = 1`, `@ManyToOne` LAZY with a named FK, the unique constraint and index inside `@Table`, the default window set in `@PrePersist`.
  - Named deviations: the PK column is `ID` (README §4 for new tables). There is no `activate()`/`deactivate()`, because it is a short-lived token like `PasswordResetToken`.
  - Its Domain object `CustomerVerifyTokenDomain` is a plain class with `from(...)` that throws `LocalizedException`.
  - `User.realm` and `Role.isSuper` were added as fields (native BOOLEAN, so no converter).
- **`build-create-repository`**: compliant. The new methods are all called from services or the synchronizer, and the existence checks use `existsBy`.
- **`build-create-dto`**: compliant for the customer DTOs. They use `@Schema` with bilingual text and examples, i18n validation keys and UTC `@JsonFormat`. Named deviation: there are no Search, Usage or Update/Create sets, because these are self-service account shapes and not a CRUD feature.
- **`build-create-mapper`**: `CustomerAccountMapper` is compliant: manual, null-safe, with a void in-place update that skips immutable fields.
- **`build-create-service`**
  - `CustomerAccountService` is compliant on `@Service @RequiredArgsConstructor @Slf4j`, transactions, `ServiceResult`, `LocalizedException` and Domain delegation (`UserDomain.createCustomer/assertCustomerVerified`, the token domains). NOTIF is reached only through `notif.crossmodule`.
  - Named deviation: the five public operations have no `@PreAuthorize`, the same pre-authentication precedent as `AuthService.login` and `PasswordResetService`. The `me` operations use `SecPermissions.ROLE_CUSTOMER`.
  - `PermissionCatalogSynchronizer` is startup infrastructure with no principal (the `BootstrapAdminPasswordRunner` precedent).
- **`build-create-controller`**: both controllers are thin and use `OperationCode.craftResponse`, `@Valid @RequestBody`, `@Operation` and a bilingual `@Tag`. Named deviation: they are not CRUD, so there are no search, usage or activate endpoints.
- **`gov-enforce-error-handling`**: compliant.
  - Every throw is a `LocalizedException` with a code from `SecErrorCodes` or `CommonErrorCodes`.
  - The five new codes have EN and AR messages.
  - Status by case: 409 `ALREADY_EXISTS`/`CONFLICT`, 403 `FORBIDDEN`, 429 `TOO_MANY_REQUESTS`.
  - The SPI records log and skip invalid definitions; they do not throw.
- **`gov-enforce-caching-rules`**: compliant. No cache was added; the rate limiter is a bucket store, not a Spring cache.
- **`gov-enforce-backend-contract`** and **`gov-validate-backend-feature`** (customer-accounts feature): the shape is not CRUD, so CRUD-only checks are scored as named deviations. Those are Usage, Search and activate/deactivate (A.3.9/12, A.6.5–A.6.8, A.1.18), plus the four-permission rule (realm authority instead of VIEW/CREATE/UPDATE/DELETE). No automatic-rejection trigger is hit. The CU.1–CU.8 checks pass.

## Notes for later steps

- **Step 07 (public files).**
  - Append `/api/v1/public/files/**` to `ErpCoreProperties.Security.DEFAULT_CUSTOMER_PUBLIC_PATHS`. That is the single list behind `erp.core.security.customer-public-paths`, and it is the customer chain's `permitAll`. A path left off it under `/api/v1/public/**` needs a CUSTOMER token.
  - Customer-chain public paths require `X-Tenant-Code` unless they are in `erp.core.tenant.exempt-paths`, the same rule as step 05.
  - `RealmEnforcementFilter` ignores stray tokens on public paths.
- **New permissions (any module or app).**
  - Add a `@Component` that implements `com.erp.sec.permission.PermissionContributor` (convention: `com.erp.<module>.permission.<Module>Permissions`). It holds the `public static final` authority constants and declares modules, screens (with names) and `PermissionDef`s, one of them a `VIEW` per screen (RULE-SEC-007).
  - Reference only your own class in `@PreAuthorize` (ArchUnit enforces this).
  - The synchronizer upserts at startup, and every `SYS_ADMIN` (super role) holds the new permission at once. No seed or grant migration is needed.
  - The authority format is `PERM_<SCREEN>_<ACTION>`, unless you pass an explicit `authority`.
- **Realms.**
  - Customer endpoints in apps go under `/api/v1/customers/**` (or a chain of the app's own) and authorize with `hasRole('CUSTOMER')` / `SecPermissions.ROLE_CUSTOMER`.
  - Staff code must keep using the STAFF-scoped `UserRepository` methods. For a customer, use `findByUsernameAndRealm(..., User.REALM_CUSTOMER)`.
  - Tokens now carry `realm`, and a token without it does not authenticate.
  - An app that replaces either core chain must give the replacement the same `@Order` (90 or 100). Otherwise Spring Security rejects it as unreachable behind the any-request staff chain.
  - The replacement must add `RealmEnforcementFilter` and `TenantResolutionFilter` itself.
- **Notifications (step 08).**
  - SEC dispatches `CUSTOMER_VERIFY_EMAIL` and `CUSTOMER_PASSWORD_RESET` through `NotificationDispatchApi.dispatchIndependently` inside `InternalCallerContext`, with variables `token`, `expiresAt`, `email`, `actionLink`, `ctaLabelEn` and `ctaLabelAr`.
  - Keep that signature and the recipient-active semantics: `UserContact.active` is true for `PENDING_VERIFICATION` as well.
  - `CustomerRealmIntegrationTest` spies on the dispatch API to read the token, and asserts one `NOTIF_LOG` row per mail. If step 08 makes dispatch asynchronous, that test must wait for the row.
- **Schema counts.** `TenantSchemaIntegrationTest` now expects 19 tenant tables, 14 unique constraints and 19 tenant entities. `PlatformTenantApiIntegrationTest` expects 4 templates per new tenant.
- **Shared files touched**:
  - `ErpCoreSecurityAutoConfiguration`, `ErpCoreProperties`, `ErpCoreOpenApiAutoConfiguration`
  - `common/domain/status/Status`
  - the i18n bundles (own block at the end)
  - `docs/DEVIATIONS.md`, `db/migration/core/README.md`
  - `CrossModuleBoundaryArchTest` (the `sec` MODULES entry, the SpEL rule, two new rules)
  - `TenantSchemaIntegrationTest` counts, `ErpCoreAutoConfigurationTest`, `ReferenceApplicationSmokeTest`, `TenantHttp`
  - every module's services (`@PreAuthorize` class names)
- **Not done (out of scope or unspecified):** customer logout, e-mail change, audit rows for customer events, and a staff-login rate limiter.
