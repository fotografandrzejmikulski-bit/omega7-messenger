# Ω7 — Kontrakt API (wersja docelowa)

## POST /v1/devices/register
Rejestruje urządzenie i jego publiczne materiały tożsamości.

## POST /v1/groups
Tworzy grupę. Maksymalnie 7 aktywnych członków.

## POST /v1/groups/{groupId}/members
Dodaje urządzenie do grupy po autoryzacji administratora grupy.

## DELETE /v1/groups/{groupId}/members/{deviceId}
Unieważnia urządzenie.

## POST /v1/messages
Przyjmuje wyłącznie pakiet E2EE. Serwer nie otrzymuje jawnej treści.

## GET /v1/sync?cursor=...
Pobiera szyfrowane zdarzenia i zmiany stanu.

## Wymagania wspólne
- TLS 1.3 preferowane.
- Uwierzytelnianie tokenem krótkotrwałym.
- Walidacja długości i schematu wejścia.
- Idempotency-Key dla wysyłania wiadomości.
- Limity częstotliwości.
- Brak logowania treści i kluczy.
