/** tm-F2 (erp-core 1.3.0, backend package E): shell tenant logo beside the platform mark, brand accent, sign-in page branding, cache lifetime, forced-change branding, TENANT_TOKEN_REVOKED — TC-FE-XCUT-015 … 024; tm-FZ: the sign-in pages on a phone — TC-FE-XCUT-025. */
import { expect, test, type APIRequestContext, type Browser, type Page, type Request } from '@playwright/test'
import { BrandingPage } from '../../pom/BrandingPage.ts'
import {
  activateTenant,
  revokeTenantTokens,
  setBrandColor,
  setTenantLogo,
  suspendTenant,
  type BrandingAnswer,
} from '../../pom/brandingHarness.ts'
import { backendUrl, e2eTenant, hasPassword, MISSING_ENV_REASON, password, PLATFORM, STATE, STORAGE_KEYS, USERS } from '../../pom/env.ts'
import { LoginPage } from '../../pom/LoginPage.ts'
import { adminToken, createStaffUser } from '../../pom/secApi.ts'
import { apiSignIn, bearer, signInViaUi, signOutViaUi, storedToken } from '../../pom/session.ts'
import { HarnessApi, T9, type TenantRow } from '../../pom/tenantHarness.ts'

const RUN = Date.now()
const TENANT_ME = '/api/v1/tenant/me'
const PUBLIC_BRANDING = /\/api\/v1\/public\/tenants\/([^/]+)\/branding$/
const ACCENT = '#1A2B3C'
const ACCENT_RGB = 'rgb(26, 43, 60)'
/* `--teal-400` of styles/tokens/colors.css — the 1.2.0 active marker. */
const TEAL_RGB = 'rgb(31, 187, 173)'
const REVOKED_MESSAGE = "Your organisation's sessions were ended. Please sign in again."

test.beforeEach(() => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
})

/** A fresh English context with no stored session and no stored tenant. */
async function freshPage(browser: Browser, viewport = { width: 1440, height: 900 }): Promise<Page> {
  const context = await browser.newContext({ viewport })
  const page = await context.newPage()
  await page.addInitScript((value) => window.localStorage.setItem('avelynq_lang', value), 'en')
  return page
}

async function storedAdminPage(browser: Browser): Promise<Page> {
  const context = await browser.newContext({ storageState: STATE.admin, viewport: { width: 1440, height: 900 } })
  return context.newPage()
}

/** Every `GET /tenant/me` answer the page receives, in order. */
function recordTenantMe(page: Page): BrandingAnswer[] {
  const answers: BrandingAnswer[] = []
  page.on('response', async (response) => {
    if (response.request().method() === 'GET' && new URL(response.url()).pathname === TENANT_ME) {
      answers.push(((await response.json()) as { data: BrandingAnswer }).data)
    }
  })
  return answers
}

/** Every public branding request the page sends (code + the two headers that must be absent). */
function recordPublicBranding(page: Page): Array<{ code: string; authorization?: string; tenant?: string }> {
  const sent: Array<{ code: string; authorization?: string; tenant?: string }> = []
  page.on('request', (request: Request) => {
    const match = PUBLIC_BRANDING.exec(new URL(request.url()).pathname)
    if (match) {
      const headers = request.headers()
      sent.push({ code: match[1], authorization: headers.authorization, tenant: headers['x-tenant-code'] })
    }
  })
  return sent
}

class Branding {
  readonly api: HarnessApi
  private readonly request: APIRequestContext
  private readonly token: string

  private constructor(request: APIRequestContext, api: HarnessApi, token: string) {
    this.request = request
    this.api = api
    this.token = token
  }

  static async open(request: APIRequestContext): Promise<Branding> {
    return new Branding(request, await HarnessApi.asAdmin(request), await adminToken(request))
  }

  async tenant(code: string): Promise<TenantRow> {
    return this.api.tenant(code)
  }

  async withLogo(code: string): Promise<{ tenant: TenantRow; logoUrl: string }> {
    const tenant = await this.tenant(code)
    return { tenant, logoUrl: await setTenantLogo(this.request, this.token, tenant) }
  }

