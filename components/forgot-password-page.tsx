"use client"

import { useState } from "react"
import Image from "next/image"
import { ArrowLeft, Eye, EyeOff, KeyRound, Mail } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  requestForgotPasswordCode,
  resetForgotPassword,
  verifyForgotPasswordCode,
} from "@/lib/auth-api"
import { AuthLanguageSwitcher } from "@/components/auth-language-switcher"
import { t, useUiLanguage } from "@/lib/i18n"

interface ForgotPasswordPageProps {
  onBackToLogin: () => void
}

type ForgotStep = "request" | "verify" | "reset"

export function ForgotPasswordPage({ onBackToLogin }: ForgotPasswordPageProps) {
  useUiLanguage()
  const [step, setStep] = useState<ForgotStep>("request")
  const [email, setEmail] = useState("")
  const [code, setCode] = useState("")
  const [resetToken, setResetToken] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  const handleSendCode = async () => {
    setError("")
    setSuccess("")
    setIsLoading(true)
    try {
      await requestForgotPasswordCode(email)
      setCode("")
      setResetToken("")
      setStep("verify")
      setSuccess(t("codeSent"))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : t("unableToSendCode"))
    } finally {
      setIsLoading(false)
    }
  }

  const handleVerifyCode = async () => {
    setError("")
    setSuccess("")
    setIsLoading(true)
    try {
      const token = await verifyForgotPasswordCode(email, code)
      setResetToken(token)
      setStep("reset")
      setSuccess(t("codeVerified"))
    } catch (verifyError) {
      setError(
        verifyError instanceof Error
          ? verifyError.message
          : t("wrongCodeResend"),
      )
    } finally {
      setIsLoading(false)
    }
  }

  const handleResetPassword = async () => {
    setError("")
    setSuccess("")
    setIsLoading(true)
    try {
      await resetForgotPassword({
        email,
        resetToken,
        newPassword,
        confirmPassword,
      })
      setSuccess(t("passwordChangedRedirecting"))
      setTimeout(() => {
        onBackToLogin()
      }, 700)
    } catch (resetError) {
      setError(resetError instanceof Error ? resetError.message : t("unableToResetPassword"))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-primary px-4 py-8">
      <AuthLanguageSwitcher />
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

      <div className="w-full max-w-md rounded-2xl bg-card p-8 shadow-xl">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-foreground">{t("forgotPassword")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {step === "request" && t("enterAccountEmailResetCode")}
            {step === "verify" && t("enter5DigitCode")}
            {step === "reset" && t("setNewPassword")}
          </p>
        </div>

        {error ? (
          <div className="mb-4 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">{error}</div>
        ) : null}
        {success ? (
          <div className="mb-4 rounded-lg bg-secondary/10 p-3 text-sm text-secondary">{success}</div>
        ) : null}

        <div className="space-y-4">
          {(step === "request" || step === "verify" || step === "reset") && (
            <div className="space-y-2">
              <Label htmlFor="fp-email" className="text-sm font-medium text-foreground">
                {t("email")}
              </Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="fp-email"
                  type="email"
                  placeholder={t("enterEmail")}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="pl-10"
                  disabled={step !== "request" || isLoading}
                  required
                />
              </div>
            </div>
          )}

          {step === "verify" || step === "reset" ? (
            <div className="space-y-2">
              <Label htmlFor="fp-code" className="text-sm font-medium text-foreground">
                {t("verificationCode")}
              </Label>
              <Input
                id="fp-code"
                type="text"
                inputMode="numeric"
                placeholder={t("enter5DigitCode")}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 5))}
                className="text-center tracking-[0.35em] text-lg"
                disabled={step === "reset" || isLoading}
              />
            </div>
          ) : null}

          {step === "reset" ? (
            <>
              <div className="space-y-2">
                <Label htmlFor="fp-new-password" className="text-sm font-medium text-foreground">
                  {t("newPassword")}
                </Label>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="fp-new-password"
                    type={showNewPassword ? "text" : "password"}
                    placeholder={t("enterNewPassword")}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="pl-10 pr-10"
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="fp-confirm-password" className="text-sm font-medium text-foreground">
                  {t("confirmPassword")}
                </Label>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="fp-confirm-password"
                    type={showConfirmPassword ? "text" : "password"}
                    placeholder={t("confirmNewPassword")}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="pl-10 pr-10"
                    disabled={isLoading}
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
            </>
          ) : null}

          {step === "request" ? (
            <Button
              className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
              disabled={isLoading || !email.trim()}
              onClick={() => void handleSendCode()}
            >
              {isLoading ? t("sending") : t("sendCode")}
            </Button>
          ) : null}

          {step === "verify" ? (
            <>
              <Button
                className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
                disabled={isLoading || code.length !== 5}
                onClick={() => void handleVerifyCode()}
              >
                {isLoading ? t("verifying") : t("verifyCode")}
              </Button>
              <Button
                variant="outline"
                className="w-full"
                disabled={isLoading}
                onClick={() => void handleSendCode()}
              >
                {isLoading ? t("sending") : t("resendCode")}
              </Button>
            </>
          ) : null}

          {step === "reset" ? (
            <Button
              className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
              disabled={isLoading || !newPassword || !confirmPassword || !resetToken}
              onClick={() => void handleResetPassword()}
            >
              {isLoading ? t("saving") : t("changePasswordButton")}
            </Button>
          ) : null}
        </div>

        <button
          type="button"
          onClick={onBackToLogin}
          className="mt-6 flex items-center justify-center gap-1 text-sm text-muted-foreground hover:text-foreground mx-auto"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("backToLogin")}
        </button>
      </div>

      <p className="mt-8 text-center text-xs text-primary-foreground/60">
        {t("westernArmenianMusicGenerator")}
      </p>
    </div>
  )
}
