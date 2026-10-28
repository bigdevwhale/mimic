package app.mimic.ui

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import app.mimic.data.DAY
import app.mimic.data.Game
import app.mimic.data.Session
import app.mimic.data.Settings
import app.mimic.data.Style
import app.mimic.domain.Genres
import app.mimic.data.Status
import app.mimic.data.Covers
import app.mimic.data.dusty
import app.mimic.domain.Mimic
import app.mimic.domain.MimicState
import app.mimic.domain.Phase
import app.mimic.domain.Sky
import app.mimic.domain.Wx
import app.mimic.ui.art.BoxLook
import app.mimic.ui.art.Boxes
import app.mimic.ui.art.CoverArt

/** Everything the screens read, made once per change. */
class Home(val games: List<Game>, val sessions: List<Session>, val styles: List<Style>, val s: Settings, val now: Long, val ru: Boolean) {
    val genres = Genres(styles)
    val mimic: MimicState = Mimic.of(games, genres)
    val hands = games.filter { it.status == Status.HANDS }
    val pile = games.filter { it.status == Status.PILE }.sortedBy { it.added }
    val shelf = games.filter { it.status == Status.SHELF || it.status == Status.DONE }
    val away = games.filter { it.status == Status.AWAY }
    fun game(id: Long) = games.firstOrNull { it.id == id }
    fun sessionsOf(id: Long) = sessions.filter { it.game == id }

    /** Live phase from the sun at the window's town (or the time zone), weather from the last answer. */
    val phase: Phase
    val wx: Wx
    init {
        val p = s.place ?: Sky.guess()
        phase = Sky.phase(now, p.lat, p.lon)
        val o = s.outside
        wx = if (s.liveWeather && o != null && now - o.at < 3 * 60 * 60 * 1000L) o.wx else Wx.CLEAR
    }
}

fun Game.art(ctx: Context): CoverArt? = Covers.art(ctx, cover)
fun Game.tint(): Int = if (color != 0) color else Boxes.colorFor(title)
fun Game.look(ctx: Context, now: Long = System.currentTimeMillis()) = BoxLook(box, genre, tint(), art(ctx), dusty(now), status == Status.DONE)

fun Game.daysSince(ms: Long, now: Long) = ((now - ms) / DAY).toInt()

/** Little things screens ask of each other: the mimic speaks, a toast, a game to eat. */
@Stable
class Bus {
    var said by mutableStateOf("" to 0L)
        private set
    var toast by mutableStateOf("" to 0L)
        private set
    var eat by mutableStateOf<Pair<Game, Long>?>(null)
    var ghost by mutableStateOf(0L)
    var scatter by mutableStateOf(0L)

    fun say(t: String) { said = t to System.nanoTime() }
    fun toast(t: String) { toast = t to System.nanoTime() }
}

val LocalBus = staticCompositionLocalOf { Bus() }
