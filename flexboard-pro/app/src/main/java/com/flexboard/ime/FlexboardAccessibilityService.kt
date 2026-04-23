package com.flexboard.ime

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FlexboardAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* not needed */ }
    override fun onInterrupt() {}

    fun typeIntoFocused(text: String): Boolean {
        val node = findFocusedEditable() ?: return false
        val existing = node.text?.toString() ?: ""
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                existing + text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun pasteIntoFocused(): Boolean {
        val node = findFocusedEditable() ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    /** Try to click the visible "Send" button (WhatsApp/Messenger/Telegram/SMS).
     * Heuristic: searches for clickable nodes whose contentDescription/text matches Send synonyms. */
    fun pressSend(): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findSendNode(root) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private val sendKeywords = listOf(
        "send", "send message", "sent", "submit",
        "بھیجیں", "ارسال", "إرسال", "ارسل"
    )

    private fun findSendNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val cd = node.contentDescription?.toString()?.lowercase() ?: ""
        val tx = node.text?.toString()?.lowercase() ?: ""
        val matches = sendKeywords.any { kw ->
            val k = kw.lowercase()
            cd == k || cd.startsWith(k) || tx == k
        }
        if (matches && (node.isClickable || node.isEnabled)) {
            // Walk up to find a clickable ancestor if needed
            var n: AccessibilityNodeInfo? = node
            while (n != null && !n.isClickable) n = n.parent
            return n ?: node
        }
        for (i in 0 until node.childCount) {
            val r = findSendNode(node.getChild(i))
            if (r != null) return r
        }
        return null
    }

    private fun findFocusedEditable(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: searchEditable(root)
    }

    private fun searchEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val r = searchEditable(node.getChild(i))
            if (r != null) return r
        }
        return null
    }

    companion object {
        @Volatile var instance: FlexboardAccessibilityService? = null
    }
}
