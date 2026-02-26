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

export interface LibrarySong {
  id: string
  title: string
  genre: string
  duration: string
  createdAt: string
  status?: "completed" | "generating" | "failed"
  prompt?: string
}

interface LibraryPageProps {
  onPlaySong?: (song: LibrarySong) => void
}

const librarySongs: LibrarySong[] = [
  { id: "1", title: "Electric Sunset", genre: "Synthwave", duration: "3:24", createdAt: "Feb 20, 2026" },
  { id: "2", title: "Mountain Echo", genre: "Folk", duration: "4:12", createdAt: "Feb 19, 2026" },
  { id: "3", title: "Urban Pulse", genre: "Hip-Hop", duration: "2:58", createdAt: "Feb 18, 2026" },
  { id: "4", title: "Starlight Waltz", genre: "Classical", duration: "5:01", createdAt: "Feb 17, 2026" },
  { id: "5", title: "Neon Rush", genre: "Electronic", duration: "3:45", createdAt: "Feb 14, 2026" },
  { id: "6", title: "Desert Wind", genre: "Ambient", duration: "6:33", createdAt: "Feb 12, 2026" },
  { id: "7", title: "City Lights", genre: "Pop", duration: "3:12", createdAt: "Feb 10, 2026" },
  { id: "8", title: "Thunder Road", genre: "Rock", duration: "4:45", createdAt: "Feb 8, 2026" },
]

export function LibraryPage({ onPlaySong }: LibraryPageProps) {
  const [search, setSearch] = useState("")
  const [playingId, setPlayingId] = useState<string | null>(null)
  const [progress, setProgress] = useState<Record<string, number>>({})
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [])

  const togglePlay = (song: LibrarySong) => {
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
      
      // Trigger mobile player
      if (onPlaySong) {
        onPlaySong({
          ...song,
          status: "completed",
          prompt: "",
        })
      }
    }
  }

  const filtered = librarySongs.filter((s) =>
    s.title.toLowerCase().includes(search.toLowerCase()) ||
    s.genre.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <main className="flex-1 overflow-hidden">
      <div className="mx-auto max-w-4xl px-6 py-8">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-wide text-foreground">
              Full Library
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              All your generated songs in one place
            </p>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search songs..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-64 pl-9 border-border bg-card text-foreground"
              />
            </div>
            <Button variant="outline" size="sm" className="gap-1.5 border-border text-foreground">
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
                className={`group flex items-center gap-4 rounded-xl border bg-card p-4 transition-all ${
                  playingId === song.id
                    ? "border-secondary shadow-md shadow-secondary/10"
                    : "border-border hover:border-primary/30 hover:shadow-sm"
                }`}
              >
                <button
                  onClick={() => togglePlay(song)}
                  className="relative h-12 w-12 shrink-0 overflow-hidden rounded-lg"
                  aria-label={playingId === song.id ? "Pause" : "Play"}
                >
                  {/* Background Image */}
                  <img
                    src={`https://picsum.photos/seed/${song.id}/100/100`}
                    alt=""
                    className="absolute inset-0 h-full w-full object-cover"
                  />
                  {/* Teal Overlay */}
                  <div className="absolute inset-0 bg-primary/70 transition-colors hover:bg-primary/80" />
                  {/* Icon */}
                  <div className="absolute inset-0 flex items-center justify-center">
                    {playingId === song.id ? (
                      <Pause className="h-5 w-5 text-white" />
                    ) : (
                      <Play className="h-5 w-5 pl-0.5 text-white" />
                    )}
                  </div>
                </button>

                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{song.title}</p>
                  <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                    <Badge variant="outline" className="border-primary/20 text-primary text-xs font-medium">
                      {song.genre}
                    </Badge>
                    <span className="flex items-center gap-0.5">
                      <Clock className="h-3 w-3" />
                      {song.duration}
                    </span>
                    <span>{song.createdAt}</span>
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

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100"
                    >
                      <MoreHorizontal className="h-4 w-4" />
                      <span className="sr-only">Song options</span>
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem>
                      <Download className="mr-2 h-4 w-4" />
                      Download
                    </DropdownMenuItem>
                    <DropdownMenuItem className="text-destructive">
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
    </main>
  )
}