  async clear(code: string): Promise<void> {
    await this.api.clearBranding((await this.tenant(code)).id)
  }

  async close(): Promise<void> {
    await this.api.signOut()
  }
}

test.describe('shell branding (SCR-SEC-010)', () => {
  test('TC-FE-XCUT-015 Shell shows the platform mark and the tenant logo: one /tenant/me at mount, [mark] · [logo] 28 px contain, the logo only through <img>', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    const { logoUrl } = await branding.withLogo(e2eTenant)
    const page = await freshPage(browser)
    try {
      const answers = recordTenantMe(page)
      const logoRequests: string[] = []
      page.on('request', (r) => {
        if (new URL(r.url()).pathname === logoUrl) logoRequests.push(r.resourceType())
      })
      await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
      const shell = new BrandingPage(page)

      await expect(shell.tenantLogo).toBeVisible()
      await expect.poll(() => answers.length).toBe(1)
      expect(answers[0].logoUrl).toBe(logoUrl)
      await expect(shell.mark).toBeVisible()
      await expect(shell.separator).toBeVisible()
      await expect(shell.tenantLogo).toHaveAttribute('src', logoUrl)
      await expect(shell.tenantLogo).toHaveAttribute('alt', 'E2E Tenant logo')
      await expect(shell.tenantLogo).toHaveCSS('object-fit', 'contain')
      expect((await shell.tenantLogo.boundingBox())?.height).toBe(28)
      await expect(shell.wordmark).toHaveCount(0)
      expect(await shell.tenantLogo.evaluate((element) => element.tagName)).toBe('IMG')
      expect(logoRequests.length).toBeGreaterThan(0)
      expect(logoRequests.every((type) => type === 'image')).toBe(true)

      await shell.openFromMenu('/security/users')
      await expect(page).toHaveURL(/\/security\/users$/)
      await expect(shell.tenantLogo).toBeVisible()
      expect(answers).toHaveLength(1)
    } finally {
      await page.context().close()
      await branding.clear(e2eTenant)
      await branding.close()
    }
  })

  test('TC-FE-XCUT-016 No logo or a broken logo → today\'s markup: mark + AVELYNQ word-mark, no separator, no toast', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    await branding.clear(PLATFORM)
    const admin = await storedAdminPage(browser)
    try {
      const answers = recordTenantMe(admin)
      await admin.goto('/dashboard')
      const shell = new BrandingPage(admin)
      await expect.poll(() => answers.length).toBe(1)
      expect(answers[0].logoUrl).toBeNull()
      await expect(shell.wordmark).toHaveText('AVELYNQ')
      await expect(shell.mark).toBeVisible()
      await expect(shell.tenantLogo).toHaveCount(0)
      await expect(shell.separator).toHaveCount(0)
      await expect(shell.toasts).toHaveCount(0)
    } finally {
      await admin.context().close()
    }

    const { logoUrl } = await branding.withLogo(e2eTenant)
    const page = await freshPage(browser)
    try {
      let routed = 0
      await page.route(`**${logoUrl}`, (route) => {
        routed += 1
        return route.fulfill({ status: 404, body: '' })
      })
      const answers = recordTenantMe(page)
      await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
      const shell = new BrandingPage(page)
      await expect.poll(() => answers.length).toBe(1)
      expect(answers[0].logoUrl).toBe(logoUrl)
      await expect(shell.wordmark).toHaveText('AVELYNQ')
      expect(routed).toBeGreaterThan(0)
      await expect(shell.tenantLogo).toHaveCount(0)
      await expect(shell.separator).toHaveCount(0)
      await expect(shell.mark).toBeVisible()
      await expect(shell.toasts).toHaveCount(0)
    } finally {
      await page.context().close()
      await branding.clear(e2eTenant)
      await branding.close()
    }
  })

  test('TC-FE-XCUT-017 Collapsed rail: the tenant logo alone (28 px) when present, else the mark alone; expanding restores the header', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    await branding.withLogo(e2eTenant)
    await branding.clear(PLATFORM)
    const page = await freshPage(browser)
    try {
      await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
      const shell = new BrandingPage(page)
      await expect(shell.tenantLogo).toBeVisible()
      await shell.toggleRail()
      await expect(shell.mark).toHaveCount(0)
      await expect(shell.tenantLogo).toBeVisible()
      expect((await shell.tenantLogo.boundingBox())?.height).toBe(28)
      await expect(shell.wordmark).toHaveCount(0)
      await expect(shell.separator).toHaveCount(0)
      await shell.toggleRail()
      await expect(shell.mark).toBeVisible()
      await expect(shell.separator).toBeVisible()
      await expect(shell.tenantLogo).toBeVisible()
    } finally {
      await page.context().close()
      await branding.clear(e2eTenant)
      await branding.close()
    }

    const admin = await storedAdminPage(browser)
    try {
      await admin.goto('/dashboard')
      const shell = new BrandingPage(admin)
      await expect(shell.wordmark).toBeVisible()
      await shell.toggleRail()
      await expect(shell.wordmark).toHaveCount(0)
      await expect(shell.mark).toBeVisible()
      await expect(shell.tenantLogo).toHaveCount(0)
      await shell.toggleRail()
      await expect(shell.wordmark).toHaveText('AVELYNQ')
    } finally {
      await admin.context().close()
    }
  })

  test('TC-FE-XCUT-018 Brand colour tints only the active menu entry; a PLATFORM session afterwards has no accent and the 1.2.0 teal', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    const tenant = await branding.tenant(e2eTenant)
    expect(await setBrandColor(branding.api, tenant, '#1a2b3c')).toBe(ACCENT)
    await branding.clear(PLATFORM)
    const page = await freshPage(browser)
    try {
      const answers = recordTenantMe(page)
      await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
      const shell = new BrandingPage(page)
      await expect.poll(() => shell.accent()).toBe(ACCENT)
      await expect.poll(() => answers.length).toBe(1)
      expect(answers[0].brandColor).toBe(ACCENT)
      const topbarBefore = await page.locator('header').first().evaluate((el) => getComputedStyle(el).backgroundColor)

      const link = await shell.openFromMenu('/security/users')
      await expect(page).toHaveURL(/\/security\/users$/)
      await expect(link).toHaveAttribute('aria-current', 'page')
      expect(await shell.activeMarkerColour(link)).toBe(ACCENT_RGB)

      await signOutViaUi(page)
      expect(await shell.accent()).toBe('')
      await signInViaUi(page, PLATFORM, USERS.admin)
      await expect(shell.wordmark).toBeVisible()
      expect(await shell.accent()).toBe('')
      const platformLink = await shell.openFromMenu('/security/users')
      await expect(platformLink).toHaveAttribute('aria-current', 'page')
      expect(await shell.activeMarkerColour(platformLink)).toBe(TEAL_RGB)
      expect(await page.locator('header').first().evaluate((el) => getComputedStyle(el).backgroundColor)).toBe(topbarBefore)
    } finally {
      await page.context().close()
      await branding.clear(e2eTenant)
      await branding.close()
    }
  })

  test('TC-FE-XCUT-022 Branding cache cleared on sign-out and tenant change: a new /tenant/me for PLATFORM, nothing of E2E_T2 shown afterwards', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    const { tenant, logoUrl } = await branding.withLogo(e2eTenant)
    await setBrandColor(branding.api, tenant, ACCENT)
    await branding.clear(PLATFORM)
    const page = await freshPage(browser)
    try {
      const answers = recordTenantMe(page)
      await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
      const shell = new BrandingPage(page)
      await expect(shell.tenantLogo).toHaveAttribute('src', logoUrl)
      await expect.poll(() => shell.accent()).toBe(ACCENT)

      await signOutViaUi(page)
      await expect(shell.tenantLogo).toHaveCount(0)
      expect(await shell.accent()).toBe('')

      const meCount = answers.length
      await shell.watchShellLogo()
      await signInViaUi(page, PLATFORM, USERS.admin)
      await expect(shell.wordmark).toHaveText('AVELYNQ')
      await expect.poll(() => answers.length).toBe(meCount + 1)
      expect(answers.at(-1)?.code).toBe(PLATFORM)
      expect(answers.at(-1)?.logoUrl).toBeNull()
      await expect(shell.tenantLogo).toHaveCount(0)
      expect(await shell.accent()).toBe('')
      expect(await shell.shellLogosSeen()).toEqual([])
    } finally {
      await page.context().close()
      await branding.clear(e2eTenant)
      await branding.close()
    }
  })
})

