package com.fintrack.shared.feature.summary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.SpendSpikeAnomaly
import com.fintrack.shared.ui.theme.PinkExpense
import com.ionspin.kotlin.bignum.decimal.BigDecimal

@Composable
fun SpendSpikeAlertCard(
    spikesResult: Result<List<SpendSpikeAnomaly>>,
    modifier: Modifier = Modifier
) {
    val sampleSpikes = remember {
        listOf(
            SpendSpikeAnomaly(
                categoryName = "Dining & Food",
                categoryId = "sample_dining",
                currentMonthAmount = BigDecimal.fromInt(12500),
                historical3MonthAvgAmount = BigDecimal.fromInt(8600),
                spikePercentage = BigDecimal.fromInt(45),
                alertMessage = "⚡ Dining & Food is +45% higher than your 3-month average (KES 12,500 vs KES 8,600 baseline)."
            ),
            SpendSpikeAnomaly(
                categoryName = "Shopping & Retail",
                categoryId = "sample_shopping",
                currentMonthAmount = BigDecimal.fromInt(6800),
                historical3MonthAvgAmount = BigDecimal.fromInt(4200),
                spikePercentage = BigDecimal.fromInt(62),
                alertMessage = "⚡ Shopping & Retail is +62% higher than your 3-month average (KES 6,800 vs KES 4,200 baseline)."
            )
        )
    }

    val spikes = if (spikesResult is Result.Success && spikesResult.data.isNotEmpty()) {
        spikesResult.data
    } else {
        sampleSpikes
    }

    var dismissedIds by remember { mutableStateOf(setOf<String>()) }
    val visibleSpikes = spikes.filter { !dismissedIds.contains(it.categoryId) }
    if (visibleSpikes.isEmpty()) return

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Spend Spike Detector",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            visibleSpikes.take(3).forEach { spike ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PinkExpense.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, PinkExpense.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = spike.alertMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { dismissedIds = dismissedIds + spike.categoryId },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Alert",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
