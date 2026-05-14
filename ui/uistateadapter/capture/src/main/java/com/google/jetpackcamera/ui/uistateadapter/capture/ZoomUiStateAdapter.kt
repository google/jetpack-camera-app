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

import android.util.Range
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState

/**
 * Creates a [ZoomUiState] from various camera and application sources.
 *
 * This function is responsible for creating the UI state for zoom-related information, such as the
 * zoom slider. It gathers the supported zoom range for the current lens and combines it with the
 * camera's real-time zoom ratio and linear zoom value.
 *
 * @param systemConstraints The capabilities of the device's camera hardware, used to find the
 *   supported zoom range for the current lens.
 * @param lensFacing The currently active [LensFacing] to look up the correct constraints and state.
 * @param cameraState The real-time state from the camera, providing the current zoom ratio and
 *   linear zoom value.
 * @return A [ZoomUiState.Enabled] object containing the zoom range, current zoom ratio, and current
 *   linear zoom value for the UI.
 */
fun ZoomUiState.Companion.from(
    systemConstraints: CameraSystemConstraints,
    lensFacing: LensFacing,
    cameraState: CameraState
): ZoomUiState = ZoomUiState.Enabled(
    primaryZoomRange =
    systemConstraints.perLensConstraints[lensFacing]?.supportedZoomRange
        ?: Range<Float>(1f, 1f),
    primaryZoomRatio = cameraState.zoomRatios[lensFacing],
    primaryLinearZoom = cameraState.linearZoomScales[lensFacing]
)
