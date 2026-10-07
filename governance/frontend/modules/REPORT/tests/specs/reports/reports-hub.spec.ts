/** Reports hub on erp-core 1.2.0 (steps 11, 12): *_REPORTS grants, run, CSV export with BOM, LOOKUP and DATETIME params, inline REPORT_PARAM_INVALID. */
import { readFile } from 'node:fs/promises'
import { expect, test } from '@playwright/test'
import { hasPassword, MISSING_ENV_REASON, STATE } from '../../pom/env.ts'
import { useLanguage } from '../../pom/session.ts'
import { openReport } from '../../pom/ReportsHubPage.ts'
const REPORT_SCREENS = ['SEC_REPORTS', 'NOTIF_REPORTS', 'AUDIT_REPORTS'] as const
const RUN_URL = (code: string) => new RegExp(`/api/v1/report/${code}/run$`)

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test('TC-FE-REPORT-001 grant: SYS_ADMIN receives SEC/NOTIF/AUDIT_REPORTS in the Roles grant tree; the menu then lists the three hubs', async ({ page }) => {
  await page.goto('/security/roles')
  await page.getByRole('button', { name: /SYS_ADMIN/ }).click()
  await expect(page).toHaveURL(/editId=\d+/)
  await page.getByRole('button', { name: 'Expand all' }).click()

  let ticked = false
  for (const pageCode of REPORT_SCREENS) {
    const screenNode = page.getByTestId(`role-grant-screen-${pageCode}`)
    await expect(screenNode).toBeVisible()
    const tickAll = screenNode.getByRole('button', { name: 'Tick all' })
    if (await tickAll.isVisible()) {
      await tickAll.click()
      ticked = true
    }
  }
  if (ticked) {
    const saved = page.waitForResponse(
      (r) => r.request().method() === 'POST' && /\/api\/v1\/sec\/roles\/\d+\/actions$/.test(r.url())
    )
    await page.getByRole('button', { name: 'Save permissions' }).click()
    expect((await saved).ok()).toBe(true)
    /* ⚠ The save is one request per screen and action; reloading before it reports would abort the rest. */
    await expect(page.getByText(/Saved \d+ permissions\./)).toBeVisible({ timeout: 30_000 })
  }

  await page.reload()
  for (const moduleCode of ['SEC', 'NOTIF', 'AUDIT']) {
    /* ⚠ Present, not visible: a module group may render collapsed in the sidebar. */
    await expect(page.locator(`a[href="/reports/${moduleCode}"]`)).toHaveCount(1)
  }
  await page.goto('/reports')
  await expect(page).toHaveURL(/\/reports\/(SEC|NOTIF|AUDIT)$/)
})

test('TC-FE-REPORT-002 /reports/SEC lists SEC_USER_LIST; activeOnly=true runs into a table with the documented columns and totalRows', async ({ page }) => {
  await openReport(page, 'SEC', 'SEC_USER_LIST')
  await expect(page.getByTestId('report-card-APP_SMOKE_REPORT')).toHaveCount(0)
  await page.getByTestId('report-param-activeOnly').check()
  const run = page.waitForResponse((r) => r.request().method() === 'POST' && RUN_URL('SEC_USER_LIST').test(r.url()))
  await page.getByTestId('report-params-run').click()
  const response = await run
  expect(response.status()).toBe(200)
  expect(response.request().postDataJSON()).toEqual({ params: { activeOnly: true }, page: 0, size: 20 })

  await expect(page.getByTestId('report-params-drawer')).toHaveCount(0)
  const table = page.getByTestId('report-result-table')
  await expect(table).toBeVisible()
  for (const [key, label] of [
    ['username', 'Username'],
    ['email', 'E-mail'],
    ['realm', 'Realm'],
    ['status', 'Status'],
    ['active', 'Active'],
    ['createdAt', 'Created at'],
  ]) {
    await expect(page.getByTestId(`report-col-${key}`)).toHaveText(label)
  }
  await expect(page.getByTestId('report-total-rows')).toHaveText(/^\d+ rows$/)
  await expect(page.getByTestId('report-row').first()).toBeVisible()
})

test.describe('Arabic UI', () => {
  test.beforeEach(async ({ page }) => {
    await useLanguage(page, 'ar')
  })

  test('TC-FE-REPORT-003 export CSV downloads a UTF-8 BOM file whose header row is Arabic', async ({ page }) => {
    await openReport(page, 'SEC', 'SEC_USER_LIST')
    await page.getByTestId('report-param-activeOnly').check()
    await page.getByTestId('report-params-run').click()
    await expect(page.getByTestId('report-result-table')).toBeVisible()
    await expect(page.getByTestId('report-col-username')).toHaveText('اسم المستخدم')

    const exportRequest = page.waitForRequest((r) => r.method() === 'POST' && /\/report\/SEC_USER_LIST\/export\?format=csv$/.test(r.url()))
    const download = page.waitForEvent('download')
    await page.getByTestId('report-export-csv').click()
    expect((await exportRequest).headers()['accept-language']).toBe('ar')
    const file = await download
    expect(file.suggestedFilename()).toBe('SEC_USER_LIST.csv')
    const bytes = await readFile(await file.path())
    expect([...bytes.subarray(0, 3)]).toEqual([0xef, 0xbb, 0xbf])
    const header = bytes.subarray(3).toString('utf8').split(/\r?\n/)[0]
    expect(header).toContain('اسم المستخدم')
    expect(header).not.toContain('Username')
    await expect(page.getByTestId('toast-container')).toContainText('تم تصدير التقرير.')
  })
})

