package com.fintrack.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fintrack.shared.feature.user.ui.onboarding.OnboardingPageOne
import com.fintrack.shared.feature.user.ui.onboarding.OnboardingPageThree
import com.fintrack.shared.feature.user.ui.onboarding.OnboardingPageTwo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingPageOne_displaysMasterYourMoneyHeadline() {
        composeTestRule.setContent {
            MaterialTheme {
                OnboardingPageOne()
            }
        }

        composeTestRule.onNodeWithText("Master Your Money").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Track expenses, manage budgets, and take complete control of your financial freedom — 100% offline and private.",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun onboardingPageTwo_displaysFeatureCards() {
        composeTestRule.setContent {
            MaterialTheme {
                OnboardingPageTwo()
            }
        }

        composeTestRule.onNodeWithText("Smart Automation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automated SMS Tracking").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Receipt OCR Scanner").assertIsDisplayed()
        composeTestRule.onNodeWithText("100% Private & Offline").assertIsDisplayed()
    }

    @Test
    fun onboardingPageThree_displaysNameInputAndErrorMessage() {
        var enteredName = "John Doe"
        composeTestRule.setContent {
            MaterialTheme {
                OnboardingPageThree(
                    name = enteredName,
                    nameError = "Name is required",
                    onNameChange = { enteredName = it },
                    onDone = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome Aboard!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Name is required").assertIsDisplayed()
    }
}
