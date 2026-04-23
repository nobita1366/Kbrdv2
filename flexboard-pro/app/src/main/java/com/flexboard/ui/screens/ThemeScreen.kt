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
import com.flexboard.utils.FontManager
import com.flexboard.utils.SettingsStore
import com.flexboard.utils.ThemeManager

@Composable
fun ThemeScreen(
    onPickFont: () -> Unit,
    onPickBackground: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = SettingsStore.prefs(ctx)
    var selectedTheme by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_THEME, "dark") ?: "dark") }
    var keyOpacity by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_OPACITY, 100)) }
    var border by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_BORDER_STYLE, "rounded") ?: "rounded") }
    var keyTextSize by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_TEXT_SIZE, 16)) }
    var keyHeight by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_HEIGHT_DP, 50)) }
    var bgOpacity by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_BG_IMAGE_OPACITY, 60)) }
    var haptic by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_HAPTIC, true)) }
    var sound by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_SOUND, false)) }
    var bold by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_FONT_BOLD, false)) }
    var italic by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_FONT_ITALIC, false)) }
    var fontsTick by remember { mutableStateOf(0) }
    val fonts = remember(fontsTick) { FontManager.list(ctx) }
    val activePath = remember(fontsTick) { FontManager.activePath(ctx) }
    val bgUri = remember(fontsTick) { prefs.getString(SettingsStore.KEY_BG_IMAGE_URI, "") ?: "" }

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
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(t.keyboardBg)))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(t.keyBg)))
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
                Text("Custom Background Image", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(if (bgUri.isBlank()) "No image set" else "Image: ${bgUri.takeLast(40)}", color = Color.LightGray)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onPickBackground(); fontsTick++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                        Text("Pick Image")
                    }
                    OutlinedButton(onClick = {
                        prefs.edit().remove(SettingsStore.KEY_BG_IMAGE_URI).apply(); fontsTick++
                    }) { Text("Remove") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Background opacity: $bgOpacity%", color = Color.White)
                Slider(
                    value = bgOpacity.toFloat(), valueRange = 0f..100f,
                    onValueChange = {
                        bgOpacity = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_BG_IMAGE_OPACITY, bgOpacity).apply()
                    }
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Keys & Sizing", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Border style", color = Color.White)
                Row {
                    listOf("none","thin","thick","rounded").forEach { s ->
                        FilterChip(selected = border == s, onClick = {
                            border = s; prefs.edit().putString(SettingsStore.KEY_BORDER_STYLE, s).apply()
                        }, label = { Text(s) }, modifier = Modifier.padding(end = 6.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Key opacity: $keyOpacity%", color = Color.White)
                Slider(value = keyOpacity.toFloat(), valueRange = 20f..100f, onValueChange = {
                    keyOpacity = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_OPACITY, keyOpacity).apply()
                })
                Text("Key text size: $keyTextSize sp", color = Color.White)
                Slider(value = keyTextSize.toFloat(), valueRange = 10f..28f, onValueChange = {
                    keyTextSize = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_TEXT_SIZE, keyTextSize).apply()
                })
                Text("Key / row height: $keyHeight dp", color = Color.White)
                Slider(value = keyHeight.toFloat(), valueRange = 36f..80f, onValueChange = {
                    keyHeight = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_HEIGHT_DP, keyHeight).apply()
                })
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Custom Fonts", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Import .ttf or .otf files. Selected font is used by the keyboard.", color = Color.LightGray)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onPickFont(); fontsTick++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                        Text("Add Font File")
                    }
                    OutlinedButton(onClick = { FontManager.setActive(ctx, null); fontsTick++ }) {
                        Text("Use Default")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (fonts.isEmpty()) {
                    Text("No custom fonts yet.", color = Color.LightGray)
                } else {
                    fonts.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(f.name, color = Color.White, modifier = Modifier.weight(1f))
                            RadioButton(selected = activePath == f.path, onClick = {
                                FontManager.setActive(ctx, f.path); fontsTick++
                            })
                            TextButton(onClick = { FontManager.delete(ctx, f.path); fontsTick++ }) { Text("Delete", color = Color(0xFFFF6A6A)) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = bold, onCheckedChange = {
                        bold = it; prefs.edit().putBoolean(SettingsStore.KEY_FONT_BOLD, it).apply()
                    })
                    Spacer(Modifier.width(6.dp)); Text("Bold", color = Color.White)
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = italic, onCheckedChange = {
                        italic = it; prefs.edit().putBoolean(SettingsStore.KEY_FONT_ITALIC, it).apply()
                    })
                    Spacer(Modifier.width(6.dp)); Text("Italic", color = Color.White)
                }
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

        Text("Tip: Re-open the keyboard (close & reopen any text field) to apply font/size/background changes.",
            color = Color.LightGray)
    }
}
