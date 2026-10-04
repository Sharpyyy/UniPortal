package com.example.uniportal.testutil

import com.example.uniportal.data.local.entity.UniversityEntity
import com.example.uniportal.data.remote.dto.UniversityDto
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CachedSearch
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.DueDeadline
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.OfflineCountry
import com.example.uniportal.domain.model.SearchParams
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.ShortlistItem
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import com.example.uniportal.domain.model.UniversityPage
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.domain.rules.ShortlistRules
import java.time.LocalDate
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

fun university(
    id: String = "test.edu",
    name: String = "Test University",
    country: String = "United States",
    countryCode: String = "US",
    stateProvince: String? = null,
    domains: List<String> = listOf(id),
    webPages: List<String> = listOf("http://www.$id/"),
): University = University(
    id = id,
    name = name,
    country = country,
    countryCode = countryCode,
    stateProvince = stateProvince,
    domains = domains,
    webPages = webPages,
)

fun universities(count: Int, startIndex: Int = 0): List<University> =
    (startIndex until startIndex + count).map { index ->
        university(id = "uni$index.edu", name = "University $index")
    }

fun universityEntity(
    id: String = "test.edu",
    name: String = "Test University",
    country: String = "United States",
    countryCode: String = "US",
    stateProvince: String? = null,
    domains: List<String> = listOf(id),
    webPages: List<String> = listOf("http://www.$id/"),
    fetchedAt: Long = FakeTimeProvider.START_TIME_MILLIS,
): UniversityEntity = UniversityEntity(
    id = id,
    name = name,
    country = country,
    countryCode = countryCode,
    stateProvince = stateProvince,
    domains = domains,
    webPages = webPages,
    fetchedAt = fetchedAt,
)

fun universityDto(
    name: String? = "Test University",
    country: String? = "United States",
    alphaTwoCode: String? = "US",
    stateProvince: String? = null,
    domains: List<String>? = listOf("test.edu"),
    webPages: List<String>? = listOf("http://www.test.edu/"),
): UniversityDto = UniversityDto(
    name = name,
    country = country,
    alphaTwoCode = alphaTwoCode,
    stateProvince = stateProvince,
    domains = domains,
    webPages = webPages,
)

fun universityDtos(
    count: Int,
    startIndex: Int = 0,
    country: String = "United States",
    alphaTwoCode: String = "US",
): List<UniversityDto> =
    (startIndex until startIndex + count).map { index ->
        universityDto(
            name = "University $index",
            country = country,
            alphaTwoCode = alphaTwoCode,
            domains = listOf("uni$index.edu"),
            webPages = listOf("http://www.uni$index.edu/"),
        )
    }

fun University.toDto(): UniversityDto = UniversityDto(
    name = name,
    country = country,
    alphaTwoCode = countryCode,
    stateProvince = stateProvince,
    domains = domains,
    webPages = webPages,
)

fun universityPage(
    items: List<University>,
    page: Int = 0,
    hasMore: Boolean = false,
): UniversityPage = UniversityPage(items = items, page = page, hasMore = hasMore)

fun searchParams(query: String = "", country: CountryFilter = CountryFilter.ALL): SearchParams =
    SearchParams(query = query, country = country)

fun cachedSearch(
    items: List<University>,
    hasMore: Boolean = false,
    loadedPages: Int = 1,
    fetchedAt: Long = FakeTimeProvider.START_TIME_MILLIS,
): CachedSearch = CachedSearch(items = items, hasMore = hasMore, loadedPages = loadedPages, fetchedAt = fetchedAt)

fun historyEntry(university: University, viewedAt: Long): HistoryEntry =
    HistoryEntry(university = university, viewedAt = viewedAt)

fun universityApplication(
    university: University = defaultUniversity(),
    status: ApplicationStatus = ApplicationStatus.INTERESTED,
    deadline: LocalDate? = null,
    priority: Int = ApplicationRules.DEFAULT_PRIORITY,
    note: String = "",
    createdAt: Long = FakeTimeProvider.START_TIME_MILLIS,
    updatedAt: Long = createdAt,
): UniversityApplication = UniversityApplication(
    university = university,
    status = status,
    deadline = deadline,
    priority = priority,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun dueDeadline(application: UniversityApplication, daysLeft: Int): DueDeadline =
    DueDeadline(application = application, daysLeft = daysLeft)

fun shortlist(
    id: Long = 1L,
    name: String = "Мечта",
    emoji: String = ShortlistRules.DEFAULT_EMOJI,
    description: String = "",
    itemCount: Int = 0,
    createdAt: Long = FakeTimeProvider.START_TIME_MILLIS,
    updatedAt: Long = createdAt,
): Shortlist = Shortlist(
    id = id,
    name = name,
    emoji = emoji,
    description = description,
    itemCount = itemCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun shortlistItem(university: University, addedAt: Long = FakeTimeProvider.START_TIME_MILLIS): ShortlistItem =
    ShortlistItem(university = university, addedAt = addedAt)

fun shortlistDetails(shortlist: Shortlist = defaultShortlist(), items: List<ShortlistItem> = emptyList()): ShortlistDetails =
    ShortlistDetails(shortlist = shortlist.copy(itemCount = items.size), items = items)

fun offlineCountry(
    countryName: String = "Russian Federation",
    countryCode: String = "RU",
    universityCount: Int = 10,
    downloadedAt: Long = FakeTimeProvider.START_TIME_MILLIS,
): OfflineCountry = OfflineCountry(
    countryName = countryName,
    countryCode = countryCode,
    universityCount = universityCount,
    downloadedAt = downloadedAt,
)

private fun defaultUniversity(): University = university()

private fun defaultShortlist(): Shortlist = shortlist()

fun httpException(code: Int = 500): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody(null)))
