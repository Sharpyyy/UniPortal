package com.example.uniportal.ui.preview

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.HistoryEntry
import com.example.uniportal.domain.model.OfflineCountry
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.model.ShortlistDetails
import com.example.uniportal.domain.model.ShortlistItem
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.model.UniversityApplication
import java.time.LocalDate
import java.util.concurrent.TimeUnit

internal object PreviewData {
    val mtsu = University(
        id = "mtsu.edu",
        name = "Middle Tennessee State University",
        country = "United States",
        countryCode = "US",
        stateProvince = "Tennessee",
        domains = listOf("mtsu.edu"),
        webPages = listOf("http://www.mtsu.edu/"),
    )

    val msu = University(
        id = "msu.ru",
        name = "Lomonosov Moscow State University",
        country = "Russian Federation",
        countryCode = "RU",
        stateProvince = null,
        domains = listOf("msu.ru", "cs.msu.ru"),
        webPages = listOf("http://www.msu.ru/"),
    )

    val tokyo = University(
        id = "u-tokyo.ac.jp",
        name = "University of Tokyo",
        country = "Japan",
        countryCode = "JP",
        stateProvince = null,
        domains = listOf("u-tokyo.ac.jp"),
        webPages = listOf("http://www.u-tokyo.ac.jp/"),
    )

    val universities = listOf(mtsu, msu, tokyo)

    private val now = System.currentTimeMillis()

    val history = listOf(
        HistoryEntry(tokyo, viewedAt = now - TimeUnit.MINUTES.toMillis(5)),
        HistoryEntry(mtsu, viewedAt = now - TimeUnit.DAYS.toMillis(1)),
        HistoryEntry(msu, viewedAt = now - TimeUnit.DAYS.toMillis(3)),
    )

    val shortlists = listOf(
        Shortlist(
            id = 1,
            name = "Dream",
            emoji = "🎯",
            description = "Strong computer science programmes",
            itemCount = 3,
            createdAt = now,
            updatedAt = now,
        ),
        Shortlist(
            id = 2,
            name = "Backup",
            emoji = "🧭",
            description = "",
            itemCount = 1,
            createdAt = now,
            updatedAt = now,
        ),
    )

    val application = UniversityApplication(
        university = mtsu,
        status = ApplicationStatus.PREPARING,
        deadline = LocalDate.now().plusDays(5),
        priority = 3,
        note = "Motivation letter, two recommendations, IELTS 7.0",
        createdAt = now,
        updatedAt = now,
    )

    val applications = listOf(
        application,
        UniversityApplication(
            university = msu,
            status = ApplicationStatus.INTERESTED,
            deadline = LocalDate.now().minusDays(2),
            priority = 2,
            note = "",
            createdAt = now,
            updatedAt = now,
        ),
        UniversityApplication(
            university = tokyo,
            status = ApplicationStatus.SUBMITTED,
            deadline = LocalDate.now().plusDays(30),
            priority = 1,
            note = "Waiting for the interview invitation",
            createdAt = now,
            updatedAt = now,
        ),
    )

    val shortlistDetails = ShortlistDetails(
        shortlist = shortlists.first(),
        items = universities.mapIndexed { index, university ->
            ShortlistItem(university = university, addedAt = now - TimeUnit.HOURS.toMillis(index * 20L + 1))
        },
    )

    val offlineCountries = listOf(
        OfflineCountry(
            countryName = "Russian Federation",
            countryCode = "RU",
            universityCount = 1043,
            downloadedAt = now - TimeUnit.HOURS.toMillis(2),
        ),
        OfflineCountry(
            countryName = "Kazakhstan",
            countryCode = "KZ",
            universityCount = 128,
            downloadedAt = now - TimeUnit.DAYS.toMillis(3),
        ),
    )
}
