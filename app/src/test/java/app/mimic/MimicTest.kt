package app.mimic

import app.mimic.data.Game
import app.mimic.data.Gave
import app.mimic.data.Genre
import app.mimic.data.Kind
import app.mimic.data.Length
import app.mimic.data.Status
import app.mimic.data.Style
import app.mimic.data.Trait
import app.mimic.domain.Genres
import app.mimic.domain.Mimic
import app.mimic.domain.Sky
import app.mimic.domain.Phase
import app.mimic.ui.Plural
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MimicTest {
    @Test fun russianPlurals() {
        assertEquals("1 день", Plural.days(1, true))
        assertEquals("3 дня", Plural.days(3, true))
        assertEquals("11 дней", Plural.days(11, true))
        assertEquals("21 день", Plural.days(21, true))
        assertEquals("1 год 7 месяцев", Plural.span(365 + 7 * 30, true))
        assertEquals("2 months", Plural.span(65, false))
    }

    @Test fun traitsComeFromEatenGamesThroughOwnGenres() {
        val own = Style(id = 1, name = "Detective", trait = Trait.PARTY)
        val sub = Style(id = 2, name = "CRPG", parent = Genre.RPG)
        val g = Genres(listOf(own, sub))
        val games = listOf(
            Game(id = 1, title = "A", genre = Genre.HORROR, status = Status.DONE, gave = Gave.HOOKED),
            Game(id = 2, title = "B", genre = Genre.OTHER, style = 1, status = Status.DONE, gave = Gave.RESTED),
            Game(id = 3, title = "C", genre = Genre.RPG, style = 2, status = Status.DONE, gave = Gave.DREAMS),
            Game(id = 4, title = "D", genre = Genre.FARM, status = Status.PILE),
        )
        val m = Mimic.of(games, g)
        assertEquals(setOf(Trait.FANG, Trait.PARTY, Trait.SCROLL), m.traits)
        assertEquals(3, m.eaten)
        assertEquals(2, m.stage)
    }

    @Test fun sniffKeepsPlayersAndPrefersFittingTime() {
        val pile = listOf(
            Game(id = 1, title = "Long", kind = Kind.VIDEO, genre = Genre.EURO, length = Length.ABYSS, added = 1),
            Game(id = 2, title = "Short", kind = Kind.VIDEO, genre = Genre.EURO, length = Length.HALF, added = 2),
            Game(id = 3, title = "Party", kind = Kind.BOARD, genre = Genre.PARTY, players = "4–8", added = 3),
        )
        val r = Mimic.sniff(pile, Mimic.Time.HALF, Mimic.Who.ONE, Mimic.Mood.HEAD)
        assertEquals("Short", r.first().title)
        assertTrue(r.none { it.title == "Party" })
    }

    @Test fun nightAtMidnightInMoscow() {
        // 2026-01-15 00:00 Moscow (UTC+3) = 21:00 UTC the day before.
        assertEquals(Phase.NIGHT, Sky.phase(1768424400000L, 55.75, 37.62))
        // 2026-06-15 12:00 Moscow = 09:00 UTC.
        assertEquals(Phase.DAY, Sky.phase(1781514000000L, 55.75, 37.62))
    }
}
