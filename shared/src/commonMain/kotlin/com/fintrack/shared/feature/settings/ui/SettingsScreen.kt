package com.fintrack.shared.feature.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.auth.ui.common.FinanceTextField
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.ui.util.toColor
import com.fintrack.shared.feature.category.ui.util.toIcon
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.ui.ConfirmationDialog
import com.fintrack.shared.feature.core.ui.FintrackDatePickerDialog
import com.fintrack.shared.feature.core.ui.FintrackTimePickerDialog
import com.fintrack.shared.feature.core.ui.biometric.BiometricResult
import com.fintrack.shared.feature.core.ui.permission.NotificationPermissionLauncher
import com.fintrack.shared.feature.core.ui.permission.PermissionRationaleDialog
import com.fintrack.shared.feature.core.ui.permission.SmsPermissionLauncher
import com.fintrack.shared.feature.core.ui.permission.rememberSmsPermissionState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.LocalBiometricAuthenticator
import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.settings.domain.model.Currency
import com.fintrack.shared.feature.settings.domain.model.ExportFormat
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import com.fintrack.shared.feature.settings.domain.util.format
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    refreshTrigger: Int,
    onGlobalRefresh: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val currentCurrency by viewModel.currency.collectAsStateWithLifecycle()
    val currentTheme by viewModel.theme.collectAsStateWithLifecycle()
    val currentTimeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val showDecimals by viewModel.showDecimals.collectAsStateWithLifecycle()
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsStateWithLifecycle()
    val isMpesaListenerEnabled by viewModel.isMpesaListenerEnabled.collectAsStateWithLifecycle()
    val isEquityListenerEnabled by viewModel.isEquityListenerEnabled.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val budgetAlertThresholds by viewModel.budgetAlertThresholds.collectAsStateWithLifecycle()
    val alertBudgetId by viewModel.alertBudgetId.collectAsStateWithLifecycle()
    val isBillReminderEnabled by viewModel.isBillReminderEnabled.collectAsStateWithLifecycle()
    val billReminderDaysBefore by viewModel.billReminderDaysBefore.collectAsStateWithLifecycle()
    val isDailySummaryEnabled by viewModel.isDailySummaryEnabled.collectAsStateWithLifecycle()
    val isWeeklySummaryEnabled by viewModel.isWeeklySummaryEnabled.collectAsStateWithLifecycle()
    val summaryNotificationTime by viewModel.summaryNotificationTime.collectAsStateWithLifecycle()
    val exportFormat by viewModel.exportFormat.collectAsStateWithLifecycle()
    val isSmsRationaleHidden by viewModel.isSmsRationaleHidden.collectAsStateWithLifecycle()
    val budgetsResult by viewModel.budgets.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    val showPermissionRequest by viewModel.showPermissionRequest.collectAsStateWithLifecycle()
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val exportStartDate by viewModel.exportStartDate.collectAsStateWithLifecycle()
    val exportEndDate by viewModel.exportEndDate.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val trackedCategoryIds by viewModel.trackedCategoryIds.collectAsStateWithLifecycle()
    val trackedCategoryNames by viewModel.trackedCategoryNames.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    val smsPermissionGranted = rememberSmsPermissionState()

    val changePasswordFormState by viewModel.changePasswordFormState.collectAsStateWithLifecycle()
    val changePasswordState by viewModel.changePasswordState.collectAsStateWithLifecycle()
    val deleteAccountState by viewModel.deleteAccountState.collectAsStateWithLifecycle()
    val seedState by viewModel.seedState.collectAsStateWithLifecycle()
    val seedProgress by viewModel.seedProgress.collectAsStateWithLifecycle()

    val biometricAuthenticator = LocalBiometricAuthenticator.current
    val scope = rememberCoroutineScope()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showTrackedCategoriesDialog by remember { mutableStateOf(false) }
    var showBudgetSelectionDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showSummaryTimePickerDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var showSmsPermissionRequest by remember { mutableStateOf<SmsPermissionTarget?>(null) }
    var showSmsRationale by remember { mutableStateOf<SmsPermissionTarget?>(null) }
    var showBillReminderDaysDialog by remember { mutableStateOf(false) }
    var showSeedConfirmation by remember { mutableStateOf(false) }

    var activeSmsTarget by remember { mutableStateOf<SmsPermissionTarget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.syncCategories()
        viewModel.loadAccounts()
        viewModel.reloadBudgets(force = false)
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.reloadBudgets(force = true, showLoading = false)
        }
    }

    LaunchedEffect(changePasswordState) {
        if (changePasswordState is SaveState.Success) {
            onShowToast("Password updated successfully", false)
            // Dismiss dialog first before resetting state
            showChangePasswordDialog = false
            viewModel.resetChangePasswordState()
        } else if (changePasswordState is SaveState.Error) {
            val exception = (changePasswordState as SaveState.Error).exception
            onShowToast(exception.message ?: "Update failed", true)
        }
    }

    LaunchedEffect(exportResult) {
        exportResult?.let { path ->
            onShowToast("Data exported to: $path", false)
            viewModel.clearExportResult()
        }
    }

    LaunchedEffect(error) {
        error?.let { message ->
            onShowToast(message, true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + paddingValues.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + paddingValues.calculateBottomPadding() + 24.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                item {
                    SettingsSection(title = "General") {
                        SettingsItem(
                            title = "Theme",
                            subtitle = when (currentTheme) {
                                AppTheme.LIGHT -> "Light"
                                AppTheme.DARK -> "Dark"
                                AppTheme.SYSTEM -> "System Default"
                            },
                            icon = Icons.Default.Palette,
                            onClick = { showThemeDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            title = "Currency",
                            subtitle = "${currentCurrency.name} (${currentCurrency.symbol})",
                            icon = Icons.Default.Payments,
                            onClick = { showCurrencyDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            title = "Time Format",
                            subtitle = if (currentTimeFormat == TimeFormat.TWELVE_HOUR) "12-hour" else "24-hour",
                            icon = Icons.Default.Schedule,
                            onClick = { showTimeFormatDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            title = "Tracked Categories",
                            subtitle = if (trackedCategoryNames.isEmpty()) "Automatic (Top Spending & Income)" else trackedCategoryNames.joinToString(
                                ", "
                            ),
                            description = "Visible in 'Category Comparison' on Home dashboard.",
                            icon = Icons.Default.Category,
                            onClick = { showTrackedCategoriesDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsToggleItem(
                            title = "Privacy Mode",
                            subtitle = "Hide balances and amounts",
                            icon = Icons.Default.VisibilityOff,
                            checked = isBalanceHidden,
                            onCheckedChange = {
                                viewModel.setBalanceHidden(it)
                                onGlobalRefresh()
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsToggleItem(
                            title = "Show Decimals",
                            subtitle = if (showDecimals) "Show cents (e.g., 0.00)" else "Clean whole numbers only",
                            icon = Icons.Default.Pin,
                            checked = showDecimals,
                            onCheckedChange = {
                                viewModel.setShowDecimals(it)
                                onGlobalRefresh()
                            }
                        )
                    }

                    SettingsSection(title = "M-Pesa Tracking") {
                        SettingsToggleItem(
                            title = "M-Pesa Auto-tracking",
                            subtitle = "Automatically log M-Pesa SMS",
                            icon = Icons.Default.Sms,
                            checked = isMpesaListenerEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (smsPermissionGranted) {
                                        viewModel.setMpesaListenerEnabled(true)
                                    } else if (isSmsRationaleHidden) {
                                        showSmsPermissionRequest = SmsPermissionTarget.MPESA
                                        activeSmsTarget = SmsPermissionTarget.MPESA
                                    } else {
                                        activeSmsTarget = SmsPermissionTarget.MPESA
                                        showSmsRationale = SmsPermissionTarget.MPESA
                                    }
                                } else {
                                    viewModel.setMpesaListenerEnabled(false)
                                }
                            }
                        )
                    }

                    SettingsSection(title = "Bank Tracking") {
                        SettingsToggleItem(
                            title = "Equity Auto-tracking",
                            subtitle = "Automatically log Equity Bank SMS",
                            icon = Icons.Default.AccountBalance,
                            checked = isEquityListenerEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (smsPermissionGranted) {
                                        viewModel.setEquityListenerEnabled(true)
                                    } else if (isSmsRationaleHidden) {
                                        showSmsPermissionRequest = SmsPermissionTarget.EQUITY
                                        activeSmsTarget = SmsPermissionTarget.EQUITY
                                    } else {
                                        activeSmsTarget = SmsPermissionTarget.EQUITY
                                        showSmsRationale = SmsPermissionTarget.EQUITY
                                    }
                                } else {
                                    viewModel.setEquityListenerEnabled(false)
                                }
                            }
                        )
                    }

                    SettingsSection(title = "Notifications") {
                        SettingsToggleItem(
                            title = "Daily Logging Nudge",
                            subtitle = "Reminder to log your transactions for the day",
                            icon = Icons.Default.Notifications,
                            checked = isReminderEnabled,
                            onCheckedChange = {
                                viewModel.setReminderEnabled(it)
                                onGlobalRefresh()
                            }
                        )

                        AnimatedVisibility(
                            visible = isReminderEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                SettingsItem(
                                    title = "Nudge Time",
                                    subtitle = reminderTime.format(currentTimeFormat),
                                    icon = Icons.Default.Schedule,
                                    onClick = { showTimePickerDialog = true }
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsToggleItem(
                            title = "Threshold Alerts",
                            subtitle = "Notify when spending reaches 50%, 80%, or 100% of limit",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            checked = budgetAlertsEnabled,
                            onCheckedChange = {
                                viewModel.setBudgetAlertsEnabled(it)
                                onGlobalRefresh()
                            }
                        )

                        AnimatedVisibility(
                            visible = budgetAlertsEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                val budgets =
                                    (budgetsResult as? Result.Success)?.data ?: emptyList()
                                val selectedBudget = budgets.find { it.budget.id == alertBudgetId }

                                SettingsItem(
                                    title = "Monitored Budget",
                                    subtitle = selectedBudget?.budget?.name ?: "Select a budget",
                                    icon = Icons.Default.AccountBalanceWallet,
                                    onClick = { showBudgetSelectionDialog = true }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                SettingsItem(
                                    title = "Active Thresholds",
                                    subtitle = if (budgetAlertThresholds.isEmpty()) "None" else budgetAlertThresholds.sorted()
                                        .joinToString("% ") { it.toString() } + "%",
                                    icon = Icons.Default.NotificationsActive,
                                    onClick = { showThresholdDialog = true }
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsToggleItem(
                            title = "Smart Bill Reminders",
                            subtitle = "Get notified before recurring payments are due",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            checked = isBillReminderEnabled,
                            onCheckedChange = {
                                viewModel.setBillReminderEnabled(it)
                                onGlobalRefresh()
                            }
                        )

                        AnimatedVisibility(
                            visible = isBillReminderEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                SettingsItem(
                                    title = "Advance Notice",
                                    subtitle = "$billReminderDaysBefore days before due date",
                                    icon = Icons.Default.EventRepeat,
                                    onClick = { showBillReminderDaysDialog = true }
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsToggleItem(
                            title = "Daily Spending Summary",
                            subtitle = "Summary of yesterday's total spending",
                            icon = Icons.Default.Today,
                            checked = isDailySummaryEnabled,
                            onCheckedChange = { viewModel.setDailySummaryEnabled(it) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsToggleItem(
                            title = "Weekly Spending Summary",
                            subtitle = "Total spending for the past week (every Sunday)",
                            icon = Icons.Default.DateRange,
                            checked = isWeeklySummaryEnabled,
                            onCheckedChange = { viewModel.setWeeklySummaryEnabled(it) }
                        )

                        AnimatedVisibility(
                            visible = isDailySummaryEnabled || isWeeklySummaryEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                SettingsItem(
                                    title = "Notification Time",
                                    subtitle = summaryNotificationTime.format(currentTimeFormat),
                                    icon = Icons.Default.Schedule,
                                    onClick = { showSummaryTimePickerDialog = true }
                                )
                            }
                        }
                    }

                    SettingsSection(title = "Security") {
                        SettingsToggleItem(
                            title = "Biometric Lock",
                            subtitle = "Use FaceID or Fingerprint",
                            icon = Icons.Default.Fingerprint,
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    scope.launch {
                                        val result = biometricAuthenticator.authenticate(
                                            title = "Enable Biometric",
                                            subtitle = "Confirm your identity to enable biometric lock"
                                        )
                                        if (result is BiometricResult.Success) {
                                            viewModel.toggleBiometric(true)
                                        } else if (result is BiometricResult.Error) {
                                            viewModel.setError(result.message)
                                        }
                                    }
                                } else {
                                    viewModel.toggleBiometric(false)
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            title = "Change Password",
                            subtitle = "Update your account password",
                            icon = Icons.Default.Lock,
                            onClick = { showChangePasswordDialog = true }
                        )
                    }

                    SettingsSection(title = "Data & Backup") {
                        SettingsItem(
                            title = "Export Transactions",
                            subtitle = "Download data as ${exportFormat.name}",
                            icon = Icons.Default.FileDownload,
                            onClick = { showExportFormatDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            title = "Delete Account",
                            subtitle = "Permanently delete your account",
                            icon = Icons.Default.PersonRemove,
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }

                    SettingsSection(title = "Portfolio Demo") {
                        val seedSubtitle = when (val state = seedState) {
                            is SaveState.Loading -> "Seeding... ${(seedProgress * 100).toInt()}%"
                            is SaveState.Success -> "Successfully seeded to ${state.data}!"
                            is SaveState.Error -> "Failed to seed data"
                            else -> "Populate charts with 6 months of sample data"
                        }

                        val iconColor =
                            if (seedState is SaveState.Success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary

                        SettingsItem(
                            title = "Seed Dummy Data",
                            subtitle = seedSubtitle,
                            icon = if (seedState is SaveState.Success) Icons.Default.Check else Icons.Default.Refresh,
                            iconTint = iconColor,
                            iconContainerColor = iconColor.copy(alpha = 0.12f),
                            onClick = { showSeedConfirmation = true }
                        )
                    }

                    SettingsSection(title = "About") {
                        SettingsItem(
                            title = "Version",
                            subtitle = "1.0.0 (Stable)",
                            icon = Icons.Default.Info,
                            onClick = { }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            title = "Help & Support",
                            subtitle = "FAQ and contact us",
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            onClick = { }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = currentCurrency,
            onCurrencySelected = {
                viewModel.setCurrency(it)
                onGlobalRefresh()
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = {
                viewModel.setTheme(it)
                onGlobalRefresh()
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showTimePickerDialog) {
        FintrackTimePickerDialog(
            initialTime = reminderTime,
            timeFormat = currentTimeFormat,
            onTimeSelected = {
                viewModel.setReminderTime(it)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    if (showSummaryTimePickerDialog) {
        FintrackTimePickerDialog(
            initialTime = summaryNotificationTime,
            timeFormat = currentTimeFormat,
            onTimeSelected = {
                viewModel.setSummaryNotificationTime(it)
                showSummaryTimePickerDialog = false
            },
            onDismiss = { showSummaryTimePickerDialog = false }
        )
    }

    if (showTimeFormatDialog) {
        TimeFormatSelectionDialog(
            currentFormat = currentTimeFormat,
            onFormatSelected = {
                viewModel.setTimeFormat(it)
                onGlobalRefresh()
                showTimeFormatDialog = false
            },
            onDismiss = { showTimeFormatDialog = false }
        )
    }

    NotificationPermissionLauncher(
        trigger = showPermissionRequest,
        onResult = { viewModel.onPermissionResult(it) },
        onDismissTrigger = { viewModel.dismissPermissionRequest() }
    )

    if (showSmsRationale != null) {
        val targetName =
            if (showSmsRationale == SmsPermissionTarget.MPESA) "M-Pesa" else "Equity Bank"
        PermissionRationaleDialog(
            title = "Enable $targetName Tracking",
            message = "FinTrack needs to read your SMS messages to automatically detect and log transactions from $targetName. Your financial data is processed privately on your device.",
            icon = if (showSmsRationale == SmsPermissionTarget.MPESA) Icons.Default.Sms else Icons.Default.AccountBalance,
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    viewModel.setSmsRationaleHidden(true)
                }
                showSmsPermissionRequest = showSmsRationale
                showSmsRationale = null
            },
            onDismiss = { dontShowAgain ->
                if (dontShowAgain) {
                    viewModel.setSmsRationaleHidden(true)
                }
                showSmsRationale = null
                activeSmsTarget = null
            }
        )
    }

    SmsPermissionLauncher(
        trigger = showSmsPermissionRequest != null,
        onResult = { granted ->
            val target = activeSmsTarget
            if (granted && target != null) {
                when (target) {
                    SmsPermissionTarget.MPESA -> viewModel.setMpesaListenerEnabled(true)
                    SmsPermissionTarget.EQUITY -> viewModel.setEquityListenerEnabled(true)
                }
            } else if (!granted && target != null) {
                onShowToast(
                    "Permission denied. Enable SMS permissions in Phone Settings for automatic tracking.",
                    true
                )
            }
            activeSmsTarget = null
            showSmsPermissionRequest = null
        },
        onDismissTrigger = { showSmsPermissionRequest = null }
    )

    if (showExportFormatDialog) {
        ExportFormatSelectionDialog(
            currentFormat = exportFormat,
            startDate = exportStartDate,
            endDate = exportEndDate,
            onFormatSelected = { viewModel.setExportFormat(it) },
            onDateRangeSelected = { start, end -> viewModel.setExportDateRange(start, end) },
            onExport = {
                scope.launch {
                    val authResult = biometricAuthenticator.authenticate(
                        title = "Export Data",
                        subtitle = "Confirm your identity to export your transactions"
                    )

                    if (authResult is BiometricResult.Success || authResult is BiometricResult.NotAvailable) {
                        viewModel.exportTransactions()
                        showExportFormatDialog = false
                    } else if (authResult is BiometricResult.Error) {
                        viewModel.setError(authResult.message)
                    }
                }
            },
            onDismiss = { showExportFormatDialog = false }
        )
    }

    if (showDeleteAccountDialog) {
        ConfirmationDialog(
            title = "Delete Account",
            message = "Are you sure you want to delete your account? This will permanently remove all your data, including accounts, transactions, and budgets. This action cannot be undone.",
            confirmLabel = "Delete Forever",
            isDestructive = true,
            isLoading = deleteAccountState is SaveState.Loading,
            isSuccess = deleteAccountState is SaveState.Success,
            errorMessage = (deleteAccountState as? SaveState.Error)?.exception?.message,
            successTitle = "Account Deleted",
            successMessage = "Your account and all associated data have been permanently deleted.",
            autoDismiss = false,
            onConfirm = {
                scope.launch {
                    val authResult = biometricAuthenticator.authenticate(
                        title = "Delete Account",
                        subtitle = "This will permanently delete your account and all data"
                    )

                    when (authResult) {
                        is BiometricResult.Success, BiometricResult.NotAvailable -> {
                            viewModel.deleteAccount()
                        }

                        is BiometricResult.Error -> {
                            viewModel.setError(authResult.message)
                        }
                    }
                }
            },
            onDismiss = {
                showDeleteAccountDialog = false
                viewModel.resetDeleteAccountState()
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            formState = changePasswordFormState,
            saveState = changePasswordState,
            onCurrentPasswordChange = viewModel::updateCurrentPassword,
            onNewPasswordChange = viewModel::updateNewPassword,
            onConfirmPasswordChange = viewModel::updateConfirmPassword,
            onConfirm = viewModel::changePassword,
            onDismiss = {
                showChangePasswordDialog = false
                viewModel.resetChangePasswordState()
            },
            onClearError = { viewModel.resetChangePasswordState() }
        )
    }

    if (showTrackedCategoriesDialog) {
        TrackedCategoriesSelectionDialog(
            allCategories = allCategories,
            selectedCategoryIds = trackedCategoryIds,
            onCategoriesSelected = {
                viewModel.updateTrackedCategories(it) {
                    onGlobalRefresh()
                }
                showTrackedCategoriesDialog = false
            },
            onDismiss = { showTrackedCategoriesDialog = false }
        )
    }

    if (showBudgetSelectionDialog) {
        BudgetSelectionDialog(
            budgets = (budgetsResult as? Result.Success)?.data ?: emptyList(),
            selectedBudgetId = alertBudgetId,
            onBudgetSelected = {
                viewModel.setAlertBudgetId(it)
                showBudgetSelectionDialog = false
            },
            onDismiss = { showBudgetSelectionDialog = false }
        )
    }

    if (showThresholdDialog) {
        BudgetThresholdDialog(
            selectedThresholds = budgetAlertThresholds,
            onThresholdsChanged = {
                viewModel.setBudgetAlertThresholds(it)
            },
            onDismiss = { showThresholdDialog = false }
        )
    }

    if (showBillReminderDaysDialog) {
        BillReminderDaysDialog(
            currentDays = billReminderDaysBefore,
            onDaysSelected = {
                viewModel.setBillReminderDaysBefore(it)
                showBillReminderDaysDialog = false
            },
            onDismiss = { showBillReminderDaysDialog = false }
        )
    }

    if (showSeedConfirmation) {
        val seedMessage = if (seedState is SaveState.Loading) {
            "Seeding dummy transactions... ${(seedProgress * 100).toInt()}% complete. Please wait while we populate your charts."
        } else {
            "This will generate multiple transactions per day for the last 6 months to populate your charts with rich data. This helps in exploring the app's features and visualizations. Real data is not affected."
        }

        val successMessage = (seedState as? SaveState.Success<String>)?.data?.let { accountName ->
            "Dummy transactions have been successfully added to '$accountName'."
        } ?: "The dummy transactions have been successfully added to your account."

        ConfirmationDialog(
            title = "Seed Portfolio Data",
            message = seedMessage,
            confirmLabel = "Seed Now",
            isLoading = seedState is SaveState.Loading,
            isSuccess = seedState is SaveState.Success,
            errorMessage = (seedState as? SaveState.Error)?.exception?.message,
            successTitle = "Seeding Complete",
            successMessage = successMessage,
            autoDismiss = false,
            onConfirm = { viewModel.seedPortfolioData() },
            onDismiss = {
                showSeedConfirmation = false
                viewModel.resetSeedState()
                onGlobalRefresh()
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    formState: ChangePasswordFormState,
    saveState: SaveState<Unit>,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                FinanceTextField(
                    value = formState.currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = "Current Password",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    isPassword = true,
                    passwordVisible = currentPasswordVisible,
                    onPasswordToggle = { currentPasswordVisible = !currentPasswordVisible },
                    colorScheme = colorScheme,
                    isError = formState.currentPasswordError != null,
                    errorMessage = formState.currentPasswordError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                FinanceTextField(
                    value = formState.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = "New Password",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    isPassword = true,
                    passwordVisible = newPasswordVisible,
                    onPasswordToggle = { newPasswordVisible = !newPasswordVisible },
                    colorScheme = colorScheme,
                    isError = formState.newPasswordError != null,
                    errorMessage = formState.newPasswordError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                FinanceTextField(
                    value = formState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm New Password",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    isPassword = true,
                    passwordVisible = confirmPasswordVisible,
                    onPasswordToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                    colorScheme = colorScheme,
                    isError = formState.confirmPasswordError != null,
                    errorMessage = formState.confirmPasswordError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = saveState is SaveState.Idle,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (saveState is SaveState.Loading || saveState is SaveState.Success) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatSelectionDialog(
    currentFormat: ExportFormat,
    startDate: String?,
    endDate: String?,
    onFormatSelected: (ExportFormat) -> Unit,
    onDateRangeSelected: (String?, String?) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
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
                modifier = Modifier.padding(24.dp).animateContentSize()
            ) {
                Text(
                    text = "Export Transactions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Filter by date range (optional) and select your preferred format.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Date Range Section
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateRangeItem(
                        label = "From",
                        date = startDate,
                        modifier = Modifier.weight(1f),
                        onClick = { showStartPicker = true },
                        onClear = { onDateRangeSelected(null, endDate) }
                    )
                    DateRangeItem(
                        label = "To",
                        date = endDate,
                        modifier = Modifier.weight(1f),
                        onClick = { showEndPicker = true },
                        onClear = { onDateRangeSelected(startDate, null) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Format Selection Section
                Text(
                    text = "File Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { format ->
                        val isSelected = format == currentFormat

                        Surface(
                            onClick = { onFormatSelected(format) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = format.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onExport,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Now")
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        FintrackDatePickerDialog(
            initialDate = startDate?.let { LocalDate.parse(it) },
            onDateSelected = {
                onDateRangeSelected(it.toString(), endDate)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        FintrackDatePickerDialog(
            initialDate = endDate?.let { LocalDate.parse(it) },
            onDateSelected = {
                onDateRangeSelected(startDate, it.toString())
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
private fun DateRangeItem(
    label: String,
    date: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = date ?: "Any",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (date != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (date != null) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFormatSelectionDialog(
    currentFormat: TimeFormat,
    onFormatSelected: (TimeFormat) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = "Select Time Format",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeFormat.entries.forEach { format ->
                        val isSelected = format == currentFormat
                        val label = if (format == TimeFormat.TWELVE_HOUR) "12-hour" else "24-hour"

                        Surface(
                            onClick = { onFormatSelected(format) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = "Select Theme",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AppTheme.entries) { theme ->
                        val isSelected = theme == currentTheme

                        Surface(
                            onClick = { onThemeSelected(theme) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (themeLabel, themeIcon) = when (theme) {
                                        AppTheme.LIGHT -> "Light" to Icons.Default.Palette
                                        AppTheme.DARK -> "Dark" to Icons.Default.Palette
                                        AppTheme.SYSTEM -> "System Default" to Icons.Default.Palette
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = themeIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Text(
                                        text = themeLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionDialog(
    currentCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = "Select Currency",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Currency.entries) { currency ->
                        val isSelected = currency == currentCurrency

                        Surface(
                            onClick = { onCurrencySelected(currency) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currency.symbol,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = currency.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = currency.code,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedCategoriesSelectionDialog(
    allCategories: List<Category>,
    selectedCategoryIds: List<String>,
    onCategoriesSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val currentSelection = remember { selectedCategoryIds.toMutableStateList() }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
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
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tracked Categories",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pin up to 2 categories to your dashboard. Default: Top spending & income.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (currentSelection.size == 2)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = "${currentSelection.size}/2",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSelection.size == 2)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reset Button
                if (currentSelection.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { currentSelection.clear() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset to Automatic Tracking")
                    }
                }

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(allCategories, key = { it.id }) { category ->
                            val isSelected by remember(category.id) {
                                derivedStateOf { currentSelection.contains(category.id) }
                            }

                            val onToggle = remember(category.id) {
                                {
                                    if (currentSelection.contains(category.id)) {
                                        currentSelection.remove(category.id)
                                    } else if (currentSelection.size < 2) {
                                        currentSelection.add(category.id)
                                    }
                                    Unit
                                }
                            }

                            CategorySelectionItem(
                                category = category,
                                isSelected = isSelected,
                                onToggle = onToggle
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).padding(horizontal = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onCategoriesSelected(currentSelection.toList()) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text("Save Selection")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelectionItem(
    category: Category,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val icon = remember(category.iconName, category.name) { category.toIcon() }
    val baseColor = remember(category.id) { category.toColor() }

    val backgroundColor by animateColorAsState(
        if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(50),
        label = "itemBg"
    )
    val borderColor by animateColorAsState(
        if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(50),
        label = "itemBorder"
    )
    val borderWidth by animateDpAsState(
        if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(50),
        label = "borderWidth"
    )

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected)
                                baseColor.copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected)
                            baseColor
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    (fadeIn(tween(40)) + scaleIn(initialScale = 0.95f, animationSpec = tween(40)))
                        .togetherWith(
                            fadeOut(tween(40)) + scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(40)
                            )
                        )
                },
                label = "selectionIcon"
            ) { selected ->
                Icon(
                    imageVector = if (selected)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Unselected",
                    tint = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSelectionDialog(
    budgets: List<BudgetWithStatus>,
    selectedBudgetId: String?,
    onBudgetSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp).animateContentSize()
            ) {
                Text(
                    text = "Select Budget to Track",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (budgets.isEmpty()) {
                    Text(
                        text = "No budgets found. Create one in the Budgets screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(budgets) { item ->
                            val isSelected = item.budget.id == selectedBudgetId

                            Surface(
                                onClick = { onBudgetSelected(item.budget.id) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = item.budget.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.budget.categories.joinToString { it.name },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onBudgetSelected(null) }) {
                        Text("Clear")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetThresholdDialog(
    selectedThresholds: Set<Int>,
    onThresholdsChanged: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val thresholds = listOf(50, 80, 100)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = "Select Alert Thresholds",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    thresholds.forEach { threshold ->
                        val isSelected = selectedThresholds.contains(threshold)

                        Surface(
                            onClick = {
                                if (isSelected) {
                                    onThresholdsChanged(selectedThresholds - threshold)
                                } else {
                                    onThresholdsChanged(selectedThresholds + threshold)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$threshold%",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) {
                                            onThresholdsChanged(selectedThresholds + threshold)
                                        } else {
                                            onThresholdsChanged(selectedThresholds - threshold)
                                        }
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
                    Text("Done")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillReminderDaysDialog(
    currentDays: Int,
    onDaysSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(1, 2, 3, 5, 7, 14)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .widthIn(max = 400.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = "Select Advance Notice",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { days ->
                        val isSelected = days == currentDays
                        val label = when (days) {
                            1 -> "1 day before"
                            7 -> "1 week before"
                            14 -> "2 weeks before"
                            else -> "$days days before"
                        }

                        Surface(
                            onClick = { onDaysSelected(days) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

enum class SmsPermissionTarget {
    MPESA, EQUITY
}

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .animateContentSize()
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    iconTint: Color = MaterialTheme.colorScheme.primary,
    description: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                        lineHeight = 14.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else null
            )
        }
    }
}
