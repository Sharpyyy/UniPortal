package com.example.uniportal.data.local

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(separator = SEPARATOR)

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\n"
    }
}
