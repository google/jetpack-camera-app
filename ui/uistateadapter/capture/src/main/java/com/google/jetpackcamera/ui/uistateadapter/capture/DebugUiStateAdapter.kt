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
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

/**
 * Creates a [Flow] of [DebugUiState] for the debug overlay.
 *
 * This function combines various data sources to construct a comprehensive [DebugUiState]
 * that drives the UI of the debug information overlay. It reacts to changes in camera settings,
 * system constraints, camera state, and user interactions with the debug UI.
 *
 * @param cameraSystem The [CameraSystem] providing real-time camera state and settings.
 * @param constraintsRepository The [ConstraintsRepository] for accessing system-wide constraints.
 * @param debugSettings The current debug-specific settings.
 * @param cameraPropertiesJSON A JSON string containing detailed camera properties for display.
 * @param trackedCaptureUiState A [MutableStateFlow] representing user-interacted UI state,
 * such as whether the debug overlay is open.
 *
 * @return A [Flow] that emits a new [DebugUiState] whenever any of its underlying data
 * sources change.
 */
fun debugUiState(
    cameraSystem: CameraSystem,
    constraintsRepository: ConstraintsRepository,
    debugSettings: DebugSettings,
    cameraPropertiesJSON: String,
    trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>
): Flow<DebugUiState> {
    return combine(
        cameraSystem.getCurrentSettings().filterNotNull(),
        constraintsRepository.systemConstraints.filterNotNull(),
        cameraSystem.getCurrentCameraState(),
        trackedCaptureUiState
    ) { cameraAppSettings, systemConstraints, cameraState, trackedUiState ->
        DebugUiState.from(
            systemConstraints,
            cameraAppSettings,
            cameraState,
            trackedUiState.isDebugOverlayOpen,
            trackedUiState.debugHidingComponents,
            debugSettings,
            cameraPropertiesJSON
        )
    }
}

/**
 * Constructs a [DebugUiState] based on the current camera and application state.
 *
 * This is a private factory function that determines whether the debug UI should be fully
 * enabled and open, enabled but closed, or completely disabled. It gathers and transforms
 * the necessary data for the UI.
 *
 * @param systemConstraints The system-level constraints for the current camera.
 * @param cameraAppSettings The current application-level camera settings.
 * @param cameraState The real-time state of the camera.
 * @param isDebugOverlayOpen Indicates whether the user has opened the debug overlay.
 * @param debugHidingComponents Indicates whether UI components are being hidden for debugging.
 * @param debugSettings The general debug settings, e.g., if debug mode is on.
 * @param cameraPropertiesJSON A JSON string containing detailed camera properties for display.
 *
 * @return An appropriate [DebugUiState] which can be [DebugUiState.Enabled.Open],
 * [DebugUiState.Enabled.Closed], or [DebugUiState.Disabled].
 */
fun DebugUiState.Companion.from(
    systemConstraints: CameraSystemConstraints,
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState,
    isDebugOverlayOpen: Boolean,
    debugHidingComponents: Boolean,
    debugSettings: DebugSettings,
    cameraPropertiesJSON: String
): DebugUiState = if (debugSettings.isDebugModeEnabled) {
    if (isDebugOverlayOpen) {
        getEnabledDebugUiState(
            systemConstraints,
            cameraAppSettings,
            cameraState,
            debugHidingComponents,
            cameraPropertiesJSON
        )
    } else {
        getClosedDebugUiState(
            cameraState,
            cameraAppSettings.cameraLensFacing,
            debugHidingComponents
        )
    }
} else {
    DebugUiState.Disabled
}

private fun getEnabledDebugUiState(
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

private fun getClosedDebugUiState(
    cameraState: CameraState,
    lensFacing: LensFacing,
    debugHidingComponents: Boolean
) = DebugUiState.Enabled.Closed(
    currentPhysicalCameraId = cameraState.debugInfo.physicalCameraId,
    currentLogicalCameraId = cameraState.debugInfo.logicalCameraId,
    currentPrimaryZoomRatio = cameraState.zoomRatios[lensFacing],
    debugHidingComponents = debugHidingComponents
)
