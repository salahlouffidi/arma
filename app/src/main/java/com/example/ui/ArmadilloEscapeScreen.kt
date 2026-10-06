package com.example.ui

import android.app.Activity
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
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ads.AdManager
import com.example.game.GameEngine
import com.example.game.HapticFeedbackHelper
import com.example.game.SoundManager
import com.example.game.StageConfig
import com.example.game.StageProgressManager
import com.example.game.MatchConfig
import com.example.game.CharacterId
import com.example.game.RoleType
import com.example.game.TableRenderer
import com.example.game.Vector2
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class GameScreenState {
    INTRO,
    MAIN_MENU,
    GAME_MODE_SELECT,
    MULTIPLAYER_CONFIG,
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
fun ArmadilloEscapeScreen(
    isInPip: Boolean = false,
    onEnterPip: (() -> Unit)? = null,
    onSetGameplayActive: ((Boolean) -> Unit)? = null,
    adManager: AdManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val effectiveAdManager = remember(adManager) { adManager ?: AdManager(context.applicationContext) }
    LaunchedEffect(effectiveAdManager) {
        effectiveAdManager.preload()
    }

    val haptics = remember { HapticFeedbackHelper(context) }
    val progressManager = remember { StageProgressManager(context) }
    val soundManager = remember { SoundManager(context) }

    var currentScreen by remember { mutableStateOf(GameScreenState.INTRO) }
    var activeMatchConfig by remember { mutableStateOf(MatchConfig.solo()) }
    var showAudioSettings by remember { mutableStateOf(false) }

    var highestUnlockedStage by remember { mutableIntStateOf(progressManager.getHighestUnlockedStage()) }
    var currentStageId by remember { mutableIntStateOf(1) }
    var showStageSelector by remember { mutableStateOf(false) }
    var stageClearDialogData by remember { mutableStateOf<StageClearData?>(null) }
    var gameOverDialogData by remember { mutableStateOf<GameOverData?>(null) }
    var adStatusMessage by remember { mutableStateOf<String?>(null) }
    var isGamePaused by remember { mutableStateOf(false) }
    var showInGameSettings by remember { mutableStateOf(false) }
    var inGameSettingsVersion by remember { mutableIntStateOf(0) }

    // Virtual Joystick Finger Input Vectors (Continuous Normalized: -1f..1f)
    // P1 (Golden Armadillo)
    var joystickInputX by remember { mutableFloatStateOf(0f) }
    var joystickInputY by remember { mutableFloatStateOf(0f) }
    // P2 (Blue Armadillo - Stages 9-16 or Multiplayer)
    var joystickInputX2 by remember { mutableFloatStateOf(0f) }
    var joystickInputY2 by remember { mutableFloatStateOf(0f) }
    // Snakes (Multiplayer human controls)
    var snake1InputX by remember { mutableFloatStateOf(0f) }
    var snake1InputY by remember { mutableFloatStateOf(0f) }
    var snake2InputX by remember { mutableFloatStateOf(0f) }
    var snake2InputY by remember { mutableFloatStateOf(0f) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                event == androidx.lifecycle.Lifecycle.Event.ON_STOP
            ) {
                if (currentScreen == GameScreenState.GAMEPLAY) {
                    isGamePaused = true
                    // Reset input vectors so the armadillo freezes in place
                    joystickInputX = 0f
                    joystickInputY = 0f
                    joystickInputX2 = 0f
                    joystickInputY2 = 0f
                    snake1InputX = 0f
                    snake1InputY = 0f
                    snake2InputX = 0f
                    snake2InputY = 0f
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentScreen) {
        onSetGameplayActive?.invoke(currentScreen == GameScreenState.GAMEPLAY)
    }

    LaunchedEffect(isInPip) {
        if (isInPip && currentScreen == GameScreenState.GAMEPLAY) {
            isGamePaused = true
            joystickInputX = 0f
            joystickInputY = 0f
            joystickInputX2 = 0f
            joystickInputY2 = 0f
            snake1InputX = 0f
            snake1InputY = 0f
            snake2InputX = 0f
            snake2InputY = 0f
        }
    }

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
            onGameOver = { stage, score, fruits, lost, time ->
                haptics.snakeCatch()
                soundManager.playGameOver()
                gameOverDialogData = GameOverData(
                    stage = stage,
                    score = score,
                    fruits = fruits,
                    lost = lost,
                    maxAllowedLosses = stage.maxAllowedLosses,
                    timeSeconds = time
                )
            }
        }
    }

    LaunchedEffect(currentStageId) {
        joystickInputX = 0f
        joystickInputY = 0f
        joystickInputX2 = 0f
        joystickInputY2 = 0f
        isGamePaused = false
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
        isGamePaused = false
        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
                lastTime = now
                frameTimeNanos = now

                if (!isGamePaused) {
                    // Direct Virtual Joystick Finger Input driving armadillo and snake physics
                    gameEngine.update(
                        dt = dt,
                        tiltX = joystickInputX,
                        tiltY = joystickInputY,
                        tiltX2 = joystickInputX2,
                        tiltY2 = joystickInputY2,
                        snake1InputX = snake1InputX,
                        snake1InputY = snake1InputY,
                        snake2InputX = snake2InputX,
                        snake2InputY = snake2InputY
                    )
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
                    activeMatchConfig = MatchConfig.solo()
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onOpenStageSelect = {
                    activeMatchConfig = MatchConfig.solo()
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onOpenAudioSettings = {
                    showAudioSettings = true
                },
                onOpenGameModeSelect = {
                    activeMatchConfig = MatchConfig.solo()
                    currentScreen = GameScreenState.STAGE_SELECT
                }
            )
        }
        GameScreenState.GAME_MODE_SELECT -> {
            GameModeSelectScreen(
                soundManager = soundManager,
                onSelectSolo = {
                    activeMatchConfig = MatchConfig.solo()
                    gameEngine.loadStage(StageConfig.getStage(currentStageId), activeMatchConfig)
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onSelectMultiplayer = {
                    currentScreen = GameScreenState.MULTIPLAYER_CONFIG
                },
                onBackToMenu = {
                    currentScreen = GameScreenState.MAIN_MENU
                }
            )
        }
        GameScreenState.MULTIPLAYER_CONFIG -> {
            MultiplayerModeScreen(
                soundManager = soundManager,
                initialConfig = activeMatchConfig,
                onConfirmMatchConfig = { config ->
                    activeMatchConfig = config
                    gameEngine.loadStage(StageConfig.getStage(currentStageId), config)
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onBackToGameModeSelect = {
                    currentScreen = GameScreenState.GAME_MODE_SELECT
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
                    val stage = StageConfig.getStage(stageId)
                    gameEngine.loadStage(stage, activeMatchConfig)
                    // Reset input vectors
                    joystickInputX = 0f
                    joystickInputY = 0f
                    joystickInputX2 = 0f
                    joystickInputY2 = 0f
                    snake1InputX = 0f
                    snake1InputY = 0f
                    snake2InputX = 0f
                    snake2InputY = 0f
                    currentScreen = GameScreenState.GAMEPLAY
                },
                onBackToMenu = {
                    if (activeMatchConfig.isSolo) {
                        currentScreen = GameScreenState.GAME_MODE_SELECT
                    } else {
                        currentScreen = GameScreenState.MULTIPLAYER_CONFIG
                    }
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
        val density = LocalDensity.current
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Fixed Table Geometry: Table occupies majority of the screen, staying completely fixed
        // tableMarginTop uses density so the arena begins cleanly underneath the single HUD row
        val tableMarginX = (screenWidth * 0.05f).coerceAtLeast(18f)
        val tableMarginTop = with(density) { 54.dp.toPx() }
        val tableMarginBottom = with(density) { 72.dp.toPx() }

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

        if (isInPip) {
            Surface(
                color = Color(0xD91E120A),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80D4AF37)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            ) {
                Text(
                    text = "⏸️ R${gameEngine.currentStage.id}: ${gameEngine.currentStage.title.uppercase()} • ${gameEngine.stageFruitsCollected}/${gameEngine.currentStage.targetFruits} 🍎 • LOST: ${gameEngine.stats.armadillosLost}/${gameEngine.currentStage.maxAllowedLosses}",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            // ==================== HUD: TOP BAR (SINGLE HORIZONTAL ROW) ====================
            val isAnyPowerActive = gameEngine.armadillo.isPowerActive || (gameEngine.armadillo2?.isPowerActive == true)
            val activePowerTimer = if (gameEngine.armadillo.isPowerActive) gameEngine.armadillo.powerTimer
                else (gameEngine.armadillo2?.powerTimer ?: 0f)
            val activePowerChances = if (gameEngine.armadillo.isPowerActive) gameEngine.armadillo.powerChancesRemaining
                else (gameEngine.armadillo2?.powerChancesRemaining ?: 0)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                // Base HUD Row (Always visible, perfectly stationary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT: Navigation & Round Selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back to Round Select
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    soundManager.playMenuClick()
                                    currentScreen = GameScreenState.STAGE_SELECT
                                }
                                .testTag("back_to_stages_button"),
                            color = Color(0xD92E1508),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80D4AF37))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Rounds",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "ROUNDS",
                                    color = Color(0xFFFFE0B2),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                    // Stage Selection Trigger
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 110.dp)
                            .clickable { showStageSelector = true }
                            .testTag("stage_badge_button"),
                        color = Color(0xD922130A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80D4AF37))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "R${gameEngine.currentStage.id}: ${gameEngine.currentStage.title.uppercase()}",
                                color = Color(0xFFFFE0B2),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "▾",
                                color = Color(0xFFFFD54F),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // CENTER: Goal, Score, Lost (ON THE SAME HORIZONTAL LINE AS R1: GARDEN)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stage Fruit Goal Tracker
                    Surface(
                        color = Color(0xD922130A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6081C784))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GOAL: ",
                                color = Color(0xFFC8E6C9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${gameEngine.stageFruitsCollected}/${gameEngine.currentStage.targetFruits} 🍎",
                                color = Color(0xFFFFD54F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Score Pill
                    Surface(
                        modifier = Modifier.scale(scoreScale.value),
                        color = Color(0xE62A170C),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7043))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCORE: ",
                                color = Color(0xFFFFCCBC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${gameEngine.stats.score}",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Armadillos Lost Pill
                    Surface(
                        color = Color(0xE6231208),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6081C784))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOST: ",
                                color = Color(0xFFC8E6C9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${gameEngine.stats.armadillosLost}/${gameEngine.currentStage.maxAllowedLosses}",
                                color = if (gameEngine.stats.armadillosLost >= gameEngine.currentStage.maxAllowedLosses) Color(0xFFFF5252) else Color(0xFFE57373),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // RIGHT: Settings, Pause (Fixed compact 40dp icon-only controls)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Settings Button (Fixed 40dp compact icon-only control)
                    Surface(
                        color = Color(0xE62A170C),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable {
                                soundManager.playMenuClick()
                                isGamePaused = true
                                showInGameSettings = true
                            }
                            .testTag("hud_settings_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Pause Button (Fixed 40dp compact icon-only control)
                    PauseButton(
                        isPaused = isGamePaused,
                        onClick = {
                            soundManager.playMenuClick()
                            isGamePaused = !isGamePaused
                        }
                    )
                }
            }

            // OVERDRIVE Independent Overlay in Dedicated Top Position
            // Positioned horizontally directly to the left of Settings and Pause in the single top row.
            // When active/inactive, base HUD components remain 100% stationary.
            OverdriveStatusPill(
                isPowerActive = isAnyPowerActive,
                powerTimer = activePowerTimer,
                powerChancesRemaining = activePowerChances,
                fruitsCollectedSinceEvent = gameEngine.fruitsCollectedSinceEvent,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 88.dp)
            )
        }


        // ==================== VIRTUAL JOYSTICKS ====================
        if (activeMatchConfig.isSolo) {
            val isDual = gameEngine.isDualArmadillo
            if (isDual) {
                // Dual Armadillo Joysticks: Left = Golden, Right = Blue
                VirtualJoystick(
                    modifier = Modifier
                        .align(AbsoluteAlignment.BottomLeft)
                        .absolutePadding(left = 18.dp, bottom = 12.dp),
                    size = 122.dp,
                    label = "GOLDEN",
                    isBlueTheme = false,
                    testTag = "virtual_joystick_p1",
                    onMove = { normX, normY ->
                        joystickInputX = normX
                        joystickInputY = normY
                    }
                )

                VirtualJoystick(
                    modifier = Modifier
                        .align(AbsoluteAlignment.BottomRight)
                        .absolutePadding(right = 18.dp, bottom = 12.dp),
                    size = 122.dp,
                    label = "BLUE",
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
                                text = "LEFT: GOLDEN  |  RIGHT: BLUE",
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
                            text = "🏆 ROUNDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else {
                // Single Joystick (Rounds 1–8) with per-round independent LEFT/RIGHT position
                val activeRoundId = gameEngine.currentStage.id
                val isSingleJoystickRight = remember(activeRoundId, currentStageId, currentScreen, showStageSelector, inGameSettingsVersion) {
                    context.getSharedPreferences("armasnake_settings", 0)
                        .getBoolean("single_joystick_right_round_$activeRoundId", false)
                }

                VirtualJoystick(
                    modifier = Modifier
                        .align(if (isSingleJoystickRight) AbsoluteAlignment.BottomRight else AbsoluteAlignment.BottomLeft)
                        .absolutePadding(
                            left = if (isSingleJoystickRight) 0.dp else 22.dp,
                            right = if (isSingleJoystickRight) 22.dp else 0.dp,
                            bottom = 12.dp
                        ),
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
                    if (isSingleJoystickRight) {
                        // When joystick is on the right, place ROUNDS button on the left
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
                                text = "🏆 ROUNDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

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

                        // Space reservation for the right-aligned VirtualJoystick
                        Spacer(modifier = Modifier.width(140.dp))
                    } else {
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

                        // Central / Right Action: ROUNDS Button
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
                                text = "🏆 ROUNDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            // ==================== MULTIPLAYER ACTIVE CONTROLS ====================
            val slotA1 = activeMatchConfig.getSlot(CharacterId.ARMADILLO_1)
            val slotA2 = activeMatchConfig.getSlot(CharacterId.ARMADILLO_2)
            val slotS1 = activeMatchConfig.getSlot(CharacterId.SNAKE_1)
            val slotS2 = activeMatchConfig.getSlot(CharacterId.SNAKE_2)
            val humanCount = activeMatchConfig.activeHumanPlayers().size

            // 1. Armadillo 1 Human Joystick
            if (slotA1.roleType == RoleType.HUMAN) {
                val isTop = humanCount >= 3 && slotA2.roleType == RoleType.HUMAN
                VirtualJoystick(
                    modifier = Modifier
                        .align(if (isTop) Alignment.TopStart else Alignment.BottomStart)
                        .padding(start = 16.dp, top = if (isTop) 64.dp else 0.dp, bottom = if (isTop) 0.dp else 12.dp),
                    size = if (humanCount >= 3) 112.dp else 122.dp,
                    label = "P${slotA1.playerNumber ?: 1} ARMADILLO 1",
                    primaryColor = Color(0xFFFFB300),
                    accentColor = Color(0xFFFF6F00),
                    testTag = "multi_joystick_a1",
                    onMove = { x, y -> joystickInputX = x; joystickInputY = y }
                )
            }

            // 2. Armadillo 2 Human Joystick
            if (slotA2.roleType == RoleType.HUMAN) {
                VirtualJoystick(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 12.dp),
                    size = if (humanCount >= 3) 112.dp else 122.dp,
                    label = "P${slotA2.playerNumber ?: 2} ARMADILLO 2",
                    primaryColor = Color(0xFF29B6F6),
                    accentColor = Color(0xFF0288D1),
                    testTag = "multi_joystick_a2",
                    onMove = { x, y -> joystickInputX2 = x; joystickInputY2 = y }
                )
            }

            // 3. Snake 1 Human Joystick
            if (slotS1.roleType == RoleType.HUMAN) {
                val isTop = humanCount >= 3 && slotS2.roleType == RoleType.HUMAN
                VirtualJoystick(
                    modifier = Modifier
                        .align(if (isTop) Alignment.TopEnd else Alignment.BottomEnd)
                        .padding(end = 16.dp, top = if (isTop) 64.dp else 0.dp, bottom = if (isTop) 0.dp else 12.dp),
                    size = if (humanCount >= 3) 112.dp else 122.dp,
                    label = "P${slotS1.playerNumber ?: 1} SNAKE 1",
                    primaryColor = Color(0xFF00E676),
                    accentColor = Color(0xFF00B0FF),
                    testTag = "multi_joystick_s1",
                    onMove = { x, y -> snake1InputX = x; snake1InputY = y }
                )
            }

            // 4. Snake 2 Human Joystick
            if (slotS2.roleType == RoleType.HUMAN) {
                VirtualJoystick(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp),
                    size = if (humanCount >= 3) 112.dp else 122.dp,
                    label = "P${slotS2.playerNumber ?: 2} SNAKE 2",
                    primaryColor = Color(0xFFAB47BC),
                    accentColor = Color(0xFFE91E63),
                    testTag = "multi_joystick_s2",
                    onMove = { x, y -> snake2InputX = x; snake2InputY = y }
                )
            }

            // ==================== MULTIPLAYER BOTTOM BAR ====================
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 135.dp, vertical = 8.dp),
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
                            text = "${activeMatchConfig.preset.icon} ${activeMatchConfig.preset.title.uppercase()}",
                            color = Color(0xFFFFE0B2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            currentScreen = GameScreenState.MULTIPLAYER_CONFIG
                            haptics.click()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF261408),
                            contentColor = Color(0xFFFFE0B2)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("change_mode_button")
                    ) {
                        Text(
                            text = "⚙️ MODES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
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
                            text = "🏆 ROUNDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
        }

        // ==================== STAGE SELECTION DIALOG ====================
        if (showStageSelector && !isInPip) {
            StageSelectionDialog(
                currentStageId = currentStageId,
                highestUnlockedStage = highestUnlockedStage,
                progressManager = progressManager,
                onSelectStage = { stageId ->
                    currentStageId = stageId
                    val config = StageConfig.getStage(stageId)
                    gameEngine.loadStage(config, activeMatchConfig)
                    showStageSelector = false
                },
                onDismiss = { showStageSelector = false }
            )
        }

        // ==================== STAGE CLEAR CELEBRATION DIALOG ====================
        stageClearDialogData?.takeIf { !isInPip }?.let { clearData ->
            StageClearDialog(
                data = clearData,
                onNextStage = {
                    val nextId = (clearData.stage.id + 1).coerceAtMost(16)
                    currentStageId = nextId
                    val config = StageConfig.getStage(nextId)
                    gameEngine.loadStage(config, activeMatchConfig)
                    stageClearDialogData = null
                },
                onRetryStage = {
                    val retryId = clearData.stage.id
                    val config = StageConfig.getStage(retryId)
                    gameEngine.resetRound()
                    stageClearDialogData = null
                },
                onSelectOther = {
                    stageClearDialogData = null
                    currentScreen = GameScreenState.STAGE_SELECT
                }
            )
        }

        // ==================== GAME OVER DIALOG ====================
        gameOverDialogData?.takeIf { !isInPip }?.let { goData ->
            GameOverDialog(
                data = goData,
                statusMessage = adStatusMessage,
                onRetryRound = {
                    val activity = context as? Activity
                    gameOverDialogData = null
                    adStatusMessage = null
                    val doResetAndStart = {
                        joystickInputX = 0f
                        joystickInputY = 0f
                        joystickInputX2 = 0f
                        joystickInputY2 = 0f
                        snake1InputX = 0f
                        snake1InputY = 0f
                        snake2InputX = 0f
                        snake2InputY = 0f
                        isGamePaused = false
                        gameEngine.resetRound()
                    }

                    if (activity != null) {
                        effectiveAdManager.showInterstitial(
                            activity = activity,
                            onFinished = {
                                doResetAndStart()
                            }
                        )
                    } else {
                        doResetAndStart()
                    }
                },
                onWatchAdSecondChance = {
                    val activity = context as? Activity
                    if (activity != null) {
                        effectiveAdManager.showRewarded(
                            activity = activity,
                            onRewardEarned = {
                                gameOverDialogData = null
                                adStatusMessage = null
                                joystickInputX = 0f
                                joystickInputY = 0f
                                joystickInputX2 = 0f
                                joystickInputY2 = 0f
                                snake1InputX = 0f
                                snake1InputY = 0f
                                snake2InputX = 0f
                                snake2InputY = 0f
                                isGamePaused = false
                                gameEngine.resetRound()
                            },
                            onUnavailable = {
                                adStatusMessage = "Rewarded test ad unavailable. Tap RETRY!"
                            }
                        )
                    } else {
                        adStatusMessage = "Rewarded ad unavailable. Tap RETRY!"
                    }
                },
                onExitToRounds = {
                    gameOverDialogData = null
                    adStatusMessage = null
                    currentScreen = GameScreenState.STAGE_SELECT
                }
            )
        }

        // ==================== AUDIO SETTINGS DIALOG ====================
        if (showAudioSettings && !isInPip) {
            AudioSettingsDialog(
                soundManager = soundManager,
                onDismiss = { showAudioSettings = false }
            )
        }

        // ==================== IN-GAME SETTINGS DIALOG ====================
        if (showInGameSettings && !isInPip) {
            InGameSettingsDialog(
                roundId = currentStageId,
                isDualArmadillo = gameEngine.isDualArmadillo,
                soundManager = soundManager,
                onControlChanged = {
                    inGameSettingsVersion++
                },
                onResume = {
                    showInGameSettings = false
                    isGamePaused = false
                },
                onDismiss = {
                    showInGameSettings = false
                },
                onMinimize = onEnterPip
            )
        }

        // ==================== PAUSE OVERLAY ====================
        if (isGamePaused && !isInPip && !showInGameSettings && stageClearDialogData == null && !showStageSelector && gameOverDialogData == null) {
            PauseOverlay(
                stage = gameEngine.currentStage,
                onResume = { isGamePaused = false },
                onSettings = { showInGameSettings = true },
                onRestart = {
                    gameEngine.resetRound()
                    isGamePaused = false
                },
                onExitToStages = {
                    isGamePaused = false
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onMinimize = onEnterPip
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
                        text = "🏆 16-ROUND JOURNEY",
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
                        val isUnlocked = progressManager.isStageUnlocked(stage.id)
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
                                        text = if (isUnlocked) "ROUND ${stage.id.toString().padStart(2, '0')}" else "🔒 R${stage.id}",
                                        color = if (isUnlocked) Color(0xFFFFD54F) else Color(0xFF666666),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(72.dp)
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
                                            text = "${stage.targetFruits} 🍎 • ${stage.maxAllowedLosses} 💔",
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
                    text = if (data.stage.id == 16) "👑 16-ROUND DUAL MASTER! 👑" else if (data.stage.id == 8) "🌟 ORIGINAL CAMPAIGN CLEARED! 🌟" else "🎉 ROUND ${data.stage.id} CLEARED!",
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
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SCORE", color = Color(0xFFBCAAA4), fontSize = 10.sp)
                            Text(text = "${data.score}", color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "FRUITS", color = Color(0xFFBCAAA4), fontSize = 10.sp)
                            Text(text = "${data.fruits}/${data.stage.targetFruits}", color = Color(0xFF81C784), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "LOST", color = Color(0xFFBCAAA4), fontSize = 10.sp)
                            Text(text = "${data.lost}/${data.stage.maxAllowedLosses}", color = Color(0xFFE57373), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "TIME", color = Color(0xFFBCAAA4), fontSize = 10.sp)
                            Text(text = formatGameTime(data.timeSeconds), color = Color(0xFFFFE0B2), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val performanceRating = when {
                    data.lost == 0 -> "★★★ PERFECT"
                    data.lost == 1 -> "★★☆ EXCELLENT"
                    data.lost < data.stage.maxAllowedLosses -> "★★☆ GREAT"
                    else -> "★☆☆ SURVIVED"
                }
                Surface(
                    color = Color(0xFF381F0E),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37))
                ) {
                    Text(
                        text = performanceRating,
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (data.isNewRecord) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⭐ NEW ROUND HIGH SCORE! ⭐",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            text = "NEXT: ROUND ${next.id} (${next.title}) →",
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
                        Text(text = "↺ RETRY ROUND", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        Text(text = "🏆 ROUNDS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatGameTime(totalSeconds: Float): String {
    val sec = totalSeconds.toInt()
    val minutes = sec / 60
    val seconds = sec % 60
    val mStr = if (minutes < 10) "0$minutes" else "$minutes"
    val sStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$mStr:$sStr"
}

data class GameOverData(
    val stage: StageConfig,
    val score: Int,
    val fruits: Int,
    val lost: Int,
    val maxAllowedLosses: Int,
    val timeSeconds: Float
)

@Composable
private fun GameOverDialog(
    data: GameOverData,
    statusMessage: String? = null,
    onRetryRound: () -> Unit,
    onWatchAdSecondChance: () -> Unit,
    onExitToRounds: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF71A0A04),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE53935)),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 4.dp)
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Banner
                Surface(
                    color = Color(0xFF3B1008),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💀 GAME OVER 💀",
                        color = Color(0xFFFF5252),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Responsive 2-column landscape layout: Left = Stats, Right = Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // LEFT COLUMN: Round info & Stats Card
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ROUND ${data.stage.id.toString().padStart(2, '0')}",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "• ${data.stage.title.uppercase()}",
                                color = Color(0xFFFFCCBC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Performance Summary Grid Card
                        Surface(
                            color = Color(0xFF241006),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ARMADILLOS LOST",
                                        color = Color(0xFFBCAAA4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${data.lost} / ${data.maxAllowedLosses} 💔",
                                        color = Color(0xFFFF5252),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SCORE",
                                        color = Color(0xFFBCAAA4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${data.score}",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "FRUITS COLLECTED",
                                        color = Color(0xFFBCAAA4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${data.fruits} / ${data.stage.targetFruits} 🍎",
                                        color = Color(0xFF81C784),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TIME SURVIVED",
                                        color = Color(0xFFBCAAA4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatGameTime(data.timeSeconds),
                                        color = Color(0xFFFFE0B2),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!statusMessage.isNullOrEmpty()) {
                            Text(
                                text = statusMessage,
                                color = Color(0xFFFF8A65),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // RIGHT COLUMN: Action Buttons
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Primary RETRY button (Prominent, Gold, Always Visible)
                        Button(
                            onClick = onRetryRound,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4AF37),
                                contentColor = Color(0xFF1B0E05)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("game_over_retry_button")
                        ) {
                            Text(
                                text = "↻ RETRY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // 2. Optional Rewarded Ad Button (Second Chance)
                        Button(
                            onClick = onWatchAdSecondChance,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B4332),
                                contentColor = Color(0xFFD8F3DC)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF52B788)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("game_over_watch_ad_button")
                        ) {
                            Text(
                                text = "▶ WATCH AD — SECOND CHANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // 3. Secondary ROUNDS button
                        Button(
                            onClick = onExitToRounds,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C190F),
                                contentColor = Color(0xFFFFD54F)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("game_over_exit_button")
                        ) {
                            Text(
                                text = "🏆 ROUNDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
