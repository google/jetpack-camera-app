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
package com.google.jetpackcamera.ui.uistateadapter.capture.compound

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class QuickSettingsUiStateAdapterTest {

    @Test
    fun from_correctlyBundlesStates() {
        val captureModeUiState = CaptureModeUiState.Unavailable
        val flashModeUiState = FlashModeUiState.Unavailable
        val flipLensUiState = FlipLensUiState.Unavailable
        val aspectRatioUiState = AspectRatioUiState.Unavailable
        val hdrUiState = HdrUiState.Unavailable
        val quickSettingsUiState = QuickSettingsUiState.from(
            captureModeUiState = captureModeUiState,
            flashModeUiState = flashModeUiState,
            flipLensUiState = flipLensUiState,
            aspectRatioUiState = aspectRatioUiState,
            hdrUiState = hdrUiState
        )

        assertThat(quickSettingsUiState).isInstanceOf(QuickSettingsUiState.Available::class.java)
        val availableState = quickSettingsUiState as QuickSettingsUiState.Available
        assertThat(availableState.captureModeUiState).isEqualTo(captureModeUiState)
        assertThat(availableState.flashModeUiState).isEqualTo(flashModeUiState)
        assertThat(availableState.flipLensUiState).isEqualTo(flipLensUiState)
        assertThat(availableState.aspectRatioUiState).isEqualTo(aspectRatioUiState)
        assertThat(availableState.hdrUiState).isEqualTo(hdrUiState)
        assertThat(availableState.titleResId).isNull()
    }

    @Test
    fun from_withTitleResId_forwardsTitleOverride() {
        val titleResId = 42
        val quickSettingsUiState = QuickSettingsUiState.from(
            captureModeUiState = CaptureModeUiState.Unavailable,
            flashModeUiState = FlashModeUiState.Unavailable,
            flipLensUiState = FlipLensUiState.Unavailable,
            aspectRatioUiState = AspectRatioUiState.Unavailable,
            hdrUiState = HdrUiState.Unavailable,
            titleResId = titleResId
        )

        assertThat((quickSettingsUiState as QuickSettingsUiState.Available).titleResId)
            .isEqualTo(titleResId)
    }

    @Test
    fun captureUiState_whenDefaultSubModeOverrideActive_enforcesPolicyAndKeepsParentTitle() =
        runBlocking {
            val nightId = CaptureSubModeId("night")
            val nightDescriptor = CaptureSubModeDescriptor(
                id = nightId,
                parentCaptureMode = CaptureMode.IMAGE_ONLY,
                labelResId = 1001,
                quickSettingsTitleResId = 2002
            )
            val nightPolicy = CameraFeaturePolicy(
                dynamicRange = SettingConfig(DynamicRange.SDR, OptionVisibility.Hidden),
                imageFormat = SettingConfig(ImageOutputFormat.JPEG, OptionVisibility.Hidden)
            )
            val backConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[LensFacing.BACK]!!
                .copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10),
                    supportedImageFormatsMap = mapOf(
                        true to setOf(ImageOutputFormat.JPEG),
                        false to setOf(ImageOutputFormat.JPEG, ImageOutputFormat.JPEG_ULTRA_HDR)
                    ),
                    supportedCaptureSubModes = setOf(nightId),
                    defaultCaptureSubModes = mapOf(CaptureMode.IMAGE_ONLY to nightId)
                )
            val constraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
                perLensConstraints = mapOf(LensFacing.BACK to backConstraints),
                captureSubModeDescriptors = mapOf(nightId to nightDescriptor),
                captureSubModePolicies = mapOf(nightId to nightPolicy)
            )
            val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = LensFacing.BACK,
                captureMode = CaptureMode.IMAGE_ONLY,
                captureSubModeId = CaptureSubModeId.DEFAULT,
                activeCaptureSubModeId = nightId
            )

            val readyState = captureUiState(
                currentSettings = MutableStateFlow(settings),
                systemConstraints = MutableStateFlow(constraints),
                currentCameraState = MutableStateFlow(CameraState()),
                trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState()),
                externalCaptureMode = ExternalCaptureMode.Standard
            ).first() as CaptureUiState.Ready

            assertThat(readyState.hdrUiState).isEqualTo(HdrUiState.Unavailable)
            val quickSettings =
                readyState.quickSettingsUiState as QuickSettingsUiState.Available
            assertThat(quickSettings.titleResId).isNull()
        }
}
