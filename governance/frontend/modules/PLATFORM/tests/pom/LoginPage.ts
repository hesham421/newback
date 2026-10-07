/** POM for `/login` (SCR-SEC-001): tenant, credentials, submit, and the inline tenant message. */
import type { Locator, Page } from '@playwright/test'

export class LoginPage {
  private readonly page: Page
  readonly form: Locator
  readonly tenant: Locator
  readonly username: Locator
  readonly password: Locator
  readonly submit: Locator

  constructor(page: Page) {
    this.page = page
    this.form = page.getByTestId('login-form')
    this.tenant = page.getByTestId('login-tenant')
    this.username = page.getByTestId('login-username')
    this.password = page.getByTestId('login-password')
    this.submit = page.getByTestId('login-submit')
  }

  async goto(): Promise<void> {
    await this.page.goto('/login')
    await this.form.waitFor()
  }

  async signIn(tenantCode: string, username: string, password: string): Promise<void> {
    await this.tenant.fill(tenantCode)
    await this.username.fill(username)
    await this.password.fill(password)
    await this.submit.click()
  }

  /** The Input primitive links its message through `aria-describedby`. */
  async tenantMessage(): Promise<Locator> {
    const describedBy = await this.tenant.getAttribute('aria-describedby')
    return this.page.locator(`#${describedBy}`)
  }
}
