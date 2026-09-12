package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
                    Text(text = "MAIN MENU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAY SOLO",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "SINGLE-PLAYER ARMASNAKE CAMPAIGN",
                        color = Color(0xFFFFE0B2),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(90.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .weight(1f)
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
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color(0x30D4AF37),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x80D4AF37))
                        ) {
                            Text(
                                text = "1 PLAYER • FULL CAMPAIGN",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(78.dp),
                                shape = CircleShape,
                                color = Color(0xFF3E2211),
                                border = BorderStroke(2.dp, Color(0xFFFFD700))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🦔", fontSize = 40.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "PLAY SOLO",
                                color = Color(0xFFFFD700),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                text = "One player • AI snakes • offline campaign",
                                color = Color(0xFFFFE0B2),
                                fontSize = 12.sp
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            FeatureBullet(icon = "🌿", text = "ROUNDS 01–08: Solo Golden Armadillo")
                            FeatureBullet(icon = "🔵", text = "ROUNDS 09–16: Dual Armadillo — one player")
                            FeatureBullet(icon = "🐍", text = "AI-controlled predator snakes")
                            FeatureBullet(icon = "⚡", text = "OVERDRIVE ability and virtual joystick controls")
                        }

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
                                .height(44.dp)
                                .testTag("start_solo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START SOLO • ROUNDS 01–16",
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
