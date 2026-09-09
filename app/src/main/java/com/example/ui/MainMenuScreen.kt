package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun MainMenuScreen(
    currentStageId: Int,
    soundManager: SoundManager,
    onStartGame: (stageId: Int) -> Unit,
    onOpenStageSelect: () -> Unit,
    onOpenAudioSettings: () -> Unit,
    onOpenGameModeSelect: () -> Unit = onOpenStageSelect
) {
    var showHowToPlay by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val bannerGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF261208),
                        Color(0xFF190C05),
                        Color(0xFF0F0703)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("main_menu_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Main Logo & Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "🦔", fontSize = 36.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ARMASNAKE",
                        color = Color(0xFFFFD700),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp
                    )
                    Text(
                        text = "LOUFFI EDITION",
                        color = Color(0xFFFF8F00),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "🐍", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Unlocked badge
            Surface(
                color = Color(0xFF3E1F0C),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFD4AF37))
            ) {
                Text(
                    text = "🏆 ALL 16 WORLDS FULLY UNLOCKED (SOLO & DUAL)",
                    color = Color(0xFFFFE082),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Primary Button -> Opens Game Mode selection
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onOpenGameModeSelect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF1F0D03)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("menu_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAY / GAME MODE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                // Stage Selection Button (All Unlocked!)
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onOpenStageSelect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E2312),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFD4AF37)),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("menu_stage_select_button")
                ) {
                    Text(
                        text = "🏆 SELECT STAGE (1–16)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Buttons: Audio & Rules
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onOpenAudioSettings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF261408),
                        contentColor = Color(0xFFFFE0B2)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("menu_audio_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUDIO SETTINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        showHowToPlay = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF261408),
                        contentColor = Color(0xFFFFE0B2)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x6081C784)),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("menu_how_to_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF81C784)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RULES & 7ARAKA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick feature pill hints
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0x40000000),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color(0x30FFFFFF))
                ) {
                    Text(
                        text = "🕹️ Virtual Joystick",
                        color = Color(0xFFBCAAA4),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color(0x40000000),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color(0x30FFFFFF))
                ) {
                    Text(
                        text = "⚡ 7araka Power Shield",
                        color = Color(0xFFFFD54F),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color(0x40000000),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color(0x30FFFFFF))
                ) {
                    Text(
                        text = "🍎 4-Fruit Scatter Chase",
                        color = Color(0xFFFF8A65),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // How to play dialog
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
                        desc = "Press and drag the on-screen joystick to roll the armadillo across the table. 360° and diagonal movement fully supported!"
                    )
                    RuleRow(
                        icon = "🍎",
                        title = "Fruit Collection & Events",
                        desc = "Collect fruits to gain points. Every 4 fruits triggers a 4-fruit bouncing scatter chase event!"
                    )
                    RuleRow(
                        icon = "⚡",
                        title = "7araka Star Ability",
                        desc = "Catching the Star Fruit activates '⚡ SPECIAL 7ARAKA ACTIVE' granting super speed, agility, and protection!"
                    )
                    RuleRow(
                        icon = "🛡️",
                        title = "Snake Shield Protection",
                        desc = "If the snake strikes while 7araka power is active, the armadillo SURVIVES! The shield absorbs the blow, 1 point is lost ('POWER LOST −1'), and play continues."
                    )
                    RuleRow(
                        icon = "👥",
                        title = "Dual Armadillo Co-op (Stages 9–16)",
                        desc = "Extra stages feature two independent armadillos! Use your left thumb for Golden P1 and right thumb for Blue P2 to coordinate and outwit the snake."
                    )
                    RuleRow(
                        icon = "👑",
                        title = "All 16 Worlds Available",
                        desc = "Stage 1 to Stage 16 are completely unlocked from the start. Jump to any environment immediately!"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
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
