package app.mimic.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** What sort of game: on a screen, on a table, or a role-playing book. */
enum class Kind { VIDEO, BOARD, RPG }

/** How the box looks on the shelf. */
enum class Box { KEY, DVD, CART, BOARD }

/** A trait the mimic grows from a genre it has eaten. */
enum class Trait { FANG, SCROLL, STRAW, DICE, MEEPLE, PARTY, SPRING }

/** Genres the mimic tells apart, each with its plate colour and the trait it gives. */
enum class Genre(val color: Long, val trait: Trait?) {
    HORROR(0x7A3A9A, Trait.FANG),
    RPG(0x3A64B8, Trait.SCROLL),
    FARM(0x4E9A3A, Trait.STRAW),
    ROGUE(0xC8402C, Trait.DICE),
    EURO(0xB8883A, Trait.MEEPLE),
    PARTY(0xD8508E, Trait.PARTY),
    PLATFORM(0xE07A18, Trait.SPRING),
    OTHER(0x8A7A6A, null),
}

/** Where a game is in its life with you. */
enum class Status { PILE, SHELF, HANDS, DONE, LET, AWAY }

/** How long a game takes, roughly; precise hours only press. */
enum class Length { HALF, EVENING, WEEK, MONTH, ABYSS }

/** What a finished game gave you, chosen at the credits. */
enum class Gave { HOOKED, RESTED, DREAMS, JUST_DONE }

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val kind: Kind = Kind.VIDEO,
    val box: Box = Box.KEY,
    val genre: Genre = Genre.OTHER,
    /** Platforms for a video game, players for a board game: shown as they came. */
    val platform: String = "",
    val players: String = "",
    val year: Int? = null,
    val maker: String = "",
    val status: Status = Status.PILE,
    val length: Length = Length.EVENING,
    /** Where you stopped and what's next, written when the game is put down. */
    val note: String = "",
    /** One line at the credits, and what the game gave. */
    val line: String = "",
    val gave: Gave? = null,
    /** File name of the cover in the covers folder, if there is one. */
    val cover: String? = null,
    /** The cover's most telling colour, or one picked from the title. */
    val color: Int = 0,
    val added: Long = System.currentTimeMillis(),
    val statusAt: Long = System.currentTimeMillis(),
    /** Last time it was in hands: the dust bunnies go by this. */
    val touched: Long = System.currentTimeMillis(),
    /** Who has it, while the crow is away with it. */
    val borrower: String = "",
    /** A 30-minute trial from the pile. */
    val trial: Boolean = false,
    /** How long it lay in the pile before anyone opened it, for the chronicle. */
    val waited: Long = 0,
    /** Your own genre or subgenre, if one is picked; [genre] stays the built-in base. */
    val style: Long? = null,
    /** Your rating, 1–5 stars; 0 is none yet. */
    @ColumnInfo(defaultValue = "0") val rating: Int = 0,
    /** A short review in your own words. */
    @ColumnInfo(defaultValue = "") val review: String = "",
    /** A popular genre for its kind (see Presets); [genre] is its built-in base. */
    @ColumnInfo(defaultValue = "") val preset: String = "",
)

/**
 * A genre of your own or a subgenre. A top-level one has neither parent; a subgenre hangs under a
 * built-in genre ([parent]) or under one of yours ([parentStyle]) and takes its colour and trait
 * unless it has its own.
 */
@Entity(tableName = "styles")
data class Style(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val trait: Trait? = null,
    val parent: Genre? = null,
    val parentStyle: Long? = null,
) {
    val top: Boolean get() = parent == null && parentStyle == null
}

/** One sitting with a game: when, how long, a few words, and any screenshots. */
@Entity(tableName = "sessions", indices = [androidx.room.Index("game")])
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val game: Long,
    val at: Long = System.currentTimeMillis(),
    val minutes: Int = 0,
    val note: String = "",
    /** File names in the shots folder, comma-separated. */
    val shots: String = "",
) {
    val shotList: List<String> get() = shots.split(',').filter { it.isNotBlank() }
}

const val DAY = 86_400_000L

/** Shelf games nobody touched for half a year grow dust bunnies. */
fun Game.dusty(now: Long): Boolean = status == Status.SHELF && now - touched > 180 * DAY
