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
import { MAX_SEARCH_KEYWORD_LENGTH } from "@/lib/api-base"
import { t, useUiLanguage } from "@/lib/i18n"

const SEARCH_DEBOUNCE_MS = 400

interface LyricsImportDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelect: (lyric: Lyric, source: "my" | "public") => void
  source: "my" | "public"
  defaultPublicLanguage?: "WESTERN_ARMENIAN"
}

export function LyricsImportDialog({
  open,
  onOpenChange,
  onSelect,
  source,
  defaultPublicLanguage = "WESTERN_ARMENIAN",
}: LyricsImportDialogProps) {
  useUiLanguage()
  const [searchInput, setSearchInput] = useState("")
  const [debouncedSearch, setDebouncedSearch] = useState("")
  const [lyrics, setLyrics] = useState<LyricSummary[]>([])
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const [publicLanguageFilter, setPublicLanguageFilter] = useState<"ALL" | "WESTERN_ARMENIAN">("ALL")

  useEffect(() => {
    if (!open) {
      return
    }
    setSearchInput("")
    setDebouncedSearch("")
    setError("")
    if (source === "public") {
      setPublicLanguageFilter(defaultPublicLanguage)
    }
  }, [open, source, defaultPublicLanguage])

  useEffect(() => {
    if (!open) {
      return
    }
    const timer = window.setTimeout(() => {
      setDebouncedSearch(searchInput.trim().slice(0, MAX_SEARCH_KEYWORD_LENGTH))
    }, SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [open, searchInput])

  useEffect(() => {
    if (!open) {
      return
    }

    setLoading(true)
    const loader =
      source === "my"
        ? listMyLyrics()
        : listPublicLyrics({
            keyword: debouncedSearch || undefined,
            language: publicLanguageFilter === "ALL" ? undefined : publicLanguageFilter,
          })

    void loader
      .then((items) => {
        setLyrics(items)
        setError("")
      })
      .catch((loadError) => {
        setError(loadError instanceof Error ? loadError.message : t("unableToLoadLyrics"))
      })
      .finally(() => {
        setLoading(false)
      })
  }, [open, source, debouncedSearch, publicLanguageFilter])

  const filtered = useMemo(() => {
    const query = searchInput.toLowerCase().trim()
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
  }, [lyrics, searchInput])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[90dvh] flex-col gap-0 overflow-hidden p-0 sm:max-w-xl">
        <DialogHeader className="shrink-0 border-b px-6 py-4">
          <DialogTitle className="text-foreground">
            {source === "my" ? t("importFromMyLyrics") : t("importFromPublicLyrics")}
          </DialogTitle>
          <DialogDescription className="text-muted-foreground">
            {source === "my"
              ? t("selectLyricsFromSaved")
              : t("selectLyricsFromPublic")}
          </DialogDescription>
        </DialogHeader>
        <div className="flex min-h-0 flex-1 flex-col gap-3 overflow-hidden px-6 py-4">
        {source === "public" ? (
          <div className="flex items-center gap-3 pb-2">
            <Label className="text-xs text-muted-foreground" htmlFor="public-language-filter">
              {t("readyLibrary")}
            </Label>
            <select
              id="public-language-filter"
              value={publicLanguageFilter}
              onChange={(e) => setPublicLanguageFilter(e.target.value as "ALL" | "WESTERN_ARMENIAN")}
              className="h-9 flex-1 rounded-md border border-border bg-card px-2 text-sm text-foreground"
            >
              <option value="ALL">{t("all")}</option>
              <option value="WESTERN_ARMENIAN">{t("westernArmenian")}</option>
            </select>
          </div>
        ) : null}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            id="lyrics-import-search"
            name="lyrics-import-search"
            autoComplete="off"
            placeholder={source === "my" ? t("searchYourLyrics") : t("searchPublicLyrics")}
            value={searchInput}
            onChange={(e) =>
              setSearchInput(e.target.value.slice(0, MAX_SEARCH_KEYWORD_LENGTH))
            }
            className="pl-9"
          />
        </div>
        <ScrollArea className="min-h-0 flex-1">
          <div className="flex flex-col gap-2 pr-4">
            {loading ? (
              <div className="py-8 text-center text-sm text-muted-foreground">{t("loading")}</div>
            ) : null}
            {!loading &&
              filtered.map((item) => (
                <button
                  key={item.id}
                  type="button"
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
                          : t("unableToLoadLyric"),
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
                      {t("wordsCount", { count: item.wordCount })}
                      {source === "my" && item.locked ? ` - ${t("locked")}` : ""}
                    </p>
                  </div>
                </button>
              ))}
            {error ? (
              <div className="py-4 text-center text-sm text-destructive">{error}</div>
            ) : null}
            {!loading && filtered.length === 0 && (
              <div className="py-8 text-center text-sm text-muted-foreground">
                {t("noLyricsFoundMatchingSearch")}
              </div>
            )}
          </div>
        </ScrollArea>
        </div>
      </DialogContent>
    </Dialog>
  )
}
