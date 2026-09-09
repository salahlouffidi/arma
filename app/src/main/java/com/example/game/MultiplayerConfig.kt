package com.example.game

import androidx.compose.ui.graphics.Color

enum class CharacterId(
    val displayName: String,
    val shortName: String,
    val icon: String,
    val primaryColor: Color,
    val accentColor: Color,
    val isSnake: Boolean
) {
    ARMADILLO_1(
        displayName = "Armadillo 1 (Golden)",
        shortName = "Armadillo 1",
        icon = "🦔",
        primaryColor = Color(0xFFFFD54F),
        accentColor = Color(0xFFD4AF37),
        isSnake = false
    ),
    ARMADILLO_2(
        displayName = "Blue Armadillo 2",
        shortName = "Armadillo 2",
        icon = "🦔",
        primaryColor = Color(0xFF40C4FF),
        accentColor = Color(0xFF0288D1),
        isSnake = false
    ),
    SNAKE_1(
        displayName = "Snake 1 (Emerald)",
        shortName = "Snake 1",
        icon = "🐍",
        primaryColor = Color(0xFF66BB6A),
        accentColor = Color(0xFF2E7D32),
        isSnake = true
    ),
    SNAKE_2(
        displayName = "Snake 2 (Amethyst)",
        shortName = "Snake 2",
        icon = "🐍",
        primaryColor = Color(0xFFE040FB),
        accentColor = Color(0xFF8E24AA),
        isSnake = true
    )
}

enum class RoleType {
    HUMAN,
    AI,
    DISABLED
}

data class CharacterSlot(
    val characterId: CharacterId,
    val roleType: RoleType,
    val playerNumber: Int? = null // 1..4 when HUMAN
)

enum class GameModePreset(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: String
) {
    SOLO("SOLO", "Play Alone (Classic Journey)", "1 PLAYER", "🦔"),
    ONE_VS_ONE("1 VS 1", "Armadillo vs Snake", "2 PLAYERS", "⚔️"),
    TWO_VS_ONE("2 VS 1", "2 Armadillos vs 1 Snake", "2P CO-OP", "🛡️"),
    ONE_VS_TWO("1 VS 2", "1 Armadillo vs 2 Snakes", "2P HUNT", "⚡"),
    TWO_VS_TWO("2 VS 2", "2 Armadillos vs 2 Snakes", "4 PLAYERS", "👑"),
    CUSTOM("CUSTOM MATCH", "Create Your Own Match", "FREE ROLES", "⚙️")
}

