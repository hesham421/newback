/** POM helpers for PLATFORM_TENANTS: filter by code, open a tenant's detail drawer, fill the create drawer. */
import { expect, type Page } from '@playwright/test'
import { password } from './env.ts'

export async function filterByCode(page: Page, code: string): Promise<void> {
  await page.getByRole('button', { name: 'Filter' }).click()
  await page.getByTestId('tenant-filter-code').fill(code)
  await page.getByRole('button', { name: 'Apply' }).click()
}

export async function openTenant(page: Page, code: string): Promise<void> {
  await page.goto('/platform/tenants')
  await filterByCode(page, code)
  await page.getByTestId(`tenant-open-${code}`).click()
  await expect(page).toHaveURL(/tenantId=\d+/)
  await expect(page.getByTestId('tenant-detail-code')).toHaveText(code)
}

export async function fillCreateForm(page: Page, code: string, adminUsername: string): Promise<void> {
  await page.getByTestId('tenant-form-code').fill(code)
  await page.getByTestId('tenant-form-name-ar').fill('مستأجر الاختبار التاسع')
  await page.getByTestId('tenant-form-name-en').fill('E2E Tenant Nine')
  await page.getByTestId('tenant-form-admin-username').fill(adminUsername)
  await page.getByTestId('tenant-form-admin-email').fill(`${adminUsername}@example.test`)
  await page.getByTestId('tenant-form-admin-password').fill(password)
  await page.getByTestId('tenant-form-admin-password-confirm').fill(password)
  await page.getByTestId('tenant-form-admin-full-name-ar').fill('مدير المستأجر التاسع')
  await page.getByTestId('tenant-form-admin-full-name-en').fill('Tenant Nine Admin')
}
