package com.example.uniportal.domain.model

data class SearchParams(
    val query: String,
    val country: CountryFilter,
)
