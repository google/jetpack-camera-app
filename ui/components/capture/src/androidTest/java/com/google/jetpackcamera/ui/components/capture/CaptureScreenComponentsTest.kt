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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureScreenComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elapsedTimeText_unavailableState_doesNotRender() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Unavailable }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertDoesNotExist()
    }

    @Test
    fun elapsedTimeText_enabledState_displaysFormattedTime() {
        val uiState = mutableStateOf<ElapsedTimeUiState>(ElapsedTimeUiState.Enabled(0L))
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = { uiState.value }
            )
        }

        val timeStates = mapOf(
            0L to "0:00",
            30_000_000_000L to "0:30",
            65_000_000_000L to "1:05",
            605_000_000_000L to "10:05"
        )
        
        timeStates.forEach { (nanos, expectedText) ->
            uiState.value = ElapsedTimeUiState.Enabled(nanos)
            composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals(expectedText)
        }
    }

    @Test
    fun elapsedTimeText_pausedState_displaysPausedFormattedTime() {
        val uiState = mutableStateOf<ElapsedTimeUiState>(ElapsedTimeUiState.Enabled(0L, isPaused = true))
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = { uiState.value }
            )
        }

        val timeStates = mapOf(
            0L to "PAUSED 0:00",
            30_000_000_000L to "PAUSED 0:30",
            65_000_000_000L to "PAUSED 1:05",
            605_000_000_000L to "PAUSED 10:05"
        )
        
        timeStates.forEach { (nanos, expectedText) ->
            uiState.value = ElapsedTimeUiState.Enabled(nanos, isPaused = true)
            composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals(expectedText)
        }
    }
}
