# Ω7 — blokady wydania produkcyjnego

Ten dokument rozdziela funkcje działające lokalnie od elementów, które wymagają infrastruktury lub niezależnej weryfikacji.

## P0 — wymagane przed publicznym wydaniem

1. Audytowana implementacja E2EE dla komunikacji grupowej do 7 osób.
2. Serwer uwierzytelniania, synchronizacji i dystrybucji zaszyfrowanych kopert.
3. Weryfikacja tożsamości urządzeń i bezpieczna rotacja kluczy.
4. Replay/downgrade protection oraz idempotencja operacji synchronizacji.
5. Push bez ujawniania treści wiadomości.
6. Szyfrowane załączniki z walidacją typu, rozmiaru i cyklu życia.
7. Testy na rzeczywistych urządzeniach Android.
8. Fuzzing parserów, protokołu, synchronizacji i obsługi załączników.
9. Niezależny audyt bezpieczeństwa.
10. Reproducible release, podpis aplikacji, SBOM i procedura aktualizacji.

## P1 — wymagane przed wersją stabilną

- backup/recovery z jednoznacznym modelem zaufania;
- obsługa zmiany urządzenia i wycofania urządzenia;
- obsługa konfliktów offline;
- rate limiting i ochrona przed abuse;
- telemetryka bez wycieku danych treściowych;
- accessibility review;
- testy wydajnościowe.

## Zakaz fałszywego statusu

Dopóki P0 nie jest zamknięte, aplikacja nie może być opisywana jako zweryfikowany produkcyjny komunikator E2EE.
