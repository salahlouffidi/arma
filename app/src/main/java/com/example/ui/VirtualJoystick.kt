package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.layout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val CONTROL_PREFS = "armasnake_settings"
private const val ACTIVE_SINGLE_ROUND = "active_single_joystick_stage_id"

private fun joystickRightKey(roundId: Int) = "single_joystick_right_round_$roundId"

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    isBlueTheme: Boolean = false,
    primaryColor: Color? = null,
    accentColor: Color? = null,
    testTag: String = "virtual_joystick",
    label: String? = null,
    onMove: (inputX: Float, inputY: Float) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val centerPx = sizePx / 2f
    val baseRadiusPx = sizePx * 0.44f
    val maxDragRadiusPx = baseRadiusPx * 0.72f
    val knobRadiusPx = sizePx * 0.20f

    val effectivePrimary = primaryColor ?: if (isBlueTheme) Color(0xFF00E5FF) else Color(0xFFFFD54F)
    val effectiveAccent = accentColor ?: if (isBlueTheme) Color(0xFF0091EA) else Color(0xFFD4AF37)

    // Only the single-player joystick is configurable. Dual rounds stay fixed.
    val activeRoundId = context.getSharedPreferences(CONTROL_PREFS, 0)
        .getInt(ACTIVE_SINGLE_ROUND, 1)
    val singleJoystickRight = testTag == "virtual_joystick" &&
        context.getSharedPreferences(CONTROL_PREFS, 0)
            .getBoolean(joystickRightKey(activeRoundId), false)

    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val currentAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.88f else 0.42f,
        animationSpec = tween(durationMillis = 180),
        label = "joystick_alpha"
    )

    val sideAwareModifier = if (singleJoystickRight) {
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val rightMargin = 22.dp.roundToPx()
            val width = constraints.maxWidth
            val x = (width - placeable.width - rightMargin).coerceAtLeast(0)
            layout(width, placeable.height) { placeable.placeRelative(x, 0) }
        }
    } else Modifier

    Box(
        modifier = modifier
            .then(sideAwareModifier)
            .size(size)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val dx = offset.x - centerPx
                        val dy = offset.y - centerPx
                        val dist = sqrt(dx * dx + dy * dy)
                        val angle = atan2(dy, dx)
                        val clampedDist = dist.coerceAtMost(maxDragRadiusPx)
                        knobOffsetX = cos(angle) * clampedDist
                        knobOffsetY = sin(angle) * clampedDist
                        onMove(
                            (knobOffsetX / maxDragRadiusPx).coerceIn(-1f, 1f),
                            (knobOffsetY / maxDragRadiusPx).coerceIn(-1f, 1f)
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = knobOffsetX + dragAmount.x
                        val newY = knobOffsetY + dragAmount.y
                        val dist = sqrt(newX * newX + newY * newY)
                        val angle = atan2(newY, newX)
                        val clampedDist = dist.coerceAtMost(maxDragRadiusPx)
                        knobOffsetX = cos(angle) * clampedDist
                        knobOffsetY = sin(angle) * clampedDist
                        onMove(
                            (knobOffsetX / maxDragRadiusPx).coerceIn(-1f, 1f),
                            (knobOffsetY / maxDragRadiusPx).coerceIn(-1f, 1f)
                        )
                    },
                    onDragEnd = {
                        isDragging = false
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        isDragging = false
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                        onMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(centerPx, centerPx)
            drawCircle(Color.Black.copy(alpha = 0.35f * currentAlpha), baseRadiusPx + 4f, center)

            val baseDiskColors = if (isBlueTheme) {
                listOf(Color(0x350A2440), Color(0x60041220))
            } else {
                listOf(Color(0x353E2723), Color(0x601A0E05))
            }
            drawCircle(
                brush = Brush.radialGradient(baseDiskColors, center, baseRadiusPx),
                radius = baseRadiusPx,
                center = center,
                alpha = currentAlpha
            )

            val ringActive = if (isBlueTheme) Color(0xFF00E5FF) else Color(0xFFFFD54F)
            val ringIdle = if (isBlueTheme) Color(0x6000B0FF) else Color(0x60D4AF37)
            drawCircle(
                color = if (isDragging) ringActive.copy(alpha = currentAlpha) else ringIdle.copy(alpha = currentAlpha),
                radius = baseRadiusPx,
                center = center,
                style = Stroke(width = if (isDragging) 2.5f else 1.5f)
            )

            val arrowDist = baseRadiusPx * 0.76f
            val arrowSize = 6f * (sizePx / 130f)
            val indicatorActive = if (isBlueTheme) Color(0xFF80D8FF) else Color(0xFFFFE082)
            val indicatorIdle = if (isBlueTheme) Color(0x8000B0FF) else Color(0x80D4AF37)
            val indicatorColor = if (isDragging) indicatorActive.copy(alpha = currentAlpha) else indicatorIdle.copy(alpha = currentAlpha)

            drawPath(Path().apply {
                moveTo(center.x, center.y - arrowDist - arrowSize)
                lineTo(center.x - arrowSize, center.y - arrowDist + arrowSize)
                lineTo(center.x + arrowSize, center.y - arrowDist + arrowSize)
                close()
            }, color = indicatorColor)
            drawPath(Path().apply {
                moveTo(center.x, center.y + arrowDist + arrowSize)
                lineTo(center.x - arrowSize, center.y + arrowDist - arrowSize)
                lineTo(center.x + arrowSize, center.y + arrowDist - arrowSize)
                close()
            }, color = indicatorColor)
            drawPath(Path().apply {
                moveTo(center.x - arrowDist - arrowSize, center.y)
                lineTo(center.x - arrowDist + arrowSize, center.y - arrowSize)
                lineTo(center.x - arrowDist + arrowSize, center.y + arrowSize)
                close()
            }, color = indicatorColor)
            drawPath(Path().apply {
                moveTo(center.x + arrowDist + arrowSize, center.y)
                lineTo(center.x + arrowDist - arrowSize, center.y - arrowSize)
                lineTo(center.x + arrowDist - arrowSize, center.y + arrowSize)
                close()
            }, color = indicatorColor)

            val guideLen = baseRadiusPx * 0.45f
            drawLine(Color(0x25FFFFFF).copy(alpha = currentAlpha), Offset(center.x - guideLen, center.y), Offset(center.x + guideLen, center.y), 1f, StrokeCap.Round)
            drawLine(Color(0x25FFFFFF).copy(alpha = currentAlpha), Offset(center.x, center.y - guideLen), Offset(center.x, center.y + guideLen), 1f, StrokeCap.Round)

            val knobCenter = Offset(center.x + knobOffsetX, center.y + knobOffsetY)
            if (isDragging) {
                val lineColor = if (isBlueTheme) Color(0x8000E5FF) else Color(0x80FFD54F)
                drawLine(lineColor.copy(alpha = currentAlpha), center, knobCenter, 2.5f, StrokeCap.Round)
            }

            drawCircle(Color.Black.copy(alpha = 0.4f * currentAlpha), knobRadiusPx + 3f, Offset(knobCenter.x + 2f, knobCenter.y + 3f))

            val knobColors = if (isBlueTheme) {
                if (isDragging) listOf(Color(0xFF80D8FF), Color(0xFF00B0FF), Color(0xFF0D47A1))
                else listOf(Color(0xFF40C4FF), Color(0xFF1976D2), Color(0xFF0A2440))
            } else {
                if (isDragging) listOf(Color(0xFFFFE082), Color(0xFFFFB74D), Color(0xFFE65100))
                else listOf(Color(0xFFFFD54F), Color(0xFFD4AF37), Color(0xFF8D6E63))
            }
            drawCircle(
                brush = Brush.radialGradient(
                    knobColors,
                    Offset(knobCenter.x - knobRadiusPx * 0.25f, knobCenter.y - knobRadiusPx * 0.25f),
                    knobRadiusPx
                ),
                radius = knobRadiusPx,
                center = knobCenter,
                alpha = currentAlpha
            )

            val rimDrag = if (isBlueTheme) Color(0xFFE1F5FE) else Color.White
            val rimIdle = if (isBlueTheme) Color(0xFF80D8FF) else Color(0xFFFFECB3)
            drawCircle(
                color = if (isDragging) rimDrag.copy(alpha = currentAlpha * 0.9f) else rimIdle.copy(alpha = currentAlpha * 0.7f),
                radius = knobRadiusPx,
                center = knobCenter,
                style = Stroke(width = if (isDragging) 2.2f else 1.5f)
            )

            val dotActive = if (isBlueTheme) Color(0xFF01579B) else Color(0xFFE65100)
            val dotIdle = if (isBlueTheme) Color(0xFF0D47A1) else Color(0xFF5D4037)
            val dotHighlight = if (isBlueTheme) Color(0xFF80D8FF) else Color(0xFFFFE082)
            drawCircle(
                color = if (isDragging) dotActive.copy(alpha = currentAlpha) else dotIdle.copy(alpha = currentAlpha),
                radius = knobRadiusPx * 0.32f,
                center = knobCenter
            )
            drawCircle(dotHighlight.copy(alpha = currentAlpha), knobRadiusPx * 0.14f, knobCenter)
        }

        if (label != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xDD140B06),
                border = BorderStroke(1.dp, effectivePrimary.copy(alpha = 0.85f))
            ) {
                Text(
                    text = label,
                    color = effectivePrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
