/**
 * Runtime playback URLs for audio/images. MongoDB keeps canonical MinIO URLs;
 * the app proxies restricted hosts through /api/v1/media/proxy for browser playback.
 */

const PROXY_PREFERRED_HOSTS = [
  "storage.beesync.co",
  "musicfile.removeai.ai",
  "tempfile.aiquickdraw.com",
] as const

const configuredBackendBaseUrl = process.env.NEXT_PUBLIC_BACKEND_URL?.trim() || ""

export function getBackendBaseUrl(): string {
  return configuredBackendBaseUrl.replace(/\/+$/, "")
}

export function shouldProxyMediaUrl(url: string): boolean {
  if (!url?.trim()) {
    return false
  }
  if (url.includes("/api/v1/media/proxy?url=")) {
    return false
  }
  try {
    const host = new URL(url.trim()).hostname.toLowerCase()
    return PROXY_PREFERRED_HOSTS.some(
      (allowed) => host === allowed || host.endsWith(`.${allowed}`),
    )
  } catch {
    return false
  }
}

export function toMediaProxyUrl(rawUrl: string, backendBaseUrl = getBackendBaseUrl()): string {
  const proxyBase = backendBaseUrl || ""
  return `${proxyBase}/api/v1/media/proxy?url=${encodeURIComponent(rawUrl.trim())}`
}

export function unwrapMediaProxyUrl(url: string): string | undefined {
  try {
    const parsed = new URL(url, typeof window !== "undefined" ? window.location.origin : "http://localhost")
    if (!parsed.pathname.endsWith("/api/v1/media/proxy")) {
      return undefined
    }
    const nested = parsed.searchParams.get("url")
    return nested?.trim() || undefined
  } catch {
    return undefined
  }
}

export type PlayableTrackUrls = {
  localAudioUrl?: string | null
  audioUrl?: string | null
  streamAudioUrl?: string | null
  providerAudioUrl?: string | null
}

export function buildPlayableAudioUrl(
  track: PlayableTrackUrls,
  backendBaseUrl = getBackendBaseUrl(),
): string | null {
  const url =
    track.localAudioUrl?.trim() ||
    track.audioUrl?.trim() ||
    track.streamAudioUrl?.trim() ||
    track.providerAudioUrl?.trim() ||
    null

  if (!url) {
    return null
  }

  if (!/^https?:\/\//i.test(url)) {
    return url
  }

  if (url.includes("/api/v1/media/proxy?url=")) {
    return url
  }

  if (shouldProxyMediaUrl(url)) {
    return toMediaProxyUrl(url, backendBaseUrl)
  }

  return url
}

export function buildPlaybackCandidates(
  track: PlayableTrackUrls,
  backendBaseUrl = getBackendBaseUrl(),
): string[] {
  const primary = buildPlayableAudioUrl(track, backendBaseUrl)
  const rawSources = [
    track.localAudioUrl,
    track.audioUrl,
    track.streamAudioUrl,
    track.providerAudioUrl,
  ].filter((value): value is string => Boolean(value?.trim()))

  const expanded: string[] = []
  if (primary) {
    expanded.push(primary)
  }

  for (const raw of rawSources) {
    const trimmed = raw.trim()
    if (!trimmed || expanded.includes(trimmed)) {
      continue
    }
    if (shouldProxyMediaUrl(trimmed)) {
      const proxied = toMediaProxyUrl(trimmed, backendBaseUrl)
      if (!expanded.includes(proxied)) {
        expanded.push(proxied)
      }
    } else if (!expanded.includes(trimmed)) {
      expanded.push(trimmed)
    }
  }

  return expanded
}
