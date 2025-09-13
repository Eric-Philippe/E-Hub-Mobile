package com.ericp.e_hub.managers

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.FragmentActivity
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyCategoryDto
import com.google.android.material.textfield.TextInputEditText
import kotlin.random.Random

class AddCategoryModalManager(
    private val activity: FragmentActivity,
    private val onCategoryAdded: (ToBuyCategoryDto) -> Unit
) {
    private var selectedColor: String = "#2196F3" // Default blue color

    fun showModal(existingCategory: ToBuyCategoryDto? = null, onUpdate: ((ToBuyCategoryDto) -> Unit)? = null) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_category, null)

        // Initialize UI components
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitleText)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.categoryNameEditText)
        val descriptionEditText = dialogView.findViewById<TextInputEditText>(R.id.categoryDescriptionEditText)
        val colorEditText = dialogView.findViewById<TextInputEditText>(R.id.categoryColorEditText)
        val colorPreviewIndicator = dialogView.findViewById<View>(R.id.colorPreviewIndicator)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        // Color buttons
        val colorButton1 = dialogView.findViewById<Button>(R.id.colorButton1)
        val colorButton2 = dialogView.findViewById<Button>(R.id.colorButton2)
        val colorButton3 = dialogView.findViewById<Button>(R.id.colorButton3)
        val colorButton4 = dialogView.findViewById<Button>(R.id.colorButton4)
        val colorButton5 = dialogView.findViewById<Button>(R.id.colorButton5)
        val colorButton6 = dialogView.findViewById<Button>(R.id.colorButton6)
        val colorButton7 = dialogView.findViewById<Button>(R.id.colorButton7)
        val colorButton8 = dialogView.findViewById<Button>(R.id.colorButton8)
        val colorButton9 = dialogView.findViewById<Button>(R.id.colorButton9)
        val randomColorButton = dialogView.findViewById<Button>(R.id.randomColorButton)

        // Set dialog title based on whether we're editing or adding
        dialogTitle.text = if (existingCategory != null) "Edit Category" else "Add Category"
        saveButton.text = if (existingCategory != null) "Update" else "Save"

        // Pre-fill if editing existing category
        existingCategory?.let { category ->
            nameEditText.setText(category.name)
            descriptionEditText.setText(category.description)
            selectedColor = category.color ?: "#2196F3"
            colorEditText.setText(selectedColor)
            updateColorPreview(selectedColor, colorPreviewIndicator)
        }

        // Set initial color
        colorEditText.setText(selectedColor)
        updateColorPreview(selectedColor, colorPreviewIndicator)

        // Create the dialog
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set up text watcher for color input
        colorEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorText = s.toString()
                if (colorText.isNotEmpty()) {
                    selectedColor = colorText
                    updateColorPreview(colorText, colorPreviewIndicator)
                }
            }
        })

        // Set up color button click listeners
        colorButton1.setOnClickListener { selectColor("#2196F3", colorEditText, colorPreviewIndicator) }
        colorButton2.setOnClickListener { selectColor("#4CAF50", colorEditText, colorPreviewIndicator) }
        colorButton3.setOnClickListener { selectColor("#FF9800", colorEditText, colorPreviewIndicator) }
        colorButton4.setOnClickListener { selectColor("#9C27B0", colorEditText, colorPreviewIndicator) }
        colorButton5.setOnClickListener { selectColor("#F44336", colorEditText, colorPreviewIndicator) }
        colorButton6.setOnClickListener { selectColor("#795548", colorEditText, colorPreviewIndicator) }
        colorButton7.setOnClickListener { selectColor("#607D8B", colorEditText, colorPreviewIndicator) }
        colorButton8.setOnClickListener { selectColor("#E91E63", colorEditText, colorPreviewIndicator) }
        colorButton9.setOnClickListener { selectColor("#00BCD4", colorEditText, colorPreviewIndicator) }
        randomColorButton.setOnClickListener { selectRandomColor(colorEditText, colorPreviewIndicator) }

        // Set up save button click listener
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            if (name.isNotEmpty()) {
                val category = ToBuyCategoryDto(
                    id = existingCategory?.id,
                    name = name,
                    description = description.ifEmpty { null },
                    color = selectedColor
                )

                if (existingCategory != null && onUpdate != null) {
                    onUpdate(category)
                } else {
                    onCategoryAdded(category)
                }
                dialog.dismiss()
            }
        }

        // Set up cancel button click listener
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    private fun selectColor(color: String, colorEditText: EditText, colorPreviewIndicator: View) {
        selectedColor = color
        colorEditText.setText(color)
        updateColorPreview(color, colorPreviewIndicator)
    }

    private fun selectRandomColor(colorEditText: EditText, colorPreviewIndicator: View) {
        val r = Random.nextInt(256)
        val g = Random.nextInt(256)
        val b = Random.nextInt(256)
        val randomColor = String.format("#%02X%02X%02X", r, g, b)

        selectedColor = randomColor
        colorEditText.setText(randomColor)
        updateColorPreview(randomColor, colorPreviewIndicator)
    }

    private fun updateColorPreview(colorText: String, colorPreviewIndicator: View) {
        try {
            val color = if (colorText.startsWith("#") && (colorText.length == 7 || colorText.length == 4)) {
                colorText.toColorInt()
            } else {
                "#666666".toColorInt() // Default gray color
            }
            colorPreviewIndicator.setBackgroundColor(color)
        } catch (_: IllegalArgumentException) {
            // Invalid color format, use default gray
            colorPreviewIndicator.setBackgroundColor("#666666".toColorInt())
        }
    }
}
