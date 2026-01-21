/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.ui.components.capture.BTN_DEBUG_HIDE_COMPONENTS_TAG
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.FOCUS_METERING_INDICATOR_TAG
import com.google.jetpackcamera.ui.components.capture.PREVIEW_DISPLAY
import com.google.jetpackcamera.utils.FOCUS_METERING_INDICATOR_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.debugExtra
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.wait
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTag
import com.google.jetpackcamera.utils.waitForNodeWithTagToDisappear
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusMeteringTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun testFocusMeteringIndicator_appearsInCorrectQuadrant() =
        runMainActivityScenarioTest(debugExtra) {
            composeTestRule.waitForCaptureButton()

            // Hide all components so we don't accidentally tap on them
            composeTestRule.onNodeWithTag(BTN_DEBUG_HIDE_COMPONENTS_TAG).performClick()

            composeTestRule.waitForNodeWithTagToDisappear(CAPTURE_BUTTON)

            // Define the four quadrants of the screen
            val quadrants = listOf(
                Pair(0.25f, 0.25f), // Top-left
                Pair(0.75f, 0.25f), // Top-right
                Pair(0.25f, 0.75f), // Bottom-left
                Pair(0.75f, 0.75f) // Bottom-right
            )

            quadrants.forEach { (x, y) ->
                // Tap on the viewfinder in the current quadrant
                composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).apply {
                    val displayBounds = fetchSemanticsNode().boundsInWindow

                    // Tap on the quadrant
                    performTouchInput { click(position = percentOffset(x, y)) }

                    // Wait for the focus metering indicator to be visible
                    composeTestRule.waitForNodeWithTag(
                        FOCUS_METERING_INDICATOR_TAG,
                        FOCUS_METERING_INDICATOR_TIMEOUT_MILLIS
                    )

                    composeTestRule.waitUntil(FOCUS_METERING_INDICATOR_TIMEOUT_MILLIS) {
                        composeTestRule.onAllNodesWithTag(FOCUS_METERING_INDICATOR_TAG).run {
                            val indicatorBounds =
                                fetchSemanticsNodes().firstOrNull()?.boundsInWindow
                                    ?: run { return@waitUntil false }
                            val indicatorCenter = Offset(
                                indicatorBounds.left + indicatorBounds.width / 2,
                                indicatorBounds.top + indicatorBounds.height / 2
                            )

                            val displayCenter = Offset(
                                displayBounds.left + displayBounds.width / 2,
                                displayBounds.top + displayBounds.height / 2
                            )

                            // Calculate signum of the difference between the center of the indicator
                            // and the center of the display
                            val indicatorXSign = if (indicatorCenter.x > displayCenter.x) 1 else -1
                            val indicatorYSign = if (indicatorCenter.y > displayCenter.y) 1 else -1

                            // Calculate signum of normalized quadrant coordinates
                            val quadrantXSign = if (x > 0.5f) 1 else -1
                            val quadrantYSign = if (y > 0.5f) 1 else -1

                            // Wait until indicator is in the correct quadrant
                            indicatorXSign == quadrantXSign && indicatorYSign == quadrantYSign
                        }
                    }

                    // Wait until twice the double tap threshold has expired so the next tap doesn't
                    // get registered as a double tap
                    val doubleTapTimeout = android.view.ViewConfiguration.getDoubleTapTimeout()
                    composeTestRule.wait(timeoutMillis = 2 * doubleTapTimeout.toLong())
                }
            }
        }
}
