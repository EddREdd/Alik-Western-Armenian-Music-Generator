"use client"

import { useEffect, useMemo, useState } from "react"
import {
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
  audioUrl?: string
  streamAudioUrl?: string
}

interface LibraryPageProps {
  songs: LibrarySong[]
  currentPlayingId?: string | null
  onPlaySong?: (song: LibrarySong) => void
  onDownloadSong?: (song: LibrarySong) => void
  onDeleteSong?: (songId: string) => Promise<void> | void
  onNavigateToLyrics?: (lyricsId: string) => void
  initialSelectedSongId?: string | null
  onClearInitialSong?: () => void
}

export function LibraryPage({
  songs,
  currentPlayingId,
  onPlaySong,
  onDownloadSong,
  onDeleteSong,
  onNavigateToLyrics,
  initialSelectedSongId,
  onClearInitialSong,
}: LibraryPageProps) {
  const [search, setSearch] = useState("")
  const [selectedSongId, setSelectedSongId] = useState<string | null>(initialSelectedSongId ?? null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [songToDelete, setSongToDelete] = useState<string | null>(null)

  const filtered = useMemo(
    () =>
      songs.filter(
        (song) =>
          song.title.toLowerCase().includes(search.toLowerCase()) ||
          song.genre.toLowerCase().includes(search.toLowerCase()),
      ),
    [songs, search],
  )

  const selectedSong = useMemo(
    () => songs.find((song) => song.id === selectedSongId) ?? null,
    [songs, selectedSongId],
  )

  useEffect(() => {
    if (initialSelectedSongId) {
      setSelectedSongId(initialSelectedSongId)
      onClearInitialSong?.()
    }
  }, [initialSelectedSongId, onClearInitialSong])

  const confirmDelete = async () => {
    if (songToDelete) {
      await onDeleteSong?.(songToDelete)
      if (selectedSongId === songToDelete) {
        setSelectedSongId(null)
      }
    }
    setSongToDelete(null)
    setDeleteDialogOpen(false)
  }

  if (selectedSong) {
    return (
      <SongDetailPage
        song={selectedSong as SongDetail}
        onBack={() => setSelectedSongId(null)}
        onDelete={(songId) => {
          setSongToDelete(songId)
          setDeleteDialogOpen(true)
        }}
        onDownload={(song) => onDownloadSong?.(song as LibrarySong)}
        onNavigateToLyrics={onNavigateToLyrics}
        onPlaySong={(song) => onPlaySong?.(song as LibrarySong)}
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
                className="w-full pl-9 lg:w-64"
              />
            </div>
            <Button variant="outline" size="sm" className="hidden gap-1.5 lg:flex">
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
                onClick={() => setSelectedSongId(song.id)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault()
                    setSelectedSongId(song.id)
                  }
                }}
                className={`group flex w-full cursor-pointer items-center gap-3 rounded-xl border bg-card p-3 text-left transition-all lg:gap-4 lg:p-4 ${
                  currentPlayingId === song.id
                    ? "border-secondary shadow-md shadow-secondary/10"
                    : "border-border hover:border-primary/30 hover:shadow-sm"
                }`}
              >
                <div
                  onClick={(e) => {
                    e.stopPropagation()
                    if (song.status === "completed") {
                      onPlaySong?.(song)
                    }
                  }}
                  className="relative h-12 w-12 shrink-0 overflow-hidden rounded-lg"
                >
                  <img
                    src={`https://picsum.photos/seed/${song.id}/100/100`}
                    alt=""
                    className="absolute inset-0 h-full w-full object-cover"
                  />
                  <div className="absolute inset-0 bg-primary/70" />
                </div>

                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{song.title}</p>
                  <div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                    <span className="flex items-center gap-0.5">
                      <Clock className="h-3 w-3" />
                      {song.duration}
                    </span>
                    <span className="hidden lg:inline">{song.createdAt}</span>
                  </div>
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
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem
                      onClick={(e) => {
                        e.stopPropagation()
                        onDownloadSong?.(song)
                      }}
                    >
                      <Download className="mr-2 h-4 w-4" />
                      Download
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="text-destructive"
                      onClick={(e) => {
                        e.stopPropagation()
                        setSongToDelete(song.id)
                        setDeleteDialogOpen(true)
                      }}
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            ))}

            {filtered.length === 0 ? (
              <div className="flex flex-col items-center py-16 text-center">
                <Music className="h-12 w-12 text-muted-foreground/40" />
                <p className="mt-4 font-semibold text-foreground">No songs found</p>
                <p className="mt-1 text-sm text-muted-foreground">Generate a song to see it here</p>
              </div>
            ) : null}
          </div>
        </ScrollArea>
      </div>

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Song</AlertDialogTitle>
            <AlertDialogDescription>
              This removes the song from your library view, but keeps the original cloud asset for admin and audit purposes.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => void confirmDelete()}
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
