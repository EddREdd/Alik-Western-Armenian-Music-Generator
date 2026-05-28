# MinIO media storage (BeeSync)

Generated songs and cover images are stored in the external MinIO bucket `alik`:

| Asset | Object key prefix | Example public URL |
|-------|-------------------|--------------------|
| Audio | `audio/` | `http://storage.beesync.co:9000/alik/audio/<trackId>-<title>.mp3` |
| Images | `images/` | `http://storage.beesync.co:9000/alik/images/<trackId>-cover.jpg` |

MongoDB keeps the **canonical MinIO URL** in `audioUrl` / `localAudioUrl`. The original provider URL is kept in `providerAudioUrl` for audit.

## Backend environment

```env
MEDIA_STORAGE_TYPE=spaces
MINIO_ENDPOINT=http://storage.beesync.co:9000
MINIO_BUCKET=alik
MINIO_PUBLIC_BASE_URL=http://storage.beesync.co:9000/alik
MEDIA_STORAGE_SPACES_ENDPOINT=http://storage.beesync.co:9000
MEDIA_STORAGE_SPACES_BUCKET=alik
MEDIA_STORAGE_SPACES_PUBLIC_BASE_URL=http://storage.beesync.co:9000/alik
MEDIA_PROXY_ALLOWED_HOSTS=storage.beesync.co,musicfile.removeai.ai,tempfile.aiquickdraw.com
```

Optional startup probe (HEAD) for anonymous read:

```env
MEDIA_STORAGE_PUBLIC_PROBE_URL=http://storage.beesync.co:9000/alik/audio/your-sample.mp3
```

## A. Public-read bucket prefix (recommended for direct browser playback)

Install [MinIO Client (`mc`)](https://min.io/docs/minio/linux/reference/minio-mc.html), then:

```bash
mc alias set beesync http://storage.beesync.co:9000 beesyncadmin "<MINIO_PASSWORD>"
mc mb --ignore-existing beesync/alik
mc anonymous set download beesync/alik/audio
mc anonymous set download beesync/alik/images
mc anonymous get beesync/alik/audio
mc anonymous get beesync/alik/images
```

If every object in `alik` is safe to read publicly:

```bash
mc anonymous set download beesync/alik
```

**Do not** grant public write (`upload`, `public`, etc.). Only **download** (read) is required.

### Verify public read

```bash
curl -I http://storage.beesync.co:9000/alik/audio/your-file.mp3
```

Expected:

```
HTTP/1.1 200 OK
Content-Type: audio/mpeg
```

Open the same URL in a browser; the file should play or download.

## B. Backend media proxy (works with private bucket)

If the bucket stays private, the app proxies playback through the backend (no anonymous MinIO policy required):

```
GET /api/v1/media/proxy?url=<encoded-remote-url>
```

Allowed hosts: `storage.beesync.co`, `musicfile.removeai.ai`, `tempfile.aiquickdraw.com`.

The frontend automatically uses this proxy for those hosts (same-origin `/api/...` via Next.js rewrite).

### Verify proxy

```bash
curl -I "http://localhost:8080/api/v1/media/proxy?url=http%3A%2F%2Fstorage.beesync.co%3A9000%2Falik%2Faudio%2Fyour-file.mp3"
```

Expected: `HTTP/1.1 200` (or `206` with `Range`), `Content-Type: audio/mpeg`, `Accept-Ranges: bytes`.

## Upload content types

The backend sets S3 `Content-Type` on upload:

- MP3 → `audio/mpeg`
- JPEG → `image/jpeg`

This avoids player issues after public access is enabled.
