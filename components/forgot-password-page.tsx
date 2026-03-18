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

interface ForgotPasswordPageProps {
  onBackToLogin: () => void
}

type ForgotStep = "request" | "verify" | "reset"

export function ForgotPasswordPage({ onBackToLogin }: ForgotPasswordPageProps) {
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
      setSuccess("Code sent. Check your email.")
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to send code")
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
      setSuccess("Code verified. Set your new password.")
    } catch (verifyError) {
      setError(
        verifyError instanceof Error
          ? verifyError.message
          : "Wrong code, please click resend for a new one",
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
      setSuccess("Password changed successfully. Redirecting to login...")
      setTimeout(() => {
        onBackToLogin()
      }, 700)
    } catch (resetError) {
      setError(resetError instanceof Error ? resetError.message : "Unable to reset password")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-primary px-4 py-8">
      <div className="mb-8">
        <Image
          src="/images/logo.png"
          alt="Alik logo"
          width={160}
          height={53}
          className="h-12 w-auto object-contain"
          priority
        />
      </div>

      <div className="w-full max-w-md rounded-2xl bg-card p-8 shadow-xl">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-foreground">Forgot Password</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {step === "request" && "Enter your account email to receive a reset code"}
            {step === "verify" && "Enter the 5-digit code sent to your email"}
            {step === "reset" && "Set your new password"}
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
                Email
              </Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="fp-email"
                  type="email"
                  placeholder="Enter your email"
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
                Verification Code
              </Label>
              <Input
                id="fp-code"
                type="text"
                inputMode="numeric"
                placeholder="Enter 5-digit code"
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
                  New Password
                </Label>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="fp-new-password"
                    type={showNewPassword ? "text" : "password"}
                    placeholder="Enter new password"
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
                  Confirm Password
                </Label>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="fp-confirm-password"
                    type={showConfirmPassword ? "text" : "password"}
                    placeholder="Confirm new password"
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
              {isLoading ? "Sending..." : "Send Code"}
            </Button>
          ) : null}

          {step === "verify" ? (
            <>
              <Button
                className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
                disabled={isLoading || code.length !== 5}
                onClick={() => void handleVerifyCode()}
              >
                {isLoading ? "Verifying..." : "Verify Code"}
              </Button>
              <Button
                variant="outline"
                className="w-full"
                disabled={isLoading}
                onClick={() => void handleSendCode()}
              >
                {isLoading ? "Sending..." : "Resend Code"}
              </Button>
            </>
          ) : null}

          {step === "reset" ? (
            <Button
              className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
              disabled={isLoading || !newPassword || !confirmPassword || !resetToken}
              onClick={() => void handleResetPassword()}
            >
              {isLoading ? "Saving..." : "Change Password"}
            </Button>
          ) : null}
        </div>

        <button
          type="button"
          onClick={onBackToLogin}
          className="mt-6 flex items-center justify-center gap-1 text-sm text-muted-foreground hover:text-foreground mx-auto"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to login
        </button>
      </div>

      <p className="mt-8 text-center text-xs text-primary-foreground/60">
        Western Armenian Music Generator
      </p>
    </div>
  )
}
