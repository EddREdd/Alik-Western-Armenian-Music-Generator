"use client"

import { useState, useCallback, useRef, useEffect } from "react"
import { AppHeader } from "@/components/app-header"
import { BottomNav } from "@/components/bottom-nav"
import { AdminPage } from "@/components/admin-page"
import { MobilePlayer, type PlayingSong } from "@/components/mobile-player"
import { SongCreatorPanel } from "@/components/song-creator-panel"
import { SongLibraryPanel, type Song } from "@/components/song-library-panel"
import { LibraryPage } from "@/components/library-page"
import { LyricsPage } from "@/components/lyrics-page"
import { LoginPage } from "@/components/login-page"
import { ForgotPasswordPage } from "@/components/forgot-password-page"
import { RegisterPage } from "@/components/register-page"
import {
  clearStoredSessionToken,
  getCurrentUser,
  getStoredSessionToken,
  logout,
  storeSessionToken,
  type AuthSession,
  type AuthUser,
} from "@/lib/auth-api"
import { createLyric } from "@/lib/lyrics-api"
import {
  createGenerationJob,
  deleteGenerationJob,
  getGenerationJob,
  listGenerationJobs,
  submitGenerationJob,
  type GenerationJob,
  type GenerationModel,
} from "@/lib/musicgen-api"

const defaultProjectId =
  process.env.NEXT_PUBLIC_DEFAULT_PROJECT_ID?.trim() || "project-1"

function mapJobToSong(job: GenerationJob): Song {
  const firstTrack = job.tracks?.[0]
  const createdAt = job.createdAt
    ? new Date(job.createdAt).toLocaleString()
    : "Just now"

  let status: Song["status"] = "generating"
  if (job.internalStatus === "COMPLETED") {
    status = "completed"
  } else if (job.internalStatus === "FAILED" || job.internalStatus === "EXPIRED") {
    status = "failed"
  }

  const minutes = firstTrack?.durationSeconds
    ? Math.floor(firstTrack.durationSeconds / 60)
    : 0
  const seconds = firstTrack?.durationSeconds
    ? firstTrack.durationSeconds % 60
    : 0

  return {
    id: job.id,
    title: job.titleFinal || "Untitled Song",
    genre: job.styleFinal?.split(",")[0]?.trim() || job.model || "Generated",
    duration: firstTrack?.durationSeconds
      ? `${minutes}:${String(seconds).padStart(2, "0")}`
      : "--:--",
    createdAt,
    status,
    prompt: job.styleFinal || job.promptFinal || "",
    lyrics: job.promptFinal || undefined,
    lyricsId: job.lyricId || undefined,
    lyricsTitle: job.lyricTitle || undefined,
    audioUrl: firstTrack?.localAudioUrl || firstTrack?.audioUrl || undefined,
    streamAudioUrl: firstTrack?.localAudioUrl || firstTrack?.streamAudioUrl || firstTrack?.audioUrl || undefined,
  }
}

