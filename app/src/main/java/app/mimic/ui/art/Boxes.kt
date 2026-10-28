package app.mimic.ui.art

import app.mimic.data.Box
import app.mimic.data.Genre
import kotlin.math.max
import kotlin.math.min

/**
 * A cover picture, kept small, and its pixel versions at every size asked for: shrunk, a little
 * more saturated, and brought down to a dozen colours so it sits with the drawn art.
 */
class CoverArt(val src: Buf) {
    private val cache = HashMap<Long, Buf>()
    private val smooth = HashMap<Long, Buf>()

    fun pixels(w: Int, h: Int): Buf = cache.getOrPut(w.toLong() shl 32 or h.toLong()) { pixelArt(src, w, h, if (w * h < 200) 6 else 12) }

    /** The picture at w×h without the palette, for people who want covers as they are. */
    fun plain(w: Int, h: Int): Buf = smooth.getOrPut(w.toLong() shl 32 or h.toLong()) { fit(src, w, h) }

    /** The cover's most telling colour: frequent, saturated, not too dark or light. */
    val dominant: Int by lazy {
        val pal = quantize(fit(src, 24, 30).d, 12)
        var best = pal.firstOrNull()
        var bs = -1.0
        for (p in pal) {
            val r = Buf.ch(p.c, 16); val g = Buf.ch(p.c, 8); val b = Buf.ch(p.c, 0)
            val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
            val sat = if (mx > 0) (mx - mn) / mx.toDouble() else 0.0
            val l = (mx + mn) / 510.0
            val sc = p.n * (sat + .15) * if (l > .12 && l < .9) 1.0 else .2
            if (sc > bs) { bs = sc; best = p }
        }
        best?.c ?: rgb(0x7A6A5A)
    }
}

/** How a box should look: its form, genre, colour, cover and wear. */
class BoxLook(val box: Box, val genre: Genre, val color: Int, val art: CoverArt? = null, val dusty: Boolean = false, val bitten: Boolean = false)

class Swatch(val c: Int, val n: Int)

/** Median cut down to [k] colours. */
fun quantize(px: IntArray, k: Int): List<Swatch> {
    val boxes = mutableListOf(px.filter { it ushr 24 > 128 }.toMutableList())
    while (boxes.size < k) {
        var bi = -1; var br = -1; var bc = 0
        boxes.forEachIndexed { i, b ->
            if (b.size < 2) return@forEachIndexed
            for (sh in intArrayOf(16, 8, 0)) {
                var mn = 255; var mx = 0
                for (c in b) { val v = Buf.ch(c, sh); if (v < mn) mn = v; if (v > mx) mx = v }
                if (mx - mn > br) { br = mx - mn; bi = i; bc = sh }
            }
        }
        if (bi < 0 || br < 8) break
        val b = boxes[bi].sortedBy { Buf.ch(it, bc) }
        val m = b.size shr 1
        boxes[bi] = b.subList(0, m).toMutableList()
        boxes.add(bi + 1, b.subList(m, b.size).toMutableList())
    }
    return boxes.filter { it.isNotEmpty() }.map { b ->
        var r = 0L; var g = 0L; var bl = 0L
        for (c in b) { r += Buf.ch(c, 16); g += Buf.ch(c, 8); bl += Buf.ch(c, 0) }
        Swatch(rgb((r / b.size).toInt(), (g / b.size).toInt(), (bl / b.size).toInt()), b.size)
    }
}

/** Crops [src] to fill w×h and shrinks it by averaging, so small sizes keep their colours. */
fun fit(src: Buf, w: Int, h: Int): Buf {
    val out = Buf(w, h)
    val s = max(w / src.w.toDouble(), h / src.h.toDouble())
    val cw = w / s; val ch = h / s
    val ox = (src.w - cw) / 2; val oy = (src.h - ch) / 2
    for (y in 0 until h) for (x in 0 until w) {
        val x0 = (ox + x * cw / w).toInt().coerceIn(0, src.w - 1); val x1 = max(x0 + 1, (ox + (x + 1) * cw / w).toInt()).coerceAtMost(src.w)
        val y0 = (oy + y * ch / h).toInt().coerceIn(0, src.h - 1); val y1 = max(y0 + 1, (oy + (y + 1) * ch / h).toInt()).coerceAtMost(src.h)
        var r = 0; var g = 0; var b = 0; var n = 0
        for (j in y0 until y1) for (i in x0 until x1) { val c = src.d[j * src.w + i]; r += Buf.ch(c, 16); g += Buf.ch(c, 8); b += Buf.ch(c, 0); n++ }
        out.d[y * w + x] = rgb(r / n, g / n, b / n)
    }
    return out
}

