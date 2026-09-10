package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.SoundManager

private const val CONTROL_PREFS = "armasnake_settings"
private fun joystickRightKey(stageId: Int) = "single_joystick_right_round_$stageId"

@Composable
fun InGameSettingsDialog(
    roundId: Int,
    isDualArmadillo: Boolean,
    soundManager: SoundManager,
    onJoystickChanged: (isRight: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(CONTROL_PREFS, 0) }

    var rightSide by remember(roundId) {
        mutableStateOf(preferences.getBoolean(joystickRightKey(roundId), false))
    }
    var isSoundOn by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var musicVol by remember { mutableFloatStateOf(soundManager.musicVolume) }
    var sfxVol by remember { mutableFloatStateOf(soundManager.sfxVolume) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF1C110A),
            border = BorderStroke(2.dp, Color(0xFFD4AF37)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("in_game_settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ROUND ${roundId.toString().padStart(2, '0')} SETTINGS",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "GAME IS CURRENTLY PAUSED",
                            color = Color(0xFFFFB74D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("in_game_settings_close_icon")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFBCAAA4))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 1: JOYSTICK SETTING (Rounds 01-08 vs Rounds 09-16)
                Surface(
                    color = Color(0xFF26150C),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "JOYSTICK CONTROLS",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isDualArmadillo) {
                            // ROUND 01–08: Single Armadillo: Choose LEFT or RIGHT
                            Text(
                                text = "Movement joystick screen position:",
                                color = Color(0xFFFFE0B2),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        rightSide = false
                                        preferences.edit().putBoolean(joystickRightKey(roundId), false).apply()
                                        onJoystickChanged(false)
                                        soundManager.playMenuClick()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!rightSide) Color(0xFFD4AF37) else Color(0xFF3E2516),
                                        contentColor = if (!rightSide) Color(0xFF1B0E05) else Color(0xFFFFE0B2)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("in_game_joystick_left")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("◀ LEFT", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        Text(
                                            "bottom-left",
                                            fontSize = 9.sp,
                                            color = if (!rightSide) Color(0xFF3E2006) else Color(0xFFBCAAA4)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        rightSide = true
                                        preferences.edit().putBoolean(joystickRightKey(roundId), true).apply()
                                        onJoystickChanged(true)
                                        soundManager.playMenuClick()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (rightSide) Color(0xFFD4AF37) else Color(0xFF3E2516),
                                        contentColor = if (rightSide) Color(0xFF1B0E05) else Color(0xFFFFE0B2)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("in_game_joystick_right")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("RIGHT ▶", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        Text(
                                            "bottom-right",
                                            fontSize = 9.sp,
                                            color = if (rightSide) Color(0xFF3E2006) else Color(0xFFBCAAA4)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Saved for ROUND ${roundId.toString().padStart(2, '0')} (takes effect immediately)",
                                color = Color(0xFFFFCC80),
                                fontSize = 9.sp
                            )
                        } else {
                            // ROUND 09–16: Dual Armadillos: Fixed positions
                            Text(
                                text = "Dual Armadillos Mode — Fixed layout:",
                                color = Color(0xFFFFE0B2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF332014),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("◀ LEFT", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("P1 Golden Armadillo", color = Color(0xFFFFE0B2), fontSize = 9.sp)
                                    }
                                }
                                Surface(
                                    color = Color(0xFF142433),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF42A5F5)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("RIGHT ▶", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("P2 Blue Armadillo", color = Color(0xFFBBDEFB), fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SECTION 2: AUDIO CONTROLS
                Surface(
                    color = Color(0xFF26150C),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSoundOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                    contentDescription = null,
                                    tint = if (isSoundOn) Color(0xFFFFD54F) else Color(0xFFE57373),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GAME AUDIO",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = isSoundOn,
                                onCheckedChange = {
                                    isSoundOn = it
                                    soundManager.isSoundEnabled = it
                                    if (it) {
                                        soundManager.startMusic()
                                        soundManager.playMenuClick()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFFD54F),
                                    checkedTrackColor = Color(0xFF6D4C41),
                                    uncheckedThumbColor = Color(0xFFBCAAA4),
                                    uncheckedTrackColor = Color(0xFF3E2723)
                                )
                            )
                        }

                        if (isSoundOn) {
                            Spacer(modifier = Modifier.height(6.dp))

                            // Music Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Music Volume", color = Color(0xFFFFE0B2), fontSize = 10.sp)
                                Text("${(musicVol * 100).toInt()}%", color = Color(0xFFFFD54F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = musicVol,
                                onValueChange = {
                                    musicVol = it
                                    soundManager.musicVolume = it
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD54F),
                                    activeTrackColor = Color(0xFFFFB74D),
                                    inactiveTrackColor = Color(0xFF3E2312)
                                ),
                                modifier = Modifier.height(26.dp)
                            )

                            // SFX Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SFX Volume", color = Color(0xFFFFE0B2), fontSize = 10.sp)
                                Text("${(sfxVol * 100).toInt()}%", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = sfxVol,
                                onValueChange = {
                                    sfxVol = it
                                    soundManager.sfxVolume = it
                                },
                                onValueChangeFinished = { soundManager.playPointGained() },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF81C784),
                                    activeTrackColor = Color(0xFF4CAF50),
                                    inactiveTrackColor = Color(0xFF1E3320)
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close / Return to Pause button
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF1B0E05)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("in_game_settings_close_button")
                ) {
                    Text(
                        text = "SAVE & RETURN TO PAUSE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
