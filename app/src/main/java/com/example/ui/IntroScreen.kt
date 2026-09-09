package com.example.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.SoundManager

/**
 * Fullscreen one-time opening video: Louffi_Edition_logo_intro_animation_202609082217.mp4
 *
 * Requirements:
 * - Plays fullscreen on initial game launch.
 * - Tap to skip / auto-advances to Main Menu upon completion.
 * - Never plays again when returning from stages or navigating menus.
 */
@Composable
fun IntroScreen(
    soundManager: SoundManager,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var hasEnded by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val rawVideoUri = remember {
        val resId = context.resources.getIdentifier(
            "louffi_edition_logo_intro_animation_202609082217",
            "raw",
            context.packageName
        )
        if (resId != 0) {
            Uri.parse("android.resource://${context.packageName}/$resId")
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (e: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tapping skips the intro video immediately
                if (!hasEnded) {
                    hasEnded = true
                    try {
                        videoViewRef?.stopPlayback()
                    } catch (e: Exception) {}
                    onFinished()
                }
            }
            .testTag("intro_video_screen"),
        contentAlignment = Alignment.Center
    ) {
        if (rawVideoUri != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(rawVideoUri)
                        setOnCompletionListener {
                            if (!hasEnded) {
                                hasEnded = true
                                onFinished()
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            if (!hasEnded) {
                                hasEnded = true
                                onFinished()
                            }
                            true
                        }
                        start()
                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LaunchedEffect(Unit) {
                if (!hasEnded) {
                    hasEnded = true
                    onFinished()
                }
            }
        }

        // Tap to Skip Pill indicator in bottom right corner
        Surface(
            color = Color(0xB0000000),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .clickable {
                    if (!hasEnded) {
                        hasEnded = true
                        try {
                            videoViewRef?.stopPlayback()
                        } catch (e: Exception) {}
                        onFinished()
                    }
                }
                .testTag("skip_intro_button")
        ) {
            Text(
                text = "TAP TO SKIP ⏩",
                color = Color(0xFFFFD54F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}
