package com.example.uniportal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.uniportal.R
import com.example.uniportal.ui.theme.PortalTheme

@Composable
fun FavouriteButton(
    isFavourite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (isFavourite) PortalTheme.colors.favourite else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "favouriteTint",
    )
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavourite) R.string.cd_remove_favourite else R.string.cd_add_favourite,
            ),
            tint = tint,
        )
    }
}
