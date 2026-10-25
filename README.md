# Funaggr - Agregátor místních akcí

## Přehled
Android aplikace pro agregaci kulturních a společenských akcí v okolí uživatele. Zobrazuje divadla, koncerty, trhy, festivaly, výstavy a další akce na příštích 14 dní v okruhu 25 km, seřazené podle vzdálenosti od aktuální polohy.

## Technický stack

- **Jazyk:** Kotlin (vestavěná podpora Kotlinu v AGP 9)
- **UI:** Jetpack Compose, Material Design 3 (dynamické barvy, tmavý režim, pull-to-refresh)
- **Architektura:** MVVM + repository, Hilt (KSP)
- **Síť:** OkHttp (HTTP cache, gzip, retry), Jsoup (HTML), kotlinx.serialization (JSON)
- **Asynchronní operace:** Kotlin Coroutines + Flow
- **Lokace:** Fused Location Provider + Android Geocoder
- **Lokální databáze:** Room (offline cache akcí a geokódovaných míst)
- **Obrázky:** Coil

## Zdroje dat a fallback

| Zdroj | Způsob | Poloha akce |
|-------|--------|-------------|
| **Kudy z nudy** (kudyznudy.cz) | HTML kalendáře s hledáním podle GPS (`?datum=od,do&lat=&lon=`), parsování schema.org microdat | obec / městská část → přibližná vzdálenost (≈) |
| **GoOut** (goout.net) | veřejné JSON API `services/entities/v1/schedules` pro nejbližší město + okolní města (`donuts`) | přesné souřadnice podniku |

Odolnost proti výpadkům:

1. Zdroje běží **paralelně**, každý s vlastním časovým limitem; chyba jednoho neovlivní ostatní.
2. Když zdroj selže, použijí se jeho **dříve uložené akce** z cache (pokud jsou ze stejného místa).
3. Když selžou všechny zdroje, cache se **nepřepisuje** a aplikace zobrazí starší data s upozorněním.
4. Parser Kudy z nudy má tři strategie: microdata → JSON-LD → odkazy `/akce/`, takže změna webu data zhorší, ale nevymaže.
5. Data z cache se znovu použijí do 30 minut, pokud se poloha nezměnila o více než 3 km.

Duplicitní akce z obou zdrojů se slučují (normalizovaný název + datum) a ponechá se záznam s více údaji.

## Struktura projektu

```
app/src/main/java/com/example/funaggr/
├── data/
│   ├── geo/          # GeoMath, CzechPlaces (offline gazetteer + GoOut city id), PlaceGeocoder
│   ├── local/        # Room: EventDatabase, EventDao, PlaceDao, CacheMetadata
│   ├── model/        # Event, EventType, LocationData
│   ├── remote/       # EventSource, KudyZNudySource/Parser, GoOutSource/Parser, EventClassifier
│   └── repository/   # EventRepository (paralelní načítání + fallback), EventMerger (deduplikace)
├── di/               # Hilt moduly
├── location/         # LocationProvider
├── ui/               # Compose obrazovka, komponenty, téma, ViewModel
├── util/             # Formátování data, vzdálenosti a ceny
├── FunaggApplication.kt
└── MainActivity.kt
```

## Sestavení a spuštění

### Požadavky
- JDK 17+ **včetně `javac`** (např. `openjdk-17-jdk` nebo JBR z Android Studia)
- Android SDK s platformou **API 37** (compileSdk 37), minSdk 26, targetSdk 36
- Gradle se stáhne přes wrapper (9.8.0)

### Příkazy
```bash
./gradlew assembleDebug        # debug APK
./gradlew installDebug         # instalace na připojené zařízení
./gradlew testDebugUnitTest    # unit testy
./gradlew assembleRelease      # release build (R8 minifikace + zmenšení zdrojů)
```

## Oprávnění

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Bez oprávnění k poloze aplikace použije výchozí polohu (Praha).

## Přidání dalšího zdroje

1. Implementujte `EventSource` v `data/remote/` (metoda `fetchEvents` při chybě vyhazuje výjimku).
2. Zaregistrujte jej v `BindingsModule` pomocí `@Binds @IntoSet`.
3. Repository jej automaticky zapojí do paralelního načítání, fallbacku i deduplikace.

## Budoucí vylepšení

- [ ] Vyhledávání a filtr podle data / ceny
- [ ] Oblíbené akce a připomenutí
- [ ] Mapa s akcemi
- [ ] Přesné souřadnice akcí z Kudy z nudy (detail akce obsahuje `data-lat`/`data-lon`)
- [ ] Nastavitelný okruh vyhledávání
