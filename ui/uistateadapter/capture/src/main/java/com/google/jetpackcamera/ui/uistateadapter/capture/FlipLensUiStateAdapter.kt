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

import com.example.uistateadapter.Utils
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState

private val ORDERED_UI_SUPPORTED_LENS_FACINGS = listOf(
    LensFacing.FRONT,
    LensFacing.BACK

)

fun FlipLensUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: SystemConstraints
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

private fun createFrom(
    selectedLensFacing: LensFacing,
    supportedLensFacings: Set<LensFacing>
): FlipLensUiState {
    // Ensure we at least support one flash mode
    check(supportedLensFacings.isNotEmpty()) {
        "No lens supported."
    }

    val availableLensFacings =
        Utils.getSelectableListFromValues(
            supportedLensFacings,
            ORDERED_UI_SUPPORTED_LENS_FACINGS
        )

    return FlipLensUiState.Available(
        selectedLensFacing = selectedLensFacing,
        availableLensFacings = availableLensFacings
    )
}
