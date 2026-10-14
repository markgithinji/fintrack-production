package com.fintrack.shared.feature.core.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color,
    highlightFontWeight: FontWeight = FontWeight.Bold
): AnnotatedString {
    if (query.isBlank() || text.isBlank()) return AnnotatedString(text)

    val queryWords = query.lowercase().trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
    if (queryWords.isEmpty()) return AnnotatedString(text)

    val lowercaseText = text.lowercase()
    return buildAnnotatedString {
        append(text)

        queryWords.forEach { word ->
            var startIndex = lowercaseText.indexOf(word)
            while (startIndex >= 0) {
                val endIndex = (startIndex + word.length).coerceAtMost(text.length)
                addStyle(
                    style = SpanStyle(
                        color = highlightColor,
                        fontWeight = highlightFontWeight,
                        background = highlightColor.copy(alpha = 0.15f)
                    ),
                    start = startIndex,
                    end = endIndex
                )
                startIndex = lowercaseText.indexOf(word, endIndex)
            }
        }
    }
}
