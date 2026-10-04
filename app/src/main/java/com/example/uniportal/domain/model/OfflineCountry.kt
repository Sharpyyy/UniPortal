package com.example.uniportal.domain.model

data class OfflineCountry(
    val countryName: String,
    val countryCode: String,
    val universityCount: Int,
    val downloadedAt: Long,
)
