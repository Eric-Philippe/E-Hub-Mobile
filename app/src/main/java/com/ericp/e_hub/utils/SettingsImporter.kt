package com.ericp.e_hub.utils

import android.content.Context
import androidx.core.content.edit
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.config.NextCloudConfig
import com.ericp.e_hub.NonogramSettingsActivity
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class SettingsImporter(private val context: Context) {

    fun importSettingsFromXml(inputStream: InputStream) {
        val apiConfig = ApiConfig(context)
        val nextCloudConfig = NextCloudConfig(context)
        val nonogramPrefs = context.getSharedPreferences(NonogramSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(inputStream)
        val rootElement = doc.documentElement

        // ApiConfig
        val apiConfigNode = rootElement.getElementsByTagName("ApiConfig").item(0) as Element
        apiConfig.setServerUrl(apiConfigNode.getElementsByTagName("serverUrl").item(0).textContent)
        apiConfig.setApiKey(apiConfigNode.getElementsByTagName("apiKey").item(0).textContent)

        // NextCloudConfig
        val nextCloudConfigNode = rootElement.getElementsByTagName("NextCloudConfig").item(0) as Element
        nextCloudConfig.setServerUrl(nextCloudConfigNode.getElementsByTagName("serverUrl").item(0).textContent)
        nextCloudConfig.setWebdavEndpoint(nextCloudConfigNode.getElementsByTagName("webdavUrl").item(0).textContent)
        nextCloudConfig.setUsername(nextCloudConfigNode.getElementsByTagName("username").item(0).textContent)
        nextCloudConfig.setPassword(nextCloudConfigNode.getElementsByTagName("password").item(0).textContent)

        // Nonogram Preferences
        val nonogramPrefsNode = rootElement.getElementsByTagName("NonogramPreferences").item(0) as Element
        nonogramPrefs.edit {
            putBoolean(NonogramSettingsActivity.KEY_VIBRATION_ENABLED, nonogramPrefsNode.getElementsByTagName("vibrationEnabled").item(0).textContent.toBoolean())
            putBoolean(NonogramSettingsActivity.KEY_TIMER_VISIBLE, nonogramPrefsNode.getElementsByTagName("timerVisible").item(0).textContent.toBoolean())
        }
    }
}
