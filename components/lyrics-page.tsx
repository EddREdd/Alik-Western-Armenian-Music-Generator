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
  listMyLyrics,
  restoreLyricVersion,
  updateLyric,
  type Lyric,
  type LyricSummary,
} from "@/lib/lyrics-api"
import { t, useUiLanguage } from "@/lib/i18n"

interface LyricsPageProps {
  onNavigateToSong?: (songId: string) => void
}

type MobileView = "list" | "preview" | "create" | "edit"

function formatDate(value: string | null) {
  if (!value) {
    return t("justNow")
  }
  return new Date(value).toLocaleString()
}

export function LyricsPage({ onNavigateToSong }: LyricsPageProps) {
  useUiLanguage()
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
  const [selectedVersion, setSelectedVersion] = useState<Lyric["versions"][number] | null>(null)
  const [versionDialogOpen, setVersionDialogOpen] = useState(false)
  const [restoringVersion, setRestoringVersion] = useState(false)
  const [error, setError] = useState("")
  const isDesktop = useIsDesktop()

  const loadLyrics = async () => {
    try {
      const items = await listMyLyrics()
      setLyrics(items)
      setError("")
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : t("unableToLoadLyrics"))
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
        title: newTitle,
        body: newContent,
        language: "WESTERN_ARMENIAN",
      })
      await loadLyrics()
      setSelectedLyric(created)
      setNewTitle("")
      setNewContent("")
      setCreateOpen(false)
      setMobileView("preview")
      setError("")
    } catch (createError) {
      setError(createError instanceof Error ? createError.message : t("unableToCreateLyric"))
    }
  }

  const handleSelectLyric = async (id: string) => {
    try {
      const lyric = await getLyric(id)
      setSelectedLyric(lyric)
      setMobileView("preview")
      setError("")
    } catch (selectError) {
      setError(selectError instanceof Error ? selectError.message : t("unableToLoadLyric"))
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
      setError(loadError instanceof Error ? loadError.message : t("unableToLoadLyric"))
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
      setError(saveError instanceof Error ? saveError.message : t("unableToUpdateLyric"))
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
      setError(deleteError instanceof Error ? deleteError.message : t("unableToDeleteLyric"))
    }
  }

  const handleMobileBack = () => {
    setMobileView("list")
    setEditingId(null)
    setSelectedLyric(null)
  }

  const openVersionDialog = (version: Lyric["versions"][number]) => {
    setSelectedVersion(version)
    setVersionDialogOpen(true)
  }

  const handleRestoreVersion = async () => {
    if (!selectedLyric || !selectedVersion) {
      return
    }
    setRestoringVersion(true)
    try {
      const restored = await restoreLyricVersion(selectedLyric.id, selectedVersion.versionNumber)
      await loadLyrics()
      setSelectedLyric(restored)
      setVersionDialogOpen(false)
      setSelectedVersion(null)
      setMobileView("preview")
      setError("")
    } catch (restoreError) {
      setError(restoreError instanceof Error ? restoreError.message : t("unableToRestoreLyricVersion"))
    } finally {
      setRestoringVersion(false)
    }
  }

  if (!isDesktop && mobileView === "create") {
    return (
      <main className="flex flex-1 flex-col overflow-hidden">
        <div className="flex items-center gap-3 border-b border-border px-4 py-3">
          <Button variant="ghost" size="sm" onClick={handleMobileBack} className="h-8 w-8 p-0 text-foreground">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-lg font-bold tracking-wide text-foreground">{t("newLyrics")}</h1>
        </div>
        <ScrollArea className="flex-1">
          <div className="flex flex-col gap-4 p-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-lyric-title">{t("title")}</Label>
              <Input id="mobile-lyric-title" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-lyric-content">{t("lyrics")}</Label>
              <Textarea
                id="mobile-lyric-content"
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                className="field-sizing-fixed min-h-[200px] max-h-[50vh] resize-y overflow-y-auto"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button onClick={handleCreate} disabled={!newTitle.trim() || !newContent.trim()}>
              {t("save")}
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
          <h1 className="text-lg font-bold tracking-wide text-foreground">
            {t("edit")} {t("lyrics")}
          </h1>
        </div>
        <ScrollArea className="flex-1">
          <div className="flex flex-col gap-4 p-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-edit-title">{t("title")}</Label>
              <Input id="mobile-edit-title" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-edit-content">{t("lyrics")}</Label>
              <Textarea
                id="mobile-edit-content"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="field-sizing-fixed min-h-[200px] max-h-[50vh] resize-y overflow-y-auto"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button onClick={handleSaveEdit} disabled={!editTitle.trim() || !editContent.trim()}>
              {t("save")}
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
              <span>{t("wordsCount", { count: selectedLyric.wordCount })}</span>
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
                {t("locked")}
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
                {t("versionHistory")}
              </p>
              <div className="space-y-2">
                {selectedLyric.versions
                  .slice()
                  .reverse()
                  .map((version) => (
                    <button
                      key={`${version.versionNumber}-${version.editedAt}`}
                      type="button"
                      onClick={() => openVersionDialog(version)}
                      className="w-full rounded-lg bg-muted/50 p-3 text-left hover:bg-muted"
                    >
                      <p className="text-xs font-medium text-foreground">
                        v{version.versionNumber} - {formatDate(version.editedAt)}
                      </p>
                    </button>
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
            <h1 className="text-lg font-bold tracking-wide text-foreground">{t("lyricsLibrary")}</h1>
            <Button
              size="sm"
              onClick={() => (isDesktop ? setCreateOpen(true) : setMobileView("create"))}
              className="gap-1.5 bg-secondary text-secondary-foreground hover:bg-secondary/90"
            >
              <Plus className="h-3.5 w-3.5" />
              {t("new")}
            </Button>
          </div>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t("searchLyrics")}
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
                    <span>{t("wordsCount", { count: entry.wordCount })}</span>
                    <span>{formatDate(entry.updatedAt)}</span>
                    <Badge variant="outline" className="px-1.5 py-0 text-[10px]">
                      v{entry.currentVersion}
                    </Badge>
                    {entry.locked ? (
                      <Badge variant="outline" className="border-secondary/30 px-1.5 py-0 text-[10px] text-secondary">
                        {t("locked")}
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
                        {t("edit")}
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-destructive"
                        onClick={(e) => {
                          e.stopPropagation()
                          void handleDelete(entry.id)
                        }}
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        {t("delete")}
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
                  <span>{t("wordsCount", { count: selectedLyric.wordCount })}</span>
                  <Badge variant="outline">v{selectedLyric.currentVersion}</Badge>
                </div>
              </div>
              {!selectedLyric.locked ? (
                <Button variant="outline" size="sm" onClick={() => handleStartEdit(selectedLyric)}>
                  <Edit3 className="mr-1 h-3.5 w-3.5" />
                  {t("edit")}
                </Button>
              ) : (
                <Badge variant="outline" className="border-secondary/30 text-secondary">
                  <Lock className="mr-1 h-3 w-3" />
                  {t("readOnly")}
                </Badge>
              )}
            </div>

            <div className="border-b border-border px-8 py-4">
              <div className="flex flex-wrap gap-2">
                {selectedLyric.linkedSongIds.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t("noGeneratedSongsLinkedYet")}</p>
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
                    {t("editHistory")}
                  </h3>
                  <div className="space-y-3">
                    {selectedLyric.versions
                      .slice()
                      .reverse()
                      .map((version) => (
                        <button
                          key={`${version.versionNumber}-${version.editedAt}`}
                          type="button"
                          onClick={() => openVersionDialog(version)}
                          className="w-full rounded-lg border border-border p-4 text-left hover:bg-muted/30"
                        >
                          <p className="text-sm font-medium text-foreground">
                            {t("version", { number: version.versionNumber })}
                          </p>
                          <p className="mt-1 text-xs text-muted-foreground">{formatDate(version.editedAt)}</p>
                        </button>
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
            <p className="mt-4 font-semibold text-foreground">{t("selectLyricsToPreview")}</p>
          </div>
        )}
      </div>

      <Dialog
        open={versionDialogOpen}
        onOpenChange={(open) => {
          setVersionDialogOpen(open)
          if (!open) {
            setSelectedVersion(null)
          }
        }}
      >
        <DialogContent className="flex max-h-[90dvh] flex-col gap-0 overflow-hidden p-0 sm:max-w-2xl">
          <DialogHeader className="shrink-0 border-b px-6 py-4">
            <DialogTitle>
              {selectedVersion ? t("version", { number: selectedVersion.versionNumber }) : t("versionPreview")}
            </DialogTitle>
            <DialogDescription>
              {selectedVersion ? formatDate(selectedVersion.editedAt) : t("selectVersionPreviewRestore")}
            </DialogDescription>
          </DialogHeader>
          {selectedVersion ? (
            <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-6 py-4">
              <div>
                <Label className="text-xs text-muted-foreground">{t("title")}</Label>
                <p className="mt-1 text-sm font-medium text-foreground">{selectedVersion.title}</p>
              </div>
              <div className="flex min-h-0 flex-1 flex-col">
                <Label className="text-xs text-muted-foreground">{t("lyrics")}</Label>
                <div className="mt-1 min-h-[120px] flex-1 overflow-y-auto rounded-md border border-border bg-muted/30 p-3">
                  <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed text-foreground">
                    {selectedVersion.body}
                  </pre>
                </div>
              </div>
            </div>
          ) : null}
          {selectedVersion ? (
            <div className="shrink-0 border-t px-6 py-4">
              <Button
                onClick={() => void handleRestoreVersion()}
                disabled={restoringVersion || !selectedLyric}
                className="w-full"
              >
                {restoringVersion
                  ? t("restoring")
                  : selectedLyric?.locked
                    ? t("restoreAsNewCopy")
                    : t("restore")}
              </Button>
            </div>
          ) : null}
        </DialogContent>
      </Dialog>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="flex max-h-[90dvh] flex-col gap-0 overflow-hidden p-0 sm:max-w-lg">
          <DialogHeader className="shrink-0 border-b px-6 py-4">
            <DialogTitle>{t("newLyrics")}</DialogTitle>
            <DialogDescription>{t("writeArmenianLyricsToSave")}</DialogDescription>
          </DialogHeader>
          <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-6 py-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="lyric-title">{t("title")}</Label>
              <Input id="lyric-title" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="lyric-content">{t("lyrics")}</Label>
              <Textarea
                id="lyric-content"
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                className="field-sizing-fixed min-h-[180px] max-h-[45vh] resize-y overflow-y-auto"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
          </div>
          <div className="shrink-0 border-t px-6 py-4">
            <Button
              onClick={handleCreate}
              disabled={!newTitle.trim() || !newContent.trim()}
              className="w-full sm:w-auto sm:ml-auto sm:flex"
            >
              {t("save")}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="flex max-h-[90dvh] flex-col gap-0 overflow-hidden p-0 sm:max-w-lg">
          <DialogHeader className="shrink-0 border-b px-6 py-4">
            <DialogTitle>
              {t("edit")} {t("lyrics")}
            </DialogTitle>
            <DialogDescription>{t("lockedLyricsCannotBeEdited")}</DialogDescription>
          </DialogHeader>
          <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-6 py-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-lyric-title">{t("title")}</Label>
              <Input id="edit-lyric-title" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-lyric-content">{t("lyrics")}</Label>
              <Textarea
                id="edit-lyric-content"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="field-sizing-fixed min-h-[180px] max-h-[45vh] resize-y overflow-y-auto"
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
          </div>
          <div className="shrink-0 border-t px-6 py-4">
            <Button
              onClick={handleSaveEdit}
              disabled={!editTitle.trim() || !editContent.trim()}
              className="w-full sm:w-auto sm:ml-auto sm:flex"
            >
              {t("save")}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </main>
  )
}
