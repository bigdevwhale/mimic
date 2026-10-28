package app.mimic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import app.mimic.ui.art.Buf
import app.mimic.ui.art.CoverArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/** A search result before it becomes a game. */
data class Draft(
    val kind: Kind,
    val title: String,
    val year: Int? = null,
    val maker: String = "",
    val platform: String = "",
    val players: String = "",
    val genre: Genre? = null,
    /** The popular genre for its kind, when Wikidata's genres point to one. */
    val preset: String = "",
    val coverUrl: String? = null,
)

/**
 * Looking a game up, so it comes in with its cover, year, maker and players. Free and keyless, as in
 * Rewind: Wikidata for the facts, the Wikipedia article's top picture for the cover. This is the only
 * thing besides the weather that goes online, and only when someone searches.
 */
object Catalog {
    private const val AGENT = "Mimic/1.0 (https://github.com/bigdevwhale/mimic)"
    private const val WD = "https://www.wikidata.org/w/api.php?format=json&"

    private val video = setOf("Q7889", "Q865493", "Q21125433")
    private val board = setOf("Q131436", "Q142714", "Q1191150", "Q1153190")
    private val rpg = setOf("Q1643932", "Q2981278")

    private fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", AGENT)
        c.connectTimeout = 8_000
        c.readTimeout = 10_000
        try { if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null } finally { c.disconnect() }
    } catch (_: Exception) { null }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** The genre from Wikidata's English genre names; null when nothing matches. */
    fun guessGenre(s: String): Genre? {
        val t = s.lowercase()
        return when {
            "horror" in t -> Genre.HORROR
            "rogue" in t || "deck-build" in t -> Genre.ROGUE
            "role-playing" in t || "rpg" in t -> Genre.RPG
            "platform" in t || "metroidvania" in t -> Genre.PLATFORM
            "farm" in t || "life sim" in t || "social sim" in t || "cozy" in t -> Genre.FARM
            "party" in t || "social deduction" in t || "word game" in t -> Genre.PARTY
            listOf("euro", "worker placement", "tile-placement", "engine", "economic", "strategy").any { it in t } -> Genre.EURO
            else -> null
        }
    }

    /** The box a game most likely came in, from its platforms. */
    fun boxFor(kind: Kind, platform: String): Box {
        if (kind != Kind.VIDEO) return Box.BOARD
        val p = platform.lowercase()
        return when {
            Regex("""(^|\W)(nes|snes|famicom|game boy|nintendo 64|mega drive|genesis|nintendo ds|3ds|switch)""").containsMatchIn(p) -> Box.CART
            Regex("playstation|xbox|gamecube|wii|dreamcast|saturn").containsMatchIn(p) -> Box.DVD
            else -> Box.KEY
        }
    }

    /** Null when the network failed; an empty list when nothing matched. */
    suspend fun search(q: String, kind: Kind, ru: Boolean): List<Draft>? = withContext(Dispatchers.IO) {
        val query = q.trim()
        if (query.length < 2) return@withContext emptyList()
        val lang = if (Regex("[а-яё]", RegexOption.IGNORE_CASE).containsMatchIn(query)) "ru" else if (ru) "ru" else "en"
        val s = get(WD + "action=wbsearchentities&search=${enc(query)}&language=$lang&uselang=$lang&type=item&limit=20") ?: return@withContext null
        val ids = JSONObject(s).optJSONArray("search")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }.orEmpty()
        if (ids.isEmpty()) return@withContext emptyList()
        val ents = get(WD + "action=wbgetentities&ids=${ids.joinToString("|")}&props=claims|labels|sitelinks&languages=ru|en|mul&sitefilter=enwiki|ruwiki")
            ?.let { JSONObject(it).optJSONObject("entities") } ?: return@withContext null

        class Raw(val label: String, val year: Int?, val makers: List<String>, val genres: List<String>, val plats: List<String>, val mn: Int?, val mx: Int?, val en: String?, val ruw: String?, val img: String?)
        val raws = ArrayList<Raw>()
        for (id in ids) {
            val e = ents.optJSONObject(id) ?: continue
            val c = e.optJSONObject("claims") ?: continue
            val p31 = ids(c, "P31")
            val isVideo = p31.any { it in video } || (c.has("P400") && !c.has("P1872"))
            val isBoard = p31.any { it in board } || c.has("P1872")
            val isRpg = p31.any { it in rpg }
            val ok = when (kind) { Kind.VIDEO -> isVideo; Kind.BOARD -> isBoard && !isVideo; Kind.RPG -> isRpg }
            if (!ok) continue
            val labels = e.optJSONObject("labels")
            val label = listOf(lang, "mul", "en", "ru").firstNotNullOfOrNull { labels?.optJSONObject(it)?.optString("value")?.ifBlank { null } } ?: continue
            val time = c.optJSONArray("P577")?.let { first(it)?.optString("time") }
            val makers = (ids(c, if (kind == Kind.VIDEO) "P178" else "P287") + ids(c, "P123")).take(1)
            val links = e.optJSONObject("sitelinks")
            raws += Raw(label, time?.let { Regex("""(\d{4})""").find(it)?.value?.toIntOrNull() }, makers, ids(c, "P136").take(6), ids(c, "P400").take(3),
                amount(c, "P1872"), amount(c, "P1873"), links?.optJSONObject("enwiki")?.optString("title"), links?.optJSONObject("ruwiki")?.optString("title"),
                c.optJSONArray("P18")?.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optString("value"))
            if (raws.size >= 8) break
        }
        val more = raws.flatMap { it.makers + it.genres + it.plats }.distinct().take(50)
        val names = HashMap<String, String>()
        val en = HashMap<String, String>()
        if (more.isNotEmpty()) get(WD + "action=wbgetentities&ids=${more.joinToString("|")}&props=labels&languages=$lang|en|mul")
            ?.let { JSONObject(it).optJSONObject("entities") }?.let { es ->
                for (id in more) {
                    val l = es.optJSONObject(id)?.optJSONObject("labels") ?: continue
                    listOf(lang, "mul", "en").firstNotNullOfOrNull { l.optJSONObject(it)?.optString("value") }?.let { names[id] = it }
                    listOf("en", "mul", lang).firstNotNullOfOrNull { l.optJSONObject(it)?.optString("value") }?.let { en[id] = it }
                }
            }
        coroutineScope {
            raws.map { r ->
                async {
                    Draft(
                        kind = kind, title = r.label, year = r.year,
                        maker = r.makers.mapNotNull { names[it] }.firstOrNull() ?: "",
                        platform = r.plats.mapNotNull { names[it] }.joinToString(", "),
                        players = r.mn?.let { mn -> if (r.mx != null && r.mx != mn) "$mn–${r.mx}" else "$mn" } ?: "",
                        genre = r.genres.mapNotNull { en[it] }.joinToString(" ").let { gs -> app.mimic.domain.Presets.guess(kind, gs)?.base ?: guessGenre(gs) },
                        preset = app.mimic.domain.Presets.guess(kind, r.genres.mapNotNull { en[it] }.joinToString(" "))?.key ?: "",
                        coverUrl = poster(r.en, "en") ?: poster(r.ruw, "ru") ?: r.img?.let { "https://commons.wikimedia.org/wiki/Special:FilePath/${enc(it.replace(' ', '_'))}?width=320" },
                    )
                }
            }.awaitAll()
        }
    }

    private fun poster(title: String?, lang: String): String? {
        title ?: return null
        val body = get("https://$lang.wikipedia.org/api/rest_v1/page/summary/${enc(title.replace(' ', '_')).replace("+", "%20")}") ?: return null
        return JSONObject(body).optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
    }

    private fun first(a: JSONArray): JSONObject? = a.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")

    private fun ids(c: JSONObject, p: String): List<String> {
        val a = c.optJSONArray(p) ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")?.optString("id")?.ifBlank { null } }
    }

    private fun amount(c: JSONObject, p: String): Int? = c.optJSONArray(p)?.let { first(it) }?.optString("amount")?.toDoubleOrNull()?.toInt()

    /** Downloads a picture's bytes, or null. */
    suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", AGENT)
            c.connectTimeout = 8_000
            c.readTimeout = 15_000
            try { if (c.responseCode in 200..299) c.inputStream.readBytes() else null } finally { c.disconnect() }
        } catch (_: Exception) { null }
    }
}

