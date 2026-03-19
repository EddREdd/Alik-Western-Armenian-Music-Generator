"use client"

import { useEffect, useMemo, useState } from "react"
import { MailPlus, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  activateInviteCode,
  deactivateInviteCode,
  freezeUser,
  generateInviteCodes,
  getAdminDashboard,
  listAdminLyrics,
  listAdminSongs,
  listAdminUsers,
  listInviteCodes,
  removeInviteCode,
  sendInviteCodeEmail,
  unfreezeUser,
  type AdminDashboard,
  type AdminInviteCode,
  type AdminLyricSummary,
  type AdminSongSummary,
  type AdminUserSummary,
} from "@/lib/admin-api"

const PAGE_SIZE = 25
const configuredBackendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_URL?.trim() || ""
const backendBaseUrl = configuredBackendBaseUrl.replace(/\/+$/, "")
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
  if (!backendBaseUrl) return trimmed
  return `${backendBaseUrl}${trimmed.startsWith("/") ? "" : "/"}${trimmed}`
}

function toProxyUrl(url: string): string {
  return `${backendBaseUrl}/api/v1/media/proxy?url=${encodeURIComponent(url)}`
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

    if (/^https?:\/\//i.test(absoluteCandidate) && backendBaseUrl) {
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
        <div><span className="text-muted-foreground">Daily</span><div className="text-xl font-semibold">{metrics.daily}</div></div>
        <div><span className="text-muted-foreground">Weekly</span><div className="text-xl font-semibold">{metrics.weekly}</div></div>
        <div><span className="text-muted-foreground">Monthly</span><div className="text-xl font-semibold">{metrics.monthly}</div></div>
        <div><span className="text-muted-foreground">Total</span><div className="text-xl font-semibold">{metrics.total}</div></div>
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
        Previous
      </Button>
      <span className="text-xs text-muted-foreground">
        Page {page} of {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages}
      >
        Next
      </Button>
    </div>
  )
}

