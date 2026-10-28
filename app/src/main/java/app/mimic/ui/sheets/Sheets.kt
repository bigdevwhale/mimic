package app.mimic.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.container
import app.mimic.data.Box
import app.mimic.data.Catalog
import app.mimic.data.Covers
import app.mimic.data.Draft
import app.mimic.data.Game
import app.mimic.data.Gave
import app.mimic.data.Genre
import app.mimic.data.Kind
import app.mimic.data.Length
import app.mimic.data.Session
import app.mimic.data.Shelf
import app.mimic.data.Shots
import app.mimic.data.Status
import app.mimic.data.Trait
import app.mimic.domain.Mimic
import app.mimic.ru
import app.mimic.ui.Home
import app.mimic.ui.LocalBus
import app.mimic.ui.Nav
import app.mimic.ui.Plural
import app.mimic.ui.Tab
import app.mimic.ui.art.BoxLook
import app.mimic.ui.art.Boxes
import app.mimic.ui.art.CoverArt
import app.mimic.ui.art.Critters
import app.mimic.ui.components.BufImage
import app.mimic.ui.components.Chip
import app.mimic.ui.components.Field
import app.mimic.ui.components.Flow
import app.mimic.ui.components.Kind as Btn
import app.mimic.ui.components.PixelButton
import app.mimic.ui.components.Plate
import app.mimic.ui.components.Says
import app.mimic.ui.components.Sheet
import app.mimic.ui.components.Stars
import app.mimic.ui.components.pixelBox
import app.mimic.ui.daysSince
import app.mimic.ui.room.CoverImage
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun genreName(g: Genre) = when (g) {
    Genre.HORROR -> R.string.g_horror; Genre.RPG -> R.string.g_rpg; Genre.FARM -> R.string.g_farm; Genre.ROGUE -> R.string.g_rogue
    Genre.EURO -> R.string.g_euro; Genre.PARTY -> R.string.g_party; Genre.PLATFORM -> R.string.g_platform; Genre.OTHER -> R.string.g_other
}

fun traitName(t: Trait) = when (t) {
    Trait.FANG -> R.string.t_fang; Trait.SCROLL -> R.string.t_scroll; Trait.STRAW -> R.string.t_straw; Trait.DICE -> R.string.t_dice
    Trait.MEEPLE -> R.string.t_meeple; Trait.PARTY -> R.string.t_party; Trait.SPRING -> R.string.t_spring
}

fun gaveName(g: Gave) = when (g) { Gave.HOOKED -> R.string.gave_hooked; Gave.RESTED -> R.string.gave_rested; Gave.DREAMS -> R.string.gave_dreams; Gave.JUST_DONE -> R.string.gave_done }

fun lengthName(l: Length) = when (l) { Length.HALF -> R.string.len_half; Length.EVENING -> R.string.len_evening; Length.WEEK -> R.string.len_week; Length.MONTH -> R.string.len_month; Length.ABYSS -> R.string.len_abyss }

fun boxName(b: Box) = when (b) { Box.KEY -> R.string.box_key; Box.DVD -> R.string.box_dvd; Box.CART -> R.string.box_cart; Box.BOARD -> R.string.box_board }

fun kindName(k: Kind) = when (k) { Kind.VIDEO -> R.string.kind_video; Kind.BOARD -> R.string.kind_board; Kind.RPG -> R.string.kind_rpg }

@Composable
fun GenrePlate(g: Genre) = Plate(stringResource(genreName(g)), C.genre(g))

/** One line about where a game is now. */
@Composable
fun statusLine(g: Game, now: Long): String {
    val ru = LocalContext.current.ru
    val span = Plural.span(g.daysSince(g.statusAt, now), ru)
    return when (g.status) {
        Status.PILE -> stringResource(R.string.st_pile, Plural.span(g.daysSince(g.added, now), ru))
        Status.SHELF -> stringResource(R.string.st_shelf, span)
        Status.HANDS -> stringResource(if (g.trial) R.string.st_trial else R.string.st_hands, span)
        Status.DONE -> stringResource(R.string.st_done, span)
        Status.LET -> stringResource(R.string.st_let, span)
        Status.AWAY -> stringResource(R.string.st_away, g.borrower, span)
    }
}

