# Agent-Based Automation Prototype

This repository hosts the hackathon prototype for an Android-first automation app that uses an AI agent (OpenAI-backed) to author flow graphs from natural language intents.

## Project Layout

- `app/` – Jetpack Compose Android client with chat, flow preview, and demo entry points.
- `runtime/` – Flow execution engine, block metadata, and local notification utilities.
- `agent-client/` – Retrofit-based client for the backend agent service plus secure token storage.
- `data/` – Room persistence layer and repositories coordinating between agent responses and runtime.
- `server/` – Ktor microservice that exposes `/intents/resolve`; currently returns a secure mock flow while wiring for OpenAI is stubbed.
- `docs/` – Architecture and product planning artifacts created earlier in the sprint.

## Getting Started

1. **Android Studio Setup**
   - Open the project in Android Studio Iguana or newer with JDK 17.
   - Let Gradle sync (requires AGP 8.2.2 and Kotlin 1.9.22).
   - Configure an emulator or device running Android 13+.

2. **Run the Backend (Optional but Recommended)**
   - Export your OpenAI key: `export OPENAI_API_KEY=sk-...` (leave unset to use the deterministic demo flow).
   - (Optional) add `openai.key` to `server/src/main/resources/application.conf`.
   - Start the service: `./gradlew :server:run` → listens on `http://localhost:8080`.
   - Logs surface validation results and any OpenAI errors. Ctrl+C to stop.

3. **Run the Demo App**
   - Launch the `Agent Automator` app from Android Studio; the debug build points to `http://10.0.2.2:8080/` (emulator loopback).
   - Tap **Run Demo Flow**. The app requests the backend, displays the generated block list, and executes the flow locally (notifications, webhook call attempt, logs).
   - Review execution steps in-app and in Logcat under `AgentViewModel`.

4. **Unit Tests**
   - `./gradlew test` runs JVM/unit tests including the `FlowEngine` smoke test.
   - For targeted checks: `./gradlew :runtime:test`.

5. **Linking App ↔ Server**
   - Update `BuildConfig.AGENT_BASE_URL` in `app/build.gradle.kts` to point at your server (e.g., `http://10.0.2.2:8080/` for emulator).
   - Replace the mock response in `server/src/main/kotlin/com/devfest/server/Server.kt` with OpenAI API calls once keys are available.

## Security Notes

- Session tokens are stored via `EncryptedSharedPreferences` (AES256) on-device.
- OpenAI keys stay server-side; the app never embeds them.
- JWT validation is wired and ready to enforce once OpenAI integration is active.
- Android runtime requests `POST_NOTIFICATIONS`, `INTERNET`, and `SEND_SMS` (the latter gracefully degrades if permission is denied).

## Next Steps

- Harden OpenAI prompt/response pipeline with retries, structured validation, and telemetry.
- Expand the flow editor UI and tie runtime execution to block handlers.
- Add instrumentation tests and streamlined CI/CD as outlined in `docs/architecture.md`.
