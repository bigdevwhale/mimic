package app.mimic.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import app.mimic.ui.components.IconBtn
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
import app.mimic.ui.components.Tips
import app.mimic.container
import app.mimic.data.Draft
import app.mimic.data.Settings
import app.mimic.ru
import app.mimic.ui.book.BookScreen
import app.mimic.ui.chronicle.ChronicleScreen
import app.mimic.ui.postcard.Look
import app.mimic.ui.postcard.PostcardScreen
import app.mimic.ui.postcard.Subject
import app.mimic.ui.components.surface
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
    data class Postcard(val subject: Subject, val look: Look = Look.NIGHT) : Overlay()
}

/** Ways screens move around. */
class Nav(val go: (Overlay?) -> Unit, val tab: (Tab, String?) -> Unit) {
    fun card(id: Long) = go(Overlay.Card(id))
    fun dig(id: Long?) = go(Overlay.Dig(id))
    fun sniff() = go(Overlay.Sniff)
    fun add() = go(Overlay.Add())
    fun postcard(subject: Subject) = go(Overlay.Postcard(subject))
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
    LaunchedEffect(Unit) {
        if (bus.said.first.isNotEmpty()) return@LaunchedEffect
        if (!Tips.shown(s, Tips.ROOM)) { bus.say(ctx.getString(R.string.tip_room)); c.write { settings.meet(Tips.key(Tips.ROOM)) } }
        else bus.say(ctx.getString(if (list.isEmpty()) R.string.say_hello_empty else R.string.say_hello))
    }

    CompositionLocalProvider(LocalBus provides bus) {
        Box(Modifier.fillMaxSize().background(C.bg)) {
            Column(Modifier.fillMaxSize()) {
                Header(h, nav, onSettings = { overlay = Overlay.Settings })
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.ROOM -> RoomScreen(h, nav)
                        Tab.SHELF -> ShelfScreen(h, nav, filter) { filter = it }
                        Tab.CHRONICLE -> ChronicleScreen(h, nav)
                        Tab.BOOK -> BookScreen(h, nav)
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
                is Overlay.Postcard -> PostcardScreen(h, o.subject, nav, o.look)
                null -> Unit
            }
            Toast(Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun Header(h: Home, nav: Nav, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.app_title), style = T.Display.copy(color = C.neon, letterSpacing = 2.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), modifier = Modifier.weight(1f), maxLines = 1)
        Counter(stringResource(R.string.ct_shelf), h.games.size - h.hands.size, C.neon) { nav.shelf("all") }
        Counter(stringResource(R.string.ct_pile), h.pile.size, C.pile) { nav.shelf("pile") }
        Counter(stringResource(R.string.ct_eaten), h.mimic.eaten, C.pink) { nav.shelf("done") }
        IconBtn(Icons.Rounded.Settings, stringResource(R.string.settings), onSettings)
    }
}

/** A counter: the number in its neon over a quiet label. */
@Composable
private fun Counter(label: String, n: Int, color: Color, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", style = T.Heading.copy(color = color))
        Text(label, style = T.Small.copy(fontSize = 11.sp, lineHeight = 13.sp, color = C.dim))
    }
}

/** The tabs on the console's dark body: an icon and a word, the chosen one lit lavender with a short bar on top. */
@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(C.bezel).navigationBarsPadding()) {
        for ((t, l, icon) in listOf(
            Triple(Tab.ROOM, R.string.tab_room, Icons.Rounded.Home), Triple(Tab.SHELF, R.string.tab_shelf, Icons.Rounded.ViewModule),
            Triple(Tab.CHRONICLE, R.string.tab_chronicle, Icons.Rounded.Insights), Triple(Tab.BOOK, R.string.tab_book, Icons.Rounded.Pets),
        )) {
            val on = t == tab
            val color = if (on) C.neonL else C.dim
            Column(Modifier.weight(1f).clickable { onTab(t) }.padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(width = 28.dp, height = 3.dp).clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)).background(if (on) C.neonL else Color.Transparent))
                Spacer(Modifier.height(7.dp))
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                Text(stringResource(l), style = T.Tag.copy(color = color, fontSize = 12.sp), maxLines = 1)
            }
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
    Text(text, style = T.Small.copy(color = C.text), modifier = modifier.statusBarsPadding().padding(top = 56.dp, start = 14.dp, end = 14.dp).fillMaxWidth()
        .surface(C.neon).padding(horizontal = 12.dp, vertical = 10.dp))
}

