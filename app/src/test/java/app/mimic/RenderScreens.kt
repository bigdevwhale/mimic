package app.mimic

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.onAllNodesWithText
import app.mimic.data.DemoSeed
import app.mimic.domain.Phase
import app.mimic.domain.Sky
import app.mimic.ui.MimicRoot
import app.mimic.ui.Overlay
import app.mimic.ui.Tab
import app.mimic.ui.art.Art
import app.mimic.ui.onboarding.Onboarding
import app.mimic.ui.theme.MimicTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo room, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    /** Today in Moscow, just before sunset: the lamp is on and the sky is pink. */
    private val evening: Long by lazy {
        val day = System.currentTimeMillis() / 86_400_000L * 86_400_000L
        (12 * 60 until 20 * 60).map { day + it * 60_000L }.first { Sky.phase(it, 55.75, 37.62) == Phase.EVENING && Sky.sunAlt(it, 55.75, 37.62) < 3 }
    }

    @Before fun still() { Art.still = true }

    private fun seed(): Pair<app.mimic.data.Settings, Map<String, Long>> = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MimicApp>()
        DemoSeed.seedIfEmpty(app, app.container)
        app.container.settings.now() to app.container.db.games().list().associate { it.title to it.id }
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { MimicTheme { content() } }
        // The demo store loads on a background thread; wait for the header before drawing.
        val title = ApplicationProvider.getApplicationContext<MimicApp>().getString(R.string.app_title)
        rule.waitUntil(15_000) { rule.mainClock.advanceTimeBy(100); rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun screen(name: String, tab: Tab, overlay: (Map<String, Long>) -> Overlay? = { null }) {
        val (s, ids) = seed()
        shot(name) { MimicRoot(s, evening, tab, overlay(ids)) }
    }

    private fun Map<String, Long>.like(vararg t: String) = entries.first { e -> t.any { e.key == it } }.value

    @Test fun welcome() = shot("0-welcome") { Onboarding() }
    @Test fun room() = screen("1-room", Tab.ROOM)
    @Test fun card() = screen("2-card", Tab.ROOM) { Overlay.Card(it.like("Ведьмак 3", "The Witcher 3")) }
    @Test fun shelf() = screen("3-shelf", Tab.SHELF)
    @Test fun chronicle() = screen("4-chronicle", Tab.CHRONICLE)
    @Test fun book() = screen("5-bestiary", Tab.BOOK)
    @Test fun sniff() = screen("6-sniff", Tab.ROOM) { Overlay.Sniff }
    @Test fun dig() = screen("7-dig", Tab.ROOM) { Overlay.Dig(null) }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }
