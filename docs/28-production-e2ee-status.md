# Ω7 — status produkcyjnego E2EE

## Stan implementacji

Gałąź `omega7-production-e2ee` zawiera warstwę integracyjną z oficjalnym `libsignal` 0.100.0 oraz backend blind-relay oparty o PostgreSQL.

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
- Twardy limit 7 urządzeń po stronie klienta i serwera.
- Backend z PostgreSQL, atomowym zużyciem jednorazowego zaproszenia, blokadą wiersza grupy przy kontroli limitu, idempotency key i monotonicznym cursorem.
- Serwer nie posiada kodu odszyfrowującego wiadomości.
- `usesCleartextTraffic=false` i uprawnienie INTERNET po stronie Androida.
- QR provisioning jest spięty z warstwą produkcyjną: owner tworzy serwerowe zaproszenie, joiner tworzy podpisany request z bundlem Signal, owner tworzy podpisane approval, joiner rejestruje bundle i otrzymuje token relay, a owner może następnie pobrać bundle i ustanowić sesję Signal przed oznaczeniem urządzenia jako zaufanego.
- Token relay oraz endpoint są przechowywane lokalnie w szyfrowanym magazynie konfiguracji.
- Approval jest samowystarczalny: zawiera dane potrzebne do odtworzenia i ponownej weryfikacji dokładnego requestu urządzenia dołączającego.
- Trwała szyfrowana kolejka outbound została dodana: szyfrogram jest zapisywany po jednorazowym wykonaniu E2EE, a ponowienia wykorzystują ten sam szyfrogram i idempotency key, zamiast ponownie wykonywać operację ratchet.
- Retry outbound ma twardy limit i rosnące opóźnienia; po wyczerpaniu budżetu wpis pozostaje w stanie oczekującym i nie jest automatycznie ponawiany bez nowej decyzji warstwy wyższej.
- CI wykonuje testy jednostkowe, lint Androida i build relay; generowanie APK pozostaje wyłączone do czasu zamknięcia gate'u produkcyjnego.

## Aktualny gate — nadal NIE production ready

Kod nie może być jeszcze oznaczony jako **niezależnie zweryfikowane produkcyjne E2EE**. Nadal wymagane są:

1. rzeczywisty test dwóch i siedmiu fizycznych urządzeń;
2. pełne spięcie wysyłania i odbierania wiadomości z trwałą kolejką, relay i `SessionCipher` — komponent kolejki i usługi wysyłającej są już zaimplementowane, ale wymagają integracji z głównym przepływem UI/workerem;
3. obsługa wyczerpania one-time prekeys oraz bezpieczne uzupełnianie ich po stronie urządzenia;
4. pełny Sesame-style lifecycle sesji: active/inactive, retry, orphaned state, bounded resend i recovery po utracie stanu;
5. revocation + poprawna re-key/reestablishment wszystkich wymaganych sesji;
6. szyfrowane załączniki;
7. privacy-safe push;
8. fuzzing parserów QR, JSON i kopert E2EE;
9. testy penetracyjne backendu, rate limiting i abuse controls;
10. testy współbieżności rejestracji, invite consumption, prekey consumption i wysyłki;
11. backup/recovery/device replacement oraz jawna polityka utraty urządzenia;
12. niezależny audyt kryptograficzny/security review;
13. podpisany reproducible release, SBOM, integralność artefaktu i procedura aktualizacji.

Nie wolno usuwać tego gate'u tylko po to, aby status brzmiał „production ready”.

## Źródło modelu sesji

Sesame jest modelem zarządzania asynchronicznymi sesjami wielourządzeniowymi: każde urządzenie utrzymuje stan sesji per urządzenie zdalne, obsługuje dodawanie/usuwanie urządzeń oraz sytuacje utraty, opóźnienia, duplikacji i zmiany klucza tożsamości. Ω7 korzysta z tych zasad na poziomie architektury, ale nie jest implementacją Signal ani oficjalnym klientem Signal. citeturn0search0
