/** CUSTOMER portal (v2-04), live on E2E_T2: the flows the backend produces on its own, inside a context that also holds the staff admin session. */
import { expect, test, type Page } from '@playwright/test'
import { CustomerPortalPage, CUSTOMER_TOKEN_KEY, uniqueCustomerEmail } from '../../pom/CustomerPortalPage.ts'
import { e2eTenant, hasPassword, MISSING_ENV_REASON, password, PLATFORM, STATE, STORAGE_KEYS } from '../../pom/env.ts'
import { ShellPage } from '../../pom/ShellPage.ts'
import { storedToken, useLanguage } from '../../pom/session.ts'

const PUBLIC = '/api/v1/public/customers'
/* ⚠ Observed limiter: 10 attempts per tenant:realm:e-mail, then 429 (GAP-SEC-015); the loop allows a little more. */
const RATE_LIMIT_BUDGET = 14

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

function answerOf(page: Page, path: string) {
  return page.waitForResponse((r) => r.url().endsWith(`${PUBLIC}/${path}`))
}

test('TC-FE-CUST-001 register a unique e-mail on E2E_T2 → 201 and the "check your e-mail" state; the call carries the tenant and no bearer of either realm', async ({ page }) => {
  const portal = new CustomerPortalPage(page)
  const email = uniqueCustomerEmail('reg')
  const sent = page.waitForRequest((r) => r.url().endsWith(`${PUBLIC}/register`))
  const answered = answerOf(page, 'register')
  await portal.register(e2eTenant, email, password)

  const request = await sent
  expect(request.headers()['x-tenant-code']).toBe(e2eTenant)
  expect(request.headers()['authorization']).toBeUndefined()
  const response = await answered
  expect(response.status()).toBe(201)
  const body = (await response.json()) as { data: { statusCode: string; realm: string } }
  expect(body.data).toMatchObject({ statusCode: 'PENDING_VERIFICATION', realm: 'CUSTOMER' })
  await expect(portal.field('customer-register-done')).toContainText(email)
  expect(await portal.storedCustomerToken()).toBeNull()
})

test('TC-FE-CUST-002 registering the same e-mail again → 409 CUSTOMER_EMAIL_TAKEN under the e-mail field', async ({ page }) => {
  const portal = new CustomerPortalPage(page)
  const email = uniqueCustomerEmail('dup')
  const first = answerOf(page, 'register')
  await portal.register(e2eTenant, email, password)
  expect((await first).status()).toBe(201)

  const second = answerOf(page, 'register')
  await portal.register(e2eTenant, email, password)
  const response = await second
  expect(response.status()).toBe(409)
  expect(((await response.json()) as { error: { code: string } }).error.code).toBe('CUSTOMER_EMAIL_TAKEN')
  await expect(portal.field('customer-register-email')).toHaveAttribute('aria-invalid', 'true')
  await expect(await portal.messageOf('customer-register-email')).toHaveText(/already exists/)
  await expect(portal.field('customer-register-done')).toHaveCount(0)
})

test('TC-FE-CUST-003 signing in before verifying → 403 CUSTOMER_NOT_VERIFIED inline with the no-resend explanation; no customer session', async ({ page }) => {
  const portal = new CustomerPortalPage(page)
  const email = uniqueCustomerEmail('unv')
  const registered = answerOf(page, 'register')
  await portal.register(e2eTenant, email, password)
  expect((await registered).status()).toBe(201)

  await portal.openLogin()
  const answered = answerOf(page, 'login')
  await portal.submitLogin(e2eTenant, email, password)
  const response = await answered
  expect(response.status()).toBe(403)
  expect(((await response.json()) as { error: { code: string } }).error.code).toBe('CUSTOMER_NOT_VERIFIED')
  await expect(portal.banner).toContainText('Verify your e-mail address first')
  await expect(portal.banner).toContainText('cannot be requested')
  await expect(page).toHaveURL(/\/customer\/login$/)
  expect(await portal.storedCustomerToken()).toBeNull()
})

