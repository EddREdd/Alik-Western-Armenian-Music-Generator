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
import { Label } from "@/components/ui/label"
import { useEffect, useMemo, useState } from "react"
import {
  getLyric,
  getPublicLyric,
  listMyLyrics,
  listPublicLyrics,
  type Lyric,
  type LyricSummary,
} from "@/lib/lyrics-api"
import { type LyricContentLanguage, t, useUiLanguage } from "@/lib/i18n"

interface LyricsImportDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelect: (lyric: Lyric, source: "my" | "public") => void
  source: "my" | "public"
  defaultPublicLanguage?: LyricContentLanguage
}

export function LyricsImportDialog({
  open,
  onOpenChange,
  onSelect,
  source,
  defaultPublicLanguage = "WESTERN_ARMENIAN",
}: LyricsImportDialogProps) {
  useUiLanguage()
  const [search, setSearch] = useState("")
  const [lyrics, setLyrics] = useState<LyricSummary[]>([])
  const [error, setError] = useState("")
  const [publicLanguageFilter, setPublicLanguageFilter] = useState<
    "ALL" | LyricContentLanguage
  >("ALL")

  useEffect(() => {
    if (!open) {
      return
    }

    const keyword = search.trim()
    const loader =
      source === "my"
        ? listMyLyrics()
        : listPublicLyrics({
            keyword: keyword || undefined,
            language: publicLanguageFilter === "ALL" ? undefined : publicLanguageFilter,
          })

    void loader
      .then((items) => {
        setLyrics(items)
        setError("")
      })
      .catch((loadError) => {
        setError(loadError instanceof Error ? loadError.message : "Unable to load lyrics")
      })
  }, [open, source, search, publicLanguageFilter])

  useEffect(() => {
    if (!open) return
    if (source === "public") {
      setPublicLanguageFilter(defaultPublicLanguage)
    }
  }, [open, source, defaultPublicLanguage])

  const filtered = useMemo(() => {
    const query = search.toLowerCase().trim()
    const seen = new Set<string>()

    return lyrics.filter((l) => {
      if (seen.has(l.id)) {
        return false
      }
      seen.add(l.id)

      if (!query) {
        return true
      }

      return (
        l.title.toLowerCase().includes(query) ||
        l.bodyPreview.toLowerCase().includes(query)
      )
    })
  }, [lyrics, search])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="text-foreground">
            {source === "my" ? t("importFromMyLyrics") : t("importFromPublicLyrics")}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground">
            {source === "my"
              ? "Select lyrics from your saved collection to use in your song."
              : `Select lyrics from the public ${t("readyLibrary")} to use in your song.`}
          </DialogDescription>
        </DialogHeader>
        {source === "public" ? (
          <div className="flex items-center gap-3 pb-2">
            <Label className="text-xs text-muted-foreground" htmlFor="public-language-filter">
              {t("readyLibrary")}
            </Label>
            <select
              id="public-language-filter"
              value={publicLanguageFilter}
              onChange={(e) => setPublicLanguageFilter(e.target.value as "ALL" | LyricContentLanguage)}
              className="h-9 flex-1 rounded-md border border-border bg-card px-2 text-sm text-foreground"
            >
              <option value="ALL">All</option>
              <option value="ENGLISH">{t("english")}</option>
              <option value="WESTERN_ARMENIAN">{t("westernArmenian")}</option>
            </select>
          </div>
        ) : null}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={source === "my" ? "Search your lyrics..." : "Search public lyrics..."}
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
                    const lyric =
                      source === "my"
                        ? await getLyric(item.id)
                        : await getPublicLyric(item.id)
                    onSelect(lyric, source)
                    onOpenChange(false)
                  } catch (selectError) {
                    setError(
                      selectError instanceof Error
                        ? selectError.message
                        : "Unable to load lyric",
                    )
                  }
                }}
                className="flex w-full items-start gap-3 overflow-hidden rounded-lg border border-border bg-card p-3 text-left transition-colors hover:border-secondary hover:bg-accent/20"
              >
                <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-primary/10">
                  <FileText className="h-4 w-4 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{item.title}</p>
                  <p className="mt-0.5 line-clamp-2 break-words text-xs text-muted-foreground">
                    {item.bodyPreview}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground/60">
                    {item.wordCount} words
                    {source === "my" && item.locked ? " • Locked" : ""}
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
