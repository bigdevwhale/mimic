package app.mimic.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.mimic.R
import app.mimic.data.Kind
import app.mimic.data.Status
import app.mimic.data.dusty
import app.mimic.ui.Home
import app.mimic.ui.art.Buf
import app.mimic.ui.art.Critters
import app.mimic.ui.art.MimicArt
import app.mimic.ui.art.MimicPose
import app.mimic.ui.art.rememberTick
import app.mimic.ui.components.BufImage
import app.mimic.ui.components.pixelBox
import app.mimic.ui.theme.C
import app.mimic.ui.theme.T

private class Entry(val kind: Critters.Kind?, val name: Int, val note: Int, val hint: Int, val known: Boolean)

/** The bestiary: who lives in the room, met ones drawn, the rest as silhouettes with a hint. */
@Composable
fun BookScreen(h: Home) {
    val tick = rememberTick()
    val f = tick / 5 % 2 == 1
    val seen = h.s.seen
    val entries = listOf(
        Entry(Critters.Kind.DUST, R.string.b_dust, R.string.b_dust_n, R.string.b_dust_h, "dust" in seen || h.games.any { it.dusty(h.now) }),
        Entry(Critters.Kind.WISP, R.string.b_wisp, R.string.b_wisp_n, R.string.b_wisp_h, "wisp" in seen || h.hands.isNotEmpty()),
        Entry(Critters.Kind.MEEPLE, R.string.b_meeple, R.string.b_meeple_n, R.string.b_meeple_h, h.games.any { it.kind != Kind.VIDEO && it.status != Status.PILE }),
        Entry(Critters.Kind.GHOST, R.string.b_ghost, R.string.b_ghost_n, R.string.b_ghost_h, "ghost" in seen || h.games.any { it.status == Status.LET }),
        Entry(Critters.Kind.DICE, R.string.b_dice, R.string.b_dice_n, R.string.b_dice_h, "dice" in seen),
        Entry(Critters.Kind.CROW, R.string.b_crow, R.string.b_crow_n, R.string.b_crow_h, "crow" in seen || h.away.isNotEmpty()),
        Entry(Critters.Kind.MERCHANT, R.string.b_merchant, R.string.b_merchant_n, R.string.b_merchant_h, false),
        Entry(Critters.Kind.GOBLIN, R.string.b_goblin, R.string.b_goblin_n, R.string.b_goblin_h, false),
        Entry(Critters.Kind.ELDER, R.string.b_elder, R.string.b_elder_n, R.string.b_elder_h, false),
        Entry(Critters.Kind.SLIME, R.string.b_slime, R.string.b_slime_n, R.string.b_slime_h, false),
        Entry(Critters.Kind.MOTH, R.string.b_moth, R.string.b_moth_n, R.string.b_moth_h, false),
        Entry(Critters.Kind.DOUBLE, R.string.b_double, R.string.b_double_n, R.string.b_double_h, false),
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp).fillMaxWidth().pixelBox(C.paper).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.tab_book), style = T.Display.copy(color = C.crim), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(stringResource(R.string.b_met, entries.count { it.known } + 1, entries.size + 1), style = T.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Column(Modifier.fillMaxWidth().pixelBox(C.note, outline = C.paperD, bevel = null).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BufImage(remember(h.mimic.stage, h.mimic.traits, f) { Buf(MimicArt.W, MimicArt.H).also { MimicArt.draw(MimicPose(h.mimic.stage, h.mimic.traits, gap = if (f) 7 else 8, wob = f), it) } }, scale = 2.dp)
            Text(stringResource(R.string.b_mimic), style = T.Heading)
            Text(stringResource(R.string.b_mimic_n, h.mimic.eaten), style = T.Small.copy(color = C.ink), textAlign = TextAlign.Center)
        }
        for (pair in entries.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (e in pair) Column(Modifier.weight(1f).pixelBox(C.note, outline = C.paperD, bevel = null).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BufImage(remember(e.kind, e.known, f) { Critters.portrait(e.kind!!, f, e.known) }, scale = 3.dp)
                Text(if (e.known) stringResource(e.name) else "???", style = T.Heading.copy(color = if (e.known) C.ink else C.muted))
                Text(stringResource(if (e.known) e.note else e.hint), style = T.Small)
            }
            if (pair.size == 1) Column(Modifier.weight(1f)) {}
        }
    }
}
