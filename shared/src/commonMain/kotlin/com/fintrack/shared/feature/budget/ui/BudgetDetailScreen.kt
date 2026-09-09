package com.fintrack.shared.feature.budget.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.fintrack.shared.feature.navigation.ui.LocalCurrency
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.fintrack.shared.ui.theme.accountChipBorder
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.ui.AccountsViewModel
import com.fintrack.shared.feature.core.data.model.ApiException
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage
import com.fintrack.shared.feature.core.ui.FinanceAmountHeader
import com.fintrack.shared.feature.core.ui.FinanceCategorySelection
import com.fintrack.shared.feature.core.ui.FinanceInputSection
import com.fintrack.shared.feature.core.ui.FinanceNumpad
import com.fintrack.shared.feature.core.ui.FinanceSaveButton
import com.fintrack.shared.feature.core.ui.FinanceTypeSection
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.fintrack.shared.feature.core.ui.KMPBackHandler
import com.fintrack.shared.feature.core.ui.MaterialToast
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.ConfirmationDialog
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatAsShortDateWithYear
import com.fintrack.shared.feature.core.ui.FintrackDatePickerDialog
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import androidx.compose.ui.platform.LocalFocusManager
import com.fintrack.shared.feature.transaction.ui.home.components.AccountIcon
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BudgetDetailScreen(
    budgetId: String?,
    onGlobalRefresh: () -> Unit,
    onShowToast: (String, Boolean) -> Unit,
    viewModel: BudgetViewModel = koinViewModel(),
    accountsViewModel: AccountsViewModel = koinViewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No SharedTransitionScope found")

    val selectedBudgetResult by viewModel.selectedBudget.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val deleteResult by viewModel.deleteResult.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()
    val validationState by viewModel.validationState.collectAsStateWithLifecycle()
    val validationError by viewModel.validationError.collectAsStateWithLifecycle()
    val accountsResult by accountsViewModel.accounts.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    var showNumpad by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    KMPBackHandler(enabled = showNumpad) {
        showNumpad = false
    }

    LaunchedEffect(showNumpad) {
        if (showNumpad) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        if (accountsResult !is Result.Success || (accountsResult as Result.Success).data.isEmpty()) {
            accountsViewModel.reloadAccounts()
        }
    }

    LaunchedEffect(budgetId, selectedBudgetResult, accountsResult, allCategories) {
        if (accountsResult is Result.Success) {
            val accountsData = (accountsResult as Result.Success<List<Account>>).data
            viewModel.initializeForm(budgetId, accountsData)
        }
    }

    LaunchedEffect(budgetId) {
        viewModel.resetDeleteResult()
        budgetId?.let { viewModel.loadBudgetById(it) }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                onGlobalRefresh()
                delay(1200)
                onSave()
                viewModel.resetSaveState()
            }
            is SaveState.Error -> {
                val message = (saveState as SaveState.Error).exception.let {
                    (it as? ApiException)?.getUserFriendlyMessage() ?: it.message ?: "Failed to save budget"
                }
                onShowToast(message, true)
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    LaunchedEffect(deleteResult) {
        if (deleteResult is Result.Success) {
            onGlobalRefresh()
            onBack()
        }
    }

    val themeColor by animateColorAsState(
        targetValue = if (formState.isExpense) PinkExpense else GreenIncome,
        animationSpec = tween(durationMillis = 500)
    )

    // Animate the bottom padding to "follow" the bottom bar sliding in/out
    val bottomBarHeight = 80.dp
    val animatedBottomPadding by animatedVisibilityScope.transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "SaveButtonPadding"
    ) { state ->
        if (state == EnterExitState.Visible) 0.dp else bottomBarHeight
    }

    LaunchedEffect(validationError) {
        validationError?.let {
            onShowToast(it, true)
            viewModel.clearValidationError()
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            budgetId != null && selectedBudgetResult is Result.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = themeColor)
                }
            }

            budgetId != null && selectedBudgetResult is Result.Error -> {
                CommonErrorState(
                    title = "Failed to load budget",
                    error = (selectedBudgetResult as Result.Error).exception,
                    onRetry = { budgetId.let { viewModel.loadBudgetById(it) } }
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    with(sharedTransitionScope) {
                        FinanceAmountHeader(
                            amount = formState.amount,
                            selectionStart = formState.amountSelectionStart,
                            selectionEnd = formState.amountSelectionEnd,
                            onSelectionChange = { start, end -> viewModel.onAmountSelectionChange(start, end) },
                            label = if (formState.isExpense) "Expense Budget Limit" else "Income Target Limit",
                            isIncome = !formState.isExpense,
                            themeColor = themeColor,
                            paddingValues = paddingValues,
                            isActive = showNumpad,
                            onToggleNumpad = { showNumpad = it },
                            modifier = Modifier.sharedBounds(
                                rememberSharedContentState(key = "budget_header_${budgetId ?: "new"}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                },
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                            )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        FinanceTypeSection(
                            isIncome = !formState.isExpense,
                            onTypeChange = { viewModel.setIsExpense(!it) }
                        )

                        FinanceInputSection(
                            label = "Budget Name",
                            value = formState.name,
                            onValueChange = { viewModel.setName(it) },
                            placeholder = "Enter budget name",
                            icon = Icons.Default.Edit,
                            onFocus = { showNumpad = false }
                        )

                        MultiAccountSelectionSection(
                            accountsResult = accountsResult,
                            selectedAccounts = formState.selectedAccounts,
                            onAccountToggle = { viewModel.toggleAccount(it) },
                            onRetry = { accountsViewModel.reloadAccounts() }
                        )

                        FinanceCategorySelection(
                            label = "Categories",
                            categories = allCategories,
                            selectedCategories = formState.selectedCategories,
                            onCategorySelectionChange = { viewModel.setCategories(it) },
                            isExpense = formState.isExpense,
                            multiSelect = true
                        )

                        PeriodSelectionSection(
                            startDate = formState.startDate,
                            endDate = formState.endDate,
                            onPeriodChange = { start, end -> viewModel.setPeriod(start, end) }
                        )

                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = animatedBottomPadding)
                .padding(20.dp)
        ) {
            AnimatedVisibility(
                visible = animatedVisibilityScope.transition.targetState == EnterExitState.Visible,
                enter = fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FinanceSaveButton(
                    saveState = saveState,
                    isFormValid = validationState is ValidationResult.Success,
                    themeColor = themeColor,
                    contentColor = if (formState.isExpense) Color.White else MaterialTheme.colorScheme.onTertiary,
                    onSaveClick = { 
                        showNumpad = false
                        focusManager.clearFocus()
                        viewModel.saveBudget() 
                    },
                    label = "Save Budget",
                    successLabel = "Saved"
                )
            }
        }

        // Delete Button (Top Right)
        if (budgetId != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), CircleShape)
                ) {
                    if (deleteResult is Result.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Budget",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                title = "Delete Budget?",
                message = "Are you sure you want to delete this budget? This action cannot be undone and you will lose all tracking for this period.",
                confirmLabel = "Delete",
                isDestructive = true,
                onConfirm = { budgetId?.let { viewModel.removeBudget(it) } },
                onDismiss = { showDeleteDialog = false }
            )
        }

        // Custom Numpad
        AnimatedVisibility(
            visible = showNumpad,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FinanceNumpad(
                onNumberClick = { num ->
                    viewModel.handleAmountInput(num)
                },
                onBackspaceClick = {
                    viewModel.handleAmountBackspace()
                },
                onDoneClick = { showNumpad = false }
            )
        }
    }
}

