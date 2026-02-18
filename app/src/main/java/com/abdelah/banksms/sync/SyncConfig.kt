package com.abdelah.banksms.sync

import android.content.Context

object SyncConfig {
    private const val PREFS = "sync_prefs"
    private const val KEY_BASE_URL = "firefly_base_url"
    private const val KEY_TOKEN = "firefly_token"

    fun getBaseUrl(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, "")
            .orEmpty()
    }

    fun getToken(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, "")
            .orEmpty()
    }

    fun save(context: Context, baseUrl: String, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, baseUrl)
            .putString(KEY_TOKEN, token)
            .apply()
    }
}
