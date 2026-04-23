package com.flexboard.ime

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.flexboard.utils.FontManager
import com.flexboard.utils.SettingsStore
import com.flexboard.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class PanelMode { KEYS, EMOJI, AUTOTYPE }

class KeyboardView(
    ctx: Context,
    private val onKey: (KeyDef) -> Unit,
    private val getSuggestions: () -> List<String>,
    private val onSuggestion: (String) -> Unit,
    private val onClipboardOpen: () -> Unit,
    private val onSwitchLanguage: () -> Unit,
    private val onOpenSettings: () -> Unit
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
    private var panelMode: PanelMode = PanelMode.KEYS

    private val suggestionRow: LinearLayout
    private val toolbarRow: LinearLayout
    private val panelContainer: FrameLayout
    private var atObserver: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        orientation = VERTICAL
        applyKeyboardBackground()
        val padPx = dp(4)
        setPadding(padPx, padPx, padPx, padPx)

        toolbarRow = buildToolbar()
        suggestionRow = buildSuggestionRow()
        panelContainer = FrameLayout(ctx)

        addView(toolbarRow)
        addView(suggestionRow)
        addView(panelContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        rebuild()
    }

    private fun applyKeyboardBackground() {
        val bgUri = prefs.getString(SettingsStore.KEY_BG_IMAGE_URI, null)
        if (!bgUri.isNullOrBlank()) {
            try {
                val opacity = (prefs.getInt(SettingsStore.KEY_BG_IMAGE_OPACITY, 60) / 100f).coerceIn(0f, 1f)
                val ins = context.contentResolver.openInputStream(Uri.parse(bgUri))
                if (ins != null) {
                    val bmp = BitmapFactory.decodeStream(ins)
                    ins.close()
                    if (bmp != null) {
                        val baseColor = ColorDrawable(theme.keyboardBg)
                        val img = BitmapDrawable(resources, bmp).apply {
                            alpha = (opacity * 255).toInt().coerceIn(0, 255)
                            setTileModeXY(android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
                            gravity = Gravity.CENTER or Gravity.FILL
                        }
                        background = LayerDrawable(arrayOf(baseColor, img))
                        return
                    }
                }
            } catch (_: Exception) {}
        }
        setBackgroundColor(theme.keyboardBg)
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    private fun buildToolbar(): LinearLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        setBackgroundColor(theme.suggestionBg)
        val h = dp(36)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, h)
        addToolbarButton("AT") { showAutoTypePanel() }
        addToolbarButton("📋") { onClipboardOpen() }
        addToolbarButton("🌐") { onSwitchLanguage() }
        addToolbarButton("😀") { showEmojiPanel() }
        addToolbarButton("⌨") { showKeysPanel() }
        addToolbarButton("⚙") { onOpenSettings() }
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
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40))
            addView(scroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
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
        showKeysPanel()
    }

    fun toggleShift() { shifted = !shifted; if (panelMode == PanelMode.KEYS) rebuild() }
    fun toggleSymbols() { symbolsMode = !symbolsMode; if (panelMode == PanelMode.KEYS) rebuild() }

    private fun activeLayout(): KbLayout = if (symbolsMode) KeyboardLayouts.SYMBOLS else currentLayout

    private fun showKeysPanel() {
        panelMode = PanelMode.KEYS
        atObserver?.cancel(); atObserver = null
        rebuild()
    }

    private fun showEmojiPanel() {
        panelMode = PanelMode.EMOJI
        atObserver?.cancel(); atObserver = null
        rebuildEmoji()
    }

    private fun showAutoTypePanel() {
        panelMode = PanelMode.AUTOTYPE
        rebuildAutoType()
        atObserver?.cancel()
        atObserver = scope.launch {
            AutoTypeEngine.state.collectLatest { rebuildAutoType() }
        }
    }

    private fun rebuild() {
        panelContainer.removeAllViews()
        val keyHeight = prefs.getInt(SettingsStore.KEY_KEY_HEIGHT_DP, 50).coerceIn(36, 80)
        val rowHeight = dp(keyHeight)
        val keyTextSize = prefs.getInt(SettingsStore.KEY_KEY_TEXT_SIZE, 16).toFloat()
        val borderStyle = prefs.getString(SettingsStore.KEY_BORDER_STYLE, "rounded") ?: "rounded"
        val keyOpacity = prefs.getInt(SettingsStore.KEY_KEY_OPACITY, 100) / 100f

        val keys = LinearLayout(context).apply { orientation = VERTICAL }

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
                                handleKeyTap(key)
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
            keys.addView(rowView)
        }
        panelContainer.addView(keys, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        refreshSuggestions()
    }

    private fun handleKeyTap(key: KeyDef) {
        when (key.type) {
            KeyType.SHIFT -> toggleShift()
            KeyType.SYMBOLS -> toggleSymbols()
            KeyType.LANGUAGE -> onSwitchLanguage()
            KeyType.EMOJI -> showEmojiPanel()
            KeyType.CLIPBOARD -> onClipboardOpen()
            else -> {
                val out = if (shifted && key.type == KeyType.CHAR) key.output.uppercase() else key.output
                onKey(key.copy(output = out))
                if (shifted && key.type == KeyType.CHAR) { shifted = false; rebuild() }
            }
        }
    }

    // ===== EMOJI PANEL =====
    private fun rebuildEmoji() {
        panelContainer.removeAllViews()
        val keyHeight = prefs.getInt(SettingsStore.KEY_KEY_HEIGHT_DP, 50).coerceIn(36, 80)
        val totalH = dp(keyHeight) * 5  // 5 rows tall
        val root = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, totalH)
        }

        // Category strip
        val catScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(theme.suggestionBg)
        }
        val catRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        val grid = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        fun loadCategory(emojis: List<String>) {
            grid.removeAllViews()
            val perRow = 8
            var rowView: LinearLayout? = null
            emojis.forEachIndexed { idx, e ->
                if (idx % perRow == 0) {
                    rowView = LinearLayout(context).apply {
                        orientation = HORIZONTAL
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
                    }
                    grid.addView(rowView)
                }
                val tv = TextView(context).apply {
                    text = e
                    textSize = 22f
                    gravity = Gravity.CENTER
                    setTextColor(theme.keyText)
                    setOnClickListener {
                        onKey(KeyDef(label = e, output = e, type = KeyType.CHAR))
                        addRecentEmoji(e)
                    }
                }
                rowView!!.addView(tv, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
        }

        // Recent first
        val recents = recentEmojis()
        if (recents.isNotEmpty()) {
            val btn = catTab("⏱") {
                loadCategory(recents)
            }
            catRow.addView(btn)
        }
        EmojiData.CATEGORIES.forEach { cat ->
            val btn = catTab(cat.emojis.firstOrNull() ?: "★") {
                loadCategory(cat.emojis)
            }
            catRow.addView(btn)
        }

        // Default to first category
        loadCategory(if (recents.isNotEmpty()) recents else EmojiData.CATEGORIES.first().emojis)

        catScroll.addView(catRow)
        root.addView(catScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(40)))

        val scroll = ScrollView(context)
        scroll.addView(grid)
        root.addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        // bottom bar with backspace + return
        val bottom = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
            setBackgroundColor(theme.suggestionBg)
        }
        val abc = TextView(context).apply {
            text = "ABC"
            setTextColor(theme.keyText)
            gravity = Gravity.CENTER
            setOnClickListener { showKeysPanel() }
        }
        val back = TextView(context).apply {
            text = "⌫"
            setTextColor(theme.keyText)
            gravity = Gravity.CENTER
            setOnClickListener { onKey(KeyDef("⌫", "back", type = KeyType.BACKSPACE)) }
        }
        val space = TextView(context).apply {
            text = "space"
            setTextColor(theme.keyText)
            gravity = Gravity.CENTER
            setOnClickListener { onKey(KeyDef("space", " ", type = KeyType.SPACE)) }
        }
        val enter = TextView(context).apply {
            text = "⏎"
            setTextColor(theme.keyText)
            gravity = Gravity.CENTER
            setOnClickListener { onKey(KeyDef("⏎", "enter", type = KeyType.ENTER)) }
        }
        bottom.addView(abc, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        bottom.addView(space, LayoutParams(0, LayoutParams.MATCH_PARENT, 4f))
        bottom.addView(back, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        bottom.addView(enter, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        root.addView(bottom)

        panelContainer.addView(root, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun catTab(label: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = label
        textSize = 18f
        setTextColor(theme.keyText)
        gravity = Gravity.CENTER
        setPadding(dp(14), 0, dp(14), 0)
        setOnClickListener { onClick() }
    }

    private fun recentEmojis(): List<String> {
        return try {
            val s = prefs.getString(SettingsStore.KEY_EMOJI_RECENT, "[]") ?: "[]"
            val a = JSONArray(s)
            (0 until a.length()).map { a.getString(it) }.take(24)
        } catch (_: Exception) { emptyList() }
    }

    private fun addRecentEmoji(e: String) {
        val cur = recentEmojis().toMutableList()
        cur.remove(e); cur.add(0, e)
        val arr = JSONArray()
        cur.take(24).forEach { arr.put(it) }
        prefs.edit().putString(SettingsStore.KEY_EMOJI_RECENT, arr.toString()).apply()
    }

    // ===== AUTO-TYPE PANEL =====
    private fun rebuildAutoType() {
        panelContainer.removeAllViews()
        val pad = dp(10)
        val keyHeight = prefs.getInt(SettingsStore.KEY_KEY_HEIGHT_DP, 50).coerceIn(36, 80)
        val totalH = dp(keyHeight) * 5

        val root = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, totalH)
            setPadding(pad, pad, pad, pad)
        }

        val state = AutoTypeEngine.state.value
        val delaySec = prefs.getInt(SettingsStore.KEY_AT_DELAY, 5)
        val loop = prefs.getBoolean(SettingsStore.KEY_AT_LOOP, false)
        val autoSend = prefs.getBoolean(SettingsStore.KEY_AT_AUTO_SEND, true)

        fun mkLabel(text: String): TextView = TextView(context).apply {
            this.text = text
            setTextColor(theme.keyText)
            textSize = 13f
        }

        val title = TextView(context).apply {
            text = "Auto-Type Panel"
            setTextColor(theme.accent)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(title)

        val statusText = if (state.total == 0) "No file/text loaded" else "${state.sourceName} · ${state.total} lines"
        root.addView(mkLabel(statusText))

        val progress = TextView(context).apply {
            text = if (state.running) {
                "▶ ${state.current}/${state.total}  · ${if (state.paused) "PAUSED" else "RUNNING"}"
            } else "⏹ Stopped"
            setTextColor(theme.keyText)
            textSize = 12f
        }
        root.addView(progress)

        val curLine = TextView(context).apply {
            text = if (state.currentLine.isNotEmpty()) "→ ${state.currentLine}" else " "
            setTextColor(Color.LTGRAY)
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        root.addView(curLine)

        // Settings row 1: delay
        val rowDelay = LinearLayout(context).apply { orientation = HORIZONTAL; layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)) }
        rowDelay.addView(mkLabel("Delay: ${delaySec}s "), LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0f))
        val minus = pillBtn("-") {
            val v = (prefs.getInt(SettingsStore.KEY_AT_DELAY, 5) - 1).coerceAtLeast(1)
            prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, v).apply(); rebuildAutoType()
        }
        val plus = pillBtn("+") {
            val v = (prefs.getInt(SettingsStore.KEY_AT_DELAY, 5) + 1).coerceAtMost(60)
            prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, v).apply(); rebuildAutoType()
        }
        rowDelay.addView(minus, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        rowDelay.addView(plus, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        val loopBtn = pillBtn(if (loop) "Loop ✓" else "Loop ✗") {
            prefs.edit().putBoolean(SettingsStore.KEY_AT_LOOP, !loop).apply(); rebuildAutoType()
        }
        val sendBtn = pillBtn(if (autoSend) "AutoSend ✓" else "AutoSend ✗") {
            prefs.edit().putBoolean(SettingsStore.KEY_AT_AUTO_SEND, !autoSend).apply(); rebuildAutoType()
        }
        rowDelay.addView(loopBtn, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.4f))
        rowDelay.addView(sendBtn, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.6f))
        root.addView(rowDelay)

        // Action row: Start/Pause/Stop/Open app
        val rowAct = LinearLayout(context).apply { orientation = HORIZONTAL; layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(48)) }
        val start = bigBtn("▶ Start", theme.accent) {
            AutoTypeForegroundService.start(context.applicationContext)
            AutoTypeEngine.start(context.applicationContext, prefs.getInt(SettingsStore.KEY_AT_START_LINE, 0))
        }
        val pause = bigBtn(if (state.paused) "▶ Resume" else "⏸ Pause", theme.keyBg) {
            if (state.paused) AutoTypeEngine.resume() else AutoTypeEngine.pause()
        }
        val stop = bigBtn("■ Stop", theme.keyBg) {
            AutoTypeEngine.stop()
            AutoTypeForegroundService.stop(context.applicationContext)
        }
        val openApp = bigBtn("⚙ Open", theme.keyBg) {
            // open app to load/manage files
            val i = android.content.Intent(context, com.flexboard.ui.MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("section", "autotype")
            context.startActivity(i)
        }
        rowAct.addView(start, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.4f))
        rowAct.addView(pause, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        rowAct.addView(stop, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        rowAct.addView(openApp, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        root.addView(rowAct)

        val backRow = LinearLayout(context).apply { orientation = HORIZONTAL; layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)) }
        val back = pillBtn("⌨ Back to keys") { showKeysPanel() }
        backRow.addView(back, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        root.addView(backRow)

        state.lastError?.let {
            root.addView(TextView(context).apply {
                text = "⚠ $it"
                setTextColor(Color.parseColor("#FF6A6A"))
                textSize = 11f
            })
        }

        panelContainer.addView(root, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun pillBtn(label: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = label
        setTextColor(theme.keyText)
        gravity = Gravity.CENTER
        textSize = 13f
        setPadding(dp(8), dp(6), dp(8), dp(6))
        background = makeKeyBackground(theme.keyBg, "rounded", 1f)
        val lp = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(2), dp(4), dp(2), dp(4)) }
        layoutParams = lp
        setOnClickListener { onClick() }
    }

    private fun bigBtn(label: String, bg: Int, onClick: () -> Unit): TextView = TextView(context).apply {
        text = label
        setTextColor(if (bg == theme.accent) Color.BLACK else theme.keyText)
        gravity = Gravity.CENTER
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        background = makeKeyBackground(bg, "rounded", 1f)
        val lp = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) }
        layoutParams = lp
        setOnClickListener { onClick() }
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        atObserver?.cancel()
        scope.cancel()
    }
}
