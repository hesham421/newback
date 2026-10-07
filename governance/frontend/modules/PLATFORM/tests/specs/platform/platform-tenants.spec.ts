/** PLATFORM_TENANTS on erp-core 1.2.0 (steps 09, 12): create E2E_T9 + t9admin, inline duplicate/invalid code, suspend (confirmed) → login refused, activate → login works without the screen, PLATFORM never suspendable. */
import { expect, test } from '@playwright/test'
import { backendUrl, hasPassword, MISSING_ENV_REASON, password, PLATFORM, USERS } from '../../pom/env.ts'
import { LoginPage } from '../../pom/LoginPage.ts'
import { bearer, signInViaUi, signOutViaUi, storedToken, useLanguage } from '../../pom/session.ts'
import { fillCreateForm, filterByCode, openTenant } from '../../pom/TenantsPage.ts'
const TENANT = 'E2E_T9'
const ADMIN = 't9admin'
const TENANTS_API = /\/api\/v1\/platform\/tenants$/
const STATUS_API = /\/api\/v1\/platform\/tenants\/\d+\/status$/

test.describe.configure({ mode: 'serial' })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test.describe('PLATFORM_TENANTS', () => {
  test('TC-FE-PLATFORM-001 create E2E_T9 (no tenant header) with its first administrator t9admin → 201 and the row is ACTIVE (idempotent across runs)', async ({ page }) => {
    await signInViaUi(page, PLATFORM, USERS.admin)
    await page.goto('/platform/tenants')
    await expect(page.getByRole('heading', { name: 'Tenants' })).toBeVisible()
    await filterByCode(page, TENANT)
    const row = page.getByTestId(`tenant-row-${TENANT}`)
    const emptyState = page.getByText('No tenant matches the filters')
    await expect(row.or(emptyState)).toBeVisible()

    if (await emptyState.isVisible()) {
      await page.getByTestId('tenant-create').click()
      await expect(page).toHaveURL(/action=create/)
      await fillCreateForm(page, TENANT.toLowerCase(), ADMIN)
      const created = page.waitForResponse((r) => r.request().method() === 'POST' && TENANTS_API.test(r.url()))
      await page.getByTestId('tenant-form-submit').click()
      const response = await created
      expect(response.status()).toBe(201)
      const sent = response.request()
      expect(sent.headers()['x-tenant-code']).toBeUndefined()
      expect(sent.headers()['authorization']).toMatch(/^Bearer /)
      expect(sent.postDataJSON()).toMatchObject({ code: TENANT, adminUsername: ADMIN })
      expect(((await response.json()) as { data: { statusCode: string } }).data.statusCode).toBe('ACTIVE')
      await expect(page.getByTestId('toast-container')).toContainText(
        `Tenant ${TENANT} has been created with its administrator ${ADMIN}.`
      )
      await expect(page).not.toHaveURL(/action=create/)
    } else {
      test.info().annotations.push({ type: 'idempotent', description: `${TENANT} already provisioned by an earlier run` })
      const badge = row.getByTestId('tenant-status-badge')
      if ((await badge.getAttribute('data-status')) === 'SUSPENDED') {
        await page.getByTestId(`tenant-open-${TENANT}`).click()
        await page.getByTestId('tenant-action-activate').click()
        await expect(page.getByTestId('tenant-detail-drawer').getByTestId('tenant-status-badge')).toHaveAttribute('data-status', 'ACTIVE')
        await page.goto('/platform/tenants')
        await filterByCode(page, TENANT)
      }
    }

    await expect(row.getByTestId('tenant-status-badge')).toHaveAttribute('data-status', 'ACTIVE')
  })

  test('TC-FE-PLATFORM-002 e2e_t9 again (upper-cased, bearer only) → inline duplicate on the code; "ab" → inline invalid code with no request', async ({ page }) => {
    await signInViaUi(page, PLATFORM, USERS.admin)
    await page.goto('/platform/tenants?action=create')
    await fillCreateForm(page, TENANT.toLowerCase(), ADMIN)
    const refused = page.waitForResponse((r) => r.request().method() === 'POST' && TENANTS_API.test(r.url()))
    await page.getByTestId('tenant-form-submit').click()
    const response = await refused
    expect(response.status()).toBe(409)
    const sent = response.request()
    expect(sent.headers()['x-tenant-code']).toBeUndefined()
    expect(sent.headers()['authorization']).toMatch(/^Bearer /)
    expect(sent.postDataJSON()).toMatchObject({ code: TENANT, adminUsername: ADMIN })
    expect(((await response.json()) as { error: { code: string } }).error.code).toBe('TENANT_CODE_DUPLICATE')
    const code = page.getByTestId('tenant-form-code')
    await expect(code).toHaveAttribute('aria-invalid', 'true')
    await expect(page.getByText('A tenant with this code already exists — choose another code.')).toBeVisible()

    let posted = false
    page.on('request', (r) => {
      if (r.method() === 'POST' && TENANTS_API.test(r.url())) posted = true
    })
    await code.fill('ab')
    await code.blur()
    await expect(code).toHaveValue('AB')
    await expect(page.getByText('Use 3 to 32 upper-case letters, digits or underscores.')).toBeVisible()
    await page.getByTestId('tenant-form-submit').click()
    await expect(page.getByText('Use 3 to 32 upper-case letters, digits or underscores.')).toBeVisible()
    expect(posted).toBe(false)
  })

  test('TC-FE-PLATFORM-003 Suspend is never offered for PLATFORM; its detail links to the platform defaults', async ({ page }) => {
    await signInViaUi(page, PLATFORM, USERS.admin)
    await openTenant(page, 'PLATFORM')
    await expect(page.getByTestId('tenant-platform-protected')).toBeVisible()
    await expect(page.getByTestId('tenant-action-suspend')).toHaveCount(0)
    await expect(page.getByTestId('tenant-link-platform-settings')).toHaveAttribute('href', '/platform/settings')
  })

  test('TC-FE-PLATFORM-004 Suspend E2E_T9 behind a ConfirmDialog → SUSPENDED; t9admin is then refused inline with TENANT_SUSPENDED', async ({ page }) => {
    await signInViaUi(page, PLATFORM, USERS.admin)
    await openTenant(page, TENANT)
    await page.getByTestId('tenant-action-suspend').click()
    const dialog = page.getByRole('alertdialog')
    await expect(dialog).toContainText(`Are you sure you want to suspend the tenant ${TENANT}?`)
    const patched = page.waitForResponse((r) => r.request().method() === 'PATCH' && STATUS_API.test(r.url()))
    await dialog.getByTestId('confirm-dialog-confirm').click()
    const response = await patched
    expect(response.status()).toBe(200)
    expect(response.request().postDataJSON()).toEqual({ statusCode: 'SUSPENDED' })
    await expect(page.getByTestId('toast-container')).toContainText(`Tenant ${TENANT} is now Suspended.`)
    await expect(page.getByTestId('tenant-detail-drawer').getByTestId('tenant-status-badge')).toHaveAttribute('data-status', 'SUSPENDED')
    await signOutViaUi(page)

    const login = new LoginPage(page)
    const refused = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/login'))
    await login.signIn(TENANT, ADMIN, password)
    const answer = await refused
    expect(answer.status()).toBe(403)
    expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('TENANT_SUSPENDED')
    await expect(login.tenant).toHaveAttribute('aria-invalid', 'true')
    await expect(await login.tenantMessage()).toHaveText('This tenant is suspended. Contact your administrator.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('TC-FE-PLATFORM-005 Activate E2E_T9 (no dialog) → ACTIVE; t9admin signs in, its menu has no /platform/tenants and the API refuses it (403)', async ({ page, request }) => {
    await signInViaUi(page, PLATFORM, USERS.admin)
    await openTenant(page, TENANT)
    const patched = page.waitForResponse((r) => r.request().method() === 'PATCH' && STATUS_API.test(r.url()))
    await page.getByTestId('tenant-action-activate').click()
    expect((await patched).status()).toBe(200)
    await expect(page.getByRole('alertdialog')).toHaveCount(0)
    await expect(page.getByTestId('toast-container')).toContainText(`Tenant ${TENANT} is now Active.`)
    await expect(page.getByTestId('tenant-detail-drawer').getByTestId('tenant-status-badge')).toHaveAttribute('data-status', 'ACTIVE')
    await signOutViaUi(page)

    await signInViaUi(page, TENANT, ADMIN)
    await expect(page.getByTestId('topbar-tenant')).toHaveText(TENANT)
    /* ⚠ Present, not visible: wait for the menu to load (a SEC link exists) before asserting an absence. */
    await expect(page.locator('a[href="/security/users"]')).toHaveCount(1)
    await expect(page.locator('a[href="/platform/tenants"]')).toHaveCount(0)
    await page.goto('/platform/tenants')
    await expect(page.getByText('Required screen: PLATFORM_TENANTS')).toBeVisible()
    const refused = await request.get(`${backendUrl}/api/v1/platform/tenants?page=0&size=1`, {
      headers: bearer((await storedToken(page)) ?? ''),
    })
    expect(refused.status()).toBe(403)
    expect(['SEC-403-FORBIDDEN', 'ACCESS_DENIED']).toContain(((await refused.json()) as { error: { code: string } }).error.code)
  })

  test('TC-FE-PLATFORM-006 the two sections of the create drawer are Disclosures: both open, each collapses (click, keyboard), and a submit with an invalid closed section re-opens only that one, with no request', async ({ page }) => {
    await signInViaUi(page, PLATFORM, USERS.admin)
    await page.goto('/platform/tenants?action=create')
    const tenantSection = page.getByTestId('tenant-form-section-tenant')
    const adminSection = page.getByTestId('tenant-form-section-admin')
    const tenantToggle = page.getByTestId('tenant-form-section-tenant-toggle')
    const adminToggle = page.getByTestId('tenant-form-section-admin-toggle')
    await expect(tenantSection).toHaveAttribute('data-open', 'true')
    await expect(adminSection).toHaveAttribute('data-open', 'true')
    await expect(adminToggle).toHaveAccessibleName(/First administrator/)

    await page.getByTestId('tenant-form-code').fill(`e2e_disc_${Date.now() % 100_000}`)
    await page.getByTestId('tenant-form-name-ar').fill('مستأجر الأقسام')
    await page.getByTestId('tenant-form-name-en').fill('E2E Disclosure tenant')
    await adminToggle.click()
    await expect(adminToggle).toHaveAttribute('aria-expanded', 'false')
    await expect(page.getByTestId('tenant-form-admin-username')).toBeHidden()
    await tenantToggle.press('Enter')
    await expect(tenantToggle).toHaveAttribute('aria-expanded', 'false')
    await expect(page.getByTestId('tenant-form-code')).toBeHidden()

    let posted = false
    page.on('request', (r) => {
      if (r.method() === 'POST' && TENANTS_API.test(r.url())) posted = true
    })
    await page.getByTestId('tenant-form-submit').click()
    await expect(adminSection).toHaveAttribute('data-open', 'true')
    await expect(page.getByTestId('tenant-form-admin-username')).toHaveAttribute('aria-invalid', 'true')
    await expect(tenantSection).toHaveAttribute('data-open', 'false')
    await expect(page).toHaveURL(/action=create/)
    expect(posted).toBe(false)
  })
})
