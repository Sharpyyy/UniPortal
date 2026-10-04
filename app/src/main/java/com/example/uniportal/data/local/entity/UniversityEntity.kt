package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "universities",
    indices = [Index("country"), Index("name")],
)
data class UniversityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val stateProvince: String?,
    val domains: List<String>,
    val webPages: List<String>,

    val fetchedAt: Long,
)
