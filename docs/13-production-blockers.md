# Ω7 — blokady wydania produkcyjnego

Ten dokument jest bramką wydania. Kod może być rozwijany przed zamknięciem wszystkich punktów, ale żaden punkt nieweryfikowany nie może być przedstawiany jako gotowy produkcyjnie.

## P0 — wymagane przed publicznym wydaniem

1. **E2EE** — integracja libsignal 0.100.0, trwały magazyn stanu, sesje urządzenie↔urządzenie i koperty wieloodbiorcze są zaimplementowane. Nadal REQUIRES VERIFICATION: testy dwóch rzeczywistych urządzeń, pełny przepływ offline/online oraz niezależny audyt.
2. **Relay** — backend PostgreSQL przechowuje wyłącznie zaszyfrowane koperty i minimalne metadane. Implementacja endpointów istnieje. REQUIRES VERIFICATION: wdrożenie produkcyjne, TLS reverse proxy, HA/backup/restore i testy awarii.
3. **Tożsamość i rotacja** — fingerprint oraz blokada zmiany identity key są obecne. REQUIRES VERIFICATION: pełny revoke + rekey grupy i przepływ zmiany urządzenia.
4. **Replay/downgrade/idempotencja** — idempotency key, monotoniczny cursor i odrzucanie niezgodnej wersji koperty są obecne. REQUIRES VERIFICATION: fuzzing i testy adversarialne protokołu/synchronizacji.
5. **Push bez wycieku** — NIEZAIMPLEMENTOWANE.
6. **Załączniki E2EE** — NIEZAIMPLEMENTOWANE.
7. **Fizyczne Androidy** — NIEZWERYFIKOWANE w tym środowisku.
8. **Fuzzing** — NIEZWERYFIKOWANY.
9. **Niezależny audyt bezpieczeństwa** — NIEWYKONANY.
10. **Reproducible release / signing / SBOM / update procedure** — NIEZAMKNIĘTE.

## P1 — wymagane przed wersją stabilną

- backup/recovery z jednoznacznym modelem zaufania;
- obsługa zmiany urządzenia i wycofania urządzenia;
- obsługa konfliktów offline;
- rate limiting i ochrona przed abuse;
- telemetryka bez wycieku danych treściowych;
- accessibility review;
- testy wydajnościowe.

## Aktualny status

Gałąź `omega7-production-e2ee` zawiera rzeczywistą integrację z libsignal oraz blind relay. Nie jest to jeszcze certyfikowany ani niezależnie audytowany produkt produkcyjny. Oficjalny libsignal 0.100.0 implementuje Signal Protocol/Double Ratchet i jest używany jako biblioteka kryptograficzna zamiast własnej implementacji kryptografii.

## Zakaz fałszywego statusu

Dopóki powyższe REQUIRES VERIFICATION/P0 nie są zamknięte, aplikacja nie może być opisywana jako niezależnie zweryfikowany produkcyjny komunikator E2EE.
