/** CUSTOMER self-service (v2-05), live: guarded profile/inbox redirects, and the anonymous public-file viewer over a file the staff side publishes. */
import { expect, test, type Page } from '@playwright/test'
import { confirm, createCategory, openFile, setScope, upload } from '../../pom/FileBrowserPage.ts'
import { frontendUrl, hasPassword, MISSING_ENV_REASON, STATE } from '../../pom/env.ts'
import { useLanguage } from '../../pom/session.ts'

const RUN = Date.now()
const CATEGORY = `E2E_PUBVIEW_${RUN}`
const FILE_NAME = `e2e-viewer-${RUN}.png`

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

for (const [id, path] of [
  ['TC-FE-CUST-008', '/customer/profile'],
  ['TC-FE-CUST-009', '/customer/inbox'],
] as const) {
  test(`${id} ${path} without a customer session redirects to /customer/login (a staff session does not count) and reads nothing`, async ({ page }) => {
    const customerCalls: string[] = []
    page.on('request', (request) => {
      if (request.url().includes('/api/v1/customers/me')) customerCalls.push(request.url())
    })
    await page.goto(path)
    await expect(page).toHaveURL(/\/customer\/login$/)
    await expect(page.getByTestId('customer-login-form')).toBeVisible()
    await expect(page.getByTestId('customer-nav-profile')).toHaveCount(0)
    expect(customerCalls).toEqual([])
  })
}

/* The category filter drawer exposes no test ids (its code field is `#file-category-filter-code`); rows and actions do. */
async function deactivateCategory(page: Page, code: string): Promise<void> {
  await page.goto('/files/categories')
  await page.getByRole('button', { name: 'Filter' }).click()
  await page.locator('#file-category-filter-code').fill(code)
  await page.getByRole('button', { name: 'Apply' }).click()
  const row = page.getByTestId(`file-category-row-${code}`)
  const deactivated = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().includes('/api/v1/files/categories/'))
  await row.getByTestId(`file-category-deactivate-${code}`).click()
  await confirm(page)
  expect((await deactivated).status()).toBe(204)
}

async function deleteFile(page: Page, fileId: number): Promise<void> {
  await openFile(page, fileId)
  const deleted = page.waitForResponse((r) => r.url().includes(`/api/v1/files/${fileId}?action=DELETE`))
  await page.getByTestId('file-meta-delete').click()
  await confirm(page)
  expect((await deleted).status()).toBe(200)
}

test('TC-FE-CUST-010 a file the staff side publishes renders anonymously in /customer/files/:tenant/:slug; after unpublish the viewer says not found', async ({ page, browser }) => {
  let categoryCreated = false
  let fileId = 0
  try {
    await createCategory(page, CATEGORY, true)
    categoryCreated = true
    await setScope(page)
    fileId = await upload(page, FILE_NAME, CATEGORY)
    await page.getByTestId(`file-row-${fileId}`).click()
    const published = page.waitForResponse((r) => r.url().endsWith(`/api/v1/files/${fileId}/visibility`))
    await page.getByTestId('file-meta-publish').click()
    expect((await published).status()).toBe(200)
    const publicUrl = (await page.getByTestId('file-meta-public-url').getAttribute('href')) ?? ''
    const match = /\/api\/v1\/public\/files\/([^/]+)\/([^/?#]+)/.exec(new URL(publicUrl).pathname)
    expect(match, publicUrl).not.toBeNull()
    const [, tenantCode, slug] = match as RegExpExecArray

    const anonymous = await browser.newContext({ baseURL: frontendUrl, viewport: { width: 1440, height: 900 } })
    try {
      const viewer = await anonymous.newPage()
      await useLanguage(viewer)
      const head = viewer.waitForResponse((r) => r.request().method() === 'HEAD' && r.url().includes(`/api/v1/public/files/${tenantCode}/${slug}`))
      await viewer.goto(`/customer/files/${tenantCode}/${slug}`)
      const answer = await head
      expect(answer.status()).toBe(200)
      expect(answer.request().headers()['authorization']).toBeUndefined()
      expect(answer.request().headers()['x-tenant-code']).toBeUndefined()

      await expect(viewer.getByTestId('public-file-viewer')).toHaveAttribute('data-kind', 'image')
      const image = viewer.getByTestId('public-file-image')
      await expect(image).toHaveAttribute('alt', FILE_NAME)
      await expect.poll(() => image.evaluate((el) => (el as HTMLImageElement).complete && (el as HTMLImageElement).naturalWidth)).toBeGreaterThan(0)
      await expect(viewer.getByRole('heading', { level: 1 })).toHaveText(FILE_NAME)
      await expect(viewer.getByTestId('public-file-download')).toHaveAttribute('download', FILE_NAME)
      await expect(viewer.getByTestId('customer-nav-profile')).toHaveCount(0)

      await openFile(page, fileId)
      await page.getByTestId('file-meta-unpublish').click()
      await confirm(page)
      await expect(page.getByTestId('file-meta-visibility').getByTestId('file-visibility-badge')).toHaveAttribute('data-visibility', 'PRIVATE')

      const gone = viewer.waitForResponse((r) => r.request().method() === 'HEAD' && r.url().includes(`/api/v1/public/files/${tenantCode}/${slug}`))
      await viewer.reload()
      expect((await gone).status()).toBe(404)
      await expect(viewer.getByTestId('public-file-not-found')).toHaveText(/This file does not exist or is no longer shared\./)
      await expect(viewer.getByTestId('public-file-image')).toHaveCount(0)
    } finally {
      await anonymous.close()
    }
  } finally {
    /* ⚠ Runs on failure too: a shared backend must not keep a published e2e file or an active e2e category. */
    if (fileId > 0) await deleteFile(page, fileId)
    if (categoryCreated) await deactivateCategory(page, CATEGORY)
  }
})
