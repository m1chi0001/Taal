# Taal 🌋

[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Release-v0.3-brightgreen)](https://github.com/m1chi0001/Taal/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Taal** to nowoczesna, w pełni natywna aplikacja mobilna na platformę Android dla studentów **Uniwersytetu WSEI w Lublinie** (dawniej Lubelska Akademia WSEI).
> Zastępuje przestarzałe interfejsy i web scrapingi bezpośrednim, wstecznie inżynierowanym klientem REST API zintegrowanym z systemem **Wirtualnego Dziekanatu (APR System / Mobilny Student)**, oferując błyskawiczny czas reakcji, pracę offline oraz immersyjny design **Material 3 Expressive**.

---

## ✨ Kluczowe funkcje

- 🌋 **Material 3 Expressive Design System:**
  - Dynamic Color (Material You) z dopracowanymi motywami jasnym, ciemnym i systemowym.
  - Oficjalne ekspresyjne figury M3X: `MaterialShapes.Cookie9Sided`, pigułki nawigacyjne, squircle i zaokrąglenia kart `24.dp`.
  - Czcionka Google Sans Flex dołączona do aplikacji jako plik zmienny (osie `wght`, `wdth`, `opsz`, `ROND`), więc działa offline.
  - Płynna fizyka sprężynowa (`MotionScheme.expressive()`) przy rozwijaniu kart i przełączaniu widoków.
  - Najnowsze komponenty: falujące paski postępu (`LinearWavyProgressIndicator`) oraz wskaźniki `LoadingIndicator`.

- ⏱️ **Android Live Notifications (Ekran Blokady & Pasek Stanu):**
  - Trwałe powiadomienie ongoing w czasie rzeczywistym (`ForegroundService` z typem `specialUse`).
  - Wizualizacja postępu trwających zajęć i osi dnia (zajęcia ➡️ przerwa ➡️ zajęcia).
  - Widoczność na Always-On Display oraz ekranie blokady bez konieczności odblokowywania telefonu.

- 📅 **Interaktywny Plan Zajęć:**
  - Domyślny, przejrzysty pasek aktualnego tygodnia (Pon–Ndz).
  - Rozwijany jednym kliknięciem pełny kalendarz siatki miesiąca z oznaczonymi dniami zjazdów/zajęć.
  - Wskaźnik trwania zajęć na żywo z dokładnością do minuty oraz podgląd przerw między wykładami.
  - Filtrowanie planu (mój plan / wszystkie grupy).

- 💳 **Finanse i Płatności:**
  - Inteligentne sortowanie należności: **Najbliższe do zapłaty ➡️ Najpóźniej do zapłaty ➡️ Zapłacone**.
  - Zwijane kafelki z natychmiastowym podglądem kwoty i przyciskiem szybkiej płatności online.
  - Sformatowany, czytelny numer indywidualnego subkonta bankowego z funkcją szybkiego kopiowania.
  - Podgląd przyznanych stypendiów oraz wniosków stypendialnych.

- 🎓 **Przedmioty, Oceny i ECTS:**
  - Zestawienie ocen bieżącego semestru wraz z sumaryczną liczbą punktów ECTS.
  - Szczegóły dotyczące formy zaliczenia, prowadzącego oraz statusu wpisu.

- 📢 **Ogłoszenia Uczelniane:**
  - Wiadomości z Dziekanatu, zarządzenia władz uczelni oraz pobieranie załączników bezpośrednio na urządzenie.

- 🔒 **Bezpieczeństwo i Android Keystore:**
  - Sprzętowo wspierane szyfrowanie poświadczeń studenta algorytmem AES-256 GCM w Android Keystore (`EncryptedCredentialStore`).
  - Natychmiastowy start aplikacji z pamięci podręcznej bez migania ekranem logowania (`WseiSplashScreen`).
  - Ekran powitalny (`WseiWelcomeScreen`) pokazywany raz, przed pierwszym logowaniem. Po wylogowaniu aplikacja wraca od razu do logowania.
  - Opcjonalny tryb ochrony prywatności (maskowanie numeru albumu i kwot opłat w miejscach publicznych).

---

## 🛠️ Architektura i technologie

Aplikacja została zbudowana zgodnie z zasadami **Clean Architecture** i wzorcem **MVI / MVVM**:

```
app/
 ├── data/
 │    ├── local/         # Room Database, DataStore Preferences, EncryptedCredentialStore
 │    ├── remote/api/    # Ktor REST Client, ArpSessionPlugin, ArpAutoReauthInterceptor
 │    └── repository/    # ScheduleRepositoryImpl (Offline-First cache & sync)
 ├── domain/
 │    ├── model/         # Niezależne modele domenowe (ClassEvent, FinancialObligation, itp.)
 │    └── repository/    # Kontrakty repozytoriów
 ├── service/            # LiveClassNotificationService, ClassAlarmScheduler, NotificationHelper
 └── ui/
      ├── components/    # WseiScreenScaffold, WseiSplashScreen, WseiScreenHeader
      ├── navigation/    # WseiAppShell, FloatingIconOnlyNavigationPill, Bento Hub
      ├── schedule/      # ScheduleScreen, ExpandableCalendar, ActiveClassHeroCard
      ├── login/         # LoginScreen (konto testowe tylko w buildach debug)
      └── theme/         # WseiExpressiveTheme, MotionScheme, ColorSchemes, M3X Shapes
```

- **Język:** Kotlin 2.1
- **UI Framework:** Jetpack Compose + Material 3 Expressive (`1.4.0-alpha08`)
- **Sieć:** Ktor Client 3.1.0 (`OkHttp` engine, Content Negotiation, `kotlinx.serialization`)
- **Baza danych:** Room 2.7 + DataStore
- **Wstrzykiwanie zależności:** Dagger Hilt 2.55
- **Wielowątkowość:** Coroutines & Flow (równoległa synchronizacja modułów `async`/`awaitAll`)

---

## 🚀 Budowanie i instalacja

### Wymagania wstępne:
- Android Studio Ladybug / Meerkat lub nowszy
- JDK 17
- Android SDK (API 35)

### Kompilacja APK:
```bash
# Sklonowanie repozytorium
git clone https://github.com/m1chi0001/Taal.git
cd Taal

# Budowanie wersji Release
./gradlew assembleRelease
```
Gotowy plik APK znajdziesz w:
`app/build/outputs/apk/release/app-release.apk`

---

## 📱 Identyfikator aplikacji

- **Application ID:** `com.M1chi0001.Taal`
- **Wersja:** `0.3` (versionCode: `3`)

---

## 📄 Licencja

Projekt jest udostępniany na warunkach licencji [MIT](LICENSE).
Aplikacja jest niezależnym projektem studenckim i nie jest oficjalnie powiązana z APR System Sp. z o.o. ani z Uniwersytetem WSEI w Lublinie.
