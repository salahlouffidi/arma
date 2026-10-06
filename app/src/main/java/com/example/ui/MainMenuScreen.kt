package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameEngine
import com.example.game.GameMode
import com.example.game.HapticHelper
import com.example.game.SoundManager

@Composable
fun MainMenuScreen(
    engine: GameEngine,
    soundManager: SoundManager,
    hapticHelper: HapticHelper,
    onStartGame: (GameMode, Int) -> Unit
) {
    val playPulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        playPulse.animateTo(
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF261208),
                        Color(0xFF3E1F0B),
                        Color(0xFF190C04)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { soundManager.isSoundEnabled = !soundManager.isSoundEnabled },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("sound_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Toggle Sound",
                        tint = if (soundManager.isSoundEnabled) Color(0xFFFFB74D) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { hapticHelper.isHapticsEnabled = !hapticHelper.isHapticsEnabled },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("haptic_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Toggle Haptics",
                        tint = if (hapticHelper.isHapticsEnabled) Color(0xFFFFB74D) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFFFF6F00),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "★ LOUFFI ARCADE EDITION ★",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ARMADILLO\nESCAPE",
                color = Color(0xFFFFB74D),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 46.sp,
                letterSpacing = 1.sp
            )

            Text(
                text = "Roll • Dodge • Frenzy",
                color = Color(0xFFFFCC80),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "High Score",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "BEST RECORD",
                            color = Color(0xFFFFCC80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${engine.highScore} PTS",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onStartGame(engine.currentMode, engine.currentStage) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .scale(playPulse.value)
                    .testTag("play_game_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ROLL TO ESCAPE!",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SELECT GAME MODE",
                color = Color(0xFFFFCC80),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            for (mode in GameMode.values()) {
                val isSelected = engine.currentMode == mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { engine.currentMode = mode }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFF9800) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0x33FF9800) else Color(0x22000000)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mode.description,
                            color = Color(0xAAFFFFFF),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "STAGE SELECT",
                color = Color(0xFFFFCC80),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(engine.stages) { stage ->
                    val isSelected = engine.currentStage == stage.stageNumber
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { engine.currentStage = stage.stageNumber }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFF9800) else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0x44FF9800) else Color(0x22000000)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STAGE ${stage.stageNumber}",
                                color = Color(0xFFFFB74D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stage.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Goal: ${stage.targetScore}",
                                color = Color(0x88FFFFFF),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
