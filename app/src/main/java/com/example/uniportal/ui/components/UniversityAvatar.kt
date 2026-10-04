package com.example.uniportal.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.AvatarContentColor
import com.example.uniportal.ui.theme.AvatarGradients
import com.example.uniportal.ui.theme.UniPortalTheme
import com.example.uniportal.util.monogram

@Composable
fun UniversityAvatar(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val gradient = remember(id) { AvatarGradients[id.hashCode().mod(AvatarGradients.size)] }
    val initials = remember(name) { monogram(name) }

    val fontSize = with(LocalDensity.current) { (size * MONOGRAM_SIZE_RATIO).toSp() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradient))

            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = AvatarContentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun PortalRing(
    modifier: Modifier = Modifier,
    ringWidth: Dp = 3.dp,
    gap: Dp = 6.dp,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ringBrush = remember(colors) {
        Brush.sweepGradient(listOf(colors.primary, colors.secondary, colors.tertiary, colors.primary))
    }
    val rotation by rememberInfiniteTransition(label = "portalRing").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = RING_ROTATION_MS, easing = LinearEasing)),
        label = "portalRingRotation",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation }
                .border(width = ringWidth, brush = ringBrush, shape = CircleShape),
        )
        Box(modifier = Modifier.padding(ringWidth + gap)) {
            content()
        }
    }
}

@Composable
fun PortalLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .border(
                width = size / 10,
                brush = Brush.sweepGradient(listOf(colors.primary, colors.secondary, colors.tertiary, colors.primary)),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size / 3)
                .background(
                    brush = Brush.radialGradient(listOf(colors.primary, Color.Transparent)),
                    shape = CircleShape,
                ),
        )
    }
}

private const val MONOGRAM_SIZE_RATIO = 0.36f
private const val RING_ROTATION_MS = 12_000

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun UniversityAvatarPreview() {
    UniPortalTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewData.universities.forEach { UniversityAvatar(id = it.id, name = it.name) }
            PortalLogo()
            PortalRing {
                UniversityAvatar(id = PreviewData.mtsu.id, name = PreviewData.mtsu.name, size = 96.dp)
            }
        }
    }
}
