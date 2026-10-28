package app.mimic.data

import kotlinx.coroutines.flow.Flow

/** Everything that happens to games: in, out of the pile, into hands, down again, eaten, lent, let go. */
class Shelf(private val dao: GameDao) {
    val games: Flow<List<Game>> = dao.all()

    suspend fun add(g: Game): Long = dao.insert(g)

    suspend fun save(g: Game) = dao.update(g)

    suspend fun delete(g: Game): List<Session> { val ss = dao.sessionsOf(g.id); dao.deleteSessionsOf(g.id); dao.delete(g); return ss }

    val sessions: Flow<List<Session>> = dao.sessions()

    /** A sitting with a game; a note also becomes the game's "where I stopped". */
    suspend fun log(s: Session) = dao.insertSession(s)

    suspend fun unlog(s: Session) = dao.deleteSession(s)

    suspend fun get(id: Long) = dao.get(id)

    val styles: Flow<List<Style>> = dao.styles()

    suspend fun addStyle(s: Style): Long = dao.insertStyle(s.copy(name = s.name.trim()))

    /** Deletes a genre of yours with its subgenres; its games keep their built-in base. */
    suspend fun deleteStyle(s: Style) {
        val ids = listOf(s.id) + dao.subStyles(s.id).map { it.id }
        dao.clearStyle(ids)
        dao.deleteStyles(ids)
    }

    suspend fun rate(id: Long, rating: Int, review: String) = dao.get(id)?.let { dao.update(it.copy(rating = rating.coerceIn(0, 5), review = review.trim())) }

    suspend fun setGenre(id: Long, genre: Genre, preset: String, style: Long?) = dao.get(id)?.let { dao.update(it.copy(genre = genre, preset = preset, style = style)) }

    private suspend fun change(id: Long, now: Long = System.currentTimeMillis(), f: (Game) -> Game) {
        val g = dao.get(id) ?: return
        dao.update(f(g).copy(statusAt = now))
    }

    /** Leaving the pile for the first time records how long it waited. */
    private fun Game.leftPile(now: Long) = if (status == Status.PILE && waited == 0L) copy(waited = (now - added).coerceAtLeast(1)) else this

    suspend fun handsFull(): Boolean = dao.list().count { it.status == Status.HANDS } >= HANDS

    /** Into hands: the dust bunnies scatter, the wisp lights. */
    suspend fun take(id: Long, trial: Boolean = false, now: Long = System.currentTimeMillis()) =
        change(id, now) { it.leftPile(now).copy(status = Status.HANDS, touched = now, trial = trial) }

    /** Put down with a note on where you stopped. */
    suspend fun putDown(id: Long, note: String, now: Long = System.currentTimeMillis()) =
        change(id, now) { it.copy(status = Status.SHELF, note = note, touched = now, trial = false) }

    /** The credits: a line, what it gave, and the mimic eats it (the box stays on the shelf, bitten). */
    suspend fun credits(id: Long, line: String, gave: Gave, rating: Int = 0, review: String = "", now: Long = System.currentTimeMillis()) =
        change(id, now) { g ->
            g.leftPile(now).copy(status = Status.DONE, line = line, gave = gave, touched = now, trial = false,
                rating = if (rating > 0) rating else g.rating, review = review.trim().ifEmpty { g.review })
        }

    suspend fun letGo(id: Long, now: Long = System.currentTimeMillis()) = change(id, now) { it.leftPile(now).copy(status = Status.LET, trial = false) }

    suspend fun toPile(id: Long, now: Long = System.currentTimeMillis()) = change(id, now) { it.copy(status = Status.PILE, trial = false) }

    suspend fun toShelf(id: Long, now: Long = System.currentTimeMillis()) = change(id, now) { it.leftPile(now).copy(status = Status.SHELF, trial = false, borrower = "") }

    suspend fun lend(id: Long, who: String, now: Long = System.currentTimeMillis()) = change(id, now) { it.leftPile(now).copy(status = Status.AWAY, borrower = who.trim()) }

    /** The crow brings it back to wherever it lived: eaten games stay eaten. */
    suspend fun back(id: Long, now: Long = System.currentTimeMillis()) = change(id, now) {
        it.copy(status = if (it.gave != null) Status.DONE else Status.SHELF, borrower = "")
    }

    companion object {
        const val HANDS = 3
    }
}
