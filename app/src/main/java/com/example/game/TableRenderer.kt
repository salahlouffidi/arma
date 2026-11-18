package com.example.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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

    // =====================================================================
    //  SHARED LIGHTING HELPERS
    //  One key light from the top-left of the screen drives every highlight
    //  and shadow so the whole scene reads as a single lit tabletop.
    // =====================================================================

    private const val LIGHT_X = -0.55f
    private const val LIGHT_Y = -0.83f
    private const val SHADOW_DX = 5f
    private const val SHADOW_DY = 9f

    private fun nowSeconds(): Float = ((System.nanoTime() / 1_000_000L) % 1_000_000L) / 1000f

    /** Light direction expressed in an object's local (rotated) frame. */
    private fun localLight(headingRad: Float): Offset {
        val c = cos(headingRad)
        val s = sin(headingRad)
        return Offset(c * LIGHT_X + s * LIGHT_Y, -s * LIGHT_X + c * LIGHT_Y)
    }

    private fun Color.darken(f: Float) = Color(red * (1f - f), green * (1f - f), blue * (1f - f), alpha)
    private fun Color.lighten(f: Float) = Color(red + (1f - red) * f, green + (1f - green) * f, blue + (1f - blue) * f, alpha)

    /** Soft, feathered elliptical shadow (radial falloff squashed into an oval). */
    private fun DrawScope.softOval(center: Offset, rx: Float, ry: Float, alpha: Float) {
        if (rx <= 0.5f || ry <= 0.5f) return
        withTransform({ scale(1f, ry / rx, pivot = center) }) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.Black.copy(alpha = alpha),
                    0.55f to Color.Black.copy(alpha = alpha * 0.65f),
                    1f to Color.Transparent,
                    center = center,
                    radius = rx
                ),
                radius = rx,
                center = center
            )
        }
    }

    private fun DrawScope.glow(center: Offset, radius: Float, color: Color) {
        if (radius <= 0.5f) return
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                0.45f to color.copy(alpha = color.alpha * 0.45f),
                1f to Color.Transparent,
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }

    private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float, points: Int, rot: Float): Path =
        Path().apply {
            for (p in 0 until points * 2) {
                val r = if (p % 2 == 0) outer else inner
                val a = (p * PI / points - PI / 2.0).toFloat() + rot
                val x = cx + cos(a) * r
                val y = cy + sin(a) * r
                if (p == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

    private fun DrawScope.sparkle(center: Offset, size: Float, color: Color) {
        val p = Path().apply {
            moveTo(center.x, center.y - size)
            quadraticTo(center.x, center.y, center.x + size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + size)
            quadraticTo(center.x, center.y, center.x - size, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - size)
            close()
        }
        glow(center, size * 1.6f, color.copy(alpha = color.alpha * 0.35f))
        drawPath(p, color)
    }

    // =====================================================================
    //  TABLE / BOARD
    // =====================================================================

    private data class Backdrop(val inner: Color, val outer: Color, val lamp: Color)

    private fun backdropFor(theme: StageTheme): Backdrop = when (theme) {
        StageTheme.GARDEN -> Backdrop(Color(0xFF2A1A0E), Color(0xFF080503), Color(0x26FFE6B0))
        StageTheme.DESERT -> Backdrop(Color(0xFF3A2410), Color(0xFF0E0603), Color(0x2AFFE0A0))
        StageTheme.JUNGLE -> Backdrop(Color(0xFF12241A), Color(0xFF030805), Color(0x22D8FFC8))
        StageTheme.MOUNTAINS -> Backdrop(Color(0xFF1E2A33), Color(0xFF05080B), Color(0x26E6F2FF))
        StageTheme.VOLCANO -> Backdrop(Color(0xFF3A0E05), Color(0xFF070201), Color(0x22FFB080))
        StageTheme.ICE -> Backdrop(Color(0xFF0D3440), Color(0xFF02090D), Color(0x30FFFFFF))
        StageTheme.DARK_TEMPLE -> Backdrop(Color(0xFF221330), Color(0xFF050208), Color(0x20E6C8FF))
        StageTheme.FINAL_ARENA -> Backdrop(Color(0xFF3A2208), Color(0xFF080401), Color(0x2EFFE7A0))
    }

    private class Board(
        val boards: List<Pair<androidx.compose.ui.geometry.Rect, Int>>,
        val grain: List<Path>,
        val knots: List<Triple<Offset, Float, Float>>,
        val cracks: List<Path>,
        val specks: List<Triple<Offset, Float, Float>>
    )

    private val boardCache = object : LinkedHashMap<List<Any>, Board>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<List<Any>, Board>?) = size > 20
    }

    private fun boardLayout(left: Float, top: Float, width: Float, height: Float, theme: StageTheme): Board {
        val key = listOf(left, top, width, height, theme)
        boardCache[key]?.let { return it }

        val rnd = java.util.Random(theme.ordinal * 7919L + 17L)
        val rows = (height / 105f).toInt().coerceIn(6, 14)
        val rowH = height / rows
        val boards = ArrayList<Pair<androidx.compose.ui.geometry.Rect, Int>>()
        val grain = ArrayList<Path>()
        val knots = ArrayList<Triple<Offset, Float, Float>>()

        for (r in 0 until rows) {
            val y0 = top + r * rowH
            // Staggered butt joints so the floor reads as real boards, not stripes
            var x = left - rnd.nextFloat() * width * 0.45f
            while (x < left + width) {
                val len = width * (0.45f + rnd.nextFloat() * 0.5f)
                val x0 = max(x, left)
                val x1 = min(x + len, left + width)
                if (x1 - x0 > 4f) {
                    boards += androidx.compose.ui.geometry.Rect(x0, y0, x1, y0 + rowH) to rnd.nextInt(8)
                }
                x += len
            }
            // Flowing grain lines
            val lines = 3 + rnd.nextInt(3)
            for (g in 0 until lines) {
                val baseY = y0 + rowH * (0.15f + 0.7f * rnd.nextFloat())
                val amp = rowH * (0.03f + rnd.nextFloat() * 0.07f)
                val freq = 0.004f + rnd.nextFloat() * 0.01f
                val phase = rnd.nextFloat() * 6.28f
                val p = Path()
                var gx = left
                p.moveTo(gx, baseY + sin(phase) * amp)
                while (gx < left + width) {
                    gx += 18f
                    p.lineTo(gx, baseY + sin(gx * freq + phase) * amp)
                }
                grain += p
            }
            if (rnd.nextFloat() < 0.55f) {
                knots += Triple(
                    Offset(left + width * (0.08f + 0.84f * rnd.nextFloat()), y0 + rowH * (0.3f + 0.4f * rnd.nextFloat())),
                    rowH * (0.06f + 0.06f * rnd.nextFloat()),
                    0.5f + rnd.nextFloat() * 0.5f
                )
            }
        }

        // Theme cracks (lava seams / ice fractures / stone cracks)
        val cracks = ArrayList<Path>()
        val crackCount = when (theme) {
            StageTheme.VOLCANO -> 7
            StageTheme.ICE -> 6
            StageTheme.MOUNTAINS, StageTheme.DARK_TEMPLE -> 4
            else -> 0
        }
        repeat(crackCount) {
            val p = Path()
            var cx = left + width * rnd.nextFloat()
            var cy = top + height * rnd.nextFloat()
            var ang = rnd.nextFloat() * 6.28f
            p.moveTo(cx, cy)
            val steps = 5 + rnd.nextInt(5)
            repeat(steps) { s ->
                ang += (rnd.nextFloat() - 0.5f) * 1.3f
                val step = 18f + rnd.nextFloat() * 30f
                cx = (cx + cos(ang) * step).coerceIn(left, left + width)
                cy = (cy + sin(ang) * step).coerceIn(top, top + height)
                p.lineTo(cx, cy)
                if (s == steps / 2) {
                    // small branch
                    val bx = cx + cos(ang + 1.1f) * step * 0.8f
                    val by = cy + sin(ang + 1.1f) * step * 0.8f
                    p.lineTo(bx.coerceIn(left, left + width), by.coerceIn(top, top + height))
                    p.moveTo(cx, cy)
                }
            }
            cracks += p
        }

        val specks = ArrayList<Triple<Offset, Float, Float>>()
        repeat(60) {
            specks += Triple(
                Offset(left + width * rnd.nextFloat(), top + height * rnd.nextFloat()),
                1f + rnd.nextFloat() * 2.4f,
                rnd.nextFloat()
            )
        }

        val b = Board(boards, grain, knots, cracks, specks)
        boardCache[key] = b
        return b
    }

    fun drawTable(
        scope: DrawScope,
        center: Offset,
        halfW: Float,
        halfH: Float,
        theme: StageTheme = StageTheme.GARDEN
    ) = with(scope) {
        val left = center.x - halfW
        val top = center.y - halfH
        val width = halfW * 2f
        val height = halfH * 2f
        val colors = getColorsForTheme(theme)
        val backdrop = backdropFor(theme)
        val t = nowSeconds()

        // 0. Ambient room: theme-tinted light pool falling off into darkness
        drawRect(
            brush = Brush.radialGradient(
                0f to backdrop.inner,
                1f to backdrop.outer,
                center = Offset(center.x, center.y - halfH * 0.2f),
                radius = max(size.width, size.height) * 0.85f
            ),
            size = size
        )

        val rail = 26f
        val outerRect = androidx.compose.ui.geometry.Rect(left - rail, top - rail, left + width + rail, top + height + rail)
        val outerR = CornerRadius(30f, 30f)

        // 1. Layered soft drop shadow under the whole table
        for (i in 4 downTo 1) {
            val grow = i * 7f
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.10f),
                topLeft = Offset(outerRect.left - grow + 4f, outerRect.top - grow * 0.4f + 16f),
                size = Size(outerRect.width + grow * 2f, outerRect.height + grow * 2f),
                cornerRadius = CornerRadius(30f + grow, 30f + grow)
            )
        }

        // 2. Rail: lit from above, darker at the bottom, with bevel edges
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors.railColors[0].lighten(0.12f),
                    colors.railColors[0],
                    colors.railColors[1],
                    colors.railColors[3].darken(0.2f)
                ),
                startY = outerRect.top,
                endY = outerRect.bottom
            ),
            topLeft = outerRect.topLeft,
            size = outerRect.size,
            cornerRadius = outerR
        )
        // grain sheen running along the rail
        drawRoundRect(
            brush = Brush.linearGradient(
                0f to Color.Transparent,
                0.35f to colors.railHighlight,
                0.5f to Color.Transparent,
                0.7f to colors.railHighlight.copy(alpha = colors.railHighlight.alpha * 0.5f),
                1f to Color.Transparent,
                start = outerRect.topLeft,
                end = outerRect.bottomRight
            ),
            topLeft = outerRect.topLeft,
            size = outerRect.size,
            cornerRadius = outerR
        )
        // outer bevel: bright top edge, dark bottom edge
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.32f),
                0.5f to Color.White.copy(alpha = 0.04f),
                1f to Color.Black.copy(alpha = 0.45f),
                startY = outerRect.top,
                endY = outerRect.bottom
            ),
            topLeft = outerRect.topLeft + Offset(1.5f, 1.5f),
            size = Size(outerRect.width - 3f, outerRect.height - 3f),
            cornerRadius = CornerRadius(29f, 29f),
            style = Stroke(width = 3f)
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = outerRect.topLeft,
            size = outerRect.size,
            cornerRadius = outerR,
            style = Stroke(width = 1.5f)
        )
        // inner lip (light catches the inner edge at the bottom, shadowed at top)
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.6f),
                1f to Color.White.copy(alpha = 0.22f),
                startY = top - 6f,
                endY = top + height + 6f
            ),
            topLeft = Offset(left - 5f, top - 5f),
            size = Size(width + 10f, height + 10f),
            cornerRadius = CornerRadius(17f, 17f),
            style = Stroke(width = 4f)
        )

        // 3. Playing surface (clipped)
        val surface = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(left, top, left + width, top + height, CornerRadius(14f, 14f)))
        }
        val layout = boardLayout(left, top, width, height, theme)
        clipPath(surface) {
            // boards
            val avg = colors.plankColors.let { pc ->
                Color(pc.map { it.red }.average().toFloat(), pc.map { it.green }.average().toFloat(), pc.map { it.blue }.average().toFloat())
            }
            for ((rect, ci) in layout.boards) {
                val base = lerp(avg, colors.plankColors[ci % colors.plankColors.size], 0.45f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(base.lighten(0.06f), base, base.darken(0.08f)),
                        startY = rect.top,
                        endY = rect.bottom
                    ),
                    topLeft = rect.topLeft,
                    size = rect.size
                )
            }
            // grain
            for ((i, g) in layout.grain.withIndex()) {
                drawPath(
                    g,
                    color = if (i % 3 == 0) colors.grainLight else colors.grainDark.copy(alpha = min(1f, colors.grainDark.alpha * 1.8f)),
                    style = Stroke(width = if (i % 2 == 0) 1.1f else 1.8f)
                )
            }
            // knots
            for ((pos, r, squash) in layout.knots) {
                withTransform({ scale(2.2f, squash, pivot = pos) }) {
                    drawCircle(colors.grainDark.copy(alpha = 0.28f), radius = r * 1.6f, center = pos, style = Stroke(1f))
                    drawCircle(colors.grainDark.copy(alpha = 0.22f), radius = r * 1.1f, center = pos, style = Stroke(1f))
                    drawCircle(colors.seamColor.copy(alpha = 0.45f), radius = r * 0.6f, center = pos)
                }
            }
            // seams: dark groove + light lower lip
            val seam = colors.seamColor.copy(alpha = min(colors.seamColor.alpha, 0.42f))
            val seamHi = colors.seamHighlight.copy(alpha = min(colors.seamHighlight.alpha, 0.16f))
            for ((rect, _) in layout.boards) {
                drawLine(seam, Offset(rect.left, rect.top), Offset(rect.right, rect.top), strokeWidth = 2.2f)
                drawLine(seamHi, Offset(rect.left, rect.top + 1.8f), Offset(rect.right, rect.top + 1.8f), strokeWidth = 1f)
                drawLine(seam, Offset(rect.left, rect.top), Offset(rect.left, rect.bottom), strokeWidth = 2f)
                drawLine(seamHi, Offset(rect.left + 1.6f, rect.top), Offset(rect.left + 1.6f, rect.bottom), strokeWidth = 0.8f)
            }

            drawThemeSurfaceFx(this, theme, layout, center, left, top, width, height, t)

            // centre medallion
            drawMedallion(this, center, min(halfW, halfH) * 0.30f, colors, theme, t)

            // overhead lamp highlight + edge vignette
            drawRect(
                brush = Brush.radialGradient(
                    0f to backdrop.lamp,
                    0.6f to backdrop.lamp.copy(alpha = backdrop.lamp.alpha * 0.3f),
                    1f to Color.Transparent,
                    center = Offset(center.x - halfW * 0.18f, center.y - halfH * 0.35f),
                    radius = max(halfW, halfH) * 1.15f
                ),
                topLeft = Offset(left, top),
                size = Size(width, height)
            )
            drawRect(
                brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.42f),
                    center = center,
                    radius = Offset(halfW, halfH).getDistance()
                ),
                topLeft = Offset(left, top),
                size = Size(width, height)
            )
            // rail casts a shadow onto the surface (strongest at the top)
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), startY = top, endY = top + 26f),
                topLeft = Offset(left, top), size = Size(width, 26f)
            )
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent), startX = left, endX = left + 18f),
                topLeft = Offset(left, top), size = Size(18f, height)
            )
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)), startX = left + width - 14f, endX = left + width),
                topLeft = Offset(left + width - 14f, top), size = Size(14f, height)
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)), startY = top + height - 10f, endY = top + height),
                topLeft = Offset(left, top + height - 10f), size = Size(width, 10f)
            )
        }

        // 4. Theme decorations that sit on / overhang the rail
        drawRailDecor(this, theme, left, top, width, height, rail)

        // 5. Metal corner brackets
        drawThemedCorner(this, left - rail, top - rail, 0f, colors)
        drawThemedCorner(this, left + width + rail, top - rail, 90f, colors)
        drawThemedCorner(this, left + width + rail, top + height + rail, 180f, colors)
        drawThemedCorner(this, left - rail, top + height + rail, 270f, colors)
    }

    private fun drawMedallion(
        scope: DrawScope,
        c: Offset,
        r: Float,
        colors: TableThemeColors,
        theme: StageTheme,
        t: Float
    ) = with(scope) {
        val accent = colors.cornerColors[1]
        // inset well
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.Black.copy(alpha = 0.0f),
                0.85f to Color.Black.copy(alpha = 0.04f),
                1f to Color.Black.copy(alpha = 0.10f),
                center = c, radius = r
            ),
            radius = r, center = c
        )
        drawCircle(accent.copy(alpha = 0.30f), radius = r, center = c, style = Stroke(3f))
        drawCircle(Color.White.copy(alpha = 0.08f), radius = r + 2.5f, center = c, style = Stroke(1f))
        drawCircle(
            accent.copy(alpha = 0.22f), radius = r * 0.78f, center = c,
            style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        )
        drawCircle(accent.copy(alpha = 0.18f), radius = r * 0.55f, center = c, style = Stroke(1.5f))
        // compass rose
        val rot = if (theme == StageTheme.DARK_TEMPLE || theme == StageTheme.FINAL_ARENA) t * 0.15f else 0f
        val rose = starPath(c.x, c.y, r * 0.5f, r * 0.14f, 4, rot)
        drawPath(rose, colors.diamondColor)
        drawPath(rose, accent.copy(alpha = 0.35f), style = Stroke(1.2f))
        val rose2 = starPath(c.x, c.y, r * 0.3f, r * 0.1f, 4, rot + (PI / 4).toFloat())
        drawPath(rose2, colors.diamondColor.copy(alpha = colors.diamondColor.alpha * 0.7f))
        drawCircle(accent.copy(alpha = 0.45f), radius = 3f, center = c)
    }

    private fun drawThemeSurfaceFx(
        scope: DrawScope,
        theme: StageTheme,
        layout: Board,
        center: Offset,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        t: Float
    ) = with(scope) {
        when (theme) {
            StageTheme.VOLCANO -> {
                val pulse = 0.75f + 0.25f * sin(t * 2.2f)
                for (c in layout.cracks) {
                    drawPath(c, Color(0xFFFF3D00).copy(alpha = 0.22f * pulse), style = Stroke(12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(c, Color(0xFFFF6D00).copy(alpha = 0.55f * pulse), style = Stroke(4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(c, Color(0xFFFFE082).copy(alpha = 0.9f), style = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                for ((p, s, ph) in layout.specks.take(24)) {
                    val a = (0.5f + 0.5f * sin(t * 3f + ph * 6.28f)) * 0.7f
                    drawCircle(Color(0xFFFF9100).copy(alpha = a), radius = s, center = p)
                }
            }
            StageTheme.ICE -> {
                // frosted sheen and fractures
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.0f),
                        0.45f to Color.White.copy(alpha = 0.18f),
                        0.5f to Color.White.copy(alpha = 0.0f),
                        0.62f to Color.White.copy(alpha = 0.10f),
                        0.68f to Color.White.copy(alpha = 0.0f),
                        start = Offset(left, top), end = Offset(left + width, top + height)
                    ),
                    topLeft = Offset(left, top), size = Size(width, height)
                )
                for (c in layout.cracks) {
                    drawPath(c, Color(0xFF006064).copy(alpha = 0.25f), style = Stroke(2.4f))
                    drawPath(c, Color.White.copy(alpha = 0.7f), style = Stroke(0.9f))
                }
                for ((p, s, ph) in layout.specks.take(18)) {
                    val a = (0.5f + 0.5f * sin(t * 2.5f + ph * 6.28f)).let { it * it }
                    if (a > 0.15f) sparkle(p, s * 2.2f, Color.White.copy(alpha = a))
                }
            }
            StageTheme.MOUNTAINS -> {
                for (c in layout.cracks) {
                    drawPath(c, Color.Black.copy(alpha = 0.35f), style = Stroke(2f))
                    drawPath(c, Color.White.copy(alpha = 0.10f), style = Stroke(1f))
                }
                for ((p, s, _) in layout.specks) drawCircle(Color.White.copy(alpha = 0.35f), radius = s * 0.8f, center = p)
            }
            StageTheme.DARK_TEMPLE -> {
                for (c in layout.cracks) drawPath(c, Color.Black.copy(alpha = 0.45f), style = Stroke(2f))
                // rune ring around the medallion
                val r = min(width, height) * 0.5f * 0.42f
                val glowA = 0.35f + 0.25f * sin(t * 1.6f)
                drawCircle(Color(0xFFD500F9).copy(alpha = glowA * 0.4f), radius = r, center = center, style = Stroke(8f))
                drawCircle(Color(0xFFEA80FC).copy(alpha = glowA), radius = r, center = center, style = Stroke(1.5f))
                for (i in 0 until 12) {
                    val a = i * (PI.toFloat() / 6f) + t * 0.1f
                    val p = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
                    rotate(a * 57.3f, pivot = p) {
                        drawRect(Color(0xFFEA80FC).copy(alpha = glowA), topLeft = p - Offset(1.5f, 7f), size = Size(3f, 14f))
                        drawRect(Color(0xFFEA80FC).copy(alpha = glowA), topLeft = p - Offset(6f, 1.5f), size = Size(6f, 3f))
                    }
                }
            }
            StageTheme.DESERT -> {
                // wind-blown sand drifts
                val rnd = java.util.Random(99)
                repeat(7) {
                    val p = Offset(left + width * rnd.nextFloat(), top + height * rnd.nextFloat())
                    val rx = 60f + rnd.nextFloat() * 120f
                    withTransform({ scale(1f, 0.35f, pivot = p) }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to Color(0x55F6D7A7), 1f to Color.Transparent, center = p, radius = rx
                            ),
                            radius = rx, center = p
                        )
                    }
                }
                for ((p, s, _) in layout.specks) drawCircle(Color(0x30FFF3D6), radius = s * 0.7f, center = p)
            }
            StageTheme.FINAL_ARENA -> {
                val inset = 22f
                drawRoundRect(
                    color = Color(0xFFFFD700).copy(alpha = 0.35f),
                    topLeft = Offset(left + inset, top + inset),
                    size = Size(width - inset * 2f, height - inset * 2f),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(2f)
                )
                drawRoundRect(
                    color = Color(0xFFFF1744).copy(alpha = 0.25f),
                    topLeft = Offset(left + inset + 6f, top + inset + 6f),
                    size = Size(width - inset * 2f - 12f, height - inset * 2f - 12f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                )
            }
            else -> Unit
        }
    }

    private fun drawRailDecor(
        scope: DrawScope,
        theme: StageTheme,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        rail: Float
    ) = with(scope) {
        when (theme) {
            StageTheme.JUNGLE, StageTheme.GARDEN -> {
                val leafA = if (theme == StageTheme.JUNGLE) Color(0xFF2E7D32) else Color(0xFF558B2F)
                val leafB = if (theme == StageTheme.JUNGLE) Color(0xFF1B5E20) else Color(0xFF33691E)
                val corners = listOf(
                    Offset(left - rail * 0.4f, top - rail * 0.4f) to 35f,
                    Offset(left + width + rail * 0.4f, top + height + rail * 0.4f) to 215f
                )
                for ((p, base) in corners) {
                    for (i in 0 until 6) {
                        val ang = base + (i - 2.5f) * 20f
                        drawLeaf(this, p, ang, 52f + (i % 2) * 16f, leafA, leafB)
                    }
                }
                if (theme == StageTheme.GARDEN) {
                    // tiny flowers on the leaf clusters
                    val fl = listOf(Offset(left + 14f, top - 6f), Offset(left - 6f, top + 16f), Offset(left + width - 12f, top + height + 4f))
                    for ((i, f) in fl.withIndex()) {
                        val col = if (i % 2 == 0) Color(0xFFFFF59D) else Color(0xFFF48FB1)
                        for (k in 0 until 5) {
                            val a = k * 1.2566f
                            drawCircle(col, radius = 3.2f, center = f + Offset(cos(a) * 3.5f, sin(a) * 3.5f))
                        }
                        drawCircle(Color(0xFFFFA000), radius = 2f, center = f)
                    }
                }
            }
            StageTheme.MOUNTAINS, StageTheme.ICE -> {
                // snow piled along the top rail
                val snow = Path().apply {
                    moveTo(left - rail + 18f, top - rail + 2f)
                    var x = left - rail + 18f
                    var i = 0
                    while (x < left + width + rail - 18f) {
                        val nx = min(x + 28f, left + width + rail - 18f)
                        quadraticTo((x + nx) / 2f, top - rail - 5f - (i % 3) * 2.5f, nx, top - rail + 2f)
                        x = nx; i++
                    }
                    lineTo(left + width + rail - 18f, top - rail + 9f)
                    lineTo(left - rail + 18f, top - rail + 9f)
                    close()
                }
                drawPath(snow, Brush.verticalGradient(listOf(Color.White, Color(0xFFD6E9F5)), startY = top - rail - 8f, endY = top - rail + 9f))
            }
            else -> Unit
        }
    }

    private fun drawLeaf(scope: DrawScope, base: Offset, angleDeg: Float, len: Float, a: Color, b: Color) = with(scope) {
        rotate(angleDeg, pivot = base) {
            val w = len * 0.32f
            val p = Path().apply {
                moveTo(base.x, base.y)
                cubicTo(base.x + len * 0.3f, base.y - w, base.x + len * 0.8f, base.y - w * 0.8f, base.x + len, base.y)
                cubicTo(base.x + len * 0.8f, base.y + w * 0.8f, base.x + len * 0.3f, base.y + w, base.x, base.y)
                close()
            }
            // leaf shadow onto rail
            translate(4f, 6f) { drawPath(p, Color.Black.copy(alpha = 0.25f)) }
            drawPath(p, Brush.linearGradient(listOf(a.lighten(0.15f), b), start = Offset(base.x, base.y - w), end = Offset(base.x, base.y + w)))
            drawLine(a.lighten(0.45f).copy(alpha = 0.7f), base, Offset(base.x + len * 0.92f, base.y), strokeWidth = 1.2f)
        }
    }

    private fun drawThemedCorner(
        scope: DrawScope,
        cornerX: Float,
        cornerY: Float,
        rotationDeg: Float,
        colors: TableThemeColors
    ) {
        scope.rotate(rotationDeg, pivot = Offset(cornerX, cornerY)) {
            val l = 46f
            val th = 15f
            val bracket = Path().apply {
                moveTo(cornerX + 8f, cornerY)
                lineTo(cornerX + l, cornerY)
                lineTo(cornerX + l, cornerY + th)
                lineTo(cornerX + th, cornerY + th)
                lineTo(cornerX + th, cornerY + l)
                lineTo(cornerX, cornerY + l)
                lineTo(cornerX, cornerY + 8f)
                quadraticTo(cornerX, cornerY, cornerX + 8f, cornerY)
                close()
            }
            translate(2.5f, 4f) { drawPath(bracket, Color.Black.copy(alpha = 0.4f)) }
            drawPath(
                bracket,
                brush = Brush.linearGradient(
                    colors = listOf(colors.cornerColors[0], colors.cornerColors[1], colors.cornerColors[2], colors.cornerColors[1]),
                    start = Offset(cornerX, cornerY),
                    end = Offset(cornerX + l, cornerY + l)
                )
            )
            drawPath(bracket, Color.White.copy(alpha = 0.35f), style = Stroke(1.2f))
            drawPath(bracket, Color.Black.copy(alpha = 0.35f), style = Stroke(0.6f))
            for (sc in listOf(Offset(cornerX + 7.5f, cornerY + 7.5f), Offset(cornerX + l - 8f, cornerY + 7.5f), Offset(cornerX + 7.5f, cornerY + l - 8f))) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.rivetHighlight, colors.cornerColors[1], colors.rivetColor),
                        center = sc - Offset(1f, 1f), radius = 4.5f
                    ),
                    radius = 3.6f, center = sc
                )
                drawLine(colors.rivetColor, sc - Offset(2.2f, -2.2f), sc + Offset(2.2f, -2.2f), strokeWidth = 1f)
            }
        }
    }

    // =====================================================================
    //  OBSTACLES
    // =====================================================================

    private fun rockPath(cx: Float, cy: Float, r: Float, seed: Int, verts: Int = 10): Path {
        val rnd = java.util.Random(seed * 31L + 7L)
        return Path().apply {
            for (i in 0 until verts) {
                val a = i * (2f * PI.toFloat() / verts) + rnd.nextFloat() * 0.25f
                val rr = r * (0.84f + rnd.nextFloat() * 0.2f)
                val x = cx + cos(a) * rr
                val y = cy + sin(a) * rr
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }

    fun drawObstacles(
        scope: DrawScope,
        obstacles: List<Obstacle>,
        boardCenter: Offset
    ) = with(scope) {
        val t = nowSeconds()
        for (obs in obstacles) {
            val cx = boardCenter.x + obs.pos.x
            val cy = boardCenter.y + obs.pos.y
            val r = obs.radius
            when (obs.type) {
                ObstacleType.ROCK -> {
                    softOval(Offset(cx + SHADOW_DX, cy + SHADOW_DY), r * 1.35f, r * 1.05f, 0.55f)
                    val body = rockPath(cx, cy, r, obs.id)
                    drawPath(
                        body,
                        Brush.radialGradient(
                            listOf(Color(0xFF9AA5B8), Color(0xFF66718A), Color(0xFF3C4559), Color(0xFF232937)),
                            center = Offset(cx + LIGHT_X * r * 0.5f, cy + LIGHT_Y * r * 0.5f),
                            radius = r * 1.5f
                        )
                    )
                    // top facet
                    val facet = rockPath(cx + LIGHT_X * r * 0.22f, cy + LIGHT_Y * r * 0.22f, r * 0.55f, obs.id + 3, 7)
                    drawPath(facet, Color.White.copy(alpha = 0.16f))
                    drawPath(facet, Color.White.copy(alpha = 0.18f), style = Stroke(1f))
                    // crack & moss speck
                    drawLine(Color.Black.copy(alpha = 0.35f), Offset(cx + r * 0.15f, cy - r * 0.1f), Offset(cx + r * 0.55f, cy + r * 0.35f), strokeWidth = 1.5f)
                    drawPath(body, Color.Black.copy(alpha = 0.45f), style = Stroke(1.6f))
                }
                ObstacleType.MOVING_BOULDER -> {
                    softOval(Offset(cx + SHADOW_DX, cy + SHADOW_DY), r * 1.35f, r * 1.1f, 0.6f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFCFD8DC), Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF263238)),
                            center = Offset(cx + LIGHT_X * r * 0.45f, cy + LIGHT_Y * r * 0.45f),
                            radius = r * 1.35f
                        ),
                        radius = r, center = Offset(cx, cy)
                    )
                    // rolling bands rotate with travel position
                    val roll = (if (obs.isHorizontal) obs.pos.x else obs.pos.y) / r * 57.3f
                    rotate(roll, pivot = Offset(cx, cy)) {
                        for (k in 0 until 3) {
                            drawArc(
                                Color(0x55263238), startAngle = k * 120f, sweepAngle = 70f, useCenter = false,
                                topLeft = Offset(cx - r * 0.7f, cy - r * 0.7f), size = Size(r * 1.4f, r * 1.4f),
                                style = Stroke(3f, cap = StrokeCap.Round)
                            )
                        }
                    }
                    // snow cap
                    drawArc(
                        Color.White.copy(alpha = 0.75f), startAngle = 200f, sweepAngle = 110f, useCenter = false,
                        topLeft = Offset(cx - r * 0.82f, cy - r * 0.82f), size = Size(r * 1.64f, r * 1.64f),
                        style = Stroke(r * 0.22f, cap = StrokeCap.Round)
                    )
                    drawCircle(Color.Black.copy(alpha = 0.4f), radius = r, center = Offset(cx, cy), style = Stroke(1.5f))
                }
                ObstacleType.LAVA_HAZARD -> {
                    val pulse = 0.85f + 0.15f * sin(t * 3f + obs.id)
                    glow(Offset(cx, cy), r * 2.1f * pulse, Color(0x99FF3D00))
                    val crust = rockPath(cx, cy, r * 1.22f, obs.id + 11, 12)
                    drawPath(crust, Brush.radialGradient(listOf(Color(0xFF4E1A0A), Color(0xFF1A0703)), center = Offset(cx, cy), radius = r * 1.3f))
                    drawPath(crust, Color(0xFFFF6D00).copy(alpha = 0.45f), style = Stroke(1.5f))
                    val pool = rockPath(cx, cy, r * 0.98f, obs.id + 23, 11)
                    drawPath(
                        pool,
                        Brush.radialGradient(
                            listOf(Color(0xFFFFF59D), Color(0xFFFFC400), Color(0xFFFF6D00), Color(0xFFD50000)),
                            center = Offset(cx + sin(t * 1.3f) * r * 0.12f, cy + cos(t * 1.1f) * r * 0.12f),
                            radius = r * pulse
                        )
                    )
                    // slowly drifting crust plates
                    for (k in 0 until 3) {
                        val a = t * 0.4f + k * 2.1f + obs.id
                        val p = Offset(cx + cos(a) * r * 0.45f, cy + sin(a) * r * 0.4f)
                        drawPath(rockPath(p.x, p.y, r * 0.2f, obs.id * 3 + k, 6), Color(0xCC5D1A08))
                    }
                    val bub = (t * 1.7f + obs.id) % 1f
                    drawCircle(Color(0xFFFFF9C4).copy(alpha = 1f - bub), radius = r * 0.12f + bub * r * 0.18f, center = Offset(cx - r * 0.25f, cy + r * 0.1f), style = Stroke(1.5f))
                }
                ObstacleType.TEMPLE_PILLAR -> {
                    softOval(Offset(cx + SHADOW_DX * 1.6f, cy + SHADOW_DY * 1.6f), r * 1.5f, r * 1.25f, 0.6f)
                    // square plinth
                    val pl = r * 1.12f
                    drawRect(
                        Brush.linearGradient(listOf(Color(0xFF6D6577), Color(0xFF2E2836)), start = Offset(cx - pl, cy - pl), end = Offset(cx + pl, cy + pl)),
                        topLeft = Offset(cx - pl, cy - pl), size = Size(pl * 2f, pl * 2f)
                    )
                    drawRect(Color.White.copy(alpha = 0.18f), topLeft = Offset(cx - pl, cy - pl), size = Size(pl * 2f, pl * 2f), style = Stroke(1.2f))
                    // fluted column top
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFB8AFC2), Color(0xFF7A7085), Color(0xFF423A4C)),
                            center = Offset(cx + LIGHT_X * r * 0.4f, cy + LIGHT_Y * r * 0.4f), radius = r * 1.2f
                        ),
                        radius = r * 0.92f, center = Offset(cx, cy)
                    )
                    for (k in 0 until 12) {
                        val a = k * (PI.toFloat() / 6f)
                        drawLine(
                            Color.Black.copy(alpha = 0.22f),
                            Offset(cx + cos(a) * r * 0.6f, cy + sin(a) * r * 0.6f),
                            Offset(cx + cos(a) * r * 0.9f, cy + sin(a) * r * 0.9f),
                            strokeWidth = 1.4f
                        )
                    }
                    val rune = 0.6f + 0.4f * sin(t * 2f + obs.id)
                    glow(Offset(cx, cy), r * 0.7f, Color(0xFFD500F9).copy(alpha = 0.5f * rune))
                    drawCircle(Color(0xFFEA80FC).copy(alpha = rune), radius = r * 0.42f, center = Offset(cx, cy), style = Stroke(2f))
                    drawPath(starPath(cx, cy, r * 0.3f, r * 0.12f, 3, t * 0.5f), Color(0xFFF3E5F5).copy(alpha = rune))
                }
            }
        }
    }

    // =====================================================================
    //  ARMADILLO
    // =====================================================================

    private class ArmPalette(
        val shellLight: Color, val shellMid: Color, val shellDark: Color, val groove: Color,
        val skin: Color, val skinDark: Color, val ear: Color, val claw: Color, val aura: List<Color>
    )

    private val TAN = ArmPalette(
        shellLight = Color(0xFFF2C38E), shellMid = Color(0xFFC4884E), shellDark = Color(0xFF6E3D19), groove = Color(0xFF3E1F0B),
        skin = Color(0xFFE8B98A), skinDark = Color(0xFFA36A3C), ear = Color(0xFFE59A8C), claw = Color(0xFFF5E6C8),
        aura = listOf(Color(0x00FFD54F), Color(0x6600E5FF), Color(0xAAFFD700), Color(0x00FFD700))
    )
    private val BLUE = ArmPalette(
        shellLight = Color(0xFF90CAF9), shellMid = Color(0xFF1E88E5), shellDark = Color(0xFF0B3C7A), groove = Color(0xFF061D3D),
        skin = Color(0xFF64B5F6), skinDark = Color(0xFF1565C0), ear = Color(0xFF80D8FF), claw = Color(0xFFE1F5FE),
        aura = listOf(Color(0x0000B0FF), Color(0x6600E5FF), Color(0xAA40C4FF), Color(0x000091EA))
    )

    fun drawArmadillo(
        scope: DrawScope,
        armadillo: ArmadilloState,
        boardCenter: Offset,
        isBlue: Boolean = false
    ) = with(scope) {
        val cx = boardCenter.x + armadillo.pos.x
        val cy = boardCenter.y + armadillo.pos.y
        val scale = armadillo.eatenScale
        if (scale <= 0.01f) return@with

        val pal = if (isBlue) BLUE else TAN
        val heading = armadillo.headingAngle
        val headingDeg = (heading * 180f / PI).toFloat()
        val ll = localLight(heading)

        // Power aura: soft energy field + orbiting sparks + shield pips
        if (armadillo.isPowerActive) {
            val pulse = sin(armadillo.auraPhase) * 4f
            val outer = (46f + pulse) * scale
            drawCircle(
                Brush.radialGradient(pal.aura, center = Offset(cx, cy), radius = outer + 10f),
                radius = outer + 10f, center = Offset(cx, cy)
            )
            rotate(armadillo.auraPhase * 40f, pivot = Offset(cx, cy)) {
                drawCircle(
                    pal.aura[2].copy(alpha = 0.8f), radius = outer - 4f, center = Offset(cx, cy),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 9f)))
                )
            }
            for (arc in 0 until 4) {
                val a = armadillo.auraPhase * 1.5f + arc * (PI.toFloat() / 2f)
                val p = Offset(cx + cos(a) * (outer - 4f), cy + sin(a) * (outer - 4f))
                sparkle(p, 6f, if (arc % 2 == 0) Color(0xFF00E5FF) else if (isBlue) Color(0xFF80D8FF) else Color(0xFFFFEA00))
            }
            for (ch in 0 until armadillo.powerChancesRemaining) {
                val a = -PI.toFloat() / 2f + (ch - 0.5f) * 0.7f
                val p = Offset(cx + cos(a) * (outer + 6f), cy + sin(a) * (outer + 6f))
                glow(p, 10f, Color(0x8800E5FF))
                drawCircle(Color(0xFF00E5FF), radius = 4.5f, center = p)
                drawCircle(Color.White, radius = 2f, center = p)
            }
        }

        // Ground shadow (screen-space offset, body-aligned)
        withTransform({
            translate(cx + SHADOW_DX * scale, cy + SHADOW_DY * scale)
            rotate(headingDeg, pivot = Offset.Zero)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            softOval(Offset(-2f, 0f), 40f, 28f, 0.55f)
        }

        withTransform({
            translate(left = cx, top = cy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            rotate(degrees = headingDeg, pivot = Offset.Zero)
        }) {
            // spawn shield
            if (armadillo.invulnerableTimer > 0f) {
                val a = (sin(armadillo.invulnerableTimer * 12f) * 0.25f + 0.4f).coerceIn(0.1f, 0.75f)
                val ring = if (isBlue) Color(0xFF00E5FF) else Color(0xFFFFD54F)
                glow(Offset.Zero, 48f, ring.copy(alpha = a * 0.35f))
                drawCircle(ring.copy(alpha = a), radius = 40f, center = Offset.Zero, style = Stroke(2.5f))
            }

            // legs + claws
            val step = sin(armadillo.legStepPhase) * 6f
            val legs = listOf(
                Offset(14f, -21f + step), Offset(14f, 21f - step),
                Offset(-17f, -22f - step), Offset(-17f, 22f + step)
            )
            for ((i, lp) in legs.withIndex()) {
                val side = if (lp.y < 0) -1f else 1f
                drawOval(
                    Brush.radialGradient(listOf(pal.skin, pal.skinDark), center = lp + Offset(ll.x * 2f, ll.y * 2f), radius = 9f),
                    topLeft = lp - Offset(7f, 5f), size = Size(14f, 10f)
                )
                for (k in -1..1) {
                    val tip = lp + Offset(7.5f + if (i < 2) 1f else 0f, k * 2.6f + side * 2.5f)
                    drawLine(pal.claw, lp + Offset(4.5f, k * 2.2f + side * 2f), tip, strokeWidth = 1.4f, cap = StrokeCap.Round)
                }
            }

            // segmented tapering tail
            val wag = sin(armadillo.tailWagPhase) * 5f
            for (k in 0 until 7) {
                val f = k / 6f
                val tx = -26f - f * 22f
                val ty = wag * f * f
                val tr = 6f - f * 3.6f
                drawCircle(
                    Brush.radialGradient(listOf(pal.shellLight, pal.shellMid, pal.shellDark), center = Offset(tx + ll.x * tr * 0.5f, ty + ll.y * tr * 0.5f), radius = tr * 1.4f),
                    radius = tr, center = Offset(tx, ty)
                )
                drawCircle(pal.groove.copy(alpha = 0.6f), radius = tr, center = Offset(tx, ty), style = Stroke(0.8f))
            }

            // head (under the shell's front edge)
            val head = Path().apply {
                moveTo(14f, -11f)
                cubicTo(24f, -12f, 33f, -6f, 40f, -2.5f)
                cubicTo(43f, -1f, 43f, 1f, 40f, 2.5f)
                cubicTo(33f, 6f, 24f, 12f, 14f, 11f)
                close()
            }
            drawPath(
                head,
                Brush.radialGradient(
                    listOf(pal.skin.lighten(0.2f), pal.skin, pal.skinDark),
                    center = Offset(24f + ll.x * 6f, ll.y * 6f), radius = 22f
                )
            )
            drawPath(head, pal.skinDark.darken(0.3f).copy(alpha = 0.6f), style = Stroke(1f))
            // armored head cap
            val cap = Path().apply {
                moveTo(17f, -7f)
                quadraticTo(27f, -6f, 32f, -2f)
                lineTo(32f, 2f)
                quadraticTo(27f, 6f, 17f, 7f)
                close()
            }
            drawPath(cap, Brush.linearGradient(listOf(pal.shellLight, pal.shellMid), start = Offset(17f, -7f), end = Offset(32f, 7f)))
            drawPath(cap, pal.groove.copy(alpha = 0.5f), style = Stroke(0.8f))
            // ears
            for (s in listOf(-1f, 1f)) {
                val ear = Path().apply {
                    moveTo(17f, s * 8f)
                    quadraticTo(14f, s * 19f, 21f, s * 17f)
                    quadraticTo(23f, s * 12f, 22f, s * 8.5f)
                    close()
                }
                drawPath(ear, pal.skinDark)
                drawPath(ear, pal.ear.copy(alpha = 0.8f), style = Stroke(1.2f))
            }
            // eyes
            for (s in listOf(-1f, 1f)) {
                val e = Offset(27f, s * 6.2f)
                drawCircle(Color(0xFF120A06), radius = 3.1f, center = e)
                drawCircle(Color.White, radius = 1.15f, center = e + Offset(0.9f, -0.9f))
                drawCircle(Color.White.copy(alpha = 0.5f), radius = 0.6f, center = e + Offset(-1f, 1f))
            }
            // nose
            drawOval(Color(0xFF2B1408), topLeft = Offset(39f, -2.6f), size = Size(4.5f, 5.2f))
            drawCircle(Color.White.copy(alpha = 0.4f), radius = 0.8f, center = Offset(40.5f, -1.2f))

            // ===== the shell =====
            val shellRect = androidx.compose.ui.geometry.Rect(-30f, -23f, 24f, 23f)
            val shell = Path().apply { addOval(shellRect) }
            // rim shadow so the shell sits above the legs
            drawOval(Color.Black.copy(alpha = 0.35f), topLeft = shellRect.topLeft + Offset(1f, 2f), size = Size(shellRect.width + 1f, shellRect.height + 1f))
            clipPath(shell) {
                // base dome
                drawRect(
                    Brush.radialGradient(
                        listOf(pal.shellLight, pal.shellMid, pal.shellDark),
                        center = Offset(-3f + ll.x * 14f, ll.y * 14f), radius = 40f
                    ),
                    topLeft = shellRect.topLeft, size = shellRect.size
                )
                // transverse bands (each band is its own little cylinder)
                val bandCount = 7
                val bx0 = -22f
                val bw = 5.6f
                for (b in 0 until bandCount) {
                    val x = bx0 + b * bw
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.22f), Color.White.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.06f)),
                            startX = x, endX = x + bw
                        ),
                        topLeft = Offset(x, shellRect.top), size = Size(bw, shellRect.height)
                    )
                    val groove = Path().apply {
                        moveTo(x, shellRect.top)
                        cubicTo(x + 3f, -8f, x + 3f, 8f, x, shellRect.bottom)
                    }
                    drawPath(groove, pal.groove.copy(alpha = 0.85f), style = Stroke(1.6f))
                    // scute studs
                    for (sy in -3..3) {
                        val py = sy * 5.6f
                        drawCircle(pal.shellLight.lighten(0.3f).copy(alpha = 0.55f - abs(sy) * 0.06f), radius = 1.05f, center = Offset(x + bw * 0.55f + 1.2f, py))
                    }
                }
                // front and rear shields: fine hex scutes
                for (side in listOf(-1, 1)) {
                    val sx = if (side < 0) -27f else 19f
                    for (row in -3..3) for (col in 0..1) {
                        val p = Offset(sx + col * 4.2f * (if (side < 0) 1f else -1f) * -1f + (row % 2) * 1.8f, row * 5f)
                        drawCircle(pal.groove.copy(alpha = 0.35f), radius = 2.2f, center = p, style = Stroke(0.7f))
                    }
                }
                // dome ambient occlusion toward the rim
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f),
                        center = Offset(-3f, 0f), radius = 30f
                    ),
                    topLeft = shellRect.topLeft, size = shellRect.size
                )
                // specular
                withTransform({ scale(1.6f, 1f, pivot = Offset(-3f + ll.x * 11f, ll.y * 11f)) }) {
                    glow(Offset(-3f + ll.x * 11f, ll.y * 11f), 9f, Color.White.copy(alpha = 0.55f))
                }
            }
            drawOval(pal.groove, topLeft = shellRect.topLeft, size = shellRect.size, style = Stroke(1.8f))
            // skirt highlight on lit side
            drawArc(
                Color.White.copy(alpha = 0.35f),
                startAngle = (atan2(ll.y, ll.x) * 57.3f) - 45f, sweepAngle = 90f, useCenter = false,
                topLeft = shellRect.topLeft + Offset(1.5f, 1.5f), size = Size(shellRect.width - 3f, shellRect.height - 3f),
                style = Stroke(1.6f, cap = StrokeCap.Round)
            )
        }
    }

    // =====================================================================
    //  SNAKES
    // =====================================================================

    private class SnakePalette(
        val light: Color, val mid: Color, val dark: Color, val outline: Color,
        val pattern: Color, val patternEdge: Color, val belly: Color, val eye: Color
    )

    private val GREEN_SNAKE = SnakePalette(
        light = Color(0xFF9CCC65), mid = Color(0xFF43A047), dark = Color(0xFF1B5E20), outline = Color(0xFF0A2E0D),
        pattern = Color(0xFF1B3A12), patternEdge = Color(0xFFFFD54F), belly = Color(0xFFE6EE9C), eye = Color(0xFFFFD54F)
    )
    private val PURPLE_SNAKE = SnakePalette(
        light = Color(0xFFE1A8F0), mid = Color(0xFF9C27B0), dark = Color(0xFF4A148C), outline = Color(0xFF1C0533),
        pattern = Color(0xFF2A0845), patternEdge = Color(0xFFFF80AB), belly = Color(0xFFF8BBD0), eye = Color(0xFFFF1F8E)
    )

    fun drawSnakes(
        scope: DrawScope,
        snakes: List<SnakeState>,
        boardCenter: Offset
    ) {
        for (i in snakes.indices) {
            drawSnake(scope, snakes[i], boardCenter, snakeIndex = i)
        }
    }

    fun drawSnake(
        scope: DrawScope,
        snake: SnakeState,
        boardCenter: Offset,
        snakeIndex: Int = 0
    ) = with(scope) {
        val pal = if (snakeIndex % 2 == 1) PURPLE_SNAKE else GREEN_SNAKE
        val headX = boardCenter.x + snake.headPos.x
        val headY = boardCenter.y + snake.headPos.y

        // 1. Spine: head + segments with the slither wave applied
        val segCount = snake.bodySegments.size
        val spine = ArrayList<Offset>(segCount + 1)
        val radii = ArrayList<Float>(segCount + 1)
        spine += Offset(headX, headY)
        radii += 14f
        for (i in 0 until segCount) {
            val seg = snake.bodySegments[i]
            val wave = sin(snake.slitherPhase + i * 0.45f) * (6f * (1f - i.toFloat() / segCount))
            val prev = if (i == 0) snake.headPos else snake.bodySegments[i - 1]
            val angle = atan2(prev.y - seg.y, prev.x - seg.x)
            val perp = angle + (PI / 2).toFloat()
            spine += Offset(boardCenter.x + seg.x + cos(perp) * wave, boardCenter.y + seg.y + sin(perp) * wave)
            val f = i.toFloat() / max(1, segCount - 1)
            radii += 14.5f - 9f * f * f - 1.5f * f
        }

        // 2. Densify so the body reads as one smooth tube
        val pts = ArrayList<Offset>()
        val rs = ArrayList<Float>()
        for (i in 0 until spine.size - 1) {
            val a = spine[i]
            val b = spine[i + 1]
            val d = (b - a).getDistance()
            val n = max(1, (d / max(2.5f, radii[i] * 0.35f)).toInt())
            for (k in 0 until n) {
                val f = k / n.toFloat()
                pts += Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
                rs += radii[i] + (radii[i + 1] - radii[i]) * f
            }
        }
        pts += spine.last(); rs += radii.last()

        // 3. Uniform soft shadow (drawn into an offscreen layer so overlaps don't stack)
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in pts) { minX = min(minX, p.x); minY = min(minY, p.y); maxX = max(maxX, p.x); maxY = max(maxY, p.y) }
        val bounds = androidx.compose.ui.geometry.Rect(minX - 40f, minY - 40f, maxX + 50f, maxY + 50f)
        drawIntoCanvas { canvas ->
            canvas.saveLayer(bounds, Paint().apply { alpha = 0.30f })
            for (i in pts.indices) drawCircle(Color.Black, radius = rs[i] * 1.12f, center = pts[i] + Offset(SHADOW_DX + 1f, SHADOW_DY + 1f))
            drawCircle(Color.Black, radius = 18f, center = Offset(headX + SHADOW_DX + 2f, headY + SHADOW_DY + 2f))
            canvas.restore()
        }

        // 3b. io-style neon aura around the body (one layer, so overlaps don't stack)
        drawIntoCanvas { canvas ->
            canvas.saveLayer(bounds, Paint().apply { alpha = if (snake.isStriking) 0.45f else 0.22f })
            for (i in pts.indices step 2) drawCircle(pal.mid.lighten(0.2f), radius = rs[i] * 1.75f, center = pts[i])
            canvas.restore()
        }

        // 4. Outline + body (tail to head)
        for (i in pts.indices.reversed()) drawCircle(pal.outline, radius = rs[i] + 1.6f, center = pts[i])
        val hl = Offset(LIGHT_X, LIGHT_Y)
        for (i in pts.indices.reversed()) {
            val r = rs[i]
            drawCircle(
                Brush.radialGradient(
                    0f to pal.light,
                    0.45f to pal.mid,
                    1f to pal.dark,
                    center = pts[i] + hl * (r * 0.45f),
                    radius = r * 1.45f
                ),
                radius = r, center = pts[i]
            )
        }

        // 5. Dorsal saddle pattern + scale texture + glossy highlight
        for (i in 1 until spine.size - 1) {
            val p = spine[i]
            val dir = spine[i - 1] - spine[i + 1]
            val ang = atan2(dir.y, dir.x)
            val r = radii[i]
            if (i % 2 == 1) {
                withTransform({ rotate(ang * 57.3f, pivot = p) }) {
                    val d = Path().apply {
                        moveTo(p.x - r * 0.85f, p.y)
                        lineTo(p.x, p.y - r * 0.62f)
                        lineTo(p.x + r * 0.85f, p.y)
                        lineTo(p.x, p.y + r * 0.62f)
                        close()
                    }
                    drawPath(d, pal.pattern.copy(alpha = 0.85f))
                    drawPath(d, pal.patternEdge.copy(alpha = 0.75f), style = Stroke(1.3f))
                    drawCircle(pal.patternEdge.copy(alpha = 0.6f), radius = r * 0.14f, center = p)
                }
            } else {
                // flank scale ticks
                val perp = ang + PI.toFloat() / 2f
                for (s in listOf(-1f, 1f)) {
                    val q = p + Offset(cos(perp), sin(perp)) * (r * 0.62f * s)
                    drawCircle(pal.dark.copy(alpha = 0.5f), radius = r * 0.16f, center = q)
                }
            }
        }
        for (i in pts.indices step 2) {
            val r = rs[i]
            drawCircle(Color.White.copy(alpha = 0.10f), radius = r * 0.32f, center = pts[i] + hl * (r * 0.5f))
        }

        // 6. Head
        val headAngleDeg = (snake.headAngle * 180f / PI).toFloat()
        val hll = localLight(snake.headAngle)
        withTransform({
            translate(left = headX, top = headY)
            rotate(degrees = headAngleDeg, pivot = Offset.Zero)
        }) {
            if (snake.tongueVisible) {
                val flick = sin(snake.tongueFlickTimer * 40f) * 2f
                val tl = 20f
                val tongue = Path().apply {
                    moveTo(22f, 0f)
                    quadraticTo(22f + tl * 0.5f, flick, 22f + tl, 0f)
                    moveTo(22f + tl, 0f); lineTo(22f + tl + 6f, -4f + flick)
                    moveTo(22f + tl, 0f); lineTo(22f + tl + 6f, 4f + flick)
                }
                drawPath(tongue, Color(0xFF7F0000), style = Stroke(3.2f, cap = StrokeCap.Round))
                drawPath(tongue, Color(0xFFFF5252), style = Stroke(1.8f, cap = StrokeCap.Round))
            }

            val head = Path().apply {
                moveTo(-10f, -12f)
                cubicTo(-2f, -17f, 10f, -16f, 18f, -9f)
                cubicTo(24f, -5f, 26f, -2f, 26f, 0f)
                cubicTo(26f, 2f, 24f, 5f, 18f, 9f)
                cubicTo(10f, 16f, -2f, 17f, -10f, 12f)
                cubicTo(-14f, 6f, -14f, -6f, -10f, -12f)
                close()
            }
            drawPath(head, pal.outline, style = Stroke(3.2f))
            drawPath(
                head,
                Brush.radialGradient(
                    0f to pal.light, 0.5f to pal.mid, 1f to pal.dark,
                    center = Offset(6f + hll.x * 8f, hll.y * 8f), radius = 26f
                )
            )
            // crown plates
            val crown = Path().apply {
                moveTo(-4f, -7f); lineTo(12f, -5f); lineTo(16f, 0f); lineTo(12f, 5f); lineTo(-4f, 7f); lineTo(-1f, 0f); close()
            }
            drawPath(crown, pal.pattern.copy(alpha = 0.7f))
            drawPath(crown, pal.patternEdge.copy(alpha = 0.6f), style = Stroke(1.1f))
            // brow ridges
            for (s in listOf(-1f, 1f)) {
                drawLine(pal.dark.darken(0.3f), Offset(6f, s * 12.5f), Offset(17f, s * 8.5f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            }
            // glossy head highlight
            withTransform({ scale(1.7f, 1f, pivot = Offset(4f + hll.x * 7f, hll.y * 7f)) }) {
                glow(Offset(4f + hll.x * 7f, hll.y * 7f), 7f, Color.White.copy(alpha = 0.35f))
            }
            // nostrils
            drawCircle(pal.outline, radius = 1.3f, center = Offset(22.5f, -2.5f))
            drawCircle(pal.outline, radius = 1.3f, center = Offset(22.5f, 2.5f))
            // eyes with glow + slit
            for (s in listOf(-1f, 1f)) {
                val e = Offset(12f, s * 9.5f)
                glow(e, 9f, pal.eye.copy(alpha = if (snake.isStriking) 0.7f else 0.35f))
                drawOval(pal.outline, topLeft = e - Offset(4.8f, 3.4f), size = Size(9.6f, 6.8f))
                drawOval(
                    Brush.radialGradient(listOf(pal.eye.lighten(0.4f), pal.eye, pal.eye.darken(0.4f)), center = e - Offset(1f, 1f), radius = 4.5f),
                    topLeft = e - Offset(4f, 2.7f), size = Size(8f, 5.4f)
                )
                drawLine(Color(0xFF120000), e - Offset(0f, 2.6f), e + Offset(0f, 2.6f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                drawCircle(Color.White.copy(alpha = 0.8f), radius = 0.9f, center = e + Offset(-1.6f, -1f))
            }
            if (snake.isStriking) {
                for (s in listOf(-1f, 1f)) {
                    val fang = Path().apply { moveTo(20f, s * 3.5f); lineTo(28f, s * 2.2f); lineTo(21f, s * 1.2f); close() }
                    drawPath(fang, Color.White)
                    drawPath(fang, Color(0x55000000), style = Stroke(0.6f))
                }
            }
        }
    }

    // =====================================================================
    //  FRUIT
    // =====================================================================

    private fun applePath(cx: Float, cy: Float, r: Float): Path = Path().apply {
        moveTo(cx, cy - r * 0.72f)
        cubicTo(cx + r * 0.45f, cy - r * 1.12f, cx + r * 1.12f, cy - r * 0.8f, cx + r * 1.02f, cy - r * 0.05f)
        cubicTo(cx + r * 0.98f, cy + r * 0.65f, cx + r * 0.5f, cy + r * 1.02f, cx + r * 0.18f, cy + r * 0.95f)
        quadraticTo(cx, cy + r * 0.88f, cx - r * 0.18f, cy + r * 0.95f)
        cubicTo(cx - r * 0.5f, cy + r * 1.02f, cx - r * 0.98f, cy + r * 0.65f, cx - r * 1.02f, cy - r * 0.05f)
        cubicTo(cx - r * 1.12f, cy - r * 0.8f, cx - r * 0.45f, cy - r * 1.12f, cx, cy - r * 0.72f)
        close()
    }

    private fun DrawScope.shinyFruitBody(
        cx: Float, cy: Float, r: Float,
        light: Color, mid: Color, dark: Color, rim: Color
    ) {
        val body = applePath(cx, cy, r)
        drawPath(body, dark.darken(0.45f), style = Stroke(2.2f))
        drawPath(
            body,
            Brush.radialGradient(
                0f to light, 0.35f to mid, 0.85f to dark, 1f to dark.darken(0.25f),
                center = Offset(cx + LIGHT_X * r * 0.45f, cy + LIGHT_Y * r * 0.35f),
                radius = r * 1.5f
            )
        )
        // reflected rim light from the table (bottom-right)
        clipPath(body) {
            drawCircle(
                Brush.radialGradient(
                    0f to Color.Transparent, 0.75f to Color.Transparent, 1f to rim,
                    center = Offset(cx - r * 0.25f, cy - r * 0.3f), radius = r * 1.32f
                ),
                radius = r * 1.4f, center = Offset(cx, cy)
            )
        }
        // top dimple
        drawOval(
            Brush.radialGradient(listOf(dark.darken(0.5f).copy(alpha = 0.8f), Color.Transparent), center = Offset(cx, cy - r * 0.68f), radius = r * 0.35f),
            topLeft = Offset(cx - r * 0.35f, cy - r * 0.85f), size = Size(r * 0.7f, r * 0.34f)
        )
        // specular: soft lobe + crisp dot
        withTransform({ rotate(-30f, pivot = Offset(cx - r * 0.42f, cy - r * 0.32f)) }) {
            drawOval(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent), center = Offset(cx - r * 0.42f, cy - r * 0.32f), radius = r * 0.36f),
                topLeft = Offset(cx - r * 0.42f - r * 0.36f, cy - r * 0.32f - r * 0.2f), size = Size(r * 0.72f, r * 0.4f)
            )
        }
        drawCircle(Color.White.copy(alpha = 0.95f), radius = r * 0.09f, center = Offset(cx - r * 0.5f, cy - r * 0.38f))
    }

    private fun DrawScope.stemAndLeaf(cx: Float, cy: Float, r: Float, stem: Color, leafA: Color, leafB: Color) {
        val stemPath = Path().apply {
            moveTo(cx, cy - r * 0.7f)
            quadraticTo(cx - r * 0.06f, cy - r * 1.1f, cx - r * 0.28f, cy - r * 1.45f)
        }
        drawPath(stemPath, stem.darken(0.4f), style = Stroke(r * 0.2f, cap = StrokeCap.Round))
        drawPath(stemPath, stem, style = Stroke(r * 0.11f, cap = StrokeCap.Round))
        val lb = Offset(cx - r * 0.06f, cy - r * 1.08f)
        val leaf = Path().apply {
            moveTo(lb.x, lb.y)
            cubicTo(lb.x + r * 0.3f, lb.y - r * 0.55f, lb.x + r * 0.85f, lb.y - r * 0.5f, lb.x + r * 1.0f, lb.y - r * 0.2f)
            cubicTo(lb.x + r * 0.75f, lb.y + r * 0.12f, lb.x + r * 0.3f, lb.y + r * 0.15f, lb.x, lb.y)
            close()
        }
        drawPath(leaf, Brush.linearGradient(listOf(leafA, leafB), start = Offset(lb.x, lb.y - r * 0.5f), end = Offset(lb.x + r, lb.y + r * 0.1f)))
        drawPath(leaf, leafB.darken(0.35f), style = Stroke(0.9f))
        drawLine(leafA.lighten(0.5f).copy(alpha = 0.8f), lb, Offset(lb.x + r * 0.85f, lb.y - r * 0.22f), strokeWidth = 0.9f)
    }

    fun drawApples(
        scope: DrawScope,
        apples: List<AppleItem>,
        boardCenter: Offset
    ) = with(scope) {
        for (apple in apples) {
            val ax = boardCenter.x + apple.pos.x
            val ay = boardCenter.y + apple.pos.y
            val bob = if (apple.isSpecialEventFruit) 0f else sin(apple.bobPhase) * 3f
            val dropH = apple.dropHeight
            val curY = ay + bob - dropH

            // contact shadow shrinks & fades as the fruit rises
            val lift = (dropH / 180f).coerceIn(0f, 0.7f)
            val bobLift = (bob + 3f) / 6f * 0.12f
            softOval(
                Offset(ax + SHADOW_DX * 0.6f + dropH * 0.08f, ay + 12f + dropH * 0.1f),
                18f * (1f - lift) * (1f - bobLift),
                9f * (1f - lift) * (1f - bobLift),
                (0.55f - lift * 0.5f).coerceAtLeast(0.12f)
            )

            when (apple.fruitType) {
                FruitType.REGULAR_APPLE -> {
                    val r = 16f
                    shinyFruitBody(ax, curY, r, Color(0xFFFF8A80), Color(0xFFE53935), Color(0xFF8E0000), Color(0x66FFB199))
                    stemAndLeaf(ax, curY, r, Color(0xFF6D4C41), Color(0xFF7CB342), Color(0xFF2E7D32))
                }
                FruitType.ESCAPE_GOLDEN -> {
                    val pulse = sin(apple.sparkleTimer * 8f) * 1.2f
                    val r = 17f + pulse
                    glow(Offset(ax, curY), r * 2.2f, Color(0x88FFD54F))
                    shinyFruitBody(ax, curY, r, Color(0xFFFFFDE7), Color(0xFFFFC107), Color(0xFFB26A00), Color(0x88FFF59D))
                    stemAndLeaf(ax, curY, r, Color(0xFFFF8F00), Color(0xFFFFF176), Color(0xFFF9A825))
                    for (k in 0 until 3) {
                        val a = apple.sparkleTimer * 2.5f + k * 2.094f
                        val tw = 0.5f + 0.5f * sin(apple.sparkleTimer * 9f + k * 2f)
                        sparkle(Offset(ax + cos(a) * r * 1.5f, curY + sin(a) * r * 1.35f), 3f + tw * 3.5f, Color(0xFFFFF8E1).copy(alpha = 0.5f + tw * 0.5f))
                    }
                }
                FruitType.ESCAPE_STAR_POWER -> {
                    val pulse = sin(apple.sparkleTimer * 10f) * 2f
                    val r = 19f + pulse
                    val c = Offset(ax, curY)
                    glow(c, r * 2.4f, Color(0x66FF007F))
                    glow(c, r * 1.8f, Color(0x8800E5FF))
                    rotate(apple.sparkleTimer * 90f, pivot = c) {
                        drawCircle(
                            Color(0xCCFFFFFF), radius = r * 1.35f, center = c,
                            style = Stroke(1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 7f)))
                        )
                    }
                    // carambola: 5 rounded lobes
                    val rot = apple.sparkleTimer * 1.2f
                    val star = starPath(ax, curY, r * 1.05f, r * 0.55f, 5, rot)
                    drawPath(star, Color(0xFF1A237E), style = Stroke(5f, join = StrokeJoin.Round))
                    drawPath(
                        star,
                        Brush.radialGradient(
                            0f to Color(0xFFFFFFFF), 0.25f to Color(0xFF84FFFF), 0.65f to Color(0xFF00B0FF), 1f to Color(0xFF6200EA),
                            center = Offset(ax + LIGHT_X * r * 0.35f, curY + LIGHT_Y * r * 0.35f), radius = r * 1.3f
                        )
                    )
                    drawPath(star, Color.White.copy(alpha = 0.85f), style = Stroke(1.6f, join = StrokeJoin.Round))
                    // ridge lines
                    for (p in 0 until 5) {
                        val a = (p * 2 * PI / 5 - PI / 2).toFloat() + rot
                        drawLine(Color.White.copy(alpha = 0.45f), c, Offset(ax + cos(a) * r * 0.9f, curY + sin(a) * r * 0.9f), strokeWidth = 1.2f)
                    }
                    drawPath(starPath(ax, curY, r * 0.34f, r * 0.15f, 5, -rot * 2f), Color(0xFFFFEA00))
                    drawCircle(Color.White, radius = r * 0.13f, center = Offset(ax - r * 0.35f, curY - r * 0.4f))
                }
            }
        }
    }

    // =====================================================================
    //  PARTICLES
    // =====================================================================

    fun drawParticles(
        scope: DrawScope,
        particles: List<Particle>,
        boardCenter: Offset
    ) = with(scope) {
        for (p in particles) {
            val c = Offset(boardCenter.x + p.pos.x, boardCenter.y + p.pos.y)
            val a = p.alpha.coerceIn(0f, 1f)
            val r = p.size * (0.4f + 0.6f * a)
            glow(c, r * 2.4f, p.color.copy(alpha = 0.45f * a))
            drawCircle(p.color.copy(alpha = a), radius = r, center = c)
            drawCircle(Color.White.copy(alpha = 0.7f * a), radius = r * 0.4f, center = c - Offset(r * 0.2f, r * 0.2f))
        }
    }

    // =====================================================================
    //  JUICE / "IO-STYLE" FEEDBACK (render-only, gameplay untouched)
    //  Trails behind the armadillos, fruit pop rings with floating "+1",
    //  hit flash and a short camera shake. Events are inferred from frame
    //  to frame state changes so the engine does not need to know about them.
    // =====================================================================

    private class Pop(val pos: Offset, val start: Float, val color: Color, val label: String)

    private val trails = HashMap<Boolean, ArrayDeque<Offset>>()
    private val lastApplePos = HashMap<Int, Pair<Offset, FruitType>>()
    private val pops = ArrayList<Pop>()
    private val wasEaten = HashMap<Boolean, Boolean>()
    private var hitAt = -10f
    private var lastFxTime = 0f

    /**
     * Call once per frame before drawing. Returns the camera shake offset to apply to the board.
     */
    fun updateFx(armadillos: List<Pair<ArmadilloState, Boolean>>, apples: List<AppleItem>): Offset {
        val t = nowSeconds()
        if (t - lastFxTime > 1.5f) {
            // resumed after a pause / new round: drop stale history
            trails.clear(); lastApplePos.clear(); pops.clear(); wasEaten.clear()
        }
        lastFxTime = t

        for ((a, blue) in armadillos) {
            val trail = trails.getOrPut(blue) { ArrayDeque() }
            val p = Offset(a.pos.x, a.pos.y)
            if (a.isEaten || a.eatenScale < 0.95f) trail.clear()
            else if (trail.isEmpty() || (trail.last() - p).getDistance() > 3f) {
                trail.addLast(p)
                while (trail.size > 14) trail.removeFirst()
            }
            val eaten = a.isEaten
            if (eaten && wasEaten[blue] != true) {
                hitAt = t
                pops += Pop(p, t, Color(0xFFFF5252), "-1")
            }
            wasEaten[blue] = eaten
        }

        val seen = HashSet<Int>()
        for (a in apples) {
            seen += a.id
            val cur = Offset(a.pos.x, a.pos.y)
            val prev = lastApplePos[a.id]
            if (prev != null && (prev.first - cur).getDistance() > 60f && a.dropHeight <= 0.5f && prev.second == FruitType.REGULAR_APPLE) {
                pops += Pop(prev.first, t, Color(0xFFFFE082), "+1")
            }
            lastApplePos[a.id] = cur to a.fruitType
        }
        val gone = lastApplePos.keys.filter { it !in seen }
        for (id in gone) {
            val (pos, type) = lastApplePos.remove(id) ?: continue
            val col = when (type) {
                FruitType.ESCAPE_STAR_POWER -> Color(0xFF00E5FF)
                FruitType.ESCAPE_GOLDEN -> Color(0xFFFFD54F)
                else -> Color(0xFFFFE082)
            }
            pops += Pop(pos, t, col, if (type == FruitType.ESCAPE_STAR_POWER) "POWER!" else "+1")
        }
        pops.removeAll { t - it.start > 0.9f }

        val k = (1f - (t - hitAt) / 0.35f).coerceIn(0f, 1f)
        return if (k > 0f) Offset(sin(t * 90f) * 9f * k, cos(t * 75f) * 7f * k) else Offset.Zero
    }

    /** Trails go under the actors. */
    fun drawTrails(scope: DrawScope, boardCenter: Offset) = with(scope) {
        for ((blue, trail) in trails) {
            if (trail.size < 3) continue
            val col = if (blue) Color(0xFF40C4FF) else Color(0xFFFFC94D)
            var i = 0
            for (p in trail) {
                val f = (i + 1f) / trail.size
                glow(boardCenter + p, 10f + 14f * f, col.copy(alpha = 0.10f + 0.22f * f))
                i++
            }
        }
    }

    private val popPaint by lazy {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }

    /** Pops, floating labels and hit flash go above everything (board-space + screen flash). */
    fun drawFxOverlay(scope: DrawScope, boardCenter: Offset) = with(scope) {
        val t = nowSeconds()
        for (p in pops) {
            val k = ((t - p.start) / 0.9f).coerceIn(0f, 1f)
            val c = boardCenter + p.pos
            val ringK = (k / 0.45f).coerceAtMost(1f)
            if (ringK < 1f) {
                drawCircle(p.color.copy(alpha = (1f - ringK) * 0.9f), radius = 14f + 36f * ringK, center = c, style = Stroke(5f * (1f - ringK) + 1f))
                for (s in 0 until 6) {
                    val a = s * 1.047f + p.start
                    val d = 10f + 34f * ringK
                    drawCircle(p.color.copy(alpha = 1f - ringK), radius = 3.5f * (1f - ringK) + 1f, center = c + Offset(cos(a) * d, sin(a) * d))
                }
            }
            val y = c.y - 26f - 46f * k
            val alpha = (1f - k * k)
            drawIntoCanvas { canvas ->
                popPaint.textSize = if (p.label.length > 2) 30f else 38f
                popPaint.style = android.graphics.Paint.Style.STROKE
                popPaint.strokeWidth = 6f
                popPaint.color = android.graphics.Color.argb((alpha * 200).toInt(), 20, 10, 4)
                canvas.nativeCanvas.drawText(p.label, c.x, y, popPaint)
                popPaint.style = android.graphics.Paint.Style.FILL
                popPaint.color = android.graphics.Color.argb((alpha * 255).toInt(), (p.color.red * 255).toInt(), (p.color.green * 255).toInt(), (p.color.blue * 255).toInt())
                canvas.nativeCanvas.drawText(p.label, c.x, y, popPaint)
            }
        }
        val flash = (1f - (t - hitAt) / 0.45f).coerceIn(0f, 1f)
        if (flash > 0f) {
            drawRect(
                Brush.radialGradient(
                    0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0xFFFF1744).copy(alpha = 0.55f * flash),
                    center = Offset(size.width / 2f, size.height / 2f), radius = size.maxDimension * 0.7f
                ),
                size = size
            )
        }
    }

}
