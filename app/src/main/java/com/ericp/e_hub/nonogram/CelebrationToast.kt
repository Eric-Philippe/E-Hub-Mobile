package com.ericp.e_hub.nonogram

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.ericp.e_hub.R

class CelebrationToast(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    fun show(rootView: ViewGroup, onComplete: (() -> Unit)? = null) {
        val layout = FrameLayout(context)

        val toastView = TextView(context).apply {
            text = context.getString(R.string.puzzle_solved)
            textSize = NonogramStyles.TextSizes.TOAST_TEXT_SIZE
            setTextColor(NonogramStyles.Colors.GREEN_TEXT)
            gravity = Gravity.CENTER
            setPadding(24, 26, 24, 16)
            background = NonogramStyles.createToastDrawable()
            elevation = NonogramStyles.Dimensions.ELEVATION_MEDIUM
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            topMargin = 0
        }

        layout.addView(toastView, params)

        val containerParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        rootView.addView(layout, containerParams)

        // Slide-in animation
        toastView.alpha = 0f
        toastView.translationY = -50f

        val slideIn = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(toastView, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(toastView, "translationY", -50f, 0f)
            )
            duration = 250
        }
        slideIn.start()

        // Auto-dismiss with fade-out animation
        handler.postDelayed({
            val fadeOut = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(toastView, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(toastView, "translationY", 0f, -30f)
                )
                duration = 200
            }
            fadeOut.start()

            handler.postDelayed({
                rootView.removeView(layout)
                onComplete?.invoke()
            }, 200)
        }, 1200)
    }
}
