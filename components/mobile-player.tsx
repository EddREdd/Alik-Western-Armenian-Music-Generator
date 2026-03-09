"use client"

import { Play, Pause, SkipBack, SkipForward, Music } from "lucide-react"

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
  onSkipBack?: () => void
  onSkipForward?: () => void
}

export function MobilePlayer({
  song,
  isPlaying,
  onPlayPause,
  onSkipBack,
  onSkipForward,
}: MobilePlayerProps) {
  return (
    <div className="fixed bottom-[3.75rem] lg:bottom-0 left-0 right-0 z-40 border-t border-primary/20 bg-card shadow-lg">
      {/* Progress bar at top */}
      <div className="h-0.5 w-full bg-muted">
        <div
          className="h-full bg-secondary transition-all duration-100"
          style={{ width: `${song?.progress || 0}%` }}
        />
      </div>

      <div className="flex items-center gap-3 px-4 py-2">
        {/* Album Art */}
        <div className="relative h-11 w-11 shrink-0 overflow-hidden rounded-lg bg-primary/10">
          {song ? (
            <>
              <img
                src={`https://picsum.photos/seed/${song.id}/100/100`}
                alt=""
                className="absolute inset-0 h-full w-full object-cover"
              />
              <div className="absolute inset-0 bg-primary/50" />
            </>
          ) : (
            <div className="flex h-full w-full items-center justify-center">
              <Music className="h-5 w-5 text-primary/50" />
            </div>
          )}
        </div>

        {/* Song Info */}
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-foreground">
            {song?.title || "No song playing"}
          </p>
          <p className="truncate text-xs text-muted-foreground">
            {song?.genre || "Select a song to play"}
          </p>
        </div>

        {/* Controls */}
        <div className="flex items-center gap-1">
          <button
            onClick={onSkipBack}
            disabled={!song}
            className="flex h-9 w-9 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="Previous"
          >
            <SkipBack className="h-4 w-4" />
          </button>

          <button
            onClick={onPlayPause}
            disabled={!song}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
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
            disabled={!song}
            className="flex h-9 w-9 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="Next"
          >
            <SkipForward className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  )
}
