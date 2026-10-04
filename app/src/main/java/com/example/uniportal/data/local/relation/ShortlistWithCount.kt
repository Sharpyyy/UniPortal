package com.example.uniportal.data.local.relation

import androidx.room.Embedded
import com.example.uniportal.data.local.entity.ShortlistEntity

data class ShortlistWithCount(
    @Embedded val shortlist: ShortlistEntity,
    val itemCount: Int,
)
