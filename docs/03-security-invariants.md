# Ω7 — Niezmienniki bezpieczeństwa

1. Kod dostępu nigdy nie jest zapisywany wprost.
2. Klucz lokalnego magazynu nigdy nie trafia do zwykłego pliku.
3. Dane wrażliwe nie są wysyłane w logach diagnostycznych.
4. Wiadomość opuszczająca aplikację musi przejść przez warstwę E2EE przed transportem.
5. Serwer nie może być wymagany do odszyfrowania treści wiadomości.
6. Niezweryfikowana zmiana klucza urządzenia jest jawnie sygnalizowana użytkownikowi.
7. Unieważnione urządzenie nie może otrzymywać nowych kluczy sesyjnych.
8. Po panic wipe lokalny materiał uwierzytelniający i lokalne dane Ω7 są usuwane.
9. Aplikacja nie wykonuje fabrycznego resetu zwykłego urządzenia.
10. Każdy błąd kryptograficzny powoduje bezpieczne zatrzymanie operacji, a nie kontynuowanie na niespójnym stanie.
