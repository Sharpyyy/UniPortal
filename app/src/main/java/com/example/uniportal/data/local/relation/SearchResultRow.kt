package com.example.uniportal.data.local.relation

import androidx.room.Embedded
import com.example.uniportal.data.local.entity.UniversityEntity

data class SearchResultRow(
    val loadedPages: Int,
    val hasMore: Boolean,
    val queryFetchedAt: Long,
    @Embedded val university: UniversityEntity?,
)
