package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.AppleItem
import com.example.game.ArmadilloState
import com.example.game.FruitType
import com.example.game.SnakeState
import com.example.game.StageTheme
import com.example.game.TableRenderer
import com.example.game.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * ARMASNAKE "pro" design system: one palette, one set of surfaces and buttons, and
 * a live rendered tabletop used as the backdrop of every menu.
 */
object ArmaColors {
    val Gold = Color(0xFFFFD45A)
    val GoldDeep = Color(0xFFC8901E)
    val GoldDark = Color(0xFF7A4E0A)
    val Ink = Color(0xFF1A0E06)
    val Panel = Color(0xE6201209)
    val PanelHi = Color(0xF02E1A0D)
    val Stroke = Color(0x55FFD45A)
    val TextHi = Color(0xFFFFF4DC)
    val TextMid = Color(0xFFE8CFA6)
    val TextLow = Color(0xFF9C8466)
    val Green = Color(0xFF7CE38B)
    val Blue = Color(0xFF6EC6FF)
    val Danger = Color(0xFFFF6B5B)
}

val GoldTextBrush = Brush.verticalGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFD45A), Color(0xFFE09A1E)))

fun proTitleStyle(size: TextUnit, letter: TextUnit = 2.sp) = TextStyle(
    brush = GoldTextBrush,
    fontSize = size,
    fontWeight = FontWeight.Black,
    letterSpacing = letter,
    shadow = Shadow(Color(0xCC000000), Offset(0f, 6f), 14f)
)

/** Full-screen menu backdrop: a slowly animated, dimmed live board with vignette. */
@Composable
fun ProBackdrop(
    modifier: Modifier = Modifier,
    theme: StageTheme = StageTheme.GARDEN,
    dim: Float = 0.55f,
    content: @Composable BoxScope.() -> Unit
) {
    val t by rememberInfiniteTransition(label = "backdrop").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    Box(modifier.fillMaxSize().background(Color(0xFF0B0603))) {
        Canvas(Modifier.fillMaxSize().clipToBounds()) {
            drawBackdropScene(this, theme, dim, t)
        }
        content()
    }
}

/** Same animated backdrop as [ProBackdrop], usable as a background modifier. */
fun Modifier.proBackdrop(theme: StageTheme = StageTheme.GARDEN, dim: Float = 0.7f): Modifier = composed {
    val t by rememberInfiniteTransition(label = "backdrop").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    this.background(Color(0xFF0B0603)).clipToBounds().drawBehind { drawBackdropScene(this, theme, dim, t) }
}

private fun drawBackdropScene(scope: DrawScope, theme: StageTheme, dim: Float, t: Float) = with(scope) {
            val halfW = size.width * 0.47f
            val halfH = size.height * 0.40f
            val c = Offset(size.width / 2f, size.height * 0.52f)
            TableRenderer.drawTable(this, c, halfW, halfH, theme)
            drawDemoActors(this, c, halfW, halfH, t, theme)
            // dim + vignette so UI reads on top
            drawRect(Color.Black.copy(alpha = dim))
            drawRect(
                Brush.radialGradient(
                    0f to Color.Transparent,
                    0.6f to Color.Black.copy(alpha = 0.15f),
                    1f to Color.Black.copy(alpha = 0.75f),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.62f
                )
            )
}

