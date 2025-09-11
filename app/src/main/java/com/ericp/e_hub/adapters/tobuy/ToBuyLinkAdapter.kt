package com.ericp.e_hub.adapters.tobuy

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.ToBuyLinkDto
import com.squareup.picasso.Picasso
import android.content.Context
import android.content.ClipboardManager
import android.widget.Toast

class ToBuyLinkAdapter(
    private val links: List<ToBuyLinkDto>
) : RecyclerView.Adapter<ToBuyLinkAdapter.LinkViewHolder>() {

    class LinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val illustrationImageView: ImageView = itemView.findViewById(R.id.illustrationImageView)
        val linkButton: Button = itemView.findViewById(R.id.linkButton)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val favouriteIndicator: View = itemView.findViewById(R.id.favouriteIndicator)
        val noImagePlaceholder: View = itemView.findViewById(R.id.noImagePlaceholder)
        val copyLinkButton: Button = itemView.findViewById(R.id.copyLinkButton) // Add this line
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tobuy_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val link = links[position]

        // Set button click listener to open URL
        holder.linkButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
            holder.itemView.context.startActivity(intent)
        }

        // Set click listener for the copy link button
        holder.copyLinkButton.setOnClickListener {
            val clipboard = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("URL", link.url)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(holder.itemView.context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        }

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
    }

    override fun getItemCount(): Int = links.size
}
