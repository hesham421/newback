/** POM for the authenticated shell: Topbar (tenant, language, inbox bell, sign-out), sidebar links, toasts and the 403 view. */
import type { Locator, Page } from '@playwright/test'

export class ShellPage {
  private readonly page: Page
  readonly tenant: Locator
  readonly language: Locator
  readonly signOut: Locator
  readonly inboxBell: Locator
  readonly toasts: Locator

  constructor(page: Page) {
    this.page = page
    this.tenant = page.getByTestId('topbar-tenant')
    this.language = page.getByTestId('topbar-language')
    this.signOut = page.getByTestId('topbar-sign-out')
    this.inboxBell = page.getByTestId('topbar-inbox-bell')
    this.toasts = page.getByTestId('toast-container')
  }

  /** Present, not visible: a module group may render collapsed in the sidebar. */
  navLink(route: string): Locator {
    return this.page.locator(`a[href="${route}"]`)
  }

  forbiddenFor(pageCode: string): Locator {
    return this.page.getByText(`Required screen: ${pageCode}`)
  }
}