/** Idle "attract mode": an armadillo circling, a snake tracing a figure eight, fruit bobbing. */
fun drawDemoActors(scope: DrawScope, c: Offset, halfW: Float, halfH: Float, t: Float, theme: StageTheme) {
    val ax = cos(t * 0.6f) * halfW * 0.42f
    val ay = sin(t * 0.6f) * halfH * 0.45f
    val heading = t * 0.6f + (Math.PI / 2).toFloat()
    TableRenderer.drawApples(
        scope,
        listOf(
            AppleItem(1, Vector2(-halfW * 0.62f, -halfH * 0.5f), bobPhase = t * 3f),
            AppleItem(2, Vector2(halfW * 0.6f, halfH * 0.48f), bobPhase = t * 3f + 1f, fruitType = FruitType.ESCAPE_GOLDEN, sparkleTimer = t),
            AppleItem(3, Vector2(halfW * 0.05f, -halfH * 0.7f), bobPhase = t * 3f + 2f)
        ),
        c
    )
    TableRenderer.drawArmadillo(
        scope,
        ArmadilloState(pos = Vector2(ax, ay), headingAngle = heading, legStepPhase = t * 14f, tailWagPhase = t * 6f, invulnerableTimer = 0f),
        c
    )
    // snake on a lemniscate
    fun lem(p: Float): Vector2 {
        val d = 1f + sin(p) * sin(p)
        return Vector2(halfW * 0.7f * cos(p) / d, halfH * 0.75f * sin(p) * cos(p) / d)
    }
    val p0 = -t * 0.35f
    val head = lem(p0)
    val segs = (1..16).map { lem(p0 + it * 0.075f) }
    val next = lem(p0 - 0.02f)
    val ang = kotlin.math.atan2(next.y - head.y, next.x - head.x)
    TableRenderer.drawSnake(
        scope,
        SnakeState(headPos = head, headAngle = ang, bodySegments = segs, slitherPhase = t * 6f, tongueVisible = (t * 2f) % 1f < 0.4f, tongueFlickTimer = t),
        c,
        snakeIndex = if (theme == StageTheme.DARK_TEMPLE || theme == StageTheme.VOLCANO) 1 else 0
    )
}

/** Small rendered preview of a stage theme (used on round cards). */
@Composable
fun ThemePreview(theme: StageTheme, modifier: Modifier = Modifier, locked: Boolean = false) {
    Canvas(modifier.clipToBounds()) {
        // draw at a fixed virtual size, then scale to fit, so rails/brackets keep proportions
        val vw = 760f
        val vh = vw * size.height / size.width
        val s = size.width / vw
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            val c = Offset(vw / 2f, vh / 2f)
            TableRenderer.drawTable(this, c, vw / 2f - 34f, vh / 2f - 34f, theme)
            if (!locked) drawDemoActors(this, c, vw / 2f - 34f, vh / 2f - 34f, theme.ordinal * 1.7f + 2f, theme)
        }
        if (locked) drawRect(Color.Black.copy(alpha = 0.62f))
    }
}

/** Glassy dark panel with a gold hairline and soft depth. */
@Composable
fun ProPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    highlighted: Boolean = false,
    accent: Color = ArmaColors.Gold,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .shadow(if (highlighted) 18.dp else 10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(ArmaColors.PanelHi, ArmaColors.Panel, Color(0xF0140A04))))
            .border(
                BorderStroke(
                    if (highlighted) 2.dp else 1.dp,
                    Brush.verticalGradient(
                        if (highlighted) listOf(accent, accent.copy(alpha = 0.5f))
                        else listOf(Color(0x66FFE7A8), Color(0x22FFE7A8))
                    )
                ),
                shape
            )
            .drawBehind {
                // top inner sheen
                drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), endY = size.height * 0.35f)
                )
            },
        content = content
    )
}

enum class ProButtonStyle { Primary, Secondary, Ghost }

/** Beveled game button with press feedback. */
@Composable
fun ProButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ProButtonStyle = ProButtonStyle.Primary,
    height: Dp = 48.dp,
    fontSize: TextUnit = 14.sp,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp),
    leading: (@Composable RowScope.() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(height / 2.6f)
    val (bg, fg, border) = when (style) {
        ProButtonStyle.Primary -> Triple(
            Brush.verticalGradient(listOf(Color(0xFFFFE89A), Color(0xFFFFC93C), Color(0xFFD98E12))),
            ArmaColors.Ink,
            Brush.verticalGradient(listOf(Color(0xFFFFF7D6), Color(0xFF8A5A0A)))
        )
        ProButtonStyle.Secondary -> Triple(
            Brush.verticalGradient(listOf(Color(0xFF4A2C15), Color(0xFF2A170A))),
            ArmaColors.Gold,
            Brush.verticalGradient(listOf(Color(0xCCFFD45A), Color(0x55FFD45A)))
        )
        ProButtonStyle.Ghost -> Triple(
            Brush.verticalGradient(listOf(Color(0x66000000), Color(0x88000000))),
            ArmaColors.TextMid,
            Brush.verticalGradient(listOf(Color(0x55FFE7A8), Color(0x22FFE7A8)))
        )
    }
    Box(
        modifier
            .scale(if (pressed) 0.96f else 1f)
            .height(height)
            .shadow(if (pressed || style == ProButtonStyle.Ghost) 2.dp else 8.dp, shape, spotColor = Color.Black)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.2.dp, border), shape)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = if (style == ProButtonStyle.Primary) 0.35f else 0.10f), Color.Transparent), endY = size.height * 0.5f)
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            androidx.compose.material3.Text(
                text = text,
                color = if (enabled) fg else fg.copy(alpha = 0.4f),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}

