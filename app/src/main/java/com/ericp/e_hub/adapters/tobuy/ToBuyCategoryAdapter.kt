package com.ericp.e_hub.adapters.tobuy

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyCategoryDto

class ToBuyCategoryAdapter(
    private val categories: MutableList<ToBuyCategoryDto>,
    private val onEditClick: (ToBuyCategoryDto) -> Unit,
    private val onDeleteClick: (ToBuyCategoryDto) -> Unit
) : RecyclerView.Adapter<ToBuyCategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryNameText: TextView = view.findViewById(R.id.categoryNameText)
        val categoryDescriptionText: TextView = view.findViewById(R.id.categoryDescriptionText)
        val categoryColorIndicator: View = view.findViewById(R.id.categoryColorIndicator)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_management, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.categoryNameText.text = category.name ?: "Unnamed Category"

        if (category.description.isNullOrBlank()) {
            holder.categoryDescriptionText.visibility = View.GONE
        } else {
            holder.categoryDescriptionText.visibility = View.VISIBLE
            holder.categoryDescriptionText.text = category.description
        }

        // Set color indicator
        val color = try {
            category.color?.toColorInt() ?: Color.GRAY
        } catch (e: Exception) {
            Color.GRAY
        }

        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(color)
        holder.categoryColorIndicator.background = drawable

        // Set click listeners
        holder.editButton.setOnClickListener { onEditClick(category) }
        holder.deleteButton.setOnClickListener { onDeleteClick(category) }
    }

    override fun getItemCount(): Int = categories.size
}
