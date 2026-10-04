package com.example.uniportal.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme

@Composable
fun UniversityCard(
    university: University,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    applicationStatus: ApplicationStatus? = null,
    shortlistCount: Int = 0,
) {
    val hasMarks = applicationStatus != null || shortlistCount > 0
    UniversityCard(
        university = university,
        onClick = onClick,
        modifier = modifier,
        marks = if (hasMarks) {
            {
                applicationStatus?.let { status ->
                    StatusPill(
                        status = status,
                        modifier = Modifier.testTag(TestTags.statusPill(university.id)),
                    )
                }
                if (shortlistCount > 0) {
                    ShortlistCountMark(
                        count = shortlistCount,
                        modifier = Modifier.testTag(TestTags.shortlistMark(university.id)),
                    )
                }
            }
        } else {
            null
        },
    ) {
        FavouriteButton(
            isFavourite = isFavourite,
            onClick = onToggleFavourite,
            modifier = Modifier.testTag(TestTags.favouriteButton(university.id)),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversityCard(
    university: University,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String = university.id,
    marks: (@Composable () -> Unit)? = null,
    trailingContent: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.universityItem(university.id)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            UniversityAvatar(id = university.id, name = university.name)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = university.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (university.country.isNotBlank()) {
                    CountryBadge(country = university.country, countryCode = university.countryCode)
                }
                university.stateProvince?.let { region -> RegionLabel(region) }
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (marks != null) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        marks()
                    }
                }
            }
            trailingContent()
        }
    }
}

@Composable
private fun RegionLabel(region: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = region,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun UniversityCardPreview() {
    UniPortalTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UniversityCard(
                university = PreviewData.mtsu,
                isFavourite = true,
                onClick = {},
                onToggleFavourite = {},
                applicationStatus = ApplicationStatus.PREPARING,
                shortlistCount = 2,
            )
            UniversityCard(
                university = PreviewData.msu,
                isFavourite = false,
                onClick = {},
                onToggleFavourite = {},
            )
        }
    }
}
