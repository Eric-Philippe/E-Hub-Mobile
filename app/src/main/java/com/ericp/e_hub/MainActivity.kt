package com.ericp.e_hub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val playNonogramButton = findViewById<Button>(R.id.playNonogramButton)
        playNonogramButton.setOnClickListener {
            val intent = Intent(this, NonogramActivity::class.java)
            startActivity(intent)
        }
    }
}
