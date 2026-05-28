import type { Metadata, Viewport } from 'next'
import { LocaleHtmlLang } from '@/components/locale-html-lang'
import './globals.css'

export const metadata: Metadata = {
  title: 'Alik - Western Armenian Music Generator',
  description: 'Create original Western Armenian music with AI. Paste your lyrics, choose a style, and generate professional-quality songs in seconds.',
}

export const viewport: Viewport = {
  themeColor: '#004144',
  userScalable: false,
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className="font-sans antialiased">
        <LocaleHtmlLang />
        {children}
      </body>
    </html>
  )
}
