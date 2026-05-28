"use client"

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Spinner } from "@/components/ui/spinner"
import { useEffect, useState } from "react"

export interface PromptDialogProps {
  open: boolean
  title: string
  message?: string
  label?: string
  defaultValue?: string
  inputType?: "text" | "number"
  confirmText: string
  cancelText: string
  loading?: boolean
  onConfirm: (value: string) => void
  onCancel: () => void
}

export function PromptDialog({
  open,
  title,
  message,
  label,
  defaultValue = "",
  inputType = "text",
  confirmText,
  cancelText,
  loading = false,
  onConfirm,
  onCancel,
}: PromptDialogProps) {
  const [value, setValue] = useState(defaultValue)

  useEffect(() => {
    if (open) {
      setValue(defaultValue)
    }
  }, [open, defaultValue])

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen && !loading) {
          onCancel()
        }
      }}
    >
      <DialogContent className="border-border bg-card sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-foreground">{title}</DialogTitle>
          {message ? (
            <DialogDescription className="text-muted-foreground">{message}</DialogDescription>
          ) : null}
        </DialogHeader>
        <div className="flex flex-col gap-2">
          {label ? <Label htmlFor="prompt-dialog-input">{label}</Label> : null}
          <Input
            id="prompt-dialog-input"
            type={inputType}
            value={value}
            onChange={(event) => setValue(event.target.value)}
            disabled={loading}
            min={inputType === "number" ? 1 : undefined}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !loading) {
                event.preventDefault()
                onConfirm(value)
              }
            }}
          />
        </div>
        <DialogFooter className="gap-2 sm:gap-2">
          <Button type="button" variant="outline" disabled={loading} onClick={onCancel}>
            {cancelText}
          </Button>
          <Button type="button" disabled={loading} onClick={() => onConfirm(value)}>
            {loading ? <Spinner className="mr-2" /> : null}
            {confirmText}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
