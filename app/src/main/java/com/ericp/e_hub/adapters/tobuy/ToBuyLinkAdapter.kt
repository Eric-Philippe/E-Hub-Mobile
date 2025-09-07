package com.ericp.e_hub.adapters.tobuy

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyLinkDto
import com.squareup.picasso.Picasso

class ToBuyLinkAdapter(
    private val links: List<ToBuyLinkDto>
) : RecyclerView.Adapter<ToBuyLinkAdapter.LinkViewHolder>() {

    class LinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val illustrationImageView: ImageView = itemView.findViewById(R.id.illustrationImageView)
        val urlTextView: TextView = itemView.findViewById(R.id.urlTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val favouriteIndicator: View = itemView.findViewById(R.id.favouriteIndicator)
        val noImagePlaceholder: View = itemView.findViewById(R.id.noImagePlaceholder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tobuy_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val link = links[position]

        // Set URL
        holder.urlTextView.text = link.url

        // Set price if available
        if (link.price != null) {
            holder.priceTextView.text = "${link.price}€"
            holder.priceTextView.visibility = View.VISIBLE
        } else {
            holder.priceTextView.visibility = View.GONE
        }

        // Set favourite indicator
        holder.favouriteIndicator.visibility = if (link.favourite) View.VISIBLE else View.GONE

        // Load illustration or show placeholder
        if (!link.illustrationUrl.isNullOrEmpty()) {
            holder.illustrationImageView.visibility = View.VISIBLE
            holder.noImagePlaceholder.visibility = View.GONE
            Picasso.get()
                .load(link.illustrationUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(holder.illustrationImageView)
        } else {
            holder.illustrationImageView.visibility = View.GONE
            holder.noImagePlaceholder.visibility = View.VISIBLE
        }

        // Click listener to open URL
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = links.size
}
