package com.example.uniportal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_countries")
data class OfflineCountryEntity(
    @PrimaryKey val countryName: String,
    val countryCode: String,
    val universityCount: Int,

    val downloadedAt: Long,
)
