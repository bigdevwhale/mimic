package app.mimic.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.vector.ImageVector
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

/** Corner radius of cards; controls use [SMALL_R]. */
val CARD_R: Dp = 16.dp
val SMALL_R: Dp = 12.dp

/**
 * A dark glass surface: rounded, filled, with a hairline edge. [outline] tints the edge (a neon for
 * emphasis); [pressed] dims it. [bevel] and [lip] are accepted for old callers and ignored.
 */
fun Modifier.surface(fill: Color, outline: Color = C.line, bevel: Color? = null, lip: Color? = null, pressed: Boolean = false, radius: Dp = CARD_R): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(if (pressed) fill.copy(alpha = fill.alpha * .8f) else fill, shape).border(1.dp, outline.copy(alpha = .9f), shape)
}

/** The mimic's speech: a card with the red button's edge. */
fun Modifier.sayBox(fill: Color = C.card): Modifier = this.surface(fill, C.pink)

/** A light tray for slots and tiles. */
fun Modifier.tray(edge: Color = C.line): Modifier = surface(C.card, edge, radius = SMALL_R)

/** The console's dark bezel around a screen. */
fun Modifier.bezel(): Modifier = this
    .drawBehind { drawRoundRect(Color.Black.copy(alpha = .12f), topLeft = Offset(0f, 4f), size = size, cornerRadius = CornerRadius(20.dp.toPx())) }
    .surface(C.bezel, C.bezel, radius = 20.dp)

@Composable
fun Panel(modifier: Modifier = Modifier, fill: Color = C.card, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp), content: @Composable ColumnScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier.surface(fill, pressed = pressed && onClick != null)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** The mimic says something. */
@Composable
fun Says(text: String, modifier: Modifier = Modifier, who: String? = null) {
    Column(modifier.fillMaxWidth().sayBox().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(who ?: stringResource(R.string.mimic_caps), style = T.Tag.copy(color = C.pink))
        Text(text, style = T.Say)
    }
}

/** Button looks: the violet main action, the red big moment, a quiet outlined one, and a plain plastic one. */
enum class Kind { BRASS, CRIM, PALE, WOOD }

@Composable
fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.BRASS, enabled: Boolean = true, small: Boolean = false) {
    // Each button sits on its own darker rim, like a pad's key; pressing pushes it down onto it.
    val (fill, ink, rim) = when (kind) {
        Kind.BRASS -> Triple(C.neon, C.onNeon, C.neonD)
        Kind.CRIM -> Triple(C.pink, Color.White, C.pinkD)
        Kind.PALE -> Triple(C.card, C.text, C.line)
        Kind.WOOD -> Triple(C.bgL, C.text, Color(0xFFB9B4C8))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val a = if (enabled) 1f else .4f
    val shape = RoundedCornerShape(SMALL_R)
    val depth = if (small) 2.dp else 3.dp
    Box(
        modifier
            .padding(bottom = depth)
            .drawBehind { drawRoundRect(rim.copy(alpha = a), topLeft = Offset(0f, depth.toPx()), size = size, cornerRadius = CornerRadius(SMALL_R.toPx())) }
            .offset { IntOffset(0, if (pressed) depth.roundToPx() else 0) }
            .clip(shape)
            .background(fill.copy(alpha = a), shape)
            .border(1.dp, rim.copy(alpha = .35f * a), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 34.dp else 46.dp)
            .padding(horizontal = if (small) 12.dp else 16.dp, vertical = if (small) 6.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = (if (small) T.Tag else T.Button).copy(color = ink.copy(alpha = a)), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** A choice chip; [color] paints it as a genre plate when on. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null, dark: Boolean = false) {
    val fill = when { on -> color ?: C.neon; dark -> C.bgL; else -> C.card2 }
    val ink = when { on && color != null -> Color.White; on -> C.onNeon; else -> C.text }
    val shape = RoundedCornerShape(50)
    Text(text, style = T.Tag.copy(color = ink), maxLines = 1,
        modifier = modifier.clip(shape).background(fill, shape).border(1.dp, if (on) fill else C.line, shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

/** A genre plate: a small rounded tag in the genre's colour. */
@Composable
fun Plate(text: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Text(text, style = T.Tag.copy(color = Color.White), modifier = modifier.clip(shape).background(color, shape).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** A status pill on tiles. */
@Composable
fun Pill(text: String, fill: Color, ink: Color) {
    val shape = RoundedCornerShape(6.dp)
    Text(text, style = T.Tag.copy(color = ink), modifier = Modifier.clip(shape).background(fill, shape).padding(horizontal = 7.dp, vertical = 2.dp), maxLines = 1)
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

/** A text field: a dark rounded well that lights its edge cyan while you type. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, single: Boolean = true, onDone: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(value, onChange, modifier.fillMaxWidth().surface(C.bgD, if (focused) C.neon else C.line, radius = SMALL_R), textStyle = style, singleLine = single, cursorBrush = SolidColor(C.neon),
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (onDone != null) ImeAction.Search else ImeAction.Default),
        keyboardActions = KeyboardActions(onSearch = { onDone?.invoke() }),
        decorationBox = { inner -> Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { if (value.isEmpty()) Text(hint, style = style.copy(color = C.dim)); inner() } })
}

/** A small round icon button. */
@Composable
fun IconBtn(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.sub) {
    Box(modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/**
 * A sheet from the bottom over a dim layer: rounded top, a grab bar and a close cross; tap outside,
 * swipe down, or back closes it.
 */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Box(Modifier.fillMaxSize().background(Color(0xC007060F)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss).imePadding(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(max = 760.dp)
                .clip(shape).background(C.card, shape).border(1.dp, C.line, shape)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .pointerInput(Unit) { var dy = 0f; detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 120) onDismiss() }) { _, d -> dy += d } }
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(Modifier.align(Alignment.TopCenter).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(C.line))
                IconBtn(Icons.Rounded.Close, stringResource(R.string.close), onDismiss, Modifier.align(Alignment.TopEnd).padding(end = 6.dp))
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

/** A full-screen page over the tabs with a back arrow. */
@Composable
fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(C.bg).clickable(remember { MutableInteractionSource() }, null) {}.imePadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack, tint = C.text)
            Spacer(Modifier.width(4.dp))
            Text(title, style = T.Title, modifier = Modifier.weight(1f), maxLines = 1)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Gap(h: Dp = 8.dp) = Spacer(Modifier.height(h))

/** A section label on the hall: bold, with a short neon dash before it. */
@Composable
fun Section(text: String, note: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 14.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(C.neon))
        Spacer(Modifier.width(8.dp))
        Text(text, style = T.Heading)
        if (note != null) Text("  $note", style = T.Small.copy(color = C.dim))
    }
}

fun Buf.toImage() = toBitmap().asImageBitmap()

/** Five stars; with [onRate] they're tappable, and tapping the current one clears it. */
@Composable
fun Stars(rating: Int, modifier: Modifier = Modifier, size: Dp = 26.dp, onRate: ((Int) -> Unit)? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(size / 8)) {
        for (i in 1..5) {
            val on = i <= rating
            Icon(if (on) Icons.Rounded.Star else Icons.Rounded.StarOutline, null, tint = if (on) C.neon else C.line,
                modifier = Modifier.size(size).then(if (onRate != null) Modifier.clickable { onRate(if (i == rating) 0 else i) } else Modifier))
        }
    }
}
