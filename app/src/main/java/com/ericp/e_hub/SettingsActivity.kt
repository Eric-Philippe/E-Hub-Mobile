package com.ericp.e_hub

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.network.ApiManager
import com.ericp.e_hub.utils.EHubApiHelper
import kotlinx.coroutines.*

class SettingsActivity : Activity() {
    private lateinit var backButton: Button

    // Cache Management
    private lateinit var clearCacheButton: Button

    private lateinit var serverUrlInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var testApiButton: Button
    private lateinit var saveApiButton: Button
    private lateinit var apiStatusText: TextView

    private lateinit var apiConfig: ApiConfig
    private lateinit var apiManager: ApiManager
    private lateinit var apiHelper: EHubApiHelper

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
        testApiButton = findViewById(R.id.testApiButton)
        saveApiButton = findViewById(R.id.saveApiButton)
        apiStatusText = findViewById(R.id.apiStatusText)

        apiConfig = ApiConfig(this)
        apiManager = ApiManager.getInstance()
        apiHelper = EHubApiHelper(this)
    }

    private fun loadSettings() {
        // Load API configuration
        serverUrlInput.setText(apiConfig.getServerUrl())
        apiKeyInput.setText(apiConfig.getApiKey() ?: "")
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
    }

    private fun clearCache() {
        apiHelper.clearAllCache()
        Toast.makeText(this, getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
    }

    private fun saveApiConfiguration() {
        val serverUrl = serverUrlInput.text.toString().trim()
        val apiKey = apiKeyInput.text.toString().trim()

        if (serverUrl.isBlank()) {
            Toast.makeText(this, "Server URL cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (apiKey.isBlank()) {
            Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        apiConfig.setServerUrl(serverUrl)
        apiConfig.setApiKey(apiKey)

        updateApiStatus()
        Toast.makeText(this, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun testApiConnection() {
        // Temporarily store current values
        val currentServerUrl = apiConfig.getServerUrl()
        val currentApiKey = apiConfig.getApiKey()

        val testServerUrl = serverUrlInput.text.toString().trim()
        val testApiKey = apiKeyInput.text.toString().trim()

        if (testServerUrl.isBlank() || testApiKey.isBlank()) {
            Toast.makeText(this, "Please enter both Server URL and API Key to test", Toast.LENGTH_SHORT).show()
            return
        }

        // Apply test values
        apiConfig.setServerUrl(testServerUrl)
        apiConfig.setApiKey(testApiKey)

        apiStatusText.text = getString(R.string.api_status_testing)
        testApiButton.isEnabled = false

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    apiManager.testConnection(this@SettingsActivity)
                }

                when (result) {
                    is ApiManager.ApiResult.Success -> {
                        apiStatusText.text = getString(R.string.api_status_connected)
                        Toast.makeText(this@SettingsActivity, "API connection successful", Toast.LENGTH_SHORT).show()
                    }
                    is ApiManager.ApiResult.Error -> {
                        apiStatusText.text = "Error: ${result.message}"
                        Toast.makeText(this@SettingsActivity, "Error de connexion: ${result.message}", Toast.LENGTH_LONG).show()
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
                }
                testApiButton.isEnabled = true
            }
        }
    }

    private fun updateApiStatus() {
        if (apiConfig.isApiKeyConfigured()) {
            apiStatusText.text = getString(R.string.api_status_connected)
        } else {
            apiStatusText.text = getString(R.string.api_status_not_configured)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
