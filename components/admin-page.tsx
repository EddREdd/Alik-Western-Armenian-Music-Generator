"use client"

import { useEffect, useMemo, useState } from "react"
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
  unfreezeUser,
  type AdminDashboard,
  type AdminInviteCode,
  type AdminLyricSummary,
  type AdminSongSummary,
  type AdminUserSummary,
} from "@/lib/admin-api"

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
  const [lyricSearch, setLyricSearch] = useState("")
  const [songSearch, setSongSearch] = useState("")

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
        !inviteSearch ||
        invite.code.toLowerCase().includes(inviteSearch.toLowerCase()) ||
        (invite.usedByEmail ?? "").toLowerCase().includes(inviteSearch.toLowerCase()),
      ),
    [invites, inviteSearch],
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
                  {filteredUsers.map((user) => (
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
                <Input value={inviteSearch} onChange={(event) => setInviteSearch(event.target.value)} placeholder="Search by code or used email" />
                <div className="space-y-3">
                  {filteredInvites.map((invite) => (
                    <div key={invite.id} className="flex flex-col gap-3 rounded-xl border p-4 lg:flex-row lg:items-center lg:justify-between">
                      <div>
                        <div className="font-mono text-sm font-semibold">{invite.code}</div>
                        <div className="text-sm text-muted-foreground">
                          {invite.usedByEmail ? `Used by ${invite.usedByEmail}` : "Unused"} • {invite.active ? "Active" : "Inactive"}
                        </div>
                      </div>
                      <Button variant="outline" size="sm" onClick={() => void handleInviteToggle(invite)} disabled={Boolean(invite.usedByUserId)}>
                        {invite.active ? "Deactivate" : "Activate"}
                      </Button>
                    </div>
                  ))}
                </div>
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
                  {filteredLyrics.map((lyric) => (
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
                  {filteredSongs.map((song) => (
                    <div key={song.id} className="rounded-xl border p-4">
                      <div className="flex flex-col gap-1 lg:flex-row lg:items-center lg:justify-between">
                        <div>
                          <div className="font-medium">{song.title ?? "Untitled Song"}</div>
                          <div className="text-sm text-muted-foreground">
                            {song.lyricTitle ?? "No linked lyric"} • {(song.tags ?? []).join(", ") || "No tags"}
                          </div>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            const url = song.streamAudioUrl || song.audioUrl
                            if (url) window.open(url, "_blank", "noopener,noreferrer")
                          }}
                        >
                          Download
                        </Button>
                      </div>
                      {song.streamAudioUrl || song.audioUrl ? (
                        <audio className="mt-3 w-full" controls src={song.streamAudioUrl || song.audioUrl || undefined} />
                      ) : (
                        <div className="mt-3 text-sm text-muted-foreground">No playable URL available.</div>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}
