package com.ericp.e_hub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.ericp.e_hub.utils.EHubApiHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : Activity() {

    private lateinit var apiHelper: EHubApiHelper
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiHelper = EHubApiHelper(this)

        val playNonogramButton = findViewById<Button>(R.id.playNonogramButton)
        playNonogramButton.setOnClickListener {
            val intent = Intent(this, NonogramActivity::class.java)
            startActivity(intent)
        }

        val nextCloudButton = findViewById<Button>(R.id.nextCloudButton)
        nextCloudButton.setOnClickListener {
            val intent = Intent(this, NextCloudGymActivity::class.java)
            startActivity(intent)
        }

        val nextCloudSettingsButton = findViewById<Button>(R.id.nextCloudSettingsButton)
        nextCloudSettingsButton.setOnClickListener {
            val intent = Intent(this, NextCloudGymSettingsActivity::class.java)
            startActivity(intent)
        }

        val settingsButton = findViewById<Button?>(R.id.settingsButton)
        settingsButton?.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        checkApiAndFetchData()
    }

    private fun checkApiAndFetchData() {
        if (!apiHelper.isApiConfigured()) {
            Toast.makeText(this, "Configure the API in settings", Toast.LENGTH_LONG).show()
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
