# Ω7 — produkcyjne E2EE: architektura

## Status

Ta gałąź przechodzi z lokalnego `E2eeNotConfigured` do integracji z oficjalną biblioteką `libsignal`.

Nie wolno jeszcze oznaczać aplikacji jako produkcyjnie zweryfikowanej. Samo dodanie biblioteki kryptograficznej nie jest równoznaczne z poprawną implementacją protokołu, trwałym storage, synchronizacją ani audytem.

## Protokół

Ω7 używa wyłącznie implementacji Signal Protocol dostarczanej przez `org.signal:libsignal-client` + `org.signal:libsignal-android`.

Dla modelu maksymalnie 7 urządzeń komunikacja grupowa jest realizowana jako zestaw niezależnych, uwierzytelnionych sesji urządzenie→urządzenie. Serwer przechowuje wyłącznie publiczne dane niezbędne do ustanowienia sesji oraz zaszyfrowane koperty wiadomości.

Nie implementujemy własnego Double Ratchet, X3DH/PQXDH ani własnego KDF.

## Model wiadomości

1. Klient tworzy wiadomość plaintext lokalnie.
2. Dla każdego docelowego urządzenia tworzona jest osobna koperta protokołu Signal.
3. Każda koperta zawiera wyłącznie ciphertext i minimalne metadane routingu.
4. Serwer nie zna plaintextu, kluczy prywatnych ani klucza grupy.
5. Odbiorca odszyfrowuje kopertę lokalnie i dopiero wtedy zapisuje plaintext do lokalnego zaszyfrowanego magazynu.

## Tożsamość i zaufanie

Każda instalacja ma niezależną tożsamość protokołu Signal. Fingerprint tożsamości musi być prezentowany użytkownikowi podczas dodawania urządzenia i przy wykryciu zmiany klucza.

Zmiana identity key powoduje stan `CHANGED` i zatrzymanie wysyłania do czasu ponownej weryfikacji.

## Prekeys

Każde urządzenie publikuje do backendu:

- public identity key;
- signed prekey + podpis;
- pulę one-time prekeys;
- dane PQXDH wymagane przez używaną wersję libsignal, jeśli API tego wymaga.

Klucze prywatne pozostają wyłącznie na urządzeniu.

## Multi-device

Invariant: `1 właściciel + maksymalnie 6 urządzeń zaufanych = maksymalnie 7`.

Po zatwierdzeniu urządzenia backend musi atomowo sprawdzić limit, oznaczyć urządzenie jako aktywne i uniemożliwić równoległe obejście limitu.

## Revocation

Odwołanie urządzenia wymaga:

- natychmiastowej blokady jego dostępu do nowych kopert;
- oznaczenia urządzenia jako `REVOKED`;
- rotacji materiału grupowego / ponownego ustanowienia sesji zgodnie z przyjętym protokołem;
- obsługi opóźnionych wiadomości bez ujawnienia nowych treści odwołanemu urządzeniu.

## Serwer

Backend jest ślepym relayem. Musi wymuszać:

- TLS;
- uwierzytelnienie urządzenia;
- autoryzację `groupId/deviceId`;
- atomowe one-time invite consumption;
- idempotency key;
- rate limiting;
- replay protection;
- monotoniczny cursor synchronizacji;
- brak plaintextu w logach;
- brak prywatnych kluczy w bazie;
- usuwanie/retencję mailboxów zgodnie z polityką prywatności.

## Warunek produkcyjny

Za produkcyjne E2EE można uznać wydanie dopiero po przejściu:

- testów libsignal na rzeczywistych urządzeniach;
- trwałego SignalProtocolStore;
- pełnego ustanawiania sesji i obsługi prekey/PQXDH;
- offline/online, reorder, duplicate, replay i recovery;
- testów 1→7 urządzeń;
- revocation + rekey;
- szyfrowanych załączników;
- fuzzingu parserów i kopert;
- testów penetracyjnych backendu;
- niezależnego audytu bezpieczeństwa kryptograficznego;
- podpisanego reproducible release.
