package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Independent overlay status pill for OVERDRIVE (formerly 7araka) and special events.
 * Treated as an independent overlay that does NOT deform, compress, or push the top HUD bar.
 */
@Composable
fun OverdriveStatusPill(
    isPowerActive: Boolean,
    powerTimer: Float,
    powerChancesRemaining: Int,
    fruitsCollectedSinceEvent: Int = 0,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isPowerActive,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xF2003844),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            shadowElevation = 8.dp,
            modifier = Modifier.testTag("overdrive_status_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡ OVERDRIVE ",
                    color = Color(0xFFFFEA00),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp
                )
                Text(
                    text = "${powerTimer.toInt().coerceAtLeast(1)}s",
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (powerChancesRemaining > 0) {
                    Spacer(modifier = Modifier.width(5.dp))
                    repeat(powerChancesRemaining) {
                        Text(
                            text = "🛡️",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
