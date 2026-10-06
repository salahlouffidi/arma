package com.example

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArmadilloEscapeTheme
import kotlinx.coroutines.delay
import kotlin.math.hypot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArmadilloEscapeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainGameApp()
                }
            }
        }
    }
}

@Composable
fun MainGameApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ArmadilloPrefs", Context.MODE_PRIVATE) }

    var currentScreen by remember { mutableStateOf("menu") }
    var selectedRound by remember { mutableStateOf(1) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    when (currentScreen) {
        "menu" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E2C)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ARMADILLO ESCAPE",
                    color = Color.Yellow,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = { currentScreen = "stage_select" },
                    modifier = Modifier.testTag("play_button")
                ) {
                    Text("Play Game")
                }
            }
        }
        "stage_select" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E2C))
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Round (1 - 16)",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { selectedRound = 1; currentScreen = "game" }) {
                        Text("Round 1")
                    }
                    Button(onClick = { selectedRound = 9; currentScreen = "game" }) {
                        Text("Round 9")
                    }
                    Button(onClick = { showSettingsDialog = true }) {
                        Text("Round Settings")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { currentScreen = "menu" }) {
                    Text("Back to Menu")
                }
            }
        }
        "game" -> {
            GameScreen(
                round = selectedRound,
                prefs = prefs,
                onBack = { currentScreen = "stage_select" }
            )
        }
    }

    if (showSettingsDialog) {
        RoundSettingsDialog(
            prefs = prefs,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun RoundSettingsDialog(prefs: SharedPreferences, onDismiss: () -> Unit) {
    var roundNum by remember { mutableStateOf(1) }
    val key = "joystick_pos_round_$roundNum"
    val defaultPos = if (roundNum in 1..8) "LEFT" else if (roundNum in 9..16 && roundNum % 2 == 1) "LEFT" else "RIGHT"
    var currentPosString = prefs.getString(key, defaultPos) ?: defaultPos
    var currentPos by remember { mutableStateOf(currentPosString) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Round $roundNum Joystick Position") },
        text = {
            Column {
                Text("Select default joystick side for Round $roundNum:")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            currentPos = "LEFT"
                            prefs.edit().putString(key, "LEFT").apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (currentPos == "LEFT") Color.Green else Color.Gray)
                    ) {
                        Text("LEFT")
                    }
                    Button(
                        onClick = {
                            currentPos = "RIGHT"
                            prefs.edit().putString(key, "RIGHT").apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (currentPos == "RIGHT") Color.Green else Color.Gray)
                    ) {
                        Text("RIGHT")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { if (roundNum > 1) roundNum-- }) { Text("Prev Round") }
                    Text("Round $roundNum", modifier = Modifier.align(Alignment.CenterVertically))
                    Button(onClick = { if (roundNum < 16) roundNum++ }) { Text("Next Round") }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun GameScreen(round: Int, prefs: SharedPreferences, onBack: () -> Unit) {
    val defaultPos = when {
        round in 1..8 -> "LEFT"
        round in 9..16 -> if (round % 2 != 0) "LEFT" else "RIGHT"
        else -> "LEFT"
    }
    val joystickPosKey = "joystick_pos_round_$round"
    val savedPos = prefs.getString(joystickPosKey, null)
    val effectivePos = savedPos ?: defaultPos

    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var playerPos by remember { mutableStateOf(Offset(400f, 300f)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            if (joystickOffset != Offset.Zero) {
                playerPos = Offset(playerPos.x + joystickOffset.x * 0.1f, playerPos.y + joystickOffset.y * 0.1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111118))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.Cyan, radius = 25f, center = playerPos)
        }

        val joystickAlignment = if (effectivePos == "LEFT") Alignment.BottomStart else Alignment.BottomEnd

        Box(
            modifier = Modifier
                .align(joystickAlignment)
                .padding(32.dp)
                .size(120.dp)
                .background(Color.DarkGray.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            joystickOffset = offset - center
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val current = change.position - center
                            val dist = hypot(current.x, current.y)
                            val maxRadius = 50f
                            joystickOffset = if (dist > maxRadius) {
                                Offset(current.x / dist * maxRadius, current.y / dist * maxRadius)
                            } else {
                                current
                            }
                        },
                        onDragEnd = {
                            joystickOffset = Offset.Zero
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = if (round in 9..16 && effectivePos == "LEFT") Color(0xFFFFD700) else Color.Blue, radius = 20f, center = center + joystickOffset)
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .testTag("retry_button")
        ) {
            Text("Back")
        }

        Text(
            text = "Round $round ($effectivePos Joystick)",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            fontSize = 18.sp
        )
    }
}
