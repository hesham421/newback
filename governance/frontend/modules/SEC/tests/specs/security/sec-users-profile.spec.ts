/** tm-F1 (erp-core 1.3.0, backend package D): users list avatar + optional columns, user profile fields, photo and set-password second levels, forced password change, my profile, topbar account menu — TC-FE-SEC-013 … 025. */
import { expect, test, type Browser, type Page, type Request } from '@playwright/test'
import { AccountPage } from '../../pom/AccountPage.ts'
import { backendUrl, hasPassword, MISSING_ENV_REASON, password, PLATFORM, STATE, USERS } from '../../pom/env.ts'
import { LoginPage } from '../../pom/LoginPage.ts'
import {
  adminToken,
  createStaffUser,
  expectTokenEnded,
  myUserPk,
  otherPassword,
  PHOTO_A,
  PHOTO_B,
  setUserPhoto,
  SVG,
} from '../../pom/secApi.ts'
import { apiSignIn, bearer, signInViaUi, storedToken, useLanguage } from '../../pom/session.ts'
import { ShellPage } from '../../pom/ShellPage.ts'
import { UsersPage } from '../../pom/UsersPage.ts'

const RUN = Date.now()
const FORCED_ROUTE = /\/account\/change-password$/
const POLICY_REFUSED = 'abcdefgh'

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

/** A fresh browser context in English with no stored session. */
async function freshPage(browser: Browser): Promise<Page> {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  await inEnglish(page)
  return page
}

/* `useLanguage` for pages this spec opens itself (a hook-like name is reserved for hooks by the linter). */
async function inEnglish(page: Page): Promise<void> {
  await page.addInitScript((value) => window.localStorage.setItem('avelynq_lang', value), 'en')
}

/** A UI sign-in that must land on the forced change page (signInViaUi expects /dashboard). */
async function signInToForcedChange(page: Page, username: string): Promise<void> {
  const login = new LoginPage(page)
  await login.goto()
  const answered = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/login'))
  await login.signIn(PLATFORM, username, password)
  const answer = await answered
  expect(answer.status()).toBe(200)
  expect(((await answer.json()) as { data: { passwordChangeRequired: boolean } }).data.passwordChangeRequired).toBe(true)
  await expect(page).toHaveURL(FORCED_ROUTE, { timeout: 30_000 })
}

/** Records the bodies of the live `GET /sec/me` answers (passed through untouched) — read before any redirect navigates away. */
async function recordMyProfileReads(page: Page): Promise<Array<{ username?: string; preferredLocale?: string | null }>> {
  const reads: Array<{ username?: string; preferredLocale?: string | null }> = []
  await page.route('**/api/v1/sec/me', async (route) => {
    if (route.request().method() !== 'GET') return route.fallback()
    const response = await route.fetch()
    reads.push(((await response.json()) as { data: { username?: string; preferredLocale?: string | null } }).data)
    await route.fulfill({ response })
  })
  return reads
}

function isMenuRequest(request: Request): boolean {
  return request.url().endsWith('/api/v1/sec/menu')
}

