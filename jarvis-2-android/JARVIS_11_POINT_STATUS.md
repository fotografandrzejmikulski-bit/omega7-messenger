# JARVIS 2.0 — 11-point implementation gate

1. Native voice input/output — IMPLEMENTED: `expo-audio` recording, backend transcription, Android TTS.
2. Push notifications — IMPLEMENTED: `expo-notifications` registration and backend push endpoint; remote push requires an EAS/development build and credentials.
3. Tasks, memory and approvals — IMPLEMENTED: mobile API client, live Command Center counters and persistent backend state.
4. Approval center — IMPLEMENTED: mobile `app/approvals.tsx` plus pending/approved/rejected API states.
5. Google Calendar — INTEGRATION CONTRACT READY. Production OAuth credentials/callback and encrypted refresh-token storage are deployment configuration.
6. Gmail — INTEGRATION CONTRACT READY. Production OAuth credentials/scopes and encrypted refresh-token storage are deployment configuration.
7. PostgreSQL + vector memory — SCHEMA + `pgvector` adapter READY. Production activation requires `DATABASE_URL` and migration execution; JSON persistence remains the active fallback.
8. n8n orchestration — ARCHITECTURE READY around the current Tools Agent model. Production activation requires deployed n8n plus service credentials.
9. Dynamic model routing — IMPLEMENTED in backend: complex/research -> `gpt-5.6-sol`, normal -> `gpt-5.6-terra`, short/simple -> `gpt-5.6-luna`; all are environment-overridable.
10. Automated verification — IMPLEMENTED in GitHub Actions with dependency installation and `expo-doctor`; backend syntax check remains available.
11. EAS APK — CONFIGURED: preview profile creates an installable Android APK and GitHub Actions can start the cloud build. The remaining external prerequisites are an Expo project link and `EXPO_TOKEN` GitHub secret.

## Additional core surfaces now live

- Agents screen reads `/api/agents` instead of a static UI list.
- Memory screen reads `/api/memory` and can persist new memory records through `/api/memory`.
- Automations now have persistent `/api/automations` GET/POST/PATCH endpoints and the mobile screen reads/toggles live state.

## Release truth

The JARVIS Android source is committed to GitHub. Core mobile/backend surfaces are implemented, while Calendar, Gmail, PostgreSQL activation and n8n remain deployment/integration work. A **release APK is not claimed until the EAS cloud build actually succeeds**. No OpenAI, Google, Telegram, Expo or database secret is committed to the repository.
