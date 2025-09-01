package com.ericp.e_hub

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.network.ApiManager
import kotlinx.coroutines.*

class SettingsActivity : Activity() {
    private lateinit var backButton: Button

    // Nouveaux éléments pour l'API
    private lateinit var serverUrlInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var testApiButton: Button
    private lateinit var saveApiButton: Button
    private lateinit var apiStatusText: TextView

    private lateinit var apiConfig: ApiConfig
    private lateinit var apiManager: ApiManager

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

        // Nouveaux éléments API
        serverUrlInput = findViewById(R.id.serverUrlInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        testApiButton = findViewById(R.id.testApiButton)
        saveApiButton = findViewById(R.id.saveApiButton)
        apiStatusText = findViewById(R.id.apiStatusText)

        apiConfig = ApiConfig(this)
        apiManager = ApiManager.getInstance()
    }

    private fun loadSettings() {
        // Charger la configuration API
        serverUrlInput.setText(apiConfig.getServerUrl())
        apiKeyInput.setText(apiConfig.getApiKey() ?: "")
        updateApiStatus()
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        // Nouveaux listeners pour l'API
        saveApiButton.setOnClickListener {
            saveApiConfiguration()
        }

        testApiButton.setOnClickListener {
            testApiConnection()
        }
    }

    private fun saveApiConfiguration() {
        val serverUrl = serverUrlInput.text.toString().trim()
        val apiKey = apiKeyInput.text.toString().trim()

        if (serverUrl.isBlank()) {
            Toast.makeText(this, "L'URL du serveur ne peut pas être vide", Toast.LENGTH_SHORT).show()
            return
        }

        if (apiKey.isBlank()) {
            Toast.makeText(this, "La clé API ne peut pas être vide", Toast.LENGTH_SHORT).show()
            return
        }

        apiConfig.setServerUrl(serverUrl)
        apiConfig.setApiKey(apiKey)

        updateApiStatus()
        Toast.makeText(this, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
    }

    private fun testApiConnection() {
        // Sauvegarder temporairement les valeurs pour le test
        val currentServerUrl = apiConfig.getServerUrl()
        val currentApiKey = apiConfig.getApiKey()

        val testServerUrl = serverUrlInput.text.toString().trim()
        val testApiKey = apiKeyInput.text.toString().trim()

        if (testServerUrl.isBlank() || testApiKey.isBlank()) {
            Toast.makeText(this, "Veuillez remplir l'URL du serveur et la clé API", Toast.LENGTH_SHORT).show()
            return
        }

        // Appliquer temporairement les nouvelles valeurs pour le test
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
                        Toast.makeText(this@SettingsActivity, "Connexion API réussie!", Toast.LENGTH_SHORT).show()
                    }
                    is ApiManager.ApiResult.Error -> {
                        apiStatusText.text = "Erreur: ${result.message}"
                        Toast.makeText(this@SettingsActivity, "Erreur de connexion: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                apiStatusText.text = "Erreur: ${e.message}"
                Toast.makeText(this@SettingsActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Restaurer les valeurs précédentes si le test a échoué
                if (apiStatusText.text.contains("Erreur") || apiStatusText.text.contains("error")) {
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
