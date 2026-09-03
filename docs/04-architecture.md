# Ω7 — Architektura

```text
UI (PL)
  ↓
Application / ViewModel
  ↓
Domain
  ├── ConversationService
  ├── MessageService
  └── DeviceTrustService
  ↓
Data
  ├── EncryptedLocalStore
  ├── MessageRepository
  └── DeviceRepository
  ↓
Cryptography
  ├── LocalKeyManager (Android Keystore)
  ├── LocalCipher (AES-GCM)
  └── E2eeEngine (adapter do audytowanej biblioteki)
  ↓
Network
  ├── AuthenticatedTransport (TLS)
  └── SyncQueue
```

Serwer docelowy przechowuje wyłącznie minimum wymagane do routingu, synchronizacji i zarządzania członkostwem. Treść wiadomości pozostaje szyfrowana E2EE.

## Granice zaufania
- Telefon: zaufany tylko w granicach bezpieczeństwa Androida i stanu blokady aplikacji.
- Serwer: niezaufany wobec treści wiadomości.
- Uczestnik: zaufany wyłącznie jako właściciel własnego urządzenia; urządzenia pozostałych osób są weryfikowane.
