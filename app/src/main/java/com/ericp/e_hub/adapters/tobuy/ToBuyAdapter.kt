package com.ericp.e_hub.adapters.tobuy

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyDto

class ToBuyAdapter(
    private val items: MutableList<ToBuyDto>,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int, ToBuyDto) -> Unit
) : RecyclerView.Adapter<ToBuyAdapter.ToBuyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToBuyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_to_buy, parent, false)
        return ToBuyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToBuyViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = items.size

    inner class ToBuyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemTitle: TextView = itemView.findViewById(R.id.itemTitle)
        private val estimatedPrice: TextView = itemView.findViewById(R.id.estimatedPrice)
        private val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
        private val categoriesRecyclerView: RecyclerView = itemView.findViewById(R.id.categoriesRecyclerView)
        private val interestLevel: TextView = itemView.findViewById(R.id.interestLevel)
        private val linkCount: TextView = itemView.findViewById(R.id.linkCount)
        private val buyStatus: TextView = itemView.findViewById(R.id.buyStatus)

        fun bind(item: ToBuyDto, position: Int) {
            itemTitle.text = item.title

            // Set price
            if (item.estimatedPrice != null) {
                estimatedPrice.text = "$${item.estimatedPrice}"
                estimatedPrice.visibility = View.VISIBLE
            } else {
                estimatedPrice.visibility = View.GONE
            }

            // Set description
            if (!item.description.isNullOrEmpty()) {
                itemDescription.text = item.description
                itemDescription.visibility = View.VISIBLE
            } else {
                itemDescription.visibility = View.GONE
            }

            // Set up categories
            setupCategories(item)

            // Set interest level
            if (!item.interest.isNullOrEmpty()) {
                interestLevel.text = item.interest
                interestLevel.visibility = View.VISIBLE
            } else {
                interestLevel.visibility = View.GONE
            }

            // Set link count
            linkCount.text = "${item.links.size} ${if (item.links.size == 1) "link" else "links"}"

            // Set buy status
            if (item.bought != null) {
                buyStatus.text = "Bought"
                buyStatus.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                buyStatus.text = "Not Bought"
                buyStatus.setTextColor(Color.parseColor("#FF5722"))
            }

            // Set click listener
            itemView.setOnClickListener {
                onItemClick(position)
            }

            // Set long click listener
            itemView.setOnLongClickListener {
                onItemLongClick(position, item)
                true
            }
        }

        private fun setupCategories(item: ToBuyDto) {
            val categoryAdapter = CategoryChipAdapter(item.categories)
            categoriesRecyclerView.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            categoriesRecyclerView.adapter = categoryAdapter
        }
    }
}