"use client"

import { useState, useCallback, useRef, useEffect } from "react"
import { AppHeader } from "@/components/app-header"
import { BottomNav } from "@/components/bottom-nav"
import { MobilePlayer, type PlayingSong } from "@/components/mobile-player"
import { SongCreatorPanel } from "@/components/song-creator-panel"
import { SongLibraryPanel, type Song } from "@/components/song-library-panel"
import { LibraryPage } from "@/components/library-page"
import { LyricsPage } from "@/components/lyrics-page"
import { LoginPage } from "@/components/login-page"
import { RegisterPage } from "@/components/register-page"
import { SettingsPage } from "@/components/settings-page"

// Demo songs for the library
const demoSongs: Song[] = [
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
]

export default function Home() {
  const [authView, setAuthView] = useState<"login" | "register" | "app">("login")
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [activeTab, setActiveTab] = useState("create")
  const [isGenerating, setIsGenerating] = useState(false)
  const [generatedSongs, setGeneratedSongs] = useState<Song[]>([])
  const [selectedSongId, setSelectedSongId] = useState<string | null>(null)
  
  // Mobile player state
  const [currentSong, setCurrentSong] = useState<PlayingSong | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  
  const allSongs = [...generatedSongs, ...demoSongs]

  // Cleanup interval on unmount
  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [])

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
          setIsPlaying(false)
          if (intervalRef.current) clearInterval(intervalRef.current)
          return { ...prev, progress: 0 }
        }
        return { ...prev, progress: prev.progress + 0.5 }
      })
    }, 100)
  }, [])

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
    const currentIndex = allSongs.findIndex((s) => s.id === currentSong.id)
    if (currentIndex === -1) return
    
    let newIndex: number
    if (direction === "back") {
      newIndex = currentIndex > 0 ? currentIndex - 1 : allSongs.length - 1
    } else {
      newIndex = currentIndex < allSongs.length - 1 ? currentIndex + 1 : 0
    }
    
    const newSong = allSongs[newIndex]
    if (newSong && newSong.status === "completed") {
      playSong(newSong)
    }
  }, [currentSong, allSongs, playSong])

  const handleGenerate = useCallback(
    async (data: {
      title: string
      lyrics: string
      prompt: string
    }) => {
      setIsGenerating(true)

      const newSong: Song = {
        id: Date.now().toString(),
        title: data.title,
        genre: data.prompt.split(",")[0]?.trim() || "Mixed",
        duration: "3:18",
        createdAt: "Just now",
        status: "generating",
        prompt: data.prompt,
      }

      setGeneratedSongs((prev) => [newSong, ...prev])

      // Simulate generation
      await new Promise((resolve) => setTimeout(resolve, 3000))

      setGeneratedSongs((prev) =>
        prev.map((s) =>
          s.id === newSong.id ? { ...s, status: "completed" as const } : s
        )
      )
      setIsGenerating(false)
    },
    []
  )

  // Bottom padding: mobile/tablet = nav (3.75rem) + player (~4rem), desktop = player only (~4rem)
  const bottomPaddingMobile = "pb-[7.5rem]"
  const bottomPaddingDesktop = "lg:pb-[4rem]"

  const handleAuthSuccess = useCallback(() => {
    const mostRecent = [...generatedSongs, ...demoSongs].find(
      (s) => s.status === "completed"
    )
    if (mostRecent) loadSongWithoutPlaying(mostRecent)
    setAuthView("app")
  }, [generatedSongs, loadSongWithoutPlaying])

  // Auth views
  if (authView === "login") {
    return (
      <LoginPage
        onLogin={handleAuthSuccess}
        onSwitchToRegister={() => setAuthView("register")}
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

  return (
    <div className="flex h-screen flex-col bg-background">
      <AppHeader activeTab={activeTab} onTabChange={setActiveTab} onOpenSettings={() => setSettingsOpen(true)} />

      <div className={`flex flex-1 flex-col overflow-hidden ${bottomPaddingMobile} ${bottomPaddingDesktop}`}>
        {activeTab === "create" && (
          <main className="flex flex-1 overflow-hidden">
            {/* Left Panel - Song Creator */}
            <div className="w-full lg:w-1/2 lg:border-r border-border overflow-hidden">
              <SongCreatorPanel
                onGenerate={handleGenerate}
                isGenerating={isGenerating}
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
                currentPlayingId={currentSong?.id}
              />
            </div>
          </main>
        )}

        {activeTab === "library" && (
          <LibraryPage
            onPlaySong={playSong}
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
      </div>

      <MobilePlayer
        song={currentSong}
        isPlaying={isPlaying}
        onPlayPause={togglePlayPause}
        onSkipBack={() => skipToSong("back")}
        onSkipForward={() => skipToSong("forward")}
      />
      <BottomNav activeTab={activeTab} onTabChange={setActiveTab} />

      {/* Settings overlay */}
      {settingsOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-end bg-black/40 backdrop-blur-sm lg:items-stretch">
          <div className="flex h-full w-full max-w-md flex-col overflow-hidden bg-background shadow-2xl animate-in slide-in-from-right duration-300">
            <SettingsPage onBack={() => setSettingsOpen(false)} />
          </div>
        </div>
      )}
    </div>
  )
}
