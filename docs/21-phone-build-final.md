# Ω7 — Budowanie bezpośrednio na telefonie

## Cel

Projekt jest przygotowany do budowania w Termuxie/Linux userland na Androidzie bez roota.

## Wymagania

- JDK 21+
- Gradle 8.13 (skrypt może pobrać oficjalną dystrybucję)
- Android SDK platform 36
- Android Build Tools 36.0.0
- dostęp do sieci podczas pierwszego pobrania Gradle i zależności

## Procedura

```sh
cd omega7-messenger
chmod +x tools/phone-doctor.sh tools/build-on-phone.sh
./tools/phone-doctor.sh
./tools/build-on-phone.sh
```

Skrypt wykonuje kolejno:

1. kontrolę JDK;
2. testy jednostkowe;
3. lint;
4. build debug;
5. build release unsigned.

## Ważne

Brak podpisu produkcyjnego jest celowy. Klucz podpisujący nie może być umieszczony w repozytorium ani w dostarczanym ZIP-ie.

Przed publikacją należy użyć prywatnego keystore i zweryfikować artefakt podpisany w kontrolowanym środowisku.
