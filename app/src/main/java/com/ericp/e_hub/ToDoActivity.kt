package com.ericp.e_hub

import android.app.Activity
import android.os.Bundle

class ToDoActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todos)
    }
}