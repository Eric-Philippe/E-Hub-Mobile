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
import com.ericp.e_hub.dto.NoteStatus
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotesActivity : Activity() {
    // UI Components
    private lateinit var backButton: Button
    private lateinit var emptyStateLayout: View
    private lateinit var notesRecyclerView: RecyclerView
    // Data and helpers
    private lateinit var noteAdapter: NoteAdapter
    private val notes = mutableListOf<NoteDto>()
    private lateinit var apiHelper: EHubApiHelper
    private lateinit var apiConfig: ApiConfig
    private var originalNotes: List<NoteDto> = emptyList()
    private val deletedNotes = mutableListOf<NoteDto>()
    private var autoSaveRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate(saveInstanceState)
        setContentView(R.layout.activity_notes)

        apiConfig = ApiConfig(this)
        apiHelper = EHubApiHelper(this)

        initializeComponents()
        setupRecyclerView()
        setupListeners()
        setupSwipeToDelete()

        fetchNotes()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
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
        // Ensure all notes have the key set
        notes.forEachIndexed { i, note ->
            if (note.key != apiConfig.getSecretKey()) {
                notes[i] = note.copy(key = apiConfig.getSecretKey())
            }
        }

        noteAdapter = NoteAdapter(
            notes,
            apiConfig.getSecretKey() ?: "",
            { position: Int, newContent: String ->
                notes[position] = notes[position].copy(content = newContent, key = apiConfig.getSecretKey() ?: "")
                scheduleAutoSave()
            },
            { idx: Int ->
                val note = notes.getOrNull(idx)
                if (note == null) {
                    ""
                } else {
                    if (!note.id.isNullOrBlank()) {
                        originalNotes.find { it.id == note.id }?.content ?: ""
                    } else {
                        ""
                    }
                }
            },
            { position: Int ->
                notesRecyclerView.post {
                    notesRecyclerView.smoothScrollToPosition(position)
                }
            }
        )
        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = noteAdapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position >= 0 && position < notes.size) {
                    val note = notes[position]

                    // Always remove from the visible list immediately
                    notes.removeAt(position)

                    // If it's a persisted note, mark for deletion on server
                    if (note.id != null && note.status != NoteStatus.DELETED) {
                        deletedNotes.add(note.copy(status = NoteStatus.DELETED, key = apiConfig.getSecretKey()))
                    }

                    noteAdapter.notifyItemRemoved(position)
                    updateEmptyState()
                    scheduleAutoSave()
                    Toast.makeText(this@NotesActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                }
            }
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.6f
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

    private fun scheduleAutoSave() {
        autoSaveRunnable?.let { handler.removeCallbacks(it) }
        autoSaveRunnable = Runnable {
            autoSaveNotes()
        }
        handler.postDelayed(autoSaveRunnable!!, 3500L)
    }

    private fun autoSaveNotes() {
        val modifiedNotes = noteAdapter.getModifiedNotes() + deletedNotes
        val notesToSave = modifiedNotes.filter {
            !(it.status == NoteStatus.EMPTY || (it.content.isEmpty() && it.id == null))
        }
        if (notesToSave.isEmpty()) return
        for (note in notesToSave) {
            when (note.status) {
                NoteStatus.CREATED -> {
                    createNote(note)
                }
                NoteStatus.EDITED -> {
                    editNote(note)
                }
                NoteStatus.DELETED -> {
                    deleteNote(note)
                }
                else -> {}
            }
        }
        deletedNotes.clear()
        Toast.makeText(this, "Notes auto-saved", Toast.LENGTH_SHORT).show()
        // Auto-add blank note at the top after auto-save
        if (notes.isEmpty() || notes[0].content.isNotEmpty()) {
            notes.add(0, NoteDto(content = "", status = NoteStatus.CREATED, key = apiConfig.getSecretKey()))
            noteAdapter.notifyItemInserted(0)
        }
        updateEmptyState()
    }

    private fun createNote(note: NoteDto) {
        apiHelper.postDataAsync(
            endpoint = Endpoints.NOTES + "/create",
            data = note,
            onSuccess = {
                val idx = notes.indexOfFirst { it === note }
                if (idx != -1) {
                    notes[idx] = notes[idx].copy(status = NoteStatus.UNTOUCHED)
                    noteAdapter.notifyItemChanged(idx)
                }
            },
            onError = {}
        )
    }

    private fun editNote(note: NoteDto) {
        apiHelper.putAsync(
            endpoint = Endpoints.NOTES + "/update/" + note.id,
            data = note,
            onSuccess = {
                val idx = notes.indexOfFirst { it === note }
                if (idx != -1) {
                    notes[idx] = notes[idx].copy(status = NoteStatus.UNTOUCHED)
                    noteAdapter.notifyItemChanged(idx)
                }
            },
            onError = {}
        )
    }

    private fun deleteNote(note: NoteDto) {
        apiHelper.deleteAsync(
            endpoint = Endpoints.NOTES + "/delete/" + note.id,
            onSuccess = {},
            onError = {}
        )
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
                    // Sort notes so newest is first (top)
                    val sortedNotes = fetchedNotes.sortedByDescending { it.created ?: "" }
                    this.notes.addAll(sortedNotes.map { it.copy(key = apiConfig.getSecretKey()) })
                    // Add blank note at the top
                    this.notes.add(0, NoteDto(content = "", status = NoteStatus.CREATED, key = apiConfig.getSecretKey()))
                    originalNotes = fetchedNotes.map { it.copy(key = apiConfig.getSecretKey()) }
                    noteAdapter.notifyDataSetChanged()
                    updateEmptyState()
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to parse notes: ${e.message}", Toast.LENGTH_LONG).show()
                        updateEmptyState()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error fetching notes: $error", Toast.LENGTH_LONG).show()
                    updateEmptyState()
                }
            }
        )
    }
}