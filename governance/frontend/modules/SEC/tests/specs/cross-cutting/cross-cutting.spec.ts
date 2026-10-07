/** XCUT: realm guard, tenant-header rule, README §4.5 error policy, i18n direction, menu gating and unknown page codes (steps 03, 04, 12). */
import { expect, test, type Request } from '@playwright/test'
import { hasPassword, MISSING_ENV_REASON, PLATFORM, STATE, STORAGE_KEYS, USERS } from '../../pom/env.ts'
import { ShellPage } from '../../pom/ShellPage.ts'
import { forgeJwt, signInViaUi, storedToken, useLanguage } from '../../pom/session.ts'

const PUBLIC_AUTH = /\/api\/v1\/sec\/auth\/(login|signup|password-reset\/request|password-reset\/complete)$/
const NOT_AVAILABLE = 'This screen is not available in this application yet.'
const USERS_LIST_TIMEOUT_MS = 30_000
const USER_FILTER_FIELDS = ['username', 'email', 'fullNameAr', 'fullNameEn', 'statusCode', 'fullName']
const OPERATORS = ['EQUALS', 'NOT_EQUALS', 'LIKE', 'GREATER_THAN', 'GREATER_THAN_OR_EQUAL', 'LESS_THAN', 'LESS_THAN_OR_EQUAL', 'IN']

/* Every page-coded screen added by steps 06–11 (README §4.6), with its route and its <h1>. */
const NEW_SCREENS = [
  { code: 'CU_CONFIGURATIONS', route: '/settings/configurations', title: 'Tenant configurations' },
  { code: 'PLATFORM_SETTINGS', route: '/platform/settings', title: 'Platform default settings' },
  { code: 'PLATFORM_TENANTS', route: '/platform/tenants', title: 'Tenants' },
  { code: 'FILE_CATEGORIES', route: '/files/categories', title: 'File categories' },
  { code: 'FILE_BROWSER', route: '/files/browser', title: 'File browser' },
  { code: 'NOTIF_TEMPLATES', route: '/notifications/templates', title: 'Notification templates' },
  { code: 'NOTIF_CHANNELS', route: '/notifications/channels', title: 'Notification channels' },
  { code: 'NOTIF_LOG', route: '/notifications/logs', title: 'Notification log' },
  { code: 'AUDIT_EVENTS', route: '/audit/events', title: 'Audit events' },
  { code: 'SEQUENCE_SERIES', route: '/sequences/series', title: 'Number series' },
  { code: 'SEC_REPORTS', route: '/reports/SEC', title: 'SEC reports' },
  { code: 'NOTIF_REPORTS', route: '/reports/NOTIF', title: 'NOTIF reports' },
  { code: 'AUDIT_REPORTS', route: '/reports/AUDIT', title: 'AUDIT reports' },
] as const

function apiError(status: number, code: string, message: string, withFieldErrors = true) {
  const error = withFieldErrors ? { code, message, fieldErrors: null } : { code, message }
  return { status, json: { success: false, error, timestamp: new Date().toISOString() } }
}

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test('TC-FE-XCUT-001 a stored CUSTOMER-realm token is treated as absent on load: /login, token cleared, never sent', async ({ page }) => {
  const now = Math.floor(Date.now() / 1000)
  const forged = forgeJwt({ sub: 'customer@example.test', uid: 9, tid: 1, realm: 'CUSTOMER', iat: now, exp: now + 3600 })
  await page.addInitScript(([key, value]) => window.localStorage.setItem(key, value), [STORAGE_KEYS.token, forged])
  const carriers: string[] = []
  page.on('request', (r) => {
    if ((r.headers()['authorization'] ?? '').includes(forged)) carriers.push(r.url())
  })

  await page.goto('/dashboard')
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByTestId('login-form')).toBeVisible()
  await expect(new ShellPage(page).signOut).toHaveCount(0)
  /* The redirect is client-side, so this is still the document the token was seeded into. */
  expect(await storedToken(page)).toBeNull()
  expect(carriers).toEqual([])
})

