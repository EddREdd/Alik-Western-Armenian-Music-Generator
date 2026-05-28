# Balians Music Generation Backend

This repository contains:
- a Next.js frontend at the repo root
- a Spring Boot backend in [backend](/E:/Alik-Western-Armenian-Music-Generator/backend)

## What The Service Does

The backend manages async music generation with Suno:
- creates internal generation jobs
- submits jobs to Suno and stores `providerTaskId`
- processes Suno callbacks
- polls `record-info` for reconciliation/recovery
- stores generated tracks
- runs daily schedule-driven generation from prompt templates
- exposes admin/ops endpoints for support and recovery

## Architecture Summary

- Framework: Spring Boot
- Database: MongoDB
- Build: Maven
- Style: modular monolith
- Provider integration: async with callback + polling fallback

## Module Overview

- `common`: shared config, errors, responses, enums
- `generation`: jobs, submission, job APIs
- `provider`: Suno HTTP client and provider DTOs
- `callback`: webhook DTOs, audit persistence, idempotent processing
- `polling`: reconciliation, poll attempts, scheduler
- `prompttemplate`: reusable generation templates
- `schedule`: daily schedule definitions, execution, run history
- `admin`: ops/admin inspection and recovery endpoints
- `health`: lightweight app readiness endpoint

## Required Environment Variables

Core:
- `APP_NAME`
- `SERVER_PORT`
- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATA_MONGODB_URI`
- `SPRING_DATA_MONGODB_DATABASE`

Provider:
- `PROVIDER_BASE_URL`
- `PROVIDER_API_KEY`
- `PROVIDER_CALLBACK_BASE_URL`

Feature toggles:
- `FEATURE_PROVIDER_SUBMISSION_ENABLED`
- `FEATURE_CALLBACK_PROCESSING_ENABLED`
- `FEATURE_ADMIN_ENDPOINTS_ENABLED`
- `POLLING_ENABLED`
- `SCHEDULE_EXECUTION_ENABLED`

Operational:
- `POLLING_INTERVAL_MS`
- `POLLING_BATCH_SIZE`
- `POLLING_BASE_DELAY_SECONDS`
- `POLLING_MAX_DELAY_SECONDS`
- `SCHEDULE_EXECUTION_INTERVAL_MS`
- `SCHEDULE_EXECUTION_BATCH_SIZE`
- `OPS_STUCK_THRESHOLD_MINUTES`
- `OPS_RAW_PAYLOAD_PREVIEW_LENGTH`
- `LOG_LEVEL_ROOT`

Example values are in [backend/.env.example](/E:/Alik-Western-Armenian-Music-Generator/backend/.env.example).

## Profiles

- `local`: local development defaults, provider submission disabled by default
- `dev`: dev-like defaults, provider submission disabled by default
- `preprod`: production-like, env-driven
- `prod`: production-like, env-driven, quieter logging

## Run Locally

From repo root:

```bash
mvn clean install
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Useful URLs:
- API docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- Health: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)
- Actuator health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

## Main Verification Flow

1. Create a prompt template:

```bash
curl -X POST http://localhost:8080/api/v1/prompt-templates \
  -H "Content-Type: application/json" \
  -d "{\"projectId\":\"project-1\",\"name\":\"Daily template\",\"customMode\":true,\"instrumental\":false,\"model\":\"V4\",\"promptTemplate\":\"Write a Western Armenian song\",\"styleTemplate\":\"folk-pop\",\"titleTemplate\":\"Daily Armenian Song\",\"active\":true,\"isDefault\":false}"
```

2. Create a generation job:

```bash
curl -X POST http://localhost:8080/api/v1/generation-jobs \
  -H "Content-Type: application/json" \
  -d "{\"projectId\":\"project-1\",\"templateId\":\"template-id\",\"sourceType\":\"MANUAL\",\"promptFinal\":\"Write a Western Armenian song\",\"styleFinal\":\"folk-pop\",\"titleFinal\":\"My Song\",\"customMode\":true,\"instrumental\":false,\"model\":\"V4\"}"
```

3. Submit the job to Suno:

```bash
curl -X POST http://localhost:8080/api/v1/generation-jobs/{jobId}/submit
```

4. Verify callback or reconcile manually:

```bash
curl -X POST http://localhost:8080/api/v1/admin/generation-jobs/{jobId}/reconcile-now
```

5. Inspect tracks/callbacks/poll attempts:

```bash
curl http://localhost:8080/api/v1/admin/generation-jobs/{jobId}/tracks
curl http://localhost:8080/api/v1/admin/generation-jobs/{jobId}/callback-events
curl http://localhost:8080/api/v1/admin/generation-jobs/{jobId}/poll-attempts
```

