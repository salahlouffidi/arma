package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Compact icon-only Pause Button for the ARMASNAKE HUD.
 * Uses standard Material Pause icon (II) and Play icon when paused.
 * Fixed 40-42dp square dimension with rounded corners matching Louffi Edition aesthetics.
 */
@Composable
fun PauseButton(
    isPaused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isPaused) Color(0xFF3E2312) else Color(0xE62A170C),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isPaused) Color(0xFFFFB74D) else Color(0x60D4AF37)
        ),
        modifier = modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .testTag("hud_pause_button")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume Game" else "Pause Game",
                tint = if (isPaused) Color(0xFFFFD54F) else Color(0xFFFFE0B2),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
