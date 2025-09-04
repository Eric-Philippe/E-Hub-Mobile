package com.ericp.e_hub.models

import com.ericp.e_hub.dto.ToBuyCategoryDto
import com.ericp.e_hub.dto.ToBuyLinkDto
import java.util.UUID

data class ToBuyFormData(
    val id: UUID? = null,
    var title: String = "",
    var description: String? = null,
    var criteria: String? = null,
    var bought: String? = null,
    var estimatedPrice: Int? = null,
    var interest: String? = null,
    val categories: MutableList<ToBuyCategoryDto> = mutableListOf(),
    val links: MutableList<ToBuyLinkDto> = mutableListOf()
)