private enum class Step { MAIN, PUT_DOWN, SESSION, CREDITS, LEND, DELETE, GENRE, RATE }

/** A game's card: its box, where it is, its note, and what can happen to it next. */
@Composable
fun CardSheet(h: Home, g: Game, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    var step by remember(g.id) { mutableStateOf(Step.MAIN) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val draft = remember(g.id, step) { SessionDraft().also { if (step == Step.PUT_DOWN) it.note = g.note } }
    val picker = rememberCoverPicker { name -> val old = g.cover; c.write { shelf.save(g.copy(cover = name, color = Covers.art(app, name)?.dominant ?: g.color)); Covers.delete(app, old) } }
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CoverImage(g, h.s.pixelCovers, 3.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(g.title, style = T.Title)
                val facts = listOfNotNull(g.year?.toString(), g.maker.ifBlank { null }, g.platform.ifBlank { null }, g.players.ifBlank { null }?.let { stringResource(R.string.players_n, it) })
                if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = T.Small)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { GamePlate(h, g); Plate(stringResource(lengthName(g.length)), C.woodL) }
                Text(statusLine(g, h.now), style = T.Small.copy(color = C.ink))
                Text(stringResource(R.string.trait_line, h.genres.trait(g)?.let { stringResource(traitName(it)) } ?: stringResource(R.string.trait_none)), style = T.Small)
            }
        }
        if (g.note.isNotBlank() && step == Step.MAIN) Note(stringResource(R.string.save_note), g.note, wisp = g.status == Status.HANDS)
        if ((g.rating > 0 || g.review.isNotBlank()) && step == Step.MAIN) Column(Modifier.fillMaxWidth().clickable { step = Step.RATE }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Stars(g.rating, size = 22.dp)
            if (g.review.isNotBlank()) Text(g.review, style = T.Body)
        }
        if (g.gave != null && step == Step.MAIN) Note(stringResource(R.string.credits_of, stringResource(gaveName(g.gave))), g.line.ifBlank { stringResource(R.string.no_words) }, tilt = 1f)
        val ss = h.sessionsOf(g.id)
        when (step) {
            Step.MAIN -> Flow(Modifier.fillMaxWidth()) {
                when (g.status) {
                    Status.HANDS -> {
                        PixelButton(stringResource(R.string.put_down), { step = Step.PUT_DOWN }, kind = Btn.PALE)
                        PixelButton(stringResource(R.string.credits), { step = Step.CREDITS }, kind = Btn.CRIM)
                    }
                    Status.SHELF, Status.PILE, Status.LET -> {
                        PixelButton(stringResource(R.string.take), {
                            if (h.hands.size >= Shelf.HANDS) bus.toast(ctx.getString(R.string.hands_full))
                            else {
                                c.write { shelf.take(g.id) }
                                if (g.status == Status.SHELF && h.shelf.any { it.id == g.id } && h.now - g.touched > 180 * app.mimic.data.DAY) bus.scatter = System.nanoTime()
                                bus.say(if (g.note.isNotBlank()) ctx.getString(R.string.say_wisp_note, g.note) else ctx.getString(R.string.say_wisp_new))
                                nav.tab(Tab.ROOM, null)
                            }
                        }, kind = Btn.BRASS)
                        if (g.status != Status.LET) PixelButton(stringResource(R.string.credits), { step = Step.CREDITS }, kind = Btn.CRIM)
                        if (g.status == Status.PILE) PixelButton(stringResource(R.string.let_go), { c.write { shelf.letGo(g.id) }; bus.ghost = System.nanoTime(); bus.say(ctx.getString(R.string.say_let, g.title)); nav.tab(Tab.ROOM, null) }, kind = Btn.PALE)
                        if (g.status == Status.SHELF) PixelButton(stringResource(R.string.to_pile), { c.write { shelf.toPile(g.id) }; nav.close() }, kind = Btn.PALE)
                        if (g.status == Status.LET) PixelButton(stringResource(R.string.to_shelf), { c.write { shelf.toShelf(g.id) }; nav.close() }, kind = Btn.PALE)
                    }
                    Status.AWAY -> PixelButton(stringResource(R.string.came_back), { c.write { shelf.back(g.id) }; bus.say(ctx.getString(R.string.say_crow_back)); nav.close() }, kind = Btn.BRASS)
                    Status.DONE -> Unit
                }
                if (g.status == Status.SHELF || g.status == Status.DONE) PixelButton(stringResource(R.string.lend), { step = Step.LEND }, kind = Btn.PALE)
                if (g.status == Status.HANDS || g.status == Status.SHELF || g.status == Status.DONE) PixelButton(stringResource(R.string.s_add), { step = Step.SESSION }, kind = Btn.PALE)
                PixelButton(stringResource(R.string.rate_btn), { step = Step.RATE }, kind = Btn.WOOD, small = true)
                PixelButton(stringResource(R.string.genre_edit), { step = Step.GENRE }, kind = Btn.WOOD, small = true)
                PixelButton(stringResource(R.string.cover_shoot), picker.shoot, kind = Btn.WOOD, small = true)
                PixelButton(stringResource(R.string.cover_pick), picker.pick, kind = Btn.WOOD, small = true)
                PixelButton(stringResource(R.string.delete), { step = Step.DELETE }, kind = Btn.WOOD, small = true)
            }
            Step.PUT_DOWN, Step.SESSION -> {
                val down = step == Step.PUT_DOWN
                SessionForm(draft, if (down) R.string.put_down_q else R.string.s_note, if (down) R.string.put_down_hint else R.string.s_note_hint)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    PixelButton(stringResource(R.string.back), { Shots.delete(ctx, draft.shots.toList()); step = Step.MAIN }, kind = Btn.PALE)
                    PixelButton(stringResource(if (down) R.string.put_down else R.string.s_save), {
                        val s = Session(game = g.id, minutes = draft.minutes, note = draft.note.trim(), shots = draft.shots.joinToString(","))
                        c.write {
                            shelf.log(s)
                            if (down) shelf.putDown(g.id, draft.note.trim()) else shelf.get(g.id)?.let { shelf.save(it.copy(touched = System.currentTimeMillis())) }
                        }
                        if (down) { bus.say(ctx.getString(R.string.say_put_down)); nav.close() } else step = Step.MAIN
                    })
                }
            }
            Step.CREDITS -> {
                var line by remember { mutableStateOf("") }
                var gave by remember { mutableStateOf(Gave.HOOKED) }
                var stars by remember { mutableStateOf(g.rating) }
                var review by remember { mutableStateOf(g.review) }
                Text(stringResource(R.string.credits_line), style = T.Body)
                Field(line, { line = it }, stringResource(R.string.credits_hint))
                Text(stringResource(R.string.credits_gave), style = T.Body)
                Flow { for (v in Gave.entries) Chip(stringResource(gaveName(v)), gave == v, { gave = v }) }
                Text(stringResource(R.string.rate_q), style = T.Body)
                Stars(stars, size = 34.dp) { stars = it }
                Field(review, { review = it }, stringResource(R.string.review_hint), single = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    PixelButton(stringResource(R.string.back), { step = Step.MAIN }, kind = Btn.PALE)
                    PixelButton(stringResource(R.string.feed), {
                        val before = h.mimic.traits
                        val tr = h.genres.trait(g)
                        c.write { shelf.credits(g.id, line.trim(), gave, stars, review) }
                        bus.eat = g to System.nanoTime()
                        bus.say(ctx.getString(R.string.say_om))
                        val msg = when {
                            tr == null -> ctx.getString(R.string.eat_no_trait, g.title)
                            tr in before -> ctx.getString(R.string.eat_had, g.title, ctx.getString(traitName(tr)))
                            else -> ctx.getString(R.string.eat_new, g.title, ctx.getString(traitName(tr)))
                        }
                        c.scope.launch { delay(1900); bus.toast(msg); c.settings.meet("mimic") }
                        nav.tab(Tab.ROOM, null)
                    }, kind = Btn.CRIM)
                }
            }
            Step.RATE -> {
                var stars by remember { mutableStateOf(g.rating) }
                var review by remember { mutableStateOf(g.review) }
                Text(stringResource(R.string.rate_q), style = T.Body)
                Stars(stars, size = 34.dp) { stars = it }
                Field(review, { review = it }, stringResource(R.string.review_hint), single = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    PixelButton(stringResource(R.string.back), { step = Step.MAIN }, kind = Btn.PALE)
                    PixelButton(stringResource(R.string.s_save), { c.write { shelf.rate(g.id, stars, review) }; step = Step.MAIN })
                }
            }
            Step.GENRE -> {
                GenrePicker(h, g.kind, g.genre, g.preset, g.style) { gn, pr, st -> c.write { shelf.setGenre(g.id, gn, pr, st) } }
                PixelButton(stringResource(R.string.done_btn), { step = Step.MAIN }, Modifier.fillMaxWidth(), kind = Btn.BRASS)
            }
            Step.LEND -> {
                var who by remember { mutableStateOf("") }
                Text(stringResource(R.string.lend_q), style = T.Body)
                Field(who, { who = it }, stringResource(R.string.lend_hint))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    PixelButton(stringResource(R.string.back), { step = Step.MAIN }, kind = Btn.PALE)
                    PixelButton(stringResource(R.string.lend), { if (who.isNotBlank()) { c.write { shelf.lend(g.id, who); settings.meet("crow") }; bus.say(ctx.getString(R.string.say_crow, who.trim())); nav.tab(Tab.ROOM, null) } }, enabled = who.isNotBlank())
                }
            }
            Step.DELETE -> {
                Text(stringResource(R.string.delete_q, g.title), style = T.Body)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    PixelButton(stringResource(R.string.back), { step = Step.MAIN }, kind = Btn.PALE)
                    PixelButton(stringResource(R.string.delete), { c.write { val ss = shelf.delete(g); Covers.delete(app, g.cover); Shots.delete(app, ss.flatMap { it.shotList }) }; nav.close() }, kind = Btn.CRIM)
                }
            }
        }
        if (step == Step.MAIN && ss.isNotEmpty()) {
            Text(stringResource(R.string.s_title, ss.size, minutesText(ss.sumOf { it.minutes }, ctx.ru)), style = T.Heading.copy(color = C.crim))
            SessionList(ss, { viewing = it }) { s -> c.write { shelf.unlog(s); Shots.delete(app, s.shotList) } }
        }
    }
    viewing?.let { ShotViewer(it) { viewing = null } }
}

