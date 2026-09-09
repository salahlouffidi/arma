package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.GameEngine
import com.example.game.HapticFeedbackHelper
import com.example.game.SoundManager
import com.example.game.StageConfig
import com.example.game.StageProgressManager
import com.example.game.TableRenderer
import com.example.game.Vector2
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class GameScreenState {
    INTRO,
    MAIN_MENU,
    STAGE_SELECT,
    GAMEPLAY
}

data class StageClearData(
    val stage: StageConfig,
    val score: Int,
    val fruits: Int,
    val lost: Int,
    val timeSeconds: Float,
    val isNewRecord: Boolean
)

@Composable
fun ArmadilloEscapeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val haptics = remember { HapticFeedbackHelper(context) }
    val progressManager = remember { StageProgressManager(context) }
    val soundManager = remember { SoundManager(context) }

    var currentScreen by remember { mutableStateOf(GameScreenState.GAMEPLAY) }
    var showAudioSettings by remember { mutableStateOf(false) }

    var highestUnlockedStage by remember { mutableIntStateOf(progressManager.getHighestUnlockedStage()) }
    var currentStageId by remember { mutableIntStateOf(1) }
    var showStageSelector by remember { mutableStateOf(false) }
    var stageClearDialogData by remember { mutableStateOf<StageClearData?>(null) }

    // Virtual Joystick Finger Input Vectors (Continuous Normalized: -1f..1f)
    // P1 (Golden Armadillo)
    var joystickInputX by remember { mutableFloatStateOf(0f) }
    var joystickInputY by remember { mutableFloatStateOf(0f) }
    // P2 (Blue Armadillo - Stages 9-16)
    var joystickInputX2 by remember { mutableFloatStateOf(0f) }
    var joystickInputY2 by remember { mutableFloatStateOf(0f) }

    // Game Engine
    val gameEngine = remember {
        GameEngine(
            onAppleCollected = {
                haptics.appleEat()
                soundManager.playFruitEaten()
                soundManager.playPointGained()
            },
            onArmadilloEaten = {
                haptics.snakeCatch()
                soundManager.playSnakeCollision()
                soundManager.playPointLost()
            },
            onWallBump = {
                haptics.wallBump()
                soundManager.playWallBump()
            },
            onSpecialEventTriggered = {
                haptics.powerActivated()
                soundManager.playSpecialFruitActivated()
            },
            onPowerActivated = {
                haptics.powerActivated()
                soundManager.playPowerActivated()
            },
            onPowerProtectedHit = {
                haptics.powerProtectedHit()
                soundManager.playProtectionHit()
                soundManager.playPointLost()
            },
            onLavaHit = {
                haptics.wallBump()
                soundManager.playLavaHit()
                soundManager.playPointLost()
            },
            onArmadilloSpawned = {
                soundManager.playNewArmadilloSpawned()
            }
        ).apply {
            onStageCompleted = { stage, score, fruits, lost, time ->
                val isNewRecord = progressManager.recordStageScore(stage.id, score)
                progressManager.completeStage(stage.id)
                highestUnlockedStage = progressManager.getHighestUnlockedStage()
                haptics.powerActivated()
                if (stage.id < 16) {
                    soundManager.playStageCompleted()
                } else {
                    soundManager.playFinalStageCompleted()
                }
                stageClearDialogData = StageClearData(
                    stage = stage,
                    score = score,
                    fruits = fruits,
                    lost = lost,
                    timeSeconds = time,
                    isNewRecord = isNewRecord
                )
            }
        }
    }

    LaunchedEffect(currentStageId) {
        joystickInputX = 0f
        joystickInputY = 0f
        joystickInputX2 = 0f
        joystickInputY2 = 0f
        val config = StageConfig.getStage(currentStageId)
        gameEngine.loadStage(config)
    }

    // Score pop animation
    val scoreScale = remember { Animatable(1f) }
    var lastScore by remember { mutableIntStateOf(0) }

    // Audio lifecycle
    DisposableEffect(Unit) {
        onDispose {
            soundManager.stopMusic()
        }
    }

    // Game Loop (active only during gameplay)
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(currentScreen) {
        if (currentScreen != GameScreenState.GAMEPLAY) return@LaunchedEffect
        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
                lastTime = now
                frameTimeNanos = now

                // Direct Virtual Joystick Finger Input driving armadillo physics
                // P1 controls original golden armadillo; P2 controls blue armadillo in Stages 9–16
                gameEngine.update(dt, joystickInputX, joystickInputY, joystickInputX2, joystickInputY2)
                tick++

                val curScore = gameEngine.stats.score
                if (curScore != lastScore) {
                    lastScore = curScore
                    scope.launch {
                        scoreScale.animateTo(1.3f, tween(100))
                        scoreScale.animateTo(1.0f, tween(150))
                    }
                }
            }
        }
    }

    // Top-level Screen Router
    when (currentScreen) {
        GameScreenState.INTRO -> {
            IntroScreen(
                soundManager = soundManager,
                onFinished = {
                    currentScreen = GameScreenState.MAIN_MENU
                }
            )
        }
        GameScreenState.MAIN_MENU -> {
            MainMenuScreen(
                currentStageId = currentStageId,
                soundManager = soundManager,
                onStartGame = { stageId ->
                    currentStageId = stageId
                    currentScreen = GameScreenState.GAMEPLAY
                },
                onOpenStageSelect = {
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onOpenAudioSettings = {
                    showAudioSettings = true
                }
            )
        }
        GameScreenState.STAGE_SELECT -> {
            StageSelectScreen(
                currentStageId = currentStageId,
                progressManager = progressManager,
                soundManager = soundManager,
                onSelectStage = { stageId ->
                    currentStageId = stageId
                    currentScreen = GameScreenState.GAMEPLAY
                },
                onBackToMenu = {
                    currentScreen = GameScreenState.MAIN_MENU
                }
            )
        }
        GameScreenState.GAMEPLAY -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF140D08))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Fixed Table Geometry: Table occupies majority of the screen, staying completely fixed
        val tableMarginX = (screenWidth * 0.05f).coerceAtLeast(18f)
        val tableMarginTop = 64f
        val tableMarginBottom = 68f

        val tableHalfW = ((screenWidth - tableMarginX * 2f) / 2f).coerceAtLeast(160f)
        val tableHalfH = ((screenHeight - (tableMarginTop + tableMarginBottom)) / 2f).coerceAtLeast(120f)
        val tableCenter = Offset(screenWidth / 2f, tableMarginTop + tableHalfH)

        LaunchedEffect(tableHalfW, tableHalfH) {
            gameEngine.setBoardDimensions(tableHalfW, tableHalfH)
        }

        // ==================== FIXED 3D TABLETOP CANVAS ====================
        // The table, background, camera NEVER rotate with phone motion
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_board_canvas")
        ) {
            // Read tick to trigger recomposition
            @Suppress("UNUSED_VARIABLE")
            val currentTick = tick

            // 1. Draw Fixed Themed Table
            TableRenderer.drawTable(
                scope = this,
                center = tableCenter,
                halfW = tableHalfW,
                halfH = tableHalfH,
                theme = gameEngine.currentStage.theme
            )

            // 1b. Draw Obstacles
            TableRenderer.drawObstacles(
                scope = this,
                obstacles = gameEngine.obstacles,
                boardCenter = tableCenter
            )

            // 2. Draw Apples
            TableRenderer.drawApples(
                scope = this,
                apples = gameEngine.apples,
                boardCenter = tableCenter
            )

            // 3. Draw 3D Armadillos
            TableRenderer.drawArmadillo(
                scope = this,
                armadillo = gameEngine.armadillo,
                boardCenter = tableCenter,
                isBlue = false
            )
            val a2 = gameEngine.armadillo2
            if (a2 != null) {
                TableRenderer.drawArmadillo(
                    scope = this,
                    armadillo = a2,
                    boardCenter = tableCenter,
                    isBlue = true
                )
            }

            // 4. Draw 3D Snakes
            TableRenderer.drawSnakes(
                scope = this,
                snakes = gameEngine.snakes,
                boardCenter = tableCenter
            )

            // 5. Draw Particles
            TableRenderer.drawParticles(
                scope = this,
                particles = gameEngine.particles,
                boardCenter = tableCenter
            )
        }

        // ==================== HUD: TOP BAR ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back to Stage Select
                Surface(
                    modifier = Modifier
                        .clickable {
                            soundManager.playMenuClick()
                            currentScreen = GameScreenState.STAGE_SELECT
                        }
                        .testTag("back_to_stages_button"),
                    color = Color(0xD92E1508),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80D4AF37))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Stages",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STAGES",
                            color = Color(0xFFFFE0B2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Stage Selection Trigger
                Surface(
                    modifier = Modifier
                        .clickable { showStageSelector = true }
                        .testTag("stage_badge_button"),
                    color = Color(0xD922130A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80D4AF37))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STAGE ${gameEngine.currentStage.id}: ${gameEngine.currentStage.title.uppercase()}",
                            color = Color(0xFFFFE0B2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "▾",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Status, Score, Goal & Lost counters
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stage Fruit Goal Tracker
                Surface(
                    color = Color(0xD922130A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6081C784))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GOAL: ",
                            color = Color(0xFFC8E6C9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${gameEngine.stageFruitsCollected}/${gameEngine.currentStage.targetFruits} 🍎",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Special Move / Protection Chances Indicator (when active)
                if (gameEngine.armadillo.isPowerActive) {
                    Surface(
                        color = Color(0xF0003B46),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ 7ARAKA ",
                                color = Color(0xFFFFEA00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${gameEngine.armadillo.powerTimer.toInt()}s",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            repeat(gameEngine.armadillo.powerChancesRemaining) {
                                Text(
                                    text = "🛡️",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    // Fruit event progress tracker
                    Surface(
                        color = Color(0xD922130A),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x50FFB300))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EVENT: ${gameEngine.fruitsCollectedSinceEvent}/4 🍎",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Score Pill
                Surface(
                    modifier = Modifier.scale(scoreScale.value),
                    color = Color(0xE62A170C),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF7043))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCORE: ",
                            color = Color(0xFFFFCCBC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${gameEngine.stats.score}",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Armadillos Lost Pill
                Surface(
                    color = Color(0xE6231208),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6081C784))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOST: ",
                            color = Color(0xFFC8E6C9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${gameEngine.stats.armadillosLost}",
                            color = Color(0xFFE57373),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Audio Toggle
                Surface(
                    color = Color(0xE62A170C),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier
                        .clickable {
                            soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                            if (soundManager.isSoundEnabled) {
                                soundManager.startMusic()
                                soundManager.playMenuClick()
                            }
                        }
                        .testTag("quick_audio_toggle")
                ) {
                    Box(modifier = Modifier.padding(6.dp)) {
                        Icon(
                            imageVector = if (soundManager.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Toggle Audio",
                            tint = if (soundManager.isSoundEnabled) Color(0xFFFFD54F) else Color(0xFFE57373),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Audio & Controls Settings
                Surface(
                    color = Color(0xE62A170C),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier
                        .clickable {
                            soundManager.playMenuClick()
                            showAudioSettings = true
                        }
                        .testTag("hud_settings_button")
                ) {
                    Box(modifier = Modifier.padding(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Audio Settings",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ==================== VIRTUAL JOYSTICKS ====================
        val isDual = gameEngine.isDualArmadillo
        if (isDual) {
            // Dual Armadillo Joysticks: Left = Golden P1, Right = Blue P2
            VirtualJoystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 12.dp),
                size = 122.dp,
                label = "P1 GOLDEN",
                isBlueTheme = false,
                testTag = "virtual_joystick_p1",
                onMove = { normX, normY ->
                    joystickInputX = normX
                    joystickInputY = normY
                }
            )

            VirtualJoystick(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 12.dp),
                size = 122.dp,
                label = "P2 BLUE",
                isBlueTheme = true,
                testTag = "virtual_joystick_p2",
                onMove = { normX, normY ->
                    joystickInputX2 = normX
                    joystickInputY2 = normY
                }
            )

            // ==================== CONTROLS: BOTTOM BAR (DUAL MODE) ====================
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 145.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xD91E120A),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x50D4AF37))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🕹️ LEFT: GOLDEN  |  RIGHT: BLUE 🕹️",
                            color = Color(0xFFFFE0B2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        showStageSelector = true
                        haptics.click()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E2312),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("stages_menu_trigger_button")
                ) {
                    Text(
                        text = "🏆 STAGES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        } else {
            // Single Joystick (Original Stages 1–8)
            VirtualJoystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, bottom = 12.dp),
                size = 126.dp,
                label = "MOVE",
                isBlueTheme = false,
                testTag = "virtual_joystick",
                onMove = { normX, normY ->
                    joystickInputX = normX
                    joystickInputY = normY
                }
            )

            // ==================== CONTROLS: BOTTOM BAR (SINGLE MODE) ====================
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Space reservation for the left-aligned VirtualJoystick
                Spacer(modifier = Modifier.width(140.dp))

                // Steering Instruction Pill
                Surface(
                    color = Color(0xD91E120A),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x50D4AF37))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DRAG THE JOYSTICK TO MOVE",
                            color = Color(0xFFFFE0B2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Central / Right Action: STAGES Button
                Button(
                    onClick = {
                        showStageSelector = true
                        haptics.click()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E2312),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("stages_menu_trigger_button")
                ) {
                    Text(
                        text = "🏆 STAGES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // ==================== STAGE SELECTION DIALOG ====================
        if (showStageSelector) {
            StageSelectionDialog(
                currentStageId = currentStageId,
                highestUnlockedStage = highestUnlockedStage,
                progressManager = progressManager,
                onSelectStage = { stageId ->
                    currentStageId = stageId
                },
                onDismiss = { showStageSelector = false }
            )
        }

        // ==================== STAGE CLEAR CELEBRATION DIALOG ====================
        stageClearDialogData?.let { clearData ->
            StageClearDialog(
                data = clearData,
                onNextStage = {
                    val nextId = (clearData.stage.id + 1).coerceAtMost(16)
                    currentStageId = nextId
                    stageClearDialogData = null
                },
                onRetryStage = {
                    val retryId = clearData.stage.id
                    val config = StageConfig.getStage(retryId)
                    gameEngine.loadStage(config)
                    stageClearDialogData = null
                },
                onSelectOther = {
                    stageClearDialogData = null
                    currentScreen = GameScreenState.STAGE_SELECT
                }
            )
        }

        // ==================== AUDIO SETTINGS DIALOG ====================
        if (showAudioSettings) {
            AudioSettingsDialog(
                soundManager = soundManager,
                onDismiss = { showAudioSettings = false }
            )
        }
    }
    }
    }
}

@Composable
private fun StageSelectionDialog(
    currentStageId: Int,
    highestUnlockedStage: Int,
    progressManager: StageProgressManager,
    onSelectStage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E130B),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD4AF37)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 16-STAGE JOURNEY",
                        color = Color(0xFFFFD700),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(StageConfig.allStages) { stage ->
                        val isUnlocked = stage.id <= highestUnlockedStage
                        val isCurrent = stage.id == currentStageId
                        val isCompleted = progressManager.isStageCompleted(stage.id)
                        val highScore = progressManager.getStageHighScore(stage.id)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isCurrent -> Color(0xFF45240F)
                                isUnlocked -> Color(0xFF2B170B)
                                else -> Color(0xFF19110B)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) Color(0xFFFFD54F) else if (isUnlocked) Color(0x40D4AF37) else Color(0x20FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isUnlocked) {
                                    onSelectStage(stage.id)
                                    onDismiss()
                                }
                                .testTag("stage_item_${stage.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isUnlocked) "${stage.id}." else "🔒",
                                        color = if (isUnlocked) Color(0xFFFFD54F) else Color(0xFF666666),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stage.title,
                                                color = if (isUnlocked) Color.White else Color(0xFF666666),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isCompleted) {
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(text = "✓", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            text = stage.subtitle,
                                            color = if (isUnlocked) Color(0xFFFFCCBC) else Color(0xFF555555),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                if (isUnlocked) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${stage.targetFruits} 🍎",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (highScore > 0) {
                                            Text(
                                                text = "Best: $highScore",
                                                color = Color(0xFF81C784),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageClearDialog(
    data: StageClearData,
    onNextStage: () -> Unit,
    onRetryStage: () -> Unit,
    onSelectOther: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF1C1008),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (data.stage.id == 16) "👑 16-STAGE DUAL MASTER! 👑" else if (data.stage.id == 8) "🌟 ORIGINAL CAMPAIGN CLEARED! 🌟" else "🎉 STAGE ${data.stage.id} CLEARED!",
                    color = Color(0xFFFFD700),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${data.stage.title} — ${data.stage.subtitle}",
                    color = Color(0xFFFFE0B2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Row
                Surface(
                    color = Color(0xFF2B180D),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40D4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SCORE", color = Color(0xFFBCAAA4), fontSize = 10.sp)
                            Text(text = "${data.score}", color = Color(0xFFFFD54F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "FRUITS", color = Color(0xFFBCAAA4), fontSize = 10.sp)
                            Text(text = "${data.fruits}/${data.stage.targetFruits}", color = Color(0xFF81C784), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "LOST", color = Color(0xFFBCAAA4), fontSize = 10.sp)
                            Text(text = "${data.lost}", color = Color(0xFFE57373), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (data.isNewRecord) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⭐ NEW STAGE HIGH SCORE! ⭐",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                if (data.stage.id < 16) {
                    val next = StageConfig.getStage(data.stage.id + 1)
                    Button(
                        onClick = onNextStage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4AF37),
                            contentColor = Color(0xFF1B0E05)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("next_stage_button")
                    ) {
                        Text(
                            text = "NEXT: STAGE ${next.id} (${next.title}) →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Button(
                        onClick = onRetryStage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color(0xFF1B0E05)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("replay_arena_button")
                    ) {
                        Text(
                            text = "👑 REPLAY FINAL DUAL ARENA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRetryStage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3E2516),
                            contentColor = Color(0xFFFFCCBC)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("retry_stage_button")
                    ) {
                        Text(text = "↺ RETRY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSelectOther,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2C190F),
                            contentColor = Color(0xFFFFE0B2)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("stages_dialog_menu_button")
                    ) {
                        Text(text = "STAGES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
