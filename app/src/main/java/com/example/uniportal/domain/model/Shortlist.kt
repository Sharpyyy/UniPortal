package com.example.uniportal.domain.model

data class Shortlist(
    val id: Long,
    val name: String,
    val emoji: String,
    val description: String,
    val itemCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ShortlistItem(
    val university: University,
    val addedAt: Long,
)

data class ShortlistDetails(
    val shortlist: Shortlist,
    val items: List<ShortlistItem>,
) {
    val universities: List<University>
        get() = items.map { it.university }
}
