package com.ericp.e_hub.adapters.tobuy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyLinkDto

class LinkEditAdapter(
    private val links: MutableList<ToBuyLinkDto>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<LinkEditAdapter.LinkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_link_edit, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val link = links[position]
        holder.bind(link, position)
    }

    override fun getItemCount(): Int = links.size

    fun addLink(link: ToBuyLinkDto) {
        links.add(link)
        notifyItemInserted(links.size - 1)
    }

    fun removeLink(position: Int) {
        if (position in 0 until links.size) {
            links.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, links.size)
        }
    }

    fun getLinks(): MutableList<ToBuyLinkDto> {
        return links
    }

    inner class LinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val urlEditText: EditText = itemView.findViewById(R.id.urlEditText)
        private val priceEditText: EditText = itemView.findViewById(R.id.priceEditText)
        private val favouriteCheckBox: CheckBox = itemView.findViewById(R.id.favouriteCheckBox)
        private val removeButton: Button = itemView.findViewById(R.id.removeButton)

        fun bind(link: ToBuyLinkDto, position: Int) {
            urlEditText.setText(link.url)
            priceEditText.setText(link.price?.toString() ?: "")
            favouriteCheckBox.isChecked = link.favourite

            removeButton.setOnClickListener {
                onRemoveClick(position)
            }

            // Update link data when fields change
            urlEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateLinkAtPosition(position, link.copy(url = urlEditText.text.toString()))
                }
            }

            priceEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val price = priceEditText.text.toString().trim()
                        .takeIf { it.isNotEmpty() }?.toShortOrNull()
                    updateLinkAtPosition(position, link.copy(price = price))
                }
            }

            favouriteCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateLinkAtPosition(position, link.copy(favourite = isChecked))
            }
        }

        private fun updateLinkAtPosition(position: Int, updatedLink: ToBuyLinkDto) {
            if (position in 0 until links.size) {
                links[position] = updatedLink
            }
        }
    }
}