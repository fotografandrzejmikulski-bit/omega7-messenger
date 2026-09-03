# Ω7 — OWASP MASVS/MASWE Gap Analysis

Stan odniesienia: OWASP MASVS oraz MASWE 1.0.0 (2026).

## STORAGE

- [x] Dane wiadomości szyfrowane w storage aplikacji.
- [x] Klucz lokalny w Android Keystore.
- [x] Rejestr zaufania szyfrowany.
- [x] Backup/transfer ograniczony konfiguracją aplikacji.
- [ ] Instrumentacyjna weryfikacja wycieku przez wszystkie ścieżki platformowe.

## CRYPTO

- [x] AES-GCM bez własnego algorytmu.
- [x] SecureRandom dla IV.
- [x] Separacja klucza magazynu i peppera uwierzytelniania.
- [ ] Produkcyjne E2EE i niezależny audyt.

## AUTH

- [x] Lokalna autoryzacja.
- [x] Trwały limit prób.
- [x] Silna biometria jako dodatkowa ścieżka odblokowania.
- [ ] Zdalna tożsamość i autoryzacja serwerowa.

## NETWORK

- [x] Cleartext HTTP zablokowany.
- [x] Brak możliwości wysłania przez transport bez skonfigurowanego E2EE.
- [ ] Produkcyjny TLS endpoint, pinning/attestation decision, replay protection i backend.

## PLATFORM

- [x] FLAG_SECURE.
- [x] Minimalne uprawnienia w manifeście.
- [ ] Testy linków/deep links, notification surfaces, accessibility services i overlay attacks na fizycznych urządzeniach.

## CODE / RESILIENCE

- [x] Walidacja wejść domenowych.
- [ ] Release minification/shrinking + reproducible build verification.
- [ ] Tamper/root/emulator threat policy po określeniu wymagań produktu.

## PRIVACY

- [x] Minimalny zakres lokalnych danych.
- [x] Brak plaintextu w transportowej warstwie aplikacji.
- [ ] Pełny przegląd telemetrii, crash reports i powiadomień po uruchomieniu backendu.
