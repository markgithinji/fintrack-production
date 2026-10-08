package com.fintrack.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.core.ui.ConfirmationDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confirmationDialog_displaysTitleAndMessage_andTriggersConfirm() {
        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            MaterialTheme {
                ConfirmationDialog(
                    title = "Clear All Data?",
                    message = "This will delete all local accounts, transactions, and categories.",
                    confirmLabel = "Delete All",
                    cancelLabel = "Cancel",
                    isDestructive = true,
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Clear All Data?").assertIsDisplayed()
        composeTestRule.onNodeWithText("This will delete all local accounts, transactions, and categories.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete All").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()

        assert(confirmed)
        assert(dismissed)
    }

    @Test
    fun confirmationDialog_loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            MaterialTheme {
                ConfirmationDialog(
                    title = "Exporting Backup",
                    message = "Please wait while your data is being encrypted and exported...",
                    confirmLabel = "Export",
                    isLoading = true,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Exporting Backup").assertIsDisplayed()
        // Confirm text should be hidden while progress is active
        composeTestRule.onNodeWithText("Export").assertDoesNotExist()
    }

    @Test
    fun confirmationDialog_successState_displaysDoneLabel() {
        composeTestRule.setContent {
            MaterialTheme {
                ConfirmationDialog(
                    title = "Export Complete",
                    message = "Backup created successfully.",
                    confirmLabel = "Export",
                    isSuccess = true,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Success!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun confirmationDialog_errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                ConfirmationDialog(
                    title = "Restore Backup",
                    message = "Restoring data from cloud...",
                    confirmLabel = "Restore",
                    errorMessage = "Corrupted file format",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Operation Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Corrupted file format").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed()
    }
}
