package app.mimic.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.mimic.BuildConfig
import app.mimic.R
import app.mimic.container
import app.mimic.data.Settings
import app.mimic.domain.Place
import app.mimic.domain.Sky
import app.mimic.ru
import app.mimic.ui.Nav
import app.mimic.ui.components.Chip
import app.mimic.ui.components.Field
import app.mimic.ui.components.Flow
import app.mimic.ui.components.Kind
import app.mimic.ui.components.Page
import app.mimic.ui.components.Panel
import app.mimic.ui.components.PixelButton
import app.mimic.ui.components.Section
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "mimic"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** A town search for the window. */
@Composable
fun PlacePicker(onPicked: (Place) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    fun go() {
        if (q.isNotBlank()) scope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Sky.search(q, if (ctx.ru) "ru" else "en") } }
            failed = r.isFailure
            results = r.getOrDefault(emptyList())
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(q, { q = it }, stringResource(R.string.place_search), Modifier.weight(1f), onDone = ::go)
            PixelButton(stringResource(R.string.search), ::go, small = true)
        }
        if (failed) Text(stringResource(R.string.place_fail), style = T.Small.copy(color = C.crimL))
        for (p in results) Panel(Modifier.fillMaxWidth(), onClick = { results = emptyList(); q = p.name; focus.clearFocus(); onPicked(p) }) {
            Text(p.name, style = T.Body)
            Text(p.region, style = T.Small)
        }
    }
}

@Composable
fun SettingsPage(s: Settings, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Page(stringResource(R.string.settings), nav::close) {
        Section(stringResource(R.string.set_covers))
        Text(stringResource(R.string.set_covers_hint), style = T.Small.copy(color = C.paperD))
        Flow {
            Chip(stringResource(R.string.set_covers_px), s.pixelCovers, { c.write { settings.update { it.copy(pixelCovers = true) } } }, dark = true)
            Chip(stringResource(R.string.set_covers_raw), !s.pixelCovers, { c.write { settings.update { it.copy(pixelCovers = false) } } }, dark = true)
        }

        Section(stringResource(R.string.set_window))
        Text(s.place?.let { stringResource(R.string.place_now, it.name) } ?: stringResource(R.string.place_none), style = T.Body.copy(color = C.paper))
        Text(stringResource(R.string.place_hint), style = T.Small.copy(color = C.paperD))
        PlacePicker { picked -> c.write { settings.update { it.copy(place = picked, outside = null) }; refreshWeather(force = true) } }
        Flow {
            Chip(stringResource(R.string.set_wx_live), s.liveWeather, { c.write { settings.update { it.copy(liveWeather = true) }; refreshWeather(force = true) } }, dark = true)
            Chip(stringResource(R.string.set_wx_fair), !s.liveWeather, { c.write { settings.update { it.copy(liveWeather = false) } } }, dark = true)
        }
        if (s.place != null) PixelButton(stringResource(R.string.place_off), { c.write { settings.update { it.copy(place = null, outside = null) } } }, Modifier.fillMaxWidth(), kind = Kind.PALE, small = true)

        Section(stringResource(R.string.set_genres))
        Text(stringResource(R.string.set_genres_hint), style = T.Small.copy(color = C.paperD))
        val styles by c.shelf.styles.collectAsStateWithLifecycle(initialValue = emptyList())
        var asking by remember { mutableStateOf<Long?>(null) }
        if (styles.isEmpty()) Text(stringResource(R.string.set_genres_none), style = T.Body.copy(color = C.paper))
        for (st in styles.filter { it.top } + styles.filter { !it.top }) {
            val parent = st.parent?.let { stringResource(app.mimic.ui.sheets.genreName(it)) } ?: st.parentStyle?.let { id -> styles.firstOrNull { it.id == id }?.name }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text((if (parent != null) "$parent · " else "") + st.name, style = T.Body.copy(color = C.paper), modifier = Modifier.weight(1f))
                Text(stringResource(if (asking == st.id) R.string.set_genre_sure else R.string.set_genre_del), style = T.Small.copy(color = if (asking == st.id) C.crimL else C.paperD),
                    modifier = Modifier.clickable { if (asking == st.id) { c.write { shelf.deleteStyle(st) }; asking = null } else asking = st.id }.padding(6.dp))
            }
        }

        Section(stringResource(R.string.set_lang))
        val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
        Flow {
            for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                Chip(stringResource(l), cur == tag, { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }, dark = true)
        }

        Section(stringResource(R.string.community))
        Text(stringResource(R.string.community_text), style = T.Small.copy(color = C.paperD))
        for ((label, sub, url) in listOf(
            Triple(R.string.community_bug, R.string.community_bug_sub, Community.BUG),
            Triple(R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
            Triple(R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
            Triple(R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
        )) Panel(Modifier.fillMaxWidth(), onClick = { open(url) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Body); Text(stringResource(sub), style = T.Small) }
                Text("›", style = T.Heading.copy(color = C.crim))
            }
        }

        Section(stringResource(R.string.about))
        Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Small.copy(color = C.paperD))
        PixelButton(stringResource(R.string.source), { open("https://github.com/bigdevwhale/mimic") }, kind = Kind.WOOD, small = true)
    }
}
