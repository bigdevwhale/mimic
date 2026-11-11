package app.mimic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
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

/**
 * A 90s home console: warm grey plastic, a dark bezel around the screen (the room), and the colours of
 * the pad's face buttons for what you press. Pixels stay in the room and on its creatures only.
 */
@Immutable
object C {
    /** The plastic, a deeper plastic for wells and trays, and a quiet control tone. */
    val bg = Color(0xFFE4E2EA)
    val bgD = Color(0xFFD7D4E0)
    val bgL = Color(0xFFD0CCDB)
    /** The dark bezel around the screen and the bar of tabs; light on it is [onBezel]. */
    val bezel = Color(0xFF2E2A47)
    val onBezel = Color(0xFFE4E2EA)
    val night = Color(0xFF1C1A28)
    /** Cards, a second card tone, and the hairlines and tracks on them. */
    val card = Color(0xFFF7F6FA)
    val card2 = Color(0xFFEFEDF5)
    val line = Color(0xFFCFCBDB)
    /** Text: main, secondary, and quiet labels. */
    val text = Color(0xFF2A2638)
    val sub = Color(0xFF6B6680)
    val dim = Color(0xFF8E89A3)
    /** The console's own violet: the main action and what's chosen. */
    val neon = Color(0xFF5B4BB0)
    val neonL = Color(0xFFA79BE0)
    val neonD = Color(0xFF3F3291)
    /** The red face button: the big moments (credits, the mimic's voice, sharing). */
    val pink = Color(0xFFE0464B)
    val pinkL = Color(0xFFF08A8D)
    val pinkD = Color(0xFFA82A2F)
    /** Lavender for the pile, the green button for finished, the blue one for lent. */
    val pile = Color(0xFF8E74D0)
    val done = Color(0xFF2F9E6A)
    val away = Color(0xFF3C7BD9)
    /** Ink on a lit button. */
    val onNeon = Color.White

    fun genre(g: Genre) = Color(0xFF000000 or g.color)
}

/** Nunito (SIL OFL) in five static weights: variable weights are ignored by some phones' font stacks. */
val Read = FontFamily(
    Font(R.font.nunito_medium, FontWeight.Medium), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold), Font(R.font.nunito_black, FontWeight.Black),
)

object T {
    private val base = TextStyle(fontFamily = Read, fontWeight = FontWeight.Medium, color = C.text)
    val Display = base.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    val Title = base.copy(fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.ExtraBold)
    val Heading = base.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
    val Body = base.copy(fontSize = 15.sp, lineHeight = 21.sp)
    val Strong = Body.copy(fontWeight = FontWeight.Bold)
    /** The mimic's voice. */
    val Say = base.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
    val Small = base.copy(fontSize = 13.sp, lineHeight = 18.sp, color = C.sub)
    /** Labels on chips, plates, counters and tabs. */
    val Tag = base.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = C.sub)
    val Button = base.copy(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
    val Number = base.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
}

@Composable
fun MimicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.neon, onPrimary = C.onNeon, background = C.bg, surface = C.card, onSurface = C.text,
            onBackground = C.text, surfaceContainer = C.card, outline = C.line, error = C.pink,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
