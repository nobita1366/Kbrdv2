package com.flexboard.ui.screens

import android.provider.Settings
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

@Composable
fun HomeScreen(onOpenImeSettings: () -> Unit, onOpenAccessibility: () -> Unit) {
    val ctx = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }

    val imeEnabled = remember(refreshTick) {
        val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
        list.contains("com.flexboard")
    }
    val imeSelected = remember(refreshTick) {
        val cur = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        cur.contains("com.flexboard")
    }
    val accessibilityOn = remember(refreshTick) {
        val flag = try { Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) } catch (_: Exception) { 0 }
        val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        flag == 1 && list.contains("com.flexboard")
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Welcome to FlexBoard Pro", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Custom Android keyboard with Auto-Type, themes, dictionary, macros, clipboard manager, emoji panel, multi-fonts, and auto-saved sentences.",
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = { refreshTick++ }) { Text("Re-check status") }
                }
            }
        }

        StepCard(
            number = "1",
            title = "Enable Keyboard",
            body = "Phone Settings > Language & Input > FlexBoard Pro toggle ON karein.",
            action = "Open Keyboard Settings",
            done = imeEnabled,
            onClick = onOpenImeSettings
        )

        StepCard(
            number = "2",
            title = "Set as default",
            body = "Kisi text field mein space-bar long-press ya keyboard switcher se 'FlexBoard Pro' choose karein.",
            action = "Open Input Picker",
            done = imeSelected,
            onClick = {
                val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
        )

        StepCard(
            number = "3",
            title = "Allow Accessibility (for Auto-Send & Auto-Type in other apps)",
            body = "Auto-Type ko WhatsApp/SMS jaise apps me message bhejne ke liye Accessibility permission chahiye.",
            action = "Open Accessibility Settings",
            done = accessibilityOn,
            onClick = onOpenAccessibility
        )

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Quick tips", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("• Keyboard ki toolbar par 😀 button = Emoji panel", color = Color.White)
                Text("• Toolbar par 'AT' button = inline Auto-Type panel (open hone par Start/Pause/Stop yahin)", color = Color.White)
                Text("• Globe 🌐 = English / Urdu / Arabic switch", color = Color.White)
                Text("• Auto-save sentences khud Sentences tab mein dikh jayengi", color = Color.White)
                Text("• Theme tab se custom background image, key size, keyboard height set karen", color = Color.White)
                Text("• Theme tab mein hi multiple fonts import & switch hote hain", color = Color.White)
            }
        }
    }
}

@Composable
private fun StepCard(number: String, title: String, body: String, action: String, done: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step $number  ", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = {},
                    label = { Text(if (done) "✓ Done" else "Pending") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (done) Color(0xFF1A4D1A) else Color(0xFF4D2A1A),
                        labelColor = if (done) Color(0xFF7CFF7C) else Color(0xFFFFB070)
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(body, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)
            ) { Text(action) }
        }
    }
}
