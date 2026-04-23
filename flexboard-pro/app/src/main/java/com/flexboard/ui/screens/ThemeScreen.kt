package com.flexboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flexboard.utils.SettingsStore
import com.flexboard.utils.ThemeManager

@Composable
fun ThemeScreen() {
    val ctx = LocalContext.current
    val prefs = SettingsStore.prefs(ctx)
    var selectedTheme by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_THEME, "dark") ?: "dark") }
    var keyOpacity by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_OPACITY, 100)) }
    var border by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_BORDER_STYLE, "rounded") ?: "rounded") }
    var keyTextSize by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_TEXT_SIZE, 16)) }
    var haptic by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_HAPTIC, true)) }
    var sound by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_SOUND, false)) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Theme & Appearance", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Built-in themes", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ThemeManager.BUILT_IN.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                .background(Color(t.keyboardBg))
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                .background(Color(t.keyBg))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(t.name, color = Color.White, modifier = Modifier.weight(1f))
                        RadioButton(selected = selectedTheme == t.id, onClick = {
                            selectedTheme = t.id; ThemeManager.setTheme(ctx, t.id)
                        })
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Keys", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Border style", color = Color.White)
                Row {
                    listOf("none","thin","thick","rounded").forEach { s ->
                        FilterChip(
                            selected = border == s, onClick = {
                                border = s; prefs.edit().putString(SettingsStore.KEY_BORDER_STYLE, s).apply()
                            },
                            label = { Text(s) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Key opacity: $keyOpacity%", color = Color.White)
                Slider(
                    value = keyOpacity.toFloat(), valueRange = 20f..100f,
                    onValueChange = {
                        keyOpacity = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_OPACITY, keyOpacity).apply()
                    }
                )
                Text("Key text size: $keyTextSize sp", color = Color.White)
                Slider(
                    value = keyTextSize.toFloat(), valueRange = 10f..28f,
                    onValueChange = {
                        keyTextSize = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_TEXT_SIZE, keyTextSize).apply()
                    }
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Feedback", color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = haptic, onCheckedChange = {
                        haptic = it; prefs.edit().putBoolean(SettingsStore.KEY_HAPTIC, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Haptic vibration", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = sound, onCheckedChange = {
                        sound = it; prefs.edit().putBoolean(SettingsStore.KEY_SOUND, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Click sound", color = Color.White)
                }
            }
        }
    }
}
