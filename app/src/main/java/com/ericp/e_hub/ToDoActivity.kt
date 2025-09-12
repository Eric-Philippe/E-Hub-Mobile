package com.ericp.e_hub

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.dto.State
import com.ericp.e_hub.dto.ToDoDto
import com.ericp.e_hub.dto.ToDoRequest
import com.ericp.e_hub.ui.todo.ToDoAdapter
import com.ericp.e_hub.ui.todo.ToDoRow
import com.ericp.e_hub.utils.api.ToDoApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import android.view.View
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.view.ViewGroup
import android.widget.GridLayout

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
    private lateinit var btnPickColor: View

    // Data
    private var roots: List<ToDoDto> = emptyList()
    private var selectedRootId: UUID? = null
    private var selectedParentId: UUID? = null // currently selected task (or root) to be used as parent for new tasks
    private val overrideStates = mutableMapOf<UUID, State>()
    private val newTasksByRoot = mutableMapOf<UUID, MutableList<ToDoDto>>()
    // New: local overrides for editing title/description/modified
    private val overrideLabels = mutableMapOf<UUID, String>()
    private val overrideDescriptions = mutableMapOf<UUID, String?>()
    private val overrideModified = mutableMapOf<UUID, String>()

    // Selected color for new root tasks
    private var selectedRootColor: String? = null

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
        btnPickColor = findViewById(R.id.btnPickColor)
        btnCreateRoot.setOnClickListener { submitNewRoot() }
        etRootLabel.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitNewRoot(); true
            } else false
        }
        btnPickColor.setOnClickListener { showColorPicker() }
        updateColorChip()

        loadData()
    }

    private fun submitNewRoot() {
        val label = etRootLabel.text?.toString()?.trim().orEmpty()
        if (label.isEmpty()) return
        if (selectedRootColor.isNullOrBlank()) {
            Toast.makeText(this, "Please pick a color for this list", Toast.LENGTH_SHORT).show()
            return
        }
        createRootLocallyAndSync(label)
        etRootLabel.setText("")
    }

    private fun createRootLocallyAndSync(label: String) {
        // Optimistic local add
        val now = LocalDateTime.now().toString()
        val tempRoot = ToDoDto(
            id = UUID.randomUUID(),
            label = label,
            state = State.TODO,
            color = selectedRootColor,
            created = now,
            modified = null,
            dueDate = null,
            description = null,
            parentId = null,
            children = emptyList()
        )
        roots = roots + tempRoot
        selectedRootId = tempRoot.id
        selectedParentId = tempRoot.id
        render()
        recycler.post { recycler.scrollToPosition(0) }

        // API create
        val req = ToDoRequest(label = label, state = State.TODO, color = selectedRootColor, parentId = null)
        api.createToDo(
            data = req,
            onSuccess = {
                // Refresh from server to replace temp item with persisted one
                refreshFromServer()
            },
            onError = { msg ->
                // Remove optimistic root and render
                roots = roots.filter { it.id != tempRoot.id }
                if (selectedRootId == tempRoot.id) {
                    selectedRootId = roots.firstOrNull()?.id
                    selectedParentId = selectedRootId
                }
                render()
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadData() {
        // Fetch tasks and render
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

    private fun refreshFromServer() {
        // Clear local temp tasks on a full refresh and keep selection when possible
        api.fetchToDos(
            onSuccess = { list ->
                val prevRoot = selectedRootId
                val prevSel = selectedParentId
                newTasksByRoot.clear()
                roots = list.filter { it.parentId == null }
                if (roots.isEmpty()) {
                    selectedRootId = null
                    selectedParentId = null
                    adapter.submitItems(emptyList())
                    return@fetchToDos
                }
                // Maintain selection if still present
                selectedRootId = roots.find { it.id == prevRoot }?.id ?: roots.first().id
                selectedParentId = prevSel?.let { sel ->
                    // verify exists in refreshed tree
                    fun existsIn(listIn: List<ToDoDto>): Boolean {
                        listIn.forEach { d ->
                            if (d.id == sel) return true
                            if (existsIn(d.children)) return true
                        }
                        return false
                    }
                    if (roots.any { it.id == sel } || roots.any { existsIn(it.children) }) sel else selectedRootId
                }
                render()
            },
            onError = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
        val oState = overrideStates[dto.id]
        val oLabel = overrideLabels[dto.id]
        val oDesc = overrideDescriptions[dto.id]
        val oMod = overrideModified[dto.id]
        var out = dto
        if (oState != null && oState != dto.state) out = out.copy(state = oState)
        if (oLabel != null && oLabel != dto.label) out = out.copy(label = oLabel)
        if (oDesc != null && oDesc != dto.description) out = out.copy(description = oDesc)
        if (oMod != null && oMod != dto.modified) out = out.copy(modified = oMod)
        return out
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

    private fun findById(id: UUID): ToDoDto? {
        fun walk(list: List<ToDoDto>): ToDoDto? {
            list.forEach { d ->
                if (d.id == id) return applyOverrides(d)
                val fromKids = walk(d.children)
                if (fromKids != null) return applyOverrides(fromKids)
            }
            return null
        }
        val inRoots = walk(roots)
        if (inRoots != null) return inRoots
        newTasksByRoot.values.forEach { list ->
            list.firstOrNull { it.id == id }?.let { return applyOverrides(it) }
        }
        return null
    }

    // Adapter callbacks
    override fun onToggleTask(id: UUID, newState: State) {
        // Capture previous for revert if needed
        val prevState = findById(id)?.state
        overrideStates[id] = newState
        overrideModified[id] = LocalDateTime.now().toString()
        render()

        // Sync with API
        api.changeToDoState(
            id = id.toString(),
            newState = newState,
            onSuccess = {
                // Clear overrides and refresh to reflect canonical server data
                clearOverridesFor(id)
                refreshFromServer()
            },
            onError = { msg ->
                // Revert UI
                if (prevState != null) {
                    if (prevState == newState) {
                        // no-op
                    } else {
                        overrideStates[id] = prevState
                    }
                } else {
                    overrideStates.remove(id)
                }
                render()
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onAddTask(label: String) {
        val parentId = selectedParentId ?: selectedRootId ?: return
        val now = LocalDateTime.now().toString()
        val tempId = UUID.randomUUID()
        val newDto = ToDoDto(
            id = tempId,
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
        recycler.post { recycler.smoothScrollToPosition(0) }

        val req = ToDoRequest(label = label, state = State.TODO, parentId = parentId)
        api.createToDo(
            data = req,
            onSuccess = {
                // Replace temp with server data via full refresh
                refreshFromServer()
            },
            onError = { msg ->
                // Remove temp
                newTasksByRoot[parentId]?.removeAll { it.id == tempId }
                render()
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
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

    override fun onOpenDetails(id: UUID) {
        val dto = findById(id) ?: return
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_todo_details, null, false)
        val etTitle = v.findViewById<EditText>(R.id.etTitle)
        val etDesc = v.findViewById<EditText>(R.id.etDescription)
        val spState = v.findViewById<Spinner>(R.id.spState)
        val tvUpdated = v.findViewById<TextView>(R.id.tvLastUpdated)
        val btnSave = v.findViewById<Button>(R.id.btnSave)
        val btnCancel = v.findViewById<Button>(R.id.btnCancel)
        val btnDelete = v.findViewById<Button>(R.id.btnDelete)

        // Populate
        etTitle.setText(dto.label)
        etDesc.setText(dto.description ?: "")
        // Spinner adapter and selection
        val items = listOf("To Do", "In Progress", "Done")
        val states = listOf(State.TODO, State.IN_PROGRESS, State.DONE)
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spState.adapter = adapter
        spState.setSelection(states.indexOf(dto.state).coerceAtLeast(0))

        val last = overrideModified[id] ?: dto.modified ?: dto.created
        tvUpdated.text = "Last updated: ${formatDisplayTime(last)}"

        val dialog = AlertDialog.Builder(this)
            .setView(v)
            .create()

        btnSave.setOnClickListener {
            val newLabel = etTitle.text?.toString()?.trim().orEmpty()
            val newDesc = etDesc.text?.toString()?.trim().orEmpty()
            val newState = states.getOrNull(spState.selectedItemPosition) ?: dto.state

            // Capture previous values for possible revert
            val prevLabel = dto.label
            val prevDesc = dto.description
            val prevState = dto.state

            // Optimistic overrides
            if (newLabel.isNotEmpty() && newLabel != dto.label) {
                overrideLabels[id] = newLabel
            }
            overrideDescriptions[id] = newDesc.ifEmpty { null }
            overrideStates[id] = newState
            overrideModified[id] = LocalDateTime.now().toString()

            render()

            // Sync with API
            val req = ToDoRequest(
                label = if (newLabel.isNotEmpty()) newLabel else prevLabel,
                state = newState,
                description = newDesc.ifEmpty { null },
                parentId = dto.parentId
            )
            api.updateToDo(
                id = id.toString(),
                data = req,
                onSuccess = {
                    // Clear overrides so server truth shows
                    clearOverridesFor(id)
                    refreshFromServer()
                    dialog.dismiss()
                },
                onError = { msg ->
                    // Revert optimistic changes
                    if (newLabel != prevLabel) overrideLabels[id] = prevLabel else overrideLabels.remove(id)
                    if ((prevDesc ?: "") != newDesc) overrideDescriptions[id] = prevDesc else overrideDescriptions.remove(id)
                    if (prevState != newState) overrideStates[id] = prevState else overrideStates.remove(id)
                    overrideModified.remove(id)

                    render()
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            )
        }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete") { _, _ ->
                    api.deleteToDo(
                        id = id.toString(),
                        onSuccess = {
                            clearOverridesFor(id)
                            // Adjust selections if needed
                            if (selectedParentId == id) selectedParentId = dto.parentId
                            if (selectedRootId == id) selectedRootId = null
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            refreshFromServer()
                        },
                        onError = { msg ->
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    private fun showColorPicker() {
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
            "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFC107", "#FF9800", "#FF5722", "#795548", "#607D8B"
        )
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val grid = GridLayout(this).apply {
            columnCount = 6
        }
        val sizePx = (28 * resources.displayMetrics.density).toInt()
        val marginPx = (8 * resources.displayMetrics.density).toInt()
        colors.forEach { hex ->
            val v = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    try {
                        setColor(Color.parseColor(hex))
                    } catch (_: Exception) {
                        setColor(Color.LTGRAY)
                    }
                    setStroke((1 * resources.displayMetrics.density).toInt(), Color.DKGRAY)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedRootColor = hex
                    updateColorChip()
                    colorDialog?.dismiss()
                }
            }
            grid.addView(v)
        }
        container.addView(grid)
        colorDialog = AlertDialog.Builder(this)
            .setTitle("Pick a color")
            .setView(container)
            .create()
        colorDialog?.show()
    }

    private var colorDialog: AlertDialog? = null

    private fun updateColorChip() {
        val chip = btnPickColor
        val gd = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            val strokePx = (1 * resources.displayMetrics.density).toInt()
            setStroke(strokePx, Color.parseColor("#BDBDBD"))
            val fill = selectedRootColor?.let {
                try { Color.parseColor(it) } catch (_: Exception) { null }
            }
            setColor(fill ?: Color.WHITE)
        }
        chip.background = gd
    }

    private fun clearOverridesFor(id: UUID) {
        overrideStates.remove(id)
        overrideLabels.remove(id)
        overrideDescriptions.remove(id)
        overrideModified.remove(id)
    }

    private fun formatDisplayTime(isoOrOther: String): String {
        return try {
            val dt = LocalDateTime.parse(isoOrOther)
            DateTimeFormatter.ofPattern("MMM d, yyyy  —  h:mma").format(dt)
        } catch (e: Exception) {
            isoOrOther
        }
    }
}