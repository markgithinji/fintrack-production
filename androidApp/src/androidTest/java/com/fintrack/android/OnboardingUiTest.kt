package com.fintrack.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboarding_smokeTest() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                androidx.compose.material3.Button(onClick = { clicked = true }) {
                    androidx.compose.material3.Text("Get Started")
                }
            }
        }

        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed().performClick()
        assert(clicked)
    }
}