export default function Home() {
  const [authView, setAuthView] = useState<"login" | "register" | "forgot-password" | "app">("login")
  const [authLoading, setAuthLoading] = useState(true)
  const [sessionToken, setSessionToken] = useState<string | null>(null)
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null)
  const [activeTab, setActiveTab] = useState("create")
  const [isGenerating, setIsGenerating] = useState(false)
  const [generatedSongs, setGeneratedSongs] = useState<Song[]>([])
  const [backendJobs, setBackendJobs] = useState<GenerationJob[]>([])
  const [generationError, setGenerationError] = useState("")
  const [selectedSongId, setSelectedSongId] = useState<string | null>(null)
  
  // Mobile player state
  const [currentSong, setCurrentSong] = useState<PlayingSong | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const playableSongsRef = useRef<Song[]>([])
  const playSongRef = useRef<((song: Song) => void) | null>(null)

  const playableSongs = generatedSongs.filter((song) => song.status === "completed")

  const refreshAllJobs = useCallback(async () => {
    try {
      const jobs = await listGenerationJobs(defaultProjectId)
      setBackendJobs(
        jobs.sort((left, right) => {
          const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0
          const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0
          return rightTime - leftTime
        }),
      )
    } catch (error) {
      console.error("Failed to load generation jobs", error)
    }
  }, [])

  const refreshTrackedJobs = useCallback(async () => {
    const activeJobIds = backendJobs
      .filter((job) => job.internalStatus === "SUBMITTED" || job.internalStatus === "IN_PROGRESS")
      .map((job) => job.id)

    if (activeJobIds.length === 0) {
      return
    }

    try {
      const refreshedJobs = await Promise.all(activeJobIds.map((jobId) => getGenerationJob(jobId)))
      setBackendJobs((prev) =>
        prev.map((job) => refreshedJobs.find((candidate) => candidate.id === job.id) || job),
      )
    } catch (error) {
      console.error("Failed to refresh generation jobs", error)
    }
  }, [backendJobs])

  useEffect(() => {
    setGeneratedSongs(backendJobs.map(mapJobToSong))
  }, [backendJobs])

  useEffect(() => {
    void refreshAllJobs()
  }, [refreshAllJobs])

  useEffect(() => {
    playableSongsRef.current = playableSongs
  }, [playableSongs])

  useEffect(() => {
    if (typeof window !== "undefined") {
      const authParam = new URLSearchParams(window.location.search).get("auth")
      if (authParam === "forgot-password") {
        setAuthView("forgot-password")
      } else if (authParam === "register") {
        setAuthView("register")
      }
    }
  }, [])

  useEffect(() => {
    const storedToken = getStoredSessionToken()
    if (!storedToken) {
      setAuthLoading(false)
      return
    }

    setSessionToken(storedToken)
    void getCurrentUser(storedToken)
      .then((user) => {
        setCurrentUser(user)
        setAuthView("app")
      })
      .catch(() => {
        clearStoredSessionToken()
        setSessionToken(null)
        setCurrentUser(null)
        setAuthView("login")
      })
      .finally(() => {
        setAuthLoading(false)
      })
  }, [])

  // Cleanup interval on unmount
  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [])

  useEffect(() => {
    const hasActiveJobs = backendJobs.some(
      (job) => job.internalStatus === "SUBMITTED" || job.internalStatus === "IN_PROGRESS",
    )

    if (!hasActiveJobs) {
      return
    }

    const interval = setInterval(() => {
      void refreshTrackedJobs()
    }, 8000)

    return () => clearInterval(interval)
  }, [backendJobs, refreshTrackedJobs])

  const loadSongWithoutPlaying = useCallback((song: Song) => {
    setCurrentSong({
      id: song.id,
      title: song.title,
      genre: song.genre,
      duration: song.duration,
      progress: 0,
    })
    setIsPlaying(false)
  }, [])

  useEffect(() => {
    if (currentSong || playableSongs.length === 0) {
      return
    }
    loadSongWithoutPlaying(playableSongs[0])
  }, [currentSong, playableSongs, loadSongWithoutPlaying])

  const playSong = useCallback((song: Song) => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    
    setCurrentSong({
      id: song.id,
      title: song.title,
      genre: song.genre,
      duration: song.duration,
      progress: 0,
    })
    setIsPlaying(true)
    
    intervalRef.current = setInterval(() => {
      setCurrentSong((prev) => {
        if (!prev) return null
        if (prev.progress >= 100) {
          if (intervalRef.current) clearInterval(intervalRef.current)
          const queue = playableSongsRef.current
          const currentIndex = queue.findIndex((candidate) => candidate.id === prev.id)
          const nextSong =
            currentIndex === -1 || queue.length === 0
              ? null
              : queue[currentIndex < queue.length - 1 ? currentIndex + 1 : 0]

          if (nextSong && nextSong.id !== prev.id) {
            setTimeout(() => playSongRef.current?.(nextSong), 0)
          } else {
            setIsPlaying(false)
          }

          return { ...prev, progress: 100 }
        }
        return { ...prev, progress: prev.progress + 0.5 }
      })
    }, 100)
  }, [])

  useEffect(() => {
    playSongRef.current = playSong
  }, [playSong])

  const togglePlayPause = useCallback(() => {
    if (isPlaying) {
      if (intervalRef.current) clearInterval(intervalRef.current)
      setIsPlaying(false)
    } else {
      setIsPlaying(true)
      intervalRef.current = setInterval(() => {
        setCurrentSong((prev) => {
          if (!prev) return null
          if (prev.progress >= 100) {
            setIsPlaying(false)
            if (intervalRef.current) clearInterval(intervalRef.current)
            return { ...prev, progress: 0 }
          }
          return { ...prev, progress: prev.progress + 0.5 }
        })
      }, 100)
    }
  }, [isPlaying])

  const closePlayer = useCallback(() => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    setCurrentSong(null)
    setIsPlaying(false)
  }, [])

  const skipToSong = useCallback((direction: "back" | "forward") => {
    if (!currentSong) return
    const currentIndex = playableSongs.findIndex((s) => s.id === currentSong.id)
    if (currentIndex === -1) return
    
    let newIndex: number
    if (direction === "back") {
      newIndex = currentIndex > 0 ? currentIndex - 1 : playableSongs.length - 1
    } else {
      newIndex = currentIndex < playableSongs.length - 1 ? currentIndex + 1 : 0
    }
    
    const newSong = playableSongs[newIndex]
    if (newSong && newSong.status === "completed") {
      playSong(newSong)
    }
  }, [currentSong, playableSongs, playSong])

  const handleDownloadSong = useCallback((song: Song) => {
    const url = song.streamAudioUrl || song.audioUrl
    if (!url) {
      return
    }

    const link = document.createElement("a")
    link.href = url
    link.download = `${song.title || "song"}.mp3`
    link.target = "_blank"
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }, [])

  const handleDeleteSong = useCallback(async (songId: string) => {
    await deleteGenerationJob(songId)
    setBackendJobs((prev) => prev.filter((job) => job.id !== songId))
    setSelectedSongId((prev) => (prev === songId ? null : prev))
    setCurrentSong((prev) => (prev?.id === songId ? null : prev))
    if (currentSong?.id === songId) {
      setIsPlaying(false)
    }
  }, [currentSong?.id])

  const handleGenerate = useCallback(
    async (data: {
      projectId: string
      lyricId?: string | null
      title: string
      lyrics: string
      stylePrompt: string
      instrumental: boolean
      model: GenerationModel
    }) => {
      setGenerationError("")
      setIsGenerating(true)

      try {
        let lyricId = data.lyricId ?? null
        if (!data.instrumental && data.lyrics.trim() && !lyricId) {
          const createdLyric = await createLyric({
            projectId: data.projectId,
            title: data.title,
            body: data.lyrics,
          })
          lyricId = createdLyric.id
        }

        const createdJob = await createGenerationJob({
          projectId: data.projectId,
          lyricId,
          title: data.title,
          lyrics: data.lyrics,
          stylePrompt: data.stylePrompt,
          instrumental: data.instrumental,
          model: data.model,
        })

        setBackendJobs((prev) => [createdJob, ...prev])

        const submittedJob = await submitGenerationJob(createdJob.id)
        setBackendJobs((prev) =>
          prev.map((job) => (job.id === submittedJob.id ? submittedJob : job)),
        )
        setCurrentUser((prev) => {
          if (!prev || prev.unlimitedCredits || prev.creditsRemaining == null) {
            return prev
          }
          return {
            ...prev,
            creditsRemaining: Math.max(0, prev.creditsRemaining - 1),
            creditsUsed: (prev.creditsUsed ?? 0) + 1,
            songsGenerated: (prev.songsGenerated ?? 0) + 2,
          }
        })
      } catch (error) {
        setGenerationError(
          error instanceof Error ? error.message : "Failed to create generation job",
        )
      } finally {
        setIsGenerating(false)
      }
    },
    []
  )

  // Bottom padding: mobile/tablet = nav (3.75rem) + player (~4rem), desktop = player only (~4rem)
  const bottomPaddingMobile = "pb-[7.5rem]"
  const bottomPaddingDesktop = "lg:pb-[4rem]"

  const handleAuthSuccess = useCallback((session: AuthSession) => {
    storeSessionToken(session.sessionToken)
    setSessionToken(session.sessionToken)
    setCurrentUser(session.user)
    const mostRecent = generatedSongs.find(
      (s) => s.status === "completed"
    )
    if (mostRecent) loadSongWithoutPlaying(mostRecent)
    setAuthView("app")
  }, [generatedSongs, loadSongWithoutPlaying])

  const handleLogout = useCallback(() => {
    // Make sign-out instant on the client; backend invalidation runs in background.
    clearStoredSessionToken()
    void logout(sessionToken)
    setSessionToken(null)
    setCurrentUser(null)
    setAuthView("login")
    if (typeof window !== "undefined") {
      window.location.replace("/")
    }
  }, [sessionToken])

  if (authLoading) {
    return <div className="flex min-h-screen items-center justify-center bg-background text-foreground">Loading...</div>
  }

  // Auth views
  if (authView === "login") {
    return (
      <LoginPage
        onLogin={handleAuthSuccess}
        onSwitchToRegister={() => setAuthView("register")}
        onForgotPassword={() => {
          setAuthView("forgot-password")
          if (typeof window !== "undefined") {
            window.history.replaceState(null, "", "/?auth=forgot-password")
          }
        }}
      />
    )
  }

  if (authView === "register") {
    return (
      <RegisterPage
        onRegisterComplete={handleAuthSuccess}
        onSwitchToLogin={() => setAuthView("login")}
      />
    )
  }

  if (authView === "forgot-password") {
    return (
      <ForgotPasswordPage
        onBackToLogin={() => {
          setAuthView("login")
          if (typeof window !== "undefined") {
            window.history.replaceState(null, "", "/")
          }
        }}
      />
    )
  }

  return (
    <div className="flex h-screen flex-col bg-background">
      <AppHeader
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onLogout={handleLogout}
        showAdmin={Boolean(currentUser?.admin)}
        creditsLabel={
          currentUser?.unlimitedCredits
            ? "Unlimited"
            : String(currentUser?.creditsRemaining ?? 0)
        }
      />

      <div className={`flex flex-1 flex-col overflow-hidden ${bottomPaddingMobile} ${bottomPaddingDesktop}`}>
        {activeTab === "create" && (
          <main className="flex flex-1 overflow-hidden">
            {/* Left Panel - Song Creator */}
            <div className="w-full lg:w-1/2 lg:border-r border-border overflow-hidden">
              <SongCreatorPanel
                onGenerate={handleGenerate}
                isGenerating={isGenerating}
                errorMessage={generationError}
                defaultProjectId={defaultProjectId}
              />
            </div>

            {/* Right Panel - Song Library (hidden on mobile/tablet) */}
            <div className="hidden lg:block lg:w-1/2 overflow-hidden">
              <SongLibraryPanel
                songs={generatedSongs}
                onSongClick={(song) => {
                  setSelectedSongId(song.id)
                  setActiveTab("library")
                }}
                onPlaySong={playSong}
                onDownloadSong={handleDownloadSong}
                onDeleteSong={(song) => void handleDeleteSong(song.id)}
                currentPlayingId={currentSong?.id}
              />
            </div>
          </main>
        )}

        {activeTab === "library" && (
          <LibraryPage
            songs={generatedSongs}
            currentPlayingId={currentSong?.id}
            onPlaySong={playSong}
            onDownloadSong={handleDownloadSong}
            onDeleteSong={handleDeleteSong}
            onNavigateToLyrics={(lyricsId) => {
              setActiveTab("lyrics")
              // Could also pass lyricsId to LyricsPage to highlight the lyrics
            }}
            initialSelectedSongId={selectedSongId}
            onClearInitialSong={() => setSelectedSongId(null)}
          />
        )}
        {activeTab === "lyrics" && (
          <LyricsPage
            onNavigateToSong={(songId) => {
              setSelectedSongId(songId)
              setActiveTab("library")
            }}
          />
        )}
        {activeTab === "admin" && currentUser?.admin && <AdminPage />}
      </div>

      <MobilePlayer
        song={currentSong}
        isPlaying={isPlaying}
        onPlayPause={togglePlayPause}
        onSkipBack={() => skipToSong("back")}
        onSkipForward={() => skipToSong("forward")}
      />
      <BottomNav activeTab={activeTab} onTabChange={setActiveTab} showAdmin={Boolean(currentUser?.admin)} />
    </div>
  )
}
