package com.example.uniportal.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.ui.theme.PortalTheme
import com.example.uniportal.ui.theme.UniPortalTheme

enum class BannerTone {
    WARNING,

    INFO,
}

@Composable
fun OfflineBanner(
    text: String,
    tone: BannerTone,
    modifier: Modifier = Modifier,
    icon: ImageVector = if (tone == BannerTone.WARNING) Icons.Filled.Warning else Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val color = when (tone) {
        BannerTone.WARNING -> PortalTheme.colors.warning
        BannerTone.INFO -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.OFFLINE_BANNER)

            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = BANNER_CONTAINER_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, color.copy(alpha = BANNER_BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(start = 12.dp, end = if (actionLabel != null) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = color),
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

private const val BANNER_CONTAINER_ALPHA = 0.12f
private const val BANNER_BORDER_ALPHA = 0.35f

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun OfflineBannerPreview() {
    UniPortalTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OfflineBanner(
                text = stringResource(R.string.search_banner_offline, stringResource(R.string.time_just_now)),
                tone = BannerTone.WARNING,
            )
            OfflineBanner(
                text = stringResource(R.string.search_banner_local_catalog),
                tone = BannerTone.INFO,
                actionLabel = stringResource(R.string.action_retry),
                onAction = {},
            )
        }
    }
}
