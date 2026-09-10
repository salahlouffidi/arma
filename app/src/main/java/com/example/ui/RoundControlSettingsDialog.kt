package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun RoundControlSettingsDialog(
    roundId: Int,
    soundManager: SoundManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(CONTROL_PREFS, 0) }
    var rightSide by remember {
        mutableStateOf(preferences.getBoolean(joystickRightKey(roundId), false))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            color = Color(0xFF1C110A),
            border = BorderStroke(2.dp, Color(0xFFD4AF37)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .testTag("round_control_settings_dialog_$roundId")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            text = "Movement joystick position",
                            color = Color(0xFFBCAAA4),
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFBCAAA4))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Where do you want the joystick?",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            rightSide = false
                            preferences.edit().putBoolean(joystickRightKey(roundId), false).apply()
                            preferences.edit().putInt("active_single_joystick_stage_id", roundId).apply()
                            soundManager.playMenuClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!rightSide) Color(0xFFD4AF37) else Color(0xFF3E2516),
                            contentColor = if (!rightSide) Color(0xFF1B0E05) else Color(0xFFFFE0B2)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("round_${roundId}_joystick_left")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("◀ LEFT", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                            preferences.edit().putInt("active_single_joystick_stage_id", roundId).apply()
                            soundManager.playMenuClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rightSide) Color(0xFFD4AF37) else Color(0xFF3E2516),
                            contentColor = if (rightSide) Color(0xFF1B0E05) else Color(0xFFFFE0B2)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("round_${roundId}_joystick_right")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RIGHT ▶", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                "bottom-right",
                                fontSize = 9.sp,
                                color = if (rightSide) Color(0xFF3E2006) else Color(0xFFBCAAA4)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Saved only for ROUND ${roundId.toString().padStart(2, '0')}",
                    color = Color(0xFFFFCC80),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF1B0E05)
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("DONE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
