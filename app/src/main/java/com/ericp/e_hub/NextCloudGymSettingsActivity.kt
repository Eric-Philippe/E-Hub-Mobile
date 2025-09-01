package com.ericp.e_hub

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.config.NextCloudConfig
import com.ericp.e_hub.network.ApiManager
import kotlinx.coroutines.*

class NextCloudGymSettingsActivity : Activity() {
    private lateinit var backButton: Button

    private lateinit var serverUrlInput: EditText
    private lateinit var webdavEndpointInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var testApiButton: Button
    private lateinit var saveConfigurationButton: Button
    private lateinit var apiStatusText: TextView

    private lateinit var nextcloudConfig: NextCloudConfig
    private lateinit var apiManager: ApiManager

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nextcloud_gym_settings)

        initializeComponents()
        loadSettings()
        setupListeners()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)

        serverUrlInput = findViewById(R.id.serverUrlInput)
        webdavEndpointInput = findViewById(R.id.webdavEndpointInput)
        usernameInput = findViewById(R.id.nextcloudUsernameInput)
        passwordInput = findViewById(R.id.nextcloudPasswordInput)

        testApiButton = findViewById(R.id.testApiButton)
        saveConfigurationButton = findViewById(R.id.saveApiButton)
        apiStatusText = findViewById(R.id.apiStatusText)

        nextcloudConfig = NextCloudConfig(this)
        apiManager = ApiManager.getInstance()
    }

    private fun loadSettings() {
        // Charger la configuration API
        serverUrlInput.setText(nextcloudConfig.getServerUrl())
        webdavEndpointInput.setText(nextcloudConfig.getWebdavEndpoint())
        passwordInput.setText(nextcloudConfig.getPassword() ?: "")
        usernameInput.setText(nextcloudConfig.getUsername() ?: "")
        updateApiStatus()
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        // Nouveaux listeners pour l'API
        saveConfigurationButton.setOnClickListener {
            saveApiConfiguration()
        }

        testApiButton.setOnClickListener {
            testApiConnection()
        }
    }

    private fun saveApiConfiguration() {
        val serverUrl = serverUrlInput.text.toString().trim()
        val webdavEndpoint = webdavEndpointInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (serverUrl.isBlank()) {
            Toast.makeText(this, "L'URL du serveur ne peut pas être vide", Toast.LENGTH_SHORT).show()
            return
        }

        if (webdavEndpoint.isBlank()) {
            Toast.makeText(this, "Le WebDAV Endpoint ne peut pas être vide", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.isBlank()) {
            Toast.makeText(this, "Le nom d'utilisateur ne peut pas être vide", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isBlank()) {
            Toast.makeText(this, "Le mot de passe ne peut pas être vide", Toast.LENGTH_SHORT).show()
            return
        }

        nextcloudConfig.setServerUrl(serverUrl)
        nextcloudConfig.setWebdavEndpoint(webdavEndpoint)
        nextcloudConfig.setUsername(username)
        nextcloudConfig.setPassword(password)

        updateApiStatus()
        Toast.makeText(this, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
    }

    private fun testApiConnection() {
        // Sauvegarder temporairement les valeurs pour le test
        val currentServerUrl = nextcloudConfig.getServerUrl()
        val currentWebdavEndpoint = nextcloudConfig.getWebdavEndpoint()
        val currentUsername = nextcloudConfig.getUsername()
        val currentPassword = nextcloudConfig.getPassword()

        val testServerUrl = serverUrlInput.text.toString().trim()

        apiStatusText.text = getString(R.string.api_status_testing)
        testApiButton.isEnabled = false

        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    apiManager.testNextCloudConnection(this@NextCloudGymSettingsActivity, nextcloudConfig)
                }

                when (result) {
                    is ApiManager.ApiResult.Success -> {
                        apiStatusText.text = getString(R.string.api_status_connected)
                        Toast.makeText(this@NextCloudGymSettingsActivity, "Connexion API réussie!", Toast.LENGTH_SHORT).show()
                    }
                    is ApiManager.ApiResult.Error -> {
                        apiStatusText.text = "Erreur: ${result.message}"
                        Toast.makeText(this@NextCloudGymSettingsActivity, "Erreur de connexion: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                apiStatusText.text = "Erreur: ${e.message}"
                Toast.makeText(this@NextCloudGymSettingsActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Restaurer les valeurs précédentes si le test a échoué
                if (apiStatusText.text.contains("Erreur") || apiStatusText.text.contains("error")) {
                    nextcloudConfig.setServerUrl(currentServerUrl)
                    nextcloudConfig.setWebdavEndpoint(currentWebdavEndpoint)
                    nextcloudConfig.setUsername(currentUsername ?: "")
                    nextcloudConfig.setPassword(currentPassword ?: "")
                }
                testApiButton.isEnabled = true
            }
        }
    }

    private fun updateApiStatus() {
        if (nextcloudConfig.isConfigured()) {
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
