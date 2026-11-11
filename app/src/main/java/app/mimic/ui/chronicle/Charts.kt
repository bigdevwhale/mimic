package app.mimic.ui.chronicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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

/** The chronicle's plain, readable pieces: cards, tiles, meters, bars and the year in days. Postcards use them too. */

internal val CARD = RoundedCornerShape(10.dp)

/** A plain rounded sheet of paper: the chronicle is for reading, not for playing. */
@Composable
internal fun Card(modifier: Modifier = Modifier, fill: Color = C.card, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().clip(CARD).background(fill).padding(horizontal = 14.dp, vertical = 12.dp), content = content)

@Composable
internal fun RowScope.Tile(n: String, label: String, fill: Color = C.card) {
    Column(Modifier.weight(1f).clip(CARD).background(fill).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n, style = T.Number)
        Text(label, style = T.Small, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
internal fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.Small, modifier = Modifier.weight(1f))
        Text(value, style = T.Strong, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
internal fun Divider() = Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(C.line))

/** One rounded bar on a track, filled to [f] of the width. */
@Composable
internal fun Meter(f: Float, color: Color, top: Dp = 3.dp) {
    Canvas(Modifier.padding(top = top).fillMaxWidth().height(8.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(C.line, cornerRadius = r)
        if (f > 0f) drawRoundRect(color, size = Size((size.width * f).coerceAtLeast(size.height), size.height), cornerRadius = r)
    }
}

/** Shares of a whole as one rounded bar split in colours. */
@Composable
internal fun Split(v: List<Int>, colors: List<Color>) {
    val total = v.sum()
    Canvas(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
        if (total == 0) { drawRect(C.line); return@Canvas }
        var x = 0f
        v.forEachIndexed { i, n ->
            val w = size.width * n / total
            drawRect(colors[i], Offset(x, 0f), Size(w, size.height))
            x += w
        }
    }
}

@Composable
internal fun Legend(labels: List<String>, colors: List<Color>) {
    Flow(Modifier.padding(top = 6.dp)) {
        labels.forEachIndexed { i, l ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                Text(" $l", style = T.Small.copy(color = C.text))
            }
        }
    }
}

/** Columns with their values on top and labels below, the tallest touching the top. */
@Composable
internal fun Bars(v: List<Int>, labels: List<String>, color: Color, fmt: (Int) -> String, height: Dp = 96.dp) {
    val max = (v.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth()) {
        v.forEachIndexed { i, x ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(height), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (x > 0) Text(fmt(x), style = T.Small.copy(color = C.text), maxLines = 1)
                        Canvas(Modifier.fillMaxWidth(.62f).height((height - 20.dp) * (x.toFloat() / max))) {
                            drawRoundRect(color, cornerRadius = CornerRadius(3.dp.toPx()))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                Text(labels[i], style = T.Small, maxLines = 1)
            }
        }
    }
}

/** The whole year, a column per week and a row per weekday, darker for longer play. */
@Composable
internal fun Heatmap(st: Chronicle, year: Int, empty: Color = C.line) {
    val first = LocalDate.of(year, 1, 1)
    val lead = first.dayOfWeek.value - 1
    val weeks = (lead + st.minutesPerDay.size + 6) / 7
    val max = st.minutesPerDay.maxOrNull()?.coerceAtLeast(1) ?: 1
    val today = LocalDate.now()
    Canvas(Modifier.fillMaxWidth().aspectRatio(weeks / 7f)) {
        val gap = 1.5.dp.toPx()
        val cell = minOf((size.width - gap * (weeks - 1)) / weeks, (size.height - gap * 6) / 7)
        val r = CornerRadius(cell / 4)
        for (d in st.minutesPerDay.indices) {
            val i = lead + d
            val m = st.minutesPerDay[d]
            val future = year == today.year && d >= today.dayOfYear
            val level = when { m <= 0 -> 0; m < max * .25f -> 1; m < max * .5f -> 2; m < max * .75f -> 3; else -> 4 }
            drawRoundRect(if (future) empty.copy(alpha = .4f) else if (level == 0) empty else heat(level), Offset((i / 7) * (cell + gap), (i % 7) * (cell + gap)), Size(cell, cell), r)
        }
    }
}

internal fun heat(level: Int) = when (level) {
    0 -> C.line
    1 -> C.pink.copy(alpha = .3f)
    2 -> C.pink.copy(alpha = .55f)
    3 -> C.pink.copy(alpha = .8f)
    else -> C.pinkL
}

internal fun pct(n: Int, total: Int) = if (total == 0) "0%" else "${(n * 100f / total).let { kotlin.math.round(it).toInt() }}%"

/** Short time for chart labels: minutes under an hour, hours with one decimal above. */
internal fun hoursShort(m: Int, ru: Boolean): String {
    val (h, min) = if (ru) "ч" to "м" else "h" to "m"
    return when {
        m <= 0 -> ""
        m < 60 -> "$m$min"
        m % 60 == 0 || m >= 600 -> "${(m + 30) / 60}$h"
        else -> String.format(Locale.US, "%.1f", m / 60f) + h
    }
}
