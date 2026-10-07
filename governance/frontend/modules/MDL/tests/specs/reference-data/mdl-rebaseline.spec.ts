/** MDL on erp-core 1.2.0 as master-detail pages (steps 05, 12, v3-05): 201 create → record page, inline MODULE-NOT-REGISTERED, owner list without FIN. */
import { expect, test } from '@playwright/test'
import { CATALOG_MODULES, hasPassword, MISSING_ENV_REASON, STATE } from '../../pom/env.ts'
import { useLanguage } from '../../pom/session.ts'
import {
  fillCreateForm,
  LOOKUP_RECORD_URL,
  LOOKUPS_LIST_PATH,
  LOOKUPS_NEW_URL,
  openCreatePage,
  uniqueKey,
} from '../../pom/LookupTypesPage.ts'

const REGISTRY_SEARCH = '**/api/v1/sec/registry/search'

interface RegistryPage {
  data: { content: Array<Record<string, unknown>>; last?: boolean }
}

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test.describe('MDL lookups on erp-core 1.2.0', () => {
  test('TC-FE-MDL-003 the owner-module select lists every registered module and no FIN, read at size 200', async ({ page }) => {
    const sizes: number[] = []
    page.on('request', (request) => {
      if (request.url().endsWith('/api/v1/sec/registry/search')) {
        sizes.push((request.postDataJSON() as { size: number }).size)
      }
    })

    await openCreatePage(page)

    const codes = await page
      .getByTestId('mdl-type-form-owner')
      .locator('option:not([value=""])')
      .evaluateAll((options) => options.map((option) => option.getAttribute('value') ?? ''))
    for (const code of CATALOG_MODULES) expect(codes).toContain(code)
    expect(codes).not.toContain('FIN')
    expect(sizes.length).toBeGreaterThan(0)
    expect(sizes.every((size) => size === 200)).toBe(true)
    await expect(page.getByTestId('mdl-value-section-locked')).toBeVisible()
  })

  test('TC-FE-MDL-002 owner FIN (a stale registry offers it) is refused inline as 409 MDL-409-MODULE-NOT-REGISTERED', async ({ page }) => {
    await page.route(REGISTRY_SEARCH, async (route) => {
      const response = await route.fetch()
      const body = (await response.json()) as RegistryPage
      if (body.data.last !== false) {
        body.data.content.push({ code: 'FIN', nameEn: 'Finance', nameAr: 'المالية', isActiveFl: true, screens: [] })
      }
      await route.fulfill({ response, json: body })
    })
    await openCreatePage(page)
    await fillCreateForm(page, uniqueKey('E2E_MDL_FIN'), 'FIN')

    const create = page.waitForResponse(
      (r) => r.url().endsWith('/api/v1/mdl/lookup-types') && r.request().method() === 'POST'
    )
    await page.getByTestId('mdl-type-form-submit').click()
    const answer = await create
    expect(answer.status()).toBe(409)
    expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('MDL-409-MODULE-NOT-REGISTERED')

    const owner = page.getByTestId('mdl-type-form-owner')
    await expect(owner).toHaveAttribute('aria-invalid', 'true')
    const describedBy = await owner.getAttribute('aria-describedby')
    await expect(page.locator(`#${describedBy}`)).toHaveText('The owning module is not registered in the Security module')
    await expect(page).toHaveURL(LOOKUPS_NEW_URL)
    await expect(page.getByTestId('toast-container')).toHaveCount(0)
  })

  test('TC-FE-MDL-001 a lookup type with owner PLATFORM is created with 201, lands on its record page, and the row appears in the key search', async ({ page }) => {
    const key = uniqueKey('E2E_MDL_PLAT')
    await openCreatePage(page)
    await fillCreateForm(page, key, 'PLATFORM')

    const create = page.waitForResponse(
      (r) => r.url().endsWith('/api/v1/mdl/lookup-types') && r.request().method() === 'POST'
    )
    await page.getByTestId('mdl-type-form-submit').click()
    const created = await create
    expect(created.status()).toBe(201)
    const newId = ((await created.json()) as { data: { lookupTypePk: number } }).data.lookupTypePk

    await expect(page.getByTestId('toast-container')).toContainText('The lookup type has been saved.')
    await expect(page).toHaveURL(LOOKUP_RECORD_URL)
    expect(page.url().endsWith(`/reference-data/lookups/${newId}`)).toBe(true)
    await expect(page.getByTestId('mdl-type-section')).toBeVisible()
    await expect(page.locator('#lookup-type-key-readonly')).toHaveValue(key)
    await expect(page.getByTestId('mdl-value-pane')).toBeVisible()

    await page.goto(LOOKUPS_LIST_PATH)
    await page.getByTestId('mdl-type-search-key').fill(key)
    await page.getByTestId('mdl-type-search-key').press('Enter')
    const row = page.locator(`[data-testid="mdl-type-row"][data-key="${key}"]`)
    await expect(row).toBeVisible()
    await expect(row).toContainText('PLATFORM')
    await expect(row).toHaveAttribute('data-active', 'true')
  })
})
