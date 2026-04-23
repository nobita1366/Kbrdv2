package com.flexboard.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.flexboard.utils.ClipboardWatcher
import com.flexboard.utils.SettingsStore
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

class FlexboardIMEService : InputMethodService() {

    private var keyboardView: KeyboardView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clipboardWatcher: ClipboardWatcher? = null
    private var currentSuggestions: List<String> = emptyList()
    private val sentenceBuf = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        clipboardWatcher = ClipboardWatcher(this).also { it.start() }
        MacroEngine.preload(this)
        scope.launch(Dispatchers.IO) { SuggestionEngine.ensureSeeded(this@FlexboardIMEService) }
        // Hook auto-type direct injection through IME's input connection
        AutoTypeEngine.injector = { text ->
            val ic = currentInputConnection
            if (ic != null) {
                ic.commitText(text, 1)
                true
            } else {
                FlexboardAccessibilityService.instance?.typeIntoFocused(text) ?: false
            }
        }
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(
            ctx = this,
            onKey = { handleKey(it) },
            getSuggestions = { currentSuggestions },
            onSuggestion = { commitSuggestion(it) },
            onAutoTypeOpen = { openSettingsTo("autotype") },
            onClipboardOpen = { openSettingsTo("clipboard") },
            onSwitchLanguage = { switchLanguage() }
        )
        keyboardView = view
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        sentenceBuf.clear()
        updateSuggestions("")
    }

    override fun onFinishInput() {
        // auto-save sentence on focus loss
        flushSentence()
        super.onFinishInput()
    }

    override fun onDestroy() {
        flushSentence()
        AutoTypeEngine.injector = null
        clipboardWatcher?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun handleKey(key: KeyDef) {
        val ic = currentInputConnection ?: return
        when (key.type) {
            KeyType.BACKSPACE -> {
                ic.deleteSurroundingText(1, 0)
                if (sentenceBuf.isNotEmpty()) sentenceBuf.deleteCharAt(sentenceBuf.length - 1)
            }
            KeyType.ENTER -> {
                flushSentence()
                ic.commitText("\n", 1)
            }
            KeyType.SPACE -> {
                // macro check
                val before = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
                val expansion = MacroEngine.checkExpansion(before)
                if (expansion != null) {
                    val (token, replacement) = expansion
                    ic.deleteSurroundingText(token.length, 0)
                    ic.commitText(replacement + " ", 1)
                    sentenceBuf.append(replacement).append(' ')
                } else {
                    ic.commitText(" ", 1)
                    sentenceBuf.append(' ')
                }
                // learn the last word
                val lastWord = before.takeLastWhile { it.isLetterOrDigit() }
                if (lastWord.isNotEmpty()) scope.launch(Dispatchers.IO) {
                    SuggestionEngine.learnWord(this@FlexboardIMEService, lastWord)
                }
                // sentence terminator detection? handled in append below
                updateSuggestions("")
            }
            KeyType.PERIOD, KeyType.COMMA -> {
                ic.commitText(key.output, 1)
                sentenceBuf.append(key.output)
                if (key.output == "." || key.output == "?" || key.output == "!" || key.output == "۔") {
                    flushSentence()
                }
            }
            else -> {
                ic.commitText(key.output, 1)
                sentenceBuf.append(key.output)
                val before = ic.getTextBeforeCursor(32, 0)?.toString() ?: ""
                val partial = before.takeLastWhile { it.isLetterOrDigit() }
                updateSuggestions(partial)
            }
        }
    }

    private fun commitSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(32, 0)?.toString() ?: ""
        val partial = before.takeLastWhile { it.isLetterOrDigit() }
        if (partial.isNotEmpty()) ic.deleteSurroundingText(partial.length, 0)
        ic.commitText("$word ", 1)
        sentenceBuf.append(word).append(' ')
        scope.launch(Dispatchers.IO) { SuggestionEngine.learnWord(this@FlexboardIMEService, word) }
        updateSuggestions("")
    }

    private fun updateSuggestions(prefix: String) {
        if (!SettingsStore.prefs(this).getBoolean(SettingsStore.KEY_SUGGESTIONS, true)) {
            currentSuggestions = emptyList()
            keyboardView?.refreshSuggestions()
            return
        }
        scope.launch {
            val ic = currentInputConnection
            val before = ic?.getTextBeforeCursor(64, 0)?.toString() ?: ""
            val prevWord = before.trimEnd().split(Regex("\\s+")).lastOrNull()?.takeIf { it.isNotBlank() }
            val list = withContext(Dispatchers.IO) {
                SuggestionEngine.suggest(this@FlexboardIMEService, prefix, prevWord)
            }
            currentSuggestions = list
            keyboardView?.refreshSuggestions()
        }
    }

    private fun flushSentence() {
        val s = sentenceBuf.toString().trim()
        sentenceBuf.clear()
        if (s.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            SuggestionEngine.learnSentence(this@FlexboardIMEService, s)
        }
    }

    private fun switchLanguage() {
        val cur = SettingsStore.prefs(this).getString(SettingsStore.KEY_LANGUAGE, "en") ?: "en"
        val next = KeyboardLayouts.nextLanguage(cur)
        keyboardView?.setLanguage(next)
    }

    private fun openSettingsTo(section: String) {
        val i = Intent(this, com.flexboard.ui.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("section", section)
        startActivity(i)
    }

    companion object {
        var instance: WeakReference<FlexboardIMEService>? = null
    }
}
