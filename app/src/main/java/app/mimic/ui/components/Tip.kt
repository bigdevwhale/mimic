package app.mimic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.data.Settings
import app.mimic.container
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T

/** First-visit hints, one per screen: the mimic explains the place once, and it's gone for good after «Got it». */
object Tips {
    const val ROOM = "room"
    const val SHELF = "shelf"
    const val CHRONICLE = "chronicle"
    const val BOOK = "book"
    const val CARD = "card"
    const val POSTCARD = "postcard"
    const val SETTINGS = "settings"
    val ALL = listOf(ROOM, SHELF, CHRONICLE, BOOK, CARD, POSTCARD, SETTINGS)

    fun key(tip: String) = "tip:$tip"
    fun shown(s: Settings, tip: String) = key(tip) in s.seen
}

/** The mimic's hint for this screen, in its speech frame, until it's dismissed. */
@Composable
fun Tip(tip: String, s: Settings, text: String, modifier: Modifier = Modifier) {
    val c = LocalContext.current.container
    var gone by remember(tip) { mutableStateOf(false) }
    if (gone || Tips.shown(s, tip)) return
    Column(modifier.fillMaxWidth().sayBox().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.mimic_caps), style = T.Tag.copy(color = C.pink))
        Text(text, style = T.Say)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            PixelButton(stringResource(R.string.tip_ok), { gone = true; c.write { settings.meet(Tips.key(tip)) } }, kind = Kind.BRASS, small = true)
        }
    }
}
