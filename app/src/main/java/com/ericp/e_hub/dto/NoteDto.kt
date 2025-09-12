package com.ericp.e_hub.dto

enum class NoteStatus {
    UNTOUCHED, EDITED, CREATED, DELETED, EMPTY
}

data class NoteDto(
    val id: String? = null,
    val content: String = "",
    val key: String? = null,
    val created: String? = null,
    val modified: String? = null,
    var status: NoteStatus = NoteStatus.UNTOUCHED
)