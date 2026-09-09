package com.fintrack.shared.feature.budget.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.util.rememberThrottleClick
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatAsShortDateWithYear
import com.fintrack.shared.feature.core.util.formatToSinglePrecision
import com.fintrack.shared.feature.core.util.toDouble
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.ui.theme.GreenIncome
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BudgetScreen(
    refreshTrigger: Int,
    viewModel: BudgetViewModel = koinViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAddBudget: () -> Unit = {},
    onBudgetClick: (BudgetWithStatus) -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No SharedTransitionScope found")

    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    val throttledOnBudgetClick = rememberThrottleClick(onClick = onBudgetClick)
    val throttledOnAddBudget = rememberThrottleClick(onClick = onAddBudget)

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.reloadBudgets(showLoading = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val currentBudgets = budgets) {
            is Result.Loading -> {
                BudgetScreenLoadingState(paddingValues)
            }

            is Result.Error -> {
                BudgetErrorRetryState(
                    error = currentBudgets.exception,
                    onRetry = { viewModel.reloadBudgets() }
                )
            }

            is Result.Success -> {
                val data = currentBudgets.data
                
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 32.dp
                    )
                ) {
                    if (data.isNotEmpty()) {
                        item {
                            BudgetSummaryHeader(data)
                        }

                        item {
                            with(sharedTransitionScope) {
                                SexyAddBudgetButton(
                                    onClick = throttledOnAddBudget,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .sharedBounds(
                                            rememberSharedContentState(key = "budget_header_new"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            boundsTransform = { _, _ ->
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            },
                                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp))
                                        )
                                )
                            }
                        }

                        items(data, key = { it.budget.id ?: it.budget.name }) { budgetWithStatus ->
                            with(sharedTransitionScope) {
                                BudgetItem(
                                    budgetWithStatus = budgetWithStatus,
                                    onClick = { throttledOnBudgetClick(budgetWithStatus) },
                                    modifier = Modifier
                                        .animateItem()
                                        .sharedBounds(
                                            rememberSharedContentState(key = "budget_header_${budgetWithStatus.budget.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            boundsTransform = { _, _ ->
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            },
                                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                                        )
                                )
                            }
                        }
                    } else {
                        item {
                            BudgetEmptyState(
                                onAddBudget = throttledOnAddBudget,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryHeader(budgets: List<BudgetWithStatus>) {
    val totalLimit = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.budget.limit }
    val totalSpent = budgets.fold(BigDecimal.ZERO) { acc, b -> acc + b.status.spent }
    val progress = if (totalLimit > BigDecimal.ZERO) {
        try {
            (totalSpent.toDouble() / totalLimit.toDouble()).toFloat().coerceIn(0f, 1f)
        } catch (_: Exception) {
            0f
        }
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Monthly Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = totalLimit.toCurrencyString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Spent ${totalSpent.toCurrencyString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${(progress * 100).toInt()}% used",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
fun SexyAddBudgetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isPressed) 90f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "add_icon_rotation"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.8f else 1f,
                        animationSpec = tween(durationMillis = 200),
                        label = "add_icon_scale"
                    )

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Budget",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = rotation
                                scaleX = scale
                                scaleY = scale
                            },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Create New Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        text = "Plan your spending strategically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun BudgetItem(
    budgetWithStatus: BudgetWithStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = budgetWithStatus.budget
    val status = budgetWithStatus.status
    val progress = if (budget.limit > BigDecimal.ZERO) {
        try {
            (status.spent.toDouble() / budget.limit.toDouble()).toFloat().coerceAtMost(1f)
        } catch (_: Exception) {
            0f
        }
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${budget.startDate.formatAsShortDateWithYear()} - ${budget.endDate.formatAsShortDateWithYear()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusChip(status.percentageUsed, status.isExceeded)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status.spent.toCurrencyString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "of ${budget.limit.toCurrencyString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (status.isExceeded) MaterialTheme.colorScheme.error
                    else if (progress > 0.8f) Color(0xFFFF9800)
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${status.percentageUsed.formatToSinglePrecision()}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (status.isExceeded) "Over budget" else "${status.remaining.toCurrencyString()} left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isExceeded) MaterialTheme.colorScheme.error else GreenIncome
                    )
                }
            }

            if (budget.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    budget.categories.take(3).forEach { category ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (budget.categories.size > 3) {
                        Text(
                            text = "+${budget.categories.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(percentage: BigDecimal, isExceeded: Boolean) {
    val (label, color) = when {
        isExceeded -> "Exceeded" to MaterialTheme.colorScheme.error
        percentage > BigDecimal.fromInt(90) -> "Critical" to Color(0xFFF44336)
        percentage > BigDecimal.fromInt(75) -> "Warning" to Color(0xFFFF9800)
        else -> "On Track" to GreenIncome
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BudgetEmptyState(
    onAddBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Master Your Money",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Create your first budget to gain clarity on your spending habits and achieve your financial goals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onAddBudget,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(0.8f),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add First Budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BudgetErrorRetryState(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    CommonErrorState(
        modifier = modifier.fillMaxSize(),
        title = "Something went wrong",
        error = error,
        onRetry = onRetry
    )
}

@Composable
fun BudgetScreenLoadingState(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding()
        )
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                val shimmerColors = listOf(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                )
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedShimmerBox(
                        modifier = Modifier.width(120.dp).height(20.dp).clip(RoundedCornerShape(4.dp)),
                        shimmerColors = shimmerColors
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier.width(180.dp).height(32.dp).clip(RoundedCornerShape(8.dp)),
                        shimmerColors = shimmerColors
                    )
                    AnimatedShimmerBox(
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        shimmerColors = shimmerColors
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                AnimatedShimmerBox(
                    modifier = Modifier.fillMaxSize(),
                    shimmerColors = listOf(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.03f),
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    )
                )
            }
        }

        items(3) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            AnimatedShimmerBox(modifier = Modifier.width(140.dp).height(24.dp))
                            Spacer(Modifier.height(4.dp))
                            AnimatedShimmerBox(modifier = Modifier.width(100.dp).height(12.dp))
                        }
                        AnimatedShimmerBox(modifier = Modifier.width(60.dp).height(24.dp).clip(RoundedCornerShape(12.dp)))
                    }
                    AnimatedShimmerBox(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AnimatedShimmerBox(modifier = Modifier.width(80.dp).height(12.dp))
                        AnimatedShimmerBox(modifier = Modifier.width(60.dp).height(12.dp))
                    }
                }
            }
        }
    }
}
