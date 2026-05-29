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
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState

/**
 * Creates a [ZoomControlUiState] from various camera and application sources.
 *
 * This function is responsible for creating the UI state for the zoom controls (e.g., the 0.5x, 1x,
 * 2x buttons). It determines the available zoom levels based on the hardware's supported zoom
 * range for the currently active lens.
 *
 * If the camera lens does not support zooming (i.e., the zoom range is a single value), this
 * function will return [ZoomControlUiState.Disabled]. Otherwise, it calculates a list of discrete
 * zoom levels to display to the user (e.g., 0.5x, 1x, 2x, 5x) based on the supported range.
 *
 * @param animateZoomState An optional target zoom ratio for an ongoing animation. If non-null, the
 *   UI can use this to show an animation progressing towards the target.
 * @param systemConstraints The capabilities of the device's camera hardware, used to find the
 *   supported zoom range for the current lens.
 * @param cameraAppSettings The current application settings, providing the selected lens facing
 *   and default zoom ratios.
 * @param cameraState The real-time state from the camera, providing the current actual zoom ratio.
 * @return A [ZoomControlUiState] which is either:
 * - [ZoomControlUiState.Enabled] containing the calculated zoom levels, current zoom ratio, and
 *   other parameters for the UI.
 * - [ZoomControlUiState.Disabled] if the current lens does not support zooming.
 */
fun ZoomControlUiState.Companion.from(
    animateZoomState: Float?,
    systemConstraints: CameraSystemConstraints,
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState
): ZoomControlUiState {
    val zoomRange =
        systemConstraints.perLensConstraints[cameraAppSettings.cameraLensFacing]
            ?.supportedZoomRange
            ?: Range(1f, 1f)

    if (zoomRange.upper == zoomRange.lower) {
        return ZoomControlUiState.Disabled
    }
    val zoomLevels: List<Float> = buildList {
        if (zoomRange.lower < 1f) {
            add(zoomRange.lower)
        }
        add(1f)
        if (zoomRange.contains(2f)) {
            add(2f)
        }
        if (zoomRange.contains(5f)) {
            add(5f)
        }
    }
    return ZoomControlUiState.Enabled(
        zoomLevels = zoomLevels,
        primaryLensFacing = cameraAppSettings.cameraLensFacing,
        initialZoomRatio = cameraAppSettings.defaultZoomRatios[cameraAppSettings.cameraLensFacing],
        primaryZoomRatio = cameraState.zoomRatios[cameraAppSettings.cameraLensFacing],
        primarySettingZoomRatio = cameraAppSettings
            .defaultZoomRatios[cameraAppSettings.cameraLensFacing],
        animatingToValue = animateZoomState
    )
}
