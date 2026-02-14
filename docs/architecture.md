# AI Automation Prototype Architecture (Hackathon Build)

## 1. Overview
- **Goal:** Android-only prototype demonstrating agent-authored automation flows, backed by a secure lightweight service that orchestrates OpenAI for flow synthesis.
- **Guiding Principles:** Privacy-first (minimal data off device), human-in-the-loop transparency, modularity for rapid iteration, hackathon-friendly scope that can scale post-event.

```mermaid
flowchart LR
    subgraph Android App
        UI[Compose UI<br/>Chat + Flow Editor]
        VM[ViewModels & UseCases]
        Engine[Flow Runtime Engine]
        Store[Room DB & Preferences]
        Blocks[Block Descriptor Registry]
    end
    subgraph Secure Backend
        API[Ktor API Gateway]
        Synth[Flow Synthesis Service<br/>(OpenAI client)]
        Vault[Secrets Manager]
        Telemetry[Event Stream]
    end
    subgraph External
        OpenAI[(OpenAI API)]
        Webhook[User Webhooks/IFTTT]
    end

    UI --> VM --> Engine
    VM --> Store
    VM --> Blocks
    UI -->|Intent JSON| API
    API --> Synth --> OpenAI
    Synth -->|Flow Graph| API --> UI
    Engine --> Webhook
    Engine --> Store
    API --> Telemetry
    Vault -.-> API
```

## 2. Android Client Architecture
- **Modules**
  - `app`: Compose UI, navigation, dependency injection (Hilt/Koin), permission flows.
  - `agent-client`: GraphQL/REST client for backend, request/response schema, retry with exponential backoff.
  - `runtime`: Flow engine executing block DAG via Kotlin Coroutines; includes block handlers and WorkManager integration.
  - `data`: Repository layer bridging Room, DataStore Preferences, and network clients.
- **Flow Editor**
  - Compose Canvas-backed node editor with snap-to-grid lanes (Trigger → Conditions → Actions).
  - Graph model stored as immutable data class; diff-based updates for undo/redo.
- **Runtime Execution**
  - Each flow instance runs inside a foreground `LifecycleService` using coroutine scopes.
  - WorkManager schedules deferred flows (location/time) with unique work IDs for cancellation.
  - State persisted in Room tables: `flows`, `blocks`, `variables`, `executions`.
- **Agent Conversation**
  - Chat screen collects intent; context bundler adds device metadata (time, installed block capabilities) after user consent.
  - Requests signed with short-lived auth token, transmitted over TLS to backend.
- **Telemetry & Logging**
  - Structured logs via Timber → logcat; optional upload of anonymized metrics (opt-in).
  - Crashlytics or open-source alternative gated behind runtime feature flag.

## 3. Backend & Infrastructure
- **API Gateway (Ktor)**
  - Stateless services deployed on Fly.io/Render/GCP Cloud Run.
  - Endpoints: `/intents/resolve`, `/flows/validate`, `/flows/templates`.
  - Input validation with kotlinx.serialization + JSON schema enforcement.
- **Flow Synthesis Service**
  - Prompt templates referencing canonical block IDs and parameters.
  - Uses OpenAI API (gpt-4.1-mini) via official SDK; responses validated against JSON schema.
  - Caches recent successful mappings in Redis (or in-memory for hackathon) keyed by normalized intent hash.
- **Security Services**
  - Secrets (OpenAI keys, signing keys) stored in cloud secrets manager (GCP Secret Manager/AWS SM).
  - JWT issuance using short (15 min) expiry; refresh tokens stored encrypted in Postgres.
- **Observability**
  - Request logging (without PII) to structured logs.
  - Basic metrics (latency, error rate) via Prometheus-compatible exporter or vendor (e.g., DataDog Lite).
  - Centralized audit trail storing prompt + model response with user consent flag.
- **Deployment**
  - CI/CD via GitHub Actions: build + lint + unit tests; deploy containers on tagged release.
  - Infra-as-code using Terraform Lite or simple YAML manifests; ensure reproducible environment.

