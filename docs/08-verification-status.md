# Ω7 Messenger — status weryfikacji

Data: 2026-09-03

## Zweryfikowane na poziomie źródeł

- Limit grupy wynosi dokładnie 7 osób.
- Interfejs użytkownika i komunikaty aplikacji są po polsku.
- Lokalny magazyn korzysta z AES-GCM i klucza z Android Keystore.
- Panic wipe niszczy klucz lokalny oraz usuwa lokalne dane aplikacji.
- Po trzech błędnych próbach kodu uruchamiany jest panic wipe.
- Ruch cleartext HTTP jest wyłączony w konfiguracji aplikacji.
- Kopie zapasowe aplikacji są wyłączone.
- Warstwa E2EE jest jawnie oddzielona od lokalnego szyfrowania i nie zawiera własnego protokołu kryptograficznego.
- Sesja ma stany LOCKED, UNLOCKED i PANIC_WIPED.
- Dodano testy jednostkowe dla limitu grupy i maszyny stanu sesji.

## Nieweryfikowane w tym środowisku

- pełny build Gradle/APK;
- instalacja i uruchomienie na fizycznym urządzeniu Android;
- testy instrumentacyjne i UI;
- testy na wielu wersjach Androida i producentach urządzeń;
- penetracyjne testy backendu;
- rzeczywista implementacja E2EE i niezależny audyt kryptograficzny;
- push notifications bez ujawniania treści;
- synchronizacja offline, konflikty i idempotencja w produkcyjnym backendzie;
- rotacja kluczy, odzyskiwanie konta i pełne zarządzanie urządzeniami;
- fuzzing parserów i protokołu;
- SBOM, podpis release, reproducible build i weryfikacja łańcucha dostaw.

## Decyzja release

STATUS: **NIE WYDAWAĆ PRODUKCYJNIE**.

Powód: brak zweryfikowanego buildu urządzeniowego oraz brak podłączonej, audytowanej warstwy E2EE i backendu produkcyjnego.

Projekt jest przygotowany tak, aby te elementy można było dodać bez obchodzenia granic bezpieczeństwa.
