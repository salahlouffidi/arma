package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.game.AppleItem
import com.example.game.ArmadilloState
import com.example.game.FruitType
import com.example.game.Obstacle
import com.example.game.ObstacleType
import com.example.game.Particle
import com.example.game.SnakeState
import com.example.game.SoundManager
import com.example.game.StageProgressManager
import com.example.game.StageTheme
import com.example.game.TableRenderer
import com.example.game.Vector2
import com.example.ui.MainMenuScreen
import com.example.ui.StageSelectScreen
import com.example.ui.theme.ArmadilloEscapeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every stage theme with a representative set of entities so the
 * look of the board, armadillos, snakes, fruit and effects can be reviewed
 * from CI screenshots (see .github/workflows/screenshots.yml).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = VisualGalleryScreenshotTest.PHONE_LAND, sdk = [36])
class VisualGalleryScreenshotTest {

    companion object {
        const val PHONE_LAND = "w915dp-h412dp-normal-long-notround-land-420dpi-keyshidden-nonav"
        const val TABLET_LAND = "w1280dp-h800dp-xlarge-long-notround-land-240dpi-keyshidden-nonav"
    }

    @get:Rule val composeTestRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { ArmadilloEscapeTheme { content() } }
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/gallery/$name.png")
    }

    @Test fun board_garden() = capture("board_1_garden") { Board(StageTheme.GARDEN) }
    @Test fun board_desert() = capture("board_2_desert") { Board(StageTheme.DESERT) }
    @Test fun board_jungle() = capture("board_3_jungle") { Board(StageTheme.JUNGLE) }
    @Test fun board_mountains() = capture("board_4_mountains") { Board(StageTheme.MOUNTAINS) }
    @Test fun board_volcano() = capture("board_5_volcano") { Board(StageTheme.VOLCANO) }
    @Test fun board_ice() = capture("board_6_ice") { Board(StageTheme.ICE) }
    @Test fun board_temple() = capture("board_7_temple") { Board(StageTheme.DARK_TEMPLE) }
    @Test fun board_final() = capture("board_8_final") { Board(StageTheme.FINAL_ARENA, dual = true) }

    @Config(qualifiers = TABLET_LAND)
    @Test fun board_tablet() = capture("board_9_tablet_jungle") { Board(StageTheme.JUNGLE, dual = true) }

    @Test fun menu_main() = capture("menu_main") {
        val ctx = LocalContext.current
        MainMenuScreen(
            currentStageId = 1,
            soundManager = SoundManager(ctx),
            onStartGame = {},
            onOpenStageSelect = {},
            onOpenAudioSettings = {}
        )
    }

    @Test fun menu_stage_select() = capture("menu_stage_select") {
        val ctx = LocalContext.current
        StageSelectScreen(
            currentStageId = 1,
            progressManager = StageProgressManager(ctx),
            soundManager = SoundManager(ctx),
            onSelectStage = {},
            onBackToMenu = {}
        )
    }

    @Composable
    private fun Board(theme: StageTheme, dual: Boolean = false) {
        Box(Modifier.fillMaxSize().background(Color(0xFF140D08))) {
            Canvas(Modifier.fillMaxSize()) {
                val marginX = (size.width * 0.05f).coerceAtLeast(18f)
                val top = 54f * density + 24f * density
                val bottom = 72f * density
                val halfW = (size.width - marginX * 2f) / 2f
                val halfH = (size.height - top - bottom) / 2f
                val c = Offset(size.width / 2f, top + halfH)

                TableRenderer.drawTable(this, c, halfW, halfH, theme)

                val obsType = when (theme) {
                    StageTheme.VOLCANO -> ObstacleType.LAVA_HAZARD
                    StageTheme.DARK_TEMPLE -> ObstacleType.TEMPLE_PILLAR
                    StageTheme.MOUNTAINS -> ObstacleType.MOVING_BOULDER
                    else -> ObstacleType.ROCK
                }
                TableRenderer.drawObstacles(
                    this,
                    listOf(
                        Obstacle(1, Vector2(-halfW * 0.45f, -halfH * 0.38f), 26f, obsType),
                        Obstacle(2, Vector2(halfW * 0.45f, halfH * 0.38f), 26f, obsType),
                        Obstacle(3, Vector2(halfW * 0.5f, -halfH * 0.05f), 22f, ObstacleType.ROCK)
                    ),
                    c
                )

                TableRenderer.drawApples(
                    this,
                    listOf(
                        AppleItem(1, Vector2(-halfW * 0.15f, -halfH * 0.55f), bobPhase = 0.5f),
                        AppleItem(2, Vector2(halfW * 0.25f, -halfH * 0.62f), fruitType = FruitType.ESCAPE_GOLDEN, sparkleTimer = 0.3f),
                        AppleItem(3, Vector2(-halfW * 0.6f, halfH * 0.1f), fruitType = FruitType.ESCAPE_STAR_POWER, sparkleTimer = 0.2f),
                        AppleItem(4, Vector2(halfW * 0.1f, halfH * 0.65f), dropHeight = 40f)
                    ),
                    c
                )

                TableRenderer.drawArmadillo(
                    this,
                    ArmadilloState(pos = Vector2(-halfW * 0.1f, halfH * 0.05f), headingAngle = -0.4f, legStepPhase = 1f, invulnerableTimer = 0f),
                    c
                )
                if (dual) {
                    TableRenderer.drawArmadillo(
                        this,
                        ArmadilloState(
                            pos = Vector2(halfW * 0.2f, halfH * 0.3f), headingAngle = 2.6f,
                            invulnerableTimer = 0f, isPowerActive = true, powerChancesRemaining = 2, auraPhase = 1.2f
                        ),
                        c,
                        isBlue = true
                    )
                }

                val snakes = buildList {
                    add(sampleSnake(Vector2(halfW * 0.35f, halfH * 0.05f), 2.8f, 0f))
                    if (dual || theme == StageTheme.VOLCANO) add(sampleSnake(Vector2(-halfW * 0.4f, halfH * 0.55f), 0.2f, 1.5f))
                }
                TableRenderer.drawSnakes(this, snakes, c)

                val parts = (0 until 14).map { i ->
                    val a = i / 14f * 6.283f
                    Particle(
                        pos = Vector2(-halfW * 0.6f + cos(a) * 34f, halfH * 0.1f + sin(a) * 34f),
                        vel = Vector2.Zero,
                        color = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFFD54F),
                        size = 5f, alpha = 0.4f + (i % 3) * 0.2f, life = 0.5f, maxLife = 1f
                    )
                }
                TableRenderer.drawParticles(this, parts, c)
            }
        }
    }

    private fun sampleSnake(head: Vector2, angle: Float, phase: Float): SnakeState {
        // Body trails behind the head along a gentle curve
        val segs = (0 until 16).map { i ->
            val d = (i + 1) * 18f
            val bend = sin(i * 0.35f + phase) * 22f
            val back = angle + Math.PI.toFloat()
            Vector2(
                head.x + cos(back) * d + cos(back + 1.57f) * bend,
                head.y + sin(back) * d + sin(back + 1.57f) * bend
            )
        }
        return SnakeState(headPos = head, headAngle = angle, bodySegments = segs, slitherPhase = phase, tongueVisible = true)
    }
}
