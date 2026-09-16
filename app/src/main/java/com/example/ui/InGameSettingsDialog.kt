package com.example.ui

import android.content.Context
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
import androidx.compose.material.icons.filled.MusicNote
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

private const val PREFS_NAME = "armasnake_settings"
private fun joystickPrefKey(roundId: Int) = "single_joystick_right_round_$roundId"

@Composable
fun InGameSettingsDialog(
    roundId: Int,
    isDualArmadillo: Boolean,
    soundManager: SoundManager,
    onControlChanged: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    onMinimize: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var isRightSide by remember {
        mutableStateOf(prefs.getBoolean(joystickPrefKey(roundId), false))
    }

    var isSoundOn by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var musicVol by remember { mutableFloatStateOf(soundManager.musicVolume) }
    var sfxVol by remember { mutableFloatStateOf(soundManager.sfxVolume) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF1C110A),
            border = BorderStroke(2.dp, Color(0xFFD4AF37)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 6.dp)
                .testTag("in_game_settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ ROUND $roundId SETTINGS",
                        color = Color(0xFFFFD54F),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFBCAAA4))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 1: Controls
                if (!isDualArmadillo && roundId in 1..8) {
                    Surface(
                        color = Color(0xFF28160B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x60D4AF37)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "JOYSTICK POSITION",
                                    color = Color(0xFFFFE0B2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isRightSide = false
                                        prefs.edit().putBoolean(joystickPrefKey(roundId), false).apply()
                                        soundManager.playMenuClick()
                                        onControlChanged()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isRightSide) Color(0xFFD4AF37) else Color(0xFF3E2312),
                                        contentColor = if (!isRightSide) Color(0xFF1B0E05) else Color(0xFFFFE0B2)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("in_game_joystick_left")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("◀ LEFT", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        Text("bottom-left", fontSize = 9.sp)
                                    }
                                }
                                Button(
                                    onClick = {
                                        isRightSide = true
                                        prefs.edit().putBoolean(joystickPrefKey(roundId), true).apply()
                                        soundManager.playMenuClick()
                                        onControlChanged()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRightSide) Color(0xFFD4AF37) else Color(0xFF3E2312),
                                        contentColor = if (isRightSide) Color(0xFF1B0E05) else Color(0xFFFFE0B2)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("in_game_joystick_right")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("RIGHT ▶", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        Text("bottom-right", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFF28160B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x60D4AF37)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🎮 DUAL CONTROLS (ROUNDS 09–16)",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "LEFT = GOLDEN ARMADILLO  •  RIGHT = BLUE ARMADILLO",
                                color = Color(0xFFBCAAA4),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Audio
                Surface(
                    color = Color(0xFF28160B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x60D4AF37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSoundOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = if (isSoundOn) Color(0xFF81C784) else Color(0xFFE57373),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MASTER AUDIO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedThumbColor = Color(0xFF888888),
                                uncheckedTrackColor = Color(0xFF333333)
                            ),
                            modifier = Modifier.testTag("in_game_sound_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Volume Sliders
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MUSIC", color = Color(0xFFFFE0B2), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${(musicVol * 100).toInt()}%", color = Color(0xFFFFD54F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = musicVol,
                        onValueChange = { musicVol = it; soundManager.musicVolume = it },
                        valueRange = 0f..1f,
                        enabled = isSoundOn,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFFFB74D),
                            inactiveTrackColor = Color(0xFF3E2312)
                        )
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SFX", color = Color(0xFFFFE0B2), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${(sfxVol * 100).toInt()}%", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = sfxVol,
                        onValueChange = { sfxVol = it; soundManager.sfxVolume = it },
                        onValueChangeFinished = { soundManager.playPointGained() },
                        valueRange = 0f..1f,
                        enabled = isSoundOn,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF81C784),
                            activeTrackColor = Color(0xFF4CAF50),
                            inactiveTrackColor = Color(0xFF1E3320)
                        )
                    )
                }

                // Minimize / PiP Button
                if (onMinimize != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onMinimize,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A2B35),
                            contentColor = Color(0xFF81D4FA)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x604FC3F7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("in_game_settings_minimize_button")
                    ) {
                        Text("🗗 MINIMIZE (PiP)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Resume & Done Button
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF1B0E05)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("in_game_settings_resume_button")
                ) {
                    Text("▶ RESUME GAME", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
