package com.flexboard.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import com.flexboard.utils.SettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

data class AutoTypeState(
    val running: Boolean = false,
    val paused: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val currentLine: String = "",
    val sourceName: String = "",
    val lastError: String? = null
)

object AutoTypeEngine {
    private val _state = MutableStateFlow(AutoTypeState())
    val state: StateFlow<AutoTypeState> = _state

    private var lines: List<String> = emptyList()
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Inject text into current field. Returns true on success. */
    var injector: ((String) -> Boolean)? = null
        @Synchronized set

    /** Press send / enter / IME action. Returns true on success. */
    var sender: (() -> Boolean)? = null
        @Synchronized set

    fun loadFromUri(ctx: Context, uri: Uri, displayName: String? = null): Int {
        val list = mutableListOf<String>()
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { seq ->
                seq.forEach { raw ->
                    val t = raw.trim()
                    if (t.isNotEmpty()) list.add(t)
                }
            }
        }
        lines = list
        val name = displayName ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file.txt"
        SettingsStore.prefs(ctx).edit()
            .putString(SettingsStore.KEY_AT_LAST_FILE, uri.toString())
            .putString(SettingsStore.KEY_AT_LAST_FILE_NAME, name)
            .apply()
        _state.value = _state.value.copy(total = list.size, current = 0, currentLine = "", sourceName = name)
        return list.size
    }

    fun loadFromText(ctx: Context, text: String, label: String = "Custom text") {
        lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        SettingsStore.prefs(ctx).edit().putString(SettingsStore.KEY_AT_CUSTOM_TEXT, text).apply()
        _state.value = _state.value.copy(total = lines.size, current = 0, currentLine = "", sourceName = label)
    }

    fun start(ctx: Context, startLine: Int = 0) {
        if (lines.isEmpty()) {
            _state.value = _state.value.copy(lastError = "No lines loaded")
            return
        }
        stop()
        val prefs = SettingsStore.prefs(ctx)
        val delaySec = prefs.getInt(SettingsStore.KEY_AT_DELAY, 5).coerceIn(1, 60)
        val loop = prefs.getBoolean(SettingsStore.KEY_AT_LOOP, false)
        val sendMode = prefs.getString(SettingsStore.KEY_AT_SEND_MODE, "direct") ?: "direct"
        val autoSend = prefs.getBoolean(SettingsStore.KEY_AT_AUTO_SEND, true)

        job = scope.launch {
            do {
                var i = startLine.coerceIn(0, lines.size)
                _state.value = _state.value.copy(running = true, paused = false, current = i, lastError = null)
                while (i < lines.size) {
                    while (_state.value.paused) delay(200)
                    if (!isActive) return@launch
                    val line = lines[i]
                    _state.value = _state.value.copy(current = i + 1, currentLine = line)
                    val ok = sendLine(ctx, line, sendMode)
                    if (!ok) {
                        _state.value = _state.value.copy(lastError = "Inject failed at line ${i + 1}")
                    } else if (autoSend) {
                        // small delay so target app can register the text before send
                        delay(150)
                        val sent = sender?.invoke() ?: false
                        if (!sent) {
                            _state.value = _state.value.copy(lastError = "Send failed at line ${i + 1} (no Send action found)")
                        }
                    }
                    i++
                    if (i < lines.size) delay(delaySec * 1000L)
                }
            } while (loop && isActive)
            _state.value = _state.value.copy(running = false, paused = false)
        }
    }

    private suspend fun sendLine(ctx: Context, line: String, mode: String): Boolean {
        val inj = injector
        return if (mode == "direct" && inj != null) {
            inj(line)
        } else {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("auto-type", line))
            FlexboardAccessibilityService.instance?.pasteIntoFocused() ?: false
        }
    }

    fun pause() { _state.value = _state.value.copy(paused = true) }
    fun resume() { _state.value = _state.value.copy(paused = false) }
    fun stop() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(running = false, paused = false)
    }
}
