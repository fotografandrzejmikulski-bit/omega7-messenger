# Ω7 Relay — kontrakt serwera

Minimalny serwer ma być ślepym przekaźnikiem dla zaszyfrowanych pakietów E2EE.

## Endpointy docelowe

- `POST /v1/pair/invites` — utworzenie krótkotrwałego zaproszenia.
- `POST /v1/pair/consume` — jednorazowe zużycie zaproszenia.
- `POST /v1/devices/approve` — zatwierdzenie urządzenia.
- `POST /v1/devices/revoke` — odwołanie urządzenia.
- `POST /v1/messages` — wyłącznie ciphertext E2EE.
- `GET /v1/sync?cursor=...` — synchronizacja zaszyfrowanych zdarzeń.
- `GET /v1/health` — health check.

## Twarde zasady

- serwer nigdy nie przyjmuje plaintextu wiadomości;
- prywatne klucze urządzeń nigdy nie trafiają na serwer;
- zaproszenia QR są krótkotrwałe i jednorazowe;
- wiadomości wymagają idempotency key;
- członkostwo jest autoryzowane per grupa i urządzenie;
- logi nie mogą zawierać treści, tokenów ani kluczy.

## Bramka produkcyjna

TLS, rate limiting, rotacja poświadczeń, trwała baza, testy awarii, backup/restore oraz niezależny audyt E2EE pozostają obowiązkowe przed produkcją.
