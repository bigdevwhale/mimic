package app.mimic.ui.postcard

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import app.mimic.R
import app.mimic.ui.components.IconBtn
import app.mimic.ui.components.Tip
import app.mimic.ui.components.Tips
import app.mimic.data.Game
import app.mimic.domain.Chronicle
import app.mimic.domain.Stats
import app.mimic.ui.Home
import app.mimic.ui.LocalBus
import app.mimic.ui.Nav
import app.mimic.ui.Plural
import app.mimic.ui.art
import app.mimic.ui.art.Buf
import app.mimic.ui.art.Critters
import app.mimic.ui.art.HandItem
import app.mimic.ui.art.MimicArt
import app.mimic.ui.art.MimicPose
import app.mimic.ui.art.RoomFx
import app.mimic.ui.art.RoomModel
import app.mimic.ui.art.RoomScene
import app.mimic.ui.art.ShelfItem
import app.mimic.ui.book.creatureText
import app.mimic.ui.chronicle.Bars
import app.mimic.ui.chronicle.GenreBars
import app.mimic.ui.chronicle.Heatmap
import app.mimic.ui.chronicle.Line
import app.mimic.ui.chronicle.Part
import app.mimic.ui.chronicle.Split
import app.mimic.ui.chronicle.Legend
import app.mimic.ui.chronicle.PART_COLORS
import app.mimic.ui.chronicle.Tile
import app.mimic.ui.chronicle.TopGames
import app.mimic.ui.chronicle.Weekdays
import app.mimic.ui.chronicle.dayParts
import app.mimic.ui.chronicle.months
import app.mimic.ui.chronicle.mostly
import app.mimic.ui.chronicle.pct
import app.mimic.ui.chronicle.pileHeadline
import app.mimic.ui.chronicle.hoursShort
import app.mimic.ui.components.BufImage
import app.mimic.ui.components.Chip
import app.mimic.ui.components.Flow
import app.mimic.ui.components.PixelButton
import app.mimic.ui.components.Stars
import app.mimic.ui.components.Kind as Btn
import app.mimic.ui.look
import app.mimic.ui.room.CoverImage
import app.mimic.ui.sheets.GamePlate
import app.mimic.ui.sheets.minutesText
import app.mimic.ui.sheets.statusLine
import app.mimic.ui.sheets.traitName
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import app.mimic.ui.tint
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Locale

/** What a postcard shows. */
sealed class Subject {
    data class Year(val year: Int, val part: Part) : Subject()
    data class Game(val id: Long) : Subject()
    data object Room : Subject()
    data object Mimic : Subject()
    data class Creature(val kind: Critters.Kind) : Subject()
}

/** A postcard's dress: the background around the paper, the ink on it and the accent of its stamp. */
enum class Look(val label: Int, val top: Color, val bottom: Color, val ink: Color, val accent: Color) {
    NIGHT(R.string.pc_night, Color(0xFFE9E7EF), Color(0xFFCFCBDB), C.text, C.neon),
    PAPER(R.string.pc_paper, Color(0xFF3A3556), Color(0xFF1C1A28), Color(0xFFE4E2EA), Color(0xFFA79BE0)),
    SUNSET(R.string.pc_sunset, Color(0xFFE0464B), Color(0xFFA82A2F), Color.White, Color.White),
    FOREST(R.string.pc_forest, Color(0xFF9BBC0F), Color(0xFF7A9A2A), Color(0xFF0F380F), Color(0xFF0F380F)),
}

