# DevTalks — Build Plan

> Semantic search engine for cloud-native conference talks. Built to learn Kotlin microservices, gRPC, Cloud Run, Pub/Sub, and Vertex AI on GCP.

## Project Overview

**Problem:** YouTube search is bad at technical specificity. Finding "talks comparing service mesh sidecar latency overhead" is nearly impossible across 50,000+ conference talks.

**Solution:** Ingest talks from KubeCon / Google Cloud Next / Spring I/O channels, transcribe them, embed semantically, and serve search via gRPC streaming.

**Stack:**
- Backend: Kotlin 1.9+ / Spring Boot 3.x
- APIs: gRPC (internal) + REST (public)
- Build: Gradle Kotlin DSL + Jib (no Dockerfiles)
- Cloud: GCP — Cloud Run, Pub/Sub, Firestore, Cloud Storage, Vertex AI Embeddings
- Transcripts: YouTube auto-generated captions (via `youtube-transcript-api`)
- Vector search: In-memory cosine similarity over Firestore-backed embeddings
- Frontend: React + TypeScript + Vite + Connect-Web
- IaC: Terraform
- CI/CD: Cloud Build → Artifact Registry → Cloud Run

**Out of scope (do not build):**
- Custom model training / fine-tuning
- User accounts beyond Firebase Auth
- Recommendations, playlists, social features
- Self-hosted vector DB
- GKE migration (Cloud Run only for v1)

---

## Project Context

Current state of the GCP environment. Treat these as ground truth — do not re-create.

**Identifiers:**
- Project ID: `devtalks-aryan-4787`
- Default region: `us-central1`
- Billing: linked, on Blaze (pay-as-you-go); budget alerts at $5 and $20 active
- Organization: None (personal Google account — intentional)
- Local dev machine: macOS, JDK 21 via SDKMAN, Node 20 via fnm, all CLIs installed

**Already provisioned in GCP:**
- Artifact Registry repo `devtalks` in `us-central1` (Docker format)
- Firestore database `(default)` in `us-central1`, Native mode, free tier
- GCS bucket `gs://devtalks-aryan-4787-tfstate` with versioning on (for Terraform remote state)

**APIs enabled:** `run`, `pubsub`, `firestore`, `storage`, `aiplatform`, `artifactregistry`, `cloudbuild`, `secretmanager`, `iam`, `iamcredentials`, `cloudscheduler`, `cloudresourcemanager`

**Authenticated locally:**
- `gcloud auth login` (user credentials)
- `gcloud auth application-default login` (ADC for Java SDKs)
- `gcloud auth configure-docker us-central1-docker.pkg.dev` (Docker push to Artifact Registry)
- `firebase login`

**Defaults set in gcloud config:** `run/region=us-central1`, `compute/region=us-central1`, `artifacts/location=us-central1`, `project=devtalks-aryan-4787`

When referencing project ID, region, or bucket names in code or Terraform, read them from environment variables / config — never hardcode.

---

## Repository Layout

```
devtalks/
├── PLAN.md                          # this file
├── README.md                        # public-facing
├── proto/                           # shared protobuf schemas
│   ├── buf.yaml
│   ├── buf.gen.yaml
│   └── devtalks/v1/
│       ├── talks.proto
│       ├── search.proto
│       ├── ingest.proto
│       └── events.proto
├── services/
│   ├── ingest-service/              # Kotlin, REST + Pub/Sub publisher
│   ├── transcribe-service/          # Kotlin, gRPC + Pub/Sub subscriber
│   ├── embed-service/               # Kotlin, gRPC + Pub/Sub subscriber
│   ├── search-service/              # Kotlin, gRPC server-streaming
│   └── api-gateway/                 # Kotlin, REST → gRPC translator
├── frontend/                        # React + Vite
├── infra/                           # Terraform
│   ├── main.tf
│   ├── pubsub.tf
│   ├── cloudrun.tf
│   ├── firestore.tf
│   └── storage.tf
└── .github/workflows/               # or cloudbuild.yaml per service
```

