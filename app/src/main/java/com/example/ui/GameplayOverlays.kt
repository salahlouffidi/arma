package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.FruitType
import com.example.game.GameEngine
import com.example.game.StageConfig
import com.example.game.WorldRenderer
import com.example.game.introTagline
import com.example.game.powerIcon
import com.example.game.powerLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/** Result of a cleared round (drives the cinematic Round Complete screen). */
data class StageClearData(
    val stage: StageConfig,
    val score: Int,
    val fruits: Int,
    val lost: Int,
    val timeSeconds: Float,
    val isNewRecord: Boolean,
    val bestScore: Int = score,
    val bestCombo: Int = 1,
    val newlyUnlocked: StageConfig? = null
)

/** Result of a failed round (drives the cinematic Game Over screen). */
data class GameOverData(
    val stage: StageConfig,
    val score: Int,
    val fruits: Int,
    val lost: Int,
    val maxAllowedLosses: Int,
    val timeSeconds: Float,
    val bestScore: Int = 0
)

private fun fmt(n: Int): String = "%,d".format(n)

// =====================================================================================
// In-game HUD (minimal console overlay)
// =====================================================================================

/**
 * Minimal console HUD. Top-left: round + fruit objective (fades back after the opening and
 * briefly returns whenever progress changes). Top-right: animated score + pause/settings.
 * [frame] is read here so only this overlay recomposes per frame.
 */
@Composable
fun BoxScope.ConsoleHud(
    engine: GameEngine,
    frame: () -> Long,
    onPause: () -> Unit,
    onSettings: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val f = frame()
    val stage = engine.currentStage
    val accent = WorldRenderer.accent(stage.theme)
    val fruits = engine.stageFruitsCollected
    val emphasis = remember { Animatable(1f) }
    LaunchedEffect(fruits, stage.id) {
        emphasis.animateTo(1f, tween(160))
        delay(if (fruits == 0) 4200L else 2200L)
        emphasis.animateTo(0.5f, tween(900))
    }
    val shownScore by animateIntAsState(targetValue = engine.stats.score, animationSpec = tween(420), label = "hudScore")

    // Top-left objective
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(start = 14.dp, top = 8.dp)
            .graphicsLayer { alpha = emphasis.value }
            .glassPanel(radius = 14.dp)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = "ROUND ${stage.id.toString().padStart(2, '0')}",
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "$fruits / ${stage.targetFruits} FRUIT",
            color = ArmaUi.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        VSpace(4.dp)
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((fruits.toFloat() / stage.targetFruits.toFloat()).coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(accent)
            )
        }
        VSpace(4.dp)
        val lost = engine.stats.armadillosLost
        val lives = (stage.maxAllowedLosses - lost).coerceAtLeast(0)
        Text(
            text = "♥".repeat(lives) + "♡".repeat((stage.maxAllowedLosses - lives).coerceAtLeast(0)),
            color = if (lives <= 1) ArmaUi.Danger else Color(0xFFFF8A80),
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
    }

    // Top-right: score + controls
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
            SectionLabel(text = "SCORE")
            Text(
                text = fmt(shownScore),
                style = TextStyle(
                    brush = ArmaUi.TitleBrush,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(color = Color(0x99000000), offset = Offset(0f, 2f), blurRadius = 6f)
                )
            )
        }
        HudRoundButton(label = "II", tag = "pause_button", onClick = onPause)
        HudRoundButton(label = "⚙", tag = "hud_settings_button", onClick = onSettings)
    }
}