/** A pinned note: where you stopped, or the credits line. */
@Composable
fun Note(label: String, text: String, wisp: Boolean = false, tilt: Float = -1f) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().rotate(tilt).pixelBox(C.note, outline = C.paperD, bevel = null).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = T.Small)
            Text(text, style = T.Body)
        }
        if (wisp) BufImage(Critters.wisp(false), Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-16).dp), scale = 3.dp)
    }
}

/** The weekly dig: the oldest box from the bottom, or any box from the pile. */
@Composable
fun DigSheet(h: Home, id: Long?, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    val g = id?.let { h.game(it) } ?: Mimic.bottom(h.games)
    Sheet(nav::close) {
        if (g == null) { Says(stringResource(R.string.say_pile_empty)); return@Sheet }
        val bottom = id == null || g.id == Mimic.bottom(h.games)?.id
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CoverImage(g, h.s.pixelCovers, 3.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(if (bottom) R.string.dig_bottom else R.string.dig_from), style = T.Small)
                Text(g.title, style = T.Title)
                GamePlate(h, g)
                Text(stringResource(R.string.dig_lay, Plural.span(g.daysSince(g.added, h.now), ctx.ru)), style = T.Small.copy(color = C.ink))
            }
        }
        Says(stringResource(if (bottom) R.string.dig_say_bottom else R.string.dig_say))
        Flow(Modifier.fillMaxWidth()) {
            PixelButton(stringResource(R.string.dig_lie), { bus.say(ctx.getString(R.string.say_dig_lie)); nav.close() }, kind = Btn.PALE)
            PixelButton(stringResource(R.string.let_go), { c.write { shelf.letGo(g.id); settings.meet("ghost") }; bus.ghost = System.nanoTime(); bus.say(ctx.getString(R.string.say_let, g.title)); nav.tab(Tab.ROOM, null) }, kind = Btn.PALE)
            PixelButton(stringResource(R.string.dig_try), {
                if (h.hands.size >= Shelf.HANDS) bus.toast(ctx.getString(R.string.hands_full))
                else { c.write { shelf.take(g.id, trial = true); shelf.get(g.id)?.let { shelf.save(it.copy(note = ctx.getString(R.string.trial_note))) } }; bus.say(ctx.getString(R.string.say_trial)); nav.tab(Tab.ROOM, null) }
            }, kind = Btn.CRIM)
        }
    }
}

