/** NOTIF on erp-core 1.2.0 (steps 08, 12): templates, channels, async delivery log + detail, staff inbox bell, read and unavailable states. */
import { expect, test } from '@playwright/test'
import { backendUrl, hasPassword, MISSING_ENV_REASON, PLATFORM, STATE, USERS } from '../../pom/env.ts'
import { ShellPage } from '../../pom/ShellPage.ts'
import { apiSignIn, bearer, decodeJwt, storedToken, useLanguage } from '../../pom/session.ts'

/* The reference app's bootstrap admin (PLATFORM tenant, userPk 1). */
const ADMIN_EMAIL = 'admin@erp.local'

interface LogRow {
  id: number
  channelTypeId: string
  notificationStatusId: string
  attempts: number
  createdAt: string
}

interface InboxRow {
  id: number
  titleEn: string | null
  createdAt: string
}

/* The backend and the browser share one machine; this only absorbs sub-second rounding. */
const CLOCK_SKEW_MS = 2_000

let newestLogId = 0
let templateCode = ''

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test.describe.serial('NOTIF screens on erp-core 1.2.0', () => {
  test('TC-FE-NOTIF-001 Templates: a missing Arabic body is refused inline; a bilingual template is created (201)', async ({ page }) => {
    const code = `E2E_WELCOME_${Date.now()}`
    templateCode = code
    let createRequests = 0
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().endsWith('/api/v1/notifications/templates')) createRequests += 1
    })

    await page.goto('/notifications/templates')
    await page.getByTestId('notif-template-create').click()
    await expect(page).toHaveURL(/action=create/)
    await page.getByTestId('notif-template-form-code').fill(code)
    await page.getByTestId('notif-template-form-name-ar').fill('ترحيب الاختبار')
    await page.getByTestId('notif-template-form-name-en').fill('E2E welcome')
    await page.getByTestId('notif-template-form-body-en').fill('Hello {0}')
    await page.getByTestId('notif-template-form-submit').click()
    await expect(page.getByText('The template body is required in both Arabic and English.')).toBeVisible()
    expect(createRequests).toBe(0)

    await page.getByTestId('notif-template-form-body-ar').fill('أهلًا {0}')
    const created = page.waitForResponse(
      (r) => r.url().endsWith('/api/v1/notifications/templates') && r.request().method() === 'POST'
    )
    await page.getByTestId('notif-template-form-submit').click()
    expect((await created).status()).toBe(201)
    await expect(page.getByTestId('toast-container')).toContainText('Template created.')
    await expect(page).not.toHaveURL(/action=create/)

    await page.getByRole('button', { name: 'Filter' }).click()
    await page.locator('#notif-template-filter-code').fill(code)
    await page.getByRole('button', { name: 'Apply' }).click()
    await expect(page.getByTestId(`notif-template-row-${code}`)).toBeVisible()
  })

  test('TC-FE-NOTIF-002 Channels: IN_APP is already seeded (409 inline), so the existing config is edited instead (200)', async ({ page }) => {
    await page.goto('/notifications/channels')
    await expect(page.getByTestId('notif-channel-row-IN_APP')).toBeVisible()

    await page.getByTestId('notif-channel-create').click()
    await page.getByTestId('notif-channel-form-type').selectOption('IN_APP')
    const created = page.waitForResponse(
      (r) => r.url().endsWith('/api/v1/notifications/channels') && r.request().method() === 'POST'
    )
    await page.getByTestId('notif-channel-form-submit').click()
    const answer = await created
    expect(answer.status()).toBe(409)
    await expect(page.getByText('This channel type is already configured.', { exact: false })).toBeVisible()
    await expect(page).toHaveURL(/action=create/)

    await page.getByRole('button', { name: 'Cancel' }).click()
    await page.getByTestId('notif-channel-edit-IN_APP').click()
    await expect(page).toHaveURL(/editId=\d+/)
    await expect(page.getByTestId('notif-channel-form-enabled')).toBeChecked()
    const saved = page.waitForResponse(
      (r) => /\/api\/v1\/notifications\/channels\/\d+$/.test(r.url()) && r.request().method() === 'PUT'
    )
    await page.getByTestId('notif-channel-form-submit').click()
    expect((await saved).status()).toBe(200)
    await expect(page.getByTestId('toast-container')).toContainText('Channel saved.')
  })

  test('TC-FE-NOTIF-003 Log: a tenant-scoped password-reset request produces an EMAIL row that settles SKIPPED_NO_PROVIDER, labelled from the lookup', async ({ page, browser }) => {
    const baselineRead = page.waitForResponse((r) => r.url().endsWith('/api/v1/notifications/logs/search'))
    const statusLookup = page.waitForResponse((r) => r.url().endsWith('/api/v1/notifications/lookups/NOTIF_STATUS'))
    await page.goto('/notifications/logs')
    const lookup = await statusLookup
    expect(lookup.status()).toBe(200)
    const statuses = ((await lookup.json()) as { data: Array<{ code: string }> }).data.map((item) => item.code)
    expect(statuses).toContain('SKIPPED_NO_PROVIDER')
    const baselineRows = ((await (await baselineRead).json()) as { data: { content: LogRow[] } }).data.content
    const baselineId = baselineRows.reduce((max, row) => Math.max(max, row.id), 0)

    const anonymous = await browser.newContext({ baseURL: test.info().project.use.baseURL })
    const resetPage = await anonymous.newPage()
    await resetPage.addInitScript("window.localStorage.setItem('avelynq_lang', 'en')")
    await resetPage.goto('/password-reset')
    await resetPage.getByTestId('reset-request-tenant').fill('PLATFORM')
    await resetPage.locator('#password-reset-email').fill(ADMIN_EMAIL)
    const requested = resetPage.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/password-reset/request'))
    await resetPage.getByRole('button', { name: 'Send Reset Instructions' }).click()
    const resetAnswer = await requested
    expect(resetAnswer.ok()).toBe(true)
    expect(resetAnswer.request().headers()['x-tenant-code']).toBe(PLATFORM)
    expect(resetAnswer.request().headers()['authorization']).toBeUndefined()
    await anonymous.close()

    let newest: LogRow | undefined
    await expect
      .poll(
        async () => {
          const read = page.waitForResponse((r) => r.url().endsWith('/api/v1/notifications/logs/search'))
          await page.getByTestId('notif-logs-refresh').click()
          const rows = ((await (await read).json()) as { data: { content: LogRow[] } }).data.content
          newest = rows.find((row) => row.channelTypeId === 'EMAIL' && row.id > baselineId)
          return newest !== undefined
        },
        { intervals: [500, 1_000, 2_000], timeout: 30_000 }
      )
      .toBe(true)
    if (!newest) throw new Error('no EMAIL log row after the reset request')
    test.info().annotations.push({
      type: 'first observed status',
      description: `${newest.notificationStatusId} attempts=${newest.attempts} createdAt=${newest.createdAt}`,
    })

    await expect(page.getByTestId('notif-logs-async-note')).toBeVisible()
    const row = page.getByTestId(`notif-log-row-${newest.id}`)
    await expect(row.getByTestId('notif-log-status')).toHaveAttribute('data-status', /QUEUED|SKIPPED_NO_PROVIDER/)
    await expect(row.getByTestId('notif-log-status')).toHaveAttribute('data-status', 'SKIPPED_NO_PROVIDER', { timeout: 60_000 })
    await expect(row.getByTestId('notif-log-status')).toHaveText('Skipped (no provider)')
    await expect(row.getByTestId('notif-log-attempts')).toHaveText(/^\d+$/)

    await page.getByTestId(`notif-log-open-${newest.id}`).click()
    await expect(page).toHaveURL(new RegExp(`logId=${newest.id}`))
    newestLogId = newest.id
  })

  test('TC-FE-NOTIF-004 Log detail: the drawer, read by id from a deep link, shows the attempts and the last error', async ({ page }) => {
    expect(newestLogId, 'NOTIF-003 found a row').toBeGreaterThan(0)
    const read = page.waitForResponse((r) => r.url().endsWith(`/api/v1/notifications/logs/${newestLogId}`))
    await page.goto(`/notifications/logs?logId=${newestLogId}`)
    const answer = await read
    expect(answer.status()).toBe(200)
    const log = ((await answer.json()) as { data: { attempts: number; lastError: string | null } }).data
    expect(log.attempts).toBeGreaterThanOrEqual(1)
    await expect(page.getByTestId('notif-log-detail')).toBeVisible()
    await expect(page.getByTestId('notif-log-last-error')).toBeVisible()
  })

  test('TC-FE-NOTIF-005 Inbox: an IN_APP notification raises the bell count; the drawer marks it read (PATCH …/read); /inbox filters unread', async ({ page, request }) => {
    await page.goto('/dashboard')
    const adminId = decodeJwt((await storedToken(page)) ?? '').uid ?? 1
    const harness = await apiSignIn(request, PLATFORM, USERS.admin)
    let itemId = 0
    try {
      /* ⚠ Other runs leave unread items too; the dispatched one is the unread id that was absent before and is not older than the dispatch. */
      const unreadRows = async (): Promise<InboxRow[]> => {
        const inbox = await request.get(`${backendUrl}/api/v1/notif/inbox?unreadOnly=true&page=0&size=200`, { headers: bearer(harness) })
        expect(inbox.status()).toBe(200)
        return ((await inbox.json()) as { data: { content: InboxRow[] } }).data.content
      }
      const before = new Set((await unreadRows()).map((row) => row.id))
      const dispatchedAt = Date.now() - CLOCK_SKEW_MS
      const dispatched = await request.post(`${backendUrl}/api/v1/notifications/dispatch`, {
        headers: bearer(harness),
        data: {
          recipientId: adminId,
          templateCode,
          channelHint: ['IN_APP'],
          moduleCode: 'SEC',
          variables: { actionLink: 'https://example.test/e2e' },
        },
      })
      expect(dispatched.status()).toBe(200)
      await expect
        .poll(
          async () => {
            const fresh = (await unreadRows()).find(
              (row) => !before.has(row.id) && row.titleEn === 'E2E welcome' && Date.parse(row.createdAt) >= dispatchedAt
            )
            itemId = fresh?.id ?? 0
            return itemId
          },
          { intervals: [250, 500, 1_000], timeout: 15_000 }
        )
        .toBeGreaterThan(0)
    } finally {
      await request.post(`${backendUrl}/api/v1/sec/auth/logout`, { headers: bearer(harness) })
    }

    const inboxRead = page.waitForResponse((r) => r.url().includes('/api/v1/notif/inbox?') && r.url().includes('unreadOnly=true'))
    await page.reload()
    const unread = ((await (await inboxRead).json()) as { data: { totalElements: number } }).data.totalElements
    expect(unread).toBeGreaterThan(0)
    const shell = new ShellPage(page)
    await expect(page.getByTestId('inbox-unread-count')).toHaveText(unread > 99 ? '99+' : String(unread))

    await shell.inboxBell.click()
    await expect(page).toHaveURL(/inbox=1/)
    await expect(page.getByTestId('inbox-drawer')).toBeVisible()
    const item = page.getByTestId(`inbox-item-${itemId}`)
    await expect(item).toContainText('Unread')
    const marked = page.waitForResponse(
      (r) => r.request().method() === 'PATCH' && r.url().endsWith(`/api/v1/notif/inbox/${itemId}/read`)
    )
    await item.getByRole('button', { name: /^Mark ".+" as read$/ }).click()
    expect((await marked).status()).toBe(200)
    await expect(item).not.toContainText('Unread')

    await page.getByTestId('inbox-view-all').click()
    await expect(page).toHaveURL(/\/inbox$/)
    await expect(page.getByTestId(`inbox-row-${itemId}`)).toContainText('Read')
    await page.getByText('Unread only', { exact: true }).click()
    await expect(page.getByTestId('inbox-unread-only')).toBeChecked()
    await expect(page).toHaveURL(/\/inbox\?unreadOnly=1$/)
    await expect(page.getByTestId(`inbox-row-${itemId}`)).toHaveCount(0)
  })

  test('TC-FE-NOTIF-006 Inbox: 403 NOTIF_CHANNEL_UNAVAILABLE leaves the bell aria-disabled with its reason, and no toast', async ({ page }) => {
    await page.route('**/api/v1/notif/inbox?*', (route) =>
      route.fulfill({
        status: 403,
        json: { success: false, error: { code: 'NOTIF_CHANNEL_UNAVAILABLE', message: 'In-app channel unavailable', fieldErrors: null } },
      })
    )
    await page.goto('/dashboard')
    const bell = new ShellPage(page).inboxBell
    await expect(bell).toHaveAttribute('aria-disabled', 'true')
    await expect(bell).toHaveAccessibleName('The in-app inbox is not available for this account.')
    await expect(page.getByTestId('inbox-unread-count')).toHaveCount(0)
    /* aria-disabled, not disabled: the reason stays reachable by keyboard, and activating it opens nothing. */
    await bell.focus()
    await expect(bell).toBeFocused()
    await page.keyboard.press('Enter')
    await expect(page).not.toHaveURL(/inbox=1/)
    await expect(page.getByTestId('inbox-drawer')).toHaveCount(0)
    await expect(page.getByTestId('toast-container')).toHaveCount(0)
  })
})
