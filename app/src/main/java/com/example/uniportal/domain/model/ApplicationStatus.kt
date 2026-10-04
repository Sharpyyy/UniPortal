package com.example.uniportal.domain.model

enum class ApplicationStatus {
    INTERESTED,
    PREPARING,
    SUBMITTED,
    WAITLISTED,
    ACCEPTED,
    REJECTED,
    ;

    val isFinal: Boolean
        get() = this == ACCEPTED || this == REJECTED

    val isActive: Boolean
        get() = this == INTERESTED || this == PREPARING

    companion object {
        fun fromName(name: String): ApplicationStatus = entries.firstOrNull { it.name == name } ?: INTERESTED
    }
}
