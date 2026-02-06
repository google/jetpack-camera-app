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
package com.google.jetpackcamera.ui.uistateadapter.capture

import android.util.Size
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState

/**
 * Creates a [DebugUiState.Enabled.Open] state from various camera and application sources.
 *
 * This function is responsible for gathering all necessary information to populate the fully
 * expanded debug UI overlay. It combines system capabilities, current settings, and real-time
 * camera state into a comprehensive UI state object.
 *
 * @param systemConstraints The constraints of the device's camera system, used to determine
 * available test patterns.
 * @param cameraAppSettings The current application and camera settings, used to get the selected
 * test pattern and current lens.
 * @param cameraState The real-time state from the camera, providing information like video
 * resolution, camera IDs, and zoom ratio.
 * @param debugHidingComponents A boolean indicating if non-debug UI components are currently hidden.
 * @param cameraPropertiesJSON A JSON string representing the detailed properties of the camera.
 *
 * @return A [DebugUiState.Enabled.Open] object fully populated with the current debug information.
 */
fun DebugUiState.Enabled.Open.Companion.from(
    systemConstraints: CameraSystemConstraints,
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState,
    debugHidingComponents: Boolean,
    cameraPropertiesJSON: String
): DebugUiState.Enabled {
    val availableTestPatterns = buildSet {
        systemConstraints.forCurrentLens(cameraAppSettings)?.supportedTestPatterns
            ?.forEach {
                if (it is TestPattern.SolidColor) {
                    addAll(TestPattern.SolidColor.PREDEFINED_COLORS)
                } else {
                    add(it)
                }
            }
    }

    return DebugUiState.Enabled.Open(
        cameraPropertiesJSON = cameraPropertiesJSON,
        videoResolution = Size(
            cameraState.videoQualityInfo.width,
            cameraState.videoQualityInfo.height
        ),
        currentPhysicalCameraId = cameraState.debugInfo.physicalCameraId,
        currentLogicalCameraId = cameraState.debugInfo.logicalCameraId,
        selectedTestPattern = cameraAppSettings.debugSettings.testPattern,
        availableTestPatterns = availableTestPatterns,
        currentPrimaryZoomRatio = cameraState.zoomRatios[cameraAppSettings.cameraLensFacing],
        debugHidingComponents = debugHidingComponents
    )
}

/**
 * Creates a [DebugUiState.Enabled.Closed] state from the current camera state.
 *
 * This function gathers the minimal information needed for the collapsed (or partially visible)
 * debug UI overlay. It provides key details like camera IDs and zoom ratio without the overhead
of
 * the full, open debug view.
 *
 * @param cameraState The real-time state from the camera, providing camera IDs and zoom ratio.
 * @param lensFacing The currently active [LensFacing] value.
 * @param debugHidingComponents A boolean indicating if non-debug UI components are currently hidden.
 *
 * @return A [DebugUiState.Enabled.Closed] object with essential debug information.
 */
fun DebugUiState.Enabled.Closed.Companion.from(
    cameraState: CameraState,
    lensFacing: LensFacing,
    debugHidingComponents: Boolean
) = DebugUiState.Enabled.Closed(
    currentPhysicalCameraId = cameraState.debugInfo.physicalCameraId,
    currentLogicalCameraId = cameraState.debugInfo.logicalCameraId,
    currentPrimaryZoomRatio = cameraState.zoomRatios[lensFacing],
    debugHidingComponents = debugHidingComponents
)
