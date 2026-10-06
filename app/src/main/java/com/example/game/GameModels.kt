package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(factor: Float) = Vector2(x * factor, y * factor)
    operator fun div(factor: Float) = if (factor != 0f) Vector2(x / factor, y / factor) else Vector2()

    fun length(): Float = sqrt(x * x + y * y)
    fun distance(other: Vector2): Float = (this - other).length()

    fun normalized(): Vector2 {
        val l = length()
        return if (l > 0.0001f) Vector2(x / l, y / l) else Vector2(0f, 0f)
    }

    companion object {
        val Zero = Vector2(0f, 0f)
        fun fromAngle(angleRad: Float, length: Float = 1f): Vector2 {
            return Vector2(cos(angleRad) * length, sin(angleRad) * length)
        }
    }
}

data class ArmadilloState(
    val pos: Vector2 = Vector2(0f, 0f),
    val vel: Vector2 = Vector2(0f, 0f),
    val radius: Float = 32f,
    val headingAngle: Float = 0f,
    val rollDistance: Float = 0f,
    val legStepPhase: Float = 0f,
    val tailWagPhase: Float = 0f,
    val isEaten: Boolean = false,
    val eatenTimer: Float = 0f,
    val eatenScale: Float = 1f,
    val invulnerableTimer: Float = 2.0f,
    val isCurled: Boolean = false,
    val isPowerActive: Boolean = false,
    val powerTimer: Float = 0f,
    val maxPowerDuration: Float = 10f,
    val powerChancesRemaining: Int = 0,
    val auraPhase: Float = 0f,
    // --- juice / power-up visual state (additive; all default to inert) ---
    val eatBounceTimer: Float = 0f,   // short squash-and-stretch pop when a fruit is eaten
    val magnetTimer: Float = 0f,      // >0 while MAGNET power pulls nearby fruit in
    val rainbowTimer: Float = 0f,     // >0 while RAINBOW score-multiplier aura is active
    val shieldCharges: Int = 0,       // SHIELD power: absorbs one snake hit
    val hitTimer: Float = 0f          // short hit-reaction flash after a blocked/absorbed hit
)

data class SnakeState(
    val headPos: Vector2 = Vector2(300f, 150f),
    val headAngle: Float = 3.14f,
    val baseSpeed: Float = 110f,
    val currentSpeed: Float = 110f,
    val bodySegments: List<Vector2> = List(16) { Vector2(300f + it * 20f, 150f) },
    val slitherPhase: Float = 0f,
    val isStriking: Boolean = false,
    val strikeTimer: Float = 0f,
    val tongueFlickTimer: Float = 0f,
    val tongueVisible: Boolean = false
)

enum class FruitType {
    REGULAR_APPLE,
    ESCAPE_GOLDEN,
    ESCAPE_STAR_POWER, // Special temporary fruit that grants the 7araka speed overdrive
    DIAMOND,           // Rare: large score bonus with a lavish collection burst
    MAGNET,            // Rare: briefly pulls nearby regular fruit toward the armadillo
    RAINBOW,           // Ultra-rare: activates a temporary score multiplier
    SHIELD,            // Rare: energy bubble that absorbs one snake hit
    FREEZE,            // Rare: ice wave that slows every snake for a few seconds
    GOLDEN             // Ultra-rare: high-value golden fruit
}

/**
 * True for the rare power-up fruits. Keeps spawn/collision logic readable and lets the
 * renderer give these far stronger visual hierarchy than ordinary apples.
 */
fun FruitType.isSpecial(): Boolean =
    this == FruitType.ESCAPE_STAR_POWER ||
    this == FruitType.DIAMOND ||
    this == FruitType.MAGNET ||
    this == FruitType.RAINBOW ||
    this == FruitType.SHIELD ||
    this == FruitType.FREEZE ||
    this == FruitType.GOLDEN

/** Number of illustrated normal-fruit varieties (apple, orange, banana, ...). Same scoring for all. */
const val FRUIT_VARIETY_COUNT = 12

