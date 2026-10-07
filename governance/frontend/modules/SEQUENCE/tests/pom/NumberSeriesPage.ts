/** POM helpers for SEQUENCE_SERIES: open the create drawer filled, submit it, filter by code. */
import { expect, type Page } from '@playwright/test'

export const SERIES_URL = /\/api\/v1\/sequence\/series$/

export async function openCreate(page: Page, code: string, pattern: string, policy: string): Promise<void> {
  await page.goto('/sequences/series')
  await page.getByTestId('series-create').click()
  await expect(page).toHaveURL(/action=create/)
  await page.getByTestId('series-form-code').fill(code)
  await page.getByTestId('series-form-reset-policy').selectOption(policy)
  await page.getByTestId('series-form-prefix').fill('INV')
  await page.getByTestId('series-form-pattern').fill(pattern)
}

export async function submitCreate(page: Page) {
  const response = page.waitForResponse((r) => r.request().method() === 'POST' && SERIES_URL.test(r.url()))
  await page.getByTestId('series-form-submit').click()
  return response
}

export async function filterByCode(page: Page, code: string): Promise<void> {
  await page.getByTestId('series-filter').click()
  await page.getByTestId('series-filter-code').fill(code)
  await page.getByRole('button', { name: 'Apply' }).click()
}
