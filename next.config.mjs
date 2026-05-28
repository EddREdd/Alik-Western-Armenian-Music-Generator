const internalBackendUrl =
  process.env.INTERNAL_BACKEND_URL?.replace(/\/+$/, "") ||
  "http://localhost:8080"

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
