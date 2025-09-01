package com.ericp.e_hub

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
        // Load Nextcloud configuration
        serverUrlInput.setText(nextcloudConfig.getServerUrl())
        webdavEndpointInput.setText(nextcloudConfig.getWebdavEndpoint())
        passwordInput.setText(nextcloudConfig.getPassword())
        usernameInput.setText(nextcloudConfig.getUsername())
        updateApiStatus()
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

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
            Toast.makeText(this, "Server URL cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (webdavEndpoint.isBlank()) {
            Toast.makeText(this, "WebDAV endpoint cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.isBlank()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isBlank()) {
            Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        nextcloudConfig.setServerUrl(serverUrl)
        nextcloudConfig.setWebdavEndpoint(webdavEndpoint)
        nextcloudConfig.setUsername(username)
        nextcloudConfig.setPassword(password)

        updateApiStatus()
        Toast.makeText(this, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun testApiConnection() {
        // Temporarily store current values
        val currentServerUrl = nextcloudConfig.getServerUrl()
        val currentWebdavEndpoint = nextcloudConfig.getWebdavEndpoint()
        val currentUsername = nextcloudConfig.getUsername()
        val currentPassword = nextcloudConfig.getPassword()

        val testServerUrl = serverUrlInput.text.toString().trim()
        val testWebdavEndpoint = webdavEndpointInput.text.toString().trim()
        val testUsername = usernameInput.text.toString().trim()
        val testPassword = passwordInput.text.toString().trim()

        if (testServerUrl.isBlank() || testWebdavEndpoint.isBlank() || testUsername.isBlank() || testPassword.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Apply test values
        nextcloudConfig.setServerUrl(testServerUrl)
        nextcloudConfig.setWebdavEndpoint(testWebdavEndpoint)
        nextcloudConfig.setUsername(testUsername)
        nextcloudConfig.setPassword(testPassword)

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
                        Toast.makeText(this@NextCloudGymSettingsActivity, "Nextcloud API connection successful", Toast.LENGTH_SHORT).show()
                    }
                    is ApiManager.ApiResult.Error -> {
                        apiStatusText.text = "Error: ${result.message}"
                        Toast.makeText(this@NextCloudGymSettingsActivity, "Error de connexion: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                apiStatusText.text = "Error: ${e.message}"
                Toast.makeText(this@NextCloudGymSettingsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Restore original values if there was an error
                if (apiStatusText.text.contains("Error") || apiStatusText.text.contains("error")) {
                    nextcloudConfig.setServerUrl(currentServerUrl ?: "")
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
