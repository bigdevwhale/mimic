package app.mimic.ui.art

import app.mimic.data.Box
import app.mimic.data.Trait
import app.mimic.domain.Phase
import app.mimic.domain.Wx
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** A shelf game as the room draws it. */
class ShelfItem(val id: Long, val look: BoxLook)

/** A game in hands: its wisp above the TV, its colours on the screen. */
class HandItem(val id: Long, val color: Int, val art: CoverArt?)

/** What the room shows: shelves, hands, the pile, who's around, the mimic, and outside. */
class RoomModel(
    val shelf: List<ShelfItem>,
    val hands: List<HandItem>,
    /** Pile boxes from the bottom (oldest) up; boxes beyond the named ones are drawn plain. */
    val pile: List<BoxLook?>,
    val dustyIds: Set<Long>,
    val crow: Boolean,
    val stage: Int,
    val traits: Set<Trait>,
    val phase: Phase,
    val wx: Wx,
    val wind: Double = 8.0,
)

/** Things that move this frame: the mimic's pose and the passing effects. */
class RoomFx(
    val pose: MimicPose,
    val shake: Int = 0,
    val falling: Pair<Int, Int>? = null,
    val ghostAge: Double? = null,
    /** Seconds since the dust bunnies were scattered, or null when they sit. */
    val bunniesAge: Double? = null,
    val crumbs: List<IntArray> = emptyList(),
    val sparks: List<DoubleArray> = emptyList(),
    val sel: IntArray? = null,
    val selOn: Boolean = false,
)

/** A tappable thing in the room and what it is. */
sealed class Spot(val r: IntArray) {
    class Wisp(r: IntArray, val id: Long) : Spot(r)
    class Shelf(r: IntArray, val id: Long) : Spot(r)
    class Crow(r: IntArray) : Spot(r)
    class Bunnies(r: IntArray) : Spot(r)
    class Mimic(r: IntArray) : Spot(r)
    class Pile(r: IntArray) : Spot(r)
    class Table(r: IntArray) : Spot(r)
    class Tv(r: IntArray) : Spot(r)
}

/**
 * The room, 176×192 art pixels: wallpaper and window at the back, two shelves, the pile in the
 * corner, the TV with the wisps of games in hands, the table with the meeples, and the mimic on the
 * rug. The light follows the phase: lamp and TV glow at night, grey days dim it.
 */
class RoomScene(val m: RoomModel, val H: Int = BASE_H) {
    companion object {
        const val W = 176
        /** The room's height at its smallest; taller screens get more floor at the bottom. */
        const val BASE_H = 192
        const val MAX_H = 270
        private const val GLASS = 0x00010203
        private val INK = rgb(0x2A160E)
        private val PAPER = rgb(0xF6EAD0)
        private val LAMP = intArrayOf(157, 26)
        private val TV = intArrayOf(147, 96)
        val MPOS = intArrayOf(50, 86)
        private val CROW = intArrayOf(30, 46)
        private val EMIT = setOf(MimicArt.EYE, MimicArt.EYEL, rgb(0xFFF2B0), rgb(0xE83030)).map { it and 0xFFFFFF }.toSet()
    }

    private class Slot(val id: Long, val x: Int, val y: Int, val w: Int, val h: Int, val img: Buf)
    private class Heap(val img: Buf, val x: Int, val y: Int, val x1: Int, val y1: Int)

    private val slots = ArrayList<Slot>()
    private val heap: Heap?
    private val bunnies = ArrayList<IntArray>()
    private val wispBase: List<IntArray>
    private val static = Buf(W, H)
    private var parts: MimicParts? = null
    private val mimicBuf = Buf(MimicArt.W, MimicArt.H)
    private val wispPos = HashMap<Long, IntArray>()

    init {
        // The top shelf fills first (the lamp stands at its end), then the lower one; what doesn't fit stays in the list.
        val rows = listOf(Triple(34, 72, 150), Triple(66, 72, 172))
        var row = 0
        var x = rows[0].second
        for (it in m.shelf) {
            val (w, h) = Boxes.spineSize(it.look.box)
            if (x + w > rows[row].third) { if (row == 1) break; row = 1; x = rows[1].second }
            slots += Slot(it.id, x, rows[row].first - h, w, h, Boxes.spine(it.look))
            x += w + 2
        }
        heap = buildHeap()
        slots.firstOrNull { it.id in m.dustyIds }?.let { bunnies += intArrayOf(it.x - 1, it.y - 9) }
        wispBase = m.hands.mapIndexed { i, _ -> intArrayOf(136 + i * 12, 72 + (i % 2) * 4) }
        drawStatic()
    }

    // ---------- the pile ----------

