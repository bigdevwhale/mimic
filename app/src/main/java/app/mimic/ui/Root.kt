package app.mimic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mimic.R
import app.mimic.container
import app.mimic.data.Draft
import app.mimic.data.Settings
import app.mimic.ru
import app.mimic.ui.book.BookScreen
import app.mimic.ui.chronicle.ChronicleScreen
import app.mimic.ui.components.pixelBox
import app.mimic.ui.onboarding.Onboarding
import app.mimic.ui.room.RoomScreen
import app.mimic.ui.settings.SettingsPage
import app.mimic.ui.sheets.AddSheet
import app.mimic.ui.sheets.CardSheet
import app.mimic.ui.sheets.DigSheet
import app.mimic.ui.sheets.DraftSheet
import app.mimic.ui.sheets.SniffSheet
import app.mimic.ui.shelf.ShelfScreen
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import kotlinx.coroutines.delay

enum class Tab { ROOM, SHELF, CHRONICLE, BOOK }

/** What is open over the tabs. */
sealed class Overlay {
    data class Card(val id: Long) : Overlay()
    data class Dig(val id: Long?) : Overlay()
    data object Sniff : Overlay()
    data class Add(val q: String = "") : Overlay()
    data class NewGame(val d: Draft, val cover: String?) : Overlay()
    data object Settings : Overlay()
}

/** Ways screens move around. */
class Nav(val go: (Overlay?) -> Unit, val tab: (Tab, String?) -> Unit) {
    fun card(id: Long) = go(Overlay.Card(id))
    fun dig(id: Long?) = go(Overlay.Dig(id))
    fun sniff() = go(Overlay.Sniff)
    fun add() = go(Overlay.Add())
    fun close() = go(null)
    fun shelf(filter: String) = tab(Tab.SHELF, filter)
}

/** [clock], [tab0] and [overlay0] let renders open a fixed moment on a given screen; the app passes none. */
@Composable
fun MimicRoot(s: Settings, clock: Long? = null, tab0: Tab = Tab.ROOM, overlay0: Overlay? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    if (!s.onboarded) { Onboarding(); return }
    val games by c.shelf.games.collectAsStateWithLifecycle(initialValue = null)
    val sessions by c.shelf.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val styles by c.shelf.styles.collectAsStateWithLifecycle(initialValue = emptyList())
    var now by remember { mutableLongStateOf(clock ?: System.currentTimeMillis()) }
    LaunchedEffect(Unit) { if (clock == null) while (true) { c.refreshWeather(); delay(60_000); now = System.currentTimeMillis() } }
    val list = games ?: return
    val h = remember(list, sessions, styles, s, now) { Home(list, sessions, styles, s, now, ctx.ru) }
    val bus = remember { Bus() }
    var tab by rememberSaveable { mutableStateOf(tab0) }
    var filter by rememberSaveable { mutableStateOf("all") }
    var overlay by remember { mutableStateOf(overlay0) }
    // Settings stay open when the activity is recreated, as it is after a language change.
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (settingsOpen && overlay == null) overlay = Overlay.Settings }
    LaunchedEffect(overlay) { settingsOpen = overlay == Overlay.Settings }
    val nav = remember { Nav({ overlay = it }, { t, f -> tab = t; if (f != null) filter = f; overlay = null }) }
    LaunchedEffect(Unit) { if (bus.said.first.isEmpty()) bus.say(ctx.getString(if (list.isEmpty()) R.string.say_hello_empty else R.string.say_hello)) }

    CompositionLocalProvider(LocalBus provides bus) {
        Box(Modifier.fillMaxSize().background(C.wood)) {
            Column(Modifier.fillMaxSize()) {
                Header(h, nav, onSettings = { overlay = Overlay.Settings })
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.ROOM -> RoomScreen(h, nav)
                        Tab.SHELF -> ShelfScreen(h, nav, filter) { filter = it }
                        Tab.CHRONICLE -> ChronicleScreen(h)
                        Tab.BOOK -> BookScreen(h)
                    }
                }
                TabBar(tab) { tab = it }
            }
            when (val o = overlay) {
                is Overlay.Card -> h.game(o.id)?.let { CardSheet(h, it, nav) }
                is Overlay.Dig -> DigSheet(h, o.id, nav)
                Overlay.Sniff -> SniffSheet(h, nav)
                is Overlay.Add -> AddSheet(o.q, nav)
                is Overlay.NewGame -> DraftSheet(h, o.d, o.cover, nav)
                Overlay.Settings -> SettingsPage(s, nav)
                null -> Unit
            }
            Toast(Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun Header(h: Home, nav: Nav, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.app_title), style = T.Display.copy(color = C.brass), modifier = Modifier.weight(1f), maxLines = 1)
        Counter(stringResource(R.string.ct_shelf), h.games.size - h.hands.size) { nav.shelf("all") }
        Counter(stringResource(R.string.ct_pile), h.pile.size) { nav.shelf("pile") }
        Counter(stringResource(R.string.ct_eaten), h.mimic.eaten) { nav.shelf("done") }
        val label = stringResource(R.string.settings)
        Box(Modifier.size(36.dp).background(C.woodD).clickable(onClick = onSettings).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val p = size.width / 9
                fun r(x: Int, y: Int, w: Int = 1, hh: Int = 1) = drawRect(C.brass, Offset(x * p, y * p), Size(w * p, hh * p))
                r(1, 1, 7, 7); for ((x, y) in listOf(3 to 0, 5 to 0, 0 to 3, 0 to 5, 8 to 3, 8 to 5, 3 to 8, 5 to 8)) r(x, y)
                drawRect(C.woodD, Offset(3 * p, 3 * p), Size(3 * p, 3 * p))
                for ((x, y) in listOf(1 to 1, 7 to 1, 1 to 7, 7 to 7)) drawRect(C.woodD, Offset(x * p, y * p), Size(p, p))
            }
        }
    }
}

@Composable
private fun Counter(label: String, n: Int, onClick: () -> Unit) {
    Row(Modifier.background(C.woodD).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text("$label ", style = T.Small.copy(color = C.paperD))
        Text("$n", style = T.Small.copy(color = C.brass))
    }
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(C.woodD).navigationBarsPadding().padding(vertical = 6.dp)) {
        for ((t, l) in listOf(Tab.ROOM to R.string.tab_room, Tab.SHELF to R.string.tab_shelf, Tab.CHRONICLE to R.string.tab_chronicle, Tab.BOOK to R.string.tab_book)) {
            val on = t == tab
            Text((if (on) "▸" else "") + stringResource(l), style = T.Button.copy(color = if (on) C.brass else C.muted), textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 8.dp), maxLines = 1)
        }
    }
}

@Composable
private fun Toast(modifier: Modifier) {
    val bus = LocalBus.current
    val (text, stamp) = bus.toast
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(stamp) { if (text.isNotEmpty()) { shown = true; delay(3200); shown = false } }
    if (!shown || text.isEmpty()) return
    Text(text, style = T.Small.copy(color = C.ink), modifier = modifier.statusBarsPadding().padding(top = 56.dp, start = 14.dp, end = 14.dp).fillMaxWidth()
        .pixelBox(C.brass).padding(horizontal = 12.dp, vertical = 10.dp))
}

