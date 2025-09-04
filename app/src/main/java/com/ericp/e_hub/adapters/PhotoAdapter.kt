package com.ericp.e_hub.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R

class PhotoAdapter(private val onPhotoRemoved: (Uri, Int) -> Unit = { _, _ -> }) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
    private val photos = mutableListOf<Uri>()

    fun setPhotos(photoUris: List<Uri>) {
        photos.clear()
        photos.addAll(photoUris)
        notifyDataSetChanged()
    }

    fun addPhotos(photoUris: List<Uri>) {
        val startPosition = photos.size
        photos.addAll(photoUris)
        notifyItemRangeInserted(startPosition, photoUris.size)
    }

    fun removePhoto(position: Int) {
        if (position in 0 until photos.size) {
            photos.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, photos.size - position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view, onPhotoRemoved)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(
        itemView: View,
        private val onPhotoRemoved: (Uri, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val removePhotoButton: Button = itemView.findViewById(R.id.removePhotoButton)

        fun bind(uri: Uri, position: Int) {
            photoImageView.setImageURI(uri)

            removePhotoButton.setOnClickListener {
                onPhotoRemoved(uri, position)
            }
        }
    }
}
