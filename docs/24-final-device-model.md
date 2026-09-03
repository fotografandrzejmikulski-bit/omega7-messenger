# Ω7 — finalny model 7 telefonów

## Zasada

Grupa ma maksymalnie 7 uczestników. Urządzenia są dodawane pojedynczo — nie wymagamy posiadania siedmiu telefonów jednocześnie.

## Progresywne dodawanie 1 → 7

Testowanie i wdrażanie urządzeń wykonujemy etapami:

1. **Telefon 1** — właściciel grupy; stan bazowy.
2. **Telefon 2** — parowanie, weryfikacja fingerprintu, synchronizacja i odwołanie testowe.
3. **Telefon 3** — ponowna weryfikacja limitu oraz komunikacja 3 urządzeń.
4. **Telefon 4** — test wielourządzeniowego stanu zaufania.
5. **Telefon 5** — test dalszego skalowania bez zmiany modelu bezpieczeństwa.
6. **Telefon 6** — test przedostatniego slotu.
7. **Telefon 7** — pełny stan grupy; właściciel + 6 urządzeń zaufanych.
8. **Telefon 8** — test negatywny: żądanie musi zostać odrzucone, ponieważ limit został osiągnięty.

Po każdym kroku poprzednie urządzenia pozostają aktywne. Dodanie kolejnego urządzenia nie resetuje ani nie zastępuje wcześniejszych urządzeń.

## Procedura parowania

1. Właściciel generuje krótkotrwałe zaproszenie QR.
2. Nowy telefon skanuje QR.
3. Nowy telefon przedstawia własną tożsamość podpisaną kluczem z Android Keystore.
4. Właściciel skanuje odpowiedź QR.
5. Właściciel widzi nazwę urządzenia i fingerprint.
6. Dopiero po ręcznym zatwierdzeniu urządzenie otrzymuje stan VERIFIED.
7. Po osiągnięciu siedmiu urządzeń dalsze zaproszenia są blokowane.

QR jest kanałem bootstrapu. Nie jest kanałem przesyłania plaintextu ani prywatnych kluczy.

## Warunki bezpieczeństwa każdego kroku

Każde nowe urządzenie musi przejść tę samą ścieżkę zaufania. Nie wolno wprowadzać specjalnego trybu „szybkiego dodawania” dla urządzeń 3–7.

Limit jest invariantem domenowym:

**1 właściciel + maksymalnie 6 urządzeń zaufanych = maksymalnie 7 urządzeń.**

## Usuwanie urządzenia

Odwołane urządzenie przechodzi do REVOKED i nie może być używane do dalszej synchronizacji. Przy produkcyjnej warstwie E2EE usunięcie uczestnika musi wywołać rotację materiału grupowego.

## Bezpieczeństwo

- zaproszenia mają krótki TTL;
- zaproszenia są podpisywane;
- żądanie urządzenia jest podpisywane;
- fingerprint służy do ręcznej weryfikacji poza kanałem QR;
- panic wipe usuwa lokalną tożsamość i rejestr zaufania;
- limit 7 jest wymuszany także w domenie, nie tylko w UI;
- test 8. urządzenia jest obowiązkowym testem negatywnym.

## Status weryfikacji

Pełne przejście 1 → 7 → odrzucenie 8 wymaga fizycznych urządzeń i testów integracyjnych. Dopóki nie zostaną wykonane, wynik oznaczamy jako **REQUIRES VERIFICATION**, a nie jako zweryfikowany produkcyjnie.
