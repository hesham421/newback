/** TM-F4 on erp-core 1.3.0: uncheck a saved screen / action in the SEC_ROLES grant tree (ConfirmDialog, cascade count, toast), super-role hint, module revoke unchanged. */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'
import { hasPassword, MISSING_ENV_REASON, ROLES, STATE } from '../../pom/env.ts'
import { RoleGrantHarness, RoleGrantTreePage } from '../../pom/RoleGrantTreePage.ts'
import { doublePress, zeroGapPress } from '../../pom/doublePress.ts'
import { ShellPage } from '../../pom/ShellPage.ts'
import { useLanguage } from '../../pom/session.ts'

const STAMP = Date.now()
const ROLE_CODE = `E2E_RV_${STAMP}`
const ROLE_NAME = `E2E revoke ${STAMP}`
const OBSOLETE_TEXT = 'cannot be withdrawn one by one'

let roleId: number | null = null

async function revokeRole(request: APIRequestContext): Promise<{ harness: RoleGrantHarness; roleId: number }> {
  const harness = await RoleGrantHarness.open(request)
  if (roleId === null) roleId = await harness.createRole(ROLE_CODE, ROLE_NAME)
  return { harness, roleId }
}

function deletesOf(page: Page): string[] {
  const sent: string[] = []
  page.on('request', (r) => {
    if (r.method() === 'DELETE' && r.url().includes('/api/v1/sec/roles/')) sent.push(r.url())
  })
  return sent
}

test.describe.configure({ mode: 'serial' })
test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test('TC-FE-SEC-026 Roles: unchecking a saved screen asks first, names the cascade (3) from the tree, then revokes it with its actions', async ({ page, request }) => {
  const { harness, roleId: id } = await revokeRole(request)
  await harness.grantSecUsers(id, ['VIEW', 'CREATE', 'UPDATE'])
  const tree = new RoleGrantTreePage(page)
  const sent = deletesOf(page)
  await tree.open(id)
  await expect(tree.screenBox('SEC_USERS')).toBeChecked()

  await tree.screenBox('SEC_USERS').click()
  const confirm = tree.dialog.getByTestId('role-revoke-screen-confirm')
  await expect(confirm).toBeVisible()
  await expect(tree.dialog).toContainText(`The role ${ROLE_NAME} loses the screen Users.`)
  await expect(tree.dialog.getByTestId('role-revoke-cascade-count')).toHaveAttribute('data-count', '3')
  await expect(tree.dialog.getByTestId('role-revoke-cascade-count')).toHaveText('3 action grant(s) of this role on the screen are revoked with it.')
  expect(sent).toHaveLength(0)

  const revoked = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/roles/${id}/screens/${harness.ids.screenId}`))
  await tree.confirm()
  const answer = await revoked
  expect(answer.status()).toBe(200)
  expect(((await answer.json()) as { data: { revokedActionGrants: number } }).data.revokedActionGrants).toBe(3)
  await expect(new ShellPage(page).toasts).toContainText('Screen grant revoked; 3 action grant(s) revoked with it.')
  await expect(tree.dialog).toHaveCount(0)

  await expect(tree.screenBox('SEC_USERS')).not.toBeChecked()
  for (const code of ['VIEW', 'CREATE', 'UPDATE']) await expect(tree.chip('SEC_USERS', code)).toHaveAttribute('aria-checked', 'false')
  await expect(tree.moduleBox('SEC')).toBeChecked()
  const held = await harness.grants(id)
  expect(held.screens).not.toContain(harness.ids.screenId)
  expect(held.modules).toContain(harness.ids.moduleId)
})

test('TC-FE-SEC-027 Roles: unchecking a saved non-VIEW action asks first (no VIEW warning) and revokes that action only', async ({ page, request }) => {
  const { harness, roleId: id } = await revokeRole(request)
  await harness.grantSecUsers(id, ['VIEW', 'CREATE', 'UPDATE'])
  const tree = new RoleGrantTreePage(page)
  const sent = deletesOf(page)
  await tree.open(id)

  await tree.chip('SEC_USERS', 'CREATE').click()
  await expect(tree.dialog.getByTestId('role-revoke-action-confirm')).toBeVisible()
  await expect(tree.dialog).toContainText(`The role ${ROLE_NAME} loses CREATE on Users.`)
  await expect(tree.dialog.getByTestId('role-revoke-view-warning')).toHaveCount(0)
  expect(sent).toHaveLength(0)

  const revoked = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/roles/${id}/actions/${harness.ids.actions.CREATE}`))
  await tree.confirm()
  const answer = await revoked
  expect(answer.status()).toBe(200)
  expect(((await answer.json()) as { data: { revokedActionGrants: number } }).data.revokedActionGrants).toBe(1)
  await expect(new ShellPage(page).toasts).toContainText('1 action grant(s) revoked.')

  await expect(tree.chip('SEC_USERS', 'CREATE')).toHaveAttribute('aria-checked', 'false')
  await expect(tree.chip('SEC_USERS', 'VIEW')).toHaveAttribute('aria-checked', 'true')
  await expect(tree.chip('SEC_USERS', 'UPDATE')).toHaveAttribute('aria-checked', 'true')
  await expect(tree.screenBox('SEC_USERS')).toBeChecked()
})

