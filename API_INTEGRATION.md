# API Integration Guide - Připojení zdrojů dat

## Přehled

Tento dokument popisuje, jak připojit aplikaci Funaggr na skutečné zdroje dat událostí v České republice.

## Dostupné zdroje dat

### 1. GoOut.net

**Typ:** Kulturní přehled a prodejce vstupenek  
**API:** Nemá veřejné oficiální API  
**Řešení:** Web scraping nebo neoficiální API

#### Implementace (Web Scraping)
```kotlin
// data/remote/GoOutDataSource.kt
class GoOutDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun fetchEvents(latitude: Double, longitude: Double): List<Event> {
        val url = "https://goout.net/cs/ajax/venue-list"
        // Implementace HTTP požadavku a parsování HTML/JSON
        // Použít knihovnu Jsoup pro HTML parsing
    }
}
```

**Gradle závislost:**
```kotlin
implementation("org.jsoup:jsoup:1.17.1")
```

### 2. Kudy z nudy (kudyznudy.cz)

**Typ:** Oficiální portál CzechTourism  
**API:** Má XML/JSON feed

#### API Endpoint
```
https://www.kudyznudy.cz/api/events
```

#### Implementace
```kotlin
// data/remote/KudyZNudyApiService.kt
interface KudyZNudyApiService {
    @GET("api/events")
    suspend fun getEvents(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("radius") radiusKm: Int,
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): Response<KudyZNudyResponse>
}

// Datové modely
data class KudyZNudyResponse(
    val items: List<KudyZNudyEvent>
)

data class KudyZNudyEvent(
    val id: String,
    val name: String,
    val description: String,
    val eventStart: String,
    val place: KudyZNudyPlace,
    val price: String?,
    val url: String
)

data class KudyZNudyPlace(
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)
```

### 3. Informuji.cz

**Typ:** Komplexní kalendář akcí  
**API:** RSS feed nebo web scraping

#### Implementace (RSS)
```kotlin
// Gradle závislost
implementation("com.rometools:rome:2.1.0")

// data/remote/InformujiczDataSource.kt
class InformujiczDataSource @Inject constructor() {
    suspend fun fetchEvents(region: String): List<Event> {
        val feedUrl = "https://www.informuji.cz/rss/akce/$region"
        // Parsování RSS feedu pomocí Rome library
    }
}
```

### 4. Facebook Events

**Typ:** Sociální síť s největší databází událostí  
**API:** Graph API (vyžaduje autorizaci)

#### Problém
Facebook omezil přístup k Events API pro nové aplikace (od 2018). Pouze schválené aplikace mají přístup.

#### Alternativa
- Web scraping (technicky složité kvůli dynamickému obsahu)
- Použít službu třetích stran (např. Eventbrite API, které integruje FB události)

### 5. Eventbrite

**Typ:** Mezinárodní platforma pro události  
**API:** Ano, dobře dokumentované

#### API Dokumentace
https://www.eventbrite.com/platform/api

#### Implementace
```kotlin
// data/remote/EventbriteApiService.kt
interface EventbriteApiService {
    @GET("events/search/")
    suspend fun searchEvents(
        @Header("Authorization") token: String,
        @Query("location.latitude") latitude: Double,
        @Query("location.longitude") longitude: Double,
        @Query("location.within") radiusKm: String,
        @Query("expand") expand: String = "venue"
    ): Response<EventbriteResponse>
}

// Získání OAuth tokenu z https://www.eventbrite.com/platform/api#/introduction/authentication
```

## Doporučená strategie integrace

### Fáze 1: Agregátor s více zdroji

Vytvořte `EventAggregator`, který načítá data z několika zdrojů najednou:

```kotlin
// data/repository/EventAggregator.kt
@Singleton
class EventAggregator @Inject constructor(
    private val kudyZNudySource: KudyZNudyDataSource,
    private val goOutSource: GoOutDataSource,
    private val informujiczSource: InformujiczDataSource,
    private val eventbriteSource: EventbriteDataSource
) {
    suspend fun aggregateEvents(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            // Paralelní načítání z více zdrojů
            val kudyEvents = async { kudyZNudySource.fetchEvents(latitude, longitude) }
            val goOutEvents = async { goOutSource.fetchEvents(latitude, longitude) }
            val informujiczEvents = async { informujiczSource.fetchEvents("praha") }
            val eventbriteEvents = async { eventbriteSource.fetchEvents(latitude, longitude) }
            
            // Spojení všech zdrojů
            val allEvents = listOf(
                kudyEvents.await(),
                goOutEvents.await(),
                informujiczEvents.await(),
                eventbriteEvents.await()
            ).flatten()
            
            // Deduplikace podle URL nebo názvu
            val uniqueEvents = deduplicateEvents(allEvents)
            
            // Seřazení podle vzdálenosti
            val sortedEvents = sortByDistance(uniqueEvents, latitude, longitude)
            
            Result.success(sortedEvents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun deduplicateEvents(events: List<Event>): List<Event> {
        return events.distinctBy { it.sourceUrl }
    }
    
    private fun sortByDistance(
        events: List<Event>,
        userLat: Double,
        userLon: Double
    ): List<Event> {
        return events.sortedBy { it.distanceKm }
    }
}
```

