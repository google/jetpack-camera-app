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
package com.google.jetpackcamera.ui.uistateadapter.viewfinder

import com.example.uistateadapter.Utils
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.ui.uistate.viewfinder.AspectRatioUiState

object AspectRatioUiStateAdapter {
    private val ORDERED_UI_SUPPORTED_ASPECT_RATIOS = listOf(
        AspectRatio.NINE_SIXTEEN,
        AspectRatio.THREE_FOUR,
        AspectRatio.ONE_ONE
    )

    fun getUiState(cameraAppSettings: CameraAppSettings): AspectRatioUiState {
        return createFrom(
            cameraAppSettings.aspectRatio,
            ORDERED_UI_SUPPORTED_ASPECT_RATIOS.toSet()
        )
    }

    private fun createFrom(
        selectedAspectRatio: AspectRatio,
        supportedAspectRatios: Set<AspectRatio>
    ): AspectRatioUiState {
        // Ensure we at least support one flash mode
        check(supportedAspectRatios.isNotEmpty()) {
            "No aspect ratio supported."
        }

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
                selectedAspectRatio = selectedAspectRatio,
                availableAspectRatios = availableAspectRatios
            )
        }
    }
}
