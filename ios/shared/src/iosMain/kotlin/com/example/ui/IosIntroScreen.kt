package com.example.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.ExperimentalComposeUiApi
import com.example.game.SoundManager
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView

/** UIView whose AVPlayerLayer always fills its bounds. */
private class PlayerView(val player: AVPlayer) : UIView(frame = platform.CoreGraphics.CGRectZero) {
    private val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player).apply {
        videoGravity = AVLayerVideoGravityResizeAspect
    }

    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}

/**
 * iOS version of the opening LOUFFI EDITION video: plays once fullscreen,
 * tap to skip, then goes to the main menu. Same signature as Android's IntroScreen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IntroScreen(
    soundManager: SoundManager,
    onFinished: () -> Unit
) {
    var hasEnded by remember { mutableStateOf(false) }
    val finish = { if (!hasEnded) { hasEnded = true; onFinished() } }

    val player = remember {
        NSBundle.mainBundle.URLForResource("louffi_edition_logo_intro_animation_202609082217", "mp4")
            ?.let { AVPlayer(uRL = it) }
    }

    DisposableEffect(player) {
        val observer = player?.let {
            NSNotificationCenter.defaultCenter.addObserverForName(
                AVPlayerItemDidPlayToEndTimeNotification, it.currentItem, NSOperationQueue.mainQueue
            ) { _ -> finish() }
        }
        player?.play()
        onDispose {
            player?.pause()
            observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                player?.pause(); finish()
            }
            .testTag("intro_video_screen"),
        contentAlignment = Alignment.Center
    ) {
        if (player != null) {
            UIKitView(
                factory = { PlayerView(player) },
                modifier = Modifier.fillMaxSize(),
                properties = UIKitInteropProperties(isInteractive = false)
            )
        } else {
            LaunchedEffect(Unit) { finish() }
        }

        Surface(
            color = Color(0xB0000000),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x60D4AF37)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .clickable { player?.pause(); finish() }
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
