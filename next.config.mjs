const internalBackendUrl =
  process.env.INTERNAL_BACKEND_URL?.replace(/\/+$/, "") ||
  "http://alik-app-mw9ydy-0d3794-95-140-192-45.traefik.me"

/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    unoptimized: true,
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${internalBackendUrl}/api/:path*`,
      },
      {
        source: "/actuator/:path*",
        destination: `${internalBackendUrl}/actuator/:path*`,
      },
      {
        source: "/api-docs",
        destination: `${internalBackendUrl}/api-docs`,
      },
      {
        source: "/swagger-ui/:path*",
        destination: `${internalBackendUrl}/swagger-ui/:path*`,
      },
      {
        source: "/swagger-ui.html",
        destination: `${internalBackendUrl}/swagger-ui.html`,
      },
    ]
  },
}

export default nextConfig
