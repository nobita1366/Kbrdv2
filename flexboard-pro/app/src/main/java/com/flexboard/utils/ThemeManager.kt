package com.flexboard.utils

import android.content.Context
import android.graphics.Color

data class KeyboardTheme(
    val id: String,
    val name: String,
    val keyboardBg: Int,
    val keyBg: Int,
    val keyText: Int,
    val suggestionBg: Int,
    val pressedKey: Int,
    val accent: Int
)

object ThemeManager {
    val BUILT_IN: List<KeyboardTheme> = listOf(
        KeyboardTheme("dark", "Dark", Color.parseColor("#0D0D0D"), Color.parseColor("#1F1F1F"),
            Color.WHITE, Color.parseColor("#161616"), Color.parseColor("#FF8C00"), Color.parseColor("#FF8C00")),
        KeyboardTheme("oled", "OLED Black", Color.BLACK, Color.parseColor("#0A0A0A"),
            Color.WHITE, Color.BLACK, Color.parseColor("#FF8C00"), Color.parseColor("#FF8C00")),
        KeyboardTheme("orange_glow", "Orange Glow", Color.BLACK, Color.parseColor("#FF8C00"),
            Color.BLACK, Color.parseColor("#1A1A1A"), Color.parseColor("#FFB347"), Color.parseColor("#FF8C00")),
        KeyboardTheme("neon", "Neon Green", Color.parseColor("#0A0A0A"), Color.parseColor("#0F1F0F"),
            Color.parseColor("#39FF14"), Color.parseColor("#0A140A"), Color.parseColor("#39FF14"), Color.parseColor("#39FF14")),
        KeyboardTheme("ice", "Ice Blue", Color.parseColor("#0A1224"), Color.parseColor("#142244"),
            Color.parseColor("#9FD8FF"), Color.parseColor("#0E1A33"), Color.parseColor("#3FA9FF"), Color.parseColor("#3FA9FF")),
        KeyboardTheme("white", "Minimal White", Color.WHITE, Color.parseColor("#F0F0F0"),
            Color.BLACK, Color.parseColor("#FAFAFA"), Color.parseColor("#FF8C00"), Color.parseColor("#FF8C00"))
    )

    fun current(ctx: Context): KeyboardTheme {
        val prefs = SettingsStore.prefs(ctx)
        val id = prefs.getString(SettingsStore.KEY_THEME, "dark") ?: "dark"
        val base = BUILT_IN.firstOrNull { it.id == id } ?: BUILT_IN[0]
        return base.copy(
            keyBg = prefs.getInt(SettingsStore.KEY_KEY_BG, base.keyBg),
            keyText = prefs.getInt(SettingsStore.KEY_KEY_TEXT, base.keyText),
            keyboardBg = prefs.getInt(SettingsStore.KEY_KB_BG, base.keyboardBg),
            suggestionBg = prefs.getInt(SettingsStore.KEY_SUGG_BG, base.suggestionBg),
            pressedKey = prefs.getInt(SettingsStore.KEY_PRESSED, base.pressedKey)
        )
    }

    fun setTheme(ctx: Context, id: String) {
        SettingsStore.prefs(ctx).edit().putString(SettingsStore.KEY_THEME, id).apply()
        BUILT_IN.firstOrNull { it.id == id }?.let { t ->
            SettingsStore.prefs(ctx).edit()
                .putInt(SettingsStore.KEY_KEY_BG, t.keyBg)
                .putInt(SettingsStore.KEY_KEY_TEXT, t.keyText)
                .putInt(SettingsStore.KEY_KB_BG, t.keyboardBg)
                .putInt(SettingsStore.KEY_SUGG_BG, t.suggestionBg)
                .putInt(SettingsStore.KEY_PRESSED, t.pressedKey)
                .apply()
        }
    }
}
