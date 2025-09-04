package com.ericp.e_hub.adapters.tobuy

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyDto
import com.ericp.e_hub.dto.ToBuyCategoryDto

data class CategorySection(
    val category: ToBuyCategoryDto,
    val items: List<ToBuyDto>,
    var isExpanded: Boolean = false
)

class CategoryAccordionAdapter(
    private val sections: MutableList<CategorySection>,
    private val onItemClick: (ToBuyDto) -> Unit,
    private val onItemLongClick: (ToBuyDto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CATEGORY_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private val expandedItems = mutableListOf<Any>()

    init {
        updateExpandedItems()
    }

    private fun updateExpandedItems() {
        expandedItems.clear()
        for (section in sections) {
            expandedItems.add(section)
            if (section.isExpanded) {
                expandedItems.addAll(section.items)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (expandedItems[position]) {
            is CategorySection -> TYPE_CATEGORY_HEADER
            is ToBuyDto -> TYPE_ITEM
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CATEGORY_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_to_buy_simple, parent, false)
                ItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryHeaderViewHolder -> {
                val section = expandedItems[position] as CategorySection
                holder.bind(section)
            }
            is ItemViewHolder -> {
                val item = expandedItems[position] as ToBuyDto
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int = expandedItems.size

    fun updateSections(newSections: List<CategorySection>) {
        val oldItemCount = expandedItems.size
        sections.clear()
        sections.addAll(newSections)
        updateExpandedItems()
        val newItemCount = expandedItems.size

        if (oldItemCount != newItemCount || sections.isEmpty()) {
            notifyDataSetChanged()
        } else {
            notifyItemRangeChanged(0, newItemCount)
        }
    }

    /**
     * Creates a true pastel color by blending the given color with white.
     * The factor determines the amount of white to blend (0.0 = original color, 1.0 = white).
     */
    private fun createPastelColor(color: Int, factor: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        val pastelRed = (red + (255 - red) * factor).toInt().coerceIn(0, 255)
        val pastelGreen = (green + (255 - green) * factor).toInt().coerceIn(0, 255)
        val pastelBlue = (blue + (255 - blue) * factor).toInt().coerceIn(0, 255)

        return Color.rgb(pastelRed, pastelGreen, pastelBlue)
    }

    private fun createDynamicGradient(categoryColor: String?): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 48f // 12dp converted to pixels

        // White background for category headers
        drawable.setColor(Color.WHITE)

        try {
            if (!categoryColor.isNullOrEmpty()) {
                val baseColor = categoryColor.toColorInt()
                // Create true pastel border by blending with white (70% white blend)
                val pastelBorderColor = createPastelColor(baseColor, 0.2f)
                drawable.setStroke(4, pastelBorderColor) // Thinner border too
            } else {
                // Default very light gray border
                drawable.setStroke(2, "#E8E8E8".toColorInt())
            }
        } catch (_: Exception) {
            // Fallback to default light gray border
            drawable.setStroke(2, "#E8E8E8".toColorInt())
        }

        return drawable
    }

    private fun createSubItemBackground(categoryColor: String?): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 24f // 8dp converted to pixels

        // Pure white background
        drawable.setColor(Color.WHITE)

        try {
            if (!categoryColor.isNullOrEmpty()) {
                val baseColor = categoryColor.toColorInt()
                // Extremely subtle pastel border (90% white blend)
                val verySubtlePastelColor = createPastelColor(baseColor, 0.2f)
                drawable.setStroke(1, verySubtlePastelColor) // Very thin border
            } else {
                // Almost invisible default border
                drawable.setStroke(1, "#F5F5F5".toColorInt())
            }
        } catch (_: Exception) {
            // Fallback to almost invisible border
            drawable.setStroke(1, "#F5F5F5".toColorInt())
        }

        return drawable
    }

    inner class CategoryHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val categoryTotal: TextView = itemView.findViewById(R.id.categoryTotal)
        private val itemCount: TextView = itemView.findViewById(R.id.itemCount)
        private val expandIcon: TextView = itemView.findViewById(R.id.expandIcon)

        fun bind(section: CategorySection) {
            categoryName.text = section.category.name ?: "Uncategorized"

            // Set dynamic gradient background based on category color
            itemView.background = createDynamicGradient(section.category.color)

            // Set category color for text if available
            section.category.color?.let { color ->
                try {
                    val baseColor = color.toColorInt()
                    // Create true pastel text color by blending with white
                    val pastelTextColor = createPastelColor(baseColor, 0.1f)
                    categoryName.setTextColor(pastelTextColor)
                    categoryTotal.setTextColor(pastelTextColor)
                    itemCount.setTextColor(pastelTextColor)
                    expandIcon.setTextColor(pastelTextColor)
                } catch (_: Exception) {
                    // Use default colors
                }
            }

            // Calculate total price for this category
            val totalPrice = section.items.sumOf { it.estimatedPrice ?: 0 }
            categoryTotal.text = itemView.context.getString(R.string.category_total_price, totalPrice)

            // Show item count
            val itemCountText = itemView.context.resources.getQuantityString(
                R.plurals.item_count,
                section.items.size,
                section.items.size
            )
            itemCount.text = itemCountText

            // Update expand icon with animation
            val targetRotation = if (section.isExpanded) 90f else 0f
            ObjectAnimator.ofFloat(expandIcon, "rotation", targetRotation).apply {
                duration = 200
                start()
            }
            expandIcon.text = "▶"

            // Set click listener to toggle expansion with ability to close
            itemView.setOnClickListener {
                val wasExpanded = section.isExpanded
                val oldExpandedCount = expandedItems.size

                // Close all other sections
                sections.forEach { it.isExpanded = false }

                // Toggle this section (allows closing if it was already open)
                section.isExpanded = !wasExpanded

                updateExpandedItems()

                val newExpandedCount = expandedItems.size
                if (newExpandedCount > oldExpandedCount) {
                    // Items were added
                    notifyItemRangeInserted(oldExpandedCount, newExpandedCount - oldExpandedCount)
                } else if (newExpandedCount < oldExpandedCount) {
                    // Items were removed
                    notifyItemRangeRemoved(newExpandedCount, oldExpandedCount - newExpandedCount)
                }
                notifyItemChanged(adapterPosition) // Update the header
            }
        }

        private fun animateSection(section: CategorySection) {
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 150
            animator.addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                itemView.alpha = 0.7f + (0.3f * progress)
            }
            animator.start()
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemTitle: TextView = itemView.findViewById(R.id.itemTitle)
        private val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
        private val estimatedPrice: TextView = itemView.findViewById(R.id.estimatedPrice)
        private val buyStatus: TextView = itemView.findViewById(R.id.buyStatus)

        fun bind(item: ToBuyDto) {
            // Find the category color for this item to create matching background
            val categoryColor = item.categories.firstOrNull()?.color
            itemView.background = createSubItemBackground(categoryColor)

            itemTitle.text = item.title

            // Set description
            if (!item.description.isNullOrEmpty()) {
                itemDescription.text = item.description
                itemDescription.visibility = View.VISIBLE
            } else {
                itemDescription.visibility = View.GONE
            }

            // Set price
            if (item.estimatedPrice != null) {
                estimatedPrice.text = itemView.context.getString(R.string.price_in_euros, item.estimatedPrice)
                estimatedPrice.visibility = View.VISIBLE
            } else {
                estimatedPrice.visibility = View.GONE
            }

            // Set buy status with dynamic colors
            if (item.bought != null) {
                buyStatus.text = "✓"
                buyStatus.setTextColor("#FFFFFF".toColorInt())
                // Change circle background to green
                buyStatus.parent?.let { parent ->
                    if (parent is ViewGroup) {
                        val drawable = GradientDrawable()
                        drawable.shape = GradientDrawable.OVAL
                        drawable.setColor("#4CAF50".toColorInt())
                        drawable.setStroke(6, "#2E7D32".toColorInt())
                        parent.background = drawable
                    }
                }
            } else {
                buyStatus.text = "○"
                buyStatus.setTextColor("#666666".toColorInt())
                // Keep default circle background
                buyStatus.parent?.let { parent ->
                    if (parent is ViewGroup) {
                        val drawable = GradientDrawable()
                        drawable.shape = GradientDrawable.OVAL
                        drawable.setColor("#F0F0F0".toColorInt())
                        drawable.setStroke(6, "#E0E0E0".toColorInt())
                        parent.background = drawable
                    }
                }
            }

            // Items slide down from the top
            itemView.alpha = 0f
            itemView.translationY = -50f // Start above, slide down
            itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setStartDelay((adapterPosition * 80).toLong()) // Stagger effect using adapterPosition
                .start()

            // Set click listeners
            itemView.setOnClickListener {
                // Add click animation
                itemView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
                onItemClick(item)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }
}
