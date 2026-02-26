"use client"

import { useState } from "react"
import {
  FileText,
  Plus,
  Search,
  MoreHorizontal,
  Trash2,
  Edit3,
  Calendar,
  ArrowLeft,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { ScrollArea } from "@/components/ui/scroll-area"
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

interface LyricsEntry {
  id: string
  title: string
  content: string
  createdAt: string
  wordCount: number
}

const initialLyrics: LyricsEntry[] = [
  {
    id: "1",
    title: "Midnight Drive",
    content:
      "Cruising down the boulevard\nCity lights are shining bright\nThe radio plays our favorite song\nAs we disappear into the night\n\n[Chorus]\nMidnight drive, we're alive\nNothing left to fear tonight\nWindows down, turn the sound\nLet the music take us higher",
    createdAt: "Feb 20, 2026",
    wordCount: 48,
  },
  {
    id: "2",
    title: "Ocean Waves",
    content:
      "The tide rolls in, the tide rolls out\nWashing all my fears away\nSalt and sand between my toes\nI could stay here every day\n\n[Chorus]\nOcean waves, carry me\nTo a place where I am free\nUnderneath the golden sun\nWhere the sky and water run as one",
    createdAt: "Feb 15, 2026",
    wordCount: 52,
  },
  {
    id: "3",
    title: "Rising Up",
    content:
      "From the ashes, from the dust\nWe rise above the noise of rust\nBroken chains upon the floor\nWe won't be silent anymore\n\n[Chorus]\nRising up, standing tall\nWe won't break, we won't fall\nHand in hand, side by side\nThis is where the fire resides",
    createdAt: "Feb 10, 2026",
    wordCount: 50,
  },
  {
    id: "4",
    title: "Summer Haze",
    content:
      "Lazy afternoons and lemonade\nThe sun is golden, the air is warm\nFireflies dance in the evening shade\nA gentle breeze before the storm\n\n[Chorus]\nSummer haze, golden days\nMemories that never fade\nClose your eyes, feel the sun\nSummer's only just begun",
    createdAt: "Feb 5, 2026",
    wordCount: 46,
  },
  {
    id: "5",
    title: "Neon Dreams",
    content:
      "Electric pulses through my veins\nNeon signs reflect the rain\nCity never sleeps tonight\nEvery shadow burns so bright\n\n[Chorus]\nNeon dreams, laser beams\nNothing's ever what it seems\nLost in light, hold on tight\nWe're the stars of this neon night",
    createdAt: "Jan 30, 2026",
    wordCount: 47,
  },
]

type MobileView = "list" | "preview" | "create"

export function LyricsPage() {
  const [search, setSearch] = useState("")
  const [lyrics, setLyrics] = useState(initialLyrics)
  const [createOpen, setCreateOpen] = useState(false)
  const [newTitle, setNewTitle] = useState("")
  const [newContent, setNewContent] = useState("")
  const [selectedLyric, setSelectedLyric] = useState<LyricsEntry | null>(null)
  const [mobileView, setMobileView] = useState<MobileView>("list")

  const filtered = lyrics.filter(
    (l) =>
      l.title.toLowerCase().includes(search.toLowerCase()) ||
      l.content.toLowerCase().includes(search.toLowerCase())
  )

  const handleCreate = () => {
    if (!newTitle.trim() || !newContent.trim()) return
    const entry: LyricsEntry = {
      id: Date.now().toString(),
      title: newTitle,
      content: newContent,
      createdAt: "Just now",
      wordCount: newContent.split(/\s+/).filter(Boolean).length,
    }
    setLyrics([entry, ...lyrics])
    setNewTitle("")
    setNewContent("")
    setCreateOpen(false)
    setMobileView("list")
  }

  const handleDelete = (id: string) => {
    setLyrics(lyrics.filter((l) => l.id !== id))
    if (selectedLyric?.id === id) {
      setSelectedLyric(null)
      setMobileView("list")
    }
  }

  const handleSelectLyric = (entry: LyricsEntry) => {
    setSelectedLyric(entry)
    setMobileView("preview")
  }

  const handleMobileNewClick = () => {
    setNewTitle("")
    setNewContent("")
    setMobileView("create")
  }

  const handleMobileBack = () => {
    setMobileView("list")
    setSelectedLyric(null)
  }

  // --- Mobile/Tablet: Full-screen Create View ---
  if (mobileView === "create") {
    return (
      <main className="flex flex-1 flex-col overflow-hidden lg:hidden">
        <div className="flex items-center gap-3 border-b border-border px-4 py-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleMobileBack}
            className="h-8 w-8 p-0 text-foreground"
          >
            <ArrowLeft className="h-5 w-5" />
            <span className="sr-only">Back to lyrics list</span>
          </Button>
          <h1 className="text-lg font-bold tracking-wide text-foreground">
            New Lyrics
          </h1>
        </div>
        <ScrollArea className="flex-1">
          <div className="flex flex-col gap-4 p-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-lyric-title" className="text-foreground">
                Title
              </Label>
              <Input
                id="mobile-lyric-title"
                placeholder="Enter a title..."
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                className="border-border text-foreground"
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="mobile-lyric-content" className="text-foreground">
                Lyrics
              </Label>
              <Textarea
                id="mobile-lyric-content"
                placeholder="Write your lyrics here..."
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                className="min-h-[300px] resize-none border-border text-foreground"
              />
            </div>
            <Button
              onClick={handleCreate}
              disabled={!newTitle.trim() || !newContent.trim()}
              className="bg-secondary text-secondary-foreground hover:bg-secondary/90 font-semibold"
            >
              Save to Library
            </Button>
          </div>
        </ScrollArea>
      </main>
    )
  }

  // --- Mobile/Tablet: Full-screen Preview View ---
  if (mobileView === "preview" && selectedLyric) {
    return (
      <main className="flex flex-1 flex-col overflow-hidden lg:hidden">
        <div className="flex items-center gap-3 border-b border-border px-4 py-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleMobileBack}
            className="h-8 w-8 p-0 text-foreground"
          >
            <ArrowLeft className="h-5 w-5" />
            <span className="sr-only">Back to lyrics list</span>
          </Button>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-lg font-bold tracking-wide text-foreground">
              {selectedLyric.title}
            </h1>
            <div className="flex items-center gap-3 text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {selectedLyric.createdAt}
              </span>
              <span>{selectedLyric.wordCount} words</span>
            </div>
          </div>
          <Button
            variant="outline"
            size="sm"
            className="shrink-0 gap-1.5 border-border text-foreground"
          >
            <Edit3 className="h-3.5 w-3.5" />
            Edit
          </Button>
        </div>
        <ScrollArea className="flex-1 px-4 py-5">
          <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed text-foreground/90">
            {selectedLyric.content}
          </pre>
        </ScrollArea>
      </main>
    )
  }

  // --- Default: List + Desktop Preview ---
  return (
    <main className="flex flex-1 overflow-hidden">
      {/* Lyrics List */}
      <div className="w-full lg:w-1/3 lg:border-r border-border">
        <div className="flex flex-col gap-3 border-b border-border p-4">
          <div className="flex items-center justify-between">
            <h1 className="text-lg font-bold tracking-wide text-foreground">
              Lyrics Library
            </h1>
            {/* Desktop: opens dialog */}
            <Button
              size="sm"
              onClick={() => setCreateOpen(true)}
              className="hidden lg:flex gap-1.5 bg-secondary text-secondary-foreground hover:bg-secondary/90"
            >
              <Plus className="h-3.5 w-3.5" />
              New
            </Button>
            {/* Mobile/Tablet: navigates to full-screen create */}
            <Button
              size="sm"
              onClick={handleMobileNewClick}
              className="flex lg:hidden gap-1.5 bg-secondary text-secondary-foreground hover:bg-secondary/90"
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
              className="pl-9 border-border bg-card text-foreground"
            />
          </div>
        </div>

        <ScrollArea className="h-[calc(100vh-200px)]">
          <div className="flex flex-col gap-1 p-2">
            {filtered.map((entry) => (
              <button
                key={entry.id}
                onClick={() => handleSelectLyric(entry)}
                className={`group flex w-full items-start gap-3 rounded-lg p-3 text-left transition-colors ${
                  selectedLyric?.id === entry.id
                    ? "bg-primary/10 border border-primary/20"
                    : "hover:bg-muted border border-transparent"
                }`}
              >
                <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-secondary/10">
                  <FileText className="h-4 w-4 text-secondary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-foreground">
                    {entry.title}
                  </p>
                  <p className="mt-0.5 truncate text-xs text-muted-foreground">
                    {entry.content.split("\n")[0]}
                  </p>
                  <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground/60">
                    <span>{entry.wordCount} words</span>
                    <span>{entry.createdAt}</span>
                  </div>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 w-6 p-0 text-muted-foreground opacity-0 group-hover:opacity-100"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreHorizontal className="h-3.5 w-3.5" />
                      <span className="sr-only">Lyrics options</span>
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem>
                      <Edit3 className="mr-2 h-4 w-4" />
                      Edit
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="text-destructive"
                      onClick={() => handleDelete(entry.id)}
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </button>
            ))}
          </div>
        </ScrollArea>
      </div>

      {/* Lyrics Preview - Desktop only */}
      <div className="hidden lg:block flex-1 bg-primary/[0.03]">
        {selectedLyric ? (
          <div className="flex h-full flex-col">
            <div className="flex items-center justify-between border-b border-border px-8 py-5">
              <div>
                <h2 className="text-xl font-bold text-foreground">
                  {selectedLyric.title}
                </h2>
                <div className="mt-1 flex items-center gap-3 text-sm text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <Calendar className="h-3.5 w-3.5" />
                    {selectedLyric.createdAt}
                  </span>
                  <span>{selectedLyric.wordCount} words</span>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 border-border text-foreground"
              >
                <Edit3 className="h-3.5 w-3.5" />
                Edit
              </Button>
            </div>
            <ScrollArea className="flex-1 px-8 py-6">
              <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed text-foreground/90">
                {selectedLyric.content}
              </pre>
            </ScrollArea>
          </div>
        ) : (
          <div className="flex h-full flex-col items-center justify-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
              <FileText className="h-8 w-8 text-primary" />
            </div>
            <p className="mt-4 font-semibold text-foreground">
              Select lyrics to preview
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              Choose from your library on the left
            </p>
          </div>
        )}
      </div>

      {/* Create Lyrics Dialog - Desktop only */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="text-foreground">New Lyrics</DialogTitle>
            <DialogDescription className="text-muted-foreground">
              Write new lyrics to save to your library.
            </DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="lyric-title" className="text-foreground">
                Title
              </Label>
              <Input
                id="lyric-title"
                placeholder="Enter a title..."
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                className="border-border text-foreground"
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="lyric-content" className="text-foreground">
                Lyrics
              </Label>
              <Textarea
                id="lyric-content"
                placeholder="Write your lyrics here..."
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                className="min-h-[200px] resize-none border-border text-foreground"
              />
            </div>
            <Button
              onClick={handleCreate}
              disabled={!newTitle.trim() || !newContent.trim()}
              className="bg-secondary text-secondary-foreground hover:bg-secondary/90 font-semibold"
            >
              Save to Library
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </main>
  )
}
