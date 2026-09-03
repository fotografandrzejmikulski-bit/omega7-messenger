# Ω7 — doprowadzenie do działania na telefonie

## Stan
Projekt jest przygotowany jako aplikacja Android i może być otwarty bezpośrednio w Android Studio. Oficjalny workflow Androida używa Gradle do budowania, a fizyczne urządzenie może być użyte jako urządzenie testowe.

## Co jest już zamknięte w kodzie
- Polski interfejs.
- Maksymalnie 7 uczestników na poziomie domeny.
- Lokalny magazyn szyfrowany AES-GCM.
- Klucz lokalnego magazynu w Android Keystore.
- Kod dostępu przechowywany jako salted PBKDF2 verifier.
- Licznik błędnych prób utrwalany między uruchomieniami.
- Trzecia błędna próba uruchamia panic wipe lokalnych danych Ω7.
- Silna biometria, jeśli urządzenie ją udostępnia.
- Blokada sesji po opuszczeniu aplikacji.
- FLAG_SECURE przeciwko zrzutom ekranu i części przypadków przechwytywania zawartości.
- Wyłączone kopie zapasowe i ruch cleartext.
- Rejestr zaufanych urządzeń przygotowany jako warstwa lokalna.
- Wyszukiwanie lokalnych wiadomości.
- Zmiana kodu dostępu.
- Ręczny panic wipe z potwierdzeniem.
- Jasne oznaczenie wiadomości jako `LOCAL_ONLY`; aplikacja nie udaje działającego E2EE.

## Czego nie wolno uznać za gotowe
1. Serwer transportowy.
2. Produkcyjne E2EE dla grupy 7 osób.
3. Weryfikacja tożsamości urządzeń przez niezależny kanał.
4. Powiadomienia z bezpiecznym ukrywaniem treści.
5. Załączniki i szyfrowane media.
6. Synchronizacja, kolejki offline i idempotencja po stronie serwera.
7. Instrumentacyjne testy na realnych urządzeniach.
8. Audyt kryptograficzny i penetracyjny.
9. Produkcyjne podpisywanie i publikacja.

## Uruchomienie na telefonie
1. Otwórz projekt `omega7-messenger` w aktualnym Android Studio.
2. Poczekaj na synchronizację Gradle.
3. Podłącz telefon z włączonym debugowaniem USB albo użyj emulatora.
4. Uruchom wariant `debug`.
5. Przejdź scenariusze z `docs/12-final-gate.md` i `docs/10-red-team.md`.
6. Zbuduj `release` dopiero po skonfigurowaniu własnego klucza podpisywania.

## Ważne
Normalna aplikacja Android nie może arbitralnie wykonać fabrycznego resetu całego telefonu. Ω7 implementuje bezpieczny odpowiednik aplikacyjny — panic wipe własnego stanu. Fabryczny wipe jest domeną zarządzanego urządzenia i odpowiednich uprawnień administracyjnych.