/** Three questions, three boxes from the pile; the die picks when you can't. */
@Composable
fun SniffSheet(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    var time by remember { mutableStateOf(Mimic.Time.EVENING) }
    var who by remember { mutableStateOf(Mimic.Who.ONE) }
    var mood by remember { mutableStateOf(Mimic.Mood.HEAD) }
    var picked by remember { mutableStateOf<Long?>(null) }
    val found = remember(time, who, mood, h.games) { Mimic.sniff(h.pile, time, who, mood, h.genres) }
    Sheet(nav::close) {
        Text(stringResource(R.string.sniff), style = T.Title)
        Text(stringResource(R.string.sniff_sub), style = T.Small)
        Text(stringResource(R.string.q_time), style = T.Small.copy(color = C.ink))
        Flow { for ((v, l) in listOf(Mimic.Time.HALF to R.string.time_half, Mimic.Time.EVENING to R.string.time_evening, Mimic.Time.WEEKEND to R.string.time_weekend)) Chip(stringResource(l), time == v, { time = v; picked = null }) }
        Text(stringResource(R.string.q_who), style = T.Small.copy(color = C.ink))
        Flow { for ((v, l) in listOf(Mimic.Who.ONE to R.string.who_one, Mimic.Who.TWO to R.string.who_two, Mimic.Who.GROUP to R.string.who_group)) Chip(stringResource(l), who == v, { who = v; picked = null }) }
        Text(stringResource(R.string.q_mood), style = T.Small.copy(color = C.ink))
        Flow { for ((v, l) in listOf(Mimic.Mood.HEAD to R.string.mood_head, Mimic.Mood.HANDS to R.string.mood_hands, Mimic.Mood.HEART to R.string.mood_heart)) Chip(stringResource(l), mood == v, { mood = v; picked = null }) }
        if (found.isEmpty()) Says(stringResource(if (h.pile.isEmpty()) R.string.say_pile_empty else R.string.sniff_none))
        for (g in found) {
            Row(Modifier.fillMaxWidth().pixelBox(if (picked == g.id) Color0.pick else C.paper, outline = if (picked == g.id) C.crim else C.paperD, bevel = null).clickable { nav.dig(g.id) }.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CoverImage(g, h.s.pixelCovers, 1.dp)
                Column(Modifier.weight(1f)) {
                    Text(g.title, style = T.Heading)
                    GamePlate(h, g)
                    Text(stringResource(R.string.sniff_row, Plural.span(g.daysSince(g.added, h.now), ctx.ru), stringResource(lengthName(g.length))), style = T.Small)
                }
            }
        }
        if (found.size > 1) PixelButton(stringResource(R.string.roll_die), {
            var roll = 1 + (Math.random() * 20).toInt()
            var again = ""
            if (roll == 1) { again = ctx.getString(R.string.die_again); roll = 2 + (Math.random() * 19).toInt() }
            val g = found[roll % found.size]
            picked = g.id
            bus.toast(listOf(again, ctx.getString(R.string.die_roll, roll, g.title)).filter { it.isNotEmpty() }.joinToString(" "))
            c.write { settings.meet("dice") }
        }, Modifier.fillMaxWidth(), kind = Btn.BRASS)
    }
}

