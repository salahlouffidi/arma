package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.game.GameEngine
import com.example.game.StageConfig
import com.example.game.TableRenderer
import com.example.ui.ArmadilloEscapeScreen
import com.example.ui.theme.ArmadilloEscapeTheme
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Long-session soak tests: play every round for minutes of game time with random input and
 * render every frame, to catch crashes that only show up after a while (v2.1 relaunch report).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w915dp-h412dp-normal-long-notround-land-420dpi-keyshidden-nonav", sdk = [36])
class SoakStressTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun everyRoundSurvivesLongPlayWithRenderingEveryFrame() {
        val rnd = Random(42)
        val bmp = ImageBitmap(1600, 720)
        val canvas = Canvas(bmp)
        val draw = CanvasDrawScope()
        for (id in 1..StageConfig.MAX_ROUND) {
            val e = GameEngine()
            e.setBoardDimensions(720f, 300f)
            e.loadStage(StageConfig.getStage(id))
            var ix = 0f; var iy = 0f; var ix2 = 0f; var iy2 = 0f
            val center = Offset(800f, 360f)
            for (step in 0 until 6000) { // ~100 s of game time per round
                if (step % 30 == 0) {
                    ix = rnd.nextFloat() * 2f - 1f; iy = rnd.nextFloat() * 2f - 1f
                    ix2 = rnd.nextFloat() * 2f - 1f; iy2 = rnd.nextFloat() * 2f - 1f
                }
                try {
                    e.update(0.016f, ix, iy, ix2, iy2)
                    if (e.isGameOver || e.isStageCompleted) e.resetRound()
                    if (step % 12 == 0) {
                        draw.draw(Density(2.6f), LayoutDirection.Ltr, canvas, Size(1600f, 720f)) {
                            val shake = TableRenderer.updateFx(
                                buildList { add(e.armadillo to false); e.armadillo2?.let { add(it to true) } },
                                e.apples
                            )
                            TableRenderer.drawTable(this, center + shake, 720f, 300f, e.currentStage.theme)
                            TableRenderer.drawObstacles(this, e.obstacles, center)
                            TableRenderer.drawTrails(this, center)
                            TableRenderer.drawApples(this, e.apples, center)
                            TableRenderer.drawPowerUps(this, e.powerUps, center)
                            if (e.magnetTimer > 0f) TableRenderer.drawMagnetAura(this, e.armadillo, center)
                            TableRenderer.drawArmadillo(this, e.armadillo, center)
                            e.armadillo2?.let { TableRenderer.drawArmadillo(this, it, center, isBlue = true) }
                            TableRenderer.drawSnakes(this, e.snakes, center, frozen = e.freezeTimer > 0f)
                            TableRenderer.drawParticles(this, e.particles, center)
                            TableRenderer.drawFxOverlay(this, center)
                        }
                    }
                } catch (t: Throwable) {
                    throw AssertionError("Crash in round $id at step $step: $t", t)
                }
            }
        }
    }

    /** Real app flow: intro -> menu -> round 1, then 3 minutes of real frames (game over dialogs etc.). */
    @Test
    fun appSurvivesMinutesOfGameplay() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { ArmadilloEscapeTheme { ArmadilloEscapeScreen() } }
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.onNodeWithTag("skip_intro_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onNodeWithTag("menu_play_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onNodeWithTag("round_card_1").performClick()
        repeat(180) { composeTestRule.mainClock.advanceTimeBy(1000) }
    }
}
