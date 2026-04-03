package com.abdelah.banksms.logging

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RuntimeLogStore {
    private const val MAX_LOG_LINES = 500
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "app.log"

    private val lock = Any()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    @Volatile
    private var logFile: File? = null

    fun initialize(context: Context) {
        synchronized(lock) {
            if (logFile != null) return

            val dir = File(context.applicationContext.filesDir, LOG_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, LOG_FILE)
            if (!file.exists()) {
                file.createNewFile()
            }

            logFile = file
            _logs.value = file.readLines().takeLast(MAX_LOG_LINES)
        }
    }

    fun append(level: String, tag: String, message: String) {
        val entry = "${timestamp()} [$level/$tag] $message"

        synchronized(lock) {
            logFile?.appendText("$entry\n")
            _logs.value = (_logs.value + entry).takeLast(MAX_LOG_LINES)
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }
}
