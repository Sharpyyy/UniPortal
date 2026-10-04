package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey val universityId: String,
    val status: String,
    val deadlineEpochDay: Long?,
    val priority: Int,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
