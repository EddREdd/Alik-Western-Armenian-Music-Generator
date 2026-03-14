"use client"

import { Library, FileText, Plus, Shield } from "lucide-react"

interface BottomNavProps {
  activeTab: string
  onTabChange: (tab: string) => void
  showAdmin?: boolean
}

export function BottomNav({ activeTab, onTabChange, showAdmin = false }: BottomNavProps) {
  const items = [
    { id: "library", label: "Library", icon: Library },
    { id: "create", label: "Create", icon: Plus, accent: true },
    { id: "lyrics", label: "Lyrics", icon: FileText },
    ...(showAdmin ? [{ id: "admin", label: "Admin", icon: Shield }] : []),
  ]

  return (
    <nav className="lg:hidden fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-primary">
      <div className="flex items-center justify-around py-1.5 pb-[max(0.375rem,env(safe-area-inset-bottom))]">
        {items.map((item) => {
          const isActive = activeTab === item.id
          const Icon = item.icon

          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className={`flex flex-col items-center gap-0.5 rounded-lg px-4 py-1 transition-colors ${
                item.accent && isActive
                  ? "text-secondary"
                  : isActive
                    ? "text-secondary"
                    : "text-primary-foreground/60"
              }`}
            >
              {item.accent ? (
                <span
                  className={`flex h-8 w-8 items-center justify-center rounded-full transition-colors ${
                    isActive
                      ? "bg-secondary text-secondary-foreground"
                      : "bg-primary-foreground/15 text-primary-foreground/80"
                  }`}
                >
                  <Icon className="h-4 w-4" />
                </span>
              ) : (
                <Icon className="h-5 w-5" />
              )}
              <span className="text-[10px] font-medium tracking-wide">
                {item.label}
              </span>
            </button>
          )
        })}
      </div>
    </nav>
  )
}
