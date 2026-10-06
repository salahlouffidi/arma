package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.StageTheme
import com.example.game.WorldRenderer

/**
 * ARMASNAKE console design language.
 * Signature motifs: dark glossy glass, golden armadillo gold + cyan energy highlights,
 * and an environment-dependent accent that the whole screen inherits.
 */
object ArmaUi {
    val Gold = Color(0xFFFFC94D)
    val GoldDeep = Color(0xFFD4A017)
    val Cyan = Color(0xFF2EE6FF)
    val Ink = Color(0xFF07070B)
    val Glass = Color(0x8C0D0D14)
    val GlassStrong = Color(0xCC0B0B12)
    val GlassStroke = Color(0x2EFFFFFF)
    val TextPrimary = Color(0xFFF5F1E8)
    val TextSecondary = Color(0xB3F5F1E8)
    val TextDim = Color(0x80F5F1E8)
    val Danger = Color(0xFFFF4D5E)
    val Success = Color(0xFF6FE3A1)

    /** Gold → warm-white title gradient used for big typography. */
    val TitleBrush: Brush = Brush.verticalGradient(listOf(Color(0xFFFFF4D6), Color(0xFFFFD36B), Color(0xFFD4A017)))
}

/** Continuous seconds clock for ambient world animation (read inside draw lambdas only → cheap). */
@Composable
fun rememberWorldTime(): State<Float> {
    val transition = rememberInfiniteTransition(label = "worldClock")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(600_000, easing = LinearEasing), RepeatMode.Restart),
        label = "worldSeconds"
    )
}

/**
 * Full-screen procedural environment that smoothly crossfades when [theme] changes
 * (e.g. while browsing rounds). [parallax] is a lambda so camera drift never recomposes callers.
 */
@Composable
fun WorldBackdrop(
    theme: StageTheme,
    modifier: Modifier = Modifier,
    parallax: () -> Float = { 0f },
    detail: Int = 2,
    dim: Float = 0f
) {
    val time by rememberWorldTime()
    var current by remember { mutableStateOf(theme) }
    var previous by remember { mutableStateOf<StageTheme?>(null) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(theme) {
        if (theme != current) {
            previous = current
            current = theme
            fade.snapTo(0f)
            fade.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            previous = null
        }
    }

    Box(modifier = modifier) {
        val prev = previous
        if (prev != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                WorldRenderer.drawWorld(this, size.width, size.height, prev, time, parallax(), detail)
            }
        }
        val shown = current
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value }
        ) {
            WorldRenderer.drawWorld(this, size.width, size.height, shown, time, parallax(), detail)
            if (dim > 0f) drawRect(color = Color.Black.copy(alpha = dim))
        }
    }
}

/** Tap without the stock Android ripple (console UIs answer with motion, not ripples). */
@Composable
fun Modifier.consoleTap(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/**
 * Console-style focusable action. Focused: scales up, brightens, gains a glow and moves forward.
 * Unfocused: sits deeper in the scene (smaller, dimmer). All transitions are springs.
 */
@Composable
fun ConsoleMenuItem(
    label: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    primary: Boolean = false,
    accent: Color = ArmaUi.Gold,
    minWidth: Dp = 220.dp,
    onClick: () -> Unit
) {
    val focus by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "menuFocus"
    )
    val shape = RoundedCornerShape(if (primary) 18.dp else 14.dp)
    val fill = if (primary) {
        Brush.horizontalGradient(
            listOf(
                accent.copy(alpha = 0.55f + 0.4f * focus),
                accent.copy(alpha = 0.25f + 0.35f * focus)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.06f + 0.10f * focus),
                Color.White.copy(alpha = 0.02f + 0.04f * focus)
            )
        )
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                val s = 0.94f + 0.10f * focus
                scaleX = s
                scaleY = s
                alpha = 0.62f + 0.38f * focus
                translationX = 14.dp.toPx() * focus
            }
            .clip(shape)
            .background(fill, shape)
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) accent.copy(alpha = 0.9f) else ArmaUi.GlassStroke,
                shape = shape
            )
            .consoleTap(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = if (primary) 14.dp else 10.dp)
    ) {
        Column {
            Text(
                text = label,
                color = if (primary) Color(0xFF1A1206) else ArmaUi.TextPrimary,
                fontSize = if (primary) 22.sp else 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = if (primary) 4.sp else 2.sp
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    color = if (primary) Color(0xCC1A1206) else ArmaUi.TextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }
        // Focus glow line on the leading edge ("moves forward" cue)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer { alpha = focus; translationX = (-22).dp.toPx() }
                .width(3.dp)
                .height(if (primary) 30.dp else 22.dp)
                .background(accent, RoundedCornerShape(2.dp))
        )
    }
}

/** Thin semi-transparent console surface. */
fun Modifier.glassPanel(radius: Dp = 16.dp, strong: Boolean = false, stroke: Color = ArmaUi.GlassStroke): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(if (strong) ArmaUi.GlassStrong else ArmaUi.Glass, shape)
        .border(1.dp, stroke, shape)
}

/** Five elegant capsules; filled ones take the world accent. */
@Composable
fun DifficultyMeter(level: Int, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .width(if (i <= level) 16.dp else 10.dp)
                    .height(5.dp)
                    .background(
                        if (i <= level) accent else Color.White.copy(alpha = 0.18f),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/** Small label/value pair used on cards and result screens. */
@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = ArmaUi.TextPrimary, big: Boolean = false) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(text = label, color = ArmaUi.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(text = value, color = valueColor, fontSize = if (big) 26.sp else 17.sp, fontWeight = FontWeight.Black)
    }
}

/**
 * Premium power-up chip: icon + label + seconds with a draining radial timer.
 * [fraction] in 0..1 (pass 1f with [seconds] < 0 for non-timed powers such as SHIELD).
 */
@Composable
fun PowerChip(icon: String, label: String, seconds: Float, fraction: Float, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .glassPanel(radius = 20.dp, stroke = accent.copy(alpha = 0.55f))
            .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                drawCircle(color = Color.White.copy(alpha = 0.15f), radius = size.minDimension / 2f - stroke / 2f, style = Stroke(width = stroke))
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(text = icon, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        if (seconds >= 0f) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format(java.util.Locale.US, "%.1f", seconds),
                color = ArmaUi.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Ambient helper for gentle floating motion (title bob, idle drift). */
@Composable
fun rememberBreath(periodMs: Int = 3200): State<Float> {
    val t = rememberInfiniteTransition(label = "breath")
    return t.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathValue"
    )
}

@Composable
internal fun SectionLabel(text: String, color: Color = ArmaUi.TextDim) {
    Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
}

@Composable
internal fun VSpace(h: Dp) = Spacer(modifier = Modifier.height(h))

@Composable
internal fun HSpace(w: Dp) = Spacer(modifier = Modifier.width(w))
