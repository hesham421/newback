/** tm-F1 harness (API only, never the UI): PLATFORM staff users with profile fields, photos, admin-set passwords and extra sessions. */
import { expect, type APIRequestContext } from '@playwright/test'
import { backendUrl, password, PLATFORM, USERS } from './env.ts'
import { apiSignIn, bearer } from './session.ts'

/** Two different valid 2×2 PNGs (the server detects the type from the bytes) and an SVG it refuses for photos. */
export const PHOTO_A = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEklEQVR4nGM4ISd3Qk6OAUIBAB8mBBG4glMAAAAAAElFTkSuQmCC',
  'base64'
)
export const PHOTO_B = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEklEQVR4nGOQkzshJ3eCAUIBABnWBBFGh5IEAAAAAElFTkSuQmCC',
  'base64'
)
export const SVG = Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="2" height="2"><rect width="2" height="2"/></svg>')

/** A policy-valid password other than the one every harness user starts with (derived, no second secret). */
export const otherPassword = (suffix: string): string => `${password}x${suffix}`

export interface StaffUserInput {
  readonly username: string
  readonly fullNameEn?: string
  readonly phone?: string
  readonly jobTitleAr?: string
  readonly jobTitleEn?: string
  /** Omitted = the 1.3.0 default (`true`): the user must change the password at the first sign-in. */
  readonly requireChangeAtNextLogin?: boolean
}

export async function adminToken(request: APIRequestContext): Promise<string> {
  return apiSignIn(request, PLATFORM, USERS.admin)
}

export async function createStaffUser(request: APIRequestContext, token: string, input: StaffUserInput): Promise<number> {
  const response = await request.post(`${backendUrl}/api/v1/sec/users`, {
    headers: bearer(token),
    data: {
      username: input.username,
      email: `${input.username}@example.test`,
      fullNameAr: 'مستخدم الاختبار',
      fullNameEn: input.fullNameEn ?? `E2E ${input.username}`,
      password,
      phone: input.phone,
      jobTitleAr: input.jobTitleAr,
      jobTitleEn: input.jobTitleEn,
      requireChangeAtNextLogin: input.requireChangeAtNextLogin,
    },
  })
  expect(response.status(), `create ${input.username}`).toBe(201)
  return ((await response.json()) as { data: { userPk: number } }).data.userPk
}

export async function setUserPhoto(request: APIRequestContext, token: string, userPk: number, png: Buffer): Promise<string> {
  const response = await request.put(`${backendUrl}/api/v1/sec/users/${userPk}/photo`, {
    headers: bearer(token),
    multipart: { file: { name: 'photo.png', mimeType: 'image/png', buffer: png } },
  })
  expect(response.status(), `photo of ${userPk}`).toBe(200)
  return ((await response.json()) as { data: { photoUrl: string } }).data.photoUrl
}

export async function myUserPk(request: APIRequestContext, token: string): Promise<number> {
  const response = await request.get(`${backendUrl}/api/v1/sec/me`, { headers: bearer(token) })
  expect(response.status()).toBe(200)
  return ((await response.json()) as { data: { userPk: number } }).data.userPk
}

/** Polls until a token is refused (its session was ended server-side). */
export async function expectTokenEnded(request: APIRequestContext, token: string): Promise<void> {
  await expect
    .poll(async () => (await request.get(`${backendUrl}/api/v1/sec/menu`, { headers: bearer(token) })).status(), {
      intervals: [250, 500, 1_000, 2_000],
      timeout: 15_000,
    })
    .toBe(401)
}
