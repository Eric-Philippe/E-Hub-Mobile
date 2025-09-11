package com.ericp.e_hub

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.adapters.tobuy.ToBuyLinkAdapter
import com.ericp.e_hub.dto.ToBuyDto
import com.ericp.e_hub.dto.ToBuyLinkDto
import com.ericp.e_hub.managers.AddToBuyModalManager
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import com.google.gson.Gson
import java.util.UUID

class ToBuyDetailsActivity : FragmentActivity() {
    private lateinit var backButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button

    // Details views
    private lateinit var descriptionLayout: LinearLayout
    private lateinit var descriptionText: TextView
    private lateinit var criteriaLayout: LinearLayout
    private lateinit var criteriaText: TextView
    private lateinit var interestLayout: LinearLayout
    private lateinit var interestText: TextView
    private lateinit var priceLayout: LinearLayout
    private lateinit var priceText: TextView
    private lateinit var boughtLayout: LinearLayout
    private lateinit var boughtText: TextView

    // Links views
    private lateinit var linksCard: CardView
    private lateinit var linksRecyclerView: RecyclerView
    private lateinit var emptyLinksLayout: LinearLayout

    private lateinit var apiHelper: EHubApiHelper
    private lateinit var modalManager: AddToBuyModalManager
    private var currentItem: ToBuyDto? = null
    private var itemId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tobuy_details)

        initializeComponents()
        setupModalManager()
        loadItemData()
        setupListeners()
    }

    private fun initializeComponents() {
        backButton = findViewById(R.id.backButton)
        titleTextView = findViewById(R.id.titleTextView)
        editButton = findViewById(R.id.editButton)
        deleteButton = findViewById(R.id.deleteButton)

        // Details views
        descriptionLayout = findViewById(R.id.descriptionLayout)
        descriptionText = findViewById(R.id.descriptionText)
        criteriaLayout = findViewById(R.id.criteriaLayout)
        criteriaText = findViewById(R.id.criteriaText)
        interestLayout = findViewById(R.id.interestLayout)
        interestText = findViewById(R.id.interestText)
        priceLayout = findViewById(R.id.priceLayout)
        priceText = findViewById(R.id.priceText)
        boughtLayout = findViewById(R.id.boughtLayout)
        boughtText = findViewById(R.id.boughtText)

        // Links views
        linksCard = findViewById(R.id.linksCard)
        linksRecyclerView = findViewById(R.id.linksRecyclerView)
        emptyLinksLayout = findViewById(R.id.emptyLinksLayout)

        apiHelper = EHubApiHelper(this)
    }

    private fun setupModalManager() {
        modalManager = AddToBuyModalManager(this) { newItem ->
            // Callback after creating a new item
            // Not used in this activity
        }
    }

    private fun loadItemData() {
        // Get item ID from intent
        itemId = intent.getStringExtra("TOBUY_ID")

        if (itemId != null && itemId != "null") {
            // Fetch full item data from API
            fetchItemFromApi(itemId!!)
        } else {
            // Use data from intent extras
            loadFromIntentExtras()
        }
    }

    private fun fetchItemFromApi(id: String) {
        apiHelper.fetchDataAsync(
            endpoint = "${Endpoints.TOBUY}/$id",
            onSuccess = { response ->
                try {
                    val gson = Gson()
                    val item: ToBuyDto = gson.fromJson(response, ToBuyDto::class.java)
                    currentItem = item
                    runOnUiThread {
                        displayItemData(item)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to parse item data: ${e.message}", Toast.LENGTH_LONG).show()
                        loadFromIntentExtras()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to load item: $error", Toast.LENGTH_LONG).show()
                    loadFromIntentExtras()
                }
            }
        )
    }

    private fun loadFromIntentExtras() {
        val item = ToBuyDto(
            id = if (itemId != "null") UUID.fromString(itemId) else null,
            title = intent.getStringExtra("TOBUY_TITLE") ?: "",
            description = intent.getStringExtra("TOBUY_DESCRIPTION"),
            criteria = intent.getStringExtra("TOBUY_CRITERIA"),
            bought = intent.getStringExtra("TOBUY_BOUGHT"),
            estimatedPrice = intent.getIntExtra("TOBUY_ESTIMATED_PRICE", 0).takeIf { it != 0 },
            interest = intent.getStringExtra("TOBUY_INTEREST"),
            categories = emptyList(),
            links = emptyList()
        )
        currentItem = item
        displayItemData(item)
    }

    private fun displayItemData(item: ToBuyDto) {
        // Set title
        titleTextView.text = item.title

        // Set description
        if (!item.description.isNullOrBlank()) {
            descriptionText.text = item.description
            descriptionLayout.visibility = View.VISIBLE
        } else {
            descriptionLayout.visibility = View.GONE
        }

        // Set criteria
        if (!item.criteria.isNullOrBlank()) {
            criteriaText.text = item.criteria
            criteriaLayout.visibility = View.VISIBLE
        } else {
            criteriaLayout.visibility = View.GONE
        }

        // Set interest
        if (!item.interest.isNullOrBlank()) {
            interestText.text = item.interest
            interestLayout.visibility = View.VISIBLE
        } else {
            interestLayout.visibility = View.GONE
        }

        // Set price
        if (item.estimatedPrice != null && item.estimatedPrice > 0) {
            priceText.text = "${item.estimatedPrice}€"
            priceLayout.visibility = View.VISIBLE
        } else {
            priceLayout.visibility = View.GONE
        }

        // Set bought status
        if (!item.bought.isNullOrBlank()) {
            boughtText.text = "Yes"
            boughtLayout.visibility = View.VISIBLE
        } else {
            boughtLayout.visibility = View.GONE
        }

        // Setup links
        setupLinks(item.links)
    }

    private fun setupLinks(links: List<ToBuyLinkDto>) {
        if (links.isNotEmpty()) {
            linksCard.visibility = View.VISIBLE
            emptyLinksLayout.visibility = View.GONE

            val adapter = ToBuyLinkAdapter(links)
            linksRecyclerView.layoutManager = LinearLayoutManager(this)
            linksRecyclerView.adapter = adapter
        } else {
            linksCard.visibility = View.GONE
            emptyLinksLayout.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        editButton.setOnClickListener {
            currentItem?.let { item ->
                modalManager.showModal(item) { updatedItem ->
                    updateItem(updatedItem)
                }
            }
        }

        deleteButton.setOnClickListener {
            confirmDeleteItem()
        }
    }

    private fun updateItem(updatedItem: ToBuyDto) {
        if (updatedItem.id != null) {
            apiHelper.putAsync(
                endpoint = "${Endpoints.TOBUY}/${updatedItem.id}",
                data = updatedItem,
                onSuccess = { response ->
                    runOnUiThread {
                        try {
                            val gson = Gson()
                            val updated: ToBuyDto = gson.fromJson(response, ToBuyDto::class.java)
                            currentItem = updated
                            displayItemData(updated)
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
            // For items without ID, just update display
            currentItem = updatedItem
            displayItemData(updatedItem)
            Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteItem() {
        currentItem?.let { item ->
            AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete \"${item.title}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteItem(item)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteItem(item: ToBuyDto) {
        if (item.id != null) {
            apiHelper.deleteAsync(
                endpoint = "${Endpoints.TOBUY}/${item.id}",
                onSuccess = {
                    runOnUiThread {
                        apiHelper.clearToBuyCache()
                        Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to delete item: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else {
            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}