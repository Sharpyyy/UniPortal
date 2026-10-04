package com.example.uniportal.domain.model

data class UniversityPage(
    val items: List<University>,
    val page: Int,
    val hasMore: Boolean,
)
