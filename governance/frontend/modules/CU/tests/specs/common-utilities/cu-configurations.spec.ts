/** CU configurations on erp-core 1.2.0 (steps 06, 12): PLATFORM_SETTINGS grant, one key in both scopes, toasts, confirm, and the E2E tenant's view. */
import { expect, test } from '@playwright/test'
import { e2eTenant, hasPassword, MISSING_ENV_REASON, STATE, USERS } from '../../pom/env.ts'
import { signInViaUi, useLanguage } from '../../pom/session.ts'
import { createEntry, filterByKey } from '../../pom/ConfigurationsPage.ts'
const RUN = Date.now()
const KEY = `E2E_KEY_${RUN}`
const TENANT_ONLY_KEY = `E2E_T2_KEY_${RUN}`

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test.describe('PLATFORM tenant, admin', () => {
  test.use({ storageState: STATE.admin })

  test('TC-FE-CU-001 grant: SYS_ADMIN receives PLATFORM_SETTINGS (VIEW + MANAGE) in the Roles grant tree; the menu then lists it', async ({ page }) => {
    await page.goto('/security/roles')
    await page.getByRole('button', { name: /SYS_ADMIN/ }).click()
    await expect(page).toHaveURL(/editId=\d+/)
    await page.getByRole('button', { name: 'Expand all' }).click()

    const screenNode = page.getByTestId('role-grant-screen-PLATFORM_SETTINGS')
    await expect(screenNode).toBeVisible()
    const tickAll = screenNode.getByRole('button', { name: 'Tick all' })
    if (await tickAll.isVisible()) {
      await tickAll.click()
      const saved = page.waitForResponse(
        (r) => r.request().method() === 'POST' && /\/api\/v1\/sec\/roles\/\d+\/actions$/.test(r.url())
      )
      await page.getByRole('button', { name: 'Save permissions' }).click()
      expect((await saved).ok()).toBe(true)
      await expect(page.getByText(/Saved \d+ permissions\./)).toBeVisible()
    }

    await page.reload()
    /* ⚠ Present, not visible: the PLATFORM group may render collapsed in the sidebar. */
    await expect(page.locator('a[href="/platform/settings"]')).toHaveCount(1)
    await expect(page.locator('a[href="/settings/configurations"]')).toHaveCount(1)
    await page.goto('/platform/settings')
    await expect(page.getByRole('heading', { name: 'Platform default settings' })).toBeVisible()
  })

  test('TC-FE-CU-002 one key in both scopes (?scope=): TENANT in /settings/configurations, PLATFORM in /platform/settings, both 201 and both listed', async ({ page }) => {
    await page.goto('/settings/configurations')
    await createEntry(page, KEY, '1', 'TENANT')
    await expect(page.getByTestId('toast-container')).toContainText('The tenant configuration has been created.')
    await expect(page).not.toHaveURL(/action=create/)
    await filterByKey(page, KEY)
    await expect(page.getByTestId(`cu-config-row-${KEY}`).getByTestId('cu-config-scope-badge')).toHaveAttribute('data-scope', 'TENANT')

    await page.goto('/platform/settings')
    await expect(page.getByText(/Visible only in the PLATFORM tenant/)).toBeVisible()
    await createEntry(page, KEY, '1', 'PLATFORM')
    await expect(page.getByTestId('toast-container')).toContainText('The platform default has been created.')
    await filterByKey(page, KEY)
    await expect(page.getByTestId(`cu-config-row-${KEY}`).getByTestId('cu-config-scope-badge')).toHaveAttribute('data-scope', 'PLATFORM')

    await page.goto('/settings/configurations')
    await filterByKey(page, KEY)
    await expect(page.getByTestId(`cu-config-row-${KEY}`).getByTestId('cu-config-scope-badge')).toHaveAttribute('data-scope', 'TENANT')
  })

  test('TC-FE-CU-003 edit shows the update toast; deactivate asks a ConfirmDialog first and shows the deactivate toast', async ({ page }) => {
    await page.goto(`/settings/configurations?editKey=${KEY}`)
    const value = page.getByTestId('cu-config-form-value')
    await expect(value).toHaveValue('1')
    await value.fill('2')
    const updated = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().includes(`/configurations/${KEY}?scope=TENANT`))
    await page.getByTestId('cu-config-form-submit').click()
    expect((await updated).status()).toBe(200)
    await expect(page.getByTestId('toast-container')).toContainText('The tenant configuration has been updated.')
    await expect(page).not.toHaveURL(/editKey=/)

    await filterByKey(page, KEY)
    const row = page.getByTestId(`cu-config-row-${KEY}`)
    await expect(row.getByRole('cell').nth(1)).toHaveText('2')
    await row.getByTestId(`cu-config-deactivate-${KEY}`).click()
    const dialog = page.getByRole('alertdialog')
    await expect(dialog).toContainText(`Are you sure you want to deactivate ${KEY}?`)
    const deactivated = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().includes(`/configurations/${KEY}?scope=TENANT`))
    await dialog.getByTestId('confirm-dialog-confirm').click()
    expect((await deactivated).status()).toBe(204)
    await expect(page.getByTestId('toast-container')).toContainText('The tenant configuration has been deactivated.')
    await expect(row).toContainText('Inactive')
    await expect(row.getByTestId(`cu-config-deactivate-${KEY}`)).toBeDisabled()
  })
})

test.describe('E2E tenant, e2eadmin', () => {
  test.beforeEach(async ({ page }) => {
    await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
  })

  test('TC-FE-CU-004 E2E tenant: SYS_ADMIN holds no PLATFORM grant, the menu has no /platform/settings, the deep link is refused; tenant overrides work', async ({ page }) => {
    await page.goto('/security/roles')
    await page.getByRole('button', { name: /SYS_ADMIN/ }).click()
    await page.getByRole('button', { name: 'Expand all' }).click()
    await expect(page.getByTestId('role-grant-module-CU').getByRole('checkbox').first()).toBeChecked()
    /* ⚠ The registry is global, so the node renders (deviation [04]); the tenant's role holds no grant on it (GAP-SEC-010). */
    await expect(page.getByTestId('role-grant-module-PLATFORM').getByRole('checkbox').first()).not.toBeChecked()

    await expect(page.locator('a[href="/settings/configurations"]')).toHaveCount(1)
    await expect(page.locator('a[href="/platform/settings"]')).toHaveCount(0)

    await page.goto('/platform/settings')
    await expect(page.getByText(/Access Restricted/i)).toBeVisible()
    await expect(page.getByText('Required screen: PLATFORM_SETTINGS')).toBeVisible()

    await page.goto('/settings/configurations')
    await createEntry(page, TENANT_ONLY_KEY, 'tenant-value', 'TENANT')
    await expect(page.getByTestId('toast-container')).toContainText('The tenant configuration has been created.')
    await filterByKey(page, TENANT_ONLY_KEY)
    await expect(page.getByTestId(`cu-config-row-${TENANT_ONLY_KEY}`).getByTestId('cu-config-scope-badge')).toHaveAttribute('data-scope', 'TENANT')
  })
})
