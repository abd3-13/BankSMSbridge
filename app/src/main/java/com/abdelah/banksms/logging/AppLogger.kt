package com.abdelah.banksms.logging

import android.util.Log

object AppLogger {
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        RuntimeLogStore.append("D", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        RuntimeLogStore.append("I", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        RuntimeLogStore.append("W", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val suffix = throwable?.message?.let { " | ${throwable::class.simpleName}: $it" } ?: ""
        RuntimeLogStore.append("E", tag, "$message$suffix")
    }
}
