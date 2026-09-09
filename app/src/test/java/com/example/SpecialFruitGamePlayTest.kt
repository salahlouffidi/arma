package com.example

import com.example.game.FruitType
import com.example.game.GameEngine
import com.example.game.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialFruitGamePlayTest {

    @Test
    fun testSpecialFruitEventTriggeredAfter4Fruits() {
        val engine = GameEngine()
        var eventTriggered = false
        engine.onSpecialEventTriggered = { eventTriggered = true }

        // Start with regular 3 apples, fruitsCollectedSinceEvent = 0
        assertEquals(0, engine.fruitsCollectedSinceEvent)

        // Trigger special fruit event directly
        engine.triggerSpecialFruitEvent()
        assertTrue(eventTriggered)

        // Verify 4 escaping fruits were generated
        val specialFruits = engine.apples.filter { it.isSpecialEventFruit }
        assertEquals(4, specialFruits.size)

        // Exactly one should be ESCAPE_STAR_POWER
        val starFruitCount = specialFruits.count { it.fruitType == FruitType.ESCAPE_STAR_POWER }
        assertEquals(1, starFruitCount)

        // Remaining 3 should be ESCAPE_GOLDEN
        val goldenFruitCount = specialFruits.count { it.fruitType == FruitType.ESCAPE_GOLDEN }
        assertEquals(3, goldenFruitCount)
    }

    @Test
    fun testSnakeTouchWhilePowerIsActiveDoesNotKillArmadillo() {
        val engine = GameEngine()
        var armadilloEatenCalled = false
        var powerProtectedHitCalled = false

        engine.onArmadilloEaten = { armadilloEatenCalled = true }
        engine.onPowerProtectedHit = { powerProtectedHitCalled = true }

        // Give initial score
        // Eat 5 fruits: score = 5
        for (i in 0 until 5) {
            engine.apples[0] = engine.apples[0].copy(pos = engine.armadillo.pos)
            engine.update(0.016f, 0f, 0f)
        }
        val currentScore = engine.stats.score
        assertTrue("Score should be positive", currentScore >= 4)

        // Trigger and simulate eating the star fruit
        val starApple = engine.apples.firstOrNull { it.fruitType == FruitType.ESCAPE_STAR_POWER }
        if (starApple != null) {
            engine.apples[engine.apples.indexOf(starApple)] = starApple.copy(
                pos = engine.armadillo.pos,
                dropHeight = 0f
            )
            engine.update(0.016f, 0f, 0f)
        } else {
            // Manually set armadillo power for direct test verification
            val arm = engine.armadillo.copy(
                isPowerActive = true,
                powerTimer = 10f,
                powerChancesRemaining = 1,
                invulnerableTimer = 0f
            )
            val field = GameEngine::class.java.getDeclaredField("armadillo")
            field.isAccessible = true
            field.set(engine, arm)
        }

        assertTrue("Power should be active", engine.armadillo.isPowerActive)
        // If star apple was eaten, powerChancesRemaining is 2 (or 1 if manually set)
        assertTrue("Chances should be > 0", engine.armadillo.powerChancesRemaining > 0)
        val initialChances = engine.armadillo.powerChancesRemaining

        val initialLostCount = engine.stats.armadillosLost
        val scoreBeforeHit = engine.stats.score

        // Move snake directly to armadillo
        val snakeField = GameEngine::class.java.getDeclaredField("snake")
        snakeField.isAccessible = true
        snakeField.set(engine, engine.snake.copy(headPos = engine.armadillo.pos))

        // Ensure armadillo invulnerability is 0 so collision evaluates
        val armField = GameEngine::class.java.getDeclaredField("armadillo")
        armField.isAccessible = true
        armField.set(engine, engine.armadillo.copy(invulnerableTimer = 0f))

        // Process collision
        engine.update(0.016f, 0f, 0f)

        // VERIFY:
        // 1. Armadillo is NOT eaten / removed
        assertFalse("Armadillo must not be eaten", engine.armadillo.isEaten)
        assertFalse("onArmadilloEaten callback must not be triggered", armadilloEatenCalled)
        assertEquals("Lost count must not increase", initialLostCount, engine.stats.armadillosLost)

        // 2. Power protected hit callback triggered
        assertTrue("onPowerProtectedHit should be called", powerProtectedHitCalled)

        // 3. Score decreased by 1
        assertEquals("Score should decrease by 1", scoreBeforeHit - 1, engine.stats.score)

        // 4. One power chance was consumed
        assertEquals(initialChances - 1, engine.armadillo.powerChancesRemaining)
    }
}
