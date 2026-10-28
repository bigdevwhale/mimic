package app.mimic.domain

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** The time of day the window shows. */
enum class Phase { NIGHT, DAWN, DAY, EVENING }

/** The weather the window shows. */
enum class Wx { CLEAR, CLOUDY, OVERCAST, DRIZZLE, RAIN, SNOW, FOG, STORM }

/** A town the window looks out on. */
data class Place(val name: String, val region: String, val lat: Double, val lon: Double)

/** What Open-Meteo said last: the weather now and today's sunrise and sunset (epoch ms). */
data class Outside(val wx: Wx, val temp: Int?, val wind: Double, val rise: Long, val set: Long, val at: Long)

object Sky {
    /** The sun's altitude in degrees at an instant. */
    fun sunAlt(ms: Long, lat: Double, lon: Double): Double {
        fun rad(d: Double) = Math.toRadians(d)
        val n = ms / 86_400_000.0 + 2440587.5 - 2451545.0
        val l = (280.46 + 0.9856474 * n) % 360
        val g = rad((357.528 + 0.9856003 * n) % 360)
        val lam = rad(l + 1.915 * sin(g) + 0.020 * sin(2 * g))
        val eps = rad(23.439 - 0.0000004 * n)
        val ra = atan2(cos(eps) * sin(lam), cos(lam))
        val dec = asin(sin(eps) * sin(lam))
        val gmst = ((18.697374558 + 24.06570982441908 * n) % 24 + 24) % 24
        val h = rad(gmst * 15 + lon) - ra
        val phi = rad(lat)
        @Suppress("UNUSED_VARIABLE") val unused = tan(0.0)
        return Math.toDegrees(asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h)))
    }

    /**
     * The phase from the sun itself: night below −6°, dawn or evening while it's low (before or after
     * noon), day above. Works with no network at all.
     */
    fun phase(ms: Long, lat: Double, lon: Double): Phase {
        val alt = sunAlt(ms, lat, lon)
        val later = sunAlt(ms + 20 * 60_000L, lat, lon)
        return when {
            alt < -6 -> Phase.NIGHT
            alt < 8 -> if (later > alt) Phase.DAWN else Phase.EVENING
            else -> Phase.DAY
        }
    }

    fun wxOf(code: Int, cloud: Double): Wx = when {
        code >= 95 -> Wx.STORM
        code in 71..77 || code == 85 || code == 86 -> Wx.SNOW
        code in 51..57 -> Wx.DRIZZLE
        code in 61..67 || code in 80..82 -> Wx.RAIN
        code == 45 || code == 48 -> Wx.FOG
        code == 3 || cloud > .8 -> Wx.OVERCAST
        code >= 1 || cloud > .3 -> Wx.CLOUDY
        else -> Wx.CLEAR
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 9000
        c.setRequestProperty("User-Agent", "Mimic/1.0 github.com/bigdevwhale/mimic")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /**
     * The weather now: from Open-Meteo, or from MET Norway when Open-Meteo can't be reached (some
     * networks block it). Nothing but the coordinates is sent.
     */
    fun fetch(lat: Double, lon: Double): Outside = runCatching { openMeteo(lat, lon) }.getOrElse { metNo(lat, lon) }

    private fun metNo(lat: Double, lon: Double): Outside {
        val j = JSONObject(get(String.format(Locale.US, "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.3f&lon=%.3f", lat, lon)))
        val now = j.getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
        val inst = now.getJSONObject("instant").getJSONObject("details")
        val sym = (now.optJSONObject("next_1_hours") ?: now.optJSONObject("next_6_hours"))?.optJSONObject("summary")?.optString("symbol_code").orEmpty()
        val cloud = inst.optDouble("cloud_area_fraction", 50.0) / 100
        val wx = when {
            "thunder" in sym -> Wx.STORM
            "snow" in sym || "sleet" in sym -> Wx.SNOW
            "lightrain" in sym -> Wx.DRIZZLE
            "rain" in sym -> Wx.RAIN
            "fog" in sym -> Wx.FOG
            "cloudy" == sym.substringBefore('_') -> Wx.OVERCAST
            "partlycloudy" in sym || "fair" in sym -> Wx.CLOUDY
            "clearsky" in sym -> Wx.CLEAR
            else -> wxOf(0, cloud)
        }
        return Outside(wx, inst.optDouble("air_temperature").takeIf { !it.isNaN() }?.let { Math.round(it).toInt() }, inst.optDouble("wind_speed", 2.0) * 3.6, 0, 0, System.currentTimeMillis())
    }

    private fun openMeteo(lat: Double, lon: Double): Outside {
        val j = JSONObject(get(String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,cloud_cover,wind_speed_10m&daily=sunrise,sunset&timezone=auto&forecast_days=1", lat, lon)))
        val cur = j.getJSONObject("current")
        val off = j.optLong("utc_offset_seconds") * 1000
        val daily = j.getJSONObject("daily")
        fun at(s: String) = Instant.parse("$s:00Z").toEpochMilli() - off
        return Outside(
            wxOf(cur.optInt("weather_code"), cur.optDouble("cloud_cover", 0.0) / 100),
            cur.optDouble("temperature_2m").takeIf { !it.isNaN() }?.let { Math.round(it).toInt() },
            cur.optDouble("wind_speed_10m", 8.0),
            at(daily.getJSONArray("sunrise").getString(0)), at(daily.getJSONArray("sunset").getString(0)),
            System.currentTimeMillis(),
        )
    }

    /** Towns matching a name, for the window's town. */
    fun search(name: String, lang: String): List<Place> {
        val q = URLEncoder.encode(name.trim(), "UTF-8")
        val j = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=$lang&format=json"))
        val r = j.optJSONArray("results") ?: return emptyList()
        return (0 until r.length()).map { i ->
            val o = r.getJSONObject(i)
            Place(o.getString("name"), listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    /** Rough coordinates for the phone's time zone when no town is set: the sun still rises at the right hour. */
    fun guess(zone: ZoneId = ZoneId.systemDefault()): Place {
        val off = zone.rules.getOffset(Instant.now()).totalSeconds / 3600.0
        return Place("", "", 50.0, (off * 15).coerceIn(-180.0, 180.0))
    }
}
