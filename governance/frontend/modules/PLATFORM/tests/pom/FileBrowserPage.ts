/** POM helpers for FILE_CATEGORIES and FILE_BROWSER: create a category, set the owner scope, upload, open a file, confirm. */
import { expect, type Page } from '@playwright/test'

export const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC',
  'base64'
)

export async function createCategory(page: Page, code: string, allowPublic: boolean): Promise<void> {
  await page.goto('/files/categories')
  await page.getByTestId('file-categories-create').click()
  await expect(page).toHaveURL(/action=create/)
  await page.getByTestId('file-category-form-code').fill(code)
  await page.getByTestId('file-category-form-name-ar').fill('فئة اختبار')
  await page.getByTestId('file-category-form-name-en').fill(`E2E ${code}`)
  if (allowPublic) await page.getByTestId('file-category-form-allow-public').check()
  const created = page.waitForResponse(
    (r) => r.url().endsWith('/api/v1/files/categories') && r.request().method() === 'POST'
  )
  await page.getByTestId('file-category-form-submit').click()
  expect((await created).status()).toBe(201)
  await expect(page.getByTestId('toast-container')).toContainText('The file category has been created.')
  await expect(page).not.toHaveURL(/action=create/)
}

export async function setScope(page: Page): Promise<void> {
  await page.goto('/files/browser')
  await expect(page.getByTestId('file-browser-scope-required')).toBeVisible()
  await page.getByTestId('file-browser-set-scope').click()
  await expect(page.getByTestId('file-scope-module')).toBeEnabled()
  await page.getByTestId('file-scope-module').selectOption('SEC')
  await page.getByTestId('file-scope-owner-type').fill('user')
  await page.getByTestId('file-scope-owner-id').fill('1')
  await page.getByRole('button', { name: 'Apply' }).click()
  await expect(page).toHaveURL(/moduleCode=SEC&ownerType=USER&ownerId=1/)
}

export async function upload(page: Page, fileName: string, categoryCode: string): Promise<number> {
  await page.getByTestId('file-browser-upload').click()
  await expect(page).toHaveURL(/action=upload/)
  await page.getByTestId('file-upload-input').setInputFiles({ name: fileName, mimeType: 'image/png', buffer: PNG })
  const category = page.getByTestId('file-upload-category')
  const value = await category.locator('option', { hasText: categoryCode }).getAttribute('value')
  await category.selectOption(value ?? '')
  const uploaded = page.waitForResponse((r) => r.url().endsWith('/api/v1/files') && r.request().method() === 'POST')
  await page.getByTestId('file-upload-submit').click()
  const response = await uploaded
  expect(response.status()).toBe(201)
  const body = (await response.json()) as { data: { id: number } }
  await expect(page).not.toHaveURL(/action=upload/)
  return body.data.id
}

export async function confirm(page: Page): Promise<void> {
  await page.getByRole('alertdialog').getByTestId('confirm-dialog-confirm').click()
  await expect(page.getByRole('alertdialog')).toHaveCount(0)
}

export async function openFile(page: Page, id: number): Promise<void> {
  await setScope(page)
  await page.getByTestId(`file-row-${id}`).click()
  await expect(page).toHaveURL(new RegExp(`fileId=${id}`))
}
