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

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistateadapter.Utils

private val ORDERED_UI_SUPPORTED_ASPECT_RATIOS = listOf(
    AspectRatio.NINE_SIXTEEN,
    AspectRatio.THREE_FOUR,
    AspectRatio.ONE_ONE
)

/**
 * Creates an [AspectRatioUiState] from [CameraAppSettings].
 *
 * @param cameraAppSettings The current camera application settings.
 * @param optionVisibility Optional developer visibility policy for aspect ratio.
 *
 * @return An [AspectRatioUiState] representing the available aspect ratios and the currently
 * selected one. If only one or no aspect ratios are supported or allowed, it returns
 * [AspectRatioUiState.Unavailable].
 */
fun AspectRatioUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    optionVisibility: OptionVisibility<AspectRatio>? = null
): AspectRatioUiState {
    if (optionVisibility is OptionVisibility.Hidden) {
        return AspectRatioUiState.Unavailable
    }

    val supportedAspectRatios = if (optionVisibility is OptionVisibility.Only) {
        ORDERED_UI_SUPPORTED_ASPECT_RATIOS.filter { it in optionVisibility.enabledOptions }.toSet()
    } else {
        ORDERED_UI_SUPPORTED_ASPECT_RATIOS.toSet()
    }

    return if (supportedAspectRatios.size <= 1) {
        AspectRatioUiState.Unavailable
    } else {
        val availableAspectRatios =
            Utils.getSelectableListFromValues(
                supportedAspectRatios,
                ORDERED_UI_SUPPORTED_ASPECT_RATIOS
            )
        val selectedAspectRatio = if (cameraAppSettings.aspectRatio in supportedAspectRatios) {
            cameraAppSettings.aspectRatio
        } else {
            supportedAspectRatios.first()
        }
        AspectRatioUiState.Available(
            selectedAspectRatio = selectedAspectRatio,
            availableAspectRatios = availableAspectRatios
        )
    }
}