    private class HeapBox(val look: BoxLook?, val k: Box, val w: Int, val h: Int, val tp: Int, val hh: Int, val c: Int, val tag: Boolean) {
        var x = 0; var y = 0; var sl = 0.0
    }

    /** Each box falls onto the pile's surface: of a few spots it takes the lowest one near the middle. */
    private fun buildHeap(): Heap? {
        val n = min(m.pile.size, 48)
        if (n == 0) return null
        val items = (0 until n).map { i ->
            val look = m.pile[i]
            val hh = hashI("p$i")
            val k = look?.box ?: arrayOf(Box.BOARD, Box.DVD, Box.BOARD, Box.CART, Box.DVD, Box.KEY, Box.BOARD, Box.DVD)[hh % 8]
            val (w, h, tp) = when (k) {
                Box.BOARD -> Triple(14 + hh % 10, 6 + (hh ushr 3) % 4, 3)
                Box.DVD -> Triple(11, 3, 1)
                Box.CART -> Triple(9, 5, 2)
                Box.KEY -> Triple(7, 2, 1)
            }
            HeapBox(look, k, w, h, tp, hh, look?.color ?: Boxes.PALETTE[(hh ushr 5) % Boxes.PALETTE.size], (hh ushr 14) % 6 == 0)
        }
        val cx = 30
        val ground = 154
        val surf = IntArray(W) { ground }
        for (it in items) {
            var bx = Int.MIN_VALUE; var brest = ground; var bsc = Double.NEGATIVE_INFINITY
            for (k in 0 until 9) {
                val x = (cx - 30 + hashI("${it.hh}:$k") % 60 - it.w / 2.0).roundToInt()
                if (x < -3 || x + it.w > 66) continue
                var rest = ground
                for (X in max(0, x) until x + it.w) rest = min(rest, surf[X])
                val sc = rest - abs(x + it.w / 2.0 - cx) * .55
                if (sc > bsc) { bsc = sc; bx = x; brest = rest }
            }
            if (bx == Int.MIN_VALUE) { bx = cx - (it.w shr 1); brest = (bx until bx + it.w).minOf { surf[it] } }
            val l = surf[max(0, bx)]; val r = surf[min(W - 1, bx + it.w - 1)]
            it.sl = if (abs(l - r) > 2 && it.k != Box.KEY) (if (l > r) .2 else -.2) else 0.0
            it.x = bx; it.y = brest - it.h + 1 + if (it.sl != 0.0) 1 else 0
            for (X in max(0, it.x) until it.x + it.w) surf[X] = min(surf[X], it.y + ((X - it.x - it.w / 2.0) * it.sl).roundToInt())
        }
        val x0 = items.minOf { it.x } - 3; val y0 = items.minOf { it.y } - 5; val x1 = items.maxOf { it.x + it.w } + 3; val y1 = 157
        val b = Buf(x1 - x0, y1 - y0)
        for (it in items) heapBox(b, it, it.x - x0, it.y - y0)
        val top = items.minBy { it.y }
        bunnies += intArrayOf(top.x + max(0, (top.w shr 1) - 9), top.y - 8)
        bunnies += intArrayOf(top.x + max(6, top.w shr 1), top.y - 8)
        return Heap(b, x0, y0, x1, y1)
    }

    private fun heapBox(x: Buf, it: HeapBox, bx: Int, by: Int) {
        val r = ramp(it.c)
        val art = it.look?.art?.pixels(it.w, it.tp)
        fun oy(dx: Int) = ((dx - it.w / 2.0) * it.sl).roundToInt()
        for (dx in -1..it.w) {
            val top = by + oy(dx)
            val cx = bx + dx
            if (dx < 0 || dx == it.w) { x.rect(cx, top, 1, it.h, INK); continue }
            for (ry in 0 until it.h) {
                var col = if (ry < it.tp) {
                    art?.get(dx, ry) ?: when (it.k) { Box.CART -> rgb(0xB8B8C4); Box.KEY -> rgb(0xECE6DA); else -> if (ry == 0) r.l else mix(r.l, r.b, .5f) }
                } else {
                    var c = when (it.k) { Box.CART -> if (ry == it.h - 1) rgb(0x5A5A68) else rgb(0x8C8C98); Box.KEY -> rgb(0xC8C0B0); else -> if (ry == it.h - 1) r.dd else r.b }
                    if (it.k == Box.CART && ry == it.tp && dx > 1 && dx < it.w - 2) c = r.b
                    if (it.k == Box.BOARD && ry == it.tp + 2 && dx > 2 && dx < it.w * .6) c = PAPER
                    if (it.k == Box.BOARD && ry == it.tp && dx > 0 && dx < it.w - 1) c = r.d
                    if (it.k == Box.DVD && ry == it.tp && dx % 3 == 1) c = PAPER
                    c
                }
                if (dx == it.w - 1) col = mix(col, INK, .35f)
                x.px(cx, top + ry, col)
            }
            x.px(cx, top - 1, INK); x.px(cx, top + it.h, INK)
        }
        if (it.k != Box.KEY) { val gx = bx + it.w - 5; x.px(gx, by + oy(it.w - 5), rgb(0xFFFFFF)); if (it.tp > 1) x.px(gx + 1, by + oy(it.w - 4) + 1, rgb(0xE8F0FF)) }
        if (it.tag && it.h > 4) x.rect(bx + 2, by + oy(2) + it.h - 3, 3, 2, rgb(0xE83A3A))
    }

