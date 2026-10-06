package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.Armadillo
import com.example.game.FruitItem
import com.example.game.FruitType
import com.example.game.GameEngine
import com.example.game.HazardItem
import com.example.game.HazardType
import com.example.game.PowerupItem
import com.example.game.ScreenState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArmadilloEscapeScreen(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }

    val overdrivePulse = remember { Animatable(1f) }
    LaunchedEffect(engine.armadillo.isOverdrive) {
        if (engine.armadillo.isOverdrive) {
            overdrivePulse.animateTo(
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            overdrivePulse.snapTo(1f)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        engine.screenWidth = w
        engine.screenHeight = h

        LaunchedEffect(engine.screenState) {
            var lastTime = 0L
            while (engine.screenState == ScreenState.PLAYING) {
                withFrameNanos { timeNanos ->
                    if (lastTime != 0L) {
                        val dt = ((timeNanos - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
                        engine.update(dt, joyX, joyY)
                    }
                    lastTime = timeNanos
                }
            }
        }

        val stageConfig = engine.stages.getOrNull((engine.currentStage - 1).coerceAtMost(engine.stages.size - 1))
            ?: engine.stages.last()

        Canvas(modifier = Modifier.fillMaxSize().testTag("game_canvas")) {
            val shakeOffsetX = (Math.random().toFloat() - 0.5f) * engine.screenShake
            val shakeOffsetY = (Math.random().toFloat() - 0.5f) * engine.screenShake

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(stageConfig.skyColorTop, stageConfig.skyColorBottom),
                    startY = 0f,
                    endY = size.height
                )
            )

            val dunePath = Path().apply {
                moveTo(0f, size.height * 0.45f)
                cubicTo(
                    size.width * 0.25f, size.height * 0.42f,
                    size.width * 0.75f, size.height * 0.48f,
                    size.width, size.height * 0.44f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = dunePath, color = stageConfig.duneColor.copy(alpha = 0.45f))

            val foregroundDune = Path().apply {
                moveTo(0f, size.height * 0.65f)
                cubicTo(
                    size.width * 0.35f, size.height * 0.68f,
                    size.width * 0.70f, size.height * 0.62f,
                    size.width, size.height * 0.66f
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path = foregroundDune, color = stageConfig.duneColor.copy(alpha = 0.75f))

            for (p in engine.particles) {
                drawCircle(
                    color = p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)),
                    radius = p.radius,
                    center = Offset(p.x + shakeOffsetX, p.y + shakeOffsetY)
                )
            }

            for (f in engine.fruits) {
                drawFruit(f, shakeOffsetX, shakeOffsetY)
            }

            for (pu in engine.powerups) {
                drawPowerup(pu, shakeOffsetX, shakeOffsetY)
            }

            for (hItem in engine.hazards) {
                drawHazard(hItem, shakeOffsetX, shakeOffsetY)
            }

            drawArmadilloPlayer(engine.armadillo, overdrivePulse.value, shakeOffsetX, shakeOffsetY)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    for (i in 1..engine.armadillo.maxHealth) {
                        val isFull = i <= engine.armadillo.health
                        Icon(
                            imageVector = if (isFull) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Health",
                            tint = if (isFull) Color(0xFFFF1744) else Color(0x66FFFFFF),
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 4.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0x88000000),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Stage ${engine.currentStage}: ${stageConfig.name}",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { engine.pauseGame() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${engine.score} PTS",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (engine.combo > 1) {
                        Text(
                            text = "COMBO x${String.format("%.1f", engine.comboMultiplier)}! (${engine.combo})",
                            color = Color(0xFFFFEB3B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val overdrivePct = if (engine.armadillo.isOverdrive) {
                        (engine.armadillo.overdriveTime / 9.0f).coerceIn(0f, 1f)
                    } else {
                        (engine.armadillo.overdriveMeter / 100f).coerceIn(0f, 1f)
                    }

                    Text(
                        text = if (engine.armadillo.isOverdrive) "⚡ OVERDRIVE ACTIVE!" else "FRENZY ${engine.armadillo.overdriveMeter.toInt()}%",
                        color = if (engine.armadillo.isOverdrive) Color(0xFF00E5FF) else Color(0xFFFFB74D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { overdrivePct },
                        modifier = Modifier
                            .width(130.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (engine.armadillo.isOverdrive) Color(0xFF00E5FF) else Color(0xFFFF9800),
                        trackColor = Color(0x44FFFFFF)
                    )
                }
            }
        }

        for (popup in engine.popups) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = (popup.x - 60f).coerceAtLeast(0f).dp,
                        top = (popup.y - 30f).coerceAtLeast(0f).dp
                    )
            ) {
                Text(
                    text = popup.text,
                    color = popup.color.copy(alpha = popup.life.coerceIn(0f, 1f)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        VirtualJoystick(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 48.dp)
                .testTag("virtual_joystick"),
            onMove = { x, y ->
                joyX = x
                joyY = y
            }
        )
    }
}

private fun DrawScope.drawFruit(fruit: FruitItem, shakeX: Float, shakeY: Float) {
    val cx = fruit.x + shakeX
    val cy = fruit.y + shakeY + sin(fruit.bounceTick) * 6f
    val r = fruit.radius

    drawCircle(
        color = fruit.type.color.copy(alpha = 0.35f),
        radius = r + 8f,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = fruit.type.color,
        radius = r,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = Color(0x88FFFFFF),
        radius = r * 0.35f,
        center = Offset(cx - r * 0.3f, cy - r * 0.3f)
    )

    if (fruit.type == FruitType.STAR_GEM) {
        drawCircle(
            color = Color.White,
            radius = r * 0.2f,
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawPowerup(pu: PowerupItem, shakeX: Float, shakeY: Float) {
    val cx = pu.x + shakeX
    val cy = pu.y + shakeY + sin(pu.bobTick) * 8f
    val r = pu.radius

    drawCircle(
        color = pu.type.color.copy(alpha = 0.4f),
        radius = r + 12f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = pu.type.color,
        radius = r,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color.White,
        radius = r * 0.4f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawHazard(hazard: HazardItem, shakeX: Float, shakeY: Float) {
    val cx = hazard.x + shakeX
    val cy = hazard.y + shakeY
    val r = hazard.radius

    rotate(hazard.rotation, pivot = Offset(cx, cy)) {
        when (hazard.type) {
            HazardType.CACTUS -> {
                drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(cx - r * 0.35f, cy - r),
                    size = Size(r * 0.7f, r * 2f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = Color(0xFF388E3C),
                    topLeft = Offset(cx - r * 0.9f, cy - r * 0.4f),
                    size = Size(r * 0.6f, r * 0.4f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF388E3C),
                    topLeft = Offset(cx + r * 0.3f, cy - r * 0.1f),
                    size = Size(r * 0.6f, r * 0.4f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
            HazardType.BOULDER -> {
                drawCircle(
                    color = Color(0xFF4E342E),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF3E2723),
                    radius = r * 0.6f,
                    center = Offset(cx + r * 0.2f, cy + r * 0.2f)
                )
            }
            HazardType.RATTLE_SNAKE -> {
                drawCircle(
                    color = Color(0xFF6D4C41),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFFFFB300),
                    radius = r * 0.65f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFFFF1744),
                    radius = 4f,
                    center = Offset(cx + r * 0.3f, cy - r * 0.2f)
                )
            }
            HazardType.TUMBLEWEED -> {
                drawCircle(
                    color = Color(0xFFFFA000),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 5f)
                )
                for (i in 0 until 8) {
                    val angle = (i * 45f) * (Math.PI / 180.0).toFloat()
                    drawLine(
                        color = Color(0xFFFF8F00),
                        start = Offset(cx, cy),
                        end = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
                        strokeWidth = 3f
                    )
                }
            }
            HazardType.SCORPION -> {
                drawCircle(
                    color = Color(0xFFBF360C),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

private fun DrawScope.drawArmadilloPlayer(
    armadillo: Armadillo,
    pulse: Float,
    shakeX: Float,
    shakeY: Float
) {
    val cx = armadillo.x + shakeX
    val cy = armadillo.y + shakeY
    val r = armadillo.radius

    if (armadillo.isOverdrive) {
        drawCircle(
            color = Color(0x6600E5FF),
            radius = r * pulse + 14f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xAA00E5FF),
            radius = r * pulse + 6f,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )
    } else if (armadillo.shieldTime > 0f) {
        drawCircle(
            color = Color(0x5500B0FF),
            radius = r + 10f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF00B0FF),
            radius = r + 8f,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )
    }

    if (armadillo.invulnerableTime > 0f && (armadillo.invulnerableTime * 15).toInt() % 2 == 0) {
        return
    }

    rotate(armadillo.angle, pivot = Offset(cx, cy)) {
        val shellColor = if (armadillo.isOverdrive) Color(0xFF80D8FF) else Color(0xFFFFB300)
        val bandColor = if (armadillo.isOverdrive) Color(0xFF00B0FF) else Color(0xFFE65100)

        drawCircle(
            color = shellColor,
            radius = r,
            center = Offset(cx, cy)
        )

        for (i in -1..1) {
            val bandY = cy + (i * r * 0.4f)
            drawRoundRect(
                color = bandColor,
                topLeft = Offset(cx - r * 0.7f, bandY - 3f),
                size = Size(r * 1.4f, 7f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        drawCircle(
            color = if (armadillo.isOverdrive) Color(0xFFE1F5FE) else Color(0xFFFFE082),
            radius = r * 0.45f,
            center = Offset(cx + r * 0.7f, cy)
        )

        drawCircle(
            color = Color(0xFF212121),
            radius = 3.5f,
            center = Offset(cx + r * 0.75f, cy - 4f)
        )
        drawCircle(
            color = Color.White,
            radius = 1.2f,
            center = Offset(cx + r * 0.77f, cy - 5f)
        )
    }
}
