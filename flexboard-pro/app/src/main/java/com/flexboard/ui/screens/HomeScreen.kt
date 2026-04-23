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
fun HomeScreen(onOpenImeSettings: () -> Unit, onOpenAccessibility: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Welcome to FlexBoard Pro", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Custom Android keyboard with Auto-Type, themes, dictionary, macros, clipboard manager, and auto-saved sentences. Enable below to get started.",
                    color = Color.White
                )
            }
        }
        StepCard(
            "1. Enable Keyboard",
            "Phone Settings > Language & Input > FlexBoard Pro toggle ON karein.",
            "Open Keyboard Settings"
        ) { onOpenImeSettings() }

        StepCard(
            "2. Allow Accessibility (Auto-Type)",
            "Auto-Type ko text fields mein inject karne ke liye Accessibility permission chahiye.",
            "Open Accessibility Settings"
        ) { onOpenAccessibility() }

        StepCard(
            "3. Switch keyboard",
            "Kisi bhi text field mein space bar long-press karein > FlexBoard Pro select karein.",
            "Got it"
        ) { }
    }
}

@Composable
private fun StepCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
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
