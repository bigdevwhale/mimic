package app.mimic.ui.art

/** The creatures of the room: small ones for the room itself, 48×48 portraits for the bestiary. */
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

    /** Bestiary portraits, 48×48 (drawn by tools/critters.py), two frames each: the second bobs up a pixel. */
    enum class Kind { DUST, WISP, MEEPLE, GHOST, DICE, CROW, MERCHANT, GOBLIN, ELDER, SLIME, MOTH, DOUBLE }

    const val PORTRAIT = CritterArt.SIZE

    fun portrait(k: Kind, f: Boolean, known: Boolean = true): Buf {
        val x = Buf(PORTRAIT, PORTRAIT)
        CritterArt.all[k.ordinal].paint(x, 0, if (f) -1 else 0)
        if (!known) x.silhouette(rgb(0x6A4A34))
        return x
    }
}
