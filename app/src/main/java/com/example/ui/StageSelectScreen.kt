package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.SoundManager
import com.example.game.StageConfig
import com.example.game.StageProgressManager

private const val CONTROL_PREFS = "armasnake_settings"

@Composable
fun StageSelectScreen(
    currentStageId: Int,
    progressManager: StageProgressManager,
    soundManager: SoundManager,
    onSelectStage: (stageId: Int) -> Unit,
    onBackToMenu: () -> Unit
) {
    var selectedFilter by remember { mutableIntStateOf(0) }
    var controlSettingsRound by remember { mutableIntStateOf(0) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(CONTROL_PREFS, 0) }

    LaunchedEffect(Unit) { soundManager.startMusic() }
    DisposableEffect(Unit) { onDispose { soundManager.stopMusic() } }

    val displayedStages = when (selectedFilter) {
        1 -> StageConfig.allStages.filter { !it.isDualArmadillo }
        2 -> StageConfig.allStages.filter { it.isDualArmadillo }
        else -> StageConfig.allStages
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF221107), Color(0xFF160B04), Color(0xFF0C0502))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("round_select_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onBackToMenu()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF331A0B), contentColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier.height(36.dp).testTag("round_select_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MENU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "🏆 16 ROUNDS • ALL UNLOCKED",
                    color = Color(0xFFFFD700), fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ALL (16)", "SOLO (01–08)", "DUAL (09–16)").forEachIndexed { index, label ->
                        val isSel = selectedFilter == index
                        Surface(
                            color = if (isSel) Color(0xFFD4AF37) else Color(0xFF261408),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFFFFD700) else Color(0x40D4AF37)),
                            modifier = Modifier.clickable {
                                soundManager.playMenuClick()
                                selectedFilter = index
                            }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color(0xFF1F0E04) else Color(0xFFFFE0B2),
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedStages) { stage ->
                    val isCurrent = stage.id == currentStageId
                    val isCompleted = progressManager.isStageCompleted(stage.id)
                    val highScore = progressManager.getStageHighScore(stage.id)

                    RoundCard(
                        stage = stage,
                        isCurrent = isCurrent,
                        isCompleted = isCompleted,
                        highScore = highScore,
                        preferences = preferences,
                        settingsVersion = settingsVersion,
                        onSettings = if (!stage.isDualArmadillo) {
                            { controlSettingsRound = stage.id }
                        } else null,
                        onClick = {
                            soundManager.playStageSelected()
                            soundManager.stopMusic()
                            preferences.edit().putInt("active_single_joystick_stage_id", stage.id).apply()
                            onSelectStage(stage.id)
                        }
                    )
                }
            }
        }
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
private fun RoundCard(
    stage: StageConfig,
    isCurrent: Boolean,
    isCompleted: Boolean,
    highScore: Int,
    preferences: android.content.SharedPreferences,
    settingsVersion: Int,
    onSettings: (() -> Unit)?,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCurrent -> Color(0xFFFFD700)
        isCompleted -> Color(0xFF81C784)
        else -> Color(0x60D4AF37)
    }
    val bgColor = when {
        isCurrent -> Color(0xFF45220C)
        isCompleted -> Color(0xFF26190F)
        else -> Color(0xFF1E120A)
    }

    val isJoystickRight = if (!stage.isDualArmadillo) {
        preferences.getBoolean("single_joystick_right_round_${stage.id}", false)
    } else false

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isCurrent) 6.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.testTag("round_card_${stage.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stage.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ROUND ${stage.id.toString().padStart(2, '0')}",
                        color = if (isCurrent) Color(0xFFFFD54F) else Color(0xFFFFCCBC),
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stage.isDualArmadillo) {
                        Surface(
                            color = Color(0xFF1565C0), shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "DUAL • SOLO", color = Color(0xFFE3F2FD), fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else if (onSettings != null) {
                        Surface(
                            color = if (isJoystickRight) Color(0xFF3E2723) else Color(0xFF261408),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, if (isJoystickRight) Color(0xFFFFB74D) else Color(0x80D4AF37)),
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Text(
                                text = if (isJoystickRight) "RIGHT ▶" else "◀ LEFT",
                                color = if (isJoystickRight) Color(0xFFFFCC80) else Color(0xFFFFE082),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = onSettings,
                            modifier = Modifier.size(28.dp).testTag("round_${stage.id}_settings")
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "ROUND ${stage.id} settings",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isCompleted) {
                        Surface(color = Color(0xFF1B5E20), shape = RoundedCornerShape(6.dp)) {
                            Icon(
                                Icons.Default.Check, contentDescription = "Completed",
                                tint = Color(0xFFB9F6CA), modifier = Modifier.size(14.dp).padding(2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(stage.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(stage.subtitle, color = Color(0xFFFFAB91), fontSize = 10.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = Color(0x60000000), shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, Color(0x30FFFFFF)), modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stage.mechanicsBadge, color = Color(0xFFFFE082), fontSize = 9.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Goal: ${stage.targetFruits} 🍎", color = Color(0xFFFFD54F),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
                if (highScore > 0) {
                    Text("Best: $highScore", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("UNLOCKED", color = Color(0xFF69F0AE), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
