package com.fintrack.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.ui.home.components.CurrentBalanceCard
import com.fintrack.shared.feature.transaction.ui.transactionlist.TransactionItem
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(kotlin.time.ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class TransactionsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleAccount = Account(
        id = "acc_mpesa",
        name = "M-Pesa Wallet",
        type = AccountType.MPESA,
        balance = BigDecimal.parseString("32450.00")
    )

    private val expenseTransaction = Transaction(
        id = "tx1",
        accountId = "acc_mpesa",
        isIncome = false,
        amount = BigDecimal.parseString("1500.00"),
        transactionCost = BigDecimal.parseString("25.00"),
        category = "Groceries",
        categoryId = "groceries_id",
        dateTime = Instant.fromEpochMilliseconds(1738368000000L),
        description = "Weekly Naivas Supermarket Shopping"
    )

    private val incomeTransaction = Transaction(
        id = "tx2",
        accountId = "acc_mpesa",
        isIncome = true,
        amount = BigDecimal.parseString("50000.00"),
        transactionCost = BigDecimal.ZERO,
        category = "Salary",
        categoryId = "salary_id",
        dateTime = Instant.fromEpochMilliseconds(1738368000000L),
        description = "Monthly Paycheck"
    )

    @Test
    fun transactionItem_expense_displaysCategoryAndMinusAmount() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    TransactionItem(
                        transaction = expenseTransaction,
                        animatedVisibilityScope = this,
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly Naivas Supermarket Shopping").assertIsDisplayed()
        composeTestRule.onNodeWithText("-KSh 1,525.00").assertIsDisplayed().performClick() // Amount + fee
        assert(clicked)
    }

    @Test
    fun transactionItem_income_displaysPlusAmount() {
        composeTestRule.setContent {
            MaterialTheme {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    TransactionItem(
                        transaction = incomeTransaction,
                        animatedVisibilityScope = this
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Salary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly Paycheck").assertIsDisplayed()
        composeTestRule.onNodeWithText("+KSh 50,000.00").assertIsDisplayed()
    }

    @Test
    fun currentBalanceCard_displaysBalanceAndTogglesVisibility() {
        var isHidden = false
        composeTestRule.setContent {
            MaterialTheme {
                CurrentBalanceCard(
                    selectedAccountResult = Result.Success(sampleAccount),
                    isBalanceHidden = isHidden,
                    isMpesaAutoSyncEnabled = false,
                    isEquityAutoSyncEnabled = false,
                    importState = null,
                    syncProgress = 0f,
                    onChangeAccountClicked = {},
                    onToggleBalanceVisibility = { isHidden = it },
                    onManualSync = {}
                )
            }
        }

        composeTestRule.onNodeWithText("M-Pesa Wallet").assertIsDisplayed()
        composeTestRule.onNodeWithText("KSh 32,450.00").assertIsDisplayed()

        // Toggle balance visibility
        composeTestRule.onNodeWithContentDescription("Hide balance").assertIsDisplayed().performClick()
        assert(isHidden)
    }

    @Test
    fun currentBalanceCard_whenBalanceHidden_displaysMaskedText() {
        composeTestRule.setContent {
            MaterialTheme {
                CurrentBalanceCard(
                    selectedAccountResult = Result.Success(sampleAccount),
                    isBalanceHidden = true,
                    isMpesaAutoSyncEnabled = false,
                    isEquityAutoSyncEnabled = false,
                    importState = null,
                    syncProgress = 0f,
                    onChangeAccountClicked = {},
                    onToggleBalanceVisibility = {},
                    onManualSync = {}
                )
            }
        }

        composeTestRule.onNodeWithText("••••••••").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Show balance").assertIsDisplayed()
    }
}
