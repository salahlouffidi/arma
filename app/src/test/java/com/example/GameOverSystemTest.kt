package com.example

import com.example.game.GameEngine
import com.example.game.StageConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameOverSystemTest {

    @Test
    fun testMaxAllowedLossesConfiguration() {
        for (roundId in 1..16) {
            val stage = StageConfig.getStage(roundId)
            when (roundId) {
                1, 2 -> assertEquals("Round $roundId should have 5 max allowed losses", 5, stage.maxAllowedLosses)
                3, 4 -> assertEquals("Round $roundId should have 4 max allowed losses", 4, stage.maxAllowedLosses)
                in 5..10 -> assertEquals("Round $roundId should have 3 max allowed losses", 3, stage.maxAllowedLosses)
                in 11..15 -> assertEquals("Round $roundId should have 2 max allowed losses", 2, stage.maxAllowedLosses)
                16 -> assertEquals("Round 16 should have 1 max allowed loss", 1, stage.maxAllowedLosses)
            }
        }
    }

    @Test
    fun testGameOverTriggeredViaCollision() {
        val engine = GameEngine()
        val stage = StageConfig.getStage(1) // maxAllowedLosses = 5
        engine.loadStage(stage)

        var gameOverTriggered = false
        var reportedLost = -1
        engine.onGameOver = { _, _, _, lost, _ ->
            gameOverTriggered = true
            reportedLost = lost
        }

        assertFalse(engine.isGameOver)

        // Repeat collisions until max losses is reached
        for (i in 1..stage.maxAllowedLosses) {
            // Elapse eaten and invulnerability timers (eaten 1.0s + invulnerable 2.0s = 3.0s total)
            for (step in 1..40) {
                engine.update(0.1f, 0f, 0f)
            }
            // Position snake directly at armadillo position
            engine.snakes[0] = engine.snakes[0].copy(headPos = engine.armadillo.pos)
            engine.update(0.016f, 0f, 0f)
        }

        assertTrue("Engine should be in game over state", engine.isGameOver)
        assertTrue("Game over callback should have fired", gameOverTriggered)
        assertEquals(stage.maxAllowedLosses, reportedLost)
        assertEquals(stage.maxAllowedLosses, engine.stats.armadillosLost)

        // Calling resetRound restores playable state
        engine.resetRound()
        assertFalse("Engine should no longer be in game over state after resetRound", engine.isGameOver)
        assertEquals(0, engine.stats.armadillosLost)
        assertEquals(stage.id, engine.currentStage.id)
    }
}
