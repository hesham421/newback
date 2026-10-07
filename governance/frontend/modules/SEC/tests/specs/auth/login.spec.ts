/** AUTH: tenant-scoped staff login, tenant/credential refusals, logout, expiry, suspended tenant, wrong portal, reset link, sign-up (steps 03, 12). */
import { expect, test } from '@playwright/test'
import { e2eTenant, hasPassword, MISSING_ENV_REASON, password, PLATFORM, STORAGE_KEYS, USERS } from '../../pom/env.ts'
import { LoginPage } from '../../pom/LoginPage.ts'
import { setTenantStatus } from '../../pom/platformApi.ts'
import { ShellPage } from '../../pom/ShellPage.ts'
import { decodeJwt, forgeJwt, signInViaUi, storedToken, useLanguage } from '../../pom/session.ts'

const LOGIN_URL = '/api/v1/sec/auth/login'

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test('TC-FE-AUTH-001 admin of PLATFORM signs in with X-Tenant-Code, gets a STAFF token, lands on /dashboard with the tenant in the Topbar', async ({ page }) => {
  const login = new LoginPage(page)
  await login.goto()

  const loginRequest = page.waitForRequest((r) => r.url().endsWith(LOGIN_URL))
  const loginResponse = page.waitForResponse((r) => r.url().endsWith(LOGIN_URL))
  const menuResponse = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/menu'))
  await login.signIn(PLATFORM, USERS.admin, password)

  const sent = await loginRequest
  expect(sent.headers()['x-tenant-code']).toBe(PLATFORM)
  expect(sent.headers()['authorization']).toBeUndefined()
  const answer = await loginResponse
  expect(answer.status()).toBe(200)
  const { accessToken, expiresIn } = ((await answer.json()) as { data: { accessToken: string; expiresIn: number } }).data
  const claims = decodeJwt(accessToken)
  expect(claims).toMatchObject({ sub: USERS.admin, realm: 'STAFF' })
  expect(typeof claims.tid).toBe('number')
  expect(typeof claims.jti).toBe('string')
  expect((claims.exp ?? 0) - (claims.iat ?? 0)).toBe(expiresIn)
  expect((await menuResponse).status()).toBe(200)
  await expect(page).toHaveURL(/\/dashboard$/, { timeout: 30_000 })
  await expect(new ShellPage(page).tenant).toHaveText(PLATFORM)
})

test('TC-FE-AUTH-002 an unknown tenant is refused inline under the Tenant field, with no toast and no navigation', async ({ page }) => {
  const login = new LoginPage(page)
  await login.goto()

  const refused = page.waitForResponse((r) => r.url().endsWith(LOGIN_URL))
  await login.signIn('NOPE_X', USERS.admin, password)
  const answer = await refused
  expect(answer.status()).toBe(404)
  const body = (await answer.json()) as { error: Record<string, unknown> }
  expect(body.error.code).toBe('TENANT_NOT_FOUND')
  expect(body.error).not.toHaveProperty('fieldErrors')

  await expect(login.tenant).toHaveAttribute('aria-invalid', 'true')
  await expect(await login.tenantMessage()).toHaveText('No tenant exists with this code.')
  await expect(page.getByTestId('toast-container')).toHaveCount(0)
  await expect(page).toHaveURL(/\/login$/)
})

test('TC-FE-AUTH-003 a suspended tenant is refused inline (TENANT_SUSPENDED) and the tenant is re-activated afterwards', async ({ page, request }) => {
  await setTenantStatus(request, e2eTenant, 'SUSPENDED')
  try {
    const login = new LoginPage(page)
    await login.goto()
    const refused = page.waitForResponse((r) => r.url().endsWith(LOGIN_URL))
    await login.signIn(e2eTenant, USERS.e2eAdmin, password)
    const answer = await refused
    expect(answer.status()).toBe(403)
    expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('TENANT_SUSPENDED')
    await expect(login.tenant).toHaveAttribute('aria-invalid', 'true')
    await expect(await login.tenantMessage()).toHaveText('This tenant is suspended. Contact your administrator.')
    await expect(page.getByTestId('toast-container')).toHaveCount(0)
    await expect(page).toHaveURL(/\/login$/)
    expect(await storedToken(page)).toBeNull()
  } finally {
    await setTenantStatus(request, e2eTenant, 'ACTIVE')
  }
})

