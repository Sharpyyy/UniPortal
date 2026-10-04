package com.example.uniportal.ui.components

import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.CountryFilter

object TestTags {
    const val SEARCH_FIELD = "search_field"
    const val UNIVERSITY_LIST = "university_list"
    const val FAVOURITES_LIST = "favourites_list"
    const val HISTORY_LIST = "history_list"

    const val LOADING_STATE = "loading_state"
    const val ERROR_STATE = "error_state"
    const val EMPTY_STATE = "empty_state"
    const val RETRY_BUTTON = "retry_button"

    const val LOAD_MORE_BUTTON = "load_more_button"
    const val LOAD_MORE_RETRY_BUTTON = "load_more_retry_button"

    const val FAVOURITES_ONLY_CHIP = "favourites_only_chip"
    const val FAVOURITES_FILTER_FIELD = "favourites_filter_field"

    const val REFRESH_BUTTON = "refresh_button"
    const val OPEN_FAVOURITES_BUTTON = "open_favourites_button"
    const val OPEN_HISTORY_BUTTON = "open_history_button"

    const val OFFLINE_BANNER = "offline_banner"

    const val UPDATING_INDICATOR = "updating_indicator"

    const val BOTTOM_NAV = "bottom_nav"

    const val DETAIL_TITLE = "detail_title"
    const val DETAIL_FAVOURITE_BUTTON = "detail_favourite_button"
    const val DETAIL_UPDATED_AT = "detail_updated_at"
    const val DETAIL_SHORTLIST_BUTTON = "detail_shortlist_button"
    const val DETAIL_APPLICATION_CARD = "detail_application_card"
    const val DETAIL_ADD_APPLICATION_BUTTON = "detail_add_application_button"
    const val DETAIL_EDIT_APPLICATION_BUTTON = "detail_edit_application_button"
    const val DETAIL_NEXT_STATUS_BUTTON = "detail_next_status_button"

    const val SHORTLIST_DIALOG = "shortlist_dialog"
    const val SHORTLIST_DIALOG_NAME_FIELD = "shortlist_dialog_name_field"
    const val SHORTLIST_DIALOG_CREATE_BUTTON = "shortlist_dialog_create_button"
    const val SHORTLIST_DIALOG_CONFIRM_BUTTON = "shortlist_dialog_confirm_button"

    const val EDITOR_DEADLINE_BUTTON = "editor_deadline_button"
    const val EDITOR_CLEAR_DEADLINE_BUTTON = "editor_clear_deadline_button"
    const val EDITOR_NOTE_FIELD = "editor_note_field"
    const val EDITOR_SAVE_BUTTON = "editor_save_button"
    const val EDITOR_DELETE_BUTTON = "editor_delete_button"
    const val CONFIRM_DELETE_BUTTON = "confirm_delete_button"

    const val LISTS_TAB_ROW = "lists_tab_row"
    const val SHORTLISTS_GRID = "shortlists_grid"
    const val NEW_SHORTLIST_BUTTON = "new_shortlist_button"

    const val SHORTLIST_FORM = "shortlist_form"
    const val SHORTLIST_FORM_NAME_FIELD = "shortlist_form_name_field"
    const val SHORTLIST_FORM_DESCRIPTION_FIELD = "shortlist_form_description_field"
    const val SHORTLIST_FORM_SAVE_BUTTON = "shortlist_form_save_button"

    const val SHORTLIST_ITEMS_LIST = "shortlist_items_list"
    const val SHORTLIST_EDIT_BUTTON = "shortlist_edit_button"
    const val SHORTLIST_DELETE_BUTTON = "shortlist_delete_button"

    const val APPLICATIONS_LIST = "applications_list"
    const val APPLICATIONS_SUMMARY = "applications_summary"
    const val APPLICATIONS_NEAREST_DEADLINE = "applications_nearest_deadline"

    const val MORE_HISTORY_ROW = "more_history_row"
    const val MORE_OFFLINE_ROW = "more_offline_row"
    const val MORE_SETTINGS_ROW = "more_settings_row"
    const val MORE_ABOUT_ROW = "more_about_row"

