/** POM for the SEC_ROLES grant tree (TM-F4): the API harness that shapes a role, and the tree / revoke-dialog locators. */
import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test'
import { backendUrl, PLATFORM, USERS } from './env.ts'
import { apiSignIn, bearer } from './session.ts'

interface Envelope<T> {
  data: T
}
interface RegistryModule {
  moduleRegPk: number
  code: string
  screens: Array<{ screenRegPk: number; pageCode: string; actions: Array<{ actionRegPk: number; actionCode: string }> }>
}

export interface SecUsersIds {
  moduleId: number
  screenId: number
  actions: Record<'VIEW' | 'CREATE' | 'UPDATE', number>
}

/** Creates roles and grants through the API as the PLATFORM admin. */
export class RoleGrantHarness {
  private readonly request: APIRequestContext
  private readonly token: string
  readonly ids: SecUsersIds

  private constructor(request: APIRequestContext, token: string, ids: SecUsersIds) {
    this.request = request
    this.token = token
    this.ids = ids
  }

  static async open(request: APIRequestContext): Promise<RoleGrantHarness> {
    const token = await apiSignIn(request, PLATFORM, USERS.admin)
    const response = await request.post(`${backendUrl}/api/v1/sec/registry/search`, {
      headers: bearer(token),
      data: { filters: [], page: 0, size: 200 },
    })
    expect(response.status()).toBe(200)
    const registry = ((await response.json()) as Envelope<{ content: RegistryModule[] }>).data.content
    const sec = registry.find((row) => row.code === 'SEC')
    const users = sec?.screens.find((row) => row.pageCode === 'SEC_USERS')
    expect(users, 'SEC_USERS in the registry').toBeDefined()
    const actionId = (code: string) => {
      const found = users?.actions.find((row) => row.actionCode === code)
      expect(found, `SEC_USERS ${code}`).toBeDefined()
      return (found as { actionRegPk: number }).actionRegPk
    }
    const ids: SecUsersIds = {
      moduleId: (sec as RegistryModule).moduleRegPk,
      screenId: (users as { screenRegPk: number }).screenRegPk,
      actions: { VIEW: actionId('VIEW'), CREATE: actionId('CREATE'), UPDATE: actionId('UPDATE') },
    }
    return new RoleGrantHarness(request, token, ids)
  }

  async createRole(code: string, nameEn: string): Promise<number> {
    const response = await this.request.post(`${backendUrl}/api/v1/sec/roles`, {
      headers: bearer(this.token),
      data: { code, nameEn, nameAr: `دور ${code}` },
    })
    expect(response.status(), `create role ${code}`).toBe(201)
    return ((await response.json()) as Envelope<{ rolePk: number }>).data.rolePk
  }

  /** Grants module SEC, screen SEC_USERS and the given actions (VIEW first), each only when not held yet. */
  async grantSecUsers(roleId: number, actions: ReadonlyArray<'VIEW' | 'CREATE' | 'UPDATE'>): Promise<void> {
    const held = await this.grants(roleId)
    const post = async (segment: string, body: Record<string, number>) => {
      const response = await this.request.post(`${backendUrl}/api/v1/sec/roles/${roleId}/${segment}`, {
        headers: bearer(this.token),
        data: body,
      })
      expect(response.status(), `grant ${segment} ${JSON.stringify(body)}`).toBe(201)
    }
    if (!held.modules.includes(this.ids.moduleId)) await post('modules', { moduleId: this.ids.moduleId })
    if (!held.screens.includes(this.ids.screenId)) await post('screens', { screenId: this.ids.screenId })
    for (const code of actions) {
      if (!held.actions.includes(this.ids.actions[code])) await post('actions', { actionId: this.ids.actions[code] })
    }
  }

  async grants(roleId: number): Promise<{ modules: number[]; screens: number[]; actions: number[] }> {
    const response = await this.request.get(`${backendUrl}/api/v1/sec/roles/${roleId}/grants`, { headers: bearer(this.token) })
    expect(response.status()).toBe(200)
    const tree = ((await response.json()) as Envelope<{
      modules: Array<{ moduleRegPk: number; granted: boolean; screens: Array<{ screenRegPk: number; granted: boolean; actions: Array<{ actionRegPk: number }> }> }>
    }>).data
    const held = { modules: [] as number[], screens: [] as number[], actions: [] as number[] }
    for (const module of tree.modules) {
      if (module.granted) held.modules.push(module.moduleRegPk)
      for (const screen of module.screens) {
        if (screen.granted) held.screens.push(screen.screenRegPk)
        held.actions.push(...screen.actions.map((action) => action.actionRegPk))
      }
    }
    return held
  }
}

/** The grant tree of one role, opened by deep link. */
export class RoleGrantTreePage {
  private readonly page: Page
  readonly dialog: Locator

  constructor(page: Page) {
    this.page = page
    this.dialog = page.getByRole('alertdialog')
  }

  async open(roleId: number): Promise<void> {
    const grants = this.page.waitForResponse((r) => r.url().endsWith(`/api/v1/sec/roles/${roleId}/grants`))
    await this.page.goto(`/security/roles?editId=${roleId}`)
    expect((await grants).status()).toBe(200)
    await this.page.getByRole('button', { name: 'Expand all' }).click()
    await expect(this.screenNode('SEC_USERS')).toBeVisible()
  }

  moduleBox(code: string): Locator {
    return this.page.getByTestId(`role-grant-module-${code}`).getByRole('checkbox').first()
  }

  screenNode(pageCode: string): Locator {
    return this.page.getByTestId(`role-grant-screen-${pageCode}`)
  }

  screenBox(pageCode: string): Locator {
    return this.screenNode(pageCode).getByRole('checkbox').first()
  }

  chip(pageCode: string, actionCode: string): Locator {
    return this.screenNode(pageCode).getByRole('checkbox', { name: actionCode, exact: true })
  }

  async confirm(): Promise<void> {
    await this.dialog.getByTestId('confirm-dialog-confirm').click()
  }
}
