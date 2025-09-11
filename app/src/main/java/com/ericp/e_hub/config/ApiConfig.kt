package com.ericp.e_hub.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class ApiConfig(context: Context) {
    private var isAuthorized: Boolean = true

    companion object {
        const val PREFS_NAME = "api_config"
        const val KEY_API_KEY = "api_key"
        const val KEY_SERVER_URL = "server_url"
        const val DEFAULT_SERVER_URL = "https://ehub.homeserver-ericp.fr"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }

    fun setApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(KEY_API_KEY, apiKey)
        }
    }

    fun getServerUrl(): String {
        return sharedPreferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        val cleanUrl = if (url.endsWith("/")) url.dropLast(1) else url
        sharedPreferences.edit {
            putString(KEY_SERVER_URL, cleanUrl)
        }
    }

    fun isApiKeyConfigured(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    fun isAuthorized(): Boolean {
        return this.isAuthorized
    }

    fun setAuthorized(authorized: Boolean) {
        this.isAuthorized = authorized
    }
}