@Composable
private fun HudRoundButton(label: String, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .glassPanel(radius = 20.dp)
            .consoleTap(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = ArmaUi.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

/** Active power-ups as premium chips with radial timers (only while something is active). */
@Composable
fun BoxScope.PowerChips(engine: GameEngine, frame: () -> Long) {
    @Suppress("UNUSED_VARIABLE") val f = frame()
    val a1 = engine.armadillo
    val a2 = engine.armadillo2
    val speed = maxOf(if (a1.isPowerActive) a1.powerTimer else 0f, if (a2?.isPowerActive == true) a2.powerTimer else 0f)
    val magnet = maxOf(a1.magnetTimer, a2?.magnetTimer ?: 0f)
    val rainbow = engine.scoreMultiplierTimer
    val freeze = engine.freezeTimer
    val shield = a1.shieldCharges > 0 || (a2?.shieldCharges ?: 0) > 0
    if (speed <= 0f && magnet <= 0f && rainbow <= 0f && freeze <= 0f && !shield) return

    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (speed > 0f) PowerChip("⚡", "SPEED", speed, speed / 10f, Color(0xFF00E5FF))
        if (magnet > 0f) PowerChip("🧲", "MAGNET", magnet, magnet / GameEngine.MAGNET_DURATION, Color(0xFFE040FB))
        if (rainbow > 0f) PowerChip("🌈", "x2", rainbow, rainbow / GameEngine.RAINBOW_DURATION, Color(0xFFFFEA00))
        if (freeze > 0f) PowerChip("❄", "FREEZE", freeze, freeze / GameEngine.FREEZE_DURATION, Color(0xFFB3E5FC))
        if (shield) PowerChip("🛡", "SHIELD", -1f, 1f, Color(0xFF40C4FF))
    }
}

/** Console-style event notification when a rare fruit appears. Never blocks input. */
@Composable
fun BoxScope.RareFruitToast(engine: GameEngine, frame: () -> Long) {
    @Suppress("UNUSED_VARIABLE") val f = frame()
    val timer = engine.rareAlertTimer
    val type = engine.rareAlertType ?: return
    if (timer <= 0f) return
    val elapsed = GameEngine.RARE_ALERT_DURATION - timer
    val inA = (elapsed / 0.25f).coerceIn(0f, 1f)
    val outA = (timer / 0.4f).coerceIn(0f, 1f)
    val detail = when (type) {
        FruitType.MAGNET -> "${GameEngine.MAGNET_DURATION.toInt()} SEC"
        FruitType.RAINBOW -> "${GameEngine.RAINBOW_DURATION.toInt()} SEC"
        FruitType.FREEZE -> "${GameEngine.FREEZE_DURATION.toInt()} SEC"
        FruitType.DIAMOND -> "+15 BONUS"
        FruitType.GOLDEN -> "+25 BONUS"
        FruitType.SHIELD -> "BLOCKS 1 BITE"
        else -> ""
    }
    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 18.dp)
            .graphicsLayer {
                alpha = inA * outA
                translationX = (1f - inA) * 60.dp.toPx()
            }
            .glassPanel(radius = 16.dp, strong = true, stroke = ArmaUi.Gold.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.End
    ) {
        SectionLabel(text = "RARE FRUIT DETECTED", color = ArmaUi.Cyan)
        Text(
            text = "${type.powerIcon()}  ${type.powerLabel()}",
            style = TextStyle(brush = ArmaUi.TitleBrush, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        )
        if (detail.isNotEmpty()) {
            Text(text = detail, color = ArmaUi.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

/** Cinematic round intro: the world is already visible; the title moves gently forward, then fades. */
@Composable
fun BoxScope.CinematicRoundIntro(engine: GameEngine, frame: () -> Long) {
    @Suppress("UNUSED_VARIABLE") val f = frame()
    val t = engine.roundIntroTimer
    if (t <= 0f) return
    val stage = engine.currentStage
    val accent = WorldRenderer.accent(stage.theme)
    val elapsed = GameEngine.ROUND_INTRO_DURATION - t
    val inA = (elapsed / 0.35f).coerceIn(0f, 1f)
    val outA = (t / 0.5f).coerceIn(0f, 1f)
    val forward = (elapsed / GameEngine.ROUND_INTRO_DURATION).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .graphicsLayer { alpha = inA * outA }
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0x99000000), Color(0x99000000), Color.Transparent)
                )
            )
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val s = 0.9f + 0.1f * forward
                scaleX = s
                scaleY = s
                translationY = (1f - forward) * 10.dp.toPx()
            }
        ) {
            SectionLabel(text = "ROUND ${stage.id.toString().padStart(2, '0')}", color = accent)
            Text(
                text = stage.name.uppercase(),
                style = TextStyle(
                    brush = ArmaUi.TitleBrush,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    shadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 4f), blurRadius = 14f)
                )
            )
            Text(text = stage.introTagline(), color = ArmaUi.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            VSpace(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(text = "TARGET")
                HSpace(8.dp)
                Text(text = "${stage.targetFruits} FRUIT", color = ArmaUi.Gold, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

/** Thin console toast for engine messages (hidden while the intro or a rare alert is showing). */
@Composable
fun BoxScope.ConsoleBanner(engine: GameEngine, frame: () -> Long) {
    @Suppress("UNUSED_VARIABLE") val f = frame()
    val text = engine.bannerMessage ?: return
    if (engine.roundIntroTimer > 0f || engine.rareAlertTimer > 0f) return
    val a = (engine.bannerTimer / 0.35f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 52.dp)
            .graphicsLayer { alpha = a }
            .glassPanel(radius = 14.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = ArmaUi.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** One-line visual teaching on Round 1 (auto-dismissed by the caller). */
@Composable
fun BoxScope.FirstRunHint(visible: Boolean) {
    if (!visible) return
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 96.dp)
            .glassPanel(radius = 16.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Drag to roll  ·  Grab fruit  ·  Chain bites for COMBOS  ·  Chase glowing power fruit",
            color = ArmaUi.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

// =====================================================================================
// Round Complete
// =====================================================================================

@Composable
fun RoundCompleteOverlay(
    data: StageClearData,
    onContinue: () -> Unit,
    onReplay: () -> Unit,
    onRoundSelect: () -> Unit
) {
    val accent = WorldRenderer.accent(data.stage.theme)
    val timeline = remember { Animatable(0f) }
    LaunchedEffect(data) {
        timeline.snapTo(0f)
        timeline.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    val worldTime by rememberWorldTime()
    val scope = rememberCoroutineScope()
    var focused by remember { mutableIntStateOf(0) }
    var countTarget by remember { mutableIntStateOf(0) }
    LaunchedEffect(data) {
        delay(650)
        countTarget = data.score
    }
    val counted by animateIntAsState(targetValue = countTarget, animationSpec = tween(900, easing = FastOutSlowInEasing), label = "scoreCount")

    fun act(index: Int, action: () -> Unit) {
        focused = index
        scope.launch {
            delay(140)
            action()
        }
    }

    fun reveal(start: Float, len: Float = 0.12f): Float = ((timeline.value - start) / len).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("round_complete_screen")
    ) {
        val compact = maxWidth < 860.dp
        // World stays visible: soft dim + celebratory confetti
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x33000000), Color(0xCC000000)),
                    center = Offset(size.width * 0.35f, size.height * 0.5f),
                    radius = size.maxDimension * 0.8f
                )
            )
            val confetti = listOf(Color(0xFFFFD700), Color(0xFFFF4081), Color(0xFF00E5FF), Color(0xFF76FF03), Color(0xFFFFFFFF), accent)
            for (i in 0 until 46) {
                val seedX = ((i * 73) % 100) / 100f
                val fall = ((((i * 37) % 100) / 100f) + worldTime * (0.06f + (i % 5) * 0.015f)) % 1f
                val x = seedX * size.width + sin(worldTime * 1.3f + i) * 14f
                val y = fall * size.height
                val c = confetti[i % confetti.size]
                drawRect(
                    color = c.copy(alpha = 0.85f),
                    topLeft = Offset(x, y),
                    size = Size(5f + (i % 3) * 2f, 9f - (i % 3) * 2f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (compact) 22.dp else 40.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: title + stats
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.graphicsLayer { alpha = reveal(0f) }) {
                    SectionLabel(text = if (data.stage.id == 16) "CAMPAIGN COMPLETE" else "ROUND COMPLETE", color = accent)
                }
                Text(
                    text = data.stage.name.uppercase(),
                    style = TextStyle(
                        brush = ArmaUi.TitleBrush,
                        fontSize = if (compact) 34.sp else 46.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        shadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 4f), blurRadius = 14f)
                    ),
                    modifier = Modifier.graphicsLayer {
                        val r = reveal(0.05f, 0.2f)
                        alpha = r
                        val s = 0.92f + 0.08f * r
                        scaleX = s
                        scaleY = s
                        translationY = (1f - r) * 16.dp.toPx()
                    }
                )
                VSpace(14.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 28.dp)) {
                    Box(modifier = Modifier.graphicsLayer { alpha = reveal(0.22f) }) {
                        StatBlock(label = "SCORE", value = fmt(counted), valueColor = ArmaUi.Gold, big = true)
                    }
                    Box(modifier = Modifier.graphicsLayer { alpha = reveal(0.30f) }) {
                        StatBlock(label = "BEST", value = fmt(maxOf(data.bestScore, data.score)), big = true)
                    }
                    Box(modifier = Modifier.graphicsLayer { alpha = reveal(0.38f) }) {
                        StatBlock(label = "BEST COMBO", value = "x${data.bestCombo}", valueColor = Color(0xFFFFAB40), big = true)
                    }
                    Box(modifier = Modifier.graphicsLayer { alpha = reveal(0.46f) }) {
                        StatBlock(label = "FRUIT", value = "${data.fruits} / ${data.stage.targetFruits}", big = true)
                    }
                }
                if (data.isNewRecord) {
                    VSpace(10.dp)
                    Box(
                        modifier = Modifier
                            .graphicsLayer { alpha = reveal(0.55f) }
                            .glassPanel(radius = 12.dp, stroke = ArmaUi.Gold)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(text = "★ NEW PERSONAL BEST", color = ArmaUi.Gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                }
                VSpace(22.dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                    modifier = Modifier.graphicsLayer {
                        val r = reveal(0.62f, 0.15f)
                        alpha = r
                        translationY = (1f - r) * 18.dp.toPx()
                    }
                ) {
                    ConsoleMenuItem(
                        label = if (data.stage.id >= 16) "PLAY AGAIN" else "CONTINUE",
                        sublabel = data.newlyUnlocked?.let { "ROUND ${it.id.toString().padStart(2, '0')} · ${it.name.uppercase()}" },
                        focused = focused == 0,
                        primary = true,
                        modifier = Modifier.testTag("complete_continue"),
                        onClick = { act(0, onContinue) }
                    )
                    ConsoleMenuItem(label = "REPLAY", focused = focused == 1, accent = accent, modifier = Modifier.testTag("complete_replay"), onClick = { act(1, onReplay) })
                    ConsoleMenuItem(label = if (compact) "ROUNDS" else "ROUND SELECT", focused = focused == 2, accent = accent, modifier = Modifier.testTag("complete_round_select"), onClick = { act(2, onRoundSelect) })
                }
            }

            // Right: new world unlock card
            val unlocked = data.newlyUnlocked
            if (unlocked != null) {
                HSpace(if (compact) 12.dp else 24.dp)
                UnlockRevealCard(stage = unlocked, progress = { reveal(0.45f, 0.4f) }, time = { worldTime }, compact = compact)
            }
        }
    }
}

/**
 * NEW WORLD UNLOCKED: the card starts dark, light reveals the environment, particles appear,
 * the lock dissolves and the card moves slightly forward.
 */
@Composable
private fun UnlockRevealCard(stage: StageConfig, progress: () -> Float, time: () -> Float, compact: Boolean) {
    val accent = WorldRenderer.accent(stage.theme)
    val shape = RoundedCornerShape(20.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.graphicsLayer { alpha = (progress() * 3f).coerceIn(0f, 1f) }) {
            SectionLabel(text = "NEW WORLD UNLOCKED", color = accent)
        }
        VSpace(8.dp)
        Box(
            modifier = Modifier
                .width(if (compact) 190.dp else 250.dp)
                .height(if (compact) 130.dp else 170.dp)
                .graphicsLayer {
                    val p = progress()
                    val s = 0.9f + 0.12f * p
                    scaleX = s
                    scaleY = s
                    alpha = (p * 2.5f).coerceIn(0f, 1f)
                }
                .clip(shape)
                .border(2.dp, accent.copy(alpha = 0.9f), shape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = progress()
                WorldRenderer.drawWorld(this, size.width, size.height, stage.theme, time(), parallax = 0f, detail = 0)
                // light sweep reveal: darkness recedes left → right
                drawRect(color = Color.Black.copy(alpha = (1f - p).coerceIn(0f, 1f) * 0.92f))
                if (p in 0.05f..0.95f) {
                    val x = size.width * p
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                            startX = x - 40f,
                            endX = x + 40f
                        ),
                        topLeft = Offset(x - 40f, 0f),
                        size = Size(80f, size.height)
                    )
                }
                // sparkle particles once revealed
                if (p > 0.4f) {
                    for (i in 0 until 14) {
                        val px = (((i * 73) % 100) / 100f) * size.width
                        val py = ((((i * 41) % 100) / 100f) - time() * 0.05f * (1 + i % 3)).let { ((it % 1f) + 1f) % 1f } * size.height
                        drawCircle(color = accent.copy(alpha = 0.7f * (p - 0.4f) / 0.6f), radius = 2f + (i % 3), center = Offset(px, py))
                    }
                }
            }
            // Lock dissolves
            Text(
                text = "🔒",
                fontSize = 34.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val p = progress()
                        alpha = (1f - p * 1.6f).coerceIn(0f, 1f)
                        val s = 1f + p * 0.8f
                        scaleX = s
                        scaleY = s
                    }
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .graphicsLayer { alpha = ((progress() - 0.5f) * 2f).coerceIn(0f, 1f) }
            ) {
                Text(text = "ROUND ${stage.id.toString().padStart(2, '0')}", color = ArmaUi.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text(text = stage.name.uppercase(), style = TextStyle(brush = ArmaUi.TitleBrush, fontSize = 20.sp, fontWeight = FontWeight.Black))
            }
        }
    }
}

