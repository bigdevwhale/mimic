package app.mimic.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.container
import app.mimic.data.Game
import app.mimic.data.Genre
import app.mimic.data.Kind as GameKind
import app.mimic.domain.Presets
import app.mimic.data.Style
import app.mimic.data.Trait
import app.mimic.ui.Home
import app.mimic.ui.art.Boxes
import app.mimic.ui.components.Chip
import app.mimic.ui.components.Field
import app.mimic.ui.components.Flow
import app.mimic.ui.components.Kind
import app.mimic.ui.components.PixelButton
import app.mimic.ui.components.Plate
import app.mimic.ui.components.pixelBox
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import kotlinx.coroutines.launch

/** A game's genre plate: your genre or the built-in one, with the subgenre after a dot. */
@Composable
fun GamePlate(h: Home, g: Game) {
    val custom = h.genres.custom(g)
    val sub = h.genres.sub(g)
    val name = (custom?.name ?: Presets.get(g.preset)?.name(h.ru) ?: stringResource(genreName(g.genre))) + (sub?.let { " · " + it.name } ?: "")
    Plate(name, Color(h.genres.color(g)))
}

/** Colours to choose from for a genre of your own. */
private val SWATCHES = (Genre.entries.filter { it != Genre.OTHER }.map { (0xFF000000 or it.color).toInt() } + Boxes.PALETTE.toList()).distinct()

private enum class Making { NONE, GENRE, SUB }

/**
 * Picks the genre: the popular ones for this kind of game, or yours, then a subgenre under it. New
 * genres (name, colour, trait) and subgenres (a name) are made right here. [onPick] gets the
 * built-in base (for the trait), the popular genre's key, and your genre's id, if any.
 */
@Composable
fun GenrePicker(h: Home, kind: GameKind, genre: Genre, preset: String, style: Long?, onPick: (Genre, String, Long?) -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val cur = h.genres.style(style)
    val customTop = cur?.let { if (it.top) it else it.parentStyle?.let(h.genres::style) }
    val sub = cur?.takeIf { !it.top }
    var making by remember { mutableStateOf(Making.NONE) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(SWATCHES[0]) }
    var trait by remember { mutableStateOf<Trait?>(null) }
    val popular = Presets.of(kind)
    val picked = Presets.get(preset)
    // The first dozen are what most people need; the rest open on request, and stay open if one of them is picked.
    var more by remember { mutableStateOf(picked != null && popular.indexOf(picked) >= 12) }

    Text(stringResource(R.string.f_genre_kind, stringResource(kindName(kind)).let { k -> if (k == k.uppercase()) k else k.lowercase() }), style = T.Small.copy(color = C.ink))
    Flow {
        for (p in if (more) popular else popular.take(12)) Chip(p.name(h.ru), customTop == null && preset == p.key, { onPick(p.base, p.key, null); making = Making.NONE }, color = C.genre(p.base))
        if (!more && popular.size > 12) Chip(stringResource(R.string.genre_more, popular.size - 12), false, { more = true })
        Chip(stringResource(R.string.g_other), customTop == null && preset.isEmpty() && genre == Genre.OTHER, { onPick(Genre.OTHER, "", null); making = Making.NONE }, color = C.genre(Genre.OTHER))
        for (s in h.genres.tops) Chip(s.name, customTop?.id == s.id, { onPick(Genre.OTHER, "", s.id); making = Making.NONE }, color = Color(if (s.color != 0) s.color else SWATCHES[0]))
        Chip(stringResource(R.string.genre_new), making == Making.GENRE, { making = Making.GENRE; name = "" })
    }
    val traitNow = if (customTop != null) (sub?.trait ?: customTop.trait) else (sub?.trait ?: genre.trait)
    Text(stringResource(R.string.trait_line, traitNow?.let { stringResource(traitName(it)) } ?: stringResource(R.string.trait_none)), style = T.Small)
    val subs = h.genres.subsOf(if (customTop == null) genre else null, customTop)
    Text(stringResource(R.string.f_sub), style = T.Small.copy(color = C.ink))
    Flow {
        Chip(stringResource(R.string.sub_none), sub == null, { onPick(genre, if (customTop == null) preset else "", customTop?.id); making = Making.NONE })
        for (s in subs) Chip(s.name, sub?.id == s.id, { onPick(genre, if (customTop == null) preset else "", s.id); making = Making.NONE })
        Chip(stringResource(R.string.sub_new), making == Making.SUB, { making = Making.SUB; name = "" })
    }
    if (making != Making.NONE) Column(Modifier.fillMaxWidth().pixelBox(C.note, outline = C.paperD, bevel = null).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val forGenre = making == Making.GENRE
        Text(stringResource(if (forGenre) R.string.genre_new_title else R.string.sub_new_title, customTop?.name ?: picked?.name(h.ru) ?: stringResource(genreName(genre))), style = T.Body)
        Field(name, { name = it }, stringResource(if (forGenre) R.string.genre_name_hint else R.string.sub_name_hint))
        if (forGenre) {
            Text(stringResource(R.string.genre_color), style = T.Small)
            Flow {
                for (sw in SWATCHES) Box(Modifier.size(30.dp).background(Color(sw)).border(if (sw == color) 3.dp else 1.dp, C.ink).clickable { color = sw })
            }
            Text(stringResource(R.string.genre_trait), style = T.Small)
            Flow {
                Chip(stringResource(R.string.trait_none), trait == null, { trait = null })
                for (t in Trait.entries) Chip(stringResource(traitName(t)), trait == t, { trait = t })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            PixelButton(stringResource(R.string.back), { making = Making.NONE }, kind = Kind.PALE, small = true)
            PixelButton(stringResource(R.string.genre_make), {
                val n = name.trim()
                if (n.isEmpty()) return@PixelButton
                val s = if (forGenre) Style(name = n, color = color, trait = trait)
                        else Style(name = n, parent = if (customTop == null) genre else null, parentStyle = customTop?.id)
                scope.launch {
                    val id = c.shelf.addStyle(s)
                    if (forGenre) onPick(Genre.OTHER, "", id) else onPick(genre, if (customTop == null) preset else "", id)
                    making = Making.NONE
                }
            }, kind = Kind.CRIM, small = true, enabled = name.isNotBlank())
        }
    }
}
