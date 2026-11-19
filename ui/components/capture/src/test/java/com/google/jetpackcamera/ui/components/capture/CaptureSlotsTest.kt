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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureSlotsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureRtlPolicy_defaultsToUnmirrored_andPropagatesThroughCompositionLocal() {
        var defaultPolicy: CaptureRtlPolicy? = null
        var overriddenPolicy: CaptureRtlPolicy? = null

        composeTestRule.setContent {
            defaultPolicy = LocalCaptureRtlPolicy.current
            CompositionLocalProvider(
                LocalCaptureRtlPolicy provides CaptureRtlPolicy(
                    mirrorSideRails = true,
                    mirrorCompactBottomBar = true
                )
            ) {
                overriddenPolicy = LocalCaptureRtlPolicy.current
            }
        }

        assertThat(defaultPolicy).isEqualTo(
            CaptureRtlPolicy(mirrorSideRails = false, mirrorCompactBottomBar = false)
        )
        assertThat(overriddenPolicy).isEqualTo(
            CaptureRtlPolicy(mirrorSideRails = true, mirrorCompactBottomBar = true)
        )
    }

    @Test
    fun captureSlotScope_exposesMetadata_overlapContainerColors_andDismissCallback() {
        var dismissedCount = 0
        val overViewfinderScope = CaptureSlotScope(
            orientation = Orientation.Vertical,
            controlSize = ControlSize.Mini,
            isOverViewfinder = true,
            isRecording = true,
            onDismissQuickSettings = { dismissedCount++ }
        )
        val offViewfinderScope = PreviewOverlaySlotScope(
            isOverViewfinder = false,
            orientation = Orientation.Horizontal,
            controlSize = ControlSize.Standard
        )

        assertThat(overViewfinderScope.orientation).isEqualTo(Orientation.Vertical)
        assertThat(overViewfinderScope.controlSize).isEqualTo(ControlSize.Mini)
        assertThat(overViewfinderScope.isOverViewfinder).isTrue()
        assertThat(overViewfinderScope.isRecording).isTrue()
        assertThat(overViewfinderScope.cameraControlContainerColor).isEqualTo(
            CameraControlBackgroundStyle.BLACK_60.containerColor
        )

        overViewfinderScope.dismissQuickSettings()
        assertThat(dismissedCount).isEqualTo(1)

        assertThat(offViewfinderScope.orientation).isEqualTo(Orientation.Horizontal)
        assertThat(offViewfinderScope.controlSize).isEqualTo(ControlSize.Standard)
        assertThat(offViewfinderScope.isOverViewfinder).isFalse()
        assertThat(offViewfinderScope.cameraControlContainerColor).isEqualTo(
            CameraControlBackgroundStyle.WHITE_20.containerColor
        )
    }

    @Test
    fun overlapAwareCaptureSlot_propagatesOrientationAndControlSize_andDefaultsOmittedSlots() {
        var observedOrientation: Orientation? = null
        var observedControlSize: ControlSize? = null
        val slots = CaptureSlots(
            topBar = { modifier ->
                observedOrientation = orientation
                observedControlSize = controlSize
                Box(modifier.size(48.dp).testTag(VIDEO_QUALITY_TAG))
            }
        )

        composeTestRule.setContent {
            OverlapAwareCaptureSlot(
                orientation = Orientation.Vertical,
                controlSize = ControlSize.Medium,
                content = slots.topBar
            )
            OverlapAwareCaptureSlot(content = slots.imageWell)
        }

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertDoesNotExist()
        assertThat(observedOrientation).isEqualTo(Orientation.Vertical)
        assertThat(observedControlSize).isEqualTo(ControlSize.Medium)
    }

    @Test
    fun captureButtonDefaults_diameterFor_mapsEachControlSize() {
        assertThat(CaptureButtonDefaults.diameterFor(ControlSize.Mini)).isEqualTo(56.dp)
        assertThat(CaptureButtonDefaults.diameterFor(ControlSize.Medium)).isEqualTo(74.dp)
        assertThat(CaptureButtonDefaults.diameterFor(ControlSize.Standard)).isEqualTo(86.dp)
    }

    @Test
    fun captureButton_insideOverlapAwareStyleProvider_sizesForEachControlSize() {
        var controlSize by mutableStateOf(ControlSize.Standard)
        val buttonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)

        composeTestRule.setContent {
            OverlapAwareStyleProvider(controlSize = controlSize) {
                CaptureButton(captureButtonUiState = buttonUiState)
            }
        }

        fun shutterHeight() = composeTestRule.onNodeWithTag(CAPTURE_BUTTON).getBoundsInRoot().height
        assertThat(shutterHeight().value).isWithin(0.5f).of(86f)

        controlSize = ControlSize.Medium
        composeTestRule.waitForIdle()
        assertThat(shutterHeight().value).isWithin(0.5f).of(74f)

        controlSize = ControlSize.Mini
        composeTestRule.waitForIdle()
        assertThat(shutterHeight().value).isWithin(0.5f).of(56f)
    }

    @Test
    fun captureScaffoldDefaults_captureButton_usesLocalControlSizeByDefault() {
        var controlSize by mutableStateOf(ControlSize.Standard)
        val buttonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)

        composeTestRule.setContent {
            OverlapAwareStyleProvider(controlSize = controlSize) {
                CaptureScaffoldDefaults.CaptureButton(
                    uiState = buttonUiState,
                    captureController = null
                )
            }
        }

        fun shutterHeight() = composeTestRule.onNodeWithTag(CAPTURE_BUTTON).getBoundsInRoot().height
        assertThat(shutterHeight().value).isWithin(0.5f).of(86f)

        controlSize = ControlSize.Medium
        composeTestRule.waitForIdle()
        assertThat(shutterHeight().value).isWithin(0.5f).of(74f)

        controlSize = ControlSize.Mini
        composeTestRule.waitForIdle()
        assertThat(shutterHeight().value).isWithin(0.5f).of(56f)
    }
}
