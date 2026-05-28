"use client"

const configuredBackendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_URL?.trim()
const backendBaseUrl = configuredBackendBaseUrl
  ? configuredBackendBaseUrl.replace(/\/+$/, "")
  : ""

function getSessionHeader() {
  if (typeof window === "undefined") {
    return null
  }
  return window.localStorage.getItem("balians.session-token")
}

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

export interface LyricVersion {
  versionNumber: number
  title: string
  body: string
  editedAt: string
}

export interface Lyric {
  id: string
  projectId: string
  title: string
  body: string
  currentVersion: number
  locked: boolean
  wordCount: number
  linkedSongIds: string[]
  versions: LyricVersion[]
  language?: string
  publicReadyLibrary?: boolean
  createdAt: string | null
  updatedAt: string | null
}

export interface LyricSummary {
  id: string
  projectId: string
  title: string
  bodyPreview: string
  wordCount: number
  locked: boolean
  linkedSongIds: string[]
  currentVersion: number
  language?: string
  publicReadyLibrary?: boolean
  createdAt: string | null
  updatedAt: string | null
}

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  headers.set("Content-Type", "application/json")

  const sessionToken = getSessionHeader()
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

export async function listMyLyrics(params?: { projectId?: string }): Promise<LyricSummary[]> {
  const projectId = params?.projectId?.trim()
  const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : ""
  return apiRequest<LyricSummary[]>(`/api/v1/lyrics${query}`)
}

// Backwards-compatible alias for older callsites.
export async function listLyrics(projectId: string): Promise<LyricSummary[]> {
  return listMyLyrics({ projectId })
}

export async function getLyric(id: string): Promise<Lyric> {
  return apiRequest<Lyric>(`/api/v1/lyrics/${id}`)
}

export async function listPublicLyrics(params?: {
  keyword?: string
  language?: string
}): Promise<LyricSummary[]> {
  const searchParams = new URLSearchParams()
  if (params?.keyword?.trim()) searchParams.set("keyword", params.keyword.trim())
  if (params?.language?.trim()) searchParams.set("language", params.language.trim())
  const query = searchParams.toString()
  return apiRequest<LyricSummary[]>(`/api/v1/lyrics/ready-library${query ? `?${query}` : ""}`)
}

export async function getPublicLyric(id: string): Promise<Lyric> {
  return apiRequest<Lyric>(`/api/v1/lyrics/ready-library/${encodeURIComponent(id)}`)
}

export async function createLyric(payload: {
  title: string
  body: string
  projectId?: string
  language?: string
}): Promise<Lyric> {
  return apiRequest<Lyric>("/api/v1/lyrics", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export async function updateLyric(
  id: string,
  payload: { title: string; body: string },
): Promise<Lyric> {
  return apiRequest<Lyric>(`/api/v1/lyrics/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  })
}

export async function deleteLyric(id: string): Promise<void> {
  await apiRequest<string>(`/api/v1/lyrics/${id}`, {
    method: "DELETE",
  })
}

export async function restoreLyricVersion(
  id: string,
  versionNumber: number,
): Promise<Lyric> {
  return apiRequest<Lyric>(
    `/api/v1/lyrics/${encodeURIComponent(id)}/versions/${versionNumber}/restore`,
    {
      method: "POST",
    },
  )
}
