# Ω7 — Production Protocol Contract

## Założenie

Ten dokument definiuje granicę między klientem Ω7, dostawcą E2EE i serwerem. Nie jest implementacją kryptografii.

## Zasady

1. Plaintext istnieje tylko po stronie klienta.
2. Transport przyjmuje wyłącznie ciphertext.
3. Każda operacja wysyłania posiada unikalny `idempotencyKey`.
4. Serwer odrzuca żądania bez ważnej autoryzacji i bez członkostwa w grupie.
5. Serwer nie interpretuje ciphertextu.
6. Klient odrzuca zmianę klucza/urządzenia wymagającą ponownej weryfikacji.
7. Synchronizacja jest odporna na duplikaty i może być ponawiana.
8. Limity rozmiaru są egzekwowane zarówno na kliencie, jak i serwerze.
9. Logi nie mogą zawierać plaintextu, tokenów, kluczy ani ciphertextu, jeśli nie jest to absolutnie wymagane diagnostycznie.
10. Błędy bezpieczeństwa są fail-closed.

## Minimalny kontrakt API

### POST /v1/messages

- Auth: krótkotrwały token sesyjny / mechanizm wynikający z wybranego protokołu rejestracji.
- Authorization: nadawca musi być członkiem grupy.
- Input: `group_id`, `ciphertext`, `idempotency_key`.
- Validation: długości, kodowanie, rozmiar payloadu, nonce/replay policy wynikające z E2EE.
- Output: potwierdzenie przyjęcia + serwerowy identyfikator zdarzenia.
- Idempotency: ponowienie tego samego klucza nie tworzy drugiej wiadomości.
- Rate limit: per device + per account + per group.

### GET /v1/sync?cursor=...

- Auth required.
- Zwraca wyłącznie zdarzenia uprawnione dla urządzenia.
- Cursor jest opaque.
- Powtórne pobranie tego samego kursora musi być bezpieczne.

## Brakujące elementy

Wybór konkretnego protokołu E2EE, format kluczy, lifecycle prekeys, urządzenia wielokrotne, grupowe uzgadnianie kluczy, bezpieczeństwo recovery i mechanizm weryfikacji fingerprintów muszą zostać zamknięte przed oznaczeniem produktu jako produkcyjny.
