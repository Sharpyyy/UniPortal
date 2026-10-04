package com.example.uniportal.domain.model

enum class CountryFilter(val apiName: String?, val code: String?) {
    ALL(null, null),
    RUSSIA("Russian Federation", "RU"),
    KAZAKHSTAN("Kazakhstan", "KZ"),
    BELARUS("Belarus", "BY"),
    UZBEKISTAN("Uzbekistan", "UZ"),
    USA("United States", "US"),
    UK("United Kingdom", "GB"),
    GERMANY("Germany", "DE"),
    FRANCE("France", "FR"),
    CHINA("China", "CN"),
    JAPAN("Japan", "JP"),
    SOUTH_KOREA("Korea, Republic of", "KR"),
    TURKIYE("Turkiye", "TR"),
    CANADA("Canada", "CA"),
}
