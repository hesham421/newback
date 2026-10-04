# Step 06 — Two auth realms (STAFF / CUSTOMER) and a pluggable permission catalog

**Branch:** `step/06-auth-realms`
**Goal:** (a) self-registered customers (storefront accounts) and back-office staff share the identity infrastructure but are strictly separated realms with different login paths, token claims and authorities; (b) permissions are contributed by each module (core or app) at startup instead of a static cross-module `PermissionConstants` class in `sec`.
**Why:** `SEC_USER` has one realm; `SEC_SIGNUP_REQUEST` is admin-approved staff onboarding; `sec/permission/PermissionConstants.java` is referenced by 108 `@PreAuthorize` usages and knows every module — an app cannot add permissions without editing the library.

## Preconditions
- Step 05 merged.

## Design (fixed decisions)
- `SEC_USER.REALM VARCHAR(16) NOT NULL` ∈ {`STAFF`,`CUSTOMER`}. Uniqueness: `(TENANT_ID, REALM, USERNAME)` and `(TENANT_ID, REALM, EMAIL)` — a person may have both a staff and a customer account.
- **STAFF**: existing flows unchanged (login, admin-approved signup request, password reset, RBAC via role grants).
- **CUSTOMER**: `POST /api/v1/public/customers/register` (tenant via `X-Tenant-Code`; email + password + name; creates user `STATUS=PENDING_VERIFICATION`, sends verification email through `notif`), `POST /api/v1/public/customers/verify` (token), `POST /api/v1/public/customers/login`, `POST /api/v1/public/customers/password-reset/{request,complete}`, `GET/PATCH /api/v1/customers/me`. Customers have **no roles**; their single authority is `ROLE_CUSTOMER`. Customer endpoints in future apps authorize with `hasRole('CUSTOMER')`.
- **JWT claims:** `realm`, `tid`, existing `jti`. Staff tokens are rejected on customer endpoints and vice-versa (checked by the filter via claim, not by path convention).
- **Security chains:** core exposes two `SecurityFilterChain` beans: `erpCoreCustomerSecurityFilterChain` (`@Order(90)`, `securityMatcher("/api/v1/public/**", "/api/v1/customers/**")`) and the existing `erpCoreSecurityFilterChain` (`@Order(100)`). Both back off with `@ConditionalOnMissingBean(name=...)`.
- **Permission catalog SPI** (`com.erp.sec.permission`):
  ```java
  public record PermissionDef(String moduleCode, String screenCode, String actionCode, String nameAr, String nameEn) {
      public String authority() { return moduleCode + ":" + screenCode + ":" + actionCode; }
  }
  public interface PermissionContributor { List<PermissionDef> permissions(); }
  ```
  Each core module provides one contributor (`SecPermissions`, `FilePermissions`, `NotifPermissions`, `MdlPermissions`, `CuPermissions`, `TenantPermissions`) exposing `public static final String` constants **inside the module** for its own `@PreAuthorize` strings. `PermissionCatalogSynchronizer` (`ApplicationRunner`) upserts contributed defs into `SEC_MODULE_REG/SEC_SCREEN_REG/SEC_ACTION_REG` (global tables) on startup; never deletes.
  The authority string format must equal what `MenuService.effectiveAuthorityCodes()` already produces — read the current format and keep it; if it differs from `module:screen:action`, adopt the existing format and document it.

## Tasks
1. **Migration `V11__sec_realms.sql`**: add `REALM` (default `'STAFF'` then drop default), rebuild unique constraints as above, add `SEC_CUSTOMER_VERIFY_TOKEN` (ID, TENANT_ID, USER_ID, TOKEN_HASH, EXPIRES_AT, USED_AT, audit, version).
2. **Entities/services**: `SecUser.realm`; `CustomerAccountService` (register/verify/login/reset/me) reusing `TokenHasher`, `PasswordEncoder`, bucket4j rate limiting (key `tenant:realm:username`), `NotificationDispatchApi` with new templates `CUSTOMER_VERIFY_EMAIL`, `CUSTOMER_PASSWORD_RESET` (seeded in `V11`, AR/EN).
3. **JWT**: add `realm` claim; `JwtAuthenticationFilter` populates authorities: STAFF → RBAC as today; CUSTOMER → `ROLE_CUSTOMER` only (no DB grant query).
4. **Chains**: implement the customer chain in `ErpCoreSecurityAutoConfiguration`; public paths for the customer chain: `register`, `verify`, `login`, `password-reset/**`.
5. **Permission catalog**: create the SPI; delete `PermissionConstants.java`; create per-module constants + contributors; replace all `@PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).X)")` with the module-local constant (`T(com.erp.file.permission.FilePermissions).X`). Implement `PermissionCatalogSynchronizer`. Remove registry seed rows for screens/actions from `V7__sec_seed.sql` **only if** the synchronizer reproduces them identically on first start (keep roles, grants and admin in the seed; grants reference action codes, so the synchronizer must run before any grant lookup — it does, grants are evaluated per request).
6. **Role grants seed**: since the bootstrap admin role must still hold all core permissions, add a `SYS_ADMIN_ALL` behaviour: role flag `IS_SUPER BOOLEAN` on `SEC_ROLE`; `MenuService` returns all catalog authorities for super roles. Seed the bootstrap admin role as super. This removes the need to re-seed grants whenever a module adds a permission.
7. **Staff signup request**: unchanged, but explicitly `REALM='STAFF'`.
8. **ArchUnit**: extend `CrossModuleBoundaryArchTest` with a rule: `sec..` must not depend on `file..|notif..|mdl..|cu..` except through `crossmodule` packages (the old constants file violated this).
9. **i18n**: `REALM_MISMATCH`, `CUSTOMER_EMAIL_TAKEN`, `CUSTOMER_NOT_VERIFIED`, `VERIFY_TOKEN_INVALID`.
10. **Tests**: customer register→verify→login→me happy path; duplicate email per tenant → 409; staff token on `/api/v1/customers/me` → 403; customer token on `/api/v1/sec/users` → 403; same email as STAFF and CUSTOMER both allowed; catalog synchronizer idempotent (run twice, row counts stable); super role sees new permission contributed by a test-only contributor.

## Acceptance
- All tests green; `PermissionConstants.java` does not exist; `grep -rn "PermissionConstants" erp-core` is empty.
- Every `@PreAuthorize` references a constant class inside its own module.
- OpenAPI shows customer endpoints under group `sec` (or a new group `customers`).

## Verification commands
```bash
mvn -q verify
grep -rn "PermissionConstants" erp-core/src || echo OK
```

## Commit
`step(06): STAFF/CUSTOMER realms with customer self-registration, per-module permission contributors replacing static constants, super role`

## Out of scope
OAuth/OIDC, social login, MFA/OTP, refresh tokens.