private object Color0 { val pick = androidx.compose.ui.graphics.Color(0xFFFDE6EA) }

/** Looking a game up by name, as in Rewind; or a photo of the box; or by hand. */
@Composable
fun AddSheet(q0: String, nav: Nav) {
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf(Kind.VIDEO) }
    var q by remember { mutableStateOf(q0) }
    var msg by remember { mutableStateOf<Int?>(R.string.add_hint) }
    var busy by remember { mutableStateOf(false) }
    val found = remember { mutableStateListOf<Draft>() }
    val previews = remember { mutableStateMapOf<String, CoverArt>() }
    val bytes = remember { HashMap<String, ByteArray>() }
    val scope = rememberCoroutineScope()
    val picker = rememberCoverPicker { name -> nav.go(app.mimic.ui.Overlay.NewGame(Draft(kind, q.trim().ifBlank { ctx.getString(R.string.new_game) }), name)) }
    fun search() {
        if (q.trim().length < 2 || busy) return
        busy = true; msg = R.string.add_busy; found.clear()
        scope.launch {
            val r = Catalog.search(q, kind, ctx.ru)
            busy = false
            msg = when { r == null -> R.string.add_offline; r.isEmpty() -> R.string.add_nothing; else -> null }
            if (r != null) found.addAll(r)
            for (d in r.orEmpty()) d.coverUrl?.let { url -> launch { Catalog.download(url)?.let { b -> bytes[url] = b; Covers.fromBytes(b)?.let { previews[url] = it } } } }
        }
    }
    Sheet(nav::close) {
        Text(stringResource(R.string.add_title), style = T.Title)
        Flow { for (k in Kind.entries) Chip(stringResource(kindName(k)), kind == k, { kind = k }) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(q, { q = it }, stringResource(R.string.add_field), Modifier.weight(1f), onDone = ::search)
            PixelButton(stringResource(R.string.search), ::search, small = true)
        }
        msg?.let { Text(stringResource(it), style = T.Small) }
        for (d in found) {
            Row(Modifier.fillMaxWidth().pixelBox(C.paper, outline = C.paperD, bevel = null).clickable {
                val name = d.coverUrl?.let { bytes[it] }?.let { Covers.save(ctx, it) }
                nav.go(app.mimic.ui.Overlay.NewGame(d, name))
            }.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val art = d.coverUrl?.let { previews[it] }
                BufImage(remember(art, d) { Boxes.cover(BoxLook(Catalog.boxFor(d.kind, d.platform), d.genre ?: Genre.OTHER, art?.dominant ?: Boxes.colorFor(d.title), art)) }, scale = 1.dp)
                Column(Modifier.weight(1f)) {
                    Text(d.title, style = T.Heading)
                    Text(listOfNotNull(d.year?.toString(), d.maker.ifBlank { null }, d.platform.ifBlank { null }, d.players.ifBlank { null }?.let { ctx.getString(R.string.players_n, it) }).joinToString(" · "), style = T.Small)
                    d.genre?.let { GenrePlate(it) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
            PixelButton(stringResource(R.string.cover_shoot), picker.shoot, kind = Btn.PALE, small = true)
            PixelButton(stringResource(R.string.by_hand), { nav.go(app.mimic.ui.Overlay.NewGame(Draft(kind, q.trim().ifBlank { ctx.getString(R.string.new_game) }), null)) }, kind = Btn.PALE, small = true)
        }
    }
    LaunchedEffect(Unit) { if (q0.isNotBlank()) search() }
}

/** The last look before a game comes in: name, box, genre, length, and where it goes. */
@Composable
fun DraftSheet(h: Home, d: Draft, cover0: String?, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    var title by remember { mutableStateOf(d.title) }
    var box by remember { mutableStateOf(Catalog.boxFor(d.kind, d.platform)) }
    var genre by remember { mutableStateOf(d.genre ?: Genre.OTHER) }
    var style by remember { mutableStateOf<Long?>(null) }
    var preset by remember { mutableStateOf(d.preset) }
    var length by remember { mutableStateOf(if (d.kind == Kind.VIDEO) Length.WEEK else Length.EVENING) }
    var where by remember { mutableStateOf(Status.PILE) }
    var cover by remember { mutableStateOf(cover0) }
    val picker = rememberCoverPicker { cover = it }
    val art = remember(cover) { Covers.art(ctx, cover) }
    val preview = Game(title = title, kind = d.kind, box = box, genre = genre, preset = preset, style = style, cover = cover, color = art?.dominant ?: Boxes.colorFor(title))
    Sheet(nav::close) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CoverImage(preview, h.s.pixelCovers, 3.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(title, { title = it }, stringResource(R.string.add_field), style = T.Heading)
                val facts = listOfNotNull(d.year?.toString(), d.maker.ifBlank { null }, d.platform.ifBlank { null }, d.players.ifBlank { null }?.let { stringResource(R.string.players_n, it) })
                Text(facts.joinToString(" · ").ifBlank { stringResource(kindName(d.kind)) }, style = T.Small)
            }
        }
        Text(stringResource(R.string.f_box), style = T.Small.copy(color = C.ink))
        Flow { for (b in Box.entries) Chip(stringResource(boxName(b)), box == b, { box = b }) }
        GenrePicker(h, d.kind, genre, preset, style) { gn, pr, st -> genre = gn; preset = pr; style = st }
        Text(stringResource(R.string.f_length), style = T.Small.copy(color = C.ink))
        Flow { for (l in Length.entries) Chip(stringResource(lengthName(l)), length == l, { length = l }) }
        Text(stringResource(R.string.f_where), style = T.Small.copy(color = C.ink))
        Flow { for ((s, l) in listOf(Status.PILE to R.string.to_pile_short, Status.SHELF to R.string.to_shelf_short, Status.HANDS to R.string.to_hands_short)) Chip(stringResource(l), where == s, { where = s }) }
        CoverButtons(picker)
        PixelButton(stringResource(R.string.add_do), {
            if (where == Status.HANDS && h.hands.size >= Shelf.HANDS) { bus.toast(ctx.getString(R.string.hands_full)); return@PixelButton }
            val now = System.currentTimeMillis()
            val g = preview.copy(title = title.trim().ifBlank { d.title }, length = length, status = where, platform = d.platform, players = d.players, year = d.year, maker = d.maker,
                added = now, statusAt = now, touched = now, waited = if (where == Status.PILE) 0 else 1)
            c.write { shelf.add(g) }
            bus.say(ctx.getString(when (where) { Status.PILE -> R.string.say_added_pile; Status.HANDS -> R.string.say_wisp_new; else -> R.string.say_added_shelf }, g.title))
            nav.tab(Tab.ROOM, null)
        }, Modifier.fillMaxWidth(), kind = Btn.CRIM)
    }
}
