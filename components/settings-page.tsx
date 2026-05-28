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
import { GoogleAuthButton } from "@/components/google-auth-button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import {
  changePassword,
  linkGoogleAccount,
  requestEmailChange,
  unlinkGoogleAccount,
  verifyEmailChange,
  type AuthUser,
} from "@/lib/auth-api"
import { t, useUiLanguage } from "@/lib/i18n"

interface SettingsPageProps {
  onBack: () => void
  sessionToken: string
  user: AuthUser
  onUserChange: (user: AuthUser) => void
  onLogout: () => Promise<void> | void
}

type Section = "main" | "change-email" | "verify-email" | "change-password"

export function SettingsPage({
  onBack,
  sessionToken,
  user,
  onUserChange,
  onLogout,
}: SettingsPageProps) {
  useUiLanguage()
  const [section, setSection] = useState<Section>("main")

  // Email change state
  const [newEmail, setNewEmail] = useState("")
  const [emailLoading, setEmailLoading] = useState(false)
  const [emailChallengePreview, setEmailChallengePreview] = useState<string | null>(null)
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
  const [googleLoading, setGoogleLoading] = useState(false)
  const [googleError, setGoogleError] = useState("")
  const [accountMessage, setAccountMessage] = useState("")
  const [logoutLoading, setLogoutLoading] = useState(false)

  const canUnlinkGoogle = user.hasPassword && !!user.email

  // --- Email change ---
  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setVerifyError("")
    setEmailLoading(true)
    try {
      const challenge = await requestEmailChange(sessionToken, newEmail)
      setEmailChallengePreview(challenge.devOtpPreview ?? null)
      setSection("verify-email")
    } catch (emailError) {
      setVerifyError(emailError instanceof Error ? emailError.message : t("unableToChangeEmail"))
    } finally {
      setEmailLoading(false)
    }
  }

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault()
    setVerifyError("")
    setVerifyLoading(true)
    try {
      const updatedUser = await verifyEmailChange(sessionToken, verificationCode)
      onUserChange(updatedUser)
      setAccountMessage(t("emailUpdatedSuccessfully"))
      setSection("main")
      setNewEmail("")
      setVerificationCode("")
      setEmailChallengePreview(null)
    } catch (codeError) {
      setVerifyError(codeError instanceof Error ? codeError.message : t("invalidCode"))
    } finally {
      setVerifyLoading(false)
    }
  }

  // --- Password change ---
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setPasswordError("")
    if (newPassword !== confirmPassword) {
      setPasswordError(t("passwordMismatchPeriod"))
      return
    }
    if (newPassword.length < 8) {
      setPasswordError(t("passwordMin8ErrorPeriod"))
      return
    }
    setPasswordLoading(true)
    try {
      const updatedUser = await changePassword(sessionToken, {
        currentPassword,
        newPassword,
        confirmPassword,
      })
      onUserChange(updatedUser)
      setPasswordSuccess(true)
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
      setTimeout(() => {
        setPasswordSuccess(false)
        setSection("main")
      }, 1800)
    } catch (changeError) {
      setPasswordError(
        changeError instanceof Error ? changeError.message : t("unableToUpdatePassword"),
      )
    } finally {
      setPasswordLoading(false)
    }
  }

  // --- Google ---
  const handleGoogleUnlink = async () => {
    if (!canUnlinkGoogle) return
    setGoogleError("")
    setGoogleLoading(true)
    try {
      const updatedUser = await unlinkGoogleAccount(sessionToken)
      onUserChange(updatedUser)
      setAccountMessage(t("googleAccountUnlinked"))
    } catch (unlinkError) {
      setGoogleError(
        unlinkError instanceof Error ? unlinkError.message : t("unableToUnlinkGoogleAccount"),
      )
    } finally {
      setGoogleLoading(false)
    }
  }

  const handleSignOut = async () => {
    if (logoutLoading) return
    setLogoutLoading(true)
    try {
      await onLogout()
    } finally {
      setLogoutLoading(false)
    }
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
          <h2 className="text-base font-semibold text-foreground">{t("emailAddress")}</h2>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-6">
          <p className="mb-6 text-sm text-muted-foreground">
            {t("yourCurrentEmail", { email: user.email ?? "" })}
          </p>
          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="new-email" className="text-sm font-medium">{t("newEmailAddress")}</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="new-email"
                  type="email"
                  placeholder={t("enterNewEmail")}
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
              {emailLoading ? t("sending") : t("sendVerificationCode")}
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
          <h2 className="text-base font-semibold text-foreground">{t("verifyEmail")}</h2>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-6">
          <div className="mb-6 flex flex-col items-center gap-3 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-secondary/10">
              <Mail className="h-7 w-7 text-secondary" />
            </div>
            <p className="text-sm text-muted-foreground">
              {t("weSentCodeTo", { email: newEmail })}
            </p>
          </div>
          <form onSubmit={handleVerifyCode} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="code" className="text-sm font-medium">{t("verificationCode")}</Label>
              <Input
                id="code"
                type="text"
                inputMode="numeric"
                placeholder={t("enter6DigitCode")}
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                className="text-center tracking-[0.4em] text-lg"
                required
              />
              {verifyError && (
                <p className="text-xs text-destructive">{verifyError}</p>
              )}
              {emailChallengePreview ? (
                <p className="text-xs text-muted-foreground">
                  {t("localDevOtpPreview")} <span className="font-semibold text-foreground">{emailChallengePreview}</span>
                </p>
              ) : null}
            </div>
            <Button
              type="submit"
              className="w-full bg-primary text-primary-foreground hover:bg-primary/90"
              disabled={verifyLoading || verificationCode.length !== 6}
            >
              {verifyLoading ? t("verifying") : t("confirmNewEmail")}
            </Button>
            <button
              type="button"
              onClick={() => setSection("change-email")}
              className="w-full text-center text-sm text-muted-foreground hover:text-foreground"
            >
              {t("didntReceiveTryAgain")}
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
          <h2 className="text-base font-semibold text-foreground">{t("changePassword")}</h2>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-6">
          {passwordSuccess ? (
            <div className="flex flex-col items-center gap-4 py-12 text-center">
              <CheckCircle2 className="h-14 w-14 text-secondary" />
              <p className="font-semibold text-foreground">{t("passwordUpdated")}</p>
            </div>
          ) : (
            <form onSubmit={handlePasswordSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="current-pw" className="text-sm font-medium">{t("currentPassword")}</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="current-pw"
                    type={showCurrent ? "text" : "password"}
                    placeholder={t("enterCurrentPassword")}
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
                <Label htmlFor="new-pw" className="text-sm font-medium">{t("newPassword")}</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="new-pw"
                    type={showNew ? "text" : "password"}
                    placeholder={t("min8Characters")}
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
                <Label htmlFor="confirm-pw" className="text-sm font-medium">{t("confirmNewPassword")}</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="confirm-pw"
                    type={showConfirm ? "text" : "password"}
                    placeholder={t("repeatNewPassword")}
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
                {passwordLoading ? t("updating") : t("updatePassword")}
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
        <h2 className="text-base font-semibold text-foreground">{t("settings")}</h2>
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* Account section */}
        <div className="px-6 py-5">
          <h3 className="mb-4 text-xs font-semibold uppercase tracking-widest text-muted-foreground">{t("account")}</h3>
          <div className="space-y-3">
            {/* Email */}
            <div className="flex items-center justify-between rounded-xl border border-border bg-card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
                  <Mail className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">{t("emailAddress")}</p>
                  <p className="text-xs text-muted-foreground">{user.email}</p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                className="text-xs"
                onClick={() => setSection("change-email")}
              >
                {t("change")}
              </Button>
            </div>

            {/* Password */}
            <div className="flex items-center justify-between rounded-xl border border-border bg-card p-4">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
                  <Lock className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <p className="text-sm font-medium text-foreground">{t("password")}</p>
                  <p className="text-xs text-muted-foreground">
                    {user.hasPassword ? t("lastChangedRecently") : t("noPasswordSet")}
                  </p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                className="text-xs"
                onClick={() => setSection("change-password")}
              >
                {user.hasPassword ? t("change") : t("set")}
              </Button>
            </div>
          </div>
        </div>

        <Separator />

        {/* Linked accounts */}
        <div className="px-6 py-5">
          <h3 className="mb-4 text-xs font-semibold uppercase tracking-widest text-muted-foreground">{t("linkedAccounts")}</h3>
          <div className="rounded-xl border border-border bg-card p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-muted">
                <Chrome className="h-4 w-4 text-foreground" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-foreground">{t("google")}</p>
                <p className="text-xs text-muted-foreground">
                  {user.googleLinked ? t("connected") : t("notConnected")}
                </p>
              </div>
              {user.googleLinked ? (
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-secondary" />
                  <Button
                    variant="outline"
                    size="sm"
                    className={`gap-1.5 text-xs ${!canUnlinkGoogle ? "cursor-not-allowed opacity-50" : "text-destructive hover:text-destructive"}`}
                    onClick={canUnlinkGoogle ? handleGoogleUnlink : undefined}
                    disabled={!canUnlinkGoogle || googleLoading}
                    title={!canUnlinkGoogle ? t("setPasswordBeforeUnlink") : t("unlinkGoogleAccount")}
                  >
                    <Link2Off className="h-3.5 w-3.5" />
                    {googleLoading ? t("unlinking") : t("unlink")}
                  </Button>
                </div>
              ) : (
                <div className="w-[220px]">
                  <GoogleAuthButton
                    text="continue_with"
                    disabled={googleLoading}
                    onCredential={async (credential) => {
                      setGoogleError("")
                      setGoogleLoading(true)
                      try {
                        const updatedUser = await linkGoogleAccount(sessionToken, credential)
                        onUserChange(updatedUser)
                        setAccountMessage(t("googleAccountLinked"))
                      } catch (linkError) {
                        setGoogleError(
                          linkError instanceof Error
                            ? linkError.message
                            : t("unableToLinkGoogleAccount"),
                        )
                      } finally {
                        setGoogleLoading(false)
                      }
                    }}
                  />
                </div>
              )}
            </div>
            {user.googleLinked && !canUnlinkGoogle && (
              <p className="mt-3 rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">
                {t("youNeedPasswordBeforeUnlink")}
              </p>
            )}
            {googleError ? (
              <p className="mt-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">
                {googleError}
              </p>
            ) : null}
          </div>
        </div>

        <Separator />

        <div className="px-6 py-5">
          <h3 className="mb-4 text-xs font-semibold uppercase tracking-widest text-muted-foreground">{t("session")}</h3>
          {accountMessage ? (
            <p className="mb-4 rounded-lg bg-secondary/10 px-3 py-2 text-xs text-secondary">
              {accountMessage}
            </p>
          ) : null}
          <button
            type="button"
            className="inline-flex h-9 w-full cursor-pointer items-center justify-center rounded-md border border-border bg-background px-4 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground disabled:cursor-not-allowed disabled:opacity-50"
            onClick={() => void handleSignOut()}
            disabled={logoutLoading}
          >
            {logoutLoading ? t("signingOut") : t("signOut")}
          </button>
        </div>
      </div>
    </div>
  )
}
