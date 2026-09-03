# Bramka E2EE

Ω7 nie implementuje własnego protokołu szyfrowania end-to-end. Interfejs `E2eeEngine` jest granicą architektoniczną.

Przed produkcją należy podłączyć aktualnie utrzymywaną, odpowiednio licencjonowaną i niezależnie zweryfikowaną implementację protokołu. Oficjalne repozytorium Signal `libsignal` opisuje bibliotekę jako używaną przez oficjalne klienty Signal, ale jednocześnie zaznacza, że użycie poza Signal jest niewspierane i że API może się zmieniać. Z tego powodu nie dodajemy jej do projektu bez osobnej analizy licencyjnej, kompatybilności Android/JNI, wersjonowania i audytu.

Brama akceptacyjna:

1. wybrana biblioteka ma potwierdzoną kompatybilność z docelowym buildem Android;
2. licencja jest zaakceptowana dla sposobu dystrybucji Ω7;
3. stan protokołu jest trwale przechowywany i odporny na rollback;
4. obsłużono rotację kluczy, nowe urządzenia, usunięcie urządzenia i zmianę klucza;
5. wiadomości grupowe mają jednoznaczną autoryzację członkostwa;
6. serwer nie otrzymuje plaintextu;
7. testy negatywne obejmują replay, downgrade, uszkodzone ciphertexty, zmianę członkostwa i kompromitację urządzenia;
8. wykonano niezależny przegląd kryptograficzny.
