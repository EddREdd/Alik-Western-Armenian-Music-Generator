"use client"

import { useState, useRef, useEffect } from "react"
import {
  ArrowLeft,
  Play,
  Pause,
  Clock,
  Calendar,
  Download,
  Trash2,
  FileText,
  Music,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"

export interface SongDetail {
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

interface SongDetailPageProps {
  song: SongDetail
  onBack: () => void
  onDelete: (songId: string) => void
  onNavigateToLyrics?: (lyricsId: string) => void
  onPlaySong?: (song: SongDetail) => void
}

export function SongDetailPage({
  song,
  onBack,
  onDelete,
  onNavigateToLyrics,
  onPlaySong,
}: SongDetailPageProps) {
  const [isPlaying, setIsPlaying] = useState(false)
  const [progress, setProgress] = useState(0)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [])

  const togglePlay = () => {
    if (isPlaying) {
      setIsPlaying(false)
      if (intervalRef.current) clearInterval(intervalRef.current)
    } else {
      setIsPlaying(true)
      if (onPlaySong) onPlaySong(song)
      intervalRef.current = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 100) {
            setIsPlaying(false)
            if (intervalRef.current) clearInterval(intervalRef.current)
            return 0
          }
          return prev + 0.5
        })
      }, 100)
    }
  }

  const handleDelete = () => {
    onDelete(song.id)
    onBack()
  }

  return (
    <main className="flex flex-1 flex-col overflow-hidden">
      {/* Header */}
      <div className="flex items-center gap-3 border-b border-border px-4 py-3 lg:px-6 lg:py-4">
        <Button
          variant="ghost"
          size="sm"
          onClick={onBack}
          className="h-8 w-8 p-0 text-foreground"
        >
          <ArrowLeft className="h-5 w-5" />
          <span className="sr-only">Back to library</span>
        </Button>
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-lg font-bold tracking-wide text-foreground lg:text-xl">
            {song.title}
          </h1>
          <div className="flex items-center gap-3 text-xs text-muted-foreground lg:text-sm">
            <Badge variant="outline" className="border-primary/20 text-primary">
              {song.genre}
            </Badge>
            <span className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {song.duration}
            </span>
            <span className="flex items-center gap-1">
              <Calendar className="h-3 w-3" />
              {song.createdAt}
            </span>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-2xl p-4 pb-8 lg:p-8 lg:pb-12">
          {/* Album Art & Player */}
          <div className="mb-8 flex flex-col items-center">
            <div className="relative mb-6 h-48 w-48 overflow-hidden rounded-2xl shadow-lg lg:h-64 lg:w-64">
              <img
                src={`https://picsum.photos/seed/${song.id}/400/400`}
                alt={song.title}
                className="h-full w-full object-cover"
              />
              <div className="absolute inset-0 bg-primary/50" />
              <button
                onClick={togglePlay}
                className="absolute inset-0 flex items-center justify-center transition-transform hover:scale-105"
                aria-label={isPlaying ? "Pause" : "Play"}
              >
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-white/20 backdrop-blur-sm">
                  {isPlaying ? (
                    <Pause className="h-8 w-8 text-white" />
                  ) : (
                    <Play className="h-8 w-8 pl-1 text-white" />
                  )}
                </div>
              </button>
            </div>

            {/* Progress Bar */}
            <div className="w-full max-w-xs">
              <div className="h-1.5 overflow-hidden rounded-full bg-primary/10">
                <div
                  className="h-full rounded-full bg-secondary transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
              <div className="mt-1 flex justify-between text-xs text-muted-foreground">
                <span>
                  {Math.floor((progress / 100) * 198)}s
                </span>
                <span>{song.duration}</span>
              </div>
            </div>
          </div>

          {/* Lyrics Section */}
          {song.lyrics && (
            <div className="mb-8">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-foreground">Lyrics</h2>
                {song.lyricsId && song.lyricsTitle && (
                  <button
                    onClick={() => onNavigateToLyrics?.(song.lyricsId!)}
                    className="flex items-center gap-1.5 rounded-full bg-secondary/10 px-3 py-1.5 text-sm font-medium text-secondary transition-colors hover:bg-secondary/20"
                  >
                    <FileText className="h-3.5 w-3.5" />
                    {song.lyricsTitle}
                  </button>
                )}
              </div>
              <div className="rounded-xl border border-border bg-card p-4 lg:p-6">
                <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed text-foreground/90">
                  {song.lyrics}
                </pre>
              </div>
            </div>
          )}

          {/* Prompt Section */}
          {song.prompt && (
            <div className="mb-8">
              <h2 className="mb-3 text-lg font-semibold text-foreground">
                Generation Prompt
              </h2>
              <div className="rounded-xl border border-border bg-card p-4">
                <p className="text-sm text-muted-foreground">{song.prompt}</p>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3">
            <Button
              variant="outline"
              className="flex-1 gap-2 border-border text-foreground"
            >
              <Download className="h-4 w-4" />
              Download
            </Button>
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button
                  variant="outline"
                  className="flex-1 gap-2 border-destructive/30 text-destructive hover:bg-destructive/10 hover:text-destructive"
                >
                  <Trash2 className="h-4 w-4" />
                  Delete
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete Song</AlertDialogTitle>
                  <AlertDialogDescription>
                    Are you sure you want to delete "{song.title}"? This action cannot be undone.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={handleDelete}
                    className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                  >
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </div>
      </div>
    </main>
  )
}
