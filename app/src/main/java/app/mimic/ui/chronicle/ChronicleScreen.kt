package app.mimic.ui.chronicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.ui.components.Tip
import app.mimic.ui.components.Tips
import app.mimic.data.Gave
import app.mimic.data.Kind
import app.mimic.domain.Chronicle
import app.mimic.domain.Stats
import app.mimic.ui.Home
import app.mimic.ui.Plural
import app.mimic.ui.art.Buf
import app.mimic.ui.art.MimicArt
import app.mimic.ui.art.MimicPose
import app.mimic.ui.components.BufImage
import app.mimic.ui.components.Chip
import app.mimic.ui.components.PixelButton
import app.mimic.ui.Nav
import app.mimic.ui.postcard.Subject
import app.mimic.ui.components.Kind as Btn
import app.mimic.ui.components.Flow
import app.mimic.ui.components.Section
import app.mimic.ui.components.Stars
import app.mimic.ui.sheets.gaveName
import app.mimic.ui.sheets.genreName
import app.mimic.ui.sheets.kindName
import app.mimic.ui.sheets.minutesText
import app.mimic.ui.sheets.traitName
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** The chronicle's tabs; a postcard of the year shows the same part. */
enum class Part(val label: Int) { OVERVIEW(R.string.c_tab_overview), TIME(R.string.c_tab_time), TASTE(R.string.c_tab_taste), PILE(R.string.c_tab_pile) }

/**
 * The chronicle: a year in games, from what the mimic ate and the sessions logged, in four tabs. The
 * headings and the mimicling stay pixel; the numbers and charts are drawn plainly, to be read.
 */
@Composable
fun ChronicleScreen(h: Home, nav: Nav) {
    var year by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }
    var part by rememberSaveable { mutableStateOf(Part.OVERVIEW) }
    val st = remember(h.games, h.sessions, year) { Stats.year(h.games, h.sessions, year, h.genres) }
    val loc = if (h.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Tip(Tips.CHRONICLE, h.s, stringResource(R.string.tip_chronicle))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Flow(Modifier.weight(1f)) { for (y in st.years) Chip("$y", y == year, { year = y }, dark = true) }
                PixelButton(stringResource(R.string.pc_share), { nav.postcard(Subject.Year(year, part)) }, kind = Btn.BRASS, small = true)
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.bgD).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (p in Part.entries) TabChip(stringResource(p.label), p == part, Modifier.weight(1f)) { part = p }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (part) {
                Part.OVERVIEW -> Overview(st, h, loc)
                Part.TIME -> Time(st, h, loc)
                Part.TASTE -> Taste(st, h)
                Part.PILE -> Pile(st, h, loc)
            }
        }
    }
}

/** A tab: brass when on, the frame's own colour when off. */
@Composable
private fun TabChip(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(if (on) C.neon else Color.Transparent).clickable(onClick = onClick).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.Tag.copy(color = if (on) C.onNeon else C.sub), maxLines = 1)
    }
}

@Composable
private fun Overview(st: Chronicle, h: Home, loc: Locale) {
    var hours by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(st.eaten.toString(), stringResource(R.string.c_eaten))
        Tile(((st.minutesPerMonth.sum() + 30) / 60).toString(), stringResource(R.string.c_hours))
        Tile(st.sessions.toString(), stringResource(R.string.c_sessions))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(st.playDays.toString(), stringResource(R.string.c_play_days))
        Tile(st.streak.toString(), stringResource(R.string.c_streak))
        Tile(st.avgSession?.toString() ?: "–", stringResource(R.string.c_per_session))
    }
    Section(stringResource(R.string.c_calendar))
    Card {
        Heatmap(st, st.year)
        HeatLegend(st)
    }
    Section(stringResource(R.string.c_by_month))
    Flow {
        Chip(stringResource(R.string.c_games), !hours, { hours = false }, dark = true)
        Chip(stringResource(R.string.c_time), hours, { hours = true }, dark = true)
    }
    Card {
        val v = if (hours) st.minutesPerMonth else st.perMonth
        Bars(v.toList(), months(loc), if (hours) C.neonD else C.pink, { if (hours) hoursShort(it, h.ru) else it.toString() })
        val best = v.indices.maxByOrNull { v[it] }
        if (best != null && v[best] > 0) Text(
            stringResource(R.string.c_best_month, Month.of(best + 1).getDisplayName(TextStyle.FULL_STANDALONE, loc), if (hours) minutesText(v[best], h.ru) else Plural.games(v[best], h.ru)),
            style = T.Small.copy(color = C.text), modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
internal fun HeatLegend(st: Chronicle) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.c_calendar_note, st.playDays, st.minutesPerDay.size), style = T.Small, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.c_less) + " ", style = T.Small)
        for (i in 0..4) Box(Modifier.padding(horizontal = 1.dp).size(9.dp).clip(RoundedCornerShape(2.dp)).background(heat(i)))
        Text(" " + stringResource(R.string.c_more), style = T.Small)
    }
}

