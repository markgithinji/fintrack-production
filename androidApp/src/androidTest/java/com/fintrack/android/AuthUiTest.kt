package com.fintrack.android

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.KeyboardType
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.auth.ui.LockScreen
import com.fintrack.shared.feature.auth.ui.common.FinanceTextField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun financeTextField_typesTextAndDisplaysLabel() {
        var text = ""
        composeTestRule.setContent {
            MaterialTheme {
                FinanceTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    colorScheme = MaterialTheme.colorScheme
                )
            }
        }

        composeTestRule.onNodeWithText("Email Address").assertIsDisplayed().performTextInput("user@fintrack.com")
        assert(text == "user@fintrack.com")
    }

    @Test
    fun financeTextField_errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                FinanceTextField(
                    value = "invalid-email",
                    onValueChange = {},
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    colorScheme = MaterialTheme.colorScheme,
                    isError = true,
                    errorMessage = "Please enter a valid email address"
                )
            }
        }

        composeTestRule.onNodeWithText("Please enter a valid email address").assertIsDisplayed()
    }

    @Test
    fun lockScreen_displaysLockUIAndTriggersUnlock() {
        var unlocked = false
        composeTestRule.setContent {
            MaterialTheme {
                LockScreen(
                    onUnlock = { unlocked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Fintrack is Locked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please authenticate to continue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Use Fingerprint").assertIsDisplayed().performClick()
        assert(unlocked)
    }
}
