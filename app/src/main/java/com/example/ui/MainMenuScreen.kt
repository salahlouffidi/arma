@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.SoundManager
import com.example.game.StageConfig
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.draw.scale

@Composable
fun MainMenuScreen(
    currentStageId: Int,
    soundManager: SoundManager,
    onStartGame: (stageId: Int) -> Unit,
    onOpenStageSelect: () -> Unit,
    onOpenAudioSettings: () -> Unit,
    onOpenGameModeSelect: () -> Unit = onOpenStageSelect,
    onOpenPrivacyOptions: (() -> Unit)? = null,
    onDebugAdInspector: (() -> Unit)? = null
) {
    var showHowToPlay by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val bannerGlow by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    ProBackdrop(
        modifier = Modifier.testTag("main_menu_screen"),
        theme = StageConfig.getStage(currentStageId).theme,
        dim = 0.45f
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            val wide = maxWidth > maxHeight
            val hero: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MascotArmadillo(Modifier.size(64.dp), heading = 0.35f)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "ARMASNAKE",
                            style = proTitleStyle(if (wide) 46.sp else 40.sp, 3.sp),
                            modifier = Modifier.scale(bannerGlow)
                        )
                        Spacer(Modifier.width(6.dp))
                        MascotSnake(Modifier.size(64.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProDivider(Modifier.width(48.dp))
                        Text(
                            modifier = if (onDebugAdInspector != null) Modifier.combinedClickable(onClick = {}, onLongClick = onDebugAdInspector) else Modifier,
                            text = "  LOUFFI EDITION  ",
                            color = Color(0xFFFFB547),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 5.sp
                        )
                        ProDivider(Modifier.width(48.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProChip("${StageConfig.MAX_ROUND} ROUNDS", color = ArmaColors.Gold, filled = true)
                        ProChip("SOLO & DUAL", color = ArmaColors.Blue)
                        ProChip("NEW: POWER-UPS & COMBOS", color = ArmaColors.Green)
                    }
                }
            }
            val actions: @Composable () -> Unit = {
                ProPanel(highlighted = true, modifier = Modifier.width(320.dp)) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ROUND ${currentStageId.toString().padStart(2, '0')} • ${StageConfig.getStage(currentStageId).title.uppercase()}",
                            color = ArmaColors.TextLow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        ProButton(
                            text = "PLAY",
                            onClick = {
                                soundManager.playMenuClick()
                                onOpenGameModeSelect()
                            },
                            height = 54.dp,
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth().testTag("menu_play_button"),
                            leading = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ArmaColors.Ink, modifier = Modifier.size(24.dp))
                            }
                        )
                        ProButton(
                            text = "SELECT ROUND",
                            onClick = {
                                soundManager.playMenuClick()
                                onOpenStageSelect()
                            },
                            style = ProButtonStyle.Secondary,
                            height = 44.dp,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().testTag("menu_round_select_button")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ProButton(
                                text = "AUDIO",
                                onClick = {
                                    soundManager.playMenuClick()
                                    onOpenAudioSettings()
                                },
                                style = ProButtonStyle.Ghost,
                                height = 40.dp,
                                fontSize = 11.sp,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).testTag("menu_audio_settings_button"),
                                leading = { Icon(Icons.Default.Settings, contentDescription = null, tint = ArmaColors.Gold, modifier = Modifier.size(16.dp)) }
                            )
                            ProButton(
                                text = "RULES",
                                onClick = {
                                    soundManager.playMenuClick()
                                    showHowToPlay = true
                                },
                                style = ProButtonStyle.Ghost,
                                height = 40.dp,
                                fontSize = 11.sp,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).testTag("menu_how_to_play_button"),
                                leading = { Icon(Icons.Default.Info, contentDescription = null, tint = ArmaColors.Green, modifier = Modifier.size(16.dp)) }
                            )
                        }
                        if (onOpenPrivacyOptions != null) {
                            Text(
                                text = "Privacy options",
                                color = ArmaColors.TextLow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { onOpenPrivacyOptions() }
                                    .padding(4.dp)
                                    .testTag("menu_privacy_options")
                            )
                        }
                    }
                }
            }
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    hero()
                    actions()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    hero()
                    actions()
                }
            }
        }

        if (showHowToPlay) {
            HowToPlayDialog(onDismiss = { showHowToPlay = false })
        }
    }
}

@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1D1007),
            border = BorderStroke(2.dp, Color(0xFFD4AF37)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📖 HOW TO PLAY — ARMASNAKE",
                    color = Color(0xFFFFD700),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RuleRow(
                        icon = "🕹️",
                        title = "Virtual Joystick Control",
                        desc = "Press and drag the on-screen joystick to roll the Armadillo across the arena. 360° and diagonal movement fully supported!"
                    )
                    RuleRow(
                        icon = "🍎",
                        title = "Fruit Collection & Events",
                        desc = "Collect fruits to gain points. Every 4 fruits triggers a 4-fruit bouncing scatter chase event!"
                    )
                    RuleRow(
                        icon = "⚡",
                        title = "OVERDRIVE Ability",
                        desc = "Catching the Star Fruit activates OVERDRIVE, granting super speed, agility, and protection!"
                    )
                    RuleRow(
                        icon = "🛡️",
                        title = "Snake Shield Protection",
                        desc = "If the snake strikes while OVERDRIVE is active, the Armadillo survives. The shield absorbs the blow, 1 point is lost, and play continues."
                    )
                    RuleRow(
                        icon = "🔵",
                        title = "Dual Armadillo Rounds 09–16",
                        desc = "Rounds 09–16 feature two independent Armadillos. Use your left thumb for Golden and right thumb for Blue to control both simultaneously in single-player mode."
                    )
                    RuleRow(
                        icon = "👑",
                        title = "Power-ups, Combos & World 3",
                        desc = "Grab MAGNET, FREEZE, x2 POINTS and SNAKE SHRINK orbs, chain fruit for combo points, and beat Rounds 17–20: Neon City, Deep Ocean, Candy Land and Cosmos. Watch out: snakes grow as you feed!"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ProStyledButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF1B0E05)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(text = "GOT IT, LET'S ROLL!", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RuleRow(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = Color(0xFFFFE082),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = Color(0xFFD7CCC8),
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}