    // ---------- the still parts ----------

    private fun drawStatic() {
        val b = static
        // обои
        b.rect(0, 0, W, 82, rgb(0xD8B47C))
        var X = 0
        while (X < W) { b.rect(X, 0, 3, 82, rgb(0xCEA870)); X += 8 }
        var k = 0; var Y = 9
        while (Y < 78) {
            X = if (k % 2 == 1) 9 else 5
            while (X < W) { b.px(X, Y - 1, rgb(0xB88C58)); b.px(X - 1, Y, rgb(0xB88C58)); b.px(X + 1, Y, rgb(0xB88C58)); b.px(X, Y + 1, rgb(0xB88C58)); b.px(X, Y, rgb(0xE8C890)); X += 8 }
            Y += 12; k++
        }
        b.rect(0, 0, W, 3, rgb(0x8A5A34)); b.rect(0, 3, W, 1, rgb(0x6A4226))
        for (i in 0 until W) if (dith(i, 79, .5)) b.px(i, 79, rgb(0xC09A64))
        b.rect(0, 80, W, 5, rgb(0x6A4024)); b.rect(0, 80, W, 1, rgb(0x8A5A34)); b.rect(0, 84, W, 1, rgb(0x3A2012))
        // пол
        var y = 85; var h = 4; var row = 0
        while (y < H) {
            val base = if (row % 2 == 1) rgb(0x9A6238) else rgb(0x8E5A32)
            b.rect(0, y, W, h, base); b.rect(0, y, W, 1, rgb(0x5E3820)); b.rect(0, y + 1, W, 1, mix(base, rgb(0xC88A54), .4f))
            X = (row * 23) % 41
            while (X < W) { b.rect(X, y + 1, 1, h - 1, rgb(0x6A4024)); X += 41 }
            X = (row * 13) % 9
            if (h > 3) while (X < W) { b.px(X, y + 2 + X % (h - 2), rgb(0x7E4E2A)); X += 9 + row % 3 }
            y += h; h += row % 2; row++
        }
        // окно
        b.rect(2, 9, 56, 2, rgb(0xE8B840)); b.rect(2, 9, 56, 1, rgb(0xFFF0A0)); b.rect(0, 8, 3, 4, rgb(0xB07C1C)); b.rect(57, 8, 3, 4, rgb(0xB07C1C))
        b.rect(10, 13, 40, 44, rgb(0x6A4024)); b.rect(11, 14, 38, 42, rgb(0x9A6A3E))
        for (j in 16 until 54) for (i in 14 until 46) b.d[j * W + i] = GLASS or (0x01 shl 24)
        b.rect(29, 16, 2, 38, rgb(0x9A6A3E)); b.rect(14, 34, 32, 2, rgb(0x9A6A3E)); b.px(29, 16, rgb(0xB88A58))
        b.rect(7, 56, 46, 3, rgb(0xB07A48)); b.rect(7, 56, 46, 1, rgb(0xD09A60)); b.rect(7, 59, 46, 1, rgb(0x5A341C))
        if (m.wx == Wx.SNOW) { b.rect(9, 55, 42, 1, rgb(0xF4F8FF)); var sx = 10; while (sx < 50) { b.px(sx, 54, rgb(0xF4F8FF)); sx += 5 } }
        for ((x0, dir) in listOf(4 to 1, 56 to -1)) {
            for (cy in 11 until 64) {
                val w = if (cy < 36) 10 else if (cy < 42) 5 else if (cy < 48) 7 else 9
                val a = if (dir > 0) x0 else x0 - w + 1
                b.rect(a, cy, w, 1, rgb(0xA83048))
                var kk = 1; while (kk < w) { b.px(a + kk, cy, rgb(0x7E1838)); kk += 3 }
                b.px(if (dir > 0) a else a + w - 1, cy, rgb(0xC84A62))
            }
            val tx = if (dir > 0) x0 + 1 else x0 - 6
            b.rect(tx, 38, 6, 2, rgb(0xE8B840)); b.px(tx, 38, rgb(0xFFF0A0))
        }
        // полки
        for (py in intArrayOf(34, 66)) {
            b.rect(68, py + 4, 106, 2, rgb(0xC09A64))
            b.rect(68, py, 106, 3, rgb(0xB07A48)); b.rect(68, py, 106, 1, rgb(0xD09A60)); b.rect(68, py + 3, 106, 1, rgb(0x5A341C))
            for (bx in intArrayOf(74, 164)) { b.rect(bx, py + 4, 2, 5, rgb(0x3A2A20)); b.line(bx + 2, py + 4, bx + 5, py + 4, rgb(0x3A2A20)); b.line(bx + 1, py + 8, bx + 5, py + 4, rgb(0x3A2A20)) }
        }
        for (s in slots) b.blit(s.img, s.x - 1, s.y - 1)
        // лампа
        b.rect(160, 31, 9, 3, rgb(0xB07C1C)); b.rect(160, 31, 9, 1, rgb(0xE8B840))
        b.line(164, 31, 167, 21, rgb(0x6A4410)); b.line(167, 21, 162, 15, rgb(0x6A4410))
        for (i in 0 until 7) { b.rect(155 - (i shr 1), 15 + i, 7 + i, 1, if (i < 2) rgb(0xFFF0A0) else if (i < 5) rgb(0xE8B840) else rgb(0xB07C1C)); b.px(155 - (i shr 1), 15 + i, rgb(0x6A4410)) }
        b.rect(154, 22, 5, 1, rgb(0xFFF2B0))
        // ковёр
        b.ell(86, 160, 47, 14, rgb(0xE8B840)); b.ell(86, 160, 45, 13, rgb(0xA83048)); b.ell(86, 160, 37, 9, rgb(0x8A2440)); b.ell(86, 160, 26, 5, rgb(0xA83048))
        var a = 0
        while (a < 360) { val rr = Math.toRadians(a.toDouble()); b.px(86 + kotlin.math.cos(rr) * 41, 160 + sin(rr) * 11.5, rgb(0xE8B840)); a += 15 }
        var fy = 155; while (fy < 166) { b.px(38, fy, rgb(0xE8D8A0)); b.px(134, fy, rgb(0xE8D8A0)); fy += 2 }
        heap?.let { b.blit(it.img, it.x, it.y) }
        // телевизор
        b.rect(128, 108, 46, 22, rgb(0x5A341C)); b.rect(128, 108, 46, 1, rgb(0x8A5A34)); b.rect(173, 108, 1, 22, rgb(0x3A2012))
        b.rect(132, 113, 38, 11, rgb(0x2A160C)); b.rect(136, 117, 16, 6, rgb(0xC8C8D0)); b.rect(136, 117, 16, 1, rgb(0xE8E8F0)); b.px(150, 119, rgb(0xE83030)); b.rect(139, 119, 8, 1, rgb(0x8A8A98))
        b.rect(130, 130, 3, 4, rgb(0x3A2012)); b.rect(169, 130, 3, 4, rgb(0x3A2012))
        b.line(146, 84, 140, 73, rgb(0x8A8A98)); b.line(150, 84, 157, 72, rgb(0x8A8A98)); b.px(140, 72, rgb(0xC8C8D0)); b.px(157, 71, rgb(0xC8C8D0))
        b.lit(ramp(rgb(0x4A4A56))) { rect(132, 84, 35, 24, it) }
        b.rect(135, 87, 24, 19, rgb(0x1A1A20))
        for (j in 88 until 105) for (i in 136 until 158) b.d[j * W + i] = GLASS or (0x01 shl 24)
        for ((cx, cy) in listOf(136 to 88, 157 to 88, 136 to 104, 157 to 104)) b.px(cx, cy, rgb(0x1A1A20))
        b.rect(161, 89, 3, 3, rgb(0xC8C8D0)); b.rect(161, 94, 3, 3, rgb(0xC8C8D0)); var vy = 99; while (vy < 105) { b.rect(160, vy, 5, 1, rgb(0x2A2A30)); vy += 2 }
        b.line(150, 123, 152, 136, rgb(0x2A2A30))
        b.lit(ramp(rgb(0x9A9AA8))) { rect(147, 136, 11, 4, it); rect(146, 137, 13, 2, it) }
        b.px(149, 137, rgb(0x2A2A30)); b.px(155, 137, rgb(0xC42C40)); b.px(156, 138, rgb(0x3A64B8))
        // стол
        b.rect(125, 160, 3, 28, rgb(0x6A3A20)); b.rect(170, 160, 3, 28, rgb(0x6A3A20)); b.rect(125, 160, 1, 28, rgb(0x8A5A34))
        b.rect(122, 149, 54, 9, rgb(0xB07A48)); b.rect(122, 149, 54, 1, rgb(0xD09A60))
        X = 124; while (X < 176) { b.px(X, 152 + X % 3, rgb(0x9A6638)); X += 7 }
        b.rect(122, 158, 54, 3, rgb(0x7A4626)); b.rect(122, 161, 54, 1, rgb(0x4A2414))
        b.rect(128, 150, 42, 7, rgb(0x5A9A48)); b.rect(128, 150, 42, 1, rgb(0x7AB860))
        X = 134; while (X < 170) { b.rect(X, 150, 1, 7, rgb(0x3E7A36)); X += 6 }
        b.rect(128, 153, 42, 1, rgb(0x3E7A36)); b.rect(140, 151, 5, 2, rgb(0xC8A060)); X = 147; while (X < 160) { b.px(X, 155, rgb(0xE8DCC0)); X += 2 }
    }

