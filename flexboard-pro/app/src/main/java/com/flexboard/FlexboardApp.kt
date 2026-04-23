package com.flexboard

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FlexboardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val text = buildString {
                    append("===== FlexBoard Pro Crash =====\n")
                    append("Time: ").append(ts).append("\n")
                    append("Thread: ").append(t.name).append("\n")
                    append("Device: ").append(android.os.Build.MANUFACTURER).append(" ")
                        .append(android.os.Build.MODEL).append(" / Android ")
                        .append(android.os.Build.VERSION.RELEASE).append(" (SDK ")
                        .append(android.os.Build.VERSION.SDK_INT).append(")\n")
                    append("App: 1.0\n\n")
                    append(sw.toString())
                }
                Log.e("FlexBoardCrash", text)
                runCatching {
                    val dir = File(filesDir, "crash").apply { mkdirs() }
                    File(dir, "last_crash.txt").writeText(text)
                    File(dir, "crash_${System.currentTimeMillis()}.txt").writeText(text)
                }
                runCatching {
                    val ext = getExternalFilesDir(null)
                    if (ext != null) {
                        File(ext, "FlexBoard_last_crash.txt").writeText(text)
                    }
                }
            } catch (_: Throwable) {}
            previous?.uncaughtException(t, e)
        }
    }
}
