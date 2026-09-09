package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.SoundManager

@Composable
fun GameModeSelectScreen(
    soundManager: SoundManager,
    onSelectSolo: () -> Unit,
    onSelectMultiplayer: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F0F06),
                        Color(0xFF140903),
                        Color(0xFF0D0502)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onBackToMenu()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2B160A),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier.testTag("mode_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MAIN MENU",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CHOOSE GAME MODE",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "HOW DO YOU WANT TO PLAY ARMASNAKE?",
                        color = Color(0xFFFFE0B2),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }

                // Balance placeholder
                Spacer(modifier = Modifier.width(90.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two Large Hero Mode Choices (Landscape Split)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ================= OPTION 1: PLAY SOLO =================
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable {
                            soundManager.playMenuClick()
                            onSelectSolo()
                        }
                        .testTag("play_solo_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF26150B),
                    border = BorderStroke(2.dp, Color(0xFFD4AF37))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x35D4AF37),
                                        Color(0x10140D08),
                                        Color(0x40000000)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top badge
                            Surface(
                                color = Color(0x30D4AF37),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0x80D4AF37))
                            ) {
                                Text(
                                    text = "1 PLAYER • CLASSIC JOURNEY",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Icon & Title
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.size(62.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF3E2211),
                                    border = BorderStroke(2.dp, Color(0xFFFFD700))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🦔", fontSize = 32.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "PLAY SOLO",
                                    color = Color(0xFFFFD700),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )

                                Text(
                                    text = "Control Armadillo • Snakes are AI",
                                    color = Color(0xFFFFE0B2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Feature List
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                FeatureBullet(icon = "🌿", text = "Stages 1–8: Solo Golden Armadillo")
                                FeatureBullet(icon = "🔵", text = "Stages 9–16: Dual Armadillo Co-op")
                                FeatureBullet(icon = "🐍", text = "Autonomous Predator AI Snakes")
                            }

                            // Action Button
                            Button(
                                onClick = {
                                    soundManager.playMenuClick()
                                    onSelectSolo()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD4AF37),
                                    contentColor = Color(0xFF1F0D03)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("start_solo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "START SOLO (STAGES 1–16)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // ================= OPTION 2: PLAY WITH OTHERS =================
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable {
                            soundManager.playMenuClick()
                            onSelectMultiplayer()
                        }
                        .testTag("play_multiplayer_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A1F2C),
                    border = BorderStroke(2.dp, Color(0xFF00E5FF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x3500E5FF),
                                        Color(0x100A101D),
                                        Color(0x40000000)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top badge
                            Surface(
                                color = Color(0x3000E5FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0x8000E5FF))
                            ) {
                                Text(
                                    text = "👥 2 TO 4 PLAYERS • BATTLE & CO-OP",
                                    color = Color(0xFF80D8FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Icon & Title
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.size(62.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF102A43),
                                    border = BorderStroke(2.dp, Color(0xFF40C4FF))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "👥", fontSize = 32.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "PLAY WITH OTHERS",
                                    color = Color(0xFFE0F7FA),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )

                                Text(
                                    text = "Play as Armadillo or Snake!",
                                    color = Color(0xFFB2EBF2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Feature List
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                FeatureBullet(icon = "⚔️", text = "1v1, 2v1, 1v2, 2v2 & Custom Roles")
                                FeatureBullet(icon = "🐍", text = "Human Snake Controls & Hunting")
                                FeatureBullet(icon = "👆", text = "Multi-Touch Virtual Joysticks & Keys")
                            }

                            // Action Button
                            Button(
                                onClick = {
                                    soundManager.playMenuClick()
                                    onSelectMultiplayer()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00B0FF),
                                    contentColor = Color(0xFF001E3C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("choose_multiplayer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CHOOSE MULTIPLAYER MODE ➔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureBullet(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(text = icon, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = Color(0xFFECEFF1),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
