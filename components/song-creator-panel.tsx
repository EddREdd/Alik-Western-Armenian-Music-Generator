"use client"

import { useState } from "react"
import { Import, Sparkles, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { LyricsImportDialog } from "@/components/lyrics-import-dialog"
import type { Lyric } from "@/lib/lyrics-api"
import { t, useUiLanguage } from "@/lib/i18n"

interface SongCreatorPanelProps {
  onGenerate: (data: {
    lyricId?: string | null
    title: string
    lyrics: string
    stylePrompt: string
  }) => void
  isGenerating: boolean
  errorMessage?: string
}

export function SongCreatorPanel({
  onGenerate,
  isGenerating,
  errorMessage,
}: SongCreatorPanelProps) {
  useUiLanguage()
  const [lyrics, setLyrics] = useState("")
  const [stylePrompt, setStylePrompt] = useState("")
  const [title, setTitle] = useState("")
  const [importMyOpen, setImportMyOpen] = useState(false)
  const [importPublicOpen, setImportPublicOpen] = useState(false)
  const [selectedLyricId, setSelectedLyricId] = useState<string | null>(null)
  const [selectedLyricLocked, setSelectedLyricLocked] = useState(false)

  const handleImportLyrics = (lyric: Lyric, source: "my" | "public") => {
    if (source === "my") {
      setSelectedLyricId(lyric.id)
      setSelectedLyricLocked(Boolean(lyric.locked))
    } else {
      setSelectedLyricId(null)
      setSelectedLyricLocked(false)
    }
    setLyrics(lyric.body)
    if (!title) setTitle(lyric.title)
  }

  const handleGenerate = () => {
    const normalizedTitle = title.trim() || "Untitled Song"
    const trimmedLyrics = lyrics.trim()
    if (!stylePrompt.trim()) return
    if (trimmedLyrics.length < 50) return

    onGenerate({
      lyricId: selectedLyricId,
      title: normalizedTitle,
      lyrics,
      stylePrompt,
    })
  }

  const trimmedLyrics = lyrics.trim()
  const lyricCharCount = trimmedLyrics.length
  const lyricIsValid = lyricCharCount >= 50

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border px-6 py-4">
        <h2 className="text-lg font-bold tracking-wide text-foreground">
          {t("createNewSong")}
        </h2>
        <p className="mt-0.5 text-sm text-muted-foreground">
          Add your lyrics and describe the style you want
        </p>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5">
        <div className="flex flex-col gap-6">
          {/* Song Title */}
          <div className="flex flex-col gap-2">
            <Label htmlFor="title" className="text-sm font-semibold text-foreground">
              {t("songTitle")}
            </Label>
            <Input
              id="title"
              placeholder="Give your song a name..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="border-border bg-card text-foreground placeholder:text-muted-foreground"
            />
          </div>

          {/* Lyrics Field */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="lyrics" className="text-sm font-semibold text-foreground">
                {t("lyrics")}
              </Label>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setImportMyOpen(true)}
                  className="gap-1.5 border-secondary/40 text-secondary hover:bg-secondary/10 hover:text-secondary"
                >
                  <Import className="h-3.5 w-3.5" />
                  {t("importFromMyLyrics")}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setImportPublicOpen(true)}
                  className="gap-1.5 border-secondary/40 text-secondary hover:bg-secondary/10 hover:text-secondary"
                >
                  <Import className="h-3.5 w-3.5" />
                  {t("importFromPublicLyrics")}
                </Button>
              </div>
            </div>
            <Textarea
              id="lyrics"
              placeholder={
                `[Verse 1]\nWrite your first verse...\n\n[Chorus]\nWrite your chorus...`
              }
              value={lyrics}
              onChange={(e) => {
                setLyrics(e.target.value)
                if (selectedLyricId) {
                  setSelectedLyricId(null)
                  setSelectedLyricLocked(false)
                }
              }}
              className="min-h-[220px] resize-none border-border bg-card font-mono text-sm text-foreground placeholder:text-muted-foreground"
            />
            <div className="flex items-center justify-between gap-3 text-xs">
              <p className="text-muted-foreground">
                {lyricCharCount}/50 characters minimum
              </p>
              {!lyricIsValid && lyricCharCount > 0 ? (
                <p className="text-destructive">{t("lyricsMinChars")}</p>
              ) : null}
            </div>
            {selectedLyricId ? (
              <p className="text-xs text-secondary">
                Using saved lyric {selectedLyricLocked ? "(locked)" : ""}. Editing the text will create a new lyric entry.
              </p>
            ) : null}
          </div>

          {/* Generation Prompt */}
          <div className="flex flex-col gap-2">
            <Label htmlFor="prompt" className="text-sm font-semibold text-foreground">
              {t("stylePrompt")}
            </Label>
            <Textarea
              id="prompt"
              placeholder="Describe the style, genre, mood, instruments, tempo, etc. Example: Upbeat indie pop with acoustic guitar, warm vocals, 120 BPM"
              value={stylePrompt}
              onChange={(e) => setStylePrompt(e.target.value)}
              className="min-h-[100px] resize-none border-border bg-card text-sm text-foreground placeholder:text-muted-foreground"
            />
          </div>

          {errorMessage && (
            <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {errorMessage}
            </div>
          )}
        </div>
      </div>

      {/* Generate Button */}
      <div className="border-t border-border px-6 py-4">
        <Button
          onClick={handleGenerate}
          disabled={
            isGenerating ||
            !title.trim() ||
            !stylePrompt.trim() ||
            !lyricIsValid
          }
          className="w-full gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90 font-semibold tracking-wide"
          size="lg"
        >
          {isGenerating ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Generating...
            </>
          ) : (
            <>
              <Sparkles className="h-4 w-4" />
              {t("generateSong")}
            </>
          )}
        </Button>
      </div>

      <LyricsImportDialog
        open={importMyOpen}
        onOpenChange={setImportMyOpen}
        onSelect={handleImportLyrics}
        source="my"
      />

      <LyricsImportDialog
        open={importPublicOpen}
        onOpenChange={setImportPublicOpen}
        onSelect={handleImportLyrics}
        source="public"
        defaultPublicLanguage="WESTERN_ARMENIAN"
      />
    </div>
  )
}