test('TC-FE-AUTH-004 wrong credentials answer 401 SEC-401-INVALID-CREDENTIALS and show "Invalid credentials" above the form', async ({ page }) => {
  const login = new LoginPage(page)
  await login.goto()
  const refused = page.waitForResponse((r) => r.url().endsWith(LOGIN_URL))
  await login.signIn(PLATFORM, 'e2e.nobody', password)
  const answer = await refused
  expect(answer.status()).toBe(401)
  expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('SEC-401-INVALID-CREDENTIALS')
  await expect(login.form.getByRole('alert')).toHaveText('Invalid credentials')
  await expect(login.tenant).not.toHaveAttribute('aria-invalid', 'true')
  await expect(page).toHaveURL(/\/login$/)
  expect(await storedToken(page)).toBeNull()
})

test('TC-FE-AUTH-005 logout calls POST /sec/auth/logout with the bearer, returns to /login, and the tenant stays pre-filled', async ({ page }) => {
  const login = new LoginPage(page)
  await login.goto()
  await expect(login.tenant).toHaveValue(PLATFORM)
  await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
  await expect(new ShellPage(page).tenant).toHaveText(e2eTenant)

  const logoutRequest = page.waitForRequest((r) => r.method() === 'POST' && r.url().endsWith('/api/v1/sec/auth/logout'))
  const logoutResponse = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/logout'))
  await new ShellPage(page).signOut.click()

  const sent = await logoutRequest
  expect(sent.headers()['authorization']).toMatch(/^Bearer /)
  expect(sent.headers()['x-tenant-code']).toBeUndefined()
  expect((await logoutResponse).status()).toBe(200)
  await expect(page).toHaveURL(/\/login$/)
  await expect(login.tenant).toHaveValue(e2eTenant)
  expect(await storedToken(page)).toBeNull()

  await page.reload()
  await expect(login.tenant).toHaveValue(e2eTenant)
})

test('TC-FE-AUTH-006 a stored STAFF token reaching its exp signs the user out to /login with the session-expired toast', async ({ page }) => {
  const now = Math.floor(Date.now() / 1000)
  /* The client signs out 30 s before exp, so exp = now + 34 s fires about 4 s after load. */
  const token = forgeJwt({ sub: USERS.admin, jti: 'e2e-expiry', uid: 1, tid: 1, realm: 'STAFF', iat: now, exp: now + 34 })
  await page.addInitScript(
    ([key, value, tenantKey]) => {
      if (!window.sessionStorage.getItem('e2e-seeded')) {
        window.localStorage.setItem(key, value)
        window.localStorage.setItem(tenantKey, 'PLATFORM')
        window.sessionStorage.setItem('e2e-seeded', '1')
      }
    },
    [STORAGE_KEYS.token, token, STORAGE_KEYS.tenant]
  )
  /* ⚠ The forged token would be refused (401) by the server before exp; held requests keep the expiry timer the only exit. */
  await page.route('**/api/v1/**', () => undefined)

  await page.goto('/dashboard')
  await expect(page.getByTestId('toast-container')).toContainText('Your session has expired. Please sign in again.', {
    timeout: 20_000,
  })
  await expect(page).toHaveURL(/\/login$/)
  expect(await storedToken(page)).toBeNull()
})

test('TC-FE-AUTH-007 a tenant suspended after login ends the session on the next request (403 TENANT_SUSPENDED → /login + toast)', async ({ page, request }) => {
  await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
  await setTenantStatus(request, e2eTenant, 'SUSPENDED')
  try {
    const refused = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/menu'))
    await page.goto('/security/users')
    const answer = await refused
    expect(answer.status()).toBe(403)
    expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('TENANT_SUSPENDED')
    await expect(page.getByTestId('toast-container')).toContainText('This tenant is suspended. Contact your administrator.')
    await expect(page).toHaveURL(/\/login$/)
    expect(await storedToken(page)).toBeNull()
  } finally {
    await setTenantStatus(request, e2eTenant, 'ACTIVE')
  }
})