data class MatchConfig(
    val preset: GameModePreset = GameModePreset.SOLO,
    val slots: Map<CharacterId, CharacterSlot> = defaultSoloSlots()
) {
    val isSolo: Boolean get() = preset == GameModePreset.SOLO

    fun getSlot(id: CharacterId): CharacterSlot {
        return slots[id] ?: CharacterSlot(id, RoleType.DISABLED)
    }

    fun isCharacterActive(id: CharacterId, stage: StageConfig): Boolean {
        if (isSolo) {
            return when (id) {
                CharacterId.ARMADILLO_1 -> true
                CharacterId.ARMADILLO_2 -> stage.isDualArmadillo
                CharacterId.SNAKE_1 -> true
                CharacterId.SNAKE_2 -> stage.snakeCount >= 2
            }
        }
        return getSlot(id).roleType != RoleType.DISABLED
    }

    fun isCharacterHuman(id: CharacterId): Boolean {
        if (isSolo) {
            return id == CharacterId.ARMADILLO_1
        }
        return getSlot(id).roleType == RoleType.HUMAN
    }

    fun getPlayerForCharacter(id: CharacterId): Int? {
        if (isSolo) {
            return if (id == CharacterId.ARMADILLO_1) 1 else null
        }
        val slot = getSlot(id)
        return if (slot.roleType == RoleType.HUMAN) slot.playerNumber else null
    }

    fun getCharacterForPlayer(playerNumber: Int): CharacterId? {
        if (isSolo) {
            return if (playerNumber == 1) CharacterId.ARMADILLO_1 else null
        }
        return slots.values.firstOrNull { it.roleType == RoleType.HUMAN && it.playerNumber == playerNumber }?.characterId
    }

    /**
     * Returns active human players in order (PlayerNumber to CharacterId)
     */
    fun activeHumanPlayers(): List<Pair<Int, CharacterId>> {
        if (isSolo) {
            return listOf(1 to CharacterId.ARMADILLO_1)
        }
        return slots.values
            .filter { it.roleType == RoleType.HUMAN && it.playerNumber != null }
            .sortedBy { it.playerNumber }
            .map { it.playerNumber!! to it.characterId }
    }

    val totalHumanCount: Int
        get() = if (isSolo) 1 else slots.values.count { it.roleType == RoleType.HUMAN }

    companion object {
        fun defaultSoloSlots(): Map<CharacterId, CharacterSlot> = mapOf(
            CharacterId.ARMADILLO_1 to CharacterSlot(CharacterId.ARMADILLO_1, RoleType.HUMAN, 1),
            CharacterId.ARMADILLO_2 to CharacterSlot(CharacterId.ARMADILLO_2, RoleType.DISABLED),
            CharacterId.SNAKE_1 to CharacterSlot(CharacterId.SNAKE_1, RoleType.AI),
            CharacterId.SNAKE_2 to CharacterSlot(CharacterId.SNAKE_2, RoleType.DISABLED)
        )

        fun solo(): MatchConfig = MatchConfig(GameModePreset.SOLO, defaultSoloSlots())

        fun oneVsOne(): MatchConfig = MatchConfig(
            preset = GameModePreset.ONE_VS_ONE,
            slots = mapOf(
                CharacterId.ARMADILLO_1 to CharacterSlot(CharacterId.ARMADILLO_1, RoleType.HUMAN, 1),
                CharacterId.ARMADILLO_2 to CharacterSlot(CharacterId.ARMADILLO_2, RoleType.DISABLED),
                CharacterId.SNAKE_1 to CharacterSlot(CharacterId.SNAKE_1, RoleType.HUMAN, 2),
                CharacterId.SNAKE_2 to CharacterSlot(CharacterId.SNAKE_2, RoleType.DISABLED)
            )
        )

        fun twoVsOne(): MatchConfig = MatchConfig(
            preset = GameModePreset.TWO_VS_ONE,
            slots = mapOf(
                CharacterId.ARMADILLO_1 to CharacterSlot(CharacterId.ARMADILLO_1, RoleType.HUMAN, 1),
                CharacterId.ARMADILLO_2 to CharacterSlot(CharacterId.ARMADILLO_2, RoleType.HUMAN, 2),
                CharacterId.SNAKE_1 to CharacterSlot(CharacterId.SNAKE_1, RoleType.AI),
                CharacterId.SNAKE_2 to CharacterSlot(CharacterId.SNAKE_2, RoleType.DISABLED)
            )
        )

        fun oneVsTwo(): MatchConfig = MatchConfig(
            preset = GameModePreset.ONE_VS_TWO,
            slots = mapOf(
                CharacterId.ARMADILLO_1 to CharacterSlot(CharacterId.ARMADILLO_1, RoleType.HUMAN, 1),
                CharacterId.ARMADILLO_2 to CharacterSlot(CharacterId.ARMADILLO_2, RoleType.DISABLED),
                CharacterId.SNAKE_1 to CharacterSlot(CharacterId.SNAKE_1, RoleType.HUMAN, 2),
                CharacterId.SNAKE_2 to CharacterSlot(CharacterId.SNAKE_2, RoleType.AI)
            )
        )

        fun twoVsTwo(): MatchConfig = MatchConfig(
            preset = GameModePreset.TWO_VS_TWO,
            slots = mapOf(
                CharacterId.ARMADILLO_1 to CharacterSlot(CharacterId.ARMADILLO_1, RoleType.HUMAN, 1),
                CharacterId.ARMADILLO_2 to CharacterSlot(CharacterId.ARMADILLO_2, RoleType.HUMAN, 2),
                CharacterId.SNAKE_1 to CharacterSlot(CharacterId.SNAKE_1, RoleType.HUMAN, 3),
                CharacterId.SNAKE_2 to CharacterSlot(CharacterId.SNAKE_2, RoleType.HUMAN, 4)
            )
        )
    }
}
