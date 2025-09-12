package com.ericp.e_hub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
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

        val playNonogramButton = findViewById<LinearLayout>(R.id.playNonogramButton)
        playNonogramButton.setOnClickListener {
            val intent = Intent(this, NonogramActivity::class.java)
            startActivity(intent)
        }

        val nextCloudButton = findViewById<LinearLayout>(R.id.nextCloudButton)
        nextCloudButton.setOnClickListener {
            val intent = Intent(this, NextCloudGymActivity::class.java)
            startActivity(intent)
        }

        val toBuyButton = findViewById<LinearLayout>(R.id.toBuyButton)
        toBuyButton.setOnClickListener {
            val intent = Intent(this, ToBuyActivity::class.java)
            startActivity(intent)
        }

        val todoButton = findViewById<LinearLayout>(R.id.todoButton)
        todoButton.setOnClickListener {
            // ToDo List button - currently links to nowhere as requested
            Toast.makeText(this, "ToDo List feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        val notesButton = findViewById<LinearLayout>(R.id.notesButton)
        notesButton.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }

        val settingsButton = findViewById<LinearLayout>(R.id.settingsButton)
        settingsButton.setOnClickListener {
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
