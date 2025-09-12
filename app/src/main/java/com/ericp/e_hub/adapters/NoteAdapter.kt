package com.ericp.e_hub.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.NoteDto
import com.ericp.e_hub.dto.NoteStatus
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val notes: MutableList<NoteDto>,
    private val secretKey: String,
    private val onNoteChanged: (Int, String) -> Unit,
    private val getOriginalContent: (Int) -> String,
    private val onNoteFocused: (Int) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteContentEdit: TextInputEditText = view.findViewById(R.id.contentEditText)
        val copyButton: Button = view.findViewById(R.id.copyButton)
        val lastUpdated: TextView = view.findViewById(R.id.lastUpdated)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        // Only setText if different and only during initial binding
        if (holder.noteContentEdit.text?.toString() != note.content) {
            holder.noteContentEdit.setText(note.content)
            holder.noteContentEdit.setSelection(note.content.length)
        }
        holder.noteContentEdit.hint = holder.noteContentEdit.context.getString(R.string.content)

        // Set up copy button
        holder.copyButton.setOnClickListener {
            val clipboard = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Note Content", note.content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(holder.itemView.context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Update last updated field
        updateLastUpdated(holder, note)

        // Remove previous watcher to avoid duplicate triggers
        holder.noteContentEdit.removeTextChangedListener(holder.noteContentEdit.getTag(-100) as? TextWatcher)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val adapterPosition = holder.bindingAdapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) return
                val newText = s?.toString() ?: ""
                val note = notes[adapterPosition]
                val orig = getOriginalContent(adapterPosition)

                val newStatus: NoteStatus = if (note.id.isNullOrBlank()) {
                    if (newText.isEmpty()) {
                        NoteStatus.EMPTY
                    } else {
                        NoteStatus.CREATED
                    }
                } else if (newText == orig) {
                    NoteStatus.UNTOUCHED
                } else if (newText.isEmpty()) {
                    NoteStatus.UNTOUCHED
                } else {
                    NoteStatus.EDITED
                }

                if (note.content != newText || note.status != newStatus) {
                    notes[adapterPosition] = note.copy(content = newText, status = newStatus, key = secretKey)
                    onNoteChanged(adapterPosition, newText)
                    updateLastUpdated(holder, notes[adapterPosition])
                }
            }
        }
        holder.noteContentEdit.addTextChangedListener(watcher)
        holder.noteContentEdit.setTag(-100, watcher)

        holder.noteContentEdit.setOnFocusChangeListener { _, hasFocus ->
            val adapterPosition = holder.bindingAdapterPosition
            if (hasFocus && adapterPosition != RecyclerView.NO_POSITION) {
                onNoteFocused(adapterPosition)
            }
        }
    }

    private fun updateLastUpdated(holder: NoteViewHolder, note: NoteDto) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val displayDate = when {
            !note.modified.isNullOrEmpty() -> {
                try {
                    val serverFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = serverFormat.parse(note.modified)
                    "Updated: ${dateFormat.format(date ?: Date())}"
                } catch (_: Exception) {
                    "Updated: ${note.modified}"
                }
            }
            !note.created.isNullOrEmpty() -> {
                try {
                    val serverFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = serverFormat.parse(note.created)
                    "Created: ${dateFormat.format(date ?: Date())}"
                } catch (_: Exception) {
                    "Created: ${note.created}"
                }
            }
            else -> "New note"
        }
        holder.lastUpdated.text = displayDate
    }

    override fun getItemCount(): Int = notes.size

    fun getModifiedNotes(): List<NoteDto> {
        return notes.filter { it.status != NoteStatus.UNTOUCHED }
    }
}