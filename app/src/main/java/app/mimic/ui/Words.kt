package app.mimic.ui

import app.mimic.domain.Phase
import app.mimic.domain.Wx

/** What the mimic says when poked, by the weather and the hour; Russian first, English second. */
object Words {
    private val idle = listOf(
        "Ам? Нет? Ну ладно." to "Om? No? Fine.",
        "В куче что-то шуршит. Наверное, самая старая коробка." to "Something rustles in the pile. The oldest box, probably.",
        "Я сыт. Но от рогалика не откажусь." to "I'm full. I wouldn't say no to a roguelike, though.",
        "Сегодня можно ничего не проходить. Я подожду." to "You don't have to finish anything today. I'll wait.",
        "Пылевики опять облюбовали старые коробки." to "The dust bunnies have moved into the old boxes again.",
        "Это не я съел жетон. Честно." to "I didn't eat the token. Honest.",
        "Огоньки над телевизором помнят, где ты остановился." to "The wisps over the TV remember where you stopped.",
    )
    private val wx = mapOf(
        Wx.RAIN to ("Слышишь дождь? Самое время для длинной игры." to "Hear the rain? Just the day for a long game."),
        Wx.DRIZZLE to ("Моросит. Можно никуда не ходить и честно поиграть." to "Drizzle. A fine excuse to stay in and play."),
        Wx.SNOW to ("Снег идёт. Можно до вечера не вылезать из-под пледа." to "Snow. Stay under the blanket till evening."),
        Wx.STORM to ("Гроза! Я спрячусь под крышку." to "Thunder! I'm hiding under my lid."),
        Wx.FOG to ("Туман, как в Сайлент Хилле. Мне нравится." to "Fog like in Silent Hill. I like it."),
        Wx.OVERCAST to ("Серо. Хорошо, что у нас лампа." to "Grey out. Good thing we have the lamp."),
    )
    private val phase = mapOf(
        Phase.NIGHT to ("Поздно уже. Одну партию — и спать?" to "It's late. One more round and bed?"),
        Phase.DAWN to ("Рассвет. Кто играет на рассвете, тот потом спит до обеда." to "Dawn. Play now and you'll sleep till noon."),
        Phase.DAY to ("Солнце. Может, погулять, а игры подождут?" to "Sunshine. Go for a walk, the games will wait."),
        Phase.EVENING to ("Вечер — лучшее время для настолок." to "Evening is the best time for board games."),
    )

    fun poke(ru: Boolean, w: Wx, p: Phase, roll: Double): String {
        val pair = if (roll < .35) wx[w] ?: phase.getValue(p) else idle[((roll - .35) / .65 * idle.size).toInt().coerceIn(0, idle.size - 1)]
        return if (ru) pair.first else pair.second
    }
}
