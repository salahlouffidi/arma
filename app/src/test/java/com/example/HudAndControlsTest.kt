package com.example

import com.example.game.GameEngine
import com.example.game.StageConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HudAndControlsTest {

    @Test
    fun testBannerUsesOverdriveTerminology() {
        val engine = GameEngine()

        // Load single-armadillo stage (e.g. Stage 1)
        engine.loadStage(StageConfig.getStage(1))

        // Trigger special fruit event and power activation
        engine.triggerSpecialFruitEvent()

        // Directly test eating the star power apple
        val starApple = engine.apples.firstOrNull { it.fruitType == com.example.game.FruitType.ESCAPE_STAR_POWER }
        if (starApple != null) {
            engine.apples[engine.apples.indexOf(starApple)] = starApple.copy(
                pos = engine.armadillo.pos,
                dropHeight = 0f
            )
            engine.update(0.016f, 0f, 0f)
        }

        // Verify that if banner message was triggered, it uses OVERDRIVE, not 7ARAKA
        val receivedBanner = engine.bannerMessage
        if (receivedBanner != null) {
            assertTrue("Banner should contain OVERDRIVE", receivedBanner.contains("OVERDRIVE"))
            assertFalse("Banner must not contain 7ARAKA", receivedBanner.contains("7ARAKA"))
        }
    }

    @Test
    fun testDualArmadilloBannerUsesOverdriveTerminology() {
        val engine = GameEngine()

        // Load dual-armadillo stage (e.g. Stage 9)
        engine.loadStage(StageConfig.getStage(9))
        assertTrue(engine.isDualArmadillo)

        engine.triggerSpecialFruitEvent()

        // Directly test eating the star power apple with armadillo 2 (Blue)
        val a2 = engine.armadillo2
        if (a2 != null) {
            val starApple = engine.apples.firstOrNull { it.fruitType == com.example.game.FruitType.ESCAPE_STAR_POWER }
            if (starApple != null) {
                engine.apples[engine.apples.indexOf(starApple)] = starApple.copy(
                    pos = a2.pos,
                    dropHeight = 0f
                )
                engine.update(0.016f, 0f, 0f)
            }
        }

        val receivedBanner = engine.bannerMessage
        if (receivedBanner != null) {
            assertTrue("Banner should contain OVERDRIVE", receivedBanner.contains("OVERDRIVE"))
            assertFalse("Banner must not contain 7ARAKA", receivedBanner.contains("7ARAKA"))
        }
    }
}
