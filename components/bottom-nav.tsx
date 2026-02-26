"use client"

import { Library, FileText, Plus } from "lucide-react"

interface BottomNavProps {
  activeTab: string
  onTabChange: (tab: string) => void
}

export function BottomNav({ activeTab, onTabChange }: BottomNavProps) {
  const items = [
    { id: "library", label: "Library", icon: Library },
    { id: "create", label: "Create", icon: Plus, accent: true },
    { id: "lyrics", label: "Lyrics", icon: FileText },
  ]

  return (
    <nav className="lg:hidden fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-primary">
      <div className="flex items-center justify-around py-2 pb-[max(0.5rem,env(safe-area-inset-bottom))]">
        {items.map((item) => {
          const isActive = activeTab === item.id
          const Icon = item.icon

          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className={`flex flex-col items-center gap-1 rounded-lg px-5 py-1.5 transition-colors ${
                item.accent && isActive
                  ? "text-secondary"
                  : isActive
                    ? "text-secondary"
                    : "text-primary-foreground/60"
              }`}
            >
              {item.accent ? (
                <span
                  className={`flex h-10 w-10 items-center justify-center rounded-full transition-colors ${
                    isActive
                      ? "bg-secondary text-secondary-foreground"
                      : "bg-primary-foreground/15 text-primary-foreground/80"
                  }`}
                >
                  <Icon className="h-5 w-5" />
                </span>
              ) : (
                <Icon className="h-5 w-5" />
              )}
              <span className="text-[11px] font-medium tracking-wide">
                {item.label}
              </span>
            </button>
          )
        })}
      </div>
    </nav>
  )
}
