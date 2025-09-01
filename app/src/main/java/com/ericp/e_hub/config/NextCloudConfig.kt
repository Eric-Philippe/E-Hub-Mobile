package com.ericp.e_hub.config

import android.content.Context
import androidx.core.content.edit

class NextCloudConfig(context: Context) {
    companion object {
        const val PREFS_NAME = "nextcloud_config"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_WEBDAV_URL = "webdav_url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getServerUrl(): String {
        return sharedPreferences.getString(KEY_SERVER_URL, "") ?: ""
    }

    fun setServerUrl(url: String) {
        val cleanUrl = if (url.endsWith("/")) url.dropLast(1) else url
        sharedPreferences.edit {
            putString(KEY_SERVER_URL, cleanUrl)
        }
    }

    fun getWebdavEndpoint(): String {
        return sharedPreferences.getString(KEY_WEBDAV_URL, "/remote.php/dav/files/") ?: "/remote.php/dav/files/"
    }

    fun setWebdavEndpoint(endpoint: String) {
        var cleanEndpoint = endpoint
        if (!cleanEndpoint.startsWith("/")) {
            cleanEndpoint = "/$cleanEndpoint"
        }
        if (!cleanEndpoint.endsWith("/")) {
            cleanEndpoint += "/"
        }
        sharedPreferences.edit {
            putString(KEY_WEBDAV_URL, cleanEndpoint)
        }
    }

    fun getUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    }

    fun setUsername(username: String) {
        sharedPreferences.edit {
            putString(KEY_USERNAME, username)
        }
    }

    fun getPassword(): String {
        return sharedPreferences.getString(KEY_PASSWORD, "") ?: ""
    }

    fun setPassword(password: String) {
        sharedPreferences.edit {
            putString(KEY_PASSWORD, password)
        }
    }

    fun isConfigured(): Boolean {
        return getServerUrl().isNotBlank() && getUsername().isNotBlank() && getPassword().isNotBlank()
    }

    fun clearCredentials() {
        sharedPreferences.edit {
            remove(KEY_USERNAME)
            remove(KEY_PASSWORD)
        }
    }


}