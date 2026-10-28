package app.mimic.ui.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import app.mimic.R
import app.mimic.data.Covers
import app.mimic.ui.components.Kind
import app.mimic.ui.components.PixelButton
import java.io.File
import java.util.UUID

/**
 * Two ways to give a game its cover: the phone's own camera (never one built into the app) or a
 * picture already on the phone. [onCover] gets the saved cover's file name.
 */
class CoverPicker(val shoot: () -> Unit, val pick: () -> Unit)

@Composable
fun rememberCoverPicker(onCover: (String) -> Unit): CoverPicker {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<Pair<File, Uri>?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pending
        if (ok && p != null && p.first.length() > 0) Covers.save(ctx, p.first.readBytes())?.let(onCover)
        p?.first?.delete()
        pending = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) Covers.saveUri(ctx, uri)?.let(onCover) }
    return remember(camera, gallery) {
        CoverPicker(
            shoot = {
                val dir = File(ctx.cacheDir, "shots").apply { mkdirs() }
                val f = File(dir, UUID.randomUUID().toString() + ".jpg")
                val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", f)
                pending = f to uri
                runCatching { camera.launch(uri) }.onFailure { pending = null }
            },
            pick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}

@Composable
fun CoverButtons(p: CoverPicker, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        PixelButton(stringResource(R.string.cover_shoot), p.shoot, kind = Kind.PALE, small = true)
        PixelButton(stringResource(R.string.cover_pick), p.pick, kind = Kind.PALE, small = true)
    }
}

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())
