package com.ericp.e_hub.adapters.tobuy

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyCategoryDto
import androidx.core.graphics.toColorInt

class CategoryChipAdapter(
    private val categories: List<ToBuyCategoryDto>
) : RecyclerView.Adapter<CategoryChipAdapter.CategoryChipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false) as TextView
        return CategoryChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryChipViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
    }

    override fun getItemCount(): Int = categories.size

    class CategoryChipViewHolder(private val chipView: TextView) : RecyclerView.ViewHolder(chipView) {
        fun bind(category: ToBuyCategoryDto) {
            chipView.text = category.name ?: "Category"

            // Set background color from category color
            try {
                val color = (category.color ?: "#666666").toColorInt()
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.setColor(color)
                drawable.cornerRadius = 12f
                chipView.background = drawable
            } catch (_: IllegalArgumentException) {
                // Fallback to default color if parsing fails
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.setColor("#666666".toColorInt())
                drawable.cornerRadius = 12f
                chipView.background = drawable
            }
        }
    }
}