/** Any picture → a pixel cover w×h: shrink, lift the saturation a little, bring down to [k] colours. */
fun pixelArt(src: Buf, w: Int, h: Int, k: Int = 10): Buf {
    val b = fit(src, w, h)
    for (i in b.d.indices) {
        val c = b.d[i]
        val r = Buf.ch(c, 16); val g = Buf.ch(c, 8); val bl = Buf.ch(c, 0)
        val l = (r + g + bl) / 3.0
        fun lift(v: Int) = (((l + (v - l) * 1.3) - 128) * 1.1 + 128).toInt().coerceIn(0, 255)
        b.d[i] = rgb(lift(r), lift(g), lift(bl))
    }
    val pal = quantize(b.d, k)
    for (i in b.d.indices) {
        val c = b.d[i]
        var best = pal[0].c; var bd = Double.MAX_VALUE
        for (p in pal) {
            val dr = Buf.ch(c, 16) - Buf.ch(p.c, 16); val dg = Buf.ch(c, 8) - Buf.ch(p.c, 8); val db = Buf.ch(c, 0) - Buf.ch(p.c, 0)
            val dd = dr * dr * .3 + dg * dg * .59 + db * db * .11
            if (dd < bd) { bd = dd; best = p.c }
        }
        b.d[i] = best
    }
    return b
}

/** Boxes on the shelf, in the hands and on cards. */
object Boxes {
    private val INK = rgb(0x2A160E)
    private val PAPER = rgb(0xF6EAD0)

    /** Spine sizes on the shelf, without the outline. */
    fun spineSize(b: Box): Pair<Int, Int> = when (b) {
        Box.CART -> 11 to 13
        Box.DVD -> 5 to 20
        Box.BOARD -> 9 to 25
        Box.KEY -> 7 to 11
    }

    fun spine(g: BoxLook): Buf {
        val (w, h) = spineSize(g.box)
        val x = Buf(w + 2, h + 2)
        val r = ramp(g.color)
        val sx = 1; val sy = 1
        when (g.box) {
            Box.CART -> {
                val p = ramp(rgb(0x9C9CA8))
                x.rect(sx, sy, w, h, p.b); x.rect(sx, sy, w, 1, p.l); x.rect(sx, sy, 1, h, p.l); x.rect(sx + w - 1, sy, 1, h, p.d); x.rect(sx, sy + h - 1, w, 1, p.dd)
                for (i in 0 until 3) x.rect(sx + 2, sy + 1 + i, w - 4, 1, if (i % 2 == 1) p.l else p.d)
                if (g.art != null) x.blit(g.art.pixels(w - 4, h - 7), sx + 2, sy + 5)
                else {
                    x.rect(sx + 2, sy + 5, w - 4, h - 7, r.b); x.rect(sx + 2, sy + 5, w - 4, 1, r.l); x.rect(sx + w - 3, sy + 5, 1, h - 7, r.d)
                    x.rect(sx + 3, sy + 7, w - 6, 1, PAPER); x.rect(sx + 3, sy + 9, w - 8, 1, mix(r.l, rgb(0xFFFFFF), .4f))
                }
            }
            Box.DVD -> {
                x.rect(sx, sy, w, h, r.b); x.rect(sx, sy, 1, h, r.l); x.rect(sx + w - 1, sy, 1, h, r.d)
                x.rect(sx, sy, w, 3, rgb(0x1C1C24)); x.px(sx + 2, sy + 1, rgb(0xE0E0E8))
                var y = sy + 5
                while (y < sy + h - 3) { if ((y * 5) % 7 < 5) x.px(sx + 2, y, PAPER); y += 2 }
                x.rect(sx, sy + h - 1, w, 1, r.dd)
            }
            Box.BOARD -> {
                x.rect(sx, sy, w, h, r.b); x.rect(sx, sy, 1, h, r.l); x.rect(sx + w - 1, sy, 1, h, r.d); x.rect(sx, sy, w, 1, r.l)
                if (g.art != null) x.blit(g.art.pixels(w - 2, 7), sx + 1, sy + 3)
                else { x.rect(sx + 1, sy + 4, w - 2, 6, PAPER); x.rect(sx + 2, sy + 6, w - 4, 1, r.d); x.rect(sx + 2, sy + 8, w - 5, 1, r.b) }
                x.rect(sx + 1, sy + h - 7, w - 2, 2, r.d); x.ell(sx + w / 2, sy + 15, 2, 2, mix(r.l, rgb(0xFFFFFF), .3f))
                x.rect(sx, sy + h - 1, w, 1, r.dd)
            }
            Box.KEY -> {
                x.rect(sx, sy, w, h, rgb(0xECE6DA)); x.rect(sx + w - 1, sy, 1, h, rgb(0xC8C0B0)); x.rect(sx, sy, w, 5, r.b); x.rect(sx, sy, w, 1, r.l)
                if (g.art != null) x.blit(g.art.pixels(w, 5), sx, sy)
                x.rect(sx + 2, sy + 1, 3, 1, INK)
                for (i in 0 until w - 2) if (i % 3 != 2) x.rect(sx + 1 + i, sy + h - 4, 1, 2, rgb(0x6A5A4A))
            }
        }
        if (g.bitten) { x.clearAt(sx + w - 2, sy, 2, 2); x.clearAt(sx + w - 3, sy); x.clearAt(sx + w - 1, sy + 2) }
        x.outline(INK)
        if (g.dusty) {
            for (i in 0 until w + 2) if (dith(i, 0, .7)) x.px(i, 0, rgb(0xD0C8BA))
            for (i in 1..w) if (dith(i, 1, .45)) x.px(i, 1, rgb(0xC0B8AA))
        }
        return x
    }

