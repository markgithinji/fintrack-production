package com.fintrack.shared.feature.summary.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DonutChartSection(
    categorySums: List<Pair<String, Float>>,
    totalAmount: Float,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    if (categorySums.isEmpty() || totalAmount <= 0f) return

    val sorted = categorySums.sortedByDescending { it.second }
    
    // Categories that should always be shown if they exist
    val priorityNames = listOf("Transaction Fees", "Transaction Cost")
    val priorityItems = sorted.filter { it.first in priorityNames }
    val regularItems = sorted.filter { it.first !in priorityNames }

    val topForChart = mutableListOf<Pair<String, Float>>()
    topForChart.addAll(priorityItems)
    
    val remainingSlots = (4 - topForChart.size).coerceAtLeast(0)
    topForChart.addAll(regularItems.take(remainingSlots))
    
    // Sort the final selection for better visualization
    topForChart.sortByDescending { it.second }

    val topNames = topForChart.map { it.first }.toSet()
    val othersTotal = categorySums.filter { it.first !in topNames }.sumOf { it.second.toDouble() }.toFloat()

    if (othersTotal > 0f) {
        topForChart.add("Others" to othersTotal)
    }

    val chartColors = List(topForChart.size) { index ->
        if (index < 4) SegmentColors[index] else SegmentColors.last()
    }

    val chartData = topForChart.map { it.first to it.second.toDouble() }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        InteractiveDonutWithText(
            categorySums = chartData,
            totalAmount = totalAmount.toDouble(),
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            segmentColors = chartColors
        )
    }
}