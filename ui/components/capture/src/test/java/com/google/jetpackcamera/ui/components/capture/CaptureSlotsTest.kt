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
import androidx.compose.foundation.layout.sizeIn
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
            isCompact = true,
            isOverViewfinder = true,
            isRecording = true,
            onDismissQuickSettings = { dismissedCount++ }
        )
        val offViewfinderScope = PreviewOverlaySlotScope(
            isOverViewfinder = false,
            orientation = Orientation.Horizontal,
            isCompact = false
        )

        assertThat(overViewfinderScope.orientation).isEqualTo(Orientation.Vertical)
        assertThat(overViewfinderScope.isCompact).isTrue()
        assertThat(overViewfinderScope.isOverViewfinder).isTrue()
        assertThat(overViewfinderScope.isRecording).isTrue()
        assertThat(overViewfinderScope.cameraControlContainerColor).isEqualTo(
            CameraControlBackgroundStyle.BLACK_60.containerColor
        )

        overViewfinderScope.dismissQuickSettings()
        assertThat(dismissedCount).isEqualTo(1)

        assertThat(offViewfinderScope.orientation).isEqualTo(Orientation.Horizontal)
        assertThat(offViewfinderScope.isCompact).isFalse()
        assertThat(offViewfinderScope.isOverViewfinder).isFalse()
        assertThat(offViewfinderScope.cameraControlContainerColor).isEqualTo(
            CameraControlBackgroundStyle.WHITE_20.containerColor
        )
    }

    @Test
    fun captureSlots_andPlaceholders_adaptAxisAndRecycleExistingTags() {
        val customSlots = CaptureSlots(
            topBar = { modifier ->
                Box(modifier.size(48.dp).testTag(VIDEO_QUALITY_TAG))
            },
            topStartContent = { modifier ->
                Box(modifier.size(24.dp).testTag(FLIP_CAMERA_BUTTON))
            }
        )
        val defaultPlaceholders = PlaceholderCaptureSlots.default()
        val taggedOverlayPlaceholders = PlaceholderCaptureSlots.sideRail(
            tagQuickSettingsOverlay = true
        )
        val slotScope = CaptureSlotScope(orientation = Orientation.Vertical)
        var useTaggedOverlay by mutableStateOf(false)

        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp)) {
                PlaceholderViewfinder()
                with(slotScope) {
                    customSlots.topBar(this, Modifier)
                    customSlots.topStartContent(this, Modifier)
                    defaultPlaceholders.zoomControls(
                        this,
                        Modifier.sizeIn(maxHeight = 40.dp)
                    )
                    if (useTaggedOverlay) {
                        taggedOverlayPlaceholders.quickSettingsOverlay(this, Modifier)
                    } else {
                        defaultPlaceholders.quickSettingsOverlay(this, Modifier)
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        val clampedZoomBounds = composeTestRule.onNodeWithTag(ZOOM_BUTTON_ROW_TAG).getBoundsInRoot()
        assertThat(clampedZoomBounds.height).isEqualTo(40.dp)

        useTaggedOverlay = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()
    }
}
