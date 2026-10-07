/** POM for the customer portal (`/customer/*`, v2-04): register, login, verify and reset forms, the banner and the shell. */
import type { Locator, Page } from '@playwright/test'

export const CUSTOMER_TOKEN_KEY = 'avelynq_customer_access_token'

/** A unique, never-delivered address per call; customers cannot be deleted, so every run leaves its own rows behind. */
export function uniqueCustomerEmail(tag: string): string {
  return `e2e.cust.${tag}.${Date.now()}.${Math.floor(Math.random() * 1e6)}@example.test`
}

export class CustomerPortalPage {
  private readonly page: Page
  readonly banner: Locator
  readonly shell: Locator

  constructor(page: Page) {
    this.page = page
    this.banner = page.getByTestId('customer-auth-error')
    this.shell = page.getByTestId('customer-shell')
  }

  field(testId: string): Locator {
    return this.page.getByTestId(testId)
  }

  /** The Input primitive links its message through `aria-describedby`. */
  async messageOf(testId: string): Promise<Locator> {
    const describedBy = await this.field(testId).getAttribute('aria-describedby')
    return this.page.locator(`#${describedBy}`)
  }

  async register(tenant: string, email: string, password: string): Promise<void> {
    await this.page.goto('/customer/register')
    await this.field('customer-register-tenant').fill(tenant)
    await this.field('customer-register-email').fill(email)
    await this.field('customer-register-full-name').fill('E2E Customer')
    await this.field('customer-register-password').fill(password)
    await this.field('customer-register-confirm-password').fill(password)
    await this.field('customer-register-submit').click()
  }

  async openLogin(): Promise<void> {
    await this.page.goto('/customer/login')
    await this.field('customer-login-form').waitFor()
  }

  async submitLogin(tenant: string, email: string, password: string): Promise<void> {
    await this.field('customer-login-tenant').fill(tenant)
    await this.field('customer-login-email').fill(email)
    await this.field('customer-login-password').fill(password)
    await this.field('customer-login-submit').click()
  }

  async storedCustomerToken(): Promise<string | null> {
    return this.page.evaluate((key) => window.localStorage.getItem(key), CUSTOMER_TOKEN_KEY)
  }
}
