package com.example.uniportal.data.local.relation

import androidx.room.Embedded
import com.example.uniportal.data.local.entity.ApplicationEntity
import com.example.uniportal.data.local.entity.UniversityEntity

data class ApplicationWithUniversity(
    @Embedded val application: ApplicationEntity,
    @Embedded val university: UniversityEntity,
)
