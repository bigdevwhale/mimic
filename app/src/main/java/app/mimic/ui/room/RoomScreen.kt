package app.mimic.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.data.Shelf
import app.mimic.domain.Mimic
import app.mimic.domain.Phase
import app.mimic.domain.Wx
import app.mimic.ui.Home
import app.mimic.ui.LocalBus
import app.mimic.ui.Nav
import app.mimic.ui.Words
import app.mimic.ui.art.Art
import app.mimic.ui.art.Boxes
import app.mimic.ui.art.Critters
import app.mimic.ui.art.HandItem
import app.mimic.ui.art.MimicPose
import app.mimic.ui.art.PixelCanvas
import app.mimic.ui.art.RoomFx
import app.mimic.ui.art.RoomModel
import app.mimic.ui.art.RoomScene
import app.mimic.ui.art.ShelfItem
import app.mimic.ui.art.hashI
import app.mimic.ui.art.rememberTick
import app.mimic.ui.art.rgb
import app.mimic.ui.art
import app.mimic.ui.components.BufImage
import app.mimic.ui.components.Kind
import app.mimic.ui.components.PixelButton
import app.mimic.ui.components.sayBox
import app.mimic.ui.components.tray
import app.mimic.ui.look
import app.mimic.ui.tint
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import kotlin.math.sin

private const val S_PER_TICK = Art.FRAME_MS / 1000.0

