package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.AccountIcon
import com.fintrack.shared.feature.core.util.DateTimeUtils
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.ionspin.kotlin.bignum.decimal.BigDecimal

@Composable
fun CurrentBalanceCardWrapper(
    selectedAccountResult: Result<Account>,
    accountsResult: Result<List<Account>>,
    defaultAccountId: String? = null,
    isBalanceHidden: Boolean,
    isMpesaAutoSyncEnabled: Boolean,
    isEquityAutoSyncEnabled: Boolean,
    importState: Result<Unit>?,
    syncProgress: Float,
    onAccountSelected: (String) -> Unit,
    onToggleBalanceVisibility: (Boolean) -> Unit,
    onManualSync: () -> Unit,
    onSyncErrorClick: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }

    CurrentBalanceCard(
        selectedAccountResult = selectedAccountResult,
        isBalanceHidden = isBalanceHidden,
        isMpesaAutoSyncEnabled = isMpesaAutoSyncEnabled,
        isEquityAutoSyncEnabled = isEquityAutoSyncEnabled,
        importState = importState,
        syncProgress = syncProgress,
        onChangeAccountClicked = { showDialog = true },
        onToggleBalanceVisibility = onToggleBalanceVisibility,
        onManualSync = onManualSync,
        onSyncErrorClick = onSyncErrorClick,
        onRetry = onRetry
    )

    if (showDialog) {
        AccountSelectionDialog(
            accountsResult = accountsResult,
            selectedAccountId = (selectedAccountResult as? Result.Success)?.data?.id,
            defaultAccountId = defaultAccountId,
            onAccountSelected = { accountId ->
                onAccountSelected(accountId)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            onRetry = onRetry
        )
    }
}

@Composable
fun CurrentBalanceCard(
    selectedAccountResult: Result<Account>,
    isBalanceHidden: Boolean,
    isMpesaAutoSyncEnabled: Boolean,
    isEquityAutoSyncEnabled: Boolean,
    importState: Result<Unit>?,
    syncProgress: Float,
    onChangeAccountClicked: () -> Unit,
    onToggleBalanceVisibility: (Boolean) -> Unit,
    onManualSync: () -> Unit,
    onSyncErrorClick: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var lastAccount by remember { mutableStateOf<Account?>(null) }
    if (selectedAccountResult is Result.Success) {
        lastAccount = selectedAccountResult.data
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            LowerRightWavesBackground(modifier = Modifier.matchParentSize())

            AnimatedContent(
                targetState = selectedAccountResult,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(500))
                },
                label = "BalanceContent"
            ) { result ->
                when (result) {
                    is Result.Loading -> {
                        val currentData = lastAccount
                        if (currentData != null) {
                            CurrentBalanceSuccessState(
                                account = currentData,
                                isBalanceHidden = isBalanceHidden,
                                isMpesaLinked = currentData.linkedSources.contains("mpesa"),
                                isEquityLinked = currentData.linkedSources.contains("equity"),
                                isMpesaAutoSyncEnabled = isMpesaAutoSyncEnabled,
                                isEquityAutoSyncEnabled = isEquityAutoSyncEnabled,
                                importState = importState,
                                syncProgress = syncProgress,
                                onChangeAccountClicked = onChangeAccountClicked,
                                onToggleBalanceVisibility = onToggleBalanceVisibility,
                                onManualSync = onManualSync,
                                onSyncErrorClick = onSyncErrorClick
                            )
                        } else {
                            CurrentBalanceLoadingState()
                        }
                    }

                    is Result.Error -> {
                        CurrentBalanceErrorState(
                            error = result.exception,
                            onRetry = onRetry
                        )
                    }

                    is Result.Success -> {
                        CurrentBalanceSuccessState(
                            account = result.data,
                            isBalanceHidden = isBalanceHidden,
                            isMpesaLinked = result.data.linkedSources.contains("mpesa"),
                            isEquityLinked = result.data.linkedSources.contains("equity"),
                            isMpesaAutoSyncEnabled = isMpesaAutoSyncEnabled,
                            isEquityAutoSyncEnabled = isEquityAutoSyncEnabled,
                            importState = importState,
                            syncProgress = syncProgress,
                            onChangeAccountClicked = onChangeAccountClicked,
                            onToggleBalanceVisibility = onToggleBalanceVisibility,
                            onManualSync = onManualSync,
                            onSyncErrorClick = onSyncErrorClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentBalanceLoadingState() {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                    shimmerColors = shimmerColors
                )
                Spacer(modifier = Modifier.width(6.dp))
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    shimmerColors = shimmerColors
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp)),
            shimmerColors = shimmerColors
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedShimmerBox(
            modifier = Modifier
                .width(180.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp)),
            shimmerColors = shimmerColors
        )
    }
}