/** The postcard maker: pick what to show and how it's dressed, then share it or save it to the gallery. */
@Composable
fun PostcardScreen(h: Home, first: Subject, nav: Nav, look0: Look = Look.NIGHT) {
    val ctx = LocalContext.current
    val bus = LocalBus.current
    val scope = rememberCoroutineScope()
    var subject by remember(first) { mutableStateOf(first) }
    var look by rememberSaveable { mutableStateOf(look0) }
    val layer = rememberGraphicsLayer()
    val year = LocalDate.now().year
    BackHandler(onBack = nav::close)

    fun capture(then: (Bitmap) -> Unit) = scope.launch { then(layer.toImageBitmap().asAndroidBitmap()) }

    Column(Modifier.fillMaxSize().background(C.bg).clickable(remember { MutableInteractionSource() }, null) {}.statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pc_title), style = T.Title, modifier = Modifier.weight(1f))
            IconBtn(Icons.Rounded.Close, stringResource(R.string.close), nav::close)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Tip(Tips.POSTCARD, h.s, stringResource(R.string.tip_postcard))
            Box(Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.widthIn(max = 420.dp).fillMaxWidth(.86f).drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }) {
                    Postcard(h, subject, look)
                }
            }
            Text(stringResource(R.string.pc_what), style = T.Tag.copy(color = C.sub))
            Flow {
                val yearOn = subject is Subject.Year
                Chip(stringResource(R.string.pc_year), yearOn, { if (!yearOn) subject = Subject.Year(year, Part.OVERVIEW) }, dark = true)
                Chip(stringResource(R.string.pc_room), subject == Subject.Room, { subject = Subject.Room }, dark = true)
                Chip(stringResource(R.string.pc_mimic), subject == Subject.Mimic, { subject = Subject.Mimic }, dark = true)
                (first as? Subject.Game)?.let { s -> h.game(s.id)?.let { Chip(it.title, subject == s, { subject = s }, dark = true) } }
                h.hands.filter { first != Subject.Game(it.id) }.forEach { g -> Chip(g.title, subject == Subject.Game(g.id), { subject = Subject.Game(g.id) }, dark = true) }
                (first as? Subject.Creature)?.let { s -> Chip(stringResource(creatureText(s.kind).first), subject == s, { subject = s }, dark = true) }
            }
            (subject as? Subject.Year)?.let { y ->
                Flow { for (p in Part.entries) Chip(stringResource(p.label), y.part == p, { subject = y.copy(part = p) }, dark = true) }
            }
            Text(stringResource(R.string.pc_look), style = T.Tag.copy(color = C.sub))
            Flow { for (l in Look.entries) Chip(stringResource(l.label), look == l, { look = l }, dark = true) }
        }
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (Build.VERSION.SDK_INT >= 29) PixelButton(stringResource(R.string.pc_save), {
                capture { bmp -> bus.toast(ctx.getString(if (save(ctx, bmp)) R.string.pc_saved else R.string.pc_save_fail)) }
            }, kind = Btn.PALE)
            PixelButton(stringResource(R.string.pc_share), { capture { share(ctx, it) } }, kind = Btn.CRIM)
        }
    }
}

/** The card itself, 4:5 like a phone photo: the dress, a logo and a stamp on top, the paper with the subject, a postmark below. */
@Composable
fun Postcard(h: Home, subject: Subject, look: Look) {
    val st = (subject as? Subject.Year)?.let { y -> remember(h.games, h.sessions, y.year) { Stats.year(h.games, h.sessions, y.year, h.genres) } }
    Column(
        Modifier.fillMaxWidth().aspectRatio(.75f).clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(look.top, look.bottom)))
            .drawBehind { sparkles(look) }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_title), style = T.Display.copy(color = look.accent))
                Text(stringResource(R.string.pc_tagline), style = T.Small.copy(color = look.ink.copy(alpha = .8f)))
            }
            Stamp(h, look, (subject as? Subject.Year)?.year ?: LocalDate.now().year)
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.card).padding(14.dp)) {
            when (subject) {
                is Subject.Year -> YearCard(st!!, subject.part, h)
                is Subject.Game -> h.game(subject.id)?.let { GameCard(it, h) }
                Subject.Room -> RoomCard(h)
                Subject.Mimic -> MimicCard(h)
                is Subject.Creature -> CreatureCard(subject.kind)
            }
        }
        Text(stringResource(R.string.pc_footer), style = T.Tag.copy(color = look.ink.copy(alpha = .75f)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
    }
}

