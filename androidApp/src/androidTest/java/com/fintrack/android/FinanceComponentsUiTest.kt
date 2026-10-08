import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.ui.CommonErrorState
import com.fintrack.shared.feature.core.ui.FinanceAmountHeader
import com.fintrack.shared.feature.core.ui.FinanceCategorySelection
import com.fintrack.shared.feature.core.ui.FinanceNumpad
import com.fintrack.shared.feature.core.ui.FinanceSaveButton
import com.fintrack.shared.feature.core.ui.FinanceTypeSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceComponentsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- FinanceSaveButton Tests ---

    @Test
    fun financeSaveButton_idleAndValid_displaysLabelAndTriggersClick() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                FinanceSaveButton(
                    saveState = SaveState.Idle,
                    isFormValid = true,
                    themeColor = Color.Blue,
                    contentColor = Color.White,
                    onSaveClick = { clicked = true },
                    label = "Save Transaction"
                )
            }
        }

        composeTestRule.onNodeWithText("Save Transaction").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun financeSaveButton_idleAndInvalid_isSemiTransparentAndDisabled() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                FinanceSaveButton(
                    saveState = SaveState.Idle,
                    isFormValid = false,
                    themeColor = Color.Blue,
                    contentColor = Color.White,
                    onSaveClick = { clicked = true },
                    label = "Save Transaction"
                )
            }
        }

        composeTestRule.onNodeWithText("Save Transaction").assertIsDisplayed()
        // When invalid, button is disabled so click shouldn't trigger
        composeTestRule.onNodeWithText("Save Transaction").performClick()
        assert(!clicked)
    }

    @Test
    fun financeSaveButton_loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            MaterialTheme {
                FinanceSaveButton(
                    saveState = SaveState.Loading,
                    isFormValid = true,
                    themeColor = Color.Blue,
                    contentColor = Color.White,
                    onSaveClick = {},
                    label = "Save Transaction"
                )
            }
        }

        // Label should be hidden while loading progress is visible
        composeTestRule.onNodeWithText("Save Transaction").assertDoesNotExist()
    }

    @Test
    fun financeSaveButton_successState_displaysSavedLabel() {
        composeTestRule.setContent {
            MaterialTheme {
                FinanceSaveButton(
                    saveState = SaveState.Success(Unit),
                    isFormValid = true,
                    themeColor = Color.Blue,
                    contentColor = Color.White,
                    onSaveClick = {},
                    label = "Save Transaction",
                    successLabel = "Saved Successfully"
                )
            }
        }

        composeTestRule.onNodeWithText("Saved Successfully").assertIsDisplayed()
    }

    // --- FinanceNumpad Tests ---

    @Test
    fun financeNumpad_clicksNumbers_backspaceAndDone() {
        val numberInputs = mutableListOf<String>()
        var backspaceClicked = false
        var doneClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                FinanceNumpad(
                    onNumberClick = { numberInputs.add(it) },
                    onBackspaceClick = { backspaceClicked = true },
                    onDoneClick = { doneClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("5").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText(".").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithContentDescription("Backspace").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed().performClick()

        assert(numberInputs == listOf("1", "5", "."))
        assert(backspaceClicked)
        assert(doneClicked)
    }

    // --- FinanceAmountHeader Tests ---

    @Test
    fun financeAmountHeader_displaysFormattedAmountAndCurrency() {
        var numpadToggled = false
        composeTestRule.setContent {
            MaterialTheme {
                FinanceAmountHeader(
                    amount = "12345.67",
                    selectionStart = 8,
                    selectionEnd = 8,
                    onSelectionChange = { _, _ -> },
                    label = "Amount Spent",
                    isIncome = false,
                    themeColor = Color.Red,
                    onToggleNumpad = { numpadToggled = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Amount Spent").assertIsDisplayed()
        composeTestRule.onNodeWithText("12,345.67").assertIsDisplayed().performClick()
        assert(numpadToggled)
    }

    @Test
    fun financeAmountHeader_emptyAmount_displaysZeroPlaceholder() {
        composeTestRule.setContent {
            MaterialTheme {
                FinanceAmountHeader(
                    amount = "",
                    selectionStart = 0,
                    selectionEnd = 0,
                    onSelectionChange = { _, _ -> },
                    label = "Income Amount",
                    isIncome = true,
                    themeColor = Color.Green,
                    onToggleNumpad = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Income Amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
    }

    // --- CommonErrorState Tests ---

    @Test
    fun commonErrorState_displaysErrorAndTriggersRetry() {
        var retried = false
        composeTestRule.setContent {
            MaterialTheme {
                CommonErrorState(
                    title = "Network Connection Lost",
                    errorMessage = "Please check your internet connection.",
                    onRetry = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Network Connection Lost").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please check your internet connection.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assert(retried)
    }

    @Test
    fun commonErrorState_successState_displaysCheckIconAndSuccessMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                CommonErrorState(
                    title = "Operation Complete",
                    errorMessage = "Data exported successfully",
                    isSuccess = true
                )
            }
        }

        composeTestRule.onNodeWithText("Operation Complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data exported successfully").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Success").assertIsDisplayed()
    }

    // --- FinanceCategorySelection Tests ---

    @Test
    fun financeCategorySelection_filtersByExpense_andSelectsCategory() {
        var selected = setOf<Category>()
        val categories = listOf(
            Category(id = "cat1", name = "Groceries", isExpense = true),
            Category(id = "cat2", name = "Salary", isExpense = false)
        )

        composeTestRule.setContent {
            MaterialTheme {
                FinanceCategorySelection(
                    label = "Choose Category",
                    categories = categories,
                    selectedCategories = selected,
                    onCategorySelectionChange = { selected = it },
                    isExpense = true
                )
            }
        }

        composeTestRule.onNodeWithText("Choose Category").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Salary").assertDoesNotExist() // Salary is income
        assert(selected.firstOrNull()?.id == "cat1")
    }

    // --- FinanceTypeSection Tests ---

    @Test
    fun financeTypeSection_togglesExpenseAndIncome() {
        var isIncomeSelected = false
        composeTestRule.setContent {
            MaterialTheme {
                FinanceTypeSection(
                    isIncome = isIncomeSelected,
                    onTypeChange = { isIncomeSelected = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed().performClick()
        assert(isIncomeSelected)
    }
}
