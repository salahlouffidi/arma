package com.example.game

import androidx.compose.ui.graphics.Color

enum class ScreenState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER,
    STAGE_CLEAR
}

enum class GameMode(val displayName: String, val description: String) {
    SURVIVAL("Desert Survival", "Dodge hazards, build combos, and survive escalating waves!"),
    FRUIT_FRENZY("Fruit Frenzy", "Race against the clock to collect as much desert juice as possible!"),
    TIME_ATTACK("Canyon Sprint", "High speed rolling with fast-spawning star gems and overdrive!")
}

enum class FruitType(val displayName: String, val points: Int, val overdriveCharge: Float, val color: Color) {
    PRICKLY_PEAR("Prickly Pear", 100, 8f, Color(0xFFE91E63)),
    GOLDEN_AGAVE("Golden Agave", 250, 15f, Color(0xFFFFD700)),
    DRAGON_FRUIT("Pitaya Melon", 400, 20f, Color(0xFFFF4081)),
    SAND_WATERMELON("Sand Melon", 600, 25f, Color(0xFF4CAF50)),
    STAR_GEM("Sun Gem", 1000, 35f, Color(0xFF00E5FF))
}

enum class HazardType(val displayName: String, val damage: Int, val color: Color) {
    CACTUS("Prickly Cactus", 1, Color(0xFF2E7D32)),
    RATTLE_SNAKE("Rattle Snake", 1, Color(0xFF8D6E63)),
    BOULDER("Rolling Boulder", 2, Color(0xFF5D4037)),
    TUMBLEWEED("Spiky Tumbleweed", 1, Color(0xFFFFB300)),
    SCORPION("Desert Scorpion", 1, Color(0xFFD84315))
}

enum class PowerupType(val displayName: String, val durationSec: Float, val color: Color) {
    SHIELD("Golden Shell", 8f, Color(0xFF00E5FF)),
    MAGNET("Juice Magnet", 6f, Color(0xFFFFD54F)),
    SPEED_DASH("Sonic Roll", 5f, Color(0xFFFF5722))
}

data class Armadillo(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 36f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angle: Float = 0f,
    var rollOffset: Float = 0f,
    var health: Int = 3,
    val maxHealth: Int = 3,
    var shieldTime: Float = 0f,
    var magnetTime: Float = 0f,
    var speedTime: Float = 0f,
    var overdriveMeter: Float = 0f,
    var isOverdrive: Boolean = false,
    var overdriveTime: Float = 0f,
    var invulnerableTime: Float = 0f
) {
    val isShielded: Boolean get() = shieldTime > 0f || isOverdrive || invulnerableTime > 0f
}

data class FruitItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: FruitType,
    val radius: Float = 22f,
    var bounceTick: Float = 0f
)

data class HazardItem(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val type: HazardType,
    var radius: Float = 28f,
    var rotation: Float = 0f,
    var animTick: Float = 0f
)

data class PowerupItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: PowerupType,
    val radius: Float = 24f,
    var bobTick: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float,
    val maxLife: Float,
    val radius: Float
)

data class ScorePopup(
    val id: Long,
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var life: Float = 1.0f
)

data class StageInfo(
    val stageNumber: Int,
    val name: String,
    val targetScore: Int,
    val hazardSpeedMultiplier: Float,
    val skyColorTop: Color,
    val skyColorBottom: Color,
    val duneColor: Color
)
