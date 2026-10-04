package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "search_results",
    primaryKeys = ["queryKey", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SearchQueryEntity::class,
            parentColumns = ["queryKey"],
            childColumns = ["queryKey"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("universityId")],
)
data class SearchResultEntity(
    val queryKey: String,
    val position: Int,
    val universityId: String,
)