test('TC-FE-XCUT-002 X-Tenant-Code rides only on public auth calls (never with Authorization); every other call is bearer-only', async ({ page }) => {
  const seen: Request[] = []
  page.on('request', (r) => {
    if (r.url().includes('/api/v1/')) seen.push(r)
  })
  await signInViaUi(page, PLATFORM, USERS.admin)
  /* ⚠ The users list renders only after the menu and the search answer; on a shared backend that can exceed 5 s. */
  const usersRead = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/users/search'), { timeout: USERS_LIST_TIMEOUT_MS })
  await page.goto('/security/users')
  expect((await usersRead).status()).toBe(200)
  await expect(page.getByTestId('user-realm-badge').first()).toBeVisible({ timeout: USERS_LIST_TIMEOUT_MS })
  await page.goto('/notifications/templates')
  await expect(page.getByRole('heading', { name: 'Notification templates' })).toBeVisible()

  const publicCalls = seen.filter((r) => PUBLIC_AUTH.test(new URL(r.url()).pathname))
  const privateCalls = seen.filter((r) => !PUBLIC_AUTH.test(new URL(r.url()).pathname))
  expect(publicCalls.length).toBeGreaterThan(0)
  expect(privateCalls.length).toBeGreaterThan(2)
  for (const request of publicCalls) {
    expect(request.headers()['x-tenant-code']).toBe(PLATFORM)
    expect(request.headers()['authorization']).toBeUndefined()
  }
  for (const request of privateCalls) {
    expect(request.headers()['authorization'], request.url()).toMatch(/^Bearer /)
    expect(request.headers()['x-tenant-code'], request.url()).toBeUndefined()
  }
})

