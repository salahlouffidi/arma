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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.game.WorldRenderer
import androidx.compose.ui.graphics.drawscope.withTransform
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
    val textMeasurer = rememberTextMeasurer()

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
    var showFirstRunHint by remember { mutableStateOf(true) }
    var showHowToPlay by remember { mutableStateOf(false) }

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

    // First-run teaching hint: show briefly whenever Round 1 gameplay begins, then auto-dismiss.
    LaunchedEffect(currentScreen, currentStageId) {
        if (currentScreen == GameScreenState.GAMEPLAY && currentStageId == 1) {
            showFirstRunHint = true
            kotlinx.coroutines.delay(7000)
            showFirstRunHint = false
        } else {
            showFirstRunHint = false
        }
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
            },
            onComboMilestone = { combo ->
                haptics.appleEat()
                soundManager.playComboMilestone(combo)
            },
            onDiamondCollected = {
                haptics.powerActivated()
                soundManager.playDiamond()
            },
            onMagnetCollected = {
                haptics.powerActivated()
                soundManager.playMagnet()
            },
            onRainbowCollected = {
                haptics.powerActivated()
                soundManager.playRainbow()
            },
            onRareFruitSpawned = {
                soundManager.playRareSpawn()
            },
            onShieldCollected = {
                haptics.powerActivated()
                soundManager.playShield()
            },
            onFreezeCollected = {
                haptics.powerActivated()
                soundManager.playFreeze()
            },
            onGoldenCollected = {
                haptics.powerActivated()
                soundManager.playGolden()
            },
            onShieldBlocked = {
                haptics.powerProtectedHit()
                soundManager.playProtectionHit()
            }
        ).apply {
            onStageCompleted = { stage, score, fruits, lost, time ->
                val nextWasUnlocked = stage.id >= 16 || progressManager.isStageUnlocked(stage.id + 1)
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
                    isNewRecord = isNewRecord,
                    bestScore = progressManager.getStageHighScore(stage.id),
                    bestCombo = this@apply.bestComboMultiplier,
                    newlyUnlocked = if (!nextWasUnlocked && progressManager.isStageUnlocked(stage.id + 1)) StageConfig.getStage(stage.id + 1) else null
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
                    timeSeconds = time,
                    bestScore = progressManager.getStageHighScore(stage.id)
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

                if (stageClearDialogData != null || gameOverDialogData != null) {
                    // Round over: the world is frozen behind the result screen, but particles,
                    // floating labels and camera reactions keep settling (cinematic, not instant).
                    gameEngine.updateEffectsOnly(dt)
                    tick++
                } else if (!isGamePaused) {
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

    // Top-level Screen Router — screens crossfade into each other (no hard cuts / black frames).
    androidx.compose.animation.Crossfade(
        targetState = currentScreen,
        animationSpec = tween(420),
        label = "screenRouter"
    ) { screen ->
    when (screen) {
        GameScreenState.INTRO -> {
            IntroScreen(
                soundManager = soundManager,
                onFinished = {
                    currentScreen = GameScreenState.MAIN_MENU
                }
            )
        }
        GameScreenState.MAIN_MENU -> {
            ConsoleHomeScreen(
                currentStageId = currentStageId,
                highestUnlocked = highestUnlockedStage,
                soundManager = soundManager,
                onPlay = {
                    // Dominant action: jump straight into the selected (or furthest unlocked) round.
                    val playId = if (progressManager.isStageUnlocked(currentStageId)) currentStageId else highestUnlockedStage
                    currentStageId = playId
                    activeMatchConfig = MatchConfig.solo()
                    gameEngine.loadStage(StageConfig.getStage(playId), activeMatchConfig)
                    joystickInputX = 0f
                    joystickInputY = 0f
                    joystickInputX2 = 0f
                    joystickInputY2 = 0f
                    currentScreen = GameScreenState.GAMEPLAY
                },
                onRounds = {
                    activeMatchConfig = MatchConfig.solo()
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onHowToPlay = { showHowToPlay = true },
                onSettings = { showAudioSettings = true },
                onMultiplayer = { currentScreen = GameScreenState.GAME_MODE_SELECT }
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
            RoundLibraryScreen(
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
                onBack = {
                    if (activeMatchConfig.isSolo) {
                        currentScreen = GameScreenState.MAIN_MENU
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

        // Slow celebratory camera pull-back while the Round Complete screen is up
        val celebrateZoom by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (stageClearDialogData != null) 1f else 0f,
            animationSpec = tween(1600),
            label = "celebrateZoom"
        )

        // ==================== FIXED 3D TABLETOP CANVAS ====================
        // The table, background, camera NEVER rotate with phone motion
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_board_canvas")
        ) {
            // Read tick to trigger redraw every frame
            @Suppress("UNUSED_VARIABLE")
            val currentTick = tick
            val stageTheme = gameEngine.currentStage.theme
            val animTime = ((frameTimeNanos / 1_000_000L) % 600_000L) / 1000f

            // 0. Full procedural world behind the arena; gentle parallax follows the player
            val camX = (gameEngine.armadillo.pos.x / gameEngine.boardHalfWidth).coerceIn(-1f, 1f)
            WorldRenderer.drawWorld(
                this, size.width, size.height, stageTheme, animTime,
                parallax = -camX * 0.35f,
                detail = 1
            )

            // Camera: controlled shake + zoom impulse (+ slow celebratory pull-back on round clear)
            val shake = gameEngine.cameraShake
            val wc = if (shake > 0.05f) {
                Offset(
                    tableCenter.x + kotlin.math.sin(tick * 1.7f) * shake,
                    tableCenter.y + kotlin.math.cos(tick * 2.3f) * shake
                )
            } else tableCenter
            val zoom = 1f + gameEngine.cameraZoomPulse - 0.05f * celebrateZoom

            withTransform({ scale(zoom, zoom, wc) }) {
                // 1. Themed table + arena surface details
                TableRenderer.drawTable(scope = this, center = wc, halfW = tableHalfW, halfH = tableHalfH, theme = stageTheme)
                WorldRenderer.drawArenaDecor(this, wc, tableHalfW, tableHalfH, stageTheme, animTime)

                // 1a. Environment reactions: glow rim, frost, rare-fruit dim
                TableRenderer.drawWorldPulse(
                    scope = this,
                    center = wc,
                    halfW = tableHalfW,
                    halfH = tableHalfH,
                    pulse = gameEngine.worldPulse,
                    color = gameEngine.worldPulseColor
                )
                TableRenderer.drawFreezeOverlay(this, wc, tableHalfW, tableHalfH, gameEngine.freezeTimer, GameEngine.FREEZE_DURATION)
                TableRenderer.drawRareDim(this, size.width, size.height, gameEngine.rareAlertTimer, GameEngine.RARE_ALERT_DURATION)

                // 1b. Obstacles
                TableRenderer.drawObstacles(scope = this, obstacles = gameEngine.obstacles, boardCenter = wc)

                // 2. Fruit
                TableRenderer.drawApples(scope = this, apples = gameEngine.apples, boardCenter = wc)

                // 3. Armadillos
                TableRenderer.drawArmadillo(scope = this, armadillo = gameEngine.armadillo, boardCenter = wc, isBlue = false)
                val a2 = gameEngine.armadillo2
                if (a2 != null) {
                    TableRenderer.drawArmadillo(scope = this, armadillo = a2, boardCenter = wc, isBlue = true)
                }

                // 4. Snakes (icy while FREEZE is active)
                TableRenderer.drawSnakes(scope = this, snakes = gameEngine.snakes, boardCenter = wc, frozen = gameEngine.freezeTimer > 0f)

                // 5. Particles
                TableRenderer.drawParticles(scope = this, particles = gameEngine.particles, boardCenter = wc)

                // 5b. Rare power-up beacon: pulsing ring + bobbing chevron pointing at it
                val rarePos = gameEngine.activeRareFruitPos
                if (rarePos != null) {
                    val bx = wc.x + rarePos.x
                    val by = wc.y + rarePos.y
                    val pulse = (kotlin.math.sin(tick * 0.2f) * 0.5f + 0.5f)
                    drawCircle(
                        color = Color(0xFFFFEA00).copy(alpha = 0.25f + 0.35f * pulse),
                        radius = 34f + 10f * pulse,
                        center = Offset(bx, by),
                        style = Stroke(width = 2.5f)
                    )
                    val chevY = by - 48f - 6f * pulse
                    val chevron = Path().apply {
                        moveTo(bx - 8f, chevY)
                        lineTo(bx, chevY + 9f)
                        lineTo(bx + 8f, chevY)
                    }
                    drawPath(chevron, color = Color(0xFFFFEA00).copy(alpha = 0.85f), style = Stroke(width = 3f))
                }

                // 5c. Combo badge floating above the player
                val comboArm = if (!gameEngine.armadillo.isEaten) gameEngine.armadillo else a2
                if (comboArm != null && !comboArm.isEaten) {
                    TableRenderer.drawComboBadge(
                        this, textMeasurer,
                        Offset(wc.x + comboArm.pos.x, wc.y + comboArm.pos.y),
                        gameEngine.comboMultiplier(),
                        animTime
                    )
                }

                // 6. Floating "+N" / combo score labels
                TableRenderer.drawFloatingTexts(scope = this, texts = gameEngine.floatingTexts, boardCenter = wc, measurer = textMeasurer)
            }

            // 7. Screen-space danger cue (subtle red edge glow when a snake is close)
            TableRenderer.drawDangerEdge(this, size.width, size.height, gameEngine.dangerLevel)
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
            // Full-screen overlay layer so each console overlay can align anywhere on screen.
            Box(modifier = Modifier.fillMaxSize()) {
            // ==================== CONSOLE HUD (minimal, contextual) ====================
            // Each overlay reads the frame counter itself, so only these small scopes
            // recompose per frame — the world canvas and joysticks are unaffected.
            ConsoleHud(
                engine = gameEngine,
                frame = { tick },
                onPause = {
                    soundManager.playMenuClick()
                    isGamePaused = !isGamePaused
                },
                onSettings = {
                    soundManager.playMenuClick()
                    isGamePaused = true
                    showInGameSettings = true
                }
            )
            PowerChips(engine = gameEngine, frame = { tick })
            RareFruitToast(engine = gameEngine, frame = { tick })
            ConsoleBanner(engine = gameEngine, frame = { tick })
            CinematicRoundIntro(engine = gameEngine, frame = { tick })
            FirstRunHint(visible = gameEngine.currentStage.id == 1 && showFirstRunHint)
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
            RoundCompleteOverlay(
                data = clearData,
                onContinue = {
                    val nextId = (clearData.stage.id + 1).coerceAtMost(16)
                    currentStageId = nextId
                    val config = StageConfig.getStage(nextId)
                    gameEngine.loadStage(config, activeMatchConfig)
                    stageClearDialogData = null
                },
                onReplay = {
                    val retryId = clearData.stage.id
                    val config = StageConfig.getStage(retryId)
                    gameEngine.resetRound()
                    stageClearDialogData = null
                },
                onRoundSelect = {
                    stageClearDialogData = null
                    currentScreen = GameScreenState.STAGE_SELECT
                }
            )
        }

        // ==================== GAME OVER DIALOG ====================
        gameOverDialogData?.takeIf { !isInPip }?.let { goData ->
            GameOverOverlay(
                data = goData,
                statusMessage = adStatusMessage,
                onRetry = {
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
                onWatchAd = {
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
                onRoundSelect = {
                    gameOverDialogData = null
                    adStatusMessage = null
                    currentScreen = GameScreenState.STAGE_SELECT
                },
                onHome = {
                    gameOverDialogData = null
                    adStatusMessage = null
                    currentScreen = GameScreenState.MAIN_MENU
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

    // ==================== GLOBAL DIALOGS (available from any screen) ====================
    // The in-game audio dialog lives in the gameplay branch; menus get their own instance here.
    if (showAudioSettings && currentScreen != GameScreenState.GAMEPLAY) {
        AudioSettingsDialog(
            soundManager = soundManager,
            onDismiss = { showAudioSettings = false }
        )
    }
    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
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

private fun formatGameTime(totalSeconds: Float): String {
    val sec = totalSeconds.toInt()
    val minutes = sec / 60
    val seconds = sec % 60
    val mStr = if (minutes < 10) "0$minutes" else "$minutes"
    val sStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$mStr:$sStr"
}

