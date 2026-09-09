package com.fintrack.shared.feature.summary.ui.components

import Period
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.util.DateTimeUtils
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatAsShortDate
import com.fintrack.shared.feature.core.util.toDouble
import com.fintrack.shared.feature.core.util.toInt
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.DistributionSummary
import com.fintrack.shared.feature.summary.domain.model.TabType
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.fintrack.shared.ui.theme.SegmentColor1
import com.fintrack.shared.ui.theme.SegmentColor3
import com.fintrack.shared.ui.theme.SegmentColor4
import com.fintrack.shared.ui.theme.SegmentColor5
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoryTotalsCardWithTabs(
    tabType: TabType,
    period: Period,
    distributionResult: Result<DistributionSummary>,
    availableWeeks: List<String> = emptyList(),
    availableMonths: List<String> = emptyList(),
    availableYears: List<String> = emptyList(),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onWeekSelected: (String) -> Unit = {},
    onMonthSelected: (String) -> Unit = {},
    onYearSelected: (String) -> Unit = {},
    onPeriodSelected: (Period) -> Unit = {},
    onPreviousPeriod: () -> Unit = {},
    onNextPeriod: () -> Unit = {},
    hasPrevious: Boolean = true,
    hasNext: Boolean = true,
    onCategoryClick: (categoryName: String, categoryId: String) -> Unit = { _, _ -> },
    onRetry: () -> Unit = {}
) {
    // Use rememberSaveable to maintain selection across navigation
    // Reset selection only when period or tab type changes
    var selectedIndex by rememberSaveable(period.toString(), tabType.toString()) {
        mutableStateOf(-1)
    }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        DistributionHelpDialog(
            isIncome = tabType is TabType.Income,
            onDismiss = { showHelpDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(period.toString(), tabType.toString()) {
                    detectTapGestures { selectedIndex = -1 }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showHelpDialog = true }
                ) {
                    Text(
                        text = "Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Help",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onPreviousPeriod,
                        enabled = hasPrevious,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Period",
                            modifier = Modifier.size(20.dp),
                            tint = if (hasPrevious)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    PeriodSelector(
                        selectedPeriod = period,
                        availableWeeks = availableWeeks,
                        availableMonths = availableMonths,
                        availableYears = availableYears,
                        onWeekSelected = onWeekSelected,
                        onMonthSelected = onMonthSelected,
                        onYearSelected = onYearSelected,
                        onPeriodSelected = onPeriodSelected
                    )

                    IconButton(
                        onClick = onNextPeriod,
                        enabled = hasNext,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Period",
                            modifier = Modifier.size(20.dp),
                            tint = if (hasNext)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                Crossfade(
                    targetState = distributionResult,
                    animationSpec = tween(durationMillis = 300),
                    label = "ChartContentFade"
                ) { result ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 450.dp)
                    ) {
                        when (result) {
                            is Result.Loading -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    LoadingDonutChartSection()
                                    Spacer(Modifier.height(16.dp))
                                    LoadingCategoryList()
                                }
                            }

                            is Result.Error -> {
                                ErrorState(
                                    message = result.exception.message ?: "Failed to load distribution",
                                    onRetry = onRetry
                                )
                            }

                            is Result.Success -> {
                                val baseCategories = when (tabType) {
                                    is TabType.Income -> result.data.incomeCategories
                                    is TabType.Expense -> result.data.expenseCategories
                                }

                                if (baseCategories.isEmpty() && (tabType !is TabType.Expense || result.data.totalTransactionCost <= BigDecimal.ZERO)) {
                                    EmptyDistributionState()
                                } else {
                                    val displayModels = baseCategories.map {
                                        CategoryDisplayModel(
                                            name = it.category,
                                            categoryId = it.categoryId,
                                            amount = it.total,
                                            percentage = it.percentage,
                                            count = it.transactionCount,
                                            avgCount = it.averageTransactionCount,
                                            trend = it.momentumTrend,
                                            insights = it.topDescriptionInsights
                                        )
                                    }.toMutableList()

                                    if (tabType is TabType.Expense && result.data.totalTransactionCost > BigDecimal.ZERO) {
                                        displayModels.add(
                                            CategoryDisplayModel(
                                                name = "Transaction Fees",
                                                categoryId = "00000000-0000-4000-a000-000000000028",
                                                amount = result.data.totalTransactionCost,
                                                count = 0
                                            )
                                        )
                                    }

                                    val totalAmount =
                                        displayModels.fold(BigDecimal.ZERO) { acc, m -> acc + m.amount }
                                    val categorySums = displayModels.map { it.name to it.amount.toDouble().toFloat() }

                                    val othersInsight = result.data.othersInsightSummary

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        DonutChartSection(
                                            categorySums = categorySums,
                                            totalAmount = totalAmount.toDouble().toFloat(),
                                            selectedIndex = selectedIndex,
                                            onSelectedIndexChange = { index -> selectedIndex = index }
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        CategoryList(
                                            displayModels = displayModels,
                                            totalAmount = totalAmount,
                                            selectedIndex = selectedIndex,
                                            onSelectedIndexChange = { index -> selectedIndex = index },
                                            onCategoryClick = onCategoryClick,
                                            segmentColors = SegmentColors,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            othersInsight = othersInsight,
                                            isIncome = tabType is TabType.Income
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDistributionState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 36.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transactions found for this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    CommonErrorState(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        title = "Failed to load distribution",
        errorMessage = message,
        onRetry = onRetry
    )
}

@Composable
fun LoadingDonutChartSection() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val mockSegments = listOf(
                "A" to 1500f,
                "B" to 1200f,
                "C" to 800f,
                "D" to 600f,
                "E" to 400f
            )
            val mockTotal = 4500f

            LoadingInteractiveDonutWithText(
                mockSegments = mockSegments,
                mockTotal = mockTotal,
                segmentColors = SegmentColors,
                chartSize = 250.dp
            )
        }
    }
}

@Composable
fun LoadingInteractiveDonutWithText(
    mockSegments: List<Pair<String, Float>>,
    mockTotal: Float,
    segmentColors: List<Color>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 250.dp
) {
    Box(modifier = modifier.size(chartSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 36.dp.toPx()
            val diameter = size.minDimension - 80.dp.toPx()
            var startAngle = -90f

            mockSegments.forEachIndexed { index, (_, amount) ->
                val allocatedAngle = (((amount / mockTotal) * 360.0).toFloat())
                val sweep = (allocatedAngle - 360f * 0.03f).coerceAtLeast(0.5f)
                drawArc(
                    color = segmentColors[index % segmentColors.size].copy(alpha = 0.2f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += allocatedAngle
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedShimmerBox(modifier = Modifier.size(24.dp).clip(CircleShape))
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedShimmerBox(
                modifier = Modifier.width(80.dp).height(14.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedShimmerBox(
                modifier = Modifier.width(120.dp).height(24.dp).clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun LoadingCategoryList() {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedShimmerBox(modifier = Modifier.size(10.dp).clip(CircleShape))
                Spacer(Modifier.width(12.dp))
                AnimatedShimmerBox(
                    modifier = Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedShimmerBox(
                        modifier = Modifier.width(80.dp).height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedShimmerBox(
                        modifier = Modifier.width(40.dp).height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoryList(
    displayModels: List<CategoryDisplayModel>,
    totalAmount: BigDecimal,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onCategoryClick: (categoryName: String, categoryId: String) -> Unit = { _, _ -> },
    segmentColors: List<Color>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    othersInsight: String? = null,
    isIncome: Boolean
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val sortedModels = displayModels.sortedByDescending { it.amount }

    // Categories that should always be shown if they exist
    val priorityNames = listOf("Transaction Fees", "Transaction Cost")
    val priorityModels = sortedModels.filter { it.name in priorityNames }
    val regularModels = sortedModels.filter { it.name !in priorityNames }

    val topModelsList = mutableListOf<CategoryDisplayModel>()
    topModelsList.addAll(priorityModels)

    val remainingSlots = (4 - topModelsList.size).coerceAtLeast(0)
    topModelsList.addAll(regularModels.take(remainingSlots))

    topModelsList.sortByDescending { it.amount }

    val topNames = topModelsList.map { it.name }.toSet()
    val remainingModels = displayModels.filter { it.name !in topNames }

    val displayList = topModelsList.toMutableList()
    val othersAmount = remainingModels.fold(BigDecimal.ZERO) { acc, m -> acc + m.amount }
    if (othersAmount > BigDecimal.ZERO) {
        val aggregatedInsights = if (othersInsight != null) listOf(othersInsight) else {
            remainingModels.flatMap { it.insights ?: emptyList() }
                .groupBy { it }
                .map { it.key to it.value.size }
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
        }

        displayList.add(
            CategoryDisplayModel(
                name = "Others",
                categoryId = remainingModels.joinToString(",") { it.categoryId },
                amount = othersAmount,
                count = remainingModels.sumOf { it.count },
                insights = aggregatedInsights
            )
        )
    }

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        displayList.forEachIndexed { index, model ->
            val categoryName = model.name
            val amount = model.amount
            val percent = if (totalAmount > BigDecimal.ZERO) {
                try {
                    // Calculate using double precision to avoid BigDecimal division truncation (which returns 0 if amount < totalAmount)
                    (amount.toDouble() / totalAmount.toDouble() * 100).toInt()
                } catch (e: Exception) {
                    0
                }
            } else 0
            val color =
                if (index < 4) segmentColors[index % segmentColors.size] else segmentColors.last()
            val isSelected = selectedIndex == index

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (sharedTransitionScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    rememberSharedContentState(key = "header_card_$categoryName"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    clipInOverlayDuringTransition = OverlayClip(
                                        RoundedCornerShape(
                                            12.dp
                                        )
                                    )
                                )
                            }
                        } else Modifier
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.5f
                ),
                onClick = {
                    onSelectedIndexChange(index)
                    onCategoryClick(categoryName, model.categoryId)
                }
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (sharedTransitionScope != null) {
                                        with(sharedTransitionScope) {
                                            Modifier.sharedElement(
                                                rememberSharedContentState(key = "category_icon_$categoryName"),
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        }
                                    } else Modifier
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.then(
                                        if (sharedTransitionScope != null) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedElement(
                                                    rememberSharedContentState(key = "category_name_$categoryName"),
                                                    animatedVisibilityScope = animatedVisibilityScope
                                                )
                                            }
                                        } else Modifier
                                    )
                                )

                                if (model.trend != null && model.trend != "STABLE") {
                                    val isUp = model.trend == "UP"
                                    val trendText = when {
                                        isIncome && isUp -> "Growing"
                                        isIncome && !isUp -> "Dropping"
                                        !isIncome && isUp -> "Rising (3 mo)"
                                        !isIncome && !isUp -> "Saving (3 mo)"
                                        else -> ""
                                    }
                                    val trendColor = if (isIncome) {
                                        if (isUp) GreenIncome else PinkExpense
                                    } else {
                                        if (isUp) PinkExpense else GreenIncome
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = trendColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = trendText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = trendColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (model.count > 0) {
                                val avgText =
                                    model.avgCount?.let { " · Usually ${it.toInt()}" } ?: ""
                                Text(
                                    text = "${model.count} times$avgText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = amount.toDouble().toCurrencyString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                    }

                    if (model.insights != null && model.insights.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val insightText = if (categoryName == "Others" && othersInsight != null) {
                            "Mainly: $othersInsight"
                        } else {
                            "Mainly: ${model.insights.take(2).joinToString(", ")}"
                        }
                        Text(
                            text = insightText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: Period,
    availableWeeks: List<String> = emptyList(),
    availableMonths: List<String> = emptyList(),
    availableYears: List<String> = emptyList(),
    onPeriodSelected: (Period) -> Unit = {},
    onWeekSelected: (String) -> Unit = {},
    onMonthSelected: (String) -> Unit = {},
    onYearSelected: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val (options, selectedCode, onSelected, currentType) = when (selectedPeriod) {
        is Period.Week -> Triple(availableWeeks, selectedPeriod.code, onWeekSelected).let {
            it.copy(
                fourth = TimeSpan.WEEK
            )
        }

        is Period.Month -> Triple(
            availableMonths,
            selectedPeriod.code,
            onMonthSelected
        ).let { it.copy(fourth = TimeSpan.MONTH) }

        is Period.Year -> Triple(availableYears, selectedPeriod.code, onYearSelected).let {
            it.copy(
                fourth = TimeSpan.YEAR
            )
        }
    }

    val selectedIndex = remember(options, selectedCode) {
        options.indexOf(selectedCode).coerceAtLeast(0)
    }

    LaunchedEffect(currentType) {
        // Reset scroll position when switching between Week/Month/Year tabs 
        // to prevent being stuck at the bottom and to keep the menu stable.
        scrollState.scrollTo(0)
    }

    LaunchedEffect(expanded) {
        if (expanded && selectedIndex >= 0) {
            scrollState.scrollTo(selectedIndex * 120)
        }
    }

    Box {
        Row(
            modifier = Modifier
                .width(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatPeriodCode(selectedCode ?: "Select", currentType),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(160.dp)
                .height(350.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    RoundedCornerShape(16.dp)
                )
        ) {
            // TimeSpan Tabs inside Dropdown (Fixed at top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimeSpan.entries.forEach { span ->
                    val isSelected = when (span) {
                        TimeSpan.WEEK -> selectedPeriod is Period.Week
                        TimeSpan.MONTH -> selectedPeriod is Period.Month
                        TimeSpan.YEAR -> selectedPeriod is Period.Year
                    }

                    val tabBackgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(durationMillis = 200)
                    )

                    val tabContentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 200)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBackgroundColor)
                            .clickable {
                                when (span) {
                                    TimeSpan.WEEK -> availableWeeks.firstOrNull()
                                        ?.let { onPeriodSelected(Period.Week(it)) }

                                    TimeSpan.MONTH -> availableMonths.firstOrNull()
                                        ?.let { onPeriodSelected(Period.Month(it)) }

                                    TimeSpan.YEAR -> availableYears.firstOrNull()
                                        ?.let { onPeriodSelected(Period.Year(it)) }
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = span.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = tabContentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

            // Scrollable options with transition animation
            AnimatedContent(
                targetState = options to currentType,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150))
                        .togetherWith(fadeOut(animationSpec = tween(100)))
                },
                label = "PeriodListAnimation",
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { (currentOptions, type) ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            if (scrollState.maxValue > 0) {
                                val viewPortHeight = size.height
                                val contentHeight = viewPortHeight + scrollState.maxValue
                                val scrollOffset = scrollState.value.toFloat()

                                val knobHeight = (viewPortHeight / contentHeight) * viewPortHeight
                                val knobStart = (scrollOffset / contentHeight) * viewPortHeight

                                drawRoundRect(
                                    color = scrollbarColor,
                                    topLeft = Offset(size.width - 6.dp.toPx(), knobStart),
                                    size = Size(4.dp.toPx(), knobHeight),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                        .verticalScroll(scrollState)
                ) {
                    if (currentOptions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        currentOptions.forEach { option ->
                            val isItemSelected = option == selectedCode
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = formatPeriodCode(option, type),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isItemSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isItemSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.background(
                                    if (isItemSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                ),
                                onClick = {
                                    onSelected(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatPeriodCode(code: String, type: TimeSpan): String {
    return try {
        when (type) {
            TimeSpan.WEEK -> {
                val range = DateTimeUtils.getIsoWeekRange(code)
                if (range != null) {
                    val (weekStart, weekEnd) = range
                    val year = weekStart.year

                    if (weekStart.month == weekEnd.month) {
                        // Same month: "Jun 22 - 28, 2024"
                        "${weekStart.formatAsShortDate()} - ${weekEnd.dayOfMonth}, $year"
                    } else {
                        // Different months: "Jun 29 - Jul 5, 2024"
                        "${weekStart.formatAsShortDate()} - ${weekEnd.formatAsShortDate()}, $year"
                    }
                } else code
            }

            TimeSpan.MONTH -> DateTimeUtils.formatMonthCode(code)
            TimeSpan.YEAR -> code
        }
    } catch (_: Exception) {
        code
    }
}

private fun <A, B, C> Triple<A, B, C>.copy(fourth: TimeSpan): Quadruple<A, B, C, TimeSpan> {
    return Quadruple(first, second, third, fourth)
}

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

val SegmentColors = listOf(
    SegmentColor3, // Blue
    SegmentColor4, // Orange
    SegmentColor1, // Red
    SegmentColor5, // Teal
    PinkExpense    // Pink
)

enum class TimeSpan(val displayName: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

data class CategoryDisplayModel(
    val name: String,
    val categoryId: String,
    val amount: BigDecimal,
    val percentage: BigDecimal? = null,
    val count: Int,
    val avgCount: BigDecimal? = null,
    val trend: String? = null,
    val insights: List<String>? = null
)

@Composable
fun DistributionHelpDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("About Distribution")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "This section shows how your ${if (isIncome) "income is sourced" else "money is being spent"} across different categories for the selected period.",
                    style = MaterialTheme.typography.bodyMedium
                )

                HelpSection(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconColor = if (isIncome) GreenIncome else PinkExpense,
                    title = if (isIncome) "Growing" else "Rising (3 mo)",
                    description = if (isIncome)
                        "This income has consistently increased over the last 3 months."
                    else "You've spent more in this category for 3 consecutive months."
                )

                HelpSection(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconColor = if (isIncome) PinkExpense else GreenIncome,
                    title = if (isIncome) "Dropping" else "Saving (3 mo)",
                    description = if (isIncome)
                        "This income has consistently decreased over the last 3 months."
                    else "Great job! Your spending in this category has decreased for 3 consecutive months."
                )

                HelpSection(
                    icon = Icons.Outlined.Info,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    title = "Usually X times",
                    description = "This is your average number of transactions in this category over the last 6 months."
                )

                HelpSection(
                    icon = Icons.Default.PieChart,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "Mainly",
                    description = "Shows the most frequent descriptions or specific shops where this money was ${if (isIncome) "received from" else "spent"}."
                )
            }
        },
        confirmButton = {
            Text(
                "Got it",
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun HelpSection(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