    // ---------- the window and the TV screen ----------

    private fun skyCol(k: Double): Int {
        val c = when (m.phase) {
            Phase.EVENING -> if (k < .5) mix(rgb(0x6A4A8A), rgb(0xE87A6A), (k * 2).toFloat()) else mix(rgb(0xE87A6A), rgb(0xF8C888), ((k - .5) * 2).toFloat())
            Phase.DAWN -> if (k < .5) mix(rgb(0x4A5A98), rgb(0xC890B0), (k * 2).toFloat()) else mix(rgb(0xC890B0), rgb(0xF8D0A0), ((k - .5) * 2).toFloat())
            Phase.DAY -> mix(rgb(0x6AA8E8), rgb(0xC8E8F8), k.toFloat())
            Phase.NIGHT -> mix(rgb(0x0C1230), rgb(0x2A3468), k.toFloat())
        }
        val grey = when (m.wx) { Wx.CLEAR -> 0f; Wx.CLOUDY -> .12f; Wx.OVERCAST -> .55f; Wx.DRIZZLE -> .5f; Wx.RAIN -> .6f; Wx.SNOW -> .5f; Wx.FOG -> .7f; Wx.STORM -> .75f }
        return mix(c, when (m.phase) { Phase.NIGHT -> rgb(0x161A26); Phase.DAY -> rgb(0xA8B0B8); else -> rgb(0x8A8090) }, grey)
    }

