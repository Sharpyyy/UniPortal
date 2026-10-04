package com.example.uniportal.data.mapper

import com.example.uniportal.domain.model.SearchParams

fun SearchParams.normalizedQuery(): String = query.trim().lowercase()

fun SearchParams.queryKey(): String = "${normalizedQuery()}|${country.apiName.orEmpty()}"

fun String.escapeLikePattern(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
