package app.mimic.ui

/** Spans of time said the way people say them: «3 дня», «1 год 7 месяцев», "2 months". */
object Plural {
    fun ru(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10; val m100 = n % 100
        val w = when { m10 == 1 && m100 != 11 -> one; m10 in 2..4 && m100 !in 12..14 -> few; else -> many }
        return "$n $w"
    }

    private fun en(n: Int, w: String) = "$n $w" + if (n == 1) "" else "s"

    fun days(n: Int, ru: Boolean) = if (ru) ru(n, "день", "дня", "дней") else en(n, "day")
    fun months(n: Int, ru: Boolean) = if (ru) ru(n, "месяц", "месяца", "месяцев") else en(n, "month")
    fun years(n: Int, ru: Boolean) = if (ru) ru(n, "год", "года", "лет") else en(n, "year")
    fun games(n: Int, ru: Boolean) = if (ru) ru(n, "игра", "игры", "игр") else en(n, "game")

    /** A length of days: days under two months, then months, then years and months. */
    fun span(days: Int, ru: Boolean): String = when {
        days < 1 -> if (ru) "меньше дня" else "less than a day"
        days < 60 -> days(days, ru)
        days < 365 -> months(days / 30, ru)
        else -> { val y = days / 365; val m = (days % 365) / 30; years(y, ru) + if (m > 0) " " + months(m, ru) else "" }
    }
}
