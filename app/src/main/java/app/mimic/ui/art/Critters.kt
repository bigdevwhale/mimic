package app.mimic.ui.art

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The creatures of the room: small ones for the room itself, 32×32 portraits for the bestiary. */
object Critters {
    private val WHITE = rgb(0xFFFFFF)
    private val PAPER = rgb(0xF6EAD0)

    fun bunny(f: Boolean): Buf {
        val x = Buf(11, 9)
        val y = if (f) 1 else 0
        x.ell(5, 4 + y, 4, 3 - y, rgb(0xB8B2A6))
        x.px(3, y, rgb(0xB8B2A6)); x.px(6, y, rgb(0xCFC9BD)); x.px(8, 1 + y, rgb(0xB8B2A6)); x.px(1, 2 + y, rgb(0xB8B2A6))
        x.px(3, 2 + y, rgb(0xDCD6CA)); x.px(4, 2 + y, rgb(0xDCD6CA)); x.px(3, 3 + y, rgb(0xDCD6CA))
        x.rect(2, 7, 7, 1, rgb(0x8A8478))
        x.px(3, 4 + y, rgb(0x2A2420)); x.px(7, 4 + y, rgb(0x2A2420)); x.px(2, 5 + y, rgb(0xD8A8A0))
        x.outline(rgb(0x5A544C))
        return x
    }

    fun wisp(f: Boolean): Buf {
        val x = Buf(9, 12)
        x.ell(4, 8, 3, 3, rgb(0xF0A030)); x.px(4 + if (f) 1 else -1, 3, rgb(0xF0A030)); x.rect(3, 4, 3, 2, rgb(0xF0A030)); if (f) x.px(4, 2, rgb(0xF0A030))
        x.ell(4, 8, 2, 2, rgb(0xFFD860)); x.rect(4, 5, 1, 2, rgb(0xFFD860))
        x.rect(3, 8, 3, 2, rgb(0xFFFBE8)); x.px(3, 8, rgb(0x7A3A10)); x.px(5, 8, rgb(0x7A3A10))
        return x
    }

    fun crow(f: Boolean): Buf {
        val x = Buf(16, 12)
        val hy = if (f) 1 else 0
        val body = rgb(0x2E2E40)
        x.ell(6, 7, 4, 3, body); x.ell(10, 3 + hy, 2, 2, body)
        x.line(2, 7, 0, 9, body); x.line(3, 8, 1, 10, body)
        x.px(5, 6, rgb(0x5A5A78)); x.px(6, 6, rgb(0x5A5A78)); x.px(4, 7, rgb(0x5A5A78))
        x.rect(9, 1 + hy, 3, 1, rgb(0xC42C58)); x.rect(9, hy, 2, 1, rgb(0xE04A70))
        x.px(13, 3 + hy, rgb(0x8A8A98)); x.px(12, 3 + hy, rgb(0x8A8A98)); x.px(10, 2 + hy, WHITE)
        x.px(5, 10, rgb(0xE8B840)); x.px(8, 10, rgb(0xE8B840))
        x.outline(rgb(0x141420))
        x.px(13, 4 + hy, rgb(0xC8B890)); x.rect(12, 6 + hy, 3, 3, PAPER); x.px(13, 7 + hy, rgb(0xE8A838))
        return x
    }

    fun meeple(col: Int, f: Boolean): Buf {
        val x = Buf(7, 8)
        val r = ramp(col)
        val y = if (f) 0 else 1
        x.rect(2, y, 3, 2, r.b); x.rect(1, 2 + y, 5, 1, r.b); x.rect(2, 3 + y, 3, 1, r.b); x.px(2, 4 + y, r.b); x.px(4, 4 + y, r.b); x.px(1, 5 + y, r.b); x.px(5, 5 + y, r.b)
        x.px(2, y, r.l); x.px(4, 3 + y, r.d)
        x.outline(r.dd)
        return x
    }

    fun ghostBox(f: Int): Buf {
        val x = Buf(14, 16)
        val c = rgb(0xDDE8FF)
        x.rect(2, 1, 10, 11, c); x.rect(2, 1, 10, 1, rgb(0xF4F8FF))
        for (i in 2 until 12) x.rect(i, 12, 1, if ((i + f) % 3 == 0) 3 else 1, c)
        x.rect(3, 3, 8, 2, rgb(0xB8C8EC)); x.rect(4, 7, 2, 2, rgb(0x3A4A78)); x.rect(8, 7, 2, 2, rgb(0x3A4A78)); x.px(6, 10, rgb(0x3A4A78))
        x.outline(rgb(0x8A9AD0))
        return x
    }

