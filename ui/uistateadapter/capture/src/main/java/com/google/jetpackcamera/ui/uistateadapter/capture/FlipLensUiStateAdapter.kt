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

import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistateadapter.Utils

private val ORDERED_UI_SUPPORTED_LENS_FACINGS = listOf(
    LensFacing.FRONT,
    LensFacing.BACK

)

/**
 * Creates a [FlipLensUiState] based on the current camera settings and system constraints.
 *
 * This function determines the UI state for the lens flipping control (e.g., front/back camera
 * button). It gathers the list of all physically available lenses from the system constraints
 * and combines it with the currently selected lens from the application settings.
 *
 * @param cameraAppSettings The current application settings, used to determine the currently
 * selected [LensFacing].
 * @param systemConstraints The hardware capabilities of the camera system, used to get the list
 * of all available lenses on the device.
 * @return A [FlipLensUiState.Available] object containing the currently selected lens and a list
 * of all available lenses for the UI to display.
 */
fun FlipLensUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints
): FlipLensUiState {
    val supportedLensFacings = systemConstraints.availableLenses.toSet()
    val availableLensFacings =
        Utils.getSelectableListFromValues(
            supportedLensFacings,
            ORDERED_UI_SUPPORTED_LENS_FACINGS
        )

    return FlipLensUiState.Available(
        selectedLensFacing = cameraAppSettings.cameraLensFacing,
        availableLensFacings = availableLensFacings
    )
}
