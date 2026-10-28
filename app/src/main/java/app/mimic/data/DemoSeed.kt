package app.mimic.data

import android.content.Context
import app.mimic.AppContainer
import app.mimic.domain.Place

/** The demo build's room (`-Pdemo`): a lived-in collection of real classics, a pile, three games in hands. */
object DemoSeed {
    suspend fun seedIfEmpty(ctx: Context, c: AppContainer) {
        if (c.db.games().count() > 0) return
        val ru = ctx.resources.configuration.locales[0].language == "ru"
        val now = System.currentTimeMillis()
        fun ago(days: Int) = now - days * DAY
        fun g(t: String, k: Kind, b: Box, gen: Genre, st: Status, added: Int, at: Int = added, note: String = "", line: String = "", gave: Gave? = null,
              plat: String = "", pl: String = "", len: Length = Length.WEEK, who: String = "", year: Int? = null, touched: Int = at) =
            Game(title = t, kind = k, box = b, genre = gen, status = st, added = ago(added), statusAt = ago(at), touched = ago(touched), note = note, line = line, gave = gave,
                platform = plat, players = pl, length = len, borrower = who, year = year, waited = if (st == Status.PILE) 0 else DAY * 20)
        fun r(a: String, b: String) = if (ru) a else b
        val games = listOf(
            g("Super Mario Bros. 3", Kind.VIDEO, Box.CART, Genre.PLATFORM, Status.DONE, 900, 300, line = r("Свисток в замке — лучшее, что случилось со мной в первом классе.", "The whistle in the castle was the best thing in first grade."), gave = Gave.DREAMS, plat = "NES", year = 1988),
            g("Zelda: A Link to the Past", Kind.VIDEO, Box.CART, Genre.RPG, Status.DONE, 800, 250, line = r("Тёмный мир пугал сильнее любого хоррора.", "The Dark World scared me more than any horror."), gave = Gave.HOOKED, plat = "SNES", year = 1991),
            g("Silent Hill 2", Kind.VIDEO, Box.DVD, Genre.HORROR, Status.SHELF, 700, 420, note = r("Отель «Лейк-вью», третий этаж. Шкатулка не открывается.", "Lakeview Hotel, third floor. The music box won't open."), plat = "PlayStation 2", year = 2001),
            g("Resident Evil 4", Kind.VIDEO, Box.DVD, Genre.HORROR, Status.DONE, 600, 200, line = r("«Чего желаете, незнакомец?» — до сих пор слышу.", "\"What're ya buyin'?\" I still hear it."), gave = Gave.HOOKED, plat = "GameCube", year = 2005),
            g(r("Каркассон", "Carcassonne"), Kind.BOARD, Box.BOARD, Genre.EURO, Status.DONE, 500, 120, line = r("Рома опять забрал все поля.", "Roma took every field again."), gave = Gave.RESTED, pl = "2–5", len = Length.EVENING),
            g(r("Терраформирование Марса", "Terraforming Mars"), Kind.BOARD, Box.BOARD, Genre.EURO, Status.SHELF, 400, 280, note = r("Коробка собрана. Не хватает одного жетона кислорода.", "Box packed. One oxygen token is missing."), pl = "1–5", len = Length.EVENING),
            g(r("Диксит", "Dixit"), Kind.BOARD, Box.BOARD, Genre.PARTY, Status.AWAY, 450, 40, pl = "3–8", len = Length.HALF, who = r("Петя", "Pete")),
            g("Hollow Knight", Kind.VIDEO, Box.KEY, Genre.PLATFORM, Status.SHELF, 350, 90, note = r("Город Слёз, у сломанного лифта. Карты нет — купить у Корнифера.", "City of Tears by the broken lift. No map yet: buy one from Cornifer."), plat = "Steam", year = 2017),
            g("Stardew Valley", Kind.VIDEO, Box.KEY, Genre.FARM, Status.DONE, 330, 160, line = r("Три года на ферме, двенадцать кур, у всех имена.", "Three years on the farm, twelve chickens, all named."), gave = Gave.RESTED, plat = "Switch", len = Length.ABYSS),
            g("Slay the Spire", Kind.VIDEO, Box.KEY, Genre.ROGUE, Status.DONE, 300, 70, line = r("Сердце побеждено. Один раз. Хватит.", "Beat the Heart. Once. Enough."), gave = Gave.HOOKED, plat = "Steam"),
            g(r("Кодовые имена", "Codenames"), Kind.BOARD, Box.BOARD, Genre.PARTY, Status.DONE, 280, 50, line = r("«Луну» отгадали с одного слова.", "Got \"Moon\" from a single clue."), gave = Gave.RESTED, pl = "4–8", len = Length.HALF),
            g("Celeste", Kind.VIDEO, Box.KEY, Genre.PLATFORM, Status.DONE, 260, 30, line = r("Клубнику не собрал, зато гору прошёл.", "Skipped the strawberries, climbed the mountain."), gave = Gave.DREAMS, plat = "Switch"),
            g(r("Ужас Аркхэма", "Arkham Horror"), Kind.BOARD, Box.BOARD, Genre.HORROR, Status.SHELF, 240, 60, note = r("Сценарий второй, Мискатоник. Колоды разложены по пакетикам.", "Scenario two, Miskatonic. Decks bagged."), pl = "1–4", len = Length.EVENING),
            g(r("Ведьмак 3", "The Witcher 3"), Kind.VIDEO, Box.DVD, Genre.RPG, Status.HANDS, 220, 9, note = r("Новиград, «Хамелеон» Лютика. Не забыть сыграть в гвинт с трактирщиком.", "Novigrad, Dandelion's Chameleon. Play gwent with the innkeeper."), plat = "PlayStation 4", len = Length.ABYSS),
            g("Hades", Kind.VIDEO, Box.KEY, Genre.ROGUE, Status.HANDS, 200, 2, note = r("Дошёл до Элизиума с луком и даром Ареса. Попробовать копьё.", "Reached Elysium with the bow and Ares' boon. Try the spear."), plat = "Steam"),
            g("Disco Elysium", Kind.VIDEO, Box.KEY, Genre.RPG, Status.HANDS, 180, 21, note = r("День второй. Найти свой пистолет. Ким ждёт у машины.", "Day two. Find my gun. Kim waits by the car."), plat = "Steam"),
            g("Divinity: Original Sin 2", Kind.VIDEO, Box.DVD, Genre.RPG, Status.PILE, 1520, len = Length.ABYSS, plat = "PlayStation 4"),
            g(r("Взрывные котята", "Exploding Kittens"), Kind.BOARD, Box.BOARD, Genre.PARTY, Status.PILE, 1100, pl = "2–5", len = Length.HALF),
            g("Into the Breach", Kind.VIDEO, Box.KEY, Genre.ROGUE, Status.PILE, 1000, len = Length.HALF),
            g("Inscryption", Kind.VIDEO, Box.KEY, Genre.ROGUE, Status.PILE, 720),
            g(r("Шакал", "Jackal"), Kind.BOARD, Box.BOARD, Genre.PARTY, Status.PILE, 700, pl = "2–4", len = Length.EVENING),
            g("Story of Seasons", Kind.VIDEO, Box.CART, Genre.FARM, Status.PILE, 580, len = Length.ABYSS, plat = "Nintendo Switch"),
            g("Alan Wake 2", Kind.VIDEO, Box.DVD, Genre.HORROR, Status.PILE, 365, len = Length.MONTH, plat = "PlayStation 5"),
            g(r("Крылья", "Wingspan"), Kind.BOARD, Box.BOARD, Genre.EURO, Status.PILE, 240, pl = "1–5", len = Length.EVENING),
            g(r("Каскадия", "Cascadia"), Kind.BOARD, Box.BOARD, Genre.EURO, Status.PILE, 180, pl = "1–4", len = Length.HALF),
            g("A Short Hike", Kind.VIDEO, Box.KEY, Genre.PLATFORM, Status.PILE, 150, len = Length.EVENING),
            g("Outer Wilds", Kind.VIDEO, Box.KEY, Genre.OTHER, Status.PILE, 90),
            g("Balatro", Kind.VIDEO, Box.KEY, Genre.ROGUE, Status.PILE, 30, len = Length.HALF),
            g(r("Остров кошек", "Isle of Cats"), Kind.BOARD, Box.BOARD, Genre.EURO, Status.PILE, 12, pl = "1–4"),
        )
        val dao = c.db.games()
        dao.insertAll(games)
        val byTitle = dao.list().associateBy { it.title }
        val sessions = listOf(
            Triple(r("Ведьмак 3", "The Witcher 3"), 9, 120 to r("Дотянул до Новиграда, полдня в гвинт.", "Made it to Novigrad, half a day of gwent.")),
            Triple(r("Ведьмак 3", "The Witcher 3"), 30, 180 to r("Кровавый барон. Тяжело.", "The Bloody Baron. Heavy.")),
            Triple("Hades", 2, 60 to r("Три забега, все до Элизиума.", "Three runs, all to Elysium.")),
            Triple("Hades", 12, 30 to ""),
            Triple("Disco Elysium", 21, 120 to r("Поговорил с галстуком. Он прав.", "Talked to my tie. It has a point.")),
            Triple("Celeste", 32, 90 to r("Глава 7. Руки трясутся.", "Chapter 7. Hands shaking.")),
            Triple(r("Каркассон", "Carcassonne"), 120, 60 to r("Втроём, Рома опять в короне.", "Three of us, Roma wins again.")),
            Triple("Slay the Spire", 75, 60 to ""),
        )
        for ((t, stars, rev) in listOf(
            Triple("Super Mario Bros. 3", 5, r("Идеальная игра. Каждый мир — отдельная выдумка.", "A perfect game. Every world is its own idea.")),
            Triple("Zelda: A Link to the Past", 5, r("До сих пор помню, где лежит каждый кусочек сердца.", "I still know where every heart piece is.")),
            Triple("Resident Evil 4", 4, r("Напряжение до самого острова, дальше чуть затянуто.", "Tense all the way to the island, a bit long after.")),
            Triple("Celeste", 5, r("Сложная и очень добрая.", "Hard and very kind.")),
            Triple("Slay the Spire", 4, ""),
            Triple("Stardew Valley", 4, r("Лучшее лекарство от плохой недели.", "The best cure for a bad week.")),
            Triple(r("Каркассон", "Carcassonne"), 3, ""),
        )) byTitle[t]?.let { dao.update(it.copy(rating = stars, review = rev)) }
        for ((t, d, ms) in sessions) byTitle[t]?.let { dao.insertSession(Session(game = it.id, at = ago(d), minutes = ms.first, note = ms.second)) }
        c.settings.update { it.copy(onboarded = true, place = Place(if (ru) "Москва" else "Moscow", "", 55.75, 37.62), seen = setOf("dice", "ghost")) }
    }
}
