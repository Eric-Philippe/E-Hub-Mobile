package com.ericp.e_hub.fragments.tobuy

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ericp.e_hub.R
import com.ericp.e_hub.adapters.tobuy.LinkEditAdapter
import com.ericp.e_hub.dto.ToBuyLinkDto
import com.ericp.e_hub.models.ToBuyFormData
import java.util.UUID

class AddToBuyStepThreeFragment : DialogFragment() {

    private lateinit var linksRecyclerView: RecyclerView
    private lateinit var newLinkUrlEditText: EditText
    private lateinit var newLinkPriceEditText: EditText
    private lateinit var newLinkFavouriteCheckBox: CheckBox
    private lateinit var addLinkButton: Button
    private lateinit var backButton: Button
    private lateinit var finishButton: Button

    private lateinit var linkAdapter: LinkEditAdapter
    private var formData: ToBuyFormData? = null
    private var onFinishClickListener: ((ToBuyFormData) -> Unit)? = null
    private var onBackClickListener: ((ToBuyFormData) -> Unit)? = null

    companion object {
        fun newInstance(formData: ToBuyFormData): AddToBuyStepThreeFragment {
            val fragment = AddToBuyStepThreeFragment()
            fragment.formData = formData
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_to_buy_step_three, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupListeners()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    private fun initializeViews(view: View) {
        linksRecyclerView = view.findViewById(R.id.linksRecyclerView)
        newLinkUrlEditText = view.findViewById(R.id.newLinkUrlEditText)
        newLinkPriceEditText = view.findViewById(R.id.newLinkPriceEditText)
        newLinkFavouriteCheckBox = view.findViewById(R.id.newLinkFavouriteCheckBox)
        addLinkButton = view.findViewById(R.id.addLinkButton)
        backButton = view.findViewById(R.id.backButton)
        finishButton = view.findViewById(R.id.finishButton)
    }

    private fun setupRecyclerView() {
        linkAdapter = LinkEditAdapter(
            formData?.links ?: mutableListOf()
        ) { position ->
            // Remove link callback
            linkAdapter.removeLink(position)
        }

        linksRecyclerView.layoutManager = LinearLayoutManager(context)
        linksRecyclerView.adapter = linkAdapter
    }

    private fun setupListeners() {
        addLinkButton.setOnClickListener {
            addNewLink()
        }

        backButton.setOnClickListener {
            val updatedFormData = updateFormDataWithLinks()
            onBackClickListener?.invoke(updatedFormData)
        }

        finishButton.setOnClickListener {
            val updatedFormData = updateFormDataWithLinks()
            onFinishClickListener?.invoke(updatedFormData)
        }
    }

    private fun addNewLink() {
        val url = newLinkUrlEditText.text.toString().trim()
        val priceText = newLinkPriceEditText.text.toString().trim()
        val price = priceText.takeIf { it.isNotEmpty() }?.toShortOrNull()
        val favourite = newLinkFavouriteCheckBox.isChecked

        if (url.isNotEmpty()) {
            val newLink = ToBuyLinkDto(
                id = UUID.randomUUID(),
                url = url,
                price = price,
                favourite = favourite,
                toBuyId = formData?.id
            )

            linkAdapter.addLink(newLink)
            clearNewLinkInputs()
        } else {
            newLinkUrlEditText.error = "URL is required"
        }
    }

    private fun clearNewLinkInputs() {
        newLinkUrlEditText.setText("")
        newLinkPriceEditText.setText("")
        newLinkFavouriteCheckBox.isChecked = false
    }

    private fun updateFormDataWithLinks(): ToBuyFormData {
        val currentLinks = linkAdapter.getLinks()
        return (formData ?: ToBuyFormData()).copy(
            links = currentLinks
        )
    }

    fun setOnFinishClickListener(listener: (ToBuyFormData) -> Unit) {
        onFinishClickListener = listener
    }

    fun setOnBackClickListener(listener: (ToBuyFormData) -> Unit) {
        onBackClickListener = listener
    }
}