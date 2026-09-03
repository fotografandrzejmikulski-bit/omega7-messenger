# JARVIS 2.0 Android

Android-first personal AI executive assistant built with Expo SDK 57 / React Native 0.86.

## Components
- Command Center mobile UI
- Agent mesh
- Task queue
- Long-term memory surface
- Automation surface
- Secure backend URL storage with Expo SecureStore
- Backend OpenAI Responses API gateway
- Approval/audit API primitives
- EAS preview APK profile

## Build on EAS
1. Install Node.js and EAS CLI.
2. Authenticate with Expo.
3. Run `npm install`.
4. Run `npx eas build --platform android --profile preview`.

For GitHub Actions, configure the repository secret `EXPO_TOKEN`; the workflow in `.github/workflows/android.yml` then builds the preview APK.

**Never place an OpenAI API key in this Android project.** Keep it only on the backend in a secret manager/environment variable.
