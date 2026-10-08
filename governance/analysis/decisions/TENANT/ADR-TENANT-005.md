# ADR-TENANT-005 — The tenant logo is set by the platform administrator from `PLATFORM_TENANTS`; no tenant self-service screen in 1.3.0

Module  : TENANT     Version : erp-core 1.3.0 (tenant-maturity plan, package E)     Stage raised : P0 (Policies) — decision D5 of the plan, recorded before the code
Status  : ACCEPTED (decision D5, 2026-10-07)

## Context
Package E gives a tenant a logo (and an optional brand colour) that the frontend shows beside the
platform mark after login and on the login page (`docs/plans/tenant-maturity-plan.md` §7, §8 F2).
Someone must upload it. Two owners were possible:
- **The tenant's own administrator**, through a new per-tenant screen `TENANT_BRANDING` with its own
  gateway action (`PERM_TENANT_BRANDING_VIEW`) and a manage action, under a registry module the
  permission catalog does not have today; the screen would be copied into every tenant's `SYS_ADMIN`
  grants by `SecTenantProvisioningContributor` and would need its own frontend module, route, menu
  entry and E2E archive.
- **The platform administrator**, from the existing `PLATFORM_TENANTS` screen (`SCR-REQ-TENANT-001`),
  behind the existing `PLATFORM_TENANT_MANAGE`, as one more attribute of the tenant record — the same
  place where the tenant is created, suspended and (from 1.3.0, package B) edited.

The permission catalog is code-defined and global (`TenantPermissions`,
`erp-core/src/main/java/com/erp/tenant/permission/TenantPermissions.java:20-45`); every new screen is a
registry row, a grant row per tenant and a frontend page code (`PROJECT-OVERVIEW.md`, `project-registry.md`).
The branding read side needs no permission at all: `GET /api/v1/tenant/me` is `isAuthenticated()` for a
token of either realm and the public branding endpoint is anonymous (plan §7 E.2).

## Decision
**Decision D5 of the plan, as taken on 2026-10-07:** the logo and the brand colour are set, replaced
and removed by a platform operator holding `PLATFORM_TENANT_MANAGE`, from the `PLATFORM_TENANTS`
screen, through `PUT` / `DELETE /api/v1/platform/tenants/{id}/logo` and
`PATCH /api/v1/platform/tenants/{id}/branding` (RULE-TENANT-020). No module, screen, permission or grant
seed is added (`V20__tenant_branding.sql` carries no registry rows); the frontend adds a branding row to
the tenant detail drawer (plan §8 F3), not a screen. A tenant administrator has no write path to
branding in 1.3.0; every user of the tenant reads it through `GET /api/v1/tenant/me`, and an anonymous
visitor through `GET /api/v1/public/tenants/{tenantCode}/branding`.

The logo document is stored in the **target tenant's own rows** (`FILE_DOCUMENT.TENANT_ID = {id}`,
written inside `TenantContext.callAs(id)`, `ownerType = CORE_TENANT`, `ownerId = {id}`,
`moduleCode = TENANT`, PUBLIC with a random slug — FILE ADR-FILE-008), so its public URL is
`/api/v1/public/files/{thatTenantCode}/{slug}` and the tenant's suspension withdraws it with the rest
of the tenant's public files (REQ-TENANT-010).

Reasons:
1. **Branding is onboarding.** The platform operator provisions the tenant and hands it over; the logo
   belongs to that handover, like the first administrator (POL-TENANT-010).
2. **No catalog growth for one field.** A self-service screen costs a registry module, a screen, two
   actions, a grant per tenant, a page code and a frontend module for an attribute that changes rarely.
3. **Platform-only is the existing shape.** `PLATFORM_TENANT_MANAGE` already covers every tenant
   attribute; the chain gate `isPlatformOperator` and RULE-TENANT-007 keep it inside PLATFORM.
4. **The read side needs nothing.** The shell and the login page read branding without a permission,
   so no tenant user is blocked by the decision.

## Consequences
- A tenant that wants to change its logo asks the platform operator (a support operation, audited as
  `TENANT_LOGO_CHANGED` with the operator as actor, in that tenant's audit log and in PLATFORM's).
- The deferred alternative is recorded for a later version: a per-tenant `TENANT_BRANDING` screen
  (registry module to be chosen, gateway + manage actions, copied by provisioning, own frontend route);
  it would reuse the same columns (`LOGO_FILE_ID`, `BRAND_COLOR`), the same `FileImageStoreApi` and
  the same read endpoints — only the write path and its permission are new, so nothing in 1.3.0 has to
  be undone.
- The PLATFORM tenant may carry a logo like any tenant (RULE-TENANT-019); the platform **mark** stays a
  static frontend asset, so a missing tenant logo always has a fallback.
- The logo's size and type rules (≤ 1 MB; PNG, JPEG, WebP, plain SVG through FILE's allow-list) are
  FILE's validation, surfaced by the tenant service as `TENANT_LOGO_INVALID` (RULE-TENANT-018). An SVG
  logo is served as an attachment with `nosniff` and a sandbox CSP and is shown through `<img>` only
  (srs-tenant.md 1.3.0 E7).

## Traces
ENT-TENANT-001 · REQ-TENANT-029, REQ-TENANT-030, REQ-TENANT-031, REQ-TENANT-032 · RULE-TENANT-018,
RULE-TENANT-019, RULE-TENANT-020, RULE-TENANT-021 · POL-TENANT-014 · SCR-REQ-TENANT-001 ·
DBF-TENANT-043, DBF-TENANT-044 · XM-TENANT-003 · plan §0 D5, §7 E.1–E.3, §8 F3, §9
