package com.flexboard.utils

import android.content.ClipboardManager
import android.content.Context
import com.flexboard.data.db.AppDatabase
import com.flexboard.data.db.ClipboardItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClipboardWatcher(private val ctx: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: return@OnPrimaryClipChangedListener
        if (text.isBlank()) return@OnPrimaryClipChangedListener
        scope.launch {
            val dao = AppDatabase.get(ctx).clipboardDao()
            dao.insert(ClipboardItem(text = text))
            // Cap to last 50 unpinned
            val count = dao.unpinnedCount()
            if (count > 50) dao.trimOldest(count - 50)
        }
    }

    fun start() { cm.addPrimaryClipChangedListener(listener) }
    fun stop() { cm.removePrimaryClipChangedListener(listener) }
}
