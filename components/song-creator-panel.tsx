"use client"

import { useState } from "react"
import { Import, Sparkles, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { LyricsImportDialog } from "@/components/lyrics-import-dialog"

interface SongCreatorPanelProps {
  onGenerate: (data: {
    title: string
    lyrics: string
    prompt: string
  }) => void
  isGenerating: boolean
}

export function SongCreatorPanel({ onGenerate, isGenerating }: SongCreatorPanelProps) {
  const [lyrics, setLyrics] = useState("")
  const [prompt, setPrompt] = useState("")
  const [title, setTitle] = useState("")
  const [importOpen, setImportOpen] = useState(false)

  const handleImportLyrics = (importedLyrics: string, importedTitle: string) => {
    setLyrics(importedLyrics)
    if (!title) setTitle(importedTitle)
  }

  const handleGenerate = () => {
    if (!lyrics.trim() && !prompt.trim()) return
    onGenerate({ title: title || "Untitled Song", lyrics, prompt })
  }

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border px-6 py-4">
        <h2 className="text-lg font-bold tracking-wide text-foreground">
          Create New Song
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
              Song Title
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
                Lyrics
              </Label>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setImportOpen(true)}
                className="gap-1.5 border-secondary/40 text-secondary hover:bg-secondary/10 hover:text-secondary"
              >
                <Import className="h-3.5 w-3.5" />
                Import from Library
              </Button>
            </div>
            <Textarea
              id="lyrics"
              placeholder={`Paste your lyrics here...\n\n[Verse 1]\nWrite your first verse...\n\n[Chorus]\nWrite your chorus...`}
              value={lyrics}
              onChange={(e) => setLyrics(e.target.value)}
              className="min-h-[220px] resize-none border-border bg-card font-mono text-sm text-foreground placeholder:text-muted-foreground"
            />
            <p className="text-xs text-muted-foreground">
              {lyrics.length > 0
                ? `${lyrics.split(/\s+/).filter(Boolean).length} words`
                : "Paste lyrics or import from your library"}
            </p>
          </div>

          {/* Generation Prompt */}
          <div className="flex flex-col gap-2">
            <Label htmlFor="prompt" className="text-sm font-semibold text-foreground">
              Style & Generation Prompt
            </Label>
            <Textarea
              id="prompt"
              placeholder="Describe the style, genre, mood, instruments, tempo, etc. Example: Upbeat indie pop with acoustic guitar, warm vocals, 120 BPM"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              className="min-h-[100px] resize-none border-border bg-card text-sm text-foreground placeholder:text-muted-foreground"
            />
          </div>

          {/* Quick Style Tags */}
          <div className="flex flex-col gap-2">
            <Label className="text-sm font-semibold text-foreground">
              Quick Styles
            </Label>
            <div className="flex flex-wrap gap-2">
              {[
                "Pop",
                "Rock",
                "R&B",
                "Hip-Hop",
                "Jazz",
                "Country",
                "Electronic",
                "Folk",
                "Classical",
              ].map((genre) => (
                <button
                  key={genre}
                  onClick={() =>
                    setPrompt((prev) =>
                      prev ? `${prev}, ${genre.toLowerCase()}` : genre.toLowerCase()
                    )
                  }
                  className="rounded-full border border-border bg-card px-3 py-1 text-xs font-medium text-foreground transition-colors hover:border-secondary hover:bg-secondary/10 hover:text-secondary"
                >
                  {genre}
                </button>
              ))}
            </div>
          </div>


        </div>
      </div>

      {/* Generate Button */}
      <div className="border-t border-border px-6 py-4">
        <Button
          onClick={handleGenerate}
          disabled={isGenerating || (!lyrics.trim() && !prompt.trim())}
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
              Generate Song
            </>
          )}
        </Button>
      </div>

      <LyricsImportDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        onSelect={handleImportLyrics}
      />
    </div>
  )
}
