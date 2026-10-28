package app.mimic

import android.app.Application
import android.content.Context
import app.mimic.data.DemoSeed
import app.mimic.data.MimicDb
import app.mimic.data.SettingsStore
import app.mimic.data.Shelf
import app.mimic.domain.Sky
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = MimicDb.create(app)
    val shelf = Shelf(db.games())
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** True while the demo build fills its room, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    /** Asks Open-Meteo for the weather if a town is set and the last answer is over half an hour old. */
    suspend fun refreshWeather(force: Boolean = false) {
        val s = settings.now()
        val p = s.place ?: return
        if (!s.liveWeather) return
        if (!force && System.currentTimeMillis() - (s.outside?.at ?: 0) < 30 * 60 * 1000L) return
        val o = runCatching { withContext(Dispatchers.IO) { Sky.fetch(p.lat, p.lon) } }.onFailure { android.util.Log.w("Mimic", "weather", it) }.getOrNull() ?: return
        settings.update { it.copy(outside = o) }
    }
}

class MimicApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { try { DemoSeed.seedIfEmpty(this@MimicApp, container) } finally { container.seeding.value = false } }
    }
}

val Context.container: AppContainer get() = (applicationContext as MimicApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
