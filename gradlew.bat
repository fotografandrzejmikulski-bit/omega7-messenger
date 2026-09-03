@echo off
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle is not installed. Open the project in Android Studio and sync Gradle files.
  exit /b 1
)
gradle -p "%~dp0" %*