    /** Bestiary portraits, 32×32, two frames each. */
    enum class Kind { DUST, WISP, MEEPLE, GHOST, DICE, CROW, MERCHANT, GOBLIN, ELDER, SLIME, MOTH, DOUBLE }

    fun portrait(k: Kind, f: Boolean, known: Boolean = true): Buf {
        val x = Buf(32, 32)
        when (k) {
            Kind.DUST -> dust(x, f)
            Kind.WISP -> wispBig(x, f)
            Kind.MEEPLE -> meepleBig(x, f)
            Kind.GHOST -> ghost(x, f)
            Kind.DICE -> dice(x, f)
            Kind.CROW -> crowBig(x, f)
            Kind.MERCHANT -> merchant(x, f)
            Kind.GOBLIN -> goblin(x, f)
            Kind.ELDER -> {
                for (y in 12 until 29) { val h = 4 + ((y - 12) shr 2); x.rect(16 - h, y, 2 * h + 1, 1, PAPER) }
                x.ell(16, 9, 4, 4, PAPER); x.line(25, 6, 25, 29, PAPER); x.rect(15, 1, 3, 5, PAPER)
            }
            Kind.SLIME -> { x.ell(16, 22, 11, 7, PAPER); x.rect(5, 22, 23, 6, PAPER); x.ell(16, 14, 4, 3, PAPER) }
            Kind.MOTH -> {
                x.ell(9, 15, 7, 6, PAPER); x.ell(23, 15, 7, 6, PAPER); x.ell(10, 23, 5, 4, PAPER); x.ell(22, 23, 5, 4, PAPER)
                x.rect(15, 9, 2, 18, PAPER); x.line(15, 9, 12, 4, PAPER); x.line(16, 9, 19, 4, PAPER)
            }
            Kind.DOUBLE -> {
                x.rect(9, 6, 14, 17, PAPER); for (i in 9 until 23) x.rect(i, 23, 1, if (i % 4 < 2) 3 else 1, PAPER)
                x.rect(4, 10, 4, 12, PAPER); x.rect(24, 10, 4, 12, PAPER)
            }
        }
        if (!known) x.silhouette(rgb(0x6A4A34))
        return x
    }

    private fun dust(x: Buf, f: Boolean) {
        val y = if (f) 1 else 0
        x.lit(Ramp(rgb(0xE0DBD0), rgb(0xB8B2A6), rgb(0x8A8478), rgb(0x5A544C))) {
            ell(16, 19 + y, 10, 8 - y, it)
            var a = 0
            while (a < 360) { val r = a * PI / 180; px(16 + cos(r) * 11, 19.0 + y + sin(r) * (9 - y), it); a += 24 }
            px(13, 9 + y, it); px(17, 9 + y, it); px(15, 10 + y, it)
        }
        x.rect(11, 18 + y, 2, 3, rgb(0x2A2420)); x.rect(19, 18 + y, 2, 3, rgb(0x2A2420)); x.px(11, 18 + y, WHITE); x.px(19, 18 + y, WHITE)
        x.px(9, 22 + y, rgb(0xE0A8A0)); x.px(22, 22 + y, rgb(0xE0A8A0)); x.rect(15, 23 + y, 2, 1, rgb(0x6A645A))
        x.rect(11, 28, 4, 1, rgb(0x6A645A)); x.rect(18, 28, 4, 1, rgb(0x6A645A))
        x.outline(rgb(0x4A443C))
        for ((a, b) in if (f) listOf(4 to 8, 27 to 12, 6 to 26) else listOf(5 to 11, 26 to 7, 25 to 26)) x.px(a, b, rgb(0xC8C2B6))
    }

    private fun wispBig(x: Buf, f: Boolean) {
        x.lit(Ramp(rgb(0xFFD860), rgb(0xF0A030), rgb(0xC06818), rgb(0x8A4A10))) {
            ell(16, 19, 7, 7, it)
            for (i in 0 until 9) rect(16 - ((9 - i) * .6).roundToInt() + if (i < 4) (if (f) 1 else -1) else 0, 4 + i, ((9 - i) * 1.2).roundToInt() + 1, 1, it)
        }
        x.ell(16, 20, 4, 4, rgb(0xFFD860)); x.ell(16, 21, 2, 2, rgb(0xFFFBE8)); x.rect(15, 13, 2, 5, rgb(0xFFD860))
        x.px(14, 19, rgb(0x7A3A10)); x.px(18, 19, rgb(0x7A3A10)); x.rect(15, 22, 3, 1, rgb(0xC06818))
        x.line(16, 26, 18, 28, rgb(0xA88850)); x.rect(17, 27, 7, 4, PAPER); x.rect(18, 28, 4, 1, rgb(0xA89880)); x.rect(18, 29, 3, 1, rgb(0xA89880))
        x.outline(rgb(0x8A4A10))
        for ((a, b) in if (f) listOf(7 to 10, 25 to 15) else listOf(8 to 14, 24 to 9)) x.px(a, b, rgb(0xFFD860))
    }

