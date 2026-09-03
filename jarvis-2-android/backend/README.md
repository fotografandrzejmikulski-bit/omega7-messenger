# JARVIS backend

Node 22 HTTP gateway for the Android client.

## Endpoints
- `GET /api/health`
- `GET /api/agents`
- `GET|POST /api/tasks`
- `PATCH /api/tasks/:id`
- `GET|POST /api/memory`
- `GET|POST /api/approvals`
- `PATCH /api/approvals/:id`
- `GET /api/audit`
- `POST /api/chat`

When `OPENAI_API_KEY` is present, `/api/chat` uses the OpenAI Responses API. The key is never sent to Android. Set `JARVIS_API_TOKEN` in production to require Bearer authentication.

This backend is intentionally dependency-light and is a foundation for the next integration layer: PostgreSQL/vector memory, Google services, Telegram voice, n8n Tools Agent, notifications and policy-enforced tool execution.
