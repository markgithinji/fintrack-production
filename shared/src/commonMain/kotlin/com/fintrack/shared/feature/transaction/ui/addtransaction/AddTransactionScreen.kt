package com.fintrack.shared.feature.transaction.ui.addtransaction

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import androidx.compose.ui.platform.LocalFocusManager
import com.fintrack.shared.feature.account.ui.AccountsViewModel
import com.fintrack.shared.feature.core.ui.AccountSelectionSection
import com.fintrack.shared.feature.core.ui.AccountIcon
import com.fintrack.shared.feature.core.data.model.ApiException
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import com.fintrack.shared.feature.core.ui.*
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.formatAsShortDateWithYear
import com.fintrack.shared.feature.settings.domain.util.format
import com.fintrack.shared.feature.navigation.ui.LocalTimeFormat
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.core.ui.util.ThousandsSeparatorOffsetMapping
import com.fintrack.shared.feature.transaction.ui.TransactionViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AddTransactionScreen(
    transactionId: String? = null,
    onShowToast: (String, Boolean) -> Unit,
    transactionsViewModel: TransactionViewModel = koinViewModel(),
    accountsViewModel: AccountsViewModel = koinViewModel(),
    onGlobalRefresh: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit = {},
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No SharedTransitionScope found")

    val saveState by transactionsViewModel.saveState.collectAsStateWithLifecycle()
    val deleteResult by transactionsViewModel.deleteResult.collectAsStateWithLifecycle()
    val accountsResult by accountsViewModel.accounts.collectAsStateWithLifecycle()
    val validationError by transactionsViewModel.validationError.collectAsStateWithLifecycle()
    val allCategories by transactionsViewModel.categories.collectAsStateWithLifecycle()
    val formState by transactionsViewModel.formState.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(value = false) }
    var showTimePicker by remember { mutableStateOf(value = false) }
    var showNumpad by remember { mutableStateOf(false) }
    var numpadTarget by remember { mutableStateOf(NumpadTarget.Amount) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    KMPBackHandler(enabled = showNumpad) {
        showNumpad = false
    }

    LaunchedEffect(showNumpad) {
        if (showNumpad) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(transactionId, accountsResult) {
        transactionsViewModel.resetDeleteResult()
        if (transactionId != null) {
            if (accountsResult is Result.Success) {
                transactionsViewModel.loadTransactionById(
                    transactionId,
                    (accountsResult as Result.Success).data
                )
            }
        } else {
            transactionsViewModel.resetSelectedTransaction()
        }
    }

    LaunchedEffect(Unit) {
        transactionsViewModel.refreshCategories()
        if (accountsResult !is Result.Success || (accountsResult as Result.Success).data.isEmpty()) {
            accountsViewModel.reloadAccounts()
        }
    }

    LaunchedEffect(deleteResult) {
        if (deleteResult is Result.Success) {
            onGlobalRefresh()
            onBack()
        }
    }

    val themeColor by animateColorAsState(
        targetValue = if (formState.isIncome) GreenIncome else PinkExpense,
        animationSpec = tween(durationMillis = 500)
    )

    // Animate the bottom padding to "follow" the bottom bar sliding in/out
    // We use the transition state to start moving IMMEDIATELY when the screen enters
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
        // When the screen is visible, the bottom bar is gone, so we want 0dp extra padding
        // When the screen is entering/exiting, we want to match the bar's position
        if (state == EnterExitState.Visible) 0.dp else bottomBarHeight
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success<*>) {
            onGlobalRefresh()
            delay(1200)
            onBack()
            transactionsViewModel.resetSaveState()
        }
    }

    LaunchedEffect(validationError) {
        validationError?.let {
            onShowToast(it, true)
            transactionsViewModel.clearValidationError()
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Error) {
            val error = (saveState as SaveState.Error).exception
            val message = (error as? ApiException)?.getUserFriendlyMessage() ?: error.message ?: "Failed to save transaction"
            onShowToast(message, true)
            transactionsViewModel.resetSaveState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            with(sharedTransitionScope) {
                FinanceAmountHeader(
                    amount = formState.amount,
                    selectionStart = formState.amountSelectionStart,
                    selectionEnd = formState.amountSelectionEnd,
                    onSelectionChange = { start, end -> transactionsViewModel.onAmountSelectionChange(start, end) },
                    label = if (formState.isIncome) "Income Amount" else "Expense Amount",
                    isIncome = formState.isIncome,
                    themeColor = themeColor,
                    paddingValues = paddingValues,
                    isActive = showNumpad && numpadTarget == NumpadTarget.Amount,
                    onToggleNumpad = {
                        numpadTarget = NumpadTarget.Amount
                        showNumpad = it
                    },
                    modifier = Modifier.sharedBounds(
                        rememberSharedContentState(key = "transaction_header_${transactionId ?: "new"}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        },
                        clipInOverlayDuringTransition = OverlayClip(
                            RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
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
                    isIncome = formState.isIncome,
                    onTypeChange = { transactionsViewModel.onTypeChange(it) }
                )

                TransactionCostSection(
                    cost = formState.transactionCost,
                    selectionStart = formState.costSelectionStart,
                    onSelectionChange = { start, end -> transactionsViewModel.onCostSelectionChange(start, end) },
                    isActive = showNumpad && numpadTarget == NumpadTarget.TransactionCost,
                    onClick = {
                        numpadTarget = NumpadTarget.TransactionCost
                        showNumpad = true
                    }
                )

                AccountSelectionSection(
                    accountsResult = accountsResult,
                    selectedAccount = formState.selectedAccount,
                    onAccountSelected = { transactionsViewModel.onAccountChange(it) },
                    onRetry = { accountsViewModel.reloadAccounts() }
                )

                FinanceCategorySelection(
                    label = "Category",
                    categories = allCategories,
                    selectedCategories = formState.selectedCategory?.let { setOf(it) } ?: emptySet(),
                    onCategorySelectionChange = { transactionsViewModel.onCategoryChange(it.firstOrNull()) },
                    isExpense = !formState.isIncome,
                    multiSelect = false
                )

                FinanceInputSection(
                    label = "Description",
                    value = formState.description,
                    onValueChange = { transactionsViewModel.onDescriptionChange(it) },
                    placeholder = "Enter description",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    singleLine = false,
                    onFocus = { showNumpad = false }
                )

                DateTimeSelectionSection(
                    dateTime = formState.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()),
                    onDateClicked = { showDatePicker = true },
                    onTimeClicked = { showTimePicker = true }
                )

                Spacer(modifier = Modifier.height(140.dp))
            }
        }

        if (transactionId != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), CircleShape)
                ) {
                    if (deleteResult is Result.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Transaction", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                title = "Delete Transaction?",
                message = "Are you sure you want to delete this transaction? This action cannot be undone.",
                confirmLabel = "Delete",
                isDestructive = true,
                onConfirm = { transactionId?.let { transactionsViewModel.deleteTransaction(it) } },
                onDismiss = { showDeleteDialog = false }
            )
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
                    isFormValid = (formState.amount.isNotBlank() && 
                                 formState.selectedCategory != null && 
                                 formState.selectedAccount != null && 
                                 formState.description.isNotBlank()),
                    themeColor = themeColor,
                    contentColor = if (formState.isIncome) MaterialTheme.colorScheme.onTertiary else Color.White,
                    onSaveClick = { 
                        showNumpad = false
                        focusManager.clearFocus()
                        if (transactionId != null) {
                            transactionsViewModel.updateTransaction(id = transactionId)
                        } else {
                            transactionsViewModel.addTransaction()
                        }
                    },
                    label = if (transactionId != null) "Update Transaction" else "Save Transaction",
                    successLabel = if (transactionId != null) "Updated" else "Saved"
                )
            }
        }

        AnimatedVisibility(
            visible = showNumpad,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FinanceNumpad(
                onNumberClick = { num ->
                    when (numpadTarget) {
                        NumpadTarget.Amount -> transactionsViewModel.handleAmountInput(num)
                        NumpadTarget.TransactionCost -> transactionsViewModel.handleCostInput(num)
                    }
                },
                onBackspaceClick = {
                    when (numpadTarget) {
                        NumpadTarget.Amount -> transactionsViewModel.handleAmountBackspace()
                        NumpadTarget.TransactionCost -> transactionsViewModel.handleCostBackspace()
                    }
                },
                onDoneClick = { showNumpad = false }
            )
        }

        if (showDatePicker) {
            val localDateTime = formState.dateTime.toLocalDateTime(TimeZone.currentSystemDefault())
            FintrackDatePickerDialog(
                initialDate = localDateTime.date,
                onDateSelected = { selectedDate ->
                    transactionsViewModel.onDateTimeChange(
                        LocalDateTime(date = selectedDate, time = localDateTime.time).toInstant(TimeZone.currentSystemDefault())
                    )
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        if (showTimePicker) {
            val localDateTime = formState.dateTime.toLocalDateTime(TimeZone.currentSystemDefault())
            FintrackTimePickerDialog(
                initialTime = localDateTime.time,
                onTimeSelected = { selectedTime ->
                    transactionsViewModel.onDateTimeChange(
                        LocalDateTime(date = localDateTime.date, time = selectedTime).toInstant(TimeZone.currentSystemDefault())
                    )
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }
    }
}

@Composable
fun TransactionCostSection(
    cost: String,
    selectionStart: Int,
    onSelectionChange: (Int, Int) -> Unit,
    isActive: Boolean,
    onClick: () -> Unit
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val transformedCost = remember(cost) {
        if (cost.isEmpty()) return@remember "0"
        val parts = cost.split(".")
        val integerPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""
        (if (integerPart.isEmpty() && decimalPart.isNotEmpty()) "0" else integerPart) + decimalPart
    }

    val offsetMapping = remember(cost) {
        ThousandsSeparatorOffsetMapping(cost)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Transaction Fees", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    Icons.Default.Payments, 
                    null, 
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
                    Text(
                        text = transformedCost,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (cost.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        ),
                        onTextLayout = { textLayoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(cost) {
                                detectTapGestures { offset ->
                                    textLayoutResult?.let { layout ->
                                        val transformedIndex = layout.getOffsetForPosition(offset)
                                        val originalIndex = offsetMapping.transformedToOriginal(transformedIndex)
                                        onSelectionChange(originalIndex, originalIndex)
                                        onClick()
                                    }
                                }
                            }
                    )

                    if (isActive) {
                        val cursorColor = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.matchParentSize()) {
                            textLayoutResult?.let { layout ->
                                val transformedIndex = offsetMapping.originalToTransformed(selectionStart)
                                val cursorRect = layout.getCursorRect(transformedIndex.coerceIn(0, transformedCost.length))
                                
                                drawLine(
                                    color = cursorColor.copy(alpha = cursorAlpha),
                                    start = Offset(cursorRect.left, cursorRect.top + 2.dp.toPx()),
                                    end = Offset(cursorRect.left, cursorRect.bottom - 2.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
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
fun DateTimeSelectionSection(
    dateTime: LocalDateTime,
    onDateClicked: () -> Unit,
    onTimeClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Date & Time", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onDateClicked), horizontalAlignment = Alignment.Start) {
                    Text("Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = dateTime.date.formatAsShortDateWithYear(), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f).clickable(onClick = onTimeClicked), horizontalAlignment = Alignment.Start) {
                    Text("Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val timeFormat = LocalTimeFormat.current
                        Text(text = dateTime.time.format(timeFormat), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

enum class NumpadTarget {
    Amount, TransactionCost
}