    private fun meepleBig(x: Buf, f: Boolean) {
        val dx = if (f) 1 else 0
        x.lit(ramp(rgb(0xD84040))) {
            ell(16 + dx, 8, 4, 4, it)
            for ((y, w) in listOf(13 to 18, 14 to 20, 15 to 20, 16 to 16, 17 to 12)) rect(16 - (w shr 1) + if (y < 15) dx else 0, y, w, 1, it)
            rect(11, 18, 10, 3, it)
            for (y in 21 until 28) {
                val sp = (y - 21) shr 1
                val gp = if (y > 21) ((y - 21) / 1.5).toInt() else 0
                rect(10 - sp, y, 12 + 2 * sp, 1, it)
                if (gp > 0) clearAt(16 - gp, y, 2 * gp, 1)
            }
        }
        x.px(14 + dx, 8, rgb(0x2A0C0C)); x.px(18 + dx, 8, rgb(0x2A0C0C))
        for ((a, b) in listOf(12 to 19, 19 to 23, 14 to 25)) x.px(a, b, rgb(0xA02828))
        x.rect(13 + dx, 2, 7, 2, rgb(0xFFE060)); x.px(13 + dx, 1, rgb(0xFFE060)); x.px(16 + dx, 1, rgb(0xFFE060)); x.px(19 + dx, 1, rgb(0xFFE060))
        x.outline(rgb(0x6A1414))
    }

    private fun ghost(x: Buf, f: Boolean) {
        val t = Buf(32, 32)
        val y = if (f) -1 else 0
        val c = rgb(0xDDE8FF)
        t.rect(9, 6 + y, 14, 17, c); t.rect(9, 6 + y, 14, 2, rgb(0xF4F8FF))
        for (i in 9 until 23) t.rect(i, 23 + y, 1, if ((i + if (f) 1 else 0) % 4 < 2) 3 else 1, c)
        t.rect(11, 9 + y, 10, 3, rgb(0xB8C8EC)); t.rect(12, 14 + y, 2, 3, rgb(0x3A4A78)); t.rect(18, 14 + y, 2, 3, rgb(0x3A4A78)); t.rect(15, 19 + y, 2, 2, rgb(0x3A4A78))
        t.rect(22, 7 + y, 1, 16, rgb(0xB8C8EC))
        t.outline(rgb(0x8A9AD0))
        x.blit(t, 0, 0, .85f)
    }

    private fun dice(x: Buf, f: Boolean) {
        val y = if (f) -1 else 0
        x.lit(ramp(rgb(0xE84860))) {
            for (yy in 6..27) {
                val h = if (yy < 11) ((yy - 6) * 9 / 5.0).roundToInt() else if (yy <= 22) 9 else ((27 - yy) * 9 / 5.0).roundToInt()
                rect(16 - h, yy + y, 2 * h + 1, 1, it)
            }
        }
        val d1 = rgb(0xA82040)
        for (s in listOf(intArrayOf(16, 11, 7, 11), intArrayOf(16, 11, 25, 11), intArrayOf(10, 20, 7, 11), intArrayOf(10, 20, 7, 22), intArrayOf(10, 20, 16, 27),
            intArrayOf(22, 20, 25, 11), intArrayOf(22, 20, 25, 22), intArrayOf(22, 20, 16, 27), intArrayOf(16, 11, 10, 20), intArrayOf(16, 11, 22, 20), intArrayOf(10, 20, 22, 20)))
            x.line(s[0], s[1] + y, s[2], s[3] + y, d1)
        for (yy in 12 until 20) { val h = ((yy - 11) * .6).toInt(); x.rect(16 - h + 1, yy + y, maxOf(0, 2 * h - 1), 1, rgb(0xFF8AA0)) }
        if (f) { x.rect(13, 16 + y, 2, 1, rgb(0x2A0C14)); x.rect(18, 16 + y, 2, 1, rgb(0x2A0C14)) }
        else { x.rect(13, 15 + y, 2, 2, WHITE); x.rect(18, 15 + y, 2, 2, WHITE); x.px(14, 16 + y, rgb(0x2A0C14)); x.px(19, 16 + y, rgb(0x2A0C14)) }
        x.rect(12, 28, 3, 1, rgb(0x2A0C14)); x.rect(18, 28, 3, 1, rgb(0x2A0C14))
        x.outline(rgb(0x5A1020))
    }

