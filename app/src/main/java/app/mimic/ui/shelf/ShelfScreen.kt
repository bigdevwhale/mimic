package app.mimic.ui.shelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.data.Status
import app.mimic.ui.Home
import app.mimic.ui.Nav
import app.mimic.ui.Overlay
import app.mimic.ui.components.Chip
import app.mimic.ui.components.Field
import app.mimic.ui.components.Flow
import app.mimic.ui.components.PixelButton
import app.mimic.ui.components.Pill
import app.mimic.ui.components.tray
import app.mimic.ui.room.CoverImage
import app.mimic.ui.sheets.genreName
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T

fun statusName(s: Status) = when (s) {
    Status.HANDS -> R.string.f_hands; Status.SHELF -> R.string.f_shelf; Status.PILE -> R.string.f_pile
    Status.DONE -> R.string.f_done; Status.AWAY -> R.string.f_away; Status.LET -> R.string.f_let
}

fun statusColors(s: Status) = when (s) {
    Status.HANDS -> C.brass to C.ink; Status.SHELF -> C.muted to C.paper; Status.PILE -> C.pile to C.paper
    Status.DONE -> C.done to C.paper; Status.AWAY -> C.away to C.paper; Status.LET -> C.woodL to C.paperD
}

private enum class Sort { NEW, NAME, GENRE, RATING }

/** Every game as a box front: search, status filters with counts, three orders. */
@Composable
fun ShelfScreen(h: Home, nav: Nav, filter: String, onFilter: (String) -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(Sort.NEW) }
    val order = listOf(Status.HANDS, Status.SHELF, Status.PILE, Status.DONE, Status.AWAY, Status.LET)
    val counts = order.associateWith { s -> h.games.count { it.status == s } }
    val list = h.games
        .filter { filter == "all" || it.status.name.lowercase() == filter }
        .filter { q.isBlank() || it.title.contains(q.trim(), ignoreCase = true) }
        .let { l -> when (sort) { Sort.NEW -> l.sortedByDescending { it.added }; Sort.NAME -> l.sortedBy { it.title.lowercase() }; Sort.RATING -> l.sortedWith(compareByDescending<app.mimic.data.Game> { it.rating }.thenBy { it.title.lowercase() }); Sort.GENRE -> l.sortedWith(compareBy({ h.genres.name(it, h.ru) ?: "" }, { it.genre.ordinal }, { it.title.lowercase() })) } }
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(3) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(q, { q = it }, stringResource(R.string.find))
                Flow {
                    Chip(stringResource(R.string.f_all, h.games.size), filter == "all", { onFilter("all") }, dark = true)
                    for (s in order) if (counts[s]!! > 0 || filter == s.name.lowercase()) Chip(stringResource(statusName(s)) + " " + counts[s], filter == s.name.lowercase(), { onFilter(s.name.lowercase()) }, dark = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.order), style = T.Small.copy(color = C.paperD))
                    for ((s, l) in listOf(Sort.NEW to R.string.sort_new, Sort.NAME to R.string.sort_name, Sort.GENRE to R.string.sort_genre, Sort.RATING to R.string.sort_rating))
                        Text(stringResource(l), style = T.Small.copy(color = if (sort == s) C.brass else C.paperD), modifier = Modifier.clickable { sort = s }.padding(vertical = 6.dp))
                }
            }
        }
        if (list.isEmpty()) item(span = { GridItemSpan(3) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (q.isNotBlank()) stringResource(R.string.find_none, q) else stringResource(R.string.shelf_empty), style = T.Body.copy(color = C.paperD), textAlign = TextAlign.Center)
                PixelButton(stringResource(R.string.add_game), { nav.go(Overlay.Add(q)) })
            }
        }
        items(list, key = { it.id }) { g ->
            Column(Modifier.tray().clickable { if (g.status == Status.PILE) nav.dig(g.id) else nav.card(g.id) }.padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box { CoverImage(g, h.s.pixelCovers, 1.5.dp) }
                Text(g.title, style = T.Small.copy(color = C.paper), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val (bg, fg) = statusColors(g.status)
                Pill(stringResource(statusName(g.status)), bg, fg)
                if (g.rating > 0) app.mimic.ui.components.Stars(g.rating, size = 11.dp)
                if (sort == Sort.GENRE) Text(h.genres.name(g, h.ru) ?: stringResource(genreName(g.genre)), style = T.Small.copy(color = C.paperD))
            }
        }
    }
}
