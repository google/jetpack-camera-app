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
