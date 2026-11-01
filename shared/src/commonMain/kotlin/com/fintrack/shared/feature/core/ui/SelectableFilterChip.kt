package com.fintrack.shared.feature.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.ui.util.rememberHapticFeedback

@Composable
fun SelectableFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val animatedBgColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primaryContainer else colorScheme.surface,
        animationSpec = tween(150),
        label = "chipBgColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
        animationSpec = tween(150),
        label = "chipContentColor"
    )

    val hapticFeedback = rememberHapticFeedback()

    Surface(
        onClick = {
            hapticFeedback()
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = animatedBgColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .animateContentSize(animationSpec = tween(200)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = animatedContentColor
            )
        }
    }
}
