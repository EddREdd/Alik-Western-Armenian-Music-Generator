"use client"

import { useState, useCallback } from "react"
import { AppHeader } from "@/components/app-header"
import { BottomNav } from "@/components/bottom-nav"
import { SongCreatorPanel } from "@/components/song-creator-panel"
import { SongLibraryPanel, type Song } from "@/components/song-library-panel"
import { LibraryPage } from "@/components/library-page"
import { LyricsPage } from "@/components/lyrics-page"

export default function Home() {
  const [activeTab, setActiveTab] = useState("create")
  const [isGenerating, setIsGenerating] = useState(false)
  const [generatedSongs, setGeneratedSongs] = useState<Song[]>([])

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

  return (
    <div className="flex h-screen flex-col bg-background">
      <AppHeader activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="flex flex-1 flex-col overflow-hidden pb-[4.5rem] lg:pb-0">
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
              <SongLibraryPanel songs={generatedSongs} />
            </div>
          </main>
        )}

        {activeTab === "library" && <LibraryPage />}
        {activeTab === "lyrics" && <LyricsPage />}
      </div>

      <BottomNav activeTab={activeTab} onTabChange={setActiveTab} />
    </div>
  )
}
