# JARVIS integrations

## Google Calendar + Gmail
The Android client must never receive Google client secrets. OAuth is server-side. Configure `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, then implement the authorization-code callback and encrypted refresh-token store. Requested scopes should be minimal: Calendar read/write and Gmail metadata/send only when those capabilities are explicitly enabled.

## Telegram
Configure `TELEGRAM_BOT_TOKEN` only on the backend. Register `/api/telegram/webhook` behind HTTPS. Voice messages are downloaded server-side and passed through the same transcription pipeline as Android voice.

## Approval boundary
External side effects (sending mail, creating calendar events, sending Telegram messages, financial actions) must create a pending approval unless the user has explicitly configured an allow-list for that action.

## Current status
The contracts and environment switches are prepared. Real Google OAuth credentials, Telegram bot token and production callback URLs are external configuration and are intentionally not committed to GitHub.