Each Kotlin service has the same internal structure:
```
services/<name>/
├── build.gradle.kts
├── src/main/kotlin/com/devtalks/<name>/
│   ├── Application.kt
│   ├── config/
│   ├── grpc/                        # gRPC service impls
│   ├── rest/                        # REST controllers (if any)
│   ├── domain/                      # business logic
│   └── infrastructure/              # GCP client wrappers
└── src/test/kotlin/...
```

---

## Conventions for Claude Code

When working in this repo, follow these rules:

1. **Never bypass the proto layer.** All service-to-service contracts live in `proto/`. Regenerate Kotlin stubs with `buf generate` after any `.proto` change. Don't define duplicate DTOs in service code — use the generated types.
2. **One service at a time.** Don't modify multiple services in a single change unless the task explicitly says "cross-service refactor."
3. **Local-first, then deploy.** Every change must run locally (Testcontainers + Pub/Sub emulator) before being deployed. Don't push to Cloud Run to test.
4. **Environment via Spring profiles.** `local`, `dev`, `prod`. Never hardcode project IDs, bucket names, or topics — read from `application-{profile}.yml`.
5. **Idempotency required.** Every Pub/Sub handler must tolerate duplicate delivery. Use the message ID as a dedup key in Firestore.
6. **Structured logging only.** Use `logstash-logback-encoder` for JSON logs. Include `traceId` in every log line. No `println`.
7. **Resource cleanup matters.** Every Cloud Run service must have `min-instances=0` outside of demos. Pub/Sub subscriptions get `--ack-deadline=600` for long-running work.
8. **Test commands before claiming done.** Each phase's "Verification" steps must pass before marking the phase complete.

---

## Phase 0 — Foundations (Day 1)

**Goal:** Empty repo wired up with auth, IaC, and one "hello world" service deployed to Cloud Run. No business logic yet. This phase exists so you discover GCP IAM / CORS / Cloud Build issues *before* there's any code to debug.

### Tasks

