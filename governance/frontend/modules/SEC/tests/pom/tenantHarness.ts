/** Harness-side PLATFORM / SEC calls for the 1.3.0 tenant cases (API only, idempotent): tenants, logos, admin passwords, the FILE-only user. */
import { expect, type APIRequestContext } from '@playwright/test'
import { backendUrl, password, PLATFORM, USERS } from './env.ts'
import { apiSignIn, bearer } from './session.ts'

export const T9 = { code: 'E2E_T9', admin: 't9admin' } as const

/** TC-FE-PLATFORM-022: a PLATFORM user with FILE_BROWSER only — no PLATFORM module, so no PLATFORM_TENANT_MANAGE. */
export const FILE_ONLY = { username: 'e2e.file', role: 'E2E_FILE_ONLY' } as const

interface Envelope<T> {
  data: T
  error?: { code: string }
}

export interface TenantRow {
  id: number
  code: string
  statusCode: string
  logoUrl?: string | null
  brandColor?: string | null
}

export class HarnessApi {
  private readonly request: APIRequestContext
  private readonly token: string

  private constructor(request: APIRequestContext, token: string) {
    this.request = request
    this.token = token
  }

  static async asAdmin(request: APIRequestContext): Promise<HarnessApi> {
    return new HarnessApi(request, await apiSignIn(request, PLATFORM, USERS.admin))
  }

  async call<T>(method: string, urlPath: string, data?: unknown): Promise<{ status: number; body: Envelope<T> | null }> {
    const response = await this.request.fetch(`${backendUrl}${urlPath}`, { method, headers: bearer(this.token), data })
    return { status: response.status(), body: (await response.json().catch(() => null)) as Envelope<T> | null }
  }

  async ok<T>(method: string, urlPath: string, data?: unknown): Promise<T> {
    const { status, body } = await this.call<T>(method, urlPath, data)
    expect(status, `${method} ${urlPath} → ${JSON.stringify(body?.error)}`).toBeLessThan(300)
    return (body?.data ?? null) as T
  }

  async tenant(code: string): Promise<TenantRow> {
    const page = await this.ok<{ content: TenantRow[] }>('POST', '/api/v1/platform/tenants/search', {
      filters: [{ field: 'code', operator: 'LIKE', value: code }],
      page: 0,
      size: 5,
    })
    const row = page.content.find((item) => item.code === code)
    expect(row, `tenant ${code}`).toBeDefined()
    return row as TenantRow
  }

  /** E2E_T9 present, ACTIVE, without logo / colour, and t9admin signing in with the E2E password and no pending change. */
  async ensureT9Baseline(): Promise<TenantRow> {
    const created = await this.call('POST', '/api/v1/platform/tenants', {
      code: T9.code,
      nameAr: 'مستأجر الاختبار التاسع',
      nameEn: 'E2E Tenant Nine',
      adminUsername: T9.admin,
      adminEmail: `${T9.admin}@example.test`,
      adminPassword: password,
      adminFullNameAr: 'مدير المستأجر التاسع',
      adminFullNameEn: 'Tenant Nine Admin',
    })
    expect([201, 409], `create ${T9.code}`).toContain(created.status)
    const tenant = await this.tenant(T9.code)
    if (tenant.statusCode !== 'ACTIVE') await this.ok('PATCH', `/api/v1/platform/tenants/${tenant.id}/status`, { statusCode: 'ACTIVE' })
    await this.clearBranding(tenant.id)
    await this.restoreAdminPassword(tenant.id, T9.admin)
    return this.tenant(T9.code)
  }

  async clearBranding(id: number): Promise<void> {
    await this.ok('DELETE', `/api/v1/platform/tenants/${id}/logo`)
    await this.ok('PATCH', `/api/v1/platform/tenants/${id}/branding`, { brandColor: null })
  }

  async restoreAdminPassword(id: number, username: string): Promise<void> {
    await this.ok('POST', `/api/v1/platform/tenants/${id}/admin-reset`, {
      username,
      newPassword: password,
      requireChangeAtNextLogin: false,
    })
  }

  /** The FILE-only role (module FILE, screen FILE_BROWSER, its four actions) and its user, created with no pending change. */
  async ensureFileOnlyUser(): Promise<void> {
    const roleCreated = await this.call('POST', '/api/v1/sec/roles', {
      code: FILE_ONLY.role,
      nameAr: 'اختبار الملفات فقط',
      nameEn: 'E2E file browser only',
    })
    expect([201, 409], `role ${FILE_ONLY.role}`).toContain(roleCreated.status)
    const roles = await this.ok<{ content: Array<{ rolePk: number; code: string }> }>('POST', '/api/v1/sec/roles/search', {
      filters: [{ field: 'code', operator: 'LIKE', value: FILE_ONLY.role }],
      page: 0,
      size: 5,
    })
    const roleId = roles.content.find((row) => row.code === FILE_ONLY.role)?.rolePk
    expect(roleId, `role ${FILE_ONLY.role}`).toBeDefined()

    const registry = await this.ok<{
      content: Array<{
        moduleRegPk: number
        code: string
        screens: Array<{ screenRegPk: number; pageCode: string; actions: Array<{ actionRegPk: number; actionCode: string }> }>
      }>
    }>('POST', '/api/v1/sec/registry/search', { filters: [], page: 0, size: 200 })
    const fileModule = registry.content.find((row) => row.code === 'FILE')
    const browser = fileModule?.screens.find((screen) => screen.pageCode === 'FILE_BROWSER')
    expect(browser, 'FILE_BROWSER in the registry').toBeDefined()
    const actions = [...(browser?.actions ?? [])].sort(
      (a, b) => Number(b.actionCode === 'VIEW') - Number(a.actionCode === 'VIEW')
    )
    const grants: Array<[string, string, number]> = [
      ['modules', 'moduleId', fileModule?.moduleRegPk ?? 0],
      ['screens', 'screenId', browser?.screenRegPk ?? 0],
      ...actions.map((action): [string, string, number] => ['actions', 'actionId', action.actionRegPk]),
    ]
    for (const [segment, field, id] of grants) {
      const granted = await this.call('POST', `/api/v1/sec/roles/${roleId}/${segment}`, { [field]: id })
      expect(
        granted.status < 300 || granted.body?.error?.code === 'SEC-409-GRANT-DUP',
        `grant ${segment} ${id} → ${granted.status} ${granted.body?.error?.code}`
      ).toBe(true)
    }

    const userCreated = await this.call('POST', '/api/v1/sec/users', {
      username: FILE_ONLY.username,
      email: `${FILE_ONLY.username}@example.test`,
      fullNameAr: 'مستخدم الملفات',
      fullNameEn: 'E2E File Only',
      password,
      roleIds: [roleId],
      requireChangeAtNextLogin: false,
    })
    expect([201, 409], `user ${FILE_ONLY.username}`).toContain(userCreated.status)
    const users = await this.ok<{ content: Array<{ userPk: number; username: string }> }>('POST', '/api/v1/sec/users/search', {
      filters: [{ field: 'username', operator: 'LIKE', value: FILE_ONLY.username }],
      page: 0,
      size: 5,
    })
    const user = users.content.find((row) => row.username === FILE_ONLY.username)
    expect(user, `user ${FILE_ONLY.username}`).toBeDefined()
    await this.ok('PUT', `/api/v1/sec/users/${user?.userPk}/roles`, { roleIds: [roleId] })
  }

  async signOut(): Promise<void> {
    await this.request.post(`${backendUrl}/api/v1/sec/auth/logout`, { headers: bearer(this.token) })
  }
}
