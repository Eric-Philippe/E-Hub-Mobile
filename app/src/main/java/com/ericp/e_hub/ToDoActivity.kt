package com.ericp.e_hub

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.dto.State
import com.ericp.e_hub.dto.ToDoDto
import com.ericp.e_hub.ui.todo.ToDoAdapter
import com.ericp.e_hub.ui.todo.ToDoRow
import com.ericp.e_hub.utils.api.ToDoApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ToDoActivity: Activity(), ToDoAdapter.Listener {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ToDoAdapter
    private lateinit var api: ToDoApi

    // Header views
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView

    // Bottom form views
    private lateinit var etRootLabel: EditText
    private lateinit var btnCreateRoot: Button

    // Data
    private var roots: List<ToDoDto> = emptyList()
    private var selectedRootId: UUID? = null
    private var selectedParentId: UUID? = null // currently selected task (or root) to be used as parent for new tasks
    private val overrideStates = mutableMapOf<UUID, State>()
    private val newTasksByRoot = mutableMapOf<UUID, MutableList<ToDoDto>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todos)

        api = ToDoApi(this)

        // Bind header
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvActivityTitle)
        tvTitle.text = getString(R.string.app_name).let { "To-Do" } // explicit title as requested
        btnBack.setOnClickListener { finish() }

        // Bind list
        recycler = findViewById(R.id.todoRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ToDoAdapter(this)
        recycler.adapter = adapter

        // Bind bottom form
        etRootLabel = findViewById(R.id.etRootLabel)
        btnCreateRoot = findViewById(R.id.btnCreateRoot)
        btnCreateRoot.setOnClickListener { submitNewRoot() }
        etRootLabel.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitNewRoot(); true
            } else false
        }

        loadData()
    }

    private fun submitNewRoot() {
        val label = etRootLabel.text?.toString()?.trim().orEmpty()
        if (label.isEmpty()) return
        createRootLocally(label)
        etRootLabel.setText("")
    }

    private fun createRootLocally(label: String) {
        val now = LocalDateTime.now().toString()
        val root = ToDoDto(
            id = UUID.randomUUID(),
            label = label,
            state = State.TODO,
            color = null,
            created = now,
            modified = null,
            dueDate = null,
            description = null,
            parentId = null,
            children = emptyList()
        )
        roots = roots + root
        selectedRootId = root.id
        selectedParentId = root.id
        render()
        recycler.post { recycler.scrollToPosition(0) }
    }

    private fun loadData() {
        // UI-only: fetch tasks and render
        api.fetchToDos(
            onSuccess = { list ->
                // The API may already return hierarchical trees via children
                roots = list.filter { it.parentId == null }
                if (roots.isEmpty()) {
                    adapter.submitItems(emptyList())
                    return@fetchToDos
                }
                if (selectedRootId == null) selectedRootId = roots.first().id
                if (selectedParentId == null) selectedParentId = selectedRootId
                render()
            },
            onError = {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                // Still render empty UI with nav bars if any known roots (none on error without cache)
                adapter.submitItems(emptyList())
            }
        )
    }

    private fun render() {
        val selRootId = selectedRootId ?: return
        val selectedRoot = roots.find { it.id == selRootId } ?: roots.first()
        val otherRoots = roots.filter { it.id != selectedRoot.id }

        val rows = mutableListOf<ToDoRow>()
        val title = selectedRoot.label.uppercase()
        val subtitle = DateTimeFormatter.ofPattern("MMMM, dd yyyy  —  h:mma").format(LocalDateTime.now())
        rows += ToDoRow.Header(title, subtitle)

        // Build task rows from selected root's children
        val children = (selectedRoot.children) + (newTasksByRoot[selectedRoot.id] ?: emptyList())
        rows += flatten(children, 0)

        // Input row
        rows += ToDoRow.Input

        // Navigation bars for other roots
        otherRoots.forEachIndexed { idx, r ->
            rows += ToDoRow.Nav(r.id, r.label.uppercase(), idx)
        }

        adapter.submitItems(rows)
    }

    private fun applyOverrides(dto: ToDoDto): ToDoDto {
        val o = overrideStates[dto.id]
        return if (o != null && o != dto.state) dto.copy(state = o) else dto
    }

    private fun flatten(list: List<ToDoDto>, level: Int): List<ToDoRow.Task> {
        val out = mutableListOf<ToDoRow.Task>()
        list.forEach { d ->
            val withState = applyOverrides(d)
            val isSelected = selectedParentId == d.id
            out += ToDoRow.Task(withState, level, isSelected)
            val kids = (d.children) + (newTasksByRoot[d.id] ?: emptyList())
            if (kids.isNotEmpty()) {
                out += flatten(kids, level + 1)
            }
        }
        return out
    }

    // Adapter callbacks (UI-only changes)
    override fun onToggleTask(id: UUID, newState: State) {
        overrideStates[id] = newState
        render()
    }

    override fun onAddTask(label: String) {
        val parentId = selectedParentId ?: selectedRootId ?: return
        val now = LocalDateTime.now().toString()
        val newDto = ToDoDto(
            id = UUID.randomUUID(),
            label = label,
            state = State.TODO,
            color = null,
            created = now,
            modified = null,
            dueDate = null,
            description = null,
            parentId = parentId,
            children = emptyList()
        )
        val list = newTasksByRoot.getOrPut(parentId) { mutableListOf() }
        list.add(newDto)
        render()
        // Optional UX feedback
        recycler.post {
            recycler.smoothScrollToPosition(0)
        }
    }

    override fun onSwitchRoot(rootId: UUID) {
        if (selectedRootId != rootId) {
            selectedRootId = rootId
            selectedParentId = rootId // default to root as parent when switching
            render()
            recycler.post { recycler.scrollToPosition(0) }
        }
    }

    override fun onSelectTask(id: UUID) {
        selectedParentId = id
        render()
    }
}