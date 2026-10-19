package com.fintrack.shared.feature.summary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.summary.domain.model.AffordabilitySimulationResult
import com.fintrack.shared.feature.summary.domain.model.AffordabilityVerdict
import com.fintrack.shared.feature.summary.domain.usecase.SimulateAffordabilityUseCase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffordabilitySimulatorDialog(
    onDismiss: () -> Unit,
    simulateAffordabilityUseCase: SimulateAffordabilityUseCase = koinInject(),
    accountRepository: AccountRepository = koinInject()
) {
    var itemPriceText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var simulationResult by remember { mutableStateOf<Result<AffordabilitySimulationResult>?>(null) }
    var accountsList by remember { mutableStateOf<List<Account>>(emptyList()) }

    LaunchedEffect(Unit) {
        val accResult = accountRepository.getAccounts()
        if (accResult is Result.Success) {
            accountsList = accResult.data
        }
    }

    LaunchedEffect(itemPriceText, selectedAccountId) {
        val price = itemPriceText.toDoubleOrNull()
        if (price != null && price > 0) {
            simulationResult = Result.Loading
            simulationResult = simulateAffordabilityUseCase(price, selectedAccountId)
        } else {
            simulationResult = null
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 440.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Can I Afford This?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Simulate unplanned purchase vs runway",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                OutlinedTextField(
                    value = itemPriceText,
                    onValueChange = { itemPriceText = it },
                    label = { Text("Item Price (e.g. 35,000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (accountsList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Source Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Surface(
                                    onClick = { selectedAccountId = null },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedAccountId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "All Accounts",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedAccountId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            items(accountsList, key = { it.id }) { acc ->
                                val isSelected = selectedAccountId == acc.id
                                Surface(
                                    onClick = { selectedAccountId = acc.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                when (val result = simulationResult) {
                    is Result.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }

                    is Result.Error -> {
                        Text(
                            text = result.exception.message ?: "Simulation error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    is Result.Success -> {
                        val sim = result.data
                        val verdictColor = when (sim.verdict) {
                            AffordabilityVerdict.SAFE -> GreenIncome
                            AffordabilityVerdict.CAUTION -> Color(0xFFFF9800)
                            AffordabilityVerdict.RISKY,
                            AffordabilityVerdict.IMMEDIATE_DEFICIT -> PinkExpense
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = verdictColor.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, verdictColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (sim.verdict) {
                                            AffordabilityVerdict.SAFE -> Icons.Default.CheckCircle
                                            AffordabilityVerdict.CAUTION -> Icons.Default.Info
                                            AffordabilityVerdict.RISKY,
                                            AffordabilityVerdict.IMMEDIATE_DEFICIT -> Icons.Default.Warning
                                        },
                                        contentDescription = null,
                                        tint = verdictColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = when (sim.verdict) {
                                            AffordabilityVerdict.SAFE -> "SAFE PURCHASE"
                                            AffordabilityVerdict.CAUTION -> "BORDERLINE CUSHION"
                                            AffordabilityVerdict.RISKY -> "HIGH OVERDRAFT RISK"
                                            AffordabilityVerdict.IMMEDIATE_DEFICIT -> "IMMEDIATE CASH DEFICIT"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = verdictColor
                                    )
                                }

                                Text(
                                    text = sim.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = sim.recommendation,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (sim.remainingBurnDaysAfterPurchase != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "Covers ${sim.remainingBurnDaysAfterPurchase} days of daily spending after bills",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (sim.savingsGoalImpactMessage != null) {
                                    Text(
                                        text = sim.savingsGoalImpactMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                }

                                HorizontalDivider(color = verdictColor.copy(alpha = 0.2f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Original EOM",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = sim.projectedEomBalanceBefore.toCurrencyString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "New EOM Projection",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = sim.projectedEomBalanceAfter.toCurrencyString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = verdictColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    null -> {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
