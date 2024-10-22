/*
 * Copyright (C) 2023 The Android Open Source Project
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

import com.google.jetpackcamera.settings.DisabledRationale.DeviceUnsupportedRationale
import com.google.jetpackcamera.settings.DisabledRationale.LensUnsupportedRationale
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.ui.DEVICE_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.FPS_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.LENS_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.PERMISSION_RECORD_AUDIO_NOT_GRANTED_TAG
import com.google.jetpackcamera.settings.ui.STABILIZATION_UNSUPPORTED_TAG

const val FPS_AUTO = 0
const val FPS_15 = 15
const val FPS_30 = 30
const val FPS_60 = 60

// seconds duration in millis
const val UNLIMITED_VIDEO_DURATION = 0L
const val FIVE_SECONDS_DURATION = 5_000L
const val TEN_SECONDS_DURATION = 10_000L
const val THIRTY_SECONDS_DURATION = 30_000L
const val SIXTY_SECONDS_DURATION = 60_000L

/**
 * Defines the current state of the [SettingsScreen].
 */
sealed interface SettingsUiState {
    data object Disabled : SettingsUiState
    data class Enabled(
        val aspectRatioUiState: AspectRatioUiState,
        val captureModeUiState: CaptureModeUiState,
        val darkModeUiState: DarkModeUiState,
        val flashUiState: FlashUiState,
        val fpsUiState: FpsUiState,
        val lensFlipUiState: FlipLensUiState,
        val stabilizationUiState: StabilizationUiState,
        val maxVideoDurationUiState: MaxVideoDurationUiState.Enabled,
        val muteAudioUiState: MuteAudioUiState
    ) : SettingsUiState
}

/** State for the individual options on Popup dialog settings */
sealed interface SingleSelectableState {
    data object Selectable : SingleSelectableState
    data class Disabled(val disabledRationale: DisabledRationale) : SingleSelectableState
}

/** Contains information on why a setting is disabled */
// TODO(b/360921588): Display information on UI regarding disabled rationale
sealed interface DisabledRationale {
    val affectedSettingNameResId: Int
    val reasonTextResId: Int
    val testTag: String

    data class PermissionRecordAudioNotGrantedRationale(
        override val affectedSettingNameResId: Int
    ) : DisabledRationale {
        override val reasonTextResId: Int = R.string.permission_record_audio_unsupported
        override val testTag = PERMISSION_RECORD_AUDIO_NOT_GRANTED_TAG
    }

    /**
     * Text will be [affectedSettingNameResId] is [R.string.device_unsupported]
     */
    data class DeviceUnsupportedRationale(override val affectedSettingNameResId: Int) :
        DisabledRationale {
        override val reasonTextResId: Int = R.string.device_unsupported
        override val testTag = DEVICE_UNSUPPORTED_TAG
    }

    data class FpsUnsupportedRationale(
        override val affectedSettingNameResId: Int,
        val currentFps: Int
    ) : DisabledRationale {
        override val reasonTextResId: Int = R.string.fps_unsupported
        override val testTag = FPS_UNSUPPORTED_TAG
    }

    data class StabilizationUnsupportedRationale(override val affectedSettingNameResId: Int) :
        DisabledRationale {
        override val reasonTextResId = R.string.stabilization_unsupported
        override val testTag = STABILIZATION_UNSUPPORTED_TAG
    }

    sealed interface LensUnsupportedRationale : DisabledRationale {
        data class FrontLensUnsupportedRationale(override val affectedSettingNameResId: Int) :
            LensUnsupportedRationale {
            override val reasonTextResId: Int = R.string.front_lens_unsupported
            override val testTag = LENS_UNSUPPORTED_TAG
        }

        data class RearLensUnsupportedRationale(override val affectedSettingNameResId: Int) :
            LensUnsupportedRationale {
            override val reasonTextResId: Int = R.string.rear_lens_unsupported
            override val testTag = LENS_UNSUPPORTED_TAG
        }
    }
}

fun getLensUnsupportedRationale(
    lensFacing: LensFacing,
    affectedSettingNameResId: Int
): LensUnsupportedRationale {
    return when (lensFacing) {
        LensFacing.BACK -> LensUnsupportedRationale.RearLensUnsupportedRationale(
            affectedSettingNameResId
        )

        LensFacing.FRONT -> LensUnsupportedRationale.FrontLensUnsupportedRationale(
            affectedSettingNameResId
        )
    }
}

// ////////////////////////////////////////////////////////////
//
// Settings that currently depend on constraints
//
// ////////////////////////////////////////////////////////////

sealed interface FpsUiState {
    data class Enabled(
        val currentSelection: Int,
        val fpsAutoState: SingleSelectableState,
        val fpsFifteenState: SingleSelectableState,
        val fpsThirtyState: SingleSelectableState,
        val fpsSixtyState: SingleSelectableState,
        // Contains text like "Selected FPS only supported by rear lens"
        val additionalContext: String = ""
    ) : FpsUiState

    // FPS selection completely disabled. Cannot open dialog.
    data class Disabled(val disabledRationale: DisabledRationale) : FpsUiState
}

sealed interface FlipLensUiState {
    val currentLensFacing: LensFacing

    data class Enabled(
        override val currentLensFacing: LensFacing
    ) : FlipLensUiState

    data class Disabled(
        override val currentLensFacing: LensFacing,
        val disabledRationale: DisabledRationale
    ) : FlipLensUiState
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
    data class Disabled(val disabledRationale: DisabledRationale) : StabilizationUiState
}

sealed interface MuteAudioUiState {
    val isMuted: Boolean

    data class Enabled(
        override val isMuted: Boolean,
        val additionalContext: String = ""
    ) : MuteAudioUiState

    data class Disabled(
        override val isMuted: Boolean,
        val disabledRationale: DisabledRationale
    ) : MuteAudioUiState
}

// ////////////////////////////////////////////////////////////
//
// Settings that DON'T currently depend on constraints
//
// ////////////////////////////////////////////////////////////

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

sealed interface MaxVideoDurationUiState {
    data class Enabled(
        val currentMaxDurationMillis: Long,
        val additionalContext: String = ""
    ) : MaxVideoDurationUiState
}

/**
 * Settings Ui State for testing, based on Typical System Constraints.
 * @see[com.google.jetpackcamera.settings.model.SystemConstraints]
 */
val TYPICAL_SETTINGS_UISTATE = SettingsUiState.Enabled(
    aspectRatioUiState = AspectRatioUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
    captureModeUiState = CaptureModeUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
    darkModeUiState = DarkModeUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.darkMode),
    muteAudioUiState = MuteAudioUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.audioMuted),
    flashUiState =
    FlashUiState.Enabled(currentFlashMode = DEFAULT_CAMERA_APP_SETTINGS.flashMode),
    fpsUiState = FpsUiState.Enabled(
        currentSelection = DEFAULT_CAMERA_APP_SETTINGS.targetFrameRate,
        fpsAutoState = SingleSelectableState.Selectable,
        fpsFifteenState = SingleSelectableState.Selectable,
        fpsThirtyState = SingleSelectableState.Selectable,
        fpsSixtyState = SingleSelectableState.Disabled(
            DeviceUnsupportedRationale(R.string.fps_rationale_prefix)
        )
    ),
    lensFlipUiState = FlipLensUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.cameraLensFacing),
    maxVideoDurationUiState = MaxVideoDurationUiState.Enabled(UNLIMITED_VIDEO_DURATION),
    stabilizationUiState =
    StabilizationUiState.Disabled(
        DeviceUnsupportedRationale(R.string.stabilization_rationale_prefix)
    )
)
