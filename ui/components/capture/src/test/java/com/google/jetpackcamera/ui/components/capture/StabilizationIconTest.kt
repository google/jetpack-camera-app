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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests that [StabilizationIcon] exposes [STABILIZATION_TAG] and its content description on one
 * node, so that a UI test can look up the tag and read which stabilization mode is in use.
 *
 * The descriptions are asserted as literal English text because automated UI tests outside this
 * project match them verbatim.
 */
@RunWith(RobolectricTestRunner::class)
class StabilizationIconTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun auto_usingPreviewStabilization_describesAutoAndModeInUse() {
        setIndicatorPillContent(StabilizationUiState.Auto(StabilizationMode.ON))

        composeTestRule.onNodeWithTag(STABILIZATION_TAG)
            .assertContentDescriptionEquals("Auto stabilization: Preview and video are stabilized")
    }

    @Test
    fun auto_usingOpticalStabilization_describesAutoAndModeInUse() {
        setIndicatorPillContent(StabilizationUiState.Auto(StabilizationMode.OPTICAL))

        composeTestRule.onNodeWithTag(STABILIZATION_TAG)
            .assertContentDescriptionEquals("Auto stabilization: Optical stabilization is Enabled")
    }

    @Test
    fun on_describesPreviewAndVideoStabilization() {
        setIndicatorPillContent(StabilizationUiState.Specific(StabilizationMode.ON))

        composeTestRule.onNodeWithTag(STABILIZATION_TAG)
            .assertContentDescriptionEquals("Preview and video are stabilized")
    }

    @Test
    fun highQuality_describesVideoStabilization() {
        setIndicatorPillContent(StabilizationUiState.Specific(StabilizationMode.HIGH_QUALITY))

        composeTestRule.onNodeWithTag(STABILIZATION_TAG)
            .assertContentDescriptionEquals("Only Video is Stabilized")
    }

    @Test
    fun optical_describesOpticalStabilization() {
        setIndicatorPillContent(StabilizationUiState.Specific(StabilizationMode.OPTICAL))

        composeTestRule.onNodeWithTag(STABILIZATION_TAG)
            .assertContentDescriptionEquals("Optical stabilization is Enabled")
    }

    @Test
    fun off_describesStabilizationOff() {
        setIndicatorPillContent(StabilizationUiState.Off)

        composeTestRule.onNodeWithTag(STABILIZATION_TAG)
            .assertContentDescriptionEquals("Stabilization is off")
    }

    @Test
    fun selectedModeNotInUse_isNotShown() {
        setIndicatorPillContent(
            StabilizationUiState.Specific(StabilizationMode.ON, active = false)
        )

        composeTestRule.onNodeWithTag(STABILIZATION_TAG).assertDoesNotExist()
    }

    @Test
    fun disabled_isNotShown() {
        setIndicatorPillContent(StabilizationUiState.Disabled)

        composeTestRule.onNodeWithTag(STABILIZATION_TAG).assertDoesNotExist()
    }

    private fun setIndicatorPillContent(stabilizationUiState: StabilizationUiState) {
        composeTestRule.setContent {
            IndicatorPill(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                cutoutRectsOverride = emptyList()
            ) {
                StabilizationIcon(stabilizationUiState = stabilizationUiState)
            }
        }
    }
}
