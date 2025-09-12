package com.ericp.e_hub.utils

import android.content.Context
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.config.NextCloudConfig
import com.ericp.e_hub.NonogramSettingsActivity
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class SettingsExporter(private val context: Context) {

    fun exportSettingsToXml(): String {
        val apiConfig = ApiConfig(context)
        val nextCloudConfig = NextCloudConfig(context)
        val nonogramPrefs = context.getSharedPreferences(NonogramSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val rootElement = doc.createElement("settings")
        doc.appendChild(rootElement)

        // ApiConfig
        val apiConfigElement = doc.createElement("ApiConfig")
        rootElement.appendChild(apiConfigElement)
        addTextElement(doc, apiConfigElement, "serverUrl", apiConfig.getServerUrl())
        addTextElement(doc, apiConfigElement, "apiKey", apiConfig.getApiKey() ?: "")
        addTextElement(doc, apiConfigElement, "secretKey", apiConfig.getSecretKey() ?: "")

        // NextCloudConfig
        val nextCloudConfigElement = doc.createElement("NextCloudConfig")
        rootElement.appendChild(nextCloudConfigElement)
        addTextElement(doc, nextCloudConfigElement, "serverUrl", nextCloudConfig.getServerUrl() ?: "")
        addTextElement(doc, nextCloudConfigElement, "webdavUrl", nextCloudConfig.getWebdavEndpoint())
        addTextElement(doc, nextCloudConfigElement, "username", nextCloudConfig.getUsername() ?: "")
        addTextElement(doc, nextCloudConfigElement, "password", nextCloudConfig.getPassword() ?: "")

        // Nonogram Preferences
        val nonogramPrefsElement = doc.createElement("NonogramPreferences")
        rootElement.appendChild(nonogramPrefsElement)
        addTextElement(doc, nonogramPrefsElement, "vibrationEnabled", nonogramPrefs.getBoolean(NonogramSettingsActivity.KEY_VIBRATION_ENABLED, true).toString())
        addTextElement(doc, nonogramPrefsElement, "timerVisible", nonogramPrefs.getBoolean(NonogramSettingsActivity.KEY_TIMER_VISIBLE, true).toString())

        return transformDomToString(doc)
    }

    private fun addTextElement(doc: Document, parent: Element, name: String, value: String) {
        val element = doc.createElement(name)
        element.appendChild(doc.createTextNode(value))
        parent.appendChild(element)
    }

    private fun transformDomToString(doc: Document): String {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }
}

