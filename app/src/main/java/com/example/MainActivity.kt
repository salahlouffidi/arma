package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.game.GameEngine
import com.example.game.HapticHelper
import com.example.game.ScreenState
import com.example.game.SoundManager
import com.example.ui.ArmadilloEscapeScreen
import com.example.ui.GameOverDialog
import com.example.ui.MainMenuScreen
import com.example.ui.PauseOverlay
import com.example.ui.StageCompleteDialog
import com.example.ui.theme.ArmadilloEscapeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val soundManager = SoundManager()
        val hapticHelper = HapticHelper(this)
        val gameEngine = GameEngine(soundManager, hapticHelper)

        setContent {
            ArmadilloEscapeTheme {
                ArmadilloEscapeApp(
                    engine = gameEngine,
                    soundManager = soundManager,
                    hapticHelper = hapticHelper
                )
            }
        }
    }
}

@Composable
fun ArmadilloEscapeApp(
    engine: GameEngine,
    soundManager: SoundManager,
    hapticHelper: HapticHelper
) {
    BackHandler(enabled = engine.screenState != ScreenState.MENU) {
        when (engine.screenState) {
            ScreenState.PLAYING -> engine.pauseGame()
            ScreenState.PAUSED -> engine.screenState = ScreenState.MENU
            ScreenState.GAME_OVER, ScreenState.STAGE_CLEAR -> engine.screenState = ScreenState.MENU
            ScreenState.MENU -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (engine.screenState) {
            ScreenState.MENU -> {
                MainMenuScreen(
                    engine = engine,
                    soundManager = soundManager,
                    hapticHelper = hapticHelper,
                    onStartGame = { mode, stage ->
                        engine.startNewGame(mode, stage)
                    }
                )
            }
            ScreenState.PLAYING -> {
                ArmadilloEscapeScreen(engine = engine)
            }
            ScreenState.PAUSED -> {
                ArmadilloEscapeScreen(engine = engine)
                PauseOverlay(
                    engine = engine,
                    soundManager = soundManager,
                    hapticHelper = hapticHelper,
                    onResume = { engine.resumeGame() },
                    onRestart = { engine.startNewGame(engine.currentMode, engine.currentStage) },
                    onMainMenu = { engine.screenState = ScreenState.MENU }
                )
            }
            ScreenState.GAME_OVER -> {
                ArmadilloEscapeScreen(engine = engine)
                GameOverDialog(
                    engine = engine,
                    onRestart = { engine.startNewGame(engine.currentMode, engine.currentStage) },
                    onMainMenu = { engine.screenState = ScreenState.MENU }
                )
            }
            ScreenState.STAGE_CLEAR -> {
                ArmadilloEscapeScreen(engine = engine)
                StageCompleteDialog(
                    engine = engine,
                    onNextStage = { engine.nextStage() },
                    onMainMenu = { engine.screenState = ScreenState.MENU }
                )
            }
        }
    }
}
