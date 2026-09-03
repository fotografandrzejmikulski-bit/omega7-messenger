#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$APP_HOME" "$@"
fi
echo "Brak lokalnego Gradle. Otwórz projekt w Android Studio albo zainstaluj Gradle zgodny z gradle-wrapper.properties." >&2
exit 127
