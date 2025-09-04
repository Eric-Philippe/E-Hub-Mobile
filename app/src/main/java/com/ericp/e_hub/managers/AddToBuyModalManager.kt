package com.ericp.e_hub.managers

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ericp.e_hub.dto.ToBuyDto
import com.ericp.e_hub.fragments.tobuy.AddToBuyStepOneFragment
import com.ericp.e_hub.fragments.tobuy.AddToBuyStepTwoFragment
import com.ericp.e_hub.fragments.tobuy.AddToBuyStepThreeFragment
import com.ericp.e_hub.models.ToBuyFormData
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints
import java.util.UUID

class AddToBuyModalManager(
    private val activity: FragmentActivity,
    private val onItemCreated: (ToBuyDto) -> Unit
) {

    private var currentFormData = ToBuyFormData()
    private var currentFragment: androidx.fragment.app.DialogFragment? = null
    private var editingItem: ToBuyDto? = null
    private var onItemUpdated: ((ToBuyDto) -> Unit)? = null
    private val apiHelper = EHubApiHelper(activity)

    fun showModal() {
        editingItem = null
        onItemUpdated = null
        resetFormData()
        showStepOne()
    }

    fun showModal(existingItem: ToBuyDto, onUpdate: (ToBuyDto) -> Unit) {
        editingItem = existingItem
        onItemUpdated = onUpdate
        currentFormData = ToBuyFormData().apply {
            title = existingItem.title
            description = existingItem.description ?: ""
            criteria = existingItem.criteria ?: ""
            bought = existingItem.bought.toString()
            estimatedPrice = existingItem.estimatedPrice
            interest = existingItem.interest ?: ""
            categories.clear()
            categories.addAll(existingItem.categories)
            links.clear()
            links.addAll(existingItem.links)
        }
        showStepOne()
    }

    private fun showStepOne(formData: ToBuyFormData? = null) {
        currentFormData = formData ?: currentFormData
        dismissCurrentFragment()

        val fragment = AddToBuyStepOneFragment.newInstance(currentFormData)
        fragment.setOnNextClickListener { updatedFormData ->
            currentFormData = updatedFormData
            fragment.dismiss()
            showStepTwo()
        }
        fragment.setOnCancelClickListener {
            fragment.dismiss()
        }

        currentFragment = fragment
        fragment.show(activity.supportFragmentManager, "AddToBuyStepOne")
    }

    private fun showStepTwo() {
        dismissCurrentFragment()

        val fragment = AddToBuyStepTwoFragment.newInstance(currentFormData)
        fragment.setOnNextClickListener { updatedFormData ->
            currentFormData = updatedFormData
            fragment.dismiss()
            showStepThree()
        }
        fragment.setOnBackClickListener { updatedFormData ->
            currentFormData = updatedFormData
            fragment.dismiss()
            showStepOne(currentFormData)
        }

        currentFragment = fragment
        fragment.show(activity.supportFragmentManager, "AddToBuyStepTwo")
    }

    private fun showStepThree() {
        dismissCurrentFragment()

        val fragment = AddToBuyStepThreeFragment.newInstance(currentFormData)
        fragment.setOnFinishClickListener { updatedFormData ->
            currentFormData = updatedFormData
            fragment.dismiss()
            createToBuyItem()
        }
        fragment.setOnBackClickListener { updatedFormData ->
            currentFormData = updatedFormData
            fragment.dismiss()
            showStepTwo()
        }

        currentFragment = fragment
        fragment.show(activity.supportFragmentManager, "AddToBuyStepThree")
    }

    private fun createToBuyItem() {
        // Trim the bought if not null
        var boughtString: String? = null;
        if (currentFormData.bought != null && currentFormData.bought != "null") {
            boughtString =
                currentFormData.bought?.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
        }
        val toBuyDto = ToBuyDto(
            id = editingItem?.id,
            title = currentFormData.title,
            description = currentFormData.description,
            criteria = currentFormData.criteria,
            bought = boughtString,
            estimatedPrice = currentFormData.estimatedPrice,
            interest = currentFormData.interest,
            categories = currentFormData.categories.toList(),
            links = currentFormData.links.toList()
        )

        if (editingItem != null) {
            // Update existing item
            onItemUpdated?.invoke(toBuyDto)
            resetFormData()
            Toast.makeText(activity, "Item updated successfully!", Toast.LENGTH_SHORT).show()
        } else {
            // Create new item
            apiHelper.postDataAsync(
                endpoint = Endpoints.TOBUY,
                data = toBuyDto,
                onSuccess = { response ->
                    // Handle successful creation
                    onItemCreated(toBuyDto)
                    resetFormData()
                    // Clear ToBuy cache after successful creation
                    apiHelper.clearToBuyCache()
                    Toast.makeText(activity, "Item created successfully!", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    // Handle error
                    Toast.makeText(activity, "Failed to create item: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun dismissCurrentFragment() {
        currentFragment?.dismiss()
        currentFragment = null
    }

    private fun resetFormData() {
        currentFormData = ToBuyFormData()
    }
}
