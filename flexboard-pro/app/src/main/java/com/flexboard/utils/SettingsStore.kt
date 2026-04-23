package com.flexboard.utils

import android.content.Context
import android.content.SharedPreferences

object SettingsStore {
    private const val NAME = "flexboard_settings"

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // Theme
    const val KEY_THEME = "theme_id"
    const val KEY_KEY_BG = "color_key_bg"
    const val KEY_KEY_TEXT = "color_key_text"
    const val KEY_KB_BG = "color_kb_bg"
    const val KEY_SUGG_BG = "color_sugg_bg"
    const val KEY_PRESSED = "color_pressed"
    const val KEY_BORDER_STYLE = "border_style"
    const val KEY_KEY_OPACITY = "key_opacity"
    const val KEY_BG_IMAGE_URI = "bg_image_uri"
    const val KEY_BG_IMAGE_OPACITY = "bg_image_opacity"

    // Keyboard
    const val KEY_HAPTIC = "haptic_enabled"
    const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
    const val KEY_SOUND = "sound_enabled"
    const val KEY_SOUND_VOLUME = "sound_volume"
    const val KEY_KB_HEIGHT_PCT = "kb_height_pct"
    const val KEY_KEY_TEXT_SIZE = "key_text_size"
    const val KEY_FONT_PATH = "custom_font_path"
    const val KEY_FONT_BOLD = "font_bold"
    const val KEY_FONT_ITALIC = "font_italic"
    const val KEY_LANGUAGE = "current_language"

    // Auto-Type
    const val KEY_AT_DELAY = "autotype_delay"
    const val KEY_AT_LOOP = "autotype_loop"
    const val KEY_AT_SEND_MODE = "autotype_send_mode" // direct | paste
    const val KEY_AT_LAST_FILE = "autotype_last_file"
    const val KEY_AT_START_LINE = "autotype_start_line"

    // Suggestions / Auto-save sentence
    const val KEY_SUGGESTIONS = "suggestions_enabled"
    const val KEY_AUTOCORRECT = "autocorrect_enabled"
    const val KEY_AUTO_SAVE_SENTENCE = "auto_save_sentence"
    const val KEY_AUTO_SAVE_MIN_LEN = "auto_save_min_len"

    // Clipboard
    const val KEY_CLIP_AUTO_DELETE_DAYS = "clip_auto_delete_days"
}