### Fáze 2: Mapper pro jednotnou strukturu

Každý zdroj má vlastní formát dat. Vytvořte mappery:

```kotlin
// data/mapper/EventMapper.kt
object EventMapper {
    fun fromKudyZNudy(source: KudyZNudyEvent, userLocation: LocationData): Event {
        return Event(
            id = "kudy_${source.id}",
            title = source.name,
            type = inferEventType(source.name, source.description),
            description = source.description,
            dateStart = source.eventStart,
            locationName = "${source.place.name}, ${source.place.city}",
            distanceKm = calculateDistance(
                userLocation.latitude, 
                userLocation.longitude,
                source.place.latitude,
                source.place.longitude
            ),
            priceCzk = parsePrice(source.price),
            sourceUrl = source.url,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    private fun inferEventType(title: String, description: String): EventType {
        val text = "$title $description".lowercase()
        return when {
            text.contains("koncert") || text.contains("hudba") -> EventType.MUSIC
            text.contains("divadlo") || text.contains("představení") -> EventType.THEATER
            text.contains("trh") || text.contains("market") -> EventType.MARKET
            text.contains("festival") -> EventType.FESTIVAL
            text.contains("dětsk") || text.contains("děti") -> EventType.KIDS
            else -> EventType.OTHER
        }
    }
    
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        // Haversine formula pro výpočet vzdálenosti
        val r = 6371 // poloměr Země v km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
    
    private fun parsePrice(priceStr: String?): Int? {
        return priceStr?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
    }
}
```

### Fáze 3: Caching strategie

```kotlin
// data/repository/EventRepository.kt (aktualizovaná verze)
@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val eventAggregator: EventAggregator
) {
    suspend fun fetchEvents(
        latitude: Double,
        longitude: Double,
        radiusKm: Int = 20,
        forceRefresh: Boolean = false
    ): Result<List<Event>> {
        // Kontrola cache
        if (!forceRefresh) {
            val cacheTime = eventDao.getLatestCacheTime()
            val isCacheValid = cacheTime?.let {
                System.currentTimeMillis() - it < CACHE_DURATION_MS
            } ?: false
            
            if (isCacheValid) {
                // Vrátit data z cache
                val cachedEvents = eventDao.getAllEventsSync()
                if (cachedEvents.isNotEmpty()) {
                    return Result.success(cachedEvents)
                }
            }
        }
        
        // Načíst nová data
        return eventAggregator.aggregateEvents(latitude, longitude, radiusKm)
            .onSuccess { events ->
                // Uložit do cache
                eventDao.deleteAll()
                eventDao.insertAll(events)
            }
    }
    
    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minut
    }
}
```

## Právní a etické aspekty

### Web Scraping
⚠️ **Upozornění:** Web scraping může být v rozporu s Terms of Service některých webů.

**Doporučení:**
1. Kontaktujte provozovatele webu a požádejte o oficiální API přístup
2. Respektujte robots.txt
3. Implementujte rate limiting (max. 1 request za 3 sekundy)
4. Cachujte data co nejdéle
5. Identifikujte se správným User-Agent

```kotlin
// Příklad rate limitingu
class RateLimitInterceptor : Interceptor {
    private val lastRequestTime = AtomicLong(0)
    private val minInterval = 3000L // 3 sekundy
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime.get()
        
        if (timeSinceLastRequest < minInterval) {
            Thread.sleep(minInterval - timeSinceLastRequest)
        }
        
        lastRequestTime.set(System.currentTimeMillis())
        return chain.proceed(chain.request())
    }
}
```

## Testování

### Mock server pro vývoj

```kotlin
// androidTest nebo test
@RunWith(AndroidJUnit4::class)
class EventRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }
    
    @Test
    fun `fetch events returns correct data`() = runTest {
        // Mock response
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""{"items": [...]}""")
        mockWebServer.enqueue(mockResponse)
        
        // Test repository
        val result = repository.fetchEvents(50.0, 14.0)
        assertTrue(result.isSuccess)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
}
```

## Shrnutí implementace

1. **Krok 1:** Začněte s jedním zdrojem (např. Kudy z nudy - má API)
2. **Krok 2:** Přidejte další zdroje postupně
3. **Krok 3:** Implementujte agregaci a deduplikaci
4. **Krok 4:** Optimalizujte caching a performance
5. **Krok 5:** Přidejte error handling a retry logic
6. **Krok 6:** Testujte s reálnými daty

## Potřebné Gradle závislosti

```kotlin
// Web scraping
implementation("org.jsoup:jsoup:1.17.1")

// RSS parsing
implementation("com.rometools:rome:2.1.0")

// XML parsing
implementation("com.tickaroo.tikxml:annotation:0.8.15")
kapt("com.tickaroo.tikxml:processor:0.8.15")
```

## Kontakty na poskytovatele dat

- **Kudy z nudy:** api@czechtourism.cz
- **GoOut:** info@goout.net
- **Informuji.cz:** redakce@informuji.cz

Vždy je lepší mít oficiální API přístup než se spoléhat na scraping!
