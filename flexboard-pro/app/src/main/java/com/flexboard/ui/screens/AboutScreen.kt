package com.flexboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("FlexBoard Pro", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
        Text("Version 1.0", color = Color.White)
        Text("Custom Android keyboard with Auto-Type Engine, themes, smart dictionary, macros, clipboard manager, multi-language (EN/UR/AR), and auto-saved sentences.", color = Color.White)
        Text("100% offline. No internet permission requested.", color = Color.LightGray)
        Spacer(Modifier.height(12.dp))
        Text("Modules:", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
        listOf(
            "1. IME Keyboard Service",
            "2. Auto-Type Engine",
            "3. Word Suggestions & Dictionary",
            "4. Theme Engine (6 built-in)",
            "5. Custom Font System",
            "6. Clipboard Manager",
            "7. Macros / Text Expansion",
            "8. Multi-Language (EN/UR/AR)",
            "9. Settings App",
            "+ Auto-Save Sentences"
        ).forEach { Text(it, color = Color.White) }
    }
}