// =====================================================================================
// Game Over
// =====================================================================================

/**
 * Cinematic Game Over: the world stays visible and first darkens / cools (camera settles),
 * then the result and actions fade in. RETRY is the main focus. All monetization paths of the
 * previous dialog are preserved (RETRY → interstitial, WATCH AD → rewarded second chance).
 */
@Composable
fun GameOverOverlay(
    data: GameOverData,
    statusMessage: String?,
    onRetry: () -> Unit,
    onWatchAd: () -> Unit,
    onRoundSelect: () -> Unit,
    onHome: () -> Unit
) {
    val veil = remember { Animatable(0f) }
    val ui = remember { Animatable(0f) }
    LaunchedEffect(data) {
        veil.snapTo(0f)
        ui.snapTo(0f)
        launch { veil.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
        delay(600)
        ui.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    val scope = rememberCoroutineScope()
    var focused by remember { mutableIntStateOf(0) }
    fun act(index: Int, action: () -> Unit) {
        focused = index
        scope.launch {
            delay(140)
            action()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_over_dialog")
    ) {
        val compact = maxWidth < 760.dp
        // Cool, darkened world (the snake/armadillo moment stays visible underneath)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val v = veil.value
            drawRect(color = Color(0xFF0A0E18).copy(alpha = 0.62f * v))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF3A0008).copy(alpha = 0.55f * v)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.7f
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = ui.value
                    val s = 0.94f + 0.06f * ui.value
                    scaleX = s
                    scaleY = s
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionLabel(text = "ROUND ${data.stage.id.toString().padStart(2, '0')} · ${data.stage.name.uppercase()}", color = ArmaUi.TextSecondary)
            Text(
                text = "GAME OVER",
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFE0E0), Color(0xFFFF5A6E), Color(0xFFB0102A))),
                    fontSize = if (compact) 40.sp else 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    shadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 5f), blurRadius = 16f)
                )
            )
            VSpace(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                StatBlock(label = "SCORE", value = fmt(data.score), valueColor = ArmaUi.Gold, big = true)
                StatBlock(label = "BEST", value = fmt(maxOf(data.bestScore, data.score)), big = true)
                StatBlock(label = "FRUIT", value = "${data.fruits} / ${data.stage.targetFruits}", big = true)
            }
            VSpace(20.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ConsoleMenuItem(
                    label = "RETRY",
                    focused = focused == 0,
                    primary = true,
                    accent = Color(0xFFFF6B6B),
                    modifier = Modifier.testTag("retry_button"),
                    onClick = { act(0, onRetry) }
                )
                ConsoleMenuItem(
                    label = if (compact) "▶ 2ND CHANCE" else "▶ SECOND CHANCE",
                    sublabel = "WATCH AN AD",
                    focused = focused == 1,
                    accent = ArmaUi.Cyan,
                    modifier = Modifier.testTag("watch_ad_button"),
                    onClick = { act(1, onWatchAd) }
                )
                ConsoleMenuItem(label = if (compact) "ROUNDS" else "ROUND SELECT", focused = focused == 2, modifier = Modifier.testTag("game_over_rounds"), onClick = { act(2, onRoundSelect) })
                ConsoleMenuItem(label = "HOME", focused = focused == 3, modifier = Modifier.testTag("game_over_home"), onClick = { act(3, onHome) })
            }
            if (statusMessage != null) {
                VSpace(10.dp)
                Text(text = statusMessage, color = ArmaUi.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
