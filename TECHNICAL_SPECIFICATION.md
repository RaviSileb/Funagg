# Technické zadání - stav implementace

## Přehled

**Funaggr** (Fun Aggregator) je Android aplikace agregující místní kulturní a společenské akce
z více zdrojů (Kudy z nudy, GoOut) s offline cache a fallbackem při výpadku zdrojů.

## Verze 2.0 – oprava nefunkčního načítání (10/2026)

### Zjištěné příčiny „žádných akcí“ ve verzi 1.0
1. **Zdrojový kód aplikace nebyl v repozitáři** – `.gitignore` vylučoval `/app/` a `/gradle/`.
   Kód byl rekonstruován z APK nainstalovaného v zařízení a přepsán.
2. **Scraper Kudy z nudy vždy selhal**: `document.select("")` vyhazuje v Jsoup
   `IllegalArgumentException: String must not be empty`. Výjimka se spolkla a vrátil se
   *úspěch s 0 akcemi*.
3. **Prázdný výsledek přepsal cache** a fallback (mock data) se nikdy nespustil, protože chyba
   nebyla hlášena jako chyba.
4. Selektory (`div.event-list-item` …) neodpovídaly skutečné struktuře webu a akce bez rozpoznaného
   města dostaly vzdálenost 999 km a byly odfiltrovány.
5. Další chyby: klasifikace „hra“ zachytávala „hrad/zahrada“, určení města podle obdélníku
   (49–51 °N, 13–15 °E = „Praha“), parsování na hlavním vlákně.

### Řešení
- Nové zdroje: Kudy z nudy (hledání podle GPS, schema.org microdata) a GoOut (JSON API).
- Paralelní načítání, per-zdroj fallback na cache, cache se nepřepisuje prázdnými daty.
- Mock data odstraněna – aplikace nikdy nezobrazuje smyšlené akce.
- Deduplikace napříč zdroji, slučování opakovaných termínů, obrázky, ceny, filtry s počty.
- Aktualizace na AGP 9.4 / Kotlin 2.4 / compileSdk 37, R8 v release buildu, unit testy.

## Funkční požadavky

| Požadavek | Stav | Implementace |
|-----------|------|--------------|
| FR-01 Žádost o oprávnění k poloze | ✅ | `EventsScreen` (ActivityResult API) |
| FR-02 Získání GPS souřadnic | ✅ | `LocationProvider` (Fused Location, timeout 10 s, poslední známá poloha) |
| FR-03 Fallback na výchozí polohu | ✅ | Praha, pokud není oprávnění/poloha |
| FR-04 Načtení dat podle lat/lon/okruhu | ✅ | `EventRepository` → `KudyZNudySource`, `GoOutSource` |
| FR-05 Indikátor načítání | ✅ | stavový řádek + pull-to-refresh |
| FR-06 Tlačítko obnovit | ✅ | Top App Bar + gesto stažení |
| FR-07 Stav načtení | ✅ | „Aktualizováno v HH:mm · zdroj počet“, varování při výpadku zdroje, offline režim |
| FR-08 Seznam karet | ✅ | `LazyColumn` + `EventCard` |
| FR-09 Řazení podle vzdálenosti | ✅ | `EventMerger` (neznámá vzdálenost na konci) |
| FR-10 Obsah karty | ✅ | název, datum/čas, místo, vzdálenost, cena, popis, typ, zdroj, obrázek |
| FR-11–13 Filtry | ✅ | `FilterBar` – kategorie s počty, prázdné kategorie skryté |
| FR-14–15 Otevření detailu | ✅ | `Intent.ACTION_VIEW` na zdrojovou URL |
| Offline režim | ✅ | Room cache, zachování při chybách |
| Material Design 3, tmavý režim | ✅ | `FunaggTheme`, dynamické barvy |

## Datový model

```kotlin
@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String,
    val title: String,
    val type: EventType,
    val description: String,
    val dateStart: String,          // ISO: 2026-10-25 nebo 2026-10-25T20:00
    val dateEnd: String?,
    val locationName: String,
    val latitude: Double?, val longitude: Double?,
    val distanceKm: Double?,        // null = neznámá
    val distanceApproximate: Boolean,
    val priceCzk: Int?,             // 0 = zdarma
    val priceText: String?,
    val sourceUrl: String,
    val imageUrl: String?,
    val source: String,             // "Kudy z nudy" | "GoOut"
    val occurrences: Int,
    val cachedAt: Long
)
```

`EventType`: ALL, MUSIC, THEATER, MARKET, FESTIVAL, KIDS, EXHIBITION, SPORT, EDUCATION, OTHER.

## Konfigurace (`EventRepository`)

| Konstanta | Hodnota |
|-----------|---------|
| `DEFAULT_RADIUS_KM` | 25 |
| `DAYS_AHEAD` | 14 |
| `CACHE_TTL_MS` | 30 min |
| `CACHE_LOCATION_TOLERANCE_KM` | 3 km |
| `SOURCE_TIMEOUT_MS` | 30 s |

Podrobnosti o zdrojích viz **API_INTEGRATION.md**, sestavení viz **README.md**.
