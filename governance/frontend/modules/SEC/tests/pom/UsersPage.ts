/** tm-F1 POM for SCR-SEC-004: the users list (quick search, Columns menu, row avatar) and the user drawer's photo / set-password second levels. */
import { expect, type Locator, type Page } from '@playwright/test'

export class UsersPage {
  private readonly page: Page
  readonly quickSearch: Locator
  readonly columnsMenu: Locator
  readonly photoRow: Locator
  readonly photoChange: Locator
  readonly setPassword: Locator
  readonly ownHint: Locator
  readonly passwordDrawer: Locator
  readonly passwordNew: Locator
  readonly passwordConfirm: Locator
  readonly passwordRequireChange: Locator
  readonly passwordSubmit: Locator
  readonly setPasswordConfirm: Locator
  readonly photoDrawer: Locator
  readonly photoFile: Locator
  readonly photoSave: Locator
  readonly photoRemove: Locator
  readonly confirmDialogConfirm: Locator
  readonly confirmDialogCancel: Locator

  constructor(page: Page) {
    this.page = page
    this.quickSearch = page.locator('#user-quick-search')
    this.columnsMenu = page.getByTestId('users-columns-menu')
    this.photoRow = page.getByTestId('user-photo-row')
    this.photoChange = page.getByTestId('user-photo-change')
    this.setPassword = page.getByTestId('user-set-password')
    this.ownHint = page.getByTestId('user-set-password-own-hint')
    this.passwordDrawer = page.getByTestId('user-password-drawer')
    this.passwordNew = page.getByTestId('user-password-new')
    this.passwordConfirm = page.getByTestId('user-password-confirm')
    this.passwordRequireChange = page.getByTestId('user-password-require-change')
    this.passwordSubmit = page.getByTestId('user-password-submit')
    this.setPasswordConfirm = page.getByTestId('user-set-password-confirm')
    this.photoDrawer = page.getByTestId('photo-drawer')
    this.photoFile = page.getByTestId('photo-file')
    this.photoSave = page.getByTestId('photo-save')
    this.photoRemove = page.getByTestId('photo-remove')
    this.confirmDialogConfirm = page.getByTestId('confirm-dialog-confirm')
    this.confirmDialogCancel = page.getByTestId('confirm-dialog-cancel')
  }

  /** Opens the list filtered by the quick search (full name) and waits for that search's answer. */
  async search(fullName: string): Promise<void> {
    const isSearch = (r: { url(): string }) => r.url().endsWith('/api/v1/sec/users/search')
    const first = this.page.waitForResponse(isSearch)
    await this.page.goto('/security/users')
    await first
    const filtered = this.page.waitForResponse((r) => isSearch(r) && (r.request().postData() ?? '').includes(fullName))
    await this.quickSearch.fill(fullName)
    await this.quickSearch.press('Enter')
    expect((await filtered).ok()).toBe(true)
  }

  row(username: string): Locator {
    return this.page.getByRole('row').filter({ hasText: `@${username}` })
  }

  viewRow(testId: string): Locator {
    return this.page.getByTestId(testId)
  }

  /** The Input primitive links its message through `aria-describedby`. */
  async messageOf(field: Locator): Promise<Locator> {
    const describedBy = await field.getAttribute('aria-describedby')
    return this.page.locator(`#${describedBy}`)
  }
}