    private var flash = false

    private fun drawSky(e: Buf, t: Double) {
        e.clear()
        val p = m.phase; val wx = m.wx
        val x0 = 14; val y0 = 16; val gw = 32; val gh = 38
        for (y in y0 until y0 + gh) e.rect(x0, y, gw, 1, skyCol((y - y0) / gh.toDouble()))
        if (wx == Wx.CLEAR || wx == Wx.CLOUDY) when (p) {
            Phase.NIGHT -> {
                for (i in 0 until 14) { val sx = x0 + 1 + hashI("s$i") % 30; val sy = y0 + 1 + hashI("y$i") % 24; if (((t * 2 + i).toInt()) % 5 != 0) e.px(sx, sy, if (i % 3 != 0) rgb(0xC8D0F0) else rgb(0xFFFFFF)) }
                e.ell(22, 24, 4, 4, rgb(0xF0ECD0)); e.ell(24, 23, 3, 3, skyCol(.2))
            }
            Phase.DAY -> { e.ell(39, 23, 4, 4, rgb(0xFFF8D0)); e.ell(39, 23, 2, 2, rgb(0xFFFFFF)) }
            Phase.EVENING -> { e.ell(36, 45, 5, 5, rgb(0xFFE0A0)); e.ell(36, 45, 3, 3, rgb(0xFFF4D0)) }
            Phase.DAWN -> { e.ell(21, 46, 5, 5, rgb(0xFFD8B0)); e.ell(21, 46, 3, 3, rgb(0xFFF0D8)) }
        }
        val nC = when (wx) { Wx.CLEAR -> 1; Wx.CLOUDY -> 3; Wx.OVERCAST -> 6; Wx.DRIZZLE -> 5; Wx.RAIN -> 6; Wx.SNOW -> 5; Wx.FOG -> 3; Wx.STORM -> 7 }
        val heavy = nC > 4
        val cc = when (p) {
            Phase.NIGHT -> if (heavy) rgb(0x2A3044) else rgb(0x3A4260)
            Phase.DAY -> if (heavy) rgb(0xC4C8D0) else rgb(0xFFFFFF)
            Phase.EVENING -> if (heavy) rgb(0x9A8090) else rgb(0xF0A8A0)
            Phase.DAWN -> if (heavy) rgb(0xA89AA8) else rgb(0xF8C8C0)
        }
        val cs = mix(cc, rgb(0), .15f)
        val dark = if (wx == Wx.STORM) mix(cc, rgb(0), .3f) else cc
        for (i in 0 until nC) {
            val h1 = hashI("c$i")
            val sp = (.4 + (h1 % 5) / 10.0) * (1 + m.wind / 20)
            val cx = x0 - 10 + ((h1 % 60 + t * sp) % (gw + 20))
            val cy = (y0 + 3 + (h1 ushr 4) % (if (heavy) 12 else 20)).toDouble()
            val rw = 5 + (h1 ushr 8) % 5
            e.ell(cx, cy + 1, rw, 2, cs); e.ell(cx, cy, rw, 2, dark); e.ell(cx - 2, cy - 1, rw - 2, 2, dark); e.ell(cx + 2, cy - 2, rw - 3, 2, dark)
        }
        val roof = when (p) { Phase.DAY -> if (heavy) rgb(0x7A8898) else rgb(0x8AA0B8); Phase.NIGHT -> rgb(0x080C1C); else -> rgb(0x5A3A5A) }
        for ((hx, w, h) in listOf(Triple(14, 8, 7), Triple(21, 6, 10), Triple(27, 9, 6), Triple(35, 6, 9), Triple(40, 6, 5))) {
            e.rect(hx, y0 + gh - h, w, h, roof); e.px(hx + 2, y0 + gh - h - 1, roof)
            if (wx == Wx.SNOW) e.rect(hx, y0 + gh - h, w, 1, rgb(0xF0F4FF))
        }
        if (p != Phase.DAY || heavy) for ((a, b) in listOf(23 to 47, 30 to 50, 37 to 48, 16 to 50)) e.px(a, b, if (p == Phase.NIGHT) rgb(0xFFD070) else rgb(0xFFC890))
        if (wx == Wx.RAIN || wx == Wx.STORM || wx == Wx.DRIZZLE) {
            val n = if (wx == Wx.DRIZZLE) 14 else if (wx == Wx.STORM) 46 else 32
            val len = if (wx == Wx.DRIZZLE) 1 else 2
            val col = if (p == Phase.NIGHT) rgb(0x5A6A98) else rgb(0xA8BCD8)
            for (i in 0 until n) {
                val h1 = hashI("r$i")
                val y = y0 + floor((h1 % gh + t * (if (wx == Wx.DRIZZLE) 40 else 95)) % gh).toInt()
                val x = x0 + ((h1 ushr 6) % gw + ((y - y0) * .3).toInt()) % gw
                e.rect(x, max(y0, y - len), 1, len + 1, col)
            }
            for (i in 0 until 7) { val h1 = hashI("d$i"); val x = x0 + h1 % gw; val y = y0 + floor(((h1 ushr 5) % gh + t * (1 + h1 % 3)) % gh).toInt(); e.px(x, y, mix(col, rgb(0xFFFFFF), .5f)); e.px(x, y + 1, col) }
        }
        if (wx == Wx.SNOW) for (i in 0 until 34) {
            val h1 = hashI("f$i")
            val y = y0 + floor((h1 % gh + t * (7 + h1 % 6)) % gh).toInt()
            val x = x0 + floor((((h1 ushr 6) % gw) + sin(t * 1.3 + i) * 2 + gw) % gw).toInt()
            e.px(x, y, rgb(0xFFFFFF)); if (i % 4 == 0) e.px(x + 1, y, rgb(0xE0E8FF))
        }
        if (wx == Wx.FOG) { val fc = if (p == Phase.NIGHT) rgb(0x4A5068) else rgb(0xD8DCE0); for (y in y0 until y0 + gh) for (x in x0 until x0 + gw) if (dith(x + (t * 2).toInt(), y, .3 + .3 * (y - y0) / gh)) e.px(x, y, fc) }
        flash = wx == Wx.STORM && t % 6.3 < .14 && (t / 6.3).toInt() % 2 == 0
        if (flash) e.rect(x0, y0, gw, gh, rgb(0xF4F4FF))
        // экран
        val hg = m.hands.firstOrNull()
        e.rect(136, 88, 22, 17, if (hg != null) mix(hg.color, rgb(0), .45f) else rgb(0x1A2040))
        if (hg != null) {
            if (hg.art != null) e.blit(hg.art.pixels(22, 17), 136, 88)
            else { e.rect(138, 100, 18, 3, mix(hg.color, rgb(0), .2f)); e.ell(147, 94, 3, 3, mix(hg.color, rgb(0xFFFFFF), .3f)) }
            if ((t * 1.5).toInt() % 2 == 1) e.rect(141, 101, 12, 1, rgb(0xFFFFFF))
        }
        var sy = 88; while (sy < 105) { for (i in 136 until 158) e.p(i, sy, rgb(0), .2f); sy += 2 }
    }

