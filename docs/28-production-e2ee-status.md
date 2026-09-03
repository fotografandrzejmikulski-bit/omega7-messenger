# Ω7 — status produkcyjnego E2EE

## Stan implementacji

Gałąź `omega7-production-e2ee` zawiera działającą warstwę integracyjną z oficjalnym `libsignal` 0.100.0 oraz backend blind-relay oparty o PostgreSQL.

### Zaimplementowane w kodzie

- `libsignal-client` + `libsignal-android` 0.100.0.
- Trwały `SignalProtocolStore`: identity key, registration ID, prekeys, signed prekey, Kyber prekey, sesje i zaufane identity są serializowane i szyfrowane lokalnie przez AES-GCM/Android Keystore.
- PQXDH prekey bundle z EC signed prekey i Kyber prekey.
- Ustanawianie sesji przez `SessionBuilder` i szyfrowanie/deszyfrowanie przez `SessionCipher`.
- Osobna sesja Signal dla każdego urządzenia docelowego.
- Koperta aplikacyjna zawierająca osobny ciphertext dla każdego zweryfikowanego urządzenia.
- Odrzucanie niedozwolonych typów wiadomości oraz ograniczenia rozmiaru.
- Persistowanie stanu po mutacjach kryptograficznych.
- Blokada automatycznej podmiany zaufanego identity key: zmiana wymaga ponownej weryfikacji.
- Twardy limit 7 urządzeń po stronie klienta.
- Backend z PostgreSQL, atomowym zużyciem jednorazowego zaproszenia, blokadą wiersza grupy przy kontroli limitu, idempotency key i monotonicznym cursorem.
- Serwer nie posiada kodu odszyfrowującego wiadomości.
- `usesCleartextTraffic=false` i uprawnienie INTERNET po stronie Androida.
- CI buduje aplikację Android i relay backend.

## Ważne ograniczenie

Kod nie może być jeszcze oznaczony jako **niezależnie zweryfikowane produkcyjne E2EE**. Nadal wymagane są:

1. rzeczywisty test dwóch i siedmiu fizycznych urządzeń;
2. integracja UI z rejestrowaniem bundle/tokenu oraz HTTP relay;
3. pełne testy offline/online, reorder, duplicate, replay i recovery;
4. revocation + poprawna rotacja/reestablishment wszystkich sesji;
5. szyfrowane załączniki;
6. privacy-safe push;
7. fuzzing parserów i kopert;
8. testy penetracyjne backendu i rate limiting;
9. niezależny audyt kryptograficzny/security review;
10. podpisany reproducible release i procedura aktualizacji.

Nie wolno usuwać tego gate'u tylko po to, aby status brzmiał „production ready”.

## Źródło protokołu

Ω7 nie implementuje własnego Double Ratchet/PQXDH/KDF. Integracja korzysta z libsignal, zgodnie z oficjalnymi specyfikacjami Signal dotyczącymi Double Ratchet i Sesame.