test.describe('as PLATFORM admin (stored session)', () => {
  test.use({ storageState: STATE.admin })

  test('TC-FE-SEC-013 Users list: the row avatar is the photo (or initials); phone and job title are hidden until chosen in the Columns menu, kept for the session, not in the URL', async ({
    page,
    request,
  }) => {
    const token = await adminToken(request)
    const withPhoto = `e2e.av.${RUN}`
    const withoutPhoto = `e2e.np.${RUN}`
    const pk = await createStaffUser(request, token, {
      username: withPhoto,
      fullNameEn: `Avatar ${RUN}`,
      phone: '+966 50 111 2222',
      jobTitleAr: 'محاسب',
      jobTitleEn: 'Accountant',
      requireChangeAtNextLogin: false,
    })
    await createStaffUser(request, token, {
      username: withoutPhoto,
      fullNameEn: `Avatar ${RUN} Initials`,
      requireChangeAtNextLogin: false,
    })
    const photoUrl = await setUserPhoto(request, token, pk, PHOTO_A)

    const users = new UsersPage(page)
    await users.search(`Avatar ${RUN}`)
    await expect(users.row(withPhoto).getByTestId('user-row-avatar').locator('img')).toHaveAttribute('src', photoUrl)
    await expect(users.row(withoutPhoto).getByTestId('user-row-avatar').locator('img')).toHaveCount(0)
    await expect(users.row(withoutPhoto).getByTestId('user-row-avatar')).toHaveText(/^\S{1,2}$/)
    await expect(page.getByRole('columnheader', { name: 'Phone' })).toHaveCount(0)
    await expect(page.getByRole('columnheader', { name: 'Job title' })).toHaveCount(0)

    await users.columnsMenu.click()
    await page.getByTestId('users-column-phone').click()
    await page.getByTestId('users-column-jobTitle').click()
    await page.keyboard.press('Escape')
    await expect(page.getByRole('columnheader', { name: 'Phone' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: 'Job title' })).toBeVisible()
    await expect(users.row(withPhoto)).toContainText('+966 50 111 2222')
    await expect(users.row(withPhoto)).toContainText('Accountant')

    await page.reload()
    await expect(page.getByRole('columnheader', { name: 'Phone' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: 'Job title' })).toBeVisible()
    expect(page.url()).not.toMatch(/phone|jobTitle|column/i)
  })

  test('TC-FE-SEC-014 Create a user with phone, job titles and preferred language; "require a password change" is on by default; the edit always sends the four fields', async ({
    page,
  }) => {
    const username = `e2e.new.${RUN}`
    await page.goto('/security/users/new')
    await page.locator('#user-create-username').fill(username)
    await page.locator('#user-create-email').fill(`${username}@example.test`)
    await page.locator('#user-create-full-name-ar').fill('مستخدم جديد')
    await page.locator('#user-create-full-name-en').fill(`New ${RUN}`)
    await page.locator('#user-create-password').fill(password)
    await page.getByTestId('user-phone').fill('+966 50 123 4567')
    await page.getByTestId('user-jobTitleAr').fill('مدير')
    await page.getByTestId('user-jobTitleEn').fill('Manager')
    await page.getByTestId('user-preferredLocale').selectOption('ar')
    await expect(page.getByTestId('user-require-change')).toBeChecked()

    const created = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/api/v1/sec/users'))
    await page.getByRole('button', { name: 'Create Account' }).click()
    const answer = await created
    expect(answer.status()).toBe(201)
    expect(answer.request().postDataJSON()).toMatchObject({
      phone: '+966 50 123 4567',
      jobTitleAr: 'مدير',
      jobTitleEn: 'Manager',
      preferredLocale: 'ar',
      requireChangeAtNextLogin: true,
    })
    await expect(page.getByText('User account created successfully.')).toBeVisible()
    const userPk = ((await answer.json()) as { data: { userPk: number } }).data.userPk

    await page.goto(`/security/users/${userPk}`)
    const users = new UsersPage(page)
    await expect(users.viewRow('user-view-phone')).toContainText('+966 50 123 4567')
    await expect(users.viewRow('user-view-job-title-en')).toContainText('Manager')
    await expect(users.viewRow('user-view-preferred-locale')).toContainText('العربية')
    await expect(users.viewRow('user-view-password-change-required')).toContainText('Yes')
    await expect(users.viewRow('user-view-password-changed-at')).not.toContainText('—')

    await page.goto(`/security/users/${userPk}/edit`)
    await expect(page.getByTestId('user-phone')).toHaveValue('+966 50 123 4567')
    await page.getByTestId('user-phone').fill('')
    const updated = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith(`/api/v1/sec/users/${userPk}`))
    await page.getByRole('button', { name: 'Save Account' }).click()
    const put = await updated
    expect(put.ok()).toBe(true)
    expect(put.request().postDataJSON()).toMatchObject({ phone: '', jobTitleAr: 'مدير', jobTitleEn: 'Manager', preferredLocale: 'ar' })
    await page.goto(`/security/users/${userPk}`)
    await expect(users.viewRow('user-view-phone')).toContainText('—')
  })

  test('TC-FE-SEC-015 An administrator sets a password: second-level Drawer, final ConfirmDialog, toast with the ended sessions, change pending', async ({
    page,
    request,
  }) => {
    const username = `e2e.pw.${RUN}`
    const userPk = await createStaffUser(request, await adminToken(request), { username, requireChangeAtNextLogin: false })
    const victim = await apiSignIn(request, PLATFORM, username)
    const users = new UsersPage(page)
    const sent: string[] = []
    page.on('request', (r) => {
      if (r.method() === 'PUT' && r.url().endsWith(`/api/v1/sec/users/${userPk}/password`)) sent.push(r.url())
    })

    await page.goto(`/security/users/${userPk}`)
    await users.setPassword.click()
    await expect(page).toHaveURL(new RegExp(`passwordFor=${userPk}`))
    await expect(users.passwordDrawer).toBeVisible()
    await expect(page.getByRole('dialog')).toHaveCount(2)

    const next = otherPassword('15')
    await users.passwordNew.fill(next)
    await users.passwordConfirm.fill(next)
    await expect(users.passwordRequireChange).toBeChecked()
    await users.passwordSubmit.click()
    await expect(users.setPasswordConfirm).toBeVisible()
    await expect(users.setPasswordConfirm).toContainText(username)
    await expect(users.setPasswordConfirm).toContainText('every open session')
    expect(sent).toHaveLength(0)

    await users.confirmDialogCancel.click()
    await expect(users.setPasswordConfirm).toHaveCount(0)
    expect(sent).toHaveLength(0)
    await expect(users.passwordNew).toHaveValue(next)

    await users.passwordSubmit.click()
    const answered = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith(`/api/v1/sec/users/${userPk}/password`))
    await users.confirmDialogConfirm.click()
    const answer = await answered
    expect(answer.status()).toBe(200)
    expect(answer.request().postDataJSON()).toEqual({ newPassword: next, requireChangeAtNextLogin: true })
    await expect(page.getByText('Password set. 1 open session(s) ended.')).toBeVisible()
    await expect(page).not.toHaveURL(/passwordFor=/)
    await expect(users.passwordDrawer).toHaveCount(0)
    await expect(users.viewRow('user-view-password-change-required')).toContainText('Yes')
    await expectTokenEnded(request, victim)
  })

  test('TC-FE-SEC-016 Set password is hidden on the own row: the hint links to my password change and no request is ever sent', async ({
    page,
    request,
  }) => {
    await page.goto('/dashboard')
    const ownPk = await myUserPk(request, (await storedToken(page)) ?? '')
    const users = new UsersPage(page)
    const sent: string[] = []
    page.on('request', (r) => {
      if (r.method() === 'PUT' && /\/api\/v1\/sec\/users\/\d+\/password$/.test(r.url())) sent.push(r.url())
    })

    await page.goto(`/security/users/${ownPk}`)
    await expect(users.ownHint).toBeVisible()
    await expect(users.ownHint.getByRole('link')).toHaveAttribute('href', '/account/profile?changePassword=1')
    await expect(users.setPassword).toHaveCount(0)

    await page.goto(`/security/users/${ownPk}?passwordFor=${ownPk}`)
    await expect(users.ownHint).toBeVisible()
    await expect(page).not.toHaveURL(/passwordFor=/)
    await expect(users.passwordDrawer).toHaveCount(0)
    expect(sent).toHaveLength(0)
  })

  test('TC-FE-SEC-017 The password policy is the server\'s: SEC-400-PASSWORD-POLICY inline with its message; 72 UTF-8 bytes counted; no 200 cap', async ({
    page,
    request,
  }) => {
    const username = `e2e.pol.${RUN}`
    const userPk = await createStaffUser(request, await adminToken(request), { username, requireChangeAtNextLogin: false })
    const users = new UsersPage(page)
    await page.goto(`/security/users/${userPk}?passwordFor=${userPk}`)
    await expect(users.passwordDrawer).toBeVisible()

    for (const refused of [POLICY_REFUSED, `${'ب'.repeat(37)}1`]) {
      await users.passwordNew.fill(refused)
      await users.passwordConfirm.fill(refused)
      await users.passwordSubmit.click()
      const answered = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith(`/api/v1/sec/users/${userPk}/password`))
      await users.confirmDialogConfirm.click()
      const answer = await answered
      expect(answer.status()).toBe(400)
      const body = (await answer.json()) as { error: { code: string; fieldErrors: Array<{ field: string; message: string }> } }
      expect(body.error.code).toBe('SEC-400-PASSWORD-POLICY')
      await expect(await users.messageOf(users.passwordNew)).toHaveText(body.error.fieldErrors[0].message)
      await expect(users.passwordDrawer).toBeVisible()
    }

    await users.passwordNew.fill('ب'.repeat(36))
    await expect(users.passwordDrawer.getByTestId('password-bytes')).toHaveText('72 / 72 bytes')
    expect(await users.passwordNew.getAttribute('maxlength')).toBeNull()
  })

  test('TC-FE-SEC-023 Another user\'s photo through ?photoFor=: a second Drawer above the user drawer; upload, list avatar, remove behind a ConfirmDialog', async ({
    page,
    request,
  }) => {
    const username = `e2e.ph2.${RUN}`
    const userPk = await createStaffUser(request, await adminToken(request), {
      username,
      fullNameEn: `Photo ${RUN}`,
      requireChangeAtNextLogin: false,
    })
    const users = new UsersPage(page)

    await page.goto(`/security/users/${userPk}`)
    await expect(users.photoRow).toBeVisible()
    await users.photoChange.click()
    await expect(page).toHaveURL(new RegExp(`photoFor=${userPk}`))
    await expect(users.photoDrawer).toBeVisible()
    await expect(page.getByRole('dialog')).toHaveCount(2)
    await page.goBack()
    await expect(page).not.toHaveURL(/photoFor=/)
    await expect(users.photoDrawer).toHaveCount(0)
    await expect(users.photoRow).toBeVisible()

    await users.photoChange.click()
    await users.photoFile.setInputFiles({ name: 'face.png', mimeType: 'image/png', buffer: PHOTO_A })
    await expect(page.getByTestId('photo-preview')).toBeVisible()
    const saved = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith(`/api/v1/sec/users/${userPk}/photo`))
    await users.photoSave.click()
    const answer = await saved
    expect(answer.status()).toBe(200)
    const photoUrl = ((await answer.json()) as { data: { photoUrl: string } }).data.photoUrl
    await expect(page.getByText('The photo has been saved.')).toBeVisible()
    await expect(users.photoRow.locator('img')).toHaveAttribute('src', photoUrl)

    await page.goto('/security/users')
    await users.search(`Photo ${RUN}`)
    await expect(users.row(username).getByTestId('user-row-avatar').locator('img')).toHaveAttribute('src', photoUrl)

    await page.goto(`/security/users/${userPk}?photoFor=${userPk}`)
    await expect(users.photoDrawer).toBeVisible()
    await users.photoRemove.click()
    const removed = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/users/${userPk}/photo`))
    await users.confirmDialogConfirm.click()
    expect((await removed).status()).toBe(204)
    await expect(page.getByText('The photo has been removed.')).toBeVisible()
    await expect(users.photoRow.locator('img')).toHaveCount(0)
  })
})

test('TC-FE-SEC-018 Forced password change at sign-in: own page without the shell or a menu read, every route redirects, the same token keeps working afterwards', async ({
  browser,
  request,
}) => {
  const username = `e2e.fc.${RUN}`
  await createStaffUser(request, await adminToken(request), { username })
  const page = await freshPage(browser)
  const menuCalls: string[] = []
  page.on('request', (r) => {
    if (isMenuRequest(r)) menuCalls.push(r.headers().authorization ?? '')
  })

  await signInToForcedChange(page, username)
  const account = new AccountPage(page)
  await expect(account.forced).toBeVisible()
  await expect(account.forcedSignOut).toBeVisible()
  await expect(new ShellPage(page).signOut).toHaveCount(0)
  expect(menuCalls).toHaveLength(0)
  const token = await storedToken(page)

  await page.goto('/dashboard')
  await expect(page).toHaveURL(FORCED_ROUTE)
  await page.goto('/security/users')
  await expect(page).toHaveURL(FORCED_ROUTE)

  const loginCalls: string[] = []
  page.on('request', (r) => {
    if (r.url().endsWith('/api/v1/sec/auth/login')) loginCalls.push(r.url())
  })
  const next = otherPassword('18')
  await account.fillPasswords(password, next)
  const changed = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/api/v1/sec/me/password'))
  const menuAfter = page.waitForResponse((r) => isMenuRequest(r.request()) && r.status() === 200)
  await account.submit.click()
  const answer = await changed
  expect(answer.status()).toBe(200)
  expect(((await answer.json()) as { data: { passwordChangeRequired: boolean } }).data.passwordChangeRequired).toBe(false)
  await expect(page.getByText('Your password has been changed. 0 other session(s) ended.')).toBeVisible()
  await expect(page).toHaveURL(/\/dashboard$/)
  const menu = await menuAfter
  expect(menu.request().headers().authorization).toBe(`Bearer ${token}`)
  expect(loginCalls).toHaveLength(0)
  await expect(new ShellPage(page).signOut).toBeVisible()
  await expect(page.getByText('Navigation could not be loaded')).toHaveCount(0)
  await page.context().close()
})

test('TC-FE-SEC-019 403 SEC-403-PASSWORD-CHANGE-REQUIRED mid-session routes to the change page: (1) live after a reload and in a second tab, (2) routed on a normal session — no toast, the token kept', async ({
  browser,
  request,
}) => {
  const username = `e2e.pend.${RUN}`
  await createStaffUser(request, await adminToken(request), { username })
  const page = await freshPage(browser)
  await signInToForcedChange(page, username)
  const token = await storedToken(page)

  /* The live answers pass through untouched; their bodies are read before the redirect navigates away. */
  const facts: string[] = []
  for (const path of ['/api/v1/sec/menu', '/api/v1/sec/me']) {
    await page.route(`**${path}`, async (route) => {
      const response = await route.fetch()
      const body = (await response.json()) as { error?: { code: string }; data?: { passwordChangeRequired?: boolean } }
      facts.push(body.error?.code ?? `passwordChangeRequired=${String(body.data?.passwordChangeRequired)}`)
      await route.fulfill({ response })
    })
  }
  await page.goto('/security/users')
  await expect(page).toHaveURL(FORCED_ROUTE)
  await expect
    .poll(() => facts.some((fact) => fact === 'SEC-403-PASSWORD-CHANGE-REQUIRED' || fact === 'passwordChangeRequired=true'))
    .toBe(true)

  const second = await page.context().newPage()
  await second.goto('/dashboard')
  await expect(second).toHaveURL(FORCED_ROUTE)
  for (const tab of [page, second]) {
    await expect(new ShellPage(tab).toasts).toHaveCount(0)
    expect(await storedToken(tab)).toBe(token)
  }
  await page.context().close()

  /* (2) A normal stored session whose menu answers the code (page.route): the transport handler alone routes it. */
  const adminContext = await browser.newContext({ storageState: STATE.admin, viewport: { width: 1440, height: 900 } })
  const admin = await adminContext.newPage()
  await inEnglish(admin)
  await admin.route('**/api/v1/sec/menu', (route) =>
    route.fulfill({
      status: 403,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, error: { code: 'SEC-403-PASSWORD-CHANGE-REQUIRED', message: 'x' } }),
    })
  )
  await admin.goto('/security/users')
  await expect(admin).toHaveURL(FORCED_ROUTE)
  await expect(new AccountPage(admin).forced).toBeVisible()
  await expect(new ShellPage(admin).toasts).toHaveCount(0)
  expect(await storedToken(admin)).toBeTruthy()
  await adminContext.close()
})

test.describe('as a signed-in staff user without roles', () => {
  test('TC-FE-SEC-020 My profile: reached from the account menu with no page code; PATCH /me sends only the changed fields; a malformed phone is refused inline before any request (SEC-U124)', async ({
    browser,
    request,
  }) => {
    const username = `e2e.me.${RUN}`
    await createStaffUser(request, await adminToken(request), { username, requireChangeAtNextLogin: false })
    const page = await freshPage(browser)
    await signInViaUi(page, PLATFORM, username)
    const account = new AccountPage(page)

    await account.menuTrigger.click()
    const read = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/me') && r.request().method() === 'GET')
    await account.menuProfile.click()
    expect((await read).status()).toBe(200)
    await expect(page).toHaveURL(/\/account\/profile$/)
    await expect(account.profile).toBeVisible()
    await expect(page.getByTestId('account-username')).toContainText(username)
    await expect(page.getByTestId('account-email')).toContainText(`${username}@example.test`)
    await expect(page.getByTestId('account-admin-only-hint')).toBeVisible()
    await expect(page.getByTestId('account-tenant')).toContainText('PLATFORM')
    await expect(page.getByText(/Required screen/)).toHaveCount(0)
    await expect(page.getByText(/role/i)).toHaveCount(0)

    await account.edit.click()
    await expect(page).toHaveURL(/editId=me/)
    await account.editField('phone').fill('+966 50 765 4321')
    await account.editField('jobTitleEn').fill('Analyst')
    const patched = page.waitForResponse((r) => r.request().method() === 'PATCH' && r.url().endsWith('/api/v1/sec/me'))
    await page.getByTestId('account-edit-submit').click()
    const answer = await patched
    expect(answer.status()).toBe(200)
    expect(answer.request().postDataJSON()).toEqual({ phone: '+966 50 765 4321', jobTitleEn: 'Analyst' })
    await expect(page.getByText('Your profile has been saved.')).toBeVisible()
    await expect(page).not.toHaveURL(/editId=/)
    await expect(page.getByTestId('account-view-phone')).toContainText('+966 50 765 4321')

    await account.edit.click()
    const patches: string[] = []
    page.on('request', (r) => {
      if (r.method() === 'PATCH' && r.url().endsWith('/api/v1/sec/me')) patches.push(r.url())
    })
    await account.editField('phone').fill('12')
    await page.getByTestId('account-edit-submit').click()
    await expect(account.editField('phone')).toHaveAttribute('aria-invalid', 'true')
    await expect(await account.messageOf(account.editField('phone'))).toHaveText('Optional. Digits, spaces and hyphens, with an optional leading +.')
    await expect(page).toHaveURL(/editId=me/)
    expect(patches).toHaveLength(0)
    await page.context().close()
  })

  test('TC-FE-SEC-021 Preferred language: saved Arabic switches at once; the topbar toggle stays local; the next sign-in applies it', async ({
    browser,
    request,
  }) => {
    const username = `e2e.loc.${RUN}`
    await createStaffUser(request, await adminToken(request), { username, requireChangeAtNextLogin: false })
    const page = await freshPage(browser)
    await signInViaUi(page, PLATFORM, username)
    const account = new AccountPage(page)
    const patches: string[] = []
    page.on('request', (r) => {
      if (r.method() === 'PATCH' && r.url().endsWith('/api/v1/sec/me')) patches.push(r.postData() ?? '')
    })

    await page.goto('/account/profile?editId=me')
    await account.editField('preferredLocale').selectOption('ar')
    const patched = page.waitForResponse((r) => r.request().method() === 'PATCH' && r.url().endsWith('/api/v1/sec/me'))
    await page.getByTestId('account-edit-submit').click()
    const answer = await patched
    expect(answer.status()).toBe(200)
    expect(answer.request().postDataJSON()).toEqual({ preferredLocale: 'ar' })
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl')
    await expect(page.locator('html')).toHaveAttribute('lang', 'ar')
    await expect(page.getByRole('heading', { level: 1, name: 'ملفي الشخصي' })).toBeVisible()

    await page.getByTestId('topbar-language').click()
    await expect(page.locator('html')).toHaveAttribute('lang', 'en')
    expect(patches).toHaveLength(1)

    const loggedOut = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/logout'))
    await page.getByTestId('topbar-sign-out').click()
    await loggedOut
    await expect(page).toHaveURL(/\/login$/)
    const reads = await recordMyProfileReads(page)
    await signInViaUi(page, PLATFORM, username)
    await expect.poll(() => reads.some((read) => read.preferredLocale === 'ar')).toBe(true)
    await expect(page.locator('html')).toHaveAttribute('lang', 'ar')
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl')
    expect(patches).toHaveLength(1)
    await page.context().close()
  })

  test('TC-FE-SEC-022 My photo: upload, replace (a new URL each time), an SVG refused inline, remove behind a ConfirmDialog — the topbar avatar follows', async ({
    browser,
    request,
  }) => {
    const username = `e2e.ph1.${RUN}`
    await createStaffUser(request, await adminToken(request), { username, requireChangeAtNextLogin: false })
    const page = await freshPage(browser)
    await signInViaUi(page, PLATFORM, username)
    const account = new AccountPage(page)
    const users = new UsersPage(page)
    const topbarImg = account.menuTrigger.locator('img')

    await page.goto('/account/profile')
    await account.photoChange.click()
    await expect(page).toHaveURL(/photo=1/)
    const urls: string[] = []
    for (const [name, buffer] of [
      ['a.png', PHOTO_A],
      ['b.png', PHOTO_B],
    ] as const) {
      await users.photoFile.setInputFiles({ name, mimeType: 'image/png', buffer })
      const saved = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/api/v1/sec/me/photo'))
      await users.photoSave.click()
      const answer = await saved
      expect(answer.status()).toBe(200)
      expect(answer.request().headers()['content-type']).toContain('multipart/form-data')
      const photoUrl = ((await answer.json()) as { data: { photoUrl: string } }).data.photoUrl
      urls.push(photoUrl)
      await expect(topbarImg).toHaveAttribute('src', photoUrl)
    }
    expect(urls[0]).not.toBe(urls[1])
    await expect(page.getByText('The photo has been saved.').first()).toBeVisible()

    await users.photoFile.setInputFiles({ name: 'logo.svg', mimeType: 'image/svg+xml', buffer: SVG })
    const refusedAnswer = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/api/v1/sec/me/photo'))
    await users.photoSave.click()
    const refused = await refusedAnswer
    expect(refused.status()).toBe(400)
    const body = (await refused.json()) as { error: { code: string; message: string; fieldErrors?: Array<{ message: string }> } }
    expect(body.error.code).toBe('SEC-400-PHOTO-INVALID')
    await expect(await users.messageOf(users.photoFile)).toHaveText(body.error.fieldErrors?.[0]?.message ?? body.error.message)
    await expect(topbarImg).toHaveAttribute('src', urls[1])

    await users.photoRemove.click()
    await expect(page.getByText('Remove this photo?')).toBeVisible()
    const removed = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith('/api/v1/sec/me/photo'))
    await users.confirmDialogConfirm.click()
    expect((await removed).status()).toBe(204)
    await expect(page.getByText('The photo has been removed.')).toBeVisible()
    await expect(topbarImg).toHaveCount(0)
    await expect(page.getByTestId('account-avatar').locator('img')).toHaveCount(0)
    await page.context().close()
  })

  test('TC-FE-SEC-024 Change my password from the profile: a wrong current password stays inline (no sign-out); success ends my other session only', async ({
    browser,
    request,
  }) => {
    const username = `e2e.cp.${RUN}`
    await createStaffUser(request, await adminToken(request), { username, requireChangeAtNextLogin: false })
    const page = await freshPage(browser)
    await signInViaUi(page, PLATFORM, username)
    const other = await apiSignIn(request, PLATFORM, username)
    const account = new AccountPage(page)

    await account.menuTrigger.click()
    await account.menuPassword.click()
    await expect(page).toHaveURL(/\/account\/profile\?changePassword=1$/)
    await expect(page.getByTestId('account-password-drawer')).toBeVisible()

    const next = otherPassword('24')
    await account.fillPasswords(`${password}-wrong`, next)
    const wrong = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/api/v1/sec/me/password'))
    await account.submit.click()
    const refusal = await wrong
    expect(refusal.status()).toBe(403)
    expect(((await refusal.json()) as { error: { code: string } }).error.code).toBe('SEC-403-PASSWORD-CURRENT-INVALID')
    await expect(await account.messageOf(account.current)).toHaveText('The current password is incorrect.')
    await expect(page).toHaveURL(/\/account\/profile/)

    await account.fillPasswords(password, next)
    const changed = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/api/v1/sec/me/password'))
    await account.submit.click()
    expect((await changed).status()).toBe(200)
    await expect(page.getByText('Your password has been changed. 1 other session(s) ended.')).toBeVisible()
    await expect(page).toHaveURL(/\/account\/profile$/)
    await expect(account.profile).toBeVisible()
    await expectTokenEnded(request, other)
    const stillMine = await page.request.get(`${backendUrl}/api/v1/sec/menu`, { headers: bearer((await storedToken(page)) ?? '') })
    expect(stillMine.status()).toBe(200)
    await page.context().close()
  })

  test('TC-FE-SEC-025 Topbar account menu from [\'me\']: name, username, two items, sign-out apart; a new identity after sign-out never sees the previous one', async ({
    browser,
    request,
  }) => {
    const username = `e2e.mn.${RUN}`
    await createStaffUser(request, await adminToken(request), {
      username,
      fullNameEn: `Menu ${RUN}`,
      requireChangeAtNextLogin: false,
    })
    const page = await freshPage(browser)
    await signInViaUi(page, PLATFORM, USERS.admin)
    const account = new AccountPage(page)
    const shell = new ShellPage(page)

    await account.menuTrigger.click()
    await expect(account.menu).toBeVisible()
    await expect(page.getByTestId('topbar-account-name')).toHaveText('System Administrator')
    await expect(page.getByTestId('topbar-account-username')).toHaveText(USERS.admin)
    await expect(account.menuProfile).toBeVisible()
    await expect(account.menuPassword).toBeVisible()
    await expect(shell.signOut).toBeVisible()
    await page.keyboard.press('Escape')

    const loggedOut = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/logout'))
    await shell.signOut.click()
    await loggedOut
    await expect(page).toHaveURL(/\/login$/)

    const reads = await recordMyProfileReads(page)
    await signInViaUi(page, PLATFORM, username)
    await expect.poll(() => reads.some((read) => read.username === username)).toBe(true)
    expect(reads.every((read) => read.username === username)).toBe(true)
    await account.menuTrigger.click()
    await expect(page.getByTestId('topbar-account-name')).toHaveText(`Menu ${RUN}`)
    await expect(page.getByTestId('topbar-account-username')).toHaveText(username)
    await expect(account.menu).not.toContainText('System Administrator')
    await page.context().close()
  })
})
