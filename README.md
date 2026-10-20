# PobieranieAplikacja (Android File Downloader)

Aplikacja mobilna na platformę Android umożliwiająca asynchroniczne sprawdzanie metadanych plików sieciowych (rozmiar, typ MIME) za pośrednictwem protokołu HTTPS oraz realizację pobierania plików w tle przy użyciu usługi `IntentService` powiązanej z systemowym paskiem powiadomień.

---

## Spis treści
1. [Funkcjonalności](#funkcjonalności)
2. [Architektura i technologie](#architektura-i-technologie)
3. [Struktura modułów](#struktura-modułów)
4. [Wymagania i uprawnienia](#wymagania-i-uprawnienia)
5. [Instalacja i uruchomienie](#instalacja-i-uruchomienie)
6. [Instrukcja użytkowania](#instrukcja-użytkowania)
7. [Ograniczenia i uwagi techniczne](#ograniczenia-i-uwagi-techniczne)

---

## Funkcjonalności

### 1. Pobieranie informacji o pliku (`AsyncTask`)
* **Weryfikacja nagłówków HTTPS**: Nawiązanie połączenia z podanym adresem URL za pomocą `HttpsURLConnection` bez konieczności pobierania całej zawartości pliku.
* **Odczyt metadanych**:
  * Rozmiar pliku w bajtach (`getContentLength()`).
  * Typ zawartości / MIME (`getContentType()`).
* **Aktualizacja widoku**: Prezentacja odczytanych wartości w czasie rzeczywistym na ekranie aplikacji za pośrednictwem `publishProgress()`.

### 2. Pobieranie pliku w tle (`MojaIntentService`)
* **Działanie w tle (Foreground Service)**: Wykorzystanie usługi `startForeground` z unikalnym identyfikatorem powiadomienia, co minimalizuje ryzyko ubicia procesu przez system Android przy niskim stanie pamięci RAM.
* **Powiadomienia z paskiem postępu**:
  * Utworzenie kanału powiadomień (`NotificationChannel`) dla systemów Android 8.0+ (API 26+).
  * Dynamiczne aktualizowanie postępu w pasku powiadomień systemu (`Notification.Builder.setProgress`).
  * Możliwość powrotu do aktywności z poziomu powiadomienia dzięki `PendingIntent` oraz `TaskStackBuilder`.
* **Strumieniowy zapis danych**:
  * Pobieranie strumienia sieciowego do bufora o rozmiarze 8192 bajtów (8 KB).
  * Zapis pliku bezpośrednio do pamięci masowej (`Environment.getExternalStorageDirectory()`).
  * Nadpisywanie istniejącego pliku o tej samej nazwie.

### 3. Komunikacja międzykomponentowa i synchronizacja UI
* **Lokalny menedżer rozgłoszeń (`LocalBroadcastManager`)**: Bezpieczne rozsyłanie komunikatów wewnątrz aplikacji z postępem (`PostepInfo`) bez narażania danych na przechwycenie przez inne aplikacje systemowe.
* **Aktualizacja interfejsu**:
  * Pasek postępu (`ProgressBar`) odzwierciedlający procent pobranych danych.
  * Tekstowa informacja o liczbie pobranych bajtów względem wagi całkowitej (`mPobranychBajtow / mRozmiar`).
  * Blokowanie przycisku pobierania na czas trwania operacji w celu zapobieżenia wielokrotnym równoległym wywołaniom.
* **Zachowanie stanu (`savedInstanceState` / `onRestoreInstanceState`)**:
  * Przetrwanie obrotu ekranu: zachowanie wprowadzonego adresu URL, odczytanych parametrów pliku, stanu pasków i blokady przycisku pobierania.

---

## Architektura i technologie

* **Język programowania**: Java
* **Platforma docelowa**: Android SDK (AndroidX, AppCompat)
* **Kluczowe komponenty**:
  * `AsyncTask`: Realizacja zapytania sieciowego o metadane (wykonywana na puli wątków tła).
  * `IntentService`: Kolejkowana usługa tła obsługująca transfer pliku strumieniem bajtowym.
  * `LocalBroadcastManager`: Szyna zdarzeń przesyłająca komunikaty o postępie między usługą a `MainActivity`.
  * `Parcelable`: Binarna serializacja obiektu `PostepInfo` przekazywanego w intencjach rozgłoszeniowych.
  * `NotificationManager` & `NotificationChannel`: Obsługa powiadomień systemowych ze statusem postępu.
  * `HttpsURLConnection`: Bezpośrednia obsługa protokołu sieciowego SSL/TLS.

---

## Struktura modułów

```text
com.example.pobieranieaplikacja/
├── MainActivity.java        # Główny ekran, obsługa uprawnień, AsyncTask dla metadanych, odbiornik BroadcastReceiver
├── MojaIntentService.java   # Usługa Foreground IntentService realizująca pobieranie strumieniowe i powiadomienia
└── PostepInfo.java          # Model danych postępu implementujący interfejs Parcelable
```

---

## Wymagania i uprawnienia

### Wymagane wpisy w `AndroidManifest.xml`
Aplikacja do prawidłowego działania wymaga zadeklarowania w manifeście następujących elementów:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

Usługa musi być również zarejestrowana w sekcji `<application>`:
```xml
<service
    android:name=".MojaIntentService"
    android:exported="false" />
```

---

## Instalacja i uruchomienie

1. Sklonuj repozytorium do katalogu lokalnego:
   ```bash
   git clone https://github.com/TomaszMarek/Android-Downloader.git
   ```
2. Otwórz projekt w środowisku **Android Studio**.
3. Zsynchronizuj projekt z plikami Gradle (`Sync Project with Gradle Files`).
4. Uruchom aplikację na emulatorze lub fizycznym urządzeniu z systemem Android (zalecany Android 7.0 - 9.0 ze względu na obsługę ścieżek `getExternalStorageDirectory`):
   * Skrót klawiszowy: `Shift + F10`.

---

## Instrukcja użytkowania

1. **Wprowadzenie adresu**: Wpisz lub wklej bezpośredni adres URL do pliku w polu tekstowym `editTextUrl` (wymagany protokół `https://`).
2. **Sprawdzenie pliku**: Kliknij przycisk **Pobierz informacje** (`buttonPobierzInformacje`). Aplikacja wyświetli rozmiar pliku w bajtach oraz jego typ MIME.
3. **Pobieranie pliku**:
   * Kliknij przycisk **Pobierz plik** (`buttonPobierzPlik`).
   * Jeśli uruchamiasz aplikację po raz pierwszy, zaakceptuj monit o przyznanie uprawnień do pamięci masowej.
   * Przycisk zostanie zablokowany, a na ekranie i w zasobniku systemowym pojawi się pasek postępu.
   * Po zakończeniu pobierania przycisk zostanie odblokowany, a plik zostanie zapisany w pamięci głównej urządzenia.

---

## Ograniczenia i uwagi techniczne

* **Protokół sieciowy**: Kod explicite rzutuje połączenie na `HttpsURLConnection`. Próba pobrania pliku z adresu `http://` (nieszyfrowanego) wyrzuci wyjątek `ClassCastException`.
* **Nowe wersje Androida (Scoped Storage)**: Metoda `Environment.getExternalStorageDirectory()` jest przestarzała od poziomu Android 10 (API 29). Zapis w ten sposób na nowszych wersjach systemu wymaga dostosowania do `MediaStore API` lub zapisu do katalogu dedykowanego aplikacji (`getExternalFilesDir()`).
* **Współbieżność (`AsyncTask` i `IntentService`)**: Obie klasy są oznaczone przez Google jako przestarzałe (*deprecated*) w nowszych wersjach SDK i w produkcyjnych rozwiązaniach zaleca się ich migrację do `WorkManager` lub bibliotek współbieżności języka Kotlin (Coroutines).
* **Flagi PendingIntent**: W systemach Android 12+ (API 31+) wymagane jest jawne zadeklarowanie flagi `PendingIntent.FLAG_IMMUTABLE` lub `PendingIntent.FLAG_MUTABLE`.
