/** tm-F2 harness (API only, as the PLATFORM admin): a tenant's logo and brand colour, suspend with a reason, revoke its tokens. */
import { expect, type APIRequestContext } from '@playwright/test'
import { backendUrl } from './env.ts'
import { PHOTO_A } from './secApi.ts'
import { bearer } from './session.ts'
import { HarnessApi, type TenantRow } from './tenantHarness.ts'

export interface BrandingAnswer {
  code: string
  nameEn: string | null
  logoUrl: string | null
  brandColor: string | null
}

/** Uploads the 2×2 PNG as the tenant's logo and returns the new public `logoUrl`. */
export async function setTenantLogo(request: APIRequestContext, token: string, tenant: TenantRow): Promise<string> {
  const response = await request.put(`${backendUrl}/api/v1/platform/tenants/${tenant.id}/logo`, {
    headers: bearer(token),
    multipart: { file: { name: 'logo.png', mimeType: 'image/png', buffer: PHOTO_A } },
  })
  expect(response.status(), `logo of ${tenant.code}`).toBe(200)
  const logoUrl = ((await response.json()) as { data: { logoUrl: string | null } }).data.logoUrl
  expect(logoUrl, `logoUrl of ${tenant.code}`).toBeTruthy()
  return logoUrl as string
}

export async function setBrandColor(api: HarnessApi, tenant: TenantRow, brandColor: string | null): Promise<string | null> {
  const answer = await api.ok<{ brandColor: string | null }>('PATCH', `/api/v1/platform/tenants/${tenant.id}/branding`, { brandColor })
  return answer.brandColor
}

export async function suspendTenant(api: HarnessApi, tenant: TenantRow): Promise<void> {
  await api.ok('PATCH', `/api/v1/platform/tenants/${tenant.id}/status`, {
    statusCode: 'SUSPENDED',
    reason: 'E2E tm-F2 suspended-branding case',
  })
}

export async function activateTenant(api: HarnessApi, tenant: TenantRow): Promise<void> {
  await api.ok('PATCH', `/api/v1/platform/tenants/${tenant.id}/status`, { statusCode: 'ACTIVE' })
}

export async function revokeTenantTokens(api: HarnessApi, tenant: TenantRow): Promise<void> {
  await api.ok('POST', `/api/v1/platform/tenants/${tenant.id}/revoke-tokens`)
}

export async function publicBranding(request: APIRequestContext, code: string): Promise<BrandingAnswer> {
  const response = await request.get(`${backendUrl}/api/v1/public/tenants/${code}/branding`)
  expect(response.status(), `public branding of ${code}`).toBe(200)
  return ((await response.json()) as { data: BrandingAnswer }).data
}
