package com.ericp.e_hub.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A decorative hexagon lattice that fades to white toward the bottom.
 */
class HexPatternView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Color.parseColor("#6D071A") // base line color
    }

    private var hexRadiusPx = dp(18f)
    private val hexPaths: MutableList<Triple<Path, Float, Float>> = mutableListOf() // Path with center (cx, cy)

    // Color phases
    private val topColor = Color.parseColor("#6D071A")
    private val midColor = Color.parseColor("#444444")
    private val whiteColor = Color.WHITE

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildHexGrid(w, h)
    }

    private fun buildHexGrid(width: Int, height: Int) {
        hexPaths.clear()
        if (width == 0 || height == 0) return
        val targetCols = 7
        hexRadiusPx = min(width / (targetCols * 2f), dp(28f)).coerceAtLeast(dp(12f))

        val r = hexRadiusPx
        val hexHeight = (sqrt(3.0) * r).toFloat()

        // Gaps between hexagons
        val gap = dp(5f)                 // horizontal gap
        val gapY = dp(5f)                // vertical gap

        val vertSpacing = hexHeight * 0.75f + gapY   // add vertical gap
        val horizSpacing = r * 1.5f + gap            // add horizontal gap

        val leftExtra = r * 2f
        var y = r
        var row = 0
        while (y - hexHeight / 2f < height) {
            val startX = if (row % 2 == 0) -leftExtra else -leftExtra + horizSpacing / 2f
            var x = startX
            while (x - r < width + r) {
                hexPaths.add(Triple(createHexPath(x, y, r), x, y))
                x += horizSpacing
            }
            y += vertSpacing
            row++
        }
    }

    private fun createHexPath(cx: Float, cy: Float, r: Float): Path {
        val path = Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((60 * i - 30).toDouble()) // pointy-top orientation
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat().coerceAtLeast(1f)
        val topPhaseEnd = 0.35f      // 0% - 20% top color
        val midPhaseEnd = 0.70f      // 20% - 60% dark grey
        val fadeStart = midPhaseEnd  // start fading to white after mid phase
        val fadeEnd = 1.0f           // fully white / invisible bottom

        for ((path, _, cy) in hexPaths) {
            val frac = (cy / h).coerceIn(0f, 1f)
            val (color, alpha) = when {
                frac <= topPhaseEnd -> {
                    topColor to 220
                }
                frac <= midPhaseEnd -> {
                    // Dark grey zone
                    midColor to 170
                }
                frac >= fadeEnd -> {
                    whiteColor to 0
                }
                else -> {
                    // Blend mid -> white and fade alpha
                    val t = ((frac - fadeStart) / (fadeEnd - fadeStart)).coerceIn(0f, 1f)
                    val blended = blend(midColor, whiteColor, t)
                    val a = ((1f - t) * 150f).toInt()
                    blended to a
                }
            }
            if (alpha <= 0) continue
            hexPaint.color = color
            hexPaint.alpha = alpha
            canvas.drawPath(path, hexPaint)
        }
    }

    private fun blend(c1: Int, c2: Int, t: Float): Int {
        val rt = 1f - t
        val a = (Color.alpha(c1) * rt + Color.alpha(c2) * t).toInt()
        val r = (Color.red(c1) * rt + Color.red(c2) * t).toInt()
        val g = (Color.green(c1) * rt + Color.green(c2) * t).toInt()
        val b = (Color.blue(c1) * rt + Color.blue(c2) * t).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun dp(v: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics
    )
}
