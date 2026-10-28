package app.mimic.domain

import app.mimic.data.Game
import app.mimic.data.Genre
import app.mimic.data.Style
import app.mimic.data.Trait

/** A genre as the chronicle counts it: one of the built-in ones, or one of yours. */
data class GenreKey(val builtin: Genre?, val style: Style?, val preset: Preset? = null) {
    fun name(ru: Boolean): String? = style?.name ?: preset?.name(ru)

    val color: Int get() = style?.color?.takeIf { it != 0 } ?: (0xFF000000.toInt() or (builtin ?: Genre.OTHER).color.toInt())
}

/** Reads a game's genre through your own genres and subgenres. */
class Genres(val styles: List<Style>) {
    private val byId = styles.associateBy { it.id }

    fun style(id: Long?): Style? = id?.let { byId[it] }

    /** The subgenre the game has, if its style is one. */
    fun sub(g: Game): Style? = style(g.style)?.takeIf { !it.top }

    /** Your top-level genre the game belongs to, through its subgenre if needed. */
    fun custom(g: Game): Style? {
        val s = style(g.style) ?: return null
        return if (s.top) s else s.parentStyle?.let { byId[it] }
    }

    /** The top-level genre: one of yours, or the built-in one. */
    fun key(g: Game): GenreKey = custom(g)?.let { GenreKey(null, it) } ?: GenreKey(g.genre, null, Presets.get(g.preset))

    /** The genre's name as shown: yours, the popular one, or null for the built-in name. */
    fun name(g: Game, ru: Boolean): String? = key(g).name(ru)

    /** The trait a game gives: the subgenre's own, then your genre's, then the built-in genre's. */
    fun trait(g: Game): Trait? {
        sub(g)?.trait?.let { return it }
        custom(g)?.let { return it.trait }
        return g.genre.trait
    }

    /** The plate colour: the subgenre's own, then the genre's. */
    fun color(g: Game): Int = sub(g)?.color?.takeIf { it != 0 } ?: key(g).color

    /** Subgenres under a built-in genre or under one of yours. */
    fun subsOf(builtin: Genre?, custom: Style?): List<Style> = styles.filter { s -> if (custom != null) s.parentStyle == custom.id else s.parent == builtin && s.parent != null }

    val tops: List<Style> get() = styles.filter { it.top }
}
