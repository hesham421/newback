/** Harness-side PLATFORM calls (never through the UI): tenant status changes used to set up and heal the suspended-tenant cases. */
import { expect, type APIRequestContext } from '@playwright/test'
import { backendUrl, PLATFORM, USERS } from './env.ts'
import { apiSignIn, bearer } from './session.ts'

export async function setTenantStatus(
  request: APIRequestContext,
  code: string,
  statusCode: 'ACTIVE' | 'SUSPENDED'
): Promise<void> {
  expect(code, 'never suspend PLATFORM').not.toBe(PLATFORM)
  const token = await apiSignIn(request, PLATFORM, USERS.admin)
  try {
    const search = await request.post(`${backendUrl}/api/v1/platform/tenants/search`, {
      headers: bearer(token),
      data: { filters: [{ field: 'code', operator: 'LIKE', value: code }], page: 0, size: 5 },
    })
    const rows = ((await search.json()) as { data: { content: Array<{ id: number; code: string }> } }).data.content
    const tenant = rows.find((row) => row.code === code)
    expect(tenant, `tenant ${code}`).toBeDefined()
    const patched = await request.patch(`${backendUrl}/api/v1/platform/tenants/${tenant?.id}/status`, {
      headers: bearer(token),
      data: { statusCode },
    })
    expect(patched.status(), `${code} → ${statusCode}`).toBe(200)
  } finally {
    await request.post(`${backendUrl}/api/v1/sec/auth/logout`, { headers: bearer(token) })
  }
}