@Composable
fun AccountSelectionSection(
    accountsResult: Result<List<Account>>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedAccountId by remember(selectedAccount) { mutableStateOf(selectedAccount?.id) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Select Account",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        when (accountsResult) {
            is Result.Loading -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is Result.Error -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Failed to load accounts",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accountChipBorder
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is Result.Success -> {
                if (accountsResult.data.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            Text(
                                text = "No accounts available. Create an account first.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(accountsResult.data) { account ->
                            AccountChip(
                                account = account,
                                isSelected = selectedAccountId == account.id,
                                onClick = { onAccountSelected(account) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiAccountSelectionSection(
    accountsResult: Result<List<Account>>,
    selectedAccounts: Set<Account>,
    onAccountToggle: (Account) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Accounts",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            
            Text(
                text = "${selectedAccounts.size} selected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (accountsResult) {
            is Result.Loading -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is Result.Error -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Failed to load accounts",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accountChipBorder
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is Result.Success -> {
                if (accountsResult.data.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            Text(
                                text = "No accounts available. Create an account first.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(accountsResult.data) { account ->
                            AccountChip(
                                account = account,
                                isSelected = selectedAccounts.any { it.id == account.id },
                                onClick = { onAccountToggle(account) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountChip(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountIcon = AccountIcon.fromAccountType(account.type, account.name)

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300)
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        animationSpec = tween(300)
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(300)
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { onClick() }
            .widthIn(min = 120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(
            width = 2.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = accountIcon.icon,
                contentDescription = account.name,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = account.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = account.balance?.toCurrencyString() ?: "${LocalCurrency.current.symbol} --",
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PeriodSelectionSection(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onPeriodChange: (LocalDate?, LocalDate?) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Budget Period", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartPicker = true },
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Start Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = startDate?.formatAsShortDateWithYear() ?: "Select",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Pick Start Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndPicker = true },
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("End Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = endDate?.formatAsShortDateWithYear() ?: "Select",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Pick End Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        FintrackDatePickerDialog(
            initialDate = startDate ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
            onDateSelected = {
                onPeriodChange(it, endDate)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        FintrackDatePickerDialog(
            initialDate = endDate ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.plus(DatePeriod(months = 1)),
            onDateSelected = {
                onPeriodChange(startDate, it)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}
