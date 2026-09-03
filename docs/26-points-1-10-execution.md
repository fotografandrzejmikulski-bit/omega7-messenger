# Ω7 — wykonanie punktów 1–10

## Status wykonawczy

Ten dokument zamyka przegląd punktów 1–10 z planu prac. Nie zastępuje testów na urządzeniach ani niezależnego audytu.

| Punkt | Zakres | Status | Wniosek |
|---|---|---|---|
| 1 | Weryfikacja kompletności repozytorium | DONE | Repozytorium zawiera aplikację Android, testy, CI, dokumentację i kontrakt serwera. |
| 2 | Synchronizacja najnowszego hardeningu MainActivity | PARTIAL | Lokalna wersja 0.8.1 zawiera re-check limitu, TTL zaproszenia, duplicate-device rejection i fingerprint w dialogu. Kopia GitHub wymaga pełnego zastąpienia pliku, więc nie oznaczamy synchronizacji jako zakończonej. |
| 3 | Modernizacja skanera QR | OPEN | Obecna implementacja używa IntentIntegrator/onActivityResult. Migracja do Activity Result ScanContract jest zadaniem technicznym przed stabilnym wydaniem. |
| 4 | Walidacja displayName | DONE | PairingRequest ogranicza nazwę do 80 znaków; dane są przycinane przy tworzeniu żądania i odrzucane przy parsowaniu ponad limit. |
| 5 | Revokacja urządzeń | PARTIAL | Model stanów posiada REVOKED, ale produkcyjne odwołanie musi być egzekwowane przez backend i połączone z rotacją kluczy grupowych E2EE. |
| 6 | Jednorazowe zaproszenie | PARTIAL | Zaproszenie ma krótki TTL i jest powiązane z aktywnym inviteId, ale pełna jednorazowość wymaga atomowego server-side consume. |
| 7 | Fallback pairing code | OPEN | Nie dodajemy słabego statycznego kodu. Jeśli fallback będzie potrzebny, musi być krótkotrwały, jednorazowy i związany z konkretnym urządzeniem/sesją. |
| 8 | Backend/relay | CONTRACT | Kontrakt endpointów jest opisany w server/README.md. Implementacja produkcyjna, TLS, trwała baza, rate limiting i mechanizmy awarii pozostają do wykonania. |
| 9 | Produkcyjne E2EE | BLOCKED BY DESIGN | Interfejs E2eeEngine istnieje, ale transport pozostaje fail-closed. Nie wdrażamy własnego protokołu kryptograficznego jako substytutu audytowanego E2EE. |
| 10 | Testy bezpieczeństwa i wydania | PARTIAL | CI Android przechodzi unit tests, lint, debug APK i release APK. Brak jeszcze testów fizycznych, fuzzingu, chaos/concurrency, recovery i niezależnego audytu. |

## Model siedmiu urządzeń

Urządzenia są dodawane progresywnie:

`1 właściciel → 2 → 3 → 4 → 5 → 6 → 7`

Po osiągnięciu siedmiu urządzeń ósme musi zostać odrzucone. Każdy etap powinien być testowany przed przejściem do następnego.

## Kryterium końca

Punkty 1–10 są przeanalizowane i przypisane do stanu wykonania. Projekt nie otrzymuje jednak statusu produkcyjnego, dopóki nie zostaną zamknięte blokady P0 z `docs/13-production-blockers.md`.

## Zasada bezpieczeństwa

Brak implementacji nie jest zastępowany deklaracją. Jeżeli komponent wymaga backendu, produkcyjnego E2EE, fizycznego urządzenia lub niezależnego audytu, pozostaje oznaczony jako OPEN/PARTIAL/BLOCKED/REQUIRES VERIFICATION.