test('TC-FE-SEC-028 Roles: unchecking VIEW warns of the cascade and the menu entry, revokes VIEW + UPDATE, and the screen stays granted', async ({ page, request }) => {
  const { harness, roleId: id } = await revokeRole(request)
  await harness.grantSecUsers(id, ['VIEW', 'UPDATE'])
  expect((await harness.grants(id)).actions).not.toContain(harness.ids.actions.CREATE)
  const tree = new RoleGrantTreePage(page)
  await tree.open(id)

  await tree.chip('SEC_USERS', 'VIEW').click()
  await expect(tree.dialog.getByTestId('role-revoke-action-confirm')).toBeVisible()
  const warning = tree.dialog.getByTestId('role-revoke-view-warning')
  await expect(warning).toHaveAttribute('data-count', '1')
  await expect(warning).toContainText('other actions on this screen (1)')
  await expect(warning).toContainText('The screen stays in the role’s menu')

  const revoked = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/roles/${id}/actions/${harness.ids.actions.VIEW}`))
  await tree.confirm()
  const answer = await revoked
  expect(answer.status()).toBe(200)
  expect(((await answer.json()) as { data: { revokedActionGrants: number } }).data.revokedActionGrants).toBe(2)
  await expect(new ShellPage(page).toasts).toContainText('2 action grant(s) revoked.')

  await expect(tree.chip('SEC_USERS', 'VIEW')).toHaveAttribute('aria-checked', 'false')
  await expect(tree.chip('SEC_USERS', 'UPDATE')).toHaveAttribute('aria-checked', 'false')
  await expect(tree.screenBox('SEC_USERS')).toBeChecked()
  const held = await harness.grants(id)
  expect(held.screens).toContain(harness.ids.screenId)
  expect(held.actions).toHaveLength(0)
})

test('TC-FE-SEC-029 Roles: a super role shows the "grants only shape its menu" hint; a normal role does not', async ({ page }) => {
  await page.goto('/security/roles')
  await page.getByRole('button', { name: /SYS_ADMIN/ }).click()
  await expect(page).toHaveURL(/editId=\d+/)
  await expect(page.getByTestId('role-super-hint')).toHaveText('This role holds every authority regardless of grants; grants only shape its menu.')
  await page.getByRole('button', { name: new RegExp(ROLES.all) }).click()
  await expect(page.getByRole('heading', { name: 'E2E every catalog grant' })).toBeVisible()
  await expect(page.getByTestId('role-super-hint')).toHaveCount(0)
})

test('TC-FE-SEC-030 Roles: the module revoke is unchanged (1 screen + 1 action) and the "one by one" text is gone', async ({ page, request }) => {
  const { harness, roleId: id } = await revokeRole(request)
  await harness.grantSecUsers(id, ['VIEW'])
  const tree = new RoleGrantTreePage(page)
  await tree.open(id)
  await expect(page.getByText(OBSOLETE_TEXT)).toHaveCount(0)

  await page.getByTestId('role-grant-module-SEC').getByRole('button', { name: 'Revoke module grant' }).click()
  const drawer = page.getByRole('dialog').filter({ hasText: 'Revoke module grant?' })
  await expect(drawer).toBeVisible()
  await expect(page.getByText(OBSOLETE_TEXT)).toHaveCount(0)
  const revoked = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/roles/${id}/modules/${harness.ids.moduleId}`))
  await drawer.getByRole('button', { name: 'Revoke module grant' }).click()
  const answer = await revoked
  expect(answer.status()).toBe(200)
  const outcome = ((await answer.json()) as { data: { revokedScreenGrants: number; revokedActionGrants: number } }).data
  expect(outcome).toEqual({ revokedScreenGrants: 1, revokedActionGrants: 1 })
  await expect(page.getByText('Module grant revoked. 1 screen grants and 1 action grants were also removed.')).toBeVisible()
  await expect(page.getByText(OBSOLETE_TEXT)).toHaveCount(0)
})

test('TC-FE-SEC-031 Roles: a double press on the revoke Confirm sends one DELETE and toasts once (SEC-U122)', async ({ page, request }) => {
  const { harness, roleId: id } = await revokeRole(request)
  await harness.grantSecUsers(id, ['VIEW', 'CREATE', 'UPDATE'])
  const tree = new RoleGrantTreePage(page)
  await tree.open(id)

  await tree.chip('SEC_USERS', 'UPDATE').click()
  await expect(tree.dialog.getByTestId('role-revoke-action-confirm')).toBeVisible()
  const sent = deletesOf(page)
  const revoked = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/roles/${id}/actions/${harness.ids.actions.UPDATE}`))
  await doublePress(tree.dialog.getByTestId('confirm-dialog-confirm'))
  expect((await revoked).status()).toBe(200)
  await expect(tree.dialog).toHaveCount(0)
  await expect(new ShellPage(page).toasts.getByText('1 action grant(s) revoked.')).toHaveCount(1)
  await expect(tree.chip('SEC_USERS', 'UPDATE')).toHaveAttribute('aria-checked', 'false')
  expect(sent).toHaveLength(1)

  await tree.chip('SEC_USERS', 'CREATE').click()
  await expect(tree.dialog.getByTestId('role-revoke-action-confirm')).toBeVisible()
  const second = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/roles/${id}/actions/${harness.ids.actions.CREATE}`))
  await zeroGapPress(tree.dialog.getByTestId('confirm-dialog-confirm'))
  expect((await second).status()).toBe(200)
  await expect(tree.dialog).toHaveCount(0)
  await expect(tree.chip('SEC_USERS', 'CREATE')).toHaveAttribute('aria-checked', 'false')
  expect(sent).toHaveLength(2)
  expect((await harness.grants(id)).actions).toEqual([harness.ids.actions.VIEW])
})
