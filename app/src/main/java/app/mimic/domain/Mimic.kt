package app.mimic.domain

import app.mimic.data.DAY
import app.mimic.data.Game
import app.mimic.data.Gave
import app.mimic.data.Genre
import app.mimic.data.Kind
import app.mimic.data.Length
import app.mimic.data.Session
import app.mimic.data.Status
import app.mimic.data.Trait
import app.mimic.ui.art.MimicArt
import java.time.Instant
import java.time.ZoneId

/** The mimic as the games made it. */
class MimicState(val eaten: Int, val traits: Set<Trait>, val stage: Int)

object Mimic {
    fun of(games: List<Game>, genres: Genres = Genres(emptyList())): MimicState {
        val done = games.filter { it.gave != null }
        val traits = done.mapNotNull { genres.trait(it) }.toSet()
        return MimicState(done.size, traits, MimicArt.stage(done.size, traits.size))
    }

    /** Time on hand, who's playing and what there is energy for: three from the pile that fit. */
    enum class Time { HALF, EVENING, WEEKEND }
    enum class Who { ONE, TWO, GROUP }
    enum class Mood { HEAD, HANDS, HEART }

    fun sniff(pile: List<Game>, time: Time, who: Who, mood: Mood, genres: Genres = Genres(emptyList())): List<Game> {
        val lengths = when (time) {
            Time.HALF -> setOf(Length.HALF)
            Time.EVENING -> setOf(Length.HALF, Length.EVENING)
            Time.WEEKEND -> setOf(Length.EVENING, Length.WEEK, Length.MONTH, Length.ABYSS)
        }
        val moodGenres = when (mood) {
            Mood.HEAD -> setOf(Genre.EURO, Genre.ROGUE, Genre.RPG)
            Mood.HANDS -> setOf(Genre.PLATFORM, Genre.ROGUE, Genre.PARTY)
            Mood.HEART -> setOf(Genre.FARM, Genre.RPG, Genre.HORROR)
        }
        val fits = pile.filter { g -> fitsPlayers(g, who) }
        return fits.sortedWith(compareByDescending<Game> { (if (it.length in lengths) 2 else 0) + (if (it.genre in moodGenres || genres.trait(it)?.let { t -> moodGenres.any { g -> g.trait == t } } == true) 1 else 0) }.thenBy { it.added }).take(3)
    }

    /** Video games are for one unless their players say otherwise; board games read the "2–5". */
    fun fitsPlayers(g: Game, who: Who): Boolean {
        val n = Regex("""\d+""").findAll(g.players).map { it.value.toInt() }.toList()
        if (n.isEmpty()) return if (g.kind == Kind.VIDEO) who == Who.ONE else true
        val lo = n.first(); val hi = n.getOrElse(1) { lo }
        return when (who) { Who.ONE -> lo <= 1; Who.TWO -> 2 in lo..hi; Who.GROUP -> hi >= 3 }
    }

    /** The oldest box at the bottom of the pile, for the weekly dig. */
    fun bottom(games: List<Game>): Game? = games.filter { it.status == Status.PILE }.minByOrNull { it.added }
}

/** A year in games, for the chronicle. */
class Chronicle(
    val year: Int,
    val eaten: Int,
    val perMonth: IntArray,
    val genres: List<Pair<GenreKey, Int>>,
    val video: Int,
    val board: Int,
    val gave: Map<Gave, Int>,
    /** Days a game lay in the pile before it was opened, on average; null with nothing to go on. */
    val avgWait: Int?,
    val oldest: Game?,
    val pile: Int,
    val hands: Int,
    val letGo: Int,
    val lent: Int,
    val traits: Set<Trait>,
    val years: List<Int>,
    /** Minutes played per month from the sessions, the number of sessions, and the game played longest. */
    val minutesPerMonth: IntArray,
    val sessions: Int,
    val shots: Int,
    val longest: Pair<Game, Int>?,
    /** The year's average rating of what was eaten, and its best-rated game. */
    val avgRating: Double?,
    val best: Game?,
)

object Stats {
    fun year(games: List<Game>, sessions: List<Session>, year: Int, genres: Genres = Genres(emptyList()), zone: ZoneId = ZoneId.systemDefault()): Chronicle {
        fun yearOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).year
        val doneThis = games.filter { it.gave != null && yearOf(it.statusAt) == year }
        val perMonth = IntArray(12)
        for (g in doneThis) perMonth[Instant.ofEpochMilli(g.statusAt).atZone(zone).monthValue - 1]++
        val waits = games.filter { it.waited > 0 }.map { (it.waited / DAY).toInt() }
        val thisYear = sessions.filter { yearOf(it.at) == year }
        val years = (games.filter { it.gave != null }.map { yearOf(it.statusAt) } + year).distinct().sortedDescending()
        return Chronicle(
            year = year,
            eaten = doneThis.size,
            perMonth = perMonth,
            genres = doneThis.groupingBy { genres.key(it) }.eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value },
            video = doneThis.count { it.kind == Kind.VIDEO },
            board = doneThis.count { it.kind != Kind.VIDEO },
            gave = doneThis.mapNotNull { it.gave }.groupingBy { it }.eachCount(),
            avgWait = if (waits.isEmpty()) null else waits.average().toInt(),
            oldest = games.filter { it.status == Status.PILE }.minByOrNull { it.added },
            pile = games.count { it.status == Status.PILE },
            hands = games.count { it.status == Status.HANDS },
            letGo = games.count { it.status == Status.LET && yearOf(it.statusAt) == year },
            lent = games.count { it.status == Status.AWAY },
            traits = doneThis.mapNotNull { genres.trait(it) }.toSet(),
            years = years,
            minutesPerMonth = IntArray(12).also { m -> for (s in thisYear) m[Instant.ofEpochMilli(s.at).atZone(zone).monthValue - 1] += s.minutes },
            sessions = thisYear.size,
            avgRating = doneThis.filter { it.rating > 0 }.takeIf { it.isNotEmpty() }?.map { it.rating }?.average(),
            best = doneThis.filter { it.rating > 0 }.maxWithOrNull(compareBy<Game> { it.rating }.thenBy { it.statusAt }),
            shots = thisYear.sumOf { it.shotList.size },
            longest = thisYear.groupBy { it.game }.mapValues { e -> e.value.sumOf { it.minutes } }.maxByOrNull { it.value }
                ?.let { (id, m) -> games.firstOrNull { it.id == id }?.let { it to m } }?.takeIf { it.second > 0 },
        )
    }
}
