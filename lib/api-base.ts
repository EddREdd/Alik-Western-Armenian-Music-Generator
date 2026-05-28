/**
 * Resolves the browser API base URL for fetch calls.
 * Production should use same-origin relative `/api/...` (empty base) so Next.js rewrites to the backend.
 */
export function getApiBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_BACKEND_URL?.trim() ?? ""
  if (!configured) {
    return ""
  }

  const normalized = configured.replace(/\/+$/, "")

  if (typeof window === "undefined") {
    return normalized
  }

  try {
    const configuredOrigin = new URL(normalized).origin
    if (configuredOrigin !== window.location.origin) {
      return ""
    }
  } catch {
    return ""
  }

  return normalized
}

export const MAX_SEARCH_KEYWORD_LENGTH = 100

export function normalizeSearchKeyword(value: string | undefined | null): string {
  if (!value) {
    return ""
  }
  return value.trim().slice(0, MAX_SEARCH_KEYWORD_LENGTH)
}

export function buildQueryString(
  params: Record<string, string | undefined | null>,
): string {
  const searchParams = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    const trimmed = value?.trim()
    if (trimmed) {
      searchParams.set(key, trimmed)
    }
  }
  const query = searchParams.toString()
  return query ? `?${query}` : ""
}
