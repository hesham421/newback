/** Grants one screen and all its actions to PLATFORM SYS_ADMIN through the Roles grant tree; a no-op when already granted. */
import { expect, type Page } from '@playwright/test'

export async function grantScreenToSysAdmin(page: Page, pageCode: string): Promise<void> {
  await page.goto('/security/roles')
  await page.getByRole('button', { name: /SYS_ADMIN/ }).click()
  await expect(page).toHaveURL(/editId=\d+/)
  await page.getByRole('button', { name: 'Expand all' }).click()
  const screenNode = page.getByTestId(`role-grant-screen-${pageCode}`)
  await expect(screenNode).toBeVisible()

  let isStaged = false
  /* ⚠ A screen whose only action is its VIEW gateway (AUDIT_EVENTS) renders no "Tick all" — tick the screen box itself. */
  const gateway = screenNode.getByRole('checkbox').first()
  if (!(await gateway.isChecked())) {
    await gateway.click()
    isStaged = true
  }
  const tickAll = screenNode.getByRole('button', { name: 'Tick all' })
  if (await tickAll.isVisible()) {
    await tickAll.click()
    isStaged = true
  }
  if (!isStaged) return

  const saved = page.waitForResponse(
    (r) => r.request().method() === 'POST' && /\/api\/v1\/sec\/roles\/\d+\/actions$/.test(r.url())
  )
  await page.getByRole('button', { name: 'Save permissions' }).click()
  expect((await saved).ok()).toBe(true)
  await expect(page.getByText(/Saved \d+ permissions\./)).toBeVisible()
}
