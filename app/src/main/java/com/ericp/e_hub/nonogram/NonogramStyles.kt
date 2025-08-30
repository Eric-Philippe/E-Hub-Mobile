package com.ericp.e_hub.nonogram

import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.toColorInt

object NonogramStyles {

    // Colors
    object Colors {
        val LIGHT_GREEN = "#E8F5E8".toColorInt()
        val MEDIUM_GREEN = "#C8E6C9".toColorInt()
        val DARK_GREEN = "#A5D6A7".toColorInt()
        val GREEN_BORDER = "#81C784".toColorInt()
        val GREEN_TEXT = "#2E7D32".toColorInt()
        val CELEBRATION_BORDER = "#FFC107".toColorInt()

        val WHITE = "#FFFFFF".toColorInt()
        val LIGHT_GRAY = "#FAFAFA".toColorInt()
        val VERY_LIGHT_GRAY = "#F5F5F5".toColorInt()
        val BORDER_GRAY = "#E0E0E0".toColorInt()
        val TEXT_GRAY = "#666666".toColorInt()

        val TOAST_BACKGROUND = "#F1F8E9".toColorInt()
        val TOAST_BORDER = "#C8E6C9".toColorInt()
        val CORNER_BACKGROUND = "#F8F9FA".toColorInt()
    }

    // Dimensions
    object Dimensions {
        const val CELL_SIZE = 140
        const val CORNER_RADIUS = 16f
        const val CLUE_CORNER_RADIUS = 12f
        const val CELL_MARGIN = 2
        const val CELL_PADDING = 8
        const val STROKE_WIDTH_THIN = 1
        const val STROKE_WIDTH_MEDIUM = 2
        const val STROKE_WIDTH_THICK = 4
        const val ELEVATION_LOW = 1f
        const val ELEVATION_MEDIUM = 3f
        const val ELEVATION_HIGH = 6f
        const val ELEVATION_CELEBRATION = 8f
    }

    // Text sizes
    object TextSizes {
        const val CLUE_TEXT_SIZE = 12f
        const val TOAST_TEXT_SIZE = 14f
    }

    fun createCellDrawable(isFilled: Boolean, isCelebration: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = Dimensions.CORNER_RADIUS

            if (isFilled) {
                colors = intArrayOf(Colors.LIGHT_GREEN, Colors.MEDIUM_GREEN, Colors.DARK_GREEN)
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                setStroke(
                    if (isCelebration) Dimensions.STROKE_WIDTH_THICK else Dimensions.STROKE_WIDTH_MEDIUM,
                    if (isCelebration) Colors.CELEBRATION_BORDER else Colors.GREEN_BORDER
                )
            } else {
                colors = intArrayOf(Colors.WHITE, Colors.LIGHT_GRAY, Colors.VERY_LIGHT_GRAY)
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                setStroke(Dimensions.STROKE_WIDTH_THIN, Colors.BORDER_GRAY)
            }
        }
    }

    fun createClueDrawable(isValid: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = Dimensions.CLUE_CORNER_RADIUS
            setStroke(Dimensions.STROKE_WIDTH_THIN, Colors.BORDER_GRAY)

            if (isValid) {
                colors = intArrayOf(Colors.LIGHT_GREEN, Colors.MEDIUM_GREEN)
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
            } else {
                setColor(Colors.LIGHT_GRAY)
            }
        }
    }

    fun createCornerDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = Dimensions.CLUE_CORNER_RADIUS
            setColor(Colors.CORNER_BACKGROUND)
        }
    }

    fun createToastDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = Dimensions.CORNER_RADIUS
            setColor(Colors.TOAST_BACKGROUND)
            setStroke(Dimensions.STROKE_WIDTH_THIN, Colors.TOAST_BORDER)
        }
    }
}
