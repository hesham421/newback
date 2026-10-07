/** POM helpers for CU_CONFIGURATIONS / PLATFORM_SETTINGS (one screen, two scopes): filter by key and create an entry. */
import { expect, type Page } from '@playwright/test'

export const CREATE_URL = /\/api\/v1\/common\/configurations\?scope=(TENANT|PLATFORM)$/

export async function filterByKey(page: Page, key: string): Promise<void> {
  await page.getByRole('button', { name: 'Filter' }).click()
  await page.getByTestId('cu-config-filter-key').fill(key)
  await page.getByRole('button', { name: 'Apply' }).click()
}

export async function createEntry(page: Page, key: string, value: string, scope: 'TENANT' | 'PLATFORM') {
  await page.getByTestId('cu-config-create').click()
  await expect(page).toHaveURL(/action=create/)
  await page.getByTestId('cu-config-form-key').fill(key.toLowerCase())
  await page.getByTestId('cu-config-form-value').fill(value)
  const response = page.waitForResponse((r) => r.request().method() === 'POST' && CREATE_URL.test(r.url()))
  await page.getByTestId('cu-config-form-submit').click()
  const created = await response
  expect(created.status()).toBe(201)
  expect(new URL(created.url()).searchParams.get('scope')).toBe(scope)
  const body = (await created.json()) as { data: { scope: string; configKey: string } }
  expect(body.data).toMatchObject({ scope, configKey: key })
}
