package com.ericp.e_hub

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.network.ApiManager
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.SettingsExporter
import com.ericp.e_hub.utils.SettingsImporter import com.google.gson.Gson
import kotlinx.coroutines.*

class SettingsActivity : Activity() {
    private lateinit var backButton: Button

    // Cache Management
    private lateinit var clearCacheButton: Button

    private lateinit var serverUrlInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var secretKeyInput: EditText
    private lateinit var testApiButton: Button
    private lateinit var saveApiButton: Button
    private lateinit var apiStatusText: TextView

    // Import/Export
    private lateinit var importSettingsButton: Button
    private lateinit var exportSettingsButton: Button

    private lateinit var apiConfig: ApiConfig
    private lateinit var apiManager: ApiManager
    private lateinit var apiHelper: EHubApiHelper

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val REQUEST_EXPORT_SETTINGS = 1001
        private const val REQUEST_IMPORT_SETTINGS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeComponents()
        loadSettings()
        setupListeners()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        clearCacheButton = findViewById(R.id.clearCacheButton)

        serverUrlInput = findViewById(R.id.serverUrlInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        secretKeyInput = findViewById(R.id.secretKeyInput)
        testApiButton = findViewById(R.id.testApiButton)
        saveApiButton = findViewById(R.id.saveApiButton)
        apiStatusText = findViewById(R.id.apiStatusText)

        importSettingsButton = findViewById(R.id.importSettingsButton)
        exportSettingsButton = findViewById(R.id.exportSettingsButton)

        apiConfig = ApiConfig(this)
        apiManager = ApiManager.getInstance()
        apiHelper = EHubApiHelper(this)
    }

    private fun loadSettings() {
        // Load API configuration
        serverUrlInput.setText(apiConfig.getServerUrl())
        apiKeyInput.setText(apiConfig.getApiKey() ?: "")
        secretKeyInput.setText(apiConfig.getSecretKey() ?: "")
        updateApiStatus()
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        clearCacheButton.setOnClickListener {
            clearCache()
        }

        saveApiButton.setOnClickListener {
            saveApiConfiguration()
        }

        testApiButton.setOnClickListener {
            testApiConnection()
        }

        importSettingsButton.setOnClickListener {
            importSettings()
        }

        exportSettingsButton.setOnClickListener {
            exportSettings()
        }
    }

    private fun clearCache() {
        apiHelper.clearAllCache()
        Toast.makeText(this, getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
    }

    private fun exportSettings() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/xml"
            putExtra(Intent.EXTRA_TITLE, "ehub_settings.xml")
        }
        startActivityForResult(intent, REQUEST_EXPORT_SETTINGS)
    }

    private fun importSettings() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/xml"
        }
        startActivityForResult(intent, REQUEST_IMPORT_SETTINGS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {
            REQUEST_EXPORT_SETTINGS -> {
                val uri = data.data
                uri?.let {
                    try {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            val xmlString = SettingsExporter(this).exportSettingsToXml()
                            outputStream.write(xmlString.toByteArray())
                        }
                        Toast.makeText(this, "Settings exported successfully.", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(this, "Failed to export settings.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            REQUEST_IMPORT_SETTINGS -> {
                val uri = data.data
                uri?.let {
                    try {
                        contentResolver.openInputStream(it)?.use { inputStream ->
                            SettingsImporter(this).importSettingsFromXml(inputStream)
                        }
                        Toast.makeText(this, "Settings imported successfully.", Toast.LENGTH_SHORT).show()
                        // Reload settings in UI
                        loadSettings()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to import settings: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun saveApiConfiguration() {
        val serverUrl = serverUrlInput.text.toString().trim()
        val apiKey = apiKeyInput.text.toString().trim()
        val secretKey = secretKeyInput.text.toString().trim()

        if (serverUrl.isBlank()) {
            Toast.makeText(this, "Server URL cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (apiKey.isBlank()) {
            Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (secretKey.isBlank()) {
            Toast.makeText(this, "Secret Key cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        apiConfig.setServerUrl(serverUrl)
        apiConfig.setApiKey(apiKey)
        apiConfig.setSecretKey(secretKey)

        updateApiStatus()
        Toast.makeText(this, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun testApiConnection() {
        // Temporarily store current values
        val currentServerUrl = apiConfig.getServerUrl()
        val currentApiKey = apiConfig.getApiKey()
        val currentSecretKey = apiConfig.getSecretKey()

        val testServerUrl = serverUrlInput.text.toString().trim()
        val testApiKey = apiKeyInput.text.toString().trim()
        val testSecretKey = secretKeyInput.text.toString().trim()

        if (testServerUrl.isBlank() || testApiKey.isBlank() || testSecretKey.isBlank()) {
            Toast.makeText(this, "Please enter both Server URL and API Key to test", Toast.LENGTH_SHORT).show()
            return
        }

        // Apply test values
        apiConfig.setServerUrl(testServerUrl)
        apiConfig.setApiKey(testApiKey)
        apiConfig.setSecretKey(testSecretKey)

        apiStatusText.text = getString(R.string.api_status_testing)
        testApiButton.isEnabled = false

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    apiManager.testConnection(this@SettingsActivity)
                }

                when (result) {
                    is ApiManager.ApiResult.Success -> {
                        val gson = Gson()
                        val healthJson = gson.toJson(result.data)
                        // If we have a connected to false
                        if (healthJson.contains("connected\\\":false")) {
                            apiStatusText.text = getString(R.string.api_status_unauthorized)
                            apiConfig.setAuthorized(false)
                        } else {
                            apiStatusText.text = getString(R.string.api_status_connected)
                            apiConfig.setAuthorized(true)
                        }

                        Toast.makeText(this@SettingsActivity, apiStatusText.text, Toast.LENGTH_LONG).show()
                    }
                    is ApiManager.ApiResult.Error -> {
                        apiStatusText.text = "Error: ${result.message}"
                        Toast.makeText(this@SettingsActivity, "Connection error: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                apiStatusText.text = "Error: ${e.message}"
                Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Restore original values if there was an error
                if (apiStatusText.text.contains("Error") || apiStatusText.text.contains("error")) {
                    apiConfig.setServerUrl(currentServerUrl)
                    if (currentApiKey != null)
                        apiConfig.setApiKey(currentApiKey)
                    if (currentSecretKey != null)
                        apiConfig.setSecretKey(currentSecretKey)
                }
                testApiButton.isEnabled = true
            }
        }
    }

    private fun updateApiStatus() {
        if (apiConfig.isApiKeyConfigured()) {
            if (apiConfig.isAuthorized())
                apiStatusText.text = getString(R.string.api_status_connected)
            else
                apiStatusText.text = getString(R.string.api_status_unauthorized)
        } else {
            apiStatusText.text = getString(R.string.api_status_not_configured)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