    /** Where the picture sits on a 40×48 cover, by box. */
    fun artRect(b: Box): IntArray = when (b) {
        Box.CART -> intArrayOf(8, 17, 24, 23)
        Box.DVD -> intArrayOf(7, 10, 26, 34)
        Box.BOARD -> intArrayOf(2, 8, 36, 30)
        Box.KEY -> intArrayOf(8, 3, 24, 22)
    }

    /** The 40×48 box front for cards and hands; [plain] keeps the cover picture unpaletted. */
    fun cover(g: BoxLook, plain: Boolean = false): Buf {
        val x = Buf(40, 48)
        val r = ramp(g.color)
        val a = artRect(g.box)
        fun art() = g.art?.let { if (plain) it.plain(a[2], a[3]) else it.pixels(a[2], a[3]) }
        when (g.box) {
            Box.CART -> {
                x.lit(ramp(rgb(0xA8A8B4))) { rect(3, 2, 34, 44, it) }
                for (i in 0 until 5) x.rect(6, 4 + i * 2, 28, 1, rgb(0x7A7A88))
                x.rect(6, 15, 28, 27, PAPER)
                val p = art()
                if (p != null) x.blit(p, 8, 17) else { x.rect(8, 17, 24, 23, r.b); x.rect(8, 17, 24, 2, r.l); icon(x, g.genre, 12, 20) }
                x.rect(14, 44, 12, 2, rgb(0x5A5A68))
            }
            Box.DVD -> {
                x.lit(ramp(rgb(0x2A2E38))) { rect(4, 1, 32, 46, it) }
                x.rect(7, 4, 26, 5, rgb(0x101016)); for (i in 0 until 6) x.px(10 + i * 3, 6, rgb(0xE0E0E8))
                val p = art()
                if (p != null) x.blit(p, 7, 10) else { x.rect(7, 10, 26, 34, r.b); x.rect(7, 10, 26, 2, r.l); x.rect(31, 10, 2, 34, r.d); icon(x, g.genre, 12, 22); x.rect(9, 13, 18, 2, PAPER) }
            }
            Box.BOARD -> {
                x.lit(r) { rect(1, 7, 38, 36, it) }
                x.rect(2, 38, 36, 4, r.d)
                val p = art()
                if (p != null) x.blit(p, 2, 8) else { x.rect(4, 11, 32, 7, PAPER); x.rect(7, 13, 18, 1, r.d); x.rect(7, 15, 12, 1, r.b); icon(x, g.genre, 12, 20) }
            }
            Box.KEY -> {
                x.lit(ramp(rgb(0xECE6DA))) { rect(7, 2, 26, 44, it) }
                val p = art()
                if (p != null) x.blit(p, 8, 3) else { x.rect(8, 3, 24, 22, r.b); x.rect(8, 3, 24, 2, r.l); icon(x, g.genre, 12, 9) }
                x.rect(17, 5, 6, 2, INK)
                for (i in 0 until 20) if (i % 3 != 2) x.rect(10 + i, 36, 1, 5, rgb(0x6A5A4A))
                x.rect(10, 29, 18, 1, rgb(0xA89880)); x.rect(10, 31, 12, 1, rgb(0xA89880))
            }
        }
        x.outline(INK)
        return x
    }

