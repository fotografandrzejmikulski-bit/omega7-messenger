# Polityka bezpieczeństwa Ω7

## Zasada nadrzędna
Ω7 nie może przedstawiać się jako komunikator E2EE o zweryfikowanym bezpieczeństwie, dopóki implementacja protokołu i infrastruktura nie przejdą niezależnego audytu.

## Zgłaszanie problemów
W przypadku znalezienia luki nie publikuj szczegółów exploita przed przygotowaniem poprawki. Wydanie produkcyjne powinno posiadać dedykowany kanał odpowiedzialnego ujawniania.

## Zakazane praktyki
- własny algorytm szyfrowania;
- przechowywanie kluczy w plaintext;
- logowanie treści wiadomości lub kodów;
- umieszczanie sekretów w repozytorium;
- poleganie na serwerze jako zaufanym źródle treści E2EE;
- obiecywanie całkowitego bezpieczeństwa urządzenia końcowego.
