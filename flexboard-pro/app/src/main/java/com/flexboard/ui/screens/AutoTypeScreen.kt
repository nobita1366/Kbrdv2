package com.flexboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flexboard.ime.AutoTypeEngine
import com.flexboard.utils.SettingsStore

@Composable
fun AutoTypeScreen(
    onPickTxt: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = SettingsStore.prefs(ctx)
    var delay by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_AT_DELAY, 5)) }
    var loop by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_AT_LOOP, false)) }
    var autoSend by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_AT_AUTO_SEND, true)) }
    var sendMode by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_AT_SEND_MODE, "direct") ?: "direct") }
    var startLine by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_AT_START_LINE, 0)) }
    var customText by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_AT_CUSTOM_TEXT, "") ?: "") }

    val state by AutoTypeEngine.state.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Auto-Type Engine", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Source A · Text file (.txt)", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Button(onClick = onPickTxt, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                    Text("Load .txt File")
                }
                Spacer(Modifier.height(6.dp))
                val name = state.sourceName.ifBlank { "—" }
                Text("Loaded: $name · ${state.total} lines", color = Color.White)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Source B · Custom write text", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Type or paste lines (one message per line)") },
                    minLines = 4, maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        AutoTypeEngine.loadFromText(ctx, customText, "Custom text")
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                        Text("Use this text")
                    }
                    OutlinedButton(onClick = { customText = "" }) { Text("Clear") }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Settings", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Delay between messages: $delay sec", color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        delay = (delay - 1).coerceAtLeast(1); prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, delay).apply()
                    }) { Text("-") }
                    Slider(
                        value = delay.toFloat(),
                        onValueChange = { delay = it.toInt().coerceIn(1, 60); prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, delay).apply() },
                        valueRange = 1f..60f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    OutlinedButton(onClick = {
                        delay = (delay + 1).coerceAtMost(60); prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, delay).apply()
                    }) { Text("+") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoSend, onCheckedChange = {
                        autoSend = it; prefs.edit().putBoolean(SettingsStore.KEY_AT_AUTO_SEND, it).apply()
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-send after each line (press Send/Enter)", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = loop, onCheckedChange = {
                        loop = it; prefs.edit().putBoolean(SettingsStore.KEY_AT_LOOP, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Loop mode", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Text("Send mode", color = Color.White)
                Row {
                    FilterChip(selected = sendMode == "direct", onClick = {
                        sendMode = "direct"; prefs.edit().putString(SettingsStore.KEY_AT_SEND_MODE, sendMode).apply()
                    }, label = { Text("Direct (IME)") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = sendMode == "paste", onClick = {
                        sendMode = "paste"; prefs.edit().putString(SettingsStore.KEY_AT_SEND_MODE, sendMode).apply()
                    }, label = { Text("Paste (Accessibility)") })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = startLine.toString(),
                    onValueChange = {
                        startLine = it.toIntOrNull() ?: 0
                        prefs.edit().putInt(SettingsStore.KEY_AT_START_LINE, startLine).apply()
                    },
                    label = { Text("Start from line (0 = beginning)") },
                    singleLine = true
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                val total = state.total.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { (state.current.toFloat() / total).coerceIn(0f, 1f) },
                    color = Color(0xFFFF8C00),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("Message ${state.current} of ${state.total}", color = Color.White)
                if (state.currentLine.isNotEmpty()) Text("> ${state.currentLine}", color = Color.LightGray)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart, enabled = !state.running,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)
                    ) { Text("Start") }
                    if (state.paused) {
                        Button(onClick = onResume) { Text("Resume") }
                    } else {
                        Button(onClick = onPause, enabled = state.running) { Text("Pause") }
                    }
                    OutlinedButton(onClick = onStop) { Text("Stop") }
                }
                state.lastError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color(0xFFFF6A6A))
                }
            }
        }

        Text(
            "Tip: Auto-send works best with Accessibility ON. Direct (IME) mode types into the focused field; if the target app exposes a Send IME action (chat apps usually do), it's pressed. Otherwise FlexBoard finds and clicks the Send button via Accessibility.",
            color = Color.LightGray
        )
    }
}
