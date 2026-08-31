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

import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureScreenComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator().setRunChecksFromRootView(true).also {
                it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
            }
        )
    }

    @Test
    fun elapsedTimeText_unavailableState_doesNotRender() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Unavailable }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledActive_displaysFormattedTime() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                // 0:30
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Enabled(30_000_000_000L) }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals("0:30")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledActive_setsContentDescription() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                // 1:05
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Enabled(65_000_000_000L) }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG)
            .assertContentDescriptionEquals("Recording time: 1 minutes and 5 seconds")
    }

    @Test
    fun elapsedTimeText_enabledPaused_displaysPausedFormattedTime() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = {
                    // PAUSED 0:30
                    ElapsedTimeUiState.Enabled(30_000_000_000L, isPaused = true)
                }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals("PAUSED 0:30")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledPaused_setsContentDescription() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = {
                    // PAUSED 1:05
                    ElapsedTimeUiState.Enabled(65_000_000_000L, isPaused = true)
                }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG)
            .assertContentDescriptionEquals("Recording paused: 1 minutes and 5 seconds")
    }

    @Test
    fun focusMeteringIndicator_unspecifiedState_doesNotRender() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Unspecified,
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun focusMeteringIndicator_runningState_renders() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = Offset(100f, 100f),
                        status = FocusMeteringUiState.Status.RUNNING
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_successState_persistsWithoutAutoDismiss() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = Offset(100f, 100f),
                        status = FocusMeteringUiState.Status.SUCCESS
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_failureState_renders() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = Offset(100f, 100f),
                        status = FocusMeteringUiState.Status.FAILURE
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_cancelledState_doesNotRender() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = Offset(100f, 100f),
                        status = FocusMeteringUiState.Status.CANCELLED
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun focusMeteringIndicator_hasAccessibilityContentDescription_whenRunning() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = Offset(100f, 100f),
                        status = FocusMeteringUiState.Status.RUNNING
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG)
            .assertContentDescriptionEquals("Focusing")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_hasAccessibilityContentDescription_whenLocked() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = Offset(100f, 100f),
                        status = FocusMeteringUiState.Status.SUCCESS
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG)
            .assertContentDescriptionEquals("Focused")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
