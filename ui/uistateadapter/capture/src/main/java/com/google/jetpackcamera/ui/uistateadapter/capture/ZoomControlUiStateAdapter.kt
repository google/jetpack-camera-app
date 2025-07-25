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
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState

fun ZoomControlUiState.Companion.from(
    animateZoomState: Float?,
    systemConstraints: SystemConstraints,
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
