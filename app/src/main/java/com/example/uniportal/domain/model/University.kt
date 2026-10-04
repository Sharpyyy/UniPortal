package com.example.uniportal.domain.model

data class University(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val stateProvince: String?,
    val domains: List<String>,
    val webPages: List<String>,
)
