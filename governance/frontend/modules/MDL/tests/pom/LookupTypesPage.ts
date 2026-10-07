/** POM helpers for MDL_LOOKUPS: unique keys, the `/reference-data/lookups/new` record page and its inline header form. */
import { expect, type Page } from '@playwright/test'

export const LOOKUPS_LIST_PATH = '/reference-data/lookups'
export const LOOKUPS_NEW_URL = /\/reference-data\/lookups\/new$/
export const LOOKUP_RECORD_URL = /\/reference-data\/lookups\/\d+$/

export function uniqueKey(prefix: string): string {
  return `${prefix}_${Date.now()}`
}

export async function openCreatePage(page: Page): Promise<void> {
  await page.goto(LOOKUPS_LIST_PATH)
  await expect(page.getByTestId('mdl-type-create')).toBeEnabled()
  await page.getByTestId('mdl-type-create').click()
  await expect(page).toHaveURL(LOOKUPS_NEW_URL)
  await expect(page.getByTestId('mdl-type-form-owner')).toBeEnabled()
}

export async function fillCreateForm(page: Page, key: string, ownerModuleCode: string): Promise<void> {
  await page.getByTestId('mdl-type-form-key').fill(key)
  await page.getByTestId('mdl-type-form-owner').selectOption(ownerModuleCode)
  await page.getByTestId('mdl-type-form-name-ar').fill('نوع اختبار')
  await page.getByTestId('mdl-type-form-name-en').fill('E2E lookup type')
}
