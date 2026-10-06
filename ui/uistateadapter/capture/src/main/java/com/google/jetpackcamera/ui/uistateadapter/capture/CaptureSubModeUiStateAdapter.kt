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

import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeOption
import com.google.jetpackcamera.ui.uistate.capture.CaptureSubModeUiState

/**
 * Derives [CaptureSubModeUiState] from the active parent [CaptureMode], lens constraints,
 * and feature policy.
 *
 * Behavior:
 * 1. If video recording is active or `externalCaptureMode != ExternalCaptureMode.Standard`,
 *    returns [CaptureSubModeUiState.Unavailable].
 * 2. Filters `CameraConstraints.supportedCaptureSubModes` (on the current lens) against
 *    `CameraSystemConstraints.captureSubModeDescriptors` whose `parentCaptureMode` matches
 *    `cameraAppSettings.captureMode`, and excludes any sub-mode whose `captureSubModePolicies`
 *    conflicts with `cameraFeaturePolicy` (`cameraFeaturePolicy.isCompatibleWith(subModePolicy)`).
 * 3. If no non-default sub-modes remain for the active parent `CaptureMode`, returns
 *    [CaptureSubModeUiState.Unavailable] (no single-pill carousel).
 * 4. Otherwise, synthesizes the `CaptureSubModeId.DEFAULT` descriptor (`sortOrder = 0`,
 *    with default label for `STANDARD` / `IMAGE_ONLY` / `VIDEO_ONLY`), sorts all descriptors
 *    by `(sortOrder, id.value)` so providers can place items either to the left (`sortOrder < 0`)
 *    or right (`sortOrder > 0`) of the default option, and returns [CaptureSubModeUiState.Available].
 */
fun CaptureSubModeUiState.Companion.from(
    systemConstraints: CameraSystemConstraints,
    cameraAppSettings: CameraAppSettings,
    videoRecordingState: VideoRecordingState,
    externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard,
    cameraFeaturePolicy: CameraFeaturePolicy? = null
): CaptureSubModeUiState {
    if (videoRecordingState !is VideoRecordingState.Inactive ||
        externalCaptureMode != ExternalCaptureMode.Standard
    ) {
        return CaptureSubModeUiState.Unavailable
    }

    val currentLensConstraints = systemConstraints.forCurrentLens(cameraAppSettings)
        ?: return CaptureSubModeUiState.Unavailable

    val parentCaptureMode = cameraAppSettings.captureMode
    val defaultOverrideId = currentLensConstraints.defaultCaptureSubModes[parentCaptureMode]

    val matchingSubModeDescriptors = currentLensConstraints.supportedCaptureSubModes
        .asSequence()
        .filter { it != CaptureSubModeId.DEFAULT && it != defaultOverrideId }
        .mapNotNull { id -> systemConstraints.captureSubModeDescriptors[id] }
        .filter { descriptor -> descriptor.parentCaptureMode == parentCaptureMode }
        .filter { descriptor ->
            val subModePolicy = systemConstraints.captureSubModePolicies[descriptor.id]
            if (cameraFeaturePolicy != null && subModePolicy != null) {
                cameraFeaturePolicy.isCompatibleWith(subModePolicy)
            } else {
                true
            }
        }
        .toList()

    if (matchingSubModeDescriptors.isEmpty()) {
        return CaptureSubModeUiState.Unavailable
    }

    val defaultLabelResId = when (parentCaptureMode) {
        CaptureMode.STANDARD -> R.string.capture_mode_standard_default
        CaptureMode.IMAGE_ONLY -> R.string.capture_mode_photo_default
        CaptureMode.VIDEO_ONLY -> R.string.capture_mode_video_default
    }

    val defaultDescriptor = CaptureSubModeDescriptor(
        id = CaptureSubModeId.DEFAULT,
        parentCaptureMode = parentCaptureMode,
        labelResId = defaultLabelResId,
        sortOrder = 0
    )

    val allDescriptors = (matchingSubModeDescriptors + defaultDescriptor)
        .sortedWith(compareBy<CaptureSubModeDescriptor> { it.sortOrder }.thenBy { it.id.value })

    val selectableOptions = allDescriptors.map { descriptor ->
        SingleSelectableUiState.SelectableUi(
            CaptureSubModeOption(
                id = descriptor.id,
                labelResId = descriptor.labelResId
            )
        )
    }

    val selectedId = if (allDescriptors.any { it.id == cameraAppSettings.captureSubModeId }) {
        cameraAppSettings.captureSubModeId
    } else {
        CaptureSubModeId.DEFAULT
    }

    return CaptureSubModeUiState.Available(
        selectedSubMode = selectedId,
        availableSubModes = selectableOptions
    )
}
