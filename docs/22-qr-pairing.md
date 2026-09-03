# Ω7 — stopniowe dodawanie do 7 urządzeń przez QR

## Cel

Grupa ma dokładnie jeden limit: **maksymalnie 7 urządzeń łącznie z urządzeniem właściciela**. Urządzenia są dodawane pojedynczo, a każde nowe urządzenie musi przejść dwustronne parowanie.

## Przebieg

1. Właściciel wybiera **Urządzenia → Dodaj urządzenie — pokaż QR**.
2. Ω7 generuje krótkotrwałe zaproszenie (5 minut), podpisane nieeksportowalnym kluczem urządzenia właściciela.
3. Nowy telefon wybiera **Dołącz ten telefon przez QR** i skanuje zaproszenie.
4. Nowy telefon weryfikuje podpis zaproszenia i zgodność identyfikatora grupy.
5. Nowy telefon generuje własne, podpisane żądanie parowania i pokazuje drugi kod QR.
6. Właściciel skanuje drugi QR.
7. Ω7 weryfikuje podpis nowego urządzenia, zgodność grupy i zgodność z aktywnym zaproszeniem.
8. Dopiero po świadomym zatwierdzeniu urządzenie otrzymuje stan `VERIFIED`.
9. Limit jest sprawdzany przed wygenerowaniem kolejnego zaproszenia.

## Zasady bezpieczeństwa

- QR nie zawiera prywatnych kluczy ani treści wiadomości.
- Zaproszenie ma krótki czas życia.
- Żądanie dołączenia jest podpisane przez nowe urządzenie.
- Właściciel musi jawnie zatwierdzić nowe urządzenie.
- Fingerprint klucza urządzenia jest zapisywany w rejestrze zaufania.
- Panic wipe usuwa tożsamość urządzenia, rejestr zaufania i oczekujące zaproszenia.
- QR nie jest traktowany jako dowód E2EE. Jest mechanizmem bootstrappingu tożsamości i autoryzacji parowania.

## Stan produkcyjny

Warstwa QR i lokalnego zaufania jest zaimplementowana. Nadal wymagają integracji i testów produkcyjnych:

- serwerowe jednorazowe zużycie zaproszenia,
- wymiana kluczy E2EE,
- synchronizacja członkostwa między urządzeniami,
- usuwanie/revokacja urządzenia po stronie serwera,
- niezależny audyt protokołu E2EE.