/** Compact pill label. */
@Composable
fun ProChip(text: String, modifier: Modifier = Modifier, color: Color = ArmaColors.TextMid, filled: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color.copy(alpha = 0.22f) else Color(0x66000000))
            .border(BorderStroke(1.dp, color.copy(alpha = if (filled) 0.8f else 0.35f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        androidx.compose.material3.Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, maxLines = 1)
    }
}

@Composable
fun ProDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0x88FFD45A), Color.Transparent)))
    )
}

/** Small rendered mascots for headers (no emoji). */
@Composable
fun MascotArmadillo(modifier: Modifier = Modifier, blue: Boolean = false, heading: Float = 0f) {
    Canvas(modifier) {
        val s = size.minDimension / 96f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            TableRenderer.drawArmadillo(
                this,
                ArmadilloState(pos = Vector2.Zero, headingAngle = heading, invulnerableTimer = 0f, legStepPhase = 1f),
                Offset(48f, 48f),
                isBlue = blue
            )
        }
    }
}

@Composable
fun MascotSnake(modifier: Modifier = Modifier, purple: Boolean = false) {
    Canvas(modifier) {
        val s = size.minDimension / 120f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            val segs = (1..12).map { i -> Vector2(i * 7.5f, sin(i * 0.55f) * 14f) }
            TableRenderer.drawSnake(
                this,
                SnakeState(headPos = Vector2(-4f, 0f), headAngle = Math.PI.toFloat(), bodySegments = segs, slitherPhase = 0f, tongueVisible = true),
                Offset(42f, 60f),
                snakeIndex = if (purple) 1 else 0
            )
        }
    }
}

/**
 * Drop-in replacement for material3 `Button(...)` used across dialogs: same parameters, but
 * rendered with the ARMASNAKE bevel style. Bright container colours map to the gold primary
 * style, dark ones to the bronze secondary style, keeping each call site's intent.
 */
@Composable
fun ProStyledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(),
    elevation: androidx.compose.material3.ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = colors.containerColor
    val lum = 0.299f * base.red + 0.587f * base.green + 0.114f * base.blue
    val primary = lum > 0.45f && base.alpha > 0.5f
    val accent = if (primary) base else colors.contentColor
    val bg = if (primary) {
        Brush.verticalGradient(listOf(base.copy(alpha = 1f).let { lerpColor(it, Color.White, 0.35f) }, base.copy(alpha = 1f), lerpColor(base.copy(alpha = 1f), Color.Black, 0.25f)))
    } else {
        Brush.verticalGradient(listOf(lerpColor(base.copy(alpha = 1f), Color.White, 0.08f), lerpColor(base.copy(alpha = 1f), Color.Black, 0.35f)))
    }
    val rimColor = border?.let { accent } ?: if (primary) Color(0xFFFFF4C8) else accent.copy(alpha = 0.55f)
    val s = RoundedCornerShape(14.dp)
    Box(
        modifier
            .scale(if (pressed) 0.97f else 1f)
            .shadow(if (primary && !pressed) 8.dp else 2.dp, s, spotColor = Color.Black)
            .clip(s)
            .background(bg)
            .border(BorderStroke(1.2.dp, Brush.verticalGradient(listOf(rimColor, rimColor.copy(alpha = 0.25f)))), s)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = if (primary) 0.32f else 0.09f), Color.Transparent), endY = size.height * 0.5f)
                )
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (enabled) colors.contentColor else colors.contentColor.copy(alpha = 0.4f),
            androidx.compose.material3.LocalTextStyle provides TextStyle(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                content = content
            )
        }
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = androidx.compose.ui.graphics.lerp(a, b, f)