    const val SETTINGS_HOME_COUNTRY_ROW = "settings_home_country_row"
    const val SETTINGS_BACKGROUND_SYNC_SWITCH = "settings_background_sync_switch"
    const val SETTINGS_WIFI_ONLY_SWITCH = "settings_wifi_only_switch"
    const val SETTINGS_SYNC_NOW_BUTTON = "settings_sync_now_button"
    const val SETTINGS_CLEAR_CACHE_BUTTON = "settings_clear_cache_button"
    const val SETTINGS_REMINDERS_SWITCH = "settings_reminders_switch"
    const val SETTINGS_OFFLINE_ROW = "settings_offline_row"

    const val OFFLINE_LIST = "offline_list"

    const val UNIVERSITY_ITEM_PREFIX = "university_item_"
    const val FAVOURITE_BUTTON_PREFIX = "favourite_button_"
    const val COUNTRY_CHIP_PREFIX = "country_chip_"
    const val STATUS_PILL_PREFIX = "status_pill_"
    const val SHORTLIST_MARK_PREFIX = "shortlist_mark_"
    const val NAV_ITEM_PREFIX = "nav_item_"
    const val SHORTLIST_OPTION_PREFIX = "shortlist_option_"
    const val EDITOR_STATUS_PREFIX = "editor_status_"
    const val EDITOR_PRIORITY_PREFIX = "editor_priority_"
    const val LISTS_TAB_PREFIX = "lists_tab_"
    const val SHORTLIST_CARD_PREFIX = "shortlist_card_"
    const val SHORTLIST_EMOJI_PREFIX = "shortlist_emoji_"
    const val REMOVE_FROM_SHORTLIST_PREFIX = "remove_from_shortlist_"
    const val APPLICATION_ITEM_PREFIX = "application_item_"
    const val APPLICATION_STATUS_BUTTON_PREFIX = "application_status_button_"
    const val STATUS_MENU_ITEM_PREFIX = "status_menu_item_"
    const val STATUS_FILTER_PREFIX = "status_filter_"
    const val SETTINGS_OPTION_PREFIX = "settings_option_"
    const val OFFLINE_COUNTRY_PREFIX = "offline_country_"
    const val DOWNLOAD_BUTTON_PREFIX = "download_button_"

    fun universityItem(id: String): String = UNIVERSITY_ITEM_PREFIX + id

    fun favouriteButton(id: String): String = FAVOURITE_BUTTON_PREFIX + id

    fun countryChip(country: CountryFilter): String = COUNTRY_CHIP_PREFIX + country.name

    fun statusPill(id: String): String = STATUS_PILL_PREFIX + id

    fun shortlistMark(id: String): String = SHORTLIST_MARK_PREFIX + id

    fun navItem(route: String): String = NAV_ITEM_PREFIX + route

    fun shortlistOption(id: Long): String = SHORTLIST_OPTION_PREFIX + id

    fun editorStatus(status: ApplicationStatus): String = EDITOR_STATUS_PREFIX + status.name

    fun editorPriority(priority: Int): String = EDITOR_PRIORITY_PREFIX + priority

    fun listsTab(name: String): String = LISTS_TAB_PREFIX + name

    fun shortlistCard(id: Long): String = SHORTLIST_CARD_PREFIX + id

    fun shortlistEmoji(index: Int): String = SHORTLIST_EMOJI_PREFIX + index

    fun removeFromShortlist(id: String): String = REMOVE_FROM_SHORTLIST_PREFIX + id

    fun applicationItem(id: String): String = APPLICATION_ITEM_PREFIX + id

    fun applicationStatusButton(id: String): String = APPLICATION_STATUS_BUTTON_PREFIX + id

    fun statusMenuItem(status: ApplicationStatus): String = STATUS_MENU_ITEM_PREFIX + status.name

    fun statusFilter(status: ApplicationStatus?): String = STATUS_FILTER_PREFIX + (status?.name ?: "ALL")

    fun settingsOption(group: String, value: String): String = "$SETTINGS_OPTION_PREFIX${group}_$value"

    fun offlineCountry(countryName: String): String = OFFLINE_COUNTRY_PREFIX + countryName

    fun downloadButton(country: CountryFilter): String = DOWNLOAD_BUTTON_PREFIX + country.name
}
