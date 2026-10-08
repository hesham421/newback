/** PLATFORM_TENANTS on erp-core 1.3.0 (TM-F3, TENANT P2_5 TEN-U20 … U131): edit, branding, suspension facts, usage, admin reset, revoke tokens, export, idempotent create, profile search. */
import { expect, test, type APIRequestContext, type Browser, type BrowserContext, type Page, type Request } from '@playwright/test'
import { e2eTenant, hasPassword, MISSING_ENV_REASON, password, PLATFORM, STATE, STORAGE_KEYS, USERS } from '../../pom/env.ts'
import { PNG } from '../../pom/FileBrowserPage.ts'
import { doublePress, zeroGapPress } from '../../pom/doublePress.ts'
import { LoginPage } from '../../pom/LoginPage.ts'
import { signInViaUi, useLanguage } from '../../pom/session.ts'
import { FILE_ONLY, HarnessApi, T9, type TenantRow } from '../../pom/tenantHarness.ts'

const TENANTS = '/api/v1/platform/tenants'
const byId = (id: number) => new RegExp(`${TENANTS}/${id}$`)
const sub = (id: number, path: string) => new RegExp(`${TENANTS}/${id}/${path}$`)
const STAMP = Date.now() % 1_000_000
const CONTACT_LOCAL = `t9.contact.${STAMP}`
const CONTACT_EMAIL = `${CONTACT_LOCAL}@example.test`
/** A fresh context: the describe's admin storage state must not follow a tenant user's sign-in. */
const SIGNED_OUT = { storageState: { cookies: [], origins: [] }, viewport: { width: 1440, height: 900 } }
/** `branding.errTenantTokenRevoked` (TEN-U135), the toast of a 401 TENANT_TOKEN_REVOKED. */
const REVOKED_MESSAGE = "Your organisation's sessions were ended. Please sign in again."
const SCRIPT_SVG = Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="8" height="8"><script>alert(1)</script></svg>')

test.describe.configure({ mode: 'serial' })
test.use({ storageState: STATE.admin })

let harnessContext: APIRequestContext
let api: HarnessApi
let t9: TenantRow
let t2: TenantRow
let platform: TenantRow
let firstLogoUrl = ''
let exportedFileId = 0
let contextB: BrowserContext | null = null

async function signInAsT9(browser: Browser, userPassword = password): Promise<{ context: BrowserContext; page: Page }> {
  const context = await browser.newContext(SIGNED_OUT)
  const page = await context.newPage()
  await page.addInitScript((key) => window.localStorage.setItem(key, 'en'), STORAGE_KEYS.lang)
  const login = new LoginPage(page)
  await login.goto()
  await login.signIn(T9.code, T9.admin, userPassword)
  await expect(page).toHaveURL(/\/dashboard$/, { timeout: 30_000 })
  return { context, page }
}

function collect(page: Page, predicate: (request: Request) => boolean): Request[] {
  const seen: Request[] = []
  page.on('request', (request) => {
    if (predicate(request)) seen.push(request)
  })
  return seen
}

async function openDetail(page: Page, id: number): Promise<void> {
  await page.goto(`/platform/tenants?tenantId=${id}`)
  await expect(page.getByTestId('tenant-detail-drawer')).toBeVisible()
  await expect(page.getByTestId('tenant-detail-code')).not.toHaveText('—')
}

function toast(page: Page) {
  return page.getByTestId('toast-container')
}

test.beforeAll(async ({ playwright }) => {
  if (!hasPassword) return
  harnessContext = await playwright.request.newContext()
  api = await HarnessApi.asAdmin(harnessContext)
  t9 = await api.ensureT9Baseline()
  t2 = await api.tenant(e2eTenant)
  platform = await api.tenant(PLATFORM)
})

