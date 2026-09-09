package com.fintrack.shared.feature.account.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.core.data.model.ApiException
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.ConfirmationDialog
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.toRelativeString
import com.fintrack.shared.feature.navigation.ui.LocalBiometricAuthenticator
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.core.ui.biometric.BiometricResult
import com.fintrack.shared.feature.settings.ui.SettingsViewModel
import com.fintrack.shared.feature.transaction.ui.home.components.AccountIcon
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    refreshTrigger: Int,
    onGlobalRefresh: () -> Unit,
    onShowToast: (String, Boolean) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    accountsViewModel: AccountsViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val accountsState by accountsViewModel.accounts.collectAsStateWithLifecycle()
    val deleteResult by accountsViewModel.deleteResult.collectAsStateWithLifecycle()
    val saveResult by accountsViewModel.saveResult.collectAsStateWithLifecycle()
    val clearDataResult by accountsViewModel.clearDataResult.collectAsStateWithLifecycle()

    val biometricAuthenticator = LocalBiometricAuthenticator.current
    val scope = rememberCoroutineScope()

    var showAccountDialog by remember { mutableStateOf<Account?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    val isOperating =
        (deleteResult is Result.Loading) || (saveResult is Result.Loading) || (clearDataResult is Result.Loading)

    LaunchedEffect(Unit) {
        accountsViewModel.reloadAccounts(showLoading = true)
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            accountsViewModel.reloadAccounts(showLoading = false)
        }
    }

    var pendingDefaultAccountId by remember { mutableStateOf<String?>(null) }
    var pendingMpesaLinked by remember { mutableStateOf(false) }
    var pendingEquityLinked by remember { mutableStateOf(false) }

    LaunchedEffect(saveResult) {
        val result = saveResult
        if (result is Result.Success) {
            onShowToast(if (isEditing) "Account updated" else "Account added", false)

            val accountId = result.data.id

            // Handle pending local settings for new accounts
            if (pendingDefaultAccountId == "NEW_ACCOUNT_PENDING") {
                settingsViewModel.setDefaultAccountId(accountId)
                pendingDefaultAccountId = null
            }

            if (pendingMpesaLinked) {
                val currentIds = settingsViewModel.mpesaLinkedAccountIds.value
                settingsViewModel.setMpesaLinkedAccountIds(currentIds + accountId)
                pendingMpesaLinked = false
            }

            if (pendingEquityLinked) {
                val currentIds = settingsViewModel.equityLinkedAccountIds.value
                settingsViewModel.setEquityLinkedAccountIds(currentIds + accountId)
                pendingEquityLinked = false
            }
        }
    }

    LaunchedEffect(saveResult, deleteResult, clearDataResult) {
        if (saveResult is Result.Success || deleteResult is Result.Success || clearDataResult is Result.Success) {
            onGlobalRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = accountsState) {
                is Result.Loading -> {
                    AccountLoadingShimmer(
                        topPadding = paddingValues.calculateTopPadding() + 12.dp,
                        bottomPadding = paddingValues.calculateBottomPadding() + 32.dp,
                    )
                }

                is Result.Success -> {
                    val defaultAccountId by settingsViewModel.defaultAccountId.collectAsStateWithLifecycle()
                    val mpesaLinkedAccountIds by settingsViewModel.mpesaLinkedAccountIds.collectAsStateWithLifecycle()
                    val equityLinkedAccountIds by settingsViewModel.equityLinkedAccountIds.collectAsStateWithLifecycle()

                    val effectiveDefaultAccountId =
                        defaultAccountId ?: state.data.find { it.type == AccountType.MPESA }?.id

                    val enrichedAccounts = remember(
                        state.data,
                        effectiveDefaultAccountId,
                        mpesaLinkedAccountIds,
                        equityLinkedAccountIds
                    ) {
                        state.data.map { account ->
                            val sources = mutableListOf<String>()
                            if (mpesaLinkedAccountIds.contains(account.id)) sources.add("mpesa")
                            if (equityLinkedAccountIds.contains(account.id)) sources.add("equity")
                            account.copy(
                                linkedSources = sources
                            )
                        }.sortedBy { it.createdAt }
                    }

                    AccountList(
                        accounts = enrichedAccounts,
                        defaultAccountId = effectiveDefaultAccountId,
                        topPadding = paddingValues.calculateTopPadding() + 12.dp,
                        bottomPadding = paddingValues.calculateBottomPadding() + 32.dp,
                        onEditAccount = {
                            if (!isOperating) {
                                accountsViewModel.clearResults()
                                showAccountDialog = it
                                isEditing = true
                            }
                        },
                        onAddAccount = {
                            if (!isOperating) {
                                accountsViewModel.clearResults()
                                showAccountDialog = Account(id = "", name = "")
                                isEditing = false
                            }
                        }
                    )
                }

                is Result.Error -> {
                    CommonErrorState(
                        modifier = Modifier.fillMaxSize(),
                        title = "Failed to load accounts",
                        error = state.exception,
                        onRetry = { accountsViewModel.reloadAccounts() }
                    )
                }
            }
        }
    }

    showAccountDialog?.let { account ->
        val defaultAccountId by settingsViewModel.defaultAccountId.collectAsStateWithLifecycle()
        val mpesaLinkedAccountIds by settingsViewModel.mpesaLinkedAccountIds.collectAsStateWithLifecycle()
        val equityLinkedAccountIds by settingsViewModel.equityLinkedAccountIds.collectAsStateWithLifecycle()

        val accounts = (accountsState as? Result.Success)?.data ?: emptyList()
        val isOnlyAccount =
            accounts.size <= 1 || (accounts.size == 1 && accounts.first().id == account.id)

        // Enrich the dialog account with local settings
        val enrichedAccount = remember(account, mpesaLinkedAccountIds, equityLinkedAccountIds) {
            val sources = mutableListOf<String>()
            if (mpesaLinkedAccountIds.contains(account.id)) sources.add("mpesa")
            if (equityLinkedAccountIds.contains(account.id)) sources.add("equity")
            account.copy(linkedSources = sources)
        }

        AccountDialog(
            account = enrichedAccount,
            isEditing = isEditing,
            isLoading = saveResult is Result.Loading ||
                    deleteResult is Result.Loading ||
                    clearDataResult is Result.Loading,
            saveResult = saveResult,
            deleteResult = deleteResult,
            clearDataResult = clearDataResult,
            accountType = account.type,
            isDefaultSelection = account.id == defaultAccountId,
            isOnlyAccount = isOnlyAccount,
            onDismiss = {
                if (!isOperating) {
                    showAccountDialog = null
                    accountsViewModel.clearResults()
                }
            },
            onDelete = {
                scope.launch {
                    val authResult = biometricAuthenticator.authenticate(
                        title = "Delete Account",
                        subtitle = "Confirm your identity to delete this account"
                    )
                    if (authResult is BiometricResult.Success || authResult is BiometricResult.NotAvailable) {
                        // Also clear local settings on delete
                        val accountId = account.id
                        settingsViewModel.setMpesaLinkedAccountIds(settingsViewModel.mpesaLinkedAccountIds.value - accountId)
                        settingsViewModel.setEquityLinkedAccountIds(settingsViewModel.equityLinkedAccountIds.value - accountId)
                        if (defaultAccountId == accountId) {
                            settingsViewModel.setDefaultAccountId(null)
                        }
                        accountsViewModel.removeAccount(accountId)
                    }
                }
            },
            onClearData = {
                scope.launch {
                    val authResult = biometricAuthenticator.authenticate(
                        title = "Clear Account Data",
                        subtitle = "Confirm your identity to delete all transactions for this account"
                    )
                    if (authResult is BiometricResult.Success || authResult is BiometricResult.NotAvailable) {
                        accountsViewModel.clearAccountData(account.id)
                    }
                }
            },
            onClearResults = { accountsViewModel.clearResults() },
            onConfirm = { name, type, sources, isDefault ->
                if (account.id.isNotEmpty()) {
                    // Update local settings for existing account
                    val accountId = account.id

                    // Default Account
                    if (isDefault) {
                        settingsViewModel.setDefaultAccountId(accountId)
                    } else if (accountId == defaultAccountId) {
                        settingsViewModel.setDefaultAccountId(null)
                    }

                    // Sync Links
                    val mpesaIds = settingsViewModel.mpesaLinkedAccountIds.value
                    if (sources.contains("mpesa")) {
                        settingsViewModel.setMpesaLinkedAccountIds(mpesaIds + accountId)
                    } else {
                        settingsViewModel.setMpesaLinkedAccountIds(mpesaIds - accountId)
                    }

                    val equityIds = settingsViewModel.equityLinkedAccountIds.value
                    if (sources.contains("equity")) {
                        settingsViewModel.setEquityLinkedAccountIds(equityIds + accountId)
                    } else {
                        settingsViewModel.setEquityLinkedAccountIds(equityIds - accountId)
                    }
                } else {
                    // For new accounts, set pending flags
                    if (isDefault) pendingDefaultAccountId = "NEW_ACCOUNT_PENDING"
                    if (sources.contains("mpesa")) pendingMpesaLinked = true
                    if (sources.contains("equity")) pendingEquityLinked = true
                }

                // Save to backend WITHOUT local preferences
                accountsViewModel.saveAccount(
                    account.copy(
                        name = name,
                        type = type,
                        isDefault = account.isDefault, // Preserve original isDefault status
                        linkedSources = emptyList() // Backend doesn't need to know
                    )
                )
            }
        )
    }
}

