package com.fintrack.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetStatus
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.budget.ui.BudgetItem
import com.fintrack.shared.feature.budget.ui.BudgetSummaryHeader
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.ui.AddCategoryDialog
import com.fintrack.shared.feature.category.ui.CategoryItem
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.ui.AccountSelectionSection
import com.fintrack.shared.feature.core.ui.ConfirmationDialog
import com.fintrack.shared.feature.core.ui.FinanceAmountHeader
import com.fintrack.shared.feature.core.ui.FinanceCategorySelection
import com.fintrack.shared.feature.core.ui.FinanceNumpad
import com.fintrack.shared.feature.core.ui.FinanceSaveButton
import com.fintrack.shared.feature.core.ui.FinanceTypeSection
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.user.ui.onboarding.OnboardingPageOne
import com.fintrack.shared.feature.user.ui.onboarding.OnboardingPageThree
import com.fintrack.shared.feature.user.ui.onboarding.OnboardingPageTwo
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(kotlin.time.ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class AppEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- E2E Feature Journey 1: Onboarding Flow ---

    @Test
    fun e2e_onboardingJourney_navigatesThroughAllPagesToCompletion() {
        var completedName = ""
        var step = 0

        composeTestRule.setContent {
            MaterialTheme {
                when (step) {
                    0 -> OnboardingPageOne()
                    1 -> OnboardingPageTwo()
                    2 -> OnboardingPageThree(
                        name = completedName,
                        nameError = if (completedName.isBlank()) "Name required" else null,
                        onNameChange = { completedName = it },
                        onDone = { step = 3 }
                    )
                    else -> Text("Welcome, $completedName!")
                }
            }
        }

        // Step 1: Page One verification
        composeTestRule.onNodeWithText("Master Your Money").assertIsDisplayed()

        // Advance to Step 2: Page Two
        step = 1
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Smart Automation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automated SMS Tracking").assertIsDisplayed()

        // Advance to Step 3: Page Three
        step = 2
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Welcome Aboard!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Name required").assertIsDisplayed()

        // Enter Name & Complete
        completedName = "Alex"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Name").performTextReplacement("Alex")
        step = 3
        composeTestRule.waitForIdle()

        // Final completion screen
        composeTestRule.onNodeWithText("Welcome, Alex!").assertIsDisplayed()
    }

    // --- E2E Feature Journey 2: Add Transaction Workflow ---

    @Test
    fun e2e_addTransactionJourney_entersAmountSelectsCategoryAndSaves() {
        var amount by mutableStateOf("")
        var isIncome by mutableStateOf(false)
        var selectedCategory by mutableStateOf<Category?>(null)
        var selectedAccount by mutableStateOf<Account?>(null)
        var saveState by mutableStateOf<SaveState<Unit>>(SaveState.Idle)
        var savedTransaction: Transaction? by mutableStateOf(null)

        val accounts = listOf(
            Account(id = "acc1", name = "M-Pesa", type = AccountType.MPESA, balance = BigDecimal.parseString("10000.00")),
            Account(id = "acc2", name = "Equity", type = AccountType.BANK, balance = BigDecimal.parseString("50000.00"))
        )
        val categories = listOf(
            Category(id = "cat1", name = "Groceries", isExpense = true),
            Category(id = "cat2", name = "Salary", isExpense = false)
        )

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        FinanceTypeSection(
                            isIncome = isIncome,
                            onTypeChange = {
                                isIncome = it
                                selectedCategory = null
                            }
                        )

                        FinanceAmountHeader(
                            amount = amount,
                            selectionStart = amount.length,
                            selectionEnd = amount.length,
                            onSelectionChange = { _, _ -> },
                            label = if (isIncome) "Income Amount" else "Expense Amount",
                            isIncome = isIncome,
                            themeColor = if (isIncome) Color.Green else Color.Red,
                            onToggleNumpad = {}
                        )

                        AccountSelectionSection(
                            accountsResult = Result.Success(accounts),
                            selectedAccount = selectedAccount,
                            onAccountSelected = { selectedAccount = it },
                            onRetry = {}
                        )

                        FinanceCategorySelection(
                            label = "Select Category",
                            categories = categories,
                            selectedCategories = selectedCategory?.let { setOf(it) } ?: emptySet(),
                            onCategorySelectionChange = { selectedCategory = it.firstOrNull() },
                            isExpense = !isIncome
                        )

                        FinanceNumpad(
                            onNumberClick = { amount += it },
                            onBackspaceClick = { if (amount.isNotEmpty()) amount = amount.dropLast(1) },
                            onDoneClick = {}
                        )

                        FinanceSaveButton(
                            saveState = saveState,
                            isFormValid = amount.isNotBlank() && selectedCategory != null && selectedAccount != null,
                            themeColor = Color.Blue,
                            contentColor = Color.White,
                            onSaveClick = {
                                saveState = SaveState.Success(Unit)
                                savedTransaction = Transaction(
                                    id = "tx_new",
                                    accountId = selectedAccount!!.id,
                                    isIncome = isIncome,
                                    amount = BigDecimal.parseString(amount),
                                    category = selectedCategory!!.name,
                                    categoryId = selectedCategory!!.id,
                                    dateTime = Instant.fromEpochMilliseconds(1738368000000L)
                                )
                            },
                            label = "Save Transaction"
                        )

                        savedTransaction?.let { tx ->
                            Text("Saved: ${tx.category} - ${tx.amount}")
                        }
                    }
                }
            }
        }

        // 1. Select Account
        composeTestRule.onNodeWithText("M-Pesa").performClick()

        // 2. Select Category
        composeTestRule.onNodeWithText("Groceries").performClick()

        // 3. Enter Amount via Numpad: 2, 5, 0, 0 -> 2500
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Verify amount display
        composeTestRule.onNodeWithText("2,500").assertIsDisplayed()

        // 4. Save
        composeTestRule.onNodeWithText("Save Transaction").performClick()

        // Verify Save Confirmation
        composeTestRule.onNodeWithText("Saved: Groceries - 2500").assertIsDisplayed()
    }

    // --- E2E Feature Journey 3: Budget Management & Threshold Alerts ---

    @Test
    fun e2e_budgetJourney_monitorsSpentVsLimitAndAlertsWhenExceeded() {
        val budget = Budget(
            id = "b1",
            accountIds = listOf("acc1"),
            name = "Groceries Budget",
            categories = listOf(Category(id = "cat1", name = "Groceries", isExpense = true)),
            limit = BigDecimal.parseString("10000.00"),
            isExpense = true,
            startDate = LocalDate(2025, 1, 1),
            endDate = LocalDate(2025, 1, 31)
        )

        var spentAmount by mutableStateOf("6000.00")

        composeTestRule.setContent {
            MaterialTheme {
                val spent = remember(spentAmount) { BigDecimal.parseString(spentAmount) }
                val isExceeded = spent > budget.limit
                val budgetWithStatus = BudgetWithStatus(
                    budget = budget,
                    status = BudgetStatus(
                        spent = spent,
                        remaining = budget.limit - spent,
                        percentageUsed = BigDecimal.parseString("60.0"),
                        isExceeded = isExceeded
                    )
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    BudgetSummaryHeader(budgets = listOf(budgetWithStatus))
                    BudgetItem(budgetWithStatus = budgetWithStatus, onClick = {})
                }
            }
        }

        // 1. Initial State (Under budget)
        composeTestRule.onNodeWithText("Groceries Budget").assertIsDisplayed()
        composeTestRule.onNodeWithText("KSh 6,000.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exceeded").assertDoesNotExist()

        // 2. Spending exceeds budget limit (12,000 > 10,000)
        spentAmount = "12000.00"
        composeTestRule.waitForIdle()

        // Verify Over-Budget Alert Status
        composeTestRule.onNodeWithText("KSh 12,000.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exceeded").assertIsDisplayed()
    }

    // --- E2E Feature Journey 4: Custom Category Creation Flow ---

    @Test
    fun e2e_categoryManagement_createsCustomCategoryAndRendersInList() {
        var showDialog by mutableStateOf(false)
        val categories = mutableListOf(
            Category(id = "c1", name = "Groceries", isExpense = true, isDefault = true),
            Category(id = "c2", name = "Rent", isExpense = true, isDefault = true)
        )

        composeTestRule.setContent {
            MaterialTheme {
                Column(modifier = Modifier.padding(16.dp)) {
                    categories.forEach { cat ->
                        CategoryItem(category = cat, onDelete = {})
                    }

                    androidx.compose.material3.Button(onClick = { showDialog = true }) {
                        Text("Add Category")
                    }

                    if (showDialog) {
                        AddCategoryDialog(
                            initialIsExpense = true,
                            onDismiss = { showDialog = false },
                            onConfirm = { name, isExpense ->
                                categories.add(
                                    Category(
                                        id = "custom_${categories.size + 1}",
                                        name = name,
                                        isExpense = isExpense,
                                        isDefault = false
                                    )
                                )
                                showDialog = false
                            }
                        )
                    }
                }
            }
        }

        // 1. Initial categories
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()

        // 2. Open Add Category Dialog
        composeTestRule.onNodeWithText("Add Category").performClick()

        // 3. Type category name "Gym & Fitness" and Save
        composeTestRule.onNodeWithText("Category Name").performTextInput("Gym & Fitness")
        composeTestRule.onNodeWithText("Save").performClick()

        // 4. Verify new category is rendered in the list
        composeTestRule.onNodeWithText("Gym & Fitness").assertIsDisplayed()
    }

    // --- E2E Feature Journey 5: Data Reset Confirmation Flow ---

    @Test
    fun e2e_settings_triggersClearDataConfirmationDialog() {
        var showClearDialog by mutableStateOf(false)
        var dataCleared by mutableStateOf(false)

        composeTestRule.setContent {
            MaterialTheme {
                Column(modifier = Modifier.padding(16.dp)) {
                    androidx.compose.material3.Button(onClick = { showClearDialog = true }) {
                        Text("Clear All User Data")
                    }

                    if (dataCleared) {
                        Text("Data Reset Successful")
                    }

                    if (showClearDialog) {
                        ConfirmationDialog(
                            title = "Clear All Data?",
                            message = "Are you sure you want to reset all account data?",
                            confirmLabel = "Delete Everything",
                            isDestructive = true,
                            onConfirm = {
                                dataCleared = true
                                showClearDialog = false
                            },
                            onDismiss = { showClearDialog = false }
                        )
                    }
                }
            }
        }

        // 1. Click Clear All User Data
        composeTestRule.onNodeWithText("Clear All User Data").performClick()

        // 2. Verify Confirmation Dialog
        composeTestRule.onNodeWithText("Clear All Data?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to reset all account data?").assertIsDisplayed()

        // 3. Confirm Delete Everything
        composeTestRule.onNodeWithText("Delete Everything").performClick()

        // 4. Verify completion
        composeTestRule.onNodeWithText("Data Reset Successful").assertIsDisplayed()
    }
}
