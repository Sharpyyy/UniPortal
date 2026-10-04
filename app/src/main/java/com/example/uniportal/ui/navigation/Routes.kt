package com.example.uniportal.ui.navigation

import android.net.Uri

object Routes {
    const val SEARCH = "search"
    const val LISTS = "lists"
    const val APPLICATIONS = "applications"
    const val MORE = "more"

    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val OFFLINE = "offline"

    const val ARG_ID = "id"
    const val DETAIL = "detail/{$ARG_ID}"

    const val ARG_SHORTLIST_ID = "shortlistId"
    const val SHORTLIST = "shortlist/{$ARG_SHORTLIST_ID}"

    const val ARG_UNIVERSITY_ID = "universityId"
    const val APPLICATION = "application/{$ARG_UNIVERSITY_ID}"

    fun detail(id: String): String = "detail/" + Uri.encode(id)

    fun shortlist(shortlistId: Long): String = "shortlist/$shortlistId"

    fun application(universityId: String): String = "application/" + Uri.encode(universityId)
}
