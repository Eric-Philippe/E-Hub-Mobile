package com.ericp.e_hub.fragments.tobuy

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.ericp.e_hub.R
import com.ericp.e_hub.models.ToBuyFormData
import java.time.LocalDateTime

class AddToBuyStepOneFragment : DialogFragment() {

    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var criteriaEditText: EditText
    private lateinit var estimatedPriceEditText: EditText
    private lateinit var interestEditText: EditText
    private lateinit var boughtCheckBox: CheckBox
    private lateinit var nextButton: Button
    private lateinit var cancelButton: Button

    private var formData: ToBuyFormData? = null
    private var onNextClickListener: ((ToBuyFormData) -> Unit)? = null
    private var onCancelClickListener: (() -> Unit)? = null

    companion object {
        fun newInstance(formData: ToBuyFormData? = null): AddToBuyStepOneFragment {
            val fragment = AddToBuyStepOneFragment()
            fragment.formData = formData
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_to_buy_step_one, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        populateFieldsWithExistingData()
        setupListeners()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            // Set explicit layout parameters to ensure full width
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }
        return dialog
    }

    private fun initializeViews(view: View) {
        titleEditText = view.findViewById(R.id.titleEditText)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        criteriaEditText = view.findViewById(R.id.criteriaEditText)
        estimatedPriceEditText = view.findViewById(R.id.estimatedPriceEditText)
        interestEditText = view.findViewById(R.id.interestEditText)
        boughtCheckBox = view.findViewById(R.id.boughtCheckBox)
        nextButton = view.findViewById(R.id.nextButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        setupInterestDropdown()
    }

    private fun setupInterestDropdown() {
        val interestLevels = arrayOf("Low", "Medium", "High", "Very High", "Must Have")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            interestLevels
        )
        (interestEditText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun populateFieldsWithExistingData() {
        formData?.let { data ->
            titleEditText.setText(data.title)
            descriptionEditText.setText(data.description)
            criteriaEditText.setText(data.criteria)
            estimatedPriceEditText.setText(data.estimatedPrice?.toString() ?: "")
            interestEditText.setText(data.interest)
            boughtCheckBox.isChecked = data.bought != "null"
        }
    }

    private fun setupListeners() {
        nextButton.setOnClickListener {
            if (validateForm()) {
                val updatedFormData = createFormDataFromInputs()
                onNextClickListener?.invoke(updatedFormData)
            }
        }

        cancelButton.setOnClickListener {
            onCancelClickListener?.invoke()
            dismiss()
        }
    }

    private fun validateForm(): Boolean {
        val title = titleEditText.text.toString().trim()
        if (title.isEmpty()) {
            titleEditText.error = "Title is required"
            return false
        }
        return true
    }

    private fun createFormDataFromInputs(): ToBuyFormData {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim().takeIf { it.isNotEmpty() }
        val criteria = criteriaEditText.text.toString().trim().takeIf { it.isNotEmpty() }
        val estimatedPrice = estimatedPriceEditText.text.toString().trim()
            .takeIf { it.isNotEmpty() }?.toIntOrNull()
        val interest = interestEditText.text.toString().trim().takeIf { it.isNotEmpty() }
        val bought = if (boughtCheckBox.isChecked) LocalDateTime.now() else null

        return (formData ?: ToBuyFormData()).copy(
            title = title,
            description = description,
            criteria = criteria,
            estimatedPrice = estimatedPrice,
            interest = interest,
            bought = bought.toString()
        )
    }

    fun setOnNextClickListener(listener: (ToBuyFormData) -> Unit) {
        onNextClickListener = listener
    }

    fun setOnCancelClickListener(listener: () -> Unit) {
        onCancelClickListener = listener
    }
}