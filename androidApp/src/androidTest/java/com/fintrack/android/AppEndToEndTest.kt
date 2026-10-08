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
class AppEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun e2e_onboardingJourney_navigatesThroughAllPagesToCompletion() {
        var completedName = ""
        var step = 0

        composeTestRule.setContent {
            MaterialTheme {
                when (step) {
                    0 -> OnboardingPageOne()
                    1 -> OnboardingPageTwo()
                    2 -> OnboardingPageThree(
                        name = completedName,
                        nameError = if (completedName.isBlank()) "Name required" else null,
                        onNameChange = { completedName = it },
                        onDone = { step = 3 }
                    )
                    else -> androidx.compose.material3.Text("Welcome, $completedName!")
                }
            }
        }

        // Step 1: Page One verification
        composeTestRule.onNodeWithText("Master Your Money").assertIsDisplayed()

        // Advance to Step 2: Page Two
        step = 1
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Smart Automation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Automated SMS Tracking").assertIsDisplayed()

        // Advance to Step 3: Page Three
        step = 2
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Welcome Aboard!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Name required").assertIsDisplayed()

        // Enter Name & Complete
        completedName = "Alex"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Name").performTextReplacement("Alex")
        step = 3
        composeTestRule.waitForIdle()

        // Final completion screen
        composeTestRule.onNodeWithText("Welcome, Alex!").assertIsDisplayed()
    }
}