test.describe('sign-in page branding (SCR-SEC-001 … 003)', () => {
  test('TC-FE-XCUT-019 Sign-in pages show the tenant logo after the code settles: one anonymous request per code, never the previous code\'s logo, a code typed again served from the cache', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    const { logoUrl } = await branding.withLogo(e2eTenant)
    await branding.clear(PLATFORM)
    const page = await freshPage(browser)
    try {
      const sent = recordPublicBranding(page)
      const login = new LoginPage(page)
      await login.goto()
      const view = new BrandingPage(page)
      await expect.poll(() => sent.map((item) => item.code)).toEqual([PLATFORM])

      await login.tenant.clear()
      await login.tenant.pressSequentially('e2e_t2')
      await expect(view.authLogo).toHaveAttribute('src', logoUrl)
      expect(sent.map((item) => item.code)).toEqual([PLATFORM, e2eTenant])
      expect(sent.every((item) => item.authorization === undefined && item.tenant === undefined)).toBe(true)
      await expect(view.authLogo).toHaveAttribute('alt', 'E2E Tenant logo')
      expect((await view.authLogo.boundingBox())?.height).toBe(40)
      const heading = page.getByRole('heading', { level: 1 })
      expect((await view.authLogo.boundingBox())!.y).toBeLessThan((await heading.boundingBox())!.y)
      await expect(view.asideMark).toHaveAttribute('src', '/brand/avelynq-mark-dark.png')

      await login.tenant.clear()
      await login.tenant.pressSequentially('PLATFORM')
      await expect(view.authLogo).toHaveCount(0)
      expect(sent.map((item) => item.code)).toEqual([PLATFORM, e2eTenant])

      await page.getByRole('link', { name: 'Sign up' }).click()
      await expect(page).toHaveURL(/\/sign-up$/)
      const signupTenant = page.getByTestId('signup-tenant')
      await signupTenant.clear()
      await signupTenant.pressSequentially('e2e_t2')
      await expect(view.authLogo).toHaveAttribute('src', logoUrl)

      await page.getByRole('link', { name: 'Back to Sign in' }).click()
      await page.getByRole('link', { name: 'Forgot password?' }).click()
      await expect(page).toHaveURL(/\/password-reset$/)
      const resetTenant = page.getByTestId('reset-request-tenant')
      await resetTenant.clear()
      await resetTenant.pressSequentially('e2e_t2')
      await expect(view.authLogo).toHaveAttribute('src', logoUrl)
      expect(sent.map((item) => item.code)).toEqual([PLATFORM, e2eTenant])
    } finally {
      await page.context().close()
      await branding.clear(e2eTenant)
      await branding.close()
    }
  })

  test('TC-FE-XCUT-020 Unknown or suspended code → mark only, no toast, no inline message; the submit shows the 1.2.0 suspended message', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    const t9 = await branding.api.ensureT9Baseline()
    await branding.withLogo(T9.code)
    await suspendTenant(branding.api, t9)
    const page = await freshPage(browser)
    try {
      const statuses: Array<[string, number]> = []
      page.on('response', (response) => {
        const match = PUBLIC_BRANDING.exec(new URL(response.url()).pathname)
        if (match) statuses.push([match[1], response.status()])
      })
      const login = new LoginPage(page)
      await login.goto()
      const view = new BrandingPage(page)

      await login.tenant.fill('NOPE_X')
      await expect.poll(() => statuses).toContainEqual(['NOPE_X', 404])
      await login.tenant.fill(T9.code)
      await expect.poll(() => statuses).toContainEqual([T9.code, 403])
      await expect(view.authLogo).toHaveCount(0)
      await expect(view.toasts).toHaveCount(0)
      await expect(login.tenant).not.toHaveAttribute('aria-invalid', 'true')

      await login.username.fill(T9.admin)
      await login.password.fill(password)
      await login.submit.click()
      await expect(await login.tenantMessage()).toHaveText('This tenant is suspended. Contact your administrator.')
      await expect(view.authLogo).toHaveCount(0)
    } finally {
      await page.context().close()
      await activateTenant(branding.api, t9)
      await branding.api.ensureT9Baseline()
      await branding.close()
    }
  })

  test('TC-FE-XCUT-021 429 honours Retry-After: mark only, no toast, no request for any code before it, the next settled code after it is asked', async ({
    browser,
  }) => {
    const page = await freshPage(browser)
    try {
      await page.clock.install()
      const asked: string[] = []
      await page.route(PUBLIC_BRANDING, (route) => {
        asked.push(PUBLIC_BRANDING.exec(new URL(route.request().url()).pathname)?.[1] ?? '')
        return route.fulfill({
          status: 429,
          headers: { 'Retry-After': '30', 'Content-Type': 'application/json' },
          body: JSON.stringify({ success: false, error: { code: 'TENANT_BRANDING_RATE_LIMITED', message: 'Too many requests' } }),
        })
      })
      const login = new LoginPage(page)
      await login.goto()
      const view = new BrandingPage(page)
      await expect.poll(() => asked).toEqual([PLATFORM])
      await expect(view.authLogo).toHaveCount(0)
      await expect(view.toasts).toHaveCount(0)

      await login.tenant.fill(e2eTenant)
      await page.clock.runFor(1_000)
      await login.tenant.fill(T9.code)
      await page.clock.runFor(1_000)
      expect(asked).toEqual([PLATFORM])

      await page.clock.fastForward(31_000)
      await login.tenant.fill(e2eTenant)
      await page.clock.runFor(1_000)
      await expect.poll(() => asked).toEqual([PLATFORM, e2eTenant])
      await expect(view.authLogo).toHaveCount(0)
      await expect(view.toasts).toHaveCount(0)
    } finally {
      await page.context().close()
    }
  })
})

