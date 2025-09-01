package com.ericp.e_hub

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit

class NonogramSettingsActivity : Activity() {

    private lateinit var vibrationSwitch: SwitchCompat
    private lateinit var timerSwitch: SwitchCompat
    private lateinit var backButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREFS_NAME = "nonogram_preferences"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_TIMER_VISIBLE = "timer_visible"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nonogram_settings)

        initializeComponents()
        loadSettings()
        setupListeners()
    }

    private fun initializeComponents() {
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        timerSwitch = findViewById(R.id.timerSwitch)
        backButton = findViewById(R.id.backButton)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    private fun loadSettings() {
        vibrationSwitch.isChecked = sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, true)
        timerSwitch.isChecked = sharedPreferences.getBoolean(KEY_TIMER_VISIBLE, true)
    }

    private fun setupListeners() {
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit {
                putBoolean(KEY_VIBRATION_ENABLED, isChecked)
            }
        }

        timerSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit {
                putBoolean(KEY_TIMER_VISIBLE, isChecked)
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
