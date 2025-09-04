package com.ericp.e_hub

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ericp.e_hub.adapters.tobuy.CategoryAccordionAdapter
import com.ericp.e_hub.adapters.tobuy.CategorySection
import com.ericp.e_hub.dto.ToBuyDto
import com.ericp.e_hub.dto.ToBuyCategoryDto
import com.ericp.e_hub.managers.AddToBuyModalManager
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ToBuyActivity : FragmentActivity() {
    private lateinit var backButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var searchEditText: EditText
    private lateinit var filterNotBoughtButton: Button
    private lateinit var filterAllButton: Button
    private lateinit var sortButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var addItemFab: FloatingActionButton
    private lateinit var emptyStateLayout: View

    private lateinit var categoryAdapter: CategoryAccordionAdapter
    private lateinit var modalManager: AddToBuyModalManager
    private lateinit var apiHelper: EHubApiHelper
    private val allToBuyItems = mutableListOf<ToBuyDto>()
    private val filteredSections = mutableListOf<CategorySection>()

    // Filter states
    private var showOnlyNotBought = true
    private var sortByPrice = false
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_to_buy)

        initializeComponents()
        setupRecyclerView()
        setupModalManager()
        setupListeners()

        // Initialize API helper and fetch existing items
        apiHelper = EHubApiHelper(this)
        fetchToBuyItems()

        updateEmptyState()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        titleTextView = findViewById(R.id.titleTextView)
        totalPriceText = findViewById(R.id.totalPriceText)
        searchEditText = findViewById(R.id.searchEditText)
        filterNotBoughtButton = findViewById(R.id.filterNotBoughtButton)
        filterAllButton = findViewById(R.id.filterAllButton)
        sortButton = findViewById(R.id.sortButton)
        recyclerView = findViewById(R.id.toBuyRecyclerView)
        addItemFab = findViewById(R.id.addItemFab)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAccordionAdapter(
            filteredSections,
            onItemClick = { item ->
                // Find the item in the original list to get its position
                val position = allToBuyItems.indexOfFirst { it.id == item.id }
                if (position >= 0) {
                    editItem(position, item)
                }
            },
            onItemLongClick = { item ->
                // Find the item in the original list to get its position
                val position = allToBuyItems.indexOfFirst { it.id == item.id }
                if (position >= 0) {
                    showItemContextMenu(position, item)
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = categoryAdapter
    }

    private fun showItemContextMenu(position: Int, item: ToBuyDto) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(this)
            .setTitle("Choose Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editItem(position, item)
                    1 -> confirmDeleteItem(position, item)
                }
            }
            .show()
    }

    private fun editItem(position: Int, item: ToBuyDto) {
        modalManager.showModal(item) { updatedItem ->
            updateToBuyItem(position, updatedItem)
        }
    }

    private fun confirmDeleteItem(position: Int, item: ToBuyDto) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete \"${item.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItem(position, item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(position: Int, item: ToBuyDto) {
        if (item.id != null) {
            apiHelper.deleteAsync(
                endpoint = "${Endpoints.TOBUY}/${item.id}",
                onSuccess = {
                    runOnUiThread {
                        allToBuyItems.removeAt(position)
                        updateCategorySections()
                        // Clear ToBuy cache after successful deletion
                        apiHelper.clearToBuyCache()
                        Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to delete item: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else {
            // For items without ID (local only), just remove from list
            allToBuyItems.removeAt(position)
            updateCategorySections()
            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToBuyItem(position: Int, updatedItem: ToBuyDto) {
        if (updatedItem.id != null) {
            apiHelper.putAsync(
                endpoint = "${Endpoints.TOBUY}/${updatedItem.id}",
                data = updatedItem,
                onSuccess = { response ->
                    runOnUiThread {
                        try {
                            val gson = Gson()
                            val updated: ToBuyDto = gson.fromJson(response, ToBuyDto::class.java)
                            allToBuyItems[position] = updated
                            updateCategorySections()
                            // Clear ToBuy cache after successful update
                            apiHelper.clearToBuyCache()
                            Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Failed to parse updated item: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to update item: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else {
            // For items without ID (local only), just update in list
            allToBuyItems[position] = updatedItem
            updateCategorySections()
            Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupModalManager() {
        modalManager = AddToBuyModalManager(this) { newItem ->
            addToBuyItem(newItem)
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        addItemFab.setOnClickListener {
            modalManager.showModal()
        }

        // Search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                updateCategorySections()
            }
        })

        // Filter buttons
        filterNotBoughtButton.setOnClickListener {
            setFilterState(notBought = true)
        }

        filterAllButton.setOnClickListener {
            setFilterState(notBought = false)
        }

        // Sort button
        sortButton.setOnClickListener {
            sortByPrice = !sortByPrice
            sortButton.text = if (sortByPrice) "Sort: Price ↓" else "Sort by Price"
            updateCategorySections()
        }
    }

    private fun setFilterState(notBought: Boolean) {
        showOnlyNotBought = notBought

        // Update button appearance with proper selected states
        if (notBought) {
            filterNotBoughtButton.isSelected = true
            filterNotBoughtButton.setTextColor(getColor(android.R.color.white))
            filterAllButton.isSelected = false
            filterAllButton.setTextColor(getColor(android.R.color.darker_gray))
        } else {
            filterAllButton.isSelected = true
            filterAllButton.setTextColor(getColor(android.R.color.white))
            filterNotBoughtButton.isSelected = false
            filterNotBoughtButton.setTextColor(getColor(android.R.color.darker_gray))
        }

        updateCategorySections()
    }

    private fun updateCategorySections() {
        // Filter items based on current criteria
        val filteredItems = allToBuyItems.filter { item ->
            val matchesFilter = if (showOnlyNotBought) item.bought == null else true
            val matchesSearch = if (searchQuery.isEmpty()) true else
                item.title.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }

        // Group items by categories
        val categoryMap = mutableMapOf<String, MutableList<ToBuyDto>>()

        for (item in filteredItems) {
            if (item.categories.isEmpty()) {
                // Handle items without categories
                val uncategorized = categoryMap.getOrPut("Uncategorized") { mutableListOf() }
                uncategorized.add(item)
            } else {
                // Add item to each of its categories
                for (category in item.categories) {
                    val categoryName = category.name ?: "Uncategorized"
                    val itemsInCategory = categoryMap.getOrPut(categoryName) { mutableListOf() }
                    itemsInCategory.add(item)
                }
            }
        }

        // Create category sections with collapsed state to prevent crashes
        val sections = categoryMap.map { (categoryName, items) ->
            val sortedItems = if (sortByPrice) {
                items.sortedByDescending { it.estimatedPrice ?: 0 }
            } else {
                items.sortedBy { it.title }
            }

            // Find the category DTO for this category name
            val categoryDto = allToBuyItems
                .flatMap { it.categories }
                .find { it.name == categoryName }
                ?: ToBuyCategoryDto(name = categoryName)

            // Always start with collapsed state when filtering to prevent crashes
            CategorySection(categoryDto, sortedItems, false)
        }.sortedBy { it.category.name }

        // Update the adapter
        filteredSections.clear()
        filteredSections.addAll(sections)
        categoryAdapter.updateSections(sections)

        // Update total price
        updateTotalPrice(filteredItems)
        updateEmptyState()
    }

    private fun updateTotalPrice(items: List<ToBuyDto>) {
        val totalPrice = items.sumOf { it.estimatedPrice ?: 0 }
        totalPriceText.text = getString(R.string.price, totalPrice)
    }

    private fun addToBuyItem(item: ToBuyDto) {
        allToBuyItems.add(item)
        updateCategorySections()
    }

    private fun updateEmptyState() {
        if (filteredSections.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun fetchToBuyItems() {
        apiHelper.fetchDataAsync(
            endpoint = Endpoints.TOBUY,
            onSuccess = { response ->
                try {
                    val gson = Gson()
                    val toBuyListType = object : TypeToken<List<ToBuyDto>>() {}.type
                    val fetchedItems: List<ToBuyDto> = gson.fromJson(response, toBuyListType)

                    // Clear existing items and add fetched items
                    allToBuyItems.clear()
                    allToBuyItems.addAll(fetchedItems)

                    // Initialize with default filter (not bought items)
                    setFilterState(notBought = true)

                } catch (e: Exception) {
                    Toast.makeText(this@ToBuyActivity, "Failed to parse ToBuy items: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                Toast.makeText(this@ToBuyActivity, "Failed to load ToBuy items: $error", Toast.LENGTH_LONG).show()
                updateEmptyState()
            }
        )
    }
}
