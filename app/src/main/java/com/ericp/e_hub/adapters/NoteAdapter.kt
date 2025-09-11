package com.ericp.e_hub.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.NoteDto

class NoteAdapter(
    private val notes: MutableList<NoteDto>,
    private val onNoteChanged: (Int, String) -> Unit,
    private val onAddEmptyNote: () -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteContentEdit: TextInputEditText = view.findViewById(R.id.descriptionEditText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.noteContentEdit.setText(note.content)
        holder.noteContentEdit.hint = holder.noteContentEdit.context.getString(R.string.description)

        // Remove previous watcher to avoid duplicate triggers
        holder.noteContentEdit.removeTextChangedListener(holder.noteContentEdit.getTag(-100) as? TextWatcher)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: ""
                if (notes[position].content != newText) {
                    onNoteChanged(position, newText)
                    // If last note and now non-empty, add new empty note
                    if (position == notes.size - 1 && newText.isNotEmpty()) {
                        onAddEmptyNote()
                    }
                }
            }
        }
        holder.noteContentEdit.addTextChangedListener(watcher)
        holder.noteContentEdit.setTag(-100, watcher)
    }

    override fun getItemCount(): Int = notes.size
}