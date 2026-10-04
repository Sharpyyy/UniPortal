package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "shortlist_items",
    primaryKeys = ["shortlistId", "universityId"],
    foreignKeys = [
        ForeignKey(
            entity = ShortlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["shortlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("universityId")],
)
data class ShortlistItemEntity(
    val shortlistId: Long,
    val universityId: String,

    val addedAt: Long,
)
