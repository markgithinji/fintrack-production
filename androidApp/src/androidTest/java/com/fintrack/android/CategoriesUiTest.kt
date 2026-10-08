package com.fintrack.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.ui.AddCategoryDialog
import com.fintrack.shared.feature.category.ui.CategoryItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriesUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun categoryItem_customCategory_displaysDeleteButtonAndTriggersDelete() {
        var deleted = false
        val customCategory = Category(
            id = "custom1",
            name = "Coffee & Snacks",
            isExpense = true,
            isDefault = false
        )

        composeTestRule.setContent {
            MaterialTheme {
                CategoryItem(
                    category = customCategory,
                    onDelete = { deleted = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Coffee & Snacks").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed().performClick()
        assert(deleted)
    }

    @Test
    fun categoryItem_defaultCategory_hidesDeleteButton() {
        val defaultCategory = Category(
            id = "def1",
            name = "Groceries",
            isExpense = true,
            isDefault = true
        )

        composeTestRule.setContent {
            MaterialTheme {
                CategoryItem(
                    category = defaultCategory,
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    @Test
    fun addCategoryDialog_inputsNameAndConfirms() {
        var addedName = ""
        var addedIsExpense = false

        composeTestRule.setContent {
            MaterialTheme {
                AddCategoryDialog(
                    initialIsExpense = true,
                    onDismiss = {},
                    onConfirm = { name, isExpense ->
                        addedName = name
                        addedIsExpense = isExpense
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Add Category").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category Name").performTextInput("Subscriptions")
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        assert(addedName == "Subscriptions")
        assert(!addedIsExpense)
    }
}