- [x] Create GCP project. Enable billing.
- [x] **Set budget alerts at $5 and $20** on the project. Cloud Billing → Budgets & alerts.
- [x] Enable APIs: `run`, `pubsub`, `firestore`, `storage`, `aiplatform`, `artifactregistry`, `cloudbuild`, `secretmanager`.
- [x] Create Artifact Registry repo `devtalks` in `us-central1`.
- [x] Initialize Firestore database in Native mode, `us-central1`.
- [x] Create GCS bucket for Terraform remote state with versioning enabled.
- [x] **Initialize git repo** with the layout above. Add `.gitignore` for Kotlin/Gradle/Terraform/Node. First commit: this PLAN.md plus an empty README.
- [x] Set up `buf` in `proto/`. Add a stub `talks.proto` with a single `HealthCheck` RPC.
- [x] Bootstrap `services/api-gateway/` with Spring Boot 3, Kotlin, Gradle. Add one REST endpoint `GET /healthz` returning `{"status":"ok"}` (served at `/api/healthz` — Google's `*.run.app` edge reserves `/healthz`).
- [x] Add Jib config to `build.gradle.kts`. Verify `./gradlew jib` pushes to Artifact Registry.
- [x] Write `infra/main.tf` with provider + remote state pointing at `gs://devtalks-aryan-4787-tfstate`. Write `cloudrun.tf` deploying `api-gateway`.
- [x] `terraform apply`. Verify the public URL responds.
- [x] Set up Firebase Auth in the same project. Anonymous + Google sign-in.
- [x] Bootstrap `frontend/` with Vite + React + TS. One page that calls `api-gateway`'s `/healthz`. Deploy to Firebase Hosting.
- [x] Set up Cloud Build trigger: push to `main` → build + deploy `api-gateway` via Jib + `terraform apply` (plus PR-check trigger, GitHub Ruleset on `main` requiring the PR check).

### Verification

```bash
# From repo root
curl https://api-gateway-XXX.run.app/healthz
# → {"status":"ok"}

# Frontend
open https://devtalks-XXX.web.app
# → page loads, shows "ok" status from the API
```

### Done when
- One green Cloud Build run.
- Healthz returns from Cloud Run.
- Frontend (on Firebase Hosting) successfully calls the API in production.
- Terraform state lives in GCS, not locally.

**Do not proceed to Phase 1 until all four are true.** This is the single highest-leverage rule in the plan.

---

## Phase 1 — Walking Skeleton (Days 2-5)

**Goal:** Manually seed 5 talks into Firestore. Frontend lists them. Click a talk → see its metadata and a YouTube embed. No transcription, no embeddings.

### Why this phase exists
You'll spend most of these days fighting Firestore security rules, CORS between Firebase Hosting and Cloud Run, IAM permissions for service-to-service calls, and React state management. Better to fight all of that before there's any ML to debug.

### Proto definitions

**`proto/devtalks/v1/talks.proto`:**
```protobuf
syntax = "proto3";
package devtalks.v1;

message Talk {
  string id = 1;                    // YouTube video ID
  string title = 2;
  string channel = 3;               // e.g. "KubeCon"
  string description = 4;
  int32 duration_seconds = 5;
  string published_at = 6;          // RFC3339
  string thumbnail_url = 7;
  TalkStatus status = 8;
}

enum TalkStatus {
  TALK_STATUS_UNSPECIFIED = 0;
  TALK_STATUS_INGESTED = 1;
  TALK_STATUS_TRANSCRIBED = 2;
  TALK_STATUS_EMBEDDED = 3;
  TALK_STATUS_FAILED = 4;
}

service TalkCatalog {
  rpc GetTalk(GetTalkRequest) returns (Talk);
  rpc ListTalks(ListTalksRequest) returns (ListTalksResponse);
}

message GetTalkRequest { string id = 1; }
message ListTalksRequest {
  int32 page_size = 1;
  string page_token = 2;
}
message ListTalksResponse {
  repeated Talk talks = 1;
  string next_page_token = 2;
}
```

### Tasks

- [ ] Implement `talks.proto`. Regenerate stubs.
- [ ] Add `TalkCatalog` gRPC service to `api-gateway` (it owns the Firestore read path for now — we'll split it out only if needed).
- [ ] Add REST endpoints in `api-gateway`: `GET /api/v1/talks`, `GET /api/v1/talks/{id}` — these call the internal gRPC `TalkCatalog`. (Yes, in-process gRPC for now. It exercises the pattern without inter-service complexity.)
- [ ] Add Firebase Auth middleware to `api-gateway`. Reject requests without a valid ID token.
- [ ] Write a one-off Kotlin script `services/api-gateway/scripts/SeedTalks.kt` that inserts 5 talks into Firestore (hardcoded data: pick 5 real KubeCon videos).
- [ ] Build the frontend list and detail pages. Use `@tanstack/react-query` for data fetching. Auth state via Firebase SDK.
- [ ] Detail page embeds the YouTube player via `<iframe>`.

### Firestore schema

Collection `talks`, document ID = YouTube video ID:
```
{
  id: string,
  title: string,
  channel: string,
  description: string,
  durationSeconds: number,
  publishedAt: timestamp,
  thumbnailUrl: string,
  status: "INGESTED" | "TRANSCRIBED" | "EMBEDDED" | "FAILED",
  createdAt: timestamp,
  updatedAt: timestamp
}
```

### Verification

```bash
# Seed
cd services/api-gateway && ./gradlew runSeedScript

# Local
./gradlew :api-gateway:bootRun
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/talks
# → list of 5 talks

# Deployed
# Frontend at https://devtalks-XXX.web.app shows 5 talk cards
# Clicking a card opens the detail page with the YouTube embed playing
```

### Done when
- Frontend (production) lists 5 talks with thumbnails and titles.
- Detail page plays the embedded video.
- Unauthenticated requests return 401.
- Cloud Build deploys both `api-gateway` and frontend on push.

---

## Phase 2 — Ingest Pipeline (Days 6-9)

**Goal:** A scheduled job pulls new videos from a YouTube channel and writes them to Firestore. No transcription yet — just metadata ingestion via Pub/Sub.

### Architecture for this phase

```
Cloud Scheduler (cron, hourly)
    ↓ HTTPS
ingest-service (Cloud Run)
    ↓ YouTube Data API v3
    ↓ writes Talk to Firestore (status=INGESTED)
    ↓ publishes to Pub/Sub topic "talks.ingested"
    ↓
(no subscribers yet — that's Phase 3)
```

### Proto additions

**`proto/devtalks/v1/events.proto`:**
```protobuf
syntax = "proto3";
package devtalks.v1;

message TalkIngestedEvent {
  string talk_id = 1;
  string ingested_at = 2;          // RFC3339
}

message TalkTranscribedEvent {
  string talk_id = 1;
  string transcript_gcs_uri = 2;
  string transcribed_at = 3;
}

message TalkEmbeddedEvent {
  string talk_id = 1;
  int32 chunk_count = 2;
  string embedded_at = 3;
}
```

Pub/Sub messages: encode as JSON for now (easier to inspect in the console). Move to binary protobuf encoding later if message volume justifies it.

### Tasks

- [ ] Bootstrap `services/ingest-service/`. Spring Boot 3 + Kotlin.
- [ ] Add YouTube Data API v3 client (`com.google.apis:google-api-services-youtube`).
- [ ] Store YouTube API key in Secret Manager. Inject via Workload Identity.
- [ ] Implement `IngestService`:
  - List videos from configured channel IDs published in last 7 days.
  - For each: check if exists in Firestore, skip if so.
  - Else: write `Talk` doc with status=INGESTED, publish `TalkIngestedEvent` to `talks.ingested` topic.
- [ ] Expose `POST /internal/run-ingest` endpoint. Protect with IAM (allow only Cloud Scheduler's service account).
- [ ] Terraform: create `talks.ingested` topic, `ingest-service` Cloud Run service, Cloud Scheduler job triggering it hourly.
- [ ] Pub/Sub emulator setup in `docker-compose.yml` for local dev.
- [ ] Configure channel IDs in `application-prod.yml`:
  - KubeCon: `UCvqRdlKsE5Q8mf8YXbdIJLw`
  - Google Cloud Tech: `UCJS9pqu9BzkAMNTmzNMNhvg`
  - Spring I/O: `UCLMPXsvSrhNPN3i9h-u8PYg` (verify these before using)

### Verification

```bash
# Trigger manually
gcloud run services proxy ingest-service --port 8080
curl -X POST http://localhost:8080/internal/run-ingest

# Check Firestore
gcloud firestore documents list --collection-ids=talks

# Check Pub/Sub
gcloud pubsub subscriptions create debug-sub --topic=talks.ingested
gcloud pubsub subscriptions pull debug-sub --auto-ack --limit=10
# → see TalkIngestedEvent messages
```

### Done when
- Scheduled run adds new talks to Firestore without duplicates.
- Every new talk emits a Pub/Sub message.
- Re-running ingest is idempotent (no duplicate writes).
- Frontend now shows real KubeCon talks.

---

## Phase 3 — Transcription (Days 10-12)

**Goal:** Pub/Sub triggers transcript fetching via the YouTube transcript API. Transcripts land in GCS as JSON with timestamped segments. Talk status flips to TRANSCRIBED.

### Tasks

- [ ] Bootstrap `services/transcribe-service/`. Spring Boot 3 + Kotlin.
- [ ] Add a YouTube transcript fetcher. Options in order of preference:
  - **Preferred:** Embed a Python sidecar in the container running `youtube-transcript-api` (mature, well-maintained library that returns timestamped segments). Call it from Kotlin via local HTTP or subprocess.
  - **Alternative:** Pure Kotlin/Java port — call YouTube's `timedtext` endpoint directly. Brittle but no extra runtime.
- [ ] Create GCS bucket `devtalks-transcripts-<project-id>` (Terraform).
- [ ] Implement Pub/Sub push subscription handler:
  - Receives `TalkIngestedEvent`.
  - Fetches the auto-generated transcript for `talkId` (English first; fall back to auto-translated if unavailable).
  - Normalizes into the canonical format below and writes to `gs://devtalks-transcripts-.../{talkId}/transcript.json`.
  - Updates Firestore: set status=TRANSCRIBED, store `transcriptUri`.
  - Publishes `TalkTranscribedEvent` to `talks.transcribed` topic.
- [ ] If transcript is unavailable for a talk (rare but happens — speaker disabled captions): set status=FAILED with reason=`NO_TRANSCRIPT`. Don't retry.
- [ ] Use `MessageId` as Firestore dedup key — write to `processed_messages/{messageId}` before doing work; skip if exists.
- [ ] Add retry policy: max 5 attempts, dead-letter to `talks.failed` topic. Network errors retry; `NO_TRANSCRIPT` does not.

### Canonical transcript format

Always normalize to this shape regardless of source. Phase 4 reads this contract.

```json
{
  "talkId": "abc123",
  "language": "en",
  "source": "youtube_auto",
  "fetchedAt": "2025-01-15T10:30:00Z",
  "segments": [
    { "startSeconds": 0.0, "endSeconds": 4.2, "text": "Welcome to KubeCon..." },
    { "startSeconds": 4.2, "endSeconds": 8.7, "text": "Today we're talking about..." }
  ]
}
```

### Trade-off note for Claude Code

A small percentage of talks won't have captions available. **Do not** add a fallback to anything more expensive — mark those FAILED and move on. The corpus is large enough that losing 5% is acceptable. Do not spend more than half a day on edge cases around caption availability.

### Verification

```bash
# Trigger end-to-end
curl -X POST .../internal/run-ingest

# Wait ~2 min (transcript fetch is fast — seconds per talk)
gsutil ls gs://devtalks-transcripts-XXX/
# → see {talkId}/transcript.json files

# Inspect one
gsutil cat gs://devtalks-transcripts-XXX/<talkId>/transcript.json | jq '.segments[0]'
# → { "startSeconds": 0.0, "endSeconds": 4.2, "text": "..." }

# Check Firestore
# → talks should have status=TRANSCRIBED

# Frontend
# → talk detail page now shows a "Transcript available" badge
```

### Done when
- A talk ingested via Phase 2 reaches TRANSCRIBED status within 2 minutes.
- Transcript JSON has segment-level timestamps in the canonical format.
- Re-delivery of the same Pub/Sub message does not re-fetch (idempotent).
- Talks without captions are marked FAILED with `NO_TRANSCRIPT`, not retried.
- Failures dead-letter; no infinite retry loops.

---

## Phase 4 — Embeddings + Vector Index (Days 13-16)

**Goal:** Transcripts get chunked and embedded via Vertex AI. Vectors are stored in Firestore and served from an in-memory index inside `search-service`. This is the ML core of the project.

### Why in-memory

For up to ~10,000 chunks (≈1,000 talks), 768-dim float32 vectors fit comfortably in <50 MB of RAM. Brute-force cosine similarity at this scale runs in <100ms per query — well under the latency budget. No standing infrastructure cost, no idle billing. The `SEARCH_BACKEND` env var keeps the door open to swap in a hosted vector index later if the corpus grows past that point.

### Tasks

- [ ] Bootstrap `services/embed-service/`. Add Vertex AI Java SDK.
- [ ] Subscribe to `talks.transcribed`.
- [ ] On message:
  - Download transcript JSON from GCS.
  - Chunk by ~500 words with 50-word overlap. Preserve start/end timestamps per chunk (use the earliest `startSeconds` and latest `endSeconds` across the segments that compose the chunk).
  - Batch call `text-embedding-004` (up to 5 per request). 768-dim output.
  - For each chunk, write to Firestore subcollection `talks/{talkId}/chunks/{chunkIndex}`:
    ```json
    {
      "chunkIndex": 0,
      "text": "...",
      "startSeconds": 12.4,
      "endSeconds": 47.8,
      "embedding": [0.012, -0.043, ...]   // 768 floats
    }
    ```
  - Update Talk status=EMBEDDED.
  - Publish `TalkEmbeddedEvent` with `chunkCount`.
- [ ] Handle Vertex AI rate limits with exponential backoff. Default quota is 600 RPM — embed in batches.
- [ ] Cost note: `text-embedding-004` is ~$0.0001 per 1K input chars. 500 talks × ~10K words × ~5 chars/word = ~$2.50 one-time. Acceptable.

### Firestore data shape rationale

Storing 768-dim vectors inline in Firestore is fine for our scale (~3 KB per chunk × ~10 chunks per talk × 1000 talks ≈ 30 MB total, well under the 1 GB free tier). Read-heavy patterns (loading the full index at search-service startup) cost reads, not writes — and we load once per cold start, not per query.

### Verification

```bash
# After a talk reaches EMBEDDED status:
gcloud firestore documents list --collection-ids=chunks \
  --filter="path:talks/<talkId>/chunks"
# → returns N chunk documents

# Inspect one
gcloud firestore documents describe \
  --resource-name="projects/<proj>/databases/(default)/documents/talks/<talkId>/chunks/0"
# → see embedding array (768 floats) + text + timestamps
```

### Done when
- A talk flows ingested → transcribed → embedded automatically.
- Firestore `chunks` subcollections are populated for all EMBEDDED talks.
- Re-embedding the same talk is idempotent (overwrites by chunkIndex, no duplicates).
- Vertex AI embedding spend stays under $5 for the first 500 talks.

---

## Phase 5 — Semantic Search (Days 17-19)

**Goal:** End-to-end search. User types a query → results appear ranked by semantic similarity → clicking a result deep-links into the YouTube video at the matching timestamp.

### Proto definitions

**`proto/devtalks/v1/search.proto`:**
```protobuf
syntax = "proto3";
package devtalks.v1;

import "devtalks/v1/talks.proto";

service SearchService {
  // Server-streaming: first result lands fast, refinement follows
  rpc Search(SearchRequest) returns (stream SearchResult);
}

message SearchRequest {
  string query = 1;
  int32 max_results = 2;            // default 20
  string channel_filter = 3;        // optional
}

message SearchResult {
  Talk talk = 1;
  string matching_chunk_text = 2;
  int32 timestamp_seconds = 3;      // jump-into point
  float relevance_score = 4;
  int32 rank = 5;
}
```

### Tasks

- [ ] Bootstrap `services/search-service/`. gRPC server-only (no REST).
- [ ] Implement an in-memory `VectorIndex` component:
  - On startup, stream all `chunks` documents from Firestore (collection group query: `collectionGroup("chunks")`).
  - Hold them in a `List<ChunkEntry>` where each entry has `talkId`, `chunkIndex`, `embedding: FloatArray`, `text`, `startSeconds`.
  - Expose `search(queryEmbedding: FloatArray, topK: Int): List<ScoredChunk>` using brute-force cosine similarity. For 10K chunks × 768 dims this is ~30ms with a tight loop.
  - Refresh trigger: subscribe to `talks.embedded` topic. On new event, fetch only the new chunks and append to the in-memory list. No full reload.
- [ ] Implement `SearchService.Search` as server-streaming:
  1. Embed query via Vertex AI (one call, ~150ms).
  2. Run `VectorIndex.search(queryEmbedding, topK=50)`.
  3. For each top-K hit, fetch the parent Talk metadata from Firestore (batch by talkId — most chunks share talks, so dedup first).
  4. Stream results back in rank order. Yield first result immediately, then continue.
- [ ] Add `SearchService` client to `api-gateway`. Expose REST endpoint `GET /api/v1/search?q=...` that consumes the gRPC stream and forwards via Server-Sent Events.
- [ ] Frontend: search box with debounced input. Results render progressively as SSE events arrive. Each result links to `https://youtube.com/watch?v={talkId}&t={timestamp}s`.
- [ ] Add a "Skip to this moment" button on the talk detail page using the timestamp.
- [ ] Configure Cloud Run for `search-service`: `min-instances=1` (keep the index warm — first-request cold-start with index load is ~5s otherwise), `memory=1Gi`, `cpu=1`. This is the one service that benefits from a warm instance.
- [ ] Add `SEARCH_BACKEND` env var with values `in_memory` (default) and `vector_search` (stub for future use). Keep the interface clean for a possible swap later.

### Why streaming here

Server-streaming gRPC is genuinely justified: the embedding call is ~150ms, in-memory similarity is ~30ms, but the Firestore Talk-metadata fetches for the deduped result set take ~500ms in serial. By streaming, the user sees the top-3 in ~250ms while the rest land over the next half-second. This is the "wow" demo moment.

### Verification

```bash
# Local end-to-end
curl 'http://localhost:8080/api/v1/search?q=kubernetes+networking'
# → SSE stream of JSON results, first within 500ms

# Frontend
# Type "service mesh sidecar performance" → 5+ results appear progressively
# Click result → YouTube opens at the right timestamp
```

### Done when
- Semantic search returns relevant results for at least 5 test queries.
- First result appears within 1 second.
- Deep-link timestamps are accurate (within ~5 seconds of the actual moment).
- Result quality is noticeably better than YouTube keyword search for technical queries.

---

## Phase 6 — Polish & Observability (Days 20-21)

**Goal:** Make it presentable. A reviewer / interviewer / future-you can clone the repo, read the README, and understand the system in 10 minutes.

### Tasks

- [ ] **README.md** with:
  - Architecture diagram (export the one from chat, or draw with Excalidraw).
  - One-paragraph problem statement.
  - "How it works" section walking through the pipeline.
  - Local dev instructions.
  - GCP setup instructions (or `terraform apply` if fully IaC).
  - Live demo URL.
- [ ] **Loom video** (90 seconds): tour the architecture, demo a search, show the GCP console.
- [ ] **Structured logging** across all services. JSON logs with `traceId`, `talkId`, `service`, `phase`.
- [ ] **Cloud Trace** instrumentation via OpenTelemetry — see a request flow across services.
- [ ] **Custom dashboard** in Cloud Monitoring: ingest rate, transcription latency, embedding cost/day, search QPS, search p95 latency.
- [ ] **Cost guardrails**: budget alerts at $5 and $20/month. Scale-to-zero everywhere except `search-service` (which needs `min-instances=1` for warm index).
- [ ] **Smoke test workflow** in Cloud Build: post-deploy, hit `/healthz` on all services + run one search query.

### Done when
- A stranger can clone the repo and have it running in their own GCP project in <1 hour.
- The Loom video is on the README.
- The Cloud Trace dashboard shows request flow across all 5 services.
- Monthly cost projected under $5 in steady state.

---

## Phase 7 — Stretch Goals (Optional, post-MVP)

Pick ONE if there's time. Don't try multiple — each is a week of work done well.

### 7a. Hybrid Search
Combine vector similarity with BM25 keyword scoring. Re-rank top-100 vector results using keyword overlap. Document the latency/quality trade-off in a blog post.

### 7b. Gemini-Powered TL;DR
New `summary-service` that, on `TalkEmbeddedEvent`, calls Gemini with the transcript and stores a 3-bullet summary in Firestore. Display on the talk detail page.

### 7c. Swap in a hosted vector index
Once the corpus crosses ~10,000 chunks, swap `SEARCH_BACKEND` to a hosted index (Vertex AI Vector Search, or a managed alternative). Keep the in-memory backend as a fallback for local dev. Document the latency / cost / operational trade-offs in a blog post.

### 7d. Open Dataset Release
Publish the transcripts + embeddings as a public GCS bucket + Hugging Face dataset. Get GitHub stars and credibility.

---

## Risk Register

Things most likely to derail this. If you hit one, fall back to the named mitigation, don't try to power through.

| Risk | Likelihood | Mitigation |
|---|---|---|
| YouTube transcript API rate limits / IP blocks | Medium | Throttle to 1 request/sec. Cache aggressively in GCS. If blocked, route through a residential proxy or fall back to manual seed. |
| Talks without auto-generated captions | Low | Mark FAILED with `NO_TRANSCRIPT`. Acceptable to lose ~5% of corpus. Do not add a paid transcription fallback. |
| In-memory index outgrows search-service memory | Low | At 10K chunks ≈ 30MB. Set `memory=1Gi` on Cloud Run for headroom. Past 10K talks, swap to hosted backend (Phase 7c). |
| `search-service` cold start with index load | Medium | `min-instances=1` keeps one warm. Acceptable cost (~$3/month at lowest tier). |
| Firebase Auth → Cloud Run JWT validation | Medium | Use Spring Security with a custom `OncePerRequestFilter` that validates Firebase tokens. Don't try IAP. |
| IAM service account proliferation | Medium | One service account per Cloud Run service. Document grants in `infra/iam.tf` comments. |
| Cloud Build slow feedback loop | Low | Run integration tests locally with Testcontainers. CI is for deploy, not first-pass testing. |
| Cost overrun on Vertex AI embeddings | Low | Budget alert at $5. Cap to 50 talks until pipeline is verified. Embeddings are a one-time cost per talk. |
| Scope creep (recommendations, accounts, etc.) | High | Re-read "Out of scope" in this file. The MVP is what's described in Phases 0-6. |

---

## Daily Workflow for Claude Code

When starting a session, do this:

1. `git status` and read the most recent commit message to understand current state.
2. Find the active phase by reading checkboxes in this file.
3. Pick the next unchecked task in the active phase.
4. Confirm understanding with the user before starting non-trivial work.
5. After completing a task: run local verification, then update the checkbox in this file in the same commit.
6. **Never start a new phase without completing all "Done when" criteria of the current phase.**

### Commands to know

```bash
# Regenerate protos
cd proto && buf generate

# Build and push one service
./gradlew :ingest-service:jib

# Deploy infra changes
cd infra && terraform plan && terraform apply

# Tail logs from a service
gcloud run services logs tail ingest-service --region=us-central1

# Local dev with emulators
docker-compose -f docker-compose.local.yml up
./gradlew :api-gateway:bootRun --args='--spring.profiles.active=local'

# Frontend dev
cd frontend && pnpm dev
```

### When stuck

- gRPC errors → check buf-generated stubs are current.
- Pub/Sub messages not arriving → check IAM grants on the push subscription's service account.
- Cloud Run 403 → service account is missing a role; check `infra/iam.tf`.
- Vertex AI 429 → batch your embedding calls, you're hitting RPM quota.
- YouTube transcript fetch returns empty → talk doesn't have auto-captions. Mark FAILED, move on.
- `search-service` returns nothing → check that the in-memory index loaded on startup. Log the chunk count after init.
- Frontend CORS → `api-gateway` needs `Access-Control-Allow-Origin` for the Firebase Hosting domain.

---

## Definition of Done (whole project)

The project is **shippable** when:
- A reviewer can search "kubernetes pod scheduling" on the live URL and get 5+ relevant talks with timestamps.
- Architecture diagram is in the README.
- 90-second Loom is on the README.
- `terraform destroy` cleanly tears down all resources.
- Repo has a license, a contributing guide, and is public.
- Monthly cost (with traffic = you + occasional reviewer) is under $5.

That's the bar. Anything beyond that is Phase 7.
