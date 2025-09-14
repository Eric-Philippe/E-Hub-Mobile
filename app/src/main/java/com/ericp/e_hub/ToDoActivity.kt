package com.ericp.e_hub

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.dto.State
import com.ericp.e_hub.dto.ToDoDto
import com.ericp.e_hub.dto.ToDoRequest
import com.ericp.e_hub.adapters.ToDoAdapter
import com.ericp.e_hub.adapters.ToDoRow
import com.ericp.e_hub.utils.api.ToDoApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

class ToDoActivity: Activity(), ToDoAdapter.Listener {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ToDoAdapter
    private lateinit var api: ToDoApi

    // Header views
    private lateinit var btnBack: Button

    // Bottom form views
    private lateinit var etRootLabel: EditText
    private lateinit var btnCreateRoot: Button
    private lateinit var btnPickColor: View

    // Data
    private var roots: List<ToDoDto> = emptyList()
    private var selectedRootId: UUID? = null
    private var selectedParentId: UUID? = null // task selected as parent when adding
    private val overrideStates = mutableMapOf<UUID, State>()
    private val newTasksByRoot = mutableMapOf<UUID, MutableList<ToDoDto>>()
    private val overrideLabels = mutableMapOf<UUID, String>()
    private val overrideDescriptions = mutableMapOf<UUID, String?>()
    private val overrideModified = mutableMapOf<UUID, String>()
    private val collapsed = mutableSetOf<UUID>() // collapsed items (effective for subtasks)

    // Selected color for new root tasks
    private var selectedRootColor: String? = null

