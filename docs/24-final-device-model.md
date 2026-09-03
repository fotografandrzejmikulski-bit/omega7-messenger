# Ω7 — finalny model 7 telefonów

## Zasada

Grupa ma maksymalnie 7 uczestników. Urządzenia są dodawane pojedynczo.

1. Właściciel generuje krótkotrwałe zaproszenie QR.
2. Nowy telefon skanuje QR.
3. Nowy telefon przedstawia własną tożsamość podpisaną kluczem z Android Keystore.
4. Właściciel skanuje odpowiedź QR.
5. Właściciel widzi nazwę urządzenia i fingerprint.
6. Dopiero po ręcznym zatwierdzeniu urządzenie otrzymuje stan VERIFIED.
7. Po osiągnięciu siedmiu urządzeń dalsze zaproszenia są blokowane.

QR jest kanałem bootstrapu. Nie jest kanałem przesyłania plaintextu ani prywatnych kluczy.

## Usuwanie urządzenia

Odwołane urządzenie przechodzi do REVOKED i nie może być używane do dalszej synchronizacji. Przy produkcyjnej warstwie E2EE usunięcie uczestnika musi wywołać rotację materiału grupowego.

## Bezpieczeństwo

- zaproszenia mają krótki TTL;
- zaproszenia są podpisywane;
- żądanie urządzenia jest podpisywane;
- fingerprint służy do ręcznej weryfikacji poza kanałem QR;
- panic wipe usuwa lokalną tożsamość i rejestr zaufania;
- limit 7 jest wymuszany także w domenie, nie tylko w UI.
