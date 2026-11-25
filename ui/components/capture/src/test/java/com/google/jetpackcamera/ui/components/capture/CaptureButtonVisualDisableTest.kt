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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureButtonVisualDisableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var uiState: CaptureButtonUiState by mutableStateOf(
        CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
    )

    @Test
    fun disabledWithoutDebounce_isVisuallyDisabledWithoutWaiting() {
        setContentWithManualClock()

        composeTestRule.runOnIdle {
            uiState = CaptureButtonUiState.Enabled.Idle(
                captureMode = CaptureMode.IMAGE_ONLY,
                isEnabled = false,
                debounceVisualDisable = false
            )
        }

        // Fails with a timeout if the change waits for the debounce delay.
        composeTestRule.mainClock.advanceTimeUntil(timeoutMillis = DEBOUNCE_MILLIS / 2) {
            isVisuallyDisabled()
        }
    }

    @Test
    fun disabledWithDebounce_isVisuallyDisabledAfterDelay() {
        setContentWithManualClock()

        composeTestRule.runOnIdle {
            uiState = CaptureButtonUiState.Enabled.Idle(
                captureMode = CaptureMode.IMAGE_ONLY,
                isEnabled = false,
                debounceVisualDisable = true
            )
        }
        composeTestRule.mainClock.advanceTimeBy(DEBOUNCE_MILLIS / 2)

        assertThat(isVisuallyDisabled()).isFalse()

        composeTestRule.mainClock.advanceTimeUntil(timeoutMillis = DEBOUNCE_MILLIS * 2) {
            isVisuallyDisabled()
        }
    }

    @Test
    fun enabledAgainBeforeDebounceDelay_isNeverVisuallyDisabled() {
        setContentWithManualClock()

        composeTestRule.runOnIdle {
            uiState = CaptureButtonUiState.Enabled.Idle(
                captureMode = CaptureMode.IMAGE_ONLY,
                isEnabled = false,
                debounceVisualDisable = true
            )
        }
        composeTestRule.mainClock.advanceTimeBy(DEBOUNCE_MILLIS / 2)
        composeTestRule.runOnIdle {
            uiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
        }
        composeTestRule.mainClock.advanceTimeBy(DEBOUNCE_MILLIS * 2)

        assertThat(isVisuallyDisabled()).isFalse()
    }

    private fun setContentWithManualClock() {
        composeTestRule.setContent {
            val isVisuallyDisabled by rememberDebouncedVisuallyDisabled(
                captureButtonUiState = uiState,
                delayMillis = DEBOUNCE_MILLIS
            )
            // Expose the state as a node, so that reading it synchronizes with composition.
            if (isVisuallyDisabled) {
                Box(Modifier.size(1.dp).testTag(VISUALLY_DISABLED_TAG))
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
    }

    private fun isVisuallyDisabled(): Boolean = composeTestRule
        .onAllNodesWithTag(VISUALLY_DISABLED_TAG)
        .fetchSemanticsNodes()
        .isNotEmpty()

    private companion object {
        const val DEBOUNCE_MILLIS = 10_000L
        const val VISUALLY_DISABLED_TAG = "VisuallyDisabled"
    }
}
