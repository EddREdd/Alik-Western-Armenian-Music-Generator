"use client"

import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/ui/spinner"
import { cn } from "@/lib/utils"

export interface ConfirmDialogProps {
  open: boolean
  title: string
  message: string
  confirmText: string
  cancelText: string
  danger?: boolean
  loading?: boolean
  loadingLabel?: string
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmText,
  cancelText,
  danger = false,
  loading = false,
  loadingLabel,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <AlertDialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen && !loading) {
          onCancel()
        }
      }}
    >
      <AlertDialogContent className="border-border bg-card sm:max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-foreground">{title}</AlertDialogTitle>
          <AlertDialogDescription className="text-muted-foreground">
            {message}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="gap-2 sm:gap-2">
          <AlertDialogCancel disabled={loading} onClick={onCancel}>
            {cancelText}
          </AlertDialogCancel>
          <Button
            type="button"
            variant={danger ? "destructive" : "default"}
            disabled={loading}
            className={cn(!danger && "bg-primary text-primary-foreground hover:bg-primary/90")}
            onClick={(event) => {
              event.preventDefault()
              onConfirm()
            }}
          >
            {loading ? <Spinner className="mr-2" /> : null}
            {loading && loadingLabel ? loadingLabel : confirmText}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