test.describe('session facts (SCR-SEC-012, any shell route)', () => {
  test('TC-FE-XCUT-023 Branding during a pending password change: /tenant/me answers and the tenant logo is on the page; no horizontal scroll at 360/390/412 px in en and ar', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    const { logoUrl } = await branding.withLogo(e2eTenant)
    const tenantAdmin = await apiSignIn(request, e2eTenant, USERS.e2eAdmin)
    const username = `e2e.brand.${RUN}`
    await createStaffUser(request, tenantAdmin, { username })
    const page = await freshPage(browser)
    try {
      const answers = recordTenantMe(page)
      const login = new LoginPage(page)
      await login.goto()
      await login.signIn(e2eTenant, username, password)
      await expect(page).toHaveURL(/\/account\/change-password$/, { timeout: 30_000 })
      const view = new BrandingPage(page)
      await expect(view.authLogo).toHaveAttribute('src', logoUrl)
      await expect.poll(() => answers.length).toBe(1)
      expect(answers[0].code).toBe(e2eTenant)
      await expect(page.getByTestId('account-forced-change')).toBeVisible()

      for (const lang of ['en', 'ar'] as const) {
        if (lang === 'ar') await page.getByRole('button', { name: 'العربية' }).click()
        for (const width of [360, 390, 412]) {
          await page.setViewportSize({ width, height: 800 })
          await expect(page.getByTestId('account-password-submit')).toBeVisible()
          expect(await view.hasHorizontalScroll(), `${lang} ${width}px`).toBe(false)
        }
      }
    } finally {
      await page.context().close()
      await request.post(`${backendUrl}/api/v1/sec/auth/logout`, { headers: bearer(tenantAdmin) })
      await branding.clear(e2eTenant)
      await branding.close()
    }
  })

  test('TC-FE-XCUT-024 TENANT_TOKEN_REVOKED ends the session with a message: token cleared, /login, one toast; the next sign-in works', async ({
    browser,
    request,
  }) => {
    const branding = await Branding.open(request)
    const t9 = await branding.api.ensureT9Baseline()
    const page = await freshPage(browser)
    try {
      const settled = page.waitForResponse((r) => new URL(r.url()).pathname === TENANT_ME && r.status() === 200)
      await signInViaUi(page, T9.code, T9.admin)
      await settled
      const shell = new BrandingPage(page)
      await expect(shell.wordmark).toBeVisible()
      await revokeTenantTokens(branding.api, t9)

      const refused = page.waitForResponse((r) => r.status() === 401)
      await shell.openFromMenu('/security/users')
      const answer = await refused
      expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('TENANT_TOKEN_REVOKED')
      await expect(page).toHaveURL(/\/login$/)
      await expect(page.getByText(REVOKED_MESSAGE)).toHaveCount(1)
      expect(await storedToken(page)).toBeNull()

      await signInViaUi(page, T9.code, T9.admin)
      expect(await page.evaluate((key) => window.localStorage.getItem(key), STORAGE_KEYS.tenant)).toBe(T9.code)
    } finally {
      await page.context().close()
      await branding.close()
    }
  })
})