test('TC-FE-AUTH-008 a login answered with a non-STAFF token is refused as the wrong portal and nothing is stored', async ({ page }) => {
  const now = Math.floor(Date.now() / 1000)
  const customerToken = forgeJwt({ sub: 'customer@example.test', uid: 9, tid: 1, realm: 'CUSTOMER', iat: now, exp: now + 3600 })
  await page.route(`**${LOGIN_URL}`, (route) =>
    route.fulfill({
      status: 200,
      json: { success: true, data: { accessToken: customerToken, tokenType: 'Bearer', expiresIn: 3600 } },
    })
  )
  const login = new LoginPage(page)
  await login.goto()
  await login.signIn(PLATFORM, USERS.admin, password)
  await expect(login.form.getByRole('alert')).toHaveText(
    'This account cannot sign in here. Use the portal meant for your account.'
  )
  await expect(page).toHaveURL(/\/login$/)
  expect(await storedToken(page)).toBeNull()
})

test('TC-FE-AUTH-009 a reset e-mail link (no tenant in it) opens the reset step with the Tenant field pre-filled', async ({ page }) => {
  await page.addInitScript(([key, value]) => window.localStorage.setItem(key, value), [STORAGE_KEYS.tenant, e2eTenant])
  for (const path of ['/password-reset/complete?token=e2e-link-token', '/reset?token=e2e-link-token']) {
    await page.goto(path)
    await expect(page.getByTestId('reset-complete-tenant')).toHaveValue(e2eTenant)
    await expect(page.locator('#password-reset-token')).toHaveValue('e2e-link-token')
  }
})

test('TC-FE-AUTH-010 sign-up sends X-Tenant-Code without a bearer and the 201 shows the pending-review state', async ({ page }) => {
  await page.goto('/sign-up')
  await page.getByTestId('signup-tenant').fill(PLATFORM)
  await page.locator('#signup-email').fill(`e2e.signup.${Date.now()}@example.test`)
  await page.locator('#signup-full-name-ar').fill('طلب تسجيل الاختبار')
  await page.locator('#signup-full-name-en').fill('E2E Sign-up')
  const sent = page.waitForRequest((r) => r.method() === 'POST' && r.url().endsWith('/api/v1/sec/auth/signup'))
  const answered = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/signup'))
  await page.getByRole('button', { name: 'Submit request' }).click()
  const request = await sent
  expect(request.headers()['x-tenant-code']).toBe(PLATFORM)
  expect(request.headers()['authorization']).toBeUndefined()
  expect((await answered).status()).toBe(201)
  await expect(page.getByText('Your request has been submitted')).toBeVisible()
})

test('TC-FE-AUTH-011 a login answered 409 CONCURRENT_MODIFICATION shows the one "try again" message above the fields, no toast, nothing stored', async ({ page }) => {
  await page.route(`**${LOGIN_URL}`, (route) =>
    route.fulfill({
      status: 409,
      json: {
        success: false,
        error: { code: 'CONCURRENT_MODIFICATION', message: 'The record was modified concurrently', fieldErrors: null },
        timestamp: new Date().toISOString(),
      },
    })
  )
  const login = new LoginPage(page)
  await login.goto()
  const refused = page.waitForResponse((r) => r.url().endsWith(LOGIN_URL))
  await login.signIn(PLATFORM, USERS.admin, password)
  expect((await refused).status()).toBe(409)
  await expect(login.form.getByRole('alert')).toHaveText('Another sign-in for this account is in progress. Please try again.')
  await expect(login.tenant).not.toHaveAttribute('aria-invalid', 'true')
  await expect(new ShellPage(page).toasts.getByText('Another sign-in', { exact: false })).toHaveCount(0)
  await expect(page).toHaveURL(/\/login$/)
  expect(await storedToken(page)).toBeNull()
})
