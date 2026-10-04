package com.example.uniportal.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.domain.model.ErrorType
import com.example.uniportal.ui.theme.UniPortalTheme
import kotlinx.coroutines.delay

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    showAfterMillis: Long = 0L,
) {
    var isVisible by remember { mutableStateOf(showAfterMillis <= 0L) }
    LaunchedEffect(showAfterMillis) {
        if (!isVisible) {
            delay(showAfterMillis)
            isVisible = true
        }
    }
    val taggedModifier = modifier.testTag(TestTags.LOADING_STATE)
    if (isVisible) {
        CenteredColumn(modifier = taggedModifier) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Box(modifier = taggedModifier.fillMaxSize())
    }
}

@Composable
fun ErrorState(
    type: ErrorType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: (@Composable () -> Unit)? = null,
) {
    StateMessage(
        emoji = type.emoji(),
        title = stringResource(type.titleRes()),
        message = stringResource(type.messageRes()),
        modifier = modifier.testTag(TestTags.ERROR_STATE),
        haloColor = MaterialTheme.colorScheme.error,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onRetry, modifier = Modifier.testTag(TestTags.RETRY_BUTTON)) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.action_retry))
            }
            secondaryAction?.invoke()
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    StateMessage(
        emoji = emoji,
        title = title,
        message = message,
        modifier = modifier.testTag(TestTags.EMPTY_STATE),
        haloColor = MaterialTheme.colorScheme.secondary,
        action = action,
    )
}

@Composable
private fun StateMessage(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    haloColor: Color = MaterialTheme.colorScheme.primary,
    action: (@Composable () -> Unit)? = null,
) {
    val emojiSize = with(LocalDensity.current) { EMOJI_SIZE.toSp() }
    CenteredColumn(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(HALO_SIZE)
                .background(
                    brush = Brush.radialGradient(listOf(haloColor.copy(alpha = HALO_ALPHA), Color.Transparent)),
                    shape = CircleShape,
                )

                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = emojiSize)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,

            modifier = Modifier
                .widthIn(max = MESSAGE_MAX_WIDTH)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            modifier = Modifier.widthIn(max = MESSAGE_MAX_WIDTH),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
private fun CenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

private const val HALO_ALPHA = 0.22f
private val HALO_SIZE = 112.dp
private val EMOJI_SIZE = 48.dp
private val MESSAGE_MAX_WIDTH = 360.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 360)
@Composable
private fun LoadingStatePreview() {
    UniPortalTheme { LoadingState() }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 420)
@Composable
private fun ErrorStatePreview() {
    UniPortalTheme { ErrorState(type = ErrorType.NETWORK, onRetry = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 360)
@Composable
private fun EmptyStatePreview() {
    UniPortalTheme {
        EmptyState(
            emoji = "🔍",
            title = stringResource(R.string.empty_search_title),
            message = stringResource(R.string.empty_search_message),
        )
    }
}
