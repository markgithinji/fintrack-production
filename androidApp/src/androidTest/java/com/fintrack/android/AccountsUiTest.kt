package com.fintrack.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.core.ui.AccountSelectionSection
import com.fintrack.shared.feature.core.util.Result
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accountSelectionSection_displaysAccountsAndClicks() {
        var selected: Account? = null
        val accounts = listOf(
            Account(id = "1", name = "M-Pesa", type = AccountType.MPESA),
            Account(id = "2", name = "Equity", type = AccountType.BANK)
        )

        composeTestRule.setContent {
            AccountSelectionSection(
                accountsResult = Result.Success(accounts),
                selectedAccount = accounts[0],
                onAccountSelected = { selected = it },
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("M-Pesa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Equity").assertIsDisplayed().performClick()
        assert(selected?.id == "2")
    }
}
