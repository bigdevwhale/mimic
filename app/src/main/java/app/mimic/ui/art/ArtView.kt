package app.mimic.ui.art

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Global switches for the art. */
object Art {
    /** Screenshots and renders want a still frame: no loop, a fixed moment. */
    @Volatile var still = false

    /** Art pixels advance about this often. */
    const val FRAME_MS = 90L
}

fun Buf.toBitmap(): Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.setPixels(d, 0, w, 0, 0, w, h) }

/** A frame counter that ticks while on screen (or sits at a fixed frame for renders). */
@Composable
fun rememberTick(): Int {
    var tick by remember { mutableIntStateOf(if (Art.still) 40 else 0) }
    if (!Art.still) {
        LaunchedEffect(Unit) {
            var last = 0L
            while (true) withFrameMillis { ms -> if (ms - last >= Art.FRAME_MS) { last = ms; tick++ } }
        }
    }
    return tick
}

/** Paints [draw] into a w×h buffer every tick and shows it with hard pixels, width filling. */
@Composable
fun PixelCanvas(w: Int, h: Int, modifier: Modifier = Modifier, fitHeight: Boolean = false, draw: (Buf, Int) -> Unit) {
    val frame = remember(w, h) { Buf(w, h) }
    val bitmap = remember(w, h) { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val tick = rememberTick()
    Canvas(modifier.aspectRatio(w.toFloat() / h, matchHeightConstraintsFirst = fitHeight)) {
        draw(frame, tick)
        bitmap.setPixels(frame.d, 0, w, 0, 0, w, h)
        val scale = size.width / w
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(w, h), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), (h * scale).roundToInt()), filterQuality = FilterQuality.None)
    }
}