export function AdminPage() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null)
  const [users, setUsers] = useState<AdminUserSummary[]>([])
  const [invites, setInvites] = useState<AdminInviteCode[]>([])
  const [lyrics, setLyrics] = useState<AdminLyricSummary[]>([])
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
      setError(loadError instanceof Error ? loadError.message : "Failed to load admin data")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadAll()
  }, [])

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
        const reason = window.prompt(`Freeze ${user.email} for what reason?`)
        if (!reason?.trim()) return
        await freezeUser(user.id, reason.trim())
      }
      setUsers(await listAdminUsers())
      setDashboard(await getAdminDashboard())
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Failed to update user")
    }
  }

  const handleGenerateInvites = async () => {
    const raw = window.prompt("How many invite codes do you want to generate?", "10")
    const count = Number(raw)
    if (!Number.isFinite(count) || count <= 0) return
    try {
      await generateInviteCodes(count)
      setInvites(await listInviteCodes())
      setDashboard(await getAdminDashboard())
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Failed to generate invite codes")
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
      setError(actionError instanceof Error ? actionError.message : "Failed to update invite code")
    }
  }

  const handleSendInviteEmail = async (invite: AdminInviteCode) => {
    const email = (inviteEmailDraftById[invite.id] ?? "").trim()
    if (!email) {
      setError("Please enter an email first")
      return
    }
    setError("")
    setSendingInviteId(invite.id)
    try {
      await sendInviteCodeEmail(invite.id, email)
      setInviteSendStateById((prev) => ({ ...prev, [invite.id]: "sent" }))
      setInviteComposerOpenById((prev) => ({ ...prev, [invite.id]: false }))
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Failed to send invite email")
    } finally {
      setSendingInviteId(null)
    }
  }

  const handleRemoveInvite = async (invite: AdminInviteCode) => {
    const usedEmail = invite.usedByEmail ? ` and user ${invite.usedByEmail}` : ""
    const confirmed = window.confirm(
      `Remove invite code ${invite.code}${usedEmail}? This deletes the invite data permanently.`,
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
      setError(actionError instanceof Error ? actionError.message : "Failed to remove invite code")
    } finally {
      setRemovingInviteId(null)
    }
  }

  return (
    <div className="flex-1 overflow-auto bg-muted/20">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-4 lg:p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Admin Panel</h1>
            <p className="text-sm text-muted-foreground">
              Monitor usage, manage invite codes, freeze accounts, and audit lyrics and songs.
            </p>
          </div>
          <Button variant="outline" onClick={() => void loadAll()} disabled={loading}>
            Refresh
          </Button>
        </div>

        {error ? <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</div> : null}

        {dashboard ? (
          <>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <MetricCard title="Song Volume" metrics={dashboard.songVolume} />
              <MetricCard title="Registrations" metrics={dashboard.registrationTrends} />
              <MetricCard title="Suno Credit Usage" metrics={dashboard.creditConsumption} />
            </div>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
              <Card><CardHeader className="pb-2"><CardDescription>Total Users</CardDescription><CardTitle>{dashboard.totalUsers}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>Frozen Users</CardDescription><CardTitle>{dashboard.frozenUsers}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>Active Invites</CardDescription><CardTitle>{dashboard.activeInviteCodes}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>Total Lyrics</CardDescription><CardTitle>{dashboard.totalLyrics}</CardTitle></CardHeader></Card>
              <Card><CardHeader className="pb-2"><CardDescription>Total Songs</CardDescription><CardTitle>{dashboard.totalSongs}</CardTitle></CardHeader></Card>
            </div>
          </>
        ) : null}

        <Tabs defaultValue="users" className="w-full">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="users">Users</TabsTrigger>
            <TabsTrigger value="invites">Invites</TabsTrigger>
            <TabsTrigger value="lyrics">Lyrics Audit</TabsTrigger>
            <TabsTrigger value="songs">Song Audit</TabsTrigger>
          </TabsList>

          <TabsContent value="users">
            <Card>
              <CardHeader>
                <CardTitle>User Management</CardTitle>
                <CardDescription>Track registrations, used invite codes, and freeze unsafe accounts.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input value={userSearch} onChange={(event) => setUserSearch(event.target.value)} placeholder="Search users by email" />
                <div className="space-y-3">
                  {pagedUsers.map((user) => (
                    <div key={user.id} className="flex flex-col gap-3 rounded-xl border p-4 lg:flex-row lg:items-center lg:justify-between">
                      <div>
                        <div className="font-medium">{user.email}</div>
                        <div className="text-sm text-muted-foreground">
                          Invite: {user.inviteCode ?? "N/A"} • Credits: {user.unlimitedCredits ? "Unlimited" : user.creditsRemaining ?? 0} • Generated: {user.generationCount ?? 0}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {user.admin ? <span className="rounded-full bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">Admin</span> : null}
                        {user.frozen ? <span className="rounded-full bg-destructive/15 px-3 py-1 text-xs font-medium text-destructive">Frozen</span> : null}
                        <Button variant={user.frozen ? "default" : "destructive"} size="sm" onClick={() => void handleFreezeToggle(user)}>
                          {user.frozen ? "Unfreeze" : "Freeze"}
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
                  <CardTitle>Invite System</CardTitle>
                  <CardDescription>Generate and manage the active pool of invite codes.</CardDescription>
                </div>
                <Button onClick={() => void handleGenerateInvites()}>Generate Codes</Button>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input
                  value={inviteSearch}
                  onChange={(event) => setInviteSearch(event.target.value)}
                  placeholder="Search by code, used email, or sent email"
                  autoComplete="off"
                />
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    size="sm"
                    variant={inviteActiveOnly ? "default" : "outline"}
                    onClick={() => setInviteActiveOnly((prev) => !prev)}
                  >
                    Active only
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant={inviteUsedOnly ? "default" : "outline"}
                    onClick={() => setInviteUsedOnly((prev) => !prev)}
                  >
                    Used only
                  </Button>
                </div>
                <div className="space-y-3">
                  {pagedInvites.length === 0 ? (
                    <div className="rounded-xl border border-dashed p-4 text-sm text-muted-foreground">
                      No invite codes match the current search/filter.
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
                        Clear filters
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
                            {invite.usedByEmail ? `Used by ${invite.usedByEmail}` : "Unused"} • {invite.active ? "Active" : "Inactive"}
                          </div>
                        {invite.lastSentToEmail ? (
                          <div className="text-xs text-muted-foreground">
                            Last sent to {invite.lastSentToEmail}
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
                            title="Send invite code by email"
                          >
                            <MailPlus className="h-3.5 w-3.5" />
                            Email
                          </Button>
                          {isInviteSent ? (
                            <Button type="button" variant="secondary" size="sm" disabled>
                              Sent Invitation
                            </Button>
                          ) : null}
                          <Button variant="outline" size="sm" onClick={() => void handleInviteToggle(invite)} disabled={Boolean(invite.usedByUserId)}>
                            {invite.active ? "Deactivate" : "Activate"}
                          </Button>
                          <Button
                            type="button"
                            variant="destructive"
                            size="sm"
                            className="gap-1.5"
                            disabled={sendingInviteId === invite.id || removingInviteId === invite.id}
                            onClick={() => void handleRemoveInvite(invite)}
                            title="Remove invite code and linked user account"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                            {removingInviteId === invite.id ? "Removing..." : "Remove"}
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
                            placeholder="Enter email to send invite code"
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
                            {sendingInviteId === invite.id ? "Sending..." : "Send Email"}
                          </Button>
                        </div>
                      ) : null}

                      {isInviteSent ? (
                        <div className="mt-3">
                          <span className="inline-flex items-center rounded-full bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">
                            Code sent
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
                <CardTitle>Lyrics Audit</CardTitle>
                <CardDescription>Full lyric inventory with keyword filtering and lock visibility.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input value={lyricSearch} onChange={(event) => setLyricSearch(event.target.value)} placeholder="Search title or lyric text" />
                <div className="space-y-3">
                  {pagedLyrics.map((lyric) => (
                    <div key={lyric.id} className="rounded-xl border p-4">
                      <div className="flex items-center justify-between gap-3">
                        <div className="font-medium">{lyric.title}</div>
                        {lyric.locked ? <span className="rounded-full bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">Locked</span> : null}
                      </div>
                      <p className="mt-2 text-sm text-muted-foreground">{lyric.bodyPreview ?? "No preview available"}</p>
                      <div className="mt-2 text-xs text-muted-foreground">Linked Songs: {lyric.linkedSongIds.length}</div>
                    </div>
                  ))}
                </div>
                <PaginationControls page={lyricsPage} totalPages={lyricsTotalPages} onPageChange={setLyricsPage} />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="songs">
            <Card>
              <CardHeader>
                <CardTitle>Song Audit</CardTitle>
                <CardDescription>Play any generated song or download it directly from the admin dashboard.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Input value={songSearch} onChange={(event) => setSongSearch(event.target.value)} placeholder="Search title, lyric title, or tags" />
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
                          <div className="font-medium">{song.title ?? "Untitled Song"}</div>
                          <div className="text-sm text-muted-foreground">
                            {song.lyricTitle ?? "No linked lyric"} • {(song.tags ?? []).join(", ") || "No tags"}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            Project: {song.projectId ?? "N/A"}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            User: {song.userEmail ?? song.userId ?? "N/A"}
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
                          Download
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
                          Your browser does not support the audio element.
                        </audio>
                      ) : (
                        <div className="mt-3 text-sm text-muted-foreground">No playable URL available.</div>
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
      </div>
    </div>
  )
}