6. Create a schedule and trigger it:

```bash
curl -X POST http://localhost:8080/api/v1/schedules \
  -H "Content-Type: application/json" \
  -d "{\"projectId\":\"project-1\",\"templateId\":\"template-id\",\"name\":\"Daily 9AM UTC\",\"timezone\":\"UTC\",\"cronExpression\":\"0 0 9 * * *\",\"enabled\":true,\"autoSubmitToProvider\":true,\"creditsMinThreshold\":1}"

curl -X POST http://localhost:8080/api/v1/admin/schedules/{scheduleId}/run-now
```

## Callback Testing Notes

- Suno callback endpoint: `POST /api/v1/integrations/suno/callback`
- raw callback payloads are stored in `callback_events`
- duplicate identical callbacks are detected using `providerTaskId + callbackType + payloadHash`

## Polling Recovery Notes

- active jobs with `providerTaskId` and due `nextPollAt` are polled automatically
- successful record-info reconciliation can repair missing tracks
- manual reconcile is available through admin endpoints

## Schedule Execution Notes

- schedules store timezone-aware `nextRunAt`
- duplicate daily runs are blocked using `scheduleDefinitionId + runDate`
- insufficient credits produce `SKIPPED` schedule runs rather than hard failures

## Common Failure Cases

- `Provider submission is disabled by configuration`
  - check `FEATURE_PROVIDER_SUBMISSION_ENABLED`
- `Provider authentication failed`
  - verify `PROVIDER_API_KEY`
- callbacks not updating jobs
  - inspect `/api/v1/admin/generation-jobs/{id}/callback-events`
- stuck jobs
  - inspect `/api/v1/admin/generation-jobs/stuck`
- schedule skips
  - inspect `/api/v1/admin/schedules/{id}/runs`

## Deployment Notes

### Production architecture (Traefik / Dokploy)

The browser must call the backend through the **frontend origin** using relative URLs such as `/api/v1/auth/login`. Next.js rewrites proxy `/api/:path*` to the backend container.

| Variable | Production value | Purpose |
|----------|------------------|---------|
| `NEXT_PUBLIC_BACKEND_URL` | *(empty)* | Browser uses same-origin `/api/...` (no cross-origin CORS) |
| `INTERNAL_BACKEND_URL` | `http://backend:8080` | Next.js server-side proxy target inside Docker |
| `CORS_ALLOWED_ORIGINS` | *(empty)* | Not needed for same-origin production traffic |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://*.traefik.me,https://*.traefik.me` | Only when the browser intentionally calls the backend public URL |

Example production env (see [.env.docker.example](/E:/Alik-Western-Armenian-Music-Generator/.env.docker.example)):

```env
NEXT_PUBLIC_BACKEND_URL=
INTERNAL_BACKEND_URL=http://backend:8080
CORS_ALLOWED_ORIGINS=
CORS_ALLOWED_ORIGIN_PATTERNS=http://*.traefik.me,https://*.traefik.me
```

After deploy, verify in the browser network tab:

- `GET /api/v1/auth/me` → frontend host (not backend Traefik host)
- `GET /api/v1/generation-jobs?page=0&size=50` → frontend host
- `POST /api/v1/auth/login` → frontend host, no CORS failure

### Local development (separate ports)

When the frontend runs on port 3000 and the browser calls the backend on port 8080 directly:

```env
NEXT_PUBLIC_BACKEND_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Use the local Compose overlay (do **not** use these values as production defaults):

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

### Build commands

Backend:

```bash
cd backend
mvn clean package
```

Frontend:

```bash
npm run build
```

Docker (production-like, empty public backend URL):

```bash
export NEXT_PUBLIC_BACKEND_URL=
export INTERNAL_BACKEND_URL=http://backend:8080
docker compose up --build
```

- `docker-compose.yml` is deployment-oriented and avoids binding fixed host ports
- `docker-compose.local.yml` is **only** for local/separate-port development
- Copy [.env.docker.example](/E:/Alik-Western-Armenian-Music-Generator/.env.docker.example) to `.env` and fill in secrets via your deployment platform
- Never commit real API keys, database passwords, or provider secrets (see [backend/.env.example](/E:/Alik-Western-Armenian-Music-Generator/backend/.env.example)); rotate any credentials that were previously committed

## MVP Out Of Scope

- asset mirroring/storage sync
- social publishing
- approval workflow
- distributed locking / clustered execution control
- advanced RBAC / IAM
- queue/broker infrastructure
