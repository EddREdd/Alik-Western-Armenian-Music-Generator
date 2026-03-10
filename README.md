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

- build the backend jar:

```bash
cd backend
mvn clean package
```

- Docker build:

```bash
cd backend
docker build -t musicgen-backend:latest .
```

- run with env vars only; do not hardcode secrets

## MVP Out Of Scope

- asset mirroring/storage sync
- social publishing
- approval workflow
- distributed locking / clustered execution control
- advanced RBAC / IAM
- queue/broker infrastructure
