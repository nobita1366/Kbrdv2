package com.flexboard.ime

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.flexboard.utils.FontManager
import com.flexboard.utils.SettingsStore
import com.flexboard.utils.ThemeManager

class KeyboardView(
    ctx: Context,
    private val onKey: (KeyDef) -> Unit,
    private val getSuggestions: () -> List<String>,
    private val onSuggestion: (String) -> Unit,
    private val onAutoTypeOpen: () -> Unit,
    private val onClipboardOpen: () -> Unit,
    private val onSwitchLanguage: () -> Unit
) : LinearLayout(ctx) {

    private val theme = ThemeManager.current(ctx)
    private val typeface: Typeface = FontManager.loadKeyTypeface(ctx)
    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    private val prefs = SettingsStore.prefs(ctx)
    private var currentLayout: KbLayout = KeyboardLayouts.forLanguage(prefs.getString(SettingsStore.KEY_LANGUAGE, "en") ?: "en")
    private var shifted = false
    private var symbolsMode = false

    private val suggestionRow: LinearLayout
    private val toolbarRow: LinearLayout
    private val keysContainer: LinearLayout

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.keyboardBg)
        val padPx = dp(4)
        setPadding(padPx, padPx, padPx, padPx)

        toolbarRow = buildToolbar()
        suggestionRow = buildSuggestionRow()
        keysContainer = LinearLayout(ctx).apply { orientation = VERTICAL }

        addView(toolbarRow)
        addView(suggestionRow)
        addView(keysContainer)

        rebuild()
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    private fun buildToolbar(): LinearLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        setBackgroundColor(theme.suggestionBg)
        val h = dp(36)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, h)
        addToolbarButton("AT") { onAutoTypeOpen() }
        addToolbarButton("📋") { onClipboardOpen() }
        addToolbarButton("🌐") { onSwitchLanguage() }
        addToolbarButton("⚙") {
            val i = android.content.Intent(context, com.flexboard.ui.MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    private fun LinearLayout.addToolbarButton(label: String, onClick: () -> Unit) {
        val tv = TextView(context).apply {
            text = label
            setTextColor(theme.keyText)
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            textSize = 14f
            setOnClickListener { onClick() }
        }
        addView(tv, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    private fun buildSuggestionRow(): LinearLayout {
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(theme.suggestionBg)
        }
        val inner = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40))
        }
        scroll.addView(inner)
        // wrap scroll so keyboard can refresh inner contents
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40))
            addView(scroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        // store inner as tag for refresh
        container.tag = inner
        return container
    }

    fun refreshSuggestions() {
        val inner = suggestionRow.tag as LinearLayout
        inner.removeAllViews()
        val list = getSuggestions()
        if (list.isEmpty()) {
            val tv = TextView(context).apply {
                text = "FlexBoard Pro"
                setTextColor(theme.keyText)
                alpha = 0.5f
                gravity = Gravity.CENTER
                setPadding(dp(16), 0, dp(16), 0)
            }
            inner.addView(tv, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            return
        }
        list.forEach { word ->
            val tv = TextView(context).apply {
                text = word
                setTextColor(theme.keyText)
                gravity = Gravity.CENTER
                setPadding(dp(20), 0, dp(20), 0)
                setOnClickListener { onSuggestion(word) }
            }
            inner.addView(tv, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
    }

    fun setLanguage(code: String) {
        prefs.edit().putString(SettingsStore.KEY_LANGUAGE, code).apply()
        currentLayout = KeyboardLayouts.forLanguage(code)
        symbolsMode = false
        rebuild()
    }

    fun toggleShift() { shifted = !shifted; rebuild() }
    fun toggleSymbols() { symbolsMode = !symbolsMode; rebuild() }

    private fun activeLayout(): KbLayout = if (symbolsMode) KeyboardLayouts.SYMBOLS else currentLayout

    private fun rebuild() {
        keysContainer.removeAllViews()
        val rowHeight = dp(50)
        val keyTextSize = prefs.getInt(SettingsStore.KEY_KEY_TEXT_SIZE, 16).toFloat()
        val borderStyle = prefs.getString(SettingsStore.KEY_BORDER_STYLE, "rounded") ?: "rounded"
        val keyOpacity = prefs.getInt(SettingsStore.KEY_KEY_OPACITY, 100) / 100f

        activeLayout().rows.forEach { row ->
            val rowView = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, rowHeight)
            }
            row.forEach { key ->
                val tv = TextView(context).apply {
                    val display = when (key.type) {
                        KeyType.SPACE -> ""
                        else -> if (shifted && key.type == KeyType.CHAR) key.label.uppercase() else key.label
                    }
                    text = display
                    setTextColor(theme.keyText)
                    gravity = Gravity.CENTER
                    setTypeface(typeface)
                    textSize = keyTextSize
                    background = makeKeyBackground(theme.keyBg, borderStyle, keyOpacity)
                    val mPx = dp(2)
                    setPadding(mPx, mPx, mPx, mPx)
                    setOnTouchListener { v, ev ->
                        when (ev.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.background = makeKeyBackground(theme.pressedKey, borderStyle, keyOpacity)
                                doFeedback(v)
                                handleKey(key)
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.background = makeKeyBackground(theme.keyBg, borderStyle, keyOpacity)
                                true
                            }
                            else -> false
                        }
                    }
                    if (!key.longPress.isNullOrEmpty()) {
                        setOnLongClickListener {
                            onKey(key.copy(output = key.longPress.first().toString()))
                            true
                        }
                    }
                }
                val params = LayoutParams(0, LayoutParams.MATCH_PARENT, key.widthWeight)
                params.setMargins(dp(2), dp(2), dp(2), dp(2))
                rowView.addView(tv, params)
            }
            keysContainer.addView(rowView)
        }
        refreshSuggestions()
    }

    private fun handleKey(key: KeyDef) {
        when (key.type) {
            KeyType.SHIFT -> toggleShift()
            KeyType.SYMBOLS -> toggleSymbols()
            KeyType.LANGUAGE -> onSwitchLanguage()
            KeyType.AUTOTYPE -> onAutoTypeOpen()
            KeyType.CLIPBOARD -> onClipboardOpen()
            else -> {
                val out = if (shifted && key.type == KeyType.CHAR) key.output.uppercase() else key.output
                onKey(key.copy(output = out))
                if (shifted && key.type == KeyType.CHAR) { shifted = false; rebuild() }
            }
        }
    }

    private fun doFeedback(v: View) {
        if (prefs.getBoolean(SettingsStore.KEY_HAPTIC, true)) {
            val intensity = prefs.getInt(SettingsStore.KEY_HAPTIC_INTENSITY, 50)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator != null) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15L, (intensity * 2).coerceIn(1, 255)))
                } else {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            } catch (_: Exception) { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
        }
        if (prefs.getBoolean(SettingsStore.KEY_SOUND, false)) {
            v.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        }
    }

    private fun makeKeyBackground(color: Int, borderStyle: String, opacity: Float): GradientDrawable {
        val d = GradientDrawable()
        val a = (Color.alpha(color) * opacity).toInt().coerceIn(0, 255)
        d.setColor(Color.argb(a, Color.red(color), Color.green(color), Color.blue(color)))
        d.cornerRadius = when (borderStyle) {
            "none" -> 0f
            "thin" -> dp(4).toFloat()
            "thick" -> dp(2).toFloat()
            else -> dp(8).toFloat()
        }
        when (borderStyle) {
            "thin" -> d.setStroke(dp(1), Color.argb(80, 255, 255, 255))
            "thick" -> d.setStroke(dp(2), Color.argb(120, 255, 255, 255))
        }
        return d
    }
}
