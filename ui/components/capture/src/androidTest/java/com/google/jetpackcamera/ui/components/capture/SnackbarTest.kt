/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jetpackcamera.ui.components.capture

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.uistate.CustomSnackbarVisuals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnackbarTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator().setRunChecksFromRootView(true).also {
                it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
            }
        )
    }

    @Test
    fun pillSnackbar_dismissButtonClick_callsOnDismissAndUsesStringResource() {
        var dismissed = false
        val dismissDescription =
            composeTestRule.activity.getString(R.string.snackbar_dismiss_content_description)

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image capture failed",
                        actionLabel = null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                        isError = true
                    ),
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Image capture failed").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(dismissDescription)
            .assertIsDisplayed()
            .performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun pillSnackbar_actionButtonClick_callsOnAction() {
        var actionPerformed = false

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image capture failed",
                        actionLabel = "Retry",
                        withDismissAction = false,
                        duration = SnackbarDuration.Short,
                        isError = true
                    ),
                    onAction = { actionPerformed = true }
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .performClick()
        assertThat(actionPerformed).isTrue()
    }

    @Test
    fun pillSnackbar_statusIcon_hasNoContentDescription() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image saved",
                        actionLabel = null,
                        withDismissAction = false,
                        duration = SnackbarDuration.Short,
                        isError = false
                    )
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Image saved").assertIsDisplayed()
        composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true
        ).assertCountEquals(0)
    }
}
