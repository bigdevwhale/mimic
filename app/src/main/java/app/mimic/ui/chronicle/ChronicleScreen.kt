package app.mimic.ui.chronicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.data.Gave
import app.mimic.domain.Stats
import app.mimic.ui.Home
import app.mimic.ui.Plural
import app.mimic.ui.art.MimicArt
import app.mimic.ui.art.MimicPose
import app.mimic.ui.components.BufImage
import app.mimic.ui.components.Chip
import app.mimic.ui.components.Flow
import app.mimic.ui.components.Panel
import app.mimic.ui.components.Section
import app.mimic.ui.sheets.gaveName
import app.mimic.ui.sheets.genreName
import app.mimic.ui.sheets.minutesText
import app.mimic.ui.sheets.traitName
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The chronicle: a year in games, from what the mimic ate and the sessions logged. */
@Composable
fun ChronicleScreen(h: Home) {
    var year by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }
    var hours by rememberSaveable { mutableStateOf(false) }
    val st = remember(h.games, h.sessions, year) { Stats.year(h.games, h.sessions, year, h.genres) }
    val loc = if (h.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (st.years.size > 1) Flow { for (y in st.years) Chip("$y", y == year, { year = y }, dark = true) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Big(st.eaten.toString(), stringResource(R.string.c_eaten), Modifier.weight(1f))
            Big(st.sessions.toString(), stringResource(R.string.c_sessions), Modifier.weight(1f))
            Big(((st.minutesPerMonth.sum() + 30) / 60).toString(), stringResource(R.string.c_hours), Modifier.weight(1f))
        }
        Section(stringResource(R.string.c_by_month))
        Flow {
            Chip(stringResource(R.string.c_games), !hours, { hours = false }, dark = true)
            Chip(stringResource(R.string.c_time), hours, { hours = true }, dark = true)
        }
        Panel(Modifier.fillMaxWidth()) {
            val v = if (hours) st.minutesPerMonth else st.perMonth
            Bars(v.map { it.toFloat() }, if (hours) C.brassD else C.crim)
            Row(Modifier.fillMaxWidth()) {
                for (m in 1..12) Text(java.time.Month.of(m).getDisplayName(TextStyle.NARROW_STANDALONE, loc), style = T.Small, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            val best = v.indices.maxByOrNull { v[it] }
            if (best != null && v[best] > 0) Text(stringResource(R.string.c_best_month, java.time.Month.of(best + 1).getDisplayName(TextStyle.FULL_STANDALONE, loc),
                if (hours) minutesText(v[best], h.ru) else Plural.games(v[best], h.ru)), style = T.Small.copy(color = C.ink))
        }
        if (st.genres.isNotEmpty()) {
            Section(stringResource(R.string.c_genres))
            Panel(Modifier.fillMaxWidth()) {
                val max = st.genres.maxOf { it.second }
                for ((g, n) in st.genres) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(g.name(h.ru) ?: stringResource(genreName(g.builtin!!)), style = T.Small.copy(color = C.ink), modifier = Modifier.width(96.dp), maxLines = 1)
                    Box(Modifier.weight(1f).height(14.dp)) { Canvas(Modifier.fillMaxWidth().height(14.dp)) { drawRect(Color(g.color), size = Size(size.width * n / max, size.height)) } }
                    Text("  $n", style = T.Small.copy(color = C.ink))
                }
                Text(stringResource(R.string.c_split, st.video, st.board), style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (st.traits.isNotEmpty() || st.eaten > 0) {
            Section(stringResource(R.string.c_mimicling, year))
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BufImage(remember(st.traits) { app.mimic.ui.art.Buf(MimicArt.W, MimicArt.H).also { MimicArt.draw(MimicPose(1, st.traits), it) } }, scale = 1.5.dp)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.c_mimicling_text), style = T.Small.copy(color = C.ink))
                        if (st.traits.isNotEmpty()) Text(st.traits.map { stringResource(traitName(it)) }.joinToString(", "), style = T.Body)
                    }
                }
            }
        }
        if (st.gave.isNotEmpty()) {
            Section(stringResource(R.string.c_gave))
            Panel(Modifier.fillMaxWidth()) { for (g in Gave.entries) st.gave[g]?.let { Text("${stringResource(gaveName(g))} — $it", style = T.Body) } }
        }
        if (st.avgRating != null) {
            Section(stringResource(R.string.c_rating))
            Panel(Modifier.fillMaxWidth()) {
                Line(stringResource(R.string.c_avg), String.format(java.util.Locale.US, "%.1f", st.avgRating))
                st.best?.let { b ->
                    Text(stringResource(R.string.c_best), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                    Text(b.title, style = T.Heading)
                    app.mimic.ui.components.Stars(b.rating, size = 16.dp)
                    if (b.review.isNotBlank()) Text(b.review, style = T.Body)
                }
            }
        }
        st.longest?.let { (g, m) ->
            Section(stringResource(R.string.c_longest))
            Panel(Modifier.fillMaxWidth()) { Text(g.title, style = T.Heading); Text(minutesText(m, h.ru) + " · " + stringResource(R.string.c_shots, st.shots), style = T.Small) }
        }
        Section(stringResource(R.string.c_pile))
        Panel(Modifier.fillMaxWidth()) {
            Line(stringResource(R.string.c_pile_now), st.pile.toString())
            Line(stringResource(R.string.c_in_hands), st.hands.toString())
            st.avgWait?.let { Line(stringResource(R.string.c_wait), Plural.span(it, h.ru)) }
            st.oldest?.let { Line(stringResource(R.string.c_oldest), it.title) }
            Line(stringResource(R.string.c_let), st.letGo.toString())
            Line(stringResource(R.string.c_lent), st.lent.toString())
        }
    }
}

@Composable
private fun Big(n: String, label: String, modifier: Modifier) {
    Panel(modifier, fill = C.paper) {
        Text(n, style = T.Number, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(label, style = T.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ColumnScope.Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = T.Small, modifier = Modifier.weight(1f))
        Text(value, style = T.Body, textAlign = TextAlign.End)
    }
}

/** Twelve pixel bars, the tallest touching the top. */
@Composable
private fun Bars(v: List<Float>, color: Color) {
    val max = (v.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val cw = size.width / 12
        val p = 2.dp.toPx()
        drawRect(C.paperD, Offset(0f, size.height - p), Size(size.width, p))
        v.forEachIndexed { i, x ->
            if (x <= 0f) return@forEachIndexed
            val bh = ((size.height - p) * x / max / p).toInt().coerceAtLeast(1) * p
            drawRect(C.ink, Offset(i * cw + cw * .2f - p, size.height - p - bh - p), Size(cw * .6f + 2 * p, bh + p))
            drawRect(color, Offset(i * cw + cw * .2f, size.height - p - bh), Size(cw * .6f, bh))
        }
    }
}
