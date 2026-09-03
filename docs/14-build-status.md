# Ω7 — status budowania

## Środowisko projektu

- Android Gradle Plugin: 8.13.0
- compileSdk: 36
- targetSdk: 36
- minSdk: 28
- JDK wymagany do budowania: 21

## Weryfikacja wykonana w środowisku roboczym

- struktura projektu: OK
- obecność manifestu i zasobów: OK
- obecność źródeł Kotlin: OK
- ZIP integralny: OK
- skrypt CI obecny: OK

## Weryfikacja niemożliwa w bieżącym środowisku

Bieżące środowisko nie zawiera Android SDK/ADB ani działającego Gradle wrappera z dystrybucją lokalną. Nie oznaczamy więc APK jako zbudowanego ani przetestowanego.

## Budowanie na maszynie deweloperskiej

1. Zainstaluj Android Studio i JDK 21.
2. Otwórz katalog projektu.
3. Pozwól Android Studio pobrać SDK i zależności.
4. Uruchom testy Gradle.
5. Zbuduj `assembleDebug`.
6. Zainstaluj APK na emulatorze lub urządzeniu.
7. Wykonaj checklistę `docs/07-release-checklist.md` oraz `docs/10-red-team.md`.
