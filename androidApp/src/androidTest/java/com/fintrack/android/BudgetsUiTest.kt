package com.fintrack.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetStatus
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.budget.ui.BudgetItem
import com.fintrack.shared.feature.budget.ui.BudgetSummaryHeader
import com.fintrack.shared.feature.budget.ui.SexyAddBudgetButton
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val startDate = LocalDate(2025, 1, 1)
    private val endDate = LocalDate(2025, 1, 31)

    private val sampleBudgetWithStatus = BudgetWithStatus(
        budget = Budget(
            id = "b1",
            accountIds = emptyList(),
            name = "Monthly Groceries",
            categories = emptyList(),
            limit = BigDecimal.parseString("20000.00"),
            isExpense = true,
            startDate = startDate,
            endDate = endDate
        ),
        status = BudgetStatus(
            spent = BigDecimal.parseString("12000.00"),
            remaining = BigDecimal.parseString("8000.00"),
            percentageUsed = BigDecimal.parseString("60.0"),
            isExceeded = false
        )
    )

    private val exceededBudgetWithStatus = BudgetWithStatus(
        budget = Budget(
            id = "b2",
            accountIds = emptyList(),
            name = "Entertainment",
            categories = emptyList(),
            limit = BigDecimal.parseString("5000.00"),
            isExpense = true,
            startDate = startDate,
            endDate = endDate
        ),
        status = BudgetStatus(
            spent = BigDecimal.parseString("6500.00"),
            remaining = BigDecimal.parseString("-1500.00"),
            percentageUsed = BigDecimal.parseString("130.0"),
            isExceeded = true
        )
    )

    @Test
    fun budgetItem_normalBudget_displaysSpentAndLimit() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                BudgetItem(
                    budgetWithStatus = sampleBudgetWithStatus,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Monthly Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("KSh 12,000.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("of KSh 20,000.00").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun budgetItem_overBudget_displaysExceededStatus() {
        composeTestRule.setContent {
            MaterialTheme {
                BudgetItem(
                    budgetWithStatus = exceededBudgetWithStatus,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Entertainment").assertIsDisplayed()
        composeTestRule.onNodeWithText("KSh 6,500.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exceeded").assertIsDisplayed()
    }

    @Test
    fun budgetSummaryHeader_displaysTotalLimitAndSpent() {
        val budgets = listOf(sampleBudgetWithStatus, exceededBudgetWithStatus)

        composeTestRule.setContent {
            MaterialTheme {
                BudgetSummaryHeader(budgets = budgets)
            }
        }

        composeTestRule.onNodeWithText("Total Monthly Budget").assertIsDisplayed()
        composeTestRule.onNodeWithText("KSh 25,000.00").assertIsDisplayed() // 20k + 5k
        composeTestRule.onNodeWithText("Spent KSh 18,500.00").assertIsDisplayed() // 12k + 6.5k
    }

    @Test
    fun sexyAddBudgetButton_clicksToAddBudget() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                SexyAddBudgetButton(
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Create New Budget").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Plan your spending strategically").assertIsDisplayed()
        assert(clicked)
    }
}
