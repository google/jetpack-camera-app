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
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.TestPattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose unit tests for [DebugLayoutReadout], [DebugFormFactor], [DebugTextBar]
 * modifier scoping, and [DebugOverlay] integration.
 */
@RunWith(RobolectricTestRunner::class)
class DebugLayoutReadoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun debugLayoutReadout_whenExplicitFormFactorProvided_displaysEachTaggedBlock() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugLayoutReadout(
                    state = DebugLayoutReadoutState(
                        widthDp = 673,
                        heightDp = 841,
                        formFactor = DebugFormFactor.FOLDABLE,
                        deviceRotation = DeviceRotation.Rotated270,
                        windowRotation = DeviceRotation.Natural
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Layout: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Form Factor: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Device Rotation: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Window Rotation: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Icon Rotation: ").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_LAYOUT_READOUT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_WINDOW_SIZE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_FORM_FACTOR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_DEVICE_ROTATION_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_WINDOW_ROTATION_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_ICON_ROTATION_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEBUG_ICON_ROTATION_INDICATOR_TAG).assertIsDisplayed()
        assertThat(readNodeText(DEBUG_WINDOW_SIZE_TAG)).isEqualTo("673x841dp")
        assertThat(readNodeText(DEBUG_FORM_FACTOR_TAG)).isEqualTo("FOLDABLE")
        assertThat(readNodeText(DEBUG_DEVICE_ROTATION_TAG)).isEqualTo("270°")
        assertThat(readNodeText(DEBUG_WINDOW_ROTATION_TAG)).isEqualTo("0°")
        assertThat(readNodeText(DEBUG_ICON_ROTATION_TAG)).isEqualTo("90°")
        assertThat(readNodeStateDescription(DEBUG_ICON_ROTATION_INDICATOR_TAG)).isEqualTo("90°")
    }

    @Test
    fun debugLayoutReadout_whenDefaultFormFactorUsed_derivesFormFactorAndDefaultRotations() {
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
        assertThat(readNodeText(DEBUG_WINDOW_SIZE_TAG)).isEqualTo("412x915dp")
        assertThat(readNodeText(DEBUG_FORM_FACTOR_TAG)).isEqualTo("COMPACT")
        assertThat(readNodeText(DEBUG_DEVICE_ROTATION_TAG)).isEqualTo("0°")
        assertThat(readNodeText(DEBUG_WINDOW_ROTATION_TAG)).isEqualTo("0°")
        assertThat(readNodeText(DEBUG_ICON_ROTATION_TAG)).isEqualTo("0°")
    }

    @Test
    fun debugLayoutReadout_appliesModifierOnlyToOuterContainer_notChildTextNodes() {
        composeTestRule.setContent {
            MaterialTheme {
                DebugLayoutReadout(
                    state = DebugLayoutReadoutState(widthDp = 412, heightDp = 915),
                    modifier = Modifier.testTag("custom_row_tag")
                )
            }
        }

        assertThat(
            composeTestRule.onAllNodesWithTag("custom_row_tag").fetchSemanticsNodes().size
        ).isEqualTo(1)
        assertThat(
            composeTestRule
                .onAllNodesWithTag("custom_row_tag", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size
        ).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun debugOverlay_whenLayoutReadoutStateIsNull_derivesLiveWindowDimensionsAndFormFactor() {
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
        assertThat(readNodeText(DEBUG_WINDOW_SIZE_TAG)).isEqualTo("360x800dp")
        assertThat(readNodeText(DEBUG_FORM_FACTOR_TAG)).isEqualTo("COMPACT")
        assertThat(readNodeText(DEBUG_DEVICE_ROTATION_TAG)).isEqualTo("0°")
        assertThat(readNodeText(DEBUG_WINDOW_ROTATION_TAG)).isEqualTo("0°")
        assertThat(readNodeText(DEBUG_ICON_ROTATION_TAG)).isEqualTo("0°")
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
                        widthDp = 800,
                        heightDp = 1280,
                        deviceRotation = DeviceRotation.Rotated90,
                        windowRotation = DeviceRotation.Natural
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(DEBUG_LAYOUT_READOUT_TAG).assertIsDisplayed()
        assertThat(readNodeText(DEBUG_WINDOW_SIZE_TAG)).isEqualTo("800x1280dp")
        assertThat(readNodeText(DEBUG_FORM_FACTOR_TAG)).isEqualTo("TABLET")
        assertThat(readNodeText(DEBUG_DEVICE_ROTATION_TAG)).isEqualTo("90°")
        assertThat(readNodeText(DEBUG_WINDOW_ROTATION_TAG)).isEqualTo("0°")
        assertThat(readNodeText(DEBUG_ICON_ROTATION_TAG)).isEqualTo("270°")
        assertThat(readNodeStateDescription(DEBUG_ICON_ROTATION_INDICATOR_TAG)).isEqualTo("270°")
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
        composeTestRule.onNodeWithTag(DEBUG_WINDOW_SIZE_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEBUG_FORM_FACTOR_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEBUG_DEVICE_ROTATION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEBUG_WINDOW_ROTATION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEBUG_ICON_ROTATION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEBUG_ICON_ROTATION_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun debugFormFactor_fromWindowDp_classifiesAllFormFactorBuckets() {
        assertThat(DebugFormFactor.fromWindowDp(412, 412))
            .isEqualTo(DebugFormFactor.COMPACT_SQUARE)
        assertThat(DebugFormFactor.fromWindowDp(500, 360))
            .isEqualTo(DebugFormFactor.COMPACT_SQUARE)
        assertThat(DebugFormFactor.fromWindowDp(433, 346))
            .isEqualTo(DebugFormFactor.COMPACT_SQUARE)
        assertThat(DebugFormFactor.fromWindowDp(346, 433))
            .isEqualTo(DebugFormFactor.COMPACT_SQUARE)

        assertThat(DebugFormFactor.fromWindowDp(276, 883))
            .isEqualTo(DebugFormFactor.COMPACT_SLIM)
        assertThat(DebugFormFactor.fromWindowDp(883, 276))
            .isEqualTo(DebugFormFactor.COMPACT_SLIM)
        assertThat(DebugFormFactor.fromWindowDp(250, 602))
            .isEqualTo(DebugFormFactor.COMPACT_SLIM)

        assertThat(DebugFormFactor.fromWindowDp(412, 915))
            .isEqualTo(DebugFormFactor.COMPACT)
        assertThat(DebugFormFactor.fromWindowDp(840, 360))
            .isEqualTo(DebugFormFactor.COMPACT)
        assertThat(DebugFormFactor.fromWindowDp(512, 650))
            .isEqualTo(DebugFormFactor.COMPACT)
        assertThat(DebugFormFactor.fromWindowDp(566, 833))
            .isEqualTo(DebugFormFactor.COMPACT)
        assertThat(DebugFormFactor.fromWindowDp(494, 602))
            .isEqualTo(DebugFormFactor.COMPACT)
        assertThat(DebugFormFactor.fromWindowDp(383, 602))
            .isEqualTo(DebugFormFactor.COMPACT)
        assertThat(DebugFormFactor.fromWindowDp(585, 602))
            .isEqualTo(DebugFormFactor.COMPACT)
        assertThat(DebugFormFactor.fromWindowDp(480, 760))
            .isEqualTo(DebugFormFactor.COMPACT)

        assertThat(DebugFormFactor.fromWindowDp(673, 841))
            .isEqualTo(DebugFormFactor.FOLDABLE)
        assertThat(DebugFormFactor.fromWindowDp(841, 673))
            .isEqualTo(DebugFormFactor.FOLDABLE)
        assertThat(DebugFormFactor.fromWindowDp(850, 900))
            .isEqualTo(DebugFormFactor.FOLDABLE)

        assertThat(DebugFormFactor.fromWindowDp(800, 1280))
            .isEqualTo(DebugFormFactor.TABLET)
        assertThat(DebugFormFactor.fromWindowDp(1280, 800))
            .isEqualTo(DebugFormFactor.TABLET)

        assertThat(DebugFormFactor.fromWindowDp(0, 800))
            .isEqualTo(DebugFormFactor.UNKNOWN)
        assertThat(DebugFormFactor.fromWindowDp(412, -1))
            .isEqualTo(DebugFormFactor.UNKNOWN)
    }

    private fun readNodeText(tag: String): String = composeTestRule
        .onNodeWithTag(tag)
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString(separator = "") { it.text }

    private fun readNodeStateDescription(tag: String): String = composeTestRule
        .onNodeWithTag(tag)
        .fetchSemanticsNode()
        .config[SemanticsProperties.StateDescription]
}

private class FakeDebugController : DebugController {
    override fun toggleDebugHidingComponents() = Unit
    override fun toggleDebugOverlay() = Unit
    override fun setTestPattern(testPattern: TestPattern) = Unit
}
