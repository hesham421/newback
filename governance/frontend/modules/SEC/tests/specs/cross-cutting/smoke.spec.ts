/** XCUT harness smoke: backend health (tenant-exempt actuator), dev-proxy forwarding (no CORS), login screen render (steps 01, 12). */
import { expect, test } from '@playwright/test'
import { backendUrl } from '../../pom/env.ts'
import { LoginPage } from '../../pom/LoginPage.ts'

test('TC-FE-XCUT-009 the reference app reports UP on /actuator/health without a tenant header', async ({ request }) => {
  const response = await request.get(`${backendUrl}/actuator/health`)
  expect(response.status()).toBe(200)
  const body = (await response.json()) as { status?: string }
  expect(body.status).toBe('UP')
})

test('TC-FE-XCUT-010 the dev proxy forwards /api same-origin to the backend (anonymous menu → 401 envelope)', async ({ request }) => {
  const response = await request.get('/api/v1/sec/menu')
  expect(response.status()).toBe(401)
  const body = (await response.json()) as { success?: boolean; error?: { code?: string } }
  expect(body.success).toBe(false)
  expect(body.error?.code).toBe('SEC-401-INVALID-CREDENTIALS')
})

test('TC-FE-XCUT-011 the login page renders the form with the Tenant field pre-filled from VITE_TENANT_CODE', async ({ page }) => {
  const login = new LoginPage(page)
  await login.goto()
  await expect(login.form).toBeVisible()
  await expect(login.tenant).toHaveValue(process.env.VITE_TENANT_CODE || 'PLATFORM')
})