/** Short human label for a power-up fruit (used by the rare-fruit notification and HUD chips). */
fun FruitType.powerLabel(): String = when (this) {
    FruitType.ESCAPE_STAR_POWER -> "SPEED"
    FruitType.DIAMOND -> "DIAMOND"
    FruitType.MAGNET -> "MAGNET"
    FruitType.RAINBOW -> "RAINBOW x2"
    FruitType.SHIELD -> "SHIELD"
    FruitType.FREEZE -> "FREEZE"
    FruitType.GOLDEN -> "GOLDEN FRUIT"
    FruitType.ESCAPE_GOLDEN -> "GOLDEN ESCAPE"
    FruitType.REGULAR_APPLE -> "FRUIT"
}

fun FruitType.powerIcon(): String = when (this) {
    FruitType.ESCAPE_STAR_POWER -> "⚡"
    FruitType.DIAMOND -> "💎"
    FruitType.MAGNET -> "🧲"
    FruitType.RAINBOW -> "🌈"
    FruitType.SHIELD -> "🛡"
    FruitType.FREEZE -> "❄"
    FruitType.GOLDEN -> "👑"
    FruitType.ESCAPE_GOLDEN -> "✨"
    FruitType.REGULAR_APPLE -> "🍎"
}

data class AppleItem(
    val id: Int,
    val pos: Vector2,
    val bobPhase: Float = 0f,
    val scale: Float = 1f,
    val isCollecting: Boolean = false,
    val fruitType: FruitType = FruitType.REGULAR_APPLE,
    // Dynamic physics for dropping & escaping
    val vel: Vector2 = Vector2.Zero,
    val dropHeight: Float = 0f, // 0 means resting on the table, >0 means falling/bouncing
    val dropVelZ: Float = 0f,
    val bounceCount: Int = 0,
    val isSpecialEventFruit: Boolean = false,
    val escapeSpeed: Float = 0f,
    val escapeAngle: Float = 0f,
    val sparkleTimer: Float = 0f,
    // --- rare power-up extras (additive; defaults keep ordinary apples unchanged) ---
    val points: Int = 1,              // score awarded on pickup (before combo / multiplier)
    val spawnPop: Float = 0f,         // 0..1 grow-in animation when a rare fruit appears
    val lifeTimer: Float = 0f,        // counts up while a timed rare fruit is on the table
    val maxLife: Float = 0f,          // >0 means this fruit expires after maxLife seconds
    val variety: Int = 0              // which illustrated normal fruit to draw (0 = apple)
)

/**
 * Short-lived "+N" / combo label that floats up from an eaten fruit and fades out.
 * Positions are in board space (same frame as apples / armadillo), mapped to screen at draw time.
 */
data class FloatingText(
    val text: String,
    val pos: Vector2,
    val vel: Vector2 = Vector2(0f, -46f),
    val color: Color = Color(0xFFFFD54F),
    val life: Float = 0f,
    val maxLife: Float = 0.9f,
    val sizeSp: Float = 16f
)

data class Particle(
    val pos: Vector2,
    val vel: Vector2,
    val color: Color,
    val size: Float,
    val alpha: Float,
    val life: Float,
    val maxLife: Float
)

data class GameStats(
    val score: Int = 0,
    val armadillosLost: Int = 0,
    val highScore: Int = 0
)

enum class StageTheme {
    GARDEN,
    DESERT,
    JUNGLE,
    MOUNTAINS,
    VOLCANO,
    ICE,
    DARK_TEMPLE,
    FINAL_ARENA
}

enum class ObstacleType {
    ROCK,
    MOVING_BOULDER,
    LAVA_HAZARD,
    TEMPLE_PILLAR
}

data class Obstacle(
    val id: Int,
    var pos: Vector2,
    val radius: Float = 26f,
    val type: ObstacleType = ObstacleType.ROCK,
    val minBound: Float = -150f,
    val maxBound: Float = 150f,
    val moveSpeed: Float = 55f,
    var moveDir: Float = 1f,
    val isHorizontal: Boolean = true
)

