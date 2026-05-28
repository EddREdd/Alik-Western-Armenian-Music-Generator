"use client"

import { LanguageSwitcher } from "@/components/language-switcher"

export function AuthLanguageSwitcher() {
  return (
    <div className="fixed right-4 top-4 z-50">
      <LanguageSwitcher className="h-9 rounded-md border border-primary-foreground/30 bg-card px-2 text-sm text-foreground shadow-sm outline-none" />
    </div>
  )
}
