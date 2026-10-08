package com.fintrack.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.core.ui.AccountChip
import com.fintrack.shared.feature.core.ui.AccountChipShimmer
import com.fintrack.shared.feature.core.ui.AccountSelectionSection
import com.fintrack.shared.feature.core.ui.MultiAccountSelectionSection
import com.fintrack.shared.feature.core.util.Result
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accountChip_displaysNameAndBalance_andClicks() {
        var clicked = false
        val account = Account(
            id = "acc1",
            name = "M-Pesa Wallet",
            type = AccountType.MPESA,
            balance = BigDecimal.parseString("15000.50")
        )

        composeTestRule.setContent {
            MaterialTheme {
                AccountChip(
                    account = account,
                    isSelected = false,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("M-Pesa Wallet").assertIsDisplayed()
        composeTestRule.onNodeWithText("KSh 15,000.50").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun accountChipShimmer_displaysShimmerPlaceholder() {
        composeTestRule.setContent {
            MaterialTheme {
                AccountChipShimmer()
            }
        }

        // Verify shimmer container is rendered
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun accountSelectionSection_success_displaysAccountsAndSelects() {
        var selectedAccount: Account? = null
        val accounts = listOf(
            Account(id = "1", name = "M-Pesa", type = AccountType.MPESA, balance = BigDecimal.parseString("2500.00")),
            Account(id = "2", name = "Equity Bank", type = AccountType.BANK, balance = BigDecimal.parseString("45000.00"))
        )

        composeTestRule.setContent {
            MaterialTheme {
                AccountSelectionSection(
                    accountsResult = Result.Success(accounts),
                    selectedAccount = accounts[0],
                    onAccountSelected = { selectedAccount = it },
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Select Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("M-Pesa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Equity Bank").assertIsDisplayed().performClick()
        assert(selectedAccount?.id == "2")
    }

    @Test
    fun accountSelectionSection_empty_displaysEmptyMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                AccountSelectionSection(
                    accountsResult = Result.Success(emptyList()),
                    selectedAccount = null,
                    onAccountSelected = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No accounts available. Create an account first.").assertIsDisplayed()
    }

    @Test
    fun accountSelectionSection_error_displaysRetryButton() {
        var retried = false
        composeTestRule.setContent {
            MaterialTheme {
                AccountSelectionSection(
                    accountsResult = Result.Error(Exception("Database error")),
                    selectedAccount = null,
                    onAccountSelected = {},
                    onRetry = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Failed to load accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assert(retried)
    }

    @Test
    fun multiAccountSelectionSection_togglesSelectionCount() {
        var toggledAccount: Account? = null
        val accounts = listOf(
            Account(id = "1", name = "Cash", type = AccountType.CASH, balance = BigDecimal.parseString("500.00")),
            Account(id = "2", name = "KCB", type = AccountType.BANK, balance = BigDecimal.parseString("12000.00"))
        )

        composeTestRule.setContent {
            MaterialTheme {
                MultiAccountSelectionSection(
                    accountsResult = Result.Success(accounts),
                    selectedAccounts = setOf(accounts[0]),
                    onAccountToggle = { toggledAccount = it },
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Select Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("KCB").assertIsDisplayed().performClick()
        assert(toggledAccount?.id == "2")
    }
}
