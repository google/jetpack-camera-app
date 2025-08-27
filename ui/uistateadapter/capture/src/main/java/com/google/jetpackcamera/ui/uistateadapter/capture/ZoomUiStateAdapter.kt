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
