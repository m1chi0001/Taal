# Taal — Wirtualny Dziekanat WSEI

[![Version](https://img.shields.io/badge/Release-v1.0.0-brightgreen)](https://github.com/m1chi0001/Taal/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Compile SDK](https://img.shields.io/badge/Compile%20SDK-37%20%7C%20Target%20SDK%2035-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203%20Expressive%201.5.0-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Taal** to nowoczesna, w pełni natywna aplikacja mobilna na system Android stworzona dla studentów **Uniwersytetu WSEI w Lublinie**.
> Zapewnia bezpośredni, szybki i działający także offline dostęp do **Wirtualnego Dziekanatu** — planu zajęć, ocen, przedmiotów, finansów, stypendiów, ogłoszeń i profilu studenta — w stylistyce **Material 3 Expressive**.

---

## Pobieranie i instalacja

1. Przejdź do zakładki [**Releases (Najnowsze wydanie v1.0.0)**](https://github.com/m1chi0001/Taal/releases/latest).
2. Pobierz podpisany plik instalacyjny **`Taal-v1.0.0.apk`** i otwórz go na telefonie lub tablecie z Androidem 8.0+.
3. Zaloguj się swoimi danymi do Wirtualnego Dziekanatu WSEI.
4. Kolejne aktualizacje aplikacja wykryje automatycznie i pozwoli pobrać oraz zainstalować jednym dotknięciem bezpośrednio w aplikacji.

> **Wskazówka dla użytkowników wersji starszych niż `v0.7.0`:** wersje `v0.1`–`v0.6.1` były podpisywane starszym kluczem testowym. Przed instalacją `v1.0.0` odinstaluj najpierw wersję `0.6.x` lub starszą. Aktualizacja z wersji `v0.7.0+` instaluje się bezpośrednio bez utraty danych.

---

## Najważniejsze funkcje

### Ekran Start i Plan zajęć
- **Inteligentny pulpit Start:** dynamiczna karta bieżących zajęć z odliczaniem czasu, informacją o przerwie i kolejnych zajęciach, a po zakończeniu dnia lub w dni wolne — czytelna karta najbliższego dnia zajęć lub zjazdu z odliczaniem i przejściem jednym przyciskiem.
- **Kalendarz tygodnia i miesiąca:** szybkie przełączanie między paskiem tygodnia (Pon–Ndz) a pełną siatką miesiąca z oznaczonymi dniami zjazdów i zajęć oraz filtrem **Mój plan / Plan grupy**.
- **Synchronizacja z kalendarzem urządzenia:** idempotentny eksport planu zajęć do wybranego kalendarza (np. Kalendarz Google) bez tworzenia duplikatów przy kolejnych synchronizacjach.

### Powiadomienia na żywo (Live Notifications)
- **Śledzenie trwających zajęć i przerw:** trwałe powiadomienie z paskiem postępu (obsługujące `ProgressStyle` na Androidzie 16, ekran blokady oraz Always-On Display) pokazuje aktualny przedmiot, salę, prowadzącego i pozostały czas, a w trakcie przerwy — odliczanie do kolejnego bloku zajęć.

### Przedmioty, Oceny i ECTS
- **Podział na semestry:** przejrzyste zestawienie przedmiotów, ocen końcowych i cząstkowych, form zaliczenia, prowadzących oraz zdobytych punktów ECTS osobno dla każdego semestru.

### Finanse i Stypendia
- **Płatności i czesne:** czytelne uporządkowanie rat (bieżące, przyszłe, opłacone), szybkie przejście do płatności online oraz kopiowanie indywidualnego numeru subkonta bankowego (oznaczanego w schowku systemowym jako dane poufne).
- **Wnioski stypendialne:** rzeczywisty status wniosków z dziekanatu, decyzje stypendialne oraz harmonogram wypłat transz.

### Ogłoszenia i Profil studenta
- **Ogłoszenia z załącznikami:** odczyt komunikatów dziekanatu z oznaczaniem przeczytanych wiadomości oraz bezpośrednim otwieraniem załączników (obsługa plików binarnych, odpowiedzi Base64 i przekierowań).
- **Profil studenta:** portret legitymacyjny w proporcjach 4:5 (ELS), karta **Tok nauki** (rok studiów, semestr, tryb studiów), lista grup zajęciowych, średnie ocen oraz zmiana hasła do dziekanatu.

### Personalizacja, dostępność i urządzenia
- **Material 3 Expressive i 6 palet barw:** wybór motywu (**Systemowy / Jasny / Ciemny**) oraz 6 palet (**Taal**, **Dynamiczna Material You**, **Kobalt**, **Terakota**, **Śliwka**, **Ocean**) z automatyczną gwarancją kontrastu **WCAG ≥ 4.5:1** oraz czcionką zmienną **Google Sans Flex** działającą offline.
- **3 języki interfejsu:** pełna obsługa języka **polskiego**, **angielskiego** i **ukraińskiego** z przełącznikiem wewnątrz aplikacji.
- **Tablety i składane smartfony:** responsywna boczna szyna nawigacji (`WideNavigationRail`) oraz układy dwupanelowe świadome zawiasu ekranu na tabletach i rozłożonych smartfonach składanych, a także pełna obsługa systemowego gestu **Predictive Back** (Android 13–16).
- **Ochrona prywatności i bezpieczeństwo:** poświadczenia szyfrowane sprzętowo w **Android Keystore** (`AES-256-GCM`) oraz tryb **Ukryj dane** maskujący wrażliwe dane studenta, kwoty i nazwiska prowadzących w miejscach publicznych.

---

## Architektura projektu

Projekt jest zorganizowany zgodnie z zasadami **Clean Architecture** (`domain` ← `data`, `domain` ← `ui`) oraz jednokierunkowym przepływem danych (**UDF / MVVM + StateFlow**):

```text
app/src/main/java/pl/lublin/wsei/plan/
├── TaalApplication.kt             # Inicjalizacja Hilt, WorkManager i sprawdzania aktualizacji
├── MainActivity.kt                # Edge-to-Edge, uprawnienia i powiadomienia na żywo
├── di/
│   ├── NetworkAndDatabaseModule.kt # Klient Ktor/OkHttp, JSON i baza danych Room
│   └── RepositoryModule.kt        # Wiązania interfejsów domenowych (@Binds @Singleton)
├── domain/
│   ├── attachments/               # Kontrakty otwierania załączników ogłoszeń
│   ├── calendar/                  # Kontrakty synchronizacji z kalendarzem urządzenia
│   ├── model/                     # Modele domenowe, parsery grup i toku studiów
│   ├── repository/                # Interfejs ScheduleRepository
│   ├── schedule/                  # Wyznaczanie aktywnego dnia i najbliższego zjazdu
│   └── update/                    # Modele wydań i komparator wersji semantycznych
├── data/
│   ├── attachments/               # Klasyfikacja, dekodowanie i otwieranie załączników
│   ├── calendar/                  # Eksport zajęć do dostawcy CalendarContract
│   ├── local/                     # Room DAO, DataStore, Keystore AES-GCM, zasoby lokalizowane
│   ├── remote/
│   │   ├── api/                   # Klient REST APR, ArpSessionInterceptor i serializatory
│   │   └── html/                  # Zapasowy parser HTML WebForms planu zajęć
│   ├── repository/                # ScheduleRepositoryImpl (Offline-First)
│   └── update/                    # GitHubUpdateRepository, UpdateCheckWorker, UpdateManager
├── l10n/                          # Formatowanie dat, kwot i etykiet zależnie od języka
├── service/                       # LiveClassNotificationService, budziki zajęć i WorkManager
└── ui/
    ├── adaptive/                  # Klasy rozmiaru okna, obsługa zawiasu i układy dwupanelowe
    ├── components/                # Wspólne komponenty Material 3 Expressive, Splash, Welcome
    ├── finances/                  # Ekran finansów, płatności online i stypendiów
    ├── home/                      # Ekran Start (pulpit studenta)
    ├── icons/                     # Spójny zestaw ikon wektorowych i emblemat WSEI × Taal
    ├── login/                     # Ekran logowania
    ├── more/                      # Sekcja Więcej, Ogłoszenia, Ustawienia i O aplikacji
    ├── navigation/                # WseiAppShell, WseiNavigationGraph, pasek i szyna nawigacji
    ├── profile/                   # Profil studenta, tok studiów, grupy i zmiana hasła
    ├── schedule/                  # ScheduleScreen, ScheduleViewModel, ScheduleUiState
    ├── subjects/                  # Przedmioty, oceny i podsumowanie ECTS
    ├── theme/                     # Theme.kt, palety barw, kontrola kontrastu WCAG i tokeny ruchu
    └── update/                    # UpdateViewModel, okno aktualizacji, arkusz zmian i Markdown
```

---

## Stos technologiczny

- **Język i narzędzia budowania:** Kotlin `2.2.21`, Android Gradle Plugin `9.1.1`, Gradle `9.3.1`, KSP `2.3.4`
- **Interfejs użytkownika:** Jetpack Compose BOM `2026.11.01`, Material 3 Expressive `1.5.0`, Material 3 Adaptive `1.3.0`, Navigation Compose `2.9.6`
- **Warstwa sieciowa:** Ktor Client `3.1.0` (`OkHttp`), `kotlinx.serialization`, Jsoup `1.18.3` (tryb zapasowy HTML)
- **Pamięć lokalna i bezpieczeństwo:** Room `2.8.4`, Jetpack DataStore `1.1.3`, Android Keystore (`AES-256-GCM`)
- **Wstrzykiwanie zależności i praca w tle:** Dagger Hilt `2.59.2`, WorkManager `2.10.0`
- **SDK:** `minSdk = 26` (Android 8.0), `targetSdk = 35`, `compileSdk = 37`

---

## Budowanie ze źródeł

### Wymagania
- **JDK 17** lub **JDK 21**
- **Android SDK** (`compileSdk 37`)

### Polecenia Gradle
```bash
# Sklonowanie repozytorium
git clone https://github.com/m1chi0001/Taal.git
cd Taal

# Uruchomienie testów jednostkowych i analizy Android Lint
./gradlew :app:testDebugUnitTest :app:lintDebug

# Zbudowanie wersji testowej Debug APK
./gradlew :app:assembleDebug

# Zbudowanie zoptymalizowanej wersji Release APK (R8 / shrinkResources)
./gradlew :app:assembleRelease
```

---

## Identyfikator i wersja

- **Nazwa aplikacji:** `Taal`
- **Application ID:** `com.M1chi0001.Taal`
- **Aktualne wydanie stabilne:** `1.0.0` (`versionCode = 20`)

---

## Licencja i zastrzeżenia

Projekt jest udostępniany na warunkach licencji [MIT](LICENSE).
**Taal** jest niezależnym projektem studenckim typu open-source i nie jest oficjalnym oprogramowaniem Uniwersytetu WSEI w Lublinie ani firmy APR System Sp. z o.o. Nazwy oraz znaki uczelni służą wyłącznie identyfikacji kompatybilności z systemem dziekanatowym dla studentów.