internal fun months(loc: Locale) = (1..12).map { Month.of(it).getDisplayName(TextStyle.NARROW_STANDALONE, loc) }

@Composable
internal fun dayParts() = listOf(R.string.c_morning, R.string.c_daytime, R.string.c_evening, R.string.c_night).map { stringResource(it) }
internal val PART_COLORS get() = listOf(C.neon, C.done, C.pink, C.pile)

/** «Most of all: evening, Saturday», or null without sessions. */
@Composable
internal fun mostly(st: Chronicle, loc: Locale): String? {
    if (st.sessions == 0) return null
    val wd = st.weekday.indices.maxByOrNull { st.weekday[it] }!!
    val dp = st.dayPart.indices.maxByOrNull { st.dayPart[it] }!!
    return stringResource(R.string.c_mostly, dayParts()[dp], DayOfWeek.of(wd + 1).getDisplayName(TextStyle.FULL_STANDALONE, loc))
}

@Composable
private fun Time(st: Chronicle, h: Home, loc: Locale) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(((st.minutesPerMonth.sum() + 30) / 60).toString(), stringResource(R.string.c_hours))
        Tile(st.playDays.toString(), stringResource(R.string.c_play_days))
        Tile(st.avgSession?.toString() ?: "–", stringResource(R.string.c_per_session))
    }
    Section(stringResource(R.string.c_when))
    Card {
        if (st.sessions == 0) Text(stringResource(R.string.c_no_sessions), style = T.Small)
        else {
            Weekdays(st, h.ru, loc)
            Spacer(Modifier.height(12.dp))
            Split(st.dayPart.toList(), PART_COLORS)
            Legend(dayParts().zip(st.dayPart.toList()).map { "${it.first} ${pct(it.second, st.sessions)}" }, PART_COLORS)
            mostly(st, loc)?.let { Text(it, style = T.Small.copy(color = C.text), modifier = Modifier.padding(top = 6.dp)) }
        }
    }
    Section(stringResource(R.string.c_by_month))
    Card { Bars(st.minutesPerMonth.toList(), months(loc), C.neonD, { hoursShort(it, h.ru) }) }
    if (st.top.isNotEmpty()) {
        Section(stringResource(R.string.c_top))
        Card { TopGames(st, h.ru) }
    }
}

@Composable
internal fun Weekdays(st: Chronicle, ru: Boolean, loc: Locale, height: Dp = 80.dp) =
    Bars(st.weekday.toList(), DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT_STANDALONE, loc) }, C.away, { hoursShort(it, ru) }, height = height)

@Composable
internal fun TopGames(st: Chronicle, ru: Boolean, count: Int = 5) {
    val max = st.top.first().second
    for ((g, m, n) in st.top.take(count)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text(g.title, style = T.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(minutesText(m, ru) + " · " + Plural.sessions(n, ru), style = T.Small)
        }
        Meter(m.toFloat() / max, if (g.color != 0) Color(g.color or 0xFF000000.toInt()) else C.neonD)
    }
    if (st.shots > 0 && count >= 5) Text(stringResource(R.string.c_shots, st.shots), style = T.Small, modifier = Modifier.padding(top = 8.dp))
}

