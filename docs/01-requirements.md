# Ω7 — Macierz wymagań

## Produkt
- REQ-F-001: komunikator grupowy dla maksymalnie 7 uczestników.
- REQ-F-002: wysyłanie, odbieranie, ponawianie i lokalne kolejkowanie wiadomości.
- REQ-F-003: odpowiedzi, reakcje, załączniki, wyszukiwanie i statusy dostarczenia — architektura przewiduje te funkcje.
- REQ-F-004: blokada aplikacji kodem dostępu.
- REQ-F-005: po 3 błędnych próbach uruchomienie panic wipe danych Ω7.
- REQ-F-006: opcjonalna biometria urządzenia jako drugi mechanizm odblokowania.

## Bezpieczeństwo
- REQ-S-001: brak przechowywania kodu dostępu w postaci jawnej.
- REQ-S-002: klucze kryptograficzne aplikacji mają być przechowywane przez Android Keystore.
- REQ-S-003: szyfrowanie danych lokalnych z uwierzytelnionym szyfrem.
- REQ-S-004: komunikacja sieciowa wyłącznie przez uwierzytelniony TLS.
- REQ-S-005: protokół E2EE ma pochodzić z audytowanej, sprawdzonej implementacji; Ω7 nie implementuje własnego protokołu kryptograficznego.
- REQ-S-006: weryfikacja tożsamości urządzeń i możliwość ich unieważnienia.
- REQ-S-007: minimalizacja metadanych i brak analityki reklamowej.
- REQ-S-008: panic wipe usuwa klucze i dane należące do Ω7; zwykła aplikacja nie wykonuje fabrycznego resetu telefonu.

## UX/UI
- REQ-U-001: cały interfejs użytkownika po polsku.
- REQ-U-002: komunikaty błędów opisują konsekwencję działania.
- REQ-U-003: stany blokady, odblokowania, wymazania, błędu i offline są jednoznaczne.
- REQ-U-004: aplikacja ma być użyteczna dla grupy do 7 osób bez przeładowania interfejsu.

## Niefunkcjonalne
- REQ-N-001: brak sekretów w repozytorium.
- REQ-N-002: testowalność warstw kryptografii, domeny i bezpieczeństwa.
- REQ-N-003: możliwość audytu ścieżki wiadomości od kompozytora do transportu.
- REQ-N-004: wydanie produkcyjne wymaga niezależnego audytu bezpieczeństwa i testów na urządzeniach.
