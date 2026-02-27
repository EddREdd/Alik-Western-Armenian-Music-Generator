"use client"

import { useState, useRef, useEffect } from "react"
import {
  Play,
  Pause,
  Clock,
  Download,
  Trash2,
  MoreHorizontal,
  Search,
  Music,
  Filter,
  ChevronRight,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { SongDetailPage, type SongDetail } from "@/components/song-detail-page"

export interface LibrarySong {
  id: string
  title: string
  genre: string
  duration: string
  createdAt: string
  status?: "completed" | "generating" | "failed"
  prompt?: string
  lyrics?: string
  lyricsId?: string
  lyricsTitle?: string
}

interface LibraryPageProps {
  onPlaySong?: (song: LibrarySong) => void
  onNavigateToLyrics?: (lyricsId: string) => void
}

const initialLibrarySongs: LibrarySong[] = [
  {
    id: "1",
    title: "Electric Sunset",
    genre: "Synthwave",
    duration: "3:24",
    createdAt: "Feb 20, 2026",
    lyrics: "Cruising down the boulevard\nCity lights are shining bright\nThe radio plays our favorite song\nAs we disappear into the night\n\n[Chorus]\nMidnight drive, we're alive\nNothing left to fear tonight\nWindows down, turn the sound\nLet the music take us higher",
    lyricsId: "1",
    lyricsTitle: "Midnight Drive",
    prompt: "Synthwave, retro 80s, pulsing bass, neon vibes",
  },
  {
    id: "2",
    title: "Mountain Echo",
    genre: "Folk",
    duration: "4:12",
    createdAt: "Feb 19, 2026",
    lyrics: "The tide rolls in, the tide rolls out\nWashing all my fears away\nSalt and sand between my toes\nI could stay here every day",
    lyricsId: "2",
    lyricsTitle: "Ocean Waves",
    prompt: "Acoustic folk, warm, fingerpicking",
  },
  {
    id: "3",
    title: "Urban Pulse",
    genre: "Hip-Hop",
    duration: "2:58",
    createdAt: "Feb 18, 2026",
    prompt: "Lo-fi hip hop, chill beats, jazzy",
  },
  {
    id: "4",
    title: "Starlight Waltz",
    genre: "Classical",
    duration: "5:01",
    createdAt: "Feb 17, 2026",
    prompt: "Orchestral, romantic, sweeping strings",
  },
  {
    id: "5",
    title: "Neon Rush",
    genre: "Electronic",
    duration: "3:45",
    createdAt: "Feb 14, 2026",
    prompt: "EDM, high energy, drop bass",
  },
  {
    id: "6",
    title: "Desert Wind",
    genre: "Ambient",
    duration: "6:33",
    createdAt: "Feb 12, 2026",
    prompt: "Ambient, atmospheric, ethereal pads",
  },
  {
    id: "7",
    title: "City Lights",
    genre: "Pop",
    duration: "3:12",
    createdAt: "Feb 10, 2026",
    prompt: "Pop, catchy hook, modern production",
  },
  {
    id: "8",
    title: "Thunder Road",
    genre: "Rock",
    duration: "4:45",
    createdAt: "Feb 8, 2026",
    prompt: "Rock, guitar driven, powerful drums",
  },
]

export function LibraryPage({ onPlaySong, onNavigateToLyrics }: LibraryPageProps) {
  const [search, setSearch] = useState("")
  const [playingId, setPlayingId] = useState<string | null>(null)
  const [progress, setProgress] = useState<Record<string, number>>({})
  const [songs, setSongs] = useState<LibrarySong[]>(initialLibrarySongs)
  const [selectedSong, setSelectedSong] = useState<LibrarySong | null>(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [songToDelete, setSongToDelete] = useState<string | null>(null)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [])

  const togglePlay = (song: LibrarySong, e?: React.MouseEvent) => {
    if (e) e.stopPropagation()
    const songId = song.id
    if (playingId === songId) {
      setPlayingId(null)
      if (intervalRef.current) clearInterval(intervalRef.current)
    } else {
      setPlayingId(songId)
      if (intervalRef.current) clearInterval(intervalRef.current)
      if (!progress[songId]) setProgress((p) => ({ ...p, [songId]: 0 }))
      intervalRef.current = setInterval(() => {
        setProgress((prev) => {
          const c = prev[songId] || 0
          if (c >= 100) {
            setPlayingId(null)
            if (intervalRef.current) clearInterval(intervalRef.current)
            return { ...prev, [songId]: 0 }
          }
          return { ...prev, [songId]: c + 0.5 }
        })
      }, 100)
      
      if (onPlaySong) {
        onPlaySong({
          ...song,
          status: "completed",
        })
      }
    }
  }

  const handleSongClick = (song: LibrarySong) => {
    setSelectedSong(song)
  }

  const handleBack = () => {
    setSelectedSong(null)
  }

  const handleDeleteSong = (songId: string) => {
    setSongs(songs.filter((s) => s.id !== songId))
    if (selectedSong?.id === songId) {
      setSelectedSong(null)
    }
    if (playingId === songId) {
      setPlayingId(null)
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }

  const openDeleteDialog = (songId: string, e: React.MouseEvent) => {
    e.stopPropagation()
    setSongToDelete(songId)
    setDeleteDialogOpen(true)
  }

  const confirmDelete = () => {
    if (songToDelete) {
      handleDeleteSong(songToDelete)
      setSongToDelete(null)
    }
    setDeleteDialogOpen(false)
  }

  const filtered = songs.filter((s) =>
    s.title.toLowerCase().includes(search.toLowerCase()) ||
    s.genre.toLowerCase().includes(search.toLowerCase())
  )

  // Show song detail page if a song is selected
  if (selectedSong) {
    return (
      <SongDetailPage
        song={selectedSong as SongDetail}
        onBack={handleBack}
        onDelete={handleDeleteSong}
        onNavigateToLyrics={onNavigateToLyrics}
        onPlaySong={(song) => {
          if (onPlaySong) {
            onPlaySong(song as LibrarySong)
          }
        }}
      />
    )
  }

  return (
    <main className="flex-1 overflow-hidden">
      <div className="mx-auto max-w-4xl px-4 py-6 lg:px-6 lg:py-8">
        <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-xl font-bold tracking-wide text-foreground lg:text-2xl">
              Song Library
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              All your generated songs in one place
            </p>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative flex-1 lg:flex-none">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search songs..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full lg:w-64 pl-9 border-border bg-card text-foreground"
              />
            </div>
            <Button variant="outline" size="sm" className="hidden lg:flex gap-1.5 border-border text-foreground">
              <Filter className="h-3.5 w-3.5" />
              Filter
            </Button>
          </div>
        </div>

        <ScrollArea className="h-[calc(100vh-220px)]">
          <div className="flex flex-col gap-2">
            {filtered.map((song) => (
              <div
                key={song.id}
                onClick={() => handleSongClick(song)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault()
                    handleSongClick(song)
                  }
                }}
                className={`group flex w-full cursor-pointer items-center gap-3 rounded-xl border bg-card p-3 text-left transition-all lg:gap-4 lg:p-4 ${
                  playingId === song.id
                    ? "border-secondary shadow-md shadow-secondary/10"
                    : "border-border hover:border-primary/30 hover:shadow-sm"
                }`}
              >
                <div
                  onClick={(e) => togglePlay(song, e)}
                  className="relative h-12 w-12 shrink-0 cursor-pointer overflow-hidden rounded-lg"
                  role="button"
                  aria-label={playingId === song.id ? "Pause" : "Play"}
                >
                  <img
                    src={`https://picsum.photos/seed/${song.id}/100/100`}
                    alt=""
                    className="absolute inset-0 h-full w-full object-cover"
                  />
                  <div className="absolute inset-0 bg-primary/70 transition-colors hover:bg-primary/80" />
                  <div className="absolute inset-0 flex items-center justify-center">
                    {playingId === song.id ? (
                      <Pause className="h-5 w-5 text-white" />
                    ) : (
                      <Play className="h-5 w-5 pl-0.5 text-white" />
                    )}
                  </div>
                </div>

                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{song.title}</p>
                  <div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                    <Badge variant="outline" className="border-primary/20 text-primary text-xs font-medium">
                      {song.genre}
                    </Badge>
                    <span className="flex items-center gap-0.5">
                      <Clock className="h-3 w-3" />
                      {song.duration}
                    </span>
                    <span className="hidden lg:inline">{song.createdAt}</span>
                  </div>
                  {playingId === song.id && (
                    <div className="mt-2 h-1 overflow-hidden rounded-full bg-primary/10">
                      <div
                        className="h-full rounded-full bg-secondary transition-all"
                        style={{ width: `${progress[song.id] || 0}%` }}
                      />
                    </div>
                  )}
                </div>

                <ChevronRight className="h-5 w-5 shrink-0 text-muted-foreground/50" />

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 shrink-0 p-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreHorizontal className="h-4 w-4" />
                      <span className="sr-only">Song options</span>
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={(e) => e.stopPropagation()}>
                      <Download className="mr-2 h-4 w-4" />
                      Download
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="text-destructive"
                      onClick={(e) => openDeleteDialog(song.id, e)}
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            ))}

            {filtered.length === 0 && (
              <div className="flex flex-col items-center py-16 text-center">
                <Music className="h-12 w-12 text-muted-foreground/40" />
                <p className="mt-4 font-semibold text-foreground">No songs found</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Try adjusting your search
                </p>
              </div>
            )}
          </div>
        </ScrollArea>
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Song</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete this song? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  )
}
