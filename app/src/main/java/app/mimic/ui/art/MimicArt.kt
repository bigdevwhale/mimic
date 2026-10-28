package app.mimic.ui.art

import app.mimic.data.Trait
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** How the mimic looks this frame. */
data class MimicPose(
    val stage: Int,
    val traits: Set<Trait>,
    /** How wide the mouth is open, in pixels: 8 at rest, 20 to swallow a box. */
    val gap: Int = 8,
    val blink: Boolean = false,
    val look: Int = 0,
    val up: Boolean = false,
    val wob: Boolean = false,
    val bounce: Int = 0,
    val noTongue: Boolean = false,
)

/** Where the drawn parts landed, for taps and for the box falling in. */
class MimicParts(val lidBot: Int, val bodyTop: Int, val l: Int, val r: Int)

/** The mimic: a chest with a mouth, eyes in the dark gap and a tongue over the lip; traits are drawn on top. */
object MimicArt {
    const val W = 72
    const val H = 76
    private const val C = W / 2

    private val WOOD = Ramp(rgb(0xD29A5C), rgb(0xAC6E3C), rgb(0x7A4626), rgb(0x4A2414))
    private val BRASS = Ramp(rgb(0xFFF0A0), rgb(0xE8B840), rgb(0xB07C1C), rgb(0x6A4410))
    private val MOUTH = rgb(0x3C0E20)
    private val MOUTH2 = rgb(0x22061A)
    private val TOOTH = rgb(0xFBF3E2)
    private val TOOTHD = rgb(0xCDBCA6)
    private val TONG = Ramp(rgb(0xFF8AA6), rgb(0xE04468), rgb(0xA02044), rgb(0x701030))
    val EYE = rgb(0xFFD84A)
    val EYEL = rgb(0xFFF8C8)
    private val MEEPC = intArrayOf(rgb(0xE88A30), rgb(0xD84040), rgb(0x4A80D0), rgb(0xF0D040), rgb(0x5AB050))
    private val STRAW = Ramp(rgb(0xFFF0A0), rgb(0xE8C860), rgb(0xB08A30), rgb(0x806020))
    private val OUT = rgb(0x2A120C)

