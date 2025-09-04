package com.ericp.e_hub.fragments.tobuy

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.adapters.tobuy.CategorySelectionAdapter
import com.ericp.e_hub.dto.ToBuyCategoryDto
import com.ericp.e_hub.models.ToBuyFormData
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.graphics.toColorInt

class AddToBuyStepTwoFragment : DialogFragment() {

    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var newCategoryEditText: EditText
    private lateinit var newCategoryDescriptionEditText: EditText
    private lateinit var newCategoryColorEditText: EditText
    private lateinit var colorPreviewIndicator: View
    private lateinit var addCategoryButton: Button
    private lateinit var backButton: Button
    private lateinit var nextButton: Button

    // Color picker buttons
    private lateinit var colorButton1: Button
    private lateinit var colorButton2: Button
    private lateinit var colorButton3: Button
    private lateinit var colorButton4: Button
    private lateinit var colorButton5: Button
    private lateinit var randomColorButton: Button

    private lateinit var categoryAdapter: CategorySelectionAdapter
    private var formData: ToBuyFormData? = null
    private var onNextClickListener: ((ToBuyFormData) -> Unit)? = null
    private var onBackClickListener: ((ToBuyFormData) -> Unit)? = null
    private lateinit var apiHelper: EHubApiHelper

    companion object {
        fun newInstance(formData: ToBuyFormData): AddToBuyStepTwoFragment {
            val fragment = AddToBuyStepTwoFragment()
            fragment.formData = formData
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_to_buy_step_two, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize API helper
        apiHelper = EHubApiHelper(requireContext())

        initializeViews(view)
        setupRecyclerView()
        setupListeners()

        // Pre-fill random color
        selectRandomColor()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    private fun initializeViews(view: View) {
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView)
        newCategoryEditText = view.findViewById(R.id.newCategoryEditText)
        newCategoryDescriptionEditText = view.findViewById(R.id.newCategoryDescriptionEditText)
        newCategoryColorEditText = view.findViewById(R.id.newCategoryColorEditText)
        colorPreviewIndicator = view.findViewById(R.id.colorPreviewIndicator)
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        backButton = view.findViewById(R.id.backButton)
        nextButton = view.findViewById(R.id.nextButton)

        // Initialize color picker buttons
        colorButton1 = view.findViewById(R.id.colorButton1)
        colorButton2 = view.findViewById(R.id.colorButton2)
        colorButton3 = view.findViewById(R.id.colorButton3)
        colorButton4 = view.findViewById(R.id.colorButton4)
        colorButton5 = view.findViewById(R.id.colorButton5)
        randomColorButton = view.findViewById(R.id.randomColorButton)
    }

    private fun setupRecyclerView() {
        // Initialize with empty list first
        categoryAdapter = CategorySelectionAdapter(
            mutableListOf(),
            formData?.categories ?: mutableListOf()
        )

        // Use GridLayoutManager with 2 columns for better space usage
        categoriesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        categoriesRecyclerView.adapter = categoryAdapter

        // Fetch categories from API
        fetchCategoriesFromApi()
    }

    private fun fetchCategoriesFromApi() {
        apiHelper.fetchDataAsync(
            endpoint = Endpoints.TOBUY_CATEGORIES,
            onSuccess = { response ->
                try {
                    val gson = Gson()
                    val categoryListType = object : TypeToken<List<ToBuyCategoryDto>>() {}.type
                    val apiCategories: List<ToBuyCategoryDto> = gson.fromJson(response, categoryListType)

                    val allCategories = apiCategories.toMutableList()

                    // Update adapter with API categories
                    updateCategoryAdapter(allCategories)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to parse categories: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(context, "Failed to load categories: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateCategoryAdapter(categories: MutableList<ToBuyCategoryDto>) {
        categoryAdapter = CategorySelectionAdapter(
            categories,
            formData?.categories ?: mutableListOf()
        )
        categoriesRecyclerView.adapter = categoryAdapter
    }

    private fun setupListeners() {
        addCategoryButton.setOnClickListener {
            addNewCategory()
        }

        backButton.setOnClickListener {
            val updatedFormData = updateFormDataWithSelections()
            onBackClickListener?.invoke(updatedFormData)
        }

        nextButton.setOnClickListener {
            val updatedFormData = updateFormDataWithSelections()
            onNextClickListener?.invoke(updatedFormData)
        }

        // Color picker button listeners
        colorButton1.setOnClickListener { selectColor("#2196F3") }
        colorButton2.setOnClickListener { selectColor("#4CAF50") }
        colorButton3.setOnClickListener { selectColor("#FF9800") }
        colorButton4.setOnClickListener { selectColor("#9C27B0") }
        colorButton5.setOnClickListener { selectColor("#F44336") }
        randomColorButton.setOnClickListener { selectRandomColor() }

        // Add TextWatcher to update color preview when user types
        newCategoryColorEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                updateColorPreview(s.toString())
            }
        })
    }

    private fun addNewCategory() {
        val name = newCategoryEditText.text.toString().trim()
        val description = newCategoryDescriptionEditText.text.toString().trim()
        val color = newCategoryColorEditText.text.toString().trim().takeIf { it.isNotEmpty() } ?: "#666666"

        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(context, "Please enter a category description", Toast.LENGTH_SHORT).show()
            return
        }

        val newCategory = ToBuyCategoryDto(
            name = name,
            description = description,
            color = color
        )

        categoryAdapter.addCategory(newCategory)
        newCategoryEditText.setText("")
        newCategoryDescriptionEditText.setText("")
        newCategoryColorEditText.setText("")

        // Reset color preview to default
        updateColorPreview("#666666")

        Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun updateFormDataWithSelections(): ToBuyFormData {
        val selectedCategories = categoryAdapter.getSelectedCategories()
        return (formData ?: ToBuyFormData()).copy(
            categories = selectedCategories
        )
    }

    private fun selectColor(color: String) {
        newCategoryColorEditText.setText(color)
        colorPreviewIndicator.setBackgroundColor(color.toColorInt())
    }

    private fun selectRandomColor() {
        val randomColor = String.format("#%06X", (0xFFFFFF and (Math.random() * 16777215).toInt()))
        newCategoryColorEditText.setText(randomColor)
        colorPreviewIndicator.setBackgroundColor(randomColor.toColorInt())
    }

    private fun updateColorPreview(colorText: String) {
        try {
            val color = if (colorText.startsWith("#") && (colorText.length == 7 || colorText.length == 4)) {
                colorText.toColorInt()
            } else {
                "#666666".toColorInt() // Default gray color
            }
            colorPreviewIndicator.setBackgroundColor(color)
        } catch (_: IllegalArgumentException) {
            // Invalid color format, use default gray
            colorPreviewIndicator.setBackgroundColor("#666666".toColorInt())
        }
    }

    fun setOnNextClickListener(listener: (ToBuyFormData) -> Unit) {
        onNextClickListener = listener
    }

    fun setOnBackClickListener(listener: (ToBuyFormData) -> Unit) {
        onBackClickListener = listener
    }
}