test.describe('as PLATFORM admin (stored session)', () => {
  test.use({ storageState: STATE.admin })

  test('TC-FE-XCUT-003 a live 404 NOT_FOUND (1.2.0 unknown path) on a list read shows the item-unavailable toast once', async ({ page }) => {
    /* The roles search is re-addressed to a path the backend does not know; the live server answers it. */
    await page.route('**/api/v1/sec/roles/search', (route) =>
      route.continue({ url: route.request().url().replace('/sec/roles/search', '/sec/e2e-unknown/search') })
    )
    const answered = page.waitForResponse((r) => /\/api\/v1\/sec\/(roles|e2e-unknown)\/search$/.test(r.url()))
    await page.goto('/security/roles')
    const answer = await answered
    expect(answer.status()).toBe(404)
    expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('NOT_FOUND')
    const toasts = new ShellPage(page).toasts
    await expect(toasts).toContainText('This item is unavailable or has been deactivated. Refresh the list and try again.')
    await expect(toasts.getByText('This item is unavailable', { exact: false })).toHaveCount(1)
  })

  test('TC-FE-XCUT-014 a 405 METHOD_NOT_ALLOWED on a list read shows the method-not-available toast and stays on the screen', async ({ page }) => {
    /* Simulated: a live 405 needs a wrong method on a known path, which the app never sends (GAP-ALL-004). */
    await page.route('**/api/v1/sec/roles/search', (route) =>
      route.fulfill(apiError(405, 'METHOD_NOT_ALLOWED', 'Request method not supported'))
    )
    const answered = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/roles/search'))
    await page.goto('/security/roles')
    expect((await answered).status()).toBe(405)
    const toasts = new ShellPage(page).toasts
    await expect(toasts).toContainText('This action is not available on the server. Refresh the page and try again.')
    await expect(page).toHaveURL(/\/security\/roles$/)
    await expect(new ShellPage(page).forbiddenFor('SEC_ROLES')).toHaveCount(0)
  })

  test('TC-FE-XCUT-004 a 409 CONCURRENT_MODIFICATION on save shows the reload-and-retry toast and re-reads the list', async ({ page }) => {
    await page.goto('/settings/configurations')
    const edit = page.locator('[data-testid^="cu-config-edit-"]:not([disabled])').first()
    await expect(edit).toBeVisible()
    await edit.click()
    await expect(page).toHaveURL(/editKey=/)
    await page.route('**/api/v1/common/configurations/*', async (route) => {
      if (route.request().method() !== 'PUT') return route.fallback()
      return route.fulfill(apiError(409, 'CONCURRENT_MODIFICATION', 'The record was modified concurrently'))
    })
    const reread = page.waitForRequest((r) => r.url().endsWith('/api/v1/common/configurations/search?scope=TENANT') || r.url().includes('/configurations/search'))
    await page.getByTestId('cu-config-form-submit').click()
    await expect(new ShellPage(page).toasts).toContainText('This record was changed by someone else. Reload it and try again.')
    await reread
  })

  test('TC-FE-XCUT-005 a page beyond the server maximum (400 field=page) resets to the first page with the page-overflow toast', async ({ page }) => {
    const overflow = page.waitForResponse((r) => r.url().includes('/api/v1/audit/events?') && r.url().includes('page=200000000'))
    const recovered = page.waitForResponse(
      (r) => r.url().includes('/api/v1/audit/events?') && new URL(r.url()).searchParams.get('page') === '0'
    )
    await page.goto('/audit/events?page=200000000')
    const refused = await overflow
    expect(refused.status()).toBe(400)
    const body = (await refused.json()) as { error: { code: string; fieldErrors: { field: string }[] } }
    expect(body.error.code).toBe('VALIDATION_ERROR')
    expect(body.error.fieldErrors.map((item) => item.field)).toContain('page')
    await expect(new ShellPage(page).toasts).toContainText('That page no longer exists; showing the first page.')
    expect((await recovered).status()).toBe(200)
    await expect(page).not.toHaveURL(/page=200000000/)
  })

  test('TC-FE-XCUT-006 an unsupported filter field is refused by the server (400) and named in a toast; emitted operators are documented ones', async ({ page }) => {
    const sentOperators: string[] = []
    const sentFields: string[] = []
    await page.route('**/api/v1/sec/users/search', async (route) => {
      const body = route.request().postDataJSON() as { filters?: Array<{ field: string; operator: string }> }
      sentOperators.push(...(body.filters ?? []).map((filter) => filter.operator))
      sentFields.push(...(body.filters ?? []).map((filter) => filter.field))
      const filters = [...(body.filters ?? []), { field: 'bogusField', operator: 'EQUALS', value: 'x' }]
      await route.continue({ postData: JSON.stringify({ ...body, filters }) })
    })
    const refused = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/users/search'))
    await page.goto('/security/users')
    const answer = await refused
    expect(answer.status()).toBe(400)
    const body = (await answer.json()) as { error: { code: string; fieldErrors: { field: string }[] } }
    expect(body.error.code).toBe('VALIDATION_ERROR')
    expect(body.error.fieldErrors.map((item) => item.field)).toContain('bogusField')
    await expect(new ShellPage(page).toasts).toContainText(
      'Filtering on "bogusField" is not supported. Clear that filter and try again.'
    )

    /* A real filter, so the mapper's own fields and operators are on the wire. */
    const filtered = page.waitForRequest(
      (r) => r.url().endsWith('/api/v1/sec/users/search') && (r.postData() ?? '').includes('admin')
    )
    await page.locator('#user-quick-search').fill('admin')
    await page.locator('#user-quick-search').press('Enter')
    await filtered
    expect(sentOperators.length).toBeGreaterThan(0)
    for (const field of sentFields) expect(USER_FILTER_FIELDS).toContain(field)
    for (const operator of sentOperators) expect(OPERATORS).toContain(operator)
  })

  test('TC-FE-XCUT-007 switching AR/EN flips <html dir/lang> and the Topbar labels', async ({ page }) => {
    await page.goto('/dashboard')
    const shell = new ShellPage(page)
    const html = page.locator('html')
    await expect(html).toHaveAttribute('dir', 'ltr')
    await expect(html).toHaveAttribute('lang', 'en')
    await expect(shell.signOut).toHaveAccessibleName('Sign out')
    await expect(shell.language).toContainText('العربية')

    await shell.language.click()
    await expect(html).toHaveAttribute('dir', 'rtl')
    await expect(html).toHaveAttribute('lang', 'ar')
    await expect(shell.signOut).toHaveAccessibleName('تسجيل الخروج')
    await expect(shell.language).toContainText('English')
    expect(await page.evaluate((key) => window.localStorage.getItem(key), STORAGE_KEYS.lang)).toBe('ar')

    await shell.language.click()
    await expect(html).toHaveAttribute('dir', 'ltr')
    await expect(shell.signOut).toHaveAccessibleName('Sign out')
  })

  test('TC-FE-XCUT-012 a 403 REALM_MISMATCH on an authenticated call ends the session with the wrong-portal toast', async ({ page }) => {
    await page.route('**/api/v1/sec/menu', (route) =>
      route.fulfill(apiError(403, 'REALM_MISMATCH', 'Token realm does not match this endpoint', false))
    )
    await page.goto('/security/users')
    await expect(new ShellPage(page).toasts).toContainText(
      'This account cannot sign in here. Use the portal meant for your account.'
    )
    await expect(page).toHaveURL(/\/login$/)
    expect(await storedToken(page)).toBeNull()
  })

  test('TC-FE-XCUT-013 an unknown page code in the effective menu renders as a disabled, non-navigable entry', async ({ page }) => {
    await page.route('**/api/v1/sec/menu', async (route) => {
      const response = await route.fetch()
      const body = (await response.json()) as { data: Array<{ code: string; screens: unknown[] }> }
      const sec = body.data.find((module) => module.code === 'SEC')
      sec?.screens.push({ screenRegPk: 999_999, pageCode: 'FIN_GL_JOURNAL', nameAr: 'شاشة غير معروفة', nameEn: 'E2E unknown screen' })
      await route.fulfill({ response, json: body })
    })
    await page.goto('/dashboard')
    const entry = page.getByText('E2E unknown screen', { exact: true })
    await expect(entry).toHaveCount(1)
    await expect(page.locator('a', { hasText: 'E2E unknown screen' })).toHaveCount(0)
    await expect(page.locator('li', { has: entry }).last()).toContainText(NOT_AVAILABLE)
    await expect(new ShellPage(page).navLink('/security/users')).toHaveCount(1)
  })
})

