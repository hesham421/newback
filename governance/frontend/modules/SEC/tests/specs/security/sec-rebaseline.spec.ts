/** SEC on erp-core 1.2.0 (steps 04, 12, v3-07): realm badges, by-id 404s, super role, grant tree, audit export, sessions, tenant confinement, sign-ups, dashboard, server header sort, toolbar export, DateField filters. */
import { readFile } from 'node:fs/promises'
import { expect, test } from '@playwright/test'
import { backendUrl, CATALOG_MODULES, e2eTenant, hasPassword, MISSING_ENV_REASON, PLATFORM, STATE, USERS } from '../../pom/env.ts'
import { ShellPage } from '../../pom/ShellPage.ts'
import { apiSignIn, bearer, signInViaUi, storedToken, useLanguage } from '../../pom/session.ts'

const REPORT_SCREENS = ['SEC_REPORTS', 'NOTIF_REPORTS', 'AUDIT_REPORTS']
const USER_FILTER_FIELDS = ['username', 'email', 'fullNameAr', 'fullNameEn', 'statusCode', 'fullName']
const ITEM_UNAVAILABLE = 'This item is unavailable or has been deactivated. Refresh the list and try again.'

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test.describe('as PLATFORM admin (stored session)', () => {
  test.use({ storageState: STATE.admin })

  test('TC-FE-SEC-001 Users: every listed account carries a STAFF realm badge; the search sends no realm filter', async ({ page }) => {
    const search = page.waitForRequest((r) => r.url().endsWith('/api/v1/sec/users/search'))
    await page.goto('/security/users')
    const body = (await search).postDataJSON() as { filters?: Array<{ field: string }> }
    for (const filter of body.filters ?? []) expect(USER_FILTER_FIELDS).toContain(filter.field)
    const badges = page.getByTestId('user-realm-badge')
    await expect(badges.first()).toBeVisible()
    const count = await badges.count()
    expect(count).toBeGreaterThan(0)
    for (let index = 0; index < count; index += 1) {
      await expect(badges.nth(index)).toHaveAttribute('data-realm', 'STAFF')
      await expect(badges.nth(index)).toHaveText('Staff')
    }
  })

  test('TC-FE-SEC-002 Users: a deep link to a customer account id (404 SEC-404-USER) closes back to the list with one toast', async ({ page }) => {
    await page.route('**/api/v1/sec/users/424242', (route) =>
      route.fulfill({
        status: 404,
        json: { success: false, error: { code: 'SEC-404-USER', message: 'User not found', fieldErrors: null } },
      })
    )
    await page.goto('/security/users/424242')
    await expect(page).toHaveURL(/\/security\/users$/)
    const toasts = new ShellPage(page).toasts
    await expect(toasts).toContainText(ITEM_UNAVAILABLE)
    await expect(toasts.getByText('This item is unavailable', { exact: false })).toHaveCount(1)
  })

  test('TC-FE-SEC-003 Roles: SYS_ADMIN carries the read-only super-role badge (isSuper)', async ({ page }) => {
    const search = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/roles/search'))
    await page.goto('/security/roles')
    const answer = await search
    const sent = answer.request().postDataJSON() as { filters?: Array<{ field: string }>; sortField?: string }
    for (const filter of sent.filters ?? []) expect(['code', 'nameAr', 'nameEn', 'isActiveFl', 'name']).toContain(filter.field)
    if (sent.sortField) expect(['code', 'nameAr', 'nameEn', 'isActiveFl']).toContain(sent.sortField)
    const roles = ((await answer.json()) as { data: { content: Array<{ code: string; isSuper?: boolean }> } }).data.content
    expect(roles.find((role) => role.code === 'SYS_ADMIN')?.isSuper).toBe(true)
    const sysAdmin = page.getByRole('button', { name: /SYS_ADMIN/ })
    await expect(sysAdmin.getByTestId('role-super-badge')).toHaveText('Super role')
    const e2eAll = page.getByRole('button', { name: /E2E_ALL/ })
    await expect(e2eAll).toBeVisible()
    await expect(e2eAll.getByTestId('role-super-badge')).toHaveCount(0)
  })

  test('TC-FE-SEC-004 Roles: the grant tree lists the 8 catalog modules incl. PLATFORM/AUDIT/SEQUENCE and the *_REPORTS screens, read at size 200', async ({ page }) => {
    const registryPages: number[] = []
    const registryFields: string[] = []
    page.on('request', (request) => {
      if (request.url().endsWith('/api/v1/sec/registry/search')) {
        const body = request.postDataJSON() as { size: number; filters?: Array<{ field: string }> }
        registryPages.push(body.size)
        registryFields.push(...(body.filters ?? []).map((filter) => filter.field))
      }
    })
    await page.goto('/security/roles')
    await page.getByRole('button', { name: /SYS_ADMIN/ }).click()
    await expect(page).toHaveURL(/editId=\d+/)
    await page.getByRole('button', { name: 'Expand all' }).click()
    for (const code of CATALOG_MODULES) {
      await expect(page.getByTestId(`role-grant-module-${code}`)).toBeVisible()
    }
    await expect(page.getByTestId('role-grant-module-FIN')).toHaveCount(0)
    for (const pageCode of REPORT_SCREENS) {
      await expect(page.getByTestId(`role-grant-screen-${pageCode}`)).toBeVisible()
    }
    expect(registryPages.length).toBeGreaterThan(0)
    expect(registryPages.every((size) => size === 200)).toBe(true)
    for (const field of registryFields) expect(['code', 'pageCode']).toContain(field)

    /* Unsaved chips name their authority opaquely, in both formats (D1); E2E_NONE holds neither screen. */
    await page.getByRole('button', { name: /E2E_NONE/ }).click()
    await page.getByRole('button', { name: 'Expand all' }).click()
    await expect(page.getByTestId('role-grant-screen-SEC_USERS').locator('[title*="PERM_SEC_USERS_"]').first()).toBeAttached()
    await expect(page.getByTestId('role-grant-screen-CU_CONFIGURATIONS').locator('[title*="CONFIG_"]').first()).toBeAttached()
  })

  test('TC-FE-SEC-005 Audit log: export downloads a CSV in the UI language; the search uses whitelisted fields', async ({ page }) => {
    const search = page.waitForRequest((r) => r.url().endsWith('/api/v1/sec/audit-log/search'))
    await page.goto('/security/audit-log')
    const body = (await search).postDataJSON() as { filters?: Array<{ field: string }>; sortField?: string }
    for (const filter of body.filters ?? []) expect(['eventTypeCode', 'occurredAt', 'actorUserId']).toContain(filter.field)
    if (body.sortField) expect(['eventTypeCode', 'actor', 'occurredAt']).toContain(body.sortField)

    const exportRequest = page.waitForRequest((r) => r.url().includes('/api/v1/sec/audit-log/export'))
    const download = page.waitForEvent('download')
    await page.getByTestId('audit-log-export').click()
    expect((await exportRequest).headers()['accept-language']).toBe('en')
    const file = await download
    expect(file.suggestedFilename()).toMatch(/\.csv$/)
    const csv = await readFile(await file.path(), 'utf8')
    expect(csv).toContain('eventTypeCode')
    await expect(new ShellPage(page).toasts).toContainText('The audit log export was downloaded.')
  })

  test('TC-FE-SEC-006 Sessions: terminating a live session (confirmed) ends it server-side — its token then answers 401', async ({ page, request }) => {
    const victim = await apiSignIn(request, PLATFORM, USERS.none)
    const listed = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/sessions/search'))
    await page.goto('/security/sessions')
    await listed
    await page.getByRole('button', { name: 'Filter' }).click()
    /* The filter applies itself after a debounce; wait for the request that carries it. */
    const filtered = page.waitForResponse(
      (r) => r.url().endsWith('/api/v1/sec/sessions/search') && (r.request().postData() ?? '').includes(USERS.none)
    )
    await page.locator('#session-filter-username').fill(USERS.none)
    const answer = await filtered
    await page.keyboard.press('Escape')
    const sent = answer.request().postDataJSON() as { filters?: Array<{ field: string }>; username?: string }
    for (const filter of sent.filters ?? []) expect(['ipAddress', 'userId', 'username']).toContain(filter.field)
    /* The victim signed in last, so its session has the highest id of this user's live sessions (read with size 200). */
    const all = await request.post(`${backendUrl}/api/v1/sec/sessions/search`, {
      headers: bearer((await storedToken(page)) ?? ''),
      data: { filters: [{ field: 'username', operator: 'LIKE', value: USERS.none }], page: 0, size: 200 },
    })
    const rows = ((await all.json()) as { data: { content: Array<{ activeSessionPk: number; username: string }> } }).data.content
    const mine = rows.filter((item) => item.username === USERS.none).sort((a, b) => b.activeSessionPk - a.activeSessionPk)[0]
    expect(mine, 'the fresh e2e.none session').toBeDefined()
    expect(((await answer.json()) as { data: { totalElements: number } }).data.totalElements).toBe(rows.length)

    const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name: String(mine.activeSessionPk), exact: true }) })
    /* ⚠ The newest session sorts last; page forward until its row is on screen. */
    for (let hop = 0; hop < 50 && !(await row.isVisible()); hop += 1) {
      const next = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/sessions/search'))
      await page.getByRole('button', { name: 'Next' }).click()
      await next
    }
    await row.getByRole('button', { name: `Terminate the session of ${USERS.none}` }).click()
    await expect(page.getByText('Terminate this session?')).toBeVisible()
    const terminated = page.waitForResponse(
      (r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/v1/sec/sessions/${mine.activeSessionPk}`)
    )
    await page.getByRole('dialog').getByRole('button', { name: 'Terminate' }).click()
    expect((await terminated).ok()).toBe(true)
    await expect(page.getByText('The session was ended. The list has been re-read from the server.')).toBeVisible()
    test.info().annotations.push({ type: 'terminated session', description: String(mine.activeSessionPk) })
    await expect
      .poll(async () => (await request.get(`${backendUrl}/api/v1/sec/menu`, { headers: bearer(victim) })).status(), {
        intervals: [250, 500, 1_000, 2_000],
        timeout: 15_000,
      })
      .toBe(401)
  })

  test('TC-FE-SEC-008 Pending sign-ups: a new staff request is listed (PENDING, submittedAt) and rejected behind a confirmation', async ({ page, request }) => {
    const email = `e2e.signup.sec.${Date.now()}@example.test`
    const created = await request.post(`${backendUrl}/api/v1/sec/auth/signup`, {
      headers: { 'X-Tenant-Code': PLATFORM },
      data: { email, fullNameAr: 'طلب تسجيل الاختبار', fullNameEn: 'E2E Pending Sign-up' },
    })
    expect(created.status()).toBe(201)

    const search = page.waitForRequest((r) => r.url().endsWith('/api/v1/sec/signup-requests/search'))
    await page.goto('/security/users/pending')
    const body = (await search).postDataJSON() as { filters: Array<{ field: string; value: string }>; sortField?: string }
    expect(body.filters).toContainEqual(expect.objectContaining({ field: 'statusCode', value: 'PENDING' }))
    expect(body.sortField).toBe('submittedAt')

    const e2eRequests = page.getByRole('button', { name: /^Reject the sign-up request from e2e\.signup\./ })
    await expect(page.getByRole('button', { name: `Reject the sign-up request from ${email}` })).toBeVisible()
    while ((await e2eRequests.count()) > 0) {
      const target = e2eRequests.first()
      const label = (await target.getAttribute('aria-label')) ?? ''
      await target.click()
      const dialog = page.getByRole('alertdialog')
      await expect(dialog).toContainText('Reject sign-up request?')
      const decided = page.waitForResponse((r) => r.request().method() === 'PATCH' && /\/signup-requests\/\d+$/.test(r.url()))
      await dialog.getByTestId('confirm-dialog-confirm').click()
      expect((await decided).ok(), label).toBe(true)
      await expect(page.getByRole('button', { name: label })).toHaveCount(0)
    }
    await expect(page.getByRole('button', { name: `Reject the sign-up request from ${email}` })).toHaveCount(0)
  })

  test('TC-FE-SEC-010 Users: a header click sorts on the server — sortField username ASC, then DESC — and the header reports aria-sort', async ({ page }) => {
    const isUsersSearch = (r: { url(): string }) => r.url().endsWith('/api/v1/sec/users/search')
    const first = page.waitForRequest(isUsersSearch)
    await page.goto('/security/users')
    expect(((await first).postDataJSON() as { sortField?: string }).sortField).toBeUndefined()
    const header = page.getByRole('columnheader', { name: /^username$/i })
    await expect(header).toHaveAttribute('aria-sort', 'none')

    const ascending = page.waitForRequest((r) => isUsersSearch(r) && (r.postDataJSON() as { sortField?: string }).sortField === 'username')
    await header.getByRole('button').click()
    expect(((await ascending).postDataJSON() as { sortDirection?: string }).sortDirection).toBe('ASC')
    await expect(header).toHaveAttribute('aria-sort', 'ascending')

    const descending = page.waitForRequest(
      (r) => isUsersSearch(r) && (r.postDataJSON() as { sortDirection?: string }).sortDirection === 'DESC'
    )
    await header.getByRole('button').click()
    expect(((await descending).postDataJSON() as { sortField?: string }).sortField).toBe('username')
    await expect(header).toHaveAttribute('aria-sort', 'descending')
  })

  test('TC-FE-SEC-011 Users: the toolbar export menu downloads the visible page as .xlsx (export code loaded on demand) with a success toast', async ({ page }) => {
    await page.goto('/security/users')
    await expect(page.getByTestId('user-realm-badge').first()).toBeVisible()
    await page.getByRole('button', { name: 'Export' }).click()
    const menu = page.getByRole('menu', { name: 'Export' })
    await expect(menu).toBeVisible()
    const download = page.waitForEvent('download')
    await menu.getByRole('menuitem', { name: /excel/i }).click()
    expect((await download).suggestedFilename()).toBe('users_list.xlsx')
    await expect(new ShellPage(page).toasts).toContainText('Export generated successfully')
    await expect(menu).toHaveCount(0)
  })

  test('TC-FE-SEC-012 Audit log: the date filters are the shared DateField; a typed range is sent as occurredAt GTE/LTE instants', async ({ page }) => {
    await page.goto('/security/audit-log')
    await page.getByRole('button', { name: 'Filter' }).click()
    const from = page.locator('#audit-filter-occurred-from')
    const to = page.locator('#audit-filter-occurred-to')
    await expect(from).toHaveAttribute('type', 'text')
    await expect(to).toHaveAttribute('type', 'text')
    await expect(page.locator('input[type="datetime-local"], input[type="date"]')).toHaveCount(0)
    await expect(page.getByTestId('audit-filter-occurred-from-open')).toBeVisible()

    await from.fill('2026-01-01T00:00')
    await to.fill('2026-12-31T23:59')
    const search = page.waitForRequest(
      (r) =>
        r.url().endsWith('/api/v1/sec/audit-log/search') &&
        ((r.postDataJSON() as { filters?: Array<{ field: string }> }).filters ?? []).some((f) => f.field === 'occurredAt')
    )
    await page.getByRole('button', { name: 'Apply', exact: true }).click()
    const filters = ((await search).postDataJSON() as { filters: Array<{ field: string; operator: string; value: string }> }).filters
    const range = filters.filter((filter) => filter.field === 'occurredAt')
    expect(range.map((filter) => filter.operator).sort()).toEqual(['GREATER_THAN_OR_EQUAL', 'LESS_THAN_OR_EQUAL'])
    for (const filter of range) expect(filter.value).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
  })

  test('TC-FE-SEC-009 Security dashboard: the users and sessions widgets state that they count staff accounts only', async ({ page }) => {
    const dashboard = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/dashboard'))
    await page.goto('/security/dashboard')
    expect((await dashboard).status()).toBe(200)
    await expect(page.getByText('Counts cover staff accounts only.')).toHaveCount(2)
  })
})

test('TC-FE-SEC-007 Tenant confinement: a PLATFORM user id opened from the E2E tenant answers 404 and closes with one toast', async ({ page }) => {
  await signInViaUi(page, e2eTenant, USERS.e2eAdmin)
  const read = page.waitForResponse((r) => /\/api\/v1\/sec\/users\/1$/.test(r.url()))
  await page.goto('/security/users/1')
  const answer = await read
  expect(answer.status()).toBe(404)
  expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('SEC-404-USER')
  await expect(page).toHaveURL(/\/security\/users$/)
  await expect(new ShellPage(page).toasts).toContainText(ITEM_UNAVAILABLE)
})
