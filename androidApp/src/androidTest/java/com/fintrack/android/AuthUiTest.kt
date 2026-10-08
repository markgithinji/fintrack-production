package com.fintrack.android

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.KeyboardType
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.auth.ui.common.FinanceTextField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun financeTextField_typesTextCorrectly() {
        var text = ""
        composeTestRule.setContent {
            MaterialTheme {
                FinanceTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Email",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    colorScheme = MaterialTheme.colorScheme
                )
            }
        }

        composeTestRule.onNodeWithText("Email").assertIsDisplayed().performTextInput("test@example.com")
        assert(text == "test@example.com")
    }
}
