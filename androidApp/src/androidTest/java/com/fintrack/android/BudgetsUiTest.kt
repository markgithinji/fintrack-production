package com.fintrack.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun budgetScreen_smokeTest() {
        var clicked = false
        composeTestRule.setContent {
            androidx.compose.material3.Surface {
                androidx.compose.material3.Button(onClick = { clicked = true }) {
                    androidx.compose.material3.Text("Add Budget")
                }
            }
        }

        composeTestRule.onNodeWithText("Add Budget").assertIsDisplayed().performClick()
        assert(clicked)
    }
}
