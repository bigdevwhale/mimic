package app.mimic.ui.art

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A small ARGB pixel buffer every picture is painted into, one art pixel per cell. Screens scale it
 * up with nearest-neighbour filtering. Colours are 0xAARRGGBB ints; drawing past the edges is ignored.
 */
class Buf(val w: Int, val h: Int) {
    val d = IntArray(w * h)

    fun copyFrom(o: Buf) = System.arraycopy(o.d, 0, d, 0, d.size)

    fun get(x: Int, y: Int): Int = if (x < 0 || y < 0 || x >= w || y >= h) 0 else d[y * w + x]
    fun filled(x: Int, y: Int): Boolean = get(x, y) ushr 24 != 0

    /** Paints a pixel, blending by [a] (0..1) over what is there. */
    fun p(x: Int, y: Int, c: Int, a: Float = 1f) {
        if (x < 0 || y < 0 || x >= w || y >= h || a <= 0f) return
        val i = y * w + x
        if (a >= 1f || d[i] ushr 24 == 0) {
            d[i] = if (a >= 1f) c or OPAQUE else (c and 0xFFFFFF) or ((a * 255).toInt() shl 24)
            return
        }
        val o = d[i]
        val r = ch(o, 16) + ((ch(c, 16) - ch(o, 16)) * a).toInt()
        val g = ch(o, 8) + ((ch(c, 8) - ch(o, 8)) * a).toInt()
        val b = ch(o, 0) + ((ch(c, 0) - ch(o, 0)) * a).toInt()
        d[i] = (max(o ushr 24, (a * 255).toInt()) shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun clearAt(x: Int, y: Int, w: Int = 1, h: Int = 1) {
        for (j in y until y + h) for (i in x until x + w) if (i in 0 until this.w && j in 0 until this.h) d[j * this.w + i] = 0
    }

    fun clear() = d.fill(0)

    /** Draws [o] at (x, y), skipping its empty pixels; [alpha] fades the whole of it. */
    fun blit(o: Buf, x: Int, y: Int, alpha: Float = 1f) {
        for (j in 0 until o.h) for (i in 0 until o.w) {
            val c = o.d[j * o.w + i]
            val a = (c ushr 24) / 255f * alpha
            if (a > 0f) p(x + i, y + j, c, a)
        }
    }

    /** Draws [o] squeezed or stretched into w×h, nearest neighbour. */
    fun blitScaled(o: Buf, x: Int, y: Int, w: Int, h: Int) {
        for (j in 0 until h) for (i in 0 until w) {
            val c = o.get(i * o.w / w, j * o.h / h)
            if (c ushr 24 != 0) p(x + i, y + j, c)
        }
    }

    companion object {
        const val OPAQUE = 0xFF000000.toInt()
        fun ch(c: Int, sh: Int) = (c shr sh) and 0xFF
    }
}

fun rgb(hex: Long): Int = (hex or 0xFF000000).toInt()
fun rgb(r: Int, g: Int, b: Int): Int = Buf.OPAQUE or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

/** Mixes two colours. */
fun mix(a: Int, b: Int, t: Float): Int {
    fun one(sh: Int) = (Buf.ch(a, sh) + (Buf.ch(b, sh) - Buf.ch(a, sh)) * t).roundToInt().coerceIn(0, 255)
    return Buf.OPAQUE or (one(16) shl 16) or (one(8) shl 8) or one(0)
}

/** Four tones of one colour: light, base, dark, deepest. */
class Ramp(val l: Int, val b: Int, val d: Int, val dd: Int)

private val LIGHT = rgb(0xFFF4D8)
private val DEEP = rgb(0x1C0C10)
fun ramp(c: Int) = Ramp(mix(c, LIGHT, .38f), c, mix(c, DEEP, .36f), mix(c, DEEP, .64f))

private val B4 = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

/** True where the 4×4 ordered dither lets a pixel through at density [t] (0..1). */
fun dith(x: Int, y: Int, t: Double): Boolean = B4[(y and 3) * 4 + (x and 3)] / 16.0 < t

/** A stable unsigned hash of a string, the same on every phone. */
fun hash(s: String): Long {
    var h = 2166136261L
    for (c in s) { h = h xor c.code.toLong(); h = (h * 16777619L) and 0xFFFFFFFFL }
    return h
}
fun hashI(s: String): Int = (hash(s) and 0x7FFFFFFF).toInt()

fun Buf.rect(x: Int, y: Int, w: Int, h: Int, c: Int) {
    for (j in y until y + h) for (i in x until x + w) p(i, j, c)
}
fun Buf.px(x: Int, y: Int, c: Int) = p(x, y, c)
fun Buf.px(x: Double, y: Double, c: Int) = p(floor(x).toInt(), floor(y).toInt(), c)

/** A filled ellipse, row by row, the way the prototype draws it. */
fun Buf.ell(cx: Double, cy: Double, rx: Int, ry: Int, c: Int, a: Float = 1f) {
    for (y in -ry..ry) {
        val w = (rx * sqrt(max(0.0, 1 - (y * y) / ((ry + .5) * (ry + .5))))).roundToInt()
        val x0 = (cx - w).roundToInt()
        val yy = (cy + y).roundToInt()
        for (i in 0..2 * w) p(x0 + i, yy, c, a)
    }
}
fun Buf.ell(cx: Int, cy: Int, rx: Int, ry: Int, c: Int, a: Float = 1f) = ell(cx.toDouble(), cy.toDouble(), rx, ry, c, a)

fun Buf.line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
    var x = x0; var y = y0
    val dx = abs(x1 - x0); val dy = -abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
    var e = dx + dy
    while (true) {
        p(x, y, c)
        if (x == x1 && y == y1) break
        val e2 = 2 * e
        if (e2 >= dy) { e += dy; x += sx }
        if (e2 <= dx) { e += dx; y += sy }
    }
}

/** Rings everything drawn with a one-pixel line of [col]. */
fun Buf.outline(col: Int) {
    val src = BooleanArray(w * h) { d[it] ushr 24 != 0 }
    for (y in 0 until h) for (x in 0 until w) {
        val i = y * w + x
        if (src[i]) continue
        if ((x > 0 && src[i - 1]) || (x < w - 1 && src[i + 1]) || (y > 0 && src[i - w]) || (y < h - 1 && src[i + w])) d[i] = col or Buf.OPAQUE
    }
}

/** Recolours a one-colour mask into a lit shape: light on top and left, shade on the bottom and right. */
fun Buf.shade(r: Ramp) {
    val m = BooleanArray(w * h) { d[it] ushr 24 != 0 }
    fun f(x: Int, y: Int) = x in 0 until w && y in 0 until h && m[y * w + x]
    for (y in 0 until h) for (x in 0 until w) {
        if (!f(x, y)) continue
        val dk = !f(x + 1, y) || !f(x, y + 1) || !f(x + 1, y + 1)
        val lt = !f(x - 1, y) || !f(x, y - 1) || !f(x - 1, y - 1)
        d[y * w + x] = if (dk) r.d else if (lt) r.l else r.b
    }
}

/** Draws a mask with [fn] on a scratch buffer, lights it with [r] and lays it on this one. */
inline fun Buf.lit(r: Ramp, fn: Buf.(Int) -> Unit) {
    val t = Buf(w, h)
    t.fn(Buf.OPAQUE)
    t.shade(r)
    blit(t, 0, 0)
}

fun Buf.silhouette(col: Int) {
    for (i in d.indices) if (d[i] ushr 24 != 0) d[i] = col or Buf.OPAQUE
}

fun Buf.trim(): Buf = this

fun minOf3(a: Int, b: Int, c: Int) = min(a, min(b, c))