/** A few pixel stars (or motes on light dresses) scattered over the background, the same every time. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.sparkles(look: Look) {
    val p = 2.dp.toPx()
    val c = look.ink.copy(alpha = .18f)
    val r = java.util.Random(look.ordinal * 31L + 7)
    repeat(26) {
        val x = r.nextFloat() * size.width
        val y = r.nextFloat() * size.height
        drawRect(c, Offset(x, y), Size(p, p))
        if (it % 5 == 0) { drawRect(c, Offset(x - p, y), Size(p, p)); drawRect(c, Offset(x + p, y), Size(p, p)); drawRect(c, Offset(x, y - p), Size(p, p)); drawRect(c, Offset(x, y + p), Size(p, p)) }
    }
}

/** A perforated stamp with the mimic as it is now and the year. */
@Composable
private fun Stamp(h: Home, look: Look, year: Int) {
    val mimic = remember(h.mimic) { Buf(MimicArt.W, MimicArt.H).also { MimicArt.draw(MimicPose(h.mimic.stage, h.mimic.traits), it) } }
    Box(Modifier.size(width = 64.dp, height = 74.dp).drawWithContent {
        drawRect(C.card2)
        val r = 3.dp.toPx()
        val bg = look.top
        var x = r
        while (x < size.width) { drawCircle(bg, r, Offset(x, 0f)); drawCircle(bg, r, Offset(x, size.height)); x += 3 * r }
        var y = r
        while (y < size.height) { drawCircle(bg, r, Offset(0f, y)); drawCircle(bg, r, Offset(size.width, y)); y += 3 * r }
        drawRect(look.accent, Offset(6.dp.toPx(), 6.dp.toPx()), Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawContent()
    }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BufImage(mimic, scale = .6.dp)
            Text("$year", style = T.Tag.copy(color = C.text))
        }
    }
}

@Composable
private fun ColumnScope.YearCard(st: Chronicle, part: Part, h: Home) {
    val loc = if (h.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
    Text(stringResource(R.string.pc_year_title, st.year), style = T.Title)
    Text(stringResource(part.label), style = T.Tag.copy(color = C.pink), modifier = Modifier.padding(bottom = 8.dp))
    when (part) {
        Part.OVERVIEW -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tile(st.eaten.toString(), stringResource(R.string.c_eaten), C.card2)
                Tile(((st.minutesPerMonth.sum() + 30) / 60).toString(), stringResource(R.string.c_hours), C.card2)
                Tile(st.playDays.toString(), stringResource(R.string.c_play_days), C.card2)
            }
            Spacer(Modifier.height(10.dp))
            Heatmap(st, st.year)
            Text(stringResource(R.string.c_calendar_note, st.playDays, st.minutesPerDay.size), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.weight(1f))
            st.best?.let { b -> Featured(b, h, stringResource(R.string.c_best)) }
        }
        Part.TIME -> {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(((st.minutesPerMonth.sum() + 30) / 60).toString(), style = T.Number)
                Text("  " + stringResource(R.string.c_hours) + " · " + Plural.sessions(st.sessions, h.ru), style = T.Small, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (st.sessions > 0) {
                Weekdays(st, h.ru, loc, height = 64.dp)
                Spacer(Modifier.height(6.dp))
                Split(st.dayPart.toList(), PART_COLORS)
                Legend(dayParts().zip(st.dayPart.toList()).map { "${it.first} ${pct(it.second, st.sessions)}" }, PART_COLORS)
                mostly(st, loc)?.let { Text(it, style = T.Strong, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.weight(1f))
                if (st.top.isNotEmpty()) TopGames(st, h.ru, count = 2)
            } else Text(stringResource(R.string.c_no_sessions), style = T.Small)
        }
        Part.TASTE -> {
            if (st.genres.isNotEmpty()) GenreBars(st, h.ru, count = 4) else Text(stringResource(R.string.c_nothing_eaten), style = T.Small)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BufImage(remember(st.traits) { Buf(MimicArt.W, MimicArt.H).also { MimicArt.draw(MimicPose(1, st.traits), it) } }, scale = 1.dp)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.c_mimicling, st.year), style = T.Heading)
                    if (st.traits.isNotEmpty()) Text(st.traits.map { stringResource(traitName(it)) }.joinToString(", "), style = T.Small.copy(color = C.text))
                    st.avgRating?.let { Text(stringResource(R.string.pc_avg, String.format(Locale.US, "%.1f", it)), style = T.Strong) }
                }
            }
        }
        Part.PILE -> {
            Text(pileHeadline(st), style = T.Heading)
            Text(stringResource(R.string.c_balance, st.added, st.eaten, st.letGo), style = T.Small)
            Spacer(Modifier.height(8.dp))
            Bars(st.addedPerMonth.toList(), months(loc), C.pile, { it.toString() }, height = 60.dp)
            Spacer(Modifier.weight(1f))
            st.avgWait?.let { Line(stringResource(R.string.c_wait), Plural.span(it, h.ru)) }
            st.oldest?.let { Line(stringResource(R.string.c_oldest), it.title) }
        }
    }
}

