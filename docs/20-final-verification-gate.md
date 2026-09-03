# Ω7 — Final Verification Gate

## Kryterium „gotowe”

Aplikacja może zostać oznaczona jako produkcyjna dopiero po przejściu wszystkich poniższych bram:

1. Build release na czystym środowisku.
2. Unit + integration + instrumented tests.
3. Test na minimum dwóch fizycznych urządzeniach Android.
4. Test 7-osobowej grupy.
5. Test parowania i zmiany klucza.
6. Test offline/online i duplikacji zdarzeń.
7. Test panic wipe po 3 błędnych próbach.
8. Test backup/restore i device transfer.
9. Test screenshot/recents/notification leakage.
10. Test fuzzing parserów i envelope.
11. Test rate limitów i replay.
12. Test kompromitacji serwera: serwer nie może odszyfrować wiadomości.
13. Audyt E2EE.
14. Weryfikacja podpisu/release integrity.
15. Przegląd niezależny bezpieczeństwa.

## „Czego jeszcze nie sprawdziliśmy?”

Na dziś odpowiedź brzmi: rzeczywiste zachowanie na urządzeniu, pełny backend, produkcyjne E2EE, wielourządzeniowość, załączniki, powiadomienia, fuzzing instrumentacyjny, recovery i niezależny audyt. Wszystkie te punkty pozostają `REQUIRES VERIFICATION`, a nie „zaliczone”.
