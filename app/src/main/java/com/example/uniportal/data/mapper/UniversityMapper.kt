package com.example.uniportal.data.mapper

import com.example.uniportal.data.remote.dto.UniversityDto
import com.example.uniportal.domain.model.University

fun UniversityDto.toDomain(): University? {
    val cleanName = name?.trim().orEmpty()
    val cleanDomains = domains.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleanName.isEmpty() || cleanDomains.isEmpty()) return null

    return University(
        id = cleanDomains.first().lowercase(),
        name = cleanName,
        country = country?.trim().orEmpty(),
        countryCode = alphaTwoCode?.trim()?.uppercase().orEmpty(),
        stateProvince = stateProvince?.trim()?.takeIf { it.isNotEmpty() },
        domains = cleanDomains,
        webPages = webPages.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
    )
}
