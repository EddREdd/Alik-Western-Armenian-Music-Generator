"use client"

export type GenerationModel = "V4" | "V4_5" | "V4_5PLUS" | "V5"
export type InternalJobStatus =
  | "DRAFT"
  | "VALIDATED"
  | "SUBMITTED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "FAILED"
  | "RETRY_PENDING"
  | "EXPIRED"

export interface GenerationTrack {
  id: string
  providerMusicId: string | null
  trackIndex: number | null
  audioUrl: string | null
  streamAudioUrl: string | null
  imageUrl: string | null
  localAudioUrl: string | null
  localImageUrl: string | null
  lyricsOrPrompt: string | null
  title: string | null
  tags: string[]
  durationSeconds: number | null
  providerCreateTime: string | null
  assetExpiryAt: string | null
  selectedFlag: boolean | null
  createdAt: string | null
}

export interface GenerationJob {
  id: string
  projectId: string
  templateId: string | null
  lyricId: string | null
  lyricTitle: string | null
  sourceType: "MANUAL" | "TEMPLATE" | "SCHEDULED"
  internalStatus: InternalJobStatus
  providerStatus: string | null
  providerTaskId: string | null
  promptFinal: string | null
  styleFinal: string | null
  titleFinal: string | null
  customMode: boolean | null
  instrumental: boolean | null
  model: GenerationModel | null
  errorCode: string | null
  errorMessage: string | null
  createdAt: string | null
  updatedAt: string | null
  submittedAt: string | null
  completedAt: string | null
  failedAt: string | null
  tracks: GenerationTrack[]
}

export interface GenerationJobListItem {
  id: string
  projectId: string
  templateId: string | null
  lyricId: string | null
  lyricTitle: string | null
  sourceType: "MANUAL" | "TEMPLATE" | "SCHEDULED"
  internalStatus: InternalJobStatus
  providerStatus: string | null
  providerTaskId: string | null
  titleFinal: string | null
  model: GenerationModel | null
  createdAt: string | null
  updatedAt: string | null
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

export class ApiRequestError extends Error {
  validationErrors?: Record<string, string>

  constructor(message: string, validationErrors?: Record<string, string>) {
    super(message)
    this.name = "ApiRequestError"
    this.validationErrors = validationErrors
  }
}

interface PageResponse<T> {
  content: T[]
}

export interface CreateGenerationPayload {
  projectId: string
  lyricId?: string | null
  title: string
  lyrics: string
  stylePrompt: string
  instrumental: boolean
  model: GenerationModel
}

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
    if (body && "message" in body) {
      const validationErrors =
        "validationErrors" in body && body.validationErrors
          ? body.validationErrors
          : undefined

      const firstValidationMessage =
        validationErrors && Object.keys(validationErrors).length > 0
          ? validationErrors[Object.keys(validationErrors)[0]]
          : undefined

      throw new ApiRequestError(
        firstValidationMessage || body.message || "Request failed",
        validationErrors,
      )
    }

    throw new ApiRequestError("Request failed")
  }

  if (!body || !("data" in body)) {
    throw new Error("Backend returned an unexpected response")
  }

  return body.data
}

export async function createGenerationJob(
  payload: CreateGenerationPayload,
): Promise<GenerationJob> {
  const promptFinal = payload.lyrics.trim() || payload.stylePrompt.trim()

  return apiRequest<GenerationJob>("/api/v1/generation-jobs", {
    method: "POST",
    body: JSON.stringify({
      projectId: payload.projectId.trim(),
      templateId: null,
      lyricId: payload.lyricId ?? null,
      sourceType: "MANUAL",
      promptFinal,
      styleFinal: payload.stylePrompt.trim(),
      titleFinal: payload.title.trim(),
      customMode: true,
      instrumental: payload.instrumental,
      model: payload.model,
    }),
  })
}

export async function submitGenerationJob(jobId: string): Promise<GenerationJob> {
  return apiRequest<GenerationJob>(`/api/v1/generation-jobs/${jobId}/submit`, {
    method: "POST",
  })
}

export async function getGenerationJob(jobId: string): Promise<GenerationJob> {
  return apiRequest<GenerationJob>(`/api/v1/generation-jobs/${jobId}`)
}

export async function deleteGenerationJob(jobId: string): Promise<void> {
  await apiRequest<string>(`/api/v1/generation-jobs/${jobId}`, {
    method: "DELETE",
  })
}

export async function listGenerationJobSummaries(
  projectId: string,
): Promise<GenerationJobListItem[]> {
  const page = await apiRequest<PageResponse<GenerationJobListItem>>(
    `/api/v1/generation-jobs?projectId=${encodeURIComponent(projectId)}&page=0&size=50`,
  )
  return page.content ?? []
}

export async function listGenerationJobs(projectId: string): Promise<GenerationJob[]> {
  const summaries = await listGenerationJobSummaries(projectId)
  return Promise.all(summaries.map((summary) => getGenerationJob(summary.id)))
}
