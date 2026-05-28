"use client"

import { useState } from "react"
import Image from "next/image"
import { Mail, Lock, Eye, EyeOff, Ticket } from "lucide-react"
import { Button } from "@/components/ui/button"
import { GoogleAuthButton } from "@/components/google-auth-button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  register,
  loginWithGoogle,
  type AuthSession,
} from "@/lib/auth-api"
import { AuthLanguageSwitcher } from "@/components/auth-language-switcher"
import { t, useUiLanguage } from "@/lib/i18n"

interface RegisterPageProps {
  onRegisterComplete: (session: AuthSession) => void
  onSwitchToLogin: () => void
}

export function RegisterPage({ onRegisterComplete, onSwitchToLogin }: RegisterPageProps) {
  useUiLanguage()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [inviteCode, setInviteCode] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")

    if (password !== confirmPassword) {
      setError(t("passwordMismatch"))
      return
    }

    if (password.length < 8) {
      setError(t("passwordMin8Error"))
      return
    }

    setIsLoading(true)

    try {
      await register({
        email,
        password,
        inviteCode,
      })
      onSwitchToLogin()
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : t("unableToRegister"))
    } finally {
      setIsLoading(false)
    }
  }

  // Registration Form Step
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-primary px-4 py-8">
      <AuthLanguageSwitcher />
      {/* Logo */}
      <div className="mb-8">
        <Image
          src="/images/logo.png"
          alt={t("alikLogoAlt")}
          width={160}
          height={53}
          className="h-12 w-auto object-contain"
          priority
        />
      </div>

      {/* Card */}
      <div className="w-full max-w-md rounded-2xl bg-card p-8 shadow-xl">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-foreground">{t("createAccount")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {t("joinAndCreateMusic")}
          </p>
        </div>

        {error && (
          <div className="mb-4 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Email */}
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-medium text-foreground">
              {t("email")}
            </Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="email"
                type="email"
                placeholder={t("enterEmail")}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="pl-10"
                required
              />
            </div>
          </div>

          {/* Password */}
          <div className="space-y-2">
            <Label htmlFor="password" className="text-sm font-medium text-foreground">
              {t("password")}
            </Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder={t("createPassword")}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="pl-10 pr-10"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
            <p className="text-xs text-muted-foreground">
              {t("mustBeAtLeast8Characters")}
            </p>
          </div>

          {/* Confirm Password */}
          <div className="space-y-2">
            <Label htmlFor="confirmPassword" className="text-sm font-medium text-foreground">
              {t("confirmPassword")}
            </Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                placeholder={t("confirmYourPassword")}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="pl-10 pr-10"
                required
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showConfirmPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>

          {/* Invite Code */}
          <div className="space-y-2">
            <Label htmlFor="inviteCode" className="text-sm font-medium text-foreground">
              {t("inviteCode")}
            </Label>
            <div className="relative">
              <Ticket className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="inviteCode"
                type="text"
                placeholder={t("enterInviteCode")}
                value={inviteCode}
                onChange={(e) => setInviteCode(e.target.value)}
                className="pl-10"
                required
              />
            </div>
            <p className="text-xs text-muted-foreground">
              {t("youNeedInviteCode")}
            </p>
          </div>

          {/* Submit Button */}
          <Button
            type="submit"
            className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
            disabled={isLoading}
          >
            {isLoading ? t("creatingAccount") : t("createAccountCta")}
          </Button>
        </form>

        {/* Divider */}
        <div className="my-6 flex items-center gap-4">
          <div className="h-px flex-1 bg-border" />
          <span className="text-xs text-muted-foreground">{t("orContinueWith")}</span>
          <div className="h-px flex-1 bg-border" />
        </div>

        {/* Google Button */}
        <GoogleAuthButton
          text="signup_with"
          disabled={isLoading}
          onCredential={async (credential) => {
            if (!inviteCode.trim()) {
              setError(t("inviteCodeGoogleRequired"))
              return
            }

            setError("")
            setIsLoading(true)
            try {
              const session = await loginWithGoogle(credential, inviteCode)
              onRegisterComplete(session)
            } catch (googleError) {
              setError(
                googleError instanceof Error ? googleError.message : t("googleSignUpFailed"),
              )
            } finally {
              setIsLoading(false)
            }
          }}
        />

        {/* Switch to Login */}
        <p className="mt-6 text-center text-sm text-muted-foreground">
          {t("accountAlreadyExists")}{" "}
          <button
            type="button"
            onClick={onSwitchToLogin}
            className="font-medium text-secondary hover:text-secondary/80 hover:underline"
          >
            {t("signInCta")}
          </button>
        </p>
      </div>

      {/* Footer */}
      <p className="mt-8 text-center text-xs text-primary-foreground/60">
        {t("westernArmenianMusicGenerator")}
      </p>
    </div>
  )
}