    /** A 16×16 genre picture for a box without a cover. */
    fun icon(x: Buf, genre: Genre, ox: Int, oy: Int) {
        val t = Buf(16, 16)
        when (genre) {
            Genre.OTHER -> { t.rect(3, 4, 10, 8, PAPER); t.rect(5, 6, 6, 1, rgb(0x8A7A6A)); t.rect(5, 8, 4, 1, rgb(0x8A7A6A)) }
            Genre.HORROR -> { t.ell(8, 8, 6, 4, rgb(0xF4F0E8)); t.ell(8, 8, 2, 3, rgb(0xC42C40)); t.px(8, 8, rgb(0x1A0808)); t.px(7, 7, rgb(0xFFFFFF)) }
            Genre.RPG -> { t.rect(7, 1, 2, 10, rgb(0xE0E8F0)); t.px(8, 1, rgb(0xA8B0C0)); t.rect(8, 2, 1, 9, rgb(0xA8B0C0)); t.rect(4, 11, 8, 2, rgb(0xE8B840)); t.rect(7, 13, 2, 2, rgb(0x7A4626)) }
            Genre.FARM -> { t.ell(11, 4, 3, 3, rgb(0xFFD040)); for (i in 0 until 3) t.rect(1, 10 + i * 2, 14, 1, if (i % 2 == 1) rgb(0x3E7A2A) else rgb(0x6AC048)); t.px(4, 9, rgb(0x6AC048)); t.px(9, 9, rgb(0x6AC048)) }
            Genre.ROGUE -> { t.rect(3, 3, 10, 10, rgb(0xF8F6EE)); t.rect(12, 3, 1, 10, rgb(0xC8C4D0)); t.rect(3, 12, 10, 1, rgb(0xC8C4D0)); for ((a, b) in listOf(5 to 5, 8 to 8, 10 to 10, 10 to 5, 5 to 10)) t.px(a, b, rgb(0xC42C40)) }
            Genre.EURO -> { val o = rgb(0xE88A30); t.ell(8, 3, 2, 2, o); t.rect(3, 6, 10, 2, o); t.rect(6, 6, 4, 5, o); t.rect(5, 11, 2, 3, o); t.rect(9, 11, 2, 3, o); t.rect(6, 10, 4, 1, o) }
            Genre.PARTY -> { t.ell(6, 6, 4, 5, rgb(0xF070B0)); t.px(4, 3, rgb(0xFFD0E8)); t.line(6, 11, 8, 15, PAPER); t.px(12, 2, rgb(0xFFE060)); t.px(13, 7, rgb(0x60C0F0)); t.px(11, 11, rgb(0x80E070)); t.px(14, 12, rgb(0xFFE060)) }
            Genre.PLATFORM -> { t.ell(8, 8, 4, 6, rgb(0xFFD040)); t.rect(7, 4, 1, 8, rgb(0xC89820)); t.px(6, 4, rgb(0xFFF6C0)); t.px(6, 5, rgb(0xFFF6C0)) }
        }
        t.outline(INK)
        x.blit(t, ox, oy)
    }

    /** A colour from a title, for boxes without a cover. */
    fun colorFor(title: String): Int = PALETTE[(hashI(title) % PALETTE.size)]

    val PALETTE = intArrayOf(rgb(0xC84A3A), rgb(0x3A6AB8), rgb(0xE0A030), rgb(0x4A9A5A), rgb(0x8A4AB0), rgb(0xD86A9A), rgb(0x2A8A9A), rgb(0xE07A28), rgb(0x6A6A7A), rgb(0xB83A6A))

    @Suppress("unused") private fun clampI(v: Int, a: Int, b: Int) = min(max(v, a), b)
}
