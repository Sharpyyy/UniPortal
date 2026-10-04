package com.example.uniportal.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme
import com.example.uniportal.util.GLOBE_EMOJI
import com.example.uniportal.util.flagEmoji
import java.util.Locale

@Composable
fun CountryFilterRow(
    selected: CountryFilter,
    onSelect: (CountryFilter) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    leadingChip: (@Composable () -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val leadingCount = if (leadingChip != null) 1 else 0

    LaunchedEffect(selected) {
        val index = if (selected == CountryFilter.ALL) {
            0
        } else {
            CountryFilter.entries.indexOf(selected) + leadingCount
        }
        val layoutInfo = listState.layoutInfo
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

        val contentEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        when {
            item == null -> listState.animateScrollToItem(index)
            item.offset < 0 -> listState.animateScrollBy(item.offset.toFloat())
            item.offset + item.size > contentEnd ->
                listState.animateScrollBy((item.offset + item.size - contentEnd).toFloat())
            else -> Unit
        }
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leadingChip?.let { chip ->

            item(key = LEADING_CHIP_KEY) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chip()
                    VerticalDivider(
                        modifier = Modifier.height(LEADING_DIVIDER_HEIGHT),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        items(items = CountryFilter.entries, key = { it.name }) { country ->
            val isSelected = country == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(country) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = country.flag(), modifier = Modifier.clearAndSetSemantics {})
                        Spacer(Modifier.width(4.dp))
                        Text(text = stringResource(country.labelRes()))
                    }
                },
                modifier = Modifier.testTag(TestTags.countryChip(country)),
                colors = portalChipColors(),
                border = portalChipBorder(selected = isSelected),
            )
        }
    }
}

@Composable
fun FavouritesOnlyChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = { onSelectedChange(!selected) },
        label = { Text(text = stringResource(R.string.filter_favourites_only)) },
        modifier = modifier.testTag(TestTags.FAVOURITES_ONLY_CHIP),
        leadingIcon = {
            Icon(
                imageVector = if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                tint = if (selected) PortalTheme.colors.favourite else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = portalChipColors(),
        border = portalChipBorder(selected = selected),
    )
}

@Composable
private fun portalChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun portalChipBorder(selected: Boolean): BorderStroke = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MaterialTheme.colorScheme.outline,
    selectedBorderColor = MaterialTheme.colorScheme.primary,
    selectedBorderWidth = 1.dp,
)

private const val LEADING_CHIP_KEY = "leading_chip"
private val LEADING_DIVIDER_HEIGHT = 24.dp

fun CountryFilter.flag(): String = code?.let(::flagEmoji) ?: GLOBE_EMOJI

@Composable
fun countryLabel(country: String, countryCode: String): String {
    val code = countryCode.trim().uppercase()
    val filter = CountryFilter.entries.firstOrNull { it.code == code }
    return if (filter != null) {
        stringResource(filter.labelRes())
    } else {
        remember(code, country) { russianCountryName(code) ?: country }
    }
}

private val RUSSIAN = Locale("ru")

private fun russianCountryName(code: String): String? {
    if (code.length != 2) return null
    val name = Locale("", code).getDisplayCountry(RUSSIAN)

    return name.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
}

@StringRes
fun CountryFilter.labelRes(): Int = when (this) {
    CountryFilter.ALL -> R.string.country_all
    CountryFilter.RUSSIA -> R.string.country_russia
    CountryFilter.KAZAKHSTAN -> R.string.country_kazakhstan
    CountryFilter.BELARUS -> R.string.country_belarus
    CountryFilter.UZBEKISTAN -> R.string.country_uzbekistan
    CountryFilter.USA -> R.string.country_usa
    CountryFilter.UK -> R.string.country_uk
    CountryFilter.GERMANY -> R.string.country_germany
    CountryFilter.FRANCE -> R.string.country_france
    CountryFilter.CHINA -> R.string.country_china
    CountryFilter.JAPAN -> R.string.country_japan
    CountryFilter.SOUTH_KOREA -> R.string.country_south_korea
    CountryFilter.TURKIYE -> R.string.country_turkiye
    CountryFilter.CANADA -> R.string.country_canada
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun CountryFilterRowPreview() {
    UniPortalTheme {
        CountryFilterRow(
            selected = CountryFilter.RUSSIA,
            onSelect = {},
            contentPadding = PaddingValues(16.dp),
            leadingChip = { FavouritesOnlyChip(selected = true, onSelectedChange = {}) },
        )
    }
}
