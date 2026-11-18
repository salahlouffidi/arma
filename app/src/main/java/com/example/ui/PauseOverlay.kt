package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.StageConfig

/**
 * Pause menu for ARMASNAKE — LOUFFI EDITION (v2.1 redesign).
 * Uses vector icons only (no emoji / rare glyphs that some phones render as boxes).
 */
@Composable
fun PauseOverlay(
    stage: StageConfig,
    onResume: () -> Unit,
    onSettings: () -> Unit,
    onRestart: () -> Unit,
    onExitToStages: () -> Unit,
    onMinimize: (() -> Unit)? = null
) {
    val accent = themeAccent(stage.theme)
    Dialog(onDismissRequest = onResume, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ProPanel(
            highlighted = true,
            accent = accent,
            corner = 24.dp,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.86f)
                .testTag("game_pause_overlay")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Theme preview strip with the round badge on top
                Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    ThemePreview(stage.theme, modifier = Modifier.fillMaxWidth().height(64.dp))
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProChip("ROUND ${stage.id.toString().padStart(2, '0')}", color = accent, filled = true)
                        ProChip(stage.title.uppercase(), color = ArmaColors.TextHi)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(text = "PAUSED", style = proTitleStyle(30.sp, 6.sp))
                Text(
                    text = "Take a breath — the snakes are waiting.",
                    color = ArmaColors.TextLow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(16.dp))

                ProButton(
                    text = "RESUME",
                    onClick = onResume,
                    height = 54.dp,
                    fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().testTag("pause_resume_button"),
                    leading = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ArmaColors.Ink, modifier = Modifier.size(24.dp)) }
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProButton(
                        text = "RETRY",
                        onClick = onRestart,
                        style = ProButtonStyle.Secondary,
                        height = 44.dp,
                        fontSize = 12.sp,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f).testTag("pause_restart_button"),
                        leading = { Icon(Icons.Default.Refresh, contentDescription = null, tint = ArmaColors.Gold, modifier = Modifier.size(18.dp)) }
                    )
                    ProButton(
                        text = "ROUNDS",
                        onClick = onExitToStages,
                        style = ProButtonStyle.Secondary,
                        height = 44.dp,
                        fontSize = 12.sp,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f).testTag("pause_exit_stages_button"),
                        leading = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = ArmaColors.Gold, modifier = Modifier.size(18.dp)) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProButton(
                        text = "SETTINGS",
                        onClick = onSettings,
                        style = ProButtonStyle.Ghost,
                        height = 40.dp,
                        fontSize = 11.sp,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f).testTag("pause_open_settings_button"),
                        leading = { Icon(Icons.Default.Settings, contentDescription = null, tint = ArmaColors.TextMid, modifier = Modifier.size(16.dp)) }
                    )
                    if (onMinimize != null) {
                        ProButton(
                            text = "MINI WINDOW",
                            onClick = onMinimize,
                            style = ProButtonStyle.Ghost,
                            height = 40.dp,
                            fontSize = 11.sp,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).testTag("pause_minimize_button")
                        )
                    }
                }
            }
        }
    }
}
