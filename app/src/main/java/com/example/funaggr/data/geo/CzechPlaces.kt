package com.example.funaggr.data.geo

import java.text.Normalizer
import java.util.Locale

/**
 * Offline gazetteer of Prague districts and larger Czech towns.
 *
 * Resolves the most common place names without any network call and maps towns to
 * GoOut city ids (Who's On First ids used by the GoOut API).
 */
object CzechPlaces {

    data class Place(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val goOutCityId: Long? = null
    )

    val places: List<Place> = listOf(
        Place("Praha", 50.0755, 14.4378, 101748113),
        Place("Praha 1", 50.0875, 14.4213),
        Place("Praha 2", 50.0730, 14.4340),
        Place("Praha 3", 50.0838, 14.4600),
        Place("Praha 4", 50.0400, 14.4500),
        Place("Praha 5", 50.0680, 14.3880),
        Place("Praha 6", 50.1010, 14.3600),
        Place("Praha 7", 50.1030, 14.4380),
        Place("Praha 8", 50.1170, 14.4650),
        Place("Praha 9", 50.1100, 14.5000),
        Place("Praha 10", 50.0680, 14.4900),
        Place("Praha 11", 50.0320, 14.5120),
        Place("Praha 12", 50.0050, 14.4250),
        Place("Praha 13", 50.0500, 14.3300),
        Place("Praha 14", 50.1050, 14.5550),
        Place("Praha 15", 50.0550, 14.5450),
        Place("Praha 16", 49.9850, 14.3600),
        Place("Praha 17", 50.0700, 14.3150),
        Place("Praha 18", 50.1450, 14.5000),
        Place("Praha 19", 50.1300, 14.5150),
        Place("Praha 20", 50.1000, 14.6300),
        Place("Praha 21", 50.0800, 14.6700),
        Place("Praha 22", 50.0300, 14.5950),
        Place("Brno", 49.1951, 16.6068, 101748109),
        Place("Ostrava", 49.8209, 18.2625, 101748125),
        Place("Plzeň", 49.7384, 13.3736, 101748111),
        Place("Liberec", 50.7663, 15.0543, 101748147),
        Place("Olomouc", 49.5938, 17.2509, 101748129),
        Place("České Budějovice", 48.9745, 14.4741, 101748103),
        Place("Hradec Králové", 50.2092, 15.8328, 101748133),
        Place("Ústí nad Labem", 50.6607, 14.0323, 101748139),
        Place("Pardubice", 50.0343, 15.7812, 101748115),
        Place("Zlín", 49.2265, 17.6707, 101748107),
        Place("Havířov", 49.7798, 18.4369, 101748121),
        Place("Kladno", 50.1473, 14.1029, 101748131),
        Place("Opava", 49.9387, 17.9026, 101748117),
        Place("Frýdek-Místek", 49.6881, 18.3536, 101748127),
        Place("Teplice", 50.6404, 13.8245, 101748135),
        Place("Karlovy Vary", 50.2310, 12.8710, 101840901),
        Place("Znojmo", 48.8555, 16.0488, 101832007),
        Place("Český Krumlov", 48.8127, 14.3175, 101782291),
        Place("Cheb", 50.0796, 12.3739, 101825339),
        Place("Tábor", 49.4144, 14.6578, 101841973),
        Place("Jablonec nad Nisou", 50.7243, 15.1711, 101822991),
        Place("Benešov", 49.7816, 14.6870, 101826635),
        Place("Čelákovice", 50.1604, 14.7501, 101825717),
        Place("Mariánské Lázně", 49.9646, 12.7012, 101825353),
        Place("Žatec", 50.3272, 13.5458, 101824313),
        Place("Jindřichův Hradec", 49.1441, 15.0030, 101831943),
        Place("Šumperk", 49.9653, 16.9706, 101840979),
        Place("Valašské Meziříčí", 49.4718, 17.9711, 101840849),
        Place("Vsetín", 49.3387, 17.9962, 101779843),
        Place("Chrudim", 49.9511, 15.7956, 101825247),
        Place("Rakovník", 50.1037, 13.7334, 101825651),
        Place("Sokolov", 50.1813, 12.6401, 101839821),
        Place("Humpolec", 49.5415, 15.3594, 101826421),
        Place("Jihlava", 49.3961, 15.5912),
        Place("Most", 50.5030, 13.6362),
        Place("Děčín", 50.7821, 14.2148),
        Place("Chomutov", 50.4605, 13.4178),
        Place("Karviná", 49.8540, 18.5417),
        Place("Mladá Boleslav", 50.4114, 14.9032),
        Place("Prostějov", 49.4719, 17.1118),
        Place("Přerov", 49.4551, 17.4509),
        Place("Třebíč", 49.2148, 15.8817),
        Place("Kolín", 50.0281, 15.2006),
        Place("Kutná Hora", 49.9484, 15.2682),
        Place("Trutnov", 50.5610, 15.9127),
        Place("Písek", 49.3088, 14.1475),
        Place("Kroměříž", 49.2979, 17.3931),
        Place("Uherské Hradiště", 49.0698, 17.4597),
        Place("Hodonín", 48.8489, 17.1324),
        Place("Břeclav", 48.7590, 16.8820),
        Place("Litoměřice", 50.5335, 14.1318),
        Place("Příbram", 49.6899, 14.0104),
        Place("Beroun", 49.9638, 14.0720),
        Place("Mělník", 50.3505, 14.4741),
        Place("Nymburk", 50.1861, 15.0417),
        Place("Havlíčkův Brod", 49.6079, 15.5807),
        Place("Strakonice", 49.2614, 13.9024),
        Place("Klatovy", 49.3955, 13.2950),
        Place("Náchod", 50.4167, 16.1629),
        Place("Česká Lípa", 50.6855, 14.5376),
        Place("Vyškov", 49.2775, 16.9990),
        Place("Blansko", 49.3631, 16.6445),
        Place("Nový Jičín", 49.5944, 18.0103),
        Place("Třinec", 49.6776, 18.6708),
        Place("Domažlice", 49.4405, 12.9298)
    )

    private val byKey: Map<String, Place> = places.associateBy { normalize(it.name) }

    /**
     * Looks up a free-form place such as "Praha 10, Praha" or "Brno-střed".
     * Tries each comma separated part, then the part before a dash.
     */
    fun lookup(text: String): Place? {
        val parts = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        for (part in parts) {
            byKey[normalize(part)]?.let { return it }
            byKey[normalize(part.substringBefore('–').substringBefore(" - "))]?.let { return it }
            byKey[normalize(part.substringBefore('-'))]?.let { return it }
        }
        return null
    }

    fun nearest(latitude: Double, longitude: Double): Place =
        places.minBy { GeoMath.distanceKm(latitude, longitude, it.latitude, it.longitude) }

    fun nearestGoOutCity(latitude: Double, longitude: Double): Place =
        places.filter { it.goOutCityId != null }
            .minBy { GeoMath.distanceKm(latitude, longitude, it.latitude, it.longitude) }

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
