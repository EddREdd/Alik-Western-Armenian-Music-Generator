"use client"

import { useEffect, useMemo, useState } from "react"
import { MailPlus, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import {
  activateInviteCode,
  deactivateInviteCode,
  freezeUser,
  generateInviteCodes,
  getAdminDashboard,
  listAdminLyrics,
  listAdminReadyLibrary,
  listAdminSongs,
  listAdminUsers,
  listInviteCodes,
  createAdminReadyLibraryLyric,
  deleteAdminReadyLibraryLyric,
  getAdminReadyLibraryLyric,
  updateAdminReadyLibraryLyric,
  removeInviteCode,
  sendInviteCodeEmail,
  unfreezeUser,
  type AdminDashboard,
  type AdminInviteCode,
  type AdminLyricSummary,
  type AdminReadyLibraryLyricDetail,
  type AdminReadyLibraryLyricSummary,
  type AdminSongSummary,
  type AdminUserSummary,
} from "@/lib/admin-api"
import { buildApiUrl } from "@/lib/api-client"
import { t, useUiLanguage } from "@/lib/i18n"

const PAGE_SIZE = 25
const proxyPreferredHosts = new Set([
  "musicfile.removeai.ai",
  "tempfile.aiquickdraw.com",
])

function normalizeHost(host: string): string {
  return host.trim().toLowerCase()
}

function isProxyPreferred(host: string): boolean {
  const normalizedHost = normalizeHost(host)
  for (const allowedHost of proxyPreferredHosts) {
    if (
      normalizedHost === normalizeHost(allowedHost) ||
      normalizedHost.endsWith(`.${normalizeHost(allowedHost)}`)
    ) {
      return true
    }
  }
  return false
}

function toAbsoluteAudioUrl(url: string): string {
  const trimmed = url.trim()
  if (!trimmed) return trimmed
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  return buildApiUrl(trimmed.startsWith("/") ? trimmed : `/${trimmed}`)
}

function toProxyUrl(url: string): string {
  return buildApiUrl(`/api/v1/media/proxy?url=${encodeURIComponent(url)}`)
}

function unwrapProxyUrl(url: string): string | undefined {
  try {
    const parsed = new URL(
      url,
      typeof window !== "undefined" ? window.location.origin : "http://localhost",
    )
    if (!parsed.pathname.endsWith("/api/v1/media/proxy")) {
      return undefined
    }
    const nestedUrl = parsed.searchParams.get("url")
    return nestedUrl?.trim() || undefined
  } catch {
    return undefined
  }
}

function buildAdminSongAudioCandidates(song: AdminSongSummary): string[] {
  const rawCandidates = [song.streamAudioUrl, song.audioUrl].filter(
    (value): value is string => Boolean(value?.trim()),
  )
  const expanded: string[] = []

  for (const rawCandidate of rawCandidates) {
    const absoluteCandidate = toAbsoluteAudioUrl(rawCandidate)
    if (!absoluteCandidate) continue
    expanded.push(absoluteCandidate)

    const unwrapped = unwrapProxyUrl(absoluteCandidate)
    if (unwrapped) {
      expanded.push(unwrapped)
      continue
    }

    if (/^https?:\/\//i.test(absoluteCandidate)) {
      try {
        const parsed = new URL(absoluteCandidate)
        if (isProxyPreferred(parsed.hostname)) {
          expanded.push(toProxyUrl(absoluteCandidate))
        }
      } catch {
        // Keep current candidate only if URL parsing fails.
      }
    }
  }

  return [...new Set(expanded)]
}

function MetricCard({
  title,
  metrics,
}: {
  title: string
  metrics: { daily: number; weekly: number; monthly: number; total: number }
}) {
  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent className="grid grid-cols-2 gap-3 text-sm">
        <div><span className="text-muted-foreground">{t("daily")}</span><div className="text-xl font-semibold">{metrics.daily}</div></div>
        <div><span className="text-muted-foreground">{t("weekly")}</span><div className="text-xl font-semibold">{metrics.weekly}</div></div>
        <div><span className="text-muted-foreground">{t("monthly")}</span><div className="text-xl font-semibold">{metrics.monthly}</div></div>
        <div><span className="text-muted-foreground">{t("total")}</span><div className="text-xl font-semibold">{metrics.total}</div></div>
      </CardContent>
    </Card>
  )
}

