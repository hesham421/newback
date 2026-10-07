/** E2E environment: the one password variable, the tenants, the users the setup provisions and their stored auth states. */
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export const password = process.env.ERP_E2E_ADMIN_PASSWORD ?? ''
export const backendUrl = process.env.ERP_BACKEND_URL || 'http://localhost:7272'
export const frontendUrl = process.env.E2E_BASE_URL || 'http://localhost:4200'
export const e2eTenant = process.env.ERP_E2E_TENANT_CODE || 'E2E_T2'
export const PLATFORM = 'PLATFORM'

export const USERS = {
  admin: 'admin',
  e2eAdmin: 'e2eadmin',
  all: 'e2e.all',
  none: 'e2e.none',
} as const

export const ROLES = { all: 'E2E_ALL', none: 'E2E_NONE' } as const

/** The eight catalog modules of S2 §D (the registry also lists the reference app's `APP`). */
export const CATALOG_MODULES = ['SEC', 'MDL', 'CU', 'FILE', 'NOTIF', 'PLATFORM', 'AUDIT', 'SEQUENCE'] as const

const AUTH_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', '.auth')

export const STATE = {
  admin: path.join(AUTH_DIR, 'admin.json'),
  all: path.join(AUTH_DIR, 'e2e-all.json'),
  none: path.join(AUTH_DIR, 'e2e-none.json'),
} as const

export const STORAGE_KEYS = {
  token: 'avelynq_access_token',
  tenant: 'avelynq_tenant_code',
  lang: 'avelynq_lang',
} as const

export const hasPassword = password !== ''
export const MISSING_ENV_REASON = 'ERP_E2E_ADMIN_PASSWORD is not exported'