test.describe('the sign-in pages on a phone (SCR-SEC-001 … 003, SCR-SEC-012)', () => {
  test('TC-FE-XCUT-025 at 360 px the brand aside is hidden on every sign-in page; the forced-change submit fits its button at 360 / 390 px in en and ar', async ({
    browser,
    request,
  }) => {
    const tenantAdmin = await apiSignIn(request, e2eTenant, USERS.e2eAdmin)
    const username = `e2e.phone.${RUN}`
    await createStaffUser(request, tenantAdmin, { username })
    const page = await freshPage(browser, { width: 360, height: 800 })
    const view = new BrandingPage(page)
    try {
      for (const lang of ['en', 'ar'] as const) {
        for (const path of ['/login', '/sign-up', '/password-reset']) {
          await page.goto(path)
          if (lang === 'ar') await page.getByRole('button', { name: 'العربية' }).click()
          await expect(page.locator('html')).toHaveAttribute('lang', lang)
          await expect(view.mobileBrand, `${lang} ${path}`).toBeVisible()
          await expect(view.brandAside, `${lang} ${path}`).toBeHidden()
          expect(await view.hasHorizontalScroll(), `${lang} ${path}`).toBe(false)
        }
      }

      const login = new LoginPage(page)
      await login.goto()
      await login.signIn(e2eTenant, username, password)
      await expect(page).toHaveURL(/\/account\/change-password$/, { timeout: 30_000 })
      const submit = page.getByTestId('account-password-submit')
      for (const lang of ['en', 'ar'] as const) {
        if (lang === 'ar') await page.getByRole('button', { name: 'العربية' }).click()
        await expect(submit).toHaveText(lang === 'en' ? 'Change password and continue' : 'تغيير كلمة المرور والمتابعة')
        for (const width of [360, 390]) {
          await page.setViewportSize({ width, height: 800 })
          await expect(view.brandAside, `${lang} ${width}px`).toBeHidden()
          expect(await view.fitsItsButton(submit), `${lang} ${width}px`).toBe(true)
          expect(await view.hasHorizontalScroll(), `${lang} ${width}px`).toBe(false)
        }
        await page.setViewportSize({ width: 360, height: 800 })
      }
    } finally {
      await page.context().close()
      await request.post(`${backendUrl}/api/v1/sec/auth/logout`, { headers: bearer(tenantAdmin) })
    }
  })
})
