package app.mimic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.mimic.R
import app.mimic.data.Genre

/** The room's own materials: dark wood, cream paper, brass plates and the crimson of the mimic's tongue. */
@Immutable
object C {
    val wood = Color(0xFF4A2C1A)
    val woodD = Color(0xFF2C180E)
    val woodL = Color(0xFF6A4228)
    val night = Color(0xFF140C08)
    val ink = Color(0xFF2A160E)
    val paper = Color(0xFFF6EAD0)
    val paperD = Color(0xFFE2CFA6)
    val note = Color(0xFFFFF8DC)
    val muted = Color(0xFF80624A)
    val brass = Color(0xFFE8B840)
    val brassL = Color(0xFFFFF0A0)
    val brassD = Color(0xFFA87418)
    val crim = Color(0xFFC42C58)
    val crimD = Color(0xFF7E1838)
    val crimL = Color(0xFFF0708C)
    val pile = Color(0xFF6A4A8A)
    val done = Color(0xFF3A7A4A)
    val away = Color(0xFF3A64B8)

    fun genre(g: Genre) = Color(0xFF000000 or g.color)
}

/** m6x11 by Daniel Linssen with Cyrillic by Natto Games (free with attribution): one family, sizes set the hierarchy. */
val Pixel = FontFamily(Font(R.font.m6x11, FontWeight.Normal))

object T {
    val Display = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 31.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Pixel, fontSize = 27.sp, lineHeight = 27.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Pixel, fontSize = 23.sp, lineHeight = 24.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Pixel, fontSize = 20.sp, lineHeight = 21.sp, color = C.ink)
    val Say = TextStyle(fontFamily = Pixel, fontSize = 18.sp, lineHeight = 20.sp, color = C.ink)
    val Small = TextStyle(fontFamily = Pixel, fontSize = 16.sp, lineHeight = 17.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Pixel, fontSize = 20.sp, lineHeight = 20.sp)
    val Number = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 30.sp, color = C.ink)
}

@Composable
fun MimicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.brass, onPrimary = C.ink, background = C.wood, surface = C.paper, onSurface = C.ink,
            onBackground = C.paper, surfaceContainer = C.paper, outline = C.ink, error = C.crim,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
