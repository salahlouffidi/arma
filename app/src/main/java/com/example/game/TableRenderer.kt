package com.example.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object TableRenderer {

    private data class TableThemeColors(
        val railColors: List<Color>,
        val railHighlight: Color,
        val plankColors: List<Color>,
        val seamColor: Color,
        val seamHighlight: Color,
        val grainLight: Color,
        val grainDark: Color,
        val inlayOuter: Color,
        val inlayInner: Color,
        val diamondColor: Color,
        val cornerColors: List<Color>,
        val rivetColor: Color,
        val rivetHighlight: Color
    )

    private fun getColorsForTheme(theme: StageTheme): TableThemeColors {
        return when (theme) {
            StageTheme.GARDEN -> TableThemeColors(
                railColors = listOf(Color(0xFF3B1E10), Color(0xFF261208), Color(0xFF33190C), Color(0xFF200D06)),
                railHighlight = Color(0x35FFFFFF),
                plankColors = listOf(
                    Color(0xFF4C2A17), Color(0xFF422413), Color(0xFF532E1A), Color(0xFF3F2111),
                    Color(0xFF4A2816), Color(0xFF56301C), Color(0xFF412312), Color(0xFF4E2C19)
                ),
                seamColor = Color(0x80100703),
                seamHighlight = Color(0x22FFFFFF),
                grainLight = Color(0x15FFFFFF),
                grainDark = Color(0x12000000),
                inlayOuter = Color(0x18D4AF37),
                inlayInner = Color(0x10D4AF37),
                diamondColor = Color(0x22D4AF37),
                cornerColors = listOf(Color(0xFFFFDF7A), Color(0xFFC59B27), Color(0xFF8B6C14)),
                rivetColor = Color(0xFF4A380A),
                rivetHighlight = Color(0xFFFFF2A8)
            )
            StageTheme.DESERT -> TableThemeColors(
                railColors = listOf(Color(0xFF5D2912), Color(0xFF451E0C), Color(0xFF3B1A0A), Color(0xFF2A1005)),
                railHighlight = Color(0x30FFE082),
                plankColors = listOf(
                    Color(0xFFC99965), Color(0xFFB88554), Color(0xFFD4A775), Color(0xFFAC7B4B),
                    Color(0xFFC3935F), Color(0xFFBA8757), Color(0xFFCD9D69), Color(0xFFA67545)
                ),
                seamColor = Color(0x7042210B),
                seamHighlight = Color(0x20FFF8E1),
                grainLight = Color(0x18FFF8E1),
                grainDark = Color(0x153E2723),
                inlayOuter = Color(0x30E65100),
                inlayInner = Color(0x20FF8F00),
                diamondColor = Color(0x35FFB300),
                cornerColors = listOf(Color(0xFFFFB74D), Color(0xFFCD7F32), Color(0xFF8C531B)),
                rivetColor = Color(0xFF4E342E),
                rivetHighlight = Color(0xFFFFE082)
            )
            StageTheme.JUNGLE -> TableThemeColors(
                railColors = listOf(Color(0xFF1E2E19), Color(0xFF131F10), Color(0xFF192615), Color(0xFF0F180C)),
                railHighlight = Color(0x30A5D6A7),
                plankColors = listOf(
                    Color(0xFF2A3D24), Color(0xFF23351E), Color(0xFF32472B), Color(0xFF1C2B18),
                    Color(0xFF283A22), Color(0xFF30442A), Color(0xFF20301A), Color(0xFF2B3D25)
                ),
                seamColor = Color(0x800A1408),
                seamHighlight = Color(0x20C8E6C9),
                grainLight = Color(0x15A5D6A7),
                grainDark = Color(0x18000000),
                inlayOuter = Color(0x3500E676),
                inlayInner = Color(0x2069F0AE),
                diamondColor = Color(0x3000E676),
                cornerColors = listOf(Color(0xFF69F0AE), Color(0xFF00BFA5), Color(0xFF1B5E20)),
                rivetColor = Color(0xFF0A2E12),
                rivetHighlight = Color(0xFFB9F6CA)
            )
            StageTheme.MOUNTAINS -> TableThemeColors(
                railColors = listOf(Color(0xFF37474F), Color(0xFF263238), Color(0xFF2F3C42), Color(0xFF1C252A)),
                railHighlight = Color(0x30CFD8DC),
                plankColors = listOf(
                    Color(0xFF455A64), Color(0xFF37474F), Color(0xFF546E7A), Color(0xFF2E3D44),
                    Color(0xFF4B606B), Color(0xFF3C4D55), Color(0xFF506873), Color(0xFF32424A)
                ),
                seamColor = Color(0x8010171B),
                seamHighlight = Color(0x22ECEFF1),
                grainLight = Color(0x16ECEFF1),
                grainDark = Color(0x18102027),
                inlayOuter = Color(0x3090CAF9),
                inlayInner = Color(0x2064B5F6),
                diamondColor = Color(0x2890CAF9),
                cornerColors = listOf(Color(0xFFCFD8DC), Color(0xFF90A4AE), Color(0xFF455A64)),
                rivetColor = Color(0xFF263238),
                rivetHighlight = Color(0xFFFFFFFF)
            )
            StageTheme.VOLCANO -> TableThemeColors(
                railColors = listOf(Color(0xFFBF360C), Color(0xFF3E1308), Color(0xFFD84315), Color(0xFF210804)),
                railHighlight = Color(0x40FF6D00),
                plankColors = listOf(
                    Color(0xFF211512), Color(0xFF1A0F0D), Color(0xFF281916), Color(0xFF170C0A),
                    Color(0xFF241614), Color(0xFF1E110E), Color(0xFF2B1C18), Color(0xFF150A08)
                ),
                seamColor = Color(0x80DD2C00),
                seamHighlight = Color(0x30FFAB40),
                grainLight = Color(0x20FF6E40),
                grainDark = Color(0x30000000),
                inlayOuter = Color(0x45FF3D00),
                inlayInner = Color(0x30FF9100),
                diamondColor = Color(0x40FF3D00),
                cornerColors = listOf(Color(0xFFFF9100), Color(0xFFFF3D00), Color(0xFFBF360C)),
                rivetColor = Color(0xFF3E2723),
                rivetHighlight = Color(0xFFFFD180)
            )
            StageTheme.ICE -> TableThemeColors(
                railColors = listOf(Color(0xFF00838F), Color(0xFF004D40), Color(0xFF006064), Color(0xFF00363A)),
                railHighlight = Color(0x4080DEEA),
                plankColors = listOf(
                    Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color(0xFF80DEEA), Color(0xFFB2DFDB),
                    Color(0xFFD7F5F8), Color(0xFFBFEFF5), Color(0xFF8DE2EC), Color(0xFFA7E7DE)
                ),
                seamColor = Color(0x60006064),
                seamHighlight = Color(0x40FFFFFF),
                grainLight = Color(0x25FFFFFF),
                grainDark = Color(0x10006064),
                inlayOuter = Color(0x4000E5FF),
                inlayInner = Color(0x2518FFFF),
                diamondColor = Color(0x3500E5FF),
                cornerColors = listOf(Color(0xFFE0F7FA), Color(0xFF00E5FF), Color(0xFF00838F)),
                rivetColor = Color(0xFF004D40),
                rivetHighlight = Color(0xFFFFFFFF)
            )
            StageTheme.DARK_TEMPLE -> TableThemeColors(
                railColors = listOf(Color(0xFF1A1224), Color(0xFF100918), Color(0xFF261933), Color(0xFF0A0510)),
                railHighlight = Color(0x35E1BEE7),
                plankColors = listOf(
                    Color(0xFF1E1728), Color(0xFF160F20), Color(0xFF251C32), Color(0xFF120C1C),
                    Color(0xFF21192C), Color(0xFF1A1224), Color(0xFF281E36), Color(0xFF140D1E)
                ),
                seamColor = Color(0x804A148C),
                seamHighlight = Color(0x25EA80FC),
                grainLight = Color(0x15E1BEE7),
                grainDark = Color(0x25000000),
                inlayOuter = Color(0x45D500F9),
                inlayInner = Color(0x30AA00FF),
                diamondColor = Color(0x40E040FB),
                cornerColors = listOf(Color(0xFFE1BEE7), Color(0xFFAB47BC), Color(0xFF4A148C)),
                rivetColor = Color(0xFF120024),
                rivetHighlight = Color(0xFFF3E5F5)
            )
            StageTheme.FINAL_ARENA -> TableThemeColors(
                railColors = listOf(Color(0xFFFFD700), Color(0xFFC59B27), Color(0xFF8B6C14), Color(0xFF5A450A)),
                railHighlight = Color(0x50FFF8E1),
                plankColors = listOf(
                    Color(0xFF3D270F), Color(0xFF2E1C0A), Color(0xFF4A3013), Color(0xFF34210C),
                    Color(0xFF422B11), Color(0xFF311E0B), Color(0xFF4D3315), Color(0xFF291807)
                ),
                seamColor = Color(0x90FFD700),
                seamHighlight = Color(0x40FFFFFF),
                grainLight = Color(0x22FFE082),
                grainDark = Color(0x20000000),
                inlayOuter = Color(0x55FFD700),
                inlayInner = Color(0x35FFC107),
                diamondColor = Color(0x45FFD700),
                cornerColors = listOf(Color(0xFFFFF176), Color(0xFFFFD700), Color(0xFFFF1744)),
                rivetColor = Color(0xFFD50000),
                rivetHighlight = Color(0xFFFFFFFF)
            )
        }
    }

    /** Ambient backdrop colours per theme: (deep edge, mid, warm centre glow, mote colour). */
    private fun backdropColors(theme: StageTheme): List<Color> = when (theme) {
        StageTheme.GARDEN -> listOf(Color(0xFF070F06), Color(0xFF12230F), Color(0xFF2E4A1C), Color(0x55C5E1A5))
        StageTheme.DESERT -> listOf(Color(0xFF140A03), Color(0xFF3A1F0A), Color(0xFF7A4318), Color(0x55FFE0B2))
        StageTheme.JUNGLE -> listOf(Color(0xFF020A05), Color(0xFF07200F), Color(0xFF145A2E), Color(0x5569F0AE))
        StageTheme.MOUNTAINS -> listOf(Color(0xFF05090D), Color(0xFF15222C), Color(0xFF3C5566), Color(0x55ECEFF1))
        StageTheme.VOLCANO -> listOf(Color(0xFF0A0201), Color(0xFF2A0703), Color(0xFF7A1A05), Color(0x66FF9100))
        StageTheme.ICE -> listOf(Color(0xFF02101A), Color(0xFF0A2A3C), Color(0xFF1F6A85), Color(0x66E0F7FA))
        StageTheme.DARK_TEMPLE -> listOf(Color(0xFF040108), Color(0xFF140722), Color(0xFF3A1263), Color(0x55E040FB))
        StageTheme.FINAL_ARENA -> listOf(Color(0xFF0A0602), Color(0xFF2A1A06), Color(0xFF6B4A0E), Color(0x66FFD700))
    }

    /**
     * Full-screen animated ambient backdrop behind the table: themed radial lighting,
     * slowly drifting motes (embers / snow / fireflies / dust), and a soft vignette.
     * Allocation-free per mote (positions derived from index + time).
     */
    fun drawBackdrop(scope: DrawScope, width: Float, height: Float, theme: StageTheme, time: Float) {
        val c = backdropColors(theme)
        val center = Offset(width / 2f, height * 0.48f)
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(c[2], c[1], c[0]),
                center = center,
                radius = maxOf(width, height) * 0.75f
            ),
            size = Size(width, height)
        )
        // Drifting motes: embers rise (volcano), snow falls (ice), others float.
        val rising = theme == StageTheme.VOLCANO || theme == StageTheme.FINAL_ARENA || theme == StageTheme.DARK_TEMPLE
        val falling = theme == StageTheme.ICE || theme == StageTheme.MOUNTAINS
        for (i in 0 until 26) {
            val seedX = ((i * 73) % 100) / 100f
            val seedY = ((i * 37) % 100) / 100f
            val speed = 0.015f + (i % 5) * 0.006f
            val drift = when {
                rising -> -time * speed
                falling -> time * speed
                else -> sin(time * 0.3f + i) * 0.02f
            }
            val y = ((seedY + drift) % 1f + 1f) % 1f
            val x = seedX + sin(time * 0.5f + i * 1.7f) * 0.015f
            val twinkle = 0.5f + 0.5f * sin(time * 2f + i * 2.3f)
            scope.drawCircle(
                color = c[3].copy(alpha = c[3].alpha * (0.4f + 0.6f * twinkle)),
                radius = 1.5f + (i % 3) * 1.1f,
                center = Offset(x * width, y * height)
            )
        }
        // Vignette
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x99000000)),
                center = center,
                radius = maxOf(width, height) * 0.7f
            ),
            size = Size(width, height)
        )
    }

    /** Environment reaction: a soft glowing rim around the arena that fades out after big moments. */
    fun drawWorldPulse(scope: DrawScope, center: Offset, halfW: Float, halfH: Float, pulse: Float, color: Color) {
        if (pulse <= 0.01f) return
        val a = pulse.coerceIn(0f, 1f)
        for (k in 0 until 3) {
            val grow = 6f + k * 9f + (1f - a) * 14f
            scope.drawRoundRect(
                color = color.copy(alpha = 0.32f * a / (k + 1)),
                topLeft = Offset(center.x - halfW - grow, center.y - halfH - grow),
                size = Size((halfW + grow) * 2f, (halfH + grow) * 2f),
                cornerRadius = CornerRadius(30f + grow, 30f + grow),
                style = Stroke(width = 6f - k * 1.5f)
            )
        }
        // faint wash over the board surface
        scope.drawRoundRect(
            color = color.copy(alpha = 0.07f * a),
            topLeft = Offset(center.x - halfW, center.y - halfH),
            size = Size(halfW * 2f, halfH * 2f),
            cornerRadius = CornerRadius(14f, 14f)
        )
    }

    fun drawTable(
        scope: DrawScope,
        center: Offset,
        halfW: Float,
        halfH: Float,
        theme: StageTheme = StageTheme.GARDEN
    ) {
        val left = center.x - halfW
        val top = center.y - halfH
        val width = halfW * 2f
        val height = halfH * 2f
        val colors = getColorsForTheme(theme)

        // 1. Drop shadow of the whole wooden table on ambient background
        scope.drawRoundRect(
            color = Color(0x70000000),
            topLeft = Offset(left + 8f, top + 12f),
            size = Size(width, height),
            cornerRadius = CornerRadius(28f, 28f)
        )

        // 2. Heavy Outer Themed Rail / Border
        val outerBorderThickness = 22f
        scope.drawRoundRect(
            brush = Brush.linearGradient(
                colors = colors.railColors,
                start = Offset(left, top),
                end = Offset(left + width, top + height)
            ),
            topLeft = Offset(left - outerBorderThickness, top - outerBorderThickness),
            size = Size(width + outerBorderThickness * 2f, height + outerBorderThickness * 2f),
            cornerRadius = CornerRadius(26f, 26f)
        )

        // Rail top highlight bevel
        scope.drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(colors.railHighlight, Color(0x05FFFFFF)),
                start = Offset(left, top),
                end = Offset(left + width, top + height)
            ),
            topLeft = Offset(left - outerBorderThickness, top - outerBorderThickness),
            size = Size(width + outerBorderThickness * 2f, height + outerBorderThickness * 2f),
            cornerRadius = CornerRadius(26f, 26f),
            style = Stroke(width = 2.5f)
        )

        // 3. Themed Board Surface Planks
        val plankCount = 8
        val plankH = height / plankCount
        val plankColors = colors.plankColors

        for (i in 0 until plankCount) {
            val pTop = top + i * plankH
            val pColor = plankColors[i % plankColors.size]

            // Plank base
            scope.drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(pColor, pColor.copy(red = pColor.red * 0.9f, green = pColor.green * 0.9f)),
                    start = Offset(left, pTop),
                    end = Offset(left + width, pTop)
                ),
                topLeft = Offset(left, pTop),
                size = Size(width, plankH)
            )

            // Texture / grain streaks
            val grainY1 = pTop + plankH * 0.3f
            val grainY2 = pTop + plankH * 0.7f
            scope.drawLine(
                color = colors.grainLight,
                start = Offset(left + 20f, grainY1),
                end = Offset(left + width - 20f, grainY1),
                strokeWidth = 1.2f
            )
            scope.drawLine(
                color = colors.grainDark,
                start = Offset(left + 40f, grainY2),
                end = Offset(left + width - 40f, grainY2),
                strokeWidth = 1.4f
            )

            // Plank seam groove
            if (i > 0) {
                scope.drawLine(
                    color = colors.seamColor,
                    start = Offset(left, pTop),
                    end = Offset(left + width, pTop),
                    strokeWidth = 2.5f
                )
                scope.drawLine(
                    color = colors.seamHighlight,
                    start = Offset(left, pTop + 1.5f),
                    end = Offset(left + width, pTop + 1.5f),
                    strokeWidth = 1f
                )
            }
        }

        // 4. Center Board Decorative Inlay
        scope.drawCircle(
            color = colors.inlayOuter,
            radius = 110f,
            center = center,
            style = Stroke(width = 3f)
        )
        scope.drawCircle(
            color = colors.inlayInner,
            radius = 70f,
            center = center,
            style = Stroke(width = 1.5f)
        )
        // Diamond points
        val dSize = 22f
        val diamondPath = Path().apply {
            moveTo(center.x, center.y - dSize)
            lineTo(center.x + dSize, center.y)
            lineTo(center.x, center.y + dSize)
            lineTo(center.x - dSize, center.y)
            close()
        }
        scope.drawPath(diamondPath, color = colors.diamondColor, style = Fill)
        scope.drawPath(diamondPath, color = colors.diamondColor.copy(alpha = 0.5f), style = Stroke(width = 1.5f))

        // 5. Deep Inner Shadow around Table Edge (Cushion / Well effect)
        scope.drawRoundRect(
            color = Color(0x60000000),
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 10f)
        )
        scope.drawRoundRect(
            color = Color(0x28000000),
            topLeft = Offset(left + 6f, top + 6f),
            size = Size(width - 12f, height - 12f),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 8f)
        )

        // 5b. Surface lighting: warm key light from upper-left, gentle falloff to the far edge
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x24FFF3E0), Color(0x00FFF3E0)),
                center = Offset(left + width * 0.3f, top + height * 0.25f),
                radius = maxOf(width, height) * 0.7f
            ),
            topLeft = Offset(left, top),
            size = Size(width, height)
        )
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x00000000), Color(0x2E000000)),
                startY = top + height * 0.55f,
                endY = top + height
            ),
            topLeft = Offset(left, top + height * 0.55f),
            size = Size(width, height * 0.45f)
        )

        // 6. Polished Themed Corner Brackets
        drawThemedCorner(scope, left, top, 0f, colors)
        drawThemedCorner(scope, left + width, top, 90f, colors)
        drawThemedCorner(scope, left + width, top + height, 180f, colors)
        drawThemedCorner(scope, left, top + height, 270f, colors)
    }

    private fun drawThemedCorner(
        scope: DrawScope,
        cornerX: Float,
        cornerY: Float,
        rotationDeg: Float,
        colors: TableThemeColors
    ) {
        scope.rotate(rotationDeg, pivot = Offset(cornerX, cornerY)) {
            val bracketPath = Path().apply {
                moveTo(cornerX - 4f, cornerY - 4f)
                lineTo(cornerX + 32f, cornerY - 4f)
                lineTo(cornerX + 32f, cornerY + 10f)
                lineTo(cornerX + 10f, cornerY + 10f)
                lineTo(cornerX + 10f, cornerY + 32f)
                lineTo(cornerX - 4f, cornerY + 32f)
                close()
            }
            drawPath(
                bracketPath,
                brush = Brush.linearGradient(
                    colors = colors.cornerColors,
                    start = Offset(cornerX - 4f, cornerY - 4f),
                    end = Offset(cornerX + 30f, cornerY + 30f)
                )
            )
            // Rivet screw on corner bracket
            drawCircle(
                color = colors.rivetColor,
                radius = 2.5f,
                center = Offset(cornerX + 6f, cornerY + 6f)
            )
            drawCircle(
                color = colors.rivetHighlight,
                radius = 1f,
                center = Offset(cornerX + 5.5f, cornerY + 5.5f)
            )
        }
    }

    fun drawObstacles(
        scope: DrawScope,
        obstacles: List<Obstacle>,
        boardCenter: Offset
    ) {
        for (obs in obstacles) {
            val cx = boardCenter.x + obs.pos.x
            val cy = boardCenter.y + obs.pos.y
            when (obs.type) {
                ObstacleType.ROCK -> {
                    // Drop shadow
                    scope.drawCircle(
                        color = Color(0x60000000),
                        radius = obs.radius * 1.15f,
                        center = Offset(cx + 4f, cy + 5f)
                    )
                    // 3D Rock texture
                    scope.drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF8D99AE),
                                Color(0xFF5C677D),
                                Color(0xFF333D4F),
                                Color(0xFF1E2530)
                            ),
                            center = Offset(cx - obs.radius * 0.35f, cy - obs.radius * 0.35f),
                            radius = obs.radius * 1.25f
                        ),
                        radius = obs.radius,
                        center = Offset(cx, cy)
                    )
                    // Rocky facet highlight
                    scope.drawCircle(
                        color = Color(0x40FFFFFF),
                        radius = obs.radius * 0.45f,
                        center = Offset(cx - obs.radius * 0.3f, cy - obs.radius * 0.3f)
                    )
                    scope.drawCircle(
                        color = Color(0x35000000),
                        radius = obs.radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5f)
                    )
                }
                ObstacleType.MOVING_BOULDER -> {
                    // Rolling mountain boulder with grooves
                    scope.drawCircle(
                        color = Color(0x70000000),
                        radius = obs.radius * 1.15f,
                        center = Offset(cx + 5f, cy + 6f)
                    )
                    scope.drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFB0BEC5),
                                Color(0xFF78909C),
                                Color(0xFF455A64),
                                Color(0xFF263238)
                            ),
                            center = Offset(cx - obs.radius * 0.35f, cy - obs.radius * 0.35f),
                            radius = obs.radius * 1.2f
                        ),
                        radius = obs.radius,
                        center = Offset(cx, cy)
                    )
                    // Rolling grooves / motion lines
                    scope.drawArc(
                        color = Color(0x50263238),
                        startAngle = 30f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(cx - obs.radius * 0.65f, cy - obs.radius * 0.65f),
                        size = Size(obs.radius * 1.3f, obs.radius * 1.3f),
                        style = Stroke(width = 3.5f)
                    )
                    scope.drawArc(
                        color = Color(0x50FFFFFF),
                        startAngle = 210f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(cx - obs.radius * 0.7f, cy - obs.radius * 0.7f),
                        size = Size(obs.radius * 1.4f, obs.radius * 1.4f),
                        style = Stroke(width = 2.5f)
                    )
                }
                ObstacleType.LAVA_HAZARD -> {
                    // Volcanic Lava Danger Zone
                    // Pulsing heat ring
                    scope.drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xAAFF3D00),
                                Color(0x55FF9100),
                                Color(0x00000000)
                            ),
                            center = Offset(cx, cy),
                            radius = obs.radius * 1.6f
                        ),
                        radius = obs.radius * 1.55f,
                        center = Offset(cx, cy)
                    )
                    // Scorched crater rim
                    scope.drawCircle(
                        color = Color(0xFF260D05),
                        radius = obs.radius * 1.15f,
                        center = Offset(cx, cy)
                    )
                    // Molten magma base
                    scope.drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFEA00), // hot yellow center
                                Color(0xFFFF6D00), // orange
                                Color(0xFFDD2C00), // red
                                Color(0xFFB71C1C)  // deep crimson
                            ),
                            center = Offset(cx, cy),
                            radius = obs.radius
                        ),
                        radius = obs.radius,
                        center = Offset(cx, cy)
                    )
                    // Molten bubbles
                    scope.drawCircle(
                        color = Color(0x99FFF9C4),
                        radius = obs.radius * 0.28f,
                        center = Offset(cx - obs.radius * 0.25f, cy - obs.radius * 0.2f)
                    )
                    scope.drawCircle(
                        color = Color(0x88FFAB00),
                        radius = obs.radius * 0.22f,
                        center = Offset(cx + obs.radius * 0.3f, cy + obs.radius * 0.22f)
                    )
                }
                ObstacleType.TEMPLE_PILLAR -> {
                    // Ancient stone carved column / pillar
                    scope.drawCircle(
                        color = Color(0x75000000),
                        radius = obs.radius * 1.2f,
                        center = Offset(cx + 6f, cy + 8f)
                    )
                    // Column base
                    scope.drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF9E9E9E),
                                Color(0xFF616161),
                                Color(0xFF424242),
                                Color(0xFF212121)
                            ),
                            center = Offset(cx - obs.radius * 0.35f, cy - obs.radius * 0.35f),
                            radius = obs.radius * 1.3f
                        ),
                        radius = obs.radius,
                        center = Offset(cx, cy)
                    )
                    // Chiseled pillar rim
                    scope.drawCircle(
                        color = Color(0x40FFFFFF),
                        radius = obs.radius - 2f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5f)
                    )
                    // Carved glowing rune inlay in pillar top
                    scope.drawCircle(
                        color = Color(0x80D500F9),
                        radius = obs.radius * 0.55f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                    scope.drawCircle(
                        color = Color(0xCCD500F9),
                        radius = obs.radius * 0.22f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }

    fun drawArmadillo(
        scope: DrawScope,
        armadillo: ArmadilloState,
        boardCenter: Offset,
        isBlue: Boolean = false
    ) {
        val cx = boardCenter.x + armadillo.pos.x
        val cy = boardCenter.y + armadillo.pos.y
        val scale = armadillo.eatenScale
        if (scale <= 0.01f) return

        val heading = armadillo.headingAngle
        val headingDeg = (heading * 180f / PI).toFloat()

        // High-speed motion streaks behind the armadillo (cyan in overdrive, soft white otherwise)
        val speedNow = armadillo.vel.length()
        if (speedNow > 260f && !armadillo.isEaten) {
            val k = ((speedNow - 260f) / 320f).coerceIn(0f, 1f)
            val back = heading + PI.toFloat()
            val streakColor = if (armadillo.isPowerActive) Color(0xFF00E5FF) else Color(0xFFFFFFFF)
            for (sIdx in -1..1) {
                val side = heading + PI.toFloat() / 2f
                val ox = cx + cos(side) * sIdx * 9f + cos(back) * 22f
                val oy = cy + sin(side) * sIdx * 9f + sin(back) * 22f
                val len = (26f + 22f * k) * (if (sIdx == 0) 1.2f else 0.8f)
                scope.drawLine(
                    color = streakColor.copy(alpha = 0.35f * k * scale),
                    start = Offset(ox, oy),
                    end = Offset(ox + cos(back) * len, oy + sin(back) * len),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Player identification: soft coloured ground glow so the armadillo reads instantly at speed
        val idGlow = if (isBlue) Color(0xFF40C4FF) else Color(0xFFFFC94D)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(idGlow.copy(alpha = 0.38f * scale), idGlow.copy(alpha = 0f)),
                center = Offset(cx, cy + 4f),
                radius = 44f
            ),
            radius = 44f,
            center = Offset(cx, cy + 4f)
        )

        // Distinct 7araka Electric/Energy Aura around the Armadillo
        if (armadillo.isPowerActive) {
            val auraPulse = (sin(armadillo.auraPhase) * 4f).coerceIn(-4f, 4f)
            val outerRadius = 42f + auraPulse
            val innerRadius = 36f + auraPulse * 0.5f

            // Radiant Cyan/Golden (or Neon Azure/Cyan for blue armadillo) energy ring
            val auraColors = if (isBlue) {
                listOf(
                    Color(0x0000B0FF),
                    Color(0x5000E5FF),
                    Color(0xA040C4FF),
                    Color(0x000091EA)
                )
            } else {
                listOf(
                    Color(0x0000E5FF),
                    Color(0x5000E5FF),
                    Color(0xA0FFD700),
                    Color(0x00FFD700)
                )
            }

            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = auraColors,
                    center = Offset(cx, cy),
                    radius = outerRadius + 8f
                ),
                radius = outerRadius + 8f,
                center = Offset(cx, cy)
            )

            // Dynamic lightning/energy arcs around armadillo
            for (arc in 0 until 4) {
                val arcAngle = armadillo.auraPhase * 1.5f + (arc * (PI.toFloat() / 2f))
                val arcX = cx + cos(arcAngle) * (innerRadius + 2f)
                val arcY = cy + sin(arcAngle) * (innerRadius + 2f)
                val arcColor = if (isBlue) {
                    if (arc % 2 == 0) Color(0xFF00E5FF) else Color(0xFF80D8FF)
                } else {
                    if (arc % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFFEA00)
                }
                scope.drawCircle(
                    color = arcColor,
                    radius = 3.5f,
                    center = Offset(arcX, arcY)
                )
            }

            // Two Extra Chances shield indicators floating above the armadillo
            for (ch in 0 until armadillo.powerChancesRemaining) {
                val shieldAngle = -PI.toFloat() / 2f + (ch - 0.5f) * 0.7f
                val shieldX = cx + cos(shieldAngle) * 48f
                val shieldY = cy + sin(shieldAngle) * 48f
                scope.drawCircle(
                    color = if (isBlue) Color(0xFF00E5FF) else Color(0xFF00E5FF),
                    radius = 4.5f,
                    center = Offset(shieldX, shieldY)
                )
                scope.drawCircle(
                    color = Color.White,
                    radius = 2f,
                    center = Offset(shieldX, shieldY)
                )
            }
        }

        // MAGNET active: orbiting magnetic particles + faint attraction field ring
        if (armadillo.magnetTimer > 0f) {
            scope.drawCircle(
                color = Color(0x33E040FB),
                radius = 46f,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
            for (k in 0 until 6) {
                val a = armadillo.auraPhase * 2f + k * (PI.toFloat() / 3f)
                val rr = 40f + sin(armadillo.auraPhase * 3f + k) * 4f
                scope.drawCircle(
                    color = if (k % 2 == 0) Color(0xFFE040FB) else Color(0xFFEA80FC),
                    radius = 3f,
                    center = Offset(cx + cos(a) * rr, cy + sin(a) * rr)
                )
            }
        }

        // RAINBOW active: shifting multicolor score-multiplier aura
        if (armadillo.rainbowTimer > 0f) {
            val cols = listOf(
                Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00),
                Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFFD500F9)
            )
            val idx = ((armadillo.auraPhase * 2f).toInt() % cols.size + cols.size) % cols.size
            scope.drawCircle(
                color = cols[idx].copy(alpha = 0.5f),
                radius = 44f,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
        }

        // SHIELD active: translucent blue energy bubble with a sweeping highlight
        if (armadillo.shieldCharges > 0 && !armadillo.isEaten) {
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x0040C4FF), Color(0x2240C4FF), Color(0x6640C4FF)),
                    center = Offset(cx, cy),
                    radius = 42f
                ),
                radius = 42f,
                center = Offset(cx, cy)
            )
            scope.drawCircle(color = Color(0xAA80D8FF), radius = 42f, center = Offset(cx, cy), style = Stroke(width = 2f))
            val sweep = armadillo.auraPhase * 40f
            scope.drawArc(
                color = Color(0xCCFFFFFF),
                startAngle = sweep, sweepAngle = 50f, useCenter = false,
                topLeft = Offset(cx - 38f, cy - 38f), size = Size(76f, 76f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }

        // Eat-bounce squash-and-stretch pop (brief scale-up right after a bite)
        val bounceScale = 1f + armadillo.eatBounceTimer.coerceIn(0f, 0.3f) * 0.5f
        val drawScale = scale * bounceScale

        scope.withTransform({
            translate(left = cx, top = cy)
            scale(scaleX = drawScale, scaleY = drawScale, pivot = Offset.Zero)
            rotate(degrees = headingDeg, pivot = Offset.Zero)
        }) {
            // 1. Soft Oval Drop Shadow on the Table
            drawOval(
                color = Color(0x60000000),
                topLeft = Offset(-36f, -14f),
                size = Size(64f, 40f)
            )

            // 2. Invulnerable Shield Aura (Grace period after spawn)
            if (armadillo.invulnerableTimer > 0f) {
                val auraAlpha = (sin(armadillo.invulnerableTimer * 12f) * 0.25f + 0.35f).coerceIn(0.1f, 0.7f)
                val ringColor = if (isBlue) Color(0xFF00E5FF) else Color(0xFFFFD54F)
                drawCircle(
                    color = ringColor.copy(alpha = auraAlpha),
                    radius = 38f,
                    center = Offset.Zero,
                    style = Stroke(width = 3f)
                )
            }

            // 3. Four Paws (Animated scuttling)
            val legOffset = sin(armadillo.legStepPhase) * 6f
            val legColor = if (isBlue) Color(0xFF1565C0) else Color(0xFF7A451F)
            val clawColor = if (isBlue) Color(0xFF80D8FF) else Color(0xFFD4AF37)

            // Front Left Paw
            drawRoundRect(
                color = legColor,
                topLeft = Offset(10f, -22f + legOffset),
                size = Size(11f, 7f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawCircle(clawColor, radius = 1.5f, center = Offset(19f, -18.5f + legOffset))

            // Front Right Paw
            drawRoundRect(
                color = legColor,
                topLeft = Offset(10f, 15f - legOffset),
                size = Size(11f, 7f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawCircle(clawColor, radius = 1.5f, center = Offset(19f, 18.5f - legOffset))

            // Hind Left Paw
            drawRoundRect(
                color = legColor,
                topLeft = Offset(-20f, -23f - legOffset),
                size = Size(12f, 8f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Hind Right Paw
            drawRoundRect(
                color = legColor,
                topLeft = Offset(-20f, 15f + legOffset),
                size = Size(12f, 8f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // 4. Tail with Segmented Rings (swaying behind)
            val tailWag = sin(armadillo.tailWagPhase) * 4f
            val tailPath = Path().apply {
                moveTo(-22f, 0f)
                quadraticTo(-32f, tailWag * 0.5f, -44f, tailWag)
            }
            val tailColor1 = if (isBlue) Color(0xFF1976D2) else Color(0xFF8D5524)
            val tailColor2 = if (isBlue) Color(0xFF0D47A1) else Color(0xFF5E3013)
            scope.drawPath(
                tailPath,
                color = tailColor1,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            scope.drawPath(
                tailPath,
                color = tailColor2,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // 5. Main Rounded Armored Shell (3D Dome)
            // Radial gradient creates spherical volume
            val shellColors = if (isBlue) {
                listOf(
                    Color(0xFF64B5F6),
                    Color(0xFF1E88E5),
                    Color(0xFF1565C0),
                    Color(0xFF0D47A1)
                )
            } else {
                listOf(
                    Color(0xFFE8B278),
                    Color(0xFFC48248),
                    Color(0xFF8E5325),
                    Color(0xFF593012)
                )
            }

            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = shellColors,
                    center = Offset(-4f, -4f),
                    radius = 34f
                ),
                topLeft = Offset(-26f, -18f),
                size = Size(46f, 36f)
            )

            // 6. Individual Transverse Shell Plates / Armor Bands
            // Distinct armored bands with highlights and rivets
            val bandOffsets = listOf(-16f, -8f, 0f, 8f)
            val grooveColor = if (isBlue) Color(0xFF0A2440) else Color(0xFF42210B)
            val plateHighlight = if (isBlue) Color(0xFFBBDEFB) else Color(0xFFFFD9B0)
            val rivetColor = if (isBlue) Color(0xFFE1F5FE) else Color(0xFFFFF2DC)

            for (bx in bandOffsets) {
                // Curved transverse armor band
                val bandPath = Path().apply {
                    moveTo(bx, -17f)
                    cubicTo(bx + 4f, -6f, bx + 4f, 6f, bx, 17f)
                }
                // Dark groove
                scope.drawPath(
                    bandPath,
                    color = grooveColor,
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )
                // Highlight rim
                scope.drawPath(
                    bandPath,
                    color = plateHighlight,
                    style = Stroke(width = 1.4f, cap = StrokeCap.Round)
                )

                // Armored rivets / studs on plates
                drawCircle(
                    color = rivetColor,
                    radius = 1.4f,
                    center = Offset(bx + 1.5f, -8f)
                )
                drawCircle(
                    color = rivetColor,
                    radius = 1.4f,
                    center = Offset(bx + 2f, 0f)
                )
                drawCircle(
                    color = rivetColor,
                    radius = 1.4f,
                    center = Offset(bx + 1.5f, 8f)
                )
            }

            // Glossy shell specular highlight (premium "lacquered armour" read)
            scope.drawArc(
                color = Color(0x55FFFFFF),
                startAngle = 195f, sweepAngle = 70f, useCenter = false,
                topLeft = Offset(-22f, -15f), size = Size(38f, 28f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Shell rim border
            val rimColor = if (isBlue) Color(0xFF0D3768) else Color(0xFF4A250E)
            scope.drawOval(
                color = rimColor,
                topLeft = Offset(-26f, -18f),
                size = Size(46f, 36f),
                style = Stroke(width = 2f)
            )

            // 7. Cute Armadillo Head & Snout (Facing positive X)
            val headCenter = Offset(24f, 0f)

            // Head base
            val headPath = Path().apply {
                moveTo(14f, -9f)
                lineTo(30f, -4f)
                cubicTo(34f, -2f, 34f, 2f, 30f, 4f)
                lineTo(14f, 9f)
                close()
            }
            val headColors = if (isBlue) {
                listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
            } else {
                listOf(Color(0xFFDCA168), Color(0xFFA86A34))
            }
            scope.drawPath(
                headPath,
                brush = Brush.linearGradient(
                    colors = headColors,
                    start = Offset(14f, 0f),
                    end = Offset(34f, 0f)
                )
            )

            // Snout / Nose
            val noseColor = if (isBlue) Color(0xFF0A192F) else Color(0xFF2B1408)
            drawCircle(
                color = noseColor,
                radius = 2.8f,
                center = Offset(32.5f, 0f)
            )

            // Cute Triangular Ears
            val earColor = if (isBlue) Color(0xFF1E88E5) else Color(0xFFC48248)
            val innerEarColor = if (isBlue) Color(0xFF80D8FF) else Color(0xFFE89A8A)

            val leftEarPath = Path().apply {
                moveTo(16f, -7f)
                lineTo(20f, -16f)
                lineTo(22f, -8f)
                close()
            }
            scope.drawPath(leftEarPath, color = earColor)
            scope.drawPath(leftEarPath, color = innerEarColor, style = Stroke(width = 1f))

            val rightEarPath = Path().apply {
                moveTo(16f, 7f)
                lineTo(20f, 16f)
                lineTo(22f, 8f)
                close()
            }
            scope.drawPath(rightEarPath, color = earColor)
            scope.drawPath(rightEarPath, color = innerEarColor, style = Stroke(width = 1f))

            // Big expressive eyes
            // Left Eye
            drawCircle(color = Color(0xFF0A192F), radius = 3.2f, center = Offset(23f, -5f))
            drawCircle(color = Color(0xFFFFFFFF), radius = 1.1f, center = Offset(24f, -5.8f))

            // Right Eye
            drawCircle(color = Color(0xFF0A192F), radius = 3.2f, center = Offset(23f, 5f))
            drawCircle(color = Color(0xFFFFFFFF), radius = 1.1f, center = Offset(24f, 4.2f))
        }

        // Hit reaction: brief bright flash ring when a bite is absorbed by Shield / Overdrive
        if (armadillo.hitTimer > 0f) {
            val h = (armadillo.hitTimer / 0.35f).coerceIn(0f, 1f)
            scope.drawCircle(color = Color.White.copy(alpha = 0.45f * h), radius = 30f + (1f - h) * 18f, center = Offset(cx, cy), style = Stroke(width = 4f))
            scope.drawCircle(color = Color(0xFFFF5252).copy(alpha = 0.25f * h), radius = 26f, center = Offset(cx, cy))
        }
    }

    fun drawSnakes(
        scope: DrawScope,
        snakes: List<SnakeState>,
        boardCenter: Offset,
        frozen: Boolean = false
    ) {
        for (i in snakes.indices) {
            drawSnake(scope, snakes[i], boardCenter, snakeIndex = i, frozen = frozen)
        }
    }

    fun drawSnake(
        scope: DrawScope,
        snake: SnakeState,
        boardCenter: Offset,
        snakeIndex: Int = 0,
        frozen: Boolean = false
    ) {
        val headX = boardCenter.x + snake.headPos.x
        val headY = boardCenter.y + snake.headPos.y

        val bodyColors = when {
            frozen -> listOf(Color(0xFFE1F5FE), Color(0xFF81D4FA), Color(0xFF0288D1), Color(0xFF01579B))
            snakeIndex == 1 -> listOf(Color(0xFFBA68C8), Color(0xFF8E24AA), Color(0xFF4A148C), Color(0xFF1E0738))
            snakeIndex == 2 -> listOf(Color(0xFFFF8A80), Color(0xFFD32F2F), Color(0xFF7F0000), Color(0xFF2B0000))
            else -> listOf(Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF0C3810))
        }
        val bellyHighlight = if (snakeIndex == 1) Color(0x40E1BEE7) else Color(0x35A5D6A7)
        val headColors = if (snakeIndex == 1) {
            listOf(Color(0xFFCE93D8), Color(0xFF9C27B0), Color(0xFF4A148C), Color(0xFF1B0530))
        } else {
            listOf(Color(0xFF66BB6A), Color(0xFF388E3C), Color(0xFF1B5E20), Color(0xFF0E3812))
        }
        val headContour = if (snakeIndex == 1) Color(0xFFE1BEE7).copy(alpha = 0.5f) else Color(0xFF81C784).copy(alpha = 0.4f)
        val eyeColor = if (snakeIndex == 1) Color(0xFFFF007F) else Color(0xFFFFD54F)

        // 1. Soft Drop Shadow for segments & head
        for (i in snake.bodySegments.indices) {
            val seg = snake.bodySegments[i]
            val segScreen = Offset(boardCenter.x + seg.x + 5f, boardCenter.y + seg.y + 6f)
            val radius = 15f - (i * 0.65f).coerceAtMost(9f)
            scope.drawCircle(
                color = Color(0x40000000),
                radius = radius * 1.05f,
                center = segScreen
            )
        }
        scope.drawCircle(
            color = Color(0x40000000),
            radius = 18f,
            center = Offset(headX + 6f, headY + 7f)
        )

        // 2. Snake Body Segments (drawn tail to head for proper z-overlap)
        val segCount = snake.bodySegments.size
        for (i in (segCount - 1) downTo 0) {
            val seg = snake.bodySegments[i]
            val segRadius = 15f - (i * 0.65f).coerceAtMost(9f)

            // Slither wave offset perpendicular to motion
            val wave = sin(snake.slitherPhase + i * 0.45f) * (6f * (1f - i.toFloat() / segCount))
            val angle = if (i == 0) snake.headAngle else {
                val prev = snake.bodySegments[i - 1]
                kotlin.math.atan2(prev.y - seg.y, prev.x - seg.x)
            }
            val perpAngle = angle + (PI / 2).toFloat()
            val offsetX = cos(perpAngle) * wave
            val offsetY = sin(perpAngle) * wave

            val sx = boardCenter.x + seg.x + offsetX
            val sy = boardCenter.y + seg.y + offsetY

            // 3D Spherical Snake Scale Segment
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = bodyColors,
                    center = Offset(sx - 3f, sy - 3f),
                    radius = segRadius * 1.3f
                ),
                radius = segRadius,
                center = Offset(sx, sy)
            )

            // Glossy sheen on each scale segment (reads as a living, wet body)
            scope.drawCircle(
                color = Color(0x33FFFFFF),
                radius = segRadius * 0.35f,
                center = Offset(sx - segRadius * 0.35f, sy - segRadius * 0.35f)
            )

            // Dorsal Diamond Scale Pattern
            if (i % 2 == 0) {
                drawDiamondScale(scope, sx, sy, segRadius * 0.55f, angle)
            }

            // Pale Belly Edge highlight
            scope.drawCircle(
                color = bellyHighlight,
                radius = segRadius,
                center = Offset(sx, sy),
                style = Stroke(width = 1.2f)
            )
        }

        // Danger cue: pulsing red threat glow under the head so the hazard reads instantly
        val threatPulse = 0.5f + 0.5f * sin(snake.slitherPhase * 0.8f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FF1744), Color(0x00FF1744)),
                center = Offset(headX, headY),
                radius = 34f + 5f * threatPulse
            ),
            radius = 34f + 5f * threatPulse,
            center = Offset(headX, headY)
        )

        // Attack feedback: a sharp red flash ring while the snake strikes
        if (snake.isStriking) {
            val k = (1f - snake.strikeTimer / 0.6f).coerceIn(0f, 1f)
            scope.drawCircle(
                color = Color(0xFFFF1744).copy(alpha = 0.55f * k),
                radius = 22f + (1f - k) * 34f,
                center = Offset(headX, headY),
                style = Stroke(width = 4f * k + 1f)
            )
        }
        if (frozen) {
            scope.drawCircle(color = Color(0x55E1F5FE), radius = 20f, center = Offset(headX, headY), style = Stroke(width = 2f))
        }

        // 3. Snake Head
        val headAngleDeg = (snake.headAngle * 180f / PI).toFloat()
        scope.withTransform({
            translate(left = headX, top = headY)
            rotate(degrees = headAngleDeg, pivot = Offset.Zero)
        }) {
            // Flickering Red Forked Tongue
            if (snake.tongueVisible) {
                val tongueLen = 22f
                val tonguePath = Path().apply {
                    moveTo(18f, 0f)
                    lineTo(18f + tongueLen, 0f)
                    // Fork
                    moveTo(18f + tongueLen, 0f)
                    lineTo(18f + tongueLen + 6f, -4f)
                    moveTo(18f + tongueLen, 0f)
                    lineTo(18f + tongueLen + 6f, 4f)
                }
                scope.drawPath(
                    tonguePath,
                    color = Color(0xFFE53935),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                )
            }

            // Head base (Diamond-shaped predatory viper head)
            val headPath = Path().apply {
                moveTo(-12f, -12f)
                lineTo(6f, -14f)
                lineTo(22f, -4f)
                cubicTo(26f, -2f, 26f, 2f, 22f, 4f)
                lineTo(6f, 14f)
                lineTo(-12f, 12f)
                close()
            }
            scope.drawPath(
                headPath,
                brush = Brush.radialGradient(
                    colors = headColors,
                    center = Offset(4f, -2f),
                    radius = 24f
                )
            )

            // Head scale contours
            scope.drawPath(
                headPath,
                color = headContour,
                style = Stroke(width = 1.8f)
            )

            // Crown Dorsal Diamond
            drawDiamondScale(scope, 2f, 0f, 7f, 0f)

            // Nostrils
            val nostrilColor = if (snakeIndex == 1) Color(0xFF1E0738) else Color(0xFF0D3310)
            drawCircle(nostrilColor, radius = 1.2f, center = Offset(19f, -2f))
            drawCircle(nostrilColor, radius = 1.2f, center = Offset(19f, 2f))

            // Eye glow halos (predator stare stays visible on dark themes)
            drawCircle(color = eyeColor.copy(alpha = 0.35f), radius = 6.5f, center = Offset(11f, -9.5f))
            drawCircle(color = eyeColor.copy(alpha = 0.35f), radius = 6.5f, center = Offset(11f, 9.5f))

            // Sinister Predatory Eyes (amber-yellow or electric magenta with black slit)
            // Left Eye
            drawOval(
                color = eyeColor,
                topLeft = Offset(7f, -12f),
                size = Size(8f, 5f)
            )
            drawLine(
                color = Color(0xFF1B0000),
                start = Offset(11f, -12f),
                end = Offset(11f, -7f),
                strokeWidth = 1.8f
            )

            // Right Eye
            drawOval(
                color = eyeColor,
                topLeft = Offset(7f, 7f),
                size = Size(8f, 5f)
            )
            drawLine(
                color = Color(0xFF1B0000),
                start = Offset(11f, 7f),
                end = Offset(11f, 12f),
                strokeWidth = 1.8f
            )

            // Strike fangs flash if striking
            if (snake.isStriking) {
                val fangPath = Path().apply {
                    moveTo(20f, -4f)
                    lineTo(25f, -2f)
                    lineTo(20f, 0f)
                    lineTo(25f, 2f)
                    lineTo(20f, 4f)
                }
                scope.drawPath(
                    fangPath,
                    color = Color(0xFFFFFFFF),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
        }
    }

    private fun drawDiamondScale(scope: DrawScope, cx: Float, cy: Float, size: Float, angleRad: Float) {
        val deg = (angleRad * 180f / PI).toFloat()
        scope.withTransform({
            translate(left = cx, top = cy)
            rotate(degrees = deg, pivot = Offset.Zero)
        }) {
            val dPath = Path().apply {
                moveTo(-size, 0f)
                lineTo(0f, -size * 0.7f)
                lineTo(size, 0f)
                lineTo(0f, size * 0.7f)
                close()
            }
            drawPath(dPath, color = Color(0xFFFFD54F).copy(alpha = 0.85f), style = Fill)
            drawPath(dPath, color = Color(0xFF795548).copy(alpha = 0.5f), style = Stroke(width = 1f))
        }
    }

    private const val SPECIAL_SCALE = 1.3f

    fun drawApples(
        scope: DrawScope,
        apples: List<AppleItem>,
        boardCenter: Offset
    ) {
        for (apple in apples) {
            val ax = boardCenter.x + apple.pos.x
            val ay = boardCenter.y + apple.pos.y
            val bob = if (apple.isSpecialEventFruit) 0f else sin(apple.bobPhase) * 3f
            val dropH = apple.dropHeight
            val curY = ay + bob - dropH // Render fruit above table based on drop height
            val isRare = apple.fruitType.isSpecial()

            // Dynamic drop shadow on the table (fades and shrinks while the fruit is in the air)
            val shadowScale = (1f - (dropH / 180f).coerceIn(0f, 0.7f)) * (if (isRare) 1.4f else 1f)
            val shadowAlpha = (0.5f - (dropH / 240f).coerceIn(0f, 0.4f)).coerceAtLeast(0.1f)
            val shadowW = 26f * shadowScale
            val shadowH = 12f * shadowScale
            scope.drawOval(
                color = Color.Black.copy(alpha = shadowAlpha),
                topLeft = Offset(ax - shadowW / 2f, ay + 8f - shadowH / 2f),
                size = Size(shadowW, shadowH)
            )

            if (isRare) {
                // Power fruit: aura + ground ring + rotating rays, body drawn larger for drama
                drawRareAura(scope, ax, curY, ay, apple)
                scope.withTransform({ scale(SPECIAL_SCALE, SPECIAL_SCALE, Offset(ax, curY)) }) {
                    drawSpecialFruit(scope, apple, ax, curY)
                }
            } else if (apple.fruitType == FruitType.ESCAPE_GOLDEN) {
                drawGoldenEscapeFruit(scope, apple, ax, curY)
            } else {
                drawNormalFruit(scope, apple.variety, ax, curY, apple.bobPhase)
            }
        }
    }

    private fun rareColor(type: FruitType): Color = when (type) {
        FruitType.ESCAPE_STAR_POWER -> Color(0xFF00E5FF)
        FruitType.DIAMOND -> Color(0xFF80DEEA)
        FruitType.MAGNET -> Color(0xFFE040FB)
        FruitType.RAINBOW -> Color(0xFFFFFFFF)
        FruitType.SHIELD -> Color(0xFF40C4FF)
        FruitType.FREEZE -> Color(0xFFB3E5FC)
        FruitType.GOLDEN -> Color(0xFFFFD700)
        else -> Color(0xFFFFD54F)
    }

    /** "That item is rare — I need it": ground ring, soft glow and slowly rotating light rays. */
    private fun drawRareAura(scope: DrawScope, x: Float, y: Float, groundY: Float, apple: AppleItem) {
        val col = rareColor(apple.fruitType)
        val t = apple.sparkleTimer
        val blink = rareBlink(apple)
        val ring = 0.5f + 0.5f * sin(t * 4f)
        scope.drawOval(
            color = col.copy(alpha = 0.35f * blink),
            topLeft = Offset(x - 26f - 6f * ring, groundY + 4f),
            size = Size(52f + 12f * ring, 12f + 3f * ring),
            style = Stroke(width = 2f)
        )
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(col.copy(alpha = 0.35f * blink), Color.Transparent),
                center = Offset(x, y),
                radius = 46f
            ),
            radius = 46f,
            center = Offset(x, y)
        )
        for (k in 0 until 8) {
            val a = t * 0.9f + k * 0.785f
            val r1 = 26f
            val r2 = 40f + 4f * sin(t * 3f + k)
            scope.drawLine(
                color = col.copy(alpha = 0.28f * blink),
                start = Offset(x + cos(a) * r1, y + sin(a) * r1),
                end = Offset(x + cos(a) * r2, y + sin(a) * r2),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }

    private fun drawGoldenEscapeFruit(scope: DrawScope, apple: AppleItem, ax: Float, curY: Float) {
        // Golden Escape Fruit - energetic shimmering yellow/gold
        val pulse = sin(apple.sparkleTimer * 8f) * 1.5f
        val fRadius = 17f + pulse

        // Outer golden glow
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x80FFD700), Color(0x00FFD700)),
                center = Offset(ax, curY),
                radius = fRadius + 8f
            ),
            radius = fRadius + 8f,
            center = Offset(ax, curY)
        )

        // Golden body
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF9C4),
                    Color(0xFFFFD54F),
                    Color(0xFFFFA000),
                    Color(0xFFE65100)
                ),
                center = Offset(ax - 4f, curY - 4f),
                radius = fRadius + 4f
            ),
            radius = fRadius,
            center = Offset(ax, curY)
        )

        // Gloss
        scope.drawCircle(
            color = Color(0xCCFFFFFF),
            radius = 4f,
            center = Offset(ax - 5f, curY - 6f)
        )

        // Golden Stem & leaf
        val stemPath = Path().apply {
            moveTo(ax, curY - 15f)
            lineTo(ax - 2f, curY - 24f)
        }
        scope.drawPath(stemPath, color = Color(0xFFFF8F00), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        val leafPath = Path().apply {
            moveTo(ax - 2f, curY - 19f)
            quadraticTo(ax + 6f, curY - 25f, ax + 11f, curY - 20f)
            quadraticTo(ax + 5f, curY - 16f, ax - 2f, curY - 19f)
        }
        scope.drawPath(leafPath, color = Color(0xFFFFD700), style = Fill)
    }

    private fun drawSpecialFruit(scope: DrawScope, apple: AppleItem, ax: Float, curY: Float) {
        when (apple.fruitType) {
            FruitType.ESCAPE_STAR_POWER -> {
                // SPECIAL TEMPORARY FRUIT / 7ARAKA STAR POWER
                // Vibrant pulsating Cyan / Magenta / Star Fruit with animated energy halo!
                val pulse = sin(apple.sparkleTimer * 10f) * 2.5f
                val fRadius = 19f + pulse

                // Electric halo ring
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x0000E5FF),
                            Color(0x8000E5FF),
                            Color(0xCCFF007F),
                            Color(0x00FF007F)
                        ),
                        center = Offset(ax, curY),
                        radius = fRadius + 14f
                    ),
                    radius = fRadius + 14f,
                    center = Offset(ax, curY)
                )

                // 3D Star Fruit Body
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE0F7FA),
                            Color(0xFF00E5FF),
                            Color(0xFF0091EA),
                            Color(0xFF6200EA)
                        ),
                        center = Offset(ax - 4f, curY - 4f),
                        radius = fRadius + 4f
                    ),
                    radius = fRadius,
                    center = Offset(ax, curY)
                )

                // Star emblem on fruit
                val starPath = Path().apply {
                    val numPoints = 5
                    for (p in 0 until numPoints * 2) {
                        val r = if (p % 2 == 0) 10f else 4.5f
                        val a = (p * PI / numPoints - PI / 2f + apple.sparkleTimer * 3f).toFloat()
                        val sx = ax + cos(a) * r
                        val sy = curY + sin(a) * r
                        if (p == 0) moveTo(sx, sy) else lineTo(sx, sy)
                    }
                    close()
                }
                scope.drawPath(starPath, color = Color(0xFFFFEA00), style = Fill)
                scope.drawPath(starPath, color = Color.White, style = Stroke(width = 1.2f))

                // Specular highlight
                scope.drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(ax - 6f, curY - 7f)
                )
            }
            FruitType.DIAMOND -> {
                val pop = if (apple.maxLife > 0f) (0.45f + 0.55f * apple.spawnPop) else 1f
                val blink = rareBlink(apple)
                val r = (17f + sin(apple.sparkleTimer * 9f) * 2f) * pop

                // Cyan halo
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x9980DEEA), Color(0x0000E5FF)),
                        center = Offset(ax, curY),
                        radius = r + 16f
                    ),
                    radius = r + 16f,
                    center = Offset(ax, curY),
                    alpha = blink
                )
                // Faceted gem body (rhombus)
                val gem = Path().apply {
                    moveTo(ax, curY - r)
                    lineTo(ax + r * 0.8f, curY - r * 0.2f)
                    lineTo(ax, curY + r)
                    lineTo(ax - r * 0.8f, curY - r * 0.2f)
                    close()
                }
                scope.drawPath(
                    gem,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE0F7FA), Color(0xFF00E5FF), Color(0xFF00838F)),
                        start = Offset(ax - r, curY - r),
                        end = Offset(ax + r, curY + r)
                    ),
                    alpha = blink
                )
                // Facet lines + sparkle
                scope.drawLine(Color(0xCCFFFFFF), Offset(ax, curY - r), Offset(ax, curY + r), strokeWidth = 1.2f)
                scope.drawLine(Color(0x88FFFFFF), Offset(ax - r * 0.8f, curY - r * 0.2f), Offset(ax + r * 0.8f, curY - r * 0.2f), strokeWidth = 1.2f)
                scope.drawCircle(Color.White.copy(alpha = 0.9f * blink), radius = 2.6f, center = Offset(ax - r * 0.25f, curY - r * 0.35f))
            }
            FruitType.MAGNET -> {
                val pop = if (apple.maxLife > 0f) (0.45f + 0.55f * apple.spawnPop) else 1f
                val blink = rareBlink(apple)
                val r = 16f * pop
                val stroke = r * 0.46f

                // Purple glow
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99E040FB), Color(0x00D500F9)),
                        center = Offset(ax, curY),
                        radius = r + 16f
                    ),
                    radius = r + 16f,
                    center = Offset(ax, curY),
                    alpha = blink
                )
                // Horseshoe magnet: top U-bend + two prongs with red/blue tips
                val bendR = r * 0.72f
                val topY = curY - r * 0.15f
                val prongLen = r * 0.95f
                scope.drawArc(
                    color = Color(0xFFBDBDBD),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(ax - bendR, topY - bendR),
                    size = Size(bendR * 2f, bendR * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                scope.drawLine(Color(0xFFBDBDBD), Offset(ax - bendR, topY), Offset(ax - bendR, topY + prongLen), strokeWidth = stroke, cap = StrokeCap.Round)
                scope.drawLine(Color(0xFFBDBDBD), Offset(ax + bendR, topY), Offset(ax + bendR, topY + prongLen), strokeWidth = stroke, cap = StrokeCap.Round)
                scope.drawLine(Color(0xFFD32F2F), Offset(ax - bendR, topY + prongLen - stroke * 0.6f), Offset(ax - bendR, topY + prongLen), strokeWidth = stroke, cap = StrokeCap.Round)
                scope.drawLine(Color(0xFF2962FF), Offset(ax + bendR, topY + prongLen - stroke * 0.6f), Offset(ax + bendR, topY + prongLen), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            FruitType.RAINBOW -> {
                val pop = if (apple.maxLife > 0f) (0.45f + 0.55f * apple.spawnPop) else 1f
                val blink = rareBlink(apple)
                val r = (17f + sin(apple.sparkleTimer * 8f) * 2f) * pop

                // White halo
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99FFFFFF), Color(0x00FFFFFF)),
                        center = Offset(ax, curY),
                        radius = r + 18f
                    ),
                    radius = r + 18f,
                    center = Offset(ax, curY),
                    alpha = blink
                )
                // Concentric rainbow rings
                val cols = listOf(
                    Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00),
                    Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFFD500F9)
                )
                for (idx in cols.indices) {
                    scope.drawCircle(
                        color = cols[idx].copy(alpha = 0.9f * blink),
                        radius = r - idx * (r / cols.size),
                        center = Offset(ax, curY),
                        style = Stroke(width = 2.4f)
                    )
                }
                scope.drawCircle(Color.White.copy(alpha = blink), radius = r * 0.28f, center = Offset(ax, curY))
            }
            FruitType.SHIELD -> {
                val pop = if (apple.maxLife > 0f) (0.45f + 0.55f * apple.spawnPop) else 1f
                val blink = rareBlink(apple)
                val r = 16f * pop
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE1F5FE), Color(0xFF40C4FF), Color(0xFF01579B)),
                        center = Offset(ax - r * 0.3f, curY - r * 0.3f),
                        radius = r * 1.5f
                    ),
                    radius = r,
                    center = Offset(ax, curY),
                    alpha = blink
                )
                val sh = Path().apply {
                    moveTo(ax, curY - r * 0.62f)
                    lineTo(ax + r * 0.5f, curY - r * 0.38f)
                    lineTo(ax + r * 0.42f, curY + r * 0.2f)
                    lineTo(ax, curY + r * 0.65f)
                    lineTo(ax - r * 0.42f, curY + r * 0.2f)
                    lineTo(ax - r * 0.5f, curY - r * 0.38f)
                    close()
                }
                scope.drawPath(sh, color = Color.White.copy(alpha = 0.92f * blink))
                scope.drawPath(sh, color = Color(0xFF0277BD), style = Stroke(width = 1.6f))
                scope.drawLine(Color(0xFF40C4FF), Offset(ax, curY - r * 0.45f), Offset(ax, curY + r * 0.45f), strokeWidth = 2f)
                val rot = apple.sparkleTimer * 1.6f
                for (k in 0 until 6) {
                    val a = rot + k * 1.047f
                    scope.drawCircle(
                        color = Color(0xFF80D8FF).copy(alpha = 0.8f * blink),
                        radius = 2f,
                        center = Offset(ax + cos(a) * r * 1.35f, curY + sin(a) * r * 1.35f)
                    )
                }
                scope.drawCircle(Color.White.copy(alpha = 0.8f * blink), radius = r * 0.18f, center = Offset(ax - r * 0.45f, curY - r * 0.5f))
            }
            FruitType.FREEZE -> {
                val pop = if (apple.maxLife > 0f) (0.45f + 0.55f * apple.spawnPop) else 1f
                val blink = rareBlink(apple)
                val r = 16f * pop
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFB3E5FC), Color(0xFF4FC3F7)),
                        center = Offset(ax - r * 0.3f, curY - r * 0.3f),
                        radius = r * 1.5f
                    ),
                    radius = r,
                    center = Offset(ax, curY),
                    alpha = blink
                )
                val rot = apple.sparkleTimer * 0.7f
                val ice = Color(0xFF0277BD).copy(alpha = blink)
                for (k in 0 until 6) {
                    val a = rot + k * 1.047f
                    scope.drawLine(ice, Offset(ax, curY), Offset(ax + cos(a) * r * 0.78f, curY + sin(a) * r * 0.78f), strokeWidth = 2f, cap = StrokeCap.Round)
                    val bx = ax + cos(a) * r * 0.5f
                    val by = curY + sin(a) * r * 0.5f
                    scope.drawLine(ice, Offset(bx, by), Offset(bx + cos(a + 0.7f) * r * 0.22f, by + sin(a + 0.7f) * r * 0.22f), strokeWidth = 1.5f, cap = StrokeCap.Round)
                    scope.drawLine(ice, Offset(bx, by), Offset(bx + cos(a - 0.7f) * r * 0.22f, by + sin(a - 0.7f) * r * 0.22f), strokeWidth = 1.5f, cap = StrokeCap.Round)
                }
                scope.drawCircle(Color.White.copy(alpha = 0.9f * blink), radius = r * 0.16f, center = Offset(ax, curY))
            }
            FruitType.GOLDEN -> {
                val pop = if (apple.maxLife > 0f) (0.45f + 0.55f * apple.spawnPop) else 1f
                val blink = rareBlink(apple)
                val r = 17f * pop + sin(apple.sparkleTimer * 6f)
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFF8D6E00)),
                        center = Offset(ax - r * 0.3f, curY - r * 0.3f),
                        radius = r * 1.4f
                    ),
                    radius = r,
                    center = Offset(ax, curY),
                    alpha = blink
                )
                scope.drawOval(Color(0xFF8D6E00).copy(alpha = blink), topLeft = Offset(ax - 4f, curY - r), size = Size(8f, 3.5f))
                val stem = Path().apply {
                    moveTo(ax, curY - r + 1f)
                    quadraticTo(ax - 3f, curY - r - 7f, ax - 5f, curY - r - 10f)
                }
                scope.drawPath(stem, color = Color(0xFF6D4C41), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
                val leaf = Path().apply {
                    moveTo(ax - 2f, curY - r - 4f)
                    quadraticTo(ax + 6f, curY - r - 10f, ax + 12f, curY - r - 6f)
                    quadraticTo(ax + 6f, curY - r - 1f, ax - 2f, curY - r - 4f)
                }
                scope.drawPath(leaf, color = Color(0xFFFFE082))
                val gl = 0.6f + 0.4f * sin(apple.sparkleTimer * 5f)
                val gx = ax + r * 0.45f
                val gy = curY - r * 0.45f
                scope.drawLine(Color.White.copy(alpha = gl * blink), Offset(gx - 7f, gy), Offset(gx + 7f, gy), strokeWidth = 2f, cap = StrokeCap.Round)
                scope.drawLine(Color.White.copy(alpha = gl * blink), Offset(gx, gy - 7f), Offset(gx, gy + 7f), strokeWidth = 2f, cap = StrokeCap.Round)
                scope.drawCircle(Color.White.copy(alpha = 0.85f * blink), radius = r * 0.2f, center = Offset(ax - r * 0.4f, curY - r * 0.35f))
            }
            else -> Unit
        }
    }

    /**
     * Twelve stylized normal fruits drawn procedurally (no bitmaps). All score the same; they exist
     * for visual richness. Each has its own silhouette, colours, highlight and a subtle idle wobble.
     */
    private fun drawNormalFruit(scope: DrawScope, variety: Int, x: Float, y: Float, phase: Float) {
        val ax = x
        val curY = y
        scope.rotate(sin(phase * 0.8f) * 6f, Offset(x, y)) {
            when (((variety % FRUIT_VARIETY_COUNT) + FRUIT_VARIETY_COUNT) % FRUIT_VARIETY_COUNT) {
                0 -> {
                        // 3D Spherical Red Apple Body
                        scope.drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF8A80),
                                    Color(0xFFE53935),
                                    Color(0xFFC62828),
                                    Color(0xFF8E0000)
                                ),
                                center = Offset(ax - 4f, curY - 4f),
                                radius = 20f
                            ),
                            radius = 16f,
                            center = Offset(ax, curY)
                        )

                        // Apple Top Dimple indent
                        scope.drawOval(
                            color = Color(0xFF6B0E0E),
                            topLeft = Offset(ax - 4f, curY - 16f),
                            size = Size(8f, 3.5f)
                        )

                        // Specular Gloss Highlight
                        scope.drawCircle(
                            color = Color(0x90FFFFFF),
                            radius = 3.5f,
                            center = Offset(ax - 5f, curY - 7f)
                        )

                        // Apple Woody Stem
                        val stemPath = Path().apply {
                            moveTo(ax, curY - 15f)
                            quadraticTo(ax - 3f, curY - 22f, ax - 5f, curY - 26f)
                        }
                        scope.drawPath(
                            stemPath,
                            color = Color(0xFF5D4037),
                            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                        )

                        // Vibrant Crisp Green Leaf
                        val leafPath = Path().apply {
                            moveTo(ax - 2f, curY - 20f)
                            quadraticTo(ax + 6f, curY - 26f, ax + 12f, curY - 22f)
                            quadraticTo(ax + 6f, curY - 17f, ax - 2f, curY - 20f)
                        }
                        scope.drawPath(leafPath, color = Color(0xFF43A047), style = Fill)
                        scope.drawPath(leafPath, color = Color(0xFF81C784), style = Stroke(width = 1f))
                }
                1 -> { // Orange
                    scope.drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFFFCC80), Color(0xFFFF9800), Color(0xFFE65100)), center = Offset(x - 4f, y - 4f), radius = 20f),
                        radius = 15f, center = Offset(x, y)
                    )
                    for (k in 0 until 6) {
                        val a = k * 1.05f + 0.4f
                        scope.drawCircle(Color(0x33BF360C), radius = 1f, center = Offset(x + cos(a) * 8f, y + sin(a) * 8f))
                    }
                    scope.drawCircle(Color(0xFF33691E), radius = 2.2f, center = Offset(x, y - 14f))
                    val leaf = Path().apply {
                        moveTo(x + 1f, y - 15f)
                        quadraticTo(x + 8f, y - 21f, x + 13f, y - 17f)
                        quadraticTo(x + 7f, y - 13f, x + 1f, y - 15f)
                    }
                    scope.drawPath(leaf, color = Color(0xFF558B2F))
                    scope.drawCircle(Color(0x99FFFFFF), radius = 3.2f, center = Offset(x - 5f, y - 6f))
                }
                2 -> { // Banana
                    val b = Path().apply {
                        moveTo(x - 15f, y - 6f)
                        quadraticTo(x, y + 14f, x + 15f, y - 9f)
                        quadraticTo(x + 1f, y + 5f, x - 15f, y - 6f)
                        close()
                    }
                    scope.drawPath(b, brush = Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFEB3B), Color(0xFFF9A825)), start = Offset(x, y - 8f), end = Offset(x, y + 10f)))
                    scope.drawPath(b, color = Color(0x55795548), style = Stroke(width = 1f))
                    scope.drawCircle(Color(0xFF5D4037), radius = 2f, center = Offset(x - 15f, y - 6f))
                    scope.drawCircle(Color(0xFF5D4037), radius = 1.6f, center = Offset(x + 15f, y - 9f))
                    val hl = Path().apply {
                        moveTo(x - 8f, y + 1f)
                        quadraticTo(x, y + 7f, x + 8f, y - 2f)
                    }
                    scope.drawPath(hl, color = Color(0x88FFFFFF), style = Stroke(width = 1.6f, cap = StrokeCap.Round))
                }
                3 -> { // Strawberry
                    val sb = Path().apply {
                        moveTo(x - 13f, y - 6f)
                        cubicTo(x - 14f, y + 6f, x - 4f, y + 15f, x, y + 17f)
                        cubicTo(x + 4f, y + 15f, x + 14f, y + 6f, x + 13f, y - 6f)
                        quadraticTo(x, y - 12f, x - 13f, y - 6f)
                        close()
                    }
                    scope.drawPath(sb, brush = Brush.radialGradient(listOf(Color(0xFFFF8A80), Color(0xFFE53935), Color(0xFFB71C1C)), center = Offset(x - 4f, y - 2f), radius = 22f))
                    for (k in 0 until 8) {
                        val sx = x + ((k * 7) % 5 - 2) * 4.2f
                        val sy = y - 2f + (k / 3) * 5.5f
                        scope.drawOval(Color(0xFFFFF176), topLeft = Offset(sx - 0.8f, sy - 1.3f), size = Size(1.6f, 2.6f))
                    }
                    for (k in 0 until 5) {
                        val a = -1.57f + (k - 2) * 0.55f
                        val lf = Path().apply {
                            moveTo(x, y - 9f)
                            lineTo(x + cos(a - 0.25f) * 9f, y - 11f + sin(a - 0.25f) * 5f)
                            lineTo(x + cos(a) * 13f, y - 9f + sin(a) * 7f)
                            close()
                        }
                        scope.drawPath(lf, color = Color(0xFF43A047))
                    }
                    scope.drawCircle(Color(0x88FFFFFF), radius = 2.6f, center = Offset(x - 5f, y - 1f))
                }
                4 -> { // Watermelon slice (flat side up)
                    scope.drawArc(Color(0xFF2E7D32), 0f, 180f, true, topLeft = Offset(x - 17f, y - 13f), size = Size(34f, 34f))
                    scope.drawArc(Color(0xFFF1F8E9), 0f, 180f, true, topLeft = Offset(x - 15f, y - 11f), size = Size(30f, 30f))
                    scope.drawArc(
                        brush = Brush.radialGradient(listOf(Color(0xFFFF8A80), Color(0xFFE53935)), center = Offset(x, y + 4f), radius = 14f),
                        startAngle = 0f, sweepAngle = 180f, useCenter = true,
                        topLeft = Offset(x - 13f, y - 9f), size = Size(26f, 26f)
                    )
                    for (k in 0 until 5) {
                        val a = 0.4f + k * 0.58f
                        scope.drawOval(Color(0xFF212121), topLeft = Offset(x + cos(a) * 7.5f - 1f, y + 4f + sin(a) * 7.5f - 1.6f), size = Size(2f, 3.2f))
                    }
                    scope.drawLine(Color(0x66FFFFFF), Offset(x - 12f, y + 4.5f), Offset(x + 12f, y + 4.5f), strokeWidth = 1.2f)
                }
                5 -> { // Pear
                    val pearColors = listOf(Color(0xFFF0F4C3), Color(0xFFC0CA33), Color(0xFF827717))
                    scope.drawCircle(brush = Brush.radialGradient(pearColors, center = Offset(x - 4f, y + 1f), radius = 18f), radius = 13f, center = Offset(x, y + 4f))
                    scope.drawCircle(brush = Brush.radialGradient(pearColors, center = Offset(x - 3f, y - 9f), radius = 12f), radius = 8.5f, center = Offset(x, y - 7f))
                    val stem = Path().apply {
                        moveTo(x, y - 15f)
                        quadraticTo(x + 1f, y - 19f, x + 3f, y - 21f)
                    }
                    scope.drawPath(stem, color = Color(0xFF5D4037), style = Stroke(width = 2f, cap = StrokeCap.Round))
                    val leaf = Path().apply {
                        moveTo(x + 2f, y - 18f)
                        quadraticTo(x + 9f, y - 23f, x + 13f, y - 18f)
                        quadraticTo(x + 8f, y - 15f, x + 2f, y - 18f)
                    }
                    scope.drawPath(leaf, color = Color(0xFF689F38))
                    scope.drawCircle(Color(0x88FFFFFF), radius = 2.8f, center = Offset(x - 5f, y))
                }
                6 -> { // Grapes
                    fun grape(gx: Float, gy: Float) {
                        scope.drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0xFFCE93D8), Color(0xFF8E24AA), Color(0xFF4A148C)), center = Offset(gx - 1.5f, gy - 1.5f), radius = 7f),
                            radius = 5.4f, center = Offset(gx, gy)
                        )
                        scope.drawCircle(Color(0x99FFFFFF), radius = 1.3f, center = Offset(gx - 1.8f, gy - 1.8f))
                    }
                    scope.drawLine(Color(0xFF5D4037), Offset(x, y - 11f), Offset(x + 2f, y - 17f), strokeWidth = 2f, cap = StrokeCap.Round)
                    grape(x - 8f, y - 6f); grape(x, y - 7f); grape(x + 8f, y - 6f)
                    grape(x - 4f, y + 1f); grape(x + 4f, y + 1f)
                    grape(x, y + 8f)
                    val leaf = Path().apply {
                        moveTo(x + 2f, y - 15f)
                        quadraticTo(x + 9f, y - 20f, x + 12f, y - 14f)
                        quadraticTo(x + 6f, y - 12f, x + 2f, y - 15f)
                    }
                    scope.drawPath(leaf, color = Color(0xFF7CB342))
                }
                7 -> { // Cherries
                    val s1 = Path().apply {
                        moveTo(x - 6f, y + 3f)
                        quadraticTo(x - 3f, y - 12f, x + 2f, y - 16f)
                    }
                    val s2 = Path().apply {
                        moveTo(x + 7f, y + 5f)
                        quadraticTo(x + 5f, y - 8f, x + 2f, y - 16f)
                    }
                    scope.drawPath(s1, color = Color(0xFF558B2F), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
                    scope.drawPath(s2, color = Color(0xFF558B2F), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
                    val cherry = listOf(Color(0xFFFF8A80), Color(0xFFD32F2F), Color(0xFF7F0000))
                    scope.drawCircle(brush = Brush.radialGradient(cherry, center = Offset(x - 8f, y + 2f), radius = 10f), radius = 7f, center = Offset(x - 6f, y + 4f))
                    scope.drawCircle(brush = Brush.radialGradient(cherry, center = Offset(x + 5f, y + 4f), radius = 10f), radius = 7f, center = Offset(x + 7f, y + 6f))
                    scope.drawCircle(Color(0xAAFFFFFF), radius = 1.8f, center = Offset(x - 8.5f, y + 1.5f))
                    scope.drawCircle(Color(0xAAFFFFFF), radius = 1.8f, center = Offset(x + 4.5f, y + 3.5f))
                    val leaf = Path().apply {
                        moveTo(x + 2f, y - 16f)
                        quadraticTo(x + 10f, y - 20f, x + 14f, y - 14f)
                        quadraticTo(x + 8f, y - 12f, x + 2f, y - 16f)
                    }
                    scope.drawPath(leaf, color = Color(0xFF7CB342))
                }
                8 -> { // Pineapple
                    for (k in 0 until 5) {
                        val dx = (k - 2).toFloat()
                        val tipH = 18f + (2 - kotlin.math.abs(k - 2)) * 3f
                        val cr = Path().apply {
                            moveTo(x + dx * 3f - 2f, y - 7f)
                            lineTo(x + dx * 5f, y - tipH)
                            lineTo(x + dx * 3f + 2f, y - 7f)
                            close()
                        }
                        scope.drawPath(cr, color = if (k % 2 == 0) Color(0xFF2E7D32) else Color(0xFF43A047))
                    }
                    scope.drawOval(
                        brush = Brush.radialGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFE65100)), center = Offset(x - 3f, y + 2f), radius = 18f),
                        topLeft = Offset(x - 11f, y - 8f), size = Size(22f, 26f)
                    )
                    for (row in 0 until 4) {
                        for (col in -1..1) {
                            scope.drawCircle(Color(0x66795548), radius = 1.3f, center = Offset(x + col * 6f + (row % 2) * 3f - 1.5f, y - 3f + row * 5.5f))
                        }
                    }
                    scope.drawCircle(Color(0x77FFFFFF), radius = 2.6f, center = Offset(x - 5f, y - 2f))
                }
                9 -> { // Peach
                    scope.drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFFFE0B2), Color(0xFFFFAB91), Color(0xFFE57373)), center = Offset(x - 4f, y - 4f), radius = 20f),
                        radius = 15f, center = Offset(x, y)
                    )
                    scope.drawArc(Color(0x55BF360C), 250f, 80f, false, topLeft = Offset(x - 2f, y - 15f), size = Size(14f, 30f), style = Stroke(width = 1.4f))
                    val leaf = Path().apply {
                        moveTo(x, y - 14f)
                        quadraticTo(x + 8f, y - 21f, x + 13f, y - 16f)
                        quadraticTo(x + 7f, y - 12f, x, y - 14f)
                    }
                    scope.drawPath(leaf, color = Color(0xFF66BB6A))
                    scope.drawCircle(Color(0x88FFFFFF), radius = 3f, center = Offset(x - 6f, y - 6f))
                }
                10 -> { // Kiwi (cut)
                    scope.drawCircle(Color(0xFF8D6E63), radius = 15f, center = Offset(x, y))
                    scope.drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFDCEDC8), Color(0xFF9CCC65), Color(0xFF558B2F)), center = Offset(x, y), radius = 13f),
                        radius = 12.5f, center = Offset(x, y)
                    )
                    scope.drawCircle(Color(0xFFF1F8E9), radius = 4f, center = Offset(x, y))
                    for (k in 0 until 10) {
                        val a = k * 0.628f
                        scope.drawOval(Color(0xFF212121), topLeft = Offset(x + cos(a) * 7f - 1f, y + sin(a) * 7f - 1f), size = Size(2f, 2f))
                    }
                    scope.drawArc(Color(0x55FFFFFF), 200f, 70f, false, topLeft = Offset(x - 12f, y - 12f), size = Size(24f, 24f), style = Stroke(width = 1.5f))
                }
                else -> { // Blueberries
                    fun berry(bx: Float, by: Float) {
                        scope.drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0xFF9FA8DA), Color(0xFF3949AB), Color(0xFF1A237E)), center = Offset(bx - 2f, by - 2f), radius = 10f),
                            radius = 7.5f, center = Offset(bx, by)
                        )
                        scope.drawCircle(Color(0xFF1A237E), radius = 2.2f, center = Offset(bx + 1f, by - 2f), style = Stroke(width = 1.2f))
                        scope.drawCircle(Color(0x88FFFFFF), radius = 1.5f, center = Offset(bx - 3f, by - 3f))
                    }
                    berry(x - 6f, y + 3f)
                    berry(x + 6f, y + 3f)
                    berry(x, y - 6f)
                }
            }
        }
    }

    /** Alpha factor that makes a rare timed fruit blink faster as its lifetime runs out. */
    private fun rareBlink(apple: AppleItem): Float {
        if (apple.maxLife <= 0f) return 1f
        val remaining = apple.maxLife - apple.lifeTimer
        return if (remaining < 2.5f) (0.45f + 0.55f * ((sin(apple.lifeTimer * 16f) + 1f) / 2f)) else 1f
    }

    /** Draws the floating "+N" / combo labels rising from eaten fruit (board space → screen). */
    fun drawFloatingTexts(
        scope: DrawScope,
        texts: List<FloatingText>,
        boardCenter: Offset,
        measurer: TextMeasurer
    ) {
        for (f in texts) {
            val alpha = (1f - f.life / f.maxLife).coerceIn(0f, 1f)
            val x = boardCenter.x + f.pos.x
            val y = boardCenter.y + f.pos.y
            val layout = measurer.measure(
                text = f.text,
                style = TextStyle(
                    color = f.color.copy(alpha = alpha),
                    fontSize = f.sizeSp.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            // Soft dark outline for legibility over any background
            val shadow = measurer.measure(
                text = f.text,
                style = TextStyle(
                    color = Color.Black.copy(alpha = alpha * 0.55f),
                    fontSize = f.sizeSp.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            scope.drawText(shadow, topLeft = Offset(x - layout.size.width / 2f + 1.5f, y - layout.size.height / 2f + 1.5f))
            scope.drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f))
        }
    }

    /**
     * Combo badge that floats just below the armadillo (instead of occupying the HUD).
     * Escalates in colour and glow with the multiplier; gently pulses.
     */
    fun drawComboBadge(
        scope: DrawScope,
        measurer: TextMeasurer,
        center: Offset,
        multiplier: Int,
        time: Float
    ) {
        if (multiplier < 2) return
        val col = when (multiplier) {
            2 -> Color(0xFFFFF3C4)
            3 -> Color(0xFFFFD54F)
            4 -> Color(0xFFFFAB40)
            else -> Color(0xFFFFD700)
        }
        val pulse = 1f + 0.06f * sin(time * 9f)
        // Sits BELOW the armadillo: "+N" score labels rise upward from eaten fruit, so the
        // badge never collides with them.
        val y = center.y + 56f
        if (multiplier >= 5) {
            scope.drawCircle(
                brush = Brush.radialGradient(listOf(Color(0x66FFD700), Color.Transparent), center = Offset(center.x, y), radius = 46f),
                radius = 46f,
                center = Offset(center.x, y)
            )
        }
        val layout = measurer.measure(
            text = "COMBO x$multiplier",
            style = TextStyle(color = col, fontSize = (11f + multiplier * 1.2f).sp, fontWeight = FontWeight.Black)
        )
        val w = layout.size.width * pulse
        val h = layout.size.height.toFloat()
        scope.drawRoundRect(
            color = Color(0xB3120A04),
            topLeft = Offset(center.x - w / 2f - 8f, y - h / 2f - 3f),
            size = Size(w + 16f, h + 6f),
            cornerRadius = CornerRadius(h, h)
        )
        scope.drawRoundRect(
            color = col.copy(alpha = 0.7f),
            topLeft = Offset(center.x - w / 2f - 8f, y - h / 2f - 3f),
            size = Size(w + 16f, h + 6f),
            cornerRadius = CornerRadius(h, h),
            style = Stroke(width = 1.5f)
        )
        scope.drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, y - h / 2f))
    }

    /** Very subtle red glow at the screen edges when a snake gets close. Never flashes. */
    fun drawDangerEdge(scope: DrawScope, width: Float, height: Float, level: Float) {
        if (level <= 0.02f) return
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Transparent, Color(0xFFFF1744).copy(alpha = 0.32f * level)),
                center = Offset(width / 2f, height / 2f),
                radius = maxOf(width, height) * 0.62f
            ),
            size = Size(width, height)
        )
    }

    /** FREEZE active: frosty rim on the arena that thaws as the timer runs out. */
    fun drawFreezeOverlay(scope: DrawScope, center: Offset, halfW: Float, halfH: Float, freezeTimer: Float, maxDuration: Float) {
        if (freezeTimer <= 0f) return
        val a = (freezeTimer / maxDuration).coerceIn(0f, 1f)
        scope.drawRoundRect(
            color = Color(0xFFB3E5FC).copy(alpha = 0.10f * a),
            topLeft = Offset(center.x - halfW, center.y - halfH),
            size = Size(halfW * 2f, halfH * 2f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        scope.drawRoundRect(
            color = Color(0xFFE1F5FE).copy(alpha = 0.55f * a),
            topLeft = Offset(center.x - halfW + 3f, center.y - halfH + 3f),
            size = Size(halfW * 2f - 6f, halfH * 2f - 6f),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 5f)
        )
        // Expanding ice wave right after pickup
        val elapsed = maxDuration - freezeTimer
        if (elapsed < 0.8f) {
            val k = elapsed / 0.8f
            scope.drawCircle(
                color = Color(0xFFE1F5FE).copy(alpha = 0.5f * (1f - k)),
                radius = maxOf(halfW, halfH) * 1.3f * k,
                center = center,
                style = Stroke(width = 6f)
            )
        }
    }

    /** Brief environment dim when a rare fruit appears, so the new item pops. */
    fun drawRareDim(scope: DrawScope, width: Float, height: Float, alertTimer: Float, maxDuration: Float) {
        if (alertTimer <= 0f) return
        val elapsed = maxDuration - alertTimer
        val inA = (elapsed / 0.25f).coerceIn(0f, 1f)
        val outA = (alertTimer / 0.8f).coerceIn(0f, 1f)
        scope.drawRect(color = Color.Black.copy(alpha = 0.22f * inA * outA), size = Size(width, height))
    }

    fun drawParticles(
        scope: DrawScope,
        particles: List<Particle>,
        boardCenter: Offset
    ) {
        for (p in particles) {
            val px = boardCenter.x + p.pos.x
            val py = boardCenter.y + p.pos.y
            if (p.size > 6f) {
                // soft glow halo on the bigger sparks only (keeps draw calls bounded)
                scope.drawCircle(
                    color = p.color.copy(alpha = p.alpha * 0.28f),
                    radius = p.size * p.alpha * 1.9f,
                    center = Offset(px, py)
                )
            }
            scope.drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size * p.alpha,
                center = Offset(px, py)
            )
        }
    }
}
