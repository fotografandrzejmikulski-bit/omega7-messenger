# n8n orchestration

JARVIS uses n8n as an orchestration layer, not as the primary reasoning engine. Use the current **Tools Agent** architecture for tool execution; do not use the deprecated Plan-and-Execute agent.

Recommended flows:

1. Router webhook -> normalize input -> policy classification -> JARVIS backend.
2. Approval webhook -> policy check -> pending approval -> callback after user decision.
3. Morning brief schedule -> Calendar/Gmail/Tasks/Memory tools -> executive synthesis -> push notification.
4. Event-driven flows -> Gmail/Calendar/Telegram -> deduplicate -> memory/audit -> notification when actionable.

Every workflow should carry `trace_id`, reject unauthenticated webhooks, and record external side effects in the JARVIS audit log.
