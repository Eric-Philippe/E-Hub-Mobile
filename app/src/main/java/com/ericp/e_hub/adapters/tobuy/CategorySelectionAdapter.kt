package com.ericp.e_hub.adapters.tobuy

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyCategoryDto
import androidx.core.graphics.toColorInt

class CategorySelectionAdapter(
    private val allCategories: MutableList<ToBuyCategoryDto>,
    private val selectedCategories: MutableList<ToBuyCategoryDto>
) : RecyclerView.Adapter<CategorySelectionAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_selection, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = allCategories[position]
        holder.bind(category)
    }

    override fun getItemCount(): Int = allCategories.size

    fun addCategory(category: ToBuyCategoryDto) {
        allCategories.add(category)
        notifyItemInserted(allCategories.size - 1)
    }

    fun getSelectedCategories(): MutableList<ToBuyCategoryDto> {
        return selectedCategories
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryButton: Button = itemView.findViewById(R.id.categoryButton)

        fun bind(category: ToBuyCategoryDto) {
            val isSelected = selectedCategories.any { it.id == category.id }
            updateButtonAppearance(category, isSelected)

            categoryButton.setOnClickListener {
                val wasSelected = selectedCategories.any { it.id == category.id }

                if (wasSelected) {
                    selectedCategories.removeAll { it.id == category.id }
                } else {
                    selectedCategories.add(category)
                }

                updateButtonAppearance(category, !wasSelected)
            }
        }

        private fun updateButtonAppearance(category: ToBuyCategoryDto, isSelected: Boolean) {
            try {
                val color = (category.color ?: "#666666").toColorInt()
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.cornerRadius = 16f

                val categoryName = category.name ?: "Category"

                if (isSelected) {
                    drawable.setColor(color)
                    categoryButton.setTextColor(Color.WHITE)
                    categoryButton.text = categoryButton.context.getString(R.string.category_button_text, categoryName)
                } else {
                    drawable.setColor(Color.WHITE)
                    categoryButton.setTextColor(color)
                    drawable.setStroke(2, color)
                    categoryButton.text = categoryButton.context.getString(R.string.category_button_text, categoryName)
                }

                categoryButton.background = drawable
            } catch (_: IllegalArgumentException) {
                // Fallback to default gray color if parsing fails
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.cornerRadius = 16f
                val categoryName = category.name ?: "Category"

                if (isSelected) {
                    drawable.setColor("#666666".toColorInt())
                    categoryButton.setTextColor(Color.WHITE)
                    categoryButton.text = categoryButton.context.getString(R.string.category_button_text, categoryName)
                } else {
                    drawable.setColor(Color.WHITE)
                    categoryButton.setTextColor("#666666".toColorInt())
                    drawable.setStroke(2, "#666666".toColorInt())
                    categoryButton.text = categoryButton.context.getString(R.string.category_button_text, categoryName)
                }

                categoryButton.background = drawable
            }
        }
    }
}