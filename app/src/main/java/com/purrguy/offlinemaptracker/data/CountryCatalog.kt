package com.purrguy.offlinemaptracker.data

/**
 * 100% free offline map catalog. All sources are OSM-based, no API key.
 * - Render tiles: Mapsforge .map vector files (street names included, small size)
 * - Routing data: Geofabrik .osm.pbf (used to build on-device GraphHopper graph)
 * - Raster fallback: OSMDroid tile pack for given bbox (cached, works offline after)
 *
 * Big countries expose regions (Russia etc.) so user downloads only what they need.
 */
data class CountryEntry(
    val id: String,          // e.g. "latvia"
    val displayName: String, // e.g. "Latvia"
    val isRegion: Boolean = false,
    val parent: String? = null,
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
    val centerLat: Double,
    val centerLon: Double,
    val mapsforgeUrl: String,
    val pbfUrl: String,
    val approxMapSizeMb: Int
)

object CountryCatalog {
    private fun mf(path: String) = "https://download.mapsforge.org/maps/v5/$path"
    private fun geo(path: String) = "https://download.geofabrik.de/$path-latest.osm.pbf"

    val entries: List<CountryEntry> = listOf(
        CountryEntry("latvia", "Latvia ⭐ (default)", false, null,
            58.09, 55.67, 28.24, 20.96, 56.95, 24.10,
            mf("europe/latvia.map"), geo("europe/latvia"), 55),
        CountryEntry("lithuania", "Lithuania", false, null,
            56.45, 53.89, 26.83, 20.95, 55.35, 23.90,
            mf("europe/lithuania.map"), geo("europe/lithuania"), 60),
        CountryEntry("estonia", "Estonia", false, null,
            59.68, 57.51, 28.21, 21.76, 58.75, 25.00,
            mf("europe/estonia.map"), geo("europe/estonia"), 45),
        CountryEntry("poland", "Poland", false, null,
            54.84, 49.00, 24.15, 14.12, 51.92, 19.15,
            mf("europe/poland.map"), geo("europe/poland"), 380),
        CountryEntry("germany", "Germany", false, null,
            55.06, 47.27, 15.04, 5.87, 51.16, 10.45,
            mf("europe/germany.map"), geo("europe/germany"), 900),
        CountryEntry("france", "France", false, null,
            51.09, 41.36, 9.56, -5.14, 46.60, 2.44,
            mf("europe/france.map"), geo("europe/france"), 950),
        CountryEntry("ukraine", "Ukraine", false, null,
            52.38, 44.39, 40.23, 22.14, 48.38, 31.16,
            mf("europe/ukraine.map"), geo("europe/ukraine"), 320),
        CountryEntry("uk", "United Kingdom", false, null,
            58.68, 49.96, 1.76, -5.72, 54.00, -2.00,
            mf("europe/united-kingdom.map"), geo("europe/united-kingdom"), 650),
        CountryEntry("usa-california", "USA — California (region)", true, "USA",
            42.01, 32.53, -114.13, -124.41, 37.00, -120.00,
            mf("north-america/us-california.map"), geo("north-america/us-california"), 450),
        CountryEntry("usa-texas", "USA — Texas (region)", true, "USA",
            36.50, 25.84, -93.51, -106.43, 31.00, -100.00,
            mf("north-america/us-texas.map"), geo("north-america/us-texas"), 500),
        // Russia split into Geofabrik regions so a phone never downloads the whole country
        CountryEntry("russia-central", "Russia — Central (region)", true, "Russia",
            58.50, 50.50, 43.00, 34.00, 55.75, 37.61,
            mf("asia/russia-central.map"), geo("asia/russia-central"), 280),
        CountryEntry("russia-northwestern", "Russia — Northwestern (region)", true, "Russia",
            70.00, 56.00, 50.00, 27.00, 60.00, 36.00,
            mf("asia/russia-northwestern.map"), geo("asia/russia-northwestern"), 220),
        CountryEntry("russia-southern", "Russia — Southern (region)", true, "Russia",
            48.00, 41.00, 48.00, 37.00, 44.50, 42.00,
            mf("asia/russia-southern.map"), geo("asia/russia-southern"), 150),
        CountryEntry("russia-volga", "Russia — Volga (region)", true, "Russia",
            60.00, 50.00, 58.00, 42.00, 55.00, 50.00,
            mf("asia/russia-volga.map"), geo("asia/russia-volga"), 200),
        CountryEntry("russia-ural", "Russia — Ural (region)", true, "Russia",
            70.00, 50.00, 68.00, 55.00, 60.00, 60.00,
            mf("asia/russia-ural.map"), geo("asia/russia-ural"), 220),
        CountryEntry("russia-siberian", "Russia — Siberian (region)", true, "Russia",
            75.00, 50.00, 110.00, 65.00, 62.00, 90.00,
            mf("asia/russia-siberian.map"), geo("asia/russia-siberian"), 300),
        CountryEntry("russia-far-eastern", "Russia — Far Eastern (region)", true, "Russia",
            72.00, 40.00, 180.00, 105.00, 58.00, 140.00,
            mf("asia/russia-far-eastern.map"), geo("asia/russia-far-eastern"), 260),
    )

    fun default(): CountryEntry = entries.first { it.id == "latvia" }
    fun byId(id: String): CountryEntry? = entries.find { it.id == id }
}
