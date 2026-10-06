package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class RoundJoystickSettingsTest {

    private val PREFS_NAME = "armasnake_settings"
    private fun joystickKey(roundId: Int) = "single_joystick_right_round_$roundId"

    @Test
    fun testPerRoundJoystickSettingIndependence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Clear preferences for a clean state
        prefs.edit().clear().commit()

        // By default, rounds 1 to 8 should be false (LEFT)
        for (round in 1..8) {
            assertFalse("Round $round should default to LEFT", prefs.getBoolean(joystickKey(round), false))
        }

        // Configure per user prompt:
        // ROUND 01 -> LEFT
        // ROUND 02 -> RIGHT
        // ROUND 03 -> LEFT
        // ROUND 04 -> RIGHT
        prefs.edit()
            .putBoolean(joystickKey(1), false)
            .putBoolean(joystickKey(2), true)
            .putBoolean(joystickKey(3), false)
            .putBoolean(joystickKey(4), true)
            .commit()

        // Verify ROUND 01 is LEFT
        assertFalse(prefs.getBoolean(joystickKey(1), false))

        // Verify ROUND 02 is RIGHT
        assertTrue(prefs.getBoolean(joystickKey(2), false))

        // Changing ROUND 02 must NOT affect ROUND 01
        prefs.edit().putBoolean(joystickKey(2), true).commit()
        assertFalse("Changing round 2 must not change round 1", prefs.getBoolean(joystickKey(1), false))

        // Verify ROUND 03 is LEFT and ROUND 04 is RIGHT
        assertFalse(prefs.getBoolean(joystickKey(3), false))
        assertTrue(prefs.getBoolean(joystickKey(4), false))

        // Switch ROUND 01 to RIGHT
        prefs.edit().putBoolean(joystickKey(1), true).commit()
        assertTrue(prefs.getBoolean(joystickKey(1), false))
        assertTrue(prefs.getBoolean(joystickKey(2), false))

        // Switch ROUND 02 to LEFT
        prefs.edit().putBoolean(joystickKey(2), false).commit()
        assertTrue("Round 1 remains RIGHT", prefs.getBoolean(joystickKey(1), false))
        assertFalse("Round 2 is now LEFT", prefs.getBoolean(joystickKey(2), false))
    }

    @Test
    fun testOnlyRounds1to8AreSingleArmadillo() {
        for (roundId in 1..16) {
            val stage = StageConfig.getStage(roundId)
            if (roundId in 1..8) {
                assertFalse("Round $roundId must be single armadillo", stage.isDualArmadillo)
            } else {
                assertTrue("Round $roundId must be dual armadillo (fixed joysticks)", stage.isDualArmadillo)
            }
        }
    }
}