test.afterAll(async () => {
  await contextB?.close()
  if (!api) return
  const now = await api.tenant(T9.code)
  if (now.statusCode !== 'ACTIVE') await api.ok('PATCH', `${TENANTS}/${now.id}/status`, { statusCode: 'ACTIVE' })
  await api.clearBranding(now.id)
  await api.restoreAdminPassword(now.id, T9.admin)
  await api.signOut()
  await harnessContext.dispose()
})

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test.describe('PLATFORM_TENANTS 1.3.0', () => {
  test('TC-FE-PLATFORM-007 edit names and profile on ?editId= (code locked): PUT sends every editable field, country upper-cased, an emptied note as ""; an invalid country is refused inline', async ({ page }) => {
    await openDetail(page, t9.id)
    await page.getByTestId('tenant-action-edit').click()
    await expect(page).toHaveURL(new RegExp(`editId=${t9.id}`))
    await expect(page).not.toHaveURL(/tenantId=/)
    const code = page.getByTestId('tenant-edit-code')
    await expect(code).toHaveValue(T9.code)
    await expect(code).toHaveAttribute('readonly', '')
    await expect(page.getByTestId('tenant-edit-drawer')).toContainText('The tenant code cannot be changed.')

    await page.getByTestId('tenant-edit-contactEmail').fill(CONTACT_EMAIL)
    await page.getByTestId('tenant-edit-contactPhone').fill('+966 11 555 0100')
    await page.getByTestId('tenant-edit-countryCode').fill('sa')
    await expect(page.getByTestId('tenant-edit-countryCode')).toHaveValue('SA')
    await page.getByTestId('tenant-edit-defaultLocale').selectOption('ar')
    await page.getByTestId('tenant-edit-timezone').fill('Asia/Riyadh')
    await page.getByTestId('tenant-edit-notes').fill('E2E notes')
    const saved = page.waitForResponse((r) => r.request().method() === 'PUT' && byId(t9.id).test(r.url()))
    await page.getByTestId('tenant-edit-submit').click()
    const response = await saved
    expect(response.status()).toBe(200)
    const body = response.request().postDataJSON() as Record<string, unknown>
    expect(body).toEqual({
      nameAr: 'مستأجر الاختبار التاسع',
      nameEn: 'E2E Tenant Nine',
      contactEmail: CONTACT_EMAIL,
      contactPhone: '+966 11 555 0100',
      countryCode: 'SA',
      defaultLocale: 'ar',
      timezone: 'Asia/Riyadh',
      notes: 'E2E notes',
    })
    expect(body).not.toHaveProperty('code')
    expect(body).not.toHaveProperty('statusCode')
    await expect(toast(page)).toContainText(`Tenant ${T9.code} has been updated.`)
    await expect(page).toHaveURL(new RegExp(`tenantId=${t9.id}`))
    await expect(page.getByTestId('tenant-detail-contactEmail')).toHaveText(CONTACT_EMAIL)
    await expect(page.getByTestId('tenant-detail-countryCode')).toHaveText('SA')
    await expect(page.getByTestId('tenant-detail-defaultLocale')).toHaveText('العربية')
    await expect(page.getByTestId('tenant-detail-notes')).toHaveText('E2E notes')

    await page.getByTestId('tenant-action-edit').click()
    await page.getByTestId('tenant-edit-notes').fill('')
    const cleared = page.waitForResponse((r) => r.request().method() === 'PUT' && byId(t9.id).test(r.url()))
    await page.getByTestId('tenant-edit-submit').click()
    const clearedResponse = await cleared
    expect(clearedResponse.status()).toBe(200)
    expect((clearedResponse.request().postDataJSON() as { notes: string }).notes).toBe('')
    await expect(page.getByTestId('tenant-detail-notes')).toHaveText('—')

    const puts = collect(page, (r) => r.method() === 'PUT' && byId(t9.id).test(r.url()))
    await page.getByTestId('tenant-action-edit').click()
    await page.getByTestId('tenant-edit-countryCode').fill('sau')
    await page.getByTestId('tenant-edit-submit').click()
    await expect(page.getByTestId('tenant-edit-countryCode')).toHaveAttribute('aria-invalid', 'true')
    await expect(page.getByTestId('tenant-edit-drawer')).toContainText('Use two upper-case letters (ISO 3166-1), e.g. SA.')
    expect(puts).toHaveLength(0)
  })

  test('TC-FE-PLATFORM-025 the filter drawer searches by contact e-mail (LIKE) and country (EQUALS, upper-cased); the detail shows the profile', async ({ page }) => {
    await page.goto('/platform/tenants')
    await page.getByRole('button', { name: 'Filter' }).click()
    await page.getByTestId('tenants-filter-contactEmail').fill(CONTACT_LOCAL)
    const byEmail = page.waitForRequest((r) => r.url().endsWith(`${TENANTS}/search`) && JSON.stringify(r.postDataJSON()).includes(CONTACT_LOCAL))
    await page.getByRole('button', { name: 'Apply' }).click()
    expect((await byEmail).postDataJSON().filters).toContainEqual({ field: 'contactEmail', operator: 'LIKE', value: CONTACT_LOCAL })
    await expect(page.getByTestId(`tenant-row-${T9.code}`)).toBeVisible()

    await page.getByRole('button', { name: 'Filter' }).click()
    await page.getByTestId('tenants-filter-contactEmail').fill('')
    await page.getByTestId('tenants-filter-countryCode').fill('sa')
    const byCountry = page.waitForRequest((r) => r.url().endsWith(`${TENANTS}/search`) && JSON.stringify(r.postDataJSON()).includes('countryCode'))
    await page.getByRole('button', { name: 'Apply' }).click()
    const filters = (await byCountry).postDataJSON().filters as Array<{ field: string }>
    expect(filters).toContainEqual({ field: 'countryCode', operator: 'EQUALS', value: 'SA' })
    for (const filter of filters) expect(['code', 'nameEn', 'nameAr', 'contactEmail', 'countryCode', 'statusCode']).toContain(filter.field)
    await expect(page.getByTestId(`tenant-row-${T9.code}`)).toBeVisible()

    await page.getByTestId(`tenant-open-${T9.code}`).click()
    await expect(page.getByTestId('tenant-detail-contactEmail')).toHaveText(CONTACT_EMAIL)
    await expect(page.getByTestId('tenant-detail-contactPhone')).toHaveText('+966 11 555 0100')
    await expect(page.getByTestId('tenant-detail-countryCode')).toHaveText('SA')
    await expect(page.getByTestId('tenant-detail-defaultLocale')).toHaveText('العربية')
    await expect(page.getByTestId('tenant-detail-timezone')).toHaveText('Asia/Riyadh')
    await expect(page.getByTestId('tenant-detail-notes')).toHaveText('—')
  })

  test('TC-FE-PLATFORM-008 Change branding opens ?logoFor=: a PNG and #1a2b3c are saved in order; the preview is the RETURNED logoUrl', async ({ page }) => {
    await openDetail(page, t9.id)
    await expect(page.getByTestId('tenant-branding-mark-only')).toHaveText('Platform mark only')
    await page.getByTestId('tenant-branding-change').click()
    await expect(page).toHaveURL(new RegExp(`tenantId=${t9.id}.*logoFor=${t9.id}`))
    const drawer = page.getByTestId('tenant-branding-drawer')
    await expect(drawer).toBeVisible()
    await expect(page.getByTestId('tenant-logo-constraints')).toContainText('PNG or WebP recommended; an SVG must be plain or optimised')

    await page.getByTestId('tenant-logo-file').setInputFiles({ name: 'logo-a.png', mimeType: 'image/png', buffer: PNG })
    await expect(page.getByTestId('tenant-logo-preview')).toHaveAttribute('src', /^blob:/)
    await page.getByTestId('tenant-brand-color').fill('#1a2b3c')
    const logoSaved = page.waitForResponse((r) => r.request().method() === 'PUT' && sub(t9.id, 'logo').test(r.url()))
    const colorSaved = page.waitForResponse((r) => r.request().method() === 'PATCH' && sub(t9.id, 'branding').test(r.url()))
    await page.getByTestId('tenant-branding-save').click()
    const logoResponse = await logoSaved
    expect(logoResponse.status()).toBe(200)
    expect(logoResponse.request().headers()['content-type']).toMatch(/^multipart\/form-data/)
    firstLogoUrl = ((await logoResponse.json()) as { data: { logoUrl: string } }).data.logoUrl
    expect(firstLogoUrl).toMatch(new RegExp(`^/api/v1/public/files/${T9.code}/`))
    const colorResponse = await colorSaved
    expect(colorResponse.status()).toBe(200)
    expect(colorResponse.request().postDataJSON()).toEqual({ brandColor: '#1a2b3c' })
    await expect(toast(page)).toContainText('The logo has been saved.')
    await expect(toast(page)).toContainText('The brand colour has been saved.')
    await expect(page.getByTestId('tenant-logo-preview')).toHaveAttribute('src', firstLogoUrl)
    await expect(page.getByTestId('tenant-branding-logo')).toHaveAttribute('src', firstLogoUrl)
    await expect(page.getByTestId('tenant-branding-color')).toHaveText('#1A2B3C')
  })

  test('TC-FE-PLATFORM-009 replacing the logo answers a new URL; the preview and the branding row follow it; no colour PATCH', async ({ page }) => {
    await page.goto(`/platform/tenants?tenantId=${t9.id}&logoFor=${t9.id}`)
    await expect(page.getByTestId('tenant-logo-preview')).toHaveAttribute('src', firstLogoUrl)
    const patches = collect(page, (r) => r.method() === 'PATCH' && sub(t9.id, 'branding').test(r.url()))
    await page.getByTestId('tenant-logo-file').setInputFiles({ name: 'logo-b.png', mimeType: 'image/png', buffer: PNG })
    const saved = page.waitForResponse((r) => r.request().method() === 'PUT' && sub(t9.id, 'logo').test(r.url()))
    await page.getByTestId('tenant-branding-save').click()
    const response = await saved
    expect(response.status()).toBe(200)
    const nextUrl = ((await response.json()) as { data: { logoUrl: string } }).data.logoUrl
    expect(nextUrl).not.toBe(firstLogoUrl)
    await expect(toast(page)).toContainText('The logo has been saved.')
    await expect(page.getByTestId('tenant-logo-preview')).toHaveAttribute('src', nextUrl)
    await expect(page.getByTestId('tenant-branding-logo')).toHaveAttribute('src', nextUrl)
    expect(patches).toHaveLength(0)
    firstLogoUrl = nextUrl
  })

  test('TC-FE-PLATFORM-010 an SVG with a script is refused inline with the server message and no colour is sent; #12345 is refused on the colour', async ({ page }) => {
    await page.goto(`/platform/tenants?tenantId=${t9.id}&logoFor=${t9.id}`)
    const patches = collect(page, (r) => r.method() === 'PATCH' && sub(t9.id, 'branding').test(r.url()))
    await page.getByTestId('tenant-logo-file').setInputFiles({ name: 'unsafe.svg', mimeType: 'image/svg+xml', buffer: SCRIPT_SVG })
    await page.getByTestId('tenant-brand-color').fill('#334455')
    const refused = page.waitForResponse((r) => r.request().method() === 'PUT' && sub(t9.id, 'logo').test(r.url()))
    await page.getByTestId('tenant-branding-save').click()
    const response = await refused
    expect(response.status()).toBe(400)
    expect(((await response.json()) as { error: { code: string } }).error.code).toBe('TENANT_LOGO_INVALID')
    await expect(page.getByTestId('tenant-logo-file')).toHaveAttribute('aria-invalid', 'true')
    await expect(page.getByTestId('tenant-branding-drawer')).toContainText('plain or optimised SVG')
    expect(patches).toHaveLength(0)
    await expect(page.getByTestId('tenant-branding-logo')).toHaveAttribute('src', firstLogoUrl)

    await page.reload()
    await page.getByTestId('tenant-brand-color').fill('#12345')
    await page.getByTestId('tenant-branding-save').click()
    await expect(page.getByTestId('tenant-brand-color')).toHaveAttribute('aria-invalid', 'true')
    await expect(page.getByTestId('tenant-branding-drawer')).toContainText('Use #RRGGBB (six hexadecimal digits).')
    expect(patches).toHaveLength(0)
    await expect(page.getByTestId('tenant-branding-color')).toHaveText('#1A2B3C')
  })

  test('TC-FE-PLATFORM-011 Remove logo asks a ConfirmDialog naming the tenant: Cancel sends nothing, confirm DELETEs (204) → "Platform mark only"', async ({ page }) => {
    await page.goto(`/platform/tenants?tenantId=${t9.id}&logoFor=${t9.id}`)
    const deletes = collect(page, (r) => r.method() === 'DELETE' && sub(t9.id, 'logo').test(r.url()))
    await page.getByTestId('tenant-logo-remove').click()
    const dialog = page.getByRole('alertdialog')
    await expect(dialog).toContainText('Remove the logo?')
    await expect(dialog).toContainText(T9.code)
    await dialog.getByTestId('confirm-dialog-cancel').click()
    await expect(dialog).toHaveCount(0)
    expect(deletes).toHaveLength(0)

    await page.getByTestId('tenant-logo-remove').click()
    const removed = page.waitForResponse((r) => r.request().method() === 'DELETE' && sub(t9.id, 'logo').test(r.url()))
    await page.getByRole('alertdialog').getByTestId('confirm-dialog-confirm').click()
    expect((await removed).status()).toBe(204)
    await expect(toast(page)).toContainText('The logo has been removed.')
    await expect(page.getByTestId('tenant-logo-mark-only')).toHaveText('Platform mark only')
    await expect(page.getByTestId('tenant-branding-mark-only')).toHaveText('Platform mark only')
  })

  test('TC-FE-PLATFORM-012 the logo is visible after re-login as that tenant', async ({ page, browser }) => {
    await page.goto(`/platform/tenants?tenantId=${t9.id}&logoFor=${t9.id}`)
    await page.getByTestId('tenant-logo-file').setInputFiles({ name: 'logo-c.png', mimeType: 'image/png', buffer: PNG })
    const saved = page.waitForResponse((r) => r.request().method() === 'PUT' && sub(t9.id, 'logo').test(r.url()))
    await page.getByTestId('tenant-branding-save').click()
    const logoUrl = ((await (await saved).json()) as { data: { logoUrl: string } }).data.logoUrl
    try {
      const context = await browser.newContext(SIGNED_OUT)
      const t9Page = await context.newPage()
      await useLanguage(t9Page)
      const branding = t9Page.waitForResponse((r) => r.url().endsWith('/api/v1/tenant/me'))
      await new LoginPage(t9Page).goto()
      await new LoginPage(t9Page).signIn(T9.code, T9.admin, password)
      expect(((await (await branding).json()) as { data: { logoUrl: string } }).data.logoUrl).toBe(logoUrl)
      await expect(t9Page.getByTestId('sidebar-brand-mark')).toBeVisible()
      await expect(t9Page.getByTestId('tenant-logo')).toHaveAttribute('src', logoUrl)
      await context.close()
    } finally {
      await api.clearBranding(t9.id)
    }
  })

  test('TC-FE-PLATFORM-013 Suspend needs a reason of 3..500 (Confirm disabled until then); the detail shows when, by whom and why', async ({ page, browser }) => {
    contextB = (await signInAsT9(browser)).context
    await openDetail(page, t9.id)
    await page.getByTestId('tenant-action-suspend').click()
    const dialog = page.getByRole('alertdialog')
    await expect(dialog).toContainText('All of its users are signed out at once and cannot sign in until it is activated again')
    const confirm = dialog.getByTestId('confirm-dialog-confirm')
    const reason = dialog.getByTestId('tenant-suspend-reason')
    await expect(reason).toBeVisible()
    await expect(confirm).toBeDisabled()
    await reason.fill('ab')
    await expect(dialog).toContainText('2 / 500')
    await expect(confirm).toBeDisabled()
    await reason.fill('Unpaid invoice')
    await expect(confirm).toBeEnabled()
    const patched = page.waitForResponse((r) => r.request().method() === 'PATCH' && sub(t9.id, 'status').test(r.url()))
    await confirm.click()
    const response = await patched
    expect(response.status()).toBe(200)
    expect(response.request().postDataJSON()).toEqual({ statusCode: 'SUSPENDED', reason: 'Unpaid invoice' })
    await expect(toast(page)).toContainText(`Tenant ${T9.code} is now Suspended.`)
    await expect(page.getByTestId('tenant-detail-drawer').getByTestId('tenant-status-badge')).toHaveAttribute('data-status', 'SUSPENDED')
    const facts = page.getByTestId('tenant-suspension-facts')
    await expect(facts.getByTestId('tenant-suspended-by')).toHaveText(USERS.admin)
    await expect(facts.getByTestId('tenant-suspension-reason')).toHaveText('Unpaid invoice')
    await expect(facts.getByTestId('tenant-suspended-at')).toContainText(String(new Date().getFullYear()))
  })

  test('TC-FE-PLATFORM-014 Activate clears the facts; a session opened before the suspension answers 401 TENANT_TOKEN_REVOKED and lands on /login; signing in again works', async ({ page }) => {
    await openDetail(page, t9.id)
    const patched = page.waitForResponse((r) => r.request().method() === 'PATCH' && sub(t9.id, 'status').test(r.url()))
    await page.getByTestId('tenant-action-activate').click()
    const response = await patched
    expect(response.status()).toBe(200)
    expect(response.request().postDataJSON()).toEqual({ statusCode: 'ACTIVE' })
    await expect(page.getByTestId('tenant-detail-drawer').getByTestId('tenant-status-badge')).toHaveAttribute('data-status', 'ACTIVE')
    await expect(page.getByTestId('tenant-suspension-facts')).toHaveCount(0)

    expect(contextB, 'context B from TC-FE-PLATFORM-013').not.toBeNull()
    const pageB = (contextB as BrowserContext).pages()[0]
    const revoked = pageB.waitForResponse((r) => r.url().includes('/api/v1/') && r.status() === 401)
    await pageB.goto('/security/users')
    const answer = await revoked
    expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('TENANT_TOKEN_REVOKED')
    await expect(pageB).toHaveURL(/\/login/)
    await expect(pageB.getByTestId('toast-container').getByText(REVOKED_MESSAGE)).toHaveCount(1)
    const login = new LoginPage(pageB)
    const signedIn = pageB.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/login'))
    await login.signIn(T9.code, T9.admin, password)
    expect((await signedIn).status()).toBe(200)
    await expect(pageB).toHaveURL(/\/dashboard$/, { timeout: 30_000 })
    await (contextB as BrowserContext).close()
    contextB = null
  })

  test('TC-FE-PLATFORM-015 usage is read only when its section opens, and again on each opening and on Refresh', async ({ page }) => {
    const usage = collect(page, (r) => r.method() === 'GET' && sub(t2.id, 'usage').test(r.url()))
    await openDetail(page, t2.id)
    await expect(page.getByTestId('tenant-usage')).toHaveAttribute('data-open', 'false')
    expect(usage).toHaveLength(0)

    const first = page.waitForResponse((r) => sub(t2.id, 'usage').test(r.url()))
    await page.getByTestId('tenant-usage-toggle').click()
    expect((await first).status()).toBe(200)
    await expect(page.getByTestId('tenant-usage-staffUsers')).toHaveText(/^[1-9]\d*$/)
    await expect(page.getByTestId('tenant-usage-collected-at')).toContainText('Counted at')
    expect(usage).toHaveLength(1)

    await page.getByTestId('tenant-usage-toggle').click()
    const second = page.waitForResponse((r) => sub(t2.id, 'usage').test(r.url()))
    await page.getByTestId('tenant-usage-toggle').click()
    await second
    await expect(page.getByTestId('tenant-usage-staffUsers')).toBeVisible()
    expect(usage).toHaveLength(2)

    const third = page.waitForResponse((r) => sub(t2.id, 'usage').test(r.url()))
    await page.getByTestId('tenant-usage-refresh').click()
    await third
    expect(usage).toHaveLength(3)
  })

  test('TC-FE-PLATFORM-016 admin reset on ?adminResetFor= asks a final ConfirmDialog; unknown user and policy refusals are inline; success toasts the sessions; the next sign-in must change the password', async ({ page, browser }) => {
    const NEW_PASSWORD = `${password}R9`
    const resets = collect(page, (r) => r.method() === 'POST' && sub(t9.id, 'admin-reset').test(r.url()))
    try {
      await openDetail(page, t9.id)
      await page.getByTestId('tenant-action-admin-reset').click()
      await expect(page).toHaveURL(new RegExp(`adminResetFor=${t9.id}`))
      const drawer = page.getByTestId('tenant-admin-reset-drawer')
      await expect(drawer).toBeVisible()
      await expect(page.getByTestId('tenant-admin-reset-require-change')).toBeChecked()

      await page.getByTestId('tenant-admin-reset-username').fill('nobody')
      await page.getByTestId('tenant-admin-reset-password').fill(NEW_PASSWORD)
      await page.getByTestId('tenant-admin-reset-confirm-password').fill(NEW_PASSWORD)
      await page.getByTestId('tenant-admin-reset-submit').click()
      const dialog = page.getByRole('alertdialog')
      await expect(dialog).toContainText('Reset this password?')
      await expect(dialog).toContainText(`The password of nobody in ${T9.code} is replaced`)
      expect(resets).toHaveLength(0)
      const notFound = page.waitForResponse((r) => sub(t9.id, 'admin-reset').test(r.url()))
      await dialog.getByTestId('confirm-dialog-confirm').click()
      expect((await notFound).status()).toBe(404)
      await expect(page.getByTestId('tenant-admin-reset-username')).toHaveAttribute('aria-invalid', 'true')
      await expect(drawer).toContainText('No staff user of this tenant has this username.')

      await page.getByTestId('tenant-admin-reset-username').fill(T9.admin)
      await page.getByTestId('tenant-admin-reset-password').fill('abcdefgh')
      await page.getByTestId('tenant-admin-reset-confirm-password').fill('abcdefgh')
      await page.getByTestId('tenant-admin-reset-submit').click()
      const policy = page.waitForResponse((r) => sub(t9.id, 'admin-reset').test(r.url()))
      await page.getByRole('alertdialog').getByTestId('confirm-dialog-confirm').click()
      const policyAnswer = await policy
      expect(policyAnswer.status()).toBe(400)
      expect(((await policyAnswer.json()) as { error: { code: string } }).error.code).toBe('SEC-400-PASSWORD-POLICY')
      await expect(page.getByTestId('tenant-admin-reset-password')).toHaveAttribute('aria-invalid', 'true')
      await expect(drawer).toContainText('password policy')

      await page.getByTestId('tenant-admin-reset-password').fill(NEW_PASSWORD)
      await page.getByTestId('tenant-admin-reset-confirm-password').fill(NEW_PASSWORD)
      await page.getByTestId('tenant-admin-reset-submit').click()
      const done = page.waitForResponse((r) => sub(t9.id, 'admin-reset').test(r.url()))
      await page.getByRole('alertdialog').getByTestId('confirm-dialog-confirm').click()
      const doneAnswer = await done
      expect(doneAnswer.status()).toBe(200)
      expect(doneAnswer.request().postDataJSON()).toEqual({
        username: T9.admin,
        newPassword: NEW_PASSWORD,
        requireChangeAtNextLogin: true,
      })
      await expect(toast(page)).toContainText(new RegExp(`The password of ${T9.admin} has been reset\\. \\d+ session\\(s\\) ended\\.`))
      await expect(page).not.toHaveURL(/adminResetFor=/)

      const context = await browser.newContext(SIGNED_OUT)
      const t9Page = await context.newPage()
      await useLanguage(t9Page)
      const login = new LoginPage(t9Page)
      await login.goto()
      const signedIn = t9Page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/login'))
      await login.signIn(T9.code, T9.admin, NEW_PASSWORD)
      const loginAnswer = await signedIn
      expect(loginAnswer.status()).toBe(200)
      expect(((await loginAnswer.json()) as { data: { passwordChangeRequired: boolean } }).data.passwordChangeRequired).toBe(true)
      await expect(t9Page).toHaveURL(/\/account\/change-password$/, { timeout: 30_000 })
      await expect(t9Page.getByTestId('account-forced-change')).toBeVisible()
      await context.close()
    } finally {
      await api.restoreAdminPassword(t9.id, T9.admin)
    }
  })

  test('TC-FE-PLATFORM-017 the PLATFORM row offers neither admin reset nor revoke tokens, and a deep-linked adminResetFor is dropped', async ({ page }) => {
    const forbidden = collect(page, (r) => sub(platform.id, 'admin-reset').test(r.url()) || sub(platform.id, 'revoke-tokens').test(r.url()))
    await openDetail(page, platform.id)
    await expect(page.getByTestId('tenant-detail-code')).toHaveText(PLATFORM)
    await expect(page.getByTestId('tenant-action-admin-reset')).toHaveCount(0)
    await expect(page.getByTestId('tenant-action-revoke-tokens')).toHaveCount(0)
    await expect(page.getByTestId('tenant-action-edit')).toBeVisible()
    await expect(page.getByTestId('tenant-action-export')).toBeVisible()
    await expect(page.getByTestId('tenant-branding-change')).toBeVisible()

    await page.goto(`/platform/tenants?tenantId=${platform.id}&adminResetFor=${platform.id}`)
    await expect(page.getByTestId('tenant-detail-code')).toHaveText(PLATFORM)
    await expect(page).not.toHaveURL(/adminResetFor=/)
    await expect(page.getByTestId('tenant-admin-reset-drawer')).toHaveCount(0)
    expect(forbidden).toHaveLength(0)
  })

  test('TC-FE-PLATFORM-018 Sign every user out asks a ConfirmDialog; POST revoke-tokens (no body) answers the count; the tenant session lands on /login', async ({ page, browser }) => {
    const { context, page: t9Page } = await signInAsT9(browser)
    try {
      await openDetail(page, t9.id)
      await page.getByTestId('tenant-action-revoke-tokens').click()
      const dialog = page.getByRole('alertdialog')
      await expect(dialog).toContainText('Sign every user of this tenant out?')
      await expect(dialog).toContainText(T9.code)
      const revoked = page.waitForResponse((r) => r.request().method() === 'POST' && sub(t9.id, 'revoke-tokens').test(r.url()))
      await dialog.getByTestId('confirm-dialog-confirm').click()
      const response = await revoked
      expect(response.status()).toBe(200)
      expect(response.request().postData() ?? '').toBe('')
      const data = ((await response.json()) as { data: { id: number; code: string; sessionsTerminated: number } }).data
      expect(data).toMatchObject({ id: t9.id, code: T9.code })
      expect(data.sessionsTerminated).toBeGreaterThanOrEqual(1)
      await expect(toast(page)).toContainText(new RegExp(`Every user of ${T9.code} has been signed out\\. ${data.sessionsTerminated} session\\(s\\) ended\\.`))
      await expect(page.getByRole('alertdialog')).toHaveCount(0)

      const refused = t9Page.waitForResponse((r) => r.url().includes('/api/v1/') && r.status() === 401)
      await t9Page.goto('/security/users')
      expect(((await (await refused).json()) as { error: { code: string } }).error.code).toBe('TENANT_TOKEN_REVOKED')
      await expect(t9Page).toHaveURL(/\/login/)
      await expect(t9Page.getByTestId('toast-container').getByText(REVOKED_MESSAGE)).toHaveCount(1)
    } finally {
      await context.close()
    }
  })

  test('TC-FE-PLATFORM-019 a 500 TENANT_REVOKE_SESSIONS_FAILED keeps the dialog open with the message and Retry; the retry repeats the call live', async ({ page }) => {
    let answered = 0
    await page.route(sub(t9.id, 'revoke-tokens'), async (route) => {
      answered += 1
      if (answered === 1) {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, error: { code: 'TENANT_REVOKE_SESSIONS_FAILED', message: 'Ending the sessions failed' } }),
        })
        return
      }
      await route.continue()
    })
    await openDetail(page, t9.id)
    await page.getByTestId('tenant-action-revoke-tokens').click()
    const dialog = page.getByRole('alertdialog')
    await dialog.getByTestId('confirm-dialog-confirm').click()
    await expect(dialog).toContainText('The old sign-ins are already refused, but ending the open sessions failed. Try again.')
    const retry = dialog.getByTestId('tenant-revoke-retry')
    await expect(retry).toBeVisible()
    await expect(page.getByText('has been signed out')).toHaveCount(0)
    await expect(page).toHaveURL(/\/platform\/tenants/)

    const live = page.waitForResponse((r) => r.request().method() === 'POST' && sub(t9.id, 'revoke-tokens').test(r.url()))
    await retry.click()
    expect((await live).status()).toBe(200)
    expect(answered).toBe(2)
    await expect(toast(page)).toContainText(`Every user of ${T9.code} has been signed out.`)
    await expect(page.getByRole('alertdialog')).toHaveCount(0)
  })

  test('TC-FE-PLATFORM-020 Export asks a ConfirmDialog, runs once with progress and a disabled Cancel, then downloads the ZIP at once with the returned token', async ({ page }) => {
    const exports = collect(page, (r) => r.method() === 'POST' && sub(t2.id, 'export').test(r.url()))
    let release: () => void = () => undefined
    const held = new Promise<void>((resolve) => {
      release = resolve
    })
    await page.route(sub(t2.id, 'export'), async (route) => {
      const response = await route.fetch({ timeout: 120_000 })
      await held
      await route.fulfill({ response })
    })
    await openDetail(page, t2.id)
    await page.getByTestId('tenant-action-export').click()
    const dialog = page.getByRole('alertdialog')
    await expect(dialog).toContainText('A large tenant takes tens of seconds')

    const exported = page.waitForResponse((r) => r.request().method() === 'POST' && sub(t2.id, 'export').test(r.url()), { timeout: 120_000 })
    const downloaded = page.waitForResponse((r) => r.url().includes('/api/v1/files/download?token='), { timeout: 120_000 })
    const download = page.waitForEvent('download', { timeout: 120_000 })
    await dialog.getByTestId('confirm-dialog-confirm').click()
    await expect(page.getByTestId('tenant-export-progress')).toBeVisible()
    await expect(page.getByTestId('tenant-export-progress')).toContainText(/Exporting… \d+ s/)
    await expect(dialog.getByTestId('confirm-dialog-cancel')).toBeDisabled()
    release()

    const response = await exported
    expect(response.status()).toBe(200)
    expect(response.request().postData() ?? '').toBe('')
    const data = ((await response.json()) as {
      data: { fileId: number; fileName: string; rowCount: number; downloadToken: string; tenantCode: string }
    }).data
    exportedFileId = data.fileId
    const tokenDownload = await downloaded
    expect(tokenDownload.status()).toBe(200)
    expect(tokenDownload.url()).toContain(`token=${encodeURIComponent(data.downloadToken)}`)
    expect((await download).suggestedFilename()).toMatch(new RegExp(`^tenant-export-${e2eTenant}-.*\\.zip$`))

    const result = page.getByTestId('tenant-export-result')
    await expect(result).toContainText(data.fileName)
    await expect(result).toContainText(`${data.rowCount} rows`)
    await expect(page.getByTestId('tenant-export-open-files')).toHaveAttribute(
      'href',
      `/files/browser?moduleCode=TENANT&ownerType=CORE_TENANT&ownerId=${t2.id}&fileId=${data.fileId}`
    )
    await expect(toast(page)).toContainText(`The export of ${e2eTenant} has been downloaded.`)
    await expect(dialog.getByTestId('confirm-dialog-confirm')).toHaveCount(0)
    expect(exports).toHaveLength(1)
  })

  test('TC-FE-PLATFORM-021 export refusals (409 / 422 / 429) and a failed download are messages inside the dialog; nothing is retried and the session stays', async ({ page }) => {
    const answers = [
      { status: 409, code: 'TENANT_EXPORT_IN_PROGRESS', message: 'running', expected: 'An export of this tenant is already running.' },
      {
        status: 422,
        code: 'TENANT_EXPORT_TOO_LARGE',
        message: 'The tenant holds 612000 rows; the export limit is 500000 rows',
        expected: 'The tenant holds 612000 rows; the export limit is 500000 rows',
      },
      { status: 429, code: 'TENANT_EXPORT_BUSY', message: 'busy', expected: 'Too many exports are running. Try again shortly.' },
    ]
    let calls = 0
    let current = answers[0]
    await page.route(sub(t2.id, 'export'), async (route) => {
      calls += 1
      await route.fulfill({
        status: current.status,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, error: { code: current.code, message: current.message } }),
      })
    })
    await openDetail(page, t2.id)
    for (const answer of answers) {
      current = answer
      const before = calls
      await page.getByTestId('tenant-action-export').click()
      const dialog = page.getByRole('alertdialog')
      await dialog.getByTestId('confirm-dialog-confirm').click()
      await expect(dialog.getByTestId('tenant-export-error')).toHaveText(answer.expected)
      expect(calls).toBe(before + 1)
      await dialog.getByTestId('confirm-dialog-cancel').click()
      await expect(dialog).toHaveCount(0)
    }

    await page.unroute(sub(t2.id, 'export'))
    await page.route(sub(t2.id, 'export'), (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            tenantId: t2.id,
            tenantCode: e2eTenant,
            fileId: 999_999,
            fileName: 'tenant-export-routed.zip',
            sizeBytes: 10,
            rowCount: 1,
            downloadToken: 'used-token',
          },
        }),
      })
    )
    await page.route(/\/api\/v1\/files\/download\?token=used-token$/, (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, error: { code: 'FILE_ACCESS_TOKEN_INVALID', message: 'invalid' } }),
      })
    )
    await page.getByTestId('tenant-action-export').click()
    await page.getByRole('alertdialog').getByTestId('confirm-dialog-confirm').click()
    await expect(page.getByTestId('tenant-export-download-error')).toHaveText(
      'The archive was created but the download failed; get it from the file browser.'
    )
    await expect(page).toHaveURL(/\/platform\/tenants/)
    await expect(page.getByTestId('tenant-detail-drawer')).toBeVisible()
  })

  test('TC-FE-PLATFORM-022 the archive is re-downloaded through the FILE deep link; a FILE-only user never sees it; the scope keeps module TENANT', async ({ page, browser }) => {
    expect(exportedFileId, 'archive of TC-FE-PLATFORM-020').toBeGreaterThan(0)
    await api.ensureFileOnlyUser()
    const deepLink = `/files/browser?moduleCode=TENANT&ownerType=CORE_TENANT&ownerId=${t2.id}`

    await openDetail(page, t2.id)
    const listed = page.waitForResponse((r) => r.url().includes('/api/v1/files?') && r.url().includes('moduleCode=TENANT'))
    await page.getByTestId('tenant-archives-link').click()
    await expect(page).toHaveURL(new RegExp(`/files/browser\\?moduleCode=TENANT&ownerType=CORE_TENANT&ownerId=${t2.id}$`))
    expect((await listed).status()).toBe(200)
    await page.getByTestId(`file-row-${exportedFileId}`).click()
    await expect(page).toHaveURL(new RegExp(`fileId=${exportedFileId}`))
    const minted = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith(`/api/v1/files/${exportedFileId}/access-token`))
    const download = page.waitForEvent('download')
    await page.getByTestId('file-meta-download').click()
    expect((await minted).status()).toBeLessThan(300)
    expect((await download).suggestedFilename()).toMatch(/\.zip$/)

    await page.goto(deepLink)
    await page.getByTestId('file-browser-scope').click()
    await expect(page.getByTestId('file-scope-module')).toHaveValue('TENANT')
    await expect(page.getByTestId('file-scope-module').locator('option[value="TENANT"]')).toHaveCount(1)
    await expect(page.getByTestId('file-scope-owner-id')).toHaveValue(String(t2.id))

    const context = await browser.newContext(SIGNED_OUT)
    try {
      const filePage = await context.newPage()
      await useLanguage(filePage)
      await signInViaUi(filePage, PLATFORM, FILE_ONLY.username)
      const ownerList = filePage.waitForResponse((r) => r.url().includes('/api/v1/files?') && r.url().includes('moduleCode=TENANT'))
      await filePage.goto(deepLink)
      expect((await ownerList).status()).toBe(200)
      await expect(filePage.getByTestId('file-browser-scope')).toBeVisible()
      await expect(filePage.getByTestId(`file-row-${exportedFileId}`)).toHaveCount(0)

      const single = filePage.waitForResponse((r) => r.request().method() === 'GET' && r.url().endsWith(`/api/v1/files/${exportedFileId}`))
      await filePage.goto(`${deepLink}&fileId=${exportedFileId}`)
      const refused = await single
      expect(refused.status()).toBe(404)
      expect(((await refused.json()) as { error: { code: string } }).error.code).toBe('FILE_DOCUMENT_NOT_FOUND')
      await expect(filePage.getByTestId('toast-container')).toContainText('This item is unavailable')
      await expect(filePage.getByText('tenant-export-')).toHaveCount(0)
    } finally {
      await context.close()
    }
  })

  test('TC-FE-PLATFORM-023 a create whose answer was lost is retried with the SAME Idempotency-Key and body → replayed 201 = success; a new form open mints a new key', async ({ page }) => {
    const code = `E2E_IK_${STAMP}`
    const sentKeys: string[] = []
    let attempts = 0
    await page.route(new RegExp(`${TENANTS}$`), async (route) => {
      if (route.request().method() !== 'POST') return route.continue()
      attempts += 1
      sentKeys.push(route.request().headers()['idempotency-key'] ?? '')
      if (attempts === 1) {
        await route.fetch()
        await route.abort('failed')
        return
      }
      if (attempts === 3) {
        await route.abort('failed')
        return
      }
      await route.continue()
    })
    await page.goto('/platform/tenants?action=create')
    await fillCreate(page, code, 'ikadmin', password)
    await page.getByTestId('tenant-form-submit').click()
    await expect(page.getByTestId('tenant-form-drawer')).toContainText('The tenants could not be processed. Please try again.')
    expect(sentKeys[0]).toMatch(/^[0-9a-f-]{36}$/)

    const replayed = page.waitForResponse((r) => r.request().method() === 'POST' && new RegExp(`${TENANTS}$`).test(r.url()))
    await page.getByTestId('tenant-form-submit').click()
    const response = await replayed
    expect(response.status()).toBe(201)
    expect(response.headers()['idempotent-replayed']).toBe('true')
    expect(sentKeys[1]).toBe(sentKeys[0])
    await expect(toast(page)).toContainText(`Tenant ${code} has been created with its administrator ikadmin.`)
    await expect(page).not.toHaveURL(/action=create/)
    const found = await api.ok<{ content: Array<{ code: string }> }>('POST', `${TENANTS}/search`, {
      filters: [{ field: 'code', operator: 'LIKE', value: code }],
      page: 0,
      size: 5,
    })
    expect(found.content.filter((row) => row.code === code)).toHaveLength(1)

    await page.getByTestId('tenant-create').click()
    await fillCreate(page, `E2E_IK2_${STAMP}`, 'ikadmin', password)
    await page.getByTestId('tenant-form-submit').click()
    await expect.poll(() => sentKeys.length).toBe(3)
    expect(sentKeys[2]).toMatch(/^[0-9a-f-]{36}$/)
    expect(sentKeys[2]).not.toBe(sentKeys[0])
  })

  test('TC-FE-PLATFORM-024 a weak administrator password is refused inline by the policy; a routed 409 IDEMPOTENCY_KEY_CONFLICT shows the banner and the next submit carries a new key', async ({ page }) => {
    const code = `E2E_PW_${STAMP}`
    const keys: string[] = []
    let routed = 0
    await page.route(new RegExp(`${TENANTS}$`), async (route) => {
      if (route.request().method() !== 'POST') return route.continue()
      keys.push(route.request().headers()['idempotency-key'] ?? '')
      if (keys.length === 1) return route.continue()
      routed += 1
      if (routed === 1) {
        return route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, error: { code: 'IDEMPOTENCY_KEY_CONFLICT', message: 'conflict' } }),
        })
      }
      return route.abort('failed')
    })
    await page.goto('/platform/tenants?action=create')
    await expect(page.getByTestId('tenant-form-drawer')).toContainText('At least 8 characters with a letter and a digit, at most 72 bytes.')
    await fillCreate(page, code, 'pwadmin', 'abcdefgh')
    const refused = page.waitForResponse((r) => r.request().method() === 'POST' && new RegExp(`${TENANTS}$`).test(r.url()))
    await page.getByTestId('tenant-form-submit').click()
    const response = await refused
    expect(response.status()).toBe(400)
    expect(((await response.json()) as { error: { code: string } }).error.code).toBe('SEC-400-PASSWORD-POLICY')
    await expect(page.getByTestId('tenant-form-admin-password')).toHaveAttribute('aria-invalid', 'true')
    await expect(page.getByTestId('tenant-form-drawer')).toContainText('password policy')
    const absent = await api.ok<{ content: Array<{ code: string }> }>('POST', `${TENANTS}/search`, {
      filters: [{ field: 'code', operator: 'LIKE', value: code }],
      page: 0,
      size: 5,
    })
    expect(absent.content.filter((row) => row.code === code)).toHaveLength(0)

    await page.getByTestId('tenant-form-admin-password').fill(password)
    await page.getByTestId('tenant-form-admin-password-confirm').fill(password)
    await page.getByTestId('tenant-form-submit').click()
    await expect(page.getByTestId('tenant-form-drawer')).toContainText('This submission was already used with other values.')
    expect(keys[1]).not.toBe(keys[0])

    await page.getByTestId('tenant-form-submit').click()
    await expect.poll(() => keys.length).toBe(3)
    expect(keys[2]).toMatch(/^[0-9a-f-]{36}$/)
    expect(keys[2]).not.toBe(keys[1])
  })

  test('TC-FE-PLATFORM-026 a double press on Save sends one request: the edit PUT and the branding PATCH (TEN-U144)', async ({ page }) => {
    const puts = collect(page, (r) => r.method() === 'PUT' && byId(t9.id).test(r.url()))
    await openDetail(page, t9.id)
    await page.getByTestId('tenant-action-edit').click()
    await page.getByTestId('tenant-edit-notes').fill('E2E double press')
    const saved = page.waitForResponse((r) => r.request().method() === 'PUT' && byId(t9.id).test(r.url()))
    await doublePress(page.getByTestId('tenant-edit-submit'))
    expect((await saved).status()).toBe(200)
    await expect(page).toHaveURL(new RegExp(`tenantId=${t9.id}`))
    await expect(page.getByTestId('tenant-detail-notes')).toHaveText('E2E double press')
    expect(puts).toHaveLength(1)

    await page.getByTestId('tenant-action-edit').click()
    await page.getByTestId('tenant-edit-notes').fill('')
    const cleared = page.waitForResponse((r) => r.request().method() === 'PUT' && byId(t9.id).test(r.url()))
    await zeroGapPress(page.getByTestId('tenant-edit-submit'))
    expect((await cleared).status()).toBe(200)
    await expect(page.getByTestId('tenant-detail-notes')).toHaveText('—')
    expect(puts).toHaveLength(2)

    const patches = collect(page, (r) => r.method() === 'PATCH' && sub(t9.id, 'branding').test(r.url()))
    const logos = collect(page, (r) => sub(t9.id, 'logo').test(r.url()))
    try {
      await page.getByTestId('tenant-branding-change').click()
      await page.getByTestId('tenant-brand-color').fill('#2a3b4c')
      const colorSaved = page.waitForResponse((r) => r.request().method() === 'PATCH' && sub(t9.id, 'branding').test(r.url()))
      await doublePress(page.getByTestId('tenant-branding-save'))
      expect((await colorSaved).status()).toBe(200)
      await expect(page.getByTestId('tenant-branding-color')).toHaveText('#2A3B4C')
      await expect(toast(page).getByText('The brand colour has been saved.')).toHaveCount(1)
      expect(patches).toHaveLength(1)
      expect(logos).toHaveLength(0)
    } finally {
      await api.clearBranding(t9.id)
    }
  })

  test('TC-FE-PLATFORM-027 a double press on a final Confirm sends one request: the admin reset and the export (TEN-U144)', async ({ page }) => {
    const NEW_PASSWORD = `${password}D7`
    const resets = collect(page, (r) => r.method() === 'POST' && sub(t9.id, 'admin-reset').test(r.url()))
    const exports = collect(page, (r) => r.method() === 'POST' && sub(t9.id, 'export').test(r.url()))
    try {
      await openDetail(page, t9.id)
      await page.getByTestId('tenant-action-admin-reset').click()
      await page.getByTestId('tenant-admin-reset-username').fill(T9.admin)
      await page.getByTestId('tenant-admin-reset-password').fill(NEW_PASSWORD)
      await page.getByTestId('tenant-admin-reset-confirm-password').fill(NEW_PASSWORD)
      await page.getByTestId('tenant-admin-reset-submit').click()
      const dialog = page.getByRole('alertdialog')
      await expect(dialog).toContainText('Reset this password?')
      const done = page.waitForResponse((r) => r.request().method() === 'POST' && sub(t9.id, 'admin-reset').test(r.url()))
      await zeroGapPress(dialog.getByTestId('confirm-dialog-confirm'))
      expect((await done).status()).toBe(200)
      await expect(page).not.toHaveURL(/adminResetFor=/)
      await expect(toast(page).getByText(new RegExp(`The password of ${T9.admin} has been reset`))).toHaveCount(1)
      expect(resets).toHaveLength(1)
    } finally {
      await api.restoreAdminPassword(t9.id, T9.admin)
    }

    await page.getByTestId('tenant-action-export').click()
    const dialog = page.getByRole('alertdialog')
    await expect(dialog).toContainText('A large tenant takes tens of seconds')
    const exported = page.waitForResponse((r) => r.request().method() === 'POST' && sub(t9.id, 'export').test(r.url()), { timeout: 120_000 })
    const download = page.waitForEvent('download', { timeout: 120_000 })
    await zeroGapPress(dialog.getByTestId('confirm-dialog-confirm'))
    expect((await exported).status()).toBe(200)
    await download
    await expect(page.getByTestId('tenant-export-result')).toBeVisible()
    await expect(dialog.getByTestId('confirm-dialog-confirm')).toHaveCount(0)
    expect(exports).toHaveLength(1)
  })
})

async function fillCreate(page: Page, code: string, adminUsername: string, adminPassword: string): Promise<void> {
  await page.getByTestId('tenant-form-code').fill(code)
  await page.getByTestId('tenant-form-name-ar').fill('مستأجر الإعادة')
  await page.getByTestId('tenant-form-name-en').fill(`E2E ${code}`)
  await page.getByTestId('tenant-form-admin-username').fill(adminUsername)
  await page.getByTestId('tenant-form-admin-email').fill(`${adminUsername}.${STAMP}@example.test`)
  await page.getByTestId('tenant-form-admin-password').fill(adminPassword)
  await page.getByTestId('tenant-form-admin-password-confirm').fill(adminPassword)
  await page.getByTestId('tenant-form-admin-full-name-ar').fill('مدير الإعادة')
  await page.getByTestId('tenant-form-admin-full-name-en').fill('Retry Admin')
}
