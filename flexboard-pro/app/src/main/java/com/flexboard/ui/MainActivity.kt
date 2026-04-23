package com.flexboard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.flexboard.ime.AutoTypeEngine
import com.flexboard.ime.AutoTypeForegroundService
import com.flexboard.ui.screens.*
import com.flexboard.ui.theme.FlexboardTheme

class MainActivity : ComponentActivity() {

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val name = queryDisplayName(it)
            AutoTypeEngine.loadFromUri(this, it, name)
        }
    }

    private val pickFont = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            contentResolver.openInputStream(it)?.let { input ->
                val name = queryDisplayName(it) ?: "custom-${System.currentTimeMillis()}.ttf"
                com.flexboard.utils.FontManager.importFont(this, input, name)
            }
        }
    }

    private val pickBg = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            com.flexboard.utils.SettingsStore.prefs(this).edit()
                .putString(com.flexboard.utils.SettingsStore.KEY_BG_IMAGE_URI, it.toString())
                .apply()
        }
    }

    private fun queryDisplayName(uri: android.net.Uri): String? {
        val proj = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        return contentResolver.query(uri, proj, null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialSection = intent?.getStringExtra("section")
        setContent {
            FlexboardTheme {
                AppRoot(
                    initialSection = initialSection,
                    onPickTxt = { pickFile.launch(arrayOf("text/plain", "*/*")) },
                    onPickFont = { pickFont.launch(arrayOf("*/*")) },
                    onPickBg = { pickBg.launch(arrayOf("image/*")) },
                    onOpenImeSettings = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onStartAutoType = {
                        AutoTypeForegroundService.start(this)
                        AutoTypeEngine.start(this, com.flexboard.utils.SettingsStore.prefs(this).getInt(com.flexboard.utils.SettingsStore.KEY_AT_START_LINE, 0))
                    },
                    onPause = { AutoTypeEngine.pause() },
                    onResume = { AutoTypeEngine.resume() },
                    onStop = {
                        AutoTypeEngine.stop()
                        AutoTypeForegroundService.stop(this)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    initialSection: String?,
    onPickTxt: () -> Unit,
    onPickFont: () -> Unit,
    onPickBg: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onStartAutoType: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val tabs = listOf("Home", "Auto-Type", "Theme", "Dictionary", "Macros", "Sentences", "Clipboard", "About")
    val initialIdx = when (initialSection) {
        "autotype" -> 1
        "theme" -> 2
        "clipboard" -> 6
        else -> 0
    }
    var selected by rememberSaveable { mutableStateOf(initialIdx) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FlexBoard Pro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D0D),
                    titleContentColor = Color(0xFFFF8C00)
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D0D0D)) {
                tabs.forEachIndexed { i, name ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = {
                            val ic = when (name) {
                                "Home" -> Icons.Default.Home
                                "Auto-Type" -> Icons.Default.PlayArrow
                                "Theme" -> Icons.Default.Palette
                                "Dictionary" -> Icons.Default.Book
                                "Macros" -> Icons.Default.Bolt
                                "Sentences" -> Icons.Default.Notes
                                "Clipboard" -> Icons.Default.ContentPaste
                                else -> Icons.Default.Info
                            }
                            Icon(ic, contentDescription = name)
                        },
                        label = { Text(name) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF8C00),
                            selectedTextColor = Color(0xFFFF8C00),
                            indicatorColor = Color(0xFF1F1F1F),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
        ) {
            when (selected) {
                0 -> HomeScreen(onOpenImeSettings, onOpenAccessibility)
                1 -> AutoTypeScreen(onPickTxt, onStartAutoType, onPause, onResume, onStop)
                2 -> ThemeScreen(onPickFont = onPickFont, onPickBackground = onPickBg)
                3 -> DictionaryScreen()
                4 -> MacroScreen()
                5 -> SavedSentencesScreen()
                6 -> ClipboardScreen()
                7 -> AboutScreen()
            }
        }
    }
}
