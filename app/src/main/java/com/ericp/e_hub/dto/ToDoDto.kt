package com.ericp.e_hub.dto

import java.util.UUID

enum class State {
    TODO,
    IN_PROGRESS,
    DONE
}

data class ToDoDto(
    val id: UUID,
    val label: String,
    val state: State,
    val color: String?,
    val created: String,
    val modified: String?,
    val dueDate: String?,
    val description: String?,
    val parentId: UUID?,
    val children: List<ToDoDto> = emptyList()
)
data class ToDoRequest(
    val label: String,
    val state: State,
    var color: String? = null,
    val dueDate: String? = null,
    val description: String? = null,
    val parentId: UUID? = null
)