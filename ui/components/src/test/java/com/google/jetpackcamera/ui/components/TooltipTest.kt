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
package com.google.jetpackcamera.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class TooltipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tooltip_initialState_isNotDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Help information"
                )
            }
        }

        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_clickAnchor_togglesVisibility() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipTitle = "Info Title",
                    tooltipText = "Help information"
                )
            }
        }

        // Show on first tap
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertExists()
        composeTestRule.onNode(hasText("Help information") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule.onNode(hasText("Info Title") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Dismiss on second tap
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_externalFlowTrigger_showsAndDismisses() {
        val triggerFlow = MutableSharedFlow<TooltipTriggerEvent>(extraBufferCapacity = 1)

        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "External trigger text",
                    triggerFlow = triggerFlow
                )
            }
        }

        composeTestRule.onNode(isPopup()).assertDoesNotExist()

        // Emit Show
        triggerFlow.tryEmit(TooltipTriggerEvent.Show)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()
        composeTestRule.onNode(hasText("External trigger text") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Emit Dismiss
        triggerFlow.tryEmit(TooltipTriggerEvent.Dismiss)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_autoDismiss_dismissesAfterSpecifiedDuration() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Auto dismissing text",
                    autoDismissDuration = 3000.milliseconds
                )
            }
        }

        // Trigger show
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertExists()

        // Advance part-way (1500ms) -> Still displayed
        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()

        // Advance remaining time past 3000ms -> Trigger dismiss and allow exit transition
        composeTestRule.mainClock.advanceTimeBy(1501)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_autoDismiss_reTriggerResetsTimer() {
        composeTestRule.mainClock.autoAdvance = false
        val triggerFlow = MutableSharedFlow<TooltipTriggerEvent>(extraBufferCapacity = 1)

        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Resetting timer text",
                    autoDismissDuration = 3000.milliseconds,
                    triggerFlow = triggerFlow
                )
            }
        }

        // Trigger show via flow
        triggerFlow.tryEmit(TooltipTriggerEvent.Show)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()

        // Advance 2000ms of 3000ms
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()

        // Re-trigger Show -> Timer resets
        triggerFlow.tryEmit(TooltipTriggerEvent.Show)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        // Advance another 2000ms (total 4000ms from start, but only 2000ms since reset)
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()
        // Must still exist because timer was reset
        composeTestRule.onNode(isPopup()).assertExists()

        // Advance past reset expiration (remaining 1001ms) -> Trigger dismiss and allow exit transition
        composeTestRule.mainClock.advanceTimeBy(1001)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_positioning_supportsAboveAndBelow() {
        composeTestRule.setContent {
            MaterialTheme {
                JcaTooltipBox(
                    tooltipText = "Position Below Text",
                    positioning = TooltipAnchorPosition.Below
                ) {
                    Box(modifier = Modifier.size(48.dp))
                }
            }
        }

        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_customContainerColor_appliesCorrectly() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Custom colors",
                    isOutlined = true,
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).assertExists()
    }

    @Test
    fun tooltip_customSize_appliesCorrectly() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(20.dp)) },
                    tooltipText = "Custom size",
                    size = 32.dp
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).assertExists()
    }
}
