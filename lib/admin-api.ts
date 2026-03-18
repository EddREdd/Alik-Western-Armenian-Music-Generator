"use client"

import { getStoredSessionToken } from "@/lib/auth-api"

const configuredBackendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_URL?.trim()
const backendBaseUrl = configuredBackendBaseUrl
  ? configuredBackendBaseUrl.replace(/\/+$/, "")
  : ""

interface ApiSuccess<T> {
  success: boolean
  timestamp: string
  data: T
}

interface ApiError {
  message?: string
}

async function adminRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const sessionToken = getStoredSessionToken()
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
    throw new Error(body && "message" in body && body.message ? body.message : "Admin request failed")
  }
  if (!body || !("data" in body)) {
    throw new Error("Backend returned an unexpected admin response")
  }
  return body.data
}

export interface MetricSnapshot {
  daily: number
  weekly: number
  monthly: number
  total: number
}

export interface AdminDashboard {
  songVolume: MetricSnapshot
  registrationTrends: MetricSnapshot
  creditConsumption: MetricSnapshot
  totalUsers: number
  frozenUsers: number
  activeInviteCodes: number
  usedInviteCodes: number
  totalLyrics: number
  totalSongs: number
}

export interface AdminUserSummary {
  id: string
  email: string
  joinDate: string
  generationCount: number | null
  creditsRemaining: number | null
  inviteCode: string | null
  frozen: boolean
  admin: boolean
  unlimitedCredits: boolean
}

export interface AdminUserDetail {
  id: string
  email: string
  joinDate: string
  generationCount: number | null
  creditsRemaining: number | null
  creditsUsed: number | null
  songsGenerated: number | null
  emailVerified: boolean
  inviteCode: string | null
  frozen: boolean
  frozenAt: string | null
  freezeReason: string | null
  admin: boolean
  unlimitedCredits: boolean
  googleEmail: string | null
}

export interface SecurityLog {
  id: string
  userId: string
  email: string
  eventType: string
  details: string
  occurredAt: string
}

export interface AdminInviteCode {
  id: string
  code: string
  active: boolean
  usedByUserId: string | null
  usedByEmail: string | null
  usedAt: string | null
  createdAt: string | null
  lastSentToEmail: string | null
  lastSentAt: string | null
}

export interface AdminLyricSummary {
  id: string
  userId: string | null
  projectId: string
  title: string
  bodyPreview: string | null
  currentVersion: number | null
  locked: boolean
  linkedSongIds: string[]
  updatedAt: string | null
}

export interface AdminSongSummary {
  id: string
  generationJobId: string
  userId: string | null
  projectId: string | null
  title: string | null
  audioUrl: string | null
  streamAudioUrl: string | null
  lyricId: string | null
  lyricTitle: string | null
  createdAt: string | null
  tags: string[] | null
}

export interface ManualActionResponse {
  action: string
  targetId: string
  status: string
  message: string
}

export async function getAdminDashboard(): Promise<AdminDashboard> {
  return adminRequest("/api/v1/admin/dashboard")
}

export async function listAdminUsers(params?: {
  email?: string
  frozenOnly?: boolean
}): Promise<AdminUserSummary[]> {
  const search = new URLSearchParams()
  if (params?.email) search.set("email", params.email)
  if (params?.frozenOnly) search.set("frozenOnly", "true")
  const query = search.toString()
  return adminRequest(`/api/v1/admin/users${query ? `?${query}` : ""}`)
}

export async function getAdminUser(id: string): Promise<AdminUserDetail> {
  return adminRequest(`/api/v1/admin/users/${id}`)
}

export async function getUserSecurityLogs(userId: string): Promise<SecurityLog[]> {
  return adminRequest(`/api/v1/admin/users/${userId}/security-logs`)
}

export async function freezeUser(userId: string, reason: string) {
  return adminRequest(`/api/v1/admin/users/${userId}/freeze`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  })
}

export async function unfreezeUser(userId: string) {
  return adminRequest(`/api/v1/admin/users/${userId}/unfreeze`, {
    method: "POST",
  })
}

export async function listInviteCodes(params?: {
  keyword?: string
  active?: boolean
  used?: boolean
}): Promise<AdminInviteCode[]> {
  const search = new URLSearchParams()
  if (params?.keyword) search.set("keyword", params.keyword)
  if (typeof params?.active === "boolean") search.set("active", String(params.active))
  if (typeof params?.used === "boolean") search.set("used", String(params.used))
  const query = search.toString()
  return adminRequest(`/api/v1/admin/invite-codes${query ? `?${query}` : ""}`)
}

export async function generateInviteCodes(count: number): Promise<AdminInviteCode[]> {
  return adminRequest("/api/v1/admin/invite-codes/generate", {
    method: "POST",
    body: JSON.stringify({ count }),
  })
}

export async function activateInviteCode(inviteId: string) {
  return adminRequest(`/api/v1/admin/invite-codes/${inviteId}/activate`, {
    method: "POST",
  })
}

export async function deactivateInviteCode(inviteId: string) {
  return adminRequest(`/api/v1/admin/invite-codes/${inviteId}/deactivate`, {
    method: "POST",
  })
}

export async function removeInviteCode(inviteId: string): Promise<ManualActionResponse> {
  return adminRequest(`/api/v1/admin/invite-codes/${inviteId}/remove`, {
    method: "POST",
  })
}

export async function sendInviteCodeEmail(
  inviteId: string,
  email: string,
): Promise<ManualActionResponse> {
  return adminRequest(`/api/v1/admin/invite-codes/${inviteId}/send-email`, {
    method: "POST",
    body: JSON.stringify({ email: email.trim() }),
  })
}

export async function listAdminLyrics(keyword?: string): Promise<AdminLyricSummary[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ""
  return adminRequest(`/api/v1/admin/lyrics${query}`)
}

export async function listAdminSongs(keyword?: string): Promise<AdminSongSummary[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ""
  return adminRequest(`/api/v1/admin/songs${query}`)
}
