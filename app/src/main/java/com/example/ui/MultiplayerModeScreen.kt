package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CharacterId
import com.example.game.CharacterSlot
import com.example.game.GameModePreset
import com.example.game.MatchConfig
import com.example.game.RoleType
import com.example.game.SoundManager

@Composable
fun MultiplayerModeScreen(
    soundManager: SoundManager,
    initialConfig: MatchConfig = MatchConfig.oneVsOne(),
    onConfirmMatchConfig: (MatchConfig) -> Unit,
    onBackToGameModeSelect: () -> Unit
) {
    var selectedPreset by remember {
        mutableStateOf(if (initialConfig.isSolo) GameModePreset.ONE_VS_ONE else initialConfig.preset)
    }

    // Editable slots for custom configuration
    var customSlots by remember {
        mutableStateOf(
            if (initialConfig.preset == GameModePreset.CUSTOM) {
                initialConfig.slots.toMutableMap()
            } else {
                MatchConfig.twoVsTwo().slots.toMutableMap()
            }
        )
    }

    var isCustomDialogVisible by remember {
        mutableStateOf(initialConfig.preset == GameModePreset.CUSTOM)
    }

    val currentConfig: MatchConfig = remember(selectedPreset, customSlots) {
        when (selectedPreset) {
            GameModePreset.ONE_VS_ONE -> MatchConfig.oneVsOne()
            GameModePreset.TWO_VS_ONE -> MatchConfig.twoVsOne()
            GameModePreset.ONE_VS_TWO -> MatchConfig.oneVsTwo()
            GameModePreset.TWO_VS_TWO -> MatchConfig.twoVsTwo()
            GameModePreset.CUSTOM -> MatchConfig(GameModePreset.CUSTOM, customSlots.toMap())
            GameModePreset.SOLO -> MatchConfig.solo()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140D1D),
                        Color(0xFF0F0914),
                        Color(0xFF09040C)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onBackToGameModeSelect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF261833),
                        contentColor = Color(0xFFE1BEE7)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x60BA68C8)),
                    modifier = Modifier.testTag("mp_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "BACK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MULTIPLAYER CONFIGURATION",
                        color = Color(0xFFE040FB),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CHOOSE A PRESET OR CONFIGURE CUSTOM ROLES",
                        color = Color(0xFFCE93D8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Proceed Button
                Button(
                    onClick = {
                        soundManager.playMenuClick()
                        onConfirmMatchConfig(currentConfig)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE040FB),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("mp_continue_stage_select_button")
                ) {
                    Text(
                        text = "CHOOSE STAGE (1–16)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content: Presets Row + Preview Card or Custom Selector
            if (isCustomDialogVisible || selectedPreset == GameModePreset.CUSTOM) {
                // ================= CUSTOM MATCH / CHOOSE YOUR SIDE VIEW =================
                CustomRoleSelectionView(
                    slots = customSlots,
                    onSlotChanged = { id, newRole, playerNum ->
                        val updated = customSlots.toMutableMap()
                        // If this playerNum is assigned to another character, remove it from that character
                        if (newRole == RoleType.HUMAN && playerNum != null) {
                            for ((k, v) in updated) {
                                if (k != id && v.roleType == RoleType.HUMAN && v.playerNumber == playerNum) {
                                    updated[k] = v.copy(roleType = RoleType.AI, playerNumber = null)
                                }
                            }
                        }
                        updated[id] = CharacterSlot(id, newRole, if (newRole == RoleType.HUMAN) playerNum else null)
                        customSlots = updated
                        selectedPreset = GameModePreset.CUSTOM
                    },
                    onCloseCustom = {
                        isCustomDialogVisible = false
                        if (selectedPreset == GameModePreset.CUSTOM) {
                            selectedPreset = GameModePreset.ONE_VS_ONE
                        }
                    }
                )
            } else {
                // ================= PRESET CARDS GRID =================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Row of 4 Presets + 1 Custom Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PresetCard(
                            title = "1 VS 1",
                            subtitle = "ARMADILLO VS SNAKE",
                            badge = "2 PLAYERS",
                            summary = "P1: Armadillo 1\nP2: Snake 1",
                            icon = "⚔️",
                            isSelected = selectedPreset == GameModePreset.ONE_VS_ONE,
                            accentColor = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playMenuClick()
                                selectedPreset = GameModePreset.ONE_VS_ONE
                            }
                        )

                        PresetCard(
                            title = "2 VS 1",
                            subtitle = "2 ARMADILLOS VS SNAKE",
                            badge = "2P CO-OP",
                            summary = "P1: Armadillo 1\nP2: Blue Armadillo 2\nSnake: AI Predator",
                            icon = "🛡️",
                            isSelected = selectedPreset == GameModePreset.TWO_VS_ONE,
                            accentColor = Color(0xFF00E5FF),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playMenuClick()
                                selectedPreset = GameModePreset.TWO_VS_ONE
                            }
                        )

                        PresetCard(
                            title = "1 VS 2",
                            subtitle = "1 ARMADILLO VS 2 SNAKES",
                            badge = "2P HUNT",
                            summary = "P1: Armadillo 1\nP2: Snake 1\nSnake 2: AI Predator",
                            icon = "⚡",
                            isSelected = selectedPreset == GameModePreset.ONE_VS_TWO,
                            accentColor = Color(0xFF76FF03),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playMenuClick()
                                selectedPreset = GameModePreset.ONE_VS_TWO
                            }
                        )

                        PresetCard(
                            title = "2 VS 2",
                            subtitle = "2 ARMADILLOS VS 2 SNAKES",
                            badge = "4 PLAYERS",
                            summary = "P1 & P2: Armadillos\nP3 & P4: Snakes",
                            icon = "👑",
                            isSelected = selectedPreset == GameModePreset.TWO_VS_TWO,
                            accentColor = Color(0xFFE040FB),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playMenuClick()
                                selectedPreset = GameModePreset.TWO_VS_TWO
                            }
                        )

                        PresetCard(
                            title = "CUSTOM",
                            subtitle = "CHOOSE YOUR SIDE",
                            badge = "ANY ROLES",
                            summary = "Pick any roles:\nHuman P1–P4, AI, or Disabled",
                            icon = "⚙️",
                            isSelected = selectedPreset == GameModePreset.CUSTOM,
                            accentColor = Color(0xFFFFD700),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                soundManager.playMenuClick()
                                selectedPreset = GameModePreset.CUSTOM
                                isCustomDialogVisible = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selected Preset Preview & HUD Summary Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF221430),
                        border = BorderStroke(1.5.dp, Color(0x80E040FB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SELECTED: ${selectedPreset.title}",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "•",
                                    color = Color(0x60FFFFFF),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                // Active character badges
                                CharacterBadgePill(
                                    name = "🦔 A1",
                                    role = currentConfig.getSlot(CharacterId.ARMADILLO_1),
                                    baseColor = Color(0xFFFFD54F)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CharacterBadgePill(
                                    name = "🦔 A2",
                                    role = currentConfig.getSlot(CharacterId.ARMADILLO_2),
                                    baseColor = Color(0xFF40C4FF)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CharacterBadgePill(
                                    name = "🐍 S1",
                                    role = currentConfig.getSlot(CharacterId.SNAKE_1),
                                    baseColor = Color(0xFF66BB6A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CharacterBadgePill(
                                    name = "🐍 S2",
                                    role = currentConfig.getSlot(CharacterId.SNAKE_2),
                                    baseColor = Color(0xFFE040FB)
                                )
                            }

                            Button(
                                onClick = {
                                    soundManager.playMenuClick()
                                    isCustomDialogVisible = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B1E54),
                                    contentColor = Color(0xFFFFD54F)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0x80D4AF37)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "CUSTOMIZE ROLES", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    title: String,
    subtitle: String,
    badge: String,
    summary: String,
    icon: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .testTag("preset_card_$title"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A153D) else Color(0xFF1D102A)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else Color(0x40BA68C8)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Badge
            Surface(
                color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0x20FFFFFF),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0x30FFFFFF))
            ) {
                Text(
                    text = badge,
                    color = if (isSelected) accentColor else Color(0xFFCE93D8),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Icon & Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFFE1BEE7),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFB39DDB),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Summary text
            Text(
                text = summary,
                color = Color(0xFFEDE7F6),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Normal
            )

            // Selection indicator
            if (isSelected) {
                Surface(
                    color = accentColor,
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CharacterBadgePill(
    name: String,
    role: CharacterSlot,
    baseColor: Color
) {
    val (label, bg) = when (role.roleType) {
        RoleType.HUMAN -> "P${role.playerNumber}" to baseColor
        RoleType.AI -> "AI" to Color(0xFF616161)
        RoleType.DISABLED -> "OFF" to Color(0xFF37474F)
    }

    Surface(
        color = Color(0x30000000),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, bg.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = bg,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = label,
                    color = if (role.roleType == RoleType.HUMAN) Color.Black else Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomRoleSelectionView(
    slots: Map<CharacterId, CharacterSlot>,
    onSlotChanged: (CharacterId, RoleType, Int?) -> Unit,
    onCloseCustom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Title banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "CHOOSE YOUR SIDE • CUSTOM ROLE SELECTION",
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Assign each character to a Human player (P1–P4), AI predator, or Disabled",
                    color = Color(0xFFE1BEE7),
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = onCloseCustom,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B1E54),
                    contentColor = Color(0xFFFFD54F)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "VIEW PRESETS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4 Character Cards side by side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CharacterRoleCard(
                charId = CharacterId.ARMADILLO_1,
                slot = slots[CharacterId.ARMADILLO_1] ?: CharacterSlot(CharacterId.ARMADILLO_1, RoleType.HUMAN, 1),
                modifier = Modifier.weight(1f),
                onRoleSelected = { role, pNum -> onSlotChanged(CharacterId.ARMADILLO_1, role, pNum) }
            )

            CharacterRoleCard(
                charId = CharacterId.ARMADILLO_2,
                slot = slots[CharacterId.ARMADILLO_2] ?: CharacterSlot(CharacterId.ARMADILLO_2, RoleType.HUMAN, 2),
                modifier = Modifier.weight(1f),
                onRoleSelected = { role, pNum -> onSlotChanged(CharacterId.ARMADILLO_2, role, pNum) }
            )

            CharacterRoleCard(
                charId = CharacterId.SNAKE_1,
                slot = slots[CharacterId.SNAKE_1] ?: CharacterSlot(CharacterId.SNAKE_1, RoleType.AI),
                modifier = Modifier.weight(1f),
                onRoleSelected = { role, pNum -> onSlotChanged(CharacterId.SNAKE_1, role, pNum) }
            )

            CharacterRoleCard(
                charId = CharacterId.SNAKE_2,
                slot = slots[CharacterId.SNAKE_2] ?: CharacterSlot(CharacterId.SNAKE_2, RoleType.DISABLED),
                modifier = Modifier.weight(1f),
                onRoleSelected = { role, pNum -> onSlotChanged(CharacterId.SNAKE_2, role, pNum) }
            )
        }
    }
}

@Composable
private fun CharacterRoleCard(
    charId: CharacterId,
    slot: CharacterSlot,
    modifier: Modifier = Modifier,
    onRoleSelected: (RoleType, Int?) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .testTag("custom_card_${charId.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1229)
        ),
        border = BorderStroke(1.5.dp, charId.primaryColor.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = charId.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = charId.shortName,
                    color = charId.primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Current Status Banner
            val statusColor = when (slot.roleType) {
                RoleType.HUMAN -> charId.primaryColor
                RoleType.AI -> Color(0xFFB0BEC5)
                RoleType.DISABLED -> Color(0xFF78909C)
            }
            val statusText = when (slot.roleType) {
                RoleType.HUMAN -> "HUMAN (PLAYER ${slot.playerNumber})"
                RoleType.AI -> "AI CONTROLLED"
                RoleType.DISABLED -> "DISABLED / OFF"
            }

            Surface(
                color = statusColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, statusColor)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Role selection buttons: P1, P2, P3, P4
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ASSIGN HUMAN PLAYER:",
                    color = Color(0xFFB39DDB),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (p in 1..4) {
                        val isCurrent = slot.roleType == RoleType.HUMAN && slot.playerNumber == p
                        Button(
                            onClick = { onRoleSelected(RoleType.HUMAN, p) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrent) charId.primaryColor else Color(0xFF2C1C3D),
                                contentColor = if (isCurrent) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("btn_${charId.name}_P$p"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(text = "P$p", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // AI & Disabled buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onRoleSelected(RoleType.AI, null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (slot.roleType == RoleType.AI) Color(0xFF78909C) else Color(0xFF2C1C3D),
                            contentColor = if (slot.roleType == RoleType.AI) Color.Black else Color(0xFFCFD8DC)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .testTag("btn_${charId.name}_AI"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(text = "🤖 AI", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onRoleSelected(RoleType.DISABLED, null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (slot.roleType == RoleType.DISABLED) Color(0xFFD32F2F) else Color(0xFF2C1C3D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .testTag("btn_${charId.name}_OFF"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(text = "❌ OFF", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
