package com.fintrack.shared.feature.planning.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.planning.domain.model.Asset
import com.fintrack.shared.feature.planning.domain.model.AssetType
import com.fintrack.shared.ui.theme.GreenIncome

@Composable
fun AssetsSummaryCard(
    totalValue: Double,
    totalAnnualYield: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GreenIncome.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = GreenIncome,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Total Investment Assets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Accumulated Holdings & MMF Yields",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Portfolio Value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalValue.toCurrencyString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenIncome
                    )
                }

                if (totalAnnualYield > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GreenIncome.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = GreenIncome,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "+${totalAnnualYield.toCurrencyString()}/yr Passive Yield",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GreenIncome,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetCard(
    asset: Asset,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = when (asset.assetType) {
                                    AssetType.GENERAL_ASSET -> "Asset Holding"
                                    AssetType.MMF_YIELD -> "MMF Yield"
                                    AssetType.SACCO_SHARES -> "Sacco Shares"
                                    AssetType.REAL_ESTATE -> "Real Estate"
                                    AssetType.STOCKS_CRYPTO -> "Stocks & Crypto"
                                    AssetType.CASH_SAVINGS -> "Cash Savings"
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (asset.annualYieldPercentage != null && asset.annualYieldPercentage > 0) {
                            Text(
                                text = "${asset.annualYieldPercentage}% p.a.",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenIncome
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = asset.currentBalance.toCurrencyString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenIncome
                    )
                    if (asset.annualPassiveYield > 0) {
                        Text(
                            text = "+${asset.annualPassiveYield.toCurrencyString()}/yr",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = GreenIncome
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Asset",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetDialog(
    onAssetCreated: (name: String, assetType: AssetType, balance: Double, yieldPercentage: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var yieldText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AssetType.MMF_YIELD) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
                            .background(GreenIncome.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = GreenIncome,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "New Asset or Investment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Log MMFs, Saccos, Real Estate, or Stock holdings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    label = { Text("Asset Name (e.g. Sanlam MMF, Sacco Shares, Land)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = {
                        balanceText = it
                        errorMsg = null
                    },
                    label = { Text("Current Value / Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = yieldText,
                    onValueChange = {
                        yieldText = it
                        errorMsg = null
                    },
                    label = { Text("Expected Annual Yield % (Optional, e.g. 11.5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val bal = balanceText.toDoubleOrNull()
                            val yield = yieldText.toDoubleOrNull()
                            if (name.isBlank()) {
                                errorMsg = "Please enter an asset name"
                            } else if (bal == null || bal <= 0) {
                                errorMsg = "Please enter a valid asset balance"
                            } else {
                                onAssetCreated(name.trim(), selectedType, bal, yield)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Asset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
