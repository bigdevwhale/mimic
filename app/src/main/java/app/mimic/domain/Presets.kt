package app.mimic.domain

import app.mimic.data.Genre
import app.mimic.data.Kind

/**
 * A popular genre offered for one kind of game. It rests on a built-in [base], which gives the
 * plate colour and the mimic's trait; [match] are English words Wikidata uses for it.
 */
class Preset(val key: String, val kind: Kind, val base: Genre, val ru: String, val en: String, val match: List<String> = emptyList()) {
    fun name(ru: Boolean) = if (ru) this.ru else en
}

object Presets {
    private fun v(k: String, b: Genre, ru: String, en: String, vararg m: String) = Preset(k, Kind.VIDEO, b, ru, en, m.toList())
    private fun b(k: String, g: Genre, ru: String, en: String, vararg m: String) = Preset(k, Kind.BOARD, g, ru, en, m.toList())
    private fun r(k: String, g: Genre, ru: String, en: String, vararg m: String) = Preset(k, Kind.RPG, g, ru, en, m.toList())

    /** Most played first, so the top of each list is what people reach for. */
    val all: List<Preset> = listOf(
        // video games
        v("v_action", Genre.OTHER, "экшен", "action", "action game", "hack and slash", "beat 'em up"),
        v("v_adventure", Genre.OTHER, "приключение", "adventure", "adventure game", "point-and-click"),
        v("v_rpg", Genre.RPG, "RPG", "RPG", "role-playing video game", "computer role-playing"),
        v("v_arpg", Genre.RPG, "экшен-RPG", "action RPG", "action role-playing"),
        v("v_jrpg", Genre.RPG, "JRPG", "JRPG", "japanese role-playing"),
        v("v_openworld", Genre.RPG, "открытый мир", "open world", "open world"),
        v("v_shooter", Genre.OTHER, "шутер", "shooter", "first-person shooter", "third-person shooter", "shooter game"),
        v("v_platformer", Genre.PLATFORM, "платформер", "platformer", "platform game"),
        v("v_metroidvania", Genre.PLATFORM, "метроидвания", "metroidvania", "metroidvania"),
        v("v_roguelike", Genre.ROGUE, "рогалик", "roguelike", "roguelike", "roguelite"),
        v("v_deckbuilder", Genre.ROGUE, "карточная", "deckbuilder", "deck-building", "digital collectible card", "card game"),
        v("v_soulslike", Genre.ROGUE, "соулслайк", "soulslike", "soulslike"),
        v("v_horror", Genre.HORROR, "хоррор", "horror", "survival horror", "horror"),
        v("v_strategy", Genre.EURO, "стратегия", "strategy", "real-time strategy", "4x", "grand strategy", "strategy video game"),
        v("v_tactics", Genre.EURO, "тактика", "tactics", "turn-based tactics", "tactical role-playing"),
        v("v_sim", Genre.FARM, "симулятор", "simulation", "simulation video game", "management", "city-building"),
        v("v_cozy", Genre.FARM, "уютная / ферма", "cozy / farming", "farm", "life simulation", "social simulation"),
        v("v_survival", Genre.FARM, "выживание", "survival", "survival game"),
        v("v_sandbox", Genre.FARM, "песочница", "sandbox", "sandbox game"),
        v("v_puzzle", Genre.OTHER, "головоломка", "puzzle", "puzzle video game"),
        v("v_novel", Genre.OTHER, "визуальная новелла", "visual novel", "visual novel", "interactive fiction"),
        v("v_stealth", Genre.OTHER, "стелс", "stealth", "stealth game"),
        v("v_fighting", Genre.OTHER, "файтинг", "fighting", "fighting game"),
        v("v_racing", Genre.OTHER, "гонки", "racing", "racing video game", "racing game"),
        v("v_sports", Genre.OTHER, "спорт", "sports", "sports video game"),
        v("v_rhythm", Genre.PARTY, "ритм", "rhythm", "rhythm game", "music video game"),
        v("v_party", Genre.PARTY, "пати", "party", "party video game"),
        v("v_mmo", Genre.RPG, "MMO", "MMO", "massively multiplayer"),
        // board games
        b("b_euro", Genre.EURO, "евро", "euro", "eurogame", "german-style"),
        b("b_workers", Genre.EURO, "выставление рабочих", "worker placement", "worker placement"),
        b("b_engine", Genre.EURO, "движок", "engine building", "engine building"),
        b("b_tiles", Genre.EURO, "выкладывание тайлов", "tile laying", "tile-laying", "tile-based"),
        b("b_family", Genre.EURO, "семейная", "family", "family game"),
        b("b_coop", Genre.OTHER, "кооператив", "co-op", "cooperative"),
        b("b_deckbuilding", Genre.ROGUE, "колодостроение", "deck-building", "deck-building"),
        b("b_cards", Genre.OTHER, "карточная", "card game", "card game", "trick-taking"),
        b("b_party", Genre.PARTY, "пати", "party", "party game", "word game"),
        b("b_deduction", Genre.PARTY, "скрытые роли", "social deduction", "social deduction", "hidden role"),
        b("b_abstract", Genre.OTHER, "абстрактная", "abstract", "abstract strategy"),
        b("b_area", Genre.OTHER, "контроль территорий", "area control", "area control", "area majority"),
        b("b_wargame", Genre.OTHER, "варгейм", "wargame", "wargame", "miniatures"),
        b("b_crawler", Genre.RPG, "данжен-кроулер", "dungeon crawler", "dungeon crawl"),
        b("b_adventure", Genre.RPG, "приключение", "adventure", "adventure board game", "campaign"),
        b("b_legacy", Genre.RPG, "легаси", "legacy", "legacy game"),
        b("b_horror", Genre.HORROR, "хоррор", "horror", "horror", "lovecraft"),
        b("b_detective", Genre.OTHER, "детектив", "detective", "deduction", "mystery"),
        b("b_rollwrite", Genre.OTHER, "roll & write", "roll & write", "roll-and-write"),
        b("b_dexterity", Genre.PARTY, "на ловкость", "dexterity", "dexterity"),
        b("b_economic", Genre.EURO, "экономическая", "economic", "economic"),
        // tabletop RPGs
        r("r_fantasy", Genre.RPG, "фэнтези", "fantasy", "fantasy"),
        r("r_dark", Genre.HORROR, "тёмное фэнтези", "dark fantasy", "dark fantasy"),
        r("r_horror", Genre.HORROR, "хоррор", "horror", "horror"),
        r("r_investigation", Genre.HORROR, "расследование", "investigation", "investigation", "lovecraftian"),
        r("r_scifi", Genre.OTHER, "научная фантастика", "sci-fi", "science fiction", "space opera"),
        r("r_cyberpunk", Genre.OTHER, "киберпанк", "cyberpunk", "cyberpunk"),
        r("r_postapoc", Genre.OTHER, "постапокалипсис", "post-apocalypse", "post-apocalyptic"),
        r("r_osr", Genre.ROGUE, "OSR", "OSR", "old school"),
        r("r_narrative", Genre.OTHER, "нарративная", "narrative", "narrative", "storytelling", "powered by the apocalypse"),
        r("r_solo", Genre.OTHER, "соло / журнальная", "solo / journaling", "solo", "journaling"),
        r("r_superhero", Genre.OTHER, "супергерои", "superhero", "superhero"),
        r("r_historical", Genre.OTHER, "историческая", "historical", "historical"),
        r("r_comedy", Genre.PARTY, "комедийная", "comedy", "comedy", "humor"),
        r("r_cozy", Genre.FARM, "уютная", "cozy", "cozy", "slice of life"),
    )

    private val byKey = all.associateBy { it.key }
    fun get(key: String?): Preset? = key?.takeIf { it.isNotEmpty() }?.let { byKey[it] }
    fun of(kind: Kind) = all.filter { it.kind == kind }

    /** The first preset of this kind whose words appear in Wikidata's English genre names. */
    fun guess(kind: Kind, genres: String): Preset? {
        val t = genres.lowercase()
        if (t.isBlank()) return null
        return of(kind).firstOrNull { p -> p.match.any { it in t } }
    }
}
