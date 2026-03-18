"use client"

const configuredBackendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_URL?.trim()
const backendBaseUrl = configuredBackendBaseUrl
  ? configuredBackendBaseUrl.replace(/\/+$/, "")
  : ""

const sessionStorageKey = "balians.session-token"

interface ApiSuccess<T> {
  success: boolean
  timestamp: string
  data: T
}

interface ApiError {
  timestamp: string
  status: number
  error: string
  code: string
  message: string
  path: string
  validationErrors?: Record<string, string>
}

export interface AuthUser {
  id: string
  email: string
  emailVerified: boolean
  hasPassword: boolean
  googleLinked: boolean
  admin: boolean
  unlimitedCredits: boolean
  creditsRemaining: number | null
  creditsUsed: number | null
  songsGenerated: number | null
  frozen: boolean
  freezeReason?: string | null
}

export interface AuthSession {
  user: AuthUser
  sessionToken: string
  sessionExpiresAt: string
}

export interface OtpChallenge {
  email: string
  purpose: string
  expiresAt: string
  devOtpPreview?: string | null
}

export interface RegisterPayload {
  email: string
  password: string
  inviteCode: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface ForgotPasswordResetPayload {
  email: string
  resetToken: string
  newPassword: string
  confirmPassword: string
}

async function authRequest<T>(
  path: string,
  init?: RequestInit,
  sessionToken?: string | null,
): Promise<T> {
  const headers = new Headers(init?.headers)
  headers.set("Content-Type", "application/json")

  if (sessionToken) {
    headers.set("X-Session-Token", sessionToken)
  }

  const response = await fetch(`${backendBaseUrl}${path}`, {
    ...init,
    headers,
    cache: "no-store",
  })

  const body = (await response.json().catch(() => null)) as ApiSuccess<T> | ApiError | null

  if (!response.ok) {
    const message =
      body && "message" in body && body.message ? body.message : "Request failed"
    throw new Error(message)
  }

  if (!body || !("data" in body)) {
    throw new Error("Backend returned an unexpected response")
  }

  return body.data
}

export function getStoredSessionToken(): string | null {
  if (typeof window === "undefined") {
    return null
  }
  return window.localStorage.getItem(sessionStorageKey)
}

export function storeSessionToken(sessionToken: string) {
  if (typeof window === "undefined") {
    return
  }
  window.localStorage.setItem(sessionStorageKey, sessionToken)
}

export function clearStoredSessionToken() {
  if (typeof window === "undefined") {
    return
  }
  window.localStorage.removeItem(sessionStorageKey)
}

export async function register(payload: RegisterPayload): Promise<void> {
  await authRequest<string>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export async function verifyRegistration(
  email: string,
  otpCode: string,
): Promise<AuthSession> {
  return authRequest<AuthSession>("/api/v1/auth/register/verify", {
    method: "POST",
    body: JSON.stringify({
      email: email.trim(),
      otpCode: otpCode.trim(),
    }),
  })
}

export async function login(payload: LoginPayload): Promise<AuthSession> {
  return authRequest<AuthSession>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export async function loginWithGoogle(
  idToken: string,
  inviteCode?: string,
): Promise<AuthSession> {
  return authRequest<AuthSession>("/api/v1/auth/google", {
    method: "POST",
    body: JSON.stringify({
      idToken,
      inviteCode: inviteCode?.trim() || null,
    }),
  })
}

export async function getCurrentUser(sessionToken: string): Promise<AuthUser> {
  return authRequest<AuthUser>("/api/v1/auth/me", undefined, sessionToken)
}

export async function requestEmailChange(
  sessionToken: string,
  newEmail: string,
): Promise<OtpChallenge> {
  return authRequest<OtpChallenge>(
    "/api/v1/auth/email/change/request",
    {
      method: "POST",
      body: JSON.stringify({ newEmail: newEmail.trim() }),
    },
    sessionToken,
  )
}

export async function verifyEmailChange(
  sessionToken: string,
  otpCode: string,
): Promise<AuthUser> {
  return authRequest<AuthUser>(
    "/api/v1/auth/email/change/verify",
    {
      method: "POST",
      body: JSON.stringify({ otpCode: otpCode.trim() }),
    },
    sessionToken,
  )
}

export async function changePassword(
  sessionToken: string,
  payload: ChangePasswordPayload,
): Promise<AuthUser> {
  return authRequest<AuthUser>(
    "/api/v1/auth/password/change",
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    sessionToken,
  )
}

export async function requestForgotPasswordCode(email: string): Promise<void> {
  await authRequest<string>("/api/v1/auth/password/forgot/request", {
    method: "POST",
    body: JSON.stringify({
      email: email.trim(),
    }),
  })
}

export async function verifyForgotPasswordCode(email: string, otpCode: string): Promise<string> {
  return authRequest<string>("/api/v1/auth/password/forgot/verify", {
    method: "POST",
    body: JSON.stringify({
      email: email.trim(),
      otpCode: otpCode.trim(),
    }),
  })
}

export async function resetForgotPassword(payload: ForgotPasswordResetPayload): Promise<void> {
  await authRequest<string>("/api/v1/auth/password/forgot/reset", {
    method: "POST",
    body: JSON.stringify({
      email: payload.email.trim(),
      resetToken: payload.resetToken.trim(),
      newPassword: payload.newPassword,
      confirmPassword: payload.confirmPassword,
    }),
  })
}

export async function linkGoogleAccount(
  sessionToken: string,
  idToken: string,
): Promise<AuthUser> {
  return authRequest<AuthUser>(
    "/api/v1/auth/google/link",
    {
      method: "POST",
      body: JSON.stringify({ idToken }),
    },
    sessionToken,
  )
}

export async function unlinkGoogleAccount(sessionToken: string): Promise<AuthUser> {
  return authRequest<AuthUser>(
    "/api/v1/auth/google/unlink",
    {
      method: "POST",
    },
    sessionToken,
  )
}

export async function logout(explicitToken?: string | null): Promise<void> {
  const storedToken = typeof window !== "undefined" ? getStoredSessionToken() : null
  const tokenToUse = explicitToken || storedToken

  try {
    if (tokenToUse) {
      const headers = new Headers()
      headers.set("Content-Type", "application/json")
      headers.set("X-Session-Token", tokenToUse)

      await fetch(`${backendBaseUrl}/api/v1/auth/logout`, {
        method: "POST",
        headers,
        cache: "no-store",
        keepalive: true,
      })
    }
  } catch {
    // Ignore backend logout failures; we'll still clear local session
  } finally {
    clearStoredSessionToken()
  }
}
