"use client"

import Image from "next/image"
import { Library, FileText, Plus, Settings, LogOut, User, Coins } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"

interface AppHeaderProps {
  activeTab: string
  onTabChange: (tab: string) => void
  onOpenSettings: () => void
}

export function AppHeader({ activeTab, onTabChange, onOpenSettings }: AppHeaderProps) {
  return (
    <header className="bg-primary text-primary-foreground">
      <div className="flex items-center justify-between px-6 py-3">
        <button
          onClick={() => onTabChange("create")}
          className="flex items-center"
        >
          <Image
            src="/images/logo.png"
            alt="Alik logo"
            width={120}
            height={40}
            className="h-8 w-auto object-contain"
            priority
          />
        </button>

        <nav className="hidden lg:flex items-center gap-1">
          <button
            onClick={() => onTabChange("library")}
            className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium tracking-wide transition-colors ${
              activeTab === "library"
                ? "bg-secondary text-secondary-foreground"
                : "text-primary-foreground/80 hover:bg-primary-foreground/10 hover:text-primary-foreground"
            }`}
          >
            <Library className="h-4 w-4" />
            Library
          </button>
          <button
            onClick={() => onTabChange("lyrics")}
            className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium tracking-wide transition-colors ${
              activeTab === "lyrics"
                ? "bg-secondary text-secondary-foreground"
                : "text-primary-foreground/80 hover:bg-primary-foreground/10 hover:text-primary-foreground"
            }`}
          >
            <FileText className="h-4 w-4" />
            Lyrics
          </button>
        </nav>

        <div className="flex items-center gap-2 lg:gap-3">
          <Button
            onClick={() => onTabChange("create")}
            className={`hidden lg:flex gap-1.5 font-medium tracking-wide ${
              activeTab === "create"
                ? "bg-secondary text-secondary-foreground hover:bg-secondary/90"
                : "bg-primary-foreground/15 text-primary-foreground hover:bg-primary-foreground/25"
            }`}
            size="sm"
          >
            <Plus className="h-4 w-4" />
            Create
          </Button>

          {/* Credit Counter */}
          <div className="flex items-center gap-1.5 rounded-full bg-primary-foreground/10 px-3 py-1.5">
            <Coins className="h-4 w-4 text-secondary" />
            <span className="text-sm font-semibold text-primary-foreground">250</span>
          </div>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className="relative h-9 w-9 rounded-full hover:bg-primary-foreground/10"
              >
                <Avatar className="h-9 w-9 border-2 border-secondary">
                  <AvatarFallback className="bg-secondary text-secondary-foreground">
                    <User className="h-5 w-5" />
                  </AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuItem onClick={onOpenSettings}>
                <Settings className="mr-2 h-4 w-4" />
                Settings
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="text-destructive focus:text-destructive">
                <LogOut className="mr-2 h-4 w-4" />
                Log Out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  )
}
