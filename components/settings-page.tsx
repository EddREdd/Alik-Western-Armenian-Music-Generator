"use client"

import { useState } from "react"
import {
  ArrowLeft,
  Mail,
  Lock,
  Eye,
  EyeOff,
  CheckCircle2,
  Chrome,
  Link2,
  Link2Off,
  ShieldCheck,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"

interface SettingsPageProps {
  onBack: () => void
}

type Section = "main" | "change-email" | "verify-email" | "change-password"

// Mock user account state
const mockUser = {
  email: "user@example.com",
  hasPassword: true,
  googleLinked: true,
}

export function SettingsPage({ onBack }: SettingsPageProps) {
  const [section, setSection] = useState<Section>("main")

  // Email change state
  const [newEmail, setNewEmail] = useState("")
  const [emailLoading, setEmailLoading] = useState(false)
  const [verificationCode, setVerificationCode] = useState("")
  const [verifyLoading, setVerifyLoading] = useState(false)
  const [verifyError, setVerifyError] = useState("")

  // Password change state
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [showCurrent, setShowCurrent] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [passwordLoading, setPasswordLoading] = useState(false)
  const [passwordError, setPasswordError] = useState("")
  const [passwordSuccess, setPasswordSuccess] = useState(false)

  // Google state
  const [googleLinked, setGoogleLinked] = useState(mockUser.googleLinked)
  const [hasPassword] = useState(mockUser.hasPassword)
  const [userEmail] = useState(mockUser.email)

  const canUnlinkGoogle = hasPassword

  // --- Email change ---
  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setEmailLoading(true)
    await new Promise((r) => setTimeout(r, 1000))
    setEmailLoading(false)
    setSection("verify-email")
  }

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault()
    setVerifyError("")
    setVerifyLoading(true)
    await new Promise((r) => setTimeout(r, 1000))
    setVerifyLoading(false)
    if (verificationCode !== "123456") {
      setVerifyError("Invalid code. Please try again.")
      return
    }
    setSection("main")
    setNewEmail("")
    setVerificationCode("")
  }

  // --- Password change ---
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setPasswordError("")
    if (newPassword !== confirmPassword) {
      setPasswordError("Passwords do not match.")
      return
    }
    if (newPassword.length < 8) {
      setPasswordError("Password must be at least 8 characters.")
      return
    }
    setPasswordLoading(true)
    await new Promise((r) => setTimeout(r, 1000))
    setPasswordLoading(false)
    setPasswordSuccess(true)
    setCurrentPassword("")
    setNewPassword("")
    setConfirmPassword("")
    setTimeout(() => {
      setPasswordSuccess(false)
      setSection("main")
    }, 1800)
  }

  // --- Google ---
  const handleGoogleLink = async () => {
    await new Promise((r) => setTimeout(r, 800))
    setGoogleLinked(true)
  }

  const handleGoogleUnlink = async () => {
    if (!canUnlinkGoogle) return
    await new Promise((r) => setTimeout(r, 800))
    setGoogleLinked(false)
  }

  // ---- Sections ----

  if (section === "change-email") {
    return (
      <div className="flex h-full flex-col">
        <div className="flex items-center gap-3 border-b border-border px-6 py-4">
          <button
            onClick={() => setSection("main")}
            className="flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <h2 className="text-base font-semibold text-foreground">Change Email</h2>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-6">
          <p className="mb-6 text-sm text-muted-foreground">
            Your current email is <span className="font-medium text-foreground">{userEmail}</span>. Enter a new email address and we'll send a verification code to confirm it.
          </p>
          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="new-email" className="text-sm font-medium">New Email Address</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="new-email"
                  type="email"
                  placeholder="Enter new email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  className="pl-10"
                  required
                />
              </div>
            </div>
            <Button
              type="submit"
              className="w-full bg-primary text-primary-foreground hover:bg-primary/90"
              disabled={emailLoading}
            >
              {emailLoading ? "Sending..." : "Send Verification Code"}
            </Button>
          </form>
        </div>
      </div>
    )
  }

  if (section === "verify-email") {
    return (
      <div className="flex h-full flex-col">
        <div className="flex items-center gap-3 border-b border-border px-6 py-4">
          <button
            onClick={() => setSection("change-email")}
            className="flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <h2 className="text-base font-semibold text-foreground">Verify Email</h2>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-6">
          <div className="mb-6 flex flex-col items-center gap-3 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-secondary/10">
              <Mail className="h-7 w-7 text-secondary" />
            </div>
            <p className="text-sm text-muted-foreground">
              We sent a 6-digit code to <span className="font-medium text-foreground">{newEmail}</span>. Enter it below to confirm your new email.
            </p>
          </div>
          <form onSubmit={handleVerifyCode} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="code" className="text-sm font-medium">Verification Code</Label>
              <Input
                id="code"
                type="text"
                inputMode="numeric"
                placeholder="Enter 6-digit code"
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                className="text-center tracking-[0.4em] text-lg"
                required
              />
              {verifyError && (
                <p className="text-xs text-destructive">{verifyError}</p>
              )}
            </div>
            <Button
              type="submit"
              className="w-full bg-primary text-primary-foreground hover:bg-primary/90"
              disabled={verifyLoading || verificationCode.length !== 6}
            >
              {verifyLoading ? "Verifying..." : "Confirm New Email"}
            </Button>
            <button
              type="button"
              onClick={() => setSection("change-email")}
              className="w-full text-center text-sm text-muted-foreground hover:text-foreground"
            >
              Didn't receive it? Try again
            </button>
          </form>
        </div>
      </div>
    )
  }

  if (section === "change-password") {
    return (
      <div className="flex h-full flex-col">
        <div className="flex items-center gap-3 border-b border-border px-6 py-4">
          <button
            onClick={() => { setSection("main"); setPasswordError(""); setPasswordSuccess(false) }}
            className="flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <h2 className="text-base font-semibold text-foreground">Change Password</h2>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-6">
          {passwordSuccess ? (
            <div className="flex flex-col items-center gap-4 py-12 text-center">
              <CheckCircle2 className="h-14 w-14 text-secondary" />
              <p className="font-semibold text-foreground">Password updated!</p>
            </div>
          ) : (
            <form onSubmit={handlePasswordSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="current-pw" className="text-sm font-medium">Current Password</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="current-pw"
                    type={showCurrent ? "text" : "password"}
                    placeholder="Enter current password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    className="pl-10 pr-10"
                    required
                  />
                  <button type="button" onClick={() => setShowCurrent(!showCurrent)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                    {showCurrent ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-pw" className="text-sm font-medium">New Password</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="new-pw"
                    type={showNew ? "text" : "password"}
                    placeholder="Min. 8 characters"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="pl-10 pr-10"
                    required
                  />
                  <button type="button" onClick={() => setShowNew(!showNew)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                    {showNew ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirm-pw" className="text-sm font-medium">Confirm New Password</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="confirm-pw"
                    type={showConfirm ? "text" : "password"}
                    placeholder="Repeat new password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="pl-10 pr-10"
                    required
                  />
                  <button type="button" onClick={() => setShowConfirm(!showConfirm)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                    {showConfirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>
              {passwordError && (
                <p className="text-xs text-destructive">{passwordError}</p>
              )}
              <Button
                type="submit"
                className="w-full bg-primary text-primary-foreground hover:bg-primary/90"
                disabled={passwordLoading}
              >
                {passwordLoading ? "Updating..." : "Update Password"}
              </Button>
            </form>
          )}
        </div>
      </div>
    )
  }

  // --- Main settings view ---
  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center gap-3 border-b border-border px-6 py-4">
        <button
          onClick={onBack}
          className="flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <h2 className="text-base font-semibold text-foreground">Settings</h2>
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* Account section */}
        <div className="px-6 py-5">
          <h3 className="mb-4 text-xs font-semibold uppercase tracking-widest text-muted-foreground">Account</h3>
          <div className="space-y-3">
            {/* Email */}
            <div className="flex items-center justify-between rounded-xl border border-border bg-card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
                  <Mail className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">Email Address</p>
                  <p className="text-xs text-muted-foreground">{userEmail}</p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                className="text-xs"
                onClick={() => setSection("change-email")}
              >
                Change
              </Button>
            </div>

            {/* Password */}
            <div className="flex items-center justify-between rounded-xl border border-border bg-card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
                  <Lock className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">Password</p>
                  <p className="text-xs text-muted-foreground">
                    {hasPassword ? "Last changed recently" : "No password set"}
                  </p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                className="text-xs"
                onClick={() => setSection("change-password")}
              >
                {hasPassword ? "Change" : "Set"}
              </Button>
            </div>
          </div>
        </div>

        <Separator />

        {/* Linked accounts */}
        <div className="px-6 py-5">
          <h3 className="mb-4 text-xs font-semibold uppercase tracking-widest text-muted-foreground">Linked Accounts</h3>
          <div className="rounded-xl border border-border bg-card p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-muted">
                <Chrome className="h-4 w-4 text-foreground" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-foreground">Google</p>
                <p className="text-xs text-muted-foreground">
                  {googleLinked ? "Connected" : "Not connected"}
                </p>
              </div>
              {googleLinked ? (
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-secondary" />
                  <Button
                    variant="outline"
                    size="sm"
                    className={`gap-1.5 text-xs ${!canUnlinkGoogle ? "cursor-not-allowed opacity-50" : "text-destructive hover:text-destructive"}`}
                    onClick={canUnlinkGoogle ? handleGoogleUnlink : undefined}
                    disabled={!canUnlinkGoogle}
                    title={!canUnlinkGoogle ? "Set a password before unlinking Google" : "Unlink Google account"}
                  >
                    <Link2Off className="h-3.5 w-3.5" />
                    Unlink
                  </Button>
                </div>
              ) : (
                <Button
                  variant="outline"
                  size="sm"
                  className="gap-1.5 text-xs"
                  onClick={handleGoogleLink}
                >
                  <Link2 className="h-3.5 w-3.5" />
                  Link
                </Button>
              )}
            </div>
            {googleLinked && !canUnlinkGoogle && (
              <p className="mt-3 rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">
                You need to set a password before unlinking your Google account.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
