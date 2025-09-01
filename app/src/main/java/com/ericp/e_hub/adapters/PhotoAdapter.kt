package com.ericp.e_hub.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R

class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
    private val photos = mutableListOf<Uri>()

    fun setPhotos(photoUris: List<Uri>) {
        photos.clear()
        photos.addAll(photoUris)
        notifyItemRangeChanged(0, photos.size)
    }

    fun addPhotos(photoUris: List<Uri>) {
        val startPosition = photos.size
        photos.addAll(photoUris)
        notifyItemRangeInserted(startPosition, photoUris.size)
    }

    fun getPhotos(): List<Uri> = photos.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)

        fun bind(uri: Uri) {
            photoImageView.setImageURI(uri)
        }
    }
}
