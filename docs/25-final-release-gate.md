# Ω7 — Final Release Gate

## Gotowe w kodzie

- polski interfejs;
- blokada aplikacji kodem i silną biometrią;
- trwały limit 3 nieudanych prób;
- panic wipe lokalnego stanu;
- Android Keystore dla kluczy lokalnych i tożsamości urządzenia;
- AES-GCM dla lokalnego magazynu;
- ochrona zrzutów ekranu;
- wyłączenie backupu aplikacji;
- blokada cleartext HTTP;
- szyfrowany rejestr zaufania;
- dwustronne QR pairing;
- limit 7 urządzeń;
- fail-closed gate dla transportu bez E2EE;
- testy domenowe;
- CI i dokumentacja bezpieczeństwa;
- instrukcja budowania na telefonie.

## Nie wolno oznaczać jako zweryfikowane bez wykonania

- produkcyjna komunikacja E2EE;
- backend/relay uruchomiony pod kontrolą operatora;
- synchronizacja siedmiu telefonów przez Internet;
- push notifications;
- załączniki szyfrowane end-to-end;
- testy instrumentacyjne na fizycznych urządzeniach;
- fuzzing i testy chaos/concurrency;
- niezależny audyt kryptograficzny;
- podpisany reproducible release APK.

## Kryterium końcowe

Projekt źródłowy jest przygotowany do budowania i testowania na telefonie. Produkcyjne E2EE pozostaje zamknięte, dopóki nie zostanie podłączona i zweryfikowana konkretna implementacja protokołu.