    // ---------- light ----------

    private fun q(v: Double, n: Int, x: Int, y: Int): Double { val s = v * n; val f = floor(s); return (f + if (dith(x, y, s - f)) 1 else 0) / n }

    private fun light(b: Buf, e: Buf, t: Double) {
        val dim = when (m.wx) { Wx.CLEAR -> 0.0; Wx.CLOUDY -> .04; Wx.OVERCAST -> .12; Wx.DRIZZLE -> .12; Wx.RAIN -> .16; Wx.SNOW -> .06; Wx.FOG -> .12; Wx.STORM -> .22 }
        val p: DoubleArray? = when (m.phase) {
            Phase.DAY -> if (dim < .05) null else doubleArrayOf(1 - dim, 1 - dim, 1 - dim * .5, dim * 1.5, dim)
            Phase.DAWN -> doubleArrayOf(.92 - dim, .8 - dim, .84 - dim, .15, .15)
            Phase.EVENING -> doubleArrayOf(.96 - dim, .8 - dim, .7 - dim, .15, .12)
            Phase.NIGHT -> doubleArrayOf(.30, .33, .56, 1.0, 1.0)
        }
        val flick = .85 + .15 * sin(t * 9)
        val fl = if (flash) .45 else 0.0
        for (y in 0 until H) for (x in 0 until W) {
            val i = y * W + x
            val c = b.d[i]
            val key = c and 0xFFFFFF
            if (c ushr 24 == 1 && key == GLASS) { b.d[i] = e.d[i] or Buf.OPAQUE; continue }
            if ((p == null && fl == 0.0) || key in EMIT) continue
            val a0 = p?.get(0) ?: 1.0; val a1 = p?.get(1) ?: 1.0; val a2 = p?.get(2) ?: 1.0
            val lw = p?.get(3) ?: 0.0; val tw = p?.get(4) ?: 0.0
            var ll = max(0.0, 1 - hypot((x - LAMP[0]).toDouble(), (y - LAMP[1]) * 1.25) / 92); ll = q(ll * ll, 4, x, y) * lw
            var tl = max(0.0, 1 - hypot((x - TV[0]).toDouble(), (y - TV[1]) * 1.2) / 46); tl = q(tl * tl, 3, x, y) * flick * tw
            val r = (Buf.ch(c, 16) * (a0 + ll + tl * .3 + fl)).toInt()
            val g = (Buf.ch(c, 8) * (a1 + ll * .78 + tl * .5 + fl)).toInt()
            val bl = (Buf.ch(c, 0) * (a2 + ll * .38 + tl * .8 + fl * 1.1)).toInt()
            b.d[i] = rgb(r, g, bl)
        }
    }

