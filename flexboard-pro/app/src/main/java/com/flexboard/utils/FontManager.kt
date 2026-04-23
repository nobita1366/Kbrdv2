package com.flexboard.utils

import android.content.Context
import android.graphics.Typeface
import java.io.File

object FontManager {
    fun loadKeyTypeface(ctx: Context): Typeface {
        val path = SettingsStore.prefs(ctx).getString(SettingsStore.KEY_FONT_PATH, null)
        val base = try {
            if (!path.isNullOrBlank() && File(path).exists()) Typeface.createFromFile(path)
            else Typeface.DEFAULT
        } catch (e: Exception) { Typeface.DEFAULT }
        val bold = SettingsStore.prefs(ctx).getBoolean(SettingsStore.KEY_FONT_BOLD, false)
        val italic = SettingsStore.prefs(ctx).getBoolean(SettingsStore.KEY_FONT_ITALIC, false)
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return Typeface.create(base, style)
    }

    fun importFont(ctx: Context, src: java.io.InputStream, fileName: String): String {
        val dir = File(ctx.filesDir, "fonts").apply { mkdirs() }
        val out = File(dir, fileName)
        src.use { input -> out.outputStream().use { input.copyTo(it) } }
        SettingsStore.prefs(ctx).edit().putString(SettingsStore.KEY_FONT_PATH, out.absolutePath).apply()
        return out.absolutePath
    }
}
