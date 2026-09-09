package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun LowerRightWavesBackground(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .background(primaryColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Base Gradient for depth
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.9f)),
                    startY = 0f,
                    endY = h
                )
            )

            // 2. Subtle Dot Grid Pattern (Texture)
            val dotSpacing = 24f
            val dotRadius = 1.5f
            for (x in 0..(w / dotSpacing).toInt()) {
                for (y in 0..(h / dotSpacing).toInt()) {
                    val alpha = if ((x + y) % 3 == 0) 0.04f else 0.02f
                    drawCircle(
                        color = primaryContainer.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(x * dotSpacing, y * dotSpacing)
                    )
                }
            }

            // 3. Large Layered Organic Shapes
            // First large shape (Bottom Left to Top Right diagonal)
            val path1 = Path().apply {
                moveTo(0f, h * 0.7f)
                cubicTo(w * 0.3f, h * 0.6f, w * 0.5f, h * 0.9f, w * 0.8f, h * 0.4f)
                cubicTo(w * 0.9f, h * 0.2f, w, h * 0.3f, w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = path1,
                brush = Brush.linearGradient(
                    colors = listOf(primaryContainer.copy(alpha = 0.12f), Color.Transparent),
                    start = Offset(w * 0.2f, h),
                    end = Offset(w, 0f)
                )
            )

            // Second shape (Bottom Right accent)
            val path2 = Path().apply {
                moveTo(w * 0.4f, h)
                cubicTo(w * 0.6f, h * 0.5f, w * 0.9f, h * 0.8f, w, h * 0.5f)
                lineTo(w, h)
                close()
            }
            drawPath(
                path = path2,
                brush = Brush.linearGradient(
                    colors = listOf(primaryContainer.copy(alpha = 0.18f), primaryContainer.copy(alpha = 0.05f)),
                    start = Offset(w * 0.7f, h),
                    end = Offset(w, h * 0.4f)
                )
            )

            // 4. Accent Glowing Orbs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryContainer.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.85f),
                    radius = w * 0.4f
                ),
                center = Offset(w * 0.85f, h * 0.85f),
                radius = w * 0.4f
            )

            // 5. Modern thin lines for "currency/financial" feel
            val lineCount = 12
            val lineGap = 12f
            for (i in 0 until lineCount) {
                val shift = i * lineGap
                drawLine(
                    color = primaryContainer.copy(alpha = 0.06f),
                    start = Offset(w - 150f + shift, h),
                    end = Offset(w + shift, h - 150f),
                    strokeWidth = 1f
                )
            }
        }
    }
}