test('TC-FE-XCUT-008 every new screen is gated by the menu: e2e.none gets the 403 view on deep links, e2e.all (explicit grants) gets the screen', async ({ browser }) => {
  test.setTimeout(120_000)
  const none = await browser.newContext({ storageState: STATE.none })
  const all = await browser.newContext({ storageState: STATE.all })
  try {
    const nonePage = await none.newPage()
    const allPage = await all.newPage()
    for (const screen of NEW_SCREENS) {
      await nonePage.goto(screen.route)
      await expect(new ShellPage(nonePage).forbiddenFor(screen.code), screen.code).toBeVisible()
      await expect(new ShellPage(nonePage).navLink(screen.route)).toHaveCount(0)

      await allPage.goto(screen.route)
      await expect(allPage.getByRole('heading', { level: 1, name: screen.title }), screen.code).toBeVisible()
      await expect(new ShellPage(allPage).forbiddenFor(screen.code)).toHaveCount(0)
      await expect(new ShellPage(allPage).navLink(screen.route)).toHaveCount(1)
    }
    await nonePage.goto('/security/dashboard')
    await expect(nonePage.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(new ShellPage(nonePage).forbiddenFor('SEC_DASHBOARD')).toHaveCount(0)
  } finally {
    await none.close()
    await all.close()
  }
})
