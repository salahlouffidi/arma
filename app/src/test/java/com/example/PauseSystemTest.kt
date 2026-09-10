package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.GameEngine
import com.example.game.StageConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PauseSystemTest {

    private val PREFS_NAME = "armasnake_settings"
    private fun joystickKey(roundId: Int) = "single_joystick_right_round_$roundId"

    @Test
    fun testGameEnginePauseStopsSimulation() {
        val engine = GameEngine()
        engine.loadStage(StageConfig.getStage(1))

        // Get initial positions
        val initialArmadilloX = engine.armadillo.pos.x
        val initialArmadilloY = engine.armadillo.pos.y
        val initialSnakeX = engine.snake.headPos.x
        val initialSnakeY = engine.snake.headPos.y
        val initialFruitCount = engine.apples.size

        // Set isPaused = true
        engine.isPaused = true
        assertTrue(engine.isPaused)

        // Try updating for 60 frames (1 second) with tilt input
        for (i in 0 until 60) {
            engine.update(
                dt = 0.016f,
                tiltX = 1.0f,
                tiltY = 1.0f
            )
        }

        // Everything must remain completely frozen
        assertEquals("Armadillo X must not change while paused", initialArmadilloX, engine.armadillo.pos.x, 0.0001f)
        assertEquals("Armadillo Y must not change while paused", initialArmadilloY, engine.armadillo.pos.y, 0.0001f)
        assertEquals("Snake X must not change while paused", initialSnakeX, engine.snake.headPos.x, 0.0001f)
        assertEquals("Snake Y must not change while paused", initialSnakeY, engine.snake.headPos.y, 0.0001f)
        assertEquals("Fruit count must remain unchanged while paused", initialFruitCount, engine.apples.size)

        // Now resume
        engine.isPaused = false
        assertFalse(engine.isPaused)

        // Update with input
        for (i in 0 until 60) {
            engine.update(
                dt = 0.016f,
                tiltX = 1.0f,
                tiltY = 1.0f
            )
        }

        // After resuming, simulation must advance
        assertTrue(
            "Armadillo or Snake must move once unpaused",
            engine.armadillo.pos.x != initialArmadilloX || engine.armadillo.pos.y != initialArmadilloY
        )
    }

    @Test
    fun testDualArmadilloSimulationPauseInRound9() {
        val engine = GameEngine()
        engine.loadStage(StageConfig.getStage(9))
        assertTrue(engine.isDualArmadillo)

        val arm1X = engine.armadillo.pos.x
        val arm1Y = engine.armadillo.pos.y
        val arm2 = engine.armadillo2
        org.junit.Assert.assertNotNull(arm2)
        val arm2X = arm2!!.pos.x
        val arm2Y = arm2.pos.y

        // Freeze
        engine.isPaused = true

        // Update with input to both joysticks
        for (i in 0 until 60) {
            engine.update(
                dt = 0.016f,
                tiltX = 1.0f,
                tiltY = 0f,
                tiltX2 = -1.0f,
                tiltY2 = 0f
            )
        }

        assertEquals(arm1X, engine.armadillo.pos.x, 0.0001f)
        assertEquals(arm1Y, engine.armadillo.pos.y, 0.0001f)
        assertEquals(arm2X, arm2.pos.x, 0.0001f)
        assertEquals(arm2Y, arm2.pos.y, 0.0001f)
    }

    @Test
    fun testPerRoundJoystickSettingIndependenceAcrossRounds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Reset
        prefs.edit().clear().commit()

        // Test user example:
        // ROUND 01 -> LEFT (false)
        // ROUND 02 -> RIGHT (true)
        // ROUND 03 -> LEFT (false)
        // ROUND 04 -> RIGHT (true)
        prefs.edit()
            .putBoolean(joystickKey(1), false)
            .putBoolean(joystickKey(2), true)
            .putBoolean(joystickKey(3), false)
            .putBoolean(joystickKey(4), true)
            .commit()

        assertEquals(false, prefs.getBoolean(joystickKey(1), false))
        assertEquals(true, prefs.getBoolean(joystickKey(2), false))
        assertEquals(false, prefs.getBoolean(joystickKey(3), false))
        assertEquals(true, prefs.getBoolean(joystickKey(4), false))

        // Changing ROUND 03 must not affect ROUND 04
        prefs.edit().putBoolean(joystickKey(3), true).commit()
        assertEquals(true, prefs.getBoolean(joystickKey(3), false))
        assertEquals(true, prefs.getBoolean(joystickKey(4), false))

        // Reverting ROUND 03 back to LEFT must not affect ROUND 04
        prefs.edit().putBoolean(joystickKey(3), false).commit()
        assertEquals(false, prefs.getBoolean(joystickKey(3), false))
        assertEquals(true, prefs.getBoolean(joystickKey(4), false))

        // Round 05 defaults to LEFT
        assertEquals(false, prefs.getBoolean(joystickKey(5), false))
    }
}
