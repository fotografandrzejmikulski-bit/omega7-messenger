# Ω7 — plan red-team

## A. Uwierzytelnianie
- 0/1/2/3 błędne próby.
- restart procesu po każdej próbie.
- rotacja/wyczyszczenie danych aplikacji.
- dostęp biometryczny po zmianie stanu sesji.

## B. Magazyn
- uszkodzony ciphertext.
- pusty plik.
- obcięty plik.
- zmieniony IV/tag.
- brak klucza Keystore.
- równoległy zapis.

## C. Sieć
- HTTP zamiast HTTPS.
- MITM certyfikatu.
- replay wiadomości.
- duplikaty żądań.
- zmieniona odpowiedź serwera.
- rate limiting.

## D. Aplikacja Android
- deep links.
- screenshoty i recent apps.
- backup/restore.
- logcat.
- eksport komponentów.
- intent injection.
- overlay/tapjacking.

## E. E2EE
- zmiana klucza urządzenia.
- nowe urządzenie.
- usunięcie urządzenia.
- usunięcie członka grupy.
- ponowne dołączenie.
- replay i reorder.
- rollback stanu kryptograficznego.

## F. Cel końcowy
Każdy finding musi mieć: ID, severity, opis, wektor, wpływ, reprodukcję, poprawkę, test regresyjny i status.
