package com.example.uniportal.testutil

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider

private const val UI_TIMEOUT_MILLIS = 5_000L

fun string(@StringRes id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

fun ComposeContentTestRule.waitUntilTagExists(tag: String, timeoutMillis: Long = UI_TIMEOUT_MILLIS) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeContentTestRule.waitUntilTagDoesNotExist(tag: String, timeoutMillis: Long = UI_TIMEOUT_MILLIS) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
    }
}

fun ComposeContentTestRule.waitUntilNodeExists(matcher: SemanticsMatcher, timeoutMillis: Long = UI_TIMEOUT_MILLIS) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }
}
