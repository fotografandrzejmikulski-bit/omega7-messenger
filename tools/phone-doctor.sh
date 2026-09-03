#!/data/data/com.termux/files/usr/bin/bash
set -u
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
echo "Ω7 PHONE DOCTOR"
echo "Projekt: $ROOT"
command -v java >/dev/null && java -version 2>&1 | head -1 || echo "FAIL: Java"
command -v unzip >/dev/null && echo "OK: unzip" || echo "FAIL: unzip"
command -v curl >/dev/null && echo "OK: curl" || echo "WARN: curl"
command -v gradle >/dev/null && gradle --version | head -3 || echo "INFO: Gradle zostanie pobrany przez build-on-phone.sh"
if [ -n "${ANDROID_HOME:-}" ]; then echo "ANDROID_HOME=$ANDROID_HOME"; else echo "WARN: ANDROID_HOME nie ustawione"; fi
if [ -n "${ANDROID_SDK_ROOT:-}" ]; then echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"; fi
[ -d "${ANDROID_HOME:-/nonexistent}/platforms/android-36" ] && echo "OK: Android SDK 36" || echo "WARN: Android SDK 36 niewykryty"
[ -d "${ANDROID_HOME:-/nonexistent}/build-tools/36.0.0" ] && echo "OK: build-tools 36.0.0" || echo "WARN: build-tools 36.0.0 niewykryty"
echo "Jeżeli środowisko jest kompletne: ./tools/build-on-phone.sh"
