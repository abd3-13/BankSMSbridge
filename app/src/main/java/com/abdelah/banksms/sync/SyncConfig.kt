package com.abdelah.banksms.sync

import android.content.Context

data class AppSyncSettings(
    val baseUrl: String,
    val token: String,
    val retryIntervalMinutes: Long,
    val bankSender: String,
    val bankRegex: String
)

object SyncConfig {
    private const val PREFS = "sync_prefs"
    private const val KEY_BASE_URL = "firefly_base_url"
    private const val KEY_TOKEN = "firefly_token"
    private const val KEY_RETRY_INTERVAL_MINUTES = "retry_interval_minutes"
    private const val KEY_BANK_SENDER = "bank_sender_number"
    private const val KEY_BANK_REGEX = "bank_regex_pattern"

    fun load(context: Context): AppSyncSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppSyncSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, "").orEmpty(),
            token = prefs.getString(KEY_TOKEN, "").orEmpty(),
            retryIntervalMinutes = prefs.getLong(KEY_RETRY_INTERVAL_MINUTES, 15L).coerceAtLeast(15L),
            bankSender = prefs.getString(KEY_BANK_SENDER, "").orEmpty(),
            bankRegex = prefs.getString(KEY_BANK_REGEX, "").orEmpty()
        )
    }

    fun getBaseUrl(context: Context): String = load(context).baseUrl

    fun getToken(context: Context): String = load(context).token

    fun getRetryIntervalMinutes(context: Context): Long = load(context).retryIntervalMinutes

    fun getBankSender(context: Context): String = load(context).bankSender

    fun getBankRegex(context: Context): String = load(context).bankRegex

    fun save(
        context: Context,
        baseUrl: String,
        token: String,
        retryIntervalMinutes: Long,
        bankSender: String,
        bankRegex: String
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, baseUrl.trim())
            .putString(KEY_TOKEN, token.trim())
            .putLong(KEY_RETRY_INTERVAL_MINUTES, retryIntervalMinutes.coerceAtLeast(15L))
            .putString(KEY_BANK_SENDER, bankSender.trim())
            .putString(KEY_BANK_REGEX, bankRegex.trim())
            .apply()
    }
}
