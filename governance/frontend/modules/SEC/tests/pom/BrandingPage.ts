/** tm-F2 POM: the shell brand header (mark, separator, tenant logo, word-mark), the sign-in pages' tenant logo, and the accent. */
import type { Locator, Page } from '@playwright/test'

export class BrandingPage {
  private readonly page: Page
  readonly mark: Locator
  readonly separator: Locator
  readonly tenantLogo: Locator
  readonly wordmark: Locator
  readonly authLogo: Locator
  readonly asideMark: Locator
  readonly brandAside: Locator
  readonly mobileBrand: Locator
  readonly toasts: Locator

  constructor(page: Page) {
    this.page = page
    this.mark = page.getByTestId('sidebar-brand-mark')
    this.separator = page.getByTestId('sidebar-brand-separator')
    this.tenantLogo = page.getByTestId('tenant-logo')
    this.wordmark = page.getByTestId('sidebar-brand-wordmark')
    this.authLogo = page.getByTestId('auth-tenant-logo')
    this.asideMark = page.locator('.avl-split__aside img')
    this.brandAside = page.getByTestId('auth-brand-aside')
    this.mobileBrand = page.getByTestId('auth-mobile-brand')
    this.toasts = page.getByTestId('toast-container')
  }

  async accent(): Promise<string> {
    return this.page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--color-accent-tenant').trim())
  }

  /** Ctrl+B toggles the desktop rail (the shell's own shortcut). */
  async toggleRail(): Promise<void> {
    await this.page.keyboard.press('Control+b')
  }

  /** Opens a screen from the menu: expands its module group, then follows the link. */
  async openFromMenu(route: string): Promise<Locator> {
    const link = this.page.locator(`a[href="${route}"]`)
    const groupId = await link.evaluate((element) => element.closest('ul')?.id ?? '')
    const disclosure = this.page.locator(`button[aria-controls="${groupId}"]`)
    if ((await disclosure.getAttribute('aria-expanded')) !== 'true') await disclosure.click()
    await link.click()
    return link
  }

  /** The background colour of the active entry's marker dot. */
  async activeMarkerColour(link: Locator): Promise<string> {
    return link.evaluate((element) => getComputedStyle(element.querySelector('span[aria-hidden="true"]') as Element).backgroundColor)
  }

  /** From now on (same document), records the `src` of every shell tenant logo that appears. */
  async watchShellLogo(): Promise<void> {
    await this.page.evaluate(() => {
      const seen: string[] = []
      ;(window as unknown as { __shellLogosSeen: string[] }).__shellLogosSeen = seen
      new MutationObserver(() => {
        const logo = document.querySelector('[data-testid="tenant-logo"]')
        if (logo) seen.push(logo.getAttribute('src') ?? '')
      }).observe(document.body, { subtree: true, childList: true, attributes: true })
    })
  }

  async shellLogosSeen(): Promise<string[]> {
    return this.page.evaluate(() => (window as unknown as { __shellLogosSeen: string[] }).__shellLogosSeen)
  }

  /** The button's content fits inside it (not clipped) and the button ends inside the viewport. */
  async fitsItsButton(button: Locator): Promise<boolean> {
    return button.evaluate((element) => {
      const box = element.getBoundingClientRect()
      return element.scrollWidth <= element.clientWidth && box.left >= 0 && box.right <= document.documentElement.clientWidth
    })
  }

  async hasHorizontalScroll(): Promise<boolean> {
    return this.page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  }
}