@Composable
private fun CurrentBalanceErrorState(
    error: Throwable,
    onRetry: () -> Unit
) {
    val displayMessage = remember(error) {
        error.getUserFriendlyMessage()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Failed to load account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrentBalanceSuccessState(
    account: Account,
    isBalanceHidden: Boolean,
    isMpesaLinked: Boolean,
    isEquityLinked: Boolean,
    isMpesaAutoSyncEnabled: Boolean,
    isEquityAutoSyncEnabled: Boolean,
    importState: Result<Unit>?,
    syncProgress: Float,
    onChangeAccountClicked: () -> Unit,
    onToggleBalanceVisibility: (Boolean) -> Unit,
    onManualSync: () -> Unit,
    onSyncErrorClick: (String) -> Unit
) {
    val balance = account.balance ?: BigDecimal.ZERO
    // Correctly determine if sync status should be shown based on account linkage
    val isLinkedAccount = isMpesaLinked || isEquityLinked
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AccountIcon.fromAccountType(account.type, account.name).icon,
                        contentDescription = "Bank",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLinkedAccount) {
                            val syncText = account.lastSyncedAt?.let { 
                                "Synced ${DateTimeUtils.toRelativeDateTimeString(it)}" 
                            } ?: "Never synced"
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isLinkedAccount,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))

                            AnimatedContent(
                                targetState = importState,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f) togetherWith
                                            fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f)
                                },
                                label = "syncStateAnimation"
                            ) { targetState ->
                                when (targetState) {
                                    is Result.Loading -> {
                                        val infiniteTransition =
                                            rememberInfiniteTransition(label = "syncRotation")
                                        val rotation by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "rotation"
                                        )

                                        Row(
                                            modifier = Modifier
                                                .height(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.onPrimary.copy(
                                                        alpha = 0.15f
                                                    )
                                                )
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Syncing...",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .rotate(rotation)
                                            )
                                            Text(
                                                text = "${(syncProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }

                                    is Result.Success -> {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Sync Complete",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    is Result.Error -> {
                                        val errorMessage =
                                            (targetState as Result.Error).exception.message
                                                ?: "Failed to sync transactions"
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        MaterialTheme.colorScheme.error.copy(
                                                            alpha = 0.2f
                                                        )
                                                    )
                                                    .clickable { onSyncErrorClick(errorMessage) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = "Sync Failed",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Surface(
                                                onClick = onManualSync,
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.onPrimary.copy(
                                                    alpha = 0.15f
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Sync,
                                                        contentDescription = "Retry Sync",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    null -> {
                                        // No manual sync icon shown by default, as auto-sync is handled on app open
                                    }
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { onToggleBalanceVisibility(!isBalanceHidden) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        AnimatedContent(
                            targetState = isBalanceHidden,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f) togetherWith
                                        fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f)
                            },
                            label = "balanceVisibilityAnimation"
                        ) { hidden ->
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (hidden) "Show Balance" else "Hide Balance",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = onChangeAccountClicked,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Switch",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
                Column {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = balance.toCurrencyString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionDialog(
    accountsResult: Result<List<Account>>,
    selectedAccountId: String?,
    defaultAccountId: String? = null,
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {}
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 280.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Select Account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    when (accountsResult) {
                        is Result.Loading -> {
                            AccountSelectionLoadingState()
                        }

                        is Result.Error -> {
                            AccountSelectionErrorState(
                                error = accountsResult.exception,
                                onRetry = onRetry
                            )
                        }

                        is Result.Success -> {
                            if (accountsResult.data.isEmpty()) {
                                AccountSelectionEmptyState()
                            } else {
                                AccountSelectionListState(
                                    accounts = accountsResult.data,
                                    selectedAccountId = selectedAccountId,
                                    defaultAccountId = defaultAccountId,
                                    onAccountSelected = { accountId ->
                                        onAccountSelected(accountId)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSelectionLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading accounts...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountSelectionErrorState(
    error: Throwable,
    onRetry: () -> Unit
) {
    CommonErrorState(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        title = "Failed to load accounts",
        error = error,
        onRetry = onRetry
    )
}

@Composable
private fun AccountSelectionEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = "No accounts",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No accounts available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountSelectionListState(
    accounts: List<Account>,
    selectedAccountId: String?,
    defaultAccountId: String? = null,
    onAccountSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items(accounts) { acc ->
            val isSelected = selectedAccountId == acc.id
            val isDefault = defaultAccountId == acc.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onAccountSelected(acc.id) }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountIcon.fromAccountType(acc.type, acc.name).icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            acc.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Default",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        (acc.balance ?: BigDecimal.ZERO).toCurrencyString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
