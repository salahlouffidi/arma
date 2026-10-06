package com.example

import com.example.game.GameEngine
import com.example.game.StageConfig
import com.example.game.StageProgressManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DualArmadilloGamePlayTest {

    @Test
    fun testAll16StagesConfigured() {
        assertEquals("Total stages must be 16", 16, StageConfig.allStages.size)

        // Stages 1-8 are single armadillo
        for (id in 1..8) {
            val stage = StageConfig.getStage(id)
            assertEquals(id, stage.id)
            assertFalse("Stage $id should NOT be dual armadillo", stage.isDualArmadillo)
        }

        // Stages 9-16 are dual armadillo
        for (id in 9..16) {
            val stage = StageConfig.getStage(id)
            assertEquals(id, stage.id)
            assertTrue("Stage $id MUST be dual armadillo", stage.isDualArmadillo)
        }
    }

    @Test
    fun testDualArmadilloSpawnsInStages9to16() {
        val engine = GameEngine()

        // Stage 1 (Solo)
        engine.loadStage(StageConfig.getStage(1))
        assertNotNull("P1 Golden armadillo must always exist", engine.armadillo)
        assertNull("P2 Blue armadillo must be null in stage 1", engine.armadillo2)
        assertFalse(engine.isDualArmadillo)

        // Stage 9 (Dual)
        engine.loadStage(StageConfig.getStage(9))
        assertNotNull("P1 Golden armadillo must exist in stage 9", engine.armadillo)
        assertNotNull("P2 Blue armadillo must exist in stage 9", engine.armadillo2)
        assertTrue(engine.isDualArmadillo)

        // Stage 16 (Dual Climax)
        engine.loadStage(StageConfig.getStage(16))
        assertNotNull("P1 Golden armadillo must exist in stage 16", engine.armadillo)
        assertNotNull("P2 Blue armadillo must exist in stage 16", engine.armadillo2)
        assertTrue(engine.isDualArmadillo)
    }

    @Test
    fun testDualJoystickMovementUpdatesBothArmadillos() {
        val engine = GameEngine()
        engine.loadStage(StageConfig.getStage(9))

        val initialP1X = engine.armadillo.pos.x
        val initialP2X = engine.armadillo2!!.pos.x

        // Move P1 to the right (+X), and P2 to the left (-X)
        for (i in 0 until 10) {
            engine.update(0.016f, tiltX = 1f, tiltY = 0f, tiltX2 = -1f, tiltY2 = 0f)
        }

        assertTrue("P1 should have moved right", engine.armadillo.pos.x > initialP1X)
        assertTrue("P2 should have moved left", engine.armadillo2!!.pos.x < initialP2X)
    }

    @Test
    fun testAllStagesUnlockedProgressionConfig() {
        assertEquals("Total stages must be 16", 16, StageConfig.allStages.size)
        val stages = StageConfig.allStages
        for (i in 1..16) {
            val s = stages.firstOrNull { it.id == i }
            assertNotNull("Stage $i must be present in StageConfig", s)
        }
    }
}