    // ---------- a frame ----------

    private val sky = Buf(W, H)

    fun draw(b: Buf, t: Double, fx: RoomFx) {
        b.copyFrom(static)
        // мимлы
        val hop = (t * 1.2).toInt() % 3
        val up = (t * 4).toInt() % 2 == 1
        listOf(rgb(0xD84040) to 140, rgb(0x3A6AD0) to 152, rgb(0xF0C030) to 162).forEachIndexed { i, (c, x) ->
            b.blit(Critters.meeple(c, hop == i && up), x, 148)
            if (i == 0) { val cy = if (hop == 0 && up) 1 else 0; b.px(143, 147 - cy, rgb(0xFFE060)); b.px(142, 148 - cy, rgb(0xFFE060)); b.px(144, 148 - cy, rgb(0xFFE060)) }
        }
        // пылевики
        val ba = fx.bunniesAge
        bunnies.forEachIndexed { i, (x, y) ->
            if (ba == null) b.blit(Critters.bunny(((t * 1.6 + i * .7).toInt()) % 3 == 0), x, y)
            else if (ba < 1.2) b.blit(Critters.bunny(true), (x - ba * 90 * (if (i % 2 == 1) 1 else -1) - ba * 40).toInt(), (y - abs(sin(ba * 12)) * 3).toInt())
        }
        // мимик
        b.ell(86, 159, 28, 3, rgb(0x5A1428))
        parts = MimicArt.draw(fx.pose, mimicBuf)
        b.blit(mimicBuf, MPOS[0] + fx.shake, MPOS[1])
        fx.falling?.let { (c, y) ->
            val r = ramp(c)
            val box = Buf(14, 16)
            box.rect(1, 1, 12, 14, r.b); box.rect(1, 1, 12, 2, r.l); box.rect(3, 5, 8, 6, PAPER); box.outline(INK)
            b.blit(box, 79, min(y, MPOS[1] + (parts?.lidBot ?: 40) - 4))
        }
        // ворон
        if (m.crow) { val f = (t * 1.3).toInt() % 4 == 0; b.blit(Critters.crow(f), CROW[0], CROW[1] - if (f) 1 else 0) }
        drawSky(sky, t)
        light(b, sky, t)
        // огоньки
        if (m.phase != Phase.DAY) for (wb in wispBase) {
            val x = wb[0] + sin(t * 1.7 + wb[0]) * 2; val y = wb[1] + sin(t * 2.3 + wb[0]) * 2
            for (j in -6..6) for (i in -6..6) if (i * i + j * j <= 36) b.lighten((x + 4 + i).toInt(), (y + 7 + j).toInt(), 58, 34, 8)
        }
        m.hands.forEachIndexed { i, h ->
            val wb = wispBase[i]
            val x = (wb[0] + sin(t * 1.7 + wb[0]) * 2).roundToInt(); val y = (wb[1] + sin(t * 2.3 + wb[0]) * 2).roundToInt()
            wispPos[h.id] = intArrayOf(x, y)
            b.blit(Critters.wisp((t * 5 + i).toInt() % 2 == 1), x, y)
        }
        fx.ghostAge?.let { d -> if (d < 6) b.blit(Critters.ghostBox((t * 3).toInt() % 3), (20 + d * 18).toInt(), (120 - d * 16 + sin(d * 3) * 3).toInt(), (1 - d / 6).toFloat() * .85f) }
        for (c in fx.crumbs) b.px(c[0], c[1], c[2])
        for (s in fx.sparks) {
            val d = s[2]
            if ((d * 6).toInt() % 2 == 1) continue
            val x = s[0].toInt(); val y = s[1].toInt(); val r = if (d < .8) 1 else 2
            b.px(x, y, rgb(0xFFFFFF))
            for (k in 1..r) { b.px(x - k, y, rgb(0xFFF2B0)); b.px(x + k, y, rgb(0xFFF2B0)); b.px(x, y - k, rgb(0xFFF2B0)); b.px(x, y + k, rgb(0xFFF2B0)) }
        }
        fx.sel?.let { if (fx.selOn) corners(b, it, rgb(0xFFFFFF)) }
    }

