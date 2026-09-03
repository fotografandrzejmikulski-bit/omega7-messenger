# Ω7 Relay Server

Serwer jest ślepym przekaźnikiem. Nie dostaje plaintextu wiadomości ani prywatnych kluczy Signal.

## Endpointy

- `GET /v1/health`
- `POST /v1/bootstrap` — utworzenie właściciela grupy; chronione `X-Bootstrap-Secret`.
- `POST /v1/pair/invites` — krótkotrwałe zaproszenie 5 min.
- `POST /v1/devices/register` — atomowe zużycie zaproszenia i dołączenie urządzenia.
- `GET /v1/keys/{groupId}/{deviceId}` — publiczny bundle PQXDH.
- `POST /v1/messages` — przyjęcie ciphertextu z idempotency key.
- `GET /v1/sync?groupId=...&deviceId=...&cursor=...` — synchronizacja kursorem.

## Uruchomienie

1. Ustaw `OMEGA7_DB_PASSWORD`, `OMEGA7_AUTH_SECRET` i `OMEGA7_BOOTSTRAP_SECRET` jako sekrety środowiskowe.
2. Zbuduj relay: `gradle build`.
3. Uruchom `docker compose up -d --build`.
4. Wystaw relay wyłącznie przez HTTPS z poprawną walidacją certyfikatu.

Sekrety nie mogą mieć wartości domyślnych.

## Model bezpieczeństwa

- limit grupy: 7 aktywnych urządzeń;
- invite token jest losowy, przechowywany na serwerze wyłącznie jako HMAC i zużywany atomowo;
- token urządzenia jest losowy, a w DB znajduje się tylko HMAC tokenu;
- wiadomości są identyfikowane przez `idempotency_key` i mają unikalność per odbiorca;
- synchronizacja używa monotonicznego `BIGSERIAL` cursor;
- serwer nie wykonuje operacji kryptograficznych na plaintextach wiadomości;
- logi aplikacji nie powinny zawierać tokenów, kluczy ani treści wiadomości.

## Przed produkcją

Wymagane są reverse-proxy TLS, rate limiting, monitoring bez treści wiadomości, backup/restore PostgreSQL, rotacja sekretów, testy penetracyjne, fuzzing API oraz niezależny audyt E2EE. Ten katalog nie jest dowodem wykonania tych testów.
