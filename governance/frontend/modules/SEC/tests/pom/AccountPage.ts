/** tm-F1 POM: the topbar account menu, `/account/profile` (SCR-SEC-011) with its drawers, and `/account/change-password` (SCR-SEC-012). */
import type { Locator, Page } from '@playwright/test'

export class AccountPage {
  private readonly page: Page
  readonly menuTrigger: Locator
  readonly menu: Locator
  readonly menuProfile: Locator
  readonly menuPassword: Locator
  readonly profile: Locator
  readonly edit: Locator
  readonly photoChange: Locator
  readonly changePassword: Locator
  readonly current: Locator
  readonly newPassword: Locator
  readonly confirm: Locator
  readonly submit: Locator
  readonly forced: Locator
  readonly forcedSignOut: Locator

  constructor(page: Page) {
    this.page = page
    this.menuTrigger = page.getByTestId('topbar-account')
    this.menu = page.getByTestId('topbar-account-menu')
    this.menuProfile = page.getByTestId('topbar-account-profile')
    this.menuPassword = page.getByTestId('topbar-account-password')
    this.profile = page.getByTestId('account-profile')
    this.edit = page.getByTestId('account-edit')
    this.photoChange = page.getByTestId('account-photo-change')
    this.changePassword = page.getByTestId('account-change-password')
    this.current = page.getByTestId('account-password-current')
    this.newPassword = page.getByTestId('account-password-new')
    this.confirm = page.getByTestId('account-password-confirm')
    this.submit = page.getByTestId('account-password-submit')
    this.forced = page.getByTestId('account-forced-change')
    this.forcedSignOut = page.getByTestId('account-forced-sign-out')
  }

  editField(name: 'fullNameAr' | 'fullNameEn' | 'phone' | 'jobTitleAr' | 'jobTitleEn' | 'preferredLocale'): Locator {
    return this.page.getByTestId(`account-${name}`)
  }

  /** The Input primitive links its message through `aria-describedby`. */
  async messageOf(field: Locator): Promise<Locator> {
    const describedBy = await field.getAttribute('aria-describedby')
    return this.page.locator(`#${describedBy}`)
  }

  async fillPasswords(current: string, next: string, confirm = next): Promise<void> {
    await this.current.fill(current)
    await this.newPassword.fill(next)
    await this.confirm.fill(confirm)
  }
}
