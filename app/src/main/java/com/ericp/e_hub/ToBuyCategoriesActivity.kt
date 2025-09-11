package com.ericp.e_hub

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.adapters.tobuy.ToBuyCategoryAdapter
import com.ericp.e_hub.dto.ToBuyCategoryDto
import com.ericp.e_hub.managers.AddCategoryModalManager
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ToBuyCategoriesActivity : FragmentActivity() {
    // UI Components
    private lateinit var backButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var addCategoryFab: FloatingActionButton
    private lateinit var emptyStateLayout: View

    // Data and helpers
    private lateinit var categoryAdapter: ToBuyCategoryAdapter
    private lateinit var modalManager: AddCategoryModalManager
    private lateinit var apiHelper: EHubApiHelper
    private val categories = mutableListOf<ToBuyCategoryDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tobuy_categories)

        initializeComponents()
        setupRecyclerView()
        setupModalManager()
        setupListeners()
        setupSwipeToDelete()

        apiHelper = EHubApiHelper(this)
        fetchCategories()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        titleTextView = findViewById(R.id.titleTextView)
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView)
        addCategoryFab = findViewById(R.id.addCategoryFab)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupRecyclerView() {
        categoryAdapter = ToBuyCategoryAdapter(
            categories = categories,
            onEditClick = { category -> editCategory(category) },
            onDeleteClick = { category -> confirmDeleteCategory(category) }
        )
        categoriesRecyclerView.layoutManager = LinearLayoutManager(this)
        categoriesRecyclerView.adapter = categoryAdapter
    }

    private fun setupModalManager() {
        modalManager = AddCategoryModalManager(this) { newCategory ->
            addCategory(newCategory)
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
        addCategoryFab.setOnClickListener { modalManager.showModal() }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val category = categories[position]

                when (direction) {
                    ItemTouchHelper.RIGHT -> confirmDeleteCategory(category)
                    ItemTouchHelper.LEFT -> editCategory(category)
                }
                // We manually reset the view, so we notify the adapter.
                categoryAdapter.notifyItemChanged(position)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                // a value of 1f or higher means the user has to swipe the entire width to trigger onSwiped
                return 0.2f
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
                        setColor(if (dX > 0) "#f44336".toColorInt() else "#4CAF50".toColorInt())
                    }

                    val icon: Drawable? = if (dX > 0) { // Right swipe - Delete
                        ContextCompat.getDrawable(this@ToBuyCategoriesActivity, android.R.drawable.ic_menu_delete)
                    } else { // Left swipe - Edit
                        ContextCompat.getDrawable(this@ToBuyCategoriesActivity, android.R.drawable.ic_menu_edit)
                    }

                    background.setBounds(
                        itemView.left, itemView.top,
                        itemView.right, itemView.bottom
                    )
                    background.draw(canvas)

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

        itemTouchHelper.attachToRecyclerView(categoriesRecyclerView)
    }

    private fun editCategory(category: ToBuyCategoryDto) {
        modalManager.showModal(category) { updatedCategory ->
            updateCategory(updatedCategory)
        }
    }

    private fun confirmDeleteCategory(category: ToBuyCategoryDto) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete \"${category.name}\"?")
            .setPositiveButton("Delete") { _, _ -> deleteCategory(category) }
            .setNegativeButton("Cancel") { _, _ ->
                // Reset the swipe position
                categoryAdapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun addCategory(category: ToBuyCategoryDto) {
        apiHelper.postDataAsync(
            endpoint = Endpoints.TOBUY_CATEGORIES,
            data = category,
            onSuccess = { response ->
                runOnUiThread {
                    try {
                        val gson = Gson()
                        val newCategory: ToBuyCategoryDto = gson.fromJson(response, ToBuyCategoryDto::class.java)
                        categories.add(newCategory)
                        categoryAdapter.notifyItemInserted(categories.size - 1)
                        updateEmptyState()
                        apiHelper.clearToBuyCache()
                        Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to parse new category: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to add category: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun updateCategory(category: ToBuyCategoryDto) {
        if (category.id != null) {
            apiHelper.putAsync(
                endpoint = "${Endpoints.TOBUY_CATEGORIES}/${category.id}",
                data = category,
                onSuccess = { response ->
                    runOnUiThread {
                        try {
                            val gson = Gson()
                            val updatedCategory: ToBuyCategoryDto = gson.fromJson(response, ToBuyCategoryDto::class.java)
                            val index = categories.indexOfFirst { it.id == category.id }
                            if (index >= 0) {
                                categories[index] = updatedCategory
                                categoryAdapter.notifyItemChanged(index)
                                apiHelper.clearToBuyCache()
                                Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Failed to parse updated category: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to update category: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun deleteCategory(category: ToBuyCategoryDto) {
        if (category.id != null) {
            apiHelper.deleteAsync(
                endpoint = "${Endpoints.TOBUY_CATEGORIES}/${category.id}",
                onSuccess = {
                    runOnUiThread {
                        val index = categories.indexOfFirst { it.id == category.id }
                        if (index >= 0) {
                            categories.removeAt(index)
                            categoryAdapter.notifyItemRemoved(index)
                            updateEmptyState()
                            apiHelper.clearToBuyCache()
                            Toast.makeText(this, "Category deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to delete category: $error", Toast.LENGTH_LONG).show()
                        // Reset the adapter to show the item again
                        categoryAdapter.notifyDataSetChanged()
                    }
                }
            )
        }
    }

    private fun updateEmptyState() {
        if (categories.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            categoriesRecyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            categoriesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun fetchCategories() {
        apiHelper.fetchDataAsync(
            endpoint = Endpoints.TOBUY_CATEGORIES,
            onSuccess = { response ->
                try {
                    val gson = Gson()
                    val categoryListType = object : TypeToken<List<ToBuyCategoryDto>>() {}.type
                    val fetchedCategories: List<ToBuyCategoryDto> = gson.fromJson(response, categoryListType)
                    categories.clear()
                    categories.addAll(fetchedCategories)
                    runOnUiThread {
                        categoryAdapter.notifyDataSetChanged()
                        updateEmptyState()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to parse categories: ${e.message}", Toast.LENGTH_LONG).show()
                        updateEmptyState()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to load categories: $error", Toast.LENGTH_LONG).show()
                    updateEmptyState()
                }
            }
        )
    }
}
