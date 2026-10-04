package com.example.uniportal.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.ui.theme.AvatarGradients
import com.example.uniportal.ui.theme.UniPortalTheme

@Composable
fun EmojiBadge(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    seed: Int = emoji.hashCode(),
) {
    val gradient = remember(seed) { AvatarGradients[seed.mod(AvatarGradients.size)] }
    val fill = remember(gradient) { Brush.linearGradient(gradient.map { it.copy(alpha = FILL_ALPHA) }) }
    val rim = remember(gradient) { Brush.linearGradient(gradient.map { it.copy(alpha = RIM_ALPHA) }) }

    val fontSize = with(LocalDensity.current) { (size * EMOJI_SIZE_RATIO).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(width = 1.dp, brush = rim, shape = CircleShape)

            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = fontSize, maxLines = 1)
    }
}

private const val FILL_ALPHA = 0.26f
private const val RIM_ALPHA = 0.6f
private const val EMOJI_SIZE_RATIO = 0.5f

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun EmojiBadgePreview() {
    UniPortalTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShortlistRules.EMOJI_OPTIONS.take(5).forEachIndexed { index, emoji ->
                EmojiBadge(emoji = emoji, seed = index)
            }
        }
    }
}
