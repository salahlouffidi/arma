package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onMove: (Float, Float) -> Unit
) {
    val sizePx = 180f
    val radius = sizePx / 2f
    val handleRadius = 36f

    var handleOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(160.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val drag = offset - center
                        val dist = sqrt(drag.x * drag.x + drag.y * drag.y)
                        val maxDist = radius - handleRadius
                        val clamped = if (dist > maxDist) {
                            val angle = atan2(drag.y, drag.x)
                            Offset(cos(angle) * maxDist, sin(angle) * maxDist)
                        } else {
                            drag
                        }
                        handleOffset = clamped
                        onMove(clamped.x / maxDist, clamped.y / maxDist)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newPos = handleOffset + dragAmount
                        val dist = sqrt(newPos.x * newPos.x + newPos.y * newPos.y)
                        val maxDist = radius - handleRadius
                        val clamped = if (dist > maxDist) {
                            val angle = atan2(newPos.y, newPos.x)
                            Offset(cos(angle) * maxDist, sin(angle) * maxDist)
                        } else {
                            newPos
                        }
                        handleOffset = clamped
                        onMove(clamped.x / maxDist, clamped.y / maxDist)
                    },
                    onDragEnd = {
                        handleOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        handleOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = Color(0x33FFFFFF),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0x88FFB74D),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            drawCircle(
                color = Color(0x44FFB74D),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            val handleCenter = center + handleOffset
            drawCircle(
                color = Color(0x66FF9800),
                radius = handleRadius + 6f,
                center = handleCenter
            )
            drawCircle(
                color = Color(0xFFFF9800),
                radius = handleRadius,
                center = handleCenter
            )
            drawCircle(
                color = Color(0xFFFFE082),
                radius = handleRadius * 0.5f,
                center = handleCenter
            )
        }
    }
}
