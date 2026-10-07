/** SEQUENCE_SERIES on erp-core 1.2.0 (steps 10, 12): grant, create 201, pattern / duplicate refusals inline, deactivate ⇄ activate, and the CREATE audit event's diff. */
import { expect, test } from '@playwright/test'
import { hasPassword, MISSING_ENV_REASON, STATE } from '../../pom/env.ts'
import { grantScreenToSysAdmin } from '../../pom/grantScreen.ts'
import { useLanguage } from '../../pom/session.ts'
import { filterByCode, openCreate, submitCreate } from '../../pom/NumberSeriesPage.ts'
const CODE = `E2E_INV_${Date.now()}`
let seriesId = ''

test.describe.configure({ mode: 'serial' })

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test('TC-FE-SEQUENCE-001 grant: SYS_ADMIN receives SEQUENCE_SERIES (VIEW + MANAGE); the menu then lists /sequences/series', async ({ page }) => {
  await grantScreenToSysAdmin(page, 'SEQUENCE_SERIES')
  await page.reload()
  await expect(page.locator('a[href="/sequences/series"]')).toHaveCount(1)
})

test('TC-FE-SEQUENCE-002 create prefix INV, pattern {PREFIX}-{YYYY}-{SEQ:6}, YEARLY → 201, toast, listed; resetPolicy is sent', async ({ page }) => {
  await openCreate(page, CODE, '{PREFIX}-{YYYY}-{SEQ:6}', 'YEARLY')
  await expect(page.getByTestId('series-pattern-preview')).toHaveText(`INV-${new Date().getFullYear()}-000001`)
  const created = await submitCreate(page)
  expect(created.status()).toBe(201)
  expect(created.request().postDataJSON()).toMatchObject({ code: CODE, prefix: 'INV', resetPolicy: 'YEARLY' })
  await expect(page.getByTestId('toast-container')).toContainText('The number series has been created.')
  await expect(page).not.toHaveURL(/action=create/)
  await filterByCode(page, CODE)
  await expect(page.getByTestId(`series-row-${CODE}`)).toHaveAttribute('data-active', 'true')
})

test('TC-FE-SEQUENCE-003 pattern {PREFIX}-{SEQ:4} with YEARLY → 400 SEQUENCE_PATTERN_INVALID inline on the pattern', async ({ page }) => {
  await openCreate(page, `${CODE}_B`, '{PREFIX}-{SEQ:4}', 'YEARLY')
  await expect(page.getByTestId('series-pattern-date-hint')).toBeVisible()
  const refused = await submitCreate(page)
  expect(refused.status()).toBe(400)
  expect(((await refused.json()) as { error: { code: string } }).error.code).toBe('SEQUENCE_PATTERN_INVALID')
  const pattern = page.getByTestId('series-form-pattern')
  await expect(pattern).toHaveAttribute('aria-invalid', 'true')
  await expect(pattern).toHaveAccessibleDescription(/The server refused this pattern/)
  await expect(page).toHaveURL(/action=create/)
  await expect(page.getByTestId('toast-container')).toHaveCount(0)
})

test('TC-FE-SEQUENCE-004 a duplicate code → 409 NUMBER_SERIES_CODE_DUPLICATE inline on the code', async ({ page }) => {
  await openCreate(page, CODE, '{PREFIX}-{YYYY}-{SEQ:6}', 'YEARLY')
  const refused = await submitCreate(page)
  expect(refused.status()).toBe(409)
  expect(((await refused.json()) as { error: { code: string } }).error.code).toBe('NUMBER_SERIES_CODE_DUPLICATE')
  const code = page.getByTestId('series-form-code')
  await expect(code).toHaveAttribute('aria-invalid', 'true')
  await expect(code).toHaveAccessibleDescription(/already exists/)
})

test('TC-FE-SEQUENCE-005 deactivate (ConfirmDialog) then activate (no dialog) toggles isActive', async ({ page }) => {
  await page.goto('/sequences/series')
  await filterByCode(page, CODE)
  const row = page.getByTestId(`series-row-${CODE}`)
  await row.getByTestId(`series-deactivate-${CODE}`).click()
  const dialog = page.getByRole('alertdialog')
  await expect(dialog).toContainText(`Deactivate ${CODE}?`)
  const deactivated = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/deactivate'))
  await dialog.getByTestId('confirm-dialog-confirm').click()
  const off = await deactivated
  expect(off.status()).toBe(200)
  expect(((await off.json()) as { data: { isActive: boolean } }).data.isActive).toBe(false)
  await expect(page.getByTestId('toast-container')).toContainText('The number series has been deactivated.')
  await expect(row).toHaveAttribute('data-active', 'false')

  const activated = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/activate'))
  await row.getByTestId(`series-activate-${CODE}`).click()
  await expect(page.getByRole('alertdialog')).toHaveCount(0)
  const on = await activated
  expect(((await on.json()) as { data: { isActive: boolean } }).data.isActive).toBe(true)
  await expect(row).toHaveAttribute('data-active', 'true')
})