    // Track if user intentionally closed the root view
    private var hasUserClosedRoot: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todos)

        api = ToDoApi(this)

        // Bind header
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Bind list
        recycler = findViewById(R.id.todoRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ToDoAdapter(this)
        recycler.adapter = adapter
        attachSwipeToDelete()

        // Bind bottom form
        etRootLabel = findViewById(R.id.etRootLabel)
        btnCreateRoot = findViewById(R.id.btnCreateRoot)
        btnPickColor = findViewById(R.id.btnPickColor)
        btnCreateRoot.setOnClickListener { submitNewRoot() }
        etRootLabel.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { submitNewRoot(); true } else false
        }
        btnPickColor.setOnClickListener { showColorPicker() }
        updateColorChip()

        loadData()
    }

    private fun attachSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                val rootId: UUID? = when (val row = adapter.getItem(pos)) {
                    is ToDoRow.Header -> row.rootId
                    is ToDoRow.Nav -> row.rootId
                    else -> null
                }
                adapter.notifyItemChanged(pos)
                if (rootId == null) return
                AlertDialog.Builder(this@ToDoActivity)
                    .setTitle("Delete list")
                    .setMessage("Delete this list and all of its tasks?")
                    .setPositiveButton("Delete") { _, _ ->
                        api.deleteToDo(
                            id = rootId.toString(),
                            onSuccess = {
                                if (selectedRootId == rootId) {
                                    selectedRootId = null
                                    selectedParentId = null
                                }
                                Toast.makeText(this@ToDoActivity, "Deleted", Toast.LENGTH_SHORT).show()
                                refreshFromServer()
                            },
                            onError = { msg -> Toast.makeText(this@ToDoActivity, msg, Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val row = adapter.getItem(viewHolder.bindingAdapterPosition)
                return when (row) {
                    is ToDoRow.Header, is ToDoRow.Nav -> super.getSwipeDirs(recyclerView, viewHolder)
                    else -> 0
                }
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recycler)
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
        // Insert, then sort by created desc
        roots = (roots + tempRoot).sortedByDescending { parseAnyDateTime(it.created) ?: LocalDateTime.MIN }
        selectedRootId = tempRoot.id
        selectedParentId = tempRoot.id
        hasUserClosedRoot = false
        seedCollapsedForRoot(tempRoot.id)
        render()
        recycler.post { recycler.scrollToPosition(0) }

        val req = ToDoRequest(label = label, state = State.TODO, color = selectedRootColor, parentId = null)
        api.createToDo(
            data = req,
            onSuccess = { refreshFromServer() },
            onError = { msg ->
                roots = roots.filter { it.id != tempRoot.id }
                if (selectedRootId == tempRoot.id) {
                    selectedRootId = roots.firstOrNull()?.id
                    selectedParentId = selectedRootId
                    selectedRootId?.let { seedCollapsedForRoot(it) }
                }
                render()
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadData() {
        api.fetchToDos(
            onSuccess = { list ->
                roots = list.filter { it.parentId == null }
                    .sortedByDescending { parseAnyDateTime(it.created) ?: LocalDateTime.MIN }
                if (roots.isEmpty()) {
                    adapter.submitItems(emptyList())
                    return@fetchToDos
                }
                // Only auto-open a root if user hasn't explicitly closed
                if (selectedRootId == null && !hasUserClosedRoot) {
                    selectedRootId = roots.first().id
                    selectedParentId = selectedRootId
                    selectedRootId?.let { seedCollapsedForRoot(it) }
                }
                if (selectedRootId != null && selectedParentId == null) selectedParentId = selectedRootId
                render()
            },
            onError = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                // keep UI as-is on error
            }
        )
    }
    private fun refreshFromServer() {
        api.fetchToDos(
            onSuccess = { list ->
                val prevRoot = selectedRootId
                val prevSel = selectedParentId
                newTasksByRoot.clear()
                roots = list.filter { it.parentId == null }
                    .sortedByDescending { parseAnyDateTime(it.created) ?: LocalDateTime.MIN }
                if (roots.isEmpty()) {
                    selectedRootId = null
                    selectedParentId = null
                    adapter.submitItems(emptyList())
                    return@fetchToDos
                }
                selectedRootId = when {
                    prevRoot != null && roots.any { it.id == prevRoot } -> prevRoot
                    hasUserClosedRoot -> null
                    else -> roots.first().id
                }
                // Seed collapsed when root changed
                if (selectedRootId != null && selectedRootId != prevRoot) seedCollapsedForRoot(selectedRootId!!)

                selectedParentId = prevSel?.let { sel ->
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
            onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun render() {
        // If no root is selected, show only the nav list of roots
        val selRootId = selectedRootId
        if (selRootId == null) {
            val rows = mutableListOf<ToDoRow>()
            roots.forEachIndexed { idx, r -> rows += ToDoRow.Nav(r.id, r.label.uppercase(), idx) }
            adapter.submitItems(rows)
            return
        }

        val selectedRoot = roots.find { it.id == selRootId } ?: roots.first()
        val otherRoots = roots.filter { it.id != selectedRoot.id }

        val rows = mutableListOf<ToDoRow>()
        val title = selectedRoot.label.uppercase()
        // Compute most recent update across the root and all descendants
        val latestIso = computeLatestTimestampIso(selectedRoot)
        val subtitle = formatHeaderTime(latestIso)
        rows += ToDoRow.Header(selectedRoot.id, title, subtitle)

        val children = mergeAndSortByCreatedDesc(selectedRoot.children, newTasksByRoot[selectedRoot.id] ?: emptyList())
        rows += flatten(children, 0)
        rows += ToDoRow.Input
        otherRoots.forEachIndexed { idx, r -> rows += ToDoRow.Nav(r.id, r.label.uppercase(), idx) }

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
            val kidsUnsorted = mergeAndSortByCreatedDesc(d.children, newTasksByRoot[d.id] ?: emptyList())
            val hasChildren = kidsUnsorted.isNotEmpty()
            val isCollapsed = collapsed.contains(d.id)
            out += ToDoRow.Task(withState, level, isSelected, hasChildren, isCollapsed)
            val shouldShowKids = hasChildren && !isCollapsed
            if (shouldShowKids) out += flatten(kidsUnsorted, level + 1)
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
        newTasksByRoot.values.forEach { list -> list.firstOrNull { it.id == id }?.let { return applyOverrides(it) } }
        return null
    }

    // Adapter callbacks
    override fun onToggleTask(id: UUID, newState: State) {
        val prevState = findById(id)?.state
        overrideStates[id] = newState
        overrideModified[id] = LocalDateTime.now().toString()
        render()
        api.changeToDoState(
            id = id.toString(),
            newState = newState,
            onSuccess = { clearOverridesFor(id); refreshFromServer() },
            onError = { msg ->
                if (prevState != null && prevState != newState) overrideStates[id] = prevState else overrideStates.remove(id)
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
            onSuccess = { refreshFromServer() },
            onError = { msg ->
                newTasksByRoot[parentId]?.removeAll { it.id == tempId }
                render()
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onSwitchRoot(rootId: UUID) {
        onSwitchRoot(rootId, null)
    }

    override fun onSwitchRoot(rootId: UUID, fromView: View?) {
        if (selectedRootId == rootId) return
        if (fromView == null) {
            recycler.animate().alpha(0f).setDuration(80).withEndAction {
                selectedRootId = rootId
                selectedParentId = rootId
                hasUserClosedRoot = false
                seedCollapsedForRoot(rootId)
                render()
                recycler.alpha = 0f
                recycler.animate().alpha(1f).setDuration(140).start()
            }.start()
            return
        }
        val content = findViewById<FrameLayout>(android.R.id.content) ?: return
        val startRect = rectIn(content, fromView)
        val headerView = recycler.findViewHolderForAdapterPosition(0)?.itemView
        val endRect = if (headerView != null) rectIn(content, headerView) else run {
            val rvRect = rectIn(content, recycler)
            val w = startRect.width()
            val h = startRect.height()
            RectF(rvRect.left + dp(16f), rvRect.top + dp(8f), rvRect.left + dp(16f) + w, rvRect.top + dp(8f) + h)
        }
        if (startRect.width() <= 0 || startRect.height() <= 0) { onSwitchRoot(rootId, null); return }
        val bmp = createBitmap(fromView.width.coerceAtLeast(1), fromView.height.coerceAtLeast(1))
        val canvas = Canvas(bmp)
        fromView.draw(canvas)
        val iv = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            setImageBitmap(bmp)
            layoutParams = FrameLayout.LayoutParams(startRect.width().toInt(), startRect.height().toInt())
            x = startRect.left
            y = startRect.top
        }
        content.addView(iv)
        recycler.animate().alpha(0.15f).setDuration(120).start()
        val sx = (endRect.width() / startRect.width()).coerceAtLeast(0.1f)
        val sy = (endRect.height() / startRect.height()).coerceAtLeast(0.1f)
        iv.animate()
            .x(endRect.left)
            .y(endRect.top)
            .scaleX(sx)
            .scaleY(sy)
            .setDuration(260)
            .withEndAction {
                content.removeView(iv)
                selectedRootId = rootId
                selectedParentId = rootId
                hasUserClosedRoot = false
                seedCollapsedForRoot(rootId)
                render()
                recycler.alpha = 0.15f
                recycler.animate().alpha(1f).setDuration(180).start()
            }
            .start()
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

        etTitle.setText(dto.label)
        etDesc.setText(dto.description ?: "")
        val items = listOf("To Do", "In Progress", "Done")
        val states = listOf(State.TODO, State.IN_PROGRESS, State.DONE)
        val spinnerAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spState.adapter = spinnerAdapter
        spState.setSelection(states.indexOf(dto.state).coerceAtLeast(0))

        val last = overrideModified[id] ?: dto.modified ?: dto.created
        tvUpdated.text = getString(R.string.last_updated, formatDisplayTime(last))

        val dialog = AlertDialog.Builder(this).setView(v).create()

        btnSave.setOnClickListener {
            val newLabel = etTitle.text?.toString()?.trim().orEmpty()
            val newDesc = etDesc.text?.toString()?.trim().orEmpty()
            val newState = states.getOrNull(spState.selectedItemPosition) ?: dto.state

            val prevLabel = dto.label
            val prevDesc = dto.description
            val prevState = dto.state

            if (newLabel.isNotEmpty() && newLabel != dto.label) overrideLabels[id] = newLabel
            overrideDescriptions[id] = newDesc.ifEmpty { null }
            overrideStates[id] = newState
            overrideModified[id] = LocalDateTime.now().toString()
            render()

            val req = ToDoRequest(
                label = newLabel.ifEmpty { prevLabel },
                state = newState,
                color = dto.color,
                description = newDesc.ifEmpty { null },
                parentId = dto.parentId
            )
            api.updateToDo(
                id = id.toString(),
                data = req,
                onSuccess = {
                    clearOverridesFor(id)
                    refreshFromServer()
                    dialog.dismiss()
                },
                onError = { msg ->
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
                            if (selectedParentId == id) selectedParentId = dto.parentId
                            if (selectedRootId == id) selectedRootId = null
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            refreshFromServer()
                        },
                        onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    override fun onToggleExpand(id: UUID) {
        if (!collapsed.add(id)) collapsed.remove(id)
        render()
    }

    // New: close the currently open root, show only root list
    override fun onCloseRoot() {
        selectedRootId = null
        selectedParentId = null
        hasUserClosedRoot = true
        render()
    }

    private var colorDialog: AlertDialog? = null

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
        val grid = GridLayout(this).apply { columnCount = 6 }
        val sizePx = (28 * resources.displayMetrics.density).toInt()
        val marginPx = (8 * resources.displayMetrics.density).toInt()
        colors.forEach { hex ->
            val v = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply { setMargins(marginPx, marginPx, marginPx, marginPx) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    try { setColor(hex.toColorInt()) } catch (_: Exception) { setColor(Color.LTGRAY) }
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
        colorDialog = AlertDialog.Builder(this).setTitle("Pick a color").setView(container).create()
        colorDialog?.show()
    }

    private fun updateColorChip() {
        val gd = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            val strokePx = (1 * resources.displayMetrics.density).toInt()
            setStroke(strokePx, "#BDBDBD".toColorInt())
            val fill = selectedRootColor?.let { try {
                it.toColorInt() } catch (_: Exception) { null } }
            setColor(fill ?: Color.WHITE)
        }
        btnPickColor.background = gd
    }

    private fun clearOverridesFor(id: UUID) {
        overrideStates.remove(id)
        overrideLabels.remove(id)
        overrideDescriptions.remove(id)
        overrideModified.remove(id)
    }

    private fun rectIn(container: ViewGroup, v: View): RectF {
        val locV = IntArray(2)
        val locC = IntArray(2)
        v.getLocationOnScreen(locV)
        container.getLocationOnScreen(locC)
        val left = (locV[0] - locC[0]).toFloat()
        val top = (locV[1] - locC[1]).toFloat()
        val right = left + v.width
        val bottom = top + v.height
        return RectF(left, top, right, bottom)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun formatDisplayTime(isoOrOther: String): String = try {
        val dt = LocalDateTime.parse(isoOrOther)
        DateTimeFormatter.ofPattern("MMM d, yyyy  —  h:mma").format(dt)
    } catch (_: Exception) { isoOrOther }

    // Seed all nodes with children under a root as collapsed so only first-level is visible
    private fun seedCollapsedForRoot(rootId: UUID) {
        collapsed.clear()
        val root = roots.find { it.id == rootId } ?: return
        val firstLevel = mergeAndSortByCreatedDesc(root.children, newTasksByRoot[root.id] ?: emptyList())
        collapsed.addAll(collectIdsWithChildren(firstLevel, startDepth = 0, minDepthToCollapse = 0))
    }

    // Helper: collect IDs of nodes with children, collapsing only from a minimum depth
    private fun collectIdsWithChildren(nodes: List<ToDoDto>, startDepth: Int, minDepthToCollapse: Int): Set<UUID> {
        val out = mutableSetOf<UUID>()
        fun walk(list: List<ToDoDto>, depth: Int) {
            list.forEach { n ->
                val kids = mergeAndSortByCreatedDesc(n.children, newTasksByRoot[n.id] ?: emptyList())
                if (kids.isNotEmpty() && depth >= minDepthToCollapse) out += n.id
                if (kids.isNotEmpty()) walk(kids, depth + 1)
            }
        }
        walk(nodes, startDepth)
        return out
    }

    // === New helpers for computing latest timestamp across a root subtree ===
    private fun parseAnyDateTime(value: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(value)
        } catch (_: Exception) {
            try {
                java.time.OffsetDateTime.parse(value).toLocalDateTime()
            } catch (_: Exception) {
                try {
                    java.time.ZonedDateTime.parse(value).toLocalDateTime()
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private fun computeLatestTimestampIso(root: ToDoDto): String {
        val best: LocalDateTime? = null
        var bestRaw: String? = null

        fun consider(raw: String?) {
            if (raw == null) return
            val parsed = parseAnyDateTime(raw)
            if (best == null) {
                bestRaw = raw
                return
            }
            if (parsed != null) {
                if (parsed.isAfter(best)) {
                    bestRaw = raw
                }
            } else  {
                // Both unparseable, keep any value
                bestRaw = raw
            }
        }

        fun walk(node: ToDoDto) {
            val d = applyOverrides(node)
            val candidate = overrideModified[d.id] ?: d.modified ?: d.created
            consider(candidate)
            val kids = (d.children) + (newTasksByRoot[d.id] ?: emptyList())
            kids.forEach { walk(it) }
        }

        walk(root)
        return bestRaw ?: root.created
    }

    private fun formatHeaderTime(isoOrOther: String): String = try {
        val dt = parseAnyDateTime(isoOrOther) ?: return isoOrOther
        DateTimeFormatter.ofPattern("MMMM, dd yyyy  —  h:mma").format(dt)
    } catch (_: Exception) { isoOrOther }

    // Merge two lists and sort by created desc
    private fun mergeAndSortByCreatedDesc(a: List<ToDoDto>, b: List<ToDoDto>): List<ToDoDto> =
        (a + b).sortedByDescending { parseAnyDateTime(it.created) ?: LocalDateTime.MIN }
}