/** One game in a row: its box, title, stars and the words it got. */
@Composable
private fun Featured(g: Game, h: Home, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CoverImage(g, h.s.pixelCovers, 1.5.dp)
        Column(Modifier.weight(1f)) {
            Text(label, style = T.Small)
            Text(g.title, style = T.Heading, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (g.rating > 0) Stars(g.rating, size = 14.dp)
            (g.review.ifBlank { g.line }).takeIf { it.isNotBlank() }?.let { Text("«$it»", style = T.Small.copy(color = C.text), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun ColumnScope.GameCard(g: Game, h: Home) {
    val ss = h.sessionsOf(g.id)
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoverImage(g, h.s.pixelCovers, (maxWidth / 3.4f) / 32) }
    Spacer(Modifier.height(8.dp))
    Text(g.title, style = T.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        GamePlate(h, g)
        Text(statusLine(g, h.now), style = T.Small.copy(color = C.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (g.rating > 0) Stars(g.rating, size = 18.dp, modifier = Modifier.padding(top = 6.dp))
    if (ss.isNotEmpty()) Text(stringResource(R.string.pc_played, minutesText(ss.sumOf { it.minutes }, h.ru), Plural.sessions(ss.size, h.ru)), style = T.Strong, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.weight(1f))
    val words = g.review.ifBlank { g.line.ifBlank { g.note } }
    if (words.isNotBlank()) Text("«$words»", style = T.Body, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ColumnScope.RoomCard(h: Home) {
    val ctx = LocalContext.current
    val buf = remember(h.games, h.phase, h.wx) {
        val m = RoomModel(
            shelf = h.shelf.map { ShelfItem(it.id, it.look(ctx, h.now)) },
            hands = h.hands.map { HandItem(it.id, it.tint(), it.art(ctx)) },
            pile = h.pile.map { it.look(ctx, h.now) },
            dustyIds = h.shelf.filter { it.look(ctx, h.now).dusty }.map { it.id }.toSet(),
            crow = h.away.isNotEmpty(), stage = h.mimic.stage, traits = h.mimic.traits, phase = h.phase, wx = h.wx,
        )
        Buf(RoomScene.W, RoomScene.BASE_H).also { RoomScene(m, RoomScene.BASE_H).draw(it, 3.0, RoomFx(MimicPose(h.mimic.stage, h.mimic.traits))) }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        val scale: Dp = minOf(maxWidth / RoomScene.W, maxHeight / RoomScene.BASE_H)
        BufImage(buf, Modifier.clip(RoundedCornerShape(6.dp)), scale = scale)
    }
    Text(stringResource(R.string.pc_room_title), style = T.Heading, modifier = Modifier.padding(top = 8.dp))
    Text(stringResource(R.string.pc_room_line, h.shelf.size, h.pile.size, h.mimic.eaten), style = T.Small.copy(color = C.text))
}

@Composable
private fun ColumnScope.MimicCard(h: Home) {
    val buf = remember(h.mimic) { Buf(MimicArt.W, MimicArt.H).also { MimicArt.draw(MimicPose(h.mimic.stage, h.mimic.traits, gap = 10), it) } }
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        BufImage(buf, scale = minOf(maxWidth * .8f / MimicArt.W, maxHeight / MimicArt.H))
    }
    Text(stringResource(R.string.pc_my_mimic), style = T.Title)
    Text(stringResource(R.string.pc_mimic_line, Plural.games(h.mimic.eaten, h.ru)), style = T.Strong)
    if (h.mimic.traits.isNotEmpty()) Text(h.mimic.traits.map { stringResource(traitName(it)) }.joinToString(", "), style = T.Small.copy(color = C.text))
}

@Composable
private fun ColumnScope.CreatureCard(k: Critters.Kind) {
    val buf = remember(k) { Critters.portrait(k, false) }
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        BufImage(buf, scale = minOf(maxWidth * .75f, maxHeight) / Critters.PORTRAIT)
    }
    val (name, note) = creatureText(k)
    Text(stringResource(R.string.pc_met), style = T.Tag.copy(color = C.pink))
    Text(stringResource(name), style = T.Title)
    Text(stringResource(note), style = T.Body)
}

private fun png(ctx: Context, bmp: Bitmap): File {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    return File(dir, "mimic-postcard.png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
}

/** Hands the picture to whatever the phone shares with. */
private fun share(ctx: Context, bmp: Bitmap) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", png(ctx, bmp))
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.pc_send_text)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri("", uri)
    ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.pc_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Puts the picture in Pictures/Mimic (Android 10+, no permission needed). */
private fun save(ctx: Context, bmp: Bitmap): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "mimic-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Mimic")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return false
    true
}.getOrDefault(false)
