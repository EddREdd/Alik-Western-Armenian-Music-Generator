"use client"

import { useState } from "react"
import { Import, Sparkles, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { LyricsImportDialog } from "@/components/lyrics-import-dialog"
import { Switch } from "@/components/ui/switch"
import type { GenerationModel } from "@/lib/musicgen-api"
import type { Lyric } from "@/lib/lyrics-api"

interface SongCreatorPanelProps {
  onGenerate: (data: {
    projectId: string
    lyricId?: string | null
    title: string
    lyrics: string
    stylePrompt: string
    instrumental: boolean
    model: GenerationModel
  }) => void
  isGenerating: boolean
  errorMessage?: string
  defaultProjectId: string
}

export function SongCreatorPanel({
  onGenerate,
  isGenerating,
  errorMessage,
  defaultProjectId,
}: SongCreatorPanelProps) {
  const [projectId, setProjectId] = useState(defaultProjectId)
  const [lyrics, setLyrics] = useState("")
  const [stylePrompt, setStylePrompt] = useState("")
  const [title, setTitle] = useState("")
  const [instrumental, setInstrumental] = useState(false)
  const [model, setModel] = useState<GenerationModel>("V4")
  const [importOpen, setImportOpen] = useState(false)
  const [selectedLyricId, setSelectedLyricId] = useState<string | null>(null)
  const [selectedLyricLocked, setSelectedLyricLocked] = useState(false)

  const handleImportLyrics = (lyric: Lyric) => {
    setSelectedLyricId(lyric.id)
    setSelectedLyricLocked(lyric.locked)
    setLyrics(lyric.body)
    if (!title) setTitle(lyric.title)
  }

  const handleGenerate = () => {
    const normalizedTitle = title.trim() || "Untitled Song"
    if (!projectId.trim() || !stylePrompt.trim()) return
    if (!instrumental && !lyrics.trim()) return

    onGenerate({
      projectId,
      lyricId: selectedLyricId,
      title: normalizedTitle,
      lyrics,
      stylePrompt,
      instrumental,
      model,
    })
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
          <div className="flex flex-col gap-2">
            <Label htmlFor="projectId" className="text-sm font-semibold text-foreground">
              Project ID
            </Label>
            <Input
              id="projectId"
              placeholder="project-1"
              value={projectId}
              onChange={(e) => setProjectId(e.target.value)}
              className="border-border bg-card text-foreground placeholder:text-muted-foreground"
            />
          </div>

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

          <div className="grid gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <Label htmlFor="model" className="text-sm font-semibold text-foreground">
                Model
              </Label>
              <select
                id="model"
                value={model}
                onChange={(e) => setModel(e.target.value as GenerationModel)}
                className="h-10 rounded-md border border-border bg-card px-3 text-sm text-foreground"
              >
                <option value="V4">V4</option>
                <option value="V4_5">V4_5</option>
                <option value="V4_5PLUS">V4_5PLUS</option>
                <option value="V5">V5</option>
              </select>
            </div>

            <div className="flex items-end">
              <div className="flex w-full items-center justify-between rounded-md border border-border bg-card px-3 py-2.5">
                <div>
                  <p className="text-sm font-semibold text-foreground">Instrumental</p>
                  <p className="text-xs text-muted-foreground">
                    Skip lyrics and generate instrumental music
                  </p>
                </div>
                <Switch checked={instrumental} onCheckedChange={setInstrumental} />
              </div>
            </div>
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
              placeholder={
                instrumental
                  ? "Instrumental mode is on. Lyrics are optional."
                  : `[Verse 1]\nWrite your first verse...\n\n[Chorus]\nWrite your chorus...`
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
            <p className="text-xs text-muted-foreground">
              {lyrics.length > 0
                ? `${lyrics.split(/\s+/).filter(Boolean).length} words`
                : "Paste lyrics or import from your library"}
            </p>
            {selectedLyricId ? (
              <p className="text-xs text-secondary">
                Using saved lyric {selectedLyricLocked ? "(locked)" : ""}. Editing the text will create a new lyric entry.
              </p>
            ) : null}
          </div>

          {/* Generation Prompt */}
          <div className="flex flex-col gap-2">
            <Label htmlFor="prompt" className="text-sm font-semibold text-foreground">
              Style Prompt
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
            !projectId.trim() ||
            !title.trim() ||
            !stylePrompt.trim() ||
            (!instrumental && !lyrics.trim())
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
              Generate Song
            </>
          )}
        </Button>
      </div>

      <LyricsImportDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        onSelect={handleImportLyrics}
        projectId={projectId}
      />
    </div>
  )
}
