"use client"

import { useState } from "react"
import Image from "next/image"
import { Mail, Lock, Eye, EyeOff } from "lucide-react"
import { Button } from "@/components/ui/button"
import { GoogleAuthButton } from "@/components/google-auth-button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { login, loginWithGoogle, type AuthSession } from "@/lib/auth-api"
import { t, useUiLanguage } from "@/lib/i18n"

interface LoginPageProps {
  onLogin: (session: AuthSession) => void
  onSwitchToRegister: () => void
  onForgotPassword?: () => void
}

export function LoginPage({ onLogin, onSwitchToRegister, onForgotPassword }: LoginPageProps) {
  useUiLanguage()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setIsLoading(true)

    try {
      const session = await login({ email, password })
      onLogin(session)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : t("unableToSignIn"))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-primary px-4 py-8">
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
          <h1 className="text-2xl font-bold text-foreground">{t("welcomeBack")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {t("signInToContinue")}
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
                placeholder={t("password")}
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
          </div>

          {/* Forgot Password */}
          <div className="text-right">
            <a
              href="/?auth=forgot-password"
              onClick={(e) => {
                if (onForgotPassword) {
                  e.preventDefault()
                  onForgotPassword()
                }
              }}
              className="cursor-pointer text-sm text-secondary hover:text-secondary/80 hover:underline"
            >
              {t("forgotPasswordLink")}
            </a>
          </div>

          {/* Submit Button */}
          <Button
            type="submit"
            className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
            disabled={isLoading}
          >
            {isLoading ? t("signingIn") : t("signIn")}
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
          text="continue_with"
          disabled={isLoading}
          onCredential={async (credential) => {
            setIsLoading(true)
            setError("")
            try {
              const session = await loginWithGoogle(credential)
              onLogin(session)
            } catch (googleError) {
              setError(
                googleError instanceof Error ? googleError.message : t("googleSignInFailed"),
              )
            } finally {
              setIsLoading(false)
            }
          }}
        />

        {/* Switch to Register */}
        <p className="mt-6 text-center text-sm text-muted-foreground">
          {t("dontHaveAccount")}{" "}
          <button
            type="button"
            onClick={onSwitchToRegister}
            className="font-medium text-secondary hover:text-secondary/80 hover:underline"
          >
            {t("signUp")}
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