test('TC-FE-SEQUENCE-006 the series CREATE is in /audit/events and its drawer shows `changes` as a diff', async ({ page }) => {
  const search = page.waitForResponse((r) => r.url().includes('/api/v1/sequence/series/search'))
  await page.goto('/sequences/series')
  await search
  await filterByCode(page, CODE)
  await page.getByTestId(`series-edit-${CODE}`).click()
  const id = new URL(page.url()).searchParams.get('editId')
  expect(id).toMatch(/^\d+$/)
  seriesId = id ?? ''

  await grantScreenToSysAdmin(page, 'AUDIT_EVENTS')
  await page.goto(`/audit/events?entityType=CORE_NUMBER_SERIES&entityId=${id}&action=CREATE`)
  const row = page.locator('[data-testid^="audit-event-row-"][data-action="CREATE"]').first()
  await expect(row).toContainText(`CORE_NUMBER_SERIES #${id}`)
  await row.click()
  const drawer = page.getByTestId('audit-event-drawer')
  const codeChange = drawer.locator('[data-testid="audit-change-row"][data-field="code"]')
  await expect(codeChange).toHaveAttribute('data-changed', 'true')
  await expect(codeChange).toContainText(CODE)
  await expect(drawer.locator('[data-testid="audit-change-row"][data-field="resetPolicy"]')).toContainText('YEARLY')
})

test('TC-FE-SEQUENCE-007 the pattern help is a Disclosure: closed by default, opened and closed by click and keyboard, its marker follows the direction', async ({ page }) => {
  await page.goto('/sequences/series?action=create')
  const help = page.getByTestId('series-pattern-help')
  const toggle = page.getByTestId('series-pattern-help-toggle')
  const panel = page.locator(`#${await toggle.getAttribute('aria-controls')}`)
  await expect(help).toHaveAttribute('data-open', 'false')
  await expect(toggle).toHaveAttribute('aria-expanded', 'false')
  await expect(toggle).toHaveAccessibleName('Pattern tokens and examples')
  await expect(panel).toBeHidden()
  await expect(page.getByTestId('series-pattern-help-marker')).toHaveClass(/ti-chevron-right/)

  await toggle.click()
  await expect(help).toHaveAttribute('data-open', 'true')
  await expect(toggle).toHaveAttribute('aria-expanded', 'true')
  await expect(panel).toBeVisible()
  await expect(panel).toContainText('{SEQ:n}')
  await expect(page.getByTestId('series-pattern-help-marker')).toHaveClass(/ti-chevron-down/)

  await toggle.press('Enter')
  await expect(toggle).toHaveAttribute('aria-expanded', 'false')
  await expect(panel).toBeHidden()
  await toggle.press('Space')
  await expect(toggle).toHaveAttribute('aria-expanded', 'true')
  await expect(page).toHaveURL(/action=create/)

  await useLanguage(page, 'ar')
  await page.reload()
  await expect(page.getByTestId('series-pattern-help-toggle')).toHaveAccessibleName('رموز النمط وأمثلة')
  await expect(page.getByTestId('series-pattern-help')).toHaveAttribute('data-open', 'false')
  await expect(page.getByTestId('series-pattern-help-marker')).toHaveClass(/ti-chevron-left/)
})

test('TC-FE-AUDIT-004 in the audit event drawer the raw JSON is a Disclosure: closed beside a parsed diff, opened by its toggle to show the payload', async ({ page }) => {
  expect(seriesId, 'TC-FE-SEQUENCE-006 provides the series id').toMatch(/^\d+$/)
  await page.goto(`/audit/events?entityType=CORE_NUMBER_SERIES&entityId=${seriesId}&action=CREATE`)
  await page.locator('[data-testid^="audit-event-row-"][data-action="CREATE"]').first().click()
  const drawer = page.getByTestId('audit-event-drawer')
  await expect(drawer.locator('[data-testid="audit-change-row"][data-field="code"]')).toContainText(CODE)

  const raw = drawer.getByTestId('audit-raw-json')
  const toggle = drawer.getByTestId('audit-raw-json-toggle')
  const panel = page.locator(`#${await toggle.getAttribute('aria-controls')}`)
  await expect(raw).toHaveAttribute('data-open', 'false')
  await expect(toggle).toHaveAttribute('aria-expanded', 'false')
  await expect(toggle).toHaveAccessibleName('Raw JSON')
  await expect(panel).toBeHidden()

  await toggle.click()
  await expect(raw).toHaveAttribute('data-open', 'true')
  await expect(panel).toBeVisible()
  await expect(panel.locator('pre')).toContainText(CODE)
  await toggle.click()
  await expect(panel).toBeHidden()
  await expect(page).toHaveURL(/eventId=\d+/)
})
