package com.fintrack.android

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.ui.FinanceNumpad
import com.fintrack.shared.feature.core.ui.FinanceSaveButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceComponentsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun financeSaveButton_displaysCorrectLabelAndClicks() {
        var clicked = false
        composeTestRule.setContent {
            FinanceSaveButton(
                saveState = SaveState.Idle,
                isFormValid = true,
                themeColor = Color.Blue,
                contentColor = Color.White,
                onSaveClick = { clicked = true },
                label = "Save Transaction"
            )
        }

        composeTestRule.onNodeWithText("Save Transaction").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun financeNumpad_clicksNumberAndDone() {
        var clickedNumber = ""
        var doneClicked = false
        composeTestRule.setContent {
            FinanceNumpad(
                onNumberClick = { clickedNumber = it },
                onBackspaceClick = {},
                onDoneClick = { doneClicked = true }
            )
        }

        composeTestRule.onNodeWithText("5").assertIsDisplayed().performClick()
        assert(clickedNumber == "5")

        composeTestRule.onNodeWithText("Done").assertIsDisplayed().performClick()
        assert(doneClicked)
    }
}
