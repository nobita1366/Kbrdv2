package com.flexboard.utils

import android.content.Context
import android.graphics.Typeface
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class FontEntry(val name: String, val path: String)

object FontManager {

    fun list(ctx: Context): List<FontEntry> {
        val raw = SettingsStore.prefs(ctx).getString(SettingsStore.KEY_FONTS_LIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                FontEntry(o.optString("name"), o.optString("path"))
            }.filter { File(it.path).exists() }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveList(ctx: Context, list: List<FontEntry>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(JSONObject().put("name", f.name).put("path", f.path))
        }
        SettingsStore.prefs(ctx).edit().putString(SettingsStore.KEY_FONTS_LIST, arr.toString()).apply()
    }

    fun activePath(ctx: Context): String? =
        SettingsStore.prefs(ctx).getString(SettingsStore.KEY_FONT_PATH, null)?.takeIf { it.isNotBlank() && File(it).exists() }

    fun setActive(ctx: Context, path: String?) {
        SettingsStore.prefs(ctx).edit().putString(SettingsStore.KEY_FONT_PATH, path ?: "").apply()
    }

    fun importFont(ctx: Context, src: java.io.InputStream, fileName: String): FontEntry {
        val dir = File(ctx.filesDir, "fonts").apply { mkdirs() }
        // Avoid filename collisions
        val safeBase = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        var out = File(dir, safeBase)
        var n = 1
        while (out.exists()) { out = File(dir, "${n}_$safeBase"); n++ }
        src.use { input -> out.outputStream().use { input.copyTo(it) } }
        val entry = FontEntry(safeBase, out.absolutePath)
        val updated = list(ctx).toMutableList().also { it.add(entry) }
        saveList(ctx, updated)
        setActive(ctx, out.absolutePath)
        return entry
    }

    fun delete(ctx: Context, path: String) {
        File(path).takeIf { it.exists() }?.delete()
        val updated = list(ctx).filterNot { it.path == path }
        saveList(ctx, updated)
        if (activePath(ctx) == path) setActive(ctx, null)
    }

    fun loadKeyTypeface(ctx: Context): Typeface {
        val path = activePath(ctx)
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
}