function PaginationControls({
  page,
  totalPages,
  onPageChange,
}: {
  page: number
  totalPages: number
  onPageChange: (nextPage: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-between pt-2">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 1}
      >
        {t("previous")}
      </Button>
      <span className="text-xs text-muted-foreground">
        {t("pageOf", { page, total: totalPages })}
      </span>
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages}
      >
        {t("next")}
      </Button>
    </div>
  )
}

export function AdminPage() {
  useUiLanguage()
  const [adminTab, setAdminTab] = useState<"users" | "invites" | "lyrics" | "ready-library" | "songs">("users")
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null)
  const [users, setUsers] = useState<AdminUserSummary[]>([])
  const [invites, setInvites] = useState<AdminInviteCode[]>([])
  const [lyrics, setLyrics] = useState<AdminLyricSummary[]>([])
  const [readyLibraryKeyword, setReadyLibraryKeyword] = useState("")
  const [readyLibraryLanguageFilter, setReadyLibraryLanguageFilter] = useState<"ALL" | "WESTERN_ARMENIAN">("WESTERN_ARMENIAN")
  const [readyLibrary, setReadyLibrary] = useState<AdminReadyLibraryLyricSummary[]>([])
  const [readyLibraryLoading, setReadyLibraryLoading] = useState(false)
  const [readyLibrarySaving, setReadyLibrarySaving] = useState(false)
  const [readyLibraryError, setReadyLibraryError] = useState("")
  const [readyLibraryDialogOpen, setReadyLibraryDialogOpen] = useState(false)
  const [readyLibraryEditingId, setReadyLibraryEditingId] = useState<string | null>(null)
  const [readyLibraryTitle, setReadyLibraryTitle] = useState("")
  const [readyLibraryBody, setReadyLibraryBody] = useState("")
  const readyLibraryLanguage = "WESTERN_ARMENIAN" as const
  const [songs, setSongs] = useState<AdminSongSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [userSearch, setUserSearch] = useState("")
  const [inviteSearch, setInviteSearch] = useState("")
  const [inviteActiveOnly, setInviteActiveOnly] = useState(false)
  const [inviteUsedOnly, setInviteUsedOnly] = useState(false)
  const [lyricSearch, setLyricSearch] = useState("")
  const [songSearch, setSongSearch] = useState("")
  const [usersPage, setUsersPage] = useState(1)
  const [invitesPage, setInvitesPage] = useState(1)
  const [lyricsPage, setLyricsPage] = useState(1)
  const [songsPage, setSongsPage] = useState(1)
  const [inviteEmailDraftById, setInviteEmailDraftById] = useState<Record<string, string>>({})
  const [inviteSendStateById, setInviteSendStateById] = useState<Record<string, "idle" | "sent">>({})
  const [sendingInviteId, setSendingInviteId] = useState<string | null>(null)
  const [removingInviteId, setRemovingInviteId] = useState<string | null>(null)
  const [inviteComposerOpenById, setInviteComposerOpenById] = useState<Record<string, boolean>>({})

  const loadAll = async () => {
    setLoading(true)
    setError("")
    try {
      const [dashboardData, userData, inviteData, lyricData, songData] = await Promise.all([
        getAdminDashboard(),
        listAdminUsers(),
        listInviteCodes(),
        listAdminLyrics(),
        listAdminSongs(),
      ])
      setDashboard(dashboardData)
      setUsers(userData)
      setInvites(inviteData)
      setLyrics(lyricData)
      setSongs(songData)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : t("failedToLoadAdminData"))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadAll()
  }, [])

  const loadReadyLibrary = async () => {
    setReadyLibraryLoading(true)
    setReadyLibraryError("")
    try {
      const items = await listAdminReadyLibrary({
        keyword: readyLibraryKeyword || undefined,
        language: readyLibraryLanguageFilter === "ALL" ? undefined : readyLibraryLanguageFilter,
      })
      setReadyLibrary(items)
    } catch (loadError) {
      setReadyLibraryError(
        loadError instanceof Error ? loadError.message : t("failedToLoadReadyLibrary"),
      )
    } finally {
      setReadyLibraryLoading(false)
    }
  }

  useEffect(() => {
    if (adminTab !== "ready-library") return
    void loadReadyLibrary()
  }, [adminTab, readyLibraryKeyword, readyLibraryLanguageFilter])

  const openCreateReadyLibrary = () => {
    setReadyLibraryEditingId(null)
    setReadyLibraryTitle("")
    setReadyLibraryBody("")
    setReadyLibraryDialogOpen(true)
  }

  const openEditReadyLibrary = async (id: string) => {
    setReadyLibraryError("")
    try {
      const detail = await getAdminReadyLibraryLyric(id)
      setReadyLibraryEditingId(id)
      setReadyLibraryTitle(detail.title ?? "")
      setReadyLibraryBody(detail.body ?? "")
      setReadyLibraryDialogOpen(true)
    } catch (loadError) {
      setReadyLibraryError(loadError instanceof Error ? loadError.message : t("unableToLoadLyric"))
    }
  }

  const handleSaveReadyLibrary = async () => {
    setReadyLibraryError("")
    setReadyLibrarySaving(true)
    try {
      const payload = {
        title: readyLibraryTitle.trim(),
        body: readyLibraryBody,
        language: readyLibraryLanguage,
      }

      if (!payload.title || !payload.body.trim()) {
        setReadyLibraryError(t("titleAndBodyRequired"))
        return
      }

      if (readyLibraryEditingId) {
        await updateAdminReadyLibraryLyric(readyLibraryEditingId, payload)
      } else {
        await createAdminReadyLibraryLyric(payload)
      }

      setReadyLibraryDialogOpen(false)
      await loadReadyLibrary()
    } catch (saveError) {
      setReadyLibraryError(saveError instanceof Error ? saveError.message : t("unableToSaveLyric"))
    } finally {
      setReadyLibrarySaving(false)
    }
  }

  const handleDeleteReadyLibrary = async (id: string) => {
    setReadyLibraryError("")
    setReadyLibrarySaving(true)
    const confirmed = window.confirm(t("confirmDeleteReadyLibraryLyric"))
    if (!confirmed) return
    try {
      await deleteAdminReadyLibraryLyric(id)
      await loadReadyLibrary()
      if (readyLibraryEditingId === id) {
        setReadyLibraryDialogOpen(false)
        setReadyLibraryEditingId(null)
      }
    } catch (deleteError) {
      setReadyLibraryError(deleteError instanceof Error ? deleteError.message : t("unableToDeleteLyric"))
    } finally {
      setReadyLibrarySaving(false)
    }
  }

  const filteredUsers = useMemo(
    () => users.filter((user) => !userSearch || user.email.toLowerCase().includes(userSearch.toLowerCase())),
    [users, userSearch],
  )
  const filteredInvites = useMemo(
    () =>
      invites.filter((invite) =>
        (!inviteSearch ||
          invite.code.toLowerCase().includes(inviteSearch.toLowerCase()) ||
          (invite.usedByEmail ?? "").toLowerCase().includes(inviteSearch.toLowerCase()) ||
          (invite.lastSentToEmail ?? "").toLowerCase().includes(inviteSearch.toLowerCase())) &&
        (!inviteActiveOnly || invite.active) &&
        (!inviteUsedOnly || Boolean(invite.usedByUserId))
      ),
    [invites, inviteSearch, inviteActiveOnly, inviteUsedOnly],
  )
  const filteredLyrics = useMemo(
    () =>
      lyrics.filter((lyric) =>
        !lyricSearch ||
        lyric.title.toLowerCase().includes(lyricSearch.toLowerCase()) ||
        (lyric.bodyPreview ?? "").toLowerCase().includes(lyricSearch.toLowerCase()),
      ),
    [lyrics, lyricSearch],
  )
  const filteredSongs = useMemo(
    () =>
      songs.filter((song) =>
        !songSearch ||
        (song.title ?? "").toLowerCase().includes(songSearch.toLowerCase()) ||
        (song.lyricTitle ?? "").toLowerCase().includes(songSearch.toLowerCase()) ||
        (song.tags ?? []).join(" ").toLowerCase().includes(songSearch.toLowerCase()),
      ),
    [songs, songSearch],
  )

  useEffect(() => {
    setUsersPage(1)
  }, [userSearch, users])

  useEffect(() => {
    setInvitesPage(1)
  }, [inviteSearch, inviteActiveOnly, inviteUsedOnly, invites])

  useEffect(() => {
    setLyricsPage(1)
  }, [lyricSearch, lyrics])

  useEffect(() => {
    setSongsPage(1)
  }, [songSearch, songs])

  const pagedUsers = useMemo(
    () => filteredUsers.slice((usersPage - 1) * PAGE_SIZE, usersPage * PAGE_SIZE),
    [filteredUsers, usersPage],
  )
  const pagedInvites = useMemo(
    () => filteredInvites.slice((invitesPage - 1) * PAGE_SIZE, invitesPage * PAGE_SIZE),
    [filteredInvites, invitesPage],
  )
  const pagedLyrics = useMemo(
    () => filteredLyrics.slice((lyricsPage - 1) * PAGE_SIZE, lyricsPage * PAGE_SIZE),
    [filteredLyrics, lyricsPage],
  )
  const pagedSongs = useMemo(
    () => filteredSongs.slice((songsPage - 1) * PAGE_SIZE, songsPage * PAGE_SIZE),
    [filteredSongs, songsPage],
  )

  const usersTotalPages = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE))
  const invitesTotalPages = Math.max(1, Math.ceil(filteredInvites.length / PAGE_SIZE))
  const lyricsTotalPages = Math.max(1, Math.ceil(filteredLyrics.length / PAGE_SIZE))
  const songsTotalPages = Math.max(1, Math.ceil(filteredSongs.length / PAGE_SIZE))

  const handleFreezeToggle = async (user: AdminUserSummary) => {
    try {
      if (user.frozen) {
        await unfreezeUser(user.id)
      } else {
        const reason = window.prompt(t("freezeUserReason", { email: user.email }))
        if (!reason?.trim()) return
        await freezeUser(user.id, reason.trim())
      }
      setUsers(await listAdminUsers())
      setDashboard(await getAdminDashboard())
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t("failedToUpdateUser"))
    }
  }

  const handleGenerateInvites = async () => {
    const raw = window.prompt(t("howManyInviteCodes"), "10")
    const count = Number(raw)
    if (!Number.isFinite(count) || count <= 0) return
    try {
      await generateInviteCodes(count)
      setInvites(await listInviteCodes())
      setDashboard(await getAdminDashboard())
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t("failedToGenerateInviteCodes"))
    }
  }

  const handleInviteToggle = async (invite: AdminInviteCode) => {
    try {
      if (invite.active) {
        await deactivateInviteCode(invite.id)
      } else {
        await activateInviteCode(invite.id)
      }
      setInvites(await listInviteCodes())
      setDashboard(await getAdminDashboard())
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t("failedToUpdateInviteCode"))
    }
  }

  const handleSendInviteEmail = async (invite: AdminInviteCode) => {
    const email = (inviteEmailDraftById[invite.id] ?? "").trim()
    if (!email) {
      setError(t("pleaseEnterEmailFirst"))
      return
    }
    setError("")
    setSendingInviteId(invite.id)
    try {
      await sendInviteCodeEmail(invite.id, email)
      setInviteSendStateById((prev) => ({ ...prev, [invite.id]: "sent" }))
      setInviteComposerOpenById((prev) => ({ ...prev, [invite.id]: false }))
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t("failedToSendInviteEmail"))
    } finally {
      setSendingInviteId(null)
    }
  }

  const handleRemoveInvite = async (invite: AdminInviteCode) => {
    const confirmed = window.confirm(
      t("confirmRemoveInviteCode", {
        code: invite.code,
        email: invite.usedByEmail ? t("andUser", { email: invite.usedByEmail }) : "",
      }),
    )
    if (!confirmed) return

    setError("")
    setRemovingInviteId(invite.id)
    try {
      await removeInviteCode(invite.id)
      const [inviteData, userData, dashboardData] = await Promise.all([
        listInviteCodes(),
        listAdminUsers(),
        getAdminDashboard(),
      ])
      setInvites(inviteData)
      setUsers(userData)
      setDashboard(dashboardData)
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t("failedToRemoveInviteCode"))
    } finally {
      setRemovingInviteId(null)
    }
  }

  return (
    <div className="flex-1 overflow-auto bg-muted/20">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-4 lg:p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">{t("adminPanel")}</h1>
            <p className="text-sm text-muted-foreground">
              {t("adminPanelDescription")}
            </p>
          </div>
          <Button variant="outline" onClick={() => void loadAll()} disabled={loading}>
            {t("refresh")}
          </Button>
        </div>

        {error ? <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</div> : null}

        {dashboard ? (
          <>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <MetricCard title={t("songVolume")} metrics={dashboard.songVolume} />
              <MetricCard title={t("registrations")} metrics={dashboard.registrationTrends} />
              <MetricCard title={t("sunoCreditUsage")} metrics={dashboard.creditConsumption} />
            </div>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
              <Card><CardHeader className="pb-2"><CardDescription>{t("totalUsers")}</CardDescription><CardTitle>{dashboard.totalUsers}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>{t("frozenUsers")}</CardDescription><CardTitle>{dashboard.frozenUsers}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>{t("activeInvites")}</CardDescription><CardTitle>{dashboard.activeInviteCodes}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>{t("totalLyrics")}</CardDescription><CardTitle>{dashboard.totalLyrics}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>{t("totalSongs")}</CardDescription><CardTitle>{dashboard.totalSongs}</CardTitle></CardHeader></Card>
            </div>
          </>
        ) : null}

        <Tabs value={adminTab} onValueChange={(value) => setAdminTab(value as typeof adminTab)} className="w-full">
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="users">{t("users")}</TabsTrigger>
            <TabsTrigger value="invites">{t("invites")}</TabsTrigger>
            <TabsTrigger value="lyrics">{t("lyricsAudit")}</TabsTrigger>
            <TabsTrigger value="ready-library">{t("readyLibrary")}</TabsTrigger>
            <TabsTrigger value="songs">{t("songAudit")}</TabsTrigger>
          </TabsList>

          <TabsContent value="users">
            <Card>
              <CardHeader>
                <CardTitle>{t("userManagement")}</CardTitle>
                <CardDescription>{t("trackRegistrations")}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input value={userSearch} onChange={(event) => setUserSearch(event.target.value)} placeholder={t("searchUsersByEmail")} />
                <div className="space-y-3">
                  {pagedUsers.map((user) => (
                    <div key={user.id} className="flex flex-col gap-3 rounded-xl border p-4 lg:flex-row lg:items-center lg:justify-between">
                      <div>
                        <div className="font-medium">{user.email}</div>
                        <div className="text-sm text-muted-foreground">
                          {t("inviteCode")}: {user.inviteCode ?? "-"} • {t("credits")}: {user.unlimitedCredits ? t("unlimited") : user.creditsRemaining ?? 0} • {t("generated")}: {user.generationCount ?? 0}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {user.admin ? <span className="rounded-full bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">{t("admin")}</span> : null}
                        {user.frozen ? <span className="rounded-full bg-destructive/15 px-3 py-1 text-xs font-medium text-destructive">{t("frozen")}</span> : null}
                        <Button variant={user.frozen ? "default" : "destructive"} size="sm" onClick={() => void handleFreezeToggle(user)}>
                          {user.frozen ? t("unfreeze") : t("freeze")}
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
                <PaginationControls page={usersPage} totalPages={usersTotalPages} onPageChange={setUsersPage} />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="invites">
            <Card>
              <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <CardTitle>{t("inviteSystem")}</CardTitle>
                  <CardDescription>{t("inviteSystemDescription")}</CardDescription>
                </div>
                <Button onClick={() => void handleGenerateInvites()}>{t("generateCodes")}</Button>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input
                  value={inviteSearch}
                  onChange={(event) => setInviteSearch(event.target.value)}
                  placeholder={t("searchByCodeUsedEmailSentEmail")}
                  autoComplete="off"
                />
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    size="sm"
                    variant={inviteActiveOnly ? "default" : "outline"}
                    onClick={() => setInviteActiveOnly((prev) => !prev)}
                  >
                    {t("activeOnly")}
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant={inviteUsedOnly ? "default" : "outline"}
                    onClick={() => setInviteUsedOnly((prev) => !prev)}
                  >
                    {t("usedOnly")}
                  </Button>
                </div>
                <div className="space-y-3">
                  {pagedInvites.length === 0 ? (
                    <div className="rounded-xl border border-dashed p-4 text-sm text-muted-foreground">
                      {t("noInviteCodesMatch")}
                      <Button
                        type="button"
                        variant="link"
                        className="ml-2 h-auto p-0"
                        onClick={() => {
                          setInviteSearch("")
                          setInviteActiveOnly(false)
                          setInviteUsedOnly(false)
                        }}
                      >
                        {t("clearFilters")}
                      </Button>
                    </div>
                  ) : null}
                  {pagedInvites.map((invite) => (
                    <div key={invite.id} className="rounded-xl border p-4">
                      {(() => {
                        const isInviteSent =
                          inviteSendStateById[invite.id] === "sent" || Boolean(invite.lastSentAt)
                        return (
                      <>
                      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                        <div className="flex-1">
                          <div className="font-mono text-sm font-semibold">{invite.code}</div>
                          <div className="text-sm text-muted-foreground">
                            {invite.usedByEmail ? t("usedBy", { email: invite.usedByEmail }) : t("unused")} • {invite.active ? t("active") : t("inactive")}
                          </div>
                        {invite.lastSentToEmail ? (
                          <div className="text-xs text-muted-foreground">
                            {t("lastSentTo", { email: invite.lastSentToEmail })}
                          </div>
                        ) : null}
                        </div>
                        <div className="flex items-center gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            className="gap-1.5"
                            disabled={Boolean(invite.usedByUserId) || isInviteSent}
                            onClick={() =>
                              setInviteComposerOpenById((prev) => ({
                                ...prev,
                                [invite.id]: !prev[invite.id],
                              }))
                            }
                            title={t("sendInviteCodeByEmail")}
                          >
                            <MailPlus className="h-3.5 w-3.5" />
                            {t("emailAction")}
                          </Button>
                          {isInviteSent ? (
                            <Button type="button" variant="secondary" size="sm" disabled>
                              {t("sentInvitation")}
                            </Button>
                          ) : null}
                          <Button variant="outline" size="sm" onClick={() => void handleInviteToggle(invite)} disabled={Boolean(invite.usedByUserId)}>
                            {invite.active ? t("deactivate") : t("activate")}
                          </Button>
                          <Button
                            type="button"
                            variant="destructive"
                            size="sm"
                            className="gap-1.5"
                            disabled={sendingInviteId === invite.id || removingInviteId === invite.id}
                            onClick={() => void handleRemoveInvite(invite)}
                            title={t("removeInviteCodeAndLinkedUser")}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                            {removingInviteId === invite.id ? t("removing") : t("remove")}
                          </Button>
                        </div>
                      </div>

                      {inviteComposerOpenById[invite.id] && !isInviteSent ? (
                        <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                          <Input
                            value={inviteEmailDraftById[invite.id] ?? ""}
                            onChange={(event) =>
                              setInviteEmailDraftById((prev) => ({ ...prev, [invite.id]: event.target.value }))
                            }
                            placeholder={t("enterEmailToSendInviteCode")}
                            className="sm:max-w-sm"
                            disabled={Boolean(invite.usedByUserId) || sendingInviteId === invite.id}
                          />
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            className="gap-1.5"
                            disabled={Boolean(invite.usedByUserId) || sendingInviteId === invite.id}
                            onClick={() => void handleSendInviteEmail(invite)}
                          >
                            <MailPlus className="h-3.5 w-3.5" />
                            {sendingInviteId === invite.id ? t("sending") : t("sendEmail")}
                          </Button>
                        </div>
                      ) : null}

                      {isInviteSent ? (
                        <div className="mt-3">
                          <span className="inline-flex items-center rounded-full bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">
                            {t("codeSentShort")}
                          </span>
                        </div>
                      ) : null}
                      </>
                        )
                      })()}
                    </div>
                  ))}
                </div>
                <PaginationControls page={invitesPage} totalPages={invitesTotalPages} onPageChange={setInvitesPage} />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="lyrics">
            <Card>
              <CardHeader>
                <CardTitle>{t("lyricsAudit")}</CardTitle>
                <CardDescription>{t("lyricsAuditDescription")}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input
                  value={lyricSearch}
                  onChange={(event) => setLyricSearch(event.target.value)}
                  placeholder={t("searchTitleLyricText")}
                />
                <div className="space-y-3">
                  {pagedLyrics.map((lyric) => (
                    <div key={lyric.id} className="rounded-xl border p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="font-medium truncate">{lyric.title}</div>
                          {lyric.language ? (
                            <div className="mt-1 text-xs text-muted-foreground">{t("language")}: {lyric.language}</div>
                          ) : null}
                          {typeof lyric.publicReadyLibrary === "boolean" ? (
                            <div className="mt-1 text-xs text-muted-foreground">
                              {lyric.publicReadyLibrary ? t("public") : t("private")}
                            </div>
                          ) : null}
                          {lyric.locked ? (
                            <div className="mt-2">
                              <span className="rounded-full bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">
                                {t("locked")}
                              </span>
                            </div>
                          ) : (
                            <div className="mt-2">
                              <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
                                {t("unlocked")}
                              </span>
                            </div>
                          )}
                        </div>
                        <div className="hidden shrink-0 lg:block" />
                      </div>
                      <p className="mt-2 text-sm text-muted-foreground">
                        {lyric.bodyPreview ?? t("noPreviewAvailable")}
                      </p>
                      <div className="mt-2 text-xs text-muted-foreground">
                        {t("linkedSongs")}: {lyric.linkedSongIds.length}
                      </div>
                    </div>
                  ))}
                </div>
                <PaginationControls
                  page={lyricsPage}
                  totalPages={lyricsTotalPages}
                  onPageChange={setLyricsPage}
                />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="ready-library">
            <Card>
              <CardHeader>
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <CardTitle>{t("readyLibrary")}</CardTitle>
                    <CardDescription>{t("readyLibraryDescription")}</CardDescription>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <Button size="sm" onClick={() => openCreateReadyLibrary()} disabled={readyLibrarySaving}>
                      {t("new")}
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <Input
                    value={readyLibraryKeyword}
                    onChange={(event) => setReadyLibraryKeyword(event.target.value)}
                    placeholder={t("searchReadyLibraryLyrics")}
                  />
                  <select
                    value={readyLibraryLanguageFilter}
                    onChange={(e) => setReadyLibraryLanguageFilter(e.target.value as "ALL" | "WESTERN_ARMENIAN")}
                    className="h-9 min-w-[220px] rounded-md border border-border bg-card px-2 text-sm text-foreground"
                  >
                    <option value="ALL">{t("all")}</option>
                    <option value="WESTERN_ARMENIAN">{t("westernArmenian")}</option>
                  </select>
                </div>

                {readyLibraryLoading ? (
                  <div className="text-sm text-muted-foreground">{t("loading")}</div>
                ) : readyLibraryError ? (
                  <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    {readyLibraryError}
                  </div>
                ) : (
                  <div className="space-y-3">
                    {readyLibrary.length === 0 ? (
                      <div className="rounded-xl border border-dashed p-4 text-sm text-muted-foreground">
                        {t("noReadyLibraryLyricsFound")}
                      </div>
                    ) : (
                      readyLibrary.map((item) => (
                        <div key={item.id} className="rounded-xl border p-4">
                          <div className="flex items-start justify-between gap-4">
                            <div className="min-w-0">
                              <div className="font-medium truncate">{item.title}</div>
                              <div className="mt-1 text-xs text-muted-foreground">{t("language")}: {item.language}</div>
                              <div className="mt-1 text-xs text-muted-foreground">
                                {t("currentVersion")}: {item.currentVersion}
                              </div>
                              <div className="mt-1 text-xs text-muted-foreground">
                                {t("updatedAt")}{" "}
                                {item.updatedAt ? new Date(item.updatedAt).toLocaleString() : "-"}
                              </div>
                              {item.createdByAdminUserId ? (
                                <div className="mt-1 text-xs text-muted-foreground">
                                  {t("createdBy")}: {item.createdByAdminUserId}
                                </div>
                              ) : null}
                              <p className="mt-2 text-sm text-muted-foreground">{item.bodyPreview}</p>
                            </div>
                            <div className="flex flex-col gap-2">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => void openEditReadyLibrary(item.id)}
                                disabled={readyLibrarySaving}
                              >
                                {t("edit")}
                              </Button>
                              <Button
                                variant="destructive"
                                size="sm"
                                onClick={() => void handleDeleteReadyLibrary(item.id)}
                                disabled={readyLibrarySaving}
                              >
                                {t("delete")}
                              </Button>
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="songs">
            <Card>
              <CardHeader>
                <CardTitle>{t("songAudit")}</CardTitle>
                <CardDescription>{t("songAuditDescription")}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input value={songSearch} onChange={(event) => setSongSearch(event.target.value)} placeholder={t("searchTitleLyricTitleTags")} />
                <div className="space-y-4">
                  {pagedSongs.map((song) => (
                    <div key={song.id} className="rounded-xl border p-4">
                      {(() => {
                        const audioCandidates = buildAdminSongAudioCandidates(song)
                        const downloadUrl = audioCandidates[0]
                        return (
                          <>
                      <div className="flex flex-col gap-1 lg:flex-row lg:items-center lg:justify-between">
                        <div>
                          <div className="font-medium">{song.title ?? t("untitledSong")}</div>
                          <div className="text-sm text-muted-foreground">
                            {song.lyricTitle ?? t("noLinkedLyric")} • {(song.tags ?? []).join(", ") || t("noTags")}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {t("project")}: {song.projectId ?? "-"}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {t("user")}: {song.userEmail ?? song.userId ?? "-"}
                          </div>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            if (downloadUrl) window.open(downloadUrl, "_blank", "noopener,noreferrer")
                          }}
                          disabled={!downloadUrl}
                        >
                          {t("download")}
                        </Button>
                      </div>
                      {audioCandidates.length > 0 ? (
                        <audio className="mt-3 w-full" controls preload="metadata">
                          {audioCandidates.map((candidate, index) => (
                            <source
                              key={`${song.id}-source-${index}`}
                              src={candidate}
                            />
                          ))}
                          {t("yourBrowserNoAudio")}
                        </audio>
                      ) : (
                        <div className="mt-3 text-sm text-muted-foreground">{t("noPlayableUrlAvailable")}</div>
                      )}
                          </>
                        )
                      })()}
                    </div>
                  ))}
                </div>
                <PaginationControls page={songsPage} totalPages={songsTotalPages} onPageChange={setSongsPage} />
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

          <Dialog
            open={readyLibraryDialogOpen}
            onOpenChange={(open) => {
              setReadyLibraryDialogOpen(open)
              if (!open) setReadyLibraryError("")
            }}
          >
            <DialogContent className="sm:max-w-lg">
              <DialogHeader>
                <DialogTitle>{readyLibraryEditingId ? t("edit") : t("create")} {t("readyLibrary")}</DialogTitle>
                <DialogDescription>
                  {readyLibraryEditingId ? t("updateReadyLibraryLyric") : t("createReadyLibraryLyric")}
                </DialogDescription>
              </DialogHeader>

              <div className="flex flex-col gap-4">
                {readyLibraryError ? (
                  <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    {readyLibraryError}
                  </div>
                ) : null}

                <div className="flex flex-col gap-2">
                  <Label htmlFor="ready-library-title">{t("songTitle")}</Label>
                  <Input
                    id="ready-library-title"
                    value={readyLibraryTitle}
                    onChange={(e) => setReadyLibraryTitle(e.target.value)}
                    placeholder={t("title")}
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="ready-library-body">{t("lyrics")}</Label>
                  <Textarea
                    id="ready-library-body"
                    value={readyLibraryBody}
                    onChange={(e) => setReadyLibraryBody(e.target.value)}
                    className="min-h-[180px] resize-none"
                  />
                </div>

                <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
                  <Button
                    onClick={() => void handleSaveReadyLibrary()}
                    disabled={
                      readyLibrarySaving ||
                      !readyLibraryTitle.trim() ||
                      !readyLibraryBody.trim()
                    }
                  >
                    {readyLibrarySaving ? t("saving") : t("save")}
                  </Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
      </div>
    </div>
  )
}
