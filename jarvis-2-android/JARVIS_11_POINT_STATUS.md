# JARVIS 2.0 — 11-point implementation gate

1. Native voice input/output — IMPLEMENTED: `expo-audio` recording, backend transcription, Android TTS.
2. Push notifications — IMPLEMENTED: `expo-notifications` registration and backend push endpoint; EAS/development build required for remote push on Android.
3. Tasks, memory and approvals — IMPLEMENTED: mobile API client and live Command Center counters; backend persistence.
4. Approval center — IMPLEMENTED at API level with pending/approved/rejected states; UI can consume `/api/approvals`.
5. Google Calendar — INTEGRATION CONTRACT READY. Production OAuth credentials/callback remain external configuration.
6. Gmail — INTEGRATION CONTRACT READY. Production OAuth credentials/scopes remain external configuration.
7. PostgreSQL + vector memory — SCHEMA + `pgvector` adapter READY. Enable with `DATABASE_URL`; migration/production provisioning remains deployment configuration.
8. n8n orchestration — ARCHITECTURE READY around current Tools Agent model; workflows must be connected to deployed services and credentials.
9. Dynamic model routing — IMPLEMENTED in backend: complex/research -> `gpt-5.6-sol`, normal -> `gpt-5.6-terra`, short/simple -> `gpt-5.6-luna` (overridable by environment variables).
10. Automated verification — IMPLEMENTED in repository workflow via `expo-doctor` and build gate; backend `npm run check` remains available.
11. EAS APK — CONFIGURED: preview profile produces installable APK and GitHub Actions can trigger the cloud build. Requires the user's Expo account token as GitHub secret `EXPO_TOKEN` and one-time EAS project linking.

## Release truth

The source is prepared and committed to GitHub. A downloadable APK is **not claimed until the EAS cloud build completes successfully**. No OpenAI, Google, Telegram, Expo or database secret is committed to the repository.
