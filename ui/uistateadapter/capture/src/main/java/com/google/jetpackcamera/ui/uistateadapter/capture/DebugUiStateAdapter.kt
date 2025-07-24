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
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.TestPattern
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState

fun DebugUiState.Open.Companion.from(
    systemConstraints: SystemConstraints,
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState,
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

    return DebugUiState.Open(
        cameraPropertiesJSON = cameraPropertiesJSON,
        videoResolution = Size(
            cameraState.videoQualityInfo.width,
            cameraState.videoQualityInfo.height
        ),
        currentPhysicalCameraId = cameraState.debugInfo.physicalCameraId,
        currentLogicalCameraId = cameraState.debugInfo.logicalCameraId,
        selectedTestPattern = cameraAppSettings.debugSettings.testPattern,
        availableTestPatterns = availableTestPatterns
    )
}

fun DebugUiState.Closed.Companion.from(cameraState: CameraState) = DebugUiState.Closed(
    currentPhysicalCameraId = cameraState.debugInfo.physicalCameraId,
    currentLogicalCameraId = cameraState.debugInfo.logicalCameraId
)
