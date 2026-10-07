/** AUDIT_EVENTS on erp-core 1.2.0 (steps 10, 12): the grant, the admin's LOGIN event, the detail drawer, and a refused `from` shown inline. */
import { expect, test } from '@playwright/test'
import { hasPassword, MISSING_ENV_REASON, STATE } from '../../pom/env.ts'
import { grantScreenToSysAdmin } from '../../pom/grantScreen.ts'
import { useLanguage } from '../../pom/session.ts'
const EVENTS_URL = /\/api\/v1\/audit\/events\?/

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test('TC-FE-AUDIT-001 grant: SYS_ADMIN receives AUDIT_EVENTS in the Roles grant tree; the menu then lists /audit/events', async ({ page }) => {
  await grantScreenToSysAdmin(page, 'AUDIT_EVENTS')
  await page.reload()
  /* ⚠ Present, not visible: the AUDIT group may render collapsed in the sidebar. */
  await expect(page.locator('a[href="/audit/events"]')).toHaveCount(1)
  await page.goto('/audit/events')
  await expect(page.getByRole('heading', { name: 'Audit events' })).toBeVisible()
})

test("TC-FE-AUDIT-002 the admin's LOGIN event is listed (newest first) and its drawer shows the event with no field changes", async ({ page }) => {
  const listed = page.waitForResponse((r) => EVENTS_URL.test(r.url()) && r.url().includes('action=LOGIN'))
  await page.goto('/audit/events?action=LOGIN&actor=admin')
  const response = await listed
  expect(response.status()).toBe(200)
  expect(new URL(response.url()).searchParams.get('size')).toBe('20')
  const body = (await response.json()) as { data: { content: { id: number; action: string; actor: string; occurredAt: string }[] } }
  const newest = body.data.content[0]
  expect(newest).toMatchObject({ action: 'LOGIN', actor: 'admin' })
  const times = body.data.content.map((event) => Date.parse(event.occurredAt))
  for (let index = 1; index < times.length; index += 1) expect(times[index]).toBeLessThanOrEqual(times[index - 1])
  await expect(page.locator('[data-testid^="audit-event-row-"]').first()).toHaveAttribute('data-testid', `audit-event-row-${newest.id}`)

  const row = page.getByTestId(`audit-event-row-${newest.id}`)
  await expect(row).toContainText('Successful login')
  await expect(row.getByTestId('audit-event-realm')).toHaveAttribute('data-realm', 'STAFF')
  await row.click()
  await expect(page).toHaveURL(new RegExp(`eventId=${newest.id}`))
  const drawer = page.getByTestId('audit-event-drawer')
  await expect(drawer).toContainText('SEC_USER #')
  await expect(drawer).toContainText('This event records no field changes.')
})

test('TC-FE-AUDIT-003 a bad `from` reaches the server, which refuses it; the refusal is shown inline under From in the filter drawer', async ({ page }) => {
  const refused = page.waitForResponse((r) => EVENTS_URL.test(r.url()) && r.url().includes('from=bad'))
  await page.goto('/audit/events?from=bad')
  const response = await refused
  expect(response.status()).toBe(400)
  expect(((await response.json()) as { error: { code: string } }).error.code).toBe('VALIDATION_ERROR')

  await expect(page.getByTestId('audit-events-error')).toContainText('The date range was refused')
  await page.getByTestId('audit-events-filter').click()
  const from = page.getByTestId('audit-event-filter-from')
  await expect(from).toHaveAttribute('aria-invalid', 'true')
  await expect(from).toHaveAccessibleDescription(/The date range was refused/)
  await expect(page.getByTestId('toast-container')).toHaveCount(0)
})
