package app.mimic.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.ui.art.Buf
import app.mimic.ui.art.toBitmap
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T
import kotlin.math.roundToInt

/** One "UI pixel": outlines, bevels and notched corners come in multiples of it. */
val PX: Dp = 2.dp

/** A pixel box: an ink outline with notched corners, a fill, a light bevel on top and a darker lip below. */
fun Modifier.pixelBox(fill: Color, outline: Color = C.ink, bevel: Color? = Color.White.copy(alpha = .4f), lip: Color? = Color(0x30200C08), pressed: Boolean = false): Modifier = drawBehind {
    val p = PX.toPx()
    val w = size.width
    val h = size.height
    drawRect(outline, Offset(p, 0f), Size(w - 2 * p, h))
    drawRect(outline, Offset(0f, p), Size(w, h - 2 * p))
    drawRect(fill, Offset(2 * p, p), Size(w - 4 * p, h - 2 * p))
    drawRect(fill, Offset(p, 2 * p), Size(w - 2 * p, h - 4 * p))
    if (!pressed) {
        bevel?.let { drawRect(it, Offset(2 * p, p), Size(w - 4 * p, p)) }
        lip?.let { drawRect(it, Offset(2 * p, h - 3 * p), Size(w - 4 * p, 2 * p)) }
    }
}

/** The mimic's speech frame: ink outside, a crimson line inside. */
fun Modifier.sayBox(fill: Color = C.paper): Modifier = drawBehind {
    val p = PX.toPx()
    val w = size.width
    val h = size.height
    drawRect(C.ink, Offset(p, 0f), Size(w - 2 * p, h))
    drawRect(C.ink, Offset(0f, p), Size(w, h - 2 * p))
    drawRect(fill, Offset(p, p), Size(w - 2 * p, h - 2 * p))
    drawRect(C.crim, Offset(2 * p, 2 * p), Size(w - 4 * p, h - 4 * p))
    drawRect(fill, Offset(3 * p, 3 * p), Size(w - 6 * p, h - 6 * p))
}

/** A dark wooden tray for slots and tiles. */
fun Modifier.tray(edge: Color = C.ink): Modifier = drawBehind {
    val p = PX.toPx()
    drawRect(edge)
    drawRect(C.woodD, Offset(p, p), Size(size.width - 2 * p, size.height - 2 * p))
    drawRect(C.woodL, Offset(p, p), Size(size.width - 2 * p, p))
}

@Composable
fun Panel(modifier: Modifier = Modifier, fill: Color = C.paper, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp), content: @Composable ColumnScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier.pixelBox(fill, pressed = pressed && onClick != null)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** The mimic says something. */
@Composable
fun Says(text: String, modifier: Modifier = Modifier, who: String? = null) {
    Column(modifier.fillMaxWidth().sayBox().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(who ?: stringResource(R.string.mimic_caps), style = T.Small.copy(color = C.crim))
        Text(text, style = T.Say)
    }
}

enum class Kind { BRASS, CRIM, PALE, WOOD }

@Composable
fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.BRASS, enabled: Boolean = true, small: Boolean = false) {
    val (fill, ink, lip) = when (kind) {
        Kind.BRASS -> Triple(C.brass, C.ink, C.brassD)
        Kind.CRIM -> Triple(C.crim, C.paper, C.crimD)
        Kind.PALE -> Triple(C.paper, C.ink, C.paperD)
        Kind.WOOD -> Triple(C.woodL, C.paper, C.woodD)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val a = if (enabled) 1f else .4f
    Box(
        modifier
            .offset { IntOffset(0, if (pressed) PX.roundToPx() else 0) }
            .pixelBox(fill.copy(alpha = a), C.ink, Color.White.copy(alpha = .35f * a), lip, pressed)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 36.dp else 46.dp)
            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (small) 6.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = (if (small) T.Small else T.Button).copy(color = ink.copy(alpha = a)), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** A choice chip; [color] paints it as a genre plate when on. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null, dark: Boolean = false) {
    val fill = when { on -> color ?: C.brass; dark -> C.woodL; else -> C.paperD }
    val ink = when { on && color != null -> C.paper; on -> C.ink; dark -> C.paper; else -> C.ink }
    Text(text, style = T.Small.copy(color = ink), maxLines = 1,
        modifier = modifier.pixelBox(fill, bevel = null, lip = null).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp))
}

