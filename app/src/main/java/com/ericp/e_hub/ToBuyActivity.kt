package com.ericp.e_hub

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.adapters.tobuy.CategoryAccordionAdapter
import com.ericp.e_hub.adapters.tobuy.CategorySection
import com.ericp.e_hub.dto.ToBuyCategoryDto
import com.ericp.e_hub.dto.ToBuyDto
import com.ericp.e_hub.managers.AddToBuyModalManager
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class ToBuyActivity : FragmentActivity() {
    // UI
    private lateinit var backButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var searchEditText: EditText
    private lateinit var filterNotBoughtButton: Button
    private lateinit var filterAllButton: Button
    private lateinit var sortPriceButton: Button
    private lateinit var sortDateButton: Button
    private lateinit var yearFilterSpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var addItemFab: FloatingActionButton
    private lateinit var emptyStateLayout: View
    private lateinit var manageCategoriesButton: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var filterToggle: ImageButton

    // Data / helpers
    private lateinit var categoryAdapter: CategoryAccordionAdapter
    private lateinit var modalManager: AddToBuyModalManager
    private lateinit var apiHelper: EHubApiHelper
    private val allToBuyItems = mutableListOf<ToBuyDto>()
    private val filteredSections = mutableListOf<CategorySection>()

    // Filters / sorting
    private var showOnlyNotBought = true
    private var searchQuery = ""
    private var selectedYear: Int? = null

    private enum class SortMode { NONE, PRICE_DESC, PRICE_ASC, DATE_NEWEST, DATE_OLDEST }
    private var sortMode: SortMode = SortMode.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_to_buy)

        initializeComponents()
        setupRecyclerView()
        setupModalManager()
        setupListeners()

        apiHelper = EHubApiHelper(this)
        fetchToBuyItems()
        updateEmptyState()
    }

    override fun onResume() {
        super.onResume()
        fetchToBuyItems()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        titleTextView = findViewById(R.id.titleTextView)
        totalPriceText = findViewById(R.id.totalPriceText)
        searchEditText = findViewById(R.id.searchEditText)
        filterNotBoughtButton = findViewById(R.id.filterNotBoughtButton)
        filterAllButton = findViewById(R.id.filterAllButton)
        sortPriceButton = findViewById(R.id.sortPriceButton)
        sortDateButton = findViewById(R.id.sortDateButton)
        yearFilterSpinner = findViewById(R.id.yearFilterSpinner)
        recyclerView = findViewById(R.id.toBuyRecyclerView)
        addItemFab = findViewById(R.id.addItemFab)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        manageCategoriesButton = findViewById(R.id.manageCategoriesButton)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        filterToggle = findViewById(R.id.filterToggle)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAccordionAdapter(
            filteredSections,
            onItemClick = { item -> openToBuyDetails(item) },
            onItemLongClick = { item ->
                val position = allToBuyItems.indexOfFirst { it.id == item.id }
                if (position >= 0) showItemContextMenu(position, item)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = categoryAdapter
    }

    private fun openToBuyDetails(item: ToBuyDto) {
        val intent = Intent(this, ToBuyDetailsActivity::class.java)
        intent.putExtra("TOBUY_ID", item.id.toString())
        intent.putExtra("TOBUY_TITLE", item.title)
        intent.putExtra("TOBUY_DESCRIPTION", item.description)
        intent.putExtra("TOBUY_CRITERIA", item.criteria)
        intent.putExtra("TOBUY_BOUGHT", item.bought)
        intent.putExtra("TOBUY_ESTIMATED_PRICE", item.estimatedPrice)
        intent.putExtra("TOBUY_INTEREST", item.interest)
        startActivity(intent)
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
            }.show()
    }

    private fun editItem(position: Int, item: ToBuyDto) {
        modalManager.showModal(item) { updatedItem -> updateToBuyItem(position, updatedItem) }
    }

    private fun confirmDeleteItem(position: Int, item: ToBuyDto) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete \"${item.title}\"?")
            .setPositiveButton("Delete") { _, _ -> deleteItem(position, item) }
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
            allToBuyItems[position] = updatedItem
            updateCategorySections()
            Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupModalManager() {
        modalManager = AddToBuyModalManager(this) { newItem -> addToBuyItem(newItem) }
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
        addItemFab.setOnClickListener { modalManager.showModal() }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim().orEmpty()
                updateCategorySections()
            }
        })

        filterNotBoughtButton.setOnClickListener { setFilterState(notBought = true) }
        filterAllButton.setOnClickListener { setFilterState(notBought = false) }

        sortPriceButton.setOnClickListener {
            sortMode = when (sortMode) {
                SortMode.PRICE_DESC -> SortMode.PRICE_ASC
                SortMode.PRICE_ASC -> SortMode.NONE
                else -> SortMode.PRICE_DESC
            }
            updateSortButtonsUI()
            updateCategorySections()
        }

        sortDateButton.setOnClickListener {
            sortMode = when (sortMode) {
                SortMode.DATE_NEWEST -> SortMode.DATE_OLDEST
                SortMode.DATE_OLDEST -> SortMode.NONE
                else -> SortMode.DATE_NEWEST
            }
            updateSortButtonsUI()
            updateCategorySections()
        }

        yearFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val value = parent.getItemAtPosition(position) as String
                selectedYear = if (value == "All") null else value.toIntOrNull()
                updateCategorySections()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        manageCategoriesButton.setOnClickListener {
            val intent = Intent(this, ToBuyCategoriesActivity::class.java)
            startActivity(intent)
        }

        swipeRefreshLayout.setOnRefreshListener {
            fetchToBuyItems()
        }

        filterToggle.setOnClickListener {
            val searchFilterLayout: LinearLayout = findViewById(R.id.searchFilterLayout)
            searchFilterLayout.isVisible = !searchFilterLayout.isVisible
        }
    }

    private fun setFilterState(notBought: Boolean) {
        showOnlyNotBought = notBought
        filterNotBoughtButton.setTextColor(if (notBought) getColor(android.R.color.white) else "#2196F3".toColorInt())
        filterAllButton.setTextColor(if (notBought) "#666666".toColorInt() else getColor(android.R.color.white))
        filterNotBoughtButton.isSelected = notBought
        filterAllButton.isSelected = !notBought
        updateCategorySections()
    }

    private fun extractYear(created: String?): Int? {
        if (created.isNullOrBlank()) return null
        return created.take(4).toIntOrNull()
    }

    private fun parseTimestamp(created: String?): Long {
        if (created.isNullOrBlank()) return 0L
        return try {
            Instant.parse(created).toEpochMilli()
        } catch (_: Exception) {
            try {
                LocalDate.parse(created).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                created.filter { it.isDigit() }.toLongOrNull() ?: 0L
            }
        }
    }

    private fun updateYearFilterOptions() {
        val years = allToBuyItems
            .mapNotNull { extractYear(it.created) }
            .distinct()
            .sortedDescending()
            .map { it.toString() }

        val entries = listOf("All") + years

        val adapter = object : ArrayAdapter<String>(this, R.layout.spinner_year_item, entries) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.spinner_year_dropdown_item, parent, false)
                val tv = view.findViewById<TextView>(R.id.spinnerYearText)
                tv.text = getItem(position)
                return view
            }
        }
        yearFilterSpinner.adapter = adapter

        selectedYear?.let { yr ->
            val idx = entries.indexOf(yr.toString())
            if (idx >= 0) yearFilterSpinner.setSelection(idx)
        }
    }

    private fun updateSortButtonsUI() {
        sortPriceButton.text = when (sortMode) {
            SortMode.PRICE_DESC -> "Price ↓"
            SortMode.PRICE_ASC -> "Price ↑"
            else -> "Price"
        }
        sortDateButton.text = when (sortMode) {
            SortMode.DATE_NEWEST -> "Date ↓"
            SortMode.DATE_OLDEST -> "Date ↑"
            else -> "Date"
        }
        val activeColor = "#2196F3".toColorInt()
        val inactiveColor = "#666666".toColorInt()
        fun setBtn(btn: Button, active: Boolean) {
            btn.isSelected = active
            btn.setTextColor(if (active) activeColor else inactiveColor)
        }
        setBtn(sortPriceButton, sortMode == SortMode.PRICE_DESC || sortMode == SortMode.PRICE_ASC)
        setBtn(sortDateButton, sortMode == SortMode.DATE_NEWEST || sortMode == SortMode.DATE_OLDEST)
    }

    private fun updateCategorySections() {
        val filteredItems = allToBuyItems.filter { item ->
            val matchesNotBought = if (showOnlyNotBought) item.bought == null else true
            val matchesSearch = searchQuery.isEmpty() || item.title.contains(searchQuery, true)
            val matchesYear = selectedYear?.let { extractYear(item.created) == it } ?: true
            matchesNotBought && matchesSearch && matchesYear
        }

        val categoryMap = mutableMapOf<String, MutableList<ToBuyDto>>()
        for (item in filteredItems) {
            val categories = item.categories.takeIf { it.isNotEmpty() } ?: listOf(ToBuyCategoryDto(name = "Uncategorized"))
            for (cat in categories) {
                val name = cat.name ?: "Uncategorized"
                categoryMap.getOrPut(name) { mutableListOf() }.add(item)
            }
        }

        data class CategoryMeta(
            val category: ToBuyCategoryDto,
            val items: List<ToBuyDto>,
            val totalPrice: Int,
            val mostRecentTs: Long,
            val oldestTs: Long
        )

        val metas = categoryMap.map { (name, items) ->
            val totalPrice = items.sumOf { it.estimatedPrice ?: 0 }
            val timestamps = items.map { parseTimestamp(it.created) }.ifEmpty { listOf(0L) }
            val mostRecent = timestamps.maxOrNull() ?: 0L
            val oldest = timestamps.minOrNull() ?: 0L

            val categoryDto = allToBuyItems.flatMap { it.categories }.find { it.name == name }
                ?: ToBuyCategoryDto(name = name)

            val sortedItems = when (sortMode) {
                SortMode.PRICE_DESC -> items.sortedByDescending { it.estimatedPrice ?: 0 }
                SortMode.PRICE_ASC -> items.sortedBy { it.estimatedPrice ?: 0 }
                SortMode.DATE_NEWEST -> items.sortedByDescending { parseTimestamp(it.created) }
                SortMode.DATE_OLDEST -> items.sortedBy { parseTimestamp(it.created) }
                SortMode.NONE -> items.sortedBy { it.title.lowercase() }
            }

            CategoryMeta(categoryDto, sortedItems, totalPrice, mostRecent, oldest)
        }

        val sortedMetas = when (sortMode) {
            SortMode.PRICE_DESC -> metas.sortedByDescending { it.totalPrice }
            SortMode.PRICE_ASC -> metas.sortedBy { it.totalPrice }
            SortMode.DATE_NEWEST -> metas.sortedByDescending { it.mostRecentTs }
            SortMode.DATE_OLDEST -> metas.sortedBy { it.oldestTs }
            SortMode.NONE -> metas.sortedBy { (it.category.name ?: "").lowercase() }
        }

        val sections = sortedMetas.map { CategorySection(it.category, it.items, false) }

        filteredSections.clear()
        filteredSections.addAll(sections)
        categoryAdapter.updateSections(sections)

        updateTotalPrice(filteredItems)
        updateEmptyState()
        updateManageCategoriesButtonVisibility()
    }

    private fun updateManageCategoriesButtonVisibility() {
        // Show the manage categories button if there are any categories with items
        val hasCategories = allToBuyItems.any { it.categories.isNotEmpty() }
        manageCategoriesButton.visibility = if (hasCategories) View.VISIBLE else View.GONE
    }

    private fun updateTotalPrice(items: List<ToBuyDto>) {
        val totalPrice = items.sumOf { it.estimatedPrice ?: 0 }
        totalPriceText.text = getString(R.string.price, totalPrice)
    }

    private fun addToBuyItem(item: ToBuyDto) {
        allToBuyItems.add(item)
        updateYearFilterOptions()
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
                runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false
                }
                try {
                    val gson = Gson()
                    val toBuyListType = object : TypeToken<List<ToBuyDto>>() {}.type
                    val fetchedItems: List<ToBuyDto> = gson.fromJson(response, toBuyListType)
                    allToBuyItems.clear()
                    allToBuyItems.addAll(fetchedItems)
                    updateYearFilterOptions()
                    setFilterState(notBought = true)
                    updateSortButtonsUI()
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to parse ToBuy items: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "Failed to load ToBuy items: $error", Toast.LENGTH_LONG).show()
                    updateEmptyState()
                }
            }
        )
    }
}
