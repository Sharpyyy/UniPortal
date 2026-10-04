package com.example.uniportal.data.local.relation

import androidx.room.Embedded
import com.example.uniportal.data.local.entity.UniversityEntity

data class HistoryWithUniversity(
    @Embedded val university: UniversityEntity,
    val viewedAt: Long,
)
