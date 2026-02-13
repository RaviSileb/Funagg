# Funaggr - Agregátor místních akcí

## Přehled
Android aplikace pro agregaci kulturních a společenských akcí v okolí uživatele. Aplikace zobrazuje události jako divadla, koncerty, trhy, festivaly a další akce seřazené podle vzdálenosti od aktuální polohy.

## Technický stack

- **Jazyk:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Architektura:** MVVM (Model-View-ViewModel)
- **Síťová komunikace:** Retrofit + OkHttp
- **Asynchronní operace:** Kotlin Coroutines + Flow
- **Lokace:** Google Play Services Location API
- **Dependency Injection:** Hilt
- **Lokální databáze:** Room (pro offline cache)
- **Permissions:** Accompanist Permissions

## Funkce

### ✅ Implementované

1. **Geolokace**
   - Automatické získání GPS souřadnic
   - Žádost o oprávnění k lokaci
   - Fallback na výchozí lokaci (Praha)

2. **Načítání dat**
   - Mock data source pro testování
   - Offline cache v Room databázi
   - Indikátor načítání
   - Tlačítko Refresh

3. **Zobrazení událostí**
   - Seznam karet seřazený podle vzdálenosti
   - Informace: název, datum, místo, vzdálenost, cena, popis
   - Barevné štítky podle typu události
   - Ripple effect při kliknutí

4. **Filtrování**
   - Horizontální chips pro výběr kategorie
   - Kategorie: Vše, Hudba, Divadlo, Trhy, Festivaly, Pro děti
   - Okamžité přepínání filtru

5. **Status bar**
   - Zobrazení aktuální polohy
   - Stav načítání (úspěch/chyba/načítání)
   - Offline režim s upozorněním

6. **Detail události**
   - Otevření externího prohlížeče se zdrojovou URL

7. **Offline režim**
   - Cachování dat v Room DB
   - Zobrazení starších dat při nedostupnosti sítě

## Struktura projektu

```
app/src/main/java/com/example/funaggr/
├── data/
│   ├── local/           # Room database, DAO, Converters
│   ├── model/           # Datové modely (Event, EventType, API models)
│   ├── remote/          # API service, Mock data source
│   └── repository/      # Repository pattern
├── di/                  # Hilt dependency injection
├── ui/
│   ├── components/      # Compose komponenty (EventCard, FilterBar, StatusBar)
│   ├── screen/          # Hlavní obrazovky (EventsScreen)
│   ├── theme/           # Theme, Colors, Typography
│   └── viewmodel/       # ViewModels
├── util/                # Utility třídy (LocationManager)
├── FunaggApplication.kt # Application třída s @HiltAndroidApp
└── MainActivity.kt      # Hlavní aktivita
```

## Instalace a spuštění

### Požadavky
- Android Studio Hedgehog nebo novější
- Android SDK 26+ (minSdk)
- JDK 17

### Kroky

1. **Klonování projektu**
   ```bash
   git clone <repository-url>
   cd Funaggr
   ```

2. **Gradle Sync**
   - Otevřete projekt v Android Studio
   - Počkejte na automatickou synchronizaci Gradle
   - Nebo spusťte ručně: `./gradlew build`

3. **Spuštění**
   - Připojte Android zařízení nebo spusťte emulátor
   - Klikněte na "Run" v Android Studio
   - Nebo použijte: `./gradlew installDebug`

## Konfigurace

### API Endpoint
Aktuálně používá mock data. Pro produkční použití upravte:

```kotlin
// app/src/main/java/com/example/funaggr/di/AppModule.kt
@Provides
@Singleton
fun provideRetrofit(...): Retrofit {
    return Retrofit.Builder()
        .baseUrl("https://your-api-url.com/") // Změňte URL
        ...
}
```

### Připojení na skutečná API

Aplikace je připravena pro integraci s těmito zdroji:
- **GoOut.net** - koncerty, divadla, kluby
- **Kudy z nudy (kudyznudy.cz)** - turistické akce, festivaly
- **Informuji.cz** - komplexní kalendář akcí
- **Facebook Events API** - lokální akce

Pro integraci:
1. Implementujte API wrappery v `data/remote/`
2. Upravte `EventRepository` pro volání skutečných API
3. Přidejte mapování z API modelů na `Event` model

## Oprávnění

Aplikace vyžaduje následující oprávnění:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Testování

### Unit testy
```bash
./gradlew test
```

### UI testy
```bash
./gradlew connectedAndroidTest
```

## Budoucí vylepšení

- [ ] Skutečná integrace s API (GoOut, Kudy z nudy, atd.)
- [ ] Webový scraping pro weby bez API
- [ ] Vyhledávání událostí
- [ ] Oblíbené události
- [ ] Kalendář/připomenutí
- [ ] Sdílení událostí
- [ ] Mapa s událostmi
- [ ] Detailní view události v aplikaci
- [ ] Filtrování podle data a ceny
- [ ] Push notifikace pro nové akce

## Licence
[MIT License nebo jiná dle preferencí]

## Autor
Vytvořeno podle technického zadání pro agregaci místních kulturních akcí v České republice.

## Kontakt
[Vaše kontaktní informace]
