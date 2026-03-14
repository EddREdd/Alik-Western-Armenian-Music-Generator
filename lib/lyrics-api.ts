"use client"

const backendBaseUrl =
  process.env.NEXT_PUBLIC_BACKEND_URL?.replace(/\/+$/, "") ??
  "http://localhost:8080"

function getSessionHeader() {
  if (typeof window === "undefined") {
    return {}
  }
  const token = window.localStorage.getItem("balians.session-token")
  return token ? { "X-Session-Token": token } : {}
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
  createdAt: string | null
  updatedAt: string | null
}

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${backendBaseUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...getSessionHeader(),
      ...(init?.headers ?? {}),
    },
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

export async function listLyrics(projectId: string): Promise<LyricSummary[]> {
  return apiRequest<LyricSummary[]>(
    `/api/v1/lyrics?projectId=${encodeURIComponent(projectId.trim())}`,
  )
}

export async function getLyric(id: string): Promise<Lyric> {
  return apiRequest<Lyric>(`/api/v1/lyrics/${id}`)
}

export async function createLyric(payload: {
  projectId: string
  title: string
  body: string
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
