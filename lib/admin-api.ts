"use client"

import { getStoredSessionToken } from "@/lib/auth-api"

const backendBaseUrl =
  process.env.NEXT_PUBLIC_BACKEND_URL?.replace(/\/+$/, "") ??
  "http://localhost:8080"

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
  const response = await fetch(`${backendBaseUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(sessionToken ? { "X-Session-Token": sessionToken } : {}),
      ...(init?.headers ?? {}),
    },
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

export async function listAdminLyrics(keyword?: string): Promise<AdminLyricSummary[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ""
  return adminRequest(`/api/v1/admin/lyrics${query}`)
}

export async function listAdminSongs(keyword?: string): Promise<AdminSongSummary[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ""
  return adminRequest(`/api/v1/admin/songs${query}`)
}