/** The room, the mimic's words, the three big actions and the games in hands. */
@Composable
fun RoomScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val bus = LocalBus.current
    val tick = rememberTick()

    // The eat animation starts on the tick it was asked for; the traits wait till the swallow.
    var eatStart by remember { mutableIntStateOf(-1) }
    var eatId by remember { mutableStateOf(-1L) }
    LaunchedEffect(bus.eat) { bus.eat?.let { (g, _) -> eatId = g.id; eatStart = tick } }
    var ghostStart by remember { mutableIntStateOf(-1000) }
    LaunchedEffect(bus.ghost) { if (bus.ghost != 0L) ghostStart = tick }
    var scatterStart by remember { mutableIntStateOf(-1000) }
    LaunchedEffect(bus.scatter) { if (bus.scatter != 0L) scatterStart = tick }
    var sel by remember { mutableStateOf<IntArray?>(null) }
    var selStart by remember { mutableIntStateOf(-100) }

    val sceneModel = remember(h.games, h.phase, h.wx, h.styles) {
        RoomModel(
            shelf = h.shelf.map { ShelfItem(it.id, it.look(ctx, h.now)) },
            hands = h.hands.map { HandItem(it.id, it.tint(), it.art(ctx)) },
            pile = h.pile.map { it.look(ctx, h.now) },
            dustyIds = h.shelf.filter { it.look(ctx, h.now).dusty }.map { it.id }.toSet(),
            crow = h.away.isNotEmpty(),
            stage = h.mimic.stage, traits = h.mimic.traits, phase = h.phase, wx = h.wx,
            wind = h.s.outside?.wind ?: 8.0,
        )
    }

    // Everything on one screen: the room takes the height that's left, the rest is a compact strip below.
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        // The room grows taller (more floor) to fill the space instead of leaving bands around it.
        val artH = ((maxHeight - 6.dp) / (maxWidth - 6.dp) * RoomScene.W).toInt().coerceIn(RoomScene.BASE_H, RoomScene.MAX_H)
        val scene = remember(sceneModel, artH) { RoomScene(sceneModel, artH) }
        Box(Modifier.tray().padding(3.dp)) {
            PixelCanvas(RoomScene.W, artH, Modifier.pointerInput(scene) {
                detectTapGestures { o ->
                    val x = o.x / size.width * RoomScene.W
                    val y = o.y / size.height * artH
                    val spot = scene.hit(x.toDouble(), y.toDouble(), tick - scatterStart > 9 / S_PER_TICK) ?: return@detectTapGestures
                    sel = spot.r; selStart = tick
                    when (spot) {
                        is app.mimic.ui.art.Spot.Wisp -> nav.card(spot.id)
                        is app.mimic.ui.art.Spot.Shelf -> nav.card(spot.id)
                        is app.mimic.ui.art.Spot.Crow -> h.away.firstOrNull()?.let { nav.card(it.id) }
                        is app.mimic.ui.art.Spot.Bunnies -> { bus.scatter = System.nanoTime(); bus.say(ctx.getString(R.string.say_bunnies)) }
                        is app.mimic.ui.art.Spot.Mimic -> bus.say(Words.poke(h.ru, h.wx, h.phase, Math.random()))
                        is app.mimic.ui.art.Spot.Pile -> nav.dig(null)
                        is app.mimic.ui.art.Spot.Table -> bus.say(ctx.getString(R.string.say_table))
                        is app.mimic.ui.art.Spot.Tv -> bus.say(ctx.getString(if (h.hands.isEmpty()) R.string.say_tv_empty else R.string.say_tv))
                    }
                }
            }, fitHeight = true) { b, tk ->
                val t = tk * S_PER_TICK
                val d = if (eatStart >= 0) (tk - eatStart) * S_PER_TICK else -1.0
                val eaten = h.game(eatId)
                val before = if (eaten != null) Mimic.of(h.games.filter { it.id != eatId }, h.genres) else h.mimic
                var pose = MimicPose(
                    stage = if (d in 0.0..1.9) before.stage else h.mimic.stage,
                    traits = if (d in 0.0..1.9) before.traits else h.mimic.traits,
                    gap = 8 + if (t % 2.6 < 1.3) 1 else 0, blink = t % 4.2 < .15,
                    look = sin(t * .35).let { if (it > .6) 1 else if (it < -.6) -1 else 0 },
                    wob = (t * 2).toInt() % 2 == 1, bounce = (t * 2).toInt() % 2,
                )
                var shake = 0
                var falling: Pair<Int, Int>? = null
                val crumbs = ArrayList<IntArray>()
                val sparks = ArrayList<DoubleArray>()
                if (d >= 0 && eaten != null) {
                    when {
                        d < .35 -> pose = pose.copy(gap = (8 + d / .35 * 12).toInt(), up = true)
                        d < .85 -> { pose = pose.copy(gap = 20, up = true); falling = eaten.tint() to (-18 + (d - .35) / .5 * (RoomScene.MPOS[1] + 30)).toInt() }
                        d < .95 -> { pose = pose.copy(gap = 1); shake = 1 }
                        d < 1.9 -> {
                            pose = pose.copy(gap = if ((d * 8).toInt() % 2 == 1) 1 else 3, blink = true); shake = if ((d * 12).toInt() % 2 == 1) 1 else -1
                            val m = scene.mouth
                            for (i in 0 until 14) {
                                val born = .95 + i * .065
                                val age = d - born
                                if (age < 0 || age > .8) continue
                                val hh = hashI("crumb$i")
                                val vx = ((hh % 80) - 40).toDouble(); val vy = -30.0 - hh % 30
                                val x = m[0] + (if (i % 2 == 0) -14 else 14) + vx * age
                                val y = m[1] + vy * age + 60 * age * age
                                crumbs += intArrayOf(x.toInt(), y.toInt(), if (i % 2 == 0) eaten.tint() else rgb(0xE8DCC0))
                            }
                        }
                    }
                    for (i in 0 until 9) {
                        val age = d - 1.9 - i * .12
                        if (age in 0.0..1.6) { val hh = hashI("spark$i"); sparks += doubleArrayOf(RoomScene.MPOS[0] + 10.0 + hh % 52, RoomScene.MPOS[1] + 18.0 + (hh shr 8) % 40, age) }
                    }
                }
                val ga = (tk - ghostStart) * S_PER_TICK
                val ba = (tk - scatterStart) * S_PER_TICK
                scene.draw(b, t, RoomFx(
                    pose = pose, shake = shake, falling = falling,
                    ghostAge = if (ga in 0.0..6.0) ga else null,
                    bunniesAge = if (ba in 0.0..9.0) ba else null,
                    crumbs = crumbs, sparks = sparks,
                    sel = sel, selOn = (tk - selStart) < 7 && (tk - selStart) % 2 == 0,
                ))
            }
            Bubble(h, Modifier.align(Alignment.BottomCenter).padding(8.dp))
        }
        }
        WeatherLine(h)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PixelButton(stringResource(R.string.sniff), nav::sniff, Modifier.weight(1f), Kind.CRIM, small = true)
            PixelButton(stringResource(R.string.dig), { nav.dig(null) }, Modifier.weight(1f), Kind.BRASS, small = true)
            PixelButton(stringResource(R.string.add_game), nav::add, Modifier.weight(1f), Kind.PALE, small = true)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.in_hands), style = T.Small.copy(color = C.brass))
            Text("  " + stringResource(R.string.hands_n, h.hands.size, Shelf.HANDS), style = T.Small.copy(color = C.paperD))
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until Shelf.HANDS) {
                val g = h.hands.getOrNull(i)
                if (g == null) {
                    Box(Modifier.weight(1f).fillMaxHeight().heightIn(min = 64.dp).tray().clickable { nav.shelf("pile") }.padding(6.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.slot_free), style = T.Small.copy(color = C.paperD), textAlign = TextAlign.Center)
                    }
                    continue
                }
                Row(Modifier.weight(1f).fillMaxHeight().tray().clickable { nav.card(g.id) }.padding(horizontal = 4.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.padding(top = 6.dp)) { CoverImage(g, h.s.pixelCovers, 1.dp) }
                        BufImage(Critters.wisp((tick + i) % 4 < 2), Modifier.offset(y = (-6).dp), scale = 2.dp)
                    }
                    Text(g.title, style = T.Small.copy(color = C.paper), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

/** The speech bubble over the room: typed out, then gone after a while; tap hides it. */
@Composable
private fun Bubble(h: Home, modifier: Modifier) {
    val bus = LocalBus.current
    val tick = rememberTick()
    val (text, stamp) = bus.said
    var start by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(stamp) { start = tick; hidden = false }
    if (text.isEmpty() || hidden) return
    val shown = ((tick - start) * 3).coerceAtMost(text.length)
    val gone = !Art.still && tick - start > 40 + text.length / 2
    if (gone) return
    Column(modifier.fillMaxWidth().sayBox().clickable { hidden = true }.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.mimic_caps), style = T.Small.copy(color = C.crim))
        Text(if (Art.still) text else text.take(shown), style = T.Say)
    }
}

