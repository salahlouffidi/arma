package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.game.SoundManager
import com.example.game.StageConfig
import com.example.game.StageProgressManager
import com.example.game.WorldRenderer
import com.example.game.difficultyLevel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val LIBRARY_PREFS = "armasnake_settings"

/**
 * Console-library style round selection: large environment cards in a perspective carousel.
 * The focused card is dominant; neighbours sit smaller and deeper. The full-screen world behind
 * the cards crossfades to the focused round. Swipe, tap a side card, or use the arrows.
 */
@Composable
fun RoundLibraryScreen(
    currentStageId: Int,
    progressManager: StageProgressManager,
    soundManager: SoundManager,
    onSelectStage: (stageId: Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(LIBRARY_PREFS, 0) }
    val scope = rememberCoroutineScope()
    val time by rememberWorldTime()

    var filter by remember { mutableIntStateOf(0) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    var controlSettingsRound by remember { mutableIntStateOf(0) }

    val stages = remember(filter) {
        when (filter) {
            1 -> StageConfig.allStages.filter { !it.isDualArmadillo }
            2 -> StageConfig.allStages.filter { it.isDualArmadillo }
            else -> StageConfig.allStages
        }
    }
    val initialIndex = remember(filter) {
        stages.indexOfFirst { it.id == currentStageId }.coerceAtLeast(0)
    }
    var focused by remember(filter) { mutableIntStateOf(initialIndex) }
    val pos = remember(filter) { Animatable(initialIndex.toFloat()) }
    val launch = remember { Animatable(0f) }
    val nudge = remember { Animatable(0f) }
    var launching by remember { mutableIntStateOf(0) }

    val highestUnlocked = progressManager.getHighestUnlockedStage()
    val focusedStage = stages[focused.coerceIn(0, stages.size - 1)]
    val accent = WorldRenderer.accent(focusedStage.theme)

    fun focusTo(index: Int) {
        val target = index.coerceIn(0, stages.size - 1)
        if (target != focused) soundManager.playMenuClick()
        focused = target
        scope.launch { pos.animateTo(target.toFloat(), spring(dampingRatio = 0.84f, stiffness = 240f)) }
    }

    fun playFocused() {
        val stage = stages[focused]
        if (!progressManager.isStageUnlocked(stage.id)) {
            soundManager.playWallBump()
            scope.launch {
                nudge.snapTo(0f)
                nudge.animateTo(1f, tween(420))
            }
            return
        }
        if (launching != 0) return
        launching = stage.id
        soundManager.playStageSelected()
        scope.launch {
            // Selected card surges toward the screen; the world behind becomes the gameplay world.
            launch.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            preferences.edit().putInt("active_single_joystick_stage_id", stage.id).apply()
            onSelectStage(stage.id)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("round_select_screen")
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val cardH = maxHeight * 0.58f
        val cardW = minOf(cardH.value * 1.34f, maxWidth.value * 0.52f).dp
        val spacingPx = with(density) { (cardW * 0.74f).toPx() }

        // 1. Full-screen world preview of the focused round (crossfades on focus change)
        WorldBackdrop(
            theme = focusedStage.theme,
            modifier = Modifier.fillMaxSize(),
            parallax = { ((focused - pos.value) * 0.8f).coerceIn(-1f, 1f) },
            detail = 2,
            dim = 0.18f
        )

        // 2. Top bar: HOME · title · filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .graphicsLayer { alpha = 1f - launch.value },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .glassPanel(radius = 20.dp)
                    .consoleTap {
                        soundManager.playMenuClick()
                        onBack()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("round_select_back_button")
            ) {
                Text(text = "‹  HOME", color = ArmaUi.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SectionLabel(text = "ROUND SELECT", color = accent)
                Text(
                    text = "$highestUnlocked / 16 WORLDS UNLOCKED",
                    color = ArmaUi.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "SOLO", "TWIN").forEachIndexed { idx, label ->
                    val sel = filter == idx
                    Box(
                        modifier = Modifier
                            .glassPanel(radius = 14.dp, stroke = if (sel) accent else ArmaUi.GlassStroke)
                            .consoleTap {
                                if (filter != idx) {
                                    soundManager.playMenuClick()
                                    filter = idx
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (sel) accent else ArmaUi.TextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        // 3. Perspective carousel (swipe anywhere in this band)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp)
                .pointerInput(filter, spacingPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val target = pos.value.roundToInt().coerceIn(0, stages.size - 1)
                            focusTo(target)
                            // re-centre even when the index did not change
                            scope.launch { pos.animateTo(target.toFloat(), spring(dampingRatio = 0.84f, stiffness = 240f)) }
                        },
                        onDragCancel = {
                            scope.launch { pos.animateTo(focused.toFloat(), spring(dampingRatio = 0.84f, stiffness = 240f)) }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val next = (pos.value - dragAmount / spacingPx).coerceIn(-0.4f, stages.size - 0.6f)
                        scope.launch { pos.snapTo(next) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Only the focused card and two neighbours each side are rendered (each card draws
            // a full procedural world, so this keeps the library smooth on mid-range phones).
            val lo = (focused - 2).coerceAtLeast(0)
            val hi = (focused + 2).coerceAtMost(stages.size - 1)
            for (i in lo..hi) {
                val stage = stages[i]
                val isFocused = i == focused
                val unlocked = progressManager.isStageUnlocked(stage.id)
                val completed = progressManager.isStageCompleted(stage.id)
                val best = progressManager.getStageHighScore(stage.id)
                val joystickRight = if (!stage.isDualArmadillo) {
                    @Suppress("UNUSED_VARIABLE") val v = settingsVersion
                    preferences.getBoolean("single_joystick_right_round_${stage.id}", false)
                } else false

                RoundCard(
                    stage = stage,
                    unlocked = unlocked,
                    completed = completed,
                    bestScore = best,
                    isFocused = isFocused,
                    joystickRight = joystickRight,
                    time = { time },
                    distance = { i - pos.value },
                    modifier = Modifier
                        .zIndex(10f - abs(i - focused))
                        .width(cardW)
                        .height(cardH)
                        .graphicsLayer {
                            val d = i - pos.value
                            val ad = abs(d).coerceAtMost(2.2f)
                            val surge = if (isFocused) launch.value else 0f
                            translationX = d * spacingPx +
                                (if (isFocused) sin(nudge.value * 12.56f) * (1f - nudge.value) * 10.dp.toPx() else 0f)
                            val s = (1f - 0.2f * ad) * (1f + surge * 0.35f)
                            scaleX = s
                            scaleY = s
                            alpha = ((1f - 0.34f * ad) * (if (isFocused) 1f else 1f - launch.value)).coerceIn(0f, 1f)
                            rotationY = (-d * 16f).coerceIn(-34f, 34f)
                            cameraDistance = 14f * density.density
                        },
                    onClick = {
                        if (!isFocused) focusTo(i) else playFocused()
                    },
                    onSettings = if (!stage.isDualArmadillo && unlocked) {
                        { controlSettingsRound = stage.id }
                    } else null
                )
            }
        }

        // 4. Bottom bar: arrows · dominant action · arrows
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .graphicsLayer { alpha = 1f - launch.value },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NavArrow(label = "‹", enabled = focused > 0) { focusTo(focused - 1) }
            val unlockedFocused = progressManager.isStageUnlocked(focusedStage.id)
            if (unlockedFocused) {
                ConsoleMenuItem(
                    label = "PLAY ROUND ${focusedStage.id.toString().padStart(2, '0')}",
                    focused = true,
                    primary = true,
                    accent = ArmaUi.Gold,
                    modifier = Modifier.testTag("round_play_button"),
                    onClick = { playFocused() }
                )
            } else {
                Box(modifier = Modifier.glassPanel(radius = 18.dp).padding(horizontal = 22.dp, vertical = 12.dp)) {
                    Text(
                        text = "🔒  CLEAR ROUND ${(focusedStage.id - 1).coerceAtLeast(1)} TO UNLOCK",
                        color = ArmaUi.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
            NavArrow(label = "›", enabled = focused < stages.size - 1) { focusTo(focused + 1) }
        }

        // 5. Launch veil: the screen gently darkens as the card surges forward
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = launch.value * 0.55f }
                .background(Color.Black)
        )
    }

    if (controlSettingsRound in 1..8) {
        RoundControlSettingsDialog(
            roundId = controlSettingsRound,
            soundManager = soundManager,
            onDismiss = {
                controlSettingsRound = 0
                settingsVersion++
            }
        )
    }
}

@Composable
private fun NavArrow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .glassPanel(radius = 24.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.3f }
            .consoleTap(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = ArmaUi.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}

/**
 * One large environment card. Uses the shared WorldRenderer for the artwork so the preview is
 * literally the world you are about to play. [distance] (index − carousel position) drives
 * info crossfade and the inner parallax without recomposing.
 */
@Composable
private fun RoundCard(
    stage: StageConfig,
    unlocked: Boolean,
    completed: Boolean,
    bestScore: Int,
    isFocused: Boolean,
    joystickRight: Boolean,
    time: () -> Float,
    distance: () -> Float,
    modifier: Modifier,
    onClick: () -> Unit,
    onSettings: (() -> Unit)?
) {
    val accent = WorldRenderer.accent(stage.theme)
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(ArmaUi.Ink, shape)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) accent.copy(alpha = 0.95f) else ArmaUi.GlassStroke,
                shape = shape
            )
            .consoleTap(onClick = onClick)
            .testTag("round_card_${stage.id}")
    ) {
        // Environment artwork
        Canvas(modifier = Modifier.fillMaxSize()) {
            WorldRenderer.drawWorld(
                this, size.width, size.height, stage.theme, time(),
                parallax = distance().coerceIn(-1f, 1f) * 0.6f,
                detail = 0
            )
            // readability scrim
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x66000000), Color.Transparent, Color(0xE6000000)),
                    startY = 0f,
                    endY = size.height
                )
            )
            if (!unlocked) drawRect(color = Color(0xA6000000))
        }

        // Top row: round number + status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ROUND ${stage.id.toString().padStart(2, '0')} / 16",
                color = ArmaUi.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (stage.isDualArmadillo) StatusChip("TWIN", ArmaUi.Cyan)
                when {
                    !unlocked -> StatusChip("LOCKED", ArmaUi.TextDim)
                    completed -> StatusChip("COMPLETE", ArmaUi.Success)
                    else -> StatusChip("AVAILABLE", ArmaUi.Gold)
                }
            }
        }

        // Lock emblem
        if (!unlocked) {
            Text(
                text = "🔒",
                fontSize = 34.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Bottom info block (fades with distance from focus)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
                .graphicsLayer {
                    val d = abs(distance()).coerceAtMost(1f)
                    alpha = 1f - 0.75f * d
                    translationY = d * 10.dp.toPx()
                }
        ) {
            Text(
                text = stage.name.uppercase(),
                style = TextStyle(
                    brush = if (unlocked) ArmaUi.TitleBrush else Brush.verticalGradient(listOf(Color(0xFFBDBDBD), Color(0xFF757575))),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    shadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 3f), blurRadius = 10f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${stage.icon}  ${stage.subtitle}",
                color = ArmaUi.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            VSpace(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    SectionLabel(text = "DIFFICULTY")
                    VSpace(4.dp)
                    DifficultyMeter(level = stage.difficultyLevel(), accent = accent)
                }
                HSpace(18.dp)
                StatBlock(label = "TARGET", value = "${stage.targetFruits} FRUIT")
                HSpace(18.dp)
                StatBlock(label = "BEST", value = if (bestScore > 0) "%,d".format(bestScore) else "—", valueColor = ArmaUi.Gold)
                HSpace(18.dp)
                StatBlock(label = "LIVES", value = "${stage.maxAllowedLosses}")
                if (isFocused && onSettings != null) {
                    HSpace(14.dp)
                    Box(
                        modifier = Modifier
                            .glassPanel(radius = 12.dp)
                            .consoleTap(onClick = onSettings)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("round_${stage.id}_settings")
                    ) {
                        Text(
                            text = if (joystickRight) "⚙ STICK ▶" else "⚙ ◀ STICK",
                            color = ArmaUi.TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .glassPanel(radius = 10.dp, stroke = color.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
    }
}
