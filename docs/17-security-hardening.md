# Ω7 — Security Hardening 0.6

## Cel

Wersja 0.6 wzmacnia lokalną granicę bezpieczeństwa i przygotowuje aplikację do późniejszego podłączenia produkcyjnego E2EE bez możliwości przypadkowego wysłania plaintextu przez transport.

## Zastosowane zabezpieczenia

- AES-256-GCM w Android Keystore dla lokalnego magazynu.
- Losowy 96-bitowy IV dla każdego szyfrowania.
- Uwierzytelniony nagłówek formatu magazynu i wersjonowanie.
- Kontrola maksymalnego rozmiaru danych lokalnych.
- Best-effort synchronizacja zapisu pliku na dysk przed podmianą.
- Niszczenie kluczy Keystore podczas panic wipe.
- Osobny nieeksportowalny klucz HMAC w Android Keystore jako pepper dla lokalnego uwierzytelniania.
- Trwały licznik błędnych prób.
- Szyfrowany lokalny rejestr zaufanych urządzeń.
- Fail-closed gate na granicy E2EE/transportu.
- Walidacja domenowych wiadomości i limitów rozmiaru.
- Brak plaintextowego transportu.

## Granice gwarancji

Ta wersja nadal nie jest produkcyjnym komunikatorem E2EE. Nie wolno twierdzić, że transport internetowy zapewnia poufność end-to-end, dopóki nie zostanie podłączona i zweryfikowana konkretna implementacja protokołu kryptograficznego.

Nie implementujemy własnego Double Ratchet, X3DH, MLS ani własnego systemu uzgadniania kluczy.

## Model serwera docelowego

Serwer powinien traktować payload jako nieprzejrzysty ciphertext i wykonywać wyłącznie routing, kolejki, antyreplay, idempotencję, autoryzację członkostwa oraz synchronizację metadanych niezbędnych do działania usługi.

Serwer nie powinien otrzymywać plaintextu wiadomości, kluczy prywatnych ani kluczy sesyjnych.

## Aktualny blok produkcyjny

Do zamknięcia pozostają: rzeczywista implementacja E2EE, backend, rejestracja urządzeń, bezpieczne parowanie, synchronizacja wielourządzeniowa, załączniki szyfrowane E2EE, powiadomienia bez wycieku treści, testy instrumentacyjne na urządzeniach oraz niezależny audyt kryptograficzny.
