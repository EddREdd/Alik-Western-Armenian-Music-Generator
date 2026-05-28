"use client"

import { apiFetchJson } from "@/lib/api-client"

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

interface PageResponse<T> {
  content: T[]
}

export interface CreateGenerationPayload {
  lyricId?: string | null
  titleFinal: string
  promptFinal: string
  styleFinal: string
  sourceType: "MANUAL" | "TEMPLATE" | "SCHEDULED"
  customMode: boolean
}

export { ApiRequestError } from "@/lib/api-client"

export async function createGenerationJob(
  payload: CreateGenerationPayload,
): Promise<GenerationJob> {
  return apiFetchJson<GenerationJob>("/api/v1/generation-jobs", {
    method: "POST",
    body: JSON.stringify({
      templateId: null,
      lyricId: payload.lyricId ?? null,
      sourceType: payload.sourceType,
      promptFinal: payload.promptFinal,
      styleFinal: payload.styleFinal,
      titleFinal: payload.titleFinal,
      customMode: payload.customMode,
    }),
  })
}

export async function submitGenerationJob(jobId: string): Promise<GenerationJob> {
  return apiFetchJson<GenerationJob>(`/api/v1/generation-jobs/${jobId}/submit`, {
    method: "POST",
  })
}

export async function getGenerationJob(jobId: string): Promise<GenerationJob> {
  return apiFetchJson<GenerationJob>(`/api/v1/generation-jobs/${jobId}`)
}

export async function reconcileGenerationJob(jobId: string): Promise<GenerationJob> {
  return apiFetchJson<GenerationJob>(`/api/v1/generation-jobs/${jobId}/reconcile-now`, {
    method: "POST",
  })
}

export async function deleteGenerationJob(jobId: string): Promise<void> {
  await apiFetchJson<string>(`/api/v1/generation-jobs/${jobId}`, {
    method: "DELETE",
  })
}

export async function listGenerationJobSummaries(): Promise<GenerationJobListItem[]> {
  const page = await apiFetchJson<PageResponse<GenerationJobListItem>>(
    `/api/v1/generation-jobs?page=0&size=50`,
  )
  return page.content ?? []
}

export async function listGenerationJobs(): Promise<GenerationJob[]> {
  const summaries = await listGenerationJobSummaries()
  return Promise.all(summaries.map((summary) => getGenerationJob(summary.id)))
}