@Composable
internal fun GenreBars(st: Chronicle, ru: Boolean, count: Int = 99) {
    val max = st.genres.maxOf { it.second }
    for ((g, n) in st.genres.take(count)) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(g.name(ru) ?: stringResource(genreName(g.builtin!!)), style = T.Body, modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(1f)) { Meter(n.toFloat() / max, Color(g.color), top = 0.dp) }
        Text(n.toString(), style = T.Strong, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun Taste(st: Chronicle, h: Home) {
    if (st.genres.isEmpty() && st.avgRating == null) { Card { Text(stringResource(R.string.c_nothing_eaten), style = T.Small) }; return }
    if (st.genres.isNotEmpty()) {
        Section(stringResource(R.string.c_genres))
        Card {
            GenreBars(st, h.ru)
            val kinds = listOf(st.video, st.board, st.rpg)
            val kindColors = listOf(C.away, C.done, C.pink)
            Spacer(Modifier.height(10.dp))
            Split(kinds, kindColors)
            Legend(Kind.entries.mapIndexed { i, k -> "${stringResource(kindName(k)).let { n -> if (n == n.uppercase()) n else n.lowercase() }} ${kinds[i]}" }, kindColors)
        }
    }
    if (st.traits.isNotEmpty() || st.eaten > 0) {
        Section(stringResource(R.string.c_mimicling, st.year))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BufImage(remember(st.traits) { Buf(MimicArt.W, MimicArt.H).also { MimicArt.draw(MimicPose(1, st.traits), it) } }, scale = 1.5.dp)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.c_mimicling_text), style = T.Small)
                    if (st.traits.isNotEmpty()) Text(st.traits.map { stringResource(traitName(it)) }.joinToString(", "), style = T.Strong, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    if (st.gave.isNotEmpty()) {
        Section(stringResource(R.string.c_gave))
        Card {
            val total = st.gave.values.sum()
            val colors = listOf(C.pink, C.done, C.pile, C.neonD)
            for ((i, g) in Gave.entries.withIndex()) {
                val n = st.gave[g] ?: 0
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(stringResource(gaveName(g)), style = T.Body, modifier = Modifier.weight(1f))
                    Text("$n · ${pct(n, total)}", style = T.Small)
                }
                Meter(n.toFloat() / total, colors[i])
            }
        }
    }
    if (st.avgRating != null) {
        Section(stringResource(R.string.c_rating))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format(Locale.US, "%.1f", st.avgRating), style = T.Number)
                    Text(stringResource(R.string.c_avg), style = T.Small, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.c_rated, st.stars.sum(), st.eaten), style = T.Small, textAlign = TextAlign.Center)
                }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    val max = st.stars.max().coerceAtLeast(1)
                    for (s in 5 downTo 1) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("$s★", style = T.Small.copy(color = C.text), modifier = Modifier.width(28.dp))
                        Box(Modifier.weight(1f)) { Meter(st.stars[s - 1].toFloat() / max, C.neon, top = 0.dp) }
                        Text(st.stars[s - 1].toString(), style = T.Small.copy(color = C.text), textAlign = TextAlign.End, modifier = Modifier.width(22.dp))
                    }
                }
            }
            st.best?.let { b ->
                Divider()
                Text(stringResource(R.string.c_best), style = T.Small)
                Text(b.title, style = T.Heading)
                Stars(b.rating, size = 16.dp)
                if (b.review.isNotBlank()) Text(b.review, style = T.Body, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
internal fun pileHeadline(st: Chronicle): String {
    val delta = st.added - st.eaten - st.letGo
    return when { delta > 0 -> stringResource(R.string.c_grew, delta); delta < 0 -> stringResource(R.string.c_shrank, -delta); else -> stringResource(R.string.c_steady) }
}

@Composable
private fun Pile(st: Chronicle, h: Home, loc: Locale) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(st.pile.toString(), stringResource(R.string.ct_pile))
        Tile(st.added.toString(), stringResource(R.string.c_new))
        Tile(st.eaten.toString(), stringResource(R.string.c_eaten))
    }
    Card {
        Text(pileHeadline(st), style = T.Strong)
        Text(stringResource(R.string.c_balance, st.added, st.eaten, st.letGo), style = T.Small)
    }
    Section(stringResource(R.string.c_new))
    Card { Bars(st.addedPerMonth.toList(), months(loc), C.pile, { it.toString() }) }
    Section(stringResource(R.string.c_pile))
    Card {
        Line(stringResource(R.string.c_pile_now), st.pile.toString())
        Line(stringResource(R.string.c_in_hands), st.hands.toString())
        Line(stringResource(R.string.c_lent), st.lent.toString())
        Line(stringResource(R.string.c_let), st.letGo.toString())
        st.avgWait?.let { Line(stringResource(R.string.c_wait), Plural.span(it, h.ru)) }
        st.toCredits?.let { Line(stringResource(R.string.c_to_credits), Plural.span(it, h.ru)) }
        st.oldest?.let { Line(stringResource(R.string.c_oldest), it.title) }
    }
}
