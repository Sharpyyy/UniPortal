package com.example.uniportal.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.uniportal.R

enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    SEARCH(Routes.SEARCH, R.string.nav_search, Icons.Filled.Search, Icons.Filled.Search),
    LISTS(Routes.LISTS, R.string.nav_lists, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    APPLICATIONS(Routes.APPLICATIONS, R.string.nav_applications, Icons.Filled.DateRange, Icons.Filled.DateRange),
    MORE(Routes.MORE, R.string.nav_more, Icons.Filled.Menu, Icons.Filled.Menu),
    ;

    companion object {
        fun fromRoute(route: String?): TopLevelDestination? = entries.firstOrNull { it.route == route }
    }
}
