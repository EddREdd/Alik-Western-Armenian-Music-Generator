"use client"

import { useEffect, useMemo, useState } from "react"
import { useIsDesktop } from "@/hooks/use-is-desktop"
import {
  FileText,
  Plus,
  Search,
  MoreHorizontal,
  Trash2,
  Edit3,
  Calendar,
  ArrowLeft,
  Music,
  History,
  Lock,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Badge } from "@/components/ui/badge"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Label } from "@/components/ui/label"
import {
  createLyric,
  deleteLyric,
  getLyric,
  listLyrics,
  updateLyric,
  type Lyric,
  type LyricSummary,
} from "@/lib/lyrics-api"

interface LyricsPageProps {
  onNavigateToSong?: (songId: string) => void
}

type MobileView = "list" | "preview" | "create" | "edit"

const defaultProjectId =
  process.env.NEXT_PUBLIC_DEFAULT_PROJECT_ID?.trim() || "project-1"

function formatDate(value: string | null) {
  if (!value) {
    return "Just now"
  }
  return new Date(value).toLocaleString()
}

export function LyricsPage({ onNavigateToSong }: LyricsPageProps) {
  const [search, setSearch] = useState("")
  const [lyrics, setLyrics] = useState<LyricSummary[]>([])
  const [selectedLyric, setSelectedLyric] = useState<Lyric | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [newTitle, setNewTitle] = useState("")
  const [newContent, setNewContent] = useState("")
  const [editTitle, setEditTitle] = useState("")
  const [editContent, setEditContent] = useState("")
  const [editingId, setEditingId] = useState<string | null>(null)
  const [mobileView, setMobileView] = useState<MobileView>("list")
  const [error, setError] = useState("")
  const isDesktop = useIsDesktop()

  const loadLyrics = async () => {
    try {
      const items = await listLyrics(defaultProjectId)
      setLyrics(items)
      setError("")
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Unable to load lyrics")
    }
  }

  useEffect(() => {
    void loadLyrics()
  }, [])

  const filtered = useMemo(
    () =>
      lyrics.filter(
        (entry) =>
          entry.title.toLowerCase().includes(search.toLowerCase()) ||
          entry.bodyPreview.toLowerCase().includes(search.toLowerCase()),
      ),
    [lyrics, search],
  )

  const handleCreate = async () => {
    if (!newTitle.trim() || !newContent.trim()) return

    try {
      const created = await createLyric({
        projectId: defaultProjectId,
        title: newTitle,
        body: newContent,
      })
      await loadLyrics()
      setSelectedLyric(created)
      setNewTitle("")
      setNewContent("")
      setCreateOpen(false)
      setMobileView("preview")
      setError("")
    } catch (createError) {
      setError(createError instanceof Error ? createError.message : "Unable to create lyric")
    }
  }

  const handleSelectLyric = async (id: string) => {
    try {
      const lyric = await getLyric(id)
      setSelectedLyric(lyric)
      setMobileView("preview")
      setError("")
    } catch (selectError) {
      setError(selectError instanceof Error ? selectError.message : "Unable to load lyric")
    }
  }

  const handleStartEdit = (lyric: Lyric | LyricSummary) => {
    if (lyric.locked) return
    setEditingId(lyric.id)
    setEditTitle(lyric.title)
    setEditContent("body" in lyric ? lyric.body : "")
    setEditOpen(true)
    setMobileView("edit")
  }

  const openEditById = async (id: string) => {
    try {
      const lyric = await getLyric(id)
      setSelectedLyric(lyric)
      handleStartEdit(lyric)
      setError("")
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Unable to load lyric")
    }
  }

  const handleSaveEdit = async () => {
    if (!editingId || !editTitle.trim() || !editContent.trim()) return

    try {
      const updated = await updateLyric(editingId, {
        title: editTitle,
        body: editContent,
      })
      await loadLyrics()
      setSelectedLyric(updated)
      setEditingId(null)
      setEditTitle("")
      setEditContent("")
      setEditOpen(false)
      setMobileView("preview")
      setError("")
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Unable to update lyric")
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteLyric(id)
      await loadLyrics()
      if (selectedLyric?.id === id) {
        setSelectedLyric(null)
      }
      setMobileView("list")
      setError("")
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Unable to delete lyric")
    }
  }

  const handleMobileBack = () => {
    setMobileView("list")
    setEditingId(null)
    setSelectedLyric(null)
  }

  if (!isDesktop && mobileView === "create") {
    return (
      <main className="flex flex-1 flex-col overflow-hidden">
        <div className="flex items-center gap-3 border-b border-border px-4 py-3">
          <Button variant="ghost" size="sm" onClick={handleMobileBack} className="h-8 w-8 p-0 text-foreground">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-lg font-bold tracking-wide text-foreground">New Lyrics</h1>
        </div>
        <ScrollArea className="flex-1">
          <div className="flex flex-col gap-4 p-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-lyric-title">Title</Label>
              <Input id="mobile-lyric-title" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-lyric-content">Lyrics</Label>
              <Textarea
                id="mobile-lyric-content"
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                className="min-h-[300px] resize-none"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button onClick={handleCreate} disabled={!newTitle.trim() || !newContent.trim()}>
              Save to Library
            </Button>
          </div>
        </ScrollArea>
      </main>
    )
  }

  if (!isDesktop && mobileView === "edit" && editingId) {
    return (
      <main className="flex flex-1 flex-col overflow-hidden">
        <div className="flex items-center gap-3 border-b border-border px-4 py-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setMobileView(selectedLyric ? "preview" : "list")
              setEditingId(null)
            }}
            className="h-8 w-8 p-0 text-foreground"
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-lg font-bold tracking-wide text-foreground">Edit Lyrics</h1>
        </div>
        <ScrollArea className="flex-1">
          <div className="flex flex-col gap-4 p-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-edit-title">Title</Label>
              <Input id="mobile-edit-title" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-edit-content">Lyrics</Label>
              <Textarea
                id="mobile-edit-content"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="min-h-[300px] resize-none"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button onClick={handleSaveEdit} disabled={!editTitle.trim() || !editContent.trim()}>
              Save Changes
            </Button>
          </div>
        </ScrollArea>
      </main>
    )
  }

  if (!isDesktop && mobileView === "preview" && selectedLyric) {
    return (
      <main className="flex flex-1 flex-col overflow-hidden">
        <div className="flex items-center gap-3 border-b border-border px-4 py-3">
          <Button variant="ghost" size="sm" onClick={handleMobileBack} className="h-8 w-8 p-0 text-foreground">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-lg font-bold tracking-wide text-foreground">{selectedLyric.title}</h1>
            <div className="flex items-center gap-3 text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {formatDate(selectedLyric.updatedAt)}
              </span>
              <span>{selectedLyric.wordCount} words</span>
            </div>
          </div>
          {!selectedLyric.locked ? (
            <Button variant="outline" size="sm" onClick={() => handleStartEdit(selectedLyric)}>
              <Edit3 className="h-3.5 w-3.5" />
            </Button>
          ) : null}
        </div>
        <div className="border-b border-border px-4 py-3">
          <div className="flex flex-wrap gap-2">
            <Badge variant="outline">v{selectedLyric.currentVersion}</Badge>
            {selectedLyric.locked ? (
              <Badge variant="outline" className="border-secondary/30 text-secondary">
                <Lock className="mr-1 h-3 w-3" />
                Locked
              </Badge>
            ) : null}
            {selectedLyric.linkedSongIds.map((songId) => (
              <button
                key={songId}
                onClick={() => onNavigateToSong?.(songId)}
                className="flex items-center gap-1 rounded-full bg-secondary/10 px-3 py-1 text-xs font-medium text-secondary"
              >
                <Music className="h-3 w-3" />
                {songId}
              </button>
            ))}
          </div>
        </div>
        <div className="flex-1 overflow-y-auto px-4 py-5">
          <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed text-foreground/90">
            {selectedLyric.body}
          </pre>
          {selectedLyric.versions.length > 0 ? (
            <div className="mt-6 rounded-xl border border-border bg-card p-4">
              <p className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
                <History className="h-4 w-4" />
                Version History
              </p>
              <div className="space-y-2">
                {selectedLyric.versions
                  .slice()
                  .reverse()
                  .map((version) => (
                    <div key={`${version.versionNumber}-${version.editedAt}`} className="rounded-lg bg-muted/50 p-3">
                      <p className="text-xs font-medium text-foreground">
                        v{version.versionNumber} • {formatDate(version.editedAt)}
                      </p>
                    </div>
                  ))}
              </div>
            </div>
          ) : null}
        </div>
      </main>
    )
  }

  return (
    <main className="flex flex-1 overflow-hidden">
      <div className="w-full border-border lg:w-1/3 lg:border-r">
        <div className="flex flex-col gap-3 border-b border-border p-4">
          <div className="flex items-center justify-between">
            <h1 className="text-lg font-bold tracking-wide text-foreground">Lyrics Library</h1>
            <Button
              size="sm"
              onClick={() => (isDesktop ? setCreateOpen(true) : setMobileView("create"))}
              className="gap-1.5 bg-secondary text-secondary-foreground hover:bg-secondary/90"
            >
              <Plus className="h-3.5 w-3.5" />
              New
            </Button>
          </div>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search lyrics..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
        </div>

        <ScrollArea className="h-[calc(100vh-200px)]">
          <div className="flex flex-col gap-1 p-2">
            {filtered.map((entry) => (
              <div
                key={entry.id}
                onClick={() => void handleSelectLyric(entry.id)}
                className={`group flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors ${
                  selectedLyric?.id === entry.id ? "border-primary/20 bg-primary/10" : "border-transparent hover:bg-muted"
                }`}
              >
                <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-secondary/10">
                  <FileText className="h-4 w-4 text-secondary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-foreground">{entry.title}</p>
                  <p className="mt-0.5 truncate text-xs text-muted-foreground">{entry.bodyPreview}</p>
                  <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground/60">
                    <span>{entry.wordCount} words</span>
                    <span>{formatDate(entry.updatedAt)}</span>
                    <Badge variant="outline" className="px-1.5 py-0 text-[10px]">
                      v{entry.currentVersion}
                    </Badge>
                    {entry.locked ? (
                      <Badge variant="outline" className="border-secondary/30 px-1.5 py-0 text-[10px] text-secondary">
                        Locked
                      </Badge>
                    ) : null}
                  </div>
                </div>
                {!entry.locked ? (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 w-6 p-0 text-muted-foreground opacity-0 group-hover:opacity-100"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <MoreHorizontal className="h-3.5 w-3.5" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem
                        onClick={(e) => {
                          e.stopPropagation()
                          void openEditById(entry.id)
                        }}
                      >
                        <Edit3 className="mr-2 h-4 w-4" />
                        Edit
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-destructive"
                        onClick={(e) => {
                          e.stopPropagation()
                          void handleDelete(entry.id)
                        }}
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        Delete
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                ) : null}
              </div>
            ))}
          </div>
        </ScrollArea>
      </div>

      <div className="hidden flex-1 bg-primary/[0.03] lg:block">
        {selectedLyric ? (
          <div className="flex h-full flex-col">
            <div className="flex items-center justify-between border-b border-border px-8 py-5">
              <div>
                <h2 className="text-xl font-bold text-foreground">{selectedLyric.title}</h2>
                <div className="mt-1 flex items-center gap-3 text-sm text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <Calendar className="h-3.5 w-3.5" />
                    {formatDate(selectedLyric.updatedAt)}
                  </span>
                  <span>{selectedLyric.wordCount} words</span>
                  <Badge variant="outline">v{selectedLyric.currentVersion}</Badge>
                </div>
              </div>
              {!selectedLyric.locked ? (
                <Button variant="outline" size="sm" onClick={() => handleStartEdit(selectedLyric)}>
                  <Edit3 className="mr-1 h-3.5 w-3.5" />
                  Edit
                </Button>
              ) : (
                <Badge variant="outline" className="border-secondary/30 text-secondary">
                  <Lock className="mr-1 h-3 w-3" />
                  Read Only
                </Badge>
              )}
            </div>

            <div className="border-b border-border px-8 py-4">
              <div className="flex flex-wrap gap-2">
                {selectedLyric.linkedSongIds.length === 0 ? (
                  <p className="text-sm text-muted-foreground">No generated songs linked yet.</p>
                ) : (
                  selectedLyric.linkedSongIds.map((songId) => (
                    <button
                      key={songId}
                      onClick={() => onNavigateToSong?.(songId)}
                      className="flex items-center gap-1.5 rounded-full bg-secondary/10 px-3 py-1.5 text-sm font-medium text-secondary"
                    >
                      <Music className="h-3.5 w-3.5" />
                      {songId}
                    </button>
                  ))
                )}
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-8 py-6">
              <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed text-foreground/90">
                {selectedLyric.body}
              </pre>
              {selectedLyric.versions.length > 0 ? (
                <div className="mt-8 rounded-xl border border-border bg-card p-5">
                  <h3 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
                    <History className="h-4 w-4" />
                    Edit History
                  </h3>
                  <div className="space-y-3">
                    {selectedLyric.versions
                      .slice()
                      .reverse()
                      .map((version) => (
                        <div key={`${version.versionNumber}-${version.editedAt}`} className="rounded-lg border border-border p-4">
                          <p className="text-sm font-medium text-foreground">
                            Version {version.versionNumber}
                          </p>
                          <p className="mt-1 text-xs text-muted-foreground">{formatDate(version.editedAt)}</p>
                        </div>
                      ))}
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        ) : (
          <div className="flex h-full flex-col items-center justify-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
              <FileText className="h-8 w-8 text-primary" />
            </div>
            <p className="mt-4 font-semibold text-foreground">Select lyrics to preview</p>
          </div>
        )}
      </div>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>New Lyrics</DialogTitle>
            <DialogDescription>Write Armenian lyrics to save to your library.</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="lyric-title">Title</Label>
              <Input id="lyric-title" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="lyric-content">Lyrics</Label>
              <Textarea
                id="lyric-content"
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                className="min-h-[200px] resize-none"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button onClick={handleCreate} disabled={!newTitle.trim() || !newContent.trim()}>
              Save to Library
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Edit Lyrics</DialogTitle>
            <DialogDescription>Locked lyrics cannot be edited.</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-lyric-title">Title</Label>
              <Input id="edit-lyric-title" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-lyric-content">Lyrics</Label>
              <Textarea
                id="edit-lyric-content"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="min-h-[200px] resize-none"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button onClick={handleSaveEdit} disabled={!editTitle.trim() || !editContent.trim()}>
              Save Changes
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </main>
  )
}
