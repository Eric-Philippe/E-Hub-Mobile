package com.ericp.e_hub.nonogram

import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.*

class GameTimer {
    private var startTime = 0L
    private var pausedTime = 0L
    private var totalElapsed = 0L
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private var onTimerUpdate: ((gameTime: String, currentTime: String) -> Unit)? = null

    fun setOnTimerUpdateListener(listener: (gameTime: String, currentTime: String) -> Unit) {
        onTimerUpdate = listener
    }

    fun start() {
        if (isRunning) return

        isRunning = true
        startTime = System.currentTimeMillis()

        timerRunnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                val elapsedMillis = totalElapsed + (System.currentTimeMillis() - startTime)
                val seconds = (elapsedMillis / 1000).toInt()
                val minutes = seconds / 60
                val hours = minutes / 60

                val gameTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d",
                    hours,
                    minutes % 60,
                    seconds % 60
                )

                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                onTimerUpdate?.invoke(gameTime, currentTime)

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(timerRunnable!!)
    }

    fun pause() {
        if (!isRunning) return

        isRunning = false
        pausedTime = System.currentTimeMillis()
        totalElapsed += (pausedTime - startTime)
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    fun resume() {
        if (isRunning) return
        start()
    }

    fun stop() {
        isRunning = false
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        totalElapsed = 0L
        pausedTime = 0L
    }

    fun restart() {
        stop()
        start()
    }
}
