package com.example.uniportal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.ui.theme.UniPortalTheme
import com.example.uniportal.util.flagEmoji

@Composable
fun CountryBadge(
    country: String,
    countryCode: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = flagEmoji(countryCode),
                modifier = Modifier.clearAndSetSemantics {},
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = countryLabel(country, countryCode),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun CountryBadgePreview() {
    UniPortalTheme {
        CountryBadge(country = "Russian Federation", countryCode = "RU", modifier = Modifier.padding(16.dp))
    }
}
