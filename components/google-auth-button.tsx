"use client"

import { useEffect, useRef, useState } from "react"

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (options: {
            client_id: string
            callback: (response: { credential?: string }) => void
          }) => void
          renderButton: (
            element: HTMLElement,
            options: Record<string, string | number | boolean>,
          ) => void
        }
      }
    }
  }
}

interface GoogleAuthButtonProps {
  onCredential: (credential: string) => void | Promise<void>
  disabled?: boolean
  text?: "signin_with" | "signup_with" | "continue_with"
}

let googleScriptPromise: Promise<void> | null = null

function ensureGoogleScript(): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Google sign-in is only available in the browser"))
  }

  if (window.google?.accounts?.id) {
    return Promise.resolve()
  }

  if (!googleScriptPromise) {
    googleScriptPromise = new Promise((resolve, reject) => {
      const existingScript = document.querySelector<HTMLScriptElement>(
        'script[src="https://accounts.google.com/gsi/client"]',
      )

      if (existingScript) {
        existingScript.addEventListener("load", () => resolve(), { once: true })
        existingScript.addEventListener(
          "error",
          () => reject(new Error("Failed to load Google sign-in")),
          { once: true },
        )
        return
      }

      const script = document.createElement("script")
      script.src = "https://accounts.google.com/gsi/client"
      script.async = true
      script.defer = true
      script.onload = () => resolve()
      script.onerror = () => reject(new Error("Failed to load Google sign-in"))
      document.head.appendChild(script)
    })
  }

  return googleScriptPromise
}

export function GoogleAuthButton({
  onCredential,
  disabled = false,
  text = "continue_with",
}: GoogleAuthButtonProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const [error, setError] = useState("")

  useEffect(() => {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID?.trim()
    if (!clientId || !containerRef.current) {
      return
    }

    let cancelled = false

    void ensureGoogleScript()
      .then(() => {
        if (cancelled || !containerRef.current || !window.google?.accounts?.id) {
          return
        }

        containerRef.current.innerHTML = ""
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: async (response) => {
            if (!response.credential) {
              setError("Google sign-in did not return a credential")
              return
            }

            try {
              setError("")
              await onCredential(response.credential)
            } catch (credentialError) {
              setError(
                credentialError instanceof Error
                  ? credentialError.message
                  : "Google sign-in failed",
              )
            }
          },
        })

        window.google.accounts.id.renderButton(containerRef.current, {
          theme: "outline",
          size: "large",
          width: 360,
          text,
          shape: "pill",
        })
      })
      .catch((scriptError) => {
        if (!cancelled) {
          setError(scriptError instanceof Error ? scriptError.message : "Google sign-in failed")
        }
      })

    return () => {
      cancelled = true
    }
  }, [onCredential, text])

  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID?.trim()
  if (!clientId) {
    return null
  }

  return (
    <div className={disabled ? "pointer-events-none opacity-60" : ""}>
      <div ref={containerRef} className="flex justify-center" />
      {error ? (
        <p className="mt-2 text-center text-xs text-destructive">{error}</p>
      ) : null}
    </div>
  )
}
