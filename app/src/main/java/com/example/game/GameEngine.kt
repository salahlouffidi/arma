package com.example.game

import androidx.compose.ui.graphics.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(
    var onScoreChanged: ((Int) -> Unit)? = null,
    var onArmadilloEaten: (() -> Unit)? = null,
    var onAppleCollected: (() -> Unit)? = null,
    var onWallBump: (() -> Unit)? = null,
    var onSpecialEventTriggered: (() -> Unit)? = null,
    var onPowerActivated: (() -> Unit)? = null,
    var onPowerProtectedHit: (() -> Unit)? = null,
    var onStageCompleted: ((StageConfig, Int, Int, Int, Float) -> Unit)? = null,
    var onLavaHit: (() -> Unit)? = null,
    var onArmadilloSpawned: (() -> Unit)? = null
) {
    // Board playable dimensions (centered at 0,0)
    var boardHalfWidth: Float = 440f
    var boardHalfHeight: Float = 260f

    // 8-Stage Progression State
    var currentStage: StageConfig = StageConfig.ALL_STAGES[0]
        private set
    var stageFruitsCollected: Int = 0
        private set
    var isStageCompleted: Boolean = false
        private set

    var armadillo = ArmadilloState(
        pos = Vector2(0f, 0f),
        vel = Vector2(0f, 0f),
        radius = 28f
    )
        private set

    // Second Blue Armadillo (Active exclusively in Stages 9–16)
    var armadillo2: ArmadilloState? = null
        private set

    val isDualArmadillo: Boolean
        get() = currentStage.isDualArmadillo || armadillo2 != null

    // Primary snake state (guarantees backward compatibility for reflection & single snake queries)
    var snake: SnakeState = SnakeState(
        headPos = Vector2(280f, 160f),
        headAngle = 3.14f,
        baseSpeed = 95f,
        currentSpeed = 95f,
        bodySegments = List(16) { Vector2(280f + it * 18f, 160f) }
    )

    // Autonomous snakes collection (supports 1 snake in stages 1-6, 2 snakes in stages 7-8 & 12-15, 3 snakes in stage 16)
    val snakes = CopyOnWriteArrayList<SnakeState>()

    // Interactive obstacles (rocks, moving boulders, lava pools, temple pillars)
    val obstacles = CopyOnWriteArrayList<Obstacle>()

    val apples = CopyOnWriteArrayList<AppleItem>()
    val particles = CopyOnWriteArrayList<Particle>()

    var stats = GameStats()
        private set

    var currentMatchConfig: MatchConfig = MatchConfig.solo()
        private set

    var snakeCatches: Int = 0
        private set

    // Fruits collected since last special event trigger (triggers every 4 collected fruits)
    var fruitsCollectedSinceEvent = 0
        private set

    // Message popup status for UI
    var bannerMessage: String? = null
        private set
    var bannerTimer: Float = 0f
        private set

    // Authoritative game simulation pause state
    var isPaused: Boolean = false

    // Physics parameters
    private val baseTiltAccelFactor = 1600f
    private val baseMaxVelocity = 480f

    // 7araka Special Power parameters
    private val powerTiltAccelFactor = 3200f // 2x responsive acceleration
    private val powerFriction = 0.965f // smoother, stronger gliding roll
    private val powerMaxVelocity = 750f // significantly faster top speed

    private var timeAliveSeconds = 0f
    private var nextAppleId = 10

    init {
        loadStage(currentStage)
    }

    fun setBoardDimensions(halfWidth: Float, halfHeight: Float) {
        boardHalfWidth = halfWidth.coerceAtLeast(180f)
        boardHalfHeight = halfHeight.coerceAtLeast(120f)
        setupObstaclesForStage(currentStage)
    }

    fun loadStage(stage: StageConfig, config: MatchConfig = currentMatchConfig) {
        currentStage = stage
        currentMatchConfig = config
        stageFruitsCollected = 0
        snakeCatches = 0
        isStageCompleted = false
        timeAliveSeconds = 0f
        fruitsCollectedSinceEvent = 0
        bannerMessage = null
        bannerTimer = 0f

        val isDual = if (config.isSolo) {
            stage.isDualArmadillo
        } else {
            config.getSlot(CharacterId.ARMADILLO_2).roleType != RoleType.DISABLED
        }

        val a1Active = if (config.isSolo) true else config.getSlot(CharacterId.ARMADILLO_1).roleType != RoleType.DISABLED
        val a1Start = if (isDual && a1Active) Vector2(-boardHalfWidth * 0.38f, 0f) else Vector2(0f, 0f)

        // Reset Original Armadillo 1
        armadillo = if (a1Active) {
            ArmadilloState(
                pos = a1Start,
                vel = Vector2(0f, 0f),
                radius = 28f,
                headingAngle = 0f,
                invulnerableTimer = 2.0f,
                isPowerActive = false,
                powerTimer = 0f,
                powerChancesRemaining = 0
            )
        } else {
            ArmadilloState(
                pos = Vector2(-9999f, -9999f),
                radius = 0f,
                isEaten = true,
                eatenScale = 0f
            )
        }

        // Initialize or Clear Second Blue Armadillo 2
        armadillo2 = if (isDual) {
            ArmadilloState(
                pos = Vector2(boardHalfWidth * 0.38f, 0f),
                vel = Vector2(0f, 0f),
                radius = 28f,
                headingAngle = Math.PI.toFloat(),
                invulnerableTimer = 2.0f,
                isPowerActive = false,
                powerTimer = 0f,
                powerChancesRemaining = 0
            )
        } else {
            null
        }

        // Initialize Snakes based on stage configuration or multiplayer config
        snakes.clear()
        val s1Head = Vector2(boardHalfWidth * 0.65f, boardHalfHeight * 0.65f)

        if (config.isSolo) {
            val s1 = SnakeState(
                headPos = s1Head,
                headAngle = 3.14f,
                baseSpeed = stage.baseSnakeSpeed,
                currentSpeed = stage.baseSnakeSpeed,
                bodySegments = List(16) { Vector2(s1Head.x + it * 18f, s1Head.y) }
            )
            snakes.add(s1)
            snake = s1

            if (stage.snakeCount >= 2) {
                val s2Head = Vector2(-boardHalfWidth * 0.65f, -boardHalfHeight * 0.65f)
                val s2 = SnakeState(
                    headPos = s2Head,
                    headAngle = 0f,
                    baseSpeed = stage.baseSnakeSpeed * 0.92f,
                    currentSpeed = stage.baseSnakeSpeed * 0.92f,
                    bodySegments = List(16) { Vector2(s2Head.x - it * 18f, s2Head.y) }
                )
                snakes.add(s2)
            }

            if (stage.snakeCount >= 3) {
                val s3Head = Vector2(-boardHalfWidth * 0.65f, boardHalfHeight * 0.65f)
                val s3 = SnakeState(
                    headPos = s3Head,
                    headAngle = 1.57f,
                    baseSpeed = stage.baseSnakeSpeed * 0.96f,
                    currentSpeed = stage.baseSnakeSpeed * 0.96f,
                    bodySegments = List(16) { Vector2(s3Head.x - it * 18f, s3Head.y) }
                )
                snakes.add(s3)
            }
        } else {
            val s1Active = config.getSlot(CharacterId.SNAKE_1).roleType != RoleType.DISABLED
            val s2Active = config.getSlot(CharacterId.SNAKE_2).roleType != RoleType.DISABLED

            if (s1Active) {
                val s1 = SnakeState(
                    headPos = s1Head,
                    headAngle = 3.14f,
                    baseSpeed = stage.baseSnakeSpeed,
                    currentSpeed = stage.baseSnakeSpeed,
                    bodySegments = List(16) { Vector2(s1Head.x + it * 18f, s1Head.y) }
                )
                snakes.add(s1)
            }

            if (s2Active) {
                val s2Head = Vector2(-boardHalfWidth * 0.65f, -boardHalfHeight * 0.65f)
                val s2 = SnakeState(
                    headPos = s2Head,
                    headAngle = 0f,
                    baseSpeed = stage.baseSnakeSpeed * 0.92f,
                    currentSpeed = stage.baseSnakeSpeed * 0.92f,
                    bodySegments = List(16) { Vector2(s2Head.x - it * 18f, s2Head.y) }
                )
                snakes.add(s2)
            }

            if (snakes.isEmpty()) {
                val s1 = SnakeState(
                    headPos = s1Head,
                    headAngle = 3.14f,
                    baseSpeed = stage.baseSnakeSpeed,
                    currentSpeed = stage.baseSnakeSpeed,
                    bodySegments = List(16) { Vector2(s1Head.x + it * 18f, s1Head.y) }
                )
                snakes.add(s1)
            }
            snake = snakes[0]
        }

        // Initialize Obstacles
        setupObstaclesForStage(stage)

        // Reset Fruits
        apples.clear()
        spawnStandardApples(3)
        particles.clear()

        if (config.isSolo) {
            if (isDual) {
                showBanner("🦔🔵 DUAL CO-OP: ${stage.name.uppercase()} — GOAL: ${stage.targetFruits} 🍎")
            } else {
                showBanner("${stage.icon} STAGE ${stage.id}: ${stage.name.uppercase()} — GOAL: ${stage.targetFruits} 🍎")
            }
        } else {
            showBanner("${config.preset.icon} ${config.preset.title}: ${stage.name.uppercase()} — GOAL: ${stage.targetFruits} 🍎")
        }
    }

    fun resetGame() {
        loadStage(currentStage, currentMatchConfig)
    }

    private fun setupObstaclesForStage(stage: StageConfig) {
        obstacles.clear()
        val hw = boardHalfWidth
        val hh = boardHalfHeight

        when (stage.theme) {
            StageTheme.GARDEN -> {
                // Easy: Gentle garden stone obstacles arranged organically across the wider grove
                obstacles.add(Obstacle(id = 1, pos = Vector2(-hw * 0.42f, hh * 0.35f), radius = 22f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 2, pos = Vector2(hw * 0.42f, -hh * 0.35f), radius = 22f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 3, pos = Vector2(0f, 0f), radius = 20f, type = ObstacleType.ROCK))
            }
            StageTheme.DESERT -> {
                // Desert: 4 Sandstone rocks scattered throughout the dunes
                obstacles.add(Obstacle(id = 1, pos = Vector2(-hw * 0.45f, -hh * 0.35f), radius = 26f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 2, pos = Vector2(hw * 0.45f, hh * 0.35f), radius = 26f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 3, pos = Vector2(-hw * 0.20f, hh * 0.40f), radius = 24f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 4, pos = Vector2(hw * 0.20f, -hh * 0.40f), radius = 24f, type = ObstacleType.ROCK))
            }
            StageTheme.ICE -> {
                // Ice World: 2 Glacial Ice Pillars + ultra-slippery surface
                obstacles.add(Obstacle(id = 1, pos = Vector2(-hw * 0.35f, 0f), radius = 24f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 2, pos = Vector2(hw * 0.35f, 0f), radius = 24f, type = ObstacleType.ROCK))
            }
            StageTheme.JUNGLE -> {
                // 4 Ancient Jungle Moss Rocks and vegetation mounds
                obstacles.add(Obstacle(id = 1, pos = Vector2(-hw * 0.45f, -hh * 0.38f), radius = 26f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 2, pos = Vector2(hw * 0.45f, hh * 0.38f), radius = 26f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 3, pos = Vector2(0f, -hh * 0.40f), radius = 24f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 4, pos = Vector2(0f, hh * 0.40f), radius = 24f, type = ObstacleType.ROCK))
            }
            StageTheme.MOUNTAINS -> {
                // 2 Moving Mountain Boulders sliding in opposite directions + 2 Crag Rocks
                obstacles.add(
                    Obstacle(
                        id = 1,
                        pos = Vector2(0f, -hh * 0.30f),
                        radius = 28f,
                        type = ObstacleType.MOVING_BOULDER,
                        minBound = -hw * 0.60f,
                        maxBound = hw * 0.60f,
                        moveSpeed = 70f,
                        moveDir = 1f,
                        isHorizontal = true
                    )
                )
                obstacles.add(
                    Obstacle(
                        id = 2,
                        pos = Vector2(0f, hh * 0.30f),
                        radius = 28f,
                        type = ObstacleType.MOVING_BOULDER,
                        minBound = -hw * 0.60f,
                        maxBound = hw * 0.60f,
                        moveSpeed = 70f,
                        moveDir = -1f,
                        isHorizontal = true
                    )
                )
                obstacles.add(Obstacle(id = 3, pos = Vector2(-hw * 0.50f, 0f), radius = 24f, type = ObstacleType.ROCK))
                obstacles.add(Obstacle(id = 4, pos = Vector2(hw * 0.50f, 0f), radius = 24f, type = ObstacleType.ROCK))
            }
            StageTheme.VOLCANO -> {
                // 4 Glowing Lava Pools that demand careful routing
                obstacles.add(Obstacle(id = 1, pos = Vector2(-hw * 0.40f, hh * 0.32f), radius = 34f, type = ObstacleType.LAVA_HAZARD))
                obstacles.add(Obstacle(id = 2, pos = Vector2(hw * 0.40f, -hh * 0.32f), radius = 34f, type = ObstacleType.LAVA_HAZARD))
                obstacles.add(Obstacle(id = 3, pos = Vector2(-hw * 0.40f, -hh * 0.32f), radius = 30f, type = ObstacleType.LAVA_HAZARD))
                obstacles.add(Obstacle(id = 4, pos = Vector2(hw * 0.40f, hh * 0.32f), radius = 30f, type = ObstacleType.LAVA_HAZARD))
            }
            StageTheme.DARK_TEMPLE -> {
                // 4 Ancient Temple Obelisks / Pillars + 1 patrolling guardian boulder
                obstacles.add(Obstacle(id = 1, pos = Vector2(-hw * 0.40f, -hh * 0.35f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(Obstacle(id = 2, pos = Vector2(hw * 0.40f, hh * 0.35f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(Obstacle(id = 3, pos = Vector2(-hw * 0.40f, hh * 0.35f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(Obstacle(id = 4, pos = Vector2(hw * 0.40f, -hh * 0.35f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(
                    Obstacle(
                        id = 5,
                        pos = Vector2(0f, 0f),
                        radius = 26f,
                        type = ObstacleType.MOVING_BOULDER,
                        minBound = -hh * 0.45f,
                        maxBound = hh * 0.45f,
                        moveSpeed = 65f,
                        moveDir = 1f,
                        isHorizontal = false
                    )
                )
            }
            StageTheme.FINAL_ARENA -> {
                // Grand Colosseum: 4 Golden Pillars, 2 Moving Barriers, 2 Lava Hazards!
                obstacles.add(Obstacle(id = 1, pos = Vector2(-hw * 0.50f, -hh * 0.30f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(Obstacle(id = 2, pos = Vector2(hw * 0.50f, -hh * 0.30f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(Obstacle(id = 3, pos = Vector2(-hw * 0.50f, hh * 0.30f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(Obstacle(id = 4, pos = Vector2(hw * 0.50f, hh * 0.30f), radius = 28f, type = ObstacleType.TEMPLE_PILLAR))
                obstacles.add(
                    Obstacle(
                        id = 5,
                        pos = Vector2(0f, -hh * 0.40f),
                        radius = 26f,
                        type = ObstacleType.MOVING_BOULDER,
                        minBound = -hw * 0.48f,
                        maxBound = hw * 0.48f,
                        moveSpeed = 85f,
                        moveDir = 1f,
                        isHorizontal = true
                    )
                )
                obstacles.add(
                    Obstacle(
                        id = 6,
                        pos = Vector2(0f, hh * 0.40f),
                        radius = 26f,
                        type = ObstacleType.MOVING_BOULDER,
                        minBound = -hw * 0.48f,
                        maxBound = hw * 0.48f,
                        moveSpeed = 85f,
                        moveDir = -1f,
                        isHorizontal = true
                    )
                )
                obstacles.add(Obstacle(id = 7, pos = Vector2(-hw * 0.20f, 0f), radius = 30f, type = ObstacleType.LAVA_HAZARD))
                obstacles.add(Obstacle(id = 8, pos = Vector2(hw * 0.20f, 0f), radius = 30f, type = ObstacleType.LAVA_HAZARD))
            }
        }
    }

    fun update(
        dt: Float,
        tiltX: Float,
        tiltY: Float,
        tiltX2: Float = 0f,
        tiltY2: Float = 0f,
        snake1InputX: Float = 0f,
        snake1InputY: Float = 0f,
        snake2InputX: Float = 0f,
        snake2InputY: Float = 0f
    ) {
        if (isPaused) return
        val clampedDt = dt.coerceIn(0.001f, 0.05f)
        timeAliveSeconds += clampedDt

        if (bannerTimer > 0f) {
            bannerTimer -= clampedDt
            if (bannerTimer <= 0f) {
                bannerMessage = null
            }
        }

        // Armadillo 1 input
        val a1Active = if (currentMatchConfig.isSolo) true else currentMatchConfig.getSlot(CharacterId.ARMADILLO_1).roleType != RoleType.DISABLED
        val a1IsHuman = currentMatchConfig.isSolo || currentMatchConfig.getSlot(CharacterId.ARMADILLO_1).roleType == RoleType.HUMAN

        if (a1Active) {
            val (effA1X, effA1Y) = if (a1IsHuman) {
                tiltX to tiltY
            } else {
                val aiMove = computeArmadilloAiInput(armadillo)
                aiMove.x to aiMove.y
            }
            updateArmadillo(clampedDt, effA1X, effA1Y)
        }

        if (armadillo2 != null) {
            val a2Active = if (currentMatchConfig.isSolo) true else currentMatchConfig.getSlot(CharacterId.ARMADILLO_2).roleType != RoleType.DISABLED
            val a2IsHuman = if (currentMatchConfig.isSolo) true else currentMatchConfig.getSlot(CharacterId.ARMADILLO_2).roleType == RoleType.HUMAN

            if (a2Active) {
                val a2 = armadillo2!!
                val (effA2X, effA2Y) = if (a2IsHuman) {
                    tiltX2 to tiltY2
                } else {
                    val aiMove = computeArmadilloAiInput(a2)
                    aiMove.x to aiMove.y
                }
                updateArmadillo2(clampedDt, effA2X, effA2Y)
            }

            // Soft physical separation between armadillos
            val a2 = armadillo2
            if (a2 != null && !armadillo.isEaten && !a2.isEaten && a1Active) {
                val dist = armadillo.pos.distance(a2.pos)
                val minDist = armadillo.radius + a2.radius
                if (dist < minDist && dist > 0.001f) {
                    val normal = (armadillo.pos - a2.pos).normalized()
                    val overlap = (minDist - dist) * 0.5f
                    armadillo = armadillo.copy(pos = armadillo.pos + normal * overlap)
                    armadillo2 = a2.copy(pos = a2.pos - normal * overlap)
                }
            }
        }

        updateObstacles(clampedDt)
        updateSnakes(clampedDt, snake1InputX, snake1InputY, snake2InputX, snake2InputY)
        updateApples(clampedDt)
        updateParticles(clampedDt)
        checkCollisions()
    }

    private fun computeArmadilloAiInput(arm: ArmadilloState): Vector2 {
        if (arm.isEaten) return Vector2.Zero
        var evadeVector = Vector2.Zero
        var minThreatDist = Float.MAX_VALUE
        for (s in snakes) {
            val d = arm.pos.distance(s.headPos)
            if (d < minThreatDist) {
                minThreatDist = d
                if (d < 160f && d > 0.001f) {
                    evadeVector = (arm.pos - s.headPos).normalized()
                }
            }
        }
        if (minThreatDist < 125f) {
            return evadeVector
        }
        val nearestApple = apples.minByOrNull { it.pos.distance(arm.pos) }
        if (nearestApple != null) {
            val toApple = (nearestApple.pos - arm.pos).normalized()
            return if (minThreatDist < 180f) {
                (toApple * 0.45f + evadeVector * 0.55f).normalized()
            } else {
                toApple
            }
        }
        return Vector2.Zero
    }

    private fun updateObstacles(dt: Float) {
        for (obs in obstacles) {
            if (obs.type == ObstacleType.MOVING_BOULDER) {
                if (obs.isHorizontal) {
                    var newX = obs.pos.x + obs.moveSpeed * obs.moveDir * dt
                    if (newX > obs.maxBound) {
                        newX = obs.maxBound
                        obs.moveDir = -1f
                    } else if (newX < obs.minBound) {
                        newX = obs.minBound
                        obs.moveDir = 1f
                    }
                    obs.pos = Vector2(newX, obs.pos.y)
                } else {
                    var newY = obs.pos.y + obs.moveSpeed * obs.moveDir * dt
                    if (newY > obs.maxBound) {
                        newY = obs.maxBound
                        obs.moveDir = -1f
                    } else if (newY < obs.minBound) {
                        newY = obs.minBound
                        obs.moveDir = 1f
                    }
                    obs.pos = Vector2(obs.pos.x, newY)
                }
            }
        }
    }

    private fun updateArmadillo(dt: Float, tiltX: Float, tiltY: Float) {
        if (armadillo.isEaten) {
            val timer = armadillo.eatenTimer + dt
            if (timer >= 1.0f) {
                spawnNewArmadillo()
            } else {
                armadillo = armadillo.copy(
                    eatenTimer = timer,
                    eatenScale = (1f - timer).coerceAtLeast(0f),
                    rollDistance = armadillo.rollDistance + 8f * dt
                )
            }
            return
        }

        // Power duration timer countdown
        var powerActive = armadillo.isPowerActive
        var powerRemaining = armadillo.powerTimer
        var chances = armadillo.powerChancesRemaining

        if (powerActive) {
            powerRemaining -= dt
            if (powerRemaining <= 0f) {
                powerActive = false
                powerRemaining = 0f
                chances = 0
                showBanner("POWER EXPIRED")
            }
        }

        // Select physics parameters based on power active (7araka mode) or stage environment friction
        val accelFactor = if (powerActive) powerTiltAccelFactor else baseTiltAccelFactor
        val curFriction = if (powerActive) powerFriction else currentStage.friction
        val maxVel = if (powerActive) powerMaxVelocity else baseMaxVelocity

        val accelX = tiltX * accelFactor
        val accelY = tiltY * accelFactor

        var newVelX = (armadillo.vel.x + accelX * dt) * curFriction
        var newVelY = (armadillo.vel.y + accelY * dt) * curFriction

        // Clamp max velocity
        val speed = sqrt(newVelX * newVelX + newVelY * newVelY)
        if (speed > maxVel) {
            val ratio = maxVel / speed
            newVelX *= ratio
            newVelY *= ratio
        }

        // Update position
        var newPosX = armadillo.pos.x + newVelX * dt
        var newPosY = armadillo.pos.y + newVelY * dt

        // Board boundary collision with soft elastic bounce
        val boundX = boardHalfWidth - armadillo.radius - 12f
        val boundY = boardHalfHeight - armadillo.radius - 12f
        var hitWall = false

        if (newPosX < -boundX) {
            newPosX = -boundX
            newVelX = -newVelX * 0.45f
            hitWall = true
        } else if (newPosX > boundX) {
            newPosX = boundX
            newVelX = -newVelX * 0.45f
            hitWall = true
        }

        if (newPosY < -boundY) {
            newPosY = -boundY
            newVelY = -newVelY * 0.45f
            hitWall = true
        } else if (newPosY > boundY) {
            newPosY = boundY
            newVelY = -newVelY * 0.45f
            hitWall = true
        }

        // Obstacle Collisions
        for (obs in obstacles) {
            if (obs.type == ObstacleType.LAVA_HAZARD) {
                // Volcanic Lava Danger Zone: Touching lava causes sizzling burn bounce and docks 1 point
                if (armadillo.invulnerableTimer <= 0f && !armadillo.isEaten) {
                    val dist = Vector2(newPosX, newPosY).distance(obs.pos)
                    if (dist < armadillo.radius + obs.radius) {
                        val bounceDir = (Vector2(newPosX, newPosY) - obs.pos).normalized()
                        newVelX = bounceDir.x * 320f
                        newVelY = bounceDir.y * 320f
                        newPosX += bounceDir.x * 15f
                        newPosY += bounceDir.y * 15f

                        val newScore = maxOf(0, stats.score - 1)
                        stats = stats.copy(score = newScore)
                        onScoreChanged?.invoke(newScore)
                        onLavaHit?.invoke()
                        spawnLavaParticles(Vector2(newPosX, newPosY))
                        showBanner("🔥 LAVA HAZARD! −1")
                        armadillo = armadillo.copy(invulnerableTimer = 1.3f)
                    }
                }
            } else {
                // Solid Physical Obstacles: ROCK, MOVING_BOULDER, TEMPLE_PILLAR
                val dist = Vector2(newPosX, newPosY).distance(obs.pos)
                val minDist = armadillo.radius + obs.radius
                if (dist < minDist && dist > 0.001f) {
                    val normal = (Vector2(newPosX, newPosY) - obs.pos).normalized()
                    val overlap = minDist - dist
                    newPosX += normal.x * overlap
                    newPosY += normal.y * overlap

                    val dot = newVelX * normal.x + newVelY * normal.y
                    if (dot < 0f) {
                        newVelX -= normal.x * dot * 1.5f
                        newVelY -= normal.y * dot * 1.5f
                    }
                    hitWall = true
                }
            }
        }

        if (hitWall && speed > 60f) {
            onWallBump?.invoke()
            spawnImpactParticles(Vector2(newPosX, newPosY))
        }

        // Heading angle smoothly follows velocity if moving
        val currentSpeed = sqrt(newVelX * newVelX + newVelY * newVelY)
        var newHeading = armadillo.headingAngle
        if (currentSpeed > 8f) {
            val targetAngle = atan2(newVelY, newVelX)
            val diff = normalizeAngle(targetAngle - armadillo.headingAngle)
            val turnSpeed = if (powerActive) 22f else 12f
            newHeading = armadillo.headingAngle + diff * (turnSpeed * dt).coerceAtMost(1f)
        }

        val rollFactor = if (powerActive) 0.16f else 0.08f
        val legFactor = if (powerActive) 0.30f else 0.15f
        val newRollDistance = armadillo.rollDistance + currentSpeed * dt * rollFactor
        val newLegPhase = armadillo.legStepPhase + currentSpeed * dt * legFactor
        val newTailWag = armadillo.tailWagPhase + (if (powerActive) 14f else 6f) * dt
        val newAuraPhase = armadillo.auraPhase + 8f * dt

        val invulnerable = (armadillo.invulnerableTimer - dt).coerceAtLeast(0f)

        // Spawn golden speed trail / sparks while 7araka power is active
        if (powerActive && currentSpeed > 30f) {
            spawnPowerTrail(Vector2(newPosX, newPosY), armadillo.headingAngle)
        }

        armadillo = armadillo.copy(
            pos = Vector2(newPosX, newPosY),
            vel = Vector2(newVelX, newVelY),
            headingAngle = newHeading,
            rollDistance = newRollDistance,
            legStepPhase = newLegPhase,
            tailWagPhase = newTailWag,
            invulnerableTimer = invulnerable,
            isPowerActive = powerActive,
            powerTimer = powerRemaining,
            powerChancesRemaining = chances,
            auraPhase = newAuraPhase
        )
    }

    private fun updateArmadillo2(dt: Float, tiltX: Float, tiltY: Float) {
        val a2 = armadillo2 ?: return
        if (a2.isEaten) {
            val timer = a2.eatenTimer + dt
            if (timer >= 1.0f) {
                spawnNewArmadillo2()
            } else {
                armadillo2 = a2.copy(
                    eatenTimer = timer,
                    eatenScale = (1f - timer).coerceAtLeast(0f),
                    rollDistance = a2.rollDistance + 8f * dt
                )
            }
            return
        }

        // Power duration timer countdown
        var powerActive = a2.isPowerActive
        var powerRemaining = a2.powerTimer
        var chances = a2.powerChancesRemaining

        if (powerActive) {
            powerRemaining -= dt
            if (powerRemaining <= 0f) {
                powerActive = false
                powerRemaining = 0f
                chances = 0
                showBanner("BLUE ARMADILLO POWER EXPIRED")
            }
        }

        // Physics parameters based on power active or stage friction
        val accelFactor = if (powerActive) powerTiltAccelFactor else baseTiltAccelFactor
        val curFriction = if (powerActive) powerFriction else currentStage.friction
        val maxVel = if (powerActive) powerMaxVelocity else baseMaxVelocity

        val accelX = tiltX * accelFactor
        val accelY = tiltY * accelFactor

        var newVelX = (a2.vel.x + accelX * dt) * curFriction
        var newVelY = (a2.vel.y + accelY * dt) * curFriction

        // Clamp max velocity
        val speed = sqrt(newVelX * newVelX + newVelY * newVelY)
        if (speed > maxVel) {
            val ratio = maxVel / speed
            newVelX *= ratio
            newVelY *= ratio
        }

        // Update position
        var newPosX = a2.pos.x + newVelX * dt
        var newPosY = a2.pos.y + newVelY * dt

        // Boundary collision
        val boundX = boardHalfWidth - a2.radius - 12f
        val boundY = boardHalfHeight - a2.radius - 12f
        var hitWall = false

        if (newPosX < -boundX) {
            newPosX = -boundX
            newVelX = -newVelX * 0.45f
            hitWall = true
        } else if (newPosX > boundX) {
            newPosX = boundX
            newVelX = -newVelX * 0.45f
            hitWall = true
        }

        if (newPosY < -boundY) {
            newPosY = -boundY
            newVelY = -newVelY * 0.45f
            hitWall = true
        } else if (newPosY > boundY) {
            newPosY = boundY
            newVelY = -newVelY * 0.45f
            hitWall = true
        }

        // Obstacle Collisions
        for (obs in obstacles) {
            if (obs.type == ObstacleType.LAVA_HAZARD) {
                if (a2.invulnerableTimer <= 0f && !a2.isEaten) {
                    val dist = Vector2(newPosX, newPosY).distance(obs.pos)
                    if (dist < a2.radius + obs.radius) {
                        val bounceDir = (Vector2(newPosX, newPosY) - obs.pos).normalized()
                        newVelX = bounceDir.x * 320f
                        newVelY = bounceDir.y * 320f
                        newPosX += bounceDir.x * 15f
                        newPosY += bounceDir.y * 15f

                        val newScore = maxOf(0, stats.score - 1)
                        stats = stats.copy(score = newScore)
                        onScoreChanged?.invoke(newScore)
                        onLavaHit?.invoke()
                        spawnLavaParticles(Vector2(newPosX, newPosY))
                        showBanner("🔥 LAVA HAZARD! −1")
                        armadillo2 = a2.copy(invulnerableTimer = 1.3f)
                    }
                }
            } else {
                val dist = Vector2(newPosX, newPosY).distance(obs.pos)
                val minDist = a2.radius + obs.radius
                if (dist < minDist && dist > 0.001f) {
                    val normal = (Vector2(newPosX, newPosY) - obs.pos).normalized()
                    val overlap = minDist - dist
                    newPosX += normal.x * overlap
                    newPosY += normal.y * overlap

                    val dot = newVelX * normal.x + newVelY * normal.y
                    if (dot < 0f) {
                        newVelX -= normal.x * dot * 1.5f
                        newVelY -= normal.y * dot * 1.5f
                    }
                    hitWall = true
                }
            }
        }

        if (hitWall && speed > 60f) {
            onWallBump?.invoke()
            spawnImpactParticles(Vector2(newPosX, newPosY))
        }

        // Heading angle
        val currentSpeed = sqrt(newVelX * newVelX + newVelY * newVelY)
        var newHeading = a2.headingAngle
        if (currentSpeed > 8f) {
            val targetAngle = atan2(newVelY, newVelX)
            val diff = normalizeAngle(targetAngle - a2.headingAngle)
            val turnSpeed = if (powerActive) 22f else 12f
            newHeading = a2.headingAngle + diff * (turnSpeed * dt).coerceAtMost(1f)
        }

        val rollFactor = if (powerActive) 0.16f else 0.08f
        val legFactor = if (powerActive) 0.30f else 0.15f
        val newRollDistance = a2.rollDistance + currentSpeed * dt * rollFactor
        val newLegPhase = a2.legStepPhase + currentSpeed * dt * legFactor
        val newTailWag = a2.tailWagPhase + (if (powerActive) 14f else 6f) * dt
        val newAuraPhase = a2.auraPhase + 8f * dt

        val invulnerable = (a2.invulnerableTimer - dt).coerceAtLeast(0f)

        if (powerActive && currentSpeed > 30f) {
            spawnPowerTrail(Vector2(newPosX, newPosY), a2.headingAngle)
        }

        armadillo2 = a2.copy(
            pos = Vector2(newPosX, newPosY),
            vel = Vector2(newVelX, newVelY),
            headingAngle = newHeading,
            rollDistance = newRollDistance,
            legStepPhase = newLegPhase,
            tailWagPhase = newTailWag,
            invulnerableTimer = invulnerable,
            isPowerActive = powerActive,
            powerTimer = powerRemaining,
            powerChancesRemaining = chances,
            auraPhase = newAuraPhase
        )
    }

    private fun updateSnakes(
        dt: Float,
        snake1InputX: Float = 0f,
        snake1InputY: Float = 0f,
        snake2InputX: Float = 0f,
        snake2InputY: Float = 0f
    ) {
        if (snakes.isEmpty()) return

        // Synchronize primary snake if modified externally
        snakes[0] = snake

        val speedMultiplier = 1f + (timeAliveSeconds * 0.015f).coerceAtMost(0.85f)

        for (sIdx in snakes.indices) {
            val curSnake = snakes[sIdx]
            val isS1Human = !currentMatchConfig.isSolo && sIdx == 0 && currentMatchConfig.getSlot(CharacterId.SNAKE_1).roleType == RoleType.HUMAN
            val isS2Human = !currentMatchConfig.isSolo && sIdx == 1 && currentMatchConfig.getSlot(CharacterId.SNAKE_2).roleType == RoleType.HUMAN

            val isHumanControlled = isS1Human || isS2Human
            val inputX = if (isS1Human) snake1InputX else if (isS2Human) snake2InputX else 0f
            val inputY = if (isS1Human) snake1InputY else if (isS2Human) snake2InputY else 0f
            val inputMag = sqrt(inputX * inputX + inputY * inputY)

            val targetSpeed: Float
            val newHeadAngle: Float

            if (isHumanControlled) {
                if (inputMag > 0.08f) {
                    val desiredAngle = atan2(inputY, inputX)
                    val angleDiff = normalizeAngle(desiredAngle - curSnake.headAngle)
                    val turnRate = 4.2f
                    val maxTurn = turnRate * dt
                    val headTurn = angleDiff.coerceIn(-maxTurn, maxTurn)
                    newHeadAngle = curSnake.headAngle + headTurn
                    targetSpeed = curSnake.baseSpeed * (0.55f + 0.70f * inputMag.coerceAtMost(1f))
                } else {
                    newHeadAngle = curSnake.headAngle
                    targetSpeed = curSnake.baseSpeed * 0.25f
                }
            } else {
                targetSpeed = curSnake.baseSpeed * speedMultiplier

                // Intelligent target selection: dynamically adapt to both armadillos
                val a1Alive = !armadillo.isEaten && (currentMatchConfig.isSolo || currentMatchConfig.getSlot(CharacterId.ARMADILLO_1).roleType != RoleType.DISABLED)
                val a2 = armadillo2
                val a2Alive = a2 != null && !a2.isEaten && (currentMatchConfig.isSolo || currentMatchConfig.getSlot(CharacterId.ARMADILLO_2).roleType != RoleType.DISABLED)

                val targetPos = when {
                    a1Alive && a2Alive -> {
                        // Both armadillos are alive!
                        if (snakes.size >= 2) {
                            if (sIdx == 0) {
                                val d1 = curSnake.headPos.distance(armadillo.pos)
                                val d2 = curSnake.headPos.distance(a2!!.pos)
                                if (d1 <= d2) armadillo.pos else a2.pos
                            } else if (sIdx == 1) {
                                val d1 = snakes[0].headPos.distance(armadillo.pos)
                                val d2 = snakes[0].headPos.distance(a2!!.pos)
                                if (d1 <= d2) a2.pos else armadillo.pos
                            } else {
                                val d1 = curSnake.headPos.distance(armadillo.pos)
                                val d2 = curSnake.headPos.distance(a2!!.pos)
                                if (d1 <= d2) armadillo.pos else a2.pos
                            }
                        } else {
                            val d1 = curSnake.headPos.distance(armadillo.pos)
                            val d2 = curSnake.headPos.distance(a2!!.pos)
                            if (d1 <= d2) armadillo.pos else a2.pos
                        }
                    }
                    a1Alive -> armadillo.pos
                    a2Alive -> a2!!.pos
                    else -> Vector2(0f, 0f)
                }

                val toTarget = targetPos - curSnake.headPos
                var desiredAngle = atan2(toTarget.y, toTarget.x)

                // Secondary snake stalks with offset angle to surround target
                if (sIdx == 1 && !a2Alive) {
                    desiredAngle = normalizeAngle(desiredAngle + 0.35f * sin(timeAliveSeconds * 2f))
                }

                val angleDiff = normalizeAngle(desiredAngle - curSnake.headAngle)

                // Smooth steering rate limit
                val turnRate = 2.4f + (speedMultiplier - 1f) * 0.6f
                val maxTurn = turnRate * dt
                val headTurn = angleDiff.coerceIn(-maxTurn, maxTurn)
                newHeadAngle = curSnake.headAngle + headTurn
            }

            // Advance head with slithering motion
            val forwardDir = Vector2(cos(newHeadAngle), sin(newHeadAngle))
            val newHeadPos = curSnake.headPos + forwardDir * (targetSpeed * dt)

            // Boundary constraint for snake head
            val sBoundX = boardHalfWidth - 35f
            val sBoundY = boardHalfHeight - 35f
            val clampedHeadX = newHeadPos.x.coerceIn(-sBoundX, sBoundX)
            val clampedHeadY = newHeadPos.y.coerceIn(-sBoundY, sBoundY)
            val clampedHeadPos = Vector2(clampedHeadX, clampedHeadY)

            // Segment trailing physics (IK)
            val segmentDist = 18f
            val newSegments = mutableListOf<Vector2>()
            var leadPos = clampedHeadPos

            for (i in curSnake.bodySegments.indices) {
                val currentSeg = curSnake.bodySegments[i]
                val segDiff = leadPos - currentSeg
                val dist = segDiff.length()
                val newSegPos = if (dist > 0.001f) {
                    leadPos - segDiff.normalized() * segmentDist
                } else {
                    currentSeg
                }
                newSegments.add(newSegPos)
                leadPos = newSegPos
            }

            val newSlitherPhase = curSnake.slitherPhase + 7.5f * dt
            val newTongueTimer = curSnake.tongueFlickTimer + dt
            val tongueVisible = (newTongueTimer % 2.5f) < 0.5f

            val strikeTimer = if (curSnake.isStriking) curSnake.strikeTimer + dt else 0f
            val isStriking = curSnake.isStriking && strikeTimer < 0.6f

            snakes[sIdx] = curSnake.copy(
                headPos = clampedHeadPos,
                headAngle = newHeadAngle,
                currentSpeed = targetSpeed,
                bodySegments = newSegments,
                slitherPhase = newSlitherPhase,
                tongueFlickTimer = newTongueTimer,
                tongueVisible = tongueVisible,
                isStriking = isStriking,
                strikeTimer = strikeTimer
            )
        }

        // Keep primary snake reference synchronized
        snake = snakes[0]
    }

    private fun updateSnake(dt: Float) {
        updateSnakes(dt)
    }

    fun showBanner(msg: String) {
        bannerMessage = msg
        bannerTimer = 2.4f
    }

    private fun updateApples(dt: Float) {
        for (i in apples.indices) {
            val apple = apples[i]

            // If it's a dropping/escaping fruit
            if (apple.isSpecialEventFruit) {
                var curDropH = apple.dropHeight
                var curDropVel = apple.dropVelZ
                var curBounces = apple.bounceCount
                var curPos = apple.pos
                var curVel = apple.vel
                var curAngle = apple.escapeAngle

                // Vertical falling & bouncing physics
                if (curDropH > 0f || curDropVel != 0f) {
                    curDropVel -= 600f * dt // gravity pulling down to table surface
                    curDropH += curDropVel * dt
                    if (curDropH <= 0f) {
                        curDropH = 0f
                        if (curBounces < 2) {
                            curDropVel = -curDropVel * 0.45f // bounce up
                            curBounces += 1
                        } else {
                            curDropVel = 0f
                        }
                    }
                }

                // Horizontal escaping movement once near table
                if (curDropH < 15f) {
                    val escapeSpeed = apple.escapeSpeed
                    curPos = curPos + Vector2(cos(curAngle) * escapeSpeed * dt, sin(curAngle) * escapeSpeed * dt)

                    // Bounce off table edges so it stays in play and can be caught!
                    val boundX = boardHalfWidth - 30f
                    val boundY = boardHalfHeight - 30f
                    var turned = false

                    if (curPos.x < -boundX) {
                        curPos = Vector2(-boundX, curPos.y)
                        curAngle = normalizeAngle(Math.PI.toFloat() - curAngle)
                        turned = true
                    } else if (curPos.x > boundX) {
                        curPos = Vector2(boundX, curPos.y)
                        curAngle = normalizeAngle(Math.PI.toFloat() - curAngle)
                        turned = true
                    }

                    if (curPos.y < -boundY) {
                        curPos = Vector2(curPos.x, -boundY)
                        curAngle = normalizeAngle(-curAngle)
                        turned = true
                    } else if (curPos.y > boundY) {
                        curPos = Vector2(curPos.x, boundY)
                        curAngle = normalizeAngle(-curAngle)
                        turned = true
                    }

                    // Slight erratic curving for fun chaotic chase
                    if (!turned) {
                        curAngle += (sin(apple.sparkleTimer * 6f) * 1.5f) * dt
                    }
                }

                val newSparkle = apple.sparkleTimer + dt
                if (apple.fruitType == FruitType.ESCAPE_STAR_POWER && Random.nextFloat() < 0.35f) {
                    spawnStarGlowParticle(curPos)
                }

                apples[i] = apple.copy(
                    pos = curPos,
                    dropHeight = curDropH,
                    dropVelZ = curDropVel,
                    bounceCount = curBounces,
                    escapeAngle = curAngle,
                    sparkleTimer = newSparkle,
                    bobPhase = apple.bobPhase + 4f * dt
                )
            } else {
                // Regular fruit: if stage has escaping fruits (Jungle, Dark Temple, Final Arena), fruits slowly scurry/evade!
                var curPos = apple.pos
                if (currentStage.hasEscapingFruits) {
                    val threatPos = when {
                        !armadillo.isEaten && armadillo2 != null && !armadillo2!!.isEaten -> {
                            if (curPos.distance(armadillo.pos) <= curPos.distance(armadillo2!!.pos)) armadillo.pos else armadillo2!!.pos
                        }
                        !armadillo.isEaten -> armadillo.pos
                        armadillo2 != null && !armadillo2!!.isEaten -> armadillo2!!.pos
                        else -> null
                    }
                    if (threatPos != null && curPos.distance(threatPos) < 140f) {
                        val awayDir = (curPos - threatPos).normalized()
                        curPos = curPos + awayDir * (55f * dt)
                        val boundX = boardHalfWidth - 35f
                        val boundY = boardHalfHeight - 35f
                        curPos = Vector2(curPos.x.coerceIn(-boundX, boundX), curPos.y.coerceIn(-boundY, boundY))
                    }
                }
                val newPhase = apple.bobPhase + 3f * dt
                apples[i] = apple.copy(pos = curPos, bobPhase = newPhase)
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            val newLife = p.life + dt
            if (newLife >= p.maxLife) {
                particles.remove(p)
            } else {
                val newPos = p.pos + p.vel * dt
                val alpha = (1f - newLife / p.maxLife).coerceIn(0f, 1f)
                val updated = p.copy(pos = newPos, life = newLife, alpha = alpha)
                particles.remove(p)
                particles.add(updated)
            }
        }
    }

    private fun checkCollisions() {
        val eatenIndices = mutableListOf<Int>()

        // 1. Armadillo 1 & Apples
        if (!armadillo.isEaten) {
            val pickupRadius = armadillo.radius + 20f

            for (i in apples.indices) {
                val apple = apples[i]
                if (armadillo.pos.distance(apple.pos) < pickupRadius && apple.dropHeight < 30f) {
                    eatenIndices.add(i)

                    // Collect Apple!
                    val newScore = stats.score + 1
                    val newHigh = maxOf(stats.highScore, newScore)
                    stats = stats.copy(score = newScore, highScore = newHigh)
                    onScoreChanged?.invoke(newScore)
                    onAppleCollected?.invoke()

                    if (apple.fruitType == FruitType.ESCAPE_STAR_POWER) {
                        // Activate 7araka Special Ability!
                        armadillo = armadillo.copy(
                            isPowerActive = true,
                            powerTimer = 10f,
                            powerChancesRemaining = 1,
                            invulnerableTimer = 0.5f
                        )
                        showBanner(if (armadillo2 != null) "⚡ GOLDEN ARMADILLO 7ARAKA!" else "⚡ SPECIAL 7ARAKA ACTIVE")
                        onPowerActivated?.invoke()
                        spawnPowerBurstParticles(armadillo.pos)
                    } else {
                        spawnAppleSparkles(apple.pos)
                    }

                    // Increment progress towards Special Event
                    fruitsCollectedSinceEvent++
                    if (fruitsCollectedSinceEvent >= 4) {
                        fruitsCollectedSinceEvent = 0
                        triggerSpecialFruitEvent()
                    }

                    // Increment progress towards Stage completion
                    stageFruitsCollected++
                    if (stageFruitsCollected >= currentStage.targetFruits && !isStageCompleted) {
                        isStageCompleted = true
                        spawnStageCelebrationParticles(armadillo.pos)
                        showBanner("🎉 STAGE ${currentStage.id} COMPLETE!")
                        onStageCompleted?.invoke(
                            currentStage,
                            stats.score,
                            stageFruitsCollected,
                            stats.armadillosLost,
                            timeAliveSeconds
                        )
                    }
                }
            }
        }

        // 2. Blue Armadillo 2 & Apples
        val a2 = armadillo2
        if (a2 != null && !a2.isEaten) {
            val pickupRadius = a2.radius + 20f

            for (i in apples.indices) {
                if (eatenIndices.contains(i)) continue
                val apple = apples[i]
                if (a2.pos.distance(apple.pos) < pickupRadius && apple.dropHeight < 30f) {
                    eatenIndices.add(i)

                    // Collect Apple!
                    val newScore = stats.score + 1
                    val newHigh = maxOf(stats.highScore, newScore)
                    stats = stats.copy(score = newScore, highScore = newHigh)
                    onScoreChanged?.invoke(newScore)
                    onAppleCollected?.invoke()

                    if (apple.fruitType == FruitType.ESCAPE_STAR_POWER) {
                        armadillo2 = a2.copy(
                            isPowerActive = true,
                            powerTimer = 10f,
                            powerChancesRemaining = 1,
                            invulnerableTimer = 0.5f
                        )
                        showBanner("⚡ BLUE ARMADILLO 7ARAKA!")
                        onPowerActivated?.invoke()
                        spawnPowerBurstParticles(a2.pos)
                    } else {
                        spawnAppleSparkles(apple.pos)
                    }

                    // Increment progress towards Special Event
                    fruitsCollectedSinceEvent++
                    if (fruitsCollectedSinceEvent >= 4) {
                        fruitsCollectedSinceEvent = 0
                        triggerSpecialFruitEvent()
                    }

                    // Increment progress towards Stage completion
                    stageFruitsCollected++
                    if (stageFruitsCollected >= currentStage.targetFruits && !isStageCompleted) {
                        isStageCompleted = true
                        spawnStageCelebrationParticles(a2.pos)
                        showBanner("🎉 STAGE ${currentStage.id} COMPLETE!")
                        onStageCompleted?.invoke(
                            currentStage,
                            stats.score,
                            stageFruitsCollected,
                            stats.armadillosLost,
                            timeAliveSeconds
                        )
                    }
                }
            }
        }

        // Remove eaten fruits or replace regular ones
        if (eatenIndices.isNotEmpty()) {
            for (idx in eatenIndices.distinct().sortedDescending()) {
                if (idx < apples.size) {
                    val eaten = apples[idx]
                    if (eaten.isSpecialEventFruit) {
                        apples.removeAt(idx)
                        if (apples.isEmpty()) {
                            spawnStandardApples(3)
                        }
                    } else {
                        apples[idx] = eaten.copy(pos = getRandomSafeApplePos())
                    }
                }
            }
        }

        // 3. Snake catches Armadillo 1
        if (!armadillo.isEaten && armadillo.invulnerableTimer <= 0f) {
            val catchRadius = armadillo.radius + 22f
            for (sIdx in snakes.indices) {
                val curSnake = snakes[sIdx]
                val distToHead = armadillo.pos.distance(curSnake.headPos)
                if (distToHead < catchRadius) {
                    if (armadillo.isPowerActive) {
                        val newScore = maxOf(0, stats.score - 1)
                        stats = stats.copy(score = newScore)
                        onScoreChanged?.invoke(newScore)

                        spawnPowerShieldHitParticles(armadillo.pos)
                        onPowerProtectedHit?.invoke()

                        armadillo = armadillo.copy(
                            isPowerActive = false,
                            powerTimer = 0f,
                            powerChancesRemaining = 0,
                            invulnerableTimer = 2.0f
                        )
                        showBanner("POWER LOST −1")

                        val pushDir = (curSnake.headPos - armadillo.pos).normalized()
                        val recoilSnake = curSnake.copy(
                            headPos = curSnake.headPos + pushDir * 35f,
                            isStriking = true,
                            strikeTimer = 0f
                        )
                        snakes[sIdx] = recoilSnake
                        if (sIdx == 0) snake = recoilSnake
                    } else {
                        val newScore = maxOf(0, stats.score - 1)
                        val newLost = stats.armadillosLost + 1
                        stats = stats.copy(score = newScore, armadillosLost = newLost)
                        onScoreChanged?.invoke(newScore)
                        onArmadilloEaten?.invoke()

                        armadillo = armadillo.copy(
                            isEaten = true,
                            eatenTimer = 0f,
                            eatenScale = 1f
                        )

                        val strikingSnake = curSnake.copy(
                            isStriking = true,
                            strikeTimer = 0f
                        )
                        snakes[sIdx] = strikingSnake
                        if (sIdx == 0) snake = strikingSnake

                        snakeCatches++
                        if (!currentMatchConfig.isSolo) {
                            val snakeP = if (sIdx == 0) currentMatchConfig.getPlayerForCharacter(CharacterId.SNAKE_1) else currentMatchConfig.getPlayerForCharacter(CharacterId.SNAKE_2)
                            if (snakeP != null) {
                                showBanner("🐍 P$snakeP CAUGHT ARMADILLO 1!")
                            }
                        }

                        spawnEatenParticles(armadillo.pos)
                    }
                    break
                }
            }
        }

        // 4. Snake catches Blue Armadillo 2
        val curA2 = armadillo2
        if (curA2 != null && !curA2.isEaten && curA2.invulnerableTimer <= 0f) {
            val catchRadius = curA2.radius + 22f
            for (sIdx in snakes.indices) {
                val curSnake = snakes[sIdx]
                val distToHead = curA2.pos.distance(curSnake.headPos)
                if (distToHead < catchRadius) {
                    if (curA2.isPowerActive) {
                        val newScore = maxOf(0, stats.score - 1)
                        stats = stats.copy(score = newScore)
                        onScoreChanged?.invoke(newScore)

                        spawnPowerShieldHitParticles(curA2.pos)
                        onPowerProtectedHit?.invoke()

                        armadillo2 = curA2.copy(
                            isPowerActive = false,
                            powerTimer = 0f,
                            powerChancesRemaining = 0,
                            invulnerableTimer = 2.0f
                        )
                        showBanner("BLUE ARMADILLO POWER LOST −1")

                        val pushDir = (curSnake.headPos - curA2.pos).normalized()
                        val recoilSnake = curSnake.copy(
                            headPos = curSnake.headPos + pushDir * 35f,
                            isStriking = true,
                            strikeTimer = 0f
                        )
                        snakes[sIdx] = recoilSnake
                        if (sIdx == 0) snake = recoilSnake
                    } else {
                        val newScore = maxOf(0, stats.score - 1)
                        val newLost = stats.armadillosLost + 1
                        stats = stats.copy(score = newScore, armadillosLost = newLost)
                        onScoreChanged?.invoke(newScore)
                        onArmadilloEaten?.invoke()

                        armadillo2 = curA2.copy(
                            isEaten = true,
                            eatenTimer = 0f,
                            eatenScale = 1f
                        )

                        val strikingSnake = curSnake.copy(
                            isStriking = true,
                            strikeTimer = 0f
                        )
                        snakes[sIdx] = strikingSnake
                        if (sIdx == 0) snake = strikingSnake

                        snakeCatches++
                        if (!currentMatchConfig.isSolo) {
                            val snakeP = if (sIdx == 0) currentMatchConfig.getPlayerForCharacter(CharacterId.SNAKE_1) else currentMatchConfig.getPlayerForCharacter(CharacterId.SNAKE_2)
                            if (snakeP != null) {
                                showBanner("🐍 P$snakeP CAUGHT ARMADILLO 2!")
                            }
                        }

                        spawnEatenParticles(curA2.pos)
                    }
                    break
                }
            }
        }
    }

    /**
     * Triggered every 4 collected fruits:
     * 4 special fruits appear above the table, drop/fall onto playing surface,
     * bounce slightly, and quickly escape in different directions.
     * One of the fruits is the STAR POWER fruit that grants the 7araka ability.
     */
    fun triggerSpecialFruitEvent() {
        onSpecialEventTriggered?.invoke()
        showBanner("🍎 SPECIAL FRUIT EVENT! CATCH THEM!")

        // Remove any old escaping fruits to keep table clean and exciting
        apples.removeAll { it.isSpecialEventFruit }

        val angles = listOf(
            Random.nextFloat() * 0.8f + 0.2f,
            Random.nextFloat() * 0.8f + 1.8f,
            Random.nextFloat() * 0.8f + 3.4f,
            Random.nextFloat() * 0.8f + 4.9f
        )

        // Generate 4 escaping fruits: 1 STAR POWER + 3 GOLDEN ESCAPE FRUITS
        val starIdx = Random.nextInt(4)

        for (i in 0 until 4) {
            val isStar = (i == starIdx)
            val dropStartH = 120f + (i * 20f)
            val escapeAngle = angles[i]
            val escapeSpeed = if (isStar) 120f else (140f + Random.nextFloat() * 40f)

            // Spawn near center of the table with offset
            val spawnOffset = Vector2(
                (Random.nextFloat() - 0.5f) * 60f,
                (Random.nextFloat() - 0.5f) * 60f
            )

            val fruit = AppleItem(
                id = nextAppleId++,
                pos = spawnOffset,
                fruitType = if (isStar) FruitType.ESCAPE_STAR_POWER else FruitType.ESCAPE_GOLDEN,
                dropHeight = dropStartH,
                dropVelZ = 0f,
                bounceCount = 0,
                isSpecialEventFruit = true,
                escapeSpeed = escapeSpeed,
                escapeAngle = escapeAngle
            )
            apples.add(fruit)

            // Sparkle burst at drop position
            spawnEventDropPuff(spawnOffset)
        }
    }

    private fun spawnStandardApples(count: Int) {
        for (i in 0 until count) {
            apples.add(
                AppleItem(
                    id = nextAppleId++,
                    pos = getRandomSafeApplePos(),
                    fruitType = FruitType.REGULAR_APPLE
                )
            )
        }
    }

    private fun spawnNewArmadillo() {
        // Safe spawn away from snake
        var candidate = Vector2(0f, 0f)
        val sBoundX = boardHalfWidth * 0.7f
        val sBoundY = boardHalfHeight * 0.7f

        for (attempt in 0..12) {
            val rx = Random.nextFloat() * (2 * sBoundX) - sBoundX
            val ry = Random.nextFloat() * (2 * sBoundY) - sBoundY
            candidate = Vector2(rx, ry)
            val distToSnake = snakes.minOfOrNull { it.headPos.distance(candidate) } ?: 999f
            val distToA2 = armadillo2?.pos?.distance(candidate) ?: 999f
            if (distToSnake > 240f && distToA2 > 100f) {
                break
            }
        }

        armadillo = ArmadilloState(
            pos = candidate,
            vel = Vector2(0f, 0f),
            radius = 28f,
            headingAngle = Random.nextFloat() * 6.28f,
            isEaten = false,
            eatenTimer = 0f,
            eatenScale = 1f,
            invulnerableTimer = 2.0f
        )

        spawnRespawnPuff(candidate)
        onArmadilloSpawned?.invoke()
    }

    private fun spawnNewArmadillo2() {
        val sBoundX = boardHalfWidth * 0.7f
        val sBoundY = boardHalfHeight * 0.7f
        var candidate = Vector2(boardHalfWidth * 0.35f, 0f)

        for (attempt in 0..12) {
            val rx = Random.nextFloat() * (2 * sBoundX) - sBoundX
            val ry = Random.nextFloat() * (2 * sBoundY) - sBoundY
            candidate = Vector2(rx, ry)
            val distToSnake = snakes.minOfOrNull { it.headPos.distance(candidate) } ?: 999f
            val distToA1 = candidate.distance(armadillo.pos)
            if (distToSnake > 240f && distToA1 > 100f) {
                break
            }
        }

        armadillo2 = ArmadilloState(
            pos = candidate,
            vel = Vector2(0f, 0f),
            radius = 28f,
            headingAngle = Random.nextFloat() * 6.28f,
            isEaten = false,
            eatenTimer = 0f,
            eatenScale = 1f,
            invulnerableTimer = 2.0f
        )

        spawnRespawnPuff(candidate)
        onArmadilloSpawned?.invoke()
    }

    private fun getRandomSafeApplePos(): Vector2 {
        val sBoundX = boardHalfWidth * 0.8f
        val sBoundY = boardHalfHeight * 0.8f
        var candidate = Vector2(0f, 0f)

        for (attempt in 0..15) {
            val rx = Random.nextFloat() * (2 * sBoundX) - sBoundX
            val ry = Random.nextFloat() * (2 * sBoundY) - sBoundY
            candidate = Vector2(rx, ry)
            val distToSnake = snakes.minOfOrNull { it.headPos.distance(candidate) } ?: 999f
            val distToA1 = candidate.distance(armadillo.pos)
            val distToA2 = armadillo2?.pos?.distance(candidate) ?: 999f
            if (distToSnake > 120f && distToA1 > 80f && distToA2 > 80f) {
                break
            }
        }
        return candidate
    }

    private fun spawnAppleSparkles(pos: Vector2) {
        val colors = listOf(
            Color(0xFFFFD54F),
            Color(0xFFFF7043),
            Color(0xFFE53935),
            Color(0xFF81C784),
            Color(0xFFFFFFFF)
        )
        for (i in 0 until 14) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 160f + 60f
            val vel = Vector2.fromAngle(angle, speed)
            particles.add(
                Particle(
                    pos = pos,
                    vel = vel,
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 7f + 5f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = Random.nextFloat() * 0.4f + 0.35f
                )
            )
        }
    }

    private fun spawnEatenParticles(pos: Vector2) {
        val colors = listOf(
            Color(0xFFD4AF37),
            Color(0xFF8D5524),
            Color(0xFFC07E44),
            Color(0xFFE57373)
        )
        for (i in 0 until 18) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 200f + 70f
            val vel = Vector2.fromAngle(angle, speed)
            particles.add(
                Particle(
                    pos = pos,
                    vel = vel,
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 9f + 6f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = Random.nextFloat() * 0.5f + 0.4f
                )
            )
        }
    }

    private fun spawnRespawnPuff(pos: Vector2) {
        val colors = listOf(
            Color(0xFFFFF8E1),
            Color(0xFFFFECB3),
            Color(0xFFD4AF37)
        )
        for (i in 0 until 12) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 110f + 30f
            val vel = Vector2.fromAngle(angle, speed)
            particles.add(
                Particle(
                    pos = pos,
                    vel = vel,
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 8f + 4f,
                    alpha = 0.9f,
                    life = 0f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun spawnImpactParticles(pos: Vector2) {
        for (i in 0 until 6) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 90f + 30f
            val vel = Vector2.fromAngle(angle, speed)
            particles.add(
                Particle(
                    pos = pos,
                    vel = vel,
                    color = Color(0x80D4AF37),
                    size = Random.nextFloat() * 4f + 3f,
                    alpha = 0.7f,
                    life = 0f,
                    maxLife = 0.25f
                )
            )
        }
    }

    private fun spawnPowerTrail(pos: Vector2, heading: Float) {
        val trailPos = pos - Vector2.fromAngle(heading, 20f)
        val colors = listOf(Color(0xFFFFD54F), Color(0xFFFF9100), Color(0xFF00E5FF), Color(0xFFFFFFFF))
        particles.add(
            Particle(
                pos = trailPos + Vector2((Random.nextFloat() - 0.5f) * 10f, (Random.nextFloat() - 0.5f) * 10f),
                vel = Vector2.fromAngle(heading + 3.14f + (Random.nextFloat() - 0.5f) * 0.8f, Random.nextFloat() * 60f + 20f),
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 6f + 3f,
                alpha = 0.85f,
                life = 0f,
                maxLife = 0.3f
            )
        )
    }

    private fun spawnStarGlowParticle(pos: Vector2) {
        particles.add(
            Particle(
                pos = pos + Vector2((Random.nextFloat() - 0.5f) * 24f, (Random.nextFloat() - 0.5f) * 24f),
                vel = Vector2((Random.nextFloat() - 0.5f) * 20f, -Random.nextFloat() * 30f - 10f),
                color = if (Random.nextBoolean()) Color(0xFF00E5FF) else Color(0xFFFFEA00),
                size = Random.nextFloat() * 5f + 2f,
                alpha = 0.9f,
                life = 0f,
                maxLife = 0.4f
            )
        )
    }

    private fun spawnPowerBurstParticles(pos: Vector2) {
        val colors = listOf(Color(0xFF00E5FF), Color(0xFFFFEA00), Color(0xFFFF3D00), Color(0xFFFFFFFF))
        for (i in 0 until 24) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 220f + 80f
            particles.add(
                Particle(
                    pos = pos,
                    vel = Vector2.fromAngle(angle, speed),
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 8f + 5f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = 0.55f
                )
            )
        }
    }

    private fun spawnPowerShieldHitParticles(pos: Vector2) {
        val colors = listOf(Color(0xFF00E5FF), Color(0xFFFF1744), Color(0xFFFFD54F), Color(0xFFFFFFFF))
        for (i in 0 until 20) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 200f + 60f
            particles.add(
                Particle(
                    pos = pos,
                    vel = Vector2.fromAngle(angle, speed),
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 9f + 4f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun spawnEventDropPuff(pos: Vector2) {
        for (i in 0 until 10) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 80f + 20f
            particles.add(
                Particle(
                    pos = pos,
                    vel = Vector2.fromAngle(angle, speed),
                    color = Color(0xFFFFD54F),
                    size = Random.nextFloat() * 6f + 3f,
                    alpha = 0.8f,
                    life = 0f,
                    maxLife = 0.35f
                )
            )
        }
    }

    private fun spawnLavaParticles(pos: Vector2) {
        val lavaColors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFDD2C00), Color(0xFF4E342E))
        for (i in 0 until 18) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 140f + 40f
            particles.add(
                Particle(
                    pos = pos,
                    vel = Vector2.fromAngle(angle, speed),
                    color = lavaColors[Random.nextInt(lavaColors.size)],
                    size = Random.nextFloat() * 8f + 4f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun spawnStageCelebrationParticles(pos: Vector2) {
        val confettiColors = listOf(
            Color(0xFFFFD700),
            Color(0xFFFF4081),
            Color(0xFF00E5FF),
            Color(0xFF76FF03),
            Color(0xFFFF6D00),
            Color(0xFFE040FB),
            Color(0xFFFFFFFF)
        )
        for (i in 0 until 40) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 260f + 80f
            particles.add(
                Particle(
                    pos = pos,
                    vel = Vector2.fromAngle(angle, speed),
                    color = confettiColors[Random.nextInt(confettiColors.size)],
                    size = Random.nextFloat() * 9f + 5f,
                    alpha = 1f,
                    life = 0f,
                    maxLife = Random.nextFloat() * 0.6f + 0.6f
                )
            )
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > Math.PI) a -= (2 * Math.PI).toFloat()
        while (a < -Math.PI) a += (2 * Math.PI).toFloat()
        return a
    }
}
