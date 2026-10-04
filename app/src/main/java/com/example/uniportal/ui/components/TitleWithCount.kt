package com.example.uniportal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.uniportal.R

@Composable
fun TitleWithCount(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    var lastCount by remember { mutableIntStateOf(count) }
    SideEffect { if (count > 0) lastCount = count }
    val shownCount = if (count > 0) count else lastCount

    Column(modifier = modifier) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AnimatedVisibility(visible = count > 0) {
            Text(
                text = pluralStringResource(R.plurals.universities_count, shownCount, shownCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
