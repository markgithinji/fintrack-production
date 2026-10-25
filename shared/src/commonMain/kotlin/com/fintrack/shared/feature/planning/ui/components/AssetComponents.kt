package com.fintrack.shared.feature.planning.ui.components

import com.fintrack.shared.feature.core.ui.SelectableFilterChip

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.fintrack.shared.feature.core.ui.util.rememberLongPressHapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
    assets: List<Asset>,
    modifier: Modifier = Modifier
) {
    val totalValue = assets.sumOf { it.currentBalance }
    val totalAnnualYield = assets.sumOf { it.annualPassiveYield }
    val liquidValue = assets.filter { it.isLiquid }.sumOf { it.currentBalance }
    val compound3YrTotal = assets.sumOf { it.compoundValue3Years }
    val purePassiveGrowth = (compound3YrTotal - totalValue).coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Investment Assets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = totalValue.toCurrencyString(),
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
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Liquid Cash Reserves",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${liquidValue.toCurrencyString()} (Ready in 24h)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (totalAnnualYield > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Annual Passive Yield",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "+${totalAnnualYield.toCurrencyString()}/yr",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenIncome
                        )
                    }
                }
            }

            if (purePassiveGrowth > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "3-Year Compound Growth Forecast",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "At current yields, portfolio will reach ${compound3YrTotal.toCurrencyString()} in 3 years (+${purePassiveGrowth.toCurrencyString()} passive growth).",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetCard(
    asset: Asset,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticLongPress = rememberLongPressHapticFeedback()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = {
                    hapticLongPress()
                    onDeleteClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
            }

            if (asset.annualYieldPercentage != null && asset.annualYieldPercentage > 0) {
                val realReturn = asset.netRealReturnPercentage
                val realReturnRounded = kotlin.math.round(realReturn * 10.0) / 10.0
                val isBeating = asset.isBeatingInflation

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBeating) GreenIncome.copy(alpha = 0.1f) else com.fintrack.shared.ui.theme.PinkExpense.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (isBeating) {
                            "Beating 6.5% inflation (+${realReturnRounded}% net real return)"
                        } else {
                            "Losing to 6.5% inflation (${realReturnRounded}% net real return)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBeating) GreenIncome else com.fintrack.shared.ui.theme.PinkExpense,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetDialog(
    asset: Asset? = null,
    onAssetSaved: (id: String?, name: String, assetType: AssetType, balance: Double, yieldPercentage: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(asset?.name ?: "") }
    var balanceText by remember { mutableStateOf(asset?.currentBalance?.let { if (it > 0) it.toString() else "" } ?: "") }
    var yieldText by remember { mutableStateOf(asset?.annualYieldPercentage?.let { if (it > 0) it.toString() else "" } ?: "") }
    var selectedType by remember { mutableStateOf(asset?.assetType ?: AssetType.MMF_YIELD) }
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
                            text = if (asset != null) "Edit Asset or Investment" else "New Asset or Investment",
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

                // Asset Type Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Asset Type",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AssetType.entries) { type ->
                            val typeLabel = when (type) {
                                AssetType.MMF_YIELD -> "MMF Yield"
                                AssetType.SACCO_SHARES -> "Sacco Shares"
                                AssetType.REAL_ESTATE -> "Real Estate"
                                AssetType.STOCKS_CRYPTO -> "Stocks & Crypto"
                                AssetType.CASH_SAVINGS -> "Cash Savings"
                                AssetType.GENERAL_ASSET -> "Asset Holding"
                            }

                            SelectableFilterChip(
                                selected = selectedType == type,
                                label = typeLabel,
                                onClick = { selectedType = type }
                            )
                        }
                    }
                }

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
                                onAssetSaved(asset?.id, name.trim(), selectedType, bal, yield)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (asset != null) "Update Asset" else "Add Asset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
