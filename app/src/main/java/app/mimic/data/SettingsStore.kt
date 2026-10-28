package app.mimic.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.mimic.domain.Outside
import app.mimic.domain.Place
import app.mimic.domain.Wx
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** The town outside the window; null keeps the sun from the time zone and no weather. */
    val place: Place? = null,
    val outside: Outside? = null,
    /** Real weather in the window, or always fair. */
    val liveWeather: Boolean = true,
    /** Covers redrawn in pixels, or shown as they are on cards. */
    val pixelCovers: Boolean = true,
    /** Creatures met, for the bestiary. */
    val seen: Set<String> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read(): Settings {
        val lat = this[LAT]
        val lon = this[LON]
        val at = this[WX_AT] ?: 0L
        return Settings(
            onboarded = this[ONBOARDED] ?: false,
            place = if (lat != null && lon != null) Place(this[PLACE] ?: "", this[REGION] ?: "", lat, lon) else null,
            outside = if (at > 0) Outside(runCatching { Wx.valueOf(this[WX] ?: "") }.getOrDefault(Wx.CLEAR), this[TEMP]?.takeIf { it != Int.MIN_VALUE },
                this[WIND] ?: 8.0, this[RISE] ?: 0, this[SET] ?: 0, at) else null,
            liveWeather = this[LIVE] ?: true,
            pixelCovers = this[PIXEL] ?: true,
            seen = this[SEEN]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
        )
    }

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            val p = s.place
            if (p != null) { it[PLACE] = p.name; it[REGION] = p.region; it[LAT] = p.lat; it[LON] = p.lon }
            else { it.remove(PLACE); it.remove(REGION); it.remove(LAT); it.remove(LON) }
            val o = s.outside
            if (o != null) { it[WX] = o.wx.name; it[TEMP] = o.temp ?: Int.MIN_VALUE; it[WIND] = o.wind; it[RISE] = o.rise; it[SET] = o.set; it[WX_AT] = o.at }
            else it.remove(WX_AT)
            it[LIVE] = s.liveWeather
            it[PIXEL] = s.pixelCovers
            it[SEEN] = s.seen.sorted().joinToString(",")
        }
    }

    /** Marks a creature as met; returns true the first time. */
    suspend fun meet(key: String): Boolean {
        var fresh = false
        update { s -> if (key in s.seen) s else { fresh = true; s.copy(seen = s.seen + key) } }
        return fresh
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PLACE = stringPreferencesKey("place")
        val REGION = stringPreferencesKey("region")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val WX = stringPreferencesKey("wx")
        val TEMP = intPreferencesKey("temp")
        val WIND = doublePreferencesKey("wind")
        val RISE = longPreferencesKey("rise")
        val SET = longPreferencesKey("set")
        val WX_AT = longPreferencesKey("wx_at")
        val LIVE = booleanPreferencesKey("live")
        val PIXEL = booleanPreferencesKey("pixel")
        val SEEN = stringPreferencesKey("seen")
    }
}
