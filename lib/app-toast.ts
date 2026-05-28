import { toast } from "@/hooks/use-toast"

type ToastInput = {
  title: string
  description?: string
}

export function toastSuccess({ title, description }: ToastInput) {
  toast({ title, description })
}

export function toastError({ title, description }: ToastInput) {
  toast({
    title,
    description,
    variant: "destructive",
  })
}

export function toastInfo({ title, description }: ToastInput) {
  toast({ title, description })
}

export function toastWarning({ title, description }: ToastInput) {
  toast({
    title,
    description,
    variant: "destructive",
  })
}
