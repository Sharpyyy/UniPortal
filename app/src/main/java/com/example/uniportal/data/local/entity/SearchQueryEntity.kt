package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_queries")
data class SearchQueryEntity(
    @PrimaryKey val queryKey: String,
    val query: String,
    val country: String,
    val loadedPages: Int,
    val hasMore: Boolean,

    val fetchedAt: Long,
)
