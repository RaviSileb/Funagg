# Integrace zdrojů dat

Tento dokument popisuje skutečně používané zdroje akcí, jejich formát a pravidla zacházení s nimi.

## Rozhraní `EventSource`

```kotlin
interface EventSource {
    val name: String
    val priority: Int                       // nižší = preferovaný při duplicitách
    suspend fun fetchEvents(query: EventQuery): List<Event>   // při chybě vyhazuje výjimku
}
```

`EventRepository` spouští všechny zaregistrované zdroje paralelně (timeout 30 s na zdroj), výsledky
slučuje přes `EventMerger` a řeší fallback (viz README).

## Kudy z nudy (`KudyZNudySource`)

- **URL:** `https://www.kudyznudy.cz/kalendar-akci?datum=YYYY-MM-DD,YYYY-MM-DD&lat=50.03&lon=14.51&stranka=N`
  - `datum` = rozsah dat (dnes … +13 dní), `lat`/`lon` = poloha uživatele (desetinná **tečka**).
  - Web vrací akce seřazené podle blízkosti, 12 na stránku; načítá se max. 5 stránek
    (první sekvenčně kvůli zjištění počtu stránek, zbytek paralelně).
- **Formát:** HTML se schema.org microdaty:
  - položka `[itemtype*=schema.org/Event]`
  - `meta[itemprop=startDate|endDate|description]`, `h3 [itemprop=name]`, `h3 a[href]`,
    `[itemprop=location] [itemprop=name]` (např. „Praha 10, Praha“), `img[itemprop=image]`
- **Fallbacky parseru:** JSON-LD (`script[type=application/ld+json]` s `@type` *Event*) → odkazy `a[href*=/akce/]`.
- **Poloha:** název místa → `CzechPlaces` (offline tabulka městských částí Prahy a měst) → Room cache
  → Android `Geocoder`. Vzdálenost je proto přibližná (`distanceApproximate = true`).
- Opakované termíny stejné akce na stejném místě se slučují (`occurrences`).

## GoOut (`GoOutSource`)

- **URL:** `https://goout.net/services/entities/v1/schedules?languages[]=cs&source=goout.net&include=events,venues,images&limit=100&donuts=true&cityIds[]=<id>&scrollId=<meta.nextScrollId>`
  - `cityIds` = id nejbližšího města z `CzechPlaces` (Who's On First id, např. Praha `101748113`, Brno `101748109`).
  - `donuts=true` = po vyčerpání akcí města API pokračuje prstenci okolních obcí (`meta.donut` = `d0`, `d2`, …).
  - Stránkuje se přes `scrollId` (max. 3 stránky); končí se, když stránka nemá žádnou akci v okruhu
    nebo jsou všechny akce za koncem období.
- **Formát:** JSON `{ meta, schedules[], included{ events[], venues[], images[] } }`;
  termín (`schedules`) odkazuje přes `relationships` na akci a podnik.
  Podnik obsahuje `latitude`/`longitude` → přesná vzdálenost. Akce bez souřadnic (online) se vyřazují.
- **Kategorie:** `mainCategory`/`categories` (`concerts`, `play`, `for_children`, `exhibitions`, …) se mapují na `EventType`.
- **Cena:** `pricing` (např. `449–1299`) + `currency`; `ticketingState = free` → zdarma.

## Pravidla slušného chování

- Identifikace aplikace v hlavičce `User-Agent` (`Funaggr/<verze>`).
- Malý počet požadavků: jedno obnovení = max. 5 + 3 požadavky, výsledky se cachují 30 minut.
- HTTP cache OkHttp (10 MB) a gzip.
- Jeden opakovaný pokus jen při síťové chybě nebo HTTP 5xx.

Oba weby nemají oficiálně dokumentované veřejné API; při změně jejich struktury se projeví chybou
v hlášení zdroje (stavový řádek) a aplikace pokračuje s ostatními zdroji / cache.
Pro produkční nasazení je vhodné domluvit oficiální přístup s provozovateli.

## Testy

- `KudyZNudyParserTest`, `GoOutParserTest` – parsování na syntetických fixturách včetně fallbacků.
- `EventRepositoryTest` – fallback při výpadku jednoho / všech zdrojů, zachování cache, znovupoužití cache.
- `EventMergerTest`, `EventClassifierTest`, `TextParsingTest`, `EventFormattingTest`.
