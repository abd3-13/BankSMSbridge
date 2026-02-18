package com.abdelah.banksms.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RuntimeLogStore {
    private const val MAX_LOG_LINES = 500

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    @Synchronized
    fun append(level: String, tag: String, message: String) {
        val entry = "${timestamp()} [$level/$tag] $message"
        _logs.value = (_logs.value + entry).takeLast(MAX_LOG_LINES)
    }

    private fun timestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }
}
