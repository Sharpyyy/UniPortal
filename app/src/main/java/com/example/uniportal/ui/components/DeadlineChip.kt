package com.example.uniportal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme

@Composable
fun DeadlineChip(
    daysLeft: Int,
    isOverdue: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = when {
        isOverdue -> MaterialTheme.colorScheme.error
        isActive && daysLeft <= URGENT_DAYS -> PortalTheme.colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val text = when {
        isOverdue -> pluralStringResource(R.plurals.deadline_overdue_days, -daysLeft, -daysLeft)
        daysLeft < 0 -> stringResource(R.string.deadline_passed)
        daysLeft == 0 -> stringResource(R.string.deadline_today)
        daysLeft == 1 -> stringResource(R.string.deadline_tomorrow)
        else -> pluralStringResource(R.plurals.deadline_days_left, daysLeft, daysLeft)
    }
    TintedPill(color = color, modifier = modifier) {
        Icon(
            imageVector = if (isOverdue) Icons.Filled.Warning else Icons.Filled.DateRange,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

private const val URGENT_DAYS = 7

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun DeadlineChipPreview() {
    UniPortalTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeadlineChip(daysLeft = -2, isOverdue = true, isActive = true)
            DeadlineChip(daysLeft = 0, isOverdue = false, isActive = true)
            DeadlineChip(daysLeft = 5, isOverdue = false, isActive = true)
            DeadlineChip(daysLeft = 40, isOverdue = false, isActive = false)
        }
    }
}