/** Cover pictures in the app's own folder, and their small pixel-ready copies in memory. */
object Covers {
    private val cache = HashMap<String, CoverArt?>()

    fun dir(context: Context): File = File(context.filesDir, "covers").apply { mkdirs() }

    fun file(context: Context, name: String?): File? = name?.let { File(dir(context), it) }?.takeIf { it.exists() }

    fun delete(context: Context, name: String?) { file(context, name)?.delete(); cache.remove(name) }

    /** Saves picture bytes as a cover, shrunk to a sane size and turned upright; the file's name. */
    fun save(context: Context, bytes: ByteArray): String? {
        val bmp = decode(bytes, 480) ?: return null
        val name = UUID.randomUUID().toString() + ".jpg"
        File(dir(context), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    fun saveUri(context: Context, uri: Uri): String? = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()?.let { save(context, it) }

    private fun decode(bytes: ByteArray, max: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return null
        var sample = 1
        while (o.outWidth / (sample * 2) >= max || o.outHeight / (sample * 2) >= max) sample *= 2
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val rot = runCatching { ExifInterface(bytes.inputStream()).rotationDegrees }.getOrDefault(0)
        return if (rot == 0) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
    }

    /** The picture as a small buffer the art can redraw at any size. */
    fun art(context: Context, name: String?): CoverArt? {
        name ?: return null
        return synchronized(cache) { cache.getOrPut(name) { file(context, name)?.readBytes()?.let { fromBytes(it) } } }
    }

    fun fromBytes(bytes: ByteArray): CoverArt? {
        val b = decode(bytes, 160) ?: return null
        val s = 120.0 / maxOf(b.width, b.height)
        val w = maxOf(8, (b.width * s).toInt()); val h = maxOf(8, (b.height * s).toInt())
        val small = Bitmap.createScaledBitmap(b, w, h, true)
        val buf = Buf(w, h)
        small.getPixels(buf.d, 0, w, 0, 0, w, h)
        for (i in buf.d.indices) buf.d[i] = buf.d[i] or Buf.OPAQUE
        return CoverArt(buf)
    }
}