/** A genre plate, like a type in an old field guide. */
@Composable
fun Plate(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = T.Small.copy(color = C.paper), modifier = modifier.pixelBox(color, bevel = null, lip = null).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** A status pill on tiles. */
@Composable
fun Pill(text: String, fill: Color, ink: Color) {
    Text(text, style = T.Small.copy(color = ink), modifier = Modifier.background(fill).padding(horizontal = 6.dp, vertical = 1.dp), maxLines = 1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) =
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

/** A still pixel picture scaled with hard pixels, [scale] dp per art pixel. */
@Composable
fun BufImage(buf: Buf, modifier: Modifier = Modifier, scale: Dp = 2.dp, smooth: Boolean = false) {
    val image = remember(buf) { buf.toBitmap().asImageBitmap() }
    Canvas(modifier.size(scale * buf.w, scale * buf.h)) {
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(buf.w, buf.h), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = if (smooth) FilterQuality.Medium else FilterQuality.None)
    }
}

/** A plain pixel text field. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, single: Boolean = true, onDone: (() -> Unit)? = null) {
    BasicTextField(value, onChange, modifier.fillMaxWidth().pixelBox(C.paper), textStyle = style, singleLine = single, cursorBrush = SolidColor(C.ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (onDone != null) ImeAction.Search else ImeAction.Default),
        keyboardActions = KeyboardActions(onSearch = { onDone?.invoke() }),
        decorationBox = { inner -> Box(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) { if (value.isEmpty()) Text(hint, style = style.copy(color = C.muted)); inner() } })
}

/**
 * A paper sheet from the bottom over a dim layer: a grab bar and a close cross on top; tap outside,
 * swipe down, or back closes it.
 */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    val closeLabel = stringResource(R.string.close)
    Box(Modifier.fillMaxSize().background(Color(0xB0140A06)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss).imePadding(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(max = 760.dp)
                .drawBehind { drawRect(C.paper); drawRect(C.ink, size = Size(size.width, PX.toPx())); drawRect(C.brass, Offset(0f, PX.toPx()), Size(size.width, 2 * PX.toPx())) }
                .clickable(remember { MutableInteractionSource() }, null) {}
                .pointerInput(Unit) { var dy = 0f; detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 120) onDismiss() }) { _, d -> dy += d } }
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(Modifier.align(Alignment.TopCenter).width(48.dp).height(4.dp).background(C.paperD))
                Box(Modifier.align(Alignment.TopEnd).padding(end = 6.dp).size(40.dp).clickable(onClick = onDismiss).semantics { contentDescription = closeLabel }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(16.dp)) {
                        val p = size.width / 8
                        for (i in 0 until 8) { drawRect(C.muted, Offset(i * p, i * p), Size(p, p)); drawRect(C.muted, Offset((7 - i) * p, i * p), Size(p, p)) }
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

/** A full-screen page over the tabs with a back arrow. */
@Composable
fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(C.wood).clickable(remember { MutableInteractionSource() }, null) {}.imePadding()) {
        Row(Modifier.fillMaxWidth().background(C.woodD).statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(20.dp)) {
                    val p = size.width / 10
                    for (i in 0 until 5) {
                        drawRect(C.brass, Offset((1 + i) * p, (5 - i) * p - p / 2), Size(p * 2, p))
                        drawRect(C.brass, Offset((1 + i) * p, (4 + i) * p + p / 2), Size(p * 2, p))
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(title, style = T.Title.copy(color = C.brass), modifier = Modifier.weight(1f), maxLines = 1)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Gap(h: Dp = 8.dp) = Spacer(Modifier.height(h))

/** A section label on wood. */
@Composable
fun Section(text: String, note: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = T.Heading.copy(color = C.brass))
        if (note != null) Text("  $note", style = T.Small.copy(color = C.paperD))
    }
}

fun Buf.toImage() = toBitmap().asImageBitmap()

private val STAR = listOf("...#...", "..###..", "#######", ".#####.", "..###..", ".##.##.", ".#...#.")

/** Five pixel stars; with [onRate] they're tappable, and tapping the current one clears it. */
@Composable
fun Stars(rating: Int, modifier: Modifier = Modifier, size: Dp = 26.dp, onRate: ((Int) -> Unit)? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(size / 6)) {
        for (i in 1..5) {
            val on = i <= rating
            Canvas(Modifier.size(size).then(if (onRate != null) Modifier.clickable { onRate(if (i == rating) 0 else i) } else Modifier)) {
                val p = this.size.width / 7
                STAR.forEachIndexed { y, row -> row.forEachIndexed { x, c -> if (c == '#') drawRect(if (on) C.brass else C.paperD, Offset(x * p, y * p), Size(p, p)) } }
                if (on) { drawRect(C.brassL, Offset(3 * p, p), Size(p, p)); drawRect(C.brassL, Offset(2 * p, 2 * p), Size(p, p)); drawRect(C.brassD, Offset(4 * p, 4 * p), Size(p, p)) }
            }
        }
    }
}
