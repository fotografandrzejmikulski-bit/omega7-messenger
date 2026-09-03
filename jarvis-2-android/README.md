# JARVIS 2.0 Android

Android-first personal AI executive assistant built with Expo SDK 57 / React Native 0.86.

## Current architecture
- Native Expo Router Android client
- Command Center with text and native voice input
- Speech output via device TTS
- Agent mesh, tasks, memory and approval center
- Push-token registration and test notification API
- Backend OpenAI Responses API gateway
- Server-side transcription endpoint
- Audit and integration-status endpoints
- EAS preview APK profile
- Root GitHub Actions workflow for APK builds

## Build an installable APK
The `preview` profile is configured with `android.buildType: "apk"`, so EAS produces an installable APK rather than an AAB.

1. Create/sign in to an Expo account.
2. Link the project to EAS once if it has not already been linked.
3. Run `npm install` inside `jarvis-2-android`.
4. Run `npx eas-cli@latest build --platform android --profile preview`.

For GitHub Actions, add an Expo access token as the repository secret `EXPO_TOKEN`. The workflow at `.github/workflows/jarvis-android-apk.yml` then validates the project and starts the EAS APK build on pushes affecting `jarvis-2-android` or manually from the Actions tab.

## Backend
The backend lives in `backend/` and runs on Node 22+. Configure it using `backend/.env.example` as a template. Keep all real credentials server-side; never put an OpenAI API key in the Android client or commit secrets.

## Release truth
An APK is considered released only after an actual EAS build finishes successfully. Configuration alone is not treated as a successful build. Google Calendar, Gmail, PostgreSQL/pgvector and n8n currently have integration contracts/architecture in the repository; they are not represented as live external services until their credentials and deployment are configured.
