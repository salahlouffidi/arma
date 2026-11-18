package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.safeDrawingPadding
import com.example.game.StageTheme
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
            .proBackdrop(StageTheme.JUNGLE, dim = 0.72f)
            .safeDrawingPadding()
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
                ProStyledButton(
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
                        text = "CHOOSE MODE",
                        style = proTitleStyle(22.sp, 3.sp)
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
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ProChip("1 PLAYER • FULL CAMPAIGN", color = ArmaColors.Gold, filled = true)
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Color(0xFF5A3418), Color(0xFF1E0F06))))
                                    .border(2.dp, Brush.verticalGradient(listOf(Color(0xFFFFF0B0), Color(0xFFB07A10))), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                MascotArmadillo(Modifier.size(84.dp), heading = -0.5f)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "PLAY SOLO", style = proTitleStyle(26.sp, 2.sp))
                            Text(
                                text = "One player • AI snakes • offline campaign",
                                color = ArmaColors.TextMid,
                                fontSize = 12.sp
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(340.dp)
                        ) {
                            FeatureBullet(icon = "01", text = "ROUNDS 01–08: Solo Golden Armadillo")
                            FeatureBullet(icon = "09", text = "ROUNDS 09–16: Dual Armadillo — one player")
                            FeatureBullet(icon = "17", text = "NEW WORLD 3: Rounds 17–20 power-up arenas")
                            FeatureBullet(icon = "AI", text = "AI-controlled predator snakes")
                            FeatureBullet(icon = "PU", text = "Power-ups, combos and growing snakes")
                            Spacer(modifier = Modifier.height(6.dp))
                            ProButton(
                                text = "START SOLO",
                                onClick = {
                                    soundManager.playMenuClick()
                                    onSelectSolo()
                                },
                                height = 50.dp,
                                fontSize = 15.sp,
                                modifier = Modifier.fillMaxWidth().testTag("start_solo_button"),
                                leading = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ArmaColors.Ink, modifier = Modifier.size(22.dp)) }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0x33FFD45A))
                .border(1.dp, Color(0x88FFD45A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, color = ArmaColors.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, color = ArmaColors.TextHi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
