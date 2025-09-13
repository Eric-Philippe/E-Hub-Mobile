package com.ericp.e_hub.ui.todo

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.dto.State
import com.ericp.e_hub.dto.ToDoDto
import java.util.UUID

sealed class ToDoRow {
    data class Header(val rootId: UUID, val title: String, val subtitle: String) : ToDoRow()
    data class Task(val dto: ToDoDto, val level: Int, val selected: Boolean) : ToDoRow()
    data object Input : ToDoRow()
    data class Nav(val rootId: UUID, val title: String, val shadeIndex: Int) : ToDoRow()
}

class ToDoAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onToggleTask(id: UUID, newState: State)
        fun onAddTask(label: String)
        fun onSwitchRoot(rootId: UUID)
        fun onSelectTask(id: UUID)
        fun onOpenDetails(id: UUID)
        fun onSwitchRoot(rootId: UUID, fromView: View?)
    }

    private val items = mutableListOf<ToDoRow>()

    fun submitItems(newItems: List<ToDoRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Expose item for helpers (e.g., swipe-to-delete)
    fun getItem(position: Int): ToDoRow? = items.getOrNull(position)

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ToDoRow.Header -> VIEW_HEADER
        is ToDoRow.Task -> VIEW_TASK
        is ToDoRow.Input -> VIEW_INPUT
        is ToDoRow.Nav -> VIEW_NAV
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_HEADER -> HeaderVH(inf.inflate(R.layout.item_todo_header, parent, false))
            VIEW_TASK -> TaskVH(inf.inflate(R.layout.item_todo_task, parent, false))
            VIEW_INPUT -> InputVH(inf.inflate(R.layout.item_todo_input, parent, false))
            VIEW_NAV -> NavVH(inf.inflate(R.layout.item_todo_nav_section, parent, false))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is ToDoRow.Header -> (holder as HeaderVH).bind(row)
            is ToDoRow.Task -> (holder as TaskVH).bind(row)
            is ToDoRow.Input -> (holder as InputVH).bind()
            is ToDoRow.Nav -> (holder as NavVH).bind(row)
        }
    }

    private inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.tvRootTitle)
        private val subtitle: TextView = view.findViewById(R.id.tvSubTitle)
        fun bind(row: ToDoRow.Header) {
            title.text = row.title
            subtitle.text = row.subtitle
        }
    }

    private inner class TaskVH(view: View) : RecyclerView.ViewHolder(view) {
        private val cb: CheckBox = view.findViewById(R.id.cb)
        private val tv: TextView = view.findViewById(R.id.tv)
        fun bind(row: ToDoRow.Task) {
            val d = row.dto
            // Indentation by level
            val padStart = (row.level * 16)
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, padStart.toFloat(), itemView.resources.displayMetrics
            ).toInt()
            (itemView as ViewGroup).setPadding(px, itemView.paddingTop, itemView.paddingRight, itemView.paddingBottom)

            tv.text = d.label
            val isDone = d.state == State.DONE
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = isDone

            // Strike-through and text color
            tv.paintFlags = if (isDone) tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG else tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            tv.setTextColor(itemView.resources.getColor(if (isDone) R.color.gray_500 else R.color.gray_700, null))

            // Background color used only when DONE
            if (isDone) {
                val bg = GradientDrawable().apply {
                    cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, itemView.resources.displayMetrics)
                    try {
                        val c = if (d.color.isNullOrBlank()) "#FFE0E0E0" else d.color
                        // apply with low alpha for a soft erase effect
                        val parsed = Color.parseColor(c)
                        val withAlpha = Color.argb(48, Color.red(parsed), Color.green(parsed), Color.blue(parsed))
                        setColor(withAlpha)
                    } catch (e: Exception) {
                        setColor(Color.argb(32, 0, 0, 0))
                    }
                }
                tv.background = bg
            } else {
                tv.background = null
            }

            // Selected visual highlight on the whole row
            val selColor = itemView.resources.getColor(R.color.gray_100, null)
            itemView.setBackgroundColor(if (row.selected) selColor else Color.TRANSPARENT)

            cb.setOnCheckedChangeListener { _, checked ->
                listener.onToggleTask(d.id, if (checked) State.DONE else State.TODO)
            }

            // Select this task as parent when tapping the row (excluding checkbox default behavior)
            itemView.setOnClickListener { listener.onSelectTask(d.id) }

            // Long-press to open details panel
            itemView.setOnLongClickListener {
                listener.onOpenDetails(d.id)
                true
            }
        }
    }

    private inner class InputVH(view: View) : RecyclerView.ViewHolder(view) {
        private val et: EditText = view.findViewById(R.id.etNewTask)
        fun bind() {
            et.setOnEditorActionListener { v, _, _ ->
                val text = v.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    listener.onAddTask(text)
                    v.text = null
                }
                true
            }
            // Avoid text watcher stacking in recycled views
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private inner class NavVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tvNavTitle)
        fun bind(row: ToDoRow.Nav) {
            tv.text = row.title
            // Alternate background shades
            val shade = when (row.shadeIndex % 4) {
                0 -> R.color.gray_100
                1 -> R.color.gray_200
                2 -> R.color.gray_300
                else -> R.color.gray_400
            }
            itemView.setBackgroundResource(shade)
            itemView.setOnClickListener { listener.onSwitchRoot(row.rootId, itemView) }
        }
    }

    companion object {
        private const val VIEW_HEADER = 1
        private const val VIEW_TASK = 2
        private const val VIEW_INPUT = 3
        private const val VIEW_NAV = 4
    }
}
