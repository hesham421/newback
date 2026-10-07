/** E2E global setup (README §4.10): E2E tenant, roles E2E_ALL / E2E_NONE, users e2e.all / e2e.none, stored auth states — idempotent. */
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { expect, test as setup, type APIRequestContext } from '@playwright/test'
import {
  backendUrl,
  CATALOG_MODULES,
  e2eTenant,
  frontendUrl,
  hasPassword,
  MISSING_ENV_REASON,
  password,
  PLATFORM,
  ROLES,
  STATE,
  STORAGE_KEYS,
  USERS,
} from '../pom/env.ts'
import { apiSignIn, bearer } from '../pom/session.ts'

interface Envelope<T> {
  success: boolean
  data: T
  error?: { code: string; message?: string }
}
interface Page<T> {
  content: T[]
}
interface RegistryAction {
  actionRegPk: number
  actionCode: string
}
interface RegistryScreen {
  screenRegPk: number
  pageCode: string
  actions: RegistryAction[]
}
interface RegistryModule {
  moduleRegPk: number
  code: string
  screens: RegistryScreen[]
}
interface GrantTree {
  modules: Array<{
    moduleRegPk: number
    granted: boolean
    screens: Array<{ screenRegPk: number; granted: boolean; actions: Array<{ actionRegPk: number }> }>
  }>
}
interface Wanted {
  modules: number[]
  screens: number[]
  actions: number[]
}

/* The same fixed body as scripts/e2e-provision.sh (README §4.10). */
const TENANT_BODY = {
  code: e2eTenant,
  nameAr: 'مستأجر الاختبار',
  nameEn: 'E2E Tenant',
  adminUsername: USERS.e2eAdmin,
  adminEmail: 'e2eadmin@example.test',
  adminPassword: password,
  adminFullNameAr: 'مدير الاختبار',
  adminFullNameEn: 'E2E Admin',
}

class Api {
  private readonly request: APIRequestContext
  private readonly token: string

  constructor(request: APIRequestContext, token: string) {
    this.request = request
    this.token = token
  }

  async call<T>(method: 'GET' | 'POST' | 'PUT' | 'PATCH', urlPath: string, data?: unknown) {
    const response = await this.request.fetch(`${backendUrl}${urlPath}`, {
      method,
      headers: bearer(this.token),
      data,
    })
    const body = (await response.json().catch(() => null)) as Envelope<T> | null
    return { status: response.status(), body }
  }

  async expectOk<T>(method: 'GET' | 'POST' | 'PUT' | 'PATCH', urlPath: string, data?: unknown): Promise<T> {
    const { status, body } = await this.call<T>(method, urlPath, data)
    expect(status, `${method} ${urlPath} → ${JSON.stringify(body?.error)}`).toBeLessThan(300)
    return (body as Envelope<T>).data
  }
}

/** 2xx = created; 409 with one of the tolerated codes = already there; anything else fails the setup. */
async function createTolerating(api: Api, urlPath: string, data: unknown, tolerated: readonly string[]): Promise<string> {
  const { status, body } = await api.call('POST', urlPath, data)
  if (status < 300) return 'created'
  const code = body?.error?.code ?? ''
  expect(tolerated, `POST ${urlPath} → ${status} ${code}`).toContain(code)
  return code
}

async function ensureTenantActive(api: Api): Promise<void> {
  await createTolerating(api, '/api/v1/platform/tenants', TENANT_BODY, ['TENANT_CODE_DUPLICATE'])
  const found = await api.expectOk<Page<{ id: number; code: string; statusCode: string }>>(
    'POST',
    '/api/v1/platform/tenants/search',
    { filters: [{ field: 'code', operator: 'LIKE', value: e2eTenant }], page: 0, size: 5 }
  )
  const tenant = found.content.find((row) => row.code === e2eTenant)
  expect(tenant, `tenant ${e2eTenant}`).toBeDefined()
  /* ⚠ A run that died between the AUTH suspend and its re-activation leaves the tenant SUSPENDED; heal it here. */
  if (tenant && tenant.statusCode !== 'ACTIVE') {
    await api.expectOk('PATCH', `/api/v1/platform/tenants/${tenant.id}/status`, { statusCode: 'ACTIVE' })
  }
}

async function ensureRole(api: Api, code: string, nameEn: string, nameAr: string): Promise<number> {
  await createTolerating(api, '/api/v1/sec/roles', { code, nameAr, nameEn }, ['SEC-409-ROLE-DUP'])
  const found = await api.expectOk<Page<{ rolePk: number; code: string }>>('POST', '/api/v1/sec/roles/search', {
    filters: [{ field: 'code', operator: 'LIKE', value: code }],
    page: 0,
    size: 5,
  })
  const role = found.content.find((row) => row.code === code)
  expect(role, `role ${code}`).toBeDefined()
  return (role as { rolePk: number }).rolePk
}

function heldGrants(tree: GrantTree): Wanted {
  const held: Wanted = { modules: [], screens: [], actions: [] }
  for (const module of tree.modules ?? []) {
    if (module.granted) held.modules.push(module.moduleRegPk)
    for (const screen of module.screens ?? []) {
      if (screen.granted) held.screens.push(screen.screenRegPk)
      for (const action of screen.actions ?? []) held.actions.push(action.actionRegPk)
    }
  }
  return held
}

