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
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState

private val ORDERED_UI_SUPPORTED_ASPECT_RATIOS = listOf(
    AspectRatio.NINE_SIXTEEN,
    AspectRatio.THREE_FOUR,
    AspectRatio.ONE_ONE
)

fun AspectRatioUiState.Companion.from(cameraAppSettings: CameraAppSettings): AspectRatioUiState {
    val supportedAspectRatios = ORDERED_UI_SUPPORTED_ASPECT_RATIOS.toSet()
    val availableAspectRatios =
        Utils.getSelectableListFromValues(
            supportedAspectRatios,
            ORDERED_UI_SUPPORTED_ASPECT_RATIOS
        )

    return if (supportedAspectRatios.size <= 1) {
        // If we only support one lens, then return "Unavailable".
        AspectRatioUiState.Unavailable
    } else {
        AspectRatioUiState.Available(
            selectedAspectRatio = cameraAppSettings.aspectRatio,
            availableAspectRatios = availableAspectRatios
        )
    }
}
