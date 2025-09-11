package com.ericp.e_hub

import android.app.Activity
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.adapters.NoteAdapter
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.dto.NoteDto
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotesActivity : Activity() {
    // UI Components
    private lateinit var backButton: Button
    private lateinit var emptyStateLayout: View
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var saveButton: Button

    // Data and helpers
    private lateinit var noteAdapter: NoteAdapter
    private val notes = mutableListOf<NoteDto>()
    private lateinit var apiHelper: EHubApiHelper
    private lateinit var apiConfig: ApiConfig
    private var hasUnsavedChanges: Boolean = false
    private var originalNotes: List<NoteDto> = emptyList()

    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate(saveInstanceState)
        setContentView(R.layout.activity_notes)

        initializeComponents()
        setupRecyclerView()
        setupListeners()
        setupSwipeToDelete()

        apiConfig = ApiConfig(this)
        apiHelper = EHubApiHelper(this)
        fetchNotes()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun setUnsavedChanges(changed: Boolean) {
        hasUnsavedChanges = changed
        saveButton.visibility = if (changed) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState() {
        if (notes.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            notesRecyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            notesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        // Ensure at least one empty note exists
        if (notes.isEmpty() || notes.last().content.isNotEmpty()) {
            notes.add(NoteDto(content = ""))
        }
        noteAdapter = NoteAdapter(
            notes,
            onNoteChanged = { position, newContent ->
                notes[position] = notes[position].copy(content = newContent)
                setUnsavedChanges(true)
            },
            onAddEmptyNote = {
                // Only add if last note is not empty
                if (notes.isEmpty() || notes.last().content.isNotEmpty()) {
                    notes.add(NoteDto(content = ""))
                    noteAdapter.notifyItemInserted(notes.size - 1)
                    setUnsavedChanges(true)
                }
            }
        )
        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = noteAdapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { onSave() }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position >= 0 && position < notes.size) {
                    notes.removeAt(position)
                    noteAdapter.notifyItemRemoved(position)
                    updateEmptyState()
                    setUnsavedChanges(true)
                    Toast.makeText(this@NotesActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                }
            }
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.5f
            }
            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val cornerRadius = 32f
                    val background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius)
                        setColor(0xFFF44336.toInt()) // Red for delete
                    }
                    background.setBounds(itemView.left, itemView.top, itemView.right, itemView.bottom)
                    background.draw(canvas)
                    val icon: Drawable? = ContextCompat.getDrawable(this@NotesActivity, android.R.drawable.ic_menu_delete)
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        if (dX > 0) {
                            it.setBounds(
                                itemView.left + iconMargin,
                                itemView.top + iconMargin,
                                itemView.left + iconMargin + it.intrinsicWidth,
                                itemView.bottom - iconMargin
                            )
                        } else {
                            it.setBounds(
                                itemView.right - iconMargin - it.intrinsicWidth,
                                itemView.top + iconMargin,
                                itemView.right - iconMargin,
                                itemView.bottom - iconMargin
                            )
                        }
                        it.draw(canvas)
                    }
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(notesRecyclerView)
    }

    private fun fetchNotes() {
        apiHelper.fetchDataAsync(
            endpoint = Endpoints.NOTES + "/all/" + apiConfig.getSecretKey(),
            onSuccess = { response ->
                try {
                    val gson = Gson()
                    val notesType = object : TypeToken<List<NoteDto>>() {}.type
                    val fetchedNotes: List<NoteDto> = gson.fromJson(response, notesType)
                    this.notes.clear()
                    this.notes.addAll(fetchedNotes)
                    originalNotes = fetchedNotes.map { it.copy() } // Save original for change tracking
                    // Always ensure an empty note at the end
                    if (this.notes.isEmpty() || this.notes.last().content.isNotEmpty()) {
                        this.notes.add(NoteDto(content = ""))
                    }
                    runOnUiThread {
                        noteAdapter.notifyDataSetChanged()
                        updateEmptyState()
                        setUnsavedChanges(false)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to parse notes: ${e.message}", Toast.LENGTH_LONG).show()
                        updateEmptyState()
                        setUnsavedChanges(false)
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error fetching notes: $error", Toast.LENGTH_LONG).show()
                    updateEmptyState()
                    setUnsavedChanges(false)
                }
            }
        )
    }

    private fun onSave() {
        // To be implemented: save notes to backend or storage
        setUnsavedChanges(false)
        Toast.makeText(this, "Notes saved (virtual)", Toast.LENGTH_SHORT).show()
    }
}