package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shortlists",
    indices = [Index(value = ["name"], unique = true)],
)
data class ShortlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
)
