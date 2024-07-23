/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.settings

import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.ui.DEVICE_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.FPS_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.LENS_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.STABILIZATION_UNSUPPORTED_TAG

sealed interface SingleSelectableState {
    data object Selectable : SingleSelectableState
    data class Disabled(val disabledRationale: Set<DisabledRationale>) : SingleSelectableState {
        init {
            require(disabledRationale.isNotEmpty())
        }
    }
}

/* Settings that currently have constraints **/
sealed interface FpsUiState {
    data class Enabled(
        val currentSelection: Int,
        val fpsAutoState: SingleSelectableState,
        val fpsFifteenState: SingleSelectableState,
        val fpsThirtyState: SingleSelectableState,
        val fpsSixtyState: SingleSelectableState,
        // Contains text like "Selected FPS only supported by rear lens"
        val additionalContext: String = "",
    ) : FpsUiState

    // FPS selection completely disabled. Cannot open dialog.
    data class Disabled(val disabledRationale: Set<DisabledRationale>) : FpsUiState {
        init {
            require(disabledRationale.isNotEmpty())
        }
    }
}

sealed interface FlipLensUiState {
    val isDefaultToFront: Boolean

    data class Enabled(
        override val isDefaultToFront: Boolean,
    ) : FlipLensUiState

    data class Disabled(
        override val isDefaultToFront: Boolean,
        val disabledRationale: Set<DisabledRationale>
    ) : FlipLensUiState {
        init {
            require(disabledRationale.isNotEmpty())
        }
    }
}

sealed interface StabilizationUiState {
    data class Enabled(
        val currentPreviewStabilization: Stabilization,
        val currentVideoStabilization: Stabilization,
        val stabilizationOnState: SingleSelectableState,
        val stabilizationHighQualityState: SingleSelectableState,
        // Contains text like "Selected stabilization mode only supported by rear lens"
        val additionalContext: String = ""
    ) : StabilizationUiState

    // Stabilization selection completely disabled. Cannot open dialog.
    data class Disabled(val disabledRationale: Set<DisabledRationale>) : StabilizationUiState
}


/* Settings that don't currently depend on constraints */

// this could be constrained w/ a check to see if a torch is available?
sealed interface FlashUiState {
    data class Enabled(
        val currentFlashMode: FlashMode,
        val additionalContext: String = ""
    ) : FlashUiState
}

sealed interface AspectRatioUiState {
    data class Enabled(
        val currentAspectRatio: AspectRatio,
        val additionalContext: String = ""
    ) : AspectRatioUiState
}

sealed interface CaptureModeUiState {
    data class Enabled(
        val currentCaptureMode: CaptureMode,
        val additionalContext: String = ""
    ) : CaptureModeUiState
}

sealed interface DarkModeUiState {
    data class Enabled(
        val currentDarkMode: DarkMode,
        val additionalContext: String = ""
    ) : DarkModeUiState
}

enum class DisabledRationale(val testTag: String, val reasonTextResId: Int) {
    DEVICE_UNSUPPORTED(
        DEVICE_UNSUPPORTED_TAG,
        R.string.device_unsupported
    ),
    FPS(FPS_UNSUPPORTED_TAG, R.string.fps_unsupported),
    STABILIZATION_UNSUPPORTED(STABILIZATION_UNSUPPORTED_TAG, R.string.stabilization_unsupported),
    LENS_UNSUPPORTED(LENS_UNSUPPORTED_TAG, R.string.lens_unsupported)
}
