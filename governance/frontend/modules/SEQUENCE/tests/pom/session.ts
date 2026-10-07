/** Session helpers: hardened UI sign-in, API sign-in for setup/harness calls, JWT payload decoding and forged tokens. */
import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { LoginPage } from './LoginPage.ts'
import { backendUrl, password, STORAGE_KEYS } from './env.ts'

/* ⚠ The dashboard URL can take longer than the default 5 s on a cold dev server; wait for the login answer, then allow 30 s. */
const LANDING_TIMEOUT_MS = 30_000

export async function useLanguage(page: Page, lang: 'en' | 'ar' = 'en'): Promise<void> {
  await page.addInitScript((value) => window.localStorage.setItem('avelynq_lang', value), lang)
}

export async function signInViaUi(page: Page, tenant: string, username: string): Promise<void> {
  const login = new LoginPage(page)
  if (!(await login.form.isVisible())) await login.goto()
  const answered = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/login'))
  await login.signIn(tenant, username, password)
  expect((await answered).status()).toBe(200)
  await expect(page).toHaveURL(/\/dashboard$/, { timeout: LANDING_TIMEOUT_MS })
}

export async function signOutViaUi(page: Page): Promise<void> {
  /* ⚠ An open drawer's scrim covers the Topbar; leave the screen first. */
  await page.goto('/dashboard')
  const loggedOut = page.waitForResponse((r) => r.url().endsWith('/api/v1/sec/auth/logout'))
  await page.getByTestId('topbar-sign-out').click()
  await loggedOut
  await expect(page).toHaveURL(/\/login$/, { timeout: LANDING_TIMEOUT_MS })
}

export async function apiSignIn(request: APIRequestContext, tenant: string, username: string): Promise<string> {
  const response = await request.post(`${backendUrl}/api/v1/sec/auth/login`, {
    headers: { 'X-Tenant-Code': tenant },
    data: { username, password },
  })
  expect(response.status(), `login ${tenant}/${username}`).toBe(200)
  const token = ((await response.json()) as { data: { accessToken: string } }).data.accessToken
  expect(token).toBeTruthy()
  return token
}

export function bearer(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}

export interface JwtClaims {
  sub?: string
  jti?: string
  uid?: number
  tid?: number
  realm?: string
  iat?: number
  exp?: number
}

export function decodeJwt(token: string): JwtClaims {
  const segment = token.split('.')[1] ?? ''
  return JSON.parse(Buffer.from(segment, 'base64url').toString('utf8')) as JwtClaims
}

/** An unsigned token with the given claims: the client decodes it (never verifies), the server would refuse it. */
export function forgeJwt(claims: JwtClaims): string {
  const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString('base64url')
  return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode(claims)}.forged-signature`
}

export async function storedToken(page: Page): Promise<string | null> {
  return page.evaluate((key) => window.localStorage.getItem(key) ?? window.sessionStorage.getItem(key), STORAGE_KEYS.token)
}