/** Grants module → screen → actions (VIEW first: the server refuses an action before its screen's VIEW gateway). */
async function grantMissing(api: Api, roleId: number, wanted: Wanted): Promise<string[]> {
  const held = heldGrants(await api.expectOk<GrantTree>('GET', `/api/v1/sec/roles/${roleId}/grants`))
  const refused: string[] = []
  const steps: Array<[keyof Wanted, string, string]> = [
    ['modules', 'modules', 'moduleId'],
    ['screens', 'screens', 'screenId'],
    ['actions', 'actions', 'actionId'],
  ]
  for (const [kind, segment, field] of steps) {
    for (const id of wanted[kind].filter((item) => !held[kind].includes(item))) {
      const outcome = await createTolerating(api, `/api/v1/sec/roles/${roleId}/${segment}`, { [field]: id }, [
        'SEC-409-GRANT-DUP',
        'SEC-409-SOD-CONFLICT',
      ])
      if (outcome === 'SEC-409-SOD-CONFLICT') refused.push(`${kind}:${id}`)
    }
  }
  return refused
}

function catalogGrants(registry: RegistryModule[]): Wanted {
  const wanted: Wanted = { modules: [], screens: [], actions: [] }
  for (const module of registry.filter((row) => (CATALOG_MODULES as readonly string[]).includes(row.code))) {
    wanted.modules.push(module.moduleRegPk)
    for (const screen of module.screens) {
      wanted.screens.push(screen.screenRegPk)
      const ordered = [...screen.actions].sort((a, b) => Number(b.actionCode === 'VIEW') - Number(a.actionCode === 'VIEW'))
      wanted.actions.push(...ordered.map((action) => action.actionRegPk))
    }
  }
  return wanted
}

function dashboardOnly(registry: RegistryModule[]): Wanted {
  const sec = registry.find((row) => row.code === 'SEC')
  const dashboard = sec?.screens.find((screen) => screen.pageCode === 'SEC_DASHBOARD')
  expect(dashboard, 'SEC_DASHBOARD in the registry').toBeDefined()
  return { modules: [(sec as RegistryModule).moduleRegPk], screens: [(dashboard as RegistryScreen).screenRegPk], actions: [] }
}

async function ensureUser(api: Api, username: string, fullNameEn: string, roleId: number): Promise<void> {
  await createTolerating(
    api,
    '/api/v1/sec/users',
    {
      username,
      email: `${username}@example.test`,
      fullNameAr: 'مستخدم الاختبار',
      fullNameEn,
      password,
      roleIds: [roleId],
    },
    ['SEC-409-USER-DUP']
  )
  const found = await api.expectOk<Page<{ userPk: number; username: string }>>('POST', '/api/v1/sec/users/search', {
    filters: [{ field: 'username', operator: 'LIKE', value: username }],
    page: 0,
    size: 5,
  })
  const user = found.content.find((row) => row.username === username)
  expect(user, `user ${username}`).toBeDefined()
  await api.expectOk('PUT', `/api/v1/sec/users/${(user as { userPk: number }).userPk}/roles`, { roleIds: [roleId] })
}

/** Ends the session a previous run stored, so live sessions do not pile up across runs (best effort). */
async function endStoredSession(request: APIRequestContext, file: string): Promise<void> {
  try {
    const stored = JSON.parse(await readFile(file, 'utf8')) as { origins?: Array<{ localStorage?: Array<{ name: string; value: string }> }> }
    const token = stored.origins?.[0]?.localStorage?.find((entry) => entry.name === STORAGE_KEYS.token)?.value
    if (token) await request.post(`${backendUrl}/api/v1/sec/auth/logout`, { headers: bearer(token) })
  } catch {
    /* No previous state, or it is unreadable: nothing to end. */
  }
}

async function writeState(request: APIRequestContext, file: string, username: string): Promise<void> {
  await endStoredSession(request, file)
  const token = await apiSignIn(request, PLATFORM, username)
  const state = {
    cookies: [],
    origins: [
      {
        origin: new URL(frontendUrl).origin,
        localStorage: [
          { name: STORAGE_KEYS.token, value: token },
          { name: STORAGE_KEYS.tenant, value: PLATFORM },
          { name: STORAGE_KEYS.lang, value: 'en' },
        ],
      },
    ],
  }
  await mkdir(path.dirname(file), { recursive: true })
  await writeFile(file, JSON.stringify(state, null, 2), 'utf8')
}

setup('provision tenant, roles, users and auth states', async ({ request }) => {
  setup.skip(!hasPassword, MISSING_ENV_REASON)
  setup.setTimeout(120_000)
  const api = new Api(request, await apiSignIn(request, PLATFORM, USERS.admin))

  await ensureTenantActive(api)

  const registry = (
    await api.expectOk<Page<RegistryModule>>('POST', '/api/v1/sec/registry/search', { filters: [], page: 0, size: 200 })
  ).content

  const allRoleId = await ensureRole(api, ROLES.all, 'E2E every catalog grant', 'اختبار كل الصلاحيات')
  const refused = await grantMissing(api, allRoleId, catalogGrants(registry))
  const noneRoleId = await ensureRole(api, ROLES.none, 'E2E dashboard only', 'اختبار لوحة المعلومات فقط')
  await grantMissing(api, noneRoleId, dashboardOnly(registry))

  await ensureUser(api, USERS.all, 'E2E All Grants', allRoleId)
  await ensureUser(api, USERS.none, 'E2E No Grants', noneRoleId)

  await writeState(request, STATE.admin, USERS.admin)
  await writeState(request, STATE.all, USERS.all)
  await writeState(request, STATE.none, USERS.none)
  setup.info().annotations.push({ type: 'E2E_ALL refused grants (SoD)', description: refused.join(', ') || 'none' })
})
