package com.abdelah.banksms.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RuntimeLogStore {
    private const val TAG = "RuntimeLogStore"
    private const val MAX_LOG_LINES = 500
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "app.log"
    private const val MAX_LOG_FILE_BYTES = 1_048_576L // 1 MB
    private const val TRIMMED_LOG_FILE_BYTES = 786_432L // 768 KB

    private val lock = Any()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    @Volatile
    private var logFile: File? = null

    fun initialize(context: Context) {
        synchronized(lock) {
            if (logFile != null) return

            runCatching {
                val dir = File(context.applicationContext.filesDir, LOG_DIR)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, LOG_FILE)
                if (!file.exists()) {
                    file.createNewFile()
                }

                trimFileIfNeeded(file)
                logFile = file
                _logs.value = readLastLines(file, MAX_LOG_LINES)
            }.onFailure {
                Log.w(TAG, "Failed to initialize persisted logs", it)
            }
        }
    }

    fun append(level: String, tag: String, message: String) {
        val entry = "${timestamp()} [$level/$tag] $message"

        synchronized(lock) {
            runCatching {
                logFile?.let { file ->
                    file.appendText("$entry\n")
                    trimFileIfNeeded(file)
                }
            }.onFailure {
                Log.w(TAG, "Failed to persist log line", it)
            }

            _logs.value = (_logs.value + entry).takeLast(MAX_LOG_LINES)
        }
    }

    private fun readLastLines(file: File, maxLines: Int): List<String> {
        if (maxLines <= 0 || !file.exists() || file.length() == 0L) {
            return emptyList()
        }

        RandomAccessFile(file, "r").use { raf ->
            val fileLength = raf.length()
            var pointer = fileLength - 1
            var linesFound = 0

            while (pointer >= 0 && linesFound <= maxLines) {
                raf.seek(pointer)
                if (raf.readByte().toInt().toChar() == '\n') {
                    linesFound++
                }
                pointer--
            }

            val start = (pointer + 1).coerceAtLeast(0)
            val bytesToRead = (fileLength - start).toInt()
            if (bytesToRead <= 0) {
                return emptyList()
            }

            val buffer = ByteArray(bytesToRead)
            raf.seek(start)
            raf.readFully(buffer)

            val lines = buffer
                .toString(StandardCharsets.UTF_8)
                .lineSequence()
                .filter { it.isNotBlank() }
                .toList()

            return lines.takeLast(maxLines)
        }
    }

    private fun trimFileIfNeeded(file: File) {
        if (!file.exists() || file.length() <= MAX_LOG_FILE_BYTES) {
            return
        }

        RandomAccessFile(file, "r").use { raf ->
            val fileLength = raf.length()
            val keepBytes = TRIMMED_LOG_FILE_BYTES.coerceAtMost(fileLength)
            val start = fileLength - keepBytes
            val buffer = ByteArray(keepBytes.toInt())

            raf.seek(start)
            raf.readFully(buffer)

            val firstNewline = buffer.indexOf('\n'.code.toByte())
            val trimmedBuffer = if (firstNewline >= 0 && firstNewline < buffer.lastIndex) {
                buffer.copyOfRange(firstNewline + 1, buffer.size)
            } else {
                buffer
            }

            file.outputStream().use { output ->
                output.write(trimmedBuffer)
            }
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }
}
