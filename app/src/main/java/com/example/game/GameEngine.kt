package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(
    private val soundManager: SoundManager,
    private val hapticHelper: HapticHelper
) {
    var screenWidth = 1080f
    var screenHeight = 2160f

    var screenState by mutableStateOf(ScreenState.MENU)
    var currentMode by mutableStateOf(GameMode.SURVIVAL)

    var score by mutableIntStateOf(0)
    var highScore by mutableIntStateOf(0)
    var combo by mutableIntStateOf(0)
    var comboMultiplier by mutableFloatStateOf(1.0f)
    var comboTimer by mutableFloatStateOf(0f)

    var fruitsCollected by mutableIntStateOf(0)
    var hazardsSmashed by mutableIntStateOf(0)
    var currentStage by mutableIntStateOf(1)

    var screenShake by mutableFloatStateOf(0f)

    val armadillo = Armadillo()
    val fruits = mutableStateListOf<FruitItem>()
    val hazards = mutableStateListOf<HazardItem>()
    val powerups = mutableStateListOf<PowerupItem>()
    val particles = mutableStateListOf<Particle>()
    val popups = mutableStateListOf<ScorePopup>()

    private var nextEntityId = 1L
    private var hazardSpawnTimer = 0f
    private var fruitSpawnTimer = 0f
    private var powerupSpawnTimer = 0f

    val stages = listOf(
        StageInfo(
            stageNumber = 1,
            name = "Sunny Dunes",
            targetScore = 2500,
            hazardSpeedMultiplier = 1.0f,
            skyColorTop = Color(0xFFFF8A65),
            skyColorBottom = Color(0xFFFFCC80),
            duneColor = Color(0xFFFFB74D)
        ),
        StageInfo(
            stageNumber = 2,
            name = "Canyon of Rattlers",
            targetScore = 6000,
            hazardSpeedMultiplier = 1.3f,
            skyColorTop = Color(0xFFD84315),
            skyColorBottom = Color(0xFFFF8A65),
            duneColor = Color(0xFFE65100)
        ),
        StageInfo(
            stageNumber = 3,
            name = "Obsidian Badlands",
            targetScore = 12000,
            hazardSpeedMultiplier = 1.6f,
            skyColorTop = Color(0xFF311B92),
            skyColorBottom = Color(0xFF880E4F),
            duneColor = Color(0xFF4A148C)
        ),
        StageInfo(
            stageNumber = 4,
            name = "Louffi's Oasis Sanctuary",
            targetScore = 20000,
            hazardSpeedMultiplier = 1.9f,
            skyColorTop = Color(0xFF00695C),
            skyColorBottom = Color(0xFF80CBC4),
            duneColor = Color(0xFF004D40)
        )
    )

    fun startNewGame(mode: GameMode = currentMode, stageNum: Int = 1) {
        currentMode = mode
        currentStage = stageNum
        score = 0
        combo = 0
        comboMultiplier = 1.0f
        comboTimer = 0f
        fruitsCollected = 0
        hazardsSmashed = 0

        fruits.clear()
        hazards.clear()
        powerups.clear()
        particles.clear()
        popups.clear()

        armadillo.x = screenWidth / 2f
        armadillo.y = screenHeight / 2f
        armadillo.vx = 0f
        armadillo.vy = 0f
        armadillo.health = armadillo.maxHealth
        armadillo.shieldTime = 0f
        armadillo.magnetTime = 0f
        armadillo.speedTime = 0f
        armadillo.overdriveMeter = 0f
        armadillo.isOverdrive = false
        armadillo.overdriveTime = 0f
        armadillo.invulnerableTime = 0f

        for (i in 0 until 5) {
            spawnFruit()
        }

        screenState = ScreenState.PLAYING
    }

    fun resumeGame() {
        if (screenState == ScreenState.PAUSED) {
            screenState = ScreenState.PLAYING
        }
    }

    fun pauseGame() {
        if (screenState == ScreenState.PLAYING) {
            screenState = ScreenState.PAUSED
        }
    }

    fun nextStage() {
        if (currentStage < stages.size) {
            currentStage++
            armadillo.health = armadillo.maxHealth
            fruits.clear()
            hazards.clear()
            for (i in 0 until 6) spawnFruit()
            screenState = ScreenState.PLAYING
            soundManager.playStageClear()
        } else {
            currentStage++
            screenState = ScreenState.PLAYING
        }
    }

    fun update(dt: Float, joystickX: Float, joystickY: Float) {
        if (screenState != ScreenState.PLAYING) return

        val stageConfig = stages.getOrNull((currentStage - 1).coerceAtMost(stages.size - 1))
            ?: stages.last()

        val maxSpeed = if (armadillo.isOverdrive || armadillo.speedTime > 0f) 620f else 420f
        val accel = 1800f
        val friction = 0.88f

        if (joystickX != 0f || joystickY != 0f) {
            armadillo.vx += joystickX * accel * dt
            armadillo.vy += joystickY * accel * dt

            val currentSpeed = sqrt(armadillo.vx * armadillo.vx + armadillo.vy * armadillo.vy)
            if (currentSpeed > maxSpeed) {
                armadillo.vx = (armadillo.vx / currentSpeed) * maxSpeed
                armadillo.vy = (armadillo.vy / currentSpeed) * maxSpeed
            }
            armadillo.angle = Math.toDegrees(atan2(armadillo.vy.toDouble(), armadillo.vx.toDouble())).toFloat()
        } else {
            armadillo.vx *= friction
            armadillo.vy *= friction
        }

        armadillo.x += armadillo.vx * dt
        armadillo.y += armadillo.vy * dt

        val pad = armadillo.radius + 10f
        armadillo.x = armadillo.x.coerceIn(pad, screenWidth - pad)
        armadillo.y = armadillo.y.coerceIn(pad + 120f, screenHeight - pad - 120f)

        val speed = sqrt(armadillo.vx * armadillo.vx + armadillo.vy * armadillo.vy)
        armadillo.rollOffset += speed * dt * 0.05f

        if (speed > 100f && Random.nextFloat() < 0.35f) {
            particles.add(
                Particle(
                    x = armadillo.x - (armadillo.vx / speed) * armadillo.radius,
                    y = armadillo.y - (armadillo.vy / speed) * armadillo.radius,
                    vx = (Random.nextFloat() - 0.5f) * 60f,
                    vy = (Random.nextFloat() - 0.5f) * 60f,
                    color = if (armadillo.isOverdrive) Color(0xFF00E5FF) else Color(0x88FFCC80),
                    life = 0.4f,
                    maxLife = 0.4f,
                    radius = Random.nextFloat() * 6f + 4f
                )
            )
        }

        if (armadillo.shieldTime > 0f) armadillo.shieldTime -= dt
        if (armadillo.magnetTime > 0f) armadillo.magnetTime -= dt
        if (armadillo.speedTime > 0f) armadillo.speedTime -= dt
        if (armadillo.invulnerableTime > 0f) armadillo.invulnerableTime -= dt

        if (armadillo.isOverdrive) {
            armadillo.overdriveTime -= dt
            if (armadillo.overdriveTime <= 0f) {
                armadillo.isOverdrive = false
                armadillo.overdriveMeter = 0f
            }
        }

        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) {
                combo = 0
                comboMultiplier = 1.0f
            }
        }

        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 15f).coerceAtLeast(0f)
        }

        hazardSpawnTimer += dt
        val hazardInterval = (1.8f / stageConfig.hazardSpeedMultiplier).coerceAtLeast(0.7f)
        if (hazardSpawnTimer >= hazardInterval && hazards.size < 12) {
            hazardSpawnTimer = 0f
            spawnHazard(stageConfig.hazardSpeedMultiplier)
        }

        fruitSpawnTimer += dt
        if (fruitSpawnTimer >= 1.2f && fruits.size < 8) {
            fruitSpawnTimer = 0f
            spawnFruit()
        }

        powerupSpawnTimer += dt
        if (powerupSpawnTimer >= 14f && powerups.isEmpty()) {
            powerupSpawnTimer = 0f
            spawnPowerup()
        }

        val isMagnetActive = armadillo.magnetTime > 0f || armadillo.isOverdrive
        for (i in fruits.indices.reversed()) {
            val fruit = fruits[i]
            fruit.bounceTick += dt * 5f

            if (isMagnetActive) {
                val dx = armadillo.x - fruit.x
                val dy = armadillo.y - fruit.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 400f && dist > 1f) {
                    fruit.x += (dx / dist) * 450f * dt
                    fruit.y += (dy / dist) * 450f * dt
                }
            }

            val dx = armadillo.x - fruit.x
            val dy = armadillo.y - fruit.y
            val hitDist = armadillo.radius + fruit.radius
            if (dx * dx + dy * dy < hitDist * hitDist) {
                collectFruit(fruit)
                fruits.removeAt(i)
            }
        }

        for (i in hazards.indices.reversed()) {
            val h = hazards[i]
            h.animTick += dt * 4f
            h.x += h.vx * dt
            h.y += h.vy * dt
            h.rotation += 45f * dt

            if (h.x - h.radius < 0 || h.x + h.radius > screenWidth) h.vx = -h.vx
            if (h.y - h.radius < 100f || h.y + h.radius > screenHeight - 80f) h.vy = -h.vy

            val dx = armadillo.x - h.x
            val dy = armadillo.y - h.y
            val hitDist = armadillo.radius + h.radius

            if (dx * dx + dy * dy < hitDist * hitDist) {
                if (armadillo.isOverdrive) {
                    destroyHazard(h, i)
                } else if (armadillo.isShielded) {
                    if (armadillo.shieldTime > 0f) {
                        armadillo.shieldTime = 0f
                        createExplosion(h.x, h.y, Color(0xFF00E5FF))
                        soundManager.playHitHazard()
                        hapticHelper.vibrateHit()
                        hazards.removeAt(i)
                    }
                } else {
                    takeDamage(h.type.damage)
                }
            }
        }

        for (i in powerups.indices.reversed()) {
            val p = powerups[i]
            p.bobTick += dt * 3f
            val dx = armadillo.x - p.x
            val dy = armadillo.y - p.y
            val hitDist = armadillo.radius + p.radius
            if (dx * dx + dy * dy < hitDist * hitDist) {
                applyPowerup(p.type)
                createExplosion(p.x, p.y, p.type.color)
                soundManager.playPowerup()
                hapticHelper.vibrateShort()
                powerups.removeAt(i)
            }
        }

        for (i in particles.indices.reversed()) {
            val p = particles[i]
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.life <= 0f) {
                particles.removeAt(i)
            }
        }

        for (i in popups.indices.reversed()) {
            val pop = popups[i]
            pop.life -= dt * 1.5f
            pop.y -= 50f * dt
            if (pop.life <= 0f) {
                popups.removeAt(i)
            }
        }

        if (score >= stageConfig.targetScore && currentStage <= stages.size) {
            screenState = ScreenState.STAGE_CLEAR
            soundManager.playStageClear()
            hapticHelper.vibrateOverdrive()
        }
    }

    private fun collectFruit(fruit: FruitItem) {
        fruitsCollected++
        combo++
        comboMultiplier = 1.0f + (combo * 0.15f).coerceAtMost(3.0f)
        comboTimer = 3.5f

        val pts = (fruit.type.points * comboMultiplier).toInt()
        score += pts
        if (score > highScore) highScore = score

        if (!armadillo.isOverdrive) {
            armadillo.overdriveMeter = (armadillo.overdriveMeter + fruit.type.overdriveCharge).coerceAtMost(100f)
            if (armadillo.overdriveMeter >= 100f) {
                triggerOverdrive()
            }
        }

        popups.add(
            ScorePopup(
                id = nextEntityId++,
                text = "+$pts",
                x = fruit.x,
                y = fruit.y,
                color = fruit.type.color
            )
        )

        createExplosion(fruit.x, fruit.y, fruit.type.color)
        if (fruit.type == FruitType.STAR_GEM) {
            soundManager.playCollectGem()
        } else {
            soundManager.playCollectFruit()
        }
        hapticHelper.vibrateShort()
    }

    private fun triggerOverdrive() {
        armadillo.isOverdrive = true
        armadillo.overdriveTime = 9.0f
        screenShake = 12f
        soundManager.playOverdriveStart()
        hapticHelper.vibrateOverdrive()
        popups.add(
            ScorePopup(
                id = nextEntityId++,
                text = "⚡ OVERDRIVE! ⚡",
                x = armadillo.x,
                y = armadillo.y - 60f,
                color = Color(0xFF00E5FF)
            )
        )
    }

    private fun destroyHazard(hazard: HazardItem, index: Int) {
        hazardsSmashed++
        val pts = (350 * comboMultiplier).toInt()
        score += pts
        if (score > highScore) highScore = score

        popups.add(
            ScorePopup(
                id = nextEntityId++,
                text = "+$pts SMASH!",
                x = hazard.x,
                y = hazard.y,
                color = Color(0xFFFFD700)
            )
        )
        createExplosion(hazard.x, hazard.y, hazard.type.color)
        soundManager.playHitHazard()
        hapticHelper.vibrateShort()
        hazards.removeAt(index)
    }

    private fun takeDamage(dmg: Int) {
        armadillo.health -= dmg
        armadillo.invulnerableTime = 1.8f
        combo = 0
        comboMultiplier = 1.0f
        screenShake = 18f
        soundManager.playHitHazard()
        hapticHelper.vibrateHit()

        createExplosion(armadillo.x, armadillo.y, Color(0xFFD32F2F))

        if (armadillo.health <= 0) {
            armadillo.health = 0
            screenState = ScreenState.GAME_OVER
            soundManager.playGameOver()
            hapticHelper.vibrateHit()
        }
    }

    private fun applyPowerup(type: PowerupType) {
        when (type) {
            PowerupType.SHIELD -> {
                armadillo.shieldTime = type.durationSec
                popups.add(ScorePopup(nextEntityId++, "SHIELD ON!", armadillo.x, armadillo.y - 50f, type.color))
            }
            PowerupType.MAGNET -> {
                armadillo.magnetTime = type.durationSec
                popups.add(ScorePopup(nextEntityId++, "JUICE MAGNET!", armadillo.x, armadillo.y - 50f, type.color))
            }
            PowerupType.SPEED_DASH -> {
                armadillo.speedTime = type.durationSec
                popups.add(ScorePopup(nextEntityId++, "SONIC ROLL!", armadillo.x, armadillo.y - 50f, type.color))
            }
        }
    }

    private fun createExplosion(x: Float, y: Float, color: Color) {
        for (i in 0 until 12) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd = Random.nextFloat() * 260f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    color = color,
                    life = Random.nextFloat() * 0.4f + 0.25f,
                    maxLife = 0.6f,
                    radius = Random.nextFloat() * 7f + 3f
                )
            )
        }
    }

    private fun spawnFruit() {
        val pad = 60f
        val x = Random.nextFloat() * (screenWidth - pad * 2) + pad
        val y = Random.nextFloat() * (screenHeight - 340f) + 160f

        val rand = Random.nextFloat()
        val type = when {
            rand < 0.45f -> FruitType.PRICKLY_PEAR
            rand < 0.70f -> FruitType.GOLDEN_AGAVE
            rand < 0.85f -> FruitType.DRAGON_FRUIT
            rand < 0.95f -> FruitType.SAND_WATERMELON
            else -> FruitType.STAR_GEM
        }

        fruits.add(FruitItem(id = nextEntityId++, x = x, y = y, type = type))
    }

    private fun spawnHazard(speedMult: Float) {
        val pad = 50f
        val fromLeft = Random.nextBoolean()
        val x = if (fromLeft) pad else screenWidth - pad
        val y = Random.nextFloat() * (screenHeight - 360f) + 180f

        val rand = Random.nextFloat()
        val type = when {
            rand < 0.40f -> HazardType.CACTUS
            rand < 0.65f -> HazardType.RATTLE_SNAKE
            rand < 0.85f -> HazardType.TUMBLEWEED
            else -> HazardType.BOULDER
        }

        val baseSpeed = when (type) {
            HazardType.CACTUS -> 40f
            HazardType.RATTLE_SNAKE -> 110f
            HazardType.TUMBLEWEED -> 150f
            HazardType.BOULDER -> 190f
            HazardType.SCORPION -> 90f
        } * speedMult

        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        hazards.add(
            HazardItem(
                id = nextEntityId++,
                x = x,
                y = y,
                vx = cos(angle) * baseSpeed,
                vy = sin(angle) * baseSpeed,
                type = type
            )
        )
    }

    private fun spawnPowerup() {
        val pad = 80f
        val x = Random.nextFloat() * (screenWidth - pad * 2) + pad
        val y = Random.nextFloat() * (screenHeight - 360f) + 180f
        val type = PowerupType.values().random()
        powerups.add(PowerupItem(id = nextEntityId++, x = x, y = y, type = type))
    }
}