@Composable
private fun WeatherLine(h: Home) {
    val o = h.s.outside
    val parts = buildList {
        h.s.place?.name?.takeIf { it.isNotBlank() }?.let { add(it) }
        if (h.s.liveWeather && o != null) o.temp?.let { add((if (it > 0) "+" else "") + it + "°") }
        add(stringResource(wxName(h.wx)))
        add(stringResource(phaseName(h.phase)))
    }
    Text(parts.joinToString(" · "), style = T.Small.copy(color = C.paperD), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
}

fun wxName(w: Wx) = when (w) {
    Wx.CLEAR -> R.string.wx_clear; Wx.CLOUDY -> R.string.wx_cloudy; Wx.OVERCAST -> R.string.wx_overcast; Wx.DRIZZLE -> R.string.wx_drizzle
    Wx.RAIN -> R.string.wx_rain; Wx.SNOW -> R.string.wx_snow; Wx.FOG -> R.string.wx_fog; Wx.STORM -> R.string.wx_storm
}

fun phaseName(p: Phase) = when (p) { Phase.NIGHT -> R.string.ph_night; Phase.DAWN -> R.string.ph_dawn; Phase.DAY -> R.string.ph_day; Phase.EVENING -> R.string.ph_evening }

/** A game's box front: pixel cover, or the real picture inside the pixel frame. */
@Composable
fun CoverImage(g: app.mimic.data.Game, pixel: Boolean, scale: androidx.compose.ui.unit.Dp) {
    val ctx = LocalContext.current
    val look = remember(g.id, g.cover, g.color, g.box, g.genre, g.status) { g.look(ctx) }
    val frame = remember(look, pixel) { Boxes.cover(look, plain = false) }
    if (pixel || look.art == null) { BufImage(frame, scale = scale); return }
    val a = Boxes.artRect(g.box)
    Box {
        BufImage(frame, scale = scale)
        val plain = remember(look) { look.art.plain(a[2] * 4, a[3] * 4) }
        BufImage(plain, Modifier.offset(x = scale * a[0], y = scale * a[1]), scale = scale / 4, smooth = true)
    }
}
