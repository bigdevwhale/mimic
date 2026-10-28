package app.mimic.ui.sheets

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.data.Session
import app.mimic.data.Shots
import app.mimic.ru
import app.mimic.ui.components.Chip
import app.mimic.ui.components.Field
import app.mimic.ui.components.Flow
import app.mimic.ui.components.Kind
import app.mimic.ui.components.PixelButton
import app.mimic.ui.components.pixelBox
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val MINUTES = listOf(15, 30, 60, 120, 180)

fun minutesText(m: Int, ru: Boolean): String = when {
    m <= 0 -> ""
    m < 60 -> if (ru) "$m мин" else "$m min"
    m % 60 == 0 -> if (ru) "${m / 60} ч" else "${m / 60} h"
    else -> if (ru) "${m / 60} ч ${m % 60} мин" else "${m / 60} h ${m % 60} min"
}

/** A session's form: how long, a few words, screenshots. Used alone and when putting a game down. */
class SessionDraft {
    var minutes by mutableStateOf(60)
    var note by mutableStateOf("")
    val shots = mutableStateListOf<String>()
}

@Composable
fun SessionForm(d: SessionDraft, noteLabel: Int, noteHint: Int) {
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris -> uris.mapNotNull { Shots.save(ctx, it) }.let { d.shots.addAll(it) } }
    var pending by remember { mutableStateOf<java.io.File?>(null) }
    // The phone's own camera, as for covers: a shot of the TV, the table after a game.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pending
        if (ok && f != null && f.length() > 0) Shots.saveFile(ctx, f)?.let { d.shots.add(it) }
        f?.delete(); pending = null
    }
    Text(stringResource(R.string.s_long), style = T.Small.copy(color = C.ink))
    Flow { for (m in MINUTES) Chip(if (m == 180) minutesText(m, ctx.ru) + "+" else minutesText(m, ctx.ru), d.minutes == m, { d.minutes = m }) }
    Text(stringResource(noteLabel), style = T.Small.copy(color = C.ink))
    Field(d.note, { d.note = it }, stringResource(noteHint), single = false)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelButton(stringResource(R.string.s_camera), {
            val f = java.io.File(java.io.File(ctx.cacheDir, "shots").apply { mkdirs() }, java.util.UUID.randomUUID().toString() + ".jpg")
            pending = f
            runCatching { camera.launch(androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", f)) }.onFailure { pending = null }
        }, kind = Kind.PALE, small = true)
        PixelButton(stringResource(R.string.s_shots), { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, kind = Kind.PALE, small = true)
        if (d.shots.isNotEmpty()) Text(stringResource(R.string.s_shots_n, d.shots.size), style = T.Small)
    }
    if (d.shots.isNotEmpty()) ShotRow(d.shots) { }
}

/** A strip of screenshot thumbnails. */
@Composable
fun ShotRow(names: List<String>, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (n in names) {
            val bmp = remember(n) { Shots.bitmap(ctx, n, 240)?.asImageBitmap() } ?: continue
            Image(bmp, null, Modifier.size(96.dp, 64.dp).pixelBox(C.ink, bevel = null, lip = null).padding(2.dp).clickable { onOpen(n) }, contentScale = ContentScale.Crop)
        }
    }
}

/** The game's sessions, newest first. */
@Composable
fun SessionList(list: List<Session>, onOpen: (String) -> Unit, onDelete: (Session) -> Unit) {
    val ctx = LocalContext.current
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", if (ctx.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH) }
    var asking by remember { mutableStateOf<Long?>(null) }
    for (s in list) {
        Column(Modifier.fillMaxWidth().pixelBox(C.paper, outline = C.paperD, bevel = null).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(listOf(fmt.format(Instant.ofEpochMilli(s.at).atZone(ZoneId.systemDefault())), minutesText(s.minutes, ctx.ru)).filter { it.isNotBlank() }.joinToString(" · "), style = T.Small, modifier = Modifier.weight(1f))
                Text(stringResource(if (asking == s.id) R.string.s_delete_sure else R.string.s_delete), style = T.Small.copy(color = if (asking == s.id) C.crim else C.muted),
                    modifier = Modifier.clickable { if (asking == s.id) onDelete(s) else asking = s.id }.padding(4.dp))
            }
            if (s.note.isNotBlank()) Text(s.note, style = T.Body)
            if (s.shotList.isNotEmpty()) ShotRow(s.shotList, onOpen)
        }
    }
}

/** A screenshot over everything; tap or back closes it. */
@Composable
fun ShotViewer(name: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    BackHandler(onBack = onClose)
    val bmp = remember(name) { Shots.bitmap(ctx, name, 2000)?.asImageBitmap() }
    Box(Modifier.fillMaxSize().background(Color(0xF0100806)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose), contentAlignment = Alignment.Center) {
        if (bmp != null) Image(bmp, null, Modifier.fillMaxWidth().padding(8.dp), contentScale = ContentScale.Fit)
    }
}
