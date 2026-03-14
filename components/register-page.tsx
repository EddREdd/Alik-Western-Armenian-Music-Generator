"use client"

import { useState } from "react"
import Image from "next/image"
import { Mail, Lock, Eye, EyeOff, Ticket, CheckCircle2, ArrowLeft } from "lucide-react"
import { Button } from "@/components/ui/button"
import { GoogleAuthButton } from "@/components/google-auth-button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  register,
  verifyRegistration,
  loginWithGoogle,
  type AuthSession,
  type OtpChallenge,
} from "@/lib/auth-api"

interface RegisterPageProps {
  onRegisterComplete: (session: AuthSession) => void
  onSwitchToLogin: () => void
}

export function RegisterPage({ onRegisterComplete, onSwitchToLogin }: RegisterPageProps) {
  const [step, setStep] = useState<"form" | "verify">("form")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [inviteCode, setInviteCode] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")
  const [verificationCode, setVerificationCode] = useState("")
  const [otpChallenge, setOtpChallenge] = useState<OtpChallenge | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")

    if (password !== confirmPassword) {
      setError("Passwords do not match")
      return
    }

    if (password.length < 8) {
      setError("Password must be at least 8 characters")
      return
    }

    setIsLoading(true)

    try {
      const challenge = await register({
        email,
        password,
        inviteCode,
      })
      setOtpChallenge(challenge)
      setStep("verify")
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Unable to register")
    } finally {
      setIsLoading(false)
    }
  }

  const handleResendEmail = async () => {
    setError("")
    setIsLoading(true)
    try {
      const challenge = await register({
        email,
        password,
        inviteCode,
      })
      setOtpChallenge(challenge)
      setStep("verify")
    } catch (resendError) {
      setError(resendError instanceof Error ? resendError.message : "Unable to resend code")
    } finally {
      setIsLoading(false)
    }
  }

  // Email Verification Step
  if (step === "verify") {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-primary px-4 py-8">
        {/* Logo */}
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

        {/* Card */}
        <div className="w-full max-w-md rounded-2xl bg-card p-8 shadow-xl text-center">
          <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-secondary/10">
            <CheckCircle2 className="h-8 w-8 text-secondary" />
          </div>

          <h1 className="text-2xl font-bold text-foreground">Check Your Email</h1>
          <p className="mt-3 text-sm text-muted-foreground">
            {"We've sent a verification code to"}
          </p>
          <p className="mt-1 font-medium text-foreground">{email}</p>

          <p className="mt-6 text-sm text-muted-foreground">
            Enter the 6-digit OTP from your email to activate your account and start creating music.
          </p>

          {otpChallenge?.devOtpPreview ? (
            <p className="mt-3 rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">
              Local/dev OTP preview: <span className="font-semibold text-foreground">{otpChallenge.devOtpPreview}</span>
            </p>
          ) : null}

          <form
            onSubmit={async (e) => {
              e.preventDefault()
              setError("")
              setIsLoading(true)
              try {
                const session = await verifyRegistration(email, verificationCode)
                onRegisterComplete(session)
              } catch (verifyError) {
                setError(
                  verifyError instanceof Error ? verifyError.message : "Unable to verify account",
                )
              } finally {
                setIsLoading(false)
              }
            }}
            className="mt-6 space-y-3"
          >
            <Input
              type="text"
              inputMode="numeric"
              placeholder="Enter 6-digit code"
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
              className="text-center tracking-[0.4em] text-lg"
              required
            />

            {error ? (
              <div className="rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
                {error}
              </div>
            ) : null}

            <Button
              className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
              disabled={isLoading || verificationCode.length !== 6}
            >
              {isLoading ? "Verifying..." : "Verify and Continue"}
            </Button>
          </form>

          <div className="mt-4 space-y-3">
            <Button
              onClick={handleResendEmail}
              variant="outline"
              className="w-full border-border"
              disabled={isLoading}
            >
              {isLoading ? "Sending..." : "Resend Code"}
            </Button>
          </div>

          <button
            type="button"
            onClick={() => {
              setStep("form")
              setVerificationCode("")
              setError("")
            }}
            className="mt-6 flex items-center justify-center gap-1 text-sm text-muted-foreground hover:text-foreground mx-auto"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to registration
          </button>
        </div>

        {/* Footer */}
        <p className="mt-8 text-center text-xs text-primary-foreground/60">
          Western Armenian Music Generator
        </p>
      </div>
    )
  }

  // Registration Form Step
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-primary px-4 py-8">
      {/* Logo */}
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

      {/* Card */}
      <div className="w-full max-w-md rounded-2xl bg-card p-8 shadow-xl">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-foreground">Create Account</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Join Alik and start creating Armenian music
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
              Email
            </Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="email"
                type="email"
                placeholder="Enter your email"
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
              Password
            </Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="Create a password"
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
              Must be at least 8 characters
            </p>
          </div>

          {/* Confirm Password */}
          <div className="space-y-2">
            <Label htmlFor="confirmPassword" className="text-sm font-medium text-foreground">
              Confirm Password
            </Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? "text" : "password"}
                placeholder="Confirm your password"
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
              Invite Code
            </Label>
            <div className="relative">
              <Ticket className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="inviteCode"
                type="text"
                placeholder="Enter your invite code"
                value={inviteCode}
                onChange={(e) => setInviteCode(e.target.value)}
                className="pl-10"
                required
              />
            </div>
            <p className="text-xs text-muted-foreground">
              You need an invite code to register
            </p>
          </div>

          {/* Submit Button */}
          <Button
            type="submit"
            className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
            disabled={isLoading}
          >
            {isLoading ? "Creating account..." : "Create Account"}
          </Button>
        </form>

        {/* Divider */}
        <div className="my-6 flex items-center gap-4">
          <div className="h-px flex-1 bg-border" />
          <span className="text-xs text-muted-foreground">or continue with</span>
          <div className="h-px flex-1 bg-border" />
        </div>

        {/* Google Button */}
        <GoogleAuthButton
          text="signup_with"
          disabled={isLoading}
          onCredential={async (credential) => {
            if (!inviteCode.trim()) {
              setError("Invite code is required for Google sign up")
              return
            }

            setError("")
            setIsLoading(true)
            try {
              const session = await loginWithGoogle(credential, inviteCode)
              onRegisterComplete(session)
            } catch (googleError) {
              setError(
                googleError instanceof Error ? googleError.message : "Google sign-up failed",
              )
            } finally {
              setIsLoading(false)
            }
          }}
        />

        {/* Switch to Login */}
        <p className="mt-6 text-center text-sm text-muted-foreground">
          Already have an account?{" "}
          <button
            type="button"
            onClick={onSwitchToLogin}
            className="font-medium text-secondary hover:text-secondary/80 hover:underline"
          >
            Sign in
          </button>
        </p>
      </div>

      {/* Footer */}
      <p className="mt-8 text-center text-xs text-primary-foreground/60">
        Western Armenian Music Generator
      </p>
    </div>
  )
}
