"use client"

import { Play, Pause, SkipBack, SkipForward, X } from "lucide-react"

export interface PlayingSong {
  id: string
  title: string
  genre: string
  duration: string
  progress: number
}

interface MobilePlayerProps {
  song: PlayingSong | null
  isPlaying: boolean
  onPlayPause: () => void
  onClose: () => void
  onSkipBack?: () => void
  onSkipForward?: () => void
}

export function MobilePlayer({
  song,
  isPlaying,
  onPlayPause,
  onClose,
  onSkipBack,
  onSkipForward,
}: MobilePlayerProps) {
  if (!song) return null

  return (
    <div className="fixed bottom-[3.5rem] lg:bottom-0 left-0 right-0 z-40 border-t border-primary/20 bg-card shadow-lg">
      {/* Progress bar at top */}
      <div className="h-1 w-full bg-muted">
        <div
          className="h-full bg-secondary transition-all duration-100"
          style={{ width: `${song.progress}%` }}
        />
      </div>

      <div className="flex items-center gap-3 px-4 py-3">
        {/* Album Art */}
        <div className="relative h-11 w-11 shrink-0 overflow-hidden rounded-lg">
          <img
            src={`https://picsum.photos/seed/${song.id}/100/100`}
            alt=""
            className="absolute inset-0 h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-primary/50" />
        </div>

        {/* Song Info */}
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-foreground">
            {song.title}
          </p>
          <p className="truncate text-xs text-muted-foreground">{song.genre}</p>
        </div>

        {/* Controls */}
        <div className="flex items-center gap-1">
          <button
            onClick={onSkipBack}
            className="flex h-9 w-9 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            aria-label="Previous"
          >
            <SkipBack className="h-4 w-4" />
          </button>

          <button
            onClick={onPlayPause}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground transition-colors hover:bg-primary/90"
            aria-label={isPlaying ? "Pause" : "Play"}
          >
            {isPlaying ? (
              <Pause className="h-5 w-5" />
            ) : (
              <Play className="h-5 w-5 pl-0.5" />
            )}
          </button>

          <button
            onClick={onSkipForward}
            className="flex h-9 w-9 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            aria-label="Next"
          >
            <SkipForward className="h-4 w-4" />
          </button>
        </div>

        {/* Close */}
        <button
          onClick={onClose}
          className="flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          aria-label="Close player"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