    fun draw(o: MimicPose, x: Buf = Buf(W, H)): MimicParts {
        x.clear()
        val s = o.stage.coerceIn(1, 3)
        val t = o.traits
        val w = intArrayOf(40, 48, 56)[s - 1]
        val bh = intArrayOf(14, 17, 20)[s - 1]
        val lh = intArrayOf(11, 13, 15)[s - 1]
        val spring = Trait.SPRING in t
        val g0 = 73
        val feet = if (spring) 0 else if (s == 1) 0 else 3
        val lift = if (spring) 5 + o.bounce else 0
        val l = C - (w shr 1)
        val r = l + w - 1
        val bodyBot = g0 - feet - lift
        val bodyTop = bodyBot - bh + 1
        val g = max(0, o.gap)
        val lidBot = bodyTop - 1 - g
        val lidTop = lidBot - lh + 1

        // ножки
        if (spring) {
            for (fx in intArrayOf(l + 5, r - 9)) {
                for (y in bodyBot + 1 until g0) {
                    if ((y - bodyBot) % 2 != 0) x.rect(fx, y, 5, 1, rgb(0xD8DCE8))
                    else { x.px(fx, y, rgb(0x6A7088)); x.px(fx + 4, y, rgb(0x6A7088)); x.px(fx + 1, y, rgb(0x9AA0B8)) }
                }
                x.rect(fx - 1, g0 - 1, 7, 1, rgb(0x4A5068))
            }
        } else if (feet > 0) {
            for (fx in intArrayOf(l + 2, r - 6)) { x.rect(fx, bodyBot + 1, 5, feet, WOOD.d); x.rect(fx, bodyBot + 1, 5, 1, WOOD.dd); x.px(fx, bodyBot + 2, WOOD.b) }
        }

        // корпус
        x.rect(l, bodyTop, w, bh, WOOD.b)
        var y = bodyTop + 4
        while (y < bodyBot) { x.rect(l, y, w, 1, WOOD.d); x.rect(l, y + 1, w, 1, mix(WOOD.b, WOOD.l, .35f)); y += 5 }
        for (i in 0 until w * bh / 16) {
            val gx = l + 2 + (i * 37) % (w - 4)
            val gy = bodyTop + 2 + (i * 11) % max(1, bh - 3)
            x.px(gx, gy, WOOD.d); x.px(gx + 1, gy, WOOD.d)
        }
        x.rect(l, bodyTop, w, 1, WOOD.l); x.rect(l, bodyTop + 1, w, 1, mix(WOOD.b, WOOD.l, .5f))
        x.rect(l, bodyTop, 1, bh, WOOD.l); x.rect(r - 1, bodyTop, 2, bh, WOOD.d); x.rect(l, bodyBot, w, 1, WOOD.dd)
        if (Trait.STRAW in t) for ((dx, dy) in listOf(6 to 5, 7 to 5, 6 to 6, w - 12 to 8, w - 11 to 8, w - 11 to 9, 14 to bh - 4, 15 to bh - 4)) if (dy < bh - 1) x.px(l + dx, bodyTop + dy, rgb(0x5A3A1C))
        val bands = intArrayOf(l + 4, r - 6)
        for (bx in bands) {
            x.rect(bx, bodyTop, 3, bh, BRASS.b); x.rect(bx, bodyTop, 1, bh, BRASS.l); x.rect(bx + 2, bodyTop, 1, bh, BRASS.d)
            var ry = bodyTop + 3
            while (ry < bodyBot - 1) { x.px(bx + 1, ry, BRASS.l); x.px(bx + 1, ry + 1, BRASS.dd); ry += 4 }
        }
        if (s >= 2) for ((cx, flip) in listOf(l to false, r - 3 to true)) {
            x.rect(cx, bodyBot - 3, 4, 4, BRASS.b); x.rect(cx + if (flip) 0 else 1, bodyBot - 2, 3, 2, WOOD.b)
            x.px(if (flip) cx + 3 else cx, bodyBot, BRASS.d)
        }
        if (s == 3) { x.rect(l, bodyBot - 7, w, 2, BRASS.b); x.rect(l, bodyBot - 7, w, 1, BRASS.l); x.rect(l, bodyBot - 5, w, 1, BRASS.dd) }
        // замок
        val lx = C - 3
        val ly = bodyTop + 3
        x.rect(lx, ly, 7, 8, BRASS.b); x.rect(lx, ly, 7, 1, BRASS.l); x.rect(lx, ly, 1, 8, BRASS.l); x.rect(lx + 6, ly, 1, 8, BRASS.d); x.rect(lx, ly + 7, 7, 1, BRASS.dd)
        x.px(lx + 3, ly + 2, MOUTH2); x.rect(lx + 3, ly + 3, 1, 3, MOUTH2); x.px(lx + 2, ly + 3, MOUTH2); x.px(lx + 4, ly + 3, MOUTH2)

        // пасть
        if (g > 0) {
            x.rect(l + 2, lidBot + 1, w - 4, g, MOUTH)
            x.rect(l + 2, lidBot + 1, w - 4, 1, MOUTH2)
            if (g > 4) x.rect(l + 6, lidBot + 2, w - 12, g - 3, MOUTH2)
            x.rect(l, lidBot + 1, 2, g, WOOD.d); x.rect(r - 1, lidBot + 1, 2, g, WOOD.dd)
        }
        fun mid(tx: Int) = kotlin.math.abs(tx + 1 - C) < 10 && g < 12
        // нижние зубы
        if (g >= 2) {
            var k = 0
            var tx = l + 3
            while (tx <= r - 5) {
                if (!mid(tx)) {
                    if (Trait.MEEPLE in t && g >= 4) {
                        val c = MEEPC[k % MEEPC.size]
                        val my = bodyTop - 1
                        x.px(tx, my, c); x.px(tx + 2, my, c); x.px(tx + 1, my - 1, c); x.rect(tx, my - 2, 3, 1, c); x.px(tx + 1, my - 3, mix(c, rgb(0xFFFFFF), .3f))
                    } else {
                        x.rect(tx, bodyTop - 1, 3, 1, TOOTH); x.px(tx + 2, bodyTop - 1, TOOTHD); x.px(tx + 1, bodyTop - 2, TOOTH)
                    }
                }
                tx += 4; k++
            }
        }
        // глаза
        if (g >= 3) {
            val ey = lidBot + 2 + max(0, (g - 8) / 2)
            if (Trait.DICE in t) {
                for ((ex, pips) in listOf(C - 9 to listOf(1 to 1, 3 to 3), C + 4 to listOf(1 to 1, 2 to 2, 3 to 3))) {
                    val h = min(5, g - 1)
                    x.rect(ex, ey, 5, h, rgb(0xF8F6EE)); x.rect(ex + 4, ey, 1, h, rgb(0xC8C4D0)); x.rect(ex, ey + h - 1, 5, 1, rgb(0xC8C4D0))
                    for ((a, b) in if (o.blink) listOf(2 to 2) else pips) if (b < h) x.px(ex + a, ey + b, rgb(0xC42C40))
                }
            } else {
                val eh = min(4, g - 1)
                for (ex in intArrayOf(C - 8, C + 4)) {
                    if (o.blink) { x.rect(ex, ey + (eh shr 1), 4, 1, EYE); continue }
                    x.rect(ex, ey, 4, eh, EYE); x.rect(ex + 1, ey, 3, 1, EYEL); x.px(ex, ey + 1, EYEL)
                    x.rect(ex + 1 + o.look, ey + max(0, eh - 3) + if (o.up) -1 else 0, 2, min(2, eh - 1), MOUTH2)
                    if (Trait.FANG in t) x.rect(ex, ey + eh - 1, 4, 1, rgb(0xFF7040))
                }
            }
        }
        // свиток
        if (Trait.SCROLL in t && g >= 3) {
            val y0 = bodyTop - min(4, g)
            val x0 = C + 1
            val x1 = r + 8
            x.rect(x0, y0, x1 - x0, 4, rgb(0xF2DFAE)); x.rect(x0, y0, x1 - x0, 1, rgb(0xFFF4D0)); x.rect(x0, y0 + 3, x1 - x0, 1, rgb(0xC9A764))
            var sx = x0 + 2
            while (sx < x1 - 3) { if ((sx * 7) % 5 < 3) x.px(sx, y0 + 1 + if (sx % 4 < 2) 0 else 1, rgb(0x8A6A3A)); sx += 2 }
            x.rect(x1, y0 - 1, 2, 6, rgb(0xD8BC80)); x.px(x1 + 1, y0 - 1, rgb(0xFFF4D0)); x.rect(x1 + 2, y0, 1, 4, rgb(0xA88850))
            x.px(x1 - 1, y0 + 4, rgb(0xC42C40)); x.px(x1 - 1, y0 + 5, rgb(0xC42C40)); x.px(x1, y0 + 6, rgb(0x8A1830))
        }
        // язык
        if (!o.noTongue) {
            val tx = l + (w * .26).roundToInt()
            val tl = intArrayOf(5, 6, 7)[s - 1] + if (o.wob) 1 else 0
            for (i in 0 until tl) {
                val ty = bodyTop - 1 + i
                val sx = tx + if (i > 2 && o.wob) 1 else 0
                if (i == tl - 1) { x.rect(sx + 1, ty, 3, 1, TONG.d); continue }
                x.rect(sx, ty, 5, 1, TONG.b); x.px(sx, ty, TONG.l); x.px(sx + 4, ty, TONG.d)
                if (i in 1 until tl - 2) x.px(sx + 2, ty, TONG.d)
            }
        }
        // верхние зубы и клыки
        if (g >= 2) {
            var tx = l + 3
            while (tx <= r - 5) {
                if (!mid(tx)) { x.rect(tx, lidBot + 1, 3, 1, TOOTH); x.px(tx + 2, lidBot + 1, TOOTHD); x.px(tx + 1, lidBot + 2, TOOTH) }
                tx += 4
            }
        }
        if (Trait.FANG in t && g >= 1) for (fx in intArrayOf(C - 12, C + 10)) {
            val len = min(g + 2, 7)
            x.rect(fx, lidBot + 1, 2, len - 1, TOOTH); x.rect(fx + 1, lidBot + 1, 1, len - 1, TOOTHD); x.px(fx, lidBot + len, TOOTH)
        }

        // крышка
        val rr = min(5, lh - 3)
        for (i in 0 until lh) {
            val ly2 = lidTop + i
            val ins = if (i < rr) (rr - sqrt((rr * rr - (rr - i) * (rr - i)).toDouble())).roundToInt() else 0
            val a = l + ins
            val ww = w - 2 * ins
            val seam = i > 1 && (i - 1) % 4 == 0
            x.rect(a, ly2, ww, 1, when {
                i < 2 -> WOOD.l
                i < 4 -> mix(WOOD.b, WOOD.l, .45f)
                seam -> WOOD.d
                i > lh - 4 -> mix(WOOD.b, WOOD.d, .35f)
                else -> WOOD.b
            })
            x.px(a, ly2, WOOD.l); x.px(a + ww - 1, ly2, WOOD.d); x.px(a + ww - 2, ly2, WOOD.d)
            for (bx in bands) if (bx >= a && bx + 2 <= a + ww - 1) { x.px(bx, ly2, BRASS.l); x.px(bx + 1, ly2, if (i % 4 == 2) BRASS.l else BRASS.b); x.px(bx + 2, ly2, BRASS.d) }
            if (s == 3 && i == 1) { var sx = a + 3; while (sx < a + ww - 3) { x.px(sx, ly2, BRASS.l); sx += 6 } }
        }
        x.rect(l, lidBot - 1, w, 1, WOOD.d); x.rect(l, lidBot, w, 1, WOOD.dd)
        if (s >= 2) { x.rect(l + 1, lidBot - 2, w - 2, 1, BRASS.b); x.px(l + 1, lidBot - 2, BRASS.l) }
        var gx = l + 2
        while (gx < r - 2) { if (dith(gx, lidTop + 3, .3)) x.px(gx, lidTop + 3 + gx % 2, WOOD.d); gx += 3 }

        // соломенная шляпа
        var hatTop = lidTop
        if (Trait.STRAW in t) {
            val cx = C - 2
            val by = lidTop + 1
            x.ell(cx, by, 17, 2, STRAW.b)
            for (hx in cx - 17..cx + 17) for (hy in by - 2..by + 2) {
                val dx = (hx - cx) / 17.5
                val dy = (hy - by) / 2.5
                if ((hx + hy * 2) % 4 == 0 && dx * dx + dy * dy <= 1) x.px(hx, hy, STRAW.d)
            }
            x.rect(cx - 16, by + 2, 33, 1, STRAW.d)
            x.rect(cx - 6, by - 7, 13, 7, STRAW.b)
            x.rect(cx - 5, by - 8, 11, 1, STRAW.l); x.rect(cx - 6, by - 7, 1, 6, STRAW.l); x.rect(cx + 6, by - 7, 1, 7, STRAW.d)
            var wy = by - 6
            while (wy < by - 2) { var wx = cx - 5; while (wx < cx + 6) { x.px(wx + if (wy % 4 != 0) 1 else 0, wy, STRAW.d); wx += 3 }; wy += 2 }
            x.rect(cx - 6, by - 3, 13, 2, rgb(0xC83838)); x.rect(cx - 6, by - 3, 13, 1, rgb(0xE86058))
            x.line(cx + 8, by - 1, cx + 12, by - 4, STRAW.l); x.px(cx + 13, by - 5, STRAW.b)
            hatTop = by - 8
        }
        // колпак
        if (Trait.PARTY in t) {
            val straw = Trait.STRAW in t
            val bx = if (straw) C - 2 else C + 10
            val by = if (straw) hatTop else lidTop + 1
            for (i in 0 until 13) {
                val hw = ((12 - i) * 4 / 12.0).roundToInt()
                val hx = bx + i / 5
                val c = if ((i / 3) % 2 == 1) rgb(0xF070B0) else rgb(0xFFE060)
                x.rect(hx - hw, by - i, 2 * hw + 1, 1, c); x.px(hx + hw, by - i, mix(c, rgb(0x5A1838), .35f))
                if (hw > 0) x.px(hx - hw, by - i, mix(c, rgb(0xFFFFFF), .4f))
            }
            val tx = bx + 2
            val ty = by - 14
            x.rect(tx - 1, ty, 3, 2, rgb(0xFFFFFF)); x.px(tx, ty - 1, rgb(0xFFFFFF)); x.px(tx + 1, ty + 1, rgb(0xD8D0E0))
        }
        x.outline(OUT)
        return MimicParts(lidBot, bodyTop, l, r)
    }

    /** The stage a mimic of this many eaten games and traits has reached. */
    fun stage(eaten: Int, traits: Int): Int = when {
        eaten == 0 -> 1
        eaten >= 12 || traits >= 4 -> 3
        else -> 2
    }
}
