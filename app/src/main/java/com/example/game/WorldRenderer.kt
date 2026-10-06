package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * ARMASNAKE's shared procedural "world". One renderer draws every environment as layered
 * scenery — sky, celestial light, far + mid silhouettes (with parallax), foreground props,
 * themed particles and a vignette — so the home screen, the round library, gameplay and the
 * end screens all live in the same visual universe.
 *
 * Performance notes:
 *  - Silhouette paths are built once per (theme, layer, size) and cached.
 *  - Particles are stateless: positions derive from index + time (no per-frame allocation of
 *    particle objects, no unbounded lists).
 *  - [detail] lowers particle counts for small previews (cards) and gameplay backgrounds.
 */
object WorldRenderer {

    data class WorldPalette(
        val skyTop: Color,
        val skyMid: Color,
        val skyBottom: Color,
        val far: Color,
        val mid: Color,
        val near: Color,
        val accent: Color,   // UI accent for this world (cards, focus glow, titles)
        val glow: Color,     // light source colour (sun / lava / magic / energy)
        val particle: Color
    )

    fun palette(theme: StageTheme): WorldPalette = when (theme) {
        StageTheme.GARDEN -> WorldPalette(
            Color(0xFF0F2A3A), Color(0xFF2E5E5A), Color(0xFFE7C77A),
            Color(0xFF1F4A33), Color(0xFF163824), Color(0xFF0C2215),
            Color(0xFF9CCC65), Color(0xFFFFE082), Color(0xFFFFF59D)
        )
        StageTheme.DESERT -> WorldPalette(
            Color(0xFF2A1438), Color(0xFFB4532A), Color(0xFFF2B35E),
            Color(0xFF8C4A26), Color(0xFF6B3519), Color(0xFF3E1E0D),
            Color(0xFFFFB74D), Color(0xFFFFE0B2), Color(0xFFFFE0B2)
        )
        StageTheme.JUNGLE -> WorldPalette(
            Color(0xFF03140C), Color(0xFF0B3B24), Color(0xFF3E7B4F),
            Color(0xFF0E3B23), Color(0xFF082A18), Color(0xFF04170D),
            Color(0xFF69F0AE), Color(0xFFB9F6CA), Color(0xFFCCFF90)
        )
        StageTheme.MOUNTAINS -> WorldPalette(
            Color(0xFF0B1A2E), Color(0xFF3D5A80), Color(0xFFB8C6DB),
            Color(0xFF5B6E86), Color(0xFF34465C), Color(0xFF1B2533),
            Color(0xFF90CAF9), Color(0xFFE3F2FD), Color(0xFFFFFFFF)
        )
        StageTheme.VOLCANO -> WorldPalette(
            Color(0xFF070203), Color(0xFF2A0806), Color(0xFF8C2A0A),
            Color(0xFF2B0E09), Color(0xFF1A0705), Color(0xFF0B0302),
            Color(0xFFFF6D00), Color(0xFFFF9100), Color(0xFFFFAB40)
        )
        StageTheme.ICE -> WorldPalette(
            Color(0xFF020B1F), Color(0xFF0D2F55), Color(0xFF4F8FBF),
            Color(0xFF7FB3D5), Color(0xFF3E6F95), Color(0xFF1B3D5C),
            Color(0xFF80DEEA), Color(0xFFE0F7FA), Color(0xFFFFFFFF)
        )
        StageTheme.DARK_TEMPLE -> WorldPalette(
            Color(0xFF05010A), Color(0xFF1A0A2E), Color(0xFF3E1C5E),
            Color(0xFF24113B), Color(0xFF160A26), Color(0xFF0A0412),
            Color(0xFFE040FB), Color(0xFFEA80FC), Color(0xFFD1A3FF)
        )
        StageTheme.FINAL_ARENA -> WorldPalette(
            Color(0xFF050308), Color(0xFF1C0F0A), Color(0xFF4A2408),
            Color(0xFF2A1A0C), Color(0xFF1A0F06), Color(0xFF0C0703),
            Color(0xFFFFD700), Color(0xFF00E5FF), Color(0xFFFFD54F)
        )
    }

    fun accent(theme: StageTheme): Color = palette(theme).accent

    // ------------------------------------------------------------------ path cache
    private val pathCache = HashMap<Long, Path>()

    private fun cacheKey(theme: StageTheme, layer: Int, w: Float, h: Float): Long =
        ((theme.ordinal * 32L + layer) * 20000L + w.toInt()) * 20000L + h.toInt()

    /** Deterministic pseudo-noise in [-1, 1] (no Random, stable between frames). */
    private fun noise(i: Int, seed: Float): Float =
        sin(i * 1.31f + seed) * 0.5f + sin(i * 0.53f + seed * 2.1f) * 0.35f + sin(i * 2.7f + seed * 0.7f) * 0.15f

