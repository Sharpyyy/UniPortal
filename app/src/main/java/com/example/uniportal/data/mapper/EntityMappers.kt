package com.example.uniportal.data.mapper

import com.example.uniportal.data.local.entity.OfflineCountryEntity
import com.example.uniportal.data.local.entity.ShortlistEntity
import com.example.uniportal.data.local.entity.UniversityEntity
import com.example.uniportal.data.local.relation.ApplicationWithUniversity
import com.example.uniportal.data.local.relation.HistoryWithUniversity
import com.example.uniportal.data.local.relation.SearchResultRow
import com.example.uniportal.data.local.relation.ShortlistItemWithUniversity
import com.example.uniportal.data.local.relation.ShortlistWithCount
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CachedSearch
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.OfflineCountry
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistItem
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import java.time.LocalDate

fun UniversityEntity.toDomain(): University = University(
    id = id,
    name = name,
    country = country,
    countryCode = countryCode,
    stateProvince = stateProvince,
    domains = domains,
    webPages = webPages,
)

fun University.toEntity(fetchedAt: Long): UniversityEntity = UniversityEntity(
    id = id,
    name = name,
    country = country,
    countryCode = countryCode,
    stateProvince = stateProvince,
    domains = domains,
    webPages = webPages,
    fetchedAt = fetchedAt,
)

fun HistoryWithUniversity.toDomain(): HistoryEntry = HistoryEntry(
    university = university.toDomain(),
    viewedAt = viewedAt,
)

fun ShortlistEntity.toDomain(itemCount: Int): Shortlist = Shortlist(
    id = id,
    name = name,
    emoji = emoji,
    description = description,
    itemCount = itemCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ShortlistWithCount.toDomain(): Shortlist = shortlist.toDomain(itemCount = itemCount)

fun ShortlistItemWithUniversity.toDomain(): ShortlistItem = ShortlistItem(
    university = university.toDomain(),
    addedAt = addedAt,
)

fun ApplicationWithUniversity.toDomain(): UniversityApplication = UniversityApplication(
    university = university.toDomain(),
    status = ApplicationStatus.fromName(application.status),
    deadline = application.deadlineEpochDay?.let { LocalDate.ofEpochDay(it) },
    priority = application.priority,
    note = application.note,
    createdAt = application.createdAt,
    updatedAt = application.updatedAt,
)

fun OfflineCountryEntity.toDomain(): OfflineCountry = OfflineCountry(
    countryName = countryName,
    countryCode = countryCode,
    universityCount = universityCount,
    downloadedAt = downloadedAt,
)

fun List<SearchResultRow>.toCachedSearch(): CachedSearch? {
    val first = firstOrNull() ?: return null
    return CachedSearch(
        items = mapNotNull { row -> row.university?.toDomain() }.distinctBy { it.id },
        hasMore = first.hasMore,
        loadedPages = first.loadedPages,
        fetchedAt = first.queryFetchedAt,
    )
}
