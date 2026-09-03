# Ω7 — model siedmiu urządzeń

## Invariant

`1 właściciel + maksymalnie 6 urządzeń zaufanych = maksymalnie 7`

Nie istnieje ścieżka UI, która pozwala przekroczyć limit.

## Enrollment states

`INVITE_CREATED → INVITE_SCANNED → DEVICE_REQUEST_CREATED → OWNER_REVIEW → VERIFIED`

Błędy powodują `REJECTED` albo `EXPIRED`. Nie wolno automatycznie przejść do `VERIFIED`.

## Identity

Każda instalacja posiada osobny identyfikator oraz nieeksportowalny klucz podpisujący w Android Keystore. Fingerprint klucza jest prezentowany właścicielowi przed zatwierdzeniem.

## Revocation

Usunięcie urządzenia musi oznaczać je jako `REVOKED` i w docelowym backendzie unieważnić jego dostęp do grupy. Po revokacji wymagany jest rekey grupy E2EE.

## Recovery

Panic wipe niszczy lokalną tożsamość urządzenia, klucze, rejestr zaufania i oczekujące zaproszenia. Odzyskanie urządzenia musi rozpocząć nowy proces enrollmentu.

## Production rule

Samo QR nie dowodzi bezpieczeństwa treści wiadomości. Połączenie musi używać właściwego protokołu E2EE, a serwer ma widzieć wyłącznie minimum metadanych i ciphertext.
