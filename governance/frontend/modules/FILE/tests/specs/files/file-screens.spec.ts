/** FILE screens on erp-core 1.2.0 (steps 07, 12, v3-07): categories with allowPublic, scoped browser, upload, download, publish/unpublish, archive/delete, server header sort. */
import { expect, test } from '@playwright/test'
import { backendUrl, hasPassword, MISSING_ENV_REASON, STATE } from '../../pom/env.ts'
import { useLanguage } from '../../pom/session.ts'
import { confirm, createCategory, openFile, setScope, upload } from '../../pom/FileBrowserPage.ts'
const RUN = Date.now()
const PUBLIC_CATEGORY = `E2E_IMG_${RUN}`
const PRIVATE_CATEGORY = `E2E_PRIV_${RUN}`
const FILE_NAME = `e2e-${RUN}.png`
let fileId = 0

test.use({ storageState: STATE.admin })

test.beforeEach(async ({ page }) => {
  test.skip(!hasPassword, MISSING_ENV_REASON)
  await useLanguage(page)
})

test.describe.serial('FILE screens on erp-core 1.2.0', () => {
  test('TC-FE-FILE-001 categories: a public and a private category are created (201); the grid shows allowPublic', async ({ page }) => {
    await createCategory(page, PUBLIC_CATEGORY, true)
    await createCategory(page, PRIVATE_CATEGORY, false)
    await page.getByRole('button', { name: 'Filter' }).click()
    await page.locator('#file-category-filter-code').fill(`_${RUN}`)
    await page.getByRole('button', { name: 'Apply' }).click()
    await expect(page.getByTestId(`file-category-row-${PUBLIC_CATEGORY}`).getByTestId('file-category-allow-public')).toHaveAttribute('data-allow-public', 'true')
    await expect(page.getByTestId(`file-category-row-${PRIVATE_CATEGORY}`).getByTestId('file-category-allow-public')).toHaveAttribute('data-allow-public', 'false')
  })

  test('TC-FE-FILE-002 upload (201) → DB provider, PRIVATE visibility, no public URL; download saves the file under its name', async ({ page }) => {
    await setScope(page)
    const sent = page.waitForRequest((r) => r.url().endsWith('/api/v1/files') && r.method() === 'POST')
    fileId = await upload(page, FILE_NAME, PUBLIC_CATEGORY)
    expect((await sent).headers()['content-type']).toMatch(/^multipart\/form-data; boundary=/)

    const row = page.getByTestId(`file-row-${fileId}`)
    await expect(row.getByTestId('file-provider-badge')).toHaveAttribute('data-provider', 'DB')
    await expect(row.getByTestId('file-visibility-badge')).toHaveAttribute('data-visibility', 'PRIVATE')

    await row.click()
    await expect(page).toHaveURL(new RegExp(`fileId=${fileId}`))
    await expect(page.getByTestId('file-meta-public-url')).toHaveCount(0)
    const downloadEvent = page.waitForEvent('download')
    await page.getByTestId('file-meta-download').click()
    const download = await downloadEvent
    expect(download.suggestedFilename()).toBe(FILE_NAME)
  })

  test('TC-FE-FILE-003 publish → PUBLIC with a public URL served without auth (GET/HEAD, tenant in the path); unpublish (confirmed) → 404', async ({ page, playwright }) => {
    await openFile(page, fileId)
    const published = page.waitForResponse((r) => r.url().endsWith(`/api/v1/files/${fileId}/visibility`))
    await page.getByTestId('file-meta-publish').click()
    const answer = await published
    expect(answer.request().postDataJSON()).toEqual({ visibility: 'PUBLIC' })
    expect(answer.status()).toBe(200)
    await expect(page.getByTestId('file-meta-visibility').getByTestId('file-visibility-badge')).toHaveAttribute('data-visibility', 'PUBLIC')
    const link = page.getByTestId('file-meta-public-url')
    const publicUrl = (await link.getAttribute('href')) ?? ''
    expect(publicUrl).toMatch(/^https?:\/\/[^/]+\/api\/v1\/public\/files\/PLATFORM\/.+/)
    const anonymous = await playwright.request.newContext()
    try {
      const viaBackend = await anonymous.head(`${backendUrl}${new URL(publicUrl).pathname}`)
      expect(viaBackend.status()).toBe(200)
      const viaFrontend = await anonymous.get(publicUrl)
      expect(viaFrontend.status()).toBe(200)
      expect(viaFrontend.headers()['content-type']).toContain('image/png')

      await page.getByTestId('file-meta-unpublish').click()
      await confirm(page)
      await expect(page.getByTestId('file-meta-visibility').getByTestId('file-visibility-badge')).toHaveAttribute('data-visibility', 'PRIVATE')
      expect((await anonymous.head(`${backendUrl}${new URL(publicUrl).pathname}`)).status()).toBe(404)
    } finally {
      await anonymous.dispose()
    }
  })

  test('TC-FE-FILE-004 archive (confirmed) → ARCHIVED; delete (confirmed, ?action=DELETE) → toast, drawer closes, row stays as DELETED', async ({ page }) => {
    await openFile(page, fileId)
    await page.getByTestId('file-meta-archive').click()
    await confirm(page)
    await expect(page.getByTestId('file-meta-status').getByTestId('file-status-badge')).toHaveAttribute('data-status', 'ARCHIVED')

    const deleted = page.waitForResponse((r) => r.url().includes(`/api/v1/files/${fileId}?action=DELETE`))
    await page.getByTestId('file-meta-delete').click()
    await confirm(page)
    expect((await deleted).status()).toBe(200)
    await expect(page.getByTestId('toast-container')).toContainText('The file has been deleted.')
    await expect(page).not.toHaveURL(/fileId=/)
    await expect(page.getByTestId(`file-row-${fileId}`).getByTestId('file-status-badge')).toHaveAttribute('data-status', 'DELETED')
  })

  test('TC-FE-FILE-005 publishing a file whose category disallows public files is refused inline (409 FILE_PUBLIC_NOT_ALLOWED)', async ({ page }) => {
    await setScope(page)
    const id = await upload(page, `e2e-priv-${RUN}.png`, PRIVATE_CATEGORY)
    await page.getByTestId(`file-row-${id}`).click()
    const refused = page.waitForResponse((r) => r.url().endsWith(`/api/v1/files/${id}/visibility`))
    await page.getByTestId('file-meta-publish').click()
    const answer = await refused
    expect(answer.status()).toBe(409)
    expect(((await answer.json()) as { error: { code: string } }).error.code).toBe('FILE_PUBLIC_NOT_ALLOWED')
    await expect(page.getByTestId('file-meta-error')).toHaveText("This file's category does not allow public files.")
    await expect(page.getByTestId('file-meta-visibility').getByTestId('file-visibility-badge')).toHaveAttribute('data-visibility', 'PRIVATE')

    await page.getByTestId('file-meta-delete').click()
    await confirm(page)
    await expect(page.getByTestId('toast-container')).toContainText('The file has been deleted.')
  })

  test('TC-FE-FILE-006 categories: deactivating asks for confirmation (204) and leaves the row inactive', async ({ page }) => {
    await page.goto('/files/categories')
    await page.getByRole('button', { name: 'Filter' }).click()
    await page.locator('#file-category-filter-code').fill(`_${RUN}`)
    await page.getByRole('button', { name: 'Apply' }).click()
    for (const code of [PUBLIC_CATEGORY, PRIVATE_CATEGORY]) {
      const row = page.getByTestId(`file-category-row-${code}`)
      const deactivated = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().includes('/api/v1/files/categories/'))
      await row.getByTestId(`file-category-deactivate-${code}`).click()
      await expect(page.getByRole('alertdialog')).toContainText(code)
      await confirm(page)
      expect((await deactivated).status()).toBe(204)
      await expect(row).toContainText(/inactive/i)
    }
  })
  test('TC-FE-FILE-007 browser: the default list is createdAt,desc; a File name header click re-reads with sort=fileName,asc and aria-sort', async ({ page }) => {
    const isList = (url: string) => new URL(url).pathname === '/api/v1/files'
    const initial = page.waitForRequest((r) => r.method() === 'GET' && isList(r.url()) && new URL(r.url()).searchParams.has('moduleCode'))
    await setScope(page)
    expect(new URL((await initial).url()).searchParams.get('sort')).toBe('createdAt,desc')
    const header = page.getByRole('columnheader', { name: /^file name$/i })
    await expect(header).toHaveAttribute('aria-sort', 'none')

    const sorted = page.waitForRequest(
      (r) => r.method() === 'GET' && isList(r.url()) && new URL(r.url()).searchParams.get('sort') === 'fileName,asc'
    )
    await header.getByRole('button').click()
    await sorted
    await expect(header).toHaveAttribute('aria-sort', 'ascending')
    await expect(page.getByRole('columnheader', { name: /^uploaded$/i })).toHaveAttribute('aria-sort', 'none')
  })
})