test('TC-FE-REPORT-004 /reports/NOTIF: NOTIF_LOG_SUMMARY with LOOKUP selects filled from NOTIF_CHANNEL / NOTIF_STATUS', async ({ page }) => {
  const channel = page.waitForResponse((r) => r.url().endsWith('/api/v1/mdl/lookups?type=NOTIF_CHANNEL'))
  const status = page.waitForResponse((r) => r.url().endsWith('/api/v1/mdl/lookups?type=NOTIF_STATUS'))
  await openReport(page, 'NOTIF', 'NOTIF_LOG_SUMMARY')
  expect((await channel).status()).toBe(200)
  expect((await status).status()).toBe(200)

  const channelSelect = page.getByTestId('report-param-channel')
  await expect(channelSelect.locator('option[value="EMAIL"]')).toHaveCount(1)
  await expect(channelSelect.locator('option[value="IN_APP"]')).toHaveCount(1)
  await expect(page.getByTestId('report-param-status').locator('option[value="SKIPPED_NO_PROVIDER"]')).toHaveCount(1)

  await channelSelect.selectOption('EMAIL')
  const run = page.waitForResponse((r) => r.request().method() === 'POST' && RUN_URL('NOTIF_LOG_SUMMARY').test(r.url()))
  await page.getByTestId('report-params-run').click()
  const response = await run
  expect(response.status()).toBe(200)
  expect(response.request().postDataJSON()).toMatchObject({ params: { channel: 'EMAIL' } })
  await expect(page.getByTestId('report-col-count')).toHaveText('Count')
})

test('TC-FE-REPORT-005 /reports/AUDIT: AUDIT_EVENT_LIST DATETIME params are the shared DateField; a forced occurredFrom=abc is refused inline on that field', async ({ page }) => {
  await openReport(page, 'AUDIT', 'AUDIT_EVENT_LIST')
  const occurredFrom = page.getByTestId('report-param-occurredFrom')
  await expect(occurredFrom).toHaveAttribute('type', 'text')
  await expect(page.getByTestId('report-param-occurredTo')).toHaveAttribute('type', 'text')
  await expect(page.getByTestId('report-params-drawer').locator('input[type="datetime-local"], input[type="date"]')).toHaveCount(0)
  await expect(page.getByTestId('report-params-drawer').getByRole('button', { name: 'Open the calendar' }).first()).toBeVisible()

  await occurredFrom.fill('2026-01-01T00:00')
  /* The harness forces a value the form itself would never send; the real backend judges it. */
  await page.route('**/api/v1/report/AUDIT_EVENT_LIST/run', async (route) => {
    const body = route.request().postDataJSON() as { params: Record<string, unknown> }
    await route.continue({ postData: JSON.stringify({ ...body, params: { ...body.params, occurredFrom: 'abc' } }) })
  })
  const run = page.waitForResponse((r) => r.request().method() === 'POST' && RUN_URL('AUDIT_EVENT_LIST').test(r.url()))
  await page.getByTestId('report-params-run').click()
  const response = await run
  expect(response.status()).toBe(400)
  const body = (await response.json()) as { error: { code: string; fieldErrors: { field: string }[] } }
  expect(body.error.code).toBe('REPORT_PARAM_INVALID')
  expect(body.error.fieldErrors.map((item) => item.field)).toEqual(['occurredFrom'])

  await expect(page.getByTestId('report-params-drawer')).toBeVisible()
  await expect(occurredFrom).toHaveAttribute('aria-invalid', 'true')
  await expect(page.getByText('This value was refused by the report.')).toBeVisible()
  await page.unroute('**/api/v1/report/AUDIT_EVENT_LIST/run')

  const valid = page.waitForResponse((r) => r.request().method() === 'POST' && RUN_URL('AUDIT_EVENT_LIST').test(r.url()))
  await page.getByTestId('report-params-run').click()
  const validResponse = await valid
  expect(validResponse.status()).toBe(200)
  expect((validResponse.request().postDataJSON() as { params: { occurredFrom: string } }).params.occurredFrom).toMatch(
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/
  )
  await expect(page.getByTestId('report-result-table')).toBeVisible()
})
