package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ArmadilloState
import com.example.game.SnakeState
import com.example.game.SoundManager
import com.example.game.StageConfig
import com.example.game.TableRenderer
import com.example.game.Vector2
import com.example.game.WorldRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cinematic home screen: the currently selected world fills the screen, a golden armadillo
 * strolls in the foreground while a snake slithers in the distance, and the camera drifts slowly.
 * One dominant action (PLAY) plus three secondary actions with console-style focus.
 */
@Composable
fun ConsoleHomeScreen(
    currentStageId: Int,
    highestUnlocked: Int,
    soundManager: SoundManager,
    onPlay: () -> Unit,
    onRounds: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    onMultiplayer: () -> Unit = {}
) {
    val stage = StageConfig.getStage(currentStageId)
    val accent = WorldRenderer.accent(stage.theme)
    val time by rememberWorldTime()
    val breath by rememberBreath(4200)
    val scope = rememberCoroutineScope()
    var focused by remember { mutableIntStateOf(0) }

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    fun activate(index: Int, action: () -> Unit) {
        if (focused != index) soundManager.playMenuClick()
        focused = index
        scope.launch {
            delay(140) // let the focus animation land before acting (console feel)
            soundManager.playStageSelected()
            action()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("console_home")) {
        // 1. Living world (slow camera drift)
        WorldBackdrop(
            theme = stage.theme,
            modifier = Modifier.fillMaxSize(),
            parallax = { sin(time * 0.08f) * 0.6f },
            detail = 2
        )

        // 2. Hero characters (drawn with the real gameplay renderers for one visual universe)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = time
            val w = size.width
            val h = size.height

            // Snake in the distance, slithering across the right side
            val sx = w * 0.76f + sin(t * 0.3f) * w * 0.07f
            val sy = h * 0.70f + cos(t * 0.45f) * h * 0.015f
            val sDir = cos(t * 0.3f)
            val sAngle = if (sDir >= 0f) 0f else Math.PI.toFloat()
            val back = if (sDir >= 0f) -1f else 1f
            val segs = List(14) { i ->
                Vector2(sx + back * (i + 1) * 15f, sy + sin(t * 2.2f - i * 0.55f) * 5f)
            }
            val snake = SnakeState(
                headPos = Vector2(sx, sy),
                headAngle = sAngle,
                bodySegments = segs,
                slitherPhase = t * 7.5f,
                tongueVisible = (t % 2.5f) < 0.45f
            )
            withTransform({ scale(0.8f, 0.8f, Offset(sx, sy)) }) {
                TableRenderer.drawSnake(this, snake, Offset.Zero)
            }

            // Golden armadillo strolling along a gentle loop in the foreground
            val r1 = w * 0.08f
            val r2 = h * 0.025f
            val a = t * 0.32f
            val ax = w * 0.3f + cos(a) * r1
            val ay = h * 0.8f + sin(a) * r2
            val dx = -sin(a) * r1
            val dy = cos(a) * r2
            val arm = ArmadilloState(
                pos = Vector2(ax, ay),
                vel = Vector2(dx * 0.2f, dy * 0.2f),
                radius = 28f,
                headingAngle = atan2(dy, dx),
                legStepPhase = t * 9f,
                tailWagPhase = t * 5f,
                auraPhase = t * 2f,
                invulnerableTimer = 0f
            )
            withTransform({ scale(2.1f, 2.1f, Offset(ax, ay)) }) {
                TableRenderer.drawArmadillo(this, arm, Offset.Zero)
            }
        }

        // 3. Title block (left)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .statusBarsPadding()
                .padding(start = 44.dp, bottom = 90.dp)
                .graphicsLayer {
                    alpha = enter.value
                    translationY = (1f - enter.value) * 28.dp.toPx() + breath * 3.dp.toPx()
                    val s = 0.96f + 0.04f * enter.value
                    scaleX = s
                    scaleY = s
                }
        ) {
            SectionLabel(text = "LOUFFI EDITION", color = accent.copy(alpha = 0.9f))
            Text(
                text = "ARMASNAKE",
                style = TextStyle(
                    brush = ArmaUi.TitleBrush,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    shadow = Shadow(color = Color(0xAA000000), offset = Offset(0f, 6f), blurRadius = 18f)
                )
            )
            Text(
                text = "Roll. Dodge. Feast.",
                color = ArmaUi.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }

        // 4. Console menu (right) — one dominant action, three secondary
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(end = 48.dp)
                .widthIn(max = 320.dp)
                .graphicsLayer {
                    alpha = enter.value
                    translationX = (1f - enter.value) * 48.dp.toPx()
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            ConsoleMenuItem(
                label = "PLAY",
                sublabel = "ROUND ${stage.id.toString().padStart(2, '0')} · ${stage.name.uppercase()}",
                focused = focused == 0,
                primary = true,
                accent = ArmaUi.Gold,
                modifier = Modifier.testTag("home_play"),
                onClick = { activate(0, onPlay) }
            )
            ConsoleMenuItem(
                label = "ROUNDS",
                sublabel = "$highestUnlocked / 16 WORLDS UNLOCKED",
                focused = focused == 1,
                accent = accent,
                modifier = Modifier.testTag("home_rounds"),
                onClick = { activate(1, onRounds) }
            )
            ConsoleMenuItem(
                label = "HOW TO PLAY",
                focused = focused == 2,
                accent = accent,
                modifier = Modifier.testTag("home_how_to_play"),
                onClick = { activate(2, onHowToPlay) }
            )
            ConsoleMenuItem(
                label = "SETTINGS",
                focused = focused == 3,
                accent = accent,
                modifier = Modifier.testTag("home_settings"),
                onClick = { activate(3, onSettings) }
            )
            ConsoleMenuItem(
                label = "VERSUS",
                sublabel = "LOCAL MULTIPLAYER",
                focused = focused == 4,
                accent = ArmaUi.Cyan,
                modifier = Modifier.testTag("home_multiplayer"),
                onClick = { activate(4, onMultiplayer) }
            )
        }

        // 5. Footer hint
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .graphicsLayer { alpha = enter.value * 0.7f }
        ) {
            SectionLabel(text = "${stage.icon}  ${stage.subtitle.uppercase()}")
        }
    }
}