@Composable
fun AccountLoadingShimmer(
    topPadding: Dp,
    bottomPadding: Dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = topPadding,
            end = 16.dp,
            bottom = bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp) // Match NetWorthHeader height
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                AnimatedShimmerBox(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        item(span = { GridItemSpan(2) }) {
            AnimatedShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        items(6) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedShimmerBox(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun AccountList(
    accounts: List<Account>,
    defaultAccountId: String?,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    onEditAccount: (Account) -> Unit,
    onAddAccount: () -> Unit
) {
    val totalBalance = remember(accounts) {
        accounts.fold(BigDecimal.ZERO) { acc, account ->
            acc + (account.balance ?: BigDecimal.ZERO)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            NetWorthHeader(totalBalance)
        }

        item(span = { GridItemSpan(2) }) {
            Text(
                text = "MY ACCOUNTS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
            )
        }

        items(
            items = accounts,
            key = { it.id }
        ) { account ->
            AccountItem(
                account = account,
                isStartAccount = account.id == defaultAccountId,
                onEdit = { onEditAccount(account) }
            )
        }
        item(key = "add_account") {
            AddAccountItem(onClick = onAddAccount)
        }
    }
}

@Composable
private fun NetWorthHeader(totalBalance: BigDecimal) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TOTAL BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = totalBalance.toCurrencyString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun AddAccountItem(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth().height(115.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Add Account",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AccountItem(
    account: Account,
    isStartAccount: Boolean,
    onEdit: () -> Unit
) {
    val accountIcon = AccountIcon.fromAccountType(account.type, account.name)

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth().height(115.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accountIcon.color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = accountIcon.icon,
                        contentDescription = null,
                        tint = accountIcon.color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isStartAccount) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Default",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = (account.balance ?: BigDecimal.ZERO).toCurrencyString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val isSyncEnabled = account.linkedSources.contains("mpesa") || account.linkedSources.contains("equity")
            if (isSyncEnabled || account.lastSyncedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = account.lastSyncedAt?.let { "Synced ${it.toRelativeString()}" } ?: "Never synced",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    minLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDialog(
    account: Account,
    isEditing: Boolean,
    isLoading: Boolean,
    saveResult: Result<Account>?,
    deleteResult: Result<Unit>?,
    clearDataResult: Result<Unit>?,
    accountType: AccountType,
    isDefaultSelection: Boolean,
    isOnlyAccount: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onClearData: () -> Unit,
    onClearResults: () -> Unit,
    onConfirm: (String, AccountType, List<String>, Boolean) -> Unit
) {
    var accountName by remember(account.id) { mutableStateOf(account.name) }
    var type by remember(account.id) { mutableStateOf(accountType) }
    var linkedSources by remember(account.id) { mutableStateOf(account.linkedSources.toSet()) }
    var isDefault by remember(account.id) {
        mutableStateOf(isDefaultSelection || isOnlyAccount)
    }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Logic to lock the default toggle: 
    // 1. If it's already the default, it's locked (must change it from another account)
    // 2. If it's the only account
    // 3. If it's M-Pesa and no other account is default (fallback)
    val isDefaultLocked = isDefaultSelection || isOnlyAccount

    // Auto-detect options based on name for new accounts
    LaunchedEffect(accountName) {
        if (!isEditing && account.id.isEmpty()) {
            val lowerName = accountName.lowercase()
            when {
                lowerName.contains("mpesa") -> {
                    if (!linkedSources.contains("mpesa")) linkedSources = linkedSources + "mpesa"
                    type = AccountType.MPESA
                }

                lowerName.contains("equity") -> {
                    if (!linkedSources.contains("equity")) linkedSources = linkedSources + "equity"
                    type = AccountType.BANK
                }

                lowerName.contains("cash") -> type = AccountType.CASH
                lowerName.contains("wallet") -> type = AccountType.WALLET
                lowerName.contains("savings") -> type = AccountType.SAVINGS
                lowerName.contains("bank") -> type = AccountType.BANK
                lowerName.contains("checking") -> type = AccountType.BANK
            }
        }
    }

    // Update default state when isOnlyAccount changes
    LaunchedEffect(isOnlyAccount) {
        if (isOnlyAccount) {
            isDefault = true
        }
    }

    val isEffectivelyLoading = isLoading || saveResult is Result.Success

    val hasChanges = accountName != account.name ||
            type != accountType ||
            isDefault != isDefaultSelection ||
            linkedSources != account.linkedSources.toSet()

    val saveError = (saveResult as? Result.Error)?.let {
        (it.exception as? ApiException)?.getUserFriendlyMessage()
            ?: it.exception.message
            ?: "An error occurred"
    }

    LaunchedEffect(saveResult) {
        if (saveResult is Result.Success) {
            onDismiss()
        }
    }

    if (showClearDataConfirm) {
        ConfirmationDialog(
            title = "Clear Account Data",
            message = "Are you sure you want to delete all transactions and budgets for '${account.name}'? This action cannot be undone.",
            confirmLabel = "Clear Data",
            isDestructive = true,
            autoDismiss = false,
            isLoading = clearDataResult is Result.Loading,
            isSuccess = clearDataResult is Result.Success,
            errorMessage = (clearDataResult as? Result.Error)?.exception?.message,
            successTitle = "Data Cleared",
            successMessage = "All transactions and budgets for '${account.name}' have been removed.",
            onConfirm = onClearData,
            onDismiss = {
                showClearDataConfirm = false
                onClearResults()
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Account",
            message = "Are you sure you want to permanently delete '${account.name}' and all its associated data?",
            confirmLabel = "Delete Account",
            isDestructive = true,
            autoDismiss = false,
            isLoading = deleteResult is Result.Loading,
            isSuccess = deleteResult is Result.Success,
            errorMessage = (deleteResult as? Result.Error)?.exception?.message,
            successTitle = "Account Deleted",
            successMessage = "The account '${account.name}' has been successfully removed.",
            onConfirm = onDelete,
            onDismiss = {
                showDeleteConfirm = false
                if (deleteResult is Result.Success) {
                    onDismiss() // Close the AccountDialog as well
                }
                onClearResults()
            }
        )
    }

    BasicAlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 420.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Account" else "Add New Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isEditing && !account.isDefault) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Account",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = accountName,
                    onValueChange = {
                        accountName = it
                        if (saveResult is Result.Error) onClearResults()
                    },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. Personal Savings") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !account.isDefault,
                    readOnly = isLoading,
                    isError = saveError != null,
                    supportingText = saveError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (account.isDefault) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.38f
                            )
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "VISUAL STYLE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AccountType.entries) { accountType ->
                            val icon = AccountIcon.fromAccountType(accountType, "")
                            FilterChip(
                                selected = type == accountType,
                                onClick = { type = accountType },
                                label = {
                                    Text(
                                        text = accountType.name.lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (type == accountType) icon.color else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = icon.color.copy(alpha = 0.15f),
                                    selectedLabelColor = icon.color,
                                    selectedLeadingIconColor = icon.color
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = type == accountType,
                                    selectedBorderColor = icon.color.copy(alpha = 0.5f),
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ACCOUNT OPTIONS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
                            AccountOptionRow(
                                title = "M-Pesa SMS Link",
                                subtitle = "Auto-track transactions",
                                icon = Icons.Default.Smartphone,
                                checked = linkedSources.contains("mpesa"),
                                onCheckedChange = { checked ->
                                    linkedSources =
                                        if (checked) linkedSources + "mpesa" else linkedSources - "mpesa"
                                },
                                enabled = true,
                                isBusy = isEffectivelyLoading
                            )

                            AccountOptionRow(
                                title = "Equity Bank SMS Link",
                                subtitle = "Auto-track bank transactions",
                                icon = Icons.Default.AccountBalance,
                                checked = linkedSources.contains("equity"),
                                onCheckedChange = { checked ->
                                    linkedSources =
                                        if (checked) linkedSources + "equity" else linkedSources - "equity"
                                },
                                enabled = true,
                                isBusy = isEffectivelyLoading
                            )

                            val lockedSubtitle = when {
                                isOnlyAccount -> "The only account must be default"
                                isDefaultSelection -> "To change default, select another account"
                                else -> "Loads this account first"
                            }

                            AccountOptionRow(
                                title = "Set as Default",
                                subtitle = lockedSubtitle,
                                icon = Icons.Default.Star,
                                checked = isDefault,
                                onCheckedChange = { isDefault = it },
                                enabled = !isDefaultLocked,
                                isBusy = isEffectivelyLoading
                            )

                            if (isEditing) {
                                AccountActionRow(
                                    title = "Clear Data",
                                    subtitle = "Delete all transactions",
                                    icon = Icons.Default.Delete,
                                    onClick = { showClearDataConfirm = true },
                                    enabled = !isEffectivelyLoading,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isEffectivelyLoading
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (accountName.isNotBlank()) onConfirm(
                                accountName,
                                type,
                                linkedSources.toList(),
                                isDefault
                            )
                        },
                        enabled = accountName.isNotBlank() && !isEffectivelyLoading && (hasChanges || !isEditing),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (isEditing) "Save Changes" else "Create Account",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = tint.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AccountOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    isBusy: Boolean = false
) {
    // Optimistic update state to prevent flicker
    var internalChecked by remember(checked) { mutableStateOf(checked) }

    fun handleToggle(newValue: Boolean) {
        if (enabled && !isBusy && newValue != internalChecked) {
            internalChecked = newValue
            onCheckedChange(newValue)
        }
    }

    val contentAlpha = if (enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled && !isBusy) { handleToggle(!internalChecked) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (internalChecked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = contentAlpha)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = contentAlpha)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (internalChecked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                },
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }

        Switch(
            checked = internalChecked,
            onCheckedChange = { handleToggle(it) },
            enabled = enabled,
            modifier = Modifier.scale(0.8f)
        )
    }
}
