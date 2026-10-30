package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.ui.chart.ChartSeries
import com.fintrack.shared.feature.core.ui.chart.InteractiveChartData
import com.fintrack.shared.feature.core.ui.chart.InteractiveLineChart
import com.fintrack.shared.feature.summary.domain.model.DaySummary
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import kotlinx.datetime.LocalDate

@Composable
fun LineChart(
    data: List<DaySummary>,
    modifier: Modifier = Modifier,
) {
    val sortedData = remember(data) { data.sortedBy { it.date } }
    val labels = remember(sortedData) {
        sortedData.map { day ->
            try {
                val date = LocalDate.parse(day.date)
                val monthName = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                if (date.day == 1) "${date.day} $monthName" else day.date.split("-").last()
            } catch (_: Exception) {
                day.date
            }
        }
    }

    val incomeValues = remember(sortedData) { sortedData.map { it.income.doubleValue(false) } }
    val expenseValues = remember(sortedData) { sortedData.map { it.expense.doubleValue(false) } }

    val chartData = remember(labels, incomeValues, expenseValues) {
        InteractiveChartData(
            labels = labels,
            series = listOf(
                ChartSeries(name = "Income", color = GreenIncome, values = incomeValues),
                ChartSeries(name = "Expenses", color = PinkExpense, values = expenseValues)
            )
        )
    }

    InteractiveLineChart(
        chartData = chartData,
        chartHeight = 200.dp,
        showYAxis = true,
        isScrollable = true,
        modifier = modifier.fillMaxWidth()
    )
}
