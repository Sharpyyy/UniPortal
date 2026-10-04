package com.example.uniportal.data.mapper

import com.example.uniportal.domain.model.University
import com.example.uniportal.testutil.universityDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UniversityMapperTest {
    @Test
    fun `given several domains with capitals and spaces, when mapped, then id is the first domain trimmed and lower-cased`() {
        val dto = universityDto(domains = listOf("  MTSU.edu ", "mtsu.org"))

        val university = dto.toDomain()

        assertEquals("mtsu.edu", university?.id)
        assertEquals(listOf("MTSU.edu", "mtsu.org"), university?.domains)
    }
}