data class StageConfig(
    val id: Int,
    val name: String,
    val subtitle: String,
    val icon: String,
    val theme: StageTheme,
    val targetFruits: Int,
    val snakeCount: Int = 1,
    val baseSnakeSpeed: Float = 95f,
    val friction: Float = 0.94f,
    val hasEscapingFruits: Boolean = false,
    val hasMovingTraps: Boolean = false,
    val isDualArmadillo: Boolean = false,
    val maxAllowedLosses: Int = 3,
    val description: String,
    val mechanicsBadge: String
) {
    val title: String get() = name

    companion object {
        val allStages: List<StageConfig> get() = ALL_STAGES

        fun getStage(id: Int): StageConfig {
            return ALL_STAGES.find { it.id == id } ?: ALL_STAGES.first()
        }

        val ALL_STAGES = listOf(
            // =========================================================================
            // ROUNDS 1–8: SOLO ARMADILLO ROUNDS
            // =========================================================================
            StageConfig(
                id = 1,
                name = "Emerald Garden",
                subtitle = "Lush Woodlands",
                icon = "🌿",
                theme = StageTheme.GARDEN,
                targetFruits = 20,
                snakeCount = 1,
                baseSnakeSpeed = 92f,
                friction = 0.94f,
                hasEscapingFruits = false,
                isDualArmadillo = false,
                maxAllowedLosses = 5,
                description = "Long introductory woodland quest with a patient snake, stone arches, and abundant sweet fruit groves.",
                mechanicsBadge = "Grand Woodland • Slow Snake • 5 Losses Allowed • 20 Apples"
            ),
            StageConfig(
                id = 2,
                name = "Sunscorch Dunes",
                subtitle = "Scorching Dunes",
                icon = "🏜️",
                theme = StageTheme.DESERT,
                targetFruits = 28,
                snakeCount = 1,
                baseSnakeSpeed = 118f,
                friction = 0.94f,
                hasEscapingFruits = false,
                isDualArmadillo = false,
                maxAllowedLosses = 5,
                description = "Expansive desert wasteland. Navigate around sandstone boulders and avoid the relentless sidewinder.",
                mechanicsBadge = "Sprawling Dunes • Faster Snake • 5 Losses Allowed • 28 Apples"
            ),
            StageConfig(
                id = 3,
                name = "Whispering Jungle",
                subtitle = "Emerald Canopy",
                icon = "🌴",
                theme = StageTheme.JUNGLE,
                targetFruits = 35,
                snakeCount = 1,
                baseSnakeSpeed = 135f,
                friction = 0.94f,
                hasEscapingFruits = true,
                isDualArmadillo = false,
                maxAllowedLosses = 4,
                description = "Deep rainforest expanse with ancient moss boulders, dense vegetation, and agile escaping fruits.",
                mechanicsBadge = "Deep Canopy • Escaping Fruits • 4 Losses Allowed • 35 Apples"
            ),
            StageConfig(
                id = 4,
                name = "Sky Mountain",
                subtitle = "Craggy Summits",
                icon = "🏔️",
                theme = StageTheme.MOUNTAINS,
                targetFruits = 42,
                snakeCount = 1,
                baseSnakeSpeed = 150f,
                friction = 0.94f,
                hasEscapingFruits = false,
                hasMovingTraps = true,
                isDualArmadillo = false,
                maxAllowedLosses = 4,
                description = "Vast high-altitude mountain pass with rolling boulders, narrow crags, and an aggressive alpine serpent.",
                mechanicsBadge = "Rolling Boulders • Narrow Crags • 4 Losses Allowed • 42 Apples"
            ),
            StageConfig(
                id = 5,
                name = "Volcano Ridge",
                subtitle = "Molten Caldera",
                icon = "🌋",
                theme = StageTheme.VOLCANO,
                targetFruits = 50,
                snakeCount = 1,
                baseSnakeSpeed = 168f,
                friction = 0.94f,
                hasEscapingFruits = false,
                isDualArmadillo = false,
                maxAllowedLosses = 3,
                description = "Intense volcanic arena with multiple bubbling magma pits that penalize points on contact.",
                mechanicsBadge = "Magma Pits • Scorching Threat • 3 Losses Allowed • 50 Apples"
            ),
            StageConfig(
                id = 6,
                name = "Frozen Peaks",
                subtitle = "Glacial Tundra",
                icon = "❄️",
                theme = StageTheme.ICE,
                targetFruits = 55,
                snakeCount = 1,
                baseSnakeSpeed = 180f,
                friction = 0.985f,
                hasEscapingFruits = false,
                isDualArmadillo = false,
                maxAllowedLosses = 3,
                description = "Massive frozen ice sheet with frictionless rolling momentum and a blindingly fast frost serpent.",
                mechanicsBadge = "Frictionless Ice • High Momentum • 3 Losses Allowed • 55 Apples"
            ),
            StageConfig(
                id = 7,
                name = "Ancient Temple",
                subtitle = "Arcane Ruins",
                icon = "🌑",
                theme = StageTheme.DARK_TEMPLE,
                targetFruits = 65,
                snakeCount = 2,
                baseSnakeSpeed = 188f,
                friction = 0.94f,
                hasEscapingFruits = true,
                hasMovingTraps = true,
                isDualArmadillo = false,
                maxAllowedLosses = 3,
                description = "Colossal labyrinth of stone pillars, arcane traps, evasive fruits, and dual stalking serpents.",
                mechanicsBadge = "Dual Stalking Serpents • Arcane Traps • 3 Losses Allowed • 65 Apples"
            ),
            StageConfig(
                id = 8,
                name = "Final Arena",
                subtitle = "Colosseum of Champions",
                icon = "👑",
                theme = StageTheme.FINAL_ARENA,
                targetFruits = 75,
                snakeCount = 2,
                baseSnakeSpeed = 205f,
                friction = 0.94f,
                hasEscapingFruits = true,
                hasMovingTraps = true,
                isDualArmadillo = false,
                maxAllowedLosses = 3,
                description = "The supreme marathon battleground: dual apex snakes, shifting barriers, lava pools, and elusive star fruits.",
                mechanicsBadge = "Grand Colosseum • Dual Apex Snakes • 3 Losses Allowed • 75 Apples"
            ),

            // =========================================================================
            // ROUNDS 9–16: 8 DUAL-ARMADILLO SOLO MISSIONS (ONE PLAYER, TWO ARMS)
            // Left Joystick: Golden Armadillo  |  Right Joystick: Blue Armadillo
            // =========================================================================
            StageConfig(
                id = 9,
                name = "Twin Gardens",
                subtitle = "Dual Woodlands",
                icon = "🌿🌿",
                theme = StageTheme.GARDEN,
                targetFruits = 32,
                snakeCount = 1,
                baseSnakeSpeed = 96f,
                friction = 0.94f,
                hasEscapingFruits = false,
                isDualArmadillo = true,
                maxAllowedLosses = 3,
                description = "Long dual-armadillo woodland quest. Control the golden and blue armadillos independently with two joysticks to harvest fruit while evading the serpent.",
                mechanicsBadge = "Dual Armadillos • Split Escape • 3 Losses Allowed • 32 Apples"
            ),
            StageConfig(
                id = 10,
                name = "Twin Dunes",
                subtitle = "Twin Dunes",
                icon = "🏜️🏜️",
                theme = StageTheme.DESERT,
                targetFruits = 40,
                snakeCount = 1,
                baseSnakeSpeed = 124f,
                friction = 0.94f,
                hasEscapingFruits = false,
                isDualArmadillo = true,
                maxAllowedLosses = 3,
                description = "Expansive dual-armadillo desert wasteland. Split up among sandstone boulders as the sidewinder dynamically targets whichever armadillo is closest.",
                mechanicsBadge = "Dual Armadillos • Sandstone Boulders • 3 Losses Allowed • 40 Apples"
            ),
            StageConfig(
                id = 11,
                name = "Twin Canopies",
                subtitle = "Twin Canopies",
                icon = "🌴🌴",
                theme = StageTheme.JUNGLE,
                targetFruits = 50,
                snakeCount = 1,
                baseSnakeSpeed = 140f,
                friction = 0.94f,
                hasEscapingFruits = true,
                isDualArmadillo = true,
                maxAllowedLosses = 2,
                description = "Dense rainforest canopy requiring coordinated pincering maneuvers between both armadillos to catch fast escaping fruits while dodging the stalking snake.",
                mechanicsBadge = "Dual Armadillos • Escaping Fruits • 2 Losses Allowed • 50 Apples"
            ),
            StageConfig(
                id = 12,
                name = "Twin Summits",
                subtitle = "Twin Summits",
                icon = "🏔️🏔️",
                theme = StageTheme.MOUNTAINS,
                targetFruits = 58,
                snakeCount = 2,
                baseSnakeSpeed = 155f,
                friction = 0.94f,
                hasMovingTraps = true,
                isDualArmadillo = true,
                maxAllowedLosses = 2,
                description = "Dual high-altitude gauntlet with rolling boulders and dual serpents that test simultaneous two-finger coordination across narrow mountain crags.",
                mechanicsBadge = "Dual Armadillos • Rolling Boulders • 2 Losses Allowed • 58 Apples"
            ),
            StageConfig(
                id = 13,
                name = "Twin Calderas",
                subtitle = "Twin Calderas",
                icon = "🌋🌋",
                theme = StageTheme.VOLCANO,
                targetFruits = 68,
                snakeCount = 2,
                baseSnakeSpeed = 172f,
                friction = 0.94f,
                isDualArmadillo = true,
                maxAllowedLosses = 2,
                description = "Treacherous magma field where both armadillos must navigate bubbling lava hazards while dual volcanic vipers apply relentless pressure.",
                mechanicsBadge = "Dual Armadillos • Magma Hazards • 2 Losses Allowed • 68 Apples"
            ),
            StageConfig(
                id = 14,
                name = "Twin Glaciers",
                subtitle = "Twin Glaciers",
                icon = "❄️❄️",
                theme = StageTheme.ICE,
                targetFruits = 75,
                snakeCount = 2,
                baseSnakeSpeed = 185f,
                friction = 0.985f,
                isDualArmadillo = true,
                maxAllowedLosses = 2,
                description = "Massive frozen ice sheet with near-frictionless momentum. Controlling two sliding armadillos simultaneously demands exceptional precision and reflexes.",
                mechanicsBadge = "Dual Armadillos • Frictionless Ice • 2 Losses Allowed • 75 Apples"
            ),
            StageConfig(
                id = 15,
                name = "Twin Sanctum",
                subtitle = "Arcane Twin Sanctum",
                icon = "🌑🌑",
                theme = StageTheme.DARK_TEMPLE,
                targetFruits = 85,
                snakeCount = 2,
                baseSnakeSpeed = 196f,
                friction = 0.94f,
                hasEscapingFruits = true,
                hasMovingTraps = true,
                isDualArmadillo = true,
                maxAllowedLosses = 2,
                description = "Ancient subterranean labyrinth with arcane stone pillars, shifting traps, fast escaping fruits, and dual stalking shadow serpents.",
                mechanicsBadge = "Dual Armadillos • Dual Shadow Serpents • 2 Losses Allowed • 85 Apples"
            ),
            StageConfig(
                id = 16,
                name = "Colosseum of Kings",
                subtitle = "Twin Colosseum of Champions",
                icon = "👑👑",
                theme = StageTheme.FINAL_ARENA,
                targetFruits = 100,
                snakeCount = 3,
                baseSnakeSpeed = 212f,
                friction = 0.94f,
                hasEscapingFruits = true,
                hasMovingTraps = true,
                isDualArmadillo = true,
                maxAllowedLosses = 1,
                description = "The ultimate dual-armadillo marathon: 3 apex predator serpents, shifting barriers, lava traps, and fast escaping fruits. The pinnacle challenge!",
                mechanicsBadge = "Dual Armadillos • 3 Apex Serpents • 1 Loss Allowed • 100 Apples"
            )
        )
    }
}

