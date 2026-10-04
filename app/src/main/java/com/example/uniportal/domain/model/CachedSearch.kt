package com.example.uniportal.domain.model

data class CachedSearch(
    val items: List<University>,
    val hasMore: Boolean,
    val loadedPages: Int,
    val fetchedAt: Long,
)