## 4. Data Contracts
- **Intent Request**
  ```json
  {
    "user_id": "uuid",
    "intent_text": "When I leave the office, turn on AC...",
    "context": {
      "location_aliases": ["Home", "Office"],
      "capabilities": ["LOCATION", "HTTP", "NOTIFICATION"],
      "time_window": {"tz": "America/New_York", "now": "2026-02-04T10:00:00-05:00"}
    },
    "session_token": "jwt"
  }
  ```
- **Flow Graph Response**
  ```json
  {
    "flow_id": "uuid",
    "title": "Leave Work Comfort Kit",
    "blocks": [
      {"id": "b1", "type": "LocationExitTrigger", "params": {...}},
      {"id": "b2", "type": "TimeWindowCondition", "params": {...}},
      {"id": "b3", "type": "HttpWebhookAction", "params": {...}}
    ],
    "edges": [
      {"from": "b1", "to": "b2", "condition": "onExit"},
      {"from": "b2", "to": "b3", "condition": "withinWindow"}
    ],
    "explanation": "Leaving office after 5 PM triggers AC and notification.",
    "risk_flags": ["Requires location permission", "Calls external webhook"]
  }
  ```
- **Execution Logs**
  - Stored locally in Room and optionally synced as hashed event summaries to backend for analytics.

## 5. Security & Privacy Posture
- **Authentication**
  - User login via Email Magic Link or OAuth (Google). No passwords stored.
  - Backend issues short-lived JWT signed with rotating asymmetric keys.
- **Authorization**
  - Claims-based access to endpoints (`scope:intent.resolve`, `scope:flow.share`).
  - Device registration tokens to correlate sessions without exposing hardware identifiers.
- **Secrets Handling**
  - OpenAI API key never shipped on device. Backend proxies requests; Android app uses signed intents.
  - Android stores auth tokens in EncryptedSharedPreferences (AES GCM via Android Keystore).
- **Permissions Governance**
  - Runtime enforces capabilities: flows cannot add blocks requiring permissions user hasn’t granted.
  - During AI synthesis, backend cross-checks requested blocks with on-device capability list; rejects mismatches.
- **Data Minimization**
  - Only send metadata necessary for flow creation; actual contacts/phone numbers stored locally and referenced via placeholders until user confirmation.
  - Optional redaction mode strips personally identifying tokens before logging prompts.
- **Secure Communication**
  - Mutual TLS between app and backend optional (certificate pinning on client).
  - Backend enforces TLS 1.2+, HSTS, and rate limiting per user/IP.
- **Threat Mitigation**
  - Input sanitization to avoid prompt injection (escape user data, set system prompts).
  - Validate block graph to prevent infinite loops or malicious webhooks.
  - Abuse detection: monitor for excessive webhook calls or SMS triggers; auto-disable flow and notify user.
- **Compliance Considerations**
  - Document data flows for GDPR-style subject access requests.
  - Store audit logs with retention policy (30 days) and encryption at rest (Postgres + pgcrypto).

## 6. Developer Workflow & Quality Gates
- **Branching:** trunk-based with feature branches `codex/<feature-name>`.
- **Code Quality:** ktlint, detekt, unit tests (JUnit5), instrumentation tests (Compose UITest) run in CI.
- **Security Testing:** dependency scanning (OWASP dependency-check), API contract tests, prompt regression tests to catch LLM drift.
- **Release Process:** internal demo builds via Gradle managed Play internal track or APK sideload; feature flags guard unfinished modules.
- **Documentation:** keep `docs/` updated with architecture, block specs, API contracts; README includes setup and threat model excerpt.

## 7. Future Hardening (Post-Hackathon)
- Move to multi-region deployment, integrate WAF and DDoS protection.
- Implement fine-grained flow versioning and rollback.
- Add on-device inference fallback with smaller distilled model.
- Expand secret rotation automation and penetration testing cadence.
