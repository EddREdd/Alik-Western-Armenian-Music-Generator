"use client"

import { FileText, Search } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { useEffect, useState } from "react"
import { getLyric, listLyrics, type Lyric, type LyricSummary } from "@/lib/lyrics-api"

interface LyricsImportDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelect: (lyric: Lyric) => void
  projectId: string
}

export function LyricsImportDialog({
  open,
  onOpenChange,
  onSelect,
  projectId,
}: LyricsImportDialogProps) {
  const [search, setSearch] = useState("")
  const [lyrics, setLyrics] = useState<LyricSummary[]>([])
  const [error, setError] = useState("")

  useEffect(() => {
    if (!open || !projectId.trim()) {
      return
    }

    void listLyrics(projectId)
      .then((items) => {
        setLyrics(items)
        setError("")
      })
      .catch((loadError) => {
        setError(loadError instanceof Error ? loadError.message : "Unable to load lyrics")
      })
  }, [open, projectId])

  const filtered = lyrics.filter(
    (l) =>
      l.title.toLowerCase().includes(search.toLowerCase()) ||
      l.bodyPreview.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-foreground">Import from Lyrics Library</DialogTitle>
          <DialogDescription className="text-muted-foreground">
            Select lyrics from your saved collection to use in your song.
          </DialogDescription>
        </DialogHeader>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search your lyrics..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <ScrollArea className="h-72">
          <div className="flex flex-col gap-2 pr-4">
            {filtered.map((item) => (
              <button
                key={item.id}
                onClick={async () => {
                  try {
                    const lyric = await getLyric(item.id)
                    onSelect(lyric)
                    onOpenChange(false)
                  } catch (selectError) {
                    setError(
                      selectError instanceof Error
                        ? selectError.message
                        : "Unable to load lyric",
                    )
                  }
                }}
                className="flex items-start gap-3 rounded-lg border border-border bg-card p-3 text-left transition-colors hover:border-secondary hover:bg-accent/20"
              >
                <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-primary/10">
                  <FileText className="h-4 w-4 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{item.title}</p>
                  <p className="mt-0.5 truncate text-xs text-muted-foreground">
                    {item.bodyPreview}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground/60">
                    {item.wordCount} words
                    {item.locked ? " • Locked" : ""}
                  </p>
                </div>
              </button>
            ))}
            {error ? (
              <div className="py-4 text-center text-sm text-destructive">{error}</div>
            ) : null}
            {filtered.length === 0 && (
              <div className="py-8 text-center text-sm text-muted-foreground">
                No lyrics found matching your search.
              </div>
            )}
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}
