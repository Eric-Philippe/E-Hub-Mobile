package com.ericp.e_hub.dto

import java.util.UUID

data class ToBuyDto(
    val id: UUID? = null,
    val created: String? = null,
    val title: String,
    val description: String? = null,
    val criteria: String? = null,
    val bought: String? = null,
    val estimatedPrice: Int? = null,
    val interest: String? = null,
    var categories: List<ToBuyCategoryDto> = emptyList(),
    val links: List<ToBuyLinkDto> = emptyList()
)

data class ToBuyLinkDto(
    val id: UUID? = null,
    val url: String,
    val price: Short? = null,
    val favourite: Boolean = false,
    val illustrationUrl: String? = null,
    val toBuyId: UUID? = null
)

data class ToBuyCategoryDto(
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null,
    var color: String? = null
)
