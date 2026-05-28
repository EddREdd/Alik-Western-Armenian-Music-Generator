"use client"

export interface ApiSuccess<T> {
  success: boolean
  timestamp: string
  data: T
}

export interface ApiErrorBody {
  timestamp?: string
  status?: number
  error?: string
  code?: string
  message?: string
  path?: string
  validationErrors?: Record<string, string>
}

export class ApiRequestError extends Error {
  validationErrors?: Record<string, string>

  constructor(message: string, validationErrors?: Record<string, string>) {
    super(message)
    this.name = "ApiRequestError"
    this.validationErrors = validationErrors
  }
}

/** Relative `/api/...` in production; absolute only when `NEXT_PUBLIC_BACKEND_URL` is set. */
export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`
  const configuredBaseUrl = process.env.NEXT_PUBLIC_BACKEND_URL?.trim()
  if (!configuredBaseUrl) {
    return normalizedPath
  }
  return `${configuredBaseUrl.replace(/\/+$/, "")}${normalizedPath}`
}

export function buildJsonHeaders(sessionToken?: string | null): Headers {
  const headers = new Headers()
  headers.set("Content-Type", "application/json")
  if (sessionToken) {
    headers.set("X-Session-Token", sessionToken)
  }
  return headers
}

export function firstValidationMessage(
  validationErrors?: Record<string, string>,
): string | undefined {
  if (!validationErrors) {
    return undefined
  }
  const keys = Object.keys(validationErrors)
  if (keys.length === 0) {
    return undefined
  }
  return validationErrors[keys[0]]
}

export async function parseApiResponse<T>(response: Response): Promise<T> {
  const body = (await response.json().catch(() => null)) as ApiSuccess<T> | ApiErrorBody | null

  if (!response.ok) {
    const validationErrors =
      body && "validationErrors" in body && body.validationErrors ? body.validationErrors : undefined
    const message =
      firstValidationMessage(validationErrors) ||
      (body && "message" in body && body.message ? body.message : undefined) ||
      "Request failed"
    throw new ApiRequestError(message, validationErrors)
  }

  if (!body || !("data" in body)) {
    throw new Error("Backend returned an unexpected response")
  }

  return body.data
}

export function toApiRequestError(error: unknown): ApiRequestError {
  if (error instanceof ApiRequestError) {
    return error
  }
  if (error instanceof TypeError) {
    return new ApiRequestError(
      "Cannot reach backend API. Please check deployment API configuration.",
    )
  }
  if (error instanceof Error) {
    return new ApiRequestError(error.message)
  }
  return new ApiRequestError("Request failed")
}

export async function apiFetchJson<T>(
  path: string,
  init?: RequestInit,
  sessionToken?: string | null,
): Promise<T> {
  const headers = new Headers(init?.headers)
  const jsonHeaders = buildJsonHeaders(sessionToken)
  jsonHeaders.forEach((value, key) => {
    if (!headers.has(key)) {
      headers.set(key, value)
    }
  })

  let response: Response
  try {
    response = await fetch(buildApiUrl(path), {
      ...init,
      headers,
      cache: "no-store",
    })
  } catch (error) {
    throw toApiRequestError(error)
  }

  return parseApiResponse<T>(response)
}