    private fun Buf.lighten(x: Int, y: Int, r: Int, g: Int, bl: Int) {
        if (x !in 0 until w || y !in 0 until h) return
        val c = d[y * w + x]
        d[y * w + x] = rgb(Buf.ch(c, 16) + r, Buf.ch(c, 8) + g, Buf.ch(c, 0) + bl)
    }

    private fun corners(b: Buf, r: IntArray, col: Int) {
        val (x, y, w, h) = r
        val l = minOf(4, w shr 1, h shr 1)
        for ((cx, cy, sx, sy) in listOf(intArrayOf(x - 1, y - 1, 1, 1), intArrayOf(x + w, y - 1, -1, 1), intArrayOf(x - 1, y + h, 1, -1), intArrayOf(x + w, y + h, -1, -1))) {
            b.rect(min(cx, cx + sx * (l - 1)), cy, l, 1, col); b.rect(cx, min(cy, cy + sy * (l - 1)), 1, l, col)
        }
    }

    /** What lies under an art pixel, most specific first. */
    fun hit(x: Double, y: Double, bunniesHere: Boolean): Spot? {
        fun inR(a: Int, b: Int, w: Int, h: Int) = x >= a && x < a + w && y >= b && y < b + h
        for ((id, p) in wispPos) if (inR(p[0] - 3, p[1] - 3, 15, 18)) return Spot.Wisp(intArrayOf(p[0] - 1, p[1] - 1, 11, 14), id)
        if (m.crow && inR(CROW[0] - 2, CROW[1] - 2, 20, 16)) return Spot.Crow(intArrayOf(CROW[0], CROW[1], 16, 12))
        if (bunniesHere) for ((bx, by) in bunnies) if (inR(bx - 3, by - 3, 17, 15)) return Spot.Bunnies(intArrayOf(bx, by, 11, 9))
        parts?.let { p -> if (inR(MPOS[0] + p.l - 2, MPOS[1] + 14, p.r - p.l + 4, 62)) return Spot.Mimic(intArrayOf(MPOS[0] + p.l - 1, MPOS[1] + 18, p.r - p.l + 3, 57)) }
        for (s in slots) if (inR(s.x - 1, s.y - 3, s.w + 2, s.h + 5)) return Spot.Shelf(intArrayOf(s.x - 1, s.y - 1, s.w + 2, s.h + 2), s.id)
        heap?.let { h -> if (inR(h.x, h.y, h.x1 - h.x, h.y1 - h.y)) return Spot.Pile(intArrayOf(h.x, h.y, h.x1 - h.x, h.y1 - h.y)) }
        if (inR(122, 145, 54, 44)) return Spot.Table(intArrayOf(122, 145, 54, 18))
        if (inR(128, 70, 46, 60)) return Spot.Tv(intArrayOf(131, 83, 37, 26))
        return null
    }

    /** Where the mimic's mouth is, for sparks and crumbs. */
    val mouth: IntArray get() = intArrayOf(MPOS[0] + MimicArt.W / 2, MPOS[1] + 46)
}