    private fun crowBig(x: Buf, f: Boolean) {
        val hy = if (f) 1 else 0
        x.lit(Ramp(rgb(0x5A5A78), rgb(0x2E2E40), rgb(0x1C1C28), rgb(0x0C0C14))) {
            ell(14, 19, 8, 6, it); ell(21, 11 + hy, 5, 5, it); line(7, 20, 2, 25, it); line(7, 21, 3, 26, it); line(8, 22, 4, 27, it)
        }
        for ((a, b) in listOf(10 to 17, 12 to 18, 14 to 19, 11 to 20)) x.px(a, b, rgb(0x5A5A78))
        x.rect(18, 5 + hy, 8, 2, rgb(0xC42C58)); x.rect(18, 5 + hy, 8, 1, rgb(0xE04A70)); x.rect(24, 7 + hy, 4, 1, rgb(0x7E1838))
        x.rect(26, 11 + hy, 3, 1, rgb(0x8A8A98)); x.rect(26, 12 + hy, 4, 1, rgb(0x6A6A78)); x.px(22, 10 + hy, WHITE); x.px(23, 10 + hy, rgb(0x141420))
        x.line(14, 25, 14, 28, rgb(0xE8B840)); x.line(18, 25, 18, 28, rgb(0xE8B840)); x.rect(13, 28, 3, 1, rgb(0xE8B840)); x.rect(17, 28, 3, 1, rgb(0xE8B840))
        x.outline(rgb(0x0C0C14))
        x.line(29, 13 + hy, 29, 17, rgb(0xC8B890)); x.rect(26, 17, 6, 5, PAPER); x.px(28, 18, rgb(0x2A160E)); x.rect(27, 20, 4, 1, rgb(0xE8A838))
    }

    private fun merchant(x: Buf, f: Boolean) {
        val cl = ramp(rgb(0x5A3A6A))
        x.lit(cl) { for (y in 10 until 29) { val h = 5 + ((y - 10) * 6 / 18.0).roundToInt(); rect(16 - h, y, 2 * h + 1, 1, it) }; ell(16, 10, 6, 6, it) }
        x.ell(16, 11, 4, 4, rgb(0x1A0C1A)); x.rect(12, 15, 9, 1, rgb(0x1A0C1A))
        val eye = if (f) rgb(0xA08020) else MimicArt.EYE
        x.px(14, 11, eye); x.px(18, 11, eye)
        x.lit(ramp(rgb(0x9A6A38))) { ell(25, 23, 4, 4, it); rect(24, 18, 3, 2, it) }
        x.rect(23, 20, 5, 1, rgb(0xC8A060)); x.px(26, 24, rgb(0xFFE060))
        x.rect(15, 18, 3, 6, cl.d); x.px(10, 26, rgb(0xE8B840))
        x.outline(rgb(0x1A0C1A))
    }

    private fun goblin(x: Buf, f: Boolean) {
        val gr = ramp(rgb(0x78B848))
        val ey = if (f) 1 else 0
        x.lit(ramp(rgb(0x8A6A3A))) { rect(11, 18, 10, 8, it); rect(10, 20, 12, 5, it) }
        x.lit(gr) {
            ell(16, 12, 6, 5, it)
            for (i in 0 until 6) { val up = (i shr 1) + if (i > 3) ey else 0; rect(10 - i, 10 - up, 2, 2, it); rect(21 + i, 10 - up, 2, 2, it) }
            rect(12, 26, 3, 3, it); rect(17, 26, 3, 3, it)
        }
        x.rect(12, 11, 3, 3, WHITE); x.rect(18, 11, 3, 3, WHITE); x.px(13, 12, rgb(0x1A2A0C)); x.px(19, 12, rgb(0x1A2A0C))
        x.px(16, 14, gr.d); x.rect(14, 16, 5, 1, rgb(0x3A1A0C)); x.px(15, 17, WHITE)
        x.lit(ramp(PAPER)) { rect(20, 18, 7, 9, it) }
        x.rect(22, 20, 3, 1, rgb(0xC42C40)); x.px(24, 21, rgb(0xC42C40)); x.px(23, 22, rgb(0xC42C40)); x.px(23, 24, rgb(0xC42C40))
        x.outline(rgb(0x2A3A14))
    }
}
