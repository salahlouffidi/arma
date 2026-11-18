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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.filled.Lock
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

    val displayedStages = when (selectedFilter) {
        1 -> StageConfig.allStages.filter { !it.isDualArmadillo }
        2 -> StageConfig.allStages.filter { it.isDualArmadillo }
        else -> StageConfig.allStages
    }

    val highestUnlocked = progressManager.getHighestUnlockedStage()

    ProBackdrop(
        modifier = Modifier.testTag("round_select_screen"),
        theme = StageConfig.getStage(currentStageId).theme,
        dim = 0.7f
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProButton(
                    text = "MENU",
                    onClick = {
                        soundManager.playMenuClick()
                        onBackToMenu()
                    },
                    style = ProButtonStyle.Secondary,
                    height = 38.dp,
                    fontSize = 11.sp,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.testTag("round_select_back_button"),
                    leading = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ArmaColors.Gold, modifier = Modifier.size(16.dp)) }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "SELECT ROUND", style = proTitleStyle(22.sp, 3.sp))
                    Text(
                        text = "CAMPAIGN PROGRESS  $highestUnlocked / 16",
                        color = ArmaColors.TextLow, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ALL (16)", "SOLO (01–08)", "DUAL (09–16)").forEachIndexed { index, label ->
                        val isSel = selectedFilter == index
                        ProButton(
                            text = label,
                            onClick = {
                                soundManager.playMenuClick()
                                selectedFilter = index
                            },
                            style = if (isSel) ProButtonStyle.Primary else ProButtonStyle.Ghost,
                            height = 32.dp,
                            fontSize = 10.sp,
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedStages) { stage ->
                    val isCurrent = stage.id == currentStageId
                    val isUnlocked = progressManager.isStageUnlocked(stage.id)
                    val isCompleted = progressManager.isStageCompleted(stage.id)
                    val highScore = progressManager.getStageHighScore(stage.id)

                    RoundCard(
                        stage = stage,
                        isUnlocked = isUnlocked,
                        isCurrent = isCurrent,
                        isCompleted = isCompleted,
                        highScore = highScore,
                        preferences = preferences,
                        settingsVersion = settingsVersion,
                        onSettings = if (!stage.isDualArmadillo && isUnlocked) {
                            { controlSettingsRound = stage.id }
                        } else null,
                        onClick = {
                            if (isUnlocked) {
                                soundManager.playStageSelected()
                                preferences.edit().putInt("active_single_joystick_stage_id", stage.id).apply()
                                onSelectStage(stage.id)
                            }
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
    isUnlocked: Boolean,
    isCurrent: Boolean,
    isCompleted: Boolean,
    highScore: Int,
    preferences: android.content.SharedPreferences,
    settingsVersion: Int,
    onSettings: (() -> Unit)?,
    onClick: () -> Unit
) {
    val borderColor = when {
        !isUnlocked -> Color(0x20FFFFFF)
        isCurrent -> Color(0xFFFFD700)
        isCompleted -> Color(0xFF81C784)
        else -> Color(0x60D4AF37)
    }
    val bgColor = when {
        !isUnlocked -> Color(0xFF140C07)
        isCurrent -> Color(0xFF45220C)
        isCompleted -> Color(0xFF26190F)
        else -> Color(0xFF1E120A)
    }

    val isJoystickRight = if (!stage.isDualArmadillo) {
        preferences.getBoolean("single_joystick_right_round_${stage.id}", false)
    } else false

    ProPanel(
        corner = 16.dp,
        highlighted = isCurrent && isUnlocked,
        accent = if (isCompleted && !isCurrent) ArmaColors.Green else ArmaColors.Gold,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isUnlocked) Modifier.clickable { onClick() } else Modifier)
            .testTag("round_card_${stage.id}")
    ) {
      Column {
        Box {
            ThemePreview(
                theme = stage.theme,
                locked = !isUnlocked,
                modifier = Modifier.fillMaxWidth().height(78.dp)
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xE0140A04)), startY = 60f)
                )
            )
            Text(
                text = stage.id.toString().padStart(2, '0'),
                style = proTitleStyle(28.sp, 1.sp),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 2.dp)
            )
            if (!isUnlocked) {
                Icon(
                    Icons.Default.Lock, contentDescription = "Locked", tint = ArmaColors.TextMid,
                    modifier = Modifier.align(Alignment.Center).size(26.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ROUND ${stage.id.toString().padStart(2, '0')}",
                        color = when {
                            !isUnlocked -> Color(0xFF757575)
                            isCurrent -> Color(0xFFFFD54F)
                            else -> Color(0xFFFFCCBC)
                        },
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isUnlocked) {
                        Surface(
                            color = Color(0xFF2B1D16),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, Color(0x40FFFFFF))
                        ) {
                            Text(
                                text = "LOCKED",
                                color = Color(0xFF9E9E9E),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (stage.isDualArmadillo) {
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
            Text(
                stage.title,
                color = if (isUnlocked) Color.White else Color(0xFF757575),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                stage.subtitle,
                color = if (isUnlocked) Color(0xFFFFAB91) else Color(0xFF616161),
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = if (isUnlocked) Color(0x60000000) else Color(0x30000000),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, if (isUnlocked) Color(0x30FFFFFF) else Color(0x15FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stage.mechanicsBadge,
                    color = if (isUnlocked) Color(0xFFFFE082) else Color(0xFF757575),
                    fontSize = 9.sp,
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
                    "${stage.targetFruits} 🍎 • Max: ${stage.maxAllowedLosses} 💔",
                    color = if (isUnlocked) Color(0xFFFFD54F) else Color(0xFF757575),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
                if (!isUnlocked) {
                    Text(
                        "🔒 LOCKED",
                        color = Color(0xFF9E9E9E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (highScore > 0) {
                    Text(
                        "Best: $highScore",
                        color = Color(0xFF81C784),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "UNLOCKED",
                        color = Color(0xFF69F0AE),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
      }
    }
}