test('TC-FE-CUST-004 /customer/verify?token=<random> auto-submits once → 409 VERIFY_TOKEN_INVALID message', async ({ page }) => {
  await page.addInitScript((tenant) => window.localStorage.setItem('avelynq_customer_tenant_code', tenant), e2eTenant)
  const portal = new CustomerPortalPage(page)
  const token = `e2e-random-${Date.now()}`
  const posts: string[] = []
  page.on('request', (r) => {
    if (r.url().endsWith(`${PUBLIC}/verify`)) posts.push(r.postData() ?? '')
  })
  const answered = answerOf(page, 'verify')
  await page.goto(`/customer/verify?token=${token}`)
  const response = await answered
  expect(response.status()).toBe(409)
  expect(((await response.json()) as { error: { code: string } }).error.code).toBe('VERIFY_TOKEN_INVALID')
  expect(response.request().headers()['x-tenant-code']).toBe(e2eTenant)
  await expect(portal.field('customer-verify-error')).toContainText('invalid, has expired or was already used')
  expect(posts).toEqual([JSON.stringify({ token })])
})

test('TC-FE-CUST-005 repeated failed sign-ins → 429 CUSTOMER_LOGIN_RATE_LIMITED with the wait hint', async ({ page }) => {
  const portal = new CustomerPortalPage(page)
  const email = uniqueCustomerEmail('rate')
  await portal.openLogin()
  let status = 0
  for (let attempt = 1; attempt <= RATE_LIMIT_BUDGET && status !== 429; attempt += 1) {
    const answered = answerOf(page, 'login')
    if (attempt === 1) await portal.submitLogin(e2eTenant, email, `wrong-${attempt}`)
    else await portal.field('customer-login-submit').click()
    status = (await answered).status()
    if (status !== 429) {
      expect(status).toBe(401)
      await expect(portal.banner).toContainText('incorrect')
    }
  }
  expect(status).toBe(429)
  await expect(portal.banner).toContainText('Too many sign-in attempts')
})

test('TC-FE-CUST-006 tenant refusal and reset flows the backend produces: unknown tenant inline, reset request confirmation, bogus reset token', async ({ page }) => {
  const portal = new CustomerPortalPage(page)
  await portal.openLogin()
  const refused = answerOf(page, 'login')
  await portal.submitLogin('NOPE_X', uniqueCustomerEmail('tenant'), password)
  expect((await refused).status()).toBe(404)
  await expect(portal.field('customer-login-tenant')).toHaveAttribute('aria-invalid', 'true')
  await expect(await portal.messageOf('customer-login-tenant')).toHaveText('No tenant exists with this code.')

  await page.goto('/customer/password-reset')
  await portal.field('customer-reset-request-tenant').fill(e2eTenant)
  await portal.field('customer-reset-request-email').fill(uniqueCustomerEmail('reset'))
  const requested = answerOf(page, 'password-reset/request')
  await portal.field('customer-reset-request-submit').click()
  expect((await requested).status()).toBe(200)
  await expect(portal.field('customer-reset-request-done')).toBeVisible()

  await page.goto('/customer/password-reset/complete?token=e2e-bogus')
  await expect(portal.field('customer-reset-complete-token')).toHaveValue('e2e-bogus')
  await portal.field('customer-reset-complete-tenant').fill(e2eTenant)
  await portal.field('customer-reset-complete-new-password').fill(password)
  await portal.field('customer-reset-complete-confirm-password').fill(password)
  const completed = answerOf(page, 'password-reset/complete')
  await portal.field('customer-reset-complete-submit').click()
  expect((await completed).status()).toBe(409)
  await expect(portal.banner).toContainText('reset link is invalid')
})

test('TC-FE-CUST-007 the staff session is untouched by the customer portal, and does not open /customer', async ({ page }) => {
  await page.goto('/customer')
  await expect(page).toHaveURL(/\/customer\/login$/)
  const staffBefore = await storedToken(page)
  expect(staffBefore).toBeTruthy()

  const portal = new CustomerPortalPage(page)
  const answered = answerOf(page, 'login')
  await portal.submitLogin(e2eTenant, uniqueCustomerEmail('iso'), password)
  expect((await answered).status()).toBe(401)
  await expect(portal.field('customer-shell')).toBeVisible()
  await expect(page.getByTestId('topbar-sign-out')).toHaveCount(0)

  expect(await storedToken(page)).toBe(staffBefore)
  expect(await page.evaluate((key) => window.localStorage.getItem(key), STORAGE_KEYS.tenant)).toBe(PLATFORM)
  expect(await portal.storedCustomerToken()).toBeNull()
  expect(await page.evaluate((key) => window.localStorage.getItem(key), CUSTOMER_TOKEN_KEY)).toBeNull()

  const staffTab = await page.context().newPage()
  await staffTab.goto('/dashboard')
  await expect(new ShellPage(staffTab).tenant).toHaveText(PLATFORM)
  await expect(new ShellPage(staffTab).signOut).toBeVisible()
  await staffTab.close()
})
