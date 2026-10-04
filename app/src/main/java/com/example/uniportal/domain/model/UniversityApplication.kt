package com.example.uniportal.domain.model

import java.time.LocalDate

data class UniversityApplication(
    val university: University,
    val status: ApplicationStatus,
    val deadline: LocalDate?,
    val priority: Int,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
