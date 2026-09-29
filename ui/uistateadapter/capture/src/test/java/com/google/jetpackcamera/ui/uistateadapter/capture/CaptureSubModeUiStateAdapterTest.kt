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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.UNLIMITED_VIDEO_DURATION
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureSubModeUiStateAdapterTest {

    private val nightModeId = CaptureSubModeId("night_mode")
    private val portraitId = CaptureSubModeId("portrait_mode")
    private val concurrentId = CaptureSubModeId("concurrent_camera")

    private val nightDescriptor = CaptureSubModeDescriptor(
        id = nightModeId,
        parentCaptureMode = CaptureMode.IMAGE_ONLY,
        labelResId = 1001,
        sortOrder = 100
    )
    private val portraitDescriptor = CaptureSubModeDescriptor(
        id = portraitId,
        parentCaptureMode = CaptureMode.IMAGE_ONLY,
        labelResId = 1002,
        sortOrder = -50
    )
    private val concurrentDescriptor = CaptureSubModeDescriptor(
        id = concurrentId,
        parentCaptureMode = CaptureMode.VIDEO_ONLY,
        labelResId = 1003,
        sortOrder = 100
    )

    @Test
    fun from_whenNoSubModesSupportedForParent_returnsUnavailable() {
        val uiState = CaptureSubModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            videoRecordingState = VideoRecordingState.Inactive()
        )

        assertThat(uiState).isEqualTo(CaptureSubModeUiState.Unavailable)
    }

    @Test
    fun from_whenSubModesSupportedForImageOnly_prependsDefaultAndSortsByOrder() {
        val backConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[
            LensFacing.BACK
        ]!!.copy(
            supportedCaptureSubModes = setOf(CaptureSubModeId.DEFAULT, nightModeId)
        )
        val systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
            perLensConstraints = mapOf(LensFacing.BACK to backConstraints),
            captureSubModeDescriptors = mapOf(nightModeId to nightDescriptor)
        )

        val uiState = CaptureSubModeUiState.from(
            systemConstraints = systemConstraints,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = LensFacing.BACK,
                captureMode = CaptureMode.IMAGE_ONLY,
                captureSubModeId = CaptureSubModeId.DEFAULT
            ),
            videoRecordingState = VideoRecordingState.Inactive()
        )

        assertThat(uiState).isInstanceOf(CaptureSubModeUiState.Available::class.java)
        val available = uiState as CaptureSubModeUiState.Available
        assertThat(available.selectedSubMode).isEqualTo(CaptureSubModeId.DEFAULT)
        assertThat(available.availableSubModes.map { it.value.id })
            .containsExactly(CaptureSubModeId.DEFAULT, nightModeId)
            .inOrder()
    }

    @Test
    fun from_whenNegativeAndPositiveSortOrders_placesLeftAndRightOfDefaultDeterministically() {
        val backConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[
            LensFacing.BACK
        ]!!.copy(
            supportedCaptureSubModes = setOf(
                CaptureSubModeId.DEFAULT,
                nightModeId,
                portraitId
            )
        )
        val systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
            perLensConstraints = mapOf(LensFacing.BACK to backConstraints),
            captureSubModeDescriptors = mapOf(
                nightModeId to nightDescriptor,
                portraitId to portraitDescriptor
            )
        )

        val uiState = CaptureSubModeUiState.from(
            systemConstraints = systemConstraints,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = LensFacing.BACK,
                captureMode = CaptureMode.IMAGE_ONLY,
                captureSubModeId = nightModeId
            ),
            videoRecordingState = VideoRecordingState.Inactive()
        )

        val available = uiState as CaptureSubModeUiState.Available
        assertThat(available.selectedSubMode).isEqualTo(nightModeId)
        assertThat(available.availableSubModes.map { it.value.id })
            .containsExactly(portraitId, CaptureSubModeId.DEFAULT, nightModeId)
            .inOrder()
    }

    @Test
    fun from_whenFeaturePolicyConflictsWithSubModePolicy_excludesIncompatibleSubMode() {
        val backConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[
            LensFacing.BACK
        ]!!.copy(
            supportedCaptureSubModes = setOf(CaptureSubModeId.DEFAULT, nightModeId)
        )
        val subModePolicy = CameraFeaturePolicy(
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            )
        )
        val systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
            perLensConstraints = mapOf(LensFacing.BACK to backConstraints),
            captureSubModeDescriptors = mapOf(nightModeId to nightDescriptor),
            captureSubModePolicies = mapOf(nightModeId to subModePolicy)
        )
        val hostPolicy = CameraFeaturePolicy(
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.HLG10,
                visibility = OptionVisibility.Hidden
            )
        )

        val uiState = CaptureSubModeUiState.from(
            systemConstraints = systemConstraints,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = LensFacing.BACK,
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            videoRecordingState = VideoRecordingState.Inactive(),
            cameraFeaturePolicy = hostPolicy
        )

        assertThat(uiState).isEqualTo(CaptureSubModeUiState.Unavailable)
    }

    @Test
    fun from_whenParentCaptureModeChanges_showsOnlyMatchingParentSubModes() {
        val backConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[
            LensFacing.BACK
        ]!!.copy(
            supportedCaptureSubModes = setOf(
                CaptureSubModeId.DEFAULT,
                nightModeId,
                concurrentId
            )
        )
        val systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
            perLensConstraints = mapOf(LensFacing.BACK to backConstraints),
            captureSubModeDescriptors = mapOf(
                nightModeId to nightDescriptor,
                concurrentId to concurrentDescriptor
            )
        )

        val imageModeState = CaptureSubModeUiState.from(
            systemConstraints = systemConstraints,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = LensFacing.BACK,
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            videoRecordingState = VideoRecordingState.Inactive()
        ) as CaptureSubModeUiState.Available
        assertThat(imageModeState.availableSubModes.map { it.value.id })
            .containsExactly(CaptureSubModeId.DEFAULT, nightModeId)
            .inOrder()

        val videoModeState = CaptureSubModeUiState.from(
            systemConstraints = systemConstraints,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = LensFacing.BACK,
                captureMode = CaptureMode.VIDEO_ONLY
            ),
            videoRecordingState = VideoRecordingState.Inactive()
        ) as CaptureSubModeUiState.Available
        assertThat(videoModeState.availableSubModes.map { it.value.id })
            .containsExactly(CaptureSubModeId.DEFAULT, concurrentId)
            .inOrder()
    }

    @Test
    fun from_whenVideoRecordingActive_returnsUnavailable() {
        val backConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[
            LensFacing.BACK
        ]!!.copy(
            supportedCaptureSubModes = setOf(CaptureSubModeId.DEFAULT, concurrentId)
        )
        val systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
            perLensConstraints = mapOf(LensFacing.BACK to backConstraints),
            captureSubModeDescriptors = mapOf(concurrentId to concurrentDescriptor)
        )
        val recordingState = VideoRecordingState.Active.Recording(
            maxDurationMillis = UNLIMITED_VIDEO_DURATION,
            audioStreamState = AudioStreamState.Active(0.0),
            elapsedTimeNanos = 1000L
        )

        val uiState = CaptureSubModeUiState.from(
            systemConstraints = systemConstraints,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = LensFacing.BACK,
                captureMode = CaptureMode.VIDEO_ONLY
            ),
            videoRecordingState = recordingState
        )

        assertThat(uiState).isEqualTo(CaptureSubModeUiState.Unavailable)
    }
}