/** 1..5 difficulty rating derived from snake speed, snake count, allowed losses and dual control. */
fun StageConfig.difficultyLevel(): Int {
    val speedPart = ((baseSnakeSpeed - 90f) / 30f).coerceAtLeast(0f)
    val snakesPart = (snakeCount - 1) * 0.8f
    val lossPart = (5 - maxAllowedLosses).coerceAtLeast(0) * 0.35f
    val dualPart = if (isDualArmadillo) 0.5f else 0f
    val raw = 1f + (speedPart + snakesPart + lossPart + dualPart) * 0.6f
    return kotlin.math.round(raw).toInt().coerceIn(1, 5)
}

/** One short cinematic line for the round intro, derived from the round's actual mechanics. */
fun StageConfig.introTagline(): String = when {
    snakeCount >= 3 -> "Three apex serpents hunt here."
    isDualArmadillo && snakeCount >= 2 -> "Two armadillos. Two serpents. Stay sharp."
    isDualArmadillo -> "Guide both armadillos at once."
    theme == StageTheme.ICE -> "The ice is slippery — momentum is your enemy."
    theme == StageTheme.VOLCANO -> "Lava pools burn your score."
    snakeCount >= 2 -> "Two serpents stalk the ruins."
    hasMovingTraps -> "Boulders roll across the arena."
    hasEscapingFruits -> "The fruit here runs from you."
    id == 1 -> "A patient snake. Learn the dance."
    else -> "The snakes are faster here."
}
