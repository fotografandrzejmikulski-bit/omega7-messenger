# Budowanie Ω7 bezpośrednio z telefonu

Telefon może służyć jako urządzenie testowe, a przy użyciu mobilnego środowiska Linux/IDE także jako środowisko budowania. Sam Android Studio nie jest aplikacją na Androida; oficjalnie wspierany workflow budowania korzysta z Gradle, a do wdrożenia na fizycznym urządzeniu można użyć APK/ADB.

## Najbezpieczniejsza ścieżka
- Android Studio na komputerze lub środowisko chmurowe/CI: synchronizacja Gradle → testy → APK.
- Telefon: instalacja APK i testy funkcjonalne.

## Jeśli budujesz na telefonie
Użyj Termux + środowiska Linux (np. proot-distro Debian/Ubuntu) z JDK i Gradle. Następnie skopiuj projekt do środowiska Linux i uruchom:

```text
cd omega7-messenger
gradle test
gradle assembleDebug
```

Skrypt `tools/build-on-phone.sh` wykonuje te dwa zadania i wypisuje ścieżkę do APK.

## Test urządzenia
Po zbudowaniu debug APK można zainstalować go na urządzeniu, np. przez ADB, jeśli debugowanie USB jest skonfigurowane.

## Krytyczne ograniczenie
Nie należy traktować samego udanego builda jako dowodu bezpieczeństwa. Przed produkcją wymagane są testy na realnym urządzeniu, testy instrumentacyjne, testy backup/restore, testy blokady sesji, test panic wipe, testy sieciowe oraz niezależny audyt E2EE.
