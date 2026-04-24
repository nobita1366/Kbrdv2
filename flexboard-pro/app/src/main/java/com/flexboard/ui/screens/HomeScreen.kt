package com.flexboard.ui.screens

import android.content.Context
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

private fun safeImeEnabled(ctx: Context): Boolean = try {
    val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
    list.contains("com.flexboard")
} catch (_: Throwable) { false }

private fun safeImeDefault(ctx: Context): Boolean = try {
    val cur = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
    cur.contains("com.flexboard")
} catch (_: Throwable) { false }

private fun safeAccessibilityOn(ctx: Context): Boolean = try {
    val flag = try { Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) } catch (_: Throwable) { 0 }
    val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
    flag == 1 && list.contains("com.flexboard")
} catch (_: Throwable) { false }

@Composable
fun HomeScreen(onOpenImeSettings: () -> Unit, onOpenAccessibility: () -> Unit) {
    val ctx = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }

    val imeEnabled = remember(refreshTick) { safeImeEnabled(ctx) }
    val imeSelected = remember(refreshTick) { safeImeDefault(ctx) }
    val accessibilityOn = remember(refreshTick) { safeAccessibilityOn(ctx) }

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
                OutlinedButton(onClick = { refreshTick++ }) { Text("Re-check status") }
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
            body = "Kisi text field mein space-bar long-press se 'FlexBoard Pro' choose karein.",
            action = "Open Input Picker",
            done = imeSelected,
            onClick = {
                try {
                    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as? android.view.inputmethod.InputMethodManager
                    imm?.showInputMethodPicker()
                } catch (_: Throwable) {}
            }
        )

        StepCard(
            number = "3",
            title = "Allow Accessibility (for Auto-Send)",
            body = "Auto-Type ko WhatsApp/SMS jaise apps me message bhejne ke liye Accessibility chahiye.",
            action = "Open Accessibility Settings",
            done = accessibilityOn,
            onClick = onOpenAccessibility
        )

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Quick tips", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("• Toolbar par 😀 button = Emoji panel", color = Color.White)
                Text("• Toolbar par 'AT' = inline Auto-Type panel (Start/Pause/Stop yahin)", color = Color.White)
                Text("• Globe 🌐 = English / Urdu / Arabic switch", color = Color.White)
                Text("• Auto-saved sentences khud Sentences tab mein dikh jayengi", color = Color.White)
                Text("• Theme tab se background image, key size, fonts set karen", color = Color.White)
            }
        }
    }
}

@Composable
private fun StepCard(
    number: String,
    title: String,
    body: String,
    action: String,
    done: Boolean,
    onClick: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step $number  ", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                val (statusText, statusColor) = if (done)
                    "✓ Done" to Color(0xFF7CFF7C)
                else
                    "Pending" to Color(0xFFFFB070)
                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold)
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
