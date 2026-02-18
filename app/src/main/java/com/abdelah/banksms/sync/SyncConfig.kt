package com.abdelah.banksms.sync

import android.content.Context
import com.abdelah.banksms.parser.SmsParser
import com.abdelah.banksms.parser.SmsParsePlugin

data class AppSyncSettings(
    val baseUrl: String,
    val token: String,
    val retryIntervalMinutes: Long,
    val parserPluginsJson: String
)

object SyncConfig {
    private const val PREFS = "sync_prefs"
    private const val KEY_BASE_URL = "firefly_base_url"
    private const val KEY_TOKEN = "firefly_token"
    private const val KEY_RETRY_INTERVAL_MINUTES = "retry_interval_minutes"
    private const val KEY_PARSER_PLUGINS_JSON = "parser_plugins_json"

    fun load(context: Context): AppSyncSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedPlugins = prefs.getString(KEY_PARSER_PLUGINS_JSON, "").orEmpty()
        val pluginsJson = if (storedPlugins.isBlank()) {
            SmsParsePlugin.listToJson(SmsParser.defaultPlugins())
        } else {
            storedPlugins
        }

        return AppSyncSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, "").orEmpty(),
            token = prefs.getString(KEY_TOKEN, "").orEmpty(),
            retryIntervalMinutes = prefs.getLong(KEY_RETRY_INTERVAL_MINUTES, 15L).coerceAtLeast(15L),
            parserPluginsJson = pluginsJson
        )
    }

    fun getBaseUrl(context: Context): String = load(context).baseUrl

    fun getToken(context: Context): String = load(context).token

    fun getRetryIntervalMinutes(context: Context): Long = load(context).retryIntervalMinutes

    fun getParserPluginsJson(context: Context): String = load(context).parserPluginsJson

    fun save(
        context: Context,
        baseUrl: String,
        token: String,
        retryIntervalMinutes: Long,
        parserPluginsJson: String
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, baseUrl.trim())
            .putString(KEY_TOKEN, token.trim())
            .putLong(KEY_RETRY_INTERVAL_MINUTES, retryIntervalMinutes.coerceAtLeast(15L))
            .putString(KEY_PARSER_PLUGINS_JSON, parserPluginsJson.trim())
            .apply()
    }
}
