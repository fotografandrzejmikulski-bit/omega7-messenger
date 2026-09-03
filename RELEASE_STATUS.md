# Ω7 Messenger — RELEASE STATUS 0.8.1

## Stan

Wersja 0.8.1 jest finalnym pakietem źródłowym do dalszego build/release.

### Utwardzone
- Polski interfejs.
- AES-256-GCM dla danych lokalnych.
- Android Keystore dla kluczy lokalnych, HMAC i tożsamości urządzenia.
- Kod dostępu z trwałym licznikiem 3 błędów i panic wipe stanu aplikacji.
- Biometria, FLAG_SECURE, blokada po zejściu aplikacji w tło.
- Backup aplikacji wyłączony.
- Fail-closed: brak wysyłania sieciowego bez skonfigurowanego E2EE.
- Dwustronne parowanie QR z podpisanymi tożsamościami i akceptacją właściciela.
- Limit maksymalnie 7 urządzeń.
- Rejestr zaufanych urządzeń szyfrowany lokalnie.
- Kolejka zaproszeń parowania również szyfrowana lokalnie.
- CI GitHub Actions przygotowane do testów, lint i budowania APK.

## Brak podstaw do deklaracji produkcyjnego E2EE
Nie deklarujemy jeszcze produkcyjnego E2EE, ponieważ wymaga ono rzeczywiście zintegrowanego i zweryfikowanego protokołu E2EE, synchronizacji kluczy/sesji oraz testów na urządzeniach. Warstwa transportowa pozostaje zamknięta do czasu spełnienia tych warunków.

## GitHub
Repozytorium projektu jest prywatne i dostępne pod właścicielem `fotografandrzejmikulski-bit/omega7-messenger`. Import źródeł do repozytorium jest wykonywany etapami. Nie należy traktować samego repozytorium jako dowodu przejścia testów CI ani audytu bezpieczeństwa.

## Weryfikacja środowiskowa
W bieżącym środowisku nie ma Android SDK, ADB ani pełnego Gradle Wrappera, dlatego nie należy twierdzić, że APK zostało tutaj skompilowane lub uruchomione na fizycznym telefonie. CI jest przygotowane do wykonania tych kroków na runnerze GitHub.
