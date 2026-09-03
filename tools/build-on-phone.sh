#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# Ω7 phone build helper. Intended for Termux or a Linux userland on Android.
# No root is required. Android SDK must already be available to Gradle.

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v java >/dev/null 2>&1; then
  echo "Brak Java. Ω7 0.6 wymaga JDK 21."
  exit 2
fi

JAVA_MAJOR="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
if [ "${JAVA_MAJOR:-0}" -lt 21 ]; then
  echo "Wymagany JDK 21+, wykryto: ${JAVA_MAJOR:-nieznany}."
  exit 2
fi

if command -v gradle >/dev/null 2>&1; then
  GRADLE_BIN="$(command -v gradle)"
else
  CACHE="${HOME}/.omega7/gradle-8.13"
  GRADLE_BIN="$CACHE/bin/gradle"
  if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$CACHE"
    TMP="${TMPDIR:-/data/data/com.termux/files/usr/tmp}/omega7-gradle.zip"
    command -v curl >/dev/null 2>&1 || { echo "Brak curl. Zainstaluj curl w Termuxie."; exit 2; }
    echo "Pobieram oficjalny Gradle 8.13..."
    curl -fL --retry 3 --proto '=https' --tlsv1.2 \
      -o "$TMP" https://services.gradle.org/distributions/gradle-8.13-bin.zip
    rm -rf "$CACHE/unpacked"
    mkdir -p "$CACHE/unpacked"
    unzip -q "$TMP" -d "$CACHE/unpacked"
    mv "$CACHE/unpacked/gradle-8.13" "$CACHE/gradle-root"
    rm -rf "$CACHE/unpacked" "$TMP"
    ln -s "$CACHE/gradle-root" "$CACHE/current" 2>/dev/null || true
    GRADLE_BIN="$CACHE/gradle-root/bin/gradle"
  fi
fi

"$GRADLE_BIN" --version
"$GRADLE_BIN" test
"$GRADLE_BIN" lintDebug
"$GRADLE_BIN" assembleDebug
"$GRADLE_BIN" assembleRelease

printf '\nGotowe.\nDebug APK: %s\nRelease APK: %s\n' \
  "$ROOT/app/build/outputs/apk/debug/app-debug.apk" \
  "$ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"
