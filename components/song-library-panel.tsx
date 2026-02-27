"use client"

import { useState, useRef, useEffect } from "react"
import {
  Play,
  Pause,
  Clock,
  MoreHorizontal,
  Download,
  Trash2,
  Music,
  ChevronRight,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

export interface Song {
  id: string
  title: string
  genre: string
  duration: string
  createdAt: string
  status: "completed" | "generating" | "failed"
  prompt: string
}

const initialSongs: Song[] = [
  {
    id: "1",
    title: "Electric Sunset",
    genre: "Synthwave",
    duration: "3:24",
    createdAt: "Today",
    status: "completed",
    prompt: "Synthwave, retro 80s, pulsing bass",
  },
  {
    id: "2",
    title: "Mountain Echo",
    genre: "Folk",
    duration: "4:12",
    createdAt: "Yesterday",
    status: "completed",
    prompt: "Acoustic folk, warm, fingerpicking",
  },
  {
    id: "3",
    title: "Urban Pulse",
    genre: "Hip-Hop",
    duration: "2:58",
    createdAt: "2 days ago",
    status: "completed",
    prompt: "Lo-fi hip hop, chill beats, jazzy",
  },
  {
    id: "4",
    title: "Starlight Waltz",
    genre: "Classical",
    duration: "5:01",
    createdAt: "3 days ago",
    status: "completed",
    prompt: "Orchestral waltz, elegant, strings",
  },
  {
    id: "5",
    title: "Neon Rush",
    genre: "Electronic",
    duration: "3:45",
    createdAt: "1 week ago",
    status: "completed",
    prompt: "EDM, high energy, festival anthem",
  },
]

interface SongLibraryPanelProps {
  songs: Song[]
  onSongClick?: (song: Song) => void
}

export function SongLibraryPanel({ songs: externalSongs, onSongClick }: SongLibraryPanelProps) {
  const allSongs = [...externalSongs, ...initialSongs]
  const [playingId, setPlayingId] = useState<string | null>(null)
  const [progress, setProgress] = useState<Record<string, number>>({})
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [])

  const togglePlay = (songId: string) => {
    if (playingId === songId) {
      setPlayingId(null)
      if (intervalRef.current) clearInterval(intervalRef.current)
    } else {
      setPlayingId(songId)
      if (intervalRef.current) clearInterval(intervalRef.current)
      if (!progress[songId]) {
        setProgress((prev) => ({ ...prev, [songId]: 0 }))
      }
      intervalRef.current = setInterval(() => {
        setProgress((prev) => {
          const current = prev[songId] || 0
          if (current >= 100) {
            setPlayingId(null)
            if (intervalRef.current) clearInterval(intervalRef.current)
            return { ...prev, [songId]: 0 }
          }
          return { ...prev, [songId]: current + 0.5 }
        })
      }, 100)
    }
  }

  return (
    <div className="flex h-full flex-col bg-primary/[0.03]">
      <div className="border-b border-border px-6 py-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold tracking-wide text-foreground">
              Song Library
            </h2>
            <p className="mt-0.5 text-sm text-muted-foreground">
              {allSongs.length} songs generated
            </p>
          </div>
        </div>
      </div>

      <ScrollArea className="flex-1">
        <div className="flex flex-col gap-2 p-4">
          {allSongs.map((song) => (
            <div
              key={song.id}
              onClick={() => onSongClick?.(song)}
              role={onSongClick ? "button" : undefined}
              tabIndex={onSongClick ? 0 : undefined}
              onKeyDown={(e) => {
                if (onSongClick && (e.key === "Enter" || e.key === " ")) {
                  e.preventDefault()
                  onSongClick(song)
                }
              }}
              className={`group rounded-xl border bg-card p-4 transition-all ${
                onSongClick ? "cursor-pointer" : ""
              } ${
                playingId === song.id
                  ? "border-secondary shadow-md shadow-secondary/10"
                  : "border-border hover:border-primary/30 hover:shadow-sm"
              }`}
            >
              <div className="flex items-center gap-3">
                {/* Play Button with Image */}
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    if (song.status === "completed") togglePlay(song.id)
                  }}
                  disabled={song.status !== "completed"}
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
                    {song.status === "generating" ? (
                      <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                    ) : playingId === song.id ? (
                      <Pause className="h-5 w-5 text-white" />
                    ) : (
                      <Play className="h-5 w-5 pl-0.5 text-white" />
                    )}
                  </div>
                </button>

                {/* Song Info */}
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="truncate text-sm font-semibold text-foreground">
                      {song.title}
                    </p>
                    {song.status === "generating" && (
                      <Badge
                        variant="outline"
                        className="border-secondary/40 text-secondary text-xs"
                      >
                        Generating
                      </Badge>
                    )}
                    {song.status === "failed" && (
                      <Badge variant="destructive" className="text-xs">
                        Failed
                      </Badge>
                    )}
                  </div>
                  <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                    <span className="flex items-center gap-0.5">
                      <Clock className="h-3 w-3" />
                      {song.duration}
                    </span>
                    <span>{song.createdAt}</span>
                  </div>
                </div>

                {/* Actions */}
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100"
                      onClick={(e) => e.stopPropagation()}
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

                {/* Chevron for clickable cards */}
                {onSongClick && (
                  <ChevronRight className="h-5 w-5 text-muted-foreground/50 transition-colors group-hover:text-primary" />
                )}
              </div>

              {/* Progress Bar */}
              {playingId === song.id && (
                <div className="mt-3">
                  <div className="h-1 overflow-hidden rounded-full bg-primary/10">
                    <div
                      className="h-full rounded-full bg-secondary transition-all"
                      style={{ width: `${progress[song.id] || 0}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          ))}

          {allSongs.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
                <Music className="h-8 w-8 text-primary" />
              </div>
              <p className="mt-4 font-semibold text-foreground">No songs yet</p>
              <p className="mt-1 text-sm text-muted-foreground">
                Create your first song using the panel on the left
              </p>
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  )
}
