# Ω7 — Model zagrożeń

## Chronione aktywa
1. Klucz urządzenia i klucze E2EE.
2. Historia wiadomości i załączniki.
3. Tożsamości uczestników i stan weryfikacji urządzeń.
4. Kod dostępu i stan sesji.
5. Tokeny transportowe i kolejka offline.

## Przeciwnicy
- A: osoba mająca fizyczny dostęp do odblokowanego telefonu.
- B: osoba próbująca odgadnąć kod aplikacji.
- C: atakujący sieciowy.
- D: złośliwy lub przejęty serwer.
- E: przejęte konto/urządzenie uczestnika.
- F: złośliwa aplikacja próbująca wykorzystać dane Ω7.

## Założenia
- System Android i jego mechanizmy bezpieczeństwa nie są traktowane jako element E2EE między uczestnikami.
- Użytkownik musi chronić kod dostępu urządzenia.
- E2EE chroni treść przed serwerem, ale nie może ochronić treści ujawnionej przez przejęte urządzenie końcowe.

## Kluczowe zabezpieczenia
- Keystore dla kluczy lokalnych.
- AES-GCM dla danych lokalnych.
- TLS dla transportu.
- E2EE przez sprawdzony protokół i bibliotekę.
- Weryfikacja kluczy urządzeń.
- Unieważnianie urządzeń.
- Limit prób i panic wipe.
- Brak sekretów w logach.

## Pozostałe ryzyka
Największym ryzykiem przed wydaniem produkcyjnym jest brak zweryfikowanej, zewnętrznie audytowanej implementacji protokołu E2EE oraz brak rzeczywistego backendu produkcyjnego. Nie należy oznaczać systemu jako produkcyjnie bezpiecznego przed ich ukończeniem.