    /** A closed silhouette ridge spanning 120% of the width (extra room for parallax). */
    private fun ridge(
        theme: StageTheme, layer: Int, w: Float, h: Float,
        baseY: Float, amp: Float, seed: Float, points: Int, smooth: Boolean
    ): Path {
        val key = cacheKey(theme, layer, w, h)
        pathCache[key]?.let { return it }
        if (pathCache.size > 96) pathCache.clear() // bounded: sizes change rarely (rotation/cards)
        val p = Path()
        val startX = -w * 0.1f
        val step = (w * 1.2f) / points
        fun yAt(i: Int) = baseY - amp * (0.5f + 0.5f * noise(i, seed))
        p.moveTo(startX, h)
        p.lineTo(startX, yAt(0))
        for (i in 1..points) {
            val x = startX + i * step
            if (smooth) {
                val px = startX + (i - 0.5f) * step
                p.quadraticTo(px, yAt(i) - amp * 0.08f, x, yAt(i))
            } else {
                p.lineTo(x, yAt(i))
            }
        }
        p.lineTo(startX + w * 1.2f, h)
        p.close()
        pathCache[key] = p
        return p
    }

    // ------------------------------------------------------------------ main entry
    /**
     * @param parallax -1..1 camera offset; layers move by different amounts for depth.
     * @param detail 0 = card preview, 1 = gameplay background, 2 = full-screen menu scene.
     */
    fun drawWorld(
        scope: DrawScope,
        width: Float,
        height: Float,
        theme: StageTheme,
        time: Float,
        parallax: Float = 0f,
        detail: Int = 2
    ) {
        if (width < 2f || height < 2f) return
        val pal = palette(theme)

        // 1. Sky
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(pal.skyTop, pal.skyMid, pal.skyBottom),
                startY = 0f,
                endY = height * 0.78f
            ),
            size = Size(width, height)
        )

        // 2. Celestial light / atmosphere per theme
        drawCelestial(scope, width, height, theme, pal, time)

        // 3. Far silhouette layer
        val farShift = parallax * width * 0.02f
        val midShift = parallax * width * 0.05f
        val nearShift = parallax * width * 0.09f
        scope.translate(left = farShift) {
            drawFar(this, width, height, theme, pal)
        }

        // 4. Atmospheric haze band between layers
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, pal.skyBottom.copy(alpha = 0.22f), Color.Transparent),
                startY = height * 0.45f,
                endY = height * 0.8f
            ),
            topLeft = Offset(0f, height * 0.45f),
            size = Size(width, height * 0.35f)
        )

        // 5. Mid silhouette layer
        scope.translate(left = midShift) {
            drawMid(this, width, height, theme, pal, time)
        }

        // 6. Foreground props
        scope.translate(left = nearShift) {
            drawNear(this, width, height, theme, pal, time)
        }

        // 7. Themed particles
        drawParticles(scope, width, height, theme, pal, time, detail)

        // 8. Vignette for focus and depth
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xB3000000)),
                center = Offset(width / 2f, height * 0.5f),
                radius = maxOf(width, height) * 0.72f
            ),
            size = Size(width, height)
        )
    }

    // ------------------------------------------------------------------ layers
    private fun drawCelestial(scope: DrawScope, w: Float, h: Float, theme: StageTheme, pal: WorldPalette, t: Float) {
        when (theme) {
            StageTheme.GARDEN, StageTheme.DESERT -> {
                // Low warm sun with a breathing halo
                val sun = Offset(w * 0.72f, h * 0.42f)
                val breathe = 1f + 0.04f * sin(t * 0.8f)
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pal.glow.copy(alpha = 0.55f), pal.glow.copy(alpha = 0f)),
                        center = sun,
                        radius = h * 0.45f * breathe
                    ),
                    radius = h * 0.45f * breathe,
                    center = sun
                )
                scope.drawCircle(color = pal.glow.copy(alpha = 0.9f), radius = h * 0.06f, center = sun)
                // Soft god-rays
                for (k in 0 until 5) {
                    val a = -2.4f + k * 0.28f + sin(t * 0.2f + k) * 0.03f
                    scope.drawLine(
                        color = pal.glow.copy(alpha = 0.06f),
                        start = sun,
                        end = Offset(sun.x + cos(a) * w, sun.y + sin(a) * w),
                        strokeWidth = h * 0.05f
                    )
                }
            }
            StageTheme.JUNGLE -> {
                // Misty sunlight breaking through a gap in the canopy
                val gap = Offset(w * 0.58f, h * 0.22f)
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66FFF59D), Color(0x2269F0AE), Color.Transparent),
                        center = gap,
                        radius = h * 0.55f
                    ),
                    radius = h * 0.55f,
                    center = gap
                )
                // Green-gold light shafts through the canopy
                for (k in 0 until 5) {
                    val x = w * (0.14f + k * 0.19f) + sin(t * 0.3f + k) * w * 0.01f
                    scope.drawLine(
                        color = (if (k % 2 == 0) Color(0xFFFFF59D) else pal.glow).copy(alpha = 0.08f + 0.04f * sin(t * 0.7f + k * 1.3f)),
                        start = Offset(x, 0f),
                        end = Offset(x - w * 0.12f, h),
                        strokeWidth = w * 0.05f
                    )
                }
            }
            StageTheme.MOUNTAINS -> {
                val sun = Offset(w * 0.25f, h * 0.3f)
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pal.glow.copy(alpha = 0.4f), Color.Transparent),
                        center = sun,
                        radius = h * 0.35f
                    ),
                    radius = h * 0.35f,
                    center = sun
                )
            }
            StageTheme.VOLCANO -> {
                // Lava light from below the horizon, flickering
                val flicker = 0.85f + 0.15f * sin(t * 5.3f) * sin(t * 1.7f)
                scope.drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, pal.glow.copy(alpha = 0.45f * flicker)),
                        startY = h * 0.35f,
                        endY = h
                    ),
                    topLeft = Offset(0f, h * 0.35f),
                    size = Size(w, h * 0.65f)
                )
            }
            StageTheme.ICE -> {
                // Aurora ribbons
                val colors = listOf(Color(0x5500E5FF), Color(0x4469F0AE), Color(0x33B388FF))
                for (k in 0 until 3) {
                    val p = Path()
                    val baseY = h * (0.16f + k * 0.07f)
                    p.moveTo(-w * 0.05f, baseY)
                    for (i in 1..6) {
                        val x = -w * 0.05f + i * w * 0.22f
                        val y = baseY + sin(t * 0.35f + i * 0.9f + k) * h * 0.05f
                        p.quadraticTo(x - w * 0.11f, y - h * 0.04f, x, y)
                    }
                    scope.drawPath(p, color = colors[k], style = Stroke(width = h * 0.045f, cap = StrokeCap.Round))
                }
                scope.drawCircle(color = Color(0xCCE0F7FA), radius = h * 0.035f, center = Offset(w * 0.82f, h * 0.18f))
            }
            StageTheme.DARK_TEMPLE -> {
                val moon = Offset(w * 0.78f, h * 0.2f)
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pal.glow.copy(alpha = 0.35f), Color.Transparent),
                        center = moon,
                        radius = h * 0.3f
                    ),
                    radius = h * 0.3f,
                    center = moon
                )
                scope.drawCircle(color = Color(0xFFE1BEE7), radius = h * 0.045f, center = moon)
                scope.drawCircle(color = pal.skyMid, radius = h * 0.04f, center = Offset(moon.x + h * 0.018f, moon.y - h * 0.008f))
            }
            StageTheme.FINAL_ARENA -> {
                // Sweeping stadium spotlights in gold and cyan
                for (k in 0 until 3) {
                    val base = Offset(w * (0.2f + k * 0.3f), h)
                    val ang = -1.57f + sin(t * 0.5f + k * 2f) * 0.35f
                    val tip = Offset(base.x + cos(ang) * h * 1.2f, base.y + sin(ang) * h * 1.2f)
                    scope.drawLine(
                        color = (if (k == 1) pal.glow else pal.accent).copy(alpha = 0.09f),
                        start = base,
                        end = tip,
                        strokeWidth = w * 0.06f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }

    private fun drawFar(scope: DrawScope, w: Float, h: Float, theme: StageTheme, pal: WorldPalette) {
        when (theme) {
            StageTheme.MOUNTAINS, StageTheme.ICE -> {
                // Jagged peaks; vertical gradient gives snowy caps for free
                val p = ridge(theme, 0, w, h, h * 0.62f, h * 0.36f, 1.7f, 9, false)
                scope.drawPath(
                    p,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFECEFF1), pal.far, pal.far),
                        startY = h * 0.26f,
                        endY = h * 0.62f
                    )
                )
            }
            StageTheme.VOLCANO -> {
                // The volcano cone with a glowing crater
                val cone = Path()
                cone.moveTo(w * 0.2f, h)
                cone.lineTo(w * 0.46f, h * 0.36f)
                cone.lineTo(w * 0.56f, h * 0.36f)
                cone.lineTo(w * 0.86f, h)
                cone.close()
                scope.drawPath(cone, color = pal.far)
                scope.drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEA00), pal.glow, Color.Transparent),
                        center = Offset(w * 0.51f, h * 0.36f),
                        radius = w * 0.09f
                    ),
                    topLeft = Offset(w * 0.42f, h * 0.31f),
                    size = Size(w * 0.18f, h * 0.1f)
                )
                // Lava rivers down the slope
                for (k in 0 until 3) {
                    val sx = w * (0.48f + k * 0.03f)
                    scope.drawLine(
                        color = pal.glow.copy(alpha = 0.55f),
                        start = Offset(sx, h * 0.38f),
                        end = Offset(sx + (k - 1) * w * 0.12f, h * 0.95f),
                        strokeWidth = 3f + k
                    )
                }
            }
            StageTheme.FINAL_ARENA -> {
                // Colosseum tiers: arched wall silhouette
                val top = h * 0.42f
                scope.drawRect(color = pal.far, topLeft = Offset(-w * 0.1f, top), size = Size(w * 1.2f, h - top))
                val archW = w / 11f
                for (i in 0 until 12) {
                    val ax = i * archW - archW * 0.3f
                    scope.drawRoundRect(
                        color = pal.skyMid,
                        topLeft = Offset(ax + archW * 0.2f, top + h * 0.06f),
                        size = Size(archW * 0.6f, h * 0.14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(archW * 0.3f, archW * 0.3f)
                    )
                }
                scope.drawLine(color = pal.accent.copy(alpha = 0.6f), start = Offset(-w * 0.1f, top), end = Offset(w * 1.1f, top), strokeWidth = 2f)
            }
            StageTheme.DARK_TEMPLE -> {
                // Ruined temple skyline: stepped ziggurat + broken columns
                val p = ridge(theme, 0, w, h, h * 0.66f, h * 0.18f, 4.2f, 7, false)
                scope.drawPath(p, color = pal.far)
                val zig = Path()
                zig.moveTo(w * 0.35f, h * 0.66f)
                zig.lineTo(w * 0.38f, h * 0.5f); zig.lineTo(w * 0.41f, h * 0.5f)
                zig.lineTo(w * 0.44f, h * 0.38f); zig.lineTo(w * 0.56f, h * 0.38f)
                zig.lineTo(w * 0.59f, h * 0.5f); zig.lineTo(w * 0.62f, h * 0.5f)
                zig.lineTo(w * 0.65f, h * 0.66f)
                zig.close()
                scope.drawPath(zig, color = pal.far)
                scope.drawCircle(color = pal.accent.copy(alpha = 0.5f), radius = h * 0.018f, center = Offset(w * 0.5f, h * 0.43f))
            }
            StageTheme.DESERT -> {
                val p = ridge(theme, 0, w, h, h * 0.66f, h * 0.16f, 2.9f, 6, true)
                scope.drawPath(p, color = pal.far)
            }
            StageTheme.JUNGLE -> {
                // Distant misty canopy layer behind the nearer canopy domes
                val back = ridge(theme, 3, w, h, h * 0.42f, h * 0.16f, 2.1f, 14, true)
                scope.drawPath(back, color = Color(0xFF1E5A3A))
                val p = ridge(theme, 0, w, h, h * 0.5f, h * 0.22f, 5.3f, 12, true)
                scope.drawPath(p, color = pal.far)
            }
            StageTheme.GARDEN -> {
                val p = ridge(theme, 0, w, h, h * 0.64f, h * 0.14f, 0.8f, 7, true)
                scope.drawPath(p, color = pal.far)
            }
        }
    }

    private fun drawMid(scope: DrawScope, w: Float, h: Float, theme: StageTheme, pal: WorldPalette, t: Float) {
        val smooth = theme != StageTheme.MOUNTAINS && theme != StageTheme.ICE && theme != StageTheme.VOLCANO
        val p = ridge(theme, 1, w, h, h * 0.8f, h * 0.14f, 3.3f + theme.ordinal, 8, smooth)
        scope.drawPath(p, color = pal.mid)
        when (theme) {
            StageTheme.DESERT -> {
                // Stone arch formation
                val ax = w * 0.18f
                scope.drawRect(color = pal.near, topLeft = Offset(ax, h * 0.6f), size = Size(w * 0.025f, h * 0.22f))
                scope.drawRect(color = pal.near, topLeft = Offset(ax + w * 0.09f, h * 0.6f), size = Size(w * 0.025f, h * 0.22f))
                scope.drawArc(
                    color = pal.near,
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(ax + w * 0.0125f, h * 0.55f),
                    size = Size(w * 0.09f, h * 0.1f),
                    style = Stroke(width = w * 0.022f)
                )
            }
            StageTheme.MOUNTAINS -> {
                // Clouds drifting below the summits
                for (k in 0 until 4) {
                    val cx = ((k * 0.29f + t * 0.008f) % 1.2f - 0.1f) * w
                    val cy = h * (0.62f + (k % 2) * 0.05f)
                    for (b in 0 until 3) {
                        scope.drawOval(
                            color = Color(0x55FFFFFF),
                            topLeft = Offset(cx + b * w * 0.035f, cy - b % 2 * h * 0.015f),
                            size = Size(w * 0.07f, h * 0.035f)
                        )
                    }
                }
            }
            StageTheme.JUNGLE -> {
                // Leaning palm silhouettes with swaying fronds
                for (k in 0 until 3) {
                    val baseX = w * (0.12f + k * 0.36f)
                    val lean = if (k % 2 == 0) 1f else -1f
                    val topX = baseX + lean * w * 0.05f
                    val topY = h * (0.36f + (k % 2) * 0.06f)
                    val trunk = Path()
                    trunk.moveTo(baseX, h * 0.86f)
                    trunk.quadraticTo(baseX + lean * w * 0.01f, h * 0.6f, topX, topY)
                    scope.drawPath(trunk, color = pal.near, style = Stroke(width = w * 0.009f, cap = StrokeCap.Round))
                    val sway = sin(t * 0.9f + k) * 0.08f
                    for (f in 0 until 6) {
                        val a = -2.9f + f * 0.55f + sway
                        val len = w * (0.07f + (f % 2) * 0.015f)
                        val ex = topX + cos(a) * len
                        val ey = topY + sin(a) * len * 0.55f + len * 0.25f
                        val frond = Path()
                        frond.moveTo(topX, topY)
                        frond.quadraticTo(topX + cos(a) * len * 0.6f, topY + sin(a) * len * 0.6f - h * 0.02f, ex, ey)
                        scope.drawPath(frond, color = pal.near, style = Stroke(width = w * 0.007f, cap = StrokeCap.Round))
                    }
                }
            }
            StageTheme.DARK_TEMPLE -> {
                // Obelisks with glowing runes
                for (k in 0 until 3) {
                    val ox = w * (0.12f + k * 0.38f)
                    val oh = h * (0.28f + (k % 2) * 0.06f)
                    val ob = Path()
                    ob.moveTo(ox, h * 0.82f)
                    ob.lineTo(ox + w * 0.012f, h * 0.82f - oh)
                    ob.lineTo(ox + w * 0.028f, h * 0.82f - oh - h * 0.03f)
                    ob.lineTo(ox + w * 0.044f, h * 0.82f - oh)
                    ob.lineTo(ox + w * 0.056f, h * 0.82f)
                    ob.close()
                    scope.drawPath(ob, color = pal.near)
                    val glow = 0.4f + 0.3f * sin(t * 1.5f + k)
                    scope.drawCircle(color = pal.accent.copy(alpha = glow), radius = w * 0.005f, center = Offset(ox + w * 0.028f, h * 0.82f - oh * 0.6f))
                }
            }
            else -> Unit
        }
    }

    private fun drawNear(scope: DrawScope, w: Float, h: Float, theme: StageTheme, pal: WorldPalette, t: Float) {
        when (theme) {
            StageTheme.GARDEN -> {
                // Grass blades swaying + flowers along the bottom edge
                for (i in 0 until 46) {
                    val x = (i / 45f) * w * 1.15f - w * 0.075f
                    val bh = h * (0.05f + ((i * 37) % 10) / 100f)
                    val sway = sin(t * 1.4f + i * 0.6f) * w * 0.006f
                    scope.drawLine(
                        color = if (i % 3 == 0) pal.accent.copy(alpha = 0.8f) else pal.near,
                        start = Offset(x, h),
                        end = Offset(x + sway, h - bh),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                val flowerColors = listOf(Color(0xFFFF80AB), Color(0xFFFFF176), Color(0xFFFFFFFF), Color(0xFFB388FF))
                for (i in 0 until 9) {
                    val x = w * ((i * 0.113f + 0.04f) % 1f)
                    val y = h * (0.94f - (i % 3) * 0.02f)
                    scope.drawCircle(color = flowerColors[i % flowerColors.size], radius = 3.5f, center = Offset(x, y))
                    scope.drawCircle(color = Color(0xFFFFD54F), radius = 1.4f, center = Offset(x, y))
                }
            }
            StageTheme.JUNGLE -> {
                // Hanging vines with leaves + big corner leaves
                for (k in 0 until 6) {
                    val vx = w * (0.05f + k * 0.18f)
                    val len = h * (0.22f + (k % 3) * 0.08f)
                    val sway = sin(t * 0.8f + k) * w * 0.01f
                    val v = Path()
                    v.moveTo(vx, 0f)
                    v.quadraticTo(vx + sway, len * 0.5f, vx + sway * 1.5f, len)
                    scope.drawPath(v, color = pal.near, style = Stroke(width = 3f))
                    for (l in 1..3) {
                        val ly = len * l / 4f
                        scope.drawOval(
                            color = pal.mid,
                            topLeft = Offset(vx + sway * (ly / len) - 7f, ly - 3f),
                            size = Size(14f, 6f)
                        )
                    }
                }
                for (side in 0..1) {
                    val cx = if (side == 0) 0f else w
                    scope.drawOval(color = pal.near, topLeft = Offset(cx - w * 0.12f, h * 0.78f), size = Size(w * 0.24f, h * 0.28f))
                }
                // Broad foreground leaves along the bottom edge, with a few bright flowers
                for (i in 0 until 9) {
                    val lx = w * (i / 8f)
                    val lw = w * 0.07f
                    val lh = h * (0.09f + (i % 3) * 0.025f)
                    val tilt = sin(t * 0.8f + i) * w * 0.004f
                    val leaf = Path()
                    leaf.moveTo(lx, h)
                    leaf.quadraticTo(lx - lw * 0.6f + tilt, h - lh * 0.6f, lx + tilt, h - lh)
                    leaf.quadraticTo(lx + lw * 0.6f + tilt, h - lh * 0.6f, lx, h)
                    leaf.close()
                    scope.drawPath(leaf, color = if (i % 2 == 0) Color(0xFF0B3A22) else pal.near)
                    if (i % 3 == 1) {
                        scope.drawCircle(color = Color(0xFFFF5252), radius = h * 0.011f, center = Offset(lx + tilt, h - lh - h * 0.006f))
                        scope.drawCircle(color = Color(0xFFFFEB3B), radius = h * 0.004f, center = Offset(lx + tilt, h - lh - h * 0.006f))
                    }
                }
            }
            StageTheme.DESERT -> {
                // Foreground dune lip
                scope.drawOval(color = pal.near, topLeft = Offset(-w * 0.2f, h * 0.88f), size = Size(w * 0.8f, h * 0.3f))
                scope.drawOval(color = pal.mid, topLeft = Offset(w * 0.45f, h * 0.9f), size = Size(w * 0.8f, h * 0.3f))
            }
            StageTheme.VOLCANO -> {
                // Cracked basalt foreground with glowing seams
                scope.drawRect(color = pal.near, topLeft = Offset(-w * 0.1f, h * 0.9f), size = Size(w * 1.2f, h * 0.1f))
                val pulse = 0.55f + 0.35f * sin(t * 2.2f)
                for (k in 0 until 7) {
                    val x0 = w * (k * 0.16f)
                    scope.drawLine(
                        color = pal.glow.copy(alpha = pulse),
                        start = Offset(x0, h * 0.92f),
                        end = Offset(x0 + w * 0.06f, h * 0.97f),
                        strokeWidth = 2f
                    )
                }
            }
            StageTheme.ICE -> {
                // Ice crystal spikes
                for (k in 0 until 8) {
                    val x = w * (k * 0.14f) - w * 0.02f
                    val sh = h * (0.08f + (k % 3) * 0.04f)
                    val sp = Path()
                    sp.moveTo(x, h)
                    sp.lineTo(x + w * 0.02f, h - sh)
                    sp.lineTo(x + w * 0.04f, h)
                    sp.close()
                    scope.drawPath(
                        sp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xCCE0F7FA), pal.near),
                            startY = h - sh,
                            endY = h
                        )
                    )
                }
            }
            StageTheme.MOUNTAINS -> {
                scope.drawPath(ridge(theme, 2, w, h, h * 0.97f, h * 0.08f, 6.1f, 10, false), color = pal.near)
            }
            StageTheme.DARK_TEMPLE -> {
                // Purple ground fog
                scope.drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, pal.accent.copy(alpha = 0.18f)),
                        startY = h * 0.75f,
                        endY = h
                    ),
                    topLeft = Offset(-w * 0.1f, h * 0.75f),
                    size = Size(w * 1.2f, h * 0.25f)
                )
            }
            StageTheme.FINAL_ARENA -> {
                // Red banners hanging from the colosseum + cyan energy line
                for (k in 0 until 5) {
                    val bx = w * (0.08f + k * 0.21f)
                    val wave = sin(t * 1.2f + k) * w * 0.004f
                    val b = Path()
                    b.moveTo(bx, h * 0.42f)
                    b.lineTo(bx + w * 0.035f, h * 0.42f)
                    b.lineTo(bx + w * 0.035f + wave, h * 0.6f)
                    b.lineTo(bx + w * 0.0175f + wave, h * 0.57f)
                    b.lineTo(bx + wave, h * 0.6f)
                    b.close()
                    scope.drawPath(b, color = Color(0xFFB71C1C))
                    scope.drawLine(color = pal.accent, start = Offset(bx, h * 0.42f), end = Offset(bx + w * 0.035f, h * 0.42f), strokeWidth = 3f)
                }
                val e = 0.4f + 0.3f * sin(t * 3f)
                scope.drawLine(color = pal.glow.copy(alpha = e), start = Offset(-w * 0.1f, h * 0.93f), end = Offset(w * 1.1f, h * 0.93f), strokeWidth = 3f)
            }
        }
    }

    private fun drawParticles(scope: DrawScope, w: Float, h: Float, theme: StageTheme, pal: WorldPalette, t: Float, detail: Int) {
        val count = when (detail) { 0 -> 10; 1 -> 22; else -> 34 }
        when (theme) {
            StageTheme.GARDEN -> {
                // Pollen motes + butterflies
                drawDrift(scope, w, h, t, count, pal.particle, rising = false, falling = false, sizeBase = 1.6f)
                val wings = listOf(Color(0xFFFFB74D), Color(0xFF80DEEA), Color(0xFFF48FB1))
                for (k in 0 until (if (detail == 0) 1 else 3)) {
                    val x = ((k * 0.31f + t * 0.025f * (1f + k * 0.25f)) % 1.1f) * w
                    val y = h * (0.55f + 0.08f * sin(t * 0.7f + k * 2f))
                    val flap = abs(sin(t * 11f + k)) * 7f + 1.5f
                    scope.drawOval(color = wings[k], topLeft = Offset(x - flap, y - 4f), size = Size(flap, 8f))
                    scope.drawOval(color = wings[k], topLeft = Offset(x, y - 4f), size = Size(flap, 8f))
                }
            }
            StageTheme.DESERT -> {
                // Horizontal dust streaks
                for (i in 0 until count) {
                    val y = h * (0.35f + ((i * 37) % 60) / 100f)
                    val x = ((i * 0.137f + t * (0.05f + (i % 4) * 0.02f)) % 1.2f - 0.1f) * w
                    scope.drawLine(
                        color = pal.particle.copy(alpha = 0.18f),
                        start = Offset(x, y),
                        end = Offset(x + w * 0.04f, y),
                        strokeWidth = 1.5f
                    )
                }
            }
            StageTheme.JUNGLE -> {
                // Fireflies blinking + low fog
                for (i in 0 until count) {
                    val x = (((i * 73) % 100) / 100f + sin(t * 0.3f + i) * 0.02f) * w
                    val y = (((i * 41) % 100) / 100f * 0.7f + 0.2f + cos(t * 0.4f + i * 1.3f) * 0.02f) * h
                    val blink = (sin(t * 3f + i * 2.1f) + 1f) / 2f
                    scope.drawCircle(color = pal.particle.copy(alpha = 0.15f + 0.75f * blink), radius = 2.2f, center = Offset(x, y))
                }
                scope.drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x33B9F6CA)),
                        startY = h * 0.7f,
                        endY = h
                    ),
                    topLeft = Offset(0f, h * 0.7f),
                    size = Size(w, h * 0.3f)
                )
            }
            StageTheme.MOUNTAINS -> {
                // Wind streaks + a few snow flecks
                for (i in 0 until count / 2) {
                    val y = h * (0.15f + ((i * 53) % 70) / 100f)
                    val x = ((i * 0.21f + t * 0.12f) % 1.3f - 0.15f) * w
                    scope.drawLine(color = Color(0x40FFFFFF), start = Offset(x, y), end = Offset(x + w * 0.06f, y - h * 0.01f), strokeWidth = 1.2f)
                }
                drawDrift(scope, w, h, t, count / 2, pal.particle, rising = false, falling = true, sizeBase = 1.2f)
            }
            StageTheme.VOLCANO -> {
                // Rising embers + smoke plumes
                drawDrift(scope, w, h, t, count, pal.particle, rising = true, falling = false, sizeBase = 1.8f)
                for (k in 0 until (if (detail == 0) 2 else 4)) {
                    val phase = ((t * 0.08f + k * 0.25f) % 1f)
                    val cx = w * (0.51f + sin(t * 0.3f + k) * 0.03f)
                    val cy = h * (0.34f - phase * 0.3f)
                    scope.drawCircle(color = Color(0xFF3E2723).copy(alpha = 0.35f * (1f - phase)), radius = h * (0.04f + phase * 0.09f), center = Offset(cx, cy))
                }
            }
            StageTheme.ICE -> {
                drawDrift(scope, w, h, t, count + 8, pal.particle, rising = false, falling = true, sizeBase = 1.6f)
            }
            StageTheme.DARK_TEMPLE -> {
                // Floating rune circles + wisps
                for (k in 0 until (if (detail == 0) 2 else 4)) {
                    val cx = w * (0.15f + k * 0.24f)
                    val cy = h * (0.35f + 0.06f * sin(t * 0.5f + k * 1.7f))
                    val r = h * 0.04f
                    val rot = t * 0.4f + k
                    scope.drawCircle(color = pal.accent.copy(alpha = 0.35f), radius = r, center = Offset(cx, cy), style = Stroke(width = 1.5f))
                    for (s in 0 until 3) {
                        val a = rot + s * 2.094f
                        scope.drawLine(
                            color = pal.accent.copy(alpha = 0.5f),
                            start = Offset(cx + cos(a) * r * 0.3f, cy + sin(a) * r * 0.3f),
                            end = Offset(cx + cos(a) * r * 0.85f, cy + sin(a) * r * 0.85f),
                            strokeWidth = 1.5f
                        )
                    }
                }
                drawDrift(scope, w, h, t, count, pal.particle, rising = true, falling = false, sizeBase = 1.3f)
            }
            StageTheme.FINAL_ARENA -> {
                // Gold sparks rising, cyan energy motes
                drawDrift(scope, w, h, t, count, pal.particle, rising = true, falling = false, sizeBase = 1.6f)
                drawDrift(scope, w, h, t + 13f, count / 3, pal.glow, rising = false, falling = false, sizeBase = 1.4f)
            }
        }
    }

    /** Stateless drifting motes (embers / snow / dust). Positions are a pure function of index + time. */
    private fun drawDrift(
        scope: DrawScope, w: Float, h: Float, t: Float, count: Int, color: Color,
        rising: Boolean, falling: Boolean, sizeBase: Float
    ) {
        for (i in 0 until count) {
            val seedX = ((i * 73) % 100) / 100f
            val seedY = ((i * 37) % 100) / 100f
            val speed = 0.02f + (i % 5) * 0.008f
            val drift = when {
                rising -> -t * speed
                falling -> t * speed
                else -> sin(t * 0.3f + i) * 0.02f
            }
            val y = ((seedY + drift) % 1f + 1f) % 1f
            val x = seedX + sin(t * 0.5f + i * 1.7f) * 0.015f
            val twinkle = 0.5f + 0.5f * sin(t * 2f + i * 2.3f)
            scope.drawCircle(
                color = color.copy(alpha = 0.25f + 0.55f * twinkle),
                radius = sizeBase + (i % 3) * 0.9f,
                center = Offset(x * w, y * h)
            )
        }
    }

    // ------------------------------------------------------------------ arena surface decor
    /**
     * Low-contrast themed details painted onto the playing surface (under all gameplay objects)
     * so each round reads as a different place without hurting readability.
     */
    fun drawArenaDecor(scope: DrawScope, center: Offset, halfW: Float, halfH: Float, theme: StageTheme, time: Float) {
        val left = center.x - halfW
        val top = center.y - halfH
        val w = halfW * 2f
        val h = halfH * 2f
        val pal = palette(theme)
        fun px(i: Int, salt: Int) = left + w * (0.06f + (((i * 61 + salt * 17) % 89) / 89f) * 0.88f)
        fun py(i: Int, salt: Int) = top + h * (0.08f + (((i * 47 + salt * 29) % 83) / 83f) * 0.84f)
        when (theme) {
            StageTheme.GARDEN -> {
                for (i in 0 until 18) {
                    val x = px(i, 1); val y = py(i, 1)
                    for (b in -1..1) {
                        scope.drawLine(color = Color(0x3381C784), start = Offset(x + b * 3f, y), end = Offset(x + b * 5f, y - 9f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                    }
                    if (i % 4 == 0) scope.drawCircle(color = Color(0x55FF80AB), radius = 2.5f, center = Offset(x + 6f, y - 4f))
                }
            }
            StageTheme.DESERT -> {
                for (i in 0 until 10) {
                    val x = px(i, 2); val y = py(i, 2)
                    scope.drawArc(color = Color(0x2E3E1E0D), startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(x - 30f, y - 8f), size = Size(60f, 16f), style = Stroke(width = 1.6f))
                }
            }
            StageTheme.JUNGLE -> {
                for (i in 0 until 14) {
                    val x = px(i, 3); val y = py(i, 3)
                    scope.drawOval(color = Color(0x2E69F0AE), topLeft = Offset(x - 8f, y - 3f), size = Size(16f, 6f))
                }
            }
            StageTheme.MOUNTAINS -> {
                for (i in 0 until 8) {
                    val x = px(i, 4); val y = py(i, 4)
                    scope.drawLine(color = Color(0x30102027), start = Offset(x, y), end = Offset(x + 18f, y + 10f), strokeWidth = 1.4f)
                    scope.drawLine(color = Color(0x30102027), start = Offset(x + 18f, y + 10f), end = Offset(x + 26f, y + 4f), strokeWidth = 1.4f)
                    if (i % 2 == 0) scope.drawOval(color = Color(0x22FFFFFF), topLeft = Offset(x - 14f, y + 12f), size = Size(28f, 9f))
                }
            }
            StageTheme.VOLCANO -> {
                val pulse = 0.18f + 0.14f * sin(time * 2.4f)
                for (i in 0 until 10) {
                    val x = px(i, 5); val y = py(i, 5)
                    scope.drawLine(color = pal.glow.copy(alpha = pulse), start = Offset(x, y), end = Offset(x + 22f, y + 7f), strokeWidth = 1.8f)
                    scope.drawLine(color = pal.glow.copy(alpha = pulse), start = Offset(x + 22f, y + 7f), end = Offset(x + 30f, y - 5f), strokeWidth = 1.4f)
                }
            }
            StageTheme.ICE -> {
                for (i in 0 until 10) {
                    val x = px(i, 6); val y = py(i, 6)
                    scope.drawLine(color = Color(0x40FFFFFF), start = Offset(x - 12f, y), end = Offset(x + 12f, y), strokeWidth = 1f)
                    scope.drawLine(color = Color(0x40FFFFFF), start = Offset(x, y - 12f), end = Offset(x, y + 12f), strokeWidth = 1f)
                    val glint = (sin(time * 2f + i * 1.9f) + 1f) / 2f
                    scope.drawCircle(color = Color.White.copy(alpha = 0.35f * glint), radius = 2f, center = Offset(x, y))
                }
            }
            StageTheme.DARK_TEMPLE -> {
                val r = minOf(halfW, halfH) * 0.72f
                val rot = time * 0.15f
                scope.drawCircle(color = pal.accent.copy(alpha = 0.10f), radius = r, center = center, style = Stroke(width = 2f))
                scope.drawCircle(color = pal.accent.copy(alpha = 0.07f), radius = r * 0.82f, center = center, style = Stroke(width = 1.5f))
                for (k in 0 until 8) {
                    val a = rot + k * 0.785f
                    scope.drawCircle(color = pal.accent.copy(alpha = 0.16f), radius = 3f, center = Offset(center.x + cos(a) * r * 0.91f, center.y + sin(a) * r * 0.91f))
                }
            }
            StageTheme.FINAL_ARENA -> {
                val r = minOf(halfW, halfH) * 0.8f
                scope.drawCircle(color = pal.accent.copy(alpha = 0.10f), radius = r, center = center, style = Stroke(width = 3f))
                val e = 0.08f + 0.06f * sin(time * 2f)
                scope.drawCircle(color = pal.glow.copy(alpha = e), radius = r * 0.92f, center = center, style = Stroke(width = 1.5f))
                for (c in 0 until 4) {
                    val cx = if (c % 2 == 0) left + 24f else left + w - 24f
                    val cy = if (c < 2) top + 24f else top + h - 24f
                    scope.drawCircle(color = Color(0xFFFF1744).copy(alpha = 0.12f + e), radius = 10f, center = Offset(cx, cy))
                }
            }
        }
    }
}
