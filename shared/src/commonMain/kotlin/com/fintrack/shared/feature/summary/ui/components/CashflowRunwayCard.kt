package com.fintrack.shared.feature.summary.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.CashflowRunway
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.fintrack.shared.ui.theme.SegmentColor1
import com.fintrack.shared.ui.theme.SegmentColor2
import com.fintrack.shared.ui.theme.SegmentColor3
import com.fintrack.shared.ui.theme.SegmentColor4

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CashflowRunwayCard(
    runwayResult: Result<CashflowRunway>,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog && runwayResult is Result.Success) {
        CashflowRunwayBottomSheet(
            runway = runwayResult.data,
            onDismiss = { showHelpDialog = false }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(202.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Sleeker Compact Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showHelpDialog = true }
                ) {
                    Text(
                        text = "Cashflow Runway",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Runway Info",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            when (runwayResult) {
                is Result.Loading -> {
                    CashflowRunwayShimmerLoading()
                }

                is Result.Error -> {
                    CommonErrorState(
                        title = "Unable to calculate cashflow runway",
                        error = runwayResult.exception,
                        onRetry = onRetry
                    )
                }

                is Result.Success -> {
                    val runway = runwayResult.data

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Main EOM Projection Banner
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (runway.isOverdraftRisk) {
                                PinkExpense.copy(alpha = 0.08f)
                            } else {
                                GreenIncome.copy(alpha = 0.08f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (runway.isOverdraftRisk) PinkExpense.copy(alpha = 0.25f) else GreenIncome.copy(alpha = 0.25f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Projected End-of-Month",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Risk Badge
                                    Surface(
                                        shape = CircleShape,
                                        color = if (runway.isOverdraftRisk) PinkExpense else GreenIncome
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (runway.isOverdraftRisk) Icons.Default.Warning else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(10.dp),
                                                tint = Color.White
                                            )
                                            Text(
                                                text = when {
                                                    runway.isOverdraftRisk && runway.estimatedDepletionDays != null && runway.estimatedDepletionDays > 0 -> "Depletes in ${runway.estimatedDepletionDays}d"
                                                    runway.isOverdraftRisk -> "Overdraft Risk"
                                                    else -> "Safe Runway"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = runway.projectedEomBalance.toCurrencyString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = if (runway.isOverdraftRisk) PinkExpense else GreenIncome
                                )
                            }
                        }

                        // 2x2 Compact Metric Grid
                        Column(
                            modifier = Modifier.weight(2f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RunwayMetricItem(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    label = "Daily Burn",
                                    value = "${runway.dailyBurnRate.toCurrencyString()}/day",
                                    icon = Icons.AutoMirrored.Filled.ShowChart,
                                    tint = SegmentColor1
                                )
                                RunwayMetricItem(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    label = "Days Left",
                                    value = "${runway.remainingDaysInMonth} days",
                                    icon = Icons.Default.CalendarToday,
                                    tint = SegmentColor2
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RunwayMetricItem(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    label = "Est. Variable",
                                    value = runway.projectedVariableExpense.toCurrencyString(),
                                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                                    tint = SegmentColor3
                                )
                                RunwayMetricItem(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    label = "Upcoming Bills",
                                        value = runway.upcomingBillsTotal.toCurrencyString(),
                                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                        tint = SegmentColor4
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun RunwayMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = tint
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CashflowRunwayShimmerLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Banner Shimmer Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Label line shimmer
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(110.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    // Risk badge shimmer
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .width(62.dp)
                            .height(18.dp)
                            .clip(CircleShape)
                    )
                }

                // Balance amount line shimmer
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(130.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // 2x2 Metric Grid Shimmers
        Column(
            modifier = Modifier.weight(2f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShimmerMetricItem(modifier = Modifier.weight(1f).fillMaxHeight())
                ShimmerMetricItem(modifier = Modifier.weight(1f).fillMaxHeight())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShimmerMetricItem(modifier = Modifier.weight(1f).fillMaxHeight())
                ShimmerMetricItem(modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ShimmerMetricItem(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon Circle Shimmer
            AnimatedShimmerBox(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )

            // Text Lines Shimmer
            Column {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(52.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(2.dp))
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(68.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowRunwayBottomSheet(
    runway: CashflowRunway,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Cashflow Runway Forecast",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Daily Burn & Scheduled Bills Breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Main Forecast Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (runway.isOverdraftRisk) PinkExpense.copy(alpha = 0.08f) else GreenIncome.copy(alpha = 0.08f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (runway.isOverdraftRisk) PinkExpense.copy(alpha = 0.25f) else GreenIncome.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Projected End-of-Month Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = runway.projectedEomBalance.toCurrencyString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (runway.isOverdraftRisk) PinkExpense else GreenIncome
                    )
                }
            }

            // Steps & Metrics Breakdown Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Forecast Calculations:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    RunwayStepRow(
                        stepNumber = "1",
                        title = "Starting Cash Balance",
                        description = runway.currentBalance.toCurrencyString()
                    )

                    RunwayStepRow(
                        stepNumber = "2",
                        title = "Daily Burn Velocity",
                        description = "${runway.dailyBurnRate.toCurrencyString()}/day for ${runway.remainingDaysInMonth} remaining days (${runway.projectedVariableExpense.toCurrencyString()} est.)"
                    )

                    RunwayStepRow(
                        stepNumber = "3",
                        title = "Scheduled Bills Total",
                        description = runway.upcomingBillsTotal.toCurrencyString()
                    )
                }
            }

            // Action Tip Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💡 Runway Extension Tip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = if (runway.isOverdraftRisk) {
                            "Pausing or rescheduling upcoming non-essential bills will immediately prevent your cash balance from dipping into overdraft before month end."
                        } else {
                            "Your cash runway is healthy! Route any remaining month-end surplus into your Savings Vaults to build your emergency buffer."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Action Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RunwayStepRow(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
