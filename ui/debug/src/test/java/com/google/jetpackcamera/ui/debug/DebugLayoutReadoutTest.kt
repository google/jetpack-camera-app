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
package com.google.jetpackcamera.ui.debug

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.TestPattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose unit tests for [DebugLayoutReadout], [DebugTextBar] modifier scoping, and
 * [DebugOverlay] integration.
 */
@RunWith(RobolectricTestRunner::class)
class DebugLayoutReadoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun debugLayoutReadout_whenLockedTwoSideRail_displaysFormattedState() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugLayoutReadout(
                    state = DebugLayoutReadoutState(
                        widthDp = 840,
                        heightDp = 360,
                        rotationDegrees = 90,
                        isRotationLocked = true,
                        layoutModeName = "TwoSideRail"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Layout: ").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_LAYOUT_READOUT_TAG).assertIsDisplayed()
        assertThat(readNodeText(DEBUG_LAYOUT_READOUT_TAG))
            .isEqualTo("840x360dp | 90° | LOCKED | TwoSideRail")
    }

    @Test
    fun debugLayoutReadout_whenDefaultsUsed_displaysUnlockedCompactPortrait() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugLayoutReadout(
                    state = DebugLayoutReadoutState(
                        widthDp = 412,
                        heightDp = 915
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Layout: ").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_LAYOUT_READOUT_TAG).assertIsDisplayed()
        assertThat(readNodeText(DEBUG_LAYOUT_READOUT_TAG))
            .isEqualTo("412x915dp | 0° | UNLOCKED | CompactPortrait")
    }

    @Test
    fun debugLayoutReadout_appliesModifierOnlyToOuterRow_notChildTextNodes() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugLayoutReadout(
                    state = DebugLayoutReadoutState(widthDp = 412, heightDp = 915),
                    modifier = Modifier.testTag("row_debug_layout")
                )
            }
        }

        assertThat(
            composeTestRule.onAllNodesWithTag("row_debug_layout").fetchSemanticsNodes().size
        ).isEqualTo(1)
        assertThat(
            composeTestRule
                .onAllNodesWithTag("row_debug_layout", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size
        ).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun debugOverlay_whenLayoutReadoutStateIsNull_derivesLiveWindowDimensions() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugOverlay(
                    onChangeZoomRatio = {},
                    debugUiState = DebugUiState.Enabled.Closed(
                        currentPhysicalCameraId = "0",
                        currentLogicalCameraId = "0",
                        currentPrimaryZoomRatio = 1f,
                        debugHidingComponents = false
                    ),
                    debugController = FakeDebugController()
                )
            }
        }

        composeTestRule.onNodeWithTag(DEBUG_LAYOUT_READOUT_TAG).assertIsDisplayed()
        assertThat(readNodeText(DEBUG_LAYOUT_READOUT_TAG))
            .isEqualTo("360x800dp | 0° | UNLOCKED | CompactPortrait")
    }

    @Test
    fun debugOverlay_whenExplicitLayoutReadoutStateProvided_displaysExplicitState() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugOverlay(
                    onChangeZoomRatio = {},
                    debugUiState = DebugUiState.Enabled.Closed(
                        currentPhysicalCameraId = "0",
                        currentLogicalCameraId = "0",
                        currentPrimaryZoomRatio = 1f,
                        debugHidingComponents = false
                    ),
                    debugController = FakeDebugController(),
                    layoutReadoutState = DebugLayoutReadoutState(
                        widthDp = 840,
                        heightDp = 360,
                        rotationDegrees = 270,
                        isRotationLocked = true,
                        layoutModeName = "TwoSideRail"
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(DEBUG_LAYOUT_READOUT_TAG).assertIsDisplayed()
        assertThat(readNodeText(DEBUG_LAYOUT_READOUT_TAG))
            .isEqualTo("840x360dp | 270° | LOCKED | TwoSideRail")
    }

    @Test
    fun debugOverlay_whenDebugHidingComponentsIsTrue_hidesLayoutReadout() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugOverlay(
                    onChangeZoomRatio = {},
                    debugUiState = DebugUiState.Enabled.Closed(
                        currentPhysicalCameraId = "0",
                        currentLogicalCameraId = "0",
                        currentPrimaryZoomRatio = 1f,
                        debugHidingComponents = true
                    ),
                    debugController = FakeDebugController(),
                    layoutReadoutState = DebugLayoutReadoutState(widthDp = 412, heightDp = 915)
                )
            }
        }

        composeTestRule.onNodeWithTag(BTN_DEBUG_HIDE_COMPONENTS_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_LAYOUT_READOUT_TAG).assertDoesNotExist()
        assertThat(
            composeTestRule.onAllNodesWithTag(DEBUG_LAYOUT_READOUT_TAG).fetchSemanticsNodes()
        ).isEmpty()
    }

    private fun readNodeText(tag: String): String = composeTestRule
        .onNodeWithTag(tag)
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString(separator = "") { it.text }
}

private class FakeDebugController : DebugController {
    override fun toggleDebugHidingComponents() = Unit
    override fun toggleDebugOverlay() = Unit
    override fun setTestPattern(testPattern: TestPattern) = Unit
}
