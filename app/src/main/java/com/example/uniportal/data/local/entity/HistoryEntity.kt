package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val universityId: String,

    val viewedAt: Long,
)
