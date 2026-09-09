package com.example.game

import android.content.Context
import android.content.SharedPreferences

class StageProgressManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("armadillo_escape_progress", Context.MODE_PRIVATE)

    fun getHighestUnlockedStage(): Int {
        // ALL 16 STAGES UNLOCKED FROM THE BEGINNING
        return 16
    }

    fun isStageUnlocked(stageId: Int): Boolean {
        // All 16 stages are available immediately
        return true
    }

    fun unlockStage(stageId: Int) {
        val current = getHighestUnlockedStage()
        if (stageId > current) {
            prefs.edit().putInt(KEY_HIGHEST_UNLOCKED, stageId.coerceIn(1, 16)).apply()
        }
    }

    fun isStageCompleted(stageId: Int): Boolean {
        return prefs.getBoolean(KEY_STAGE_COMPLETED_PREFIX + stageId, false)
    }

    fun completeStage(stageId: Int) {
        markStageCompleted(stageId)
    }

    fun markStageCompleted(stageId: Int) {
        prefs.edit().putBoolean(KEY_STAGE_COMPLETED_PREFIX + stageId, true).apply()
        if (stageId < 16) {
            unlockStage(stageId + 1)
        }
    }

    fun getStageHighScore(stageId: Int): Int {
        return prefs.getInt(KEY_STAGE_HIGH_SCORE_PREFIX + stageId, 0)
    }

    fun recordStageScore(stageId: Int, score: Int): Boolean {
        val current = getStageHighScore(stageId)
        val isNewRecord = score > current
        if (isNewRecord) {
            saveStageHighScore(stageId, score)
        }
        return isNewRecord
    }

    fun saveStageHighScore(stageId: Int, score: Int) {
        val current = getStageHighScore(stageId)
        if (score > current) {
            prefs.edit().putInt(KEY_STAGE_HIGH_SCORE_PREFIX + stageId, score).apply()
        }
    }

    fun resetAllProgress() {
        prefs.edit().clear().putInt(KEY_HIGHEST_UNLOCKED, 1).apply()
    }

    companion object {
        private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked_stage"
        private const val KEY_STAGE_COMPLETED_PREFIX = "stage_completed_"
        private const val KEY_STAGE_HIGH_SCORE_PREFIX = "stage_high_score_"
    }
}
