package com.example.uniportal.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme

@StringRes
fun ApplicationStatus.labelRes(): Int = when (this) {
    ApplicationStatus.INTERESTED -> R.string.status_interested
    ApplicationStatus.PREPARING -> R.string.status_preparing
    ApplicationStatus.SUBMITTED -> R.string.status_submitted
    ApplicationStatus.WAITLISTED -> R.string.status_waitlisted
    ApplicationStatus.ACCEPTED -> R.string.status_accepted
    ApplicationStatus.REJECTED -> R.string.status_rejected
}

@StringRes
fun ApplicationStatus.descriptionRes(): Int = when (this) {
    ApplicationStatus.INTERESTED -> R.string.status_interested_description
    ApplicationStatus.PREPARING -> R.string.status_preparing_description
    ApplicationStatus.SUBMITTED -> R.string.status_submitted_description
    ApplicationStatus.WAITLISTED -> R.string.status_waitlisted_description
    ApplicationStatus.ACCEPTED -> R.string.status_accepted_description
    ApplicationStatus.REJECTED -> R.string.status_rejected_description
}

@StringRes
fun priorityLabelRes(priority: Int): Int = when {
    priority <= 1 -> R.string.priority_low
    priority == 2 -> R.string.priority_medium
    else -> R.string.priority_high
}

fun ApplicationStatus.emoji(): String = when (this) {
    ApplicationStatus.INTERESTED -> "💡"
    ApplicationStatus.PREPARING -> "📝"
    ApplicationStatus.SUBMITTED -> "📨"
    ApplicationStatus.WAITLISTED -> "⏳"
    ApplicationStatus.ACCEPTED -> "🎉"
    ApplicationStatus.REJECTED -> "🚫"
}

@Composable
@ReadOnlyComposable
fun ApplicationStatus.accentColor(): Color = when (this) {
    ApplicationStatus.INTERESTED -> MaterialTheme.colorScheme.secondary
    ApplicationStatus.PREPARING -> MaterialTheme.colorScheme.tertiary
    ApplicationStatus.SUBMITTED -> PortalTheme.colors.info
    ApplicationStatus.WAITLISTED -> PortalTheme.colors.warning
    ApplicationStatus.ACCEPTED -> MaterialTheme.colorScheme.primary
    ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
}

@Composable
fun StatusPill(
    status: ApplicationStatus,
    modifier: Modifier = Modifier,
) {
    val color = status.accentColor()
    TintedPill(color = color, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ShortlistCountMark(
    count: Int,
    modifier: Modifier = Modifier,
) {
    TintedPill(color = MaterialTheme.colorScheme.tertiary, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = pluralStringResource(R.plurals.shortlist_mark, count, count),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun TintedPill(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = PILL_CONTAINER_ALPHA),
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = PILL_BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private const val PILL_CONTAINER_ALPHA = 0.14f
private const val PILL_BORDER_ALPHA = 0.4f

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun StatusPillPreview() {
    UniPortalTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ApplicationStatus.entries.forEach { StatusPill(status = it) }
            ShortlistCountMark(count = 2)
        }
    }
}
