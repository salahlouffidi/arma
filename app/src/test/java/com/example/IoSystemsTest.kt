package com.example

import com.example.game.AppleItem
import com.example.game.FruitType
import com.example.game.GameEngine
import com.example.game.PowerType
import com.example.game.PowerUpItem
import com.example.game.StageConfig
import com.example.game.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v2.0 gameplay systems: power-ups, combos, fruit values, snake growth/shrink, World 3. */
class IoSystemsTest {

    private fun engineOnRound(id: Int = 1) = GameEngine().apply {
        setBoardDimensions(500f, 280f)
        loadStage(StageConfig.getStage(id))
        // keep snakes far away from the armadillo
        for (i in snakes.indices) snakes[i] = snakes[i].copy(headPos = Vector2(480f, 260f))
        snake = snakes[0]
    }

    private fun GameEngine.feedFruitAtArmadillo(kind: Int = 0) {
        apples.clear()
        apples.add(AppleItem(id = 9000 + stageFruitsCollected, pos = armadillo.pos, fruitType = FruitType.REGULAR_APPLE, kind = kind))
        update(0.016f, 0f, 0f)
    }

    /** Regression: 4th fruit (special event) while Fruit Rain is on the board crashed the app. */
    @Test
    fun specialEventDuringFruitRainDoesNotCrash() {
        val e = engineOnRound()
        repeat(3) {
            e.triggerSurprise(com.example.game.SurpriseEvent.FRUIT_RAIN)
            repeat(8) {
                // put a regular fruit plus all landed rain fruit right on the armadillo
                e.apples.add(AppleItem(id = 5000 + it, pos = e.armadillo.pos, fruitType = FruitType.REGULAR_APPLE))
                e.update(0.016f, 0f, 0f)
            }
            repeat(120) { e.update(0.016f, 0f, 0f) }
        }
        assertTrue(e.stats.score > 0)
    }

    @Test
    fun worldThreeRoundsExist() {
        assertEquals(20, StageConfig.MAX_ROUND)
        for (id in 17..20) assertEquals(id, StageConfig.getStage(id).id)
    }

    @Test
    fun comboRaisesPointsAndResetsAfterWindow() {
        val e = engineOnRound()
        e.feedFruitAtArmadillo()
        assertEquals(1, e.stats.score)
        e.feedFruitAtArmadillo()
        assertEquals("second fruit in window = 1 + combo bonus 1", 3, e.stats.score)
        assertEquals(2, e.comboCount)
        repeat(200) { e.apples.clear(); e.update(0.016f, 0f, 0f) } // > COMBO_WINDOW
        assertEquals(0, e.comboCount)
    }

    @Test
    fun watermelonIsWorthThree() {
        val e = engineOnRound()
        e.feedFruitAtArmadillo(kind = 4)
        assertEquals(3, e.stats.score)
    }

    @Test
    fun doublePowerUpDoublesPoints() {
        val e = engineOnRound()
        e.powerUps.add(PowerUpItem(id = 1, pos = e.armadillo.pos, type = PowerType.DOUBLE))
        e.apples.clear()
        e.update(0.016f, 0f, 0f)
        assertTrue(e.doubleTimer > 0f)
        e.feedFruitAtArmadillo()
        assertEquals(2, e.stats.score)
    }

    @Test
    fun freezeSlowsSnakes() {
        val e = engineOnRound()
        assertEquals(1f, e.freezeFactor, 0.001f)
        e.powerUps.add(PowerUpItem(id = 1, pos = e.armadillo.pos, type = PowerType.FREEZE))
        e.apples.clear()
        e.update(0.016f, 0f, 0f)
        assertTrue(e.freezeFactor < 1f)
    }

    @Test
    fun shrinkCutsSnakesButKeepsMinimum() {
        val e = engineOnRound()
        val before = e.snakes[0].bodySegments.size
        e.powerUps.add(PowerUpItem(id = 1, pos = e.armadillo.pos, type = PowerType.SHRINK))
        e.apples.clear()
        e.update(0.016f, 0f, 0f)
        val after = e.snakes[0].bodySegments.size
        assertTrue(after < before)
        assertTrue(after >= GameEngine.MIN_SNAKE_SEGMENTS)
    }

    @Test
    fun snakesGrowAsPlayerFeeds() {
        val e = engineOnRound()
        val before = e.snakes[0].bodySegments.size
        repeat(GameEngine.GROWTH_EVERY_FRUITS) { e.feedFruitAtArmadillo() }
        assertTrue(e.snakes[0].bodySegments.size > before)
    }